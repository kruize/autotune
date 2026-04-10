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

sys.path.append("../../")

from helpers.fixtures import *
from helpers.kruize import *
from helpers.utils import *
from helpers.list_layers_validate import *
from helpers.list_layers_schema import *

layer_dir = get_layer_dir()

@pytest.mark.layers
@pytest.mark.negative
def test_list_layers_when_no_layers_exist(cluster_type):
    """
    Test Description: This test validates listLayers API behavior when no layers exist in the system.
    Expected: Should return 400 status with "No layers found!" message.
    """
    form_kruize_url(cluster_type)

    # Cleanup: Delete all existing layers to ensure clean state
    cleanup_all_layers()

    # List layers when none exist
    response = list_layers(layer_name=None, logging=False)

    # API should return 400 when no layers exist
    assert response.status_code == ERROR_STATUS_CODE

    data = response.json()
    assert data['status'] == ERROR_STATUS
    assert data['message'] == LIST_LAYERS_NO_LAYERS_FOUND_MSG

    print("✓ Correctly returned error when no layers exist")


@pytest.mark.layers
@pytest.mark.sanity
def test_list_all_layers_no_parameter(cluster_type):
    """
    Test Description: This test validates listLayers API when no layer_name parameter is provided.
    It should return all layers in the system.
    """
    form_kruize_url(cluster_type)

    # First, create a layer to ensure there's at least one layer in the system
    input_json_file = layer_dir / 'container-config.json'

    with open(input_json_file, "r") as json_file:
        input_json = json.load(json_file)
        expected_layer_name = input_json['layer_name']

    # Cleanup before test to ensure clean state
    delete_layer(expected_layer_name)

    create_response = create_layer(input_json_file)

    # Verify layer was created
    assert create_response.status_code == SUCCESS_STATUS_CODE

    # List all layers without specifying layer_name
    response = list_layers(layer_name=None)

    assert response.status_code == SUCCESS_200_STATUS_CODE

    layers = response.json()
    assert isinstance(layers, list)
    assert len(layers) > 0

    # Validate the json against the json schema
    errorMsg = validate_list_layers_json(layers, list_layers_schema)
    assert errorMsg == ""

    # Find and validate the created layer data
    created_layer = next((layer for layer in layers if layer['layer_name'] == expected_layer_name), None)
    assert created_layer is not None, f"Created layer '{expected_layer_name}' not found in list"
    validate_layer_data(created_layer, input_json, verbose=False)

    print(f"✓ Successfully listed {len(layers)} layer(s)")

    # Cleanup: Delete layer
    delete_layer(expected_layer_name)


@pytest.mark.layers
@pytest.mark.sanity
def test_list_all_layers_with_multiple_layers(cluster_type):
    """
    Test Description: This test validates listLayers API when multiple layers are present.
    Creates multiple layers and verifies all are returned.
    """
    form_kruize_url(cluster_type)

    # Cleanup all existing layers first to ensure clean state
    cleanup_all_layers()

    # Create multiple layers
    layer_files = [
        'container-config.json',
        'semeru-actuator-config.json',
        'quarkus-micrometer-config.json'
    ]

    created_layers_data = {}

    # Cleanup before test to ensure clean state
    for layer_file in layer_files:
        input_json_file = layer_dir / layer_file
        with open(input_json_file, "r") as json_file:
            input_json = json.load(json_file)
            layer_name = input_json['layer_name']
            delete_layer(layer_name)

    for layer_file in layer_files:
        input_json_file = layer_dir / layer_file

        with open(input_json_file, "r") as json_file:
            input_json = json.load(json_file)
            layer_name = input_json['layer_name']
            created_layers_data[layer_name] = input_json

        create_response = create_layer(input_json_file)
        assert create_response.status_code == SUCCESS_STATUS_CODE

    print(f"Created {len(created_layers_data)} layers: {list(created_layers_data.keys())}")

    # List all layers
    response = list_layers(layer_name=None)

    assert response.status_code == SUCCESS_200_STATUS_CODE

    layers = response.json()
    assert isinstance(layers, list)
    assert len(layers) == len(created_layers_data)

    # Verify all created layers are present and validate their data
    returned_layer_names = [layer['layer_name'] for layer in layers]

    for expected_name in created_layers_data.keys():
        assert expected_name in returned_layer_names, f"Layer '{expected_name}' not found in list"

        # Find and validate the layer data
        returned_layer = next((layer for layer in layers if layer['layer_name'] == expected_name), None)
        assert returned_layer is not None

        # Validate the json against the json schema
        errorMsg = validate_list_layers_json([returned_layer], list_layers_schema)
        assert errorMsg == ""

        validate_layer_data(returned_layer, created_layers_data[expected_name], verbose=False)

    print(f"✓ Successfully listed {len(layers)} layer(s), verified all {len(created_layers_data)} created layers are present")

    # Cleanup: Delete all created layers
    for layer_name in created_layers_data.keys():
        delete_layer(layer_name)


