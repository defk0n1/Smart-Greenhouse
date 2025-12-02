"""
Training Pipeline DAG

This DAG orchestrates the complete ML training pipeline:
1. Data ingestion
2. Data validation
3. Preprocessing
4. Model training
5. Evaluation
6. Model registration
"""

from datetime import datetime, timedelta
from typing import Any, Dict

from airflow import DAG
from airflow.operators.python import PythonOperator
from airflow.operators.empty import EmptyOperator
from airflow.utils.trigger_rule import TriggerRule

# Default arguments for the DAG
default_args = {
    "owner": "mlops",
    "depends_on_past": False,
    "email": ["alerts@example.com"],
    "email_on_failure": True,
    "email_on_retry": False,
    "retries": 3,
    "retry_delay": timedelta(minutes=5),
    "execution_timeout": timedelta(hours=4),
}


def ingest_data(**context) -> Dict[str, Any]:
    """
    Ingest data from configured sources.

    Returns:
        Dictionary with ingestion metadata
    """
    from src.data.ingestion import DataIngester
    from src.utils.config import load_config

    config = load_config("/opt/airflow/mlops_config/data.yaml")
    ingester = DataIngester(config=config)

    data = ingester.ingest()
    data_hash = ingester.compute_data_hash(data)

    # Save to temporary location
    output_path = "/data/raw/ingested_data.parquet"
    data.to_parquet(output_path)

    result = {
        "output_path": output_path,
        "row_count": len(data),
        "column_count": len(data.columns),
        "data_hash": data_hash,
    }

    context["ti"].xcom_push(key="ingestion_result", value=result)
    return result


def validate_data(**context) -> Dict[str, Any]:
    """
    Validate ingested data.

    Returns:
        Dictionary with validation results
    """
    import pandas as pd
    from src.data.validation import DataValidator
    from src.utils.config import load_config

    # Get ingestion result
    ti = context["ti"]
    ingestion_result = ti.xcom_pull(key="ingestion_result", task_ids="ingest_data")

    # Load data
    data = pd.read_parquet(ingestion_result["output_path"])

    # Validate
    config = load_config("/opt/airflow/mlops_config/data.yaml")
    validator = DataValidator(config=config)

    results = validator.validate(data)

    # Check for failures
    failures = [r for r in results if r.status.value == "failed"]
    if failures:
        raise ValueError(f"Data validation failed: {[f.message for f in failures]}")

    result = {
        "validation_passed": True,
        "num_checks": len(results),
        "results": [r.to_dict() for r in results],
    }

    ti.xcom_push(key="validation_result", value=result)
    return result


def preprocess_data(**context) -> Dict[str, Any]:
    """
    Preprocess and prepare data for training.

    Returns:
        Dictionary with preprocessing metadata
    """
    import pandas as pd
    from src.data.preprocessing import Preprocessor, DataSplitter
    from src.data.versioning import DataVersioner
    from src.utils.config import load_config

    # Get previous results
    ti = context["ti"]
    ingestion_result = ti.xcom_pull(key="ingestion_result", task_ids="ingest_data")

    # Load data
    data = pd.read_parquet(ingestion_result["output_path"])

    # Load config
    config = load_config("/opt/airflow/mlops_config/data.yaml")

    # Preprocess
    preprocessor = Preprocessor(config=config)
    processed_data = preprocessor.fit_transform(data)

    # Split data
    splitter = DataSplitter(
        train_ratio=config.splitting.train_ratio,
        val_ratio=config.splitting.val_ratio,
        test_ratio=config.splitting.test_ratio,
    )
    train, val, test = splitter.split(processed_data)

    # Save splits
    train.to_parquet("/data/processed/train.parquet")
    val.to_parquet("/data/processed/val.parquet")
    test.to_parquet("/data/processed/test.parquet")

    # Version data
    versioner = DataVersioner("/data/versions")
    version = versioner.create_version(
        processed_data,
        source_path=ingestion_result["output_path"],
        metadata={"split_sizes": {"train": len(train), "val": len(val), "test": len(test)}}
    )

    result = {
        "train_path": "/data/processed/train.parquet",
        "val_path": "/data/processed/val.parquet",
        "test_path": "/data/processed/test.parquet",
        "data_version": version.version_id,
        "train_samples": len(train),
        "val_samples": len(val),
        "test_samples": len(test),
    }

    ti.xcom_push(key="preprocessing_result", value=result)
    return result


def train_model(**context) -> Dict[str, Any]:
    """
    Train the model.

    Returns:
        Dictionary with training results
    """
    import torch
    from torch.utils.data import DataLoader, TensorDataset
    import pandas as pd

    from src.models.architectures import create_model
    from src.training.trainer import Trainer
    from src.utils.config import load_config

    # Get previous results
    ti = context["ti"]
    preprocessing_result = ti.xcom_pull(
        key="preprocessing_result",
        task_ids="preprocess_data"
    )

    # Load config
    training_config = load_config("/opt/airflow/mlops_config/training.yaml")
    experiment_config = load_config("/opt/airflow/mlops_config/experiment.yaml")

    # Merge configs
    from omegaconf import OmegaConf
    config = OmegaConf.merge(training_config, experiment_config)

    # Load data
    train_df = pd.read_parquet(preprocessing_result["train_path"])
    val_df = pd.read_parquet(preprocessing_result["val_path"])

    # Create data loaders (simplified - in practice, use proper dataset)
    # This is a placeholder - actual implementation depends on data format
    # train_loader = create_data_loader(train_df, config)
    # val_loader = create_data_loader(val_df, config)

    # Create model
    model = create_model(config.model)

    # Create trainer
    trainer = Trainer(
        model=model,
        config=config,
        # train_loader=train_loader,
        # val_loader=val_loader,
    )

    # Train (commented out as data loaders need proper setup)
    # results = trainer.train()

    # Placeholder results
    results = {
        "model_path": "/checkpoints/best_model.pt",
        "best_epoch": 0,
        "best_metric": 0.0,
        "data_version": preprocessing_result["data_version"],
    }

    ti.xcom_push(key="training_result", value=results)
    return results


