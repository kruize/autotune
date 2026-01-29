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

layer_dir = get_layer_dir()


@pytest.mark.sanity
def test_list_all_layers_no_parameter(cluster_type):
    """
    Test Description: This test validates listLayers API when no layer_name parameter is provided.
    It should return all layers in the system.
    """
    form_kruize_url(cluster_type)

    # First, create a layer to ensure there's at least one layer in the system
    input_json_file = layer_dir / 'container-config.json'
    create_response = create_layer(input_json_file)

    # Verify layer was created
    assert create_response.status_code == SUCCESS_STATUS_CODE

    # List all layers without specifying layer_name
    response = list_layers(layer_name=None)

    assert response.status_code == SUCCESS_200_STATUS_CODE

    layers = response.json()
    assert isinstance(layers, list)
    assert len(layers) > 0

    # Verify each layer has required fields
    for layer in layers:
        assert 'layer_name' in layer
        assert 'metadata' in layer
        assert 'layer_level' in layer
        assert 'layer_presence' in layer
        assert 'tunables' in layer

    print(f"✓ Successfully listed {len(layers)} layer(s)")


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
        'openj9-actuator-config.json',
        'quarkus-micrometer-config.json'
    ]

    created_layer_names = []

    for layer_file in layer_files:
        input_json_file = layer_dir / layer_file

        with open(input_json_file, "r") as json_file:
            input_json = json.load(json_file)
            created_layer_names.append(input_json['layer_name'])

        create_response = create_layer(input_json_file)
        assert create_response.status_code == SUCCESS_STATUS_CODE

    print(f"Created {len(created_layer_names)} layers: {created_layer_names}")

    # List all layers
    response = list_layers(layer_name=None)

    assert response.status_code == SUCCESS_200_STATUS_CODE

    layers = response.json()
    assert isinstance(layers, list)
    assert len(layers) >= len(created_layer_names)

    # Verify all created layers are present
    returned_layer_names = [layer['layer_name'] for layer in layers]

    for expected_name in created_layer_names:
        assert expected_name in returned_layer_names, f"Layer '{expected_name}' not found in list"

    print(f"✓ Successfully listed {len(layers)} layer(s), verified all {len(created_layer_names)} created layers are present")


