"""
Training Module

This module provides training pipelines and utilities for PyTorch models.
"""

from src.training.trainer import Trainer, create_trainer
from src.training.callbacks import (
    Callback,
    CallbackList,
    CheckpointCallback,
    EarlyStoppingCallback,
    WandBCallback,
    LearningRateMonitorCallback,
    GradientMonitorCallback,
    ModelArtifactCallback,
)
from src.training.distributed import (
    DistributedModel,
    DistributedTrainer,
    setup_distributed,
    cleanup_distributed,
    is_main_process,
    get_world_size,
    get_rank,
    synchronize,
    all_reduce_tensor,
    create_distributed_data_loader,
)

__all__ = [
    # Trainer
    "Trainer",
    "create_trainer",
    # Callbacks
    "Callback",
    "CallbackList",
    "CheckpointCallback",
    "EarlyStoppingCallback",
    "WandBCallback",
    "LearningRateMonitorCallback",
    "GradientMonitorCallback",
    "ModelArtifactCallback",
    # Distributed
    "DistributedModel",
    "DistributedTrainer",
    "setup_distributed",
    "cleanup_distributed",
    "is_main_process",
    "get_world_size",
    "get_rank",
    "synchronize",
    "all_reduce_tensor",
    "create_distributed_data_loader",
]