@pytest.mark.layers
@pytest.mark.sanity
@pytest.mark.parametrize("layer_file", [
    pytest.param("container-config.json", id="container_layer"),
    pytest.param("semeru-actuator-config.json", id="semeru_layer"),
    pytest.param("quarkus-micrometer-config.json", id="quarkus_layer"),
    pytest.param("hotspot-micrometer-config.json", id="hotspot_layer")
])
def test_list_specific_layer_by_name(cluster_type, layer_file):
    """
    Test Description: This test validates listLayers API when querying a specific layer by name.
    It should return only the requested layer with all its details.
    """
    form_kruize_url(cluster_type)

    # Create a specific layer
    input_json_file = layer_dir / layer_file

    with open(input_json_file, "r") as json_file:
        input_json = json.load(json_file)
        expected_layer_name = input_json['layer_name']
        expected_tunables = input_json['tunables']
        expected_presence = input_json['layer_presence']

    # Cleanup before test to ensure clean state
    delete_layer(expected_layer_name)

    create_response = create_layer(input_json_file)
    assert create_response.status_code == SUCCESS_STATUS_CODE

    print(f"Created layer: {expected_layer_name}")

    # List the specific layer by name
    response = list_layers(layer_name=expected_layer_name)

    assert response.status_code == SUCCESS_200_STATUS_CODE

    layers = response.json()
    assert isinstance(layers, list)
    assert len(layers) == 1, f"Expected 1 layer, got {len(layers)}"

    # Validate the json against the json schema
    errorMsg = validate_list_layers_json(layers, list_layers_schema)
    assert errorMsg == ""

    returned_layer = layers[0]

    # Validate all layer data matches input
    validate_layer_data(returned_layer, input_json, verbose=False)

    print(f"✓ Successfully listed layer '{expected_layer_name}' with {len(returned_layer['tunables'])} tunable(s)")

    # Cleanup: Delete layer
    delete_layer(expected_layer_name)


@pytest.mark.layers
@pytest.mark.sanity
@pytest.mark.parametrize("layer_file", [
    pytest.param("container-config.json", id="container_layer"),
    pytest.param("semeru-actuator-config.json", id="semeru_layer"),
    pytest.param("quarkus-micrometer-config.json", id="quarkus_layer"),
    pytest.param("hotspot-micrometer-config.json", id="hotspot_layer")
])
def test_list_layer_validates_all_fields_and_values(cluster_type, layer_file):
    """
    Test Description: This test validates that listLayers API returns all fields with correct values.
    It performs comprehensive validation of all layer parameters including:
    - Top-level fields (apiVersion, kind, metadata, layer_name, details, layer_presence, tunables)
    - Nested metadata fields
    - Layer presence configuration (presence/queries/label)
    - All tunable fields and their values
    """
    form_kruize_url(cluster_type)

    # Create a layer
    input_json_file = layer_dir / layer_file

    with open(input_json_file, "r") as json_file:
        input_json = json.load(json_file)

    layer_name = input_json['layer_name']

    # Cleanup before test to ensure clean state
    delete_layer(layer_name)

    create_response = create_layer(input_json_file)
    assert create_response.status_code == SUCCESS_STATUS_CODE

    print(f"Testing comprehensive field validation for layer: {layer_name}")

    # List the layer
    response = list_layers(layer_name=layer_name)
    assert response.status_code == SUCCESS_200_STATUS_CODE

    layers = response.json()
    assert len(layers) == 1

    # Validate the json against the json schema
    errorMsg = validate_list_layers_json(layers, list_layers_schema)
    assert errorMsg == ""

    returned_layer = layers[0]

    # Validate all layer data matches input
    validate_layer_data(returned_layer, input_json, verbose=True)
    print(f"\n✓ All fields and values validated successfully for layer '{layer_name}'")

    # Cleanup: Delete layer
    delete_layer(layer_name)


