# MLOps Pipeline Template

A comprehensive, production-ready MLOps pipeline using PyTorch, Weights & Biases, Apache Airflow, and Docker.

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              MLOps Pipeline                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌────────────┐ │
│  │    Data      │───▶│ Preprocessing│───▶│   Training   │───▶│  Registry  │ │
│  │  Ingestion   │    │   & Feature  │    │     Job      │    │  & Deploy  │ │
│  │              │    │  Engineering │    │              │    │            │ │
│  └──────────────┘    └──────────────┘    └──────────────┘    └────────────┘ │
│         │                   │                   │                   │       │
│         ▼                   ▼                   ▼                   ▼       │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │                          Apache Airflow                                 ││
│  │                     (Orchestration & Scheduling)                        ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│         │                   │                   │                   │       │
│         ▼                   ▼                   ▼                   ▼       │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │                        Weights & Biases                                 ││
│  │              (Experiment Tracking & Model Registry)                     ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 📁 Project Structure

```
mlops/
├── airflow/                    # Apache Airflow configuration
│   ├── dags/                   # DAG definitions
│   │   ├── training_pipeline.py
│   │   └── data_pipeline.py
│   ├── plugins/                # Custom Airflow plugins
│   │   └── operators/
│   └── config/                 # Airflow configuration files
│       └── airflow.cfg
├── docker/                     # Docker configuration
│   ├── airflow/                # Airflow Dockerfile
│   ├── training/               # Training environment Dockerfile
│   └── docker-compose.yml      # Multi-service orchestration
├── src/                        # Source code
│   ├── data/                   # Data ingestion & processing
│   │   ├── ingestion.py
│   │   ├── preprocessing.py
│   │   ├── validation.py
│   │   └── versioning.py
│   ├── models/                 # Model definitions
│   │   ├── base.py
│   │   └── architectures.py
│   ├── training/               # Training logic
│   │   ├── trainer.py
│   │   ├── distributed.py
│   │   └── callbacks.py
│   ├── evaluation/             # Model evaluation
│   │   ├── metrics.py
│   │   └── evaluator.py
│   └── utils/                  # Utilities
│       ├── config.py
│       ├── logging.py
│       └── registry.py
├── config/                     # Configuration files
│   ├── training.yaml
│   ├── data.yaml
│   └── experiment.yaml
├── notebooks/                  # Jupyter notebooks
│   └── exploration.ipynb
├── tests/                      # Unit tests
│   ├── test_data.py
│   ├── test_models.py
│   └── test_training.py
├── scripts/                    # Utility scripts
│   ├── setup.sh
│   ├── train.sh
│   └── deploy.sh
├── requirements.txt            # Python dependencies
├── .env.example                # Environment variables template
└── README.md                   # Documentation
```

## 🚀 Quick Start

### Prerequisites

- Docker & Docker Compose
- Python 3.9+
- NVIDIA GPU with CUDA support (optional, for GPU training)

### 1. Clone and Setup

```bash
cd mlops
cp .env.example .env
# Edit .env with your configurations (W&B API key, etc.)
```

### 2. Start Services

```bash
# Start all services
docker-compose -f docker/docker-compose.yml up -d

# View Airflow UI at http://localhost:8080
# Default credentials: airflow / airflow
```

### 3. Run Training

```bash
# Local training
python -m src.training.trainer --config config/training.yaml

# Distributed training (multi-GPU)
torchrun --nproc_per_node=2 -m src.training.trainer --config config/training.yaml --distributed
```

## 📦 Components

### Data Pipeline

The data pipeline handles ingestion, validation, preprocessing, and versioning:

```python
from src.data.ingestion import DataIngester
from src.data.preprocessing import Preprocessor
from src.data.validation import DataValidator

# Initialize components
ingester = DataIngester(config_path="config/data.yaml")
validator = DataValidator()
preprocessor = Preprocessor()

# Run pipeline
data = ingester.ingest()
validator.validate(data)
processed_data = preprocessor.transform(data)
```

### Training Pipeline

PyTorch training with W&B integration:

```python
from src.training.trainer import Trainer
from src.models.architectures import create_model
from src.utils.config import load_config

config = load_config("config/training.yaml")
model = create_model(config.model)
trainer = Trainer(model, config)
trainer.train()
```

### Experiment Tracking

