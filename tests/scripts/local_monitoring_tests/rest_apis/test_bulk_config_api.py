"""
Copyright (c) 2026 Red Hat, IBM Corporation and others.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""
import logging
import pytest
import sys
import json
import copy

sys.path.append("../../")
from helpers.fixtures import *
from helpers.kruize import *
from helpers.utils import *

# Set up logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# Helper function to create a valid bulk config payload
def create_valid_bulk_config(config_name="test-bulk-config"):
    return {
        "config_name": config_name,
        "cluster_name": "test-cluster",
        "datasources": ["prometheus-1"],
        "namespaces": ["default", "monitoring"],
        "labels": {
            "app": "test",
            "env": "dev"
        },
        "experiment_types": ["container"],
        "metadata_profile": "cluster-metadata-local-monitoring",
        "performance_profile": "resource-optimization-local-monitoring",
        "trial_settings": {
            "measurement_duration": "15min"
        },
        "recommendation_settings": {
            "scheduling": "24h",
            "terms": ["short", "medium"],
            "models": ["performance"]
        },
        "webhook_url": "http://example.com/webhook",
        "enabled": True
    }


@pytest.mark.sanity
def test_create_bulk_config(cluster_type):
    """
    Test Description: Validate creating a bulk config with valid input
    """
    form_kruize_url(cluster_type)
    
    # Setup: Ensure metric and metadata profiles exist
    delete_and_create_metric_profile()
    delete_and_create_metadata_profile()
    
    config_name = "test-bulk-config-create"
    bulk_config = create_valid_bulk_config(config_name)
    
    # Cleanup: Delete if exists
    delete_bulk_config(config_name, logging=False)
    
    # Test: Create bulk config
    response = create_bulk_config(bulk_config, logging=True)
    
    assert response.status_code == SUCCESS_STATUS_CODE
    data = response.json()
    assert data.get("config_name") == config_name
    
    # Verify it was created by listing
    list_response = list_bulk_configs(config_name=config_name, logging=False)
    assert list_response.status_code == SUCCESS_200_STATUS_CODE
    
    # Cleanup
    delete_bulk_config(config_name, logging=False)


@pytest.mark.sanity
def test_create_duplicate_bulk_config(cluster_type):
    """
    Test Description: Validate error when creating duplicate bulk config
    """
    form_kruize_url(cluster_type)
    
    delete_and_create_metric_profile()
    delete_and_create_metadata_profile()
    
    config_name = "test-bulk-config-duplicate"
    bulk_config = create_valid_bulk_config(config_name)
    
    # Cleanup
    delete_bulk_config(config_name, logging=False)
    
    # Create first time
    response1 = create_bulk_config(bulk_config, logging=False)
    assert response1.status_code == SUCCESS_STATUS_CODE
    
    # Try to create again - should fail
    response2 = create_bulk_config(bulk_config, logging=False)
    assert response2.status_code == ERROR_409_STATUS_CODE
    assert "already exists" in response2.json().get("error", "").lower()
    
    # Cleanup
    delete_bulk_config(config_name, logging=False)


@pytest.mark.sanity
def test_list_all_bulk_configs(cluster_type):
    """
    Test Description: Validate listing all bulk configs
    """
    form_kruize_url(cluster_type)
    
    delete_and_create_metric_profile()
    delete_and_create_metadata_profile()
    
    # Create multiple configs
    config1 = create_valid_bulk_config("test-list-config-1")
    config2 = create_valid_bulk_config("test-list-config-2")
    
    delete_bulk_config("test-list-config-1", logging=False)
    delete_bulk_config("test-list-config-2", logging=False)
    
    create_bulk_config(config1, logging=False)
    create_bulk_config(config2, logging=False)
    
    # List all
    response = list_bulk_configs(logging=True)
    assert response.status_code == SUCCESS_200_STATUS_CODE
    
    configs = response.json()
    assert isinstance(configs, list)
    assert len(configs) >= 2
    
    # Cleanup
    delete_bulk_config("test-list-config-1", logging=False)
    delete_bulk_config("test-list-config-2", logging=False)


@pytest.mark.sanity
def test_list_bulk_config_by_name(cluster_type):
    """
    Test Description: Validate listing a specific bulk config by name
    """
    form_kruize_url(cluster_type)
    
    delete_and_create_metric_profile()
    delete_and_create_metadata_profile()
    
    config_name = "test-list-by-name"
    bulk_config = create_valid_bulk_config(config_name)
    
    delete_bulk_config(config_name, logging=False)
    create_bulk_config(bulk_config, logging=False)
    
    # List by name
    response = list_bulk_configs(config_name=config_name, logging=True)
    assert response.status_code == SUCCESS_200_STATUS_CODE
    
    data = response.json()
    assert data.get("config_name") == config_name
    assert data.get("cluster_name") == "test-cluster"
    
    # Cleanup
    delete_bulk_config(config_name, logging=False)


@pytest.mark.sanity
def test_update_bulk_config(cluster_type):
    """
    Test Description: Validate updating a bulk config
    """
    form_kruize_url(cluster_type)
    
    delete_and_create_metric_profile()
    delete_and_create_metadata_profile()
    
    config_name = "test-update-config"
    bulk_config = create_valid_bulk_config(config_name)
    
    delete_bulk_config(config_name, logging=False)
    create_bulk_config(bulk_config, logging=False)
    
    # Update config
    update_data = {
        "namespaces": ["default", "monitoring", "kube-system"],
        "enabled": False
    }
    
    response = update_bulk_config(config_name, update_data, logging=True)
    assert response.status_code == SUCCESS_200_STATUS_CODE
    
    # Verify update
    list_response = list_bulk_configs(config_name=config_name, logging=False)
    updated_config = list_response.json()
    assert len(updated_config.get("namespaces", [])) == 3
    assert updated_config.get("enabled") == False
    
    # Cleanup
    delete_bulk_config(config_name, logging=False)


@pytest.mark.sanity
def test_delete_bulk_config(cluster_type):
    """
    Test Description: Validate deleting a bulk config
    """
    form_kruize_url(cluster_type)
    
    delete_and_create_metric_profile()
    delete_and_create_metadata_profile()
    
    config_name = "test-delete-config"
    bulk_config = create_valid_bulk_config(config_name)
    
    delete_bulk_config(config_name, logging=False)
    create_bulk_config(bulk_config, logging=False)
    
    # Delete config
    response = delete_bulk_config(config_name, logging=True)
    assert response.status_code == SUCCESS_200_STATUS_CODE
    assert "deleted successfully" in response.json().get("message", "").lower()
    
    # Verify deletion
    list_response = list_bulk_configs(config_name=config_name, logging=False)
    assert list_response.status_code == ERROR_404_STATUS_CODE


@pytest.mark.negative
def test_create_bulk_config_missing_config_name(cluster_type):
    """
    Test Description: Validate error when config_name is missing
    """
    form_kruize_url(cluster_type)
    
    delete_and_create_metric_profile()
    delete_and_create_metadata_profile()
    
    bulk_config = create_valid_bulk_config()
    del bulk_config["config_name"]
    
    response = create_bulk_config(bulk_config, logging=False)
    assert response.status_code == ERROR_STATUS_CODE
    assert "config_name" in response.json().get("error", "").lower()


@pytest.mark.negative
def test_create_bulk_config_missing_cluster_name(cluster_type):
    """
    Test Description: Validate error when cluster_name is missing
    """
    form_kruize_url(cluster_type)
    
    delete_and_create_metric_profile()
    delete_and_create_metadata_profile()
    
    bulk_config = create_valid_bulk_config("test-missing-cluster")
    del bulk_config["cluster_name"]
    
    response = create_bulk_config(bulk_config, logging=False)
    assert response.status_code == ERROR_STATUS_CODE
    assert "cluster_name" in response.json().get("error", "").lower()
    
    # Cleanup
    delete_bulk_config("test-missing-cluster", logging=False)


@pytest.mark.negative
def test_create_bulk_config_missing_datasources(cluster_type):
    """
    Test Description: Validate error when datasources are missing
    """
    form_kruize_url(cluster_type)
    
    delete_and_create_metric_profile()
    delete_and_create_metadata_profile()
    
    bulk_config = create_valid_bulk_config("test-missing-datasources")
    del bulk_config["datasources"]
    
    response = create_bulk_config(bulk_config, logging=False)
    assert response.status_code == ERROR_STATUS_CODE
    assert "datasource" in response.json().get("error", "").lower()
    
    # Cleanup
    delete_bulk_config("test-missing-datasources", logging=False)


@pytest.mark.negative
def test_create_bulk_config_invalid_datasource(cluster_type):
    """
    Test Description: Validate error when datasource doesn't exist
    """
    form_kruize_url(cluster_type)
    
    delete_and_create_metric_profile()
    delete_and_create_metadata_profile()
    
    bulk_config = create_valid_bulk_config("test-invalid-datasource")
    bulk_config["datasources"] = ["non-existent-datasource"]
    
    response = create_bulk_config(bulk_config, logging=False)
    assert response.status_code == ERROR_STATUS_CODE
    
    # Cleanup
    delete_bulk_config("test-invalid-datasource", logging=False)


@pytest.mark.negative
def test_create_bulk_config_missing_metadata_profile(cluster_type):
    """
    Test Description: Validate error when metadata_profile is missing
    """
    form_kruize_url(cluster_type)
    
    delete_and_create_metric_profile()
    delete_and_create_metadata_profile()
    
    bulk_config = create_valid_bulk_config("test-missing-metadata")
    del bulk_config["metadata_profile"]
    
    response = create_bulk_config(bulk_config, logging=False)
    assert response.status_code == ERROR_STATUS_CODE
    assert "metadata_profile" in response.json().get("error", "").lower()
    
    # Cleanup
    delete_bulk_config("test-missing-metadata", logging=False)


@pytest.mark.negative
def test_create_bulk_config_invalid_metadata_profile(cluster_type):
    """
    Test Description: Validate error when metadata_profile doesn't exist
    """
    form_kruize_url(cluster_type)
    
    delete_and_create_metric_profile()
    delete_and_create_metadata_profile()
    
    bulk_config = create_valid_bulk_config("test-invalid-metadata")
    bulk_config["metadata_profile"] = "non-existent-metadata-profile"
    
    response = create_bulk_config(bulk_config, logging=False)
    assert response.status_code == ERROR_STATUS_CODE
    assert "does not exist" in response.json().get("error", "").lower()
    
    # Cleanup
    delete_bulk_config("test-invalid-metadata", logging=False)


@pytest.mark.negative
def test_create_bulk_config_invalid_performance_profile(cluster_type):
    """
    Test Description: Validate error when performance_profile doesn't exist
    """
    form_kruize_url(cluster_type)
    
    delete_and_create_metric_profile()
    delete_and_create_metadata_profile()
    
    bulk_config = create_valid_bulk_config("test-invalid-performance")
    bulk_config["performance_profile"] = "non-existent-performance-profile"
    
    response = create_bulk_config(bulk_config, logging=False)
    assert response.status_code == ERROR_STATUS_CODE
    assert "does not exist" in response.json().get("error", "").lower()
    
    # Cleanup
    delete_bulk_config("test-invalid-performance", logging=False)


@pytest.mark.negative
def test_create_bulk_config_invalid_config_name_format(cluster_type):
    """
    Test Description: Validate error when config_name has invalid characters
    """
    form_kruize_url(cluster_type)
    
    delete_and_create_metric_profile()
    delete_and_create_metadata_profile()
    
    bulk_config = create_valid_bulk_config("test config with spaces!")
    
    response = create_bulk_config(bulk_config, logging=False)
    assert response.status_code == ERROR_STATUS_CODE
    assert "alphanumeric" in response.json().get("error", "").lower()


@pytest.mark.negative
def test_update_non_existent_bulk_config(cluster_type):
    """
    Test Description: Validate error when updating non-existent config
    """
    form_kruize_url(cluster_type)
    
    update_data = {"enabled": False}
    
    response = update_bulk_config("non-existent-config", update_data, logging=False)
    assert response.status_code == ERROR_404_STATUS_CODE
    assert "not found" in response.json().get("error", "").lower()


@pytest.mark.negative
def test_delete_non_existent_bulk_config(cluster_type):
    """
    Test Description: Validate error when deleting non-existent config
    """
    form_kruize_url(cluster_type)
    
    response = delete_bulk_config("non-existent-config", logging=False)
    assert response.status_code == ERROR_404_STATUS_CODE
    assert "not found" in response.json().get("error", "").lower()


@pytest.mark.negative
def test_list_non_existent_bulk_config(cluster_type):
    """
    Test Description: Validate error when listing non-existent config
    """
    form_kruize_url(cluster_type)
    
    response = list_bulk_configs(config_name="non-existent-config", logging=False)
    assert response.status_code == ERROR_404_STATUS_CODE


@pytest.mark.extended
def test_create_bulk_config_with_minimal_fields(cluster_type):
    """
    Test Description: Validate creating bulk config with only required fields
    """
    form_kruize_url(cluster_type)
    
    delete_and_create_metric_profile()
    delete_and_create_metadata_profile()
    
    config_name = "test-minimal-config"
    minimal_config = {
        "config_name": config_name,
        "cluster_name": "test-cluster",
        "datasources": ["prometheus-1"],
        "experiment_types": ["container"],
        "metadata_profile": "cluster-metadata-local-monitoring",
        "recommendation_settings": {
            "scheduling": "24h",
            "terms": ["short"],
            "models": ["performance"]
        }
    }
    
    delete_bulk_config(config_name, logging=False)
    
    response = create_bulk_config(minimal_config, logging=True)
    assert response.status_code == SUCCESS_STATUS_CODE
    
    # Cleanup
    delete_bulk_config(config_name, logging=False)


@pytest.mark.extended
def test_update_bulk_config_partial_update(cluster_type):
    """
    Test Description: Validate partial update of bulk config
    """
    form_kruize_url(cluster_type)
    
    delete_and_create_metric_profile()
    delete_and_create_metadata_profile()
    
    config_name = "test-partial-update"
    bulk_config = create_valid_bulk_config(config_name)
    
    delete_bulk_config(config_name, logging=False)
    create_bulk_config(bulk_config, logging=False)
    
    # Update only enabled field
    update_data = {"enabled": False}
    
    response = update_bulk_config(config_name, update_data, logging=True)
    assert response.status_code == SUCCESS_200_STATUS_CODE
    
    # Verify other fields remain unchanged
    list_response = list_bulk_configs(config_name=config_name, logging=False)
    updated_config = list_response.json()
    assert updated_config.get("enabled") == False
    assert updated_config.get("cluster_name") == "test-cluster"  # Should remain unchanged
    
    # Cleanup
    delete_bulk_config(config_name, logging=False)

