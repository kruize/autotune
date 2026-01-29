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
@pytest.mark.parametrize("layer_file", [
    pytest.param("container-config.json", id="bounded_tunables"),
    pytest.param("openj9-actuator-config.json", id="mixed_tunables"),
    pytest.param("hotspot-micrometer-config.json", id="mixed_tunables_hotspot")
])
def test_create_layer_with_different_tunable_types(cluster_type, layer_file):
    """
    Test Description: This test validates createLayer API with different tunable types:
    - Bounded tunables (lower_bound, upper_bound, step)
    - Categorical tunables (choices list)
    - Mixed tunables (combination of both)
    """
    form_kruize_url(cluster_type)

    # Use existing layer file from manifests
    input_json_file = layer_dir / layer_file

    # Read the layer config to get the layer name
    with open(input_json_file, "r") as json_file:
        input_json = json.load(json_file)
        layer_name = input_json['layer_name']

    # Create layer
    response = create_layer(input_json_file)
    data = response.json()

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_LAYER_SUCCESS_MSG % layer_name

    print(f"✓ Layer '{layer_name}' created successfully")


@pytest.mark.sanity
@pytest.mark.parametrize("layer_file", [
    pytest.param("container-config.json", id="presence_always"),
    pytest.param("openj9-actuator-config.json", id="presence_queries"),
    pytest.param("quarkus-micrometer-config.json", id="presence_label")
])
def test_create_layer_with_different_presence_types(cluster_type, layer_file):
    """
    Test Description: This test validates createLayer API with different layer_presence configurations:
    - presence='always' (always applicable)
    - queries (query-based detection)
    - label (label-based detection)
    """
    form_kruize_url(cluster_type)

    # Use existing layer file from manifests
    input_json_file = layer_dir / layer_file

    # Read the layer config to get the layer name
    with open(input_json_file, "r") as json_file:
        input_json = json.load(json_file)
        layer_name = input_json['layer_name']

    # Create layer
    response = create_layer(input_json_file)
    data = response.json()

    # Verify creation
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_LAYER_SUCCESS_MSG % layer_name

    print(f"✓ Layer '{layer_name}' created successfully")


@pytest.mark.sanity
def test_create_layer_with_minimum_required_fields(cluster_type):
    """
    Test Description: This test validates createLayer API with only minimum required fields
    Uses quarkus-micrometer-config.json which has minimal configuration
    """
    form_kruize_url(cluster_type)

    # Use quarkus config as it has minimal fields
    input_json_file = layer_dir / 'quarkus-micrometer-config.json'

    with open(input_json_file, "r") as json_file:
        input_json = json.load(json_file)
        layer_name = input_json['layer_name']

    response = create_layer(input_json_file)
    data = response.json()

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_LAYER_SUCCESS_MSG % layer_name

    print(f"✓ Layer '{layer_name}' created successfully with minimum required fields")
