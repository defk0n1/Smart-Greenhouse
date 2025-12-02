#!/bin/bash
# Setup script for MLOps Pipeline
# This script initializes the environment and dependencies

set -e

echo "=== MLOps Pipeline Setup ==="

# Check for required tools
command -v docker >/dev/null 2>&1 || { echo "Docker is required but not installed. Aborting." >&2; exit 1; }
command -v docker-compose >/dev/null 2>&1 || { echo "Docker Compose is required but not installed. Aborting." >&2; exit 1; }

# Create environment file if not exists
if [ ! -f .env ]; then
    echo "Creating .env file from template..."
    cp .env.example .env
    echo "Please edit .env file with your configurations."
fi

# Create required directories
echo "Creating required directories..."
mkdir -p data/raw data/processed data/versions data/cache
mkdir -p models checkpoints logs
mkdir -p airflow/logs

# Set permissions for Airflow
echo "Setting permissions..."
AIRFLOW_UID=$(id -u)
export AIRFLOW_UID
echo "AIRFLOW_UID=${AIRFLOW_UID}" >> .env

# Build Docker images
echo "Building Docker images..."
cd docker
docker-compose build

echo "=== Setup Complete ==="
echo ""
echo "To start the services, run:"
echo "  cd docker && docker-compose up -d"
echo ""
echo "Access Airflow at: http://localhost:8080"
echo "Default credentials: airflow / airflow"
echo ""
echo "To run training locally:"
echo "  python -m src.training.trainer --config config/training.yaml"
