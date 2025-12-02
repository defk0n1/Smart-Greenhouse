"""
Data Validation Module

This module provides data validation and quality checking capabilities.
Includes schema validation, quality checks, and drift detection.
"""

import logging
from dataclasses import dataclass
from enum import Enum
from typing import Any, Dict, List, Optional, Union

import numpy as np
import pandas as pd
from omegaconf import DictConfig, OmegaConf
from pydantic import BaseModel, Field, validator

from src.utils.logging import get_logger

logger = get_logger(__name__)


class ValidationStatus(Enum):
    """Validation result status."""
    PASSED = "passed"
    WARNING = "warning"
    FAILED = "failed"


@dataclass
class ValidationResult:
    """Result of a validation check."""
    check_name: str
    status: ValidationStatus
    message: str
    details: Optional[Dict[str, Any]] = None

    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary."""
        return {
            "check_name": self.check_name,
            "status": self.status.value,
            "message": self.message,
            "details": self.details
        }


class ColumnSchema(BaseModel):
    """Schema definition for a column."""
    name: str
    type: str = Field(..., description="Expected data type")
    nullable: bool = True
    min_value: Optional[float] = None
    max_value: Optional[float] = None
    values: Optional[List[Any]] = None
    pattern: Optional[str] = None


class DataSchema(BaseModel):
    """Schema definition for a dataset."""
    columns: List[ColumnSchema]

    def get_column(self, name: str) -> Optional[ColumnSchema]:
        """Get column schema by name."""
        for col in self.columns:
            if col.name == name:
                return col
        return None


class BaseCheck:
    """Base class for validation checks."""

    def __init__(self, name: str):
        self.name = name

    def run(self, data: pd.DataFrame) -> ValidationResult:
        """Run the validation check."""
        raise NotImplementedError


class NullCheck(BaseCheck):
    """Check for null values in specified columns."""

    def __init__(
        self,
        columns: List[str],
        threshold: float = 0.0,
        name: str = "null_check"
    ):
        """
        Initialize null check.

        Args:
            columns: Columns to check
            threshold: Maximum allowed null ratio (0.0 = no nulls allowed)
            name: Check name
        """
        super().__init__(name)
        self.columns = columns
        self.threshold = threshold

    def run(self, data: pd.DataFrame) -> ValidationResult:
        """Run null check."""
        results = {}
        failed_columns = []

        for col in self.columns:
            if col not in data.columns:
                continue

            null_ratio = data[col].isnull().sum() / len(data)
            results[col] = null_ratio

            if null_ratio > self.threshold:
                failed_columns.append(col)

        if failed_columns:
            return ValidationResult(
                check_name=self.name,
                status=ValidationStatus.FAILED,
                message=f"Null threshold exceeded in columns: {failed_columns}",
                details={"null_ratios": results}
            )

        return ValidationResult(
            check_name=self.name,
            status=ValidationStatus.PASSED,
            message="All columns within null threshold",
            details={"null_ratios": results}
        )


class DuplicateCheck(BaseCheck):
    """Check for duplicate rows."""

    def __init__(
        self,
        threshold: float = 0.01,
        subset: Optional[List[str]] = None,
        name: str = "duplicate_check"
    ):
        """
        Initialize duplicate check.

        Args:
            threshold: Maximum allowed duplicate ratio
            subset: Columns to consider for duplicates
            name: Check name
        """
        super().__init__(name)
        self.threshold = threshold
        self.subset = subset

    def run(self, data: pd.DataFrame) -> ValidationResult:
        """Run duplicate check."""
        duplicates = data.duplicated(subset=self.subset).sum()
        duplicate_ratio = duplicates / len(data)

        if duplicate_ratio > self.threshold:
            return ValidationResult(
                check_name=self.name,
                status=ValidationStatus.FAILED,
                message=f"Duplicate ratio {duplicate_ratio:.2%} exceeds threshold {self.threshold:.2%}",
                details={"duplicate_count": duplicates, "duplicate_ratio": duplicate_ratio}
            )

        return ValidationResult(
            check_name=self.name,
            status=ValidationStatus.PASSED,
            message=f"Duplicate ratio {duplicate_ratio:.2%} within threshold",
            details={"duplicate_count": duplicates, "duplicate_ratio": duplicate_ratio}
        )


class ValueRangeCheck(BaseCheck):
    """Check if values are within expected range."""

    def __init__(
        self,
        column: str,
        min_value: Optional[float] = None,
        max_value: Optional[float] = None,
        name: str = "value_range_check",
        fail_on_missing: bool = True
    ):
        """
        Initialize value range check.

        Args:
            column: Column to check
            min_value: Minimum allowed value
            max_value: Maximum allowed value
            name: Check name
            fail_on_missing: Whether to fail if column is missing
        """
        super().__init__(name)
        self.column = column
        self.min_value = min_value
        self.max_value = max_value
        self.fail_on_missing = fail_on_missing

    def run(self, data: pd.DataFrame) -> ValidationResult:
        """Run value range check."""
        if self.column not in data.columns:
            status = ValidationStatus.FAILED if self.fail_on_missing else ValidationStatus.WARNING
            return ValidationResult(
                check_name=self.name,
                status=status,
                message=f"Column '{self.column}' not found"
            )

        col_data = data[self.column].dropna()
        actual_min = col_data.min()
        actual_max = col_data.max()

        violations = []

        if self.min_value is not None and actual_min < self.min_value:
            violations.append(f"min={actual_min} < {self.min_value}")

        if self.max_value is not None and actual_max > self.max_value:
            violations.append(f"max={actual_max} > {self.max_value}")

        if violations:
            return ValidationResult(
                check_name=self.name,
                status=ValidationStatus.FAILED,
                message=f"Value range violations in '{self.column}': {', '.join(violations)}",
                details={"actual_min": actual_min, "actual_max": actual_max}
            )

        return ValidationResult(
            check_name=self.name,
            status=ValidationStatus.PASSED,
            message=f"Values in '{self.column}' within expected range",
            details={"actual_min": actual_min, "actual_max": actual_max}
        )


class ClassBalanceCheck(BaseCheck):
    """Check class balance for classification tasks."""

    def __init__(
        self,
        column: str,
        min_ratio: float = 0.05,
        name: str = "class_balance_check",
        fail_on_missing: bool = True
    ):
        """
        Initialize class balance check.

        Args:
            column: Column containing class labels
            min_ratio: Minimum ratio for each class
            name: Check name
            fail_on_missing: Whether to fail if column is missing
        """
        super().__init__(name)
        self.column = column
        self.min_ratio = min_ratio
        self.fail_on_missing = fail_on_missing

    def run(self, data: pd.DataFrame) -> ValidationResult:
        """Run class balance check."""
        if self.column not in data.columns:
            status = ValidationStatus.FAILED if self.fail_on_missing else ValidationStatus.WARNING
            return ValidationResult(
                check_name=self.name,
                status=status,
                message=f"Column '{self.column}' not found"
            )

        class_counts = data[self.column].value_counts()
        class_ratios = class_counts / len(data)

        imbalanced_classes = class_ratios[class_ratios < self.min_ratio].index.tolist()

        if imbalanced_classes:
            return ValidationResult(
                check_name=self.name,
                status=ValidationStatus.WARNING,
                message=f"Imbalanced classes: {imbalanced_classes}",
                details={"class_ratios": class_ratios.to_dict()}
            )

        return ValidationResult(
            check_name=self.name,
            status=ValidationStatus.PASSED,
            message="Class distribution is balanced",
            details={"class_ratios": class_ratios.to_dict()}
        )


class SchemaValidator:
    """Validates data against a defined schema."""

    def __init__(self, schema: DataSchema):
        """
        Initialize schema validator.

        Args:
            schema: Data schema definition
        """
        self.schema = schema

    def validate(self, data: pd.DataFrame) -> List[ValidationResult]:
        """
        Validate data against schema.

        Args:
            data: Input dataframe

        Returns:
            List of validation results
        """
        results = []

        # Check for missing columns
        expected_columns = {col.name for col in self.schema.columns}
        actual_columns = set(data.columns)
        missing_columns = expected_columns - actual_columns

        if missing_columns:
            results.append(ValidationResult(
                check_name="schema_columns",
                status=ValidationStatus.FAILED,
                message=f"Missing columns: {missing_columns}"
            ))

        # Validate each column
        for col_schema in self.schema.columns:
            if col_schema.name not in data.columns:
                continue

            col_data = data[col_schema.name]

            # Check nullable
            if not col_schema.nullable and col_data.isnull().any():
                results.append(ValidationResult(
                    check_name=f"nullable_{col_schema.name}",
                    status=ValidationStatus.FAILED,
                    message=f"Column '{col_schema.name}' contains nulls but is not nullable"
                ))

            # Check allowed values
            if col_schema.values is not None:
                invalid_values = set(col_data.dropna().unique()) - set(col_schema.values)
                if invalid_values:
                    results.append(ValidationResult(
                        check_name=f"allowed_values_{col_schema.name}",
                        status=ValidationStatus.FAILED,
                        message=f"Invalid values in '{col_schema.name}': {invalid_values}"
                    ))

            # Check value range
            if col_schema.min_value is not None or col_schema.max_value is not None:
                check = ValueRangeCheck(
                    column=col_schema.name,
                    min_value=col_schema.min_value,
                    max_value=col_schema.max_value
                )
                results.append(check.run(data))

        if not results:
            results.append(ValidationResult(
                check_name="schema_validation",
                status=ValidationStatus.PASSED,
                message="Schema validation passed"
            ))

        return results


class DataValidator:
    """
    Main data validation class that runs all configured checks.
    """

    def __init__(
        self,
        config: Optional[Union[Dict, DictConfig]] = None,
        config_path: Optional[str] = None
    ):
        """
        Initialize data validator.

        Args:
            config: Configuration dictionary or OmegaConf object
            config_path: Path to configuration YAML file
        """
        if config_path:
            self.config = OmegaConf.load(config_path)
        elif config:
            self.config = OmegaConf.create(config) if isinstance(config, dict) else config
        else:
            self.config = OmegaConf.create({"validation": {"enabled": True}})

        self.checks: List[BaseCheck] = []
        self.schema_validator: Optional[SchemaValidator] = None

        self._init_validators()

    def _init_validators(self) -> None:
        """Initialize validators from configuration."""
        if not self.config.validation.get("enabled", True):
            return

        # Initialize schema validator
        schema_config = self.config.validation.get("schema", {})
        if schema_config.get("enforce", False) and "columns" in schema_config:
            columns = [ColumnSchema(**col) for col in schema_config["columns"]]
            self.schema_validator = SchemaValidator(DataSchema(columns=columns))

        # Initialize quality checks
        quality_checks = self.config.validation.get("quality_checks", [])
        for check_config in quality_checks:
            check = self._create_check(check_config)
            if check:
                self.checks.append(check)

    def _create_check(self, config: Dict) -> Optional[BaseCheck]:
        """Create a check from configuration."""
        check_type = config.get("type")

        if check_type == "null_check":
            return NullCheck(
                columns=config.get("columns", []),
                threshold=config.get("threshold", 0.0)
            )
        elif check_type == "duplicate_check":
            return DuplicateCheck(
                threshold=config.get("threshold", 0.01)
            )
        elif check_type == "value_range":
            return ValueRangeCheck(
                column=config.get("column"),
                min_value=config.get("min_value"),
                max_value=config.get("max_value")
            )
        elif check_type == "class_balance":
            return ClassBalanceCheck(
                column=config.get("column"),
                min_ratio=config.get("min_ratio", 0.05)
            )

        logger.warning(f"Unknown check type: {check_type}")
        return None

    def validate(self, data: pd.DataFrame) -> List[ValidationResult]:
        """
        Run all validation checks.

        Args:
            data: Input dataframe

        Returns:
            List of validation results
        """
        logger.info(f"Validating data with {len(data)} rows")

        results = []

        # Run schema validation
        if self.schema_validator:
            results.extend(self.schema_validator.validate(data))

        # Run quality checks
        for check in self.checks:
            result = check.run(data)
            results.append(result)
            logger.info(f"Check '{check.name}': {result.status.value}")

        return results

    def validate_or_raise(self, data: pd.DataFrame) -> None:
        """
        Validate data and raise exception on failure.

        Args:
            data: Input dataframe

        Raises:
            ValueError: If any check fails
        """
        results = self.validate(data)

        failed = [r for r in results if r.status == ValidationStatus.FAILED]

        if failed:
            messages = [r.message for r in failed]
            raise ValueError(f"Data validation failed: {'; '.join(messages)}")

        logger.info("Data validation passed")
