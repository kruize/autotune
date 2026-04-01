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

sys.path.append("../../")

from helpers.fixtures import *
from helpers.kruize import *
from helpers.utils import *

layer_dir = get_layer_dir()

mandatory_fields = [
    ("apiVersion", ERROR_STATUS_CODE, ERROR_STATUS),
    ("kind", ERROR_STATUS_CODE, ERROR_STATUS),
    ("metadata", ERROR_STATUS_CODE, ERROR_STATUS),
    ("layer_name", ERROR_STATUS_CODE, ERROR_STATUS),
    ("layer_presence", ERROR_STATUS_CODE, ERROR_STATUS),
    ("tunables", ERROR_STATUS_CODE, ERROR_STATUS),
]


@pytest.mark.layers
@pytest.mark.sanity
def test_update_layer(cluster_type):
    """
    Test Description: This test validates the response status code of updateLayer API by passing a
    valid input for the layer update
    """
    form_kruize_url(cluster_type)

    input_json_file = layer_dir / "container-config.json"
    with open(input_json_file, "r") as f:
        layer_data = json.load(f)

    layer_name = "test-update-layer"
    create_data = copy.deepcopy(layer_data)
    create_data['layer_name'] = layer_name
    create_data['metadata']['name'] = layer_name

    delete_layer(layer_name)

    # Create layer
    tmp_create = f"/tmp/create_{layer_name}.json"
    with open(tmp_create, "w") as f:
        json.dump(create_data, f)

    create_response = create_layer(tmp_create)
    assert create_response.status_code == SUCCESS_STATUS_CODE

    # Update layer - change bounds
    update_data = copy.deepcopy(create_data)
    update_data['tunables'][0]['upper_bound'] = "4096"
    update_data['tunables'][0]['lower_bound'] = "1024"
    update_data['tunables'][0]['step'] = 2

    tmp_update = f"/tmp/update_{layer_name}.json"
    with open(tmp_update, "w") as f:
        json.dump(update_data, f)

    response = update_layer(layer_name, tmp_update)
    data = response.json()

    assert response.status_code == SUCCESS_200_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS

    print(f"✓ Successfully updated layer")

    # Cleanup
    delete_layer(layer_name)
    os.remove(tmp_create)
    os.remove(tmp_update)


@pytest.mark.layers
@pytest.mark.sanity
@pytest.mark.parametrize("test_name, layer_file", [
    ("change_presence_type", "container-config.json"),
    ("add_queries", "hotspot-micrometer-config.json"),
    ("modify_queries", "hotspot-micrometer-config.json"),
    ("change_details", "container-config.json"),
    ("add_tunable", "container-config.json"),
    ("remove_tunable", "container-config.json"),
    ("modify_tunable", "container-config.json"),
])
def test_update_layer_variations(cluster_type, test_name, layer_file):
    """
    Test Description: This test validates various updateLayer API scenarios
    """
    form_kruize_url(cluster_type)

    input_json_file = layer_dir / layer_file
    with open(input_json_file, "r") as f:
        layer_data = json.load(f)

    layer_name = f"test-update-{test_name}"
    create_data = copy.deepcopy(layer_data)
    create_data['layer_name'] = layer_name
    create_data['metadata']['name'] = layer_name

    delete_layer(layer_name)

    # Create layer
    tmp_create = f"/tmp/create_{layer_name}.json"
    with open(tmp_create, "w") as f:
        json.dump(create_data, f)

    create_response = create_layer(tmp_create)
    assert create_response.status_code == SUCCESS_STATUS_CODE

    # Modify based on test scenario
    update_data = copy.deepcopy(create_data)

    if test_name == "change_presence_type":
        update_data['layer_presence'] = {
            "queries": [{"datasource": "prometheus", "query": "up"}]
        }
    elif test_name == "add_queries":
        if 'queries' in update_data['layer_presence']:
            update_data['layer_presence']['queries'].append({
                "datasource": "prometheus",
                "query": "process_cpu_usage"
            })
    elif test_name == "modify_queries":
        if 'queries' in update_data['layer_presence']:
            update_data['layer_presence']['queries'][0]['query'] = "jvm_memory_used_bytes"
    elif test_name == "change_details":
        update_data['details'] = "Updated description for testing"
    elif test_name == "add_tunable":
        update_data['tunables'].append({
            "name": "newTunable",
            "value_type": "double",
            "upper_bound": "100",
            "lower_bound": "10",
            "step": 1,
            "units": "Gi"
        })
    elif test_name == "remove_tunable":
        update_data['tunables'] = update_data['tunables'][:-1]
    elif test_name == "modify_tunable":
        update_data['tunables'][0]['units'] = "GiB"

    tmp_update = f"/tmp/update_{layer_name}.json"
    with open(tmp_update, "w") as f:
        json.dump(update_data, f)

    response = update_layer(layer_name, tmp_update)

    assert response.status_code == SUCCESS_200_STATUS_CODE
    data = response.json()
    assert data['status'] == SUCCESS_STATUS

    print(f"✓ Successfully updated layer: {test_name}")

    # Cleanup
    delete_layer(layer_name)
    os.remove(tmp_create)
    os.remove(tmp_update)


