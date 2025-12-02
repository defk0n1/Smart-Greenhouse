"""
Configuration Utilities Module

This module provides configuration loading and management utilities.
"""

import os
from pathlib import Path
from typing import Any, Dict, List, Optional, Union

from omegaconf import DictConfig, OmegaConf

from src.utils.logging import get_logger

logger = get_logger(__name__)


def load_config(
    config_path: Union[str, Path],
    overrides: Optional[Dict[str, Any]] = None
) -> DictConfig:
    """
    Load configuration from YAML file.

    Args:
        config_path: Path to configuration file
        overrides: Dictionary of configuration overrides

    Returns:
        OmegaConf DictConfig object
    """
    config_path = Path(config_path)

    if not config_path.exists():
        raise FileNotFoundError(f"Configuration file not found: {config_path}")

    # Load base configuration
    config = OmegaConf.load(config_path)

    # Apply overrides
    if overrides:
        override_config = OmegaConf.create(overrides)
        config = OmegaConf.merge(config, override_config)

    # Resolve environment variables
    OmegaConf.resolve(config)

    logger.info(f"Loaded configuration from {config_path}")

    return config


def load_configs(
    config_paths: List[Union[str, Path]],
    overrides: Optional[Dict[str, Any]] = None
) -> DictConfig:
    """
    Load and merge multiple configuration files.

    Args:
        config_paths: List of paths to configuration files
        overrides: Dictionary of configuration overrides

    Returns:
        Merged OmegaConf DictConfig object
    """
    configs = []

    for path in config_paths:
        config = load_config(path)
        configs.append(config)

    # Merge all configs
    merged = OmegaConf.merge(*configs)

    # Apply overrides
    if overrides:
        override_config = OmegaConf.create(overrides)
        merged = OmegaConf.merge(merged, override_config)

    return merged


def save_config(config: DictConfig, path: Union[str, Path]) -> None:
    """
    Save configuration to YAML file.

    Args:
        config: Configuration to save
        path: Path to save configuration
    """
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)

    with open(path, "w") as f:
        OmegaConf.save(config, f)

    logger.info(f"Saved configuration to {path}")


def get_config_value(
    config: DictConfig,
    key: str,
    default: Any = None
) -> Any:
    """
    Get a configuration value by dot-separated key.

    Args:
        config: Configuration object
        key: Dot-separated key (e.g., 'training.optimizer.lr')
        default: Default value if key not found

    Returns:
        Configuration value or default
    """
    keys = key.split(".")
    value = config

    try:
        for k in keys:
            value = value[k]
        return value
    except (KeyError, TypeError):
        return default


def set_config_value(
    config: DictConfig,
    key: str,
    value: Any
) -> DictConfig:
    """
    Set a configuration value by dot-separated key.

    Args:
        config: Configuration object
        key: Dot-separated key
        value: Value to set

    Returns:
        Modified configuration
    """
    keys = key.split(".")
    current = config

    for k in keys[:-1]:
        if k not in current:
            current[k] = {}
        current = current[k]

    current[keys[-1]] = value
    return config


def resolve_env_vars(config: DictConfig) -> DictConfig:
    """
    Resolve environment variables in configuration.

    Supports ${ENV_VAR} and ${ENV_VAR:default} syntax.

    Args:
        config: Configuration object

    Returns:
        Configuration with resolved environment variables
    """
    def resolve_value(value: Any) -> Any:
        if isinstance(value, str):
            # Check for ${...} pattern
            import re
            pattern = r'\$\{([^}:]+)(?::([^}]*))?\}'

            def replace(match):
                var_name = match.group(1)
                default = match.group(2)
                env_value = os.environ.get(var_name)

                if env_value is not None:
                    return env_value
                elif default is not None:
                    return default
                else:
                    return match.group(0)

            return re.sub(pattern, replace, value)
        return value

    def resolve_dict(d: DictConfig) -> DictConfig:
        for key in d:
            if isinstance(d[key], DictConfig):
                resolve_dict(d[key])
            else:
                d[key] = resolve_value(d[key])
        return d

    return resolve_dict(config)


def validate_config(
    config: DictConfig,
    required_keys: List[str]
) -> bool:
    """
    Validate that configuration contains required keys.

    Args:
        config: Configuration object
        required_keys: List of required dot-separated keys

    Returns:
        True if all required keys are present

    Raises:
        ValueError: If any required key is missing
    """
    missing = []

    for key in required_keys:
        if get_config_value(config, key) is None:
            missing.append(key)

    if missing:
        raise ValueError(f"Missing required configuration keys: {missing}")

    return True


def config_to_dict(config: DictConfig) -> Dict[str, Any]:
    """
    Convert OmegaConf configuration to plain dictionary.

    Args:
        config: Configuration object

    Returns:
        Plain dictionary
    """
    return OmegaConf.to_container(config, resolve=True)


def dict_to_config(d: Dict[str, Any]) -> DictConfig:
    """
    Convert dictionary to OmegaConf configuration.

    Args:
        d: Dictionary

    Returns:
        OmegaConf DictConfig
    """
    return OmegaConf.create(d)


class ConfigManager:
    """
    Configuration manager for handling multiple configuration files.
    """

    def __init__(self, base_dir: Union[str, Path] = "config"):
        """
        Initialize configuration manager.

        Args:
            base_dir: Base directory for configuration files
        """
        self.base_dir = Path(base_dir)
        self._configs: Dict[str, DictConfig] = {}

    def load(
        self,
        name: str,
        filename: Optional[str] = None
    ) -> DictConfig:
        """
        Load a configuration by name.

        Args:
            name: Configuration name
            filename: Optional filename (defaults to {name}.yaml)

        Returns:
            Configuration object
        """
        if name in self._configs:
            return self._configs[name]

        filename = filename or f"{name}.yaml"
        path = self.base_dir / filename

        config = load_config(path)
        self._configs[name] = config

        return config

    def get(self, name: str) -> Optional[DictConfig]:
        """
        Get a loaded configuration.

        Args:
            name: Configuration name

        Returns:
            Configuration object or None
        """
        return self._configs.get(name)

    def merge_all(self) -> DictConfig:
        """
        Merge all loaded configurations.

        Returns:
            Merged configuration
        """
        if not self._configs:
            return OmegaConf.create({})

        return OmegaConf.merge(*self._configs.values())

    def save_merged(self, path: Union[str, Path]) -> None:
        """
        Save merged configuration to file.

        Args:
            path: Path to save configuration
        """
        merged = self.merge_all()
        save_config(merged, path)
