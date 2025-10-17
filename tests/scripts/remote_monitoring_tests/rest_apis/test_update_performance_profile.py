"""
Copyright (c) 2025 IBM Corporation and others.

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
import sys


sys.path.append("../../")

from helpers.fixtures import *
from helpers.utils import *

perf_profile_dir = get_metric_profile_dir()
mandatory_fields = [
    ("name", ERROR_STATUS_CODE, ERROR_STATUS),
    ("sloInfo", ERROR_STATUS_CODE, ERROR_STATUS),
    ("direction", ERROR_STATUS_CODE, ERROR_STATUS),
    ("objective_function", ERROR_STATUS_CODE, ERROR_STATUS),
    ("function_type", ERROR_STATUS_CODE, ERROR_STATUS),
    ("function_variables", ERROR_STATUS_CODE, ERROR_STATUS),
    ("metric_name", ERROR_STATUS_CODE, ERROR_STATUS),
    ("datasource", ERROR_STATUS_CODE, ERROR_STATUS),
    ("value_type", ERROR_STATUS_CODE, ERROR_STATUS),
    ("aggregation_functions", ERROR_STATUS_CODE, ERROR_STATUS),
    ("function", ERROR_STATUS_CODE, ERROR_STATUS),
    ("query", ERROR_STATUS_CODE, ERROR_STATUS)
]

@pytest.mark.perf_profile
def test_update_performance_profile(cluster_type):
    """
    Test Description: This test validates the response status code of updatePerformanceProfile API by passing a
    valid input for the json
    """
    # Form the kruize url
    form_kruize_url(cluster_type)
    perf_profile_json_file = "../json_files/resource_optimization_openshift_v1.json"
    # Delete any existing profile
    response = delete_performance_profile(perf_profile_json_file)
    print("delete performance profile = ", response.status_code)

    # Create the performance profile
    response = create_performance_profile(perf_profile_json_file)
    data = response.json()
    print(data['message'])

    with open(perf_profile_json_file, "r") as f:
        json_data = json.load(f)
    perf_profile_name = json_data["name"]

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert CREATE_PERF_PROFILE_SUCCESS_MSG % perf_profile_name in data['message']

    # Update the performance profile
    perf_profile_json_file = perf_profile_dir / 'resource_optimization_openshift.json'
    response = update_performance_profile(perf_profile_json_file)

    with open(perf_profile_json_file, "r") as f:
        json_data = json.load(f)
    perf_profile_name = json_data["name"]
    perf_profile_version_v2 = json_data["profile_version"]

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == UPDATE_PERF_PROFILE_SUCCESS_MSG % (perf_profile_name, perf_profile_version_v2)

    # Validate using listPerformanceProfile API
    response = list_performance_profiles()
    perf_profile_version = response.json()[0]["profile_version"]
    assert perf_profile_version == perf_profile_version_v2

    response = delete_performance_profile(perf_profile_json_file)
    print("delete performance profile = ", response.status_code)


@pytest.mark.perf_profile
def test_update_performance_profile_with_duplicate_data(cluster_type):
    """
    Test Description: This test validates the response message of updatePerformanceProfile API by passing the same data twice
    """
    # Form the kruize url
    form_kruize_url(cluster_type)
    perf_profile_json_file = "../json_files/resource_optimization_openshift_v1.json"
    # Delete any existing profile
    response = delete_performance_profile(perf_profile_json_file)
    print("delete performance profile = ", response.status_code)

    # Create the performance profile
    response = create_performance_profile(perf_profile_json_file)
    data = response.json()
    print(data['message'])

    with open(perf_profile_json_file, "r") as f:
        json_data = json.load(f)
    perf_profile_name = json_data["name"]

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert CREATE_PERF_PROFILE_SUCCESS_MSG % perf_profile_name in data['message']

    # Update the performance profile
    perf_profile_json_file = perf_profile_dir / 'resource_optimization_openshift.json'
    response = update_performance_profile(perf_profile_json_file)

    with open(perf_profile_json_file, "r") as f:
        json_data = json.load(f)
    perf_profile_name = json_data["name"]
    perf_profile_version_v2 = json_data["profile_version"]

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == UPDATE_PERF_PROFILE_SUCCESS_MSG % (perf_profile_name, perf_profile_version_v2)

    # Validate using listPerformanceProfile API
    response = list_performance_profiles()
    perf_profile_version = response.json()[0]["profile_version"]
    assert perf_profile_version == perf_profile_version_v2

    # Update the performance profile again
    perf_profile_json_file = perf_profile_dir / 'resource_optimization_openshift.json'
    response = update_performance_profile(perf_profile_json_file)
    data = response.json()
    print(data['message'])

    assert response.status_code == ERROR_409_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == UPDATE_PERF_PROFILE_ALREADY_UPDATED_MSG % (perf_profile_name, perf_profile_version_v2)

    response = delete_performance_profile(perf_profile_json_file)
    print("delete performance profile = ", response.status_code)


@pytest.mark.perf_profile
def test_update_performance_profile_with_missing_profile(cluster_type):
    """
    Test Description: This test validates the response message of updatePerformanceProfile API by passing the missing profile name
    """
    # Form the kruize url
    form_kruize_url(cluster_type)
    perf_profile_json_file = perf_profile_dir / 'resource_optimization_openshift.json'
    # Delete any existing profile
    response = delete_performance_profile(perf_profile_json_file)
    print("delete performance profile = ", response.status_code)

    # Update the performance profile
    response = update_performance_profile(perf_profile_json_file)
    data = response.json()
    print(data['message'])

    with open(perf_profile_json_file, "r") as f:
        json_data = json.load(f)
    perf_profile_name = json_data["name"]

    assert response.status_code == ERROR_404_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == UPDATE_PERF_PROFILE_MISSING_PROFILE_ERROR_MSG % perf_profile_name

    response = delete_performance_profile(perf_profile_json_file)
    print("delete performance profile = ", response.status_code)


@pytest.mark.perf_profile
def test_update_performance_profile_with_unsupported_version(cluster_type):
    """
    Test Description: This test validates the response message of updatePerformanceProfile API by specifying the
    incorrect version
    """
    # Form the kruize url
    form_kruize_url(cluster_type)
    perf_profile_json_file = "../json_files/resource_optimization_openshift_v1.json"
    # Delete any existing profile
    response = delete_performance_profile(perf_profile_json_file)
    print("delete performance profile = ", response.status_code)

    # create performance profile
    response = create_performance_profile(perf_profile_json_file)
    data = response.json()
    print(data['message'])

    with open(perf_profile_json_file, "r") as f:
        json_data = json.load(f)  # parses JSON once

    perf_profile_name = json_data["name"]
    perf_profile_version = json_data["profile_version"]

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert CREATE_PERF_PROFILE_SUCCESS_MSG % perf_profile_name in data['message']

    # Update the performance profile
    response = update_performance_profile(perf_profile_json_file)
    data = response.json()
    print(data['message'])

    assert response.status_code == ERROR_409_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == UPDATE_PERF_PROFILE_UNSUPPORTED_VERSION_ERROR_MSG % (perf_profile_version,
                                PERFORMANCE_PROFILE_SUPPORTED_VERSION, perf_profile_name, )

    response = delete_performance_profile(perf_profile_json_file)
    print("delete performance profile = ", response.status_code)


@pytest.mark.perf_profile
@pytest.mark.parametrize("field, expected_status_code, expected_status", mandatory_fields)
def test_update_performance_profiles_mandatory_fields(cluster_type, field, expected_status_code, expected_status):
    """
    Test Description: This test Validates error response of updatePerformanceProfile API when mandatory fields are missing.
    """

    # Form the kruize url
    form_kruize_url(cluster_type)
    input_json_file_v1 = "../json_files/resource_optimization_openshift_v1.json"
    input_json_file = perf_profile_dir / 'resource_optimization_openshift.json'
    # Delete any existing profile
    response = delete_performance_profile(input_json_file_v1)
    print("delete performance profile = ", response.status_code)
    # Create performance profile using the specified json
    response = create_performance_profile(input_json_file_v1)
    data = response.json()
    print(data['message'])

    json_file = "/tmp/create_performance_profile.json"
    json_data = json.load(open(input_json_file))

    if field == "name":
        json_data.pop("name", None)
    elif field == "sloInfo":
        json_data.pop("slo", None)
    elif field == "direction":
        json_data['slo'].pop("direction", None)
    elif field == "objective_function":
        json_data['slo'].pop("objective_function", None)
        field = "objectiveFunction"
    elif field == "function_type":
        json_data['slo']['objective_function'].pop("function_type", None)
    elif field == "function_variables":
        json_data['slo'].pop("function_variables", None)
        field = "functionVariables"
    elif field == "metric_name":
        json_data['slo']['function_variables'][0].pop("name", None)
        field = "name"
    elif field == "datasource":
        json_data['slo']['function_variables'][0].pop("datasource", None)
    elif field == "value_type":
        json_data['slo']['function_variables'][0].pop("value_type", None)
        field = "valueType"
    elif field == "aggregation_functions":
        json_data['slo']['function_variables'][0].pop("aggregation_functions", None)
    elif field == "function":
        json_data['slo']['function_variables'][0]['aggregation_functions'][0].pop("function", None)
    elif field == "query":
        json_data['slo']['function_variables'][0]['aggregation_functions'][0].pop("query", None)

    print("\n*****************************************")
    print(json_data)
    print("*****************************************\n")
    data = json.dumps(json_data)
    with open(json_file, 'w') as file:
        file.write(data)

    # Update the performance profile
    response = update_performance_profile(json_file)
    data = response.json()
    print(data['message'])

    assert response.status_code == expected_status_code, \
        f"Mandatory field check failed for {field} actual - {response.status_code} expected - {expected_status_code}"
    assert data['status'] == expected_status

    if field == "aggregation_functions":
        assert data['message'] == AGGR_FUNC_MISSING_MANDATORY_PARAMETERS_MSG
    else:
        assert data['message'] == CREATE_METRIC_PROFILE_MISSING_MANDATORY_PARAMETERS_MSG % field
