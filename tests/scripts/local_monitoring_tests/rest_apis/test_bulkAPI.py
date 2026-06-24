"""
Copyright (c) 2024 Red Hat, IBM Corporation and others.

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
import requests
import sys
import json

sys.path.append("../../")
from helpers.fixtures import *
from helpers.kruize import *
from helpers.utils import *
from helpers.list_metric_profiles_validate import *
from helpers.list_metric_profiles_without_parameters_schema import *
from helpers.list_metadata_profiles_validate import *
from helpers.list_metadata_profiles_schema import *

metric_profile_dir = get_metric_profile_dir()
metadata_profile_dir = get_metadata_profile_dir()

# Set up logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# Base valid payload generator
def base_payload():
    return {
        "filter": {
            "exclude": {"namespace": [], "workload": [], "containers": [], "labels": {}},
            "include": {"namespace": [], "workload": [], "containers": [], "labels": {}}
        },
        "metadata_profile": "cluster-metadata-local-monitoring",
        "measurement_duration": "15mins",
        "time_range": {}
    }

def filtered_payload():
    payload = base_payload()
    payload["filter"]["include"]["namespace"] = ["default"]
    payload["filter"]["include"]["workload"] = ["sysbench"]
    payload["filter"]["include"]["containers"] = ["sysbench"]
    return payload

@pytest.mark.test_bulk_api_ros
@pytest.mark.sanity
@pytest.mark.parametrize("bulk_request_payload, expected_job_id_present", [
    ({}, True),  # Test with an empty payload to check if a job_id is created.
    (base_payload(),True),  # Test with a sample payload with some JSON content
    (filtered_payload(), True)  # Test with payload with filters
])    
def test_bulk_post_request(cluster_type, bulk_request_payload, expected_job_id_present, caplog):
    form_kruize_url(cluster_type)
    URL = get_kruize_url()

    delete_and_create_metric_profile()

    # list and validate default metric profile
    metric_profile_input_json_file = metric_profile_dir / 'resource_optimization_local_monitoring.json'
    json_data = json.load(open(metric_profile_input_json_file))
    metric_profile_name = json_data['metadata']['name']

    response = list_metric_profiles(name=metric_profile_name, logging=False)
    metric_profile_json = response.json()

    assert response.status_code == SUCCESS_200_STATUS_CODE

    errorMsg = validate_list_metric_profiles_json(metric_profile_json, list_metric_profiles_schema)
    assert errorMsg == ""

    delete_and_create_metadata_profile()

    # list and validate default metadata profile
    metadata_profile_input_json_file = metadata_profile_dir / 'bulk_cluster_metadata_local_monitoring.json'
    json_data = json.load(open(metadata_profile_input_json_file))
    metadata_profile_name = json_data['metadata']['name']

    response = list_metadata_profiles(name=metadata_profile_name, logging=False)
    metadata_profile_json = response.json()

    assert response.status_code == SUCCESS_200_STATUS_CODE

    errorMsg = validate_list_metadata_profiles_json(metadata_profile_json, list_metadata_profiles_schema)
    assert errorMsg == ""

    with caplog.at_level(logging.INFO):
        # Log request payload and curl command for POST request
        response = post_bulk_api(bulk_request_payload, logging)

        # Check if job_id is present in the response
        job_id_present = "job_id" in response.json() and isinstance(response.json()["job_id"], str)
        assert job_id_present == expected_job_id_present, f"Expected job_id presence to be {expected_job_id_present} but was {job_id_present}"

        # If a job_id is generated, run the GET request test
        if job_id_present:
            validate_job_status(response.json()["job_id"], URL, caplog)


@pytest.mark.test_bulk_api_ros
@pytest.mark.parametrize("start, end, expected_error", [
    ("2025-01-01T12:00:00Z", "2025-01-02T12:00:00Z", "Valid"),               # Valid scenario
    ("", "", "Invalid date format. Must follow ISO 8601 format (YYYY-MM-DDTHH:mm:ss.sssZ) for the jobId:"), # empty
    ("2024-01-01 10:00:00", "2024-01-01T12:00:00Z", "Invalid date format. Must follow ISO 8601 format (YYYY-MM-DDTHH:mm:ss.sssZ) for the jobId:"),  # bad format
    ("2025-01-02T12:00:00Z", "2025-01-01T12:00:00Z", "Start time should be before end time for the jobId:"),  # start > end
    ])
def test_bulk_api_time_range_validation(cluster_type, start, end, expected_error, caplog):
    """
    Validates all negative time-range scenarios for Bulk API.
    """
    form_kruize_url(cluster_type)
    URL = get_kruize_url()
    payload = base_payload()

    payload["time_range"]["start"] = start
    payload["time_range"]["end"] = end

    delete_and_create_metric_profile()

    # list and validate default metric profile
    metric_profile_input_json_file = metric_profile_dir / 'resource_optimization_local_monitoring.json'
    json_data = json.load(open(metric_profile_input_json_file))
    metric_profile_name = json_data['metadata']['name']

    response = list_metric_profiles(name=metric_profile_name, logging=False)
    metric_profile_json = response.json()

    assert response.status_code == SUCCESS_200_STATUS_CODE

    errorMsg = validate_list_metric_profiles_json(metric_profile_json, list_metric_profiles_schema)
    assert errorMsg == ""

    delete_and_create_metadata_profile()

    # list and validate default metadata profile
    metadata_profile_input_json_file = metadata_profile_dir / 'bulk_cluster_metadata_local_monitoring.json'
    json_data = json.load(open(metadata_profile_input_json_file))
    metadata_profile_name = json_data['metadata']['name']

    response = list_metadata_profiles(name=metadata_profile_name, logging=False)
    metadata_profile_json = response.json()

    assert response.status_code == SUCCESS_200_STATUS_CODE

    errorMsg = validate_list_metadata_profiles_json(metadata_profile_json, list_metadata_profiles_schema)
    assert errorMsg == ""
    if expected_error == "Valid":
        expected_job_id_present = True
        with caplog.at_level(logging.INFO):
            # Log request payload and curl command for POST request
            response = post_bulk_api(payload, logging)

            # Check if job_id is present in the response
            job_id_present = "job_id" in response.json() and isinstance(response.json()["job_id"], str)
            assert job_id_present == expected_job_id_present, f"Expected job_id presence to be {expected_job_id_present} but was {job_id_present}"

            # If a job_id is generated, run the GET request test
            if job_id_present:
                validate_job_status(response.json()["job_id"], URL, caplog)
    else:
        response = post_bulk_api(payload, logging)
        print("Response:", response.json())
        assert response.status_code == ERROR_STATUS_CODE
        assert expected_error in response.json()["message"]


@pytest.mark.test_bulk_api_ros
def test_bulk_validate_datasource_missing(cluster_type):
    job_id = "job-missing-ds"
    ds_name = "ds-missing-test"
    form_kruize_url(cluster_type)
    URL = get_kruize_url()

    delete_and_create_metric_profile()

    # list and validate default metric profile
    metric_profile_input_json_file = metric_profile_dir / 'resource_optimization_local_monitoring.json'
    json_data = json.load(open(metric_profile_input_json_file))
    metric_profile_name = json_data['metadata']['name']

    response = list_metric_profiles(name=metric_profile_name, logging=False)
    metric_profile_json = response.json()

    assert response.status_code == SUCCESS_200_STATUS_CODE

    errorMsg = validate_list_metric_profiles_json(metric_profile_json, list_metric_profiles_schema)
    assert errorMsg == ""

    delete_and_create_metadata_profile()

    # list and validate default metadata profile
    metadata_profile_input_json_file = metadata_profile_dir / 'bulk_cluster_metadata_local_monitoring.json'
    json_data = json.load(open(metadata_profile_input_json_file))
    metadata_profile_name = json_data['metadata']['name']

    response = list_metadata_profiles(name=metadata_profile_name, logging=False)
    metadata_profile_json = response.json()

    assert response.status_code == SUCCESS_200_STATUS_CODE

    errorMsg = validate_list_metadata_profiles_json(metadata_profile_json, list_metadata_profiles_schema)
    assert errorMsg == ""

    # verify list does not contain it
    items = list_datasources().json()
    print("Items:", items)
    datasources = items.get("datasources", [])
    assert all(ds.get("name") != ds_name for ds in datasources), \
        f"Datasource with name '{ds_name}' already exists"

    # Build payload referencing the missing datasource
    payload = base_payload()
    payload["datasource"] = ds_name
    payload["time_range"]["start"] = "2025-01-01T00:00:00Z"
    payload["time_range"]["end"] = "2025-01-02T02:00:00Z"

    response = post_bulk_api(payload, logging)
    print("Response:", response.json())
    assert response.status_code == ERROR_STATUS_CODE
    assert DATASOURCE_NOT_SERVICEABLE in response.json()["message"]


@pytest.mark.test_bulk_api_ros
@pytest.mark.sanity
@pytest.mark.parametrize(
    "filter_setup, expected",
    [
        pytest.param(
            {
                "include": {
                    "namespace": ["default"],
                    "workload": ["wl1"],
                    "containers": ["ctr1"]
                }
            },
            {
                "namespace": ["default"],
                "workload": ["wl1"],
                "containers": ["ctr1"]
            },
            id="namespace_workload_container"
        ),
        pytest.param(
            {
                "include": {
                    "namespace": ["default"]
                }
            },
            {
                "namespace": ["default"]
            },
            id="namespace_only"
        ),
        pytest.param(
            {
                "include": {
                    "labels": {
                        "cost": "true"
                    }
                }
            },
            {
                "labels": {"cost": "true"},
                "mode": "include"
            },
            id="labels_include"
        ),
        pytest.param(
            {
                "exclude": {
                    "labels": {
                        "cost": "true"
                    }
                }
            },
            {
                "labels": {"cost": "true"},
                "mode": "exclude"
            },
            id="labels_exclude"
        ),
    ]
)
def test_bulk_api_filter_application(
    cluster_type,
    filter_setup,
    expected,
    caplog
):
    """
    Validate that filters applied in POST bulk API
    are correctly reflected in GET bulk API metadata response.
    """

    # ---------------- SETUP ----------------
    form_kruize_url(cluster_type)

    payload = base_payload()

    for filter_type, values in filter_setup.items():
        payload["filter"].setdefault(filter_type, {})
        payload["filter"][filter_type].update(values)

    delete_and_create_metric_profile()
    delete_and_create_metadata_profile()

    # ---------------- POST ----------------
    with caplog.at_level(logging.INFO):
        response = post_bulk_api(payload, logging)
        assert response.status_code == SUCCESS_200_STATUS_CODE

        job_id = response.json().get("job_id")
        assert job_id, "job_id not found in POST bulk API response"

    # ---------------- GET (polling) ----------------
    metadata = None
    for _ in range(10):
        get_response = get_bulk_job_status(
            job_id,
            include="metadata",
            logger=logging
        )
        assert get_response.status_code == SUCCESS_200_STATUS_CODE

        metadata = get_response.json().get("metadata", {})
        if metadata.get("datasources"):
            break

        time.sleep(5)

    # ---------------- VALIDATION ----------------

    datasources = metadata.get("datasources", {})

    # Label-based filters → empty metadata is VALID
    if "labels" in expected:
        assert isinstance(datasources, dict)
        return

    # For non-label filters, datasource must exist
    assert datasources, "Bulk job did not return metadata"

    for ds_data in datasources.values():
        clusters = ds_data.get("clusters", {})
        assert clusters, "No clusters found in metadata"

        for cluster_data in clusters.values():
            namespaces = cluster_data.get("namespaces", {})

            # Namespace filter validation
            if "namespace" in expected:
                assert list(namespaces.keys()) == expected["namespace"]

            for ns_data in namespaces.values():
                workloads = ns_data.get("workloads", {})

                if "workload" in expected:
                    assert set(workloads.keys()).issubset(
                        set(expected["workload"])
                    )

                for wl_data in workloads.values():
                    containers = wl_data.get("containers", {})

                    if "containers" in expected:
                        assert set(containers.keys()).issubset(
                            set(expected["containers"])
                        )


@pytest.mark.test_bulk_api_ros
@pytest.mark.parametrize("cluster_name, expected_status, expected_error", [
    ("prod-cluster", SUCCESS_200_STATUS_CODE, None),  # Valid cluster name
    ("cluster-01", SUCCESS_200_STATUS_CODE, None),  # Valid with numbers
    ("cluster.us-east.prod", SUCCESS_200_STATUS_CODE, None),  # Valid with dots
    ("  prod-cluster  ", SUCCESS_200_STATUS_CODE, None),  # Valid with whitespace (should be trimmed)
    ("", ERROR_STATUS_CODE, "cluster_name cannot be an empty string"),  # Empty string
    ("Cluster-A", ERROR_STATUS_CODE, "Invalid cluster_name format"),  # Uppercase
    ("-cluster", ERROR_STATUS_CODE, "Invalid cluster_name format"),  # Starts with hyphen
    ("cluster_name", ERROR_STATUS_CODE, "Invalid cluster_name format"),  # Underscore
    ("a" * 254, ERROR_STATUS_CODE, "cluster_name is too long"),  # Exceeds max length
])
def test_bulk_api_cluster_name_validation(cluster_type, cluster_name, expected_status, expected_error, caplog):
    """
    Validates cluster_name field validation in Bulk API.
    Tests valid formats, invalid formats, and edge cases.
    """
    form_kruize_url(cluster_type)
    
    payload = base_payload()
    payload["cluster_name"] = cluster_name
    payload["time_range"]["start"] = "2025-01-01T00:00:00Z"
    payload["time_range"]["end"] = "2025-01-02T00:00:00Z"
    
    delete_and_create_metric_profile()
    delete_and_create_metadata_profile()
    
    with caplog.at_level(logging.INFO):
        response = post_bulk_api(payload, logging)
        
        assert response.status_code == expected_status, \
            f"Expected status {expected_status} but got {response.status_code}. Response: {response.json()}"
        
        if expected_error:
            assert expected_error in response.json()["message"], \
                f"Expected error message to contain '{expected_error}' but got: {response.json()['message']}"
        else:
            # Valid cluster name should create a job
            assert "job_id" in response.json(), "Expected job_id in response for valid cluster_name"


@pytest.mark.test_bulk_api_ros
@pytest.mark.parametrize("model_settings, expected_status, expected_error", [
    ({"models": ["performance"]}, SUCCESS_200_STATUS_CODE, None),  # Valid single model
    ({"models": ["cost"]}, SUCCESS_200_STATUS_CODE, None),  # Valid cost model
    ({"models": ["performance", "cost"]}, SUCCESS_200_STATUS_CODE, None),  # Valid multiple models
    ({"models": ["Performance", "COST"]}, SUCCESS_200_STATUS_CODE, None),  # Valid case-insensitive
    ({"models": []}, ERROR_STATUS_CODE, "model_settings.models cannot be null or empty"),  # Empty list
    ({"models": ["invalid"]}, ERROR_STATUS_CODE, "Invalid model name"),  # Invalid model
    ({"models": ["performance", "invalid"]}, ERROR_STATUS_CODE, "Invalid model name"),  # Mixed valid/invalid
    ({}, ERROR_STATUS_CODE, "model_settings.models cannot be null or empty"),  # Missing models field
])
def test_bulk_api_model_settings_validation(cluster_type, model_settings, expected_status, expected_error, caplog):
    """
    Validates model_settings field validation in Bulk API.
    Tests valid models, invalid models, and edge cases.
    """
    form_kruize_url(cluster_type)
    
    payload = base_payload()
    payload["model_settings"] = model_settings
    payload["time_range"]["start"] = "2025-01-01T00:00:00Z"
    payload["time_range"]["end"] = "2025-01-02T00:00:00Z"
    
    delete_and_create_metric_profile()
    delete_and_create_metadata_profile()
    
    with caplog.at_level(logging.INFO):
        response = post_bulk_api(payload, logging)
        
        assert response.status_code == expected_status, \
            f"Expected status {expected_status} but got {response.status_code}. Response: {response.json()}"
        
        if expected_error:
            assert expected_error in response.json()["message"], \
                f"Expected error message to contain '{expected_error}' but got: {response.json()['message']}"
        else:
            # Valid model settings should create a job
            assert "job_id" in response.json(), "Expected job_id in response for valid model_settings"


@pytest.mark.test_bulk_api_ros
@pytest.mark.parametrize("term_settings, expected_status, expected_error", [
    ({"terms": ["short"]}, SUCCESS_200_STATUS_CODE, None),  # Valid single term
    ({"terms": ["medium"]}, SUCCESS_200_STATUS_CODE, None),  # Valid medium term
    ({"terms": ["long"]}, SUCCESS_200_STATUS_CODE, None),  # Valid long term
    ({"terms": ["short", "long"]}, SUCCESS_200_STATUS_CODE, None),  # Valid multiple terms
    ({"terms": ["Short", "LONG"]}, SUCCESS_200_STATUS_CODE, None),  # Valid case-insensitive
    ({"terms": ["short", "medium", "long"]}, SUCCESS_200_STATUS_CODE, None),  # All terms
    ({"terms": []}, ERROR_STATUS_CODE, "term_settings.terms cannot be null or empty"),  # Empty list
    ({"terms": ["invalid"]}, ERROR_STATUS_CODE, "Invalid term name"),  # Invalid term
    ({"terms": ["short", "invalid"]}, ERROR_STATUS_CODE, "Invalid term name"),  # Mixed valid/invalid
    ({}, ERROR_STATUS_CODE, "term_settings.terms cannot be null or empty"),  # Missing terms field
])
def test_bulk_api_term_settings_validation(cluster_type, term_settings, expected_status, expected_error, caplog):
    """
    Validates term_settings field validation in Bulk API.
    Tests valid terms, invalid terms, and edge cases.
    """
    form_kruize_url(cluster_type)
    
    payload = base_payload()
    payload["term_settings"] = term_settings
    payload["time_range"]["start"] = "2025-01-01T00:00:00Z"
    payload["time_range"]["end"] = "2025-01-02T00:00:00Z"
    
    delete_and_create_metric_profile()
    delete_and_create_metadata_profile()
    
    with caplog.at_level(logging.INFO):
        response = post_bulk_api(payload, logging)
        
        assert response.status_code == expected_status, \
            f"Expected status {expected_status} but got {response.status_code}. Response: {response.json()}"
        
        if expected_error:
            assert expected_error in response.json()["message"], \
                f"Expected error message to contain '{expected_error}' but got: {response.json()['message']}"
        else:
            # Valid term settings should create a job
            assert "job_id" in response.json(), "Expected job_id in response for valid term_settings"


@pytest.mark.test_bulk_api_ros
@pytest.mark.sanity
def test_bulk_api_combined_custom_settings(cluster_type, caplog):
    """
    Validates that cluster_name, model_settings, and term_settings
    can be used together in a single bulk API request.
    This simulates the IRI use case.
    """
    form_kruize_url(cluster_type)
    URL = get_kruize_url()
    
    payload = base_payload()
    payload["cluster_name"] = "iri-prod-cluster"
    payload["model_settings"] = {"models": ["performance"]}
    payload["term_settings"] = {"terms": ["long"]}
    payload["time_range"]["start"] = "2025-01-01T00:00:00Z"
    payload["time_range"]["end"] = "2025-01-02T00:00:00Z"
    
    delete_and_create_metric_profile()
    delete_and_create_metadata_profile()
    
    with caplog.at_level(logging.INFO):
        response = post_bulk_api(payload, logging)
        
        assert response.status_code == SUCCESS_200_STATUS_CODE, \
            f"Expected success but got {response.status_code}. Response: {response.json()}"
        
        assert "job_id" in response.json(), "Expected job_id in response"
        job_id = response.json()["job_id"]
        
        # Validate job status
        validate_job_status(job_id, URL, caplog)


@pytest.mark.test_bulk_api_ros
def test_bulk_api_backward_compatibility(cluster_type, caplog):
    """
    Validates that bulk API works without new fields (backward compatibility).
    Ensures existing workflows are not broken.
    """
    form_kruize_url(cluster_type)
    URL = get_kruize_url()
    
    # Use base payload without cluster_name, model_settings, or term_settings
    payload = base_payload()
    payload["time_range"]["start"] = "2025-01-01T00:00:00Z"
    payload["time_range"]["end"] = "2025-01-02T00:00:00Z"
    
    delete_and_create_metric_profile()
    delete_and_create_metadata_profile()
    
    with caplog.at_level(logging.INFO):
        response = post_bulk_api(payload, logging)
        
        assert response.status_code == SUCCESS_200_STATUS_CODE, \
            f"Backward compatibility test failed. Status: {response.status_code}, Response: {response.json()}"
        
        assert "job_id" in response.json(), "Expected job_id in response for backward compatible payload"
        job_id = response.json()["job_id"]
        
        # Validate job status
        validate_job_status(job_id, URL, caplog)
