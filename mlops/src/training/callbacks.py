"""
Training Callbacks Module

This module provides callback classes for training events
including checkpointing, early stopping, and W&B logging.
"""

import os
from abc import ABC, abstractmethod
from pathlib import Path
from typing import Any, Dict, List, Optional, TYPE_CHECKING

import torch

from src.utils.logging import get_logger

logger = get_logger(__name__)

if TYPE_CHECKING:
    from src.training.trainer import Trainer

try:
    import wandb
    WANDB_AVAILABLE = True
except ImportError:
    WANDB_AVAILABLE = False


class Callback(ABC):
    """Abstract base class for callbacks."""

    def on_train_begin(self, trainer: "Trainer") -> None:
        """Called at the beginning of training."""
        pass

    def on_train_end(self, trainer: "Trainer") -> None:
        """Called at the end of training."""
        pass

    def on_epoch_begin(self, trainer: "Trainer", epoch: int) -> None:
        """Called at the beginning of each epoch."""
        pass

    def on_epoch_end(
        self,
        trainer: "Trainer",
        epoch: int,
        metrics: Dict[str, float]
    ) -> None:
        """Called at the end of each epoch."""
        pass

    def on_batch_begin(self, trainer: "Trainer", batch_idx: int) -> None:
        """Called at the beginning of each batch."""
        pass

    def on_batch_end(
        self,
        trainer: "Trainer",
        batch_idx: int,
        loss: float
    ) -> None:
        """Called at the end of each batch."""
        pass


class CallbackList:
    """Container for managing multiple callbacks."""

    def __init__(self, callbacks: Optional[List[Callback]] = None):
        """
        Initialize callback list.

        Args:
            callbacks: List of callbacks
        """
        self.callbacks = callbacks or []
        self.should_stop = False

    def append(self, callback: Callback) -> None:
        """Add a callback."""
        self.callbacks.append(callback)

    def on_train_begin(self, trainer: "Trainer") -> None:
        """Propagate train begin event."""
        for callback in self.callbacks:
            callback.on_train_begin(trainer)

    def on_train_end(self, trainer: "Trainer") -> None:
        """Propagate train end event."""
        for callback in self.callbacks:
            callback.on_train_end(trainer)

    def on_epoch_begin(self, trainer: "Trainer", epoch: int) -> None:
        """Propagate epoch begin event."""
        for callback in self.callbacks:
            callback.on_epoch_begin(trainer, epoch)

    def on_epoch_end(
        self,
        trainer: "Trainer",
        epoch: int,
        metrics: Dict[str, float]
    ) -> None:
        """Propagate epoch end event."""
        for callback in self.callbacks:
            callback.on_epoch_end(trainer, epoch, metrics)

            # Check for early stopping
            if isinstance(callback, EarlyStoppingCallback) and callback.should_stop:
                self.should_stop = True

    def on_batch_begin(self, trainer: "Trainer", batch_idx: int) -> None:
        """Propagate batch begin event."""
        for callback in self.callbacks:
            callback.on_batch_begin(trainer, batch_idx)

    def on_batch_end(
        self,
        trainer: "Trainer",
        batch_idx: int,
        loss: float
    ) -> None:
        """Propagate batch end event."""
        for callback in self.callbacks:
            callback.on_batch_end(trainer, batch_idx, loss)


class CheckpointCallback(Callback):
    """Callback for saving model checkpoints."""

    def __init__(
        self,
        save_dir: str = "./checkpoints",
        frequency: int = 5,
        save_best: bool = True,
        monitor: str = "val_loss",
        mode: str = "min",
        keep_last: int = 3
    ):
        """
        Initialize checkpoint callback.

        Args:
            save_dir: Directory to save checkpoints
            frequency: Save every N epochs
            save_best: Whether to save best model
            monitor: Metric to monitor for best model
            mode: 'min' or 'max'
            keep_last: Number of recent checkpoints to keep
        """
        self.save_dir = Path(save_dir)
        self.frequency = frequency
        self.save_best = save_best
        self.monitor = monitor
        self.mode = mode
        self.keep_last = keep_last

        self.best_metric = float("inf") if mode == "min" else float("-inf")
        self.saved_checkpoints: List[str] = []

    def on_train_begin(self, trainer: "Trainer") -> None:
        """Create checkpoint directory."""
        self.save_dir.mkdir(parents=True, exist_ok=True)

    def on_epoch_end(
        self,
        trainer: "Trainer",
        epoch: int,
        metrics: Dict[str, float]
    ) -> None:
        """Save checkpoint if conditions are met."""
        # Regular checkpoint
        if (epoch + 1) % self.frequency == 0:
            path = self.save_dir / f"checkpoint_epoch_{epoch + 1}.pt"
            trainer.save_checkpoint(str(path))
            self._manage_checkpoints(str(path))

        # Best model checkpoint
        if self.save_best and self.monitor in metrics:
            current_metric = metrics[self.monitor]
            is_best = (
                (self.mode == "min" and current_metric < self.best_metric) or
                (self.mode == "max" and current_metric > self.best_metric)
            )

            if is_best:
                self.best_metric = current_metric
                trainer.best_metric = current_metric
                trainer.best_epoch = epoch

                path = self.save_dir / "best_model.pt"
                trainer.save_checkpoint(str(path))
                logger.info(f"Saved best model with {self.monitor}={current_metric:.4f}")

    def _manage_checkpoints(self, new_path: str) -> None:
        """Manage checkpoint files to keep only recent ones."""
        self.saved_checkpoints.append(new_path)

        while len(self.saved_checkpoints) > self.keep_last:
            old_path = self.saved_checkpoints.pop(0)
            if os.path.exists(old_path) and "best_model" not in old_path:
                os.remove(old_path)
                logger.debug(f"Removed old checkpoint: {old_path}")


