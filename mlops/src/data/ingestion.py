"""
Data Ingestion Module

This module provides pluggable data ingestion components for various data sources.
Supports local files, cloud storage (S3, GCS), HTTP APIs, and databases.
"""

import hashlib
import logging
from abc import ABC, abstractmethod
from pathlib import Path
from typing import Any, Dict, List, Optional, Union

import pandas as pd
from omegaconf import DictConfig, OmegaConf

from src.utils.logging import get_logger

logger = get_logger(__name__)


class DataSource(ABC):
    """Abstract base class for data sources."""

    @abstractmethod
    def ingest(self) -> pd.DataFrame:
        """Ingest data from the source."""
        pass

    @abstractmethod
    def validate_connection(self) -> bool:
        """Validate connection to the data source."""
        pass


class LocalDataSource(DataSource):
    """Data source for local file system."""

    def __init__(
        self,
        path: Union[str, Path],
        format: str = "csv",
        **kwargs: Any
    ):
        """
        Initialize local data source.

        Args:
            path: Path to the data file or directory
            format: File format (csv, parquet, json, image_folder)
            **kwargs: Additional arguments for the reader
        """
        self.path = Path(path)
        self.format = format.lower()
        self.kwargs = kwargs

    def validate_connection(self) -> bool:
        """Check if the path exists."""
        return self.path.exists()

    def ingest(self) -> pd.DataFrame:
        """Load data from local file system."""
        if not self.validate_connection():
            raise FileNotFoundError(f"Path not found: {self.path}")

        logger.info(f"Ingesting data from {self.path}")

        if self.path.is_file():
            return self._read_file(self.path)
        elif self.path.is_dir():
            return self._read_directory()
        else:
            raise ValueError(f"Invalid path: {self.path}")

    def _read_file(self, file_path: Path) -> pd.DataFrame:
        """Read a single file."""
        readers = {
            "csv": pd.read_csv,
            "parquet": pd.read_parquet,
            "json": pd.read_json,
            "excel": pd.read_excel,
        }

        if self.format not in readers:
            raise ValueError(f"Unsupported format: {self.format}")

        return readers[self.format](file_path, **self.kwargs)

    def _read_directory(self) -> pd.DataFrame:
        """Read all files from a directory."""
        pattern = f"*.{self.format}"
        files = list(self.path.glob(pattern))

        if not files:
            raise ValueError(f"No {self.format} files found in {self.path}")

        dfs = [self._read_file(f) for f in files]
        return pd.concat(dfs, ignore_index=True)


class S3DataSource(DataSource):
    """Data source for AWS S3."""

    def __init__(
        self,
        bucket: str,
        prefix: str = "",
        region: str = "us-east-1",
        format: str = "csv",
        **kwargs: Any
    ):
        """
        Initialize S3 data source.

        Args:
            bucket: S3 bucket name
            prefix: S3 key prefix
            region: AWS region
            format: File format
            **kwargs: Additional arguments
        """
        self.bucket = bucket
        self.prefix = prefix
        self.region = region
        self.format = format
        self.kwargs = kwargs

    def validate_connection(self) -> bool:
        """Validate S3 connection."""
        try:
            import boto3
            s3 = boto3.client("s3", region_name=self.region)
            s3.head_bucket(Bucket=self.bucket)
            return True
        except Exception as e:
            logger.error(f"S3 connection failed: {e}")
            return False

    def ingest(self) -> pd.DataFrame:
        """Load data from S3."""
        import boto3
        import io

        s3 = boto3.client("s3", region_name=self.region)

        # List objects
        response = s3.list_objects_v2(Bucket=self.bucket, Prefix=self.prefix)
        objects = response.get("Contents", [])

        if not objects:
            raise ValueError(f"No objects found in s3://{self.bucket}/{self.prefix}")

        # Filter by format
        objects = [obj for obj in objects if obj["Key"].endswith(f".{self.format}")]

        dfs = []
        for obj in objects:
            response = s3.get_object(Bucket=self.bucket, Key=obj["Key"])
            content = response["Body"].read()
            buffer = io.BytesIO(content)

            if self.format == "csv":
                df = pd.read_csv(buffer, **self.kwargs)
            elif self.format == "parquet":
                df = pd.read_parquet(buffer, **self.kwargs)
            elif self.format == "json":
                df = pd.read_json(buffer, **self.kwargs)
            else:
                raise ValueError(f"Unsupported format: {self.format}")

            dfs.append(df)

        return pd.concat(dfs, ignore_index=True)