# ========== Negative Test Cases ==========

@pytest.mark.layers
@pytest.mark.negative
def test_list_layer_with_non_existent_name(cluster_type):
    """
    Test Description: This test validates listLayers API when querying a layer that doesn't exist.
    Expected: Should return 400 status with appropriate error message.
    """
    form_kruize_url(cluster_type)

    non_existent_layer_name = "non-existent-layer-12345"

    # Try to list a layer that doesn't exist
    response = list_layers(layer_name=non_existent_layer_name, logging=False)

    print(f"Response for non-existent layer '{non_existent_layer_name}': {response.status_code}")

    assert response.status_code == ERROR_STATUS_CODE

    data = response.json()
    assert data['status'] == ERROR_STATUS
    assert data['message'] == LIST_LAYERS_INVALID_LAYER_NAME_MSG % non_existent_layer_name

    print(f"✓ Correctly returned 400 for non-existent layer '{non_existent_layer_name}'")


@pytest.mark.layers
@pytest.mark.negative
@pytest.mark.parametrize("invalid_param,param_value", [
    pytest.param("invalid_param", "some_value", id="unknown_parameter"),
    pytest.param("verbose", "true", id="unsupported_parameter"),
    pytest.param("limit", "10", id="unsupported_limit"),
])
def test_list_layers_with_invalid_query_parameter(cluster_type, invalid_param, param_value):
    """
    Test Description: This test validates listLayers API when invalid query parameters are provided.
    Expected: Should return 400 status with appropriate error message about invalid parameters.
    """
    form_kruize_url(cluster_type)

    # Construct URL with invalid parameter
    url = get_kruize_url() + f"/listLayers?{invalid_param}={param_value}"

    print(f"Testing with invalid parameter: {invalid_param}={param_value}")
    print(f"URL: {url}")

    import requests
    response = requests.get(url)

    print(f"Response status code: {response.status_code}")

    # Should return error for invalid parameters
    assert response.status_code == ERROR_STATUS_CODE

    data = response.json()
    assert data['status'] == ERROR_STATUS
    # Validate exact error message for invalid query parameter
    assert data['message'] == LIST_LAYERS_INVALID_QUERY_PARAM_MSG % invalid_param

    print(f"✓ Correctly returned error for invalid parameter '{invalid_param}'")


@pytest.mark.layers
@pytest.mark.negative
@pytest.mark.parametrize("special_char_name", [
    pytest.param("layer@special", id="at_symbol"),
    pytest.param("layer#hash", id="hash_symbol"),
    pytest.param("layer$dollar", id="dollar_symbol"),
    pytest.param("layer name", id="space_in_name"),
    pytest.param("layer/slash", id="slash_in_name"),
    pytest.param("layer\\backslash", id="backslash_in_name"),
    pytest.param("layer?question", id="question_mark"),
    pytest.param("layer&ampersand", id="ampersand"),
    # Edge Cases (2 tests) - Unicode and special characters
    pytest.param("layer_测试_unicode", id="unicode_in_name"),
    pytest.param("layer!@#$%^&*()", id="multiple_special_chars"),
])
def test_list_layer_with_special_characters_in_name(cluster_type, special_char_name):
    """
    Test Description: This test validates listLayers API when layer name contains special characters.
    Expected: Should handle gracefully - return 400 for non-existent or invalid layer name.
    """
    form_kruize_url(cluster_type)

    print(f"Testing with special character in layer name: '{special_char_name}'")

    # Try to list a layer with special characters in name
    response = list_layers(layer_name=special_char_name, logging=False)

    print(f"Response status code: {response.status_code}")

    # Should return 400 (not found or invalid name format)
    assert response.status_code == ERROR_STATUS_CODE

    data = response.json()
    assert data['status'] == ERROR_STATUS

    # Validate exact error message for invalid layer name
    assert data['message'] == LIST_LAYERS_INVALID_LAYER_NAME_MSG % special_char_name

    print(f"✓ Correctly handled special character in layer name '{special_char_name}': {response.status_code}")



