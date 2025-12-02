"""
Tests for Models Module

This module contains unit tests for model architectures
and model-related utilities.
"""

import pytest
import torch
import torch.nn as nn
from omegaconf import OmegaConf


@pytest.fixture
def model_config():
    """Create model configuration for testing."""
    return OmegaConf.create({
        "architecture": "resnet18",
        "num_classes": 10,
        "pretrained": False,  # Disable pretrained for faster tests
        "dropout_rate": 0.2,
    })


@pytest.fixture
def custom_model_config():
    """Create custom MLP configuration for testing."""
    return OmegaConf.create({
        "architecture": "custom",
        "num_classes": 10,
        "input_dim": 784,
        "dropout_rate": 0.2,
        "custom": {
            "hidden_dims": [256, 128],
            "activation": "relu",
            "batch_norm": True,
        }
    })


@pytest.fixture
def sample_image_batch():
    """Create sample image batch for testing."""
    return torch.randn(4, 3, 224, 224)


@pytest.fixture
def sample_flat_batch():
    """Create sample flat batch for testing."""
    return torch.randn(4, 784)


class TestBaseModel:
    """Tests for BaseModel class."""

    def test_base_model_attributes(self, model_config):
        """Test base model has expected attributes."""
        from src.models.architectures import create_model

        model = create_model(model_config)

        assert hasattr(model, "config")
        assert hasattr(model, "num_parameters")
        assert hasattr(model, "num_trainable_parameters")

    def test_model_parameter_count(self, model_config):
        """Test parameter counting."""
        from src.models.architectures import create_model

        model = create_model(model_config)

        assert model.num_parameters > 0
        assert model.num_trainable_parameters > 0
        assert model.num_trainable_parameters <= model.num_parameters

    def test_model_freeze(self, model_config):
        """Test model freezing."""
        from src.models.architectures import create_model

        model = create_model(model_config)
        model.freeze()

        # All parameters should be frozen
        for param in model.parameters():
            assert not param.requires_grad

    def test_model_unfreeze(self, model_config):
        """Test model unfreezing."""
        from src.models.architectures import create_model

        model = create_model(model_config)
        model.freeze()
        model.unfreeze()

        # All parameters should be unfrozen
        for param in model.parameters():
            assert param.requires_grad


class TestResNetClassifier:
    """Tests for ResNet classifier."""

    def test_resnet18_forward(self, model_config, sample_image_batch):
        """Test ResNet18 forward pass."""
        from src.models.architectures import create_model

        model = create_model(model_config)
        output = model(sample_image_batch)

        assert output.shape == (4, 10)  # batch_size, num_classes

    def test_resnet50_forward(self, sample_image_batch):
        """Test ResNet50 forward pass."""
        from src.models.architectures import create_model

        config = OmegaConf.create({
            "architecture": "resnet50",
            "num_classes": 100,
            "pretrained": False,
        })

        model = create_model(config)
        output = model(sample_image_batch)

        assert output.shape == (4, 100)

    def test_resnet_freeze_backbone(self, sample_image_batch):
        """Test ResNet with frozen backbone."""
        from src.models.architectures import ResNetClassifier

        config = OmegaConf.create({
            "architecture": "resnet18",
            "num_classes": 10,
            "pretrained": False,
            "freeze_backbone": True,
        })

        model = ResNetClassifier(
            config=config,
            num_classes=10,
            variant="resnet18",
            pretrained=False,
            freeze_backbone=True
        )

        # Backbone should be frozen
        for param in model.backbone.parameters():
            assert not param.requires_grad

        # Classifier should not be frozen
        for param in model.classifier.parameters():
            assert param.requires_grad


