"""
Evaluation Module

This module provides model evaluation and metrics computation utilities.
"""

from src.evaluation.metrics import (
    Metric,
    Accuracy,
    Precision,
    Recall,
    F1Score,
    AUROC,
    MSE,
    MAE,
    R2Score,
    MetricCollection,
    create_classification_metrics,
    create_regression_metrics,
    compute_confusion_matrix,
    get_classification_report,
)
from src.evaluation.evaluator import (
    Evaluator,
    ModelComparison,
    evaluate_model,
)

__all__ = [
    # Metrics
    "Metric",
    "Accuracy",
    "Precision",
    "Recall",
    "F1Score",
    "AUROC",
    "MSE",
    "MAE",
    "R2Score",
    "MetricCollection",
    "create_classification_metrics",
    "create_regression_metrics",
    "compute_confusion_matrix",
    "get_classification_report",
    # Evaluator
    "Evaluator",
    "ModelComparison",
    "evaluate_model",
]
