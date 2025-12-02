"""
Tests for Training Module

This module contains unit tests for training components
including trainer, callbacks, and distributed training utilities.
"""

import pytest
import torch
import torch.nn as nn
from torch.utils.data import DataLoader, TensorDataset
from omegaconf import OmegaConf
import tempfile
from pathlib import Path


@pytest.fixture
def training_config():
    """Create training configuration for testing."""
    return OmegaConf.create({
        "model": {
            "architecture": "custom",
            "num_classes": 10,
            "input_dim": 100,
            "custom": {
                "hidden_dims": [64, 32],
                "activation": "relu",
                "batch_norm": True,
            }
        },
        "training": {
            "epochs": 2,
            "batch_size": 16,
            "gradient_accumulation_steps": 1,
            "optimizer": {
                "type": "adam",
                "learning_rate": 0.01,
                "weight_decay": 0.0,
            },
            "scheduler": {
                "type": "cosine",
                "min_lr": 1e-6,
            },
            "loss": {
                "type": "cross_entropy",
            },
            "mixed_precision": {
                "enabled": False,
            },
            "gradient_clipping": {
                "enabled": False,
            },
            "early_stopping": {
                "enabled": False,
            },
        },
        "checkpointing": {
            "enabled": False,
            "save_dir": "/tmp/checkpoints",
            "frequency": 1,
            "save_best": True,
            "monitor": "val_loss",
            "mode": "min",
            "keep_last": 2,
        },
        "distributed": {
            "enabled": False,
        },
    })


@pytest.fixture
def sample_model():
    """Create sample model for testing."""
    from src.models.architectures import CustomMLP

    config = OmegaConf.create({"architecture": "custom"})
    return CustomMLP(
        config=config,
        input_dim=100,
        num_classes=10,
        hidden_dims=[64, 32],
    )


@pytest.fixture
def sample_data_loaders():
    """Create sample data loaders for testing."""
    # Create random data
    X_train = torch.randn(64, 100)
    y_train = torch.randint(0, 10, (64,))
    X_val = torch.randn(16, 100)
    y_val = torch.randint(0, 10, (16,))

    train_dataset = TensorDataset(X_train, y_train)
    val_dataset = TensorDataset(X_val, y_val)

    train_loader = DataLoader(train_dataset, batch_size=16, shuffle=True)
    val_loader = DataLoader(val_dataset, batch_size=16)

    return train_loader, val_loader


