"""
Trainer Module

This module provides the main training pipeline with W&B integration,
checkpointing, and comprehensive logging.
"""

import os
import time
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple, Union

import torch
import torch.nn as nn
from omegaconf import DictConfig, OmegaConf
from torch.cuda.amp import GradScaler, autocast
from torch.optim import Optimizer
from torch.optim.lr_scheduler import _LRScheduler
from torch.utils.data import DataLoader
from tqdm import tqdm

from src.models.base import BaseModel
from src.training.callbacks import (
    Callback,
    CallbackList,
    CheckpointCallback,
    EarlyStoppingCallback,
    WandBCallback,
)
from src.utils.config import load_config
from src.utils.logging import get_logger

logger = get_logger(__name__)

try:
    import wandb
    WANDB_AVAILABLE = True
except ImportError:
    WANDB_AVAILABLE = False
    logger.warning("wandb not available. Install with: pip install wandb")


class Trainer:
    """
    Main trainer class for PyTorch models.

    Features:
    - W&B integration for experiment tracking
    - Mixed precision training
    - Gradient accumulation
    - Checkpointing with resume capability
    - Early stopping
    - Learning rate scheduling
    """

    def __init__(
        self,
        model: BaseModel,
        config: DictConfig,
        train_loader: Optional[DataLoader] = None,
        val_loader: Optional[DataLoader] = None,
        optimizer: Optional[Optimizer] = None,
        scheduler: Optional[_LRScheduler] = None,
        criterion: Optional[nn.Module] = None,
        callbacks: Optional[List[Callback]] = None,
        device: Optional[str] = None,
    ):
        """
        Initialize trainer.

        Args:
            model: Model to train
            config: Training configuration
            train_loader: Training data loader
            val_loader: Validation data loader
            optimizer: Optimizer (created from config if None)
            scheduler: LR scheduler (created from config if None)
            criterion: Loss function (created from config if None)
            callbacks: List of callbacks
            device: Device to use (auto-detected if None)
        """
        self.config = config
        self.device = device or self._get_device()

        # Move model to device
        self.model = model.to(self.device)

        # Data loaders
        self.train_loader = train_loader
        self.val_loader = val_loader

        # Training components
        self.optimizer = optimizer or self._create_optimizer()
        self.scheduler = scheduler or self._create_scheduler()
        self.criterion = criterion or self._create_criterion()

        # Mixed precision
        self.use_amp = config.training.get("mixed_precision", {}).get("enabled", False)
        self.scaler = GradScaler() if self.use_amp else None
        self.amp_dtype = self._get_amp_dtype()

        # Training state
        self.current_epoch = 0
        self.global_step = 0
        self.best_metric = float("inf")
        self.best_epoch = 0

        # Callbacks
        self.callbacks = CallbackList(callbacks or self._create_default_callbacks())

        # Gradient accumulation
        self.grad_accum_steps = config.training.get("gradient_accumulation_steps", 1)

        # Gradient clipping
        self.grad_clip_enabled = config.training.get("gradient_clipping", {}).get("enabled", False)
        self.grad_clip_max_norm = config.training.get("gradient_clipping", {}).get("max_norm", 1.0)

        # Initialize W&B
        self._init_wandb()

        logger.info(f"Trainer initialized on device: {self.device}")
        logger.info(f"Mixed precision: {self.use_amp}")
        logger.info(f"Model parameters: {model.num_trainable_parameters:,}")

    def _get_device(self) -> str:
        """Get the best available device."""
        if torch.cuda.is_available():
            return "cuda"
        elif torch.backends.mps.is_available():
            return "mps"
        return "cpu"

    def _get_amp_dtype(self) -> torch.dtype:
        """Get AMP dtype from config."""
        dtype_str = self.config.training.get("mixed_precision", {}).get("dtype", "float16")
        if dtype_str == "bfloat16" and torch.cuda.is_bf16_supported():
            return torch.bfloat16
        return torch.float16

    def _create_optimizer(self) -> Optimizer:
        """Create optimizer from config."""
        opt_config = self.config.training.optimizer
        opt_type = opt_config.get("type", "adamw").lower()
        lr = opt_config.get("learning_rate", 0.001)
        weight_decay = opt_config.get("weight_decay", 0.01)

        if opt_type == "adam":
            return torch.optim.Adam(
                self.model.parameters(),
                lr=lr,
                betas=tuple(opt_config.get("betas", [0.9, 0.999])),
                weight_decay=weight_decay
            )
        elif opt_type == "adamw":
            return torch.optim.AdamW(
                self.model.parameters(),
                lr=lr,
                betas=tuple(opt_config.get("betas", [0.9, 0.999])),
                weight_decay=weight_decay
            )
        elif opt_type == "sgd":
            return torch.optim.SGD(
                self.model.parameters(),
                lr=lr,
                momentum=opt_config.get("momentum", 0.9),
                weight_decay=weight_decay
            )
        else:
            raise ValueError(f"Unknown optimizer: {opt_type}")

    def _create_scheduler(self) -> Optional[_LRScheduler]:
        """Create learning rate scheduler from config."""
        sched_config = self.config.training.get("scheduler", {})
        sched_type = sched_config.get("type", "cosine").lower()

        if sched_type == "cosine":
            return torch.optim.lr_scheduler.CosineAnnealingLR(
                self.optimizer,
                T_max=self.config.training.epochs,
                eta_min=sched_config.get("min_lr", 1e-6)
            )
        elif sched_type == "step":
            return torch.optim.lr_scheduler.StepLR(
                self.optimizer,
                step_size=sched_config.get("step_size", 30),
                gamma=sched_config.get("gamma", 0.1)
            )
        elif sched_type == "plateau":
            return torch.optim.lr_scheduler.ReduceLROnPlateau(
                self.optimizer,
                mode="min",
                patience=sched_config.get("patience", 10),
                factor=sched_config.get("factor", 0.5)
            )
        elif sched_type == "none":
            return None
        else:
            raise ValueError(f"Unknown scheduler: {sched_type}")

    def _create_criterion(self) -> nn.Module:
        """Create loss function from config."""
        loss_config = self.config.training.get("loss", {})
        loss_type = loss_config.get("type", "cross_entropy").lower()

        if loss_type == "cross_entropy":
            label_smoothing = loss_config.get("label_smoothing", 0.0)
            return nn.CrossEntropyLoss(label_smoothing=label_smoothing)
        elif loss_type == "mse":
            return nn.MSELoss()
        elif loss_type == "bce":
            return nn.BCEWithLogitsLoss()
        else:
            raise ValueError(f"Unknown loss type: {loss_type}")

    def _create_default_callbacks(self) -> List[Callback]:
        """Create default callbacks."""
        callbacks = []

        # Checkpoint callback
        if self.config.checkpointing.get("enabled", True):
            callbacks.append(CheckpointCallback(
                save_dir=self.config.checkpointing.get("save_dir", "./checkpoints"),
                frequency=self.config.checkpointing.get("frequency", 5),
                save_best=self.config.checkpointing.get("save_best", True),
                monitor=self.config.checkpointing.get("monitor", "val_loss"),
                mode=self.config.checkpointing.get("mode", "min"),
                keep_last=self.config.checkpointing.get("keep_last", 3)
            ))

        # Early stopping callback
        es_config = self.config.training.get("early_stopping", {})
        if es_config.get("enabled", False):
            callbacks.append(EarlyStoppingCallback(
                patience=es_config.get("patience", 10),
                min_delta=es_config.get("min_delta", 0.001),
                monitor=es_config.get("monitor", "val_loss"),
                mode=es_config.get("mode", "min")
            ))

        # W&B callback
        if WANDB_AVAILABLE:
            callbacks.append(WandBCallback())

        return callbacks

    def _init_wandb(self) -> None:
        """Initialize Weights & Biases."""
        if not WANDB_AVAILABLE:
            return

        wandb_config = self.config.get("wandb", {})
        if wandb_config.get("mode", "online") == "disabled":
            return

        # Initialize W&B run
        wandb.init(
            project=wandb_config.get("project", "mlops-pipeline"),
            entity=wandb_config.get("entity"),
            name=wandb_config.get("name"),
            notes=wandb_config.get("notes"),
            tags=list(wandb_config.get("tags", [])),
            config=OmegaConf.to_container(self.config, resolve=True),
            mode=wandb_config.get("mode", "online"),
            resume=wandb_config.get("resume", "allow"),
        )

        # Watch model
        if wandb_config.get("log_gradients", True):
            wandb.watch(self.model, log="all", log_freq=100)

        logger.info(f"W&B initialized: {wandb.run.name}")

    def train(self) -> Dict[str, Any]:
        """
        Run training loop.

        Returns:
            Dictionary with training results
        """
        logger.info("Starting training...")
        self.callbacks.on_train_begin(self)

        epochs = self.config.training.epochs
        start_time = time.time()

        try:
            for epoch in range(self.current_epoch, epochs):
                self.current_epoch = epoch

                # Train epoch
                train_metrics = self._train_epoch()

                # Validate
                val_metrics = {}
                if self.val_loader is not None:
                    val_metrics = self._validate()

                # Combine metrics
                metrics = {**train_metrics, **val_metrics}
                metrics["epoch"] = epoch
                metrics["learning_rate"] = self.optimizer.param_groups[0]["lr"]

                # Update scheduler
                if self.scheduler is not None:
                    if isinstance(self.scheduler, torch.optim.lr_scheduler.ReduceLROnPlateau):
                        self.scheduler.step(val_metrics.get("val_loss", train_metrics["train_loss"]))
                    else:
                        self.scheduler.step()

                # Callbacks
                self.callbacks.on_epoch_end(self, epoch, metrics)

                # Log to W&B
                if WANDB_AVAILABLE and wandb.run is not None:
                    wandb.log(metrics, step=epoch)

                # Check early stopping
                if self.callbacks.should_stop:
                    logger.info(f"Early stopping triggered at epoch {epoch}")
                    break

                logger.info(
                    f"Epoch {epoch + 1}/{epochs} - "
                    f"train_loss: {train_metrics['train_loss']:.4f} - "
                    f"val_loss: {val_metrics.get('val_loss', 'N/A')}"
                )

        except KeyboardInterrupt:
            logger.info("Training interrupted by user")

        self.callbacks.on_train_end(self)

        total_time = time.time() - start_time
        results = {
            "total_epochs": self.current_epoch + 1,
            "best_epoch": self.best_epoch,
            "best_metric": self.best_metric,
            "total_time": total_time,
        }

        logger.info(f"Training completed in {total_time:.2f}s")

        if WANDB_AVAILABLE and wandb.run is not None:
            wandb.finish()

        return results

    def _train_epoch(self) -> Dict[str, float]:
        """Train for one epoch."""
        self.model.train()
        total_loss = 0.0
        num_batches = 0

        self.callbacks.on_epoch_begin(self, self.current_epoch)

        pbar = tqdm(
            self.train_loader,
            desc=f"Epoch {self.current_epoch + 1}",
            leave=True
        )

        self.optimizer.zero_grad()

        for batch_idx, batch in enumerate(pbar):
            loss = self._train_step(batch, batch_idx)
            total_loss += loss
            num_batches += 1

            self.global_step += 1

            pbar.set_postfix({"loss": loss})

        avg_loss = total_loss / num_batches
        return {"train_loss": avg_loss}

    def _train_step(self, batch: Tuple, batch_idx: int) -> float:
        """Execute single training step."""
        # Unpack batch
        if len(batch) == 2:
            inputs, targets = batch
        else:
            inputs, targets = batch[0], batch[1]

        inputs = inputs.to(self.device)
        targets = targets.to(self.device)

        # Forward pass with optional mixed precision
        if self.use_amp:
            with autocast(dtype=self.amp_dtype):
                outputs = self.model(inputs)
                loss = self.criterion(outputs, targets)
                loss = loss / self.grad_accum_steps
        else:
            outputs = self.model(inputs)
            loss = self.criterion(outputs, targets)
            loss = loss / self.grad_accum_steps

        # Backward pass
        if self.use_amp:
            self.scaler.scale(loss).backward()
        else:
            loss.backward()

        # Gradient accumulation
        if (batch_idx + 1) % self.grad_accum_steps == 0:
            # Gradient clipping
            if self.grad_clip_enabled:
                if self.use_amp:
                    self.scaler.unscale_(self.optimizer)
                torch.nn.utils.clip_grad_norm_(
                    self.model.parameters(),
                    self.grad_clip_max_norm
                )

            # Optimizer step
            if self.use_amp:
                self.scaler.step(self.optimizer)
                self.scaler.update()
            else:
                self.optimizer.step()

            self.optimizer.zero_grad()

        return loss.item() * self.grad_accum_steps

    def _validate(self) -> Dict[str, float]:
        """Run validation."""
        self.model.eval()
        total_loss = 0.0
        correct = 0
        total = 0

        with torch.no_grad():
            for batch in self.val_loader:
                if len(batch) == 2:
                    inputs, targets = batch
                else:
                    inputs, targets = batch[0], batch[1]

                inputs = inputs.to(self.device)
                targets = targets.to(self.device)

                if self.use_amp:
                    with autocast(dtype=self.amp_dtype):
                        outputs = self.model(inputs)
                        loss = self.criterion(outputs, targets)
                else:
                    outputs = self.model(inputs)
                    loss = self.criterion(outputs, targets)

                total_loss += loss.item()

                # Calculate accuracy
                _, predicted = outputs.max(1)
                total += targets.size(0)
                correct += predicted.eq(targets).sum().item()

        avg_loss = total_loss / len(self.val_loader)
        accuracy = correct / total if total > 0 else 0

        return {
            "val_loss": avg_loss,
            "val_accuracy": accuracy
        }

    def save_checkpoint(self, path: str, **extra_state: Any) -> None:
        """Save training checkpoint."""
        checkpoint = {
            "epoch": self.current_epoch,
            "global_step": self.global_step,
            "best_metric": self.best_metric,
            "best_epoch": self.best_epoch,
            "model_state_dict": self.model.state_dict(),
            "optimizer_state_dict": self.optimizer.state_dict(),
            "config": OmegaConf.to_container(self.config, resolve=True),
        }

        if self.scheduler is not None:
            checkpoint["scheduler_state_dict"] = self.scheduler.state_dict()

        if self.scaler is not None:
            checkpoint["scaler_state_dict"] = self.scaler.state_dict()

        checkpoint.update(extra_state)

        torch.save(checkpoint, path)
        logger.info(f"Saved checkpoint to {path}")

    def load_checkpoint(self, path: str) -> None:
        """Load training checkpoint."""
        checkpoint = torch.load(path, map_location=self.device)

        self.model.load_state_dict(checkpoint["model_state_dict"])
        self.optimizer.load_state_dict(checkpoint["optimizer_state_dict"])

        self.current_epoch = checkpoint.get("epoch", 0)
        self.global_step = checkpoint.get("global_step", 0)
        self.best_metric = checkpoint.get("best_metric", float("inf"))
        self.best_epoch = checkpoint.get("best_epoch", 0)

        if self.scheduler is not None and "scheduler_state_dict" in checkpoint:
            self.scheduler.load_state_dict(checkpoint["scheduler_state_dict"])

        if self.scaler is not None and "scaler_state_dict" in checkpoint:
            self.scaler.load_state_dict(checkpoint["scaler_state_dict"])

        logger.info(f"Loaded checkpoint from {path} (epoch {self.current_epoch})")


def create_trainer(
    model: BaseModel,
    config_path: str,
    train_loader: Optional[DataLoader] = None,
    val_loader: Optional[DataLoader] = None,
) -> Trainer:
    """
    Factory function to create a trainer.

    Args:
        model: Model to train
        config_path: Path to training configuration
        train_loader: Training data loader
        val_loader: Validation data loader

    Returns:
        Trainer instance
    """
    config = load_config(config_path)
    return Trainer(
        model=model,
        config=config,
        train_loader=train_loader,
        val_loader=val_loader
    )
