"""
Data Pipeline DAG

This DAG handles data ingestion and preprocessing tasks separately
from the training pipeline. Useful for scheduled data updates.
"""

from datetime import datetime, timedelta
from typing import Any, Dict

from airflow import DAG
from airflow.operators.python import PythonOperator
from airflow.operators.empty import EmptyOperator
from airflow.sensors.filesystem import FileSensor
from airflow.utils.trigger_rule import TriggerRule

# Default arguments
default_args = {
    "owner": "mlops",
    "depends_on_past": False,
    "email": ["alerts@example.com"],
    "email_on_failure": True,
    "email_on_retry": False,
    "retries": 2,
    "retry_delay": timedelta(minutes=5),
}


def check_data_source(**context) -> Dict[str, Any]:
    """
    Check if new data is available.

    Returns:
        Dictionary with data availability status
    """
    import os
    from datetime import datetime

    # Check for new data (placeholder logic)
    data_dir = "/data/raw"
    new_data_available = os.path.exists(data_dir) and os.listdir(data_dir)

    result = {
        "new_data_available": bool(new_data_available),
        "check_time": datetime.now().isoformat(),
    }

    context["ti"].xcom_push(key="data_check_result", value=result)
    return result


def ingest_new_data(**context) -> Dict[str, Any]:
    """
    Ingest new data from configured sources.

    Returns:
        Dictionary with ingestion results
    """
    from src.data.ingestion import DataIngester
    from src.utils.config import load_config

    config = load_config("/opt/airflow/mlops_config/data.yaml")
    ingester = DataIngester(config=config)

    try:
        data = ingester.ingest()
        data_hash = ingester.compute_data_hash(data)

        # Save to staging location
        output_path = "/data/staging/new_data.parquet"
        data.to_parquet(output_path)

        result = {
            "success": True,
            "output_path": output_path,
            "row_count": len(data),
            "column_count": len(data.columns),
            "data_hash": data_hash,
        }
    except Exception as e:
        result = {
            "success": False,
            "error": str(e),
        }

    context["ti"].xcom_push(key="ingestion_result", value=result)
    return result


def validate_new_data(**context) -> Dict[str, Any]:
    """
    Validate newly ingested data.

    Returns:
        Dictionary with validation results
    """
    import pandas as pd
    from src.data.validation import DataValidator
    from src.utils.config import load_config

    ti = context["ti"]
    ingestion_result = ti.xcom_pull(key="ingestion_result", task_ids="ingest_new_data")

    if not ingestion_result.get("success"):
        raise ValueError(f"Ingestion failed: {ingestion_result.get('error')}")

    # Load data
    data = pd.read_parquet(ingestion_result["output_path"])

    # Validate
    config = load_config("/opt/airflow/mlops_config/data.yaml")
    validator = DataValidator(config=config)

    results = validator.validate(data)

    failures = [r for r in results if r.status.value == "failed"]
    warnings = [r for r in results if r.status.value == "warning"]

    result = {
        "validation_passed": len(failures) == 0,
        "num_checks": len(results),
        "num_failures": len(failures),
        "num_warnings": len(warnings),
        "failures": [f.message for f in failures],
        "warnings": [w.message for w in warnings],
    }

    if failures:
        raise ValueError(f"Data validation failed: {result['failures']}")

    ti.xcom_push(key="validation_result", value=result)
    return result


def preprocess_and_version(**context) -> Dict[str, Any]:
    """
    Preprocess data and create a new version.

    Returns:
        Dictionary with preprocessing and versioning results
    """
    import pandas as pd
    from src.data.preprocessing import Preprocessor, DataSplitter
    from src.data.versioning import DataVersioner
    from src.utils.config import load_config

    ti = context["ti"]
    ingestion_result = ti.xcom_pull(key="ingestion_result", task_ids="ingest_new_data")

    # Load data
    data = pd.read_parquet(ingestion_result["output_path"])

    # Load config
    config = load_config("/opt/airflow/mlops_config/data.yaml")

    # Preprocess
    preprocessor = Preprocessor(config=config)
    processed_data = preprocessor.fit_transform(data)

    # Split data
    split_config = config.get("splitting", {})
    splitter = DataSplitter(
        train_ratio=split_config.get("train_ratio", 0.7),
        val_ratio=split_config.get("val_ratio", 0.15),
        test_ratio=split_config.get("test_ratio", 0.15),
        stratify_column=split_config.get("stratify_column"),
    )

    train, val, test = splitter.split(processed_data)

    # Save processed data
    train_path = "/data/processed/train.parquet"
    val_path = "/data/processed/val.parquet"
    test_path = "/data/processed/test.parquet"

    train.to_parquet(train_path)
    val.to_parquet(val_path)
    test.to_parquet(test_path)

    # Version the data
    versioner = DataVersioner("/data/versions")
    version = versioner.create_version(
        processed_data,
        source_path=ingestion_result["output_path"],
        metadata={
            "original_hash": ingestion_result["data_hash"],
            "split_sizes": {
                "train": len(train),
                "val": len(val),
                "test": len(test)
            },
        }
    )

    result = {
        "version_id": version.version_id,
        "data_hash": version.data_hash,
        "train_path": train_path,
        "val_path": val_path,
        "test_path": test_path,
        "train_samples": len(train),
        "val_samples": len(val),
        "test_samples": len(test),
    }

    ti.xcom_push(key="preprocessing_result", value=result)
    return result


