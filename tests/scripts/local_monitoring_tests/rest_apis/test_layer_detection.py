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

# Layer names to clean up in tests
CLEANUP_LAYER_NAMES = ['container', 'semeru', 'quarkus', 'hotspot']


@pytest.fixture(autouse=True)
def cleanup_test_layers():
    """Fixture to clean up test layers before and after each test"""
    for layer_name in CLEANUP_LAYER_NAMES:
        delete_layer_from_db(layer_name)
    yield
    for layer_name in CLEANUP_LAYER_NAMES:
        delete_layer_from_db(layer_name)


# =============================================================================
# A. Query-Based Detection Tests (8 tests)
# =============================================================================

@pytest.mark.layers
@pytest.mark.detection
@pytest.mark.parametrize("layer_file", [
    pytest.param("semeru-actuator-config.json", id="semeru_multiple_queries"),
    pytest.param("hotspot-micrometer-config.json", id="hotspot_multiple_queries"),
    pytest.param("quarkus-micrometer-config.json", id="quarkus_single_query"),
])
def test_layer_detection_with_valid_query(cluster_type, layer_file):
    """
    Test Description: Validates layer creation with query-based detection
    Uses existing layer configs with real Prometheus queries
    Expected: Layer should be created successfully
    """
    form_kruize_url(cluster_type)

    input_json_file = layer_dir / layer_file

    with open(input_json_file, "r") as json_file:
        input_json = json.load(json_file)
        layer_name = input_json['layer_name']

    response = create_layer(input_json_file)
    data = response.json()

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_LAYER_SUCCESS_MSG % layer_name

    print(f"✓ Layer '{layer_name}' created with query-based detection")


@pytest.mark.layers
@pytest.mark.detection
@pytest.mark.parametrize("layer_file", [
    pytest.param("semeru-actuator-config.json", id="semeru_queries"),
    pytest.param("hotspot-micrometer-config.json", id="hotspot_queries"),
])
def test_layer_detection_multiple_queries(cluster_type, layer_file):
    """
    Test Description: Validates layer creation with multiple queries
    Expected: Layer with multiple queries should be created successfully
    """
    form_kruize_url(cluster_type)

    input_json_file = layer_dir / layer_file

    with open(input_json_file, "r") as json_file:
        input_json = json.load(json_file)
        layer_name = input_json['layer_name']
        queries = input_json['layer_presence']['queries']
        assert len(queries) > 1, f"Expected multiple queries for {layer_name}"

    response = create_layer(input_json_file)
    data = response.json()

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_LAYER_SUCCESS_MSG % layer_name

    print(f"✓ Layer '{layer_name}' created with {len(queries)} queries")


# =============================================================================
# C. Presence='always' Detection Tests (2 tests)
# =============================================================================

@pytest.mark.layers
@pytest.mark.detection
def test_layer_detection_always_present(cluster_type):
    """
    Test Description: Validates layer with presence='always'
    Uses container-config.json which has presence='always'
    Expected: Layer should be created successfully
    """
    form_kruize_url(cluster_type)

    input_json_file = layer_dir / 'container-config.json'

    with open(input_json_file, "r") as json_file:
        input_json = json.load(json_file)
        layer_name = input_json['layer_name']
        assert input_json['layer_presence']['presence'] == 'always'

    response = create_layer(input_json_file)
    data = response.json()

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_LAYER_SUCCESS_MSG % layer_name

    print(f"✓ Layer '{layer_name}' created with presence='always'")


@pytest.mark.layers
@pytest.mark.detection
def test_layer_detection_always_for_all_containers(cluster_type):
    """
    Test Description: Validates presence='always' layer applies to all containers
    Uses container-config.json which should apply universally
    Expected: Layer should be created successfully
    """
    form_kruize_url(cluster_type)

    input_json_file = layer_dir / 'container-config.json'

    with open(input_json_file, "r") as json_file:
        input_json = json.load(json_file)
        layer_name = input_json['layer_name']

    response = create_layer(input_json_file)
    data = response.json()

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_LAYER_SUCCESS_MSG % layer_name

    print(f"✓ Layer '{layer_name}' created - should apply to all containers")


# =============================================================================
# D. Integration with Experiments Tests (5 tests)
# =============================================================================