@pytest.mark.layers
@pytest.mark.negative
def test_update_layer_non_existent(cluster_type):
    """
    Test Description: This test validates updateLayer API rejects update to non-existent layer
    """
    form_kruize_url(cluster_type)

    input_json_file = layer_dir / "container-config.json"
    with open(input_json_file, "r") as f:
        layer_data = json.load(f)

    layer_name = "test-non-existent-layer"
    update_data = copy.deepcopy(layer_data)
    update_data['layer_name'] = layer_name
    update_data['metadata']['name'] = layer_name

    tmp_update = f"/tmp/update_{layer_name}.json"
    with open(tmp_update, "w") as f:
        json.dump(update_data, f)

    response = update_layer(layer_name, tmp_update)

    assert response.status_code == ERROR_404_STATUS_CODE
    data = response.json()
    assert data['status'] == ERROR_STATUS
    assert data['message'] == UPDATE_LAYER_NOT_FOUND_MSG % layer_name

    print(f"✓ Correctly rejected: non_existent_layer")
    os.remove(tmp_update)


@pytest.mark.layers
@pytest.mark.negative
def test_update_layer_blank_name(cluster_type):
    """
    Test Description: This test validates updateLayer API rejects blank/empty layer name
    """
    form_kruize_url(cluster_type)

    input_json_file = layer_dir / "container-config.json"
    with open(input_json_file, "r") as f:
        layer_data = json.load(f)

    update_data = copy.deepcopy(layer_data)

    # Test with empty name - use requests directly
    import requests
    url = get_kruize_url() + "/updateLayer?name="
    response = requests.put(url, json=update_data)

    assert response.status_code == ERROR_STATUS_CODE
    data = response.json()
    assert data['status'] == ERROR_STATUS
    assert data['message'] == UPDATE_LAYER_INVALID_NAME_MSG

    print(f"✓ Correctly rejected: blank_name")


@pytest.mark.layers
@pytest.mark.negative
def test_update_layer_null_tunables(cluster_type):
    """
    Test Description: This test validates updateLayer API rejects null tunables
    """
    form_kruize_url(cluster_type)

    input_json_file = layer_dir / "container-config.json"
    with open(input_json_file, "r") as f:
        layer_data = json.load(f)

    layer_name = "test-null-tunables"
    create_data = copy.deepcopy(layer_data)
    create_data['layer_name'] = layer_name
    create_data['metadata']['name'] = layer_name

    delete_layer(layer_name)

    tmp_create = f"/tmp/create_{layer_name}.json"
    with open(tmp_create, "w") as f:
        json.dump(create_data, f)

    create_response = create_layer(tmp_create)
    assert create_response.status_code == SUCCESS_STATUS_CODE

    # Update with null tunables
    update_data = copy.deepcopy(create_data)
    update_data['tunables'] = None

    tmp_update = f"/tmp/update_{layer_name}.json"
    with open(tmp_update, "w") as f:
        json.dump(update_data, f)

    response = update_layer(layer_name, tmp_update)

    assert response.status_code == ERROR_STATUS_CODE
    data = response.json()
    assert data['status'] == ERROR_STATUS
    assert data['message'] == LAYER_TUNABLES_NULL_JSON_MSG

    print(f"✓ Correctly rejected: null_tunables")

    # Cleanup
    delete_layer(layer_name)
    os.remove(tmp_create)
    os.remove(tmp_update)