Automatic W&B integration for logging:

```python
import wandb

# Initialized automatically by trainer
# Logs: metrics, hyperparameters, system metrics, artifacts
```

### Model Registry

Version and promote models through stages:

```python
from src.utils.registry import ModelRegistry

registry = ModelRegistry()
registry.register_model(model, metrics, config)
registry.promote_model(model_name, from_stage="dev", to_stage="staging")
```

## ⚙️ Configuration

### Training Configuration (`config/training.yaml`)

```yaml
model:
  architecture: "resnet50"
  num_classes: 10
  pretrained: true

training:
  epochs: 100
  batch_size: 32
  learning_rate: 0.001
  optimizer: "adam"
  scheduler:
    type: "cosine"
    warmup_epochs: 5

distributed:
  enabled: false
  backend: "nccl"
  world_size: 2
```

### Data Configuration (`config/data.yaml`)

```yaml
ingestion:
  source_type: "local"
  path: "/data/raw"
  format: "csv"

preprocessing:
  normalize: true
  augmentation:
    enabled: true
    techniques:
      - "random_crop"
      - "horizontal_flip"

versioning:
  enabled: true
  strategy: "hash"
```

### Experiment Configuration (`config/experiment.yaml`)

```yaml
wandb:
  project: "mlops-pipeline"
  entity: "your-team"
  tags:
    - "production"
    - "v1.0"

logging:
  level: "INFO"
  format: "structured"

checkpointing:
  enabled: true
  frequency: 5
  keep_last: 3
```

## 🔄 Airflow DAGs

### Training Pipeline DAG

The training pipeline DAG orchestrates the entire ML workflow:

1. **data_ingestion** - Fetches data from configured sources
2. **data_validation** - Validates data quality
3. **preprocessing** - Feature engineering and transformation
4. **training** - Model training with W&B logging
5. **evaluation** - Model evaluation and metrics
6. **registration** - Register model to W&B registry

### Scheduled Retraining

Configure automatic retraining:

```python
# In training_pipeline.py
dag = DAG(
    'training_pipeline',
    schedule_interval='@weekly',  # Or cron expression
    default_args={
        'retries': 3,
        'retry_delay': timedelta(minutes=5),
    }
)
```

## 🐳 Docker Services

| Service | Port | Description |
|---------|------|-------------|
| airflow-webserver | 8080 | Airflow Web UI |
| airflow-scheduler | - | Airflow Scheduler |
| postgres | 5432 | Airflow metadata database |
| redis | 6379 | Celery broker |
| training | - | Training environment |

## 📊 W&B Integration

### Experiment Tracking

- Automatic metric logging
- Hyperparameter tracking
- System metrics (GPU, CPU, memory)
- Model artifact versioning

### Model Registry

- Model versioning
- Performance metrics storage
- Promotion workflow (dev → staging → production)

### Dashboard

View experiments at: `https://wandb.ai/<entity>/<project>`

## 🧪 Testing

```bash
# Run all tests
pytest tests/

# Run specific test module
pytest tests/test_training.py

# Run with coverage
pytest tests/ --cov=src --cov-report=html
```

## 📝 Logging

Structured logging throughout the pipeline:

```python
from src.utils.logging import get_logger

logger = get_logger(__name__)
logger.info("Training started", extra={"epoch": 1, "batch_size": 32})
```

## 🔒 Security

- Environment variables for sensitive data
- No secrets in code or configs
- Docker network isolation
- Volume permissions management

## 🔧 Troubleshooting

### Common Issues

1. **W&B Authentication Failed**
   ```bash
   export WANDB_API_KEY=your_api_key
   # Or add to .env file
   ```

2. **Docker Compose Fails**
   ```bash
   docker-compose down -v
   docker-compose up --build
   ```

3. **GPU Not Detected**
   ```bash
   # Ensure nvidia-docker is installed
   docker run --gpus all nvidia/cuda:11.0-base nvidia-smi
   ```

## 📚 Additional Resources

- [PyTorch Documentation](https://pytorch.org/docs/)
- [Weights & Biases Docs](https://docs.wandb.ai/)
- [Apache Airflow Docs](https://airflow.apache.org/docs/)
- [Docker Documentation](https://docs.docker.com/)

## 📄 License

MIT License - see LICENSE file for details.