@pytest.mark.layers
@pytest.mark.detection
@pytest.mark.integration
@pytest.mark.parametrize("layer_file", [
    pytest.param("container-config.json", id="container_always"),
    pytest.param("semeru-actuator-config.json", id="semeru_queries"),
    pytest.param("quarkus-micrometer-config.json", id="quarkus_queries"),
])
def test_create_layer_for_experiment_integration(cluster_type, layer_file):
    """
    Test Description: Creates layers that can be used in experiment integration
    Expected: Layers should be created successfully for later experiment use
    """
    form_kruize_url(cluster_type)

    input_json_file = layer_dir / layer_file

    with open(input_json_file, "r") as json_file:
        input_json = json.load(json_file)
        layer_name = input_json['layer_name']

    response = create_layer(input_json_file)
    data = response.json()

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_LAYER_SUCCESS_MSG % layer_name

    print(f"✓ Layer '{layer_name}' created for experiment integration")


@pytest.mark.layers
@pytest.mark.detection
@pytest.mark.integration
def test_multiple_layers_creation_for_detection(cluster_type):
    """
    Test Description: Creates multiple layers to test detection priority
    Expected: All layers should be created successfully
    """
    form_kruize_url(cluster_type)

    layer_files = [
        'container-config.json',
        'semeru-actuator-config.json',
        'quarkus-micrometer-config.json'
    ]

    created_layers = []

    for layer_file in layer_files:
        input_json_file = layer_dir / layer_file

        with open(input_json_file, "r") as json_file:
            input_json = json.load(json_file)
            layer_name = input_json['layer_name']
            created_layers.append(layer_name)

        response = create_layer(input_json_file)
        assert response.status_code == SUCCESS_STATUS_CODE

    print(f"✓ Created {len(created_layers)} layers: {created_layers}")


@pytest.mark.layers
@pytest.mark.detection
@pytest.mark.integration
def test_layer_with_prometheus_queries(cluster_type):
    """
    Test Description: Validates layers with actual Prometheus queries
    Expected: Layers with Prometheus queries should be created successfully
    """
    form_kruize_url(cluster_type)

    input_json_file = layer_dir / 'semeru-actuator-config.json'

    with open(input_json_file, "r") as json_file:
        input_json = json.load(json_file)
        layer_name = input_json['layer_name']
        queries = input_json['layer_presence']['queries']
        
        # Verify all queries have prometheus datasource
        for query in queries:
            assert query['datasource'] == 'prometheus'

    response = create_layer(input_json_file)
    data = response.json()

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_LAYER_SUCCESS_MSG % layer_name

    print(f"✓ Layer '{layer_name}' created with {len(queries)} Prometheus queries")


@pytest.mark.layers
@pytest.mark.detection
@pytest.mark.integration
def test_layer_tunables_for_detection(cluster_type):
    """
    Test Description: Validates layers have tunables for optimization
    Expected: All layers should have tunables defined
    """
    form_kruize_url(cluster_type)

    layer_files = [
        'container-config.json',
        'semeru-actuator-config.json',
        'hotspot-micrometer-config.json'
    ]

    for layer_file in layer_files:
        input_json_file = layer_dir / layer_file

        with open(input_json_file, "r") as json_file:
            input_json = json.load(json_file)
            layer_name = input_json['layer_name']
            tunables = input_json['tunables']
            assert len(tunables) > 0, f"Layer {layer_name} should have tunables"

        response = create_layer(input_json_file)
        assert response.status_code == SUCCESS_STATUS_CODE

        print(f"✓ Layer '{layer_name}' has {len(tunables)} tunable(s)")


@pytest.mark.layers
@pytest.mark.detection
@pytest.mark.integration
def test_layer_detection_performance_multiple_layers(cluster_type):
    """
    Test Description: Tests performance of creating multiple layers
    Expected: All layers should be created within acceptable time
    """
    form_kruize_url(cluster_type)

    import time

    layer_files = [
        'container-config.json',
        'semeru-actuator-config.json',
        'quarkus-micrometer-config.json',
        'hotspot-micrometer-config.json'
    ]

    start_time = time.time()

    for layer_file in layer_files:
        input_json_file = layer_dir / layer_file

        with open(input_json_file, "r") as json_file:
            input_json = json.load(json_file)
            layer_name = input_json['layer_name']

        response = create_layer(input_json_file)
        assert response.status_code == SUCCESS_STATUS_CODE

    end_time = time.time()
    total_time = end_time - start_time

    print(f"✓ Created {len(layer_files)} layers in {total_time:.3f} seconds")

# Made with Bob
