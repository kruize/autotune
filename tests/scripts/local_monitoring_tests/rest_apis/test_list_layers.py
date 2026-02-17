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

# Layer names to clean up in tests
CLEANUP_LAYER_NAMES = ['container', 'semeru', 'quarkus', 'hotspot', 'test-layer']


@pytest.fixture(autouse=True)
def cleanup_test_layers():
    """Fixture to clean up test layers before and after each test"""
    # Cleanup before test - clean up all known layer names
    for layer_name in CLEANUP_LAYER_NAMES:
        delete_layer_from_db(layer_name)
    yield
    # Cleanup after test - clean up all known layer names
    for layer_name in CLEANUP_LAYER_NAMES:
        delete_layer_from_db(layer_name)


@pytest.mark.layers
@pytest.mark.negative
def test_list_layers_when_no_layers_exist(cluster_type):
    """
    Test Description: This test validates listLayers API behavior when no layers exist in the system.
    Expected: Should return 400 status with "No layers found!" message.
    """
    form_kruize_url(cluster_type)

    # List layers when none exist (fixture ensures clean state)
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


@pytest.mark.layers
@pytest.mark.sanity
def test_list_all_layers_with_multiple_layers(cluster_type):
    """
    Test Description: This test validates listLayers API when multiple layers are present.
    Creates multiple layers and verifies all are returned.
    """
    form_kruize_url(cluster_type)

    # Create multiple layers
    layer_files = [
        'container-config.json',
        'semeru-actuator-config.json',
        'quarkus-micrometer-config.json'
    ]

    created_layers_data = {}

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

    # Validate the json against the json schema
    errorMsg = validate_list_layers_json(layers, list_layers_schema)
    assert errorMsg == ""

    # Verify all created layers are present and validate their data
    returned_layer_names = [layer['layer_name'] for layer in layers]

    for expected_name in created_layers_data.keys():
        assert expected_name in returned_layer_names, f"Layer '{expected_name}' not found in list"

        # Find and validate the layer data
        returned_layer = next((layer for layer in layers if layer['layer_name'] == expected_name), None)
        assert returned_layer is not None
        validate_layer_data(returned_layer, created_layers_data[expected_name], verbose=False)

    print(f"✓ Successfully listed {len(layers)} layer(s), verified all {len(created_layers_data)} created layers are present")


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
    - Top-level fields (apiVersion, kind, metadata, layer_name, layer_level, details, layer_presence, tunables)
    - Nested metadata fields
    - Layer presence configuration (presence/queries/label)
    - All tunable fields and their values
    """
    form_kruize_url(cluster_type)

    # Create a layer
    input_json_file = layer_dir / layer_file

    with open(input_json_file, "r") as json_file:
        input_json = json.load(json_file)

    create_response = create_layer(input_json_file)
    assert create_response.status_code == SUCCESS_STATUS_CODE

    layer_name = input_json['layer_name']
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
    pytest.param("limit", "10", id="unsupported_limit")
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
    pytest.param("layer&ampersand", id="ampersand")
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