class EarlyStoppingCallback(Callback):
    """Callback for early stopping based on metric improvement."""

    def __init__(
        self,
        patience: int = 10,
        min_delta: float = 0.001,
        monitor: str = "val_loss",
        mode: str = "min"
    ):
        """
        Initialize early stopping callback.

        Args:
            patience: Number of epochs to wait for improvement
            min_delta: Minimum change to qualify as improvement
            monitor: Metric to monitor
            mode: 'min' or 'max'
        """
        self.patience = patience
        self.min_delta = min_delta
        self.monitor = monitor
        self.mode = mode

        self.best_metric = float("inf") if mode == "min" else float("-inf")
        self.counter = 0
        self.should_stop = False

    def on_epoch_end(
        self,
        trainer: "Trainer",
        epoch: int,
        metrics: Dict[str, float]
    ) -> None:
        """Check if training should stop."""
        if self.monitor not in metrics:
            return

        current_metric = metrics[self.monitor]

        if self.mode == "min":
            improved = current_metric < (self.best_metric - self.min_delta)
        else:
            improved = current_metric > (self.best_metric + self.min_delta)

        if improved:
            self.best_metric = current_metric
            self.counter = 0
        else:
            self.counter += 1

            if self.counter >= self.patience:
                self.should_stop = True
                logger.info(
                    f"Early stopping: {self.monitor} did not improve for "
                    f"{self.patience} epochs"
                )


class WandBCallback(Callback):
    """Callback for Weights & Biases logging."""

    def __init__(self, log_frequency: int = 10):
        """
        Initialize W&B callback.

        Args:
            log_frequency: Log metrics every N batches
        """
        self.log_frequency = log_frequency

    def on_train_begin(self, trainer: "Trainer") -> None:
        """Log training start."""
        if not WANDB_AVAILABLE or wandb.run is None:
            return

        # Log model summary
        wandb.run.summary["model_parameters"] = trainer.model.num_trainable_parameters
        wandb.run.summary["device"] = str(trainer.device)

    def on_epoch_end(
        self,
        trainer: "Trainer",
        epoch: int,
        metrics: Dict[str, float]
    ) -> None:
        """Log epoch metrics."""
        if not WANDB_AVAILABLE or wandb.run is None:
            return

        # Metrics are logged in trainer, but we can add more here
        pass

    def on_train_end(self, trainer: "Trainer") -> None:
        """Log training summary."""
        if not WANDB_AVAILABLE or wandb.run is None:
            return

        wandb.run.summary["best_metric"] = trainer.best_metric
        wandb.run.summary["best_epoch"] = trainer.best_epoch
        wandb.run.summary["total_epochs"] = trainer.current_epoch + 1


class LearningRateMonitorCallback(Callback):
    """Callback for monitoring learning rate changes."""

    def on_epoch_end(
        self,
        trainer: "Trainer",
        epoch: int,
        metrics: Dict[str, float]
    ) -> None:
        """Log learning rate."""
        lr = trainer.optimizer.param_groups[0]["lr"]
        logger.debug(f"Epoch {epoch + 1} - Learning rate: {lr:.6f}")

        if WANDB_AVAILABLE and wandb.run is not None:
            wandb.log({"learning_rate": lr}, step=epoch)


class GradientMonitorCallback(Callback):
    """Callback for monitoring gradient statistics."""

    def __init__(self, log_frequency: int = 100):
        """
        Initialize gradient monitor.

        Args:
            log_frequency: Log every N batches
        """
        self.log_frequency = log_frequency
        self.batch_count = 0

    def on_batch_end(
        self,
        trainer: "Trainer",
        batch_idx: int,
        loss: float
    ) -> None:
        """Log gradient statistics."""
        self.batch_count += 1

        if self.batch_count % self.log_frequency != 0:
            return

        total_norm = 0.0
        for p in trainer.model.parameters():
            if p.grad is not None:
                param_norm = p.grad.data.norm(2)
                total_norm += param_norm.item() ** 2

        total_norm = total_norm ** 0.5

        if WANDB_AVAILABLE and wandb.run is not None:
            wandb.log({
                "gradient_norm": total_norm,
                "batch_loss": loss
            }, step=trainer.global_step)


class ModelArtifactCallback(Callback):
    """Callback for saving model artifacts to W&B."""

    def __init__(
        self,
        artifact_name: str = "model",
        artifact_type: str = "model"
    ):
        """
        Initialize model artifact callback.

        Args:
            artifact_name: Name for the artifact
            artifact_type: Type of artifact
        """
        self.artifact_name = artifact_name
        self.artifact_type = artifact_type

    def on_train_end(self, trainer: "Trainer") -> None:
        """Save model as W&B artifact."""
        if not WANDB_AVAILABLE or wandb.run is None:
            return

        # Save best model as artifact
        checkpoint_path = Path(trainer.config.checkpointing.save_dir) / "best_model.pt"

        if checkpoint_path.exists():
            artifact = wandb.Artifact(
                name=self.artifact_name,
                type=self.artifact_type,
                metadata={
                    "best_metric": trainer.best_metric,
                    "best_epoch": trainer.best_epoch,
                    "architecture": trainer.config.model.get("architecture", "unknown")
                }
            )
            artifact.add_file(str(checkpoint_path))
            wandb.log_artifact(artifact)

            logger.info(f"Logged model artifact: {self.artifact_name}")
