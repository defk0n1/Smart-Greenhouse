"""
Model Registry Module

This module provides model registry functionality using W&B
for versioning, metadata storage, and model promotion workflow.
"""

import json
import os
from dataclasses import asdict, dataclass, field
from datetime import datetime
from enum import Enum
from pathlib import Path
from typing import Any, Dict, List, Optional, Union

import torch
from omegaconf import DictConfig, OmegaConf

from src.utils.logging import get_logger

logger = get_logger(__name__)

try:
    import wandb
    WANDB_AVAILABLE = True
except ImportError:
    WANDB_AVAILABLE = False


class ModelStage(Enum):
    """Model deployment stages."""
    DEV = "dev"
    STAGING = "staging"
    PRODUCTION = "production"
    ARCHIVED = "archived"


@dataclass
class ModelMetadata:
    """Metadata for a registered model."""
    name: str
    version: str
    stage: str
    created_at: str
    metrics: Dict[str, float]
    training_config: Dict[str, Any]
    data_version: Optional[str] = None
    git_commit: Optional[str] = None
    description: Optional[str] = None
    tags: List[str] = field(default_factory=list)
    extra: Dict[str, Any] = field(default_factory=dict)

    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary."""
        return asdict(self)

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "ModelMetadata":
        """Create from dictionary."""
        return cls(**data)


class ModelRegistry:
    """
    Model registry for managing model versions and deployments.

    Supports both W&B artifact registry and local file-based registry.
    """

    def __init__(
        self,
        registry_type: str = "wandb",
        local_path: Optional[str] = None,
        project: Optional[str] = None,
        entity: Optional[str] = None
    ):
        """
        Initialize model registry.

        Args:
            registry_type: Type of registry ('wandb' or 'local')
            local_path: Path for local registry
            project: W&B project name
            entity: W&B entity/team name
        """
        self.registry_type = registry_type
        self.local_path = Path(local_path) if local_path else Path("./registry")
        self.project = project or os.environ.get("WANDB_PROJECT", "mlops-pipeline")
        self.entity = entity or os.environ.get("WANDB_ENTITY")

        if registry_type == "local":
            self.local_path.mkdir(parents=True, exist_ok=True)
            self._local_metadata: Dict[str, List[ModelMetadata]] = {}
            self._load_local_metadata()

    def _load_local_metadata(self) -> None:
        """Load local registry metadata."""
        metadata_file = self.local_path / "metadata.json"
        if metadata_file.exists():
            with open(metadata_file, "r") as f:
                data = json.load(f)
                for name, versions in data.items():
                    self._local_metadata[name] = [
                        ModelMetadata.from_dict(v) for v in versions
                    ]

    def _save_local_metadata(self) -> None:
        """Save local registry metadata."""
        metadata_file = self.local_path / "metadata.json"
        data = {
            name: [m.to_dict() for m in versions]
            for name, versions in self._local_metadata.items()
        }
        with open(metadata_file, "w") as f:
            json.dump(data, f, indent=2)

    def register_model(
        self,
        model: torch.nn.Module,
        name: str,
        metrics: Dict[str, float],
        config: Union[Dict, DictConfig],
        checkpoint_path: Optional[str] = None,
        data_version: Optional[str] = None,
        description: Optional[str] = None,
        tags: Optional[List[str]] = None,
        stage: ModelStage = ModelStage.DEV
    ) -> str:
        """
        Register a model in the registry.

        Args:
            model: PyTorch model
            name: Model name
            metrics: Performance metrics
            config: Training configuration
            checkpoint_path: Path to model checkpoint
            data_version: Data version used for training
            description: Model description
            tags: Tags for the model
            stage: Initial deployment stage

        Returns:
            Version string
        """
        version = self._generate_version()
        timestamp = datetime.now().isoformat()

        # Prepare config
        if isinstance(config, DictConfig):
            config = OmegaConf.to_container(config, resolve=True)

        # Create metadata
        metadata = ModelMetadata(
            name=name,
            version=version,
            stage=stage.value,
            created_at=timestamp,
            metrics=metrics,
            training_config=config,
            data_version=data_version,
            git_commit=self._get_git_commit(),
            description=description,
            tags=tags or []
        )

        if self.registry_type == "wandb":
            version = self._register_wandb(model, metadata, checkpoint_path)
        else:
            version = self._register_local(model, metadata, checkpoint_path)

        logger.info(f"Registered model '{name}' version {version}")
        return version

    def _generate_version(self) -> str:
        """Generate version string."""
        return f"v{datetime.now().strftime('%Y%m%d%H%M%S')}"

    def _get_git_commit(self) -> Optional[str]:
        """Get current git commit hash."""
        try:
            import subprocess
            result = subprocess.run(
                ["git", "rev-parse", "HEAD"],
                capture_output=True,
                text=True
            )
            if result.returncode == 0:
                return result.stdout.strip()[:8]
        except Exception:
            pass
        return None

    def _register_wandb(
        self,
        model: torch.nn.Module,
        metadata: ModelMetadata,
        checkpoint_path: Optional[str]
    ) -> str:
        """Register model using W&B."""
        if not WANDB_AVAILABLE:
            raise RuntimeError("wandb not available. Install with: pip install wandb")

        # Initialize W&B if needed
        if wandb.run is None:
            wandb.init(
                project=self.project,
                entity=self.entity,
                job_type="register_model"
            )

        # Create artifact
        artifact = wandb.Artifact(
            name=metadata.name,
            type="model",
            metadata=metadata.to_dict()
        )

        # Save model if no checkpoint provided
        if checkpoint_path is None:
            checkpoint_path = f"/tmp/{metadata.name}_{metadata.version}.pt"
            torch.save(model.state_dict(), checkpoint_path)

        artifact.add_file(checkpoint_path)

        # Log artifact
        wandb.log_artifact(artifact, aliases=[metadata.stage, "latest"])

        return artifact.version

    def _register_local(
        self,
        model: torch.nn.Module,
        metadata: ModelMetadata,
        checkpoint_path: Optional[str]
    ) -> str:
        """Register model locally."""
        # Create model directory
        model_dir = self.local_path / metadata.name / metadata.version
        model_dir.mkdir(parents=True, exist_ok=True)

        # Save model
        model_path = model_dir / "model.pt"
        if checkpoint_path:
            import shutil
            shutil.copy(checkpoint_path, model_path)
        else:
            torch.save(model.state_dict(), model_path)

        # Save metadata
        metadata_path = model_dir / "metadata.json"
        with open(metadata_path, "w") as f:
            json.dump(metadata.to_dict(), f, indent=2)

        # Update registry
        if metadata.name not in self._local_metadata:
            self._local_metadata[metadata.name] = []
        self._local_metadata[metadata.name].append(metadata)
        self._save_local_metadata()

        return metadata.version

    def get_model(
        self,
        name: str,
        version: Optional[str] = None,
        stage: Optional[ModelStage] = None
    ) -> tuple:
        """
        Get a model from the registry.

        Args:
            name: Model name
            version: Specific version (or latest if None)
            stage: Get model at specific stage

        Returns:
            Tuple of (model_state_dict, metadata)
        """
        if self.registry_type == "wandb":
            return self._get_wandb_model(name, version, stage)
        else:
            return self._get_local_model(name, version, stage)

    def _get_wandb_model(
        self,
        name: str,
        version: Optional[str],
        stage: Optional[ModelStage]
    ) -> tuple:
        """Get model from W&B."""
        if not WANDB_AVAILABLE:
            raise RuntimeError("wandb not available")

        api = wandb.Api()

        if version:
            artifact_path = f"{self.entity}/{self.project}/{name}:{version}"
        elif stage:
            artifact_path = f"{self.entity}/{self.project}/{name}:{stage.value}"
        else:
            artifact_path = f"{self.entity}/{self.project}/{name}:latest"

        artifact = api.artifact(artifact_path)
        artifact_dir = artifact.download()

        # Load model
        model_files = list(Path(artifact_dir).glob("*.pt"))
        if not model_files:
            raise FileNotFoundError(f"No model file found in artifact")

        state_dict = torch.load(model_files[0], map_location="cpu")
        metadata = ModelMetadata.from_dict(artifact.metadata)

        return state_dict, metadata

    def _get_local_model(
        self,
        name: str,
        version: Optional[str],
        stage: Optional[ModelStage]
    ) -> tuple:
        """Get model from local registry."""
        if name not in self._local_metadata:
            raise ValueError(f"Model '{name}' not found in registry")

        versions = self._local_metadata[name]

        if version:
            metadata = next((m for m in versions if m.version == version), None)
        elif stage:
            metadata = next(
                (m for m in reversed(versions) if m.stage == stage.value),
                None
            )
        else:
            metadata = versions[-1]  # Latest

        if metadata is None:
            raise ValueError(f"No matching model version found")

        model_path = self.local_path / name / metadata.version / "model.pt"
        state_dict = torch.load(model_path, map_location="cpu")

        return state_dict, metadata

    def promote_model(
        self,
        name: str,
        version: str,
        to_stage: ModelStage,
        metrics_threshold: Optional[Dict[str, float]] = None
    ) -> bool:
        """
        Promote a model to a new stage.

        Args:
            name: Model name
            version: Model version
            to_stage: Target stage
            metrics_threshold: Optional metrics requirements

        Returns:
            True if promotion successful
        """
        # Get model metadata
        _, metadata = self.get_model(name, version)

        # Check metrics threshold
        if metrics_threshold:
            for metric, threshold in metrics_threshold.items():
                if metric in metadata.metrics:
                    if metadata.metrics[metric] < threshold:
                        logger.warning(
                            f"Model does not meet threshold for {metric}: "
                            f"{metadata.metrics[metric]} < {threshold}"
                        )
                        return False

        if self.registry_type == "wandb":
            self._promote_wandb(name, version, to_stage)
        else:
            self._promote_local(name, version, to_stage)

        logger.info(f"Promoted model '{name}' {version} to {to_stage.value}")
        return True

    def _promote_wandb(
        self,
        name: str,
        version: str,
        to_stage: ModelStage
    ) -> None:
        """Promote model in W&B."""
        if not WANDB_AVAILABLE:
            raise RuntimeError("wandb not available")

        api = wandb.Api()
        artifact_path = f"{self.entity}/{self.project}/{name}:{version}"
        artifact = api.artifact(artifact_path)

        # Add stage alias
        artifact.aliases.append(to_stage.value)
        artifact.save()

    def _promote_local(
        self,
        name: str,
        version: str,
        to_stage: ModelStage
    ) -> None:
        """Promote model in local registry."""
        if name not in self._local_metadata:
            raise ValueError(f"Model '{name}' not found")

        for metadata in self._local_metadata[name]:
            if metadata.version == version:
                metadata.stage = to_stage.value
                break

        self._save_local_metadata()

    def list_models(self, name: Optional[str] = None) -> List[ModelMetadata]:
        """
        List registered models.

        Args:
            name: Filter by model name

        Returns:
            List of model metadata
        """
        if self.registry_type == "wandb":
            return self._list_wandb_models(name)
        else:
            return self._list_local_models(name)

    def _list_wandb_models(self, name: Optional[str]) -> List[ModelMetadata]:
        """List models from W&B."""
        if not WANDB_AVAILABLE:
            raise RuntimeError("wandb not available")

        api = wandb.Api()
        collections = api.artifact_type(
            type_name="model",
            project=f"{self.entity}/{self.project}"
        ).collections()

        models = []
        for collection in collections:
            if name and collection.name != name:
                continue

            for artifact in collection.versions():
                metadata = ModelMetadata.from_dict(artifact.metadata)
                models.append(metadata)

        return models

    def _list_local_models(self, name: Optional[str]) -> List[ModelMetadata]:
        """List models from local registry."""
        if name:
            return self._local_metadata.get(name, [])

        models = []
        for versions in self._local_metadata.values():
            models.extend(versions)

        return models

    def get_best_model(
        self,
        name: str,
        metric: str,
        mode: str = "max"
    ) -> tuple:
        """
        Get the best model based on a metric.

        Args:
            name: Model name
            metric: Metric to compare
            mode: 'max' or 'min'

        Returns:
            Tuple of (model_state_dict, metadata)
        """
        models = self.list_models(name)

        if not models:
            raise ValueError(f"No models found for '{name}'")

        # Filter models with the metric
        models_with_metric = [m for m in models if metric in m.metrics]

        if not models_with_metric:
            raise ValueError(f"No models found with metric '{metric}'")

        # Find best
        if mode == "max":
            best = max(models_with_metric, key=lambda m: m.metrics[metric])
        else:
            best = min(models_with_metric, key=lambda m: m.metrics[metric])

        return self.get_model(name, best.version)


def create_registry(config: Optional[DictConfig] = None) -> ModelRegistry:
    """
    Factory function to create a model registry.

    Args:
        config: Optional configuration

    Returns:
        ModelRegistry instance
    """
    if config is None:
        return ModelRegistry()

    registry_config = config.get("registry", {})

    return ModelRegistry(
        registry_type=registry_config.get("type", "wandb"),
        local_path=registry_config.get("local_path"),
        project=registry_config.get("project"),
        entity=registry_config.get("entity")
    )
