"""
Metrics Module

This module provides evaluation metrics for classification and regression tasks.
"""

from typing import Any, Dict, List, Optional, Union

import numpy as np
import torch
from sklearn.metrics import (
    accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    confusion_matrix,
    classification_report,
    roc_auc_score,
    mean_squared_error,
    mean_absolute_error,
    r2_score,
)

from src.utils.logging import get_logger

logger = get_logger(__name__)


class Metric:
    """Base class for metrics."""

    def __init__(self, name: str):
        """
        Initialize metric.

        Args:
            name: Metric name
        """
        self.name = name
        self.reset()

    def reset(self) -> None:
        """Reset metric state."""
        self._predictions: List[np.ndarray] = []
        self._targets: List[np.ndarray] = []

    def update(
        self,
        predictions: Union[np.ndarray, torch.Tensor],
        targets: Union[np.ndarray, torch.Tensor]
    ) -> None:
        """
        Update metric with new predictions and targets.

        Args:
            predictions: Model predictions
            targets: Ground truth targets
        """
        if isinstance(predictions, torch.Tensor):
            predictions = predictions.detach().cpu().numpy()
        if isinstance(targets, torch.Tensor):
            targets = targets.detach().cpu().numpy()

        self._predictions.append(predictions)
        self._targets.append(targets)

    def compute(self) -> float:
        """Compute metric value."""
        raise NotImplementedError

    def get_predictions_and_targets(self) -> tuple:
        """Get concatenated predictions and targets."""
        predictions = np.concatenate(self._predictions)
        targets = np.concatenate(self._targets)
        return predictions, targets


class Accuracy(Metric):
    """Classification accuracy metric."""

    def __init__(self):
        super().__init__("accuracy")

    def compute(self) -> float:
        """Compute accuracy."""
        predictions, targets = self.get_predictions_and_targets()

        if predictions.ndim > 1:
            predictions = predictions.argmax(axis=1)

        return accuracy_score(targets, predictions)


class Precision(Metric):
    """Precision metric for classification."""

    def __init__(self, average: str = "weighted"):
        """
        Initialize precision metric.

        Args:
            average: Averaging strategy ('micro', 'macro', 'weighted')
        """
        super().__init__("precision")
        self.average = average

    def compute(self) -> float:
        """Compute precision."""
        predictions, targets = self.get_predictions_and_targets()

        if predictions.ndim > 1:
            predictions = predictions.argmax(axis=1)

        return precision_score(targets, predictions, average=self.average, zero_division=0)


class Recall(Metric):
    """Recall metric for classification."""

    def __init__(self, average: str = "weighted"):
        """
        Initialize recall metric.

        Args:
            average: Averaging strategy
        """
        super().__init__("recall")
        self.average = average

    def compute(self) -> float:
        """Compute recall."""
        predictions, targets = self.get_predictions_and_targets()

        if predictions.ndim > 1:
            predictions = predictions.argmax(axis=1)

        return recall_score(targets, predictions, average=self.average, zero_division=0)


class F1Score(Metric):
    """F1 score metric for classification."""

    def __init__(self, average: str = "weighted"):
        """
        Initialize F1 score metric.

        Args:
            average: Averaging strategy
        """
        super().__init__("f1")
        self.average = average

    def compute(self) -> float:
        """Compute F1 score."""
        predictions, targets = self.get_predictions_and_targets()

        if predictions.ndim > 1:
            predictions = predictions.argmax(axis=1)

        return f1_score(targets, predictions, average=self.average, zero_division=0)


class AUROC(Metric):
    """Area Under ROC Curve metric."""

    def __init__(self, average: str = "weighted", multi_class: str = "ovr"):
        """
        Initialize AUROC metric.

        Args:
            average: Averaging strategy
            multi_class: Multi-class handling strategy
        """
        super().__init__("auroc")
        self.average = average
        self.multi_class = multi_class

    def compute(self) -> float:
        """Compute AUROC."""
        predictions, targets = self.get_predictions_and_targets()

        try:
            if predictions.ndim == 1 or predictions.shape[1] == 2:
                # Binary classification
                if predictions.ndim > 1:
                    predictions = predictions[:, 1]
                return roc_auc_score(targets, predictions)
            else:
                # Multi-class
                return roc_auc_score(
                    targets,
                    predictions,
                    average=self.average,
                    multi_class=self.multi_class
                )
        except ValueError as e:
            logger.warning(f"AUROC computation failed: {e}")
            return 0.0