class TestCustomMLP:
    """Tests for Custom MLP."""

    def test_custom_mlp_forward(self, custom_model_config, sample_flat_batch):
        """Test Custom MLP forward pass."""
        from src.models.architectures import create_model

        model = create_model(custom_model_config)
        output = model(sample_flat_batch)

        assert output.shape == (4, 10)

    def test_custom_mlp_different_activation(self, sample_flat_batch):
        """Test Custom MLP with different activation."""
        from src.models.architectures import CustomMLP

        config = OmegaConf.create({"architecture": "custom"})

        for activation in ["relu", "gelu", "silu", "leaky_relu"]:
            model = CustomMLP(
                config=config,
                input_dim=784,
                num_classes=10,
                hidden_dims=[128, 64],
                activation=activation,
            )
            output = model(sample_flat_batch)
            assert output.shape == (4, 10)

    def test_custom_mlp_no_batch_norm(self, sample_flat_batch):
        """Test Custom MLP without batch normalization."""
        from src.models.architectures import CustomMLP

        config = OmegaConf.create({"architecture": "custom"})

        model = CustomMLP(
            config=config,
            input_dim=784,
            num_classes=10,
            hidden_dims=[128, 64],
            batch_norm=False,
        )
        output = model(sample_flat_batch)
        assert output.shape == (4, 10)


class TestModelFactory:
    """Tests for model factory function."""

    def test_create_model_resnet_variants(self, sample_image_batch):
        """Test creating different ResNet variants."""
        from src.models.architectures import create_model

        variants = ["resnet18", "resnet34"]  # Skip larger models for speed

        for variant in variants:
            config = OmegaConf.create({
                "architecture": variant,
                "num_classes": 10,
                "pretrained": False,
            })

            model = create_model(config)
            output = model(sample_image_batch)

            assert output.shape == (4, 10), f"Failed for {variant}"

    def test_create_model_unknown_architecture(self):
        """Test creating model with unknown architecture raises error."""
        from src.models.architectures import create_model

        config = OmegaConf.create({
            "architecture": "unknown_model",
            "num_classes": 10,
        })

        with pytest.raises(ValueError):
            create_model(config)

    def test_get_available_architectures(self):
        """Test getting available architectures."""
        from src.models.architectures import get_available_architectures

        architectures = get_available_architectures()

        assert "resnet18" in architectures
        assert "resnet50" in architectures
        assert "custom" in architectures


class TestModelSaveLoad:
    """Tests for model save/load functionality."""

    def test_model_save_and_load(self, model_config, tmp_path):
        """Test saving and loading model."""
        from src.models.architectures import create_model

        model = create_model(model_config)

        # Save model
        save_path = tmp_path / "model.pt"
        model.save(str(save_path))

        # Create new model and load
        new_model = create_model(model_config)
        new_model.load(str(save_path))

        # Check weights are the same
        for p1, p2 in zip(model.parameters(), new_model.parameters()):
            assert torch.allclose(p1, p2)

    def test_model_save_with_optimizer(self, model_config, tmp_path):
        """Test saving model with optimizer state."""
        from src.models.architectures import create_model

        model = create_model(model_config)
        optimizer = torch.optim.Adam(model.parameters(), lr=0.001)

        # Do some optimization steps
        dummy_input = torch.randn(2, 3, 224, 224)
        dummy_target = torch.randint(0, 10, (2,))

        output = model(dummy_input)
        loss = nn.CrossEntropyLoss()(output, dummy_target)
        loss.backward()
        optimizer.step()

        # Save
        save_path = tmp_path / "checkpoint.pt"
        model.save(str(save_path), include_optimizer=True, optimizer=optimizer)

        # Load
        new_model = create_model(model_config)
        new_optimizer = torch.optim.Adam(new_model.parameters(), lr=0.001)
        new_model.load(str(save_path), optimizer=new_optimizer)

        # Check optimizer state
        assert len(new_optimizer.state) > 0


class TestWeightInitialization:
    """Tests for weight initialization."""

    def test_kaiming_init(self):
        """Test Kaiming weight initialization."""
        from src.models.base import init_weights

        linear = nn.Linear(100, 50)
        init_weights(linear, method="kaiming")

        # Weights should be initialized (not zeros)
        assert linear.weight.abs().mean() > 0
        assert linear.bias.abs().mean() < 0.01  # Bias should be near zero

    def test_xavier_init(self):
        """Test Xavier weight initialization."""
        from src.models.base import init_weights

        linear = nn.Linear(100, 50)
        init_weights(linear, method="xavier")

        assert linear.weight.abs().mean() > 0


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
