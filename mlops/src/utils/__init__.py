"""
Utilities Module

This module provides utility functions and classes for configuration,
logging, and model registry management.
"""

from src.utils.config import (
    load_config,
    load_configs,
    save_config,
    get_config_value,
    set_config_value,
    resolve_env_vars,
    validate_config,
    config_to_dict,
    dict_to_config,
    ConfigManager,
)
from src.utils.logging import (
    setup_logging,
    get_logger,
    LoggerAdapter,
    create_logger_with_context,
    TrainingLogger,
)
from src.utils.registry import (
    ModelRegistry,
    ModelMetadata,
    ModelStage,
    create_registry,
)

__all__ = [
    # Config
    "load_config",
    "load_configs",
    "save_config",
    "get_config_value",
    "set_config_value",
    "resolve_env_vars",
    "validate_config",
    "config_to_dict",
    "dict_to_config",
    "ConfigManager",
    # Logging
    "setup_logging",
    "get_logger",
    "LoggerAdapter",
    "create_logger_with_context",
    "TrainingLogger",
    # Registry
    "ModelRegistry",
    "ModelMetadata",
    "ModelStage",
    "create_registry",
]
