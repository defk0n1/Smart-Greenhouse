"""
MLOps Pipeline Source Package

This package provides a comprehensive MLOps pipeline with:
- Data ingestion and preprocessing
- Model training with distributed support
- Experiment tracking with W&B
- Model evaluation and registry
"""

__version__ = "1.0.0"

from src.data import (
    DataIngester,
    Preprocessor,
    DataValidator,
    DataVersioner,
)
from src.models import (
    create_model,
    BaseModel,
)
from src.training import (
    Trainer,
    create_trainer,
)
from src.evaluation import (
    Evaluator,
    evaluate_model,
)
from src.utils import (
    load_config,
    get_logger,
    ModelRegistry,
)

__all__ = [
    # Version
    "__version__",
    # Data
    "DataIngester",
    "Preprocessor",
    "DataValidator",
    "DataVersioner",
    # Models
    "create_model",
    "BaseModel",
    # Training
    "Trainer",
    "create_trainer",
    # Evaluation
    "Evaluator",
    "evaluate_model",
    # Utils
    "load_config",
    "get_logger",
    "ModelRegistry",
]