@pytest.mark.layers
@pytest.mark.negative
def test_update_layer_name_mismatch(cluster_type):
    """
    Test Description: This test validates updateLayer API rejects name mismatch between URL and payload
    """
    form_kruize_url(cluster_type)

    input_json_file = layer_dir / "container-config.json"
    with open(input_json_file, "r") as f:
        layer_data = json.load(f)

    layer_name = "test-name-mismatch"
    create_data = copy.deepcopy(layer_data)
    create_data['layer_name'] = layer_name
    create_data['metadata']['name'] = layer_name

    delete_layer(layer_name)

    tmp_create = f"/tmp/create_{layer_name}.json"
    with open(tmp_create, "w") as f:
        json.dump(create_data, f)

    create_response = create_layer(tmp_create)
    assert create_response.status_code == SUCCESS_STATUS_CODE

    # Update with mismatched name
    update_data = copy.deepcopy(create_data)
    different_name = "different-name"
    update_data['layer_name'] = different_name

    tmp_update = f"/tmp/update_{layer_name}.json"
    with open(tmp_update, "w") as f:
        json.dump(update_data, f)

    response = update_layer(layer_name, tmp_update)

    assert response.status_code == ERROR_STATUS_CODE
    data = response.json()
    assert data['status'] == ERROR_STATUS
    assert data['message'] == UPDATE_LAYER_NAME_MISMATCH_MSG % (layer_name, different_name)

    print(f"✓ Correctly rejected: name_mismatch")

    # Cleanup
    delete_layer(layer_name)
    os.remove(tmp_create)
    os.remove(tmp_update)


@pytest.mark.layers
@pytest.mark.negative
def test_update_layer_invalid_bounds(cluster_type):
    """
    Test Description: This test validates updateLayer API rejects invalid tunable bounds
    """
    form_kruize_url(cluster_type)

    input_json_file = layer_dir / "container-config.json"
    with open(input_json_file, "r") as f:
        layer_data = json.load(f)

    layer_name = "test-invalid-bounds"
    create_data = copy.deepcopy(layer_data)
    create_data['layer_name'] = layer_name
    create_data['metadata']['name'] = layer_name

    delete_layer(layer_name)

    tmp_create = f"/tmp/create_{layer_name}.json"
    with open(tmp_create, "w") as f:
        json.dump(create_data, f)

    create_response = create_layer(tmp_create)
    assert create_response.status_code == SUCCESS_STATUS_CODE

    # Update with invalid bounds (lower > upper)
    update_data = copy.deepcopy(create_data)
    update_data['tunables'][0]['lower_bound'] = "1000"
    update_data['tunables'][0]['upper_bound'] = "100"
    tunable_name = update_data['tunables'][0]['name']

    tmp_update = f"/tmp/update_{layer_name}.json"
    with open(tmp_update, "w") as f:
        json.dump(update_data, f)

    response = update_layer(layer_name, tmp_update)

    assert response.status_code == ERROR_STATUS_CODE
    data = response.json()
    assert data['status'] == ERROR_STATUS
    # Error message format: "Validation failed: Tunable 'memoryRequest' has invalid bounds; lowerBound (1000.0) must be less than upperBound (100.0)"
    assert "Validation failed" in data['message']
    assert tunable_name in data['message']
    assert "lowerBound" in data['message']
    assert "upperBound" in data['message']

    print(f"✓ Correctly rejected: invalid_tunable_bounds")

    # Cleanup
    delete_layer(layer_name)
    os.remove(tmp_create)
    os.remove(tmp_update)


