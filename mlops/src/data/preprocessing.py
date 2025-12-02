"""
Data Preprocessing Module

This module provides preprocessing and feature engineering capabilities
for tabular and image data.
"""

import logging
from abc import ABC, abstractmethod
from typing import Any, Dict, List, Optional, Tuple, Union

import numpy as np
import pandas as pd
from omegaconf import DictConfig, OmegaConf
from sklearn.compose import ColumnTransformer
from sklearn.impute import SimpleImputer
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import (
    LabelEncoder,
    MinMaxScaler,
    OneHotEncoder,
    OrdinalEncoder,
    RobustScaler,
    StandardScaler,
)

from src.utils.logging import get_logger

logger = get_logger(__name__)


class BasePreprocessor(ABC):
    """Abstract base class for preprocessors."""

    @abstractmethod
    def fit(self, data: pd.DataFrame) -> "BasePreprocessor":
        """Fit the preprocessor to the data."""
        pass

    @abstractmethod
    def transform(self, data: pd.DataFrame) -> pd.DataFrame:
        """Transform the data."""
        pass

    def fit_transform(self, data: pd.DataFrame) -> pd.DataFrame:
        """Fit and transform the data."""
        return self.fit(data).transform(data)


class NumericalPreprocessor(BasePreprocessor):
    """Preprocessor for numerical features."""

    SCALERS = {
        "standard": StandardScaler,
        "minmax": MinMaxScaler,
        "robust": RobustScaler,
    }

    def __init__(
        self,
        strategy: str = "standard",
        handle_outliers: bool = True,
        outlier_method: str = "clip",
        outlier_threshold: float = 3.0,
        missing_strategy: str = "mean",
        columns: Optional[List[str]] = None
    ):
        """
        Initialize numerical preprocessor.

        Args:
            strategy: Scaling strategy (standard, minmax, robust, none)
            handle_outliers: Whether to handle outliers
            outlier_method: Method to handle outliers (clip, remove, winsorize)
            outlier_threshold: Z-score threshold for outliers
            missing_strategy: Strategy for missing values (mean, median, most_frequent)
            columns: Specific columns to process
        """
        self.strategy = strategy
        self.handle_outliers = handle_outliers
        self.outlier_method = outlier_method
        self.outlier_threshold = outlier_threshold
        self.missing_strategy = missing_strategy
        self.columns = columns

        self.scaler = None
        self.imputer = None
        self._fitted = False
        self._stats: Dict[str, Dict[str, float]] = {}

    def fit(self, data: pd.DataFrame) -> "NumericalPreprocessor":
        """Fit the preprocessor."""
        cols = self.columns or data.select_dtypes(include=[np.number]).columns.tolist()

        if not cols:
            logger.warning("No numerical columns found")
            return self

        # Compute statistics for outlier handling
        for col in cols:
            self._stats[col] = {
                "mean": data[col].mean(),
                "std": data[col].std(),
                "min": data[col].min(),
                "max": data[col].max(),
            }

        # Fit imputer
        self.imputer = SimpleImputer(strategy=self.missing_strategy)
        self.imputer.fit(data[cols])

        # Fit scaler
        if self.strategy != "none" and self.strategy in self.SCALERS:
            self.scaler = self.SCALERS[self.strategy]()
            # Handle outliers before fitting scaler
            temp_data = self._handle_outliers(data[cols].copy())
            temp_data = pd.DataFrame(
                self.imputer.transform(temp_data),
                columns=cols
            )
            self.scaler.fit(temp_data)

        self._fitted = True
        return self

    def transform(self, data: pd.DataFrame) -> pd.DataFrame:
        """Transform the data."""
        if not self._fitted:
            raise RuntimeError("Preprocessor must be fitted before transform")

        cols = self.columns or data.select_dtypes(include=[np.number]).columns.tolist()

        if not cols:
            return data

        result = data.copy()

        # Handle outliers
        if self.handle_outliers:
            result[cols] = self._handle_outliers(result[cols])

        # Impute missing values
        result[cols] = self.imputer.transform(result[cols])

        # Scale features
        if self.scaler is not None:
            result[cols] = self.scaler.transform(result[cols])

        return result

    def _handle_outliers(self, data: pd.DataFrame) -> pd.DataFrame:
        """Handle outliers in the data."""
        result = data.copy()

        for col in result.columns:
            if col not in self._stats:
                continue

            mean = self._stats[col]["mean"]
            std = self._stats[col]["std"]

            if std == 0:
                continue

            z_scores = np.abs((result[col] - mean) / std)

            if self.outlier_method == "clip":
                lower = mean - self.outlier_threshold * std
                upper = mean + self.outlier_threshold * std
                result[col] = result[col].clip(lower=lower, upper=upper)
            elif self.outlier_method == "winsorize":
                lower_percentile = (1 - 0.99) / 2
                upper_percentile = 1 - lower_percentile
                lower = result[col].quantile(lower_percentile)
                upper = result[col].quantile(upper_percentile)
                result[col] = result[col].clip(lower=lower, upper=upper)

        return result


