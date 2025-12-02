"""
Evaluator Module

This module provides model evaluation functionality with
comprehensive metrics computation and reporting.
"""

from pathlib import Path
from typing import Any, Dict, List, Optional, Union

import numpy as np
import torch
import torch.nn as nn
from omegaconf import DictConfig, OmegaConf
from torch.utils.data import DataLoader
from tqdm import tqdm

from src.evaluation.metrics import (
    MetricCollection,
    create_classification_metrics,
    create_regression_metrics,
    compute_confusion_matrix,
    get_classification_report,
)
from src.models.base import BaseModel
from src.utils.logging import get_logger

logger = get_logger(__name__)

try:
    import wandb
    WANDB_AVAILABLE = True
except ImportError:
    WANDB_AVAILABLE = False


class Evaluator:
    """
    Model evaluator for comprehensive evaluation and reporting.
    """

    def __init__(
        self,
        model: BaseModel,
        data_loader: DataLoader,
        metrics: Optional[MetricCollection] = None,
        device: Optional[str] = None,
        task_type: str = "classification"
    ):
        """
        Initialize evaluator.

        Args:
            model: Model to evaluate
            data_loader: Data loader for evaluation data
            metrics: Metric collection (auto-created if None)
            device: Device to use
            task_type: Type of task ('classification' or 'regression')
        """
        self.model = model
        self.data_loader = data_loader
        self.device = device or self._get_device()
        self.task_type = task_type

        # Initialize metrics
        if metrics is None:
            if task_type == "classification":
                self.metrics = create_classification_metrics()
            else:
                self.metrics = create_regression_metrics()
        else:
            self.metrics = metrics

        # Move model to device
        self.model = self.model.to(self.device)

        # Storage for predictions
        self._all_predictions: List[np.ndarray] = []
        self._all_targets: List[np.ndarray] = []
        self._all_probs: List[np.ndarray] = []

    def _get_device(self) -> str:
        """Get the best available device."""
        if torch.cuda.is_available():
            return "cuda"
        elif torch.backends.mps.is_available():
            return "mps"
        return "cpu"

    def evaluate(self) -> Dict[str, Any]:
        """
        Run evaluation.

        Returns:
            Dictionary with evaluation results
        """
        logger.info("Starting evaluation...")

        self.model.eval()
        self.metrics.reset()
        self._all_predictions = []
        self._all_targets = []
        self._all_probs = []

        with torch.no_grad():
            for batch in tqdm(self.data_loader, desc="Evaluating"):
                # Unpack batch
                if len(batch) == 2:
                    inputs, targets = batch
                else:
                    inputs, targets = batch[0], batch[1]

                inputs = inputs.to(self.device)
                targets = targets.to(self.device)

                # Forward pass
                outputs = self.model(inputs)

                # Get predictions
                if self.task_type == "classification":
                    probs = torch.softmax(outputs, dim=1)
                    preds = outputs.argmax(dim=1)

                    self._all_probs.append(probs.cpu().numpy())
                    self._all_predictions.append(preds.cpu().numpy())
                else:
                    self._all_predictions.append(outputs.cpu().numpy())

                self._all_targets.append(targets.cpu().numpy())

                # Update metrics
                self.metrics.update(outputs, targets)

        # Compute metrics
        results = self.metrics.compute()

        # Add additional analysis for classification
        if self.task_type == "classification":
            predictions = np.concatenate(self._all_predictions)
            targets = np.concatenate(self._all_targets)

            results["confusion_matrix"] = compute_confusion_matrix(predictions, targets)
            results["classification_report"] = get_classification_report(predictions, targets)

        logger.info(f"Evaluation complete: {results}")

        return results

    def get_predictions(self) -> Dict[str, np.ndarray]:
        """
        Get all predictions and targets.

        Returns:
            Dictionary with predictions, targets, and probabilities
        """
        result = {
            "predictions": np.concatenate(self._all_predictions),
            "targets": np.concatenate(self._all_targets),
        }

        if self._all_probs:
            result["probabilities"] = np.concatenate(self._all_probs)

        return result

    def log_to_wandb(
        self,
        results: Dict[str, Any],
        prefix: str = "eval"
    ) -> None:
        """
        Log evaluation results to W&B.

        Args:
            results: Evaluation results
            prefix: Prefix for metric names
        """
        if not WANDB_AVAILABLE or wandb.run is None:
            logger.warning("W&B not available for logging")
            return

        # Log scalar metrics
        log_dict = {}
        for key, value in results.items():
            if isinstance(value, (int, float)):
                log_dict[f"{prefix}/{key}"] = value

        wandb.log(log_dict)

        # Log confusion matrix
        if "confusion_matrix" in results:
            cm = results["confusion_matrix"]
            wandb.log({
                f"{prefix}/confusion_matrix": wandb.plot.confusion_matrix(
                    y_true=np.concatenate(self._all_targets),
                    preds=np.concatenate(self._all_predictions),
                    class_names=[str(i) for i in range(cm.shape[0])]
                )
            })

        logger.info("Logged evaluation results to W&B")


class ModelComparison:
    """
    Utility for comparing multiple models.
    """

    def __init__(
        self,
        data_loader: DataLoader,
        device: Optional[str] = None,
        task_type: str = "classification"
    ):
        """
        Initialize model comparison.

        Args:
            data_loader: Data loader for evaluation
            device: Device to use
            task_type: Type of task
        """
        self.data_loader = data_loader
        self.device = device
        self.task_type = task_type

        self.results: Dict[str, Dict[str, Any]] = {}

    def add_model(self, name: str, model: BaseModel) -> None:
        """
        Add and evaluate a model.

        Args:
            name: Model name
            model: Model to evaluate
        """
        evaluator = Evaluator(
            model=model,
            data_loader=self.data_loader,
            device=self.device,
            task_type=self.task_type
        )

        results = evaluator.evaluate()
        self.results[name] = results

        logger.info(f"Evaluated model '{name}'")

    def get_comparison(self, metrics: Optional[List[str]] = None) -> Dict[str, Dict[str, float]]:
        """
        Get comparison of models.

        Args:
            metrics: List of metrics to compare (all if None)

        Returns:
            Dictionary mapping model names to metric values
        """
        comparison = {}

        for name, results in self.results.items():
            comparison[name] = {}

            for key, value in results.items():
                if isinstance(value, (int, float)):
                    if metrics is None or key in metrics:
                        comparison[name][key] = value

        return comparison

    def get_best_model(self, metric: str, mode: str = "max") -> str:
        """
        Get the best model based on a metric.

        Args:
            metric: Metric to use for comparison
            mode: 'max' or 'min'

        Returns:
            Name of the best model
        """
        best_name = None
        best_value = None

        for name, results in self.results.items():
            if metric not in results:
                continue

            value = results[metric]

            if best_value is None:
                best_name = name
                best_value = value
            elif mode == "max" and value > best_value:
                best_name = name
                best_value = value
            elif mode == "min" and value < best_value:
                best_name = name
                best_value = value

        return best_name


def evaluate_model(
    model: BaseModel,
    data_loader: DataLoader,
    config: Optional[DictConfig] = None,
    device: Optional[str] = None
) -> Dict[str, Any]:
    """
    Convenience function to evaluate a model.

    Args:
        model: Model to evaluate
        data_loader: Data loader
        config: Optional configuration
        device: Device to use

    Returns:
        Evaluation results
    """
    task_type = "classification"
    if config is not None:
        task_type = config.get("task_type", "classification")

    evaluator = Evaluator(
        model=model,
        data_loader=data_loader,
        device=device,
        task_type=task_type
    )

    return evaluator.evaluate()