@pytest.mark.layers
@pytest.mark.negative
def test_update_layer_duplicate_tunables(cluster_type):
    """
    Test Description: This test validates updateLayer API rejects duplicate tunable names
    """
    form_kruize_url(cluster_type)

    input_json_file = layer_dir / "container-config.json"
    with open(input_json_file, "r") as f:
        layer_data = json.load(f)

    layer_name = "test-duplicate-tunables"
    create_data = copy.deepcopy(layer_data)
    create_data['layer_name'] = layer_name
    create_data['metadata']['name'] = layer_name

    delete_layer(layer_name)

    tmp_create = f"/tmp/create_{layer_name}.json"
    with open(tmp_create, "w") as f:
        json.dump(create_data, f)

    create_response = create_layer(tmp_create)
    assert create_response.status_code == SUCCESS_STATUS_CODE

    # Update with duplicate tunables
    update_data = copy.deepcopy(create_data)
    duplicate_tunable_name = update_data['tunables'][0]['name']
    update_data['tunables'].append(update_data['tunables'][0])

    tmp_update = f"/tmp/update_{layer_name}.json"
    with open(tmp_update, "w") as f:
        json.dump(update_data, f)

    response = update_layer(layer_name, tmp_update)

    assert response.status_code == ERROR_STATUS_CODE
    data = response.json()
    assert data['status'] == ERROR_STATUS
    assert data['message'] == LAYER_DUPLICATE_TUNABLE_NAMES_MSG % duplicate_tunable_name

    print(f"✓ Correctly rejected: duplicate_tunable_names")

    # Cleanup
    delete_layer(layer_name)
    os.remove(tmp_create)
    os.remove(tmp_update)


@pytest.mark.layers
@pytest.mark.negative
def test_update_layer_remove_all_tunables(cluster_type):
    """
    Test Description: This test validates updateLayer API rejects removal of all tunables
    """
    form_kruize_url(cluster_type)

    input_json_file = layer_dir / "container-config.json"
    with open(input_json_file, "r") as f:
        layer_data = json.load(f)

    layer_name = "test-remove-all-tunables"
    create_data = copy.deepcopy(layer_data)
    create_data['layer_name'] = layer_name
    create_data['metadata']['name'] = layer_name

    delete_layer(layer_name)

    tmp_create = f"/tmp/create_{layer_name}.json"
    with open(tmp_create, "w") as f:
        json.dump(create_data, f)

    create_response = create_layer(tmp_create)
    assert create_response.status_code == SUCCESS_STATUS_CODE

    # Update with empty tunables array
    update_data = copy.deepcopy(create_data)
    update_data['tunables'] = []

    tmp_update = f"/tmp/update_{layer_name}.json"
    with open(tmp_update, "w") as f:
        json.dump(update_data, f)

    response = update_layer(layer_name, tmp_update)

    assert response.status_code == ERROR_STATUS_CODE
    data = response.json()
    assert data['status'] == ERROR_STATUS
    assert data['message'] == LAYER_TUNABLES_EMPTY_MSG

    print(f"✓ Correctly rejected: remove_all_tunables")

    # Cleanup
    delete_layer(layer_name)
    os.remove(tmp_create)
    os.remove(tmp_update)


@pytest.mark.layers
@pytest.mark.negative
def test_update_layer_invalid_presence(cluster_type):
    """
    Test Description: This test validates updateLayer API rejects invalid presence configuration
    """
    form_kruize_url(cluster_type)

    input_json_file = layer_dir / "container-config.json"
    with open(input_json_file, "r") as f:
        layer_data = json.load(f)

    layer_name = "test-invalid-presence"
    create_data = copy.deepcopy(layer_data)
    create_data['layer_name'] = layer_name
    create_data['metadata']['name'] = layer_name

    delete_layer(layer_name)

    tmp_create = f"/tmp/create_{layer_name}.json"
    with open(tmp_create, "w") as f:
        json.dump(create_data, f)

    create_response = create_layer(tmp_create)
    assert create_response.status_code == SUCCESS_STATUS_CODE

    # Update with empty presence
    update_data = copy.deepcopy(create_data)
    update_data['layer_presence'] = {}

    tmp_update = f"/tmp/update_{layer_name}.json"
    with open(tmp_update, "w") as f:
        json.dump(update_data, f)

    response = update_layer(layer_name, tmp_update)

    assert response.status_code == ERROR_STATUS_CODE
    data = response.json()
    assert data['status'] == ERROR_STATUS
    assert data['message'] == LAYER_PRESENCE_MISSING_MSG

    print(f"✓ Correctly rejected: invalid_presence_config")

    # Cleanup
    delete_layer(layer_name)
    os.remove(tmp_create)
    os.remove(tmp_update)


