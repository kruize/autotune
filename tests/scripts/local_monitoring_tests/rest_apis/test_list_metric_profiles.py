"""
Copyright (c) 2024, 2024 Red Hat, IBM Corporation and others.

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
from helpers.list_metric_profiles_validate import *
from helpers.list_metric_profiles_without_parameters_schema import *

metric_profile_dir = get_metric_profile_dir()

@pytest.mark.sanity
def test_list_metric_profiles_with_name(cluster_type):
    """
    Test Description: This test validates the response status code of listMetricProfiles API by validating the output
    JSON response by passing metric profile 'name' query parameter
    """
    input_json_file = metric_profile_dir / 'resource_optimization_local_monitoring.json'
    form_kruize_url(cluster_type)

    response = delete_metric_profile(input_json_file)
    print("delete metric profile = ", response.status_code)

    # Create metric profile using the specified json
    response = create_metric_profile(input_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS

    json_file = open(input_json_file, "r")
    input_json = json.loads(json_file.read())
    metric_profile_name = input_json['metadata']['name']
    assert data['message'] == CREATE_METRIC_PROFILE_SUCCESS_MSG % metric_profile_name

    response = list_metric_profiles(metric_profile_name)
    list_metric_profiles_json = response.json()

    # Validate the json against the json schema
    errorMsg = validate_list_metric_profiles_json(list_metric_profiles_json, list_metric_profiles_schema)
    assert errorMsg == ""


    response = delete_metric_profile(input_json_file)
    print("delete metric profile = ", response.status_code)


@pytest.mark.sanity
def test_list_metric_profiles_without_parameters(cluster_type):
    """
    Test Description: This test validates the response status code of listMetricProfiles API by validating the output
    JSON response without any parameters - expected output is listing all the metric profile names
    """
    input_json_file = metric_profile_dir / 'resource_optimization_local_monitoring.json'
    form_kruize_url(cluster_type)

    response = delete_metric_profile(input_json_file)
    print("delete metric profile = ", response.status_code)

    # Create metric profile using the specified json
    response = create_metric_profile(input_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS

    json_file = open(input_json_file, "r")
    input_json = json.loads(json_file.read())
    metric_profile_name = input_json['metadata']['name']
    assert data['message'] == CREATE_METRIC_PROFILE_SUCCESS_MSG % metric_profile_name

    response = list_metric_profiles()
    list_metric_profiles_json = response.json()

    # Validate the json against the json schema
    errorMsg = validate_list_metric_profiles_json(list_metric_profiles_json, list_metric_profiles_without_parameters_schema)
    assert errorMsg == ""


    response = delete_metric_profile(input_json_file)
    print("delete metric profile = ", response.status_code)


@pytest.mark.negative
def test_list_metric_profiles_without_creating_profile(cluster_type):
    """
    Test Description: This test validates the response status code of listMetricProfiles API by validating the output
    JSON response without creating metric profile - expected output is an error message
    """
    input_json_file = metric_profile_dir / 'resource_optimization_local_monitoring.json'
    form_kruize_url(cluster_type)

    response = delete_metric_profile(input_json_file)
    print("delete metric profile = ", response.status_code)


    json_file = open(input_json_file, "r")
    input_json = json.loads(json_file.read())
    metric_profile_name = input_json['metadata']['name']

    response = list_metric_profiles(metric_profile_name)
    data = response.json()

    assert response.status_code == ERROR_STATUS_CODE
    assert data['message'] == LIST_METRIC_PROFILES_INVALID_NAME % metric_profile_name


    response = delete_metric_profile(input_json_file)
    print("delete metric profile = ", response.status_code)


@pytest.mark.negative
@pytest.mark.parametrize("test_name, expected_status_code, name",
                         [
                             ("blank_name", 400, ""),
                             ("null_name", 400, "null"),
                             ("invalid_name", 400, "xyz")
                         ]
                         )
def test_list_metric_profiles_invalid_name(test_name, expected_status_code, name, cluster_type):
    """
    Test Description: This test validates the response status code of listMetricProfiles API by validating the output
    JSON response by passing invalid query parameter 'name' - expected output is an error message
    """
    input_json_file = metric_profile_dir / 'resource_optimization_local_monitoring.json'
    form_kruize_url(cluster_type)

    response = delete_metric_profile(input_json_file)
    print("delete metric profile = ", response.status_code)

    # Create metric profile using the specified json
    response = create_metric_profile(input_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS

    json_file = open(input_json_file, "r")
    input_json = json.loads(json_file.read())
    metric_profile_name = input_json['metadata']['name']
    assert data['message'] == CREATE_METRIC_PROFILE_SUCCESS_MSG % metric_profile_name

    response = list_metric_profiles(name=name)
    data = response.json()

    assert response.status_code == ERROR_STATUS_CODE
    assert data['message'] == LIST_METRIC_PROFILES_INVALID_NAME % name

    response = delete_metric_profile(input_json_file)
    print("delete metric profile = ", response.status_code)


@pytest.mark.sanity
@pytest.mark.parametrize("verbose", ["true", "false"])
def test_list_metric_profiles_with_verbose(verbose, cluster_type):
    """
    Test Description: This test validates the response status code of listMetricProfiles API by validating the output
    JSON response by passing 'verbose' query parameter - expected output is list of all the metric profiles created
    including all the metric profile fields when verbose=true and list of only the profile names when verbose=false
    """
    input_json_file = metric_profile_dir / 'resource_optimization_local_monitoring.json'
    form_kruize_url(cluster_type)

    response = delete_metric_profile(input_json_file)
    print("delete metric profile = ", response.status_code)

    # Create metric profile using the specified json
    response = create_metric_profile(input_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS

    json_file = open(input_json_file, "r")
    input_json = json.loads(json_file.read())
    metric_profile_name = input_json['metadata']['name']
    assert data['message'] == CREATE_METRIC_PROFILE_SUCCESS_MSG % metric_profile_name

    response = list_metric_profiles(verbose=verbose)
    list_metric_profiles_json = response.json()

    # Validate the json against the json schema
    errorMsg = validate_list_metric_profiles_json(list_metric_profiles_json, list_metric_profiles_schema)
    assert errorMsg == ""


    response = delete_metric_profile(input_json_file)
    print("delete metric profile = ", response.status_code)


@pytest.mark.sanity
@pytest.mark.parametrize("verbose", ["false", "true"])
def test_list_metric_profiles_name_and_verbose(verbose, cluster_type):
    """
    Test Description: This test validates the response status code of listMetricProfiles API by validating the output
    JSON response by passing both 'name' and 'verbose' query parameters - expected output is metric profile of the specified
    name as verbose is set to true when name parameter is passed
    """
    input_json_file = metric_profile_dir / 'resource_optimization_local_monitoring.json'
    form_kruize_url(cluster_type)

    response = delete_metric_profile(input_json_file)
    print("delete metric profile = ", response.status_code)

    # Create metric profile using the specified json
    response = create_metric_profile(input_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS

    json_file = open(input_json_file, "r")
    input_json = json.loads(json_file.read())
    metric_profile_name = input_json['metadata']['name']
    assert data['message'] == CREATE_METRIC_PROFILE_SUCCESS_MSG % metric_profile_name

    response = list_metric_profiles(name= metric_profile_name, verbose=verbose)
    list_metric_profiles_json = response.json()

    # Validate the json against the json schema
    errorMsg = validate_list_metric_profiles_json(list_metric_profiles_json, list_metric_profiles_schema)
    assert errorMsg == ""


    response = delete_metric_profile(input_json_file)
    print("delete metric profile = ", response.status_code)

