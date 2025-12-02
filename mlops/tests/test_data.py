"""
Tests for Data Module

This module contains unit tests for data ingestion, preprocessing,
validation, and versioning components.
"""

import os
import tempfile
from pathlib import Path
from unittest.mock import MagicMock, patch

import numpy as np
import pandas as pd
import pytest

# Test fixtures
@pytest.fixture
def sample_dataframe():
    """Create a sample dataframe for testing."""
    np.random.seed(42)
    return pd.DataFrame({
        "feature_1": np.random.randn(100),
        "feature_2": np.random.randn(100),
        "feature_3": np.random.choice(["A", "B", "C"], 100),
        "label": np.random.randint(0, 3, 100),
    })


@pytest.fixture
def temp_data_dir():
    """Create a temporary directory for test data."""
    with tempfile.TemporaryDirectory() as tmpdir:
        yield Path(tmpdir)


@pytest.fixture
def sample_config():
    """Create sample configuration for testing."""
    from omegaconf import OmegaConf
    return OmegaConf.create({
        "ingestion": {
            "source_type": "local",
            "local": {
                "path": "/tmp/test_data",
                "format": "csv"
            }
        },
        "preprocessing": {
            "numerical": {
                "strategy": "standard",
                "handle_outliers": {
                    "enabled": True,
                    "method": "clip",
                    "threshold": 3.0
                }
            },
            "categorical": {
                "strategy": "onehot",
                "handle_unknown": "ignore"
            }
        },
        "validation": {
            "enabled": True,
            "quality_checks": [
                {"type": "null_check", "columns": ["feature_1"], "threshold": 0.0},
                {"type": "duplicate_check", "threshold": 0.01}
            ]
        },
        "splitting": {
            "train_ratio": 0.7,
            "val_ratio": 0.15,
            "test_ratio": 0.15
        }
    })


class TestDataPreprocessing:
    """Tests for data preprocessing."""

    def test_numerical_preprocessor_fit_transform(self, sample_dataframe):
        """Test numerical preprocessing fit and transform."""
        from src.data.preprocessing import NumericalPreprocessor

        preprocessor = NumericalPreprocessor(strategy="standard")
        result = preprocessor.fit_transform(sample_dataframe)

        # Check that numerical columns are standardized
        assert abs(result["feature_1"].mean()) < 0.1
        assert abs(result["feature_1"].std() - 1.0) < 0.1

    def test_numerical_preprocessor_outlier_handling(self, sample_dataframe):
        """Test outlier handling in numerical preprocessing."""
        from src.data.preprocessing import NumericalPreprocessor

        # Add outliers
        df = sample_dataframe.copy()
        df.loc[0, "feature_1"] = 100  # Extreme outlier

        preprocessor = NumericalPreprocessor(
            strategy="standard",
            handle_outliers=True,
            outlier_method="clip",
            outlier_threshold=3.0
        )
        result = preprocessor.fit_transform(df)

        # Outlier should be clipped
        assert result["feature_1"].max() < 100

    def test_categorical_preprocessor_onehot(self, sample_dataframe):
        """Test categorical preprocessing with one-hot encoding."""
        from src.data.preprocessing import CategoricalPreprocessor

        preprocessor = CategoricalPreprocessor(strategy="onehot")
        result = preprocessor.fit_transform(sample_dataframe)

        # Check one-hot encoded columns exist
        assert "feature_3_A" in result.columns
        assert "feature_3_B" in result.columns
        assert "feature_3_C" in result.columns

    def test_preprocessor_combined(self, sample_dataframe, sample_config):
        """Test combined preprocessor."""
        from src.data.preprocessing import Preprocessor

        preprocessor = Preprocessor(config=sample_config)
        result = preprocessor.fit_transform(sample_dataframe)

        # Check output is valid
        assert len(result) == len(sample_dataframe)
        assert not result.isnull().any().any()

    def test_data_splitter(self, sample_dataframe):
        """Test data splitting."""
        from src.data.preprocessing import DataSplitter

        splitter = DataSplitter(
            train_ratio=0.7,
            val_ratio=0.15,
            test_ratio=0.15,
            random_state=42
        )

        train, val, test = splitter.split(sample_dataframe)

        # Check split sizes
        assert len(train) == 70
        assert len(val) == 15
        assert len(test) == 15


