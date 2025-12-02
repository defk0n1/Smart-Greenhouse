#!/bin/bash
# Training script for local execution
# Supports single-GPU and multi-GPU (distributed) training

set -e

# Default values
CONFIG_PATH="${CONFIG_PATH:-config/training.yaml}"
EXPERIMENT_CONFIG="${EXPERIMENT_CONFIG:-config/experiment.yaml}"
DATA_CONFIG="${DATA_CONFIG:-config/data.yaml}"
DISTRIBUTED="${DISTRIBUTED:-false}"
NUM_GPUS="${NUM_GPUS:-1}"
RESUME="${RESUME:-}"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --config)
            CONFIG_PATH="$2"
            shift 2
            ;;
        --distributed)
            DISTRIBUTED=true
            shift
            ;;
        --num-gpus)
            NUM_GPUS="$2"
            shift 2
            ;;
        --resume)
            RESUME="$2"
            shift 2
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

echo "=== MLOps Training Script ==="
echo "Configuration: ${CONFIG_PATH}"
echo "Distributed: ${DISTRIBUTED}"
echo "Number of GPUs: ${NUM_GPUS}"

# Check for CUDA
if python -c "import torch; print(torch.cuda.is_available())" | grep -q "True"; then
    echo "CUDA is available"
    CUDA_AVAILABLE=true
else
    echo "CUDA not available, using CPU"
    CUDA_AVAILABLE=false
fi

# Set environment variables
export PYTHONPATH="${PYTHONPATH}:$(pwd)/src"

# Build training command
if [ "$DISTRIBUTED" = true ] && [ "$CUDA_AVAILABLE" = true ]; then
    echo "Running distributed training on ${NUM_GPUS} GPUs..."
    
    TRAINING_CMD="torchrun \
        --standalone \
        --nproc_per_node=${NUM_GPUS} \
        -m src.training.trainer \
        --config ${CONFIG_PATH} \
        --distributed"
else
    echo "Running single-GPU/CPU training..."
    
    TRAINING_CMD="python -m src.training.trainer \
        --config ${CONFIG_PATH}"
fi

# Add resume flag if provided
if [ -n "$RESUME" ]; then
    TRAINING_CMD="${TRAINING_CMD} --resume ${RESUME}"
fi

# Run training
echo "Starting training..."
eval $TRAINING_CMD

echo "=== Training Complete ==="