class MSE(Metric):
    """Mean Squared Error metric for regression."""

    def __init__(self):
        super().__init__("mse")

    def compute(self) -> float:
        """Compute MSE."""
        predictions, targets = self.get_predictions_and_targets()
        return mean_squared_error(targets, predictions)


class MAE(Metric):
    """Mean Absolute Error metric for regression."""

    def __init__(self):
        super().__init__("mae")

    def compute(self) -> float:
        """Compute MAE."""
        predictions, targets = self.get_predictions_and_targets()
        return mean_absolute_error(targets, predictions)


class R2Score(Metric):
    """R-squared score for regression."""

    def __init__(self):
        super().__init__("r2")

    def compute(self) -> float:
        """Compute R2 score."""
        predictions, targets = self.get_predictions_and_targets()
        return r2_score(targets, predictions)


class MetricCollection:
    """Collection of metrics for easy management."""

    def __init__(self, metrics: Optional[List[Metric]] = None):
        """
        Initialize metric collection.

        Args:
            metrics: List of metrics to track
        """
        self.metrics = {m.name: m for m in (metrics or [])}

    def add(self, metric: Metric) -> None:
        """Add a metric to the collection."""
        self.metrics[metric.name] = metric

    def reset(self) -> None:
        """Reset all metrics."""
        for metric in self.metrics.values():
            metric.reset()

    def update(
        self,
        predictions: Union[np.ndarray, torch.Tensor],
        targets: Union[np.ndarray, torch.Tensor]
    ) -> None:
        """Update all metrics."""
        for metric in self.metrics.values():
            metric.update(predictions, targets)

    def compute(self) -> Dict[str, float]:
        """Compute all metrics."""
        return {name: metric.compute() for name, metric in self.metrics.items()}


def create_classification_metrics(average: str = "weighted") -> MetricCollection:
    """
    Create a collection of classification metrics.

    Args:
        average: Averaging strategy for multi-class metrics

    Returns:
        MetricCollection with standard classification metrics
    """
    return MetricCollection([
        Accuracy(),
        Precision(average=average),
        Recall(average=average),
        F1Score(average=average),
        AUROC(average=average),
    ])


def create_regression_metrics() -> MetricCollection:
    """
    Create a collection of regression metrics.

    Returns:
        MetricCollection with standard regression metrics
    """
    return MetricCollection([
        MSE(),
        MAE(),
        R2Score(),
    ])


def compute_confusion_matrix(
    predictions: Union[np.ndarray, torch.Tensor],
    targets: Union[np.ndarray, torch.Tensor],
    labels: Optional[List[int]] = None
) -> np.ndarray:
    """
    Compute confusion matrix.

    Args:
        predictions: Model predictions
        targets: Ground truth targets
        labels: List of labels

    Returns:
        Confusion matrix as numpy array
    """
    if isinstance(predictions, torch.Tensor):
        predictions = predictions.detach().cpu().numpy()
    if isinstance(targets, torch.Tensor):
        targets = targets.detach().cpu().numpy()

    if predictions.ndim > 1:
        predictions = predictions.argmax(axis=1)

    return confusion_matrix(targets, predictions, labels=labels)


def get_classification_report(
    predictions: Union[np.ndarray, torch.Tensor],
    targets: Union[np.ndarray, torch.Tensor],
    target_names: Optional[List[str]] = None
) -> str:
    """
    Get detailed classification report.

    Args:
        predictions: Model predictions
        targets: Ground truth targets
        target_names: Names for each class

    Returns:
        Classification report string
    """
    if isinstance(predictions, torch.Tensor):
        predictions = predictions.detach().cpu().numpy()
    if isinstance(targets, torch.Tensor):
        targets = targets.detach().cpu().numpy()

    if predictions.ndim > 1:
        predictions = predictions.argmax(axis=1)

    return classification_report(targets, predictions, target_names=target_names, zero_division=0)