@pytest.mark.layers
@pytest.mark.extended
@pytest.mark.parametrize("field, expected_status_code, expected_status", mandatory_fields)
def test_update_layer_mandatory_fields(cluster_type, field, expected_status_code, expected_status):
    """
    Test Description: This test validates the update API of layer by missing the mandatory fields and validating
    the error message and status code
    """
    form_kruize_url(cluster_type)

    input_json_file = layer_dir / "container-config.json"
    with open(input_json_file, "r") as f:
        layer_data = json.load(f)

    layer_name = f"test-missing-{field}"
    create_data = copy.deepcopy(layer_data)
    create_data['layer_name'] = layer_name
    create_data['metadata']['name'] = layer_name

    delete_layer(layer_name)

    tmp_create = f"/tmp/create_{layer_name}.json"
    with open(tmp_create, "w") as f:
        json.dump(create_data, f)

    create_response = create_layer(tmp_create)
    assert create_response.status_code == SUCCESS_STATUS_CODE

    # Prepare update with missing field
    update_data = copy.deepcopy(create_data)

    if field == "apiVersion":
        update_data.pop("apiVersion", None)
    elif field == "kind":
        update_data.pop("kind", None)
    elif field == "metadata":
        update_data.pop("metadata", None)
    elif field == "layer_name":
        update_data.pop("layer_name", None)
    elif field == "layer_presence":
        update_data.pop("layer_presence", None)
    elif field == "tunables":
        update_data.pop("tunables", None)

    tmp_update = f"/tmp/update_{layer_name}.json"
    with open(tmp_update, "w") as f:
        json.dump(update_data, f)

    response = update_layer(layer_name, tmp_update)

    assert response.status_code == expected_status_code, \
        f"Mandatory field check failed for {field} actual - {response.status_code} expected - {expected_status_code}"
    data = response.json()
    assert data['status'] == expected_status

    # Verify error message contains information about the missing field
    if field in ["apiVersion", "kind", "metadata", "layer_name", "layer_presence", "tunables"]:
        # Check that error message mentions the missing field
        assert field in data['message'] or data['message'] == UPDATE_LAYER_MISSING_REQUIRED_FIELD_MSG % field, \
            f"Error message should mention missing field '{field}', got: {data['message']}"

    print(f"✓ Correctly rejected: missing_{field}")

    # Cleanup
    delete_layer(layer_name)
    os.remove(tmp_create)
    os.remove(tmp_update)


@pytest.mark.layers
@pytest.mark.negative
def test_update_layer_invalid_query_syntax(cluster_type):
    """
    Test Description: This test validates updateLayer API rejects invalid PromQL query syntax
    """
    form_kruize_url(cluster_type)

    input_json_file = layer_dir / "container-config.json"
    with open(input_json_file, "r") as f:
        layer_data = json.load(f)

    layer_name = "test-invalid-promql"
    create_data = copy.deepcopy(layer_data)
    create_data['layer_name'] = layer_name
    create_data['metadata']['name'] = layer_name

    delete_layer(layer_name)

    tmp_create = f"/tmp/create_{layer_name}.json"
    with open(tmp_create, "w") as f:
        json.dump(create_data, f)

    create_response = create_layer(tmp_create)
    assert create_response.status_code == SUCCESS_STATUS_CODE

    # Update with invalid PromQL query syntax
    update_data = copy.deepcopy(create_data)
    invalid_query = "up{job="
    update_data['layer_presence'] = {
        "queries": [{"datasource": "prometheus", "query": invalid_query}]  # Invalid syntax - unclosed brace
    }

    tmp_update = f"/tmp/update_{layer_name}.json"
    with open(tmp_update, "w") as f:
        json.dump(update_data, f)

    response = update_layer(layer_name, tmp_update)

    assert response.status_code == ERROR_STATUS_CODE
    data = response.json()
    assert data['status'] == ERROR_STATUS
    # Verify error message mentions query validation
    assert "Validation failed" in data['message']
    assert "query" in data['message'].lower()
    assert invalid_query in data['message']

    print(f"✓ Correctly rejected: invalid_query_syntax")

    # Cleanup
    delete_layer(layer_name)
    os.remove(tmp_create)
    os.remove(tmp_update)