class TestTrainer:
    """Tests for Trainer class."""

    def test_trainer_initialization(self, sample_model, training_config, sample_data_loaders):
        """Test trainer initialization."""
        from src.training.trainer import Trainer

        train_loader, val_loader = sample_data_loaders

        trainer = Trainer(
            model=sample_model,
            config=training_config,
            train_loader=train_loader,
            val_loader=val_loader,
        )

        assert trainer.model is not None
        assert trainer.optimizer is not None
        assert trainer.criterion is not None

    def test_trainer_device_detection(self, sample_model, training_config, sample_data_loaders):
        """Test automatic device detection."""
        from src.training.trainer import Trainer

        train_loader, val_loader = sample_data_loaders

        trainer = Trainer(
            model=sample_model,
            config=training_config,
            train_loader=train_loader,
            val_loader=val_loader,
        )

        # Device should be detected
        assert trainer.device in ["cuda", "mps", "cpu"]

    def test_trainer_optimizer_creation(self, sample_model, training_config, sample_data_loaders):
        """Test optimizer creation from config."""
        from src.training.trainer import Trainer

        train_loader, val_loader = sample_data_loaders

        for opt_type in ["adam", "adamw", "sgd"]:
            config = training_config.copy()
            config.training.optimizer.type = opt_type

            trainer = Trainer(
                model=sample_model,
                config=config,
                train_loader=train_loader,
                val_loader=val_loader,
            )

            assert trainer.optimizer is not None

    def test_trainer_scheduler_creation(self, sample_model, training_config, sample_data_loaders):
        """Test scheduler creation from config."""
        from src.training.trainer import Trainer

        train_loader, val_loader = sample_data_loaders

        for sched_type in ["cosine", "step"]:
            config = training_config.copy()
            config.training.scheduler.type = sched_type

            trainer = Trainer(
                model=sample_model,
                config=config,
                train_loader=train_loader,
                val_loader=val_loader,
            )

            assert trainer.scheduler is not None

    def test_trainer_train_step(self, sample_model, training_config, sample_data_loaders):
        """Test single training step."""
        from src.training.trainer import Trainer

        train_loader, val_loader = sample_data_loaders

        trainer = Trainer(
            model=sample_model,
            config=training_config,
            train_loader=train_loader,
            val_loader=val_loader,
        )

        # Get a batch
        batch = next(iter(train_loader))
        trainer.model.train()

        # Execute training step
        loss = trainer._train_step(batch, 0)

        assert loss > 0
        assert isinstance(loss, float)

    def test_trainer_validation(self, sample_model, training_config, sample_data_loaders):
        """Test validation."""
        from src.training.trainer import Trainer

        train_loader, val_loader = sample_data_loaders

        trainer = Trainer(
            model=sample_model,
            config=training_config,
            train_loader=train_loader,
            val_loader=val_loader,
        )

        val_metrics = trainer._validate()

        assert "val_loss" in val_metrics
        assert "val_accuracy" in val_metrics
        assert val_metrics["val_loss"] > 0
        assert 0 <= val_metrics["val_accuracy"] <= 1

    def test_trainer_checkpoint_save_load(self, sample_model, training_config, sample_data_loaders):
        """Test checkpoint saving and loading."""
        from src.training.trainer import Trainer

        train_loader, val_loader = sample_data_loaders

        trainer = Trainer(
            model=sample_model,
            config=training_config,
            train_loader=train_loader,
            val_loader=val_loader,
        )

        # Train for a bit
        trainer._train_epoch()

        with tempfile.TemporaryDirectory() as tmpdir:
            checkpoint_path = Path(tmpdir) / "checkpoint.pt"

            # Save
            trainer.save_checkpoint(str(checkpoint_path))
            assert checkpoint_path.exists()

            # Modify trainer state
            trainer.current_epoch = 99

            # Load
            trainer.load_checkpoint(str(checkpoint_path))

            # State should be restored
            assert trainer.current_epoch == 0


