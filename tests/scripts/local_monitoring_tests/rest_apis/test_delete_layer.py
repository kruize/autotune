"""
Copyright (c) 2026 IBM Corporation and others.

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

import pytest
import json
import sys
import os
import copy
import requests

sys.path.append("../../")

from helpers.fixtures import *
from helpers.kruize import *
from helpers.utils import *

layer_dir = get_layer_dir()


# =============================================================================
# A. POSITIVE SCENARIOS - Delete Layer Successfully (4 tests)
# Note: We don't test sequential deletion of multiple layers separately because
# test_delete_layer_verify_removal already covers deletion behavior with multiple
# layers (verifying one is removed while others remain).
# =============================================================================

@pytest.mark.layers
@pytest.mark.sanity
def test_delete_layer_success(cluster_type):
    """
    Test Description: This test validates deleteLayer API successfully deletes an existing layer
    """
    form_kruize_url(cluster_type)

    input_json_file = layer_dir / "container-config.json"
    with open(input_json_file, "r") as f:
        layer_data = json.load(f)

    layer_name = "test-delete-layer"
    create_data = copy.deepcopy(layer_data)
    create_data['layer_name'] = layer_name
    create_data['metadata']['name'] = layer_name

    # Setup: Create layer first
    tmp_create = f"/tmp/create_{layer_name}.json"
    with open(tmp_create, "w") as f:
        json.dump(create_data, f)

    # Cleanup: Delete layer if it exists from previous run
    delete_layer(layer_name)

    create_response = create_layer(tmp_create)
    assert create_response.status_code == SUCCESS_STATUS_CODE

    # Verify layer exists before deletion
    list_response = list_layers(layer_name=layer_name)
    assert list_response.status_code == SUCCESS_200_STATUS_CODE

    # TEST: Delete the layer
    response = delete_layer(layer_name)
    data = response.json()

    # Verify deletion success
    assert response.status_code == SUCCESS_200_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == DELETE_LAYER_SUCCESS_MSG % layer_name

    # Verify layer is actually gone
    list_response = list_layers(layer_name=layer_name)
    assert list_response.status_code == ERROR_404_STATUS_CODE

    print(f"✓ Successfully deleted layer: {layer_name}")

    # Cleanup temp file (layer is already deleted)
    os.remove(tmp_create)


@pytest.mark.layers
@pytest.mark.sanity
def test_delete_layer_verify_removal(cluster_type):
    """
    Test Description: This test validates that deleted layer is removed from list of all layers
    """
    form_kruize_url(cluster_type)

    input_json_file = layer_dir / "container-config.json"
    with open(input_json_file, "r") as f:
        layer_data = json.load(f)

    # Create multiple layers
    layer_names = ["test-delete-verify-1", "test-delete-verify-2", "test-delete-verify-3"]
    tmp_files = []

    for layer_name in layer_names:
        create_data = copy.deepcopy(layer_data)
        create_data['layer_name'] = layer_name
        create_data['metadata']['name'] = layer_name

        tmp_file = f"/tmp/create_{layer_name}.json"
        tmp_files.append(tmp_file)

        with open(tmp_file, "w") as f:
            json.dump(create_data, f)

        # Cleanup: Delete layer if it exists from previous run
        delete_layer(layer_name)

        create_response = create_layer(tmp_file)
        assert create_response.status_code == SUCCESS_STATUS_CODE

    # List all layers before deletion
    list_response = list_layers()
    assert list_response.status_code == SUCCESS_200_STATUS_CODE
    all_layers_before = list_response.json()

    # Verify all our test layers exist in the list
    layer_names_before = [layer['layer_name'] for layer in all_layers_before]
    for layer_name in layer_names:
        assert layer_name in layer_names_before

    # Delete the first layer
    deleted_layer = layer_names[0]
    response = delete_layer(deleted_layer)
    data = response.json()

    assert response.status_code == SUCCESS_200_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == DELETE_LAYER_SUCCESS_MSG % deleted_layer

    # List all layers after deletion
    list_response = list_layers()
    assert list_response.status_code == SUCCESS_200_STATUS_CODE
    all_layers_after = list_response.json()

    # Verify deleted layer is NOT in the list
    layer_names_after = [layer['layer_name'] for layer in all_layers_after]
    assert deleted_layer not in layer_names_after

    # Verify other layers still exist
    assert layer_names[1] in layer_names_after
    assert layer_names[2] in layer_names_after

    print(f"✓ Successfully verified {deleted_layer} removed from list")

    # Cleanup remaining layers
    delete_layer(layer_names[1])
    delete_layer(layer_names[2])

    # Cleanup temp files
    for tmp_file in tmp_files:
        os.remove(tmp_file)


@pytest.mark.layers
@pytest.mark.sanity
def test_delete_layer_recreate(cluster_type):
    """
    Test Description: This test validates deleting a layer and recreating it with the same name
    """
    form_kruize_url(cluster_type)

    input_json_file = layer_dir / "container-config.json"
    with open(input_json_file, "r") as f:
        layer_data = json.load(f)

    layer_name = "test-delete-recreate"
    create_data = copy.deepcopy(layer_data)
    create_data['layer_name'] = layer_name
    create_data['metadata']['name'] = layer_name

    tmp_create = f"/tmp/create_{layer_name}.json"
    with open(tmp_create, "w") as f:
        json.dump(create_data, f)

    # Cleanup: Delete layer if it exists from previous run
    delete_layer(layer_name)

    # First creation
    create_response = create_layer(tmp_create)
    assert create_response.status_code == SUCCESS_STATUS_CODE
    print(f"✓ Created layer first time: {layer_name}")

    # Verify it exists
    list_response = list_layers(layer_name=layer_name)
    assert list_response.status_code == SUCCESS_200_STATUS_CODE

    # Delete the layer
    response = delete_layer(layer_name)
    data = response.json()

    assert response.status_code == SUCCESS_200_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == DELETE_LAYER_SUCCESS_MSG % layer_name
    print(f"✓ Deleted layer: {layer_name}")

    # Verify it's gone
    list_response = list_layers(layer_name=layer_name)
    assert list_response.status_code == ERROR_404_STATUS_CODE

    # Recreate with the same name (should succeed - no conflict)
    create_response = create_layer(tmp_create)
    data = create_response.json()

    assert create_response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_LAYER_SUCCESS_MSG % layer_name
    print(f"✓ Recreated layer with same name: {layer_name}")

    # Verify recreated layer exists
    list_response = list_layers(layer_name=layer_name)
    assert list_response.status_code == SUCCESS_200_STATUS_CODE

    print(f"✓ Successfully deleted and recreated layer: {layer_name}")

    # Cleanup: Delete the recreated layer
    delete_layer(layer_name)
    os.remove(tmp_create)


@pytest.mark.layers
@pytest.mark.sanity
def test_delete_layer_all_types(cluster_type):
    """
    Test Description: This test validates deleting layers with different presence types
    (presence='always', queries, label)
    """
    form_kruize_url(cluster_type)

    # Layer 1: presence='always'
    input_json_file1 = layer_dir / "container-config.json"
    with open(input_json_file1, "r") as f:
        layer_data1 = json.load(f)

    layer_name1 = "test-delete-presence-always"
    create_data1 = copy.deepcopy(layer_data1)
    create_data1['layer_name'] = layer_name1
    create_data1['metadata']['name'] = layer_name1

    tmp_file1 = f"/tmp/create_{layer_name1}.json"
    with open(tmp_file1, "w") as f:
        json.dump(create_data1, f)

    # Cleanup: Delete layer if it exists from previous run
    delete_layer(layer_name1)

    create_response1 = create_layer(tmp_file1)
    assert create_response1.status_code == SUCCESS_STATUS_CODE

    # Layer 2: queries-based presence
    input_json_file2 = layer_dir / "hotspot-micrometer-config.json"
    with open(input_json_file2, "r") as f:
        layer_data2 = json.load(f)

    layer_name2 = "test-delete-presence-queries"
    create_data2 = copy.deepcopy(layer_data2)
    create_data2['layer_name'] = layer_name2
    create_data2['metadata']['name'] = layer_name2

    tmp_file2 = f"/tmp/create_{layer_name2}.json"
    with open(tmp_file2, "w") as f:
        json.dump(create_data2, f)

    # Cleanup: Delete layer if it exists from previous run
    delete_layer(layer_name2)

    create_response2 = create_layer(tmp_file2)
    assert create_response2.status_code == SUCCESS_STATUS_CODE

    # Layer 3: label-based presence
    input_json_file3 = layer_dir / "quarkus-micrometer-config.json"
    with open(input_json_file3, "r") as f:
        layer_data3 = json.load(f)

    layer_name3 = "test-delete-presence-label"
    create_data3 = copy.deepcopy(layer_data3)
    create_data3['layer_name'] = layer_name3
    create_data3['metadata']['name'] = layer_name3

    tmp_file3 = f"/tmp/create_{layer_name3}.json"
    with open(tmp_file3, "w") as f:
        json.dump(create_data3, f)

    # Cleanup: Delete layer if it exists from previous run
    delete_layer(layer_name3)

    create_response3 = create_layer(tmp_file3)
    assert create_response3.status_code == SUCCESS_STATUS_CODE

    # Delete all three layers
    layers = [
        (layer_name1, "presence='always'"),
        (layer_name2, "queries"),
        (layer_name3, "label")
    ]

    for layer_name, presence_type in layers:
        # Delete layer
        response = delete_layer(layer_name)
        data = response.json()

        # Verify deletion success
        assert response.status_code == SUCCESS_200_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == DELETE_LAYER_SUCCESS_MSG % layer_name

        # Verify it's gone
        list_response = list_layers(layer_name=layer_name)
        assert list_response.status_code == ERROR_404_STATUS_CODE

        print(f"✓ Successfully deleted layer with {presence_type}: {layer_name}")

    # Cleanup temp files
    os.remove(tmp_file1)
    os.remove(tmp_file2)
    os.remove(tmp_file3)


# =============================================================================
# B. NEGATIVE SCENARIOS - Delete Layer Failures (8 tests)
# =============================================================================

@pytest.mark.layers
@pytest.mark.negative
def test_delete_layer_non_existent(cluster_type):
    """
    Test Description: This test validates deleting a layer that doesn't exist returns 404
    """
    form_kruize_url(cluster_type)

    layer_name = "non-existent-layer-12345"

    # Try to delete non-existent layer
    response = delete_layer(layer_name)
    data = response.json()

    # Verify error response
    assert response.status_code == ERROR_404_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == DELETE_LAYER_NOT_FOUND_MSG % layer_name

    print(f"✓ Correctly rejected deletion of non-existent layer: {layer_name}")


@pytest.mark.layers
@pytest.mark.negative
def test_delete_layer_empty_name(cluster_type):
    """
    Test Description: This test validates deleting with empty layer name returns error
    """
    form_kruize_url(cluster_type)

    layer_name = ""

    # Try to delete with empty name
    response = delete_layer(layer_name)
    data = response.json()

    # Verify error response
    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == DELETE_LAYER_INVALID_NAME_MSG

    print(f"✓ Correctly rejected deletion with empty layer name")


@pytest.mark.layers
@pytest.mark.negative
def test_delete_layer_null_name(cluster_type):
    """
    Test Description: This test validates deleting with null layer name returns error
    """
    form_kruize_url(cluster_type)

    # Make direct request without name parameter (simulating null)
    url = get_kruize_url() + "/deleteLayer"
    response = requests.delete(url, params={'name': None})
    data = response.json()

    # Verify error response
    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == DELETE_LAYER_INVALID_NAME_MSG

    print(f"✓ Correctly rejected deletion with null layer name")


@pytest.mark.layers
@pytest.mark.negative
def test_delete_layer_invalid_name(cluster_type):
    """
    Test Description: This test validates deleting with invalid characters in name returns error
    """
    form_kruize_url(cluster_type)

    layer_name = "@#$%^&*()"

    # Try to delete with invalid characters
    response = delete_layer(layer_name)
    data = response.json()

    # Verify error response
    assert response.status_code == ERROR_404_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == DELETE_LAYER_NOT_FOUND_MSG % layer_name

    print(f"✓ Correctly rejected deletion with invalid layer name")


@pytest.mark.layers
@pytest.mark.negative
def test_delete_layer_missing_name_param(cluster_type):
    """
    Test Description: This test validates deleting without name parameter returns error
    """
    form_kruize_url(cluster_type)

    # Make direct request without name parameter
    url = get_kruize_url() + "/deleteLayer"
    response = requests.delete(url)
    data = response.json()

    # Verify error response
    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == DELETE_LAYER_INVALID_NAME_MSG

    print(f"✓ Correctly rejected deletion without name parameter")


@pytest.mark.layers
@pytest.mark.negative
@pytest.mark.skip(reason="TODO: Requires experiment creation and association with layer")
def test_delete_layer_with_active_experiments(cluster_type):
    """
    Test Description: This test validates that deleting a layer with active experiments fails
    """
    form_kruize_url(cluster_type)

    # TODO: This test requires:
    # 1. Create a layer
    # 2. Create an experiment that uses this layer
    # 3. Try to delete the layer (should fail)
    # 4. Cleanup: delete experiment, then delete layer

    layer_name = "test-layer-with-experiments"

    # Try to delete layer with active experiments
    response = delete_layer(layer_name)
    data = response.json()

    # Verify error response
    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    # Message format: "Cannot delete layer 'name' as it is currently associated with N active experiment(s)"
    assert "Cannot delete layer" in data['message']
    assert layer_name in data['message']
    assert "active experiment" in data['message']

    print(f"✓ Correctly rejected deletion of layer with active experiments")


@pytest.mark.layers
@pytest.mark.negative
def test_delete_layer_twice(cluster_type):
    """
    Test Description: This test validates that deleting the same layer twice fails the second time
    """
    form_kruize_url(cluster_type)

    input_json_file = layer_dir / "container-config.json"
    with open(input_json_file, "r") as f:
        layer_data = json.load(f)

    layer_name = "test-delete-twice"
    create_data = copy.deepcopy(layer_data)
    create_data['layer_name'] = layer_name
    create_data['metadata']['name'] = layer_name

    tmp_create = f"/tmp/create_{layer_name}.json"
    with open(tmp_create, "w") as f:
        json.dump(create_data, f)

    # Cleanup: Delete layer if it exists from previous run
    delete_layer(layer_name)

    # Setup: Create layer
    create_response = create_layer(tmp_create)
    assert create_response.status_code == SUCCESS_STATUS_CODE

    # First deletion - should succeed
    response1 = delete_layer(layer_name)
    data1 = response1.json()

    assert response1.status_code == SUCCESS_200_STATUS_CODE
    assert data1['status'] == SUCCESS_STATUS
    assert data1['message'] == DELETE_LAYER_SUCCESS_MSG % layer_name

    print(f"✓ First deletion succeeded: {layer_name}")

    # Second deletion - should fail (layer already deleted)
    response2 = delete_layer(layer_name)
    data2 = response2.json()

    assert response2.status_code == ERROR_404_STATUS_CODE
    assert data2['status'] == ERROR_STATUS
    assert data2['message'] == DELETE_LAYER_NOT_FOUND_MSG % layer_name

    print(f"✓ Second deletion correctly failed with 'layer not found'")

    # Cleanup temp file (layer already deleted)
    os.remove(tmp_create)


@pytest.mark.layers
@pytest.mark.negative
def test_delete_layer_sql_injection(cluster_type):
    """
    Test Description: This test validates that SQL injection attempts are handled safely
    """
    form_kruize_url(cluster_type)

    # Try various SQL injection patterns
    sql_injection_patterns = [
        "'; DROP TABLE layers; --",
        "' OR '1'='1",
        "1' UNION SELECT * FROM layers--",
        "admin'--",
        "' OR 1=1--"
    ]

    for injection_pattern in sql_injection_patterns:
        response = delete_layer(injection_pattern)
        data = response.json()

        # Should return not found (treated as normal layer name that doesn't exist)
        assert response.status_code == ERROR_404_STATUS_CODE
        assert data['status'] == ERROR_STATUS
        assert data['message'] == DELETE_LAYER_NOT_FOUND_MSG % injection_pattern

        print(f"✓ SQL injection pattern handled safely: {injection_pattern}")

    print(f"✓ All {len(sql_injection_patterns)} SQL injection patterns handled correctly")