class TestDataValidation:
    """Tests for data validation."""

    def test_null_check_pass(self, sample_dataframe):
        """Test null check passes with no nulls."""
        from src.data.validation import NullCheck

        check = NullCheck(columns=["feature_1", "feature_2"], threshold=0.0)
        result = check.run(sample_dataframe)

        assert result.status.value == "passed"

    def test_null_check_fail(self, sample_dataframe):
        """Test null check fails with nulls."""
        from src.data.validation import NullCheck

        df = sample_dataframe.copy()
        df.loc[0:10, "feature_1"] = None

        check = NullCheck(columns=["feature_1"], threshold=0.0)
        result = check.run(df)

        assert result.status.value == "failed"

    def test_duplicate_check_pass(self, sample_dataframe):
        """Test duplicate check passes."""
        from src.data.validation import DuplicateCheck

        check = DuplicateCheck(threshold=0.01)
        result = check.run(sample_dataframe)

        assert result.status.value == "passed"

    def test_value_range_check(self, sample_dataframe):
        """Test value range check."""
        from src.data.validation import ValueRangeCheck

        check = ValueRangeCheck(
            column="feature_1",
            min_value=-10.0,
            max_value=10.0
        )
        result = check.run(sample_dataframe)

        assert result.status.value == "passed"

    def test_data_validator_combined(self, sample_dataframe, sample_config):
        """Test combined data validator."""
        from src.data.validation import DataValidator

        validator = DataValidator(config=sample_config)
        results = validator.validate(sample_dataframe)

        # Should have multiple check results
        assert len(results) >= 2


class TestDataVersioning:
    """Tests for data versioning."""

    def test_create_version(self, sample_dataframe, temp_data_dir):
        """Test creating a data version."""
        from src.data.versioning import DataVersioner

        versioner = DataVersioner(storage_path=temp_data_dir)
        version = versioner.create_version(sample_dataframe)

        assert version.version_id is not None
        assert version.row_count == len(sample_dataframe)
        assert version.column_count == len(sample_dataframe.columns)

    def test_get_version(self, sample_dataframe, temp_data_dir):
        """Test retrieving a version."""
        from src.data.versioning import DataVersioner

        versioner = DataVersioner(storage_path=temp_data_dir)
        created = versioner.create_version(sample_dataframe)

        retrieved = versioner.get_version(created.version_id)

        assert retrieved is not None
        assert retrieved.version_id == created.version_id

    def test_list_versions(self, sample_dataframe, temp_data_dir):
        """Test listing versions."""
        from src.data.versioning import DataVersioner

        versioner = DataVersioner(storage_path=temp_data_dir)

        # Create multiple versions
        v1 = versioner.create_version(sample_dataframe)
        sample_dataframe.loc[0, "feature_1"] = 999  # Modify data
        v2 = versioner.create_version(sample_dataframe)

        versions = versioner.list_versions()

        assert len(versions) == 2

    def test_compare_versions(self, sample_dataframe, temp_data_dir):
        """Test comparing versions."""
        from src.data.versioning import DataVersioner

        versioner = DataVersioner(storage_path=temp_data_dir)

        # Create first version
        v1 = versioner.create_version(sample_dataframe)

        # Modify and create second version
        df2 = sample_dataframe.copy()
        df2["new_column"] = 1
        v2 = versioner.create_version(df2)

        comparison = versioner.compare_versions(v1.version_id, v2.version_id)

        assert comparison["column_count_diff"] == 1


class TestDataIngestion:
    """Tests for data ingestion."""

    def test_local_data_source(self, sample_dataframe, temp_data_dir):
        """Test local data source ingestion."""
        from src.data.ingestion import LocalDataSource

        # Save test data
        data_path = temp_data_dir / "test.csv"
        sample_dataframe.to_csv(data_path, index=False)

        source = LocalDataSource(path=data_path, format="csv")
        assert source.validate_connection()

        data = source.ingest()
        assert len(data) == len(sample_dataframe)

    def test_local_data_source_directory(self, sample_dataframe, temp_data_dir):
        """Test local data source with directory."""
        from src.data.ingestion import LocalDataSource

        # Save multiple test files
        for i in range(3):
            path = temp_data_dir / f"test_{i}.csv"
            sample_dataframe.to_csv(path, index=False)

        source = LocalDataSource(path=temp_data_dir, format="csv")
        data = source.ingest()

        assert len(data) == len(sample_dataframe) * 3

    def test_data_ingester_compute_hash(self, sample_dataframe, sample_config, temp_data_dir):
        """Test data hash computation."""
        from src.data.ingestion import DataIngester

        # Save test data
        data_path = temp_data_dir / "test.csv"
        sample_dataframe.to_csv(data_path, index=False)

        config = sample_config.copy()
        config.ingestion.local.path = str(data_path)

        ingester = DataIngester(config=config)
        hash1 = ingester.compute_data_hash(sample_dataframe)
        hash2 = ingester.compute_data_hash(sample_dataframe)

        # Same data should have same hash
        assert hash1 == hash2

        # Modified data should have different hash
        df_modified = sample_dataframe.copy()
        df_modified.loc[0, "feature_1"] = 999
        hash3 = ingester.compute_data_hash(df_modified)

        assert hash1 != hash3


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