class TestCallbacks:
    """Tests for training callbacks."""

    def test_checkpoint_callback(self, sample_model, training_config, sample_data_loaders):
        """Test checkpoint callback."""
        from src.training.trainer import Trainer
        from src.training.callbacks import CheckpointCallback

        train_loader, val_loader = sample_data_loaders

        with tempfile.TemporaryDirectory() as tmpdir:
            callback = CheckpointCallback(
                save_dir=tmpdir,
                frequency=1,
                save_best=True,
                monitor="val_loss",
                mode="min",
                keep_last=2,
            )

            config = training_config.copy()
            config.checkpointing.enabled = False  # Disable default

            trainer = Trainer(
                model=sample_model,
                config=config,
                train_loader=train_loader,
                val_loader=val_loader,
                callbacks=[callback],
            )

            # Simulate epoch end
            callback.on_train_begin(trainer)
            callback.on_epoch_end(trainer, 0, {"val_loss": 0.5})

            # Check checkpoint was saved
            checkpoints = list(Path(tmpdir).glob("*.pt"))
            assert len(checkpoints) >= 1

    def test_early_stopping_callback(self, sample_model, training_config, sample_data_loaders):
        """Test early stopping callback."""
        from src.training.trainer import Trainer
        from src.training.callbacks import EarlyStoppingCallback

        train_loader, val_loader = sample_data_loaders

        callback = EarlyStoppingCallback(
            patience=2,
            min_delta=0.01,
            monitor="val_loss",
            mode="min",
        )

        config = training_config.copy()
        config.training.early_stopping.enabled = False

        trainer = Trainer(
            model=sample_model,
            config=config,
            train_loader=train_loader,
            val_loader=val_loader,
            callbacks=[callback],
        )

        # Simulate improving metrics
        callback.on_epoch_end(trainer, 0, {"val_loss": 1.0})
        assert not callback.should_stop

        callback.on_epoch_end(trainer, 1, {"val_loss": 0.5})
        assert not callback.should_stop

        # Simulate non-improving metrics
        callback.on_epoch_end(trainer, 2, {"val_loss": 0.5})
        callback.on_epoch_end(trainer, 3, {"val_loss": 0.5})

        assert callback.should_stop

    def test_callback_list(self, sample_model, training_config, sample_data_loaders):
        """Test callback list management."""
        from src.training.callbacks import Callback, CallbackList

        class DummyCallback(Callback):
            def __init__(self):
                self.train_begin_called = False
                self.epoch_end_called = False

            def on_train_begin(self, trainer):
                self.train_begin_called = True

            def on_epoch_end(self, trainer, epoch, metrics):
                self.epoch_end_called = True

        cb1 = DummyCallback()
        cb2 = DummyCallback()

        callback_list = CallbackList([cb1, cb2])

        # Call methods
        callback_list.on_train_begin(None)
        callback_list.on_epoch_end(None, 0, {})

        # Check all callbacks were called
        assert cb1.train_begin_called
        assert cb2.train_begin_called
        assert cb1.epoch_end_called
        assert cb2.epoch_end_called


class TestDistributed:
    """Tests for distributed training utilities."""

    def test_distributed_utils_available(self):
        """Test distributed utilities are importable."""
        from src.training.distributed import (
            setup_distributed,
            cleanup_distributed,
            is_main_process,
            get_world_size,
            get_rank,
        )

        # Without distributed init, should return defaults
        assert is_main_process() is True
        assert get_world_size() == 1
        assert get_rank() == 0

    def test_distributed_data_loader_creation(self):
        """Test creating distributed data loader."""
        from src.training.distributed import create_distributed_data_loader

        # Create sample dataset
        X = torch.randn(100, 10)
        y = torch.randint(0, 2, (100,))
        dataset = TensorDataset(X, y)

        # Create loader (without actual distributed setup)
        loader = create_distributed_data_loader(
            dataset,
            batch_size=16,
            shuffle=True,
            num_workers=0,
        )

        assert loader is not None
        assert len(loader) > 0


class TestMetrics:
    """Tests for training metrics."""

    def test_accuracy_metric(self):
        """Test accuracy metric computation."""
        from src.evaluation.metrics import Accuracy

        metric = Accuracy()

        # Simulate predictions
        predictions = torch.tensor([[0.9, 0.1], [0.1, 0.9], [0.8, 0.2]])
        targets = torch.tensor([0, 1, 0])

        metric.update(predictions, targets)
        accuracy = metric.compute()

        assert accuracy == 1.0  # All correct

    def test_f1_metric(self):
        """Test F1 score metric computation."""
        from src.evaluation.metrics import F1Score

        metric = F1Score(average="macro")

        # Simulate predictions
        predictions = torch.tensor([[0.9, 0.1], [0.1, 0.9], [0.8, 0.2], [0.2, 0.8]])
        targets = torch.tensor([0, 1, 0, 1])

        metric.update(predictions, targets)
        f1 = metric.compute()

        assert 0 <= f1 <= 1

    def test_metric_collection(self):
        """Test metric collection."""
        from src.evaluation.metrics import MetricCollection, Accuracy, F1Score

        collection = MetricCollection([Accuracy(), F1Score()])

        predictions = torch.tensor([[0.9, 0.1], [0.1, 0.9]])
        targets = torch.tensor([0, 1])

        collection.update(predictions, targets)
        metrics = collection.compute()

        assert "accuracy" in metrics
        assert "f1" in metrics


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