@pytest.mark.layers
@pytest.mark.negative
def test_update_layer_change_to_invalid_value_type(cluster_type):
    """
    Test Description: This test validates updateLayer API rejects changing tunable value_type to invalid type
    """
    form_kruize_url(cluster_type)

    input_json_file = layer_dir / "container-config.json"
    with open(input_json_file, "r") as f:
        layer_data = json.load(f)

    layer_name = "test-invalid-value-type"
    create_data = copy.deepcopy(layer_data)
    create_data['layer_name'] = layer_name
    create_data['metadata']['name'] = layer_name

    delete_layer(layer_name)

    tmp_create = f"/tmp/create_{layer_name}.json"
    with open(tmp_create, "w") as f:
        json.dump(create_data, f)

    create_response = create_layer(tmp_create)
    assert create_response.status_code == SUCCESS_STATUS_CODE

    # Update with invalid value_type
    update_data = copy.deepcopy(create_data)
    tunable_name = update_data['tunables'][0]['name']
    update_data['tunables'][0]['value_type'] = "invalid_type"

    tmp_update = f"/tmp/update_{layer_name}.json"
    with open(tmp_update, "w") as f:
        json.dump(update_data, f)

    response = update_layer(layer_name, tmp_update)

    assert response.status_code == ERROR_STATUS_CODE
    data = response.json()
    assert data['status'] == ERROR_STATUS
    assert data['message'] == TUNABLE_INVALID_VALUE_TYPE_MSG % tunable_name

    print(f"✓ Correctly rejected: change_to_invalid_value_type")

    # Cleanup
    delete_layer(layer_name)
    os.remove(tmp_create)
    os.remove(tmp_update)


@pytest.mark.layers
@pytest.mark.negative
@pytest.mark.skip(reason="TODO: Implement when layer-experiment association and locking is implemented")
def test_update_layer_with_experiments_active(cluster_type):
    """
    Test Description: This test validates updateLayer API behavior when layer is used in active experiments

    TODO: This test should verify that:
    1. Layers cannot be updated while associated with active experiments, OR
    2. Updates are allowed but with appropriate warnings/validations

    This depends on business logic that may not be implemented yet.
    """
    form_kruize_url(cluster_type)

    input_json_file = layer_dir / "container-config.json"
    with open(input_json_file, "r") as f:
        layer_data = json.load(f)

    layer_name = "test-layer-with-experiment"
    create_data = copy.deepcopy(layer_data)
    create_data['layer_name'] = layer_name
    create_data['metadata']['name'] = layer_name

    delete_layer(layer_name)

    tmp_create = f"/tmp/create_{layer_name}.json"
    with open(tmp_create, "w") as f:
        json.dump(create_data, f)

    create_response = create_layer(tmp_create)
    assert create_response.status_code == SUCCESS_STATUS_CODE

    # TODO: Create an experiment using this layer
    # experiment_data = {...}
    # create_experiment(experiment_json_file)

    # Try to update layer while experiment is active
    update_data = copy.deepcopy(create_data)
    update_data['details'] = "Updated while experiment active"

    tmp_update = f"/tmp/update_{layer_name}.json"
    with open(tmp_update, "w") as f:
        json.dump(update_data, f)

    response = update_layer(layer_name, tmp_update)

    # TODO: Determine expected behavior
    # Option 1: Should fail if layer is locked
    # assert response.status_code == ERROR_STATUS_CODE
    # assert "experiment" in data['message'].lower() or "in use" in data['message'].lower()

    # Option 2: Should succeed with warnings
    # assert response.status_code == SUCCESS_200_STATUS_CODE
    # assert data.get('warnings') is not None

    print(f"✓ Test placeholder for: update_layer_with_experiments_active")

    # Cleanup
    delete_layer(layer_name)
    os.remove(tmp_create)
    os.remove(tmp_update)