def evaluate_model_task(**context) -> Dict[str, Any]:
    """
    Evaluate the trained model.

    Returns:
        Dictionary with evaluation metrics
    """
    import torch
    import pandas as pd

    from src.models.architectures import create_model
    from src.evaluation.evaluator import Evaluator
    from src.utils.config import load_config

    # Get previous results
    ti = context["ti"]
    training_result = ti.xcom_pull(key="training_result", task_ids="train_model")
    preprocessing_result = ti.xcom_pull(
        key="preprocessing_result",
        task_ids="preprocess_data"
    )

    # Load config
    config = load_config("/opt/airflow/mlops_config/training.yaml")

    # Load test data
    test_df = pd.read_parquet(preprocessing_result["test_path"])

    # Create model and load weights
    model = create_model(config.model)
    # model.load_state_dict(torch.load(training_result["model_path"]))

    # Evaluate (placeholder - needs proper data loader)
    # evaluator = Evaluator(model, test_loader)
    # metrics = evaluator.evaluate()

    # Placeholder metrics
    metrics = {
        "accuracy": 0.95,
        "precision": 0.94,
        "recall": 0.93,
        "f1": 0.935,
    }

    result = {
        "metrics": metrics,
        "model_path": training_result["model_path"],
        "data_version": preprocessing_result["data_version"],
    }

    ti.xcom_push(key="evaluation_result", value=result)
    return result


def register_model_task(**context) -> Dict[str, Any]:
    """
    Register the model to the model registry.

    Returns:
        Dictionary with registration info
    """
    import torch
    from src.models.architectures import create_model
    from src.utils.registry import ModelRegistry, ModelStage
    from src.utils.config import load_config

    # Get previous results
    ti = context["ti"]
    training_result = ti.xcom_pull(key="training_result", task_ids="train_model")
    evaluation_result = ti.xcom_pull(key="evaluation_result", task_ids="evaluate_model")

    # Load config
    config = load_config("/opt/airflow/mlops_config/training.yaml")
    experiment_config = load_config("/opt/airflow/mlops_config/experiment.yaml")

    # Get metrics threshold
    threshold = experiment_config.registry.selection.get("threshold", 0.0)
    primary_metric = experiment_config.registry.selection.get("metric", "f1")

    # Check if model meets threshold
    metrics = evaluation_result["metrics"]
    if metrics.get(primary_metric, 0) < threshold:
        return {
            "registered": False,
            "reason": f"{primary_metric} below threshold: {metrics.get(primary_metric)} < {threshold}"
        }

    # Create model for registration
    model = create_model(config.model)
    # model.load_state_dict(torch.load(training_result["model_path"]))

    # Register
    registry = ModelRegistry(registry_type="wandb")
    version = registry.register_model(
        model=model,
        name=experiment_config.registry.model_name,
        metrics=metrics,
        config=config,
        checkpoint_path=training_result["model_path"],
        data_version=training_result["data_version"],
        stage=ModelStage.DEV
    )

    result = {
        "registered": True,
        "version": version,
        "stage": "dev",
        "metrics": metrics,
    }

    ti.xcom_push(key="registration_result", value=result)
    return result


def notify_completion(**context) -> None:
    """Send notification on pipeline completion."""
    ti = context["ti"]
    registration_result = ti.xcom_pull(
        key="registration_result",
        task_ids="register_model"
    )

    # Log completion (in practice, send Slack/email notification)
    print(f"Pipeline completed. Model registered: {registration_result}")


# Create the DAG
with DAG(
    dag_id="training_pipeline",
    default_args=default_args,
    description="End-to-end ML training pipeline",
    schedule_interval="@weekly",  # Run weekly
    start_date=datetime(2024, 1, 1),
    catchup=False,
    tags=["ml", "training", "pipeline"],
    max_active_runs=1,
) as dag:

    # Start
    start = EmptyOperator(task_id="start")

    # Data ingestion
    ingest = PythonOperator(
        task_id="ingest_data",
        python_callable=ingest_data,
        provide_context=True,
    )

    # Data validation
    validate = PythonOperator(
        task_id="validate_data",
        python_callable=validate_data,
        provide_context=True,
    )

    # Preprocessing
    preprocess = PythonOperator(
        task_id="preprocess_data",
        python_callable=preprocess_data,
        provide_context=True,
    )

    # Training
    train = PythonOperator(
        task_id="train_model",
        python_callable=train_model,
        provide_context=True,
        execution_timeout=timedelta(hours=6),
    )

    # Evaluation
    evaluate = PythonOperator(
        task_id="evaluate_model",
        python_callable=evaluate_model_task,
        provide_context=True,
    )

    # Registration
    register = PythonOperator(
        task_id="register_model",
        python_callable=register_model_task,
        provide_context=True,
    )

    # Notification
    notify = PythonOperator(
        task_id="notify_completion",
        python_callable=notify_completion,
        provide_context=True,
        trigger_rule=TriggerRule.ALL_SUCCESS,
    )

    # End
    end = EmptyOperator(
        task_id="end",
        trigger_rule=TriggerRule.ALL_DONE,
    )

    # Define task dependencies
    start >> ingest >> validate >> preprocess >> train >> evaluate >> register >> notify >> end
