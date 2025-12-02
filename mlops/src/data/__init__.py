"""
Data Module

This module provides data ingestion, preprocessing, validation, and versioning capabilities.
"""

from src.data.ingestion import DataIngester, create_ingester
from src.data.preprocessing import (
    Preprocessor,
    DataSplitter,
    NumericalPreprocessor,
    CategoricalPreprocessor,
)
from src.data.validation import DataValidator, ValidationResult, ValidationStatus
from src.data.versioning import DataVersioner, DataVersion, create_versioner

__all__ = [
    # Ingestion
    "DataIngester",
    "create_ingester",
    # Preprocessing
    "Preprocessor",
    "DataSplitter",
    "NumericalPreprocessor",
    "CategoricalPreprocessor",
    # Validation
    "DataValidator",
    "ValidationResult",
    "ValidationStatus",
    # Versioning
    "DataVersioner",
    "DataVersion",
    "create_versioner",
]