def detect_data_drift(**context) -> Dict[str, Any]:
    """
    Detect data drift compared to previous version.

    Returns:
        Dictionary with drift detection results
    """
    import pandas as pd
    from src.data.versioning import DataVersioner

    ti = context["ti"]
    preprocessing_result = ti.xcom_pull(
        key="preprocessing_result",
        task_ids="preprocess_and_version"
    )

    versioner = DataVersioner("/data/versions")

    # Get current and previous versions
    versions = versioner.list_versions()

    if len(versions) < 2:
        # No previous version to compare
        return {
            "drift_detected": False,
            "reason": "No previous version available for comparison",
        }

    current_version = versions[0]
    previous_version = versions[1]

    # Compare versions
    comparison = versioner.compare_versions(
        previous_version.version_id,
        current_version.version_id
    )

    # Check for significant changes
    significant_drift = (
        abs(comparison["row_count_diff"]) > len(current_version.row_count) * 0.1 or
        len(comparison["schema_changes"]) > 0
    )

    result = {
        "drift_detected": significant_drift,
        "row_count_diff": comparison["row_count_diff"],
        "schema_changes": comparison["schema_changes"],
        "statistics_changes": comparison["statistics_changes"],
        "current_version": current_version.version_id,
        "previous_version": previous_version.version_id,
    }

    ti.xcom_push(key="drift_result", value=result)
    return result


def trigger_retraining(**context) -> Dict[str, Any]:
    """
    Decide whether to trigger model retraining.

    Returns:
        Dictionary with retraining decision
    """
    from airflow.operators.trigger_dagrun import TriggerDagRunOperator

    ti = context["ti"]
    drift_result = ti.xcom_pull(key="drift_result", task_ids="detect_data_drift")
    preprocessing_result = ti.xcom_pull(
        key="preprocessing_result",
        task_ids="preprocess_and_version"
    )

    should_retrain = drift_result.get("drift_detected", False)

    if should_retrain:
        # In a real scenario, trigger the training DAG
        # TriggerDagRunOperator(
        #     task_id="trigger_training",
        #     trigger_dag_id="training_pipeline",
        #     conf={"data_version": preprocessing_result["version_id"]},
        # ).execute(context)
        pass

    result = {
        "retraining_triggered": should_retrain,
        "reason": "Data drift detected" if should_retrain else "No significant drift",
        "data_version": preprocessing_result["version_id"],
    }

    ti.xcom_push(key="retraining_decision", value=result)
    return result


def cleanup_staging(**context) -> None:
    """Clean up staging data after processing."""
    import os
    import shutil

    staging_dir = "/data/staging"
    if os.path.exists(staging_dir):
        shutil.rmtree(staging_dir)
        os.makedirs(staging_dir, exist_ok=True)

    print("Staging directory cleaned up")


# Create the DAG
with DAG(
    dag_id="data_pipeline",
    default_args=default_args,
    description="Data ingestion and preprocessing pipeline",
    schedule_interval="@daily",  # Run daily
    start_date=datetime(2024, 1, 1),
    catchup=False,
    tags=["ml", "data", "pipeline"],
    max_active_runs=1,
) as dag:

    # Start
    start = EmptyOperator(task_id="start")

    # Check for new data
    check_data = PythonOperator(
        task_id="check_data_source",
        python_callable=check_data_source,
        provide_context=True,
    )

    # Ingest new data
    ingest = PythonOperator(
        task_id="ingest_new_data",
        python_callable=ingest_new_data,
        provide_context=True,
    )

    # Validate data
    validate = PythonOperator(
        task_id="validate_new_data",
        python_callable=validate_new_data,
        provide_context=True,
    )

    # Preprocess and version
    preprocess = PythonOperator(
        task_id="preprocess_and_version",
        python_callable=preprocess_and_version,
        provide_context=True,
    )

    # Detect drift
    detect_drift = PythonOperator(
        task_id="detect_data_drift",
        python_callable=detect_data_drift,
        provide_context=True,
    )

    # Trigger retraining decision
    retrain_decision = PythonOperator(
        task_id="trigger_retraining",
        python_callable=trigger_retraining,
        provide_context=True,
    )

    # Cleanup
    cleanup = PythonOperator(
        task_id="cleanup_staging",
        python_callable=cleanup_staging,
        provide_context=True,
        trigger_rule=TriggerRule.ALL_DONE,
    )

    # End
    end = EmptyOperator(
        task_id="end",
        trigger_rule=TriggerRule.ALL_DONE,
    )

    # Define task dependencies
    (
        start
        >> check_data
        >> ingest
        >> validate
        >> preprocess
        >> detect_drift
        >> retrain_decision
        >> cleanup
        >> end
    )