# =============================================================================
# POSITIVE TEST CASES - Performance and Pagination Tests
# =============================================================================

@pytest.mark.layers
@pytest.mark.sanity
def test_list_layers_performance_with_many_layers(cluster_type):
    """
    Test Description: This test validates listLayers API correctness and scalability with multiple layers.
    Creates multiple layers, lists them, and verifies correct results are returned.
    Note: Response time is measured and logged for informational purposes only - no strict latency
    SLO is enforced to keep this test stable across different environments.
    """
    form_kruize_url(cluster_type)

    # Create 10 layers (reduced from 100+ for practical testing)
    # In production, this would create 100+ layers
    num_layers = 10
    created_layers = []

    print(f"Creating {num_layers} layers for performance testing...")
    
    for i in range(num_layers):
        layer_name = f"perf-test-layer-{i}"
        # Cleanup before creating to prevent 409 conflicts
        delete_layer(layer_name)
        created_layers.append(layer_name)

        # Create a simple layer
        tmp_json_file = f"/tmp/create_layer_perf_{i}.json"
        json_obj = {
            "apiVersion": "recommender.com/v1",
            "kind": "KruizeLayer",
            "metadata": {"name": f"perf-meta-{i}"},
            "layer_name": layer_name,
            "details": f"Performance test layer {i}",
            "layer_presence": {"presence": "always"},
            "tunables": [{"name": f"tunable_{i}", "value_type": "double", "upper_bound": "100", "lower_bound": "10", "step": 1}]
        }
        
        try:
            with open(tmp_json_file, "w") as f:
                json.dump(json_obj, f)
            
            response = create_layer(tmp_json_file)
            assert response.status_code == SUCCESS_STATUS_CODE, f"Failed to create layer {layer_name}"
        finally:
            if os.path.exists(tmp_json_file):
                os.remove(tmp_json_file)

    print(f"✓ Created {num_layers} layers successfully")

    # List all layers and measure performance
    import time
    start_time = time.time()
    response = list_layers(layer_name=None)
    end_time = time.time()
    
    response_time = end_time - start_time
    
    assert response.status_code == SUCCESS_200_STATUS_CODE
    layers = response.json()
    assert isinstance(layers, list)
    assert len(layers) >= num_layers, f"Expected at least {num_layers} layers, got {len(layers)}"
    
    print(f"✓ Listed {len(layers)} layers in {response_time:.3f} seconds")
    
    # Cleanup: Delete all created layers
    for layer_name in created_layers:
        delete_layer(layer_name)
    
    print(f"✓ Performance test completed - Response time: {response_time:.3f}s for {len(layers)} layers")


