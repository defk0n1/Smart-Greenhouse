"""
Logging Utilities Module

This module provides structured logging capabilities.
"""

import logging
import sys
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, Optional

try:
    import structlog
    STRUCTLOG_AVAILABLE = True
except ImportError:
    STRUCTLOG_AVAILABLE = False


# Global logger cache
_loggers: Dict[str, logging.Logger] = {}


def setup_logging(
    level: str = "INFO",
    log_format: str = "structured",
    log_file: Optional[str] = None,
    log_to_console: bool = True
) -> None:
    """
    Set up logging configuration.

    Args:
        level: Logging level (DEBUG, INFO, WARNING, ERROR, CRITICAL)
        log_format: Format type ('structured' or 'simple')
        log_file: Optional path to log file
        log_to_console: Whether to log to console
    """
    log_level = getattr(logging, level.upper(), logging.INFO)

    # Configure root logger
    root_logger = logging.getLogger()
    root_logger.setLevel(log_level)

    # Clear existing handlers
    root_logger.handlers = []

    # Create formatter
    if log_format == "structured" and STRUCTLOG_AVAILABLE:
        _setup_structlog(log_level)
    else:
        formatter = logging.Formatter(
            "%(asctime)s - %(name)s - %(levelname)s - %(message)s",
            datefmt="%Y-%m-%d %H:%M:%S"
        )

        # Console handler
        if log_to_console:
            console_handler = logging.StreamHandler(sys.stdout)
            console_handler.setLevel(log_level)
            console_handler.setFormatter(formatter)
            root_logger.addHandler(console_handler)

        # File handler
        if log_file:
            log_path = Path(log_file)
            log_path.parent.mkdir(parents=True, exist_ok=True)

            file_handler = logging.FileHandler(log_file)
            file_handler.setLevel(log_level)
            file_handler.setFormatter(formatter)
            root_logger.addHandler(file_handler)


def _setup_structlog(level: int) -> None:
    """Set up structlog configuration."""
    if not STRUCTLOG_AVAILABLE:
        return

    structlog.configure(
        processors=[
            structlog.stdlib.filter_by_level,
            structlog.stdlib.add_logger_name,
            structlog.stdlib.add_log_level,
            structlog.stdlib.PositionalArgumentsFormatter(),
            structlog.processors.TimeStamper(fmt="iso"),
            structlog.processors.StackInfoRenderer(),
            structlog.processors.format_exc_info,
            structlog.processors.UnicodeDecoder(),
            structlog.processors.JSONRenderer()
        ],
        context_class=dict,
        logger_factory=structlog.stdlib.LoggerFactory(),
        wrapper_class=structlog.stdlib.BoundLogger,
        cache_logger_on_first_use=True,
    )

    # Configure standard logging to work with structlog
    logging.basicConfig(
        format="%(message)s",
        stream=sys.stdout,
        level=level,
    )


def get_logger(name: str) -> logging.Logger:
    """
    Get a logger by name.

    Args:
        name: Logger name (usually __name__)

    Returns:
        Logger instance
    """
    if name in _loggers:
        return _loggers[name]

    if STRUCTLOG_AVAILABLE:
        logger = structlog.get_logger(name)
    else:
        logger = logging.getLogger(name)

    _loggers[name] = logger
    return logger


class LoggerAdapter(logging.LoggerAdapter):
    """
    Logger adapter with context support.
    """

    def __init__(
        self,
        logger: logging.Logger,
        extra: Optional[Dict[str, Any]] = None
    ):
        """
        Initialize logger adapter.

        Args:
            logger: Base logger
            extra: Extra context to include in logs
        """
        super().__init__(logger, extra or {})

    def process(self, msg: str, kwargs: Dict[str, Any]) -> tuple:
        """Process log message with extra context."""
        extra = kwargs.get("extra", {})
        extra.update(self.extra)
        kwargs["extra"] = extra
        return msg, kwargs

    def bind(self, **kwargs: Any) -> "LoggerAdapter":
        """
        Create a new adapter with additional context.

        Args:
            **kwargs: Additional context

        Returns:
            New LoggerAdapter with merged context
        """
        new_extra = {**self.extra, **kwargs}
        return LoggerAdapter(self.logger, new_extra)


def create_logger_with_context(
    name: str,
    **context: Any
) -> LoggerAdapter:
    """
    Create a logger with context.

    Args:
        name: Logger name
        **context: Context to include in all logs

    Returns:
        LoggerAdapter with context
    """
    logger = get_logger(name)
    return LoggerAdapter(logger, context)


class TrainingLogger:
    """
    Specialized logger for training progress.
    """

    def __init__(
        self,
        name: str = "training",
        log_frequency: int = 100
    ):
        """
        Initialize training logger.

        Args:
            name: Logger name
            log_frequency: Log every N iterations
        """
        self.logger = get_logger(name)
        self.log_frequency = log_frequency
        self._step = 0
        self._epoch = 0

    def set_epoch(self, epoch: int) -> None:
        """Set current epoch."""
        self._epoch = epoch

    def log_step(
        self,
        step: int,
        metrics: Dict[str, float],
        force: bool = False
    ) -> None:
        """
        Log training step.

        Args:
            step: Current step
            metrics: Metrics to log
            force: Force logging regardless of frequency
        """
        self._step = step

        if not force and step % self.log_frequency != 0:
            return

        metrics_str = ", ".join(f"{k}={v:.4f}" for k, v in metrics.items())
        self.logger.info(
            f"Epoch {self._epoch + 1} - Step {step} - {metrics_str}"
        )

    def log_epoch(
        self,
        epoch: int,
        train_metrics: Dict[str, float],
        val_metrics: Optional[Dict[str, float]] = None
    ) -> None:
        """
        Log epoch summary.

        Args:
            epoch: Epoch number
            train_metrics: Training metrics
            val_metrics: Validation metrics
        """
        self._epoch = epoch

        train_str = ", ".join(f"{k}={v:.4f}" for k, v in train_metrics.items())
        log_msg = f"Epoch {epoch + 1} - Train: {train_str}"

        if val_metrics:
            val_str = ", ".join(f"{k}={v:.4f}" for k, v in val_metrics.items())
            log_msg += f" - Val: {val_str}"

        self.logger.info(log_msg)

    def log_model_info(
        self,
        model_name: str,
        num_params: int,
        trainable_params: int
    ) -> None:
        """
        Log model information.

        Args:
            model_name: Name of the model
            num_params: Total number of parameters
            trainable_params: Number of trainable parameters
        """
        self.logger.info(
            f"Model: {model_name} - "
            f"Total params: {num_params:,} - "
            f"Trainable params: {trainable_params:,}"
        )

    def log_error(self, error: Exception, context: Optional[str] = None) -> None:
        """
        Log an error.

        Args:
            error: Exception to log
            context: Additional context
        """
        msg = f"Error: {error}"
        if context:
            msg = f"{context}: {msg}"
        self.logger.error(msg, exc_info=True)
