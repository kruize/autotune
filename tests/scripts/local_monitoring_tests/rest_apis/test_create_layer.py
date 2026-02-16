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
from jinja2 import Environment, FileSystemLoader

sys.path.append("../../")

from helpers.fixtures import *
from helpers.kruize import *
from helpers.utils import *

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
@pytest.mark.sanity
@pytest.mark.parametrize("layer_file", [
    pytest.param("container-config.json", id="bounded_tunables"),
    pytest.param("semeru-actuator-config.json", id="mixed_tunables"),
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


@pytest.mark.layers
@pytest.mark.sanity
@pytest.mark.parametrize("layer_file", [
    pytest.param("container-config.json", id="presence_always"),
    pytest.param("semeru-actuator-config.json", id="presence_queries"),
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


@pytest.mark.layers
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

    # Cleanup before test to ensure clean state
    delete_layer_from_db(layer_name)

    response = create_layer(input_json_file)
    data = response.json()

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_LAYER_SUCCESS_MSG % layer_name

    print(f"✓ Layer '{layer_name}' created successfully with minimum required fields")

    # Cleanup: Delete layer from database
    delete_layer_from_db(layer_name)


# =============================================================================
# NEGATIVE TEST CASES - A. Mandatory Fields Missing/NULL/Empty
# =============================================================================
# This section tests that the createLayer API properly validates and rejects
# requests where mandatory fields are missing, null, or empty. Tests include:
# - metadata.name: null or empty string
# - layer_name: null or empty string
# - layer_presence: null value
# - tunables: null or empty array
# Expected: API returns 400 status with specific error messages for each case

@pytest.mark.layers
@pytest.mark.negative
@pytest.mark.parametrize("test_name, expected_error_msg, apiVersion, kind, metadata_name, layer_name, layer_level, details, layer_presence, tunables", [
    ("null_metadata_name", LAYER_METADATA_NAME_NULL_JSON_MSG, "recommender.com/v1", "KruizeLayer", None, "test-layer", 1, "test layer", '{"presence": "always"}', '[{"name": "t1", "value_type": "double", "upper_bound": "100", "lower_bound": "10", "step": 1}]'),
    ("empty_metadata_name", LAYER_METADATA_NAME_EMPTY_MSG, "recommender.com/v1", "KruizeLayer", "", "test-layer", 1, "test layer", '{"presence": "always"}', '[{"name": "t1", "value_type": "double", "upper_bound": "100", "lower_bound": "10", "step": 1}]'),
    ("null_layer_name", LAYER_NAME_NULL_JSON_MSG, "recommender.com/v1", "KruizeLayer", "test-meta", None, 1, "test layer", '{"presence": "always"}', '[{"name": "t1", "value_type": "double", "upper_bound": "100", "lower_bound": "10", "step": 1}]'),
    ("empty_layer_name", LAYER_NAME_EMPTY_MSG, "recommender.com/v1", "KruizeLayer", "test-meta", "", 1, "test layer", '{"presence": "always"}', '[{"name": "t1", "value_type": "double", "upper_bound": "100", "lower_bound": "10", "step": 1}]'),
    ("null_layer_presence", LAYER_PRESENCE_NULL_JSON_MSG, "recommender.com/v1", "KruizeLayer", "test-meta", "test-layer", 1, "test layer", 'null', '[{"name": "t1", "value_type": "double", "upper_bound": "100", "lower_bound": "10", "step": 1}]'),
    ("null_tunables", LAYER_TUNABLES_NULL_JSON_MSG, "recommender.com/v1", "KruizeLayer", "test-meta", "test-layer", 1, "test layer", '{"presence": "always"}', 'null'),
    ("empty_tunables_array", LAYER_TUNABLES_EMPTY_MSG, "recommender.com/v1", "KruizeLayer", "test-meta", "test-layer", 1, "test layer", '{"presence": "always"}', '[]'),
])
def test_create_layer_mandatory_fields_validation(test_name, expected_error_msg, apiVersion, kind, metadata_name, layer_name, layer_level, details, layer_presence, tunables, cluster_type, cleanup_test_layers):
    """
    Test Description: Validates createLayer API rejects requests with missing/null/empty mandatory fields
    """
    form_kruize_url(cluster_type)

    tmp_json_file = f"/tmp/create_layer_{test_name}.json"
    environment = Environment(loader=FileSystemLoader("../json_files/"))
    template = environment.get_template("create_layer_template.json")

    content = template.render(
        apiVersion=apiVersion,
        kind=kind,
        metadata_name=metadata_name,
        layer_name=layer_name,
        layer_level=layer_level,
        details=details,
        layer_presence=layer_presence,
        tunables=tunables
    )

    try:
        with open(tmp_json_file, "w") as f:
            f.write(content)

        response = create_layer(tmp_json_file)
        data = response.json()

        # Validate HTTP status code
        assert response.status_code == ERROR_STATUS_CODE, f"Expected status {ERROR_STATUS_CODE}, got {response.status_code}"

        # Validate exact error message
        assert 'message' in data, "Response missing 'message' field"
        assert data['message'] == expected_error_msg, \
            f"Expected exact error message '{expected_error_msg}', got '{data['message']}'"

        # Validate response structure
        assert 'httpcode' in data, "Response missing 'httpcode' field"
        assert data['httpcode'] == ERROR_STATUS_CODE, f"Expected httpcode {ERROR_STATUS_CODE}, got {data['httpcode']}"

        print(f"✓ Correctly rejected: {test_name}")
    finally:
        # Cleanup temporary file
        if os.path.exists(tmp_json_file):
            os.remove(tmp_json_file)


# =============================================================================
# NEGATIVE TEST CASES - B. Invalid/Negative/Duplicate Values
# =============================================================================
# This section tests that the createLayer API validates field values and rejects
# invalid or duplicate values. Tests include:
# - tunables: duplicate tunable names within the same layer
# - duplicate layer_name: attempting to create a layer with an existing name (409)
# Expected: API returns 400 status (or 409 for duplicates) with specific error messages

@pytest.mark.layers
@pytest.mark.negative
@pytest.mark.parametrize("test_name, expected_error_msg, apiVersion, kind, metadata_name, layer_name, layer_level, details, layer_presence, tunables", [
    ("duplicate_tunable_names", LAYER_DUPLICATE_TUNABLE_NAMES_MSG % 'duplicate', "recommender.com/v1", "KruizeLayer", "test-meta", "test-layer", 1, "test layer", '{"presence": "always"}', '[{"name": "duplicate", "value_type": "double", "upper_bound": "100", "lower_bound": "10", "step": 1}, {"name": "duplicate", "value_type": "double", "upper_bound": "50", "lower_bound": "5", "step": 1}]'),
])
def test_create_layer_invalid_values(test_name, expected_error_msg, apiVersion, kind, metadata_name, layer_name, layer_level, details, layer_presence, tunables, cluster_type, cleanup_test_layers):
    """
    Test Description: Validates createLayer API rejects requests with invalid/negative/duplicate values
    """
    form_kruize_url(cluster_type)

    tmp_json_file = f"/tmp/create_layer_{test_name}.json"
    environment = Environment(loader=FileSystemLoader("../json_files/"))
    template = environment.get_template("create_layer_template.json")

    content = template.render(
        apiVersion=apiVersion,
        kind=kind,
        metadata_name=metadata_name,
        layer_name=layer_name,
        layer_level=layer_level,
        details=details,
        layer_presence=layer_presence,
        tunables=tunables
    )

    try:
        with open(tmp_json_file, "w") as f:
            f.write(content)

        response = create_layer(tmp_json_file)
        data = response.json()

        # Validate HTTP status code
        assert response.status_code == ERROR_STATUS_CODE, f"Expected status {ERROR_STATUS_CODE}, got {response.status_code}"

        # Validate exact error message
        assert 'message' in data, "Response missing 'message' field"
        assert data['message'] == expected_error_msg, \
            f"Expected exact error message '{expected_error_msg}', got '{data['message']}'"

        # Validate response structure
        assert 'httpcode' in data, "Response missing 'httpcode' field"
        assert data['httpcode'] == ERROR_STATUS_CODE, f"Expected httpcode {ERROR_STATUS_CODE}, got {data['httpcode']}"

        print(f"✓ Correctly rejected: {test_name}")
    finally:
        # Cleanup temporary file
        if os.path.exists(tmp_json_file):
            os.remove(tmp_json_file)


@pytest.mark.layers
@pytest.mark.negative
def test_create_layer_duplicate_layer_name(cluster_type):
    """
    Test Description: Validates createLayer API rejects duplicate layer names
    """
    form_kruize_url(cluster_type)

    input_json_file = layer_dir / 'container-config.json'

    with open(input_json_file, "r") as json_file:
        input_json = json.load(json_file)
        layer_name = input_json['layer_name']

    # Cleanup before test to ensure clean state
    delete_layer_from_db(layer_name)

    response1 = create_layer(input_json_file)

    assert response1.status_code == SUCCESS_STATUS_CODE

    # Try to create the same layer again
    response2 = create_layer(input_json_file)
    data2 = response2.json()

    assert response2.status_code == ERROR_409_STATUS_CODE
    assert data2['message'] == LAYER_DUPLICATE_MSG % layer_name
    print(f"✓ Correctly rejected duplicate layer: {layer_name}")

    # Cleanup: Delete the layer that was successfully created
    delete_layer_from_db(layer_name)


# =============================================================================
# NEGATIVE TEST CASES - C. Wrong layer_presence Combinations
# =============================================================================
# This section tests that layer_presence configuration is properly validated.
# A layer must specify exactly ONE detection method. Tests include:
# - Empty layer_presence object (no detection method specified)
# - Multiple detection methods: presence + queries
# - Multiple detection methods: presence + label
# - Multiple detection methods: queries + label
# - All three detection methods: presence + queries + label
# Expected: API returns 400 status indicating only one presence type is allowed

@pytest.mark.layers
@pytest.mark.negative
@pytest.mark.parametrize("test_name, expected_error_msg, apiVersion, kind, metadata_name, layer_name, layer_level, details, layer_presence, tunables", [
    ("empty_layer_presence", LAYER_PRESENCE_MISSING_MSG, "recommender.com/v1", "KruizeLayer", "test-meta", "test-layer", 1, "test layer", '{}', '[{"name": "t1", "value_type": "double", "upper_bound": "100", "lower_bound": "10", "step": 1}]'),
    ("presence_and_queries", LAYER_PRESENCE_MULTIPLE_TYPES_MSG, "recommender.com/v1", "KruizeLayer", "test-meta", "test-layer", 1, "test layer", '{"presence": "always", "queries": [{"datasource": "prometheus", "query": "test"}]}', '[{"name": "t1", "value_type": "double", "upper_bound": "100", "lower_bound": "10", "step": 1}]'),
    ("presence_and_label", LAYER_PRESENCE_MULTIPLE_TYPES_MSG, "recommender.com/v1", "KruizeLayer", "test-meta", "test-layer", 1, "test layer", '{"presence": "always", "label": [{"name": "test", "value": "test"}]}', '[{"name": "t1", "value_type": "double", "upper_bound": "100", "lower_bound": "10", "step": 1}]'),
    ("queries_and_label", LAYER_PRESENCE_MULTIPLE_TYPES_MSG, "recommender.com/v1", "KruizeLayer", "test-meta", "test-layer", 1, "test layer", '{"queries": [{"datasource": "prometheus", "query": "test"}], "label": [{"name": "test", "value": "test"}]}', '[{"name": "t1", "value_type": "double", "upper_bound": "100", "lower_bound": "10", "step": 1}]'),
    ("all_three_types", LAYER_PRESENCE_MULTIPLE_TYPES_MSG, "recommender.com/v1", "KruizeLayer", "test-meta", "test-layer", 1, "test layer", '{"presence": "always", "queries": [{"datasource": "prometheus", "query": "test"}], "label": [{"name": "test", "value": "test"}]}', '[{"name": "t1", "value_type": "double", "upper_bound": "100", "lower_bound": "10", "step": 1}]'),
])
def test_create_layer_presence_combinations(test_name, expected_error_msg, apiVersion, kind, metadata_name, layer_name, layer_level, details, layer_presence, tunables, cluster_type, cleanup_test_layers):
    """
    Test Description: Validates createLayer API rejects invalid layer_presence combinations
    """
    form_kruize_url(cluster_type)

    tmp_json_file = f"/tmp/create_layer_{test_name}.json"
    environment = Environment(loader=FileSystemLoader("../json_files/"))
    template = environment.get_template("create_layer_template.json")

    content = template.render(
        apiVersion=apiVersion,
        kind=kind,
        metadata_name=metadata_name,
        layer_name=layer_name,
        layer_level=layer_level,
        details=details,
        layer_presence=layer_presence,
        tunables=tunables
    )

    try:
        with open(tmp_json_file, "w") as f:
            f.write(content)

        response = create_layer(tmp_json_file)
        data = response.json()

        # Validate HTTP status code
        assert response.status_code == ERROR_STATUS_CODE, f"Expected status {ERROR_STATUS_CODE}, got {response.status_code}"

        # Validate exact error message
        assert 'message' in data, "Response missing 'message' field"
        assert data['message'] == expected_error_msg, \
            f"Expected exact error message '{expected_error_msg}', got '{data['message']}'"

        # Validate response structure
        assert 'httpcode' in data, "Response missing 'httpcode' field"
        assert data['httpcode'] == ERROR_STATUS_CODE, f"Expected httpcode {ERROR_STATUS_CODE}, got {data['httpcode']}"

        print(f"✓ Correctly rejected: {test_name}")
    finally:
        # Cleanup temporary file
        if os.path.exists(tmp_json_file):
            os.remove(tmp_json_file)


# =============================================================================
# NEGATIVE TEST CASES - D. Tunable Bounds/Step Validation
# =============================================================================
# This section tests validation of numeric tunable bounds and step values.
# For numeric tunables (double, int), bounds and step must be valid. Tests include:
# - Null bounds: upper_bound or lower_bound is null
# - Non-numeric bounds: upper_bound or lower_bound contains non-numeric strings
# - Null or invalid step: step is null, zero, or negative
# - Negative bounds: upper_bound or lower_bound is negative
# - Invalid range: lower_bound >= upper_bound
# - Invalid step: step is greater than the range (upper_bound - lower_bound)
# Expected: API returns 400 status with specific validation error messages

@pytest.mark.layers
@pytest.mark.negative
@pytest.mark.parametrize("test_name, expected_error_msg, apiVersion, kind, metadata_name, layer_name, layer_level, details, layer_presence, tunables", [
    ("tunable_null_upper_bound", TUNABLE_NULL_BOUNDS_MSG % 't1', "recommender.com/v1", "KruizeLayer", "test-meta", "test-layer", 1, "test layer", '{"presence": "always"}', '[{"name": "t1", "value_type": "double", "upper_bound": null, "lower_bound": "10", "step": 1}]'),
    ("tunable_null_lower_bound", TUNABLE_NULL_BOUNDS_MSG % 't1', "recommender.com/v1", "KruizeLayer", "test-meta", "test-layer", 1, "test layer", '{"presence": "always"}', '[{"name": "t1", "value_type": "double", "upper_bound": "100", "lower_bound": null, "step": 1}]'),
    ("tunable_non_numeric_upper_bound", "Validation failed: ERROR: Tunable: Tunable 't1' has non-numeric bounds: upper=abc, lower=10", "recommender.com/v1", "KruizeLayer", "test-meta", "test-layer", 1, "test layer", '{"presence": "always"}', '[{"name": "t1", "value_type": "double", "upper_bound": "abc", "lower_bound": "10", "step": 1}]'),
    ("tunable_non_numeric_lower_bound", "Validation failed: ERROR: Tunable: Tunable 't1' has non-numeric bounds: upper=100, lower=xyz", "recommender.com/v1", "KruizeLayer", "test-meta", "test-layer", 1, "test layer", '{"presence": "always"}', '[{"name": "t1", "value_type": "double", "upper_bound": "100", "lower_bound": "xyz", "step": 1}]'),
    ("tunable_null_step", TUNABLE_NULL_STEP_MSG % 't1', "recommender.com/v1", "KruizeLayer", "test-meta", "test-layer", 1, "test layer", '{"presence": "always"}', '[{"name": "t1", "value_type": "double", "upper_bound": "100", "lower_bound": "10", "step": null}]'),
    ("tunable_zero_step", "Validation failed: ERROR: Tunable: Tunable 't1' has invalid step; step must be > 0, got: 0.0", "recommender.com/v1", "KruizeLayer", "test-meta", "test-layer", 1, "test layer", '{"presence": "always"}', '[{"name": "t1", "value_type": "double", "upper_bound": "100", "lower_bound": "10", "step": 0}]'),
    ("tunable_negative_step", "Validation failed: ERROR: Tunable: Tunable 't1' has invalid step; step must be > 0, got: -5.0", "recommender.com/v1", "KruizeLayer", "test-meta", "test-layer", 1, "test layer", '{"presence": "always"}', '[{"name": "t1", "value_type": "double", "upper_bound": "100", "lower_bound": "10", "step": -5}]'),
    ("tunable_negative_upper_bound", "Validation failed: ERROR: Tunable: Tunable 't1' has negative bounds; upperBound: -100.0 lowerBound: 10.0", "recommender.com/v1", "KruizeLayer", "test-meta", "test-layer", 1, "test layer", '{"presence": "always"}', '[{"name": "t1", "value_type": "double", "upper_bound": "-100", "lower_bound": "10", "step": 1}]'),
    ("tunable_negative_lower_bound", "Validation failed: ERROR: Tunable: Tunable 't1' has negative bounds; upperBound: 100.0 lowerBound: -50.0", "recommender.com/v1", "KruizeLayer", "test-meta", "test-layer", 1, "test layer", '{"presence": "always"}', '[{"name": "t1", "value_type": "double", "upper_bound": "100", "lower_bound": "-50", "step": 1}]'),
    ("tunable_lower_gte_upper", "Validation failed: ERROR: Tunable: Tunable 't1' has invalid bounds; lowerBound (100.0) must be less than upperBound (50.0)", "recommender.com/v1", "KruizeLayer", "test-meta", "test-layer", 1, "test layer", '{"presence": "always"}', '[{"name": "t1", "value_type": "double", "upper_bound": "50", "lower_bound": "100", "step": 1}]'),
    ("tunable_step_greater_than_range", "Validation failed: ERROR: Tunable: Tunable 't1' has invalid step; step (100.0) must be <= (upperBound - lowerBound) (90.0)", "recommender.com/v1", "KruizeLayer", "test-meta", "test-layer", 1, "test layer", '{"presence": "always"}', '[{"name": "t1", "value_type": "double", "upper_bound": "100", "lower_bound": "10", "step": 100}]'),
])
def test_create_layer_tunable_bounds_validation(test_name, expected_error_msg, apiVersion, kind, metadata_name, layer_name, layer_level, details, layer_presence, tunables, cluster_type, cleanup_test_layers):
    """
    Test Description: Validates createLayer API rejects invalid tunable bounds/step configurations
    """
    form_kruize_url(cluster_type)

    tmp_json_file = f"/tmp/create_layer_{test_name}.json"
    environment = Environment(loader=FileSystemLoader("../json_files/"))
    template = environment.get_template("create_layer_template.json")

    content = template.render(
        apiVersion=apiVersion,
        kind=kind,
        metadata_name=metadata_name,
        layer_name=layer_name,
        layer_level=layer_level,
        details=details,
        layer_presence=layer_presence,
        tunables=tunables
    )

    try:
        with open(tmp_json_file, "w") as f:
            f.write(content)

        response = create_layer(tmp_json_file)
        data = response.json()

        # Validate HTTP status code
        assert response.status_code == ERROR_STATUS_CODE, f"Expected status {ERROR_STATUS_CODE}, got {response.status_code}"

        # Validate exact error message
        assert 'message' in data, "Response missing 'message' field"
        assert data['message'] == expected_error_msg, \
            f"Expected exact error message '{expected_error_msg}', got '{data['message']}'"

        # Validate response structure
        assert 'httpcode' in data, "Response missing 'httpcode' field"
        assert data['httpcode'] == ERROR_STATUS_CODE, f"Expected httpcode {ERROR_STATUS_CODE}, got {data['httpcode']}"

        print(f"✓ Correctly rejected: {test_name}")
    finally:
        # Cleanup temporary file
        if os.path.exists(tmp_json_file):
            os.remove(tmp_json_file)


# =============================================================================
# NEGATIVE TEST CASES - E. Categorical Tunable Validation
# =============================================================================
# This section tests validation specific to categorical tunables.
# Categorical tunables must have choices and cannot have numeric bounds. Tests include:
# - Null choices: choices field is null
# - Empty choices: choices array is empty
# - Mixed configuration: categorical tunable has both choices and numeric bounds/step
# Expected: API returns 400 status indicating categorical tunables must have
# non-empty choices and cannot mix with bounds/step configuration

@pytest.mark.layers
@pytest.mark.negative
@pytest.mark.parametrize("test_name, expected_error_msg, apiVersion, kind, metadata_name, layer_name, layer_level, details, layer_presence, tunables", [
    ("categorical_null_choices", TUNABLE_MISSING_CONFIG_MSG % 't1', "recommender.com/v1", "KruizeLayer", "test-meta", "test-layer", 1, "test layer", '{"presence": "always"}', '[{"name": "t1", "value_type": "categorical", "choices": null}]'),
    ("categorical_empty_choices", TUNABLE_MISSING_CONFIG_MSG % 't1', "recommender.com/v1", "KruizeLayer", "test-meta", "test-layer", 1, "test layer", '{"presence": "always"}', '[{"name": "t1", "value_type": "categorical", "choices": []}]'),
    ("categorical_with_bounds", TUNABLE_MIXED_CONFIG_MSG % 't1', "recommender.com/v1", "KruizeLayer", "test-meta", "test-layer", 1, "test layer", '{"presence": "always"}', '[{"name": "t1", "value_type": "categorical", "choices": ["opt1", "opt2"], "upper_bound": "100", "lower_bound": "10", "step": 5}]'),
])
def test_create_layer_categorical_tunable_validation(test_name, expected_error_msg, apiVersion, kind, metadata_name, layer_name, layer_level, details, layer_presence, tunables, cluster_type, cleanup_test_layers):
    """
    Test Description: Validates createLayer API rejects invalid categorical tunable configurations
    """
    form_kruize_url(cluster_type)

    tmp_json_file = f"/tmp/create_layer_{test_name}.json"
    environment = Environment(loader=FileSystemLoader("../json_files/"))
    template = environment.get_template("create_layer_template.json")

    content = template.render(
        apiVersion=apiVersion,
        kind=kind,
        metadata_name=metadata_name,
        layer_name=layer_name,
        layer_level=layer_level,
        details=details,
        layer_presence=layer_presence,
        tunables=tunables
    )

    try:
        with open(tmp_json_file, "w") as f:
            f.write(content)

        response = create_layer(tmp_json_file)
        data = response.json()

        # Validate HTTP status code
        assert response.status_code == ERROR_STATUS_CODE, f"Expected status {ERROR_STATUS_CODE}, got {response.status_code}"

        # Validate exact error message
        assert 'message' in data, "Response missing 'message' field"
        assert data['message'] == expected_error_msg, \
            f"Expected exact error message '{expected_error_msg}', got '{data['message']}'"

        # Validate response structure
        assert 'httpcode' in data, "Response missing 'httpcode' field"
        assert data['httpcode'] == ERROR_STATUS_CODE, f"Expected httpcode {ERROR_STATUS_CODE}, got {data['httpcode']}"

        print(f"✓ Correctly rejected: {test_name}")
    finally:
        # Cleanup temporary file
        if os.path.exists(tmp_json_file):
            os.remove(tmp_json_file)