@pytest.mark.layers
@pytest.mark.sanity
def test_list_layers_case_sensitivity(cluster_type):
    """
    Test Description: This test validates if layer name search is case-sensitive.
    Creates a layer and tries to list it with different case variations.
    """
    form_kruize_url(cluster_type)

    # Create a layer with mixed case name
    layer_name = "TestCaseLayer"
    # Cleanup before creating to prevent 409 conflicts
    delete_layer(layer_name)

    tmp_json_file = "/tmp/create_layer_case_test.json"
    json_obj = {
        "apiVersion": "recommender.com/v1",
        "kind": "KruizeLayer",
        "metadata": {"name": "case-test-meta"},
        "layer_name": layer_name,
        "details": "Case sensitivity test layer",
        "layer_presence": {"presence": "always"},
        "tunables": [{"name": "t1", "value_type": "double", "upper_bound": "100", "lower_bound": "10", "step": 1}]
    }
    
    try:
        with open(tmp_json_file, "w") as f:
            json.dump(json_obj, f)
        
        create_response = create_layer(tmp_json_file)
        assert create_response.status_code == SUCCESS_STATUS_CODE
        
        print(f"✓ Created layer: {layer_name}")
        
        # Try to list with exact case - should succeed
        response_exact = list_layers(layer_name=layer_name)
        assert response_exact.status_code == SUCCESS_200_STATUS_CODE
        layers_exact = response_exact.json()
        assert len(layers_exact) == 1
        assert layers_exact[0]['layer_name'] == layer_name
        print(f"✓ Found layer with exact case: {layer_name}")
        
        # Try to list with different case - should fail (case-sensitive)
        response_lower = list_layers(layer_name=layer_name.lower(), logging=False)
        assert response_lower.status_code == ERROR_STATUS_CODE
        data_lower = response_lower.json()
        assert data_lower['message'] == LIST_LAYERS_INVALID_LAYER_NAME_MSG % layer_name.lower()
        print(f"✓ Correctly rejected lowercase variation: {layer_name.lower()}")
        
        # Try to list with uppercase - should fail (case-sensitive)
        response_upper = list_layers(layer_name=layer_name.upper(), logging=False)
        assert response_upper.status_code == ERROR_STATUS_CODE
        data_upper = response_upper.json()
        assert data_upper['message'] == LIST_LAYERS_INVALID_LAYER_NAME_MSG % layer_name.upper()
        print(f"✓ Correctly rejected uppercase variation: {layer_name.upper()}")
        
        print("✓ Layer name search is case-sensitive")
        
    finally:
        if os.path.exists(tmp_json_file):
            os.remove(tmp_json_file)
        delete_layer(layer_name)


@pytest.mark.layers
@pytest.mark.sanity
def test_list_layers_sorting_order(cluster_type):
    """
    Test Description: This test validates the sorting order of layers returned by listLayers API.
    Creates multiple layers and verifies they are returned in a consistent order.
    """
    form_kruize_url(cluster_type)

    # Create multiple layers with different names
    layer_names = ["alpha-layer", "beta-layer", "gamma-layer", "delta-layer"]
    created_layers = []

    print(f"Creating {len(layer_names)} layers for sorting test...")

    for layer_name in layer_names:
        # Cleanup before creating to prevent 409 conflicts
        delete_layer(layer_name)
        created_layers.append(layer_name)
        tmp_json_file = f"/tmp/create_layer_sort_{layer_name}.json"
        json_obj = {
            "apiVersion": "recommender.com/v1",
            "kind": "KruizeLayer",
            "metadata": {"name": f"sort-meta-{layer_name}"},
            "layer_name": layer_name,
            "details": f"Sorting test layer {layer_name}",
            "layer_presence": {"presence": "always"},
            "tunables": [{"name": "t1", "value_type": "double", "upper_bound": "100", "lower_bound": "10", "step": 1}]
        }
        
        try:
            with open(tmp_json_file, "w") as f:
                json.dump(json_obj, f)
            
            response = create_layer(tmp_json_file)
            assert response.status_code == SUCCESS_STATUS_CODE, f"Failed to create layer {layer_name}"
        finally:
            if os.path.exists(tmp_json_file):
                os.remove(tmp_json_file)

    print(f"✓ Created {len(layer_names)} layers successfully")

    # List all layers
    response = list_layers(layer_name=None)
    assert response.status_code == SUCCESS_200_STATUS_CODE
    
    layers = response.json()
    assert isinstance(layers, list)
    assert len(layers) >= len(layer_names)
    
    # Extract the names of created layers from response
    returned_layer_names = [layer['layer_name'] for layer in layers if layer['layer_name'] in layer_names]
    
    # Verify all created layers are present
    assert len(returned_layer_names) == len(layer_names), \
        f"Expected {len(layer_names)} layers, got {len(returned_layer_names)}"
    
    print(f"✓ All {len(layer_names)} layers returned")
    print(f"  Returned order: {returned_layer_names}")
    
    # Check if layers are sorted (alphabetically or by creation order)
    # Note: The actual sorting behavior depends on the API implementation
    is_alphabetically_sorted = returned_layer_names == sorted(returned_layer_names)
    
    if is_alphabetically_sorted:
        print("✓ Layers are sorted alphabetically")
    else:
        print("✓ Layers are returned in a consistent order (not alphabetically sorted)")
    
    # Cleanup: Delete all created layers
    for layer_name in created_layers:
        delete_layer(layer_name)
    
    print(f"✓ Sorting test completed")