@pytest.mark.sanity
@pytest.mark.parametrize("layer_file", [
    pytest.param("container-config.json", id="container_layer"),
    pytest.param("openj9-actuator-config.json", id="openj9_layer"),
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

    returned_layer = layers[0]

    # Verify layer name matches
    assert returned_layer['layer_name'] == expected_layer_name

    # Verify metadata
    assert 'metadata' in returned_layer
    assert returned_layer['metadata']['name'] == input_json['metadata']['name']

    # Verify layer_level
    assert returned_layer['layer_level'] == input_json['layer_level']

    # Verify layer_presence structure
    assert 'layer_presence' in returned_layer

    # Verify tunables
    assert 'tunables' in returned_layer
    assert len(returned_layer['tunables']) == len(expected_tunables)

    # Verify tunable names match
    returned_tunable_names = [t['name'] for t in returned_layer['tunables']]
    expected_tunable_names = [t['name'] for t in expected_tunables]

    for expected_name in expected_tunable_names:
        assert expected_name in returned_tunable_names, f"Tunable '{expected_name}' not found"

    print(f"✓ Successfully listed layer '{expected_layer_name}' with {len(returned_layer['tunables'])} tunable(s)")


@pytest.mark.sanity
@pytest.mark.parametrize("layer_file", [
    pytest.param("container-config.json", id="container_layer"),
    pytest.param("openj9-actuator-config.json", id="openj9_layer"),
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

    returned_layer = layers[0]

    # ========== Validate Top-Level Fields ==========

    # 1. apiVersion
    assert 'apiVersion' in returned_layer
    assert returned_layer['apiVersion'] == input_json['apiVersion']
    print(f"  ✓ apiVersion: {returned_layer['apiVersion']}")

    # 2. kind
    assert 'kind' in returned_layer
    assert returned_layer['kind'] == input_json['kind']
    print(f"  ✓ kind: {returned_layer['kind']}")

    # 3. metadata
    assert 'metadata' in returned_layer
    assert isinstance(returned_layer['metadata'], dict)
    assert 'name' in returned_layer['metadata']
    assert returned_layer['metadata']['name'] == input_json['metadata']['name']
    print(f"  ✓ metadata.name: {returned_layer['metadata']['name']}")

    # 4. layer_name
    assert 'layer_name' in returned_layer
    assert returned_layer['layer_name'] == input_json['layer_name']
    print(f"  ✓ layer_name: {returned_layer['layer_name']}")

    # 5. layer_level
    assert 'layer_level' in returned_layer
    assert returned_layer['layer_level'] == input_json['layer_level']
    print(f"  ✓ layer_level: {returned_layer['layer_level']}")

    # 6. details (optional field)
    if 'details' in input_json:
        assert 'details' in returned_layer
        assert returned_layer['details'] == input_json['details']
        print(f"  ✓ details: {returned_layer['details']}")

    # ========== Validate layer_presence ==========

    assert 'layer_presence' in returned_layer
    assert isinstance(returned_layer['layer_presence'], dict)

    input_presence = input_json['layer_presence']
    returned_presence = returned_layer['layer_presence']

    # Check presence type and validate accordingly
    if 'presence' in input_presence:
        assert 'presence' in returned_presence
        assert returned_presence['presence'] == input_presence['presence']
        print(f"  ✓ layer_presence.presence: {returned_presence['presence']}")

    if 'queries' in input_presence:
        assert 'queries' in returned_presence
        assert isinstance(returned_presence['queries'], list)
        assert len(returned_presence['queries']) == len(input_presence['queries'])

        for i, query in enumerate(input_presence['queries']):
            returned_query = returned_presence['queries'][i]
            assert 'datasource' in returned_query
            assert returned_query['datasource'] == query['datasource']
            assert 'query' in returned_query
            assert returned_query['query'] == query['query']
            assert 'key' in returned_query
            assert returned_query['key'] == query['key']

        print(f"  ✓ layer_presence.queries: {len(returned_presence['queries'])} queries validated")

    if 'label' in input_presence:
        assert 'label' in returned_presence
        assert isinstance(returned_presence['label'], list)
        assert len(returned_presence['label']) == len(input_presence['label'])

        for i, label in enumerate(input_presence['label']):
            returned_label = returned_presence['label'][i]
            assert 'name' in returned_label
            assert returned_label['name'] == label['name']
            assert 'value' in returned_label
            assert returned_label['value'] == label['value']

        print(f"  ✓ layer_presence.label: {len(returned_presence['label'])} labels validated")

    # ========== Validate tunables ==========

    assert 'tunables' in returned_layer
    assert isinstance(returned_layer['tunables'], list)
    assert len(returned_layer['tunables']) == len(input_json['tunables'])
    print(f"  ✓ tunables count: {len(returned_layer['tunables'])}")

    # Validate each tunable in detail
    for i, input_tunable in enumerate(input_json['tunables']):
        # Find matching tunable by name
        tunable_name = input_tunable['name']
        returned_tunable = None

        for rt in returned_layer['tunables']:
            if rt['name'] == tunable_name:
                returned_tunable = rt
                break

        assert returned_tunable is not None, f"Tunable '{tunable_name}' not found in response"

        # Validate tunable name
        assert returned_tunable['name'] == input_tunable['name']

        # Validate value_type
        assert 'value_type' in returned_tunable
        assert returned_tunable['value_type'] == input_tunable['value_type']

        # Validate description (optional)
        if 'description' in input_tunable:
            assert 'description' in returned_tunable
            assert returned_tunable['description'] == input_tunable['description']

        # Validate bounded tunable fields (lower_bound, upper_bound, step)
        if 'lower_bound' in input_tunable:
            assert 'lower_bound' in returned_tunable
            assert returned_tunable['lower_bound'] == input_tunable['lower_bound']
            assert 'upper_bound' in returned_tunable
            assert returned_tunable['upper_bound'] == input_tunable['upper_bound']
            assert 'step' in returned_tunable
            assert returned_tunable['step'] == input_tunable['step']
            print(f"    ✓ Tunable '{tunable_name}': bounded (lower={input_tunable['lower_bound']}, upper={input_tunable['upper_bound']}, step={input_tunable['step']})")

        # Validate categorical tunable fields (choices)
        if 'choices' in input_tunable:
            assert 'choices' in returned_tunable
            assert isinstance(returned_tunable['choices'], list)
            assert len(returned_tunable['choices']) == len(input_tunable['choices'])

            # Verify all choices are present
            for choice in input_tunable['choices']:
                assert choice in returned_tunable['choices'], f"Choice '{choice}' not found in tunable '{tunable_name}'"

            print(f"    ✓ Tunable '{tunable_name}': categorical ({len(input_tunable['choices'])} choices)")

    print(f"\n✓ All fields and values validated successfully for layer '{layer_name}'")


# ========== Negative Test Cases ==========

@pytest.mark.negative
def test_list_layer_with_non_existent_name(cluster_type):
    """
    Test Description: This test validates listLayers API when querying a layer that doesn't exist.
    Expected: Should return 404 status with appropriate error message.
    """
    form_kruize_url(cluster_type)

    non_existent_layer_name = "non-existent-layer-12345"

    # Try to list a layer that doesn't exist
    response = list_layers(layer_name=non_existent_layer_name, logging=False)

    print(f"Response for non-existent layer '{non_existent_layer_name}': {response.status_code}")

    assert response.status_code == ERROR_404_STATUS_CODE

    data = response.json()
    assert data['status'] == ERROR_STATUS
    assert LAYER_NOT_FOUND_MSG % non_existent_layer_name in data['message']

    print(f"✓ Correctly returned 404 for non-existent layer '{non_existent_layer_name}'")


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
    # Error message should mention the invalid parameter
    assert invalid_param in data['message'] or "invalid" in data['message'].lower()

    print(f"✓ Correctly returned error for invalid parameter '{invalid_param}'")


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
    Expected: Should handle gracefully - either return 404 for non-existent layer or 400 for invalid name format.
    """
    form_kruize_url(cluster_type)

    print(f"Testing with special character in layer name: '{special_char_name}'")

    # Try to list a layer with special characters in name
    response = list_layers(layer_name=special_char_name, logging=False)

    print(f"Response status code: {response.status_code}")

    # Should return either 404 (not found) or 400 (invalid name format)
    assert response.status_code in [ERROR_STATUS_CODE, ERROR_404_STATUS_CODE]

    data = response.json()
    assert data['status'] == ERROR_STATUS

    # Error message should indicate either not found or invalid name
    assert (LAYER_NOT_FOUND_MSG % special_char_name in data['message'] or
            "invalid" in data['message'].lower() or
            "not found" in data['message'].lower())

    print(f"✓ Correctly handled special character in layer name '{special_char_name}': {response.status_code}")