class CategoricalPreprocessor(BasePreprocessor):
    """Preprocessor for categorical features."""

    def __init__(
        self,
        strategy: str = "onehot",
        handle_unknown: str = "ignore",
        missing_strategy: str = "most_frequent",
        columns: Optional[List[str]] = None
    ):
        """
        Initialize categorical preprocessor.

        Args:
            strategy: Encoding strategy (onehot, label, ordinal)
            handle_unknown: How to handle unknown categories
            missing_strategy: Strategy for missing values
            columns: Specific columns to process
        """
        self.strategy = strategy
        self.handle_unknown = handle_unknown
        self.missing_strategy = missing_strategy
        self.columns = columns

        self.encoders: Dict[str, Any] = {}
        self.imputer = None
        self._fitted = False

    def fit(self, data: pd.DataFrame) -> "CategoricalPreprocessor":
        """Fit the preprocessor."""
        cols = self.columns or data.select_dtypes(
            include=["object", "category"]
        ).columns.tolist()

        if not cols:
            logger.warning("No categorical columns found")
            return self

        # Fit imputer
        self.imputer = SimpleImputer(strategy=self.missing_strategy)
        self.imputer.fit(data[cols].astype(str))

        # Fit encoders
        for col in cols:
            if self.strategy == "onehot":
                encoder = OneHotEncoder(
                    handle_unknown=self.handle_unknown,
                    sparse_output=False
                )
            elif self.strategy == "label":
                encoder = LabelEncoder()
            elif self.strategy == "ordinal":
                encoder = OrdinalEncoder(
                    handle_unknown="use_encoded_value",
                    unknown_value=-1
                )
            else:
                raise ValueError(f"Unknown strategy: {self.strategy}")

            # Handle missing values before fitting
            col_data = data[col].fillna("__missing__")

            if self.strategy == "label":
                encoder.fit(col_data)
            else:
                encoder.fit(col_data.values.reshape(-1, 1))

            self.encoders[col] = encoder

        self._fitted = True
        return self

    def transform(self, data: pd.DataFrame) -> pd.DataFrame:
        """Transform the data."""
        if not self._fitted:
            raise RuntimeError("Preprocessor must be fitted before transform")

        cols = list(self.encoders.keys())

        if not cols:
            return data

        result = data.copy()

        for col in cols:
            encoder = self.encoders[col]
            col_data = result[col].fillna("__missing__")

            if self.strategy == "onehot":
                encoded = encoder.transform(col_data.values.reshape(-1, 1))
                feature_names = [f"{col}_{cat}" for cat in encoder.categories_[0]]
                encoded_df = pd.DataFrame(encoded, columns=feature_names, index=result.index)
                result = result.drop(columns=[col])
                result = pd.concat([result, encoded_df], axis=1)
            elif self.strategy == "label":
                # Handle unknown categories
                known_classes = set(encoder.classes_)
                col_data = col_data.apply(
                    lambda x: x if x in known_classes else encoder.classes_[0]
                )
                result[col] = encoder.transform(col_data)
            elif self.strategy == "ordinal":
                result[col] = encoder.transform(
                    col_data.values.reshape(-1, 1)
                ).flatten()

        return result


