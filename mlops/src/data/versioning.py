"""
Data Versioning Module

This module provides data versioning capabilities for tracking
data changes and maintaining data lineage.
"""

import hashlib
import json
import logging
from dataclasses import asdict, dataclass, field
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Optional, Union

import pandas as pd
from omegaconf import DictConfig, OmegaConf

from src.utils.logging import get_logger

logger = get_logger(__name__)


@dataclass
class DataVersion:
    """Represents a version of a dataset."""
    version_id: str
    data_hash: str
    created_at: str
    row_count: int
    column_count: int
    schema: Dict[str, str]
    statistics: Dict[str, Dict[str, float]]
    source_path: Optional[str] = None
    parent_version: Optional[str] = None
    metadata: Dict[str, Any] = field(default_factory=dict)

    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary."""
        return asdict(self)

    def to_json(self) -> str:
        """Convert to JSON string."""
        return json.dumps(self.to_dict(), indent=2)

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "DataVersion":
        """Create from dictionary."""
        return cls(**data)

    @classmethod
    def from_json(cls, json_str: str) -> "DataVersion":
        """Create from JSON string."""
        return cls.from_dict(json.loads(json_str))


class DataVersioner:
    """
    Manages data versions for tracking changes and maintaining lineage.
    """

    def __init__(
        self,
        storage_path: Union[str, Path],
        algorithm: str = "sha256"
    ):
        """
        Initialize data versioner.

        Args:
            storage_path: Path to store version metadata
            algorithm: Hashing algorithm for version IDs
        """
        self.storage_path = Path(storage_path)
        self.storage_path.mkdir(parents=True, exist_ok=True)
        self.algorithm = algorithm

        self._versions: Dict[str, DataVersion] = {}
        self._load_versions()

    def _load_versions(self) -> None:
        """Load existing versions from storage."""
        versions_file = self.storage_path / "versions.json"

        if versions_file.exists():
            with open(versions_file, "r") as f:
                data = json.load(f)
                for version_data in data.get("versions", []):
                    version = DataVersion.from_dict(version_data)
                    self._versions[version.version_id] = version

    def _save_versions(self) -> None:
        """Save versions to storage."""
        versions_file = self.storage_path / "versions.json"

        data = {
            "versions": [v.to_dict() for v in self._versions.values()],
            "updated_at": datetime.now().isoformat()
        }

        with open(versions_file, "w") as f:
            json.dump(data, f, indent=2)

    def compute_hash(
        self,
        data: pd.DataFrame,
        include_schema: bool = True
    ) -> str:
        """
        Compute hash of dataframe.

        Args:
            data: Input dataframe
            include_schema: Whether to include schema in hash

        Returns:
            Hash string
        """
        hasher = hashlib.new(self.algorithm)

        # Hash data content
        data_bytes = data.to_json(orient="records").encode()
        hasher.update(data_bytes)

        # Optionally hash schema
        if include_schema:
            schema_str = json.dumps(dict(data.dtypes.astype(str)), sort_keys=True)
            hasher.update(schema_str.encode())

        return hasher.hexdigest()[:16]

    def compute_statistics(self, data: pd.DataFrame) -> Dict[str, Dict[str, float]]:
        """
        Compute basic statistics for numerical columns.

        Args:
            data: Input dataframe

        Returns:
            Dictionary of statistics per column
        """
        stats = {}

        for col in data.select_dtypes(include=["number"]).columns:
            stats[col] = {
                "mean": float(data[col].mean()),
                "std": float(data[col].std()),
                "min": float(data[col].min()),
                "max": float(data[col].max()),
                "median": float(data[col].median()),
                "null_count": int(data[col].isnull().sum())
            }

        return stats

    def create_version(
        self,
        data: pd.DataFrame,
        source_path: Optional[str] = None,
        parent_version: Optional[str] = None,
        metadata: Optional[Dict[str, Any]] = None
    ) -> DataVersion:
        """
        Create a new version for a dataset.

        Args:
            data: Input dataframe
            source_path: Path to source data
            parent_version: ID of parent version
            metadata: Additional metadata

        Returns:
            DataVersion object
        """
        data_hash = self.compute_hash(data)

        # Check if version already exists
        for version in self._versions.values():
            if version.data_hash == data_hash:
                logger.info(f"Data matches existing version: {version.version_id}")
                return version

        # Create new version
        version = DataVersion(
            version_id=f"v_{data_hash}_{datetime.now().strftime('%Y%m%d%H%M%S')}",
            data_hash=data_hash,
            created_at=datetime.now().isoformat(),
            row_count=len(data),
            column_count=len(data.columns),
            schema={col: str(dtype) for col, dtype in data.dtypes.items()},
            statistics=self.compute_statistics(data),
            source_path=source_path,
            parent_version=parent_version,
            metadata=metadata or {}
        )

        self._versions[version.version_id] = version
        self._save_versions()

        logger.info(f"Created new data version: {version.version_id}")

        return version

    def get_version(self, version_id: str) -> Optional[DataVersion]:
        """
        Get a specific version.

        Args:
            version_id: Version ID

        Returns:
            DataVersion object or None
        """
        return self._versions.get(version_id)

    def get_latest_version(self) -> Optional[DataVersion]:
        """
        Get the latest version.

        Returns:
            Latest DataVersion or None
        """
        if not self._versions:
            return None

        return max(
            self._versions.values(),
            key=lambda v: v.created_at
        )

    def list_versions(self) -> List[DataVersion]:
        """
        List all versions sorted by creation time.

        Returns:
            List of DataVersion objects
        """
        return sorted(
            self._versions.values(),
            key=lambda v: v.created_at,
            reverse=True
        )

    def compare_versions(
        self,
        version_id_1: str,
        version_id_2: str
    ) -> Dict[str, Any]:
        """
        Compare two data versions.

        Args:
            version_id_1: First version ID
            version_id_2: Second version ID

        Returns:
            Comparison results
        """
        v1 = self.get_version(version_id_1)
        v2 = self.get_version(version_id_2)

        if not v1 or not v2:
            raise ValueError("One or both versions not found")

        comparison = {
            "row_count_diff": v2.row_count - v1.row_count,
            "column_count_diff": v2.column_count - v1.column_count,
            "schema_changes": {},
            "statistics_changes": {}
        }

        # Compare schemas
        all_columns = set(v1.schema.keys()) | set(v2.schema.keys())
        for col in all_columns:
            if col not in v1.schema:
                comparison["schema_changes"][col] = {"change": "added"}
            elif col not in v2.schema:
                comparison["schema_changes"][col] = {"change": "removed"}
            elif v1.schema[col] != v2.schema[col]:
                comparison["schema_changes"][col] = {
                    "change": "type_changed",
                    "from": v1.schema[col],
                    "to": v2.schema[col]
                }

        # Compare statistics
        all_stat_columns = set(v1.statistics.keys()) | set(v2.statistics.keys())
        for col in all_stat_columns:
            if col in v1.statistics and col in v2.statistics:
                col_changes = {}
                for stat in ["mean", "std", "min", "max"]:
                    if stat in v1.statistics[col] and stat in v2.statistics[col]:
                        diff = v2.statistics[col][stat] - v1.statistics[col][stat]
                        if abs(diff) > 0.001:
                            col_changes[stat] = {
                                "from": v1.statistics[col][stat],
                                "to": v2.statistics[col][stat],
                                "diff": diff
                            }
                if col_changes:
                    comparison["statistics_changes"][col] = col_changes

        return comparison

    def delete_version(self, version_id: str) -> bool:
        """
        Delete a version.

        Args:
            version_id: Version ID to delete

        Returns:
            True if deleted, False if not found
        """
        if version_id in self._versions:
            del self._versions[version_id]
            self._save_versions()
            logger.info(f"Deleted version: {version_id}")
            return True

        return False


def create_versioner(config_path: str) -> DataVersioner:
    """
    Factory function to create a data versioner.

    Args:
        config_path: Path to configuration file

    Returns:
        DataVersioner instance
    """
    config = OmegaConf.load(config_path)

    storage_path = config.versioning.storage.get("path", "/data/versions")
    algorithm = config.versioning.hash.get("algorithm", "sha256")

    return DataVersioner(storage_path=storage_path, algorithm=algorithm)
