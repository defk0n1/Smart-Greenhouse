"""
Distributed Training Module

This module provides utilities for distributed multi-GPU training
using PyTorch's DistributedDataParallel.
"""

import os
from typing import Any, Dict, Optional

import torch
import torch.distributed as dist
import torch.nn as nn
from omegaconf import DictConfig
from torch.nn.parallel import DistributedDataParallel as DDP
from torch.utils.data import DataLoader, DistributedSampler

from src.models.base import BaseModel
from src.utils.logging import get_logger

logger = get_logger(__name__)


def setup_distributed(
    backend: str = "nccl",
    init_method: str = "env://",
    world_size: Optional[int] = None,
    rank: Optional[int] = None
) -> Dict[str, Any]:
    """
    Initialize distributed training environment.

    Args:
        backend: Distributed backend (nccl, gloo, mpi)
        init_method: Initialization method
        world_size: Total number of processes
        rank: Current process rank

    Returns:
        Dictionary with distributed setup info
    """
    # Get world size and rank from environment if not provided
    if world_size is None:
        world_size = int(os.environ.get("WORLD_SIZE", 1))
    if rank is None:
        rank = int(os.environ.get("RANK", 0))

    local_rank = int(os.environ.get("LOCAL_RANK", 0))

    # Initialize process group
    if not dist.is_initialized():
        dist.init_process_group(
            backend=backend,
            init_method=init_method,
            world_size=world_size,
            rank=rank
        )

    # Set device for this process
    if torch.cuda.is_available():
        torch.cuda.set_device(local_rank)
        device = torch.device(f"cuda:{local_rank}")
    else:
        device = torch.device("cpu")

    info = {
        "world_size": world_size,
        "rank": rank,
        "local_rank": local_rank,
        "device": device,
        "is_main_process": rank == 0
    }

    logger.info(f"Distributed setup complete: {info}")

    return info


def cleanup_distributed() -> None:
    """Clean up distributed training."""
    if dist.is_initialized():
        dist.destroy_process_group()
        logger.info("Distributed cleanup complete")


def is_main_process() -> bool:
    """Check if this is the main process."""
    if not dist.is_initialized():
        return True
    return dist.get_rank() == 0


def get_world_size() -> int:
    """Get world size."""
    if not dist.is_initialized():
        return 1
    return dist.get_world_size()


def get_rank() -> int:
    """Get current rank."""
    if not dist.is_initialized():
        return 0
    return dist.get_rank()


def synchronize() -> None:
    """Synchronize all processes."""
    if not dist.is_initialized():
        return
    dist.barrier()


def all_reduce_tensor(tensor: torch.Tensor, op: str = "mean") -> torch.Tensor:
    """
    All-reduce a tensor across all processes.

    Args:
        tensor: Tensor to reduce
        op: Reduction operation (mean, sum, max, min)

    Returns:
        Reduced tensor
    """
    if not dist.is_initialized():
        return tensor

    tensor = tensor.clone()

    if op == "sum":
        dist.all_reduce(tensor, op=dist.ReduceOp.SUM)
    elif op == "mean":
        dist.all_reduce(tensor, op=dist.ReduceOp.SUM)
        tensor /= get_world_size()
    elif op == "max":
        dist.all_reduce(tensor, op=dist.ReduceOp.MAX)
    elif op == "min":
        dist.all_reduce(tensor, op=dist.ReduceOp.MIN)
    else:
        raise ValueError(f"Unknown reduction operation: {op}")

    return tensor


def all_gather_object(obj: Any) -> list:
    """
    Gather objects from all processes.

    Args:
        obj: Object to gather

    Returns:
        List of objects from all processes
    """
    if not dist.is_initialized():
        return [obj]

    world_size = get_world_size()
    output = [None] * world_size
    dist.all_gather_object(output, obj)

    return output


