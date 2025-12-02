# Airflow Dockerfile
# Based on Apache Airflow official image with additional ML dependencies

FROM apache/airflow:2.7.3-python3.10

USER root

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    git \
    curl \
    vim \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

USER airflow

# Install Python dependencies
COPY requirements.txt /tmp/requirements.txt
RUN pip install --no-cache-dir -r /tmp/requirements.txt

# Set environment variables
ENV PYTHONPATH="${PYTHONPATH}:/opt/airflow/src"
ENV AIRFLOW_HOME=/opt/airflow

# Create directories
RUN mkdir -p ${AIRFLOW_HOME}/dags ${AIRFLOW_HOME}/logs ${AIRFLOW_HOME}/plugins

WORKDIR ${AIRFLOW_HOME}
