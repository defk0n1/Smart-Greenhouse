# Training Environment Dockerfile
# Optimized for PyTorch training with GPU support

# Use NVIDIA CUDA base image for GPU support
FROM nvidia/cuda:11.8-cudnn8-runtime-ubuntu22.04

# Set environment variables
ENV DEBIAN_FRONTEND=noninteractive
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1
ENV PIP_NO_CACHE_DIR=1
ENV PIP_DISABLE_PIP_VERSION_CHECK=1

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    python3.10 \
    python3.10-dev \
    python3.10-venv \
    python3-pip \
    build-essential \
    git \
    curl \
    vim \
    libgl1-mesa-glx \
    libglib2.0-0 \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Create symlinks for Python
RUN ln -sf /usr/bin/python3.10 /usr/bin/python && \
    ln -sf /usr/bin/python3.10 /usr/bin/python3

# Upgrade pip
RUN python -m pip install --upgrade pip setuptools wheel

# Create app directory
WORKDIR /app

# Install Python dependencies
COPY requirements.txt /tmp/requirements.txt
RUN pip install --no-cache-dir -r /tmp/requirements.txt

# Set environment variables
ENV PYTHONPATH="${PYTHONPATH}:/app/src"
ENV DATA_DIR=/data
ENV MODEL_DIR=/models
ENV LOG_DIR=/logs
ENV CHECKPOINT_DIR=/checkpoints

# Create necessary directories
RUN mkdir -p /data /models /logs /checkpoints

# Create non-root user for security
RUN useradd -m -u 1000 mlops
RUN chown -R mlops:mlops /app /data /models /logs /checkpoints
USER mlops

# Default command
CMD ["python", "-m", "src.training.trainer", "--config", "/app/config/training.yaml"]