class DistributedModel:
    """
    Wrapper for distributed model management.
    """

    def __init__(
        self,
        model: BaseModel,
        config: DictConfig,
        device: Optional[torch.device] = None
    ):
        """
        Initialize distributed model wrapper.

        Args:
            model: Base model
            config: Distributed configuration
            device: Device to use
        """
        self.base_model = model
        self.config = config
        self.device = device or torch.device("cuda" if torch.cuda.is_available() else "cpu")

        self.ddp_model: Optional[DDP] = None
        self._setup_distributed()

    def _setup_distributed(self) -> None:
        """Set up distributed training."""
        dist_config = self.config.distributed

        if not dist_config.get("enabled", False):
            self.base_model = self.base_model.to(self.device)
            return

        # Setup distributed environment
        self.dist_info = setup_distributed(
            backend=dist_config.get("backend", "nccl")
        )

        # Move model to device
        self.base_model = self.base_model.to(self.dist_info["device"])

        # Wrap with DDP
        self.ddp_model = DDP(
            self.base_model,
            device_ids=[self.dist_info["local_rank"]],
            find_unused_parameters=dist_config.get("find_unused_parameters", False)
        )

        # Sync batch norm if enabled
        if dist_config.get("sync_batch_norm", True):
            self.ddp_model = nn.SyncBatchNorm.convert_sync_batchnorm(self.ddp_model)

        logger.info(f"Distributed model setup complete on rank {self.dist_info['rank']}")

    @property
    def model(self) -> nn.Module:
        """Get the model (DDP or base)."""
        if self.ddp_model is not None:
            return self.ddp_model
        return self.base_model

    @property
    def module(self) -> BaseModel:
        """Get the underlying module (unwrapped from DDP)."""
        if self.ddp_model is not None:
            return self.ddp_model.module
        return self.base_model

    def save_checkpoint(self, path: str, **kwargs: Any) -> None:
        """Save checkpoint (only on main process)."""
        if is_main_process():
            self.module.save(path, **kwargs)

    def load_checkpoint(self, path: str, **kwargs: Any) -> Dict[str, Any]:
        """Load checkpoint."""
        # Load on all processes
        checkpoint = self.module.load(path, **kwargs)

        # Synchronize
        synchronize()

        return checkpoint


def create_distributed_data_loader(
    dataset: torch.utils.data.Dataset,
    batch_size: int,
    shuffle: bool = True,
    num_workers: int = 4,
    pin_memory: bool = True,
    drop_last: bool = False
) -> DataLoader:
    """
    Create a distributed data loader.

    Args:
        dataset: Dataset to load
        batch_size: Batch size per GPU
        shuffle: Whether to shuffle
        num_workers: Number of data loading workers
        pin_memory: Whether to pin memory
        drop_last: Whether to drop last incomplete batch

    Returns:
        DataLoader with DistributedSampler if distributed
    """
    sampler = None

    if dist.is_initialized():
        sampler = DistributedSampler(
            dataset,
            num_replicas=get_world_size(),
            rank=get_rank(),
            shuffle=shuffle
        )
        # Don't shuffle in DataLoader when using sampler
        shuffle = False

    return DataLoader(
        dataset,
        batch_size=batch_size,
        shuffle=shuffle,
        sampler=sampler,
        num_workers=num_workers,
        pin_memory=pin_memory,
        drop_last=drop_last
    )


class DistributedTrainer:
    """
    Extended trainer for distributed training.
    """

    def __init__(
        self,
        model: BaseModel,
        config: DictConfig,
        train_dataset: torch.utils.data.Dataset,
        val_dataset: Optional[torch.utils.data.Dataset] = None
    ):
        """
        Initialize distributed trainer.

        Args:
            model: Model to train
            config: Training configuration
            train_dataset: Training dataset
            val_dataset: Validation dataset
        """
        self.config = config
        dist_config = config.distributed

        # Setup distributed
        if dist_config.get("enabled", False):
            self.dist_info = setup_distributed(
                backend=dist_config.get("backend", "nccl")
            )
            self.device = self.dist_info["device"]
        else:
            self.dist_info = None
            self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

        # Create distributed model
        self.distributed_model = DistributedModel(model, config, self.device)
        self.model = self.distributed_model.model

        # Create data loaders
        batch_size = config.training.batch_size
        dataloader_config = config.get("dataloader", {})

        self.train_loader = create_distributed_data_loader(
            train_dataset,
            batch_size=batch_size,
            shuffle=True,
            num_workers=dataloader_config.get("num_workers", 4),
            pin_memory=dataloader_config.get("pin_memory", True),
            drop_last=dataloader_config.get("drop_last", False)
        )

        if val_dataset is not None:
            self.val_loader = create_distributed_data_loader(
                val_dataset,
                batch_size=config.get("validation", {}).get("batch_size", batch_size * 2),
                shuffle=False,
                num_workers=dataloader_config.get("num_workers", 4),
                pin_memory=dataloader_config.get("pin_memory", True),
                drop_last=False
            )
        else:
            self.val_loader = None

    def train(self) -> Dict[str, Any]:
        """Run distributed training."""
        from src.training.trainer import Trainer

        # Create trainer with distributed model
        trainer = Trainer(
            model=self.model,
            config=self.config,
            train_loader=self.train_loader,
            val_loader=self.val_loader,
            device=str(self.device)
        )

        # Train
        results = trainer.train()

        # Cleanup
        if self.dist_info is not None:
            cleanup_distributed()

        return results
