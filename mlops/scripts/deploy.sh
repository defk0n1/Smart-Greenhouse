#!/bin/bash
# Model deployment script
# Promotes models through stages and prepares for deployment

set -e

# Default values
MODEL_NAME="${MODEL_NAME:-mlops-model}"
VERSION="${VERSION:-latest}"
FROM_STAGE="${FROM_STAGE:-dev}"
TO_STAGE="${TO_STAGE:-staging}"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --model)
            MODEL_NAME="$2"
            shift 2
            ;;
        --version)
            VERSION="$2"
            shift 2
            ;;
        --from)
            FROM_STAGE="$2"
            shift 2
            ;;
        --to)
            TO_STAGE="$2"
            shift 2
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

echo "=== Model Deployment Script ==="
echo "Model: ${MODEL_NAME}"
echo "Version: ${VERSION}"
echo "Promoting: ${FROM_STAGE} -> ${TO_STAGE}"

# Set Python path
export PYTHONPATH="${PYTHONPATH}:$(pwd)/src"

# Run promotion script
python << EOF
from src.utils.registry import ModelRegistry, ModelStage

registry = ModelRegistry(registry_type="wandb")

# Get model
print(f"Fetching model '{MODEL_NAME}' version '{VERSION}'...")
state_dict, metadata = registry.get_model("${MODEL_NAME}", "${VERSION}")

print(f"Current stage: {metadata.stage}")
print(f"Metrics: {metadata.metrics}")

# Promote
to_stage = ModelStage.${TO_STAGE^^}
success = registry.promote_model(
    name="${MODEL_NAME}",
    version="${VERSION}",
    to_stage=to_stage
)

if success:
    print(f"Successfully promoted to {to_stage.value}")
else:
    print("Promotion failed")
    exit(1)
EOF

echo ""
echo "=== Deployment Complete ==="
echo ""
echo "Model ${MODEL_NAME}:${VERSION} is now in ${TO_STAGE} stage."
echo ""
echo "Next steps:"
if [ "$TO_STAGE" = "staging" ]; then
    echo "  1. Run integration tests"
    echo "  2. Validate performance metrics"
    echo "  3. Promote to production with: ./deploy.sh --to production"
elif [ "$TO_STAGE" = "production" ]; then
    echo "  1. Model is now available for production inference"
    echo "  2. Monitor model performance in W&B dashboard"
fi