class Preprocessor:
    """
    Main preprocessing class that combines numerical and categorical preprocessing.
    """

    def __init__(
        self,
        config: Optional[Union[Dict, DictConfig]] = None,
        config_path: Optional[str] = None
    ):
        """
        Initialize preprocessor.

        Args:
            config: Configuration dictionary or OmegaConf object
            config_path: Path to configuration YAML file
        """
        if config_path:
            self.config = OmegaConf.load(config_path)
        elif config:
            self.config = OmegaConf.create(config) if isinstance(config, dict) else config
        else:
            # Default configuration
            self.config = OmegaConf.create({
                "preprocessing": {
                    "numerical": {"strategy": "standard"},
                    "categorical": {"strategy": "onehot"},
                }
            })

        self._init_preprocessors()
        self._fitted = False

    def _init_preprocessors(self) -> None:
        """Initialize preprocessor components."""
        num_config = self.config.preprocessing.get("numerical", {})
        cat_config = self.config.preprocessing.get("categorical", {})

        self.numerical_preprocessor = NumericalPreprocessor(
            strategy=num_config.get("strategy", "standard"),
            handle_outliers=num_config.get("handle_outliers", {}).get("enabled", True),
            outlier_method=num_config.get("handle_outliers", {}).get("method", "clip"),
            outlier_threshold=num_config.get("handle_outliers", {}).get("threshold", 3.0),
        )

        self.categorical_preprocessor = CategoricalPreprocessor(
            strategy=cat_config.get("strategy", "onehot"),
            handle_unknown=cat_config.get("handle_unknown", "ignore"),
        )

    def fit(self, data: pd.DataFrame) -> "Preprocessor":
        """Fit the preprocessor."""
        logger.info(f"Fitting preprocessor on {len(data)} samples")

        self.numerical_preprocessor.fit(data)
        self.categorical_preprocessor.fit(data)

        self._fitted = True
        return self

    def transform(self, data: pd.DataFrame) -> pd.DataFrame:
        """Transform the data."""
        if not self._fitted:
            raise RuntimeError("Preprocessor must be fitted before transform")

        logger.info(f"Transforming {len(data)} samples")

        result = self.numerical_preprocessor.transform(data)
        result = self.categorical_preprocessor.transform(result)

        return result

    def fit_transform(self, data: pd.DataFrame) -> pd.DataFrame:
        """Fit and transform the data."""
        return self.fit(data).transform(data)


class DataSplitter:
    """Utility class for splitting data."""

    def __init__(
        self,
        train_ratio: float = 0.7,
        val_ratio: float = 0.15,
        test_ratio: float = 0.15,
        shuffle: bool = True,
        stratify_column: Optional[str] = None,
        random_state: int = 42
    ):
        """
        Initialize data splitter.

        Args:
            train_ratio: Ratio of training data
            val_ratio: Ratio of validation data
            test_ratio: Ratio of test data
            shuffle: Whether to shuffle before splitting
            stratify_column: Column to stratify by
            random_state: Random seed
        """
        if abs(train_ratio + val_ratio + test_ratio - 1.0) > 0.01:
            raise ValueError("Ratios must sum to 1.0")

        self.train_ratio = train_ratio
        self.val_ratio = val_ratio
        self.test_ratio = test_ratio
        self.shuffle = shuffle
        self.stratify_column = stratify_column
        self.random_state = random_state

    def split(
        self,
        data: pd.DataFrame
    ) -> Tuple[pd.DataFrame, pd.DataFrame, pd.DataFrame]:
        """
        Split data into train, validation, and test sets.

        Args:
            data: Input dataframe

        Returns:
            Tuple of (train, val, test) dataframes
        """
        from sklearn.model_selection import train_test_split

        stratify = data[self.stratify_column] if self.stratify_column else None

        # First split: train + val vs test
        train_val, test = train_test_split(
            data,
            test_size=self.test_ratio,
            shuffle=self.shuffle,
            stratify=stratify,
            random_state=self.random_state
        )

        # Second split: train vs val
        val_ratio_adjusted = self.val_ratio / (1 - self.test_ratio)
        stratify_train = (
            train_val[self.stratify_column] if self.stratify_column else None
        )

        train, val = train_test_split(
            train_val,
            test_size=val_ratio_adjusted,
            shuffle=self.shuffle,
            stratify=stratify_train,
            random_state=self.random_state
        )

        logger.info(
            f"Split data: train={len(train)}, val={len(val)}, test={len(test)}"
        )

        return train, val, test