class HTTPDataSource(DataSource):
    """Data source for HTTP/REST APIs."""

    def __init__(
        self,
        url: str,
        auth_type: str = "none",
        headers: Optional[Dict[str, str]] = None,
        params: Optional[Dict[str, Any]] = None,
        **kwargs: Any
    ):
        """
        Initialize HTTP data source.

        Args:
            url: API endpoint URL
            auth_type: Authentication type (none, basic, bearer, api_key)
            headers: HTTP headers
            params: Query parameters
            **kwargs: Additional arguments
        """
        self.url = url
        self.auth_type = auth_type
        self.headers = headers or {}
        self.params = params or {}
        self.kwargs = kwargs

    def validate_connection(self) -> bool:
        """Validate HTTP connection."""
        try:
            import requests
            response = requests.head(self.url, headers=self.headers, timeout=10)
            return response.status_code < 400
        except Exception as e:
            logger.error(f"HTTP connection failed: {e}")
            return False

    def ingest(self) -> pd.DataFrame:
        """Fetch data from HTTP API."""
        import requests

        response = requests.get(
            self.url,
            headers=self.headers,
            params=self.params,
            timeout=60
        )
        response.raise_for_status()

        data = response.json()

        # Handle nested data
        if isinstance(data, dict) and "data" in data:
            data = data["data"]

        return pd.DataFrame(data)


class DatabaseDataSource(DataSource):
    """Data source for SQL databases."""

    def __init__(
        self,
        connection_string: str,
        query: str,
        **kwargs: Any
    ):
        """
        Initialize database data source.

        Args:
            connection_string: SQLAlchemy connection string
            query: SQL query to execute
            **kwargs: Additional arguments
        """
        self.connection_string = connection_string
        self.query = query
        self.kwargs = kwargs

    def validate_connection(self) -> bool:
        """Validate database connection."""
        try:
            from sqlalchemy import create_engine
            engine = create_engine(self.connection_string)
            with engine.connect() as conn:
                conn.execute("SELECT 1")
            return True
        except Exception as e:
            logger.error(f"Database connection failed: {e}")
            return False

    def ingest(self) -> pd.DataFrame:
        """Execute query and return results."""
        from sqlalchemy import create_engine

        engine = create_engine(self.connection_string)
        return pd.read_sql(self.query, engine, **self.kwargs)


class DataIngester:
    """
    Main data ingestion class that orchestrates data loading from various sources.
    """

    SOURCE_TYPES = {
        "local": LocalDataSource,
        "s3": S3DataSource,
        "http": HTTPDataSource,
        "database": DatabaseDataSource,
    }

    def __init__(
        self,
        config: Optional[Union[Dict, DictConfig]] = None,
        config_path: Optional[str] = None
    ):
        """
        Initialize data ingester.

        Args:
            config: Configuration dictionary or OmegaConf object
            config_path: Path to configuration YAML file
        """
        if config_path:
            self.config = OmegaConf.load(config_path)
        elif config:
            self.config = OmegaConf.create(config) if isinstance(config, dict) else config
        else:
            raise ValueError("Either config or config_path must be provided")

        self.source = self._create_source()

    def _create_source(self) -> DataSource:
        """Create data source based on configuration."""
        source_type = self.config.ingestion.source_type

        if source_type not in self.SOURCE_TYPES:
            raise ValueError(f"Unsupported source type: {source_type}")

        source_config = self.config.ingestion.get(source_type, {})
        source_class = self.SOURCE_TYPES[source_type]

        return source_class(**source_config)

    def ingest(self) -> pd.DataFrame:
        """
        Ingest data from configured source.

        Returns:
            pd.DataFrame: Ingested data
        """
        logger.info(f"Starting data ingestion from {self.config.ingestion.source_type}")

        if not self.source.validate_connection():
            raise ConnectionError("Failed to validate data source connection")

        data = self.source.ingest()

        logger.info(f"Ingested {len(data)} rows, {len(data.columns)} columns")

        return data

    def compute_data_hash(self, data: pd.DataFrame) -> str:
        """
        Compute hash of dataframe for versioning.

        Args:
            data: Input dataframe

        Returns:
            str: SHA256 hash of the data
        """
        # Convert to bytes and hash
        data_bytes = data.to_json().encode()
        return hashlib.sha256(data_bytes).hexdigest()[:16]


def create_ingester(config_path: str) -> DataIngester:
    """
    Factory function to create a data ingester.

    Args:
        config_path: Path to configuration file

    Returns:
        DataIngester: Configured ingester instance
    """
    return DataIngester(config_path=config_path)
