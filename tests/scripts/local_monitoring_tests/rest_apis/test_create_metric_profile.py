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
import copy

sys.path.append("../../")

from helpers.fixtures import *
from helpers.kruize import *
from helpers.utils import *
from helpers.list_metric_profiles_validate import *
from helpers.list_metric_profiles_without_parameters_schema import *

mandatory_fields = [
    ("apiVersion", ERROR_500_STATUS_CODE, ERROR_STATUS),
    ("kind", ERROR_500_STATUS_CODE, ERROR_STATUS),
    ("metadata", ERROR_500_STATUS_CODE, ERROR_STATUS),
    ("name", ERROR_500_STATUS_CODE, ERROR_STATUS),
    ("slo", ERROR_500_STATUS_CODE, ERROR_STATUS)
]


@pytest.mark.sanity
def test_create_metric_profile(cluster_type):
    """
    Test Description: This test validates the response status code of createMetricProfile API by passing a
    valid input for the json
    """
    input_json_file = "../json_files/resource_optimization_openshift_metric_profile.json"
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

    response = list_metric_profiles(name=metric_profile_name)
    metric_profile_json = response.json()

    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json against the json schema
    errorMsg = validate_list_metric_profiles_json(metric_profile_json, list_metric_profiles_schema)
    assert errorMsg == ""

    response = delete_metric_profile(input_json_file)
    print("delete metric profile = ", response.status_code)


@pytest.mark.sanity
def test_create_duplicate_metric_profile(cluster_type):
    """
    Test Description: This test validates the response status code of createMetricProfile API by specifying the
    same metric profile name
    """
    input_json_file = "../json_files/resource_optimization_openshift_metric_profile.json"
    json_data = json.load(open(input_json_file))

    metric_profile_name = json_data['metadata']['name']
    print("name = ", metric_profile_name)

    form_kruize_url(cluster_type)

    response = delete_metric_profile(input_json_file)
    print("delete metric profile = ", response.status_code)

    # Create metric profile using the specified json
    response = create_metric_profile(input_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_METRIC_PROFILE_SUCCESS_MSG % metric_profile_name

    # Create metric profile using the specified json
    response = create_metric_profile(input_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == ERROR_409_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == METRIC_PROFILE_EXISTS_MSG % metric_profile_name

    response = delete_metric_profile(input_json_file)
    print("delete metric profile = ", response.status_code)


@pytest.mark.sanity
def test_create_multiple_metric_profiles(cluster_type):
    """
    Test Description: This test validates the creation of multiple metric profiles using different json files
    """

    input_json_file = "../json_files/resource_optimization_openshift_metric_profile.json"
    output_json_file = "/tmp/create_metric_profile.json"
    temp_json_file = "/tmp/temp_profile.json"

    input_json_data = json.load(open(input_json_file, 'r'))

    form_kruize_url(cluster_type)

    metric_profiles = []

    input_metric_profile_name = input_json_data['metadata']['name']

    # Create metric profile using the specified json
    num_metric_profiles = 100
    for i in range(num_metric_profiles):
        json_data = copy.deepcopy(input_json_data)
        # Modify the name for each profile
        metric_profile_name = f"{input_metric_profile_name}_{i}"
        json_data['metadata']['name'] = metric_profile_name

        # Write the modified profile to a temporary file
        with open(temp_json_file, 'w') as file:
            json.dump(json_data, file, indent=4)

        response = delete_metric_profile(temp_json_file)
        print("delete metric profile = ", response.status_code)

        response = create_metric_profile(temp_json_file)

        data = response.json()
        print(data['message'])

        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == CREATE_METRIC_PROFILE_SUCCESS_MSG % metric_profile_name

        response = list_metric_profiles(name=metric_profile_name)
        metric_profile_json = response.json()

        assert response.status_code == SUCCESS_200_STATUS_CODE

        # Validate the json against the json schema
        errorMsg = validate_list_metric_profiles_json(metric_profile_json, list_metric_profiles_schema)
        assert errorMsg == ""

        metric_profiles.append(copy.deepcopy(json_data))

        response = delete_metric_profile(temp_json_file)
        print("delete metric profile = ", response.status_code)

    # list all the metric profile names created
    response = list_metric_profiles()
    list_metric_profiles_json = response.json()

    assert len(list_metric_profiles_json) == num_metric_profiles, f"Expected {num_metric_profiles} metric profiles in response, but got {len(list_metric_profiles_json)}"
    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json against the json schema
    errorMsg = validate_list_metric_profiles_json(list_metric_profiles_json, list_metric_profiles_without_parameters_schema)
    assert errorMsg == ""

    # Write the profiles to the output file
    with open(output_json_file, 'w') as file:
        json.dump(metric_profiles, file, indent=4)


@pytest.mark.extended
@pytest.mark.parametrize("field, expected_status_code, expected_status", mandatory_fields)
def test_create_metric_profiles_mandatory_fields(cluster_type, field, expected_status_code, expected_status):
    """
    Test Description: This test validates the creation of metric profile by missing the mandatory fields and validating
    the error message and status code
    """

    form_kruize_url(cluster_type)

    # Create metric profile using the specified json
    json_file = "/tmp/create_metric_profile.json"
    input_json_file = "../json_files/resource_optimization_openshift_metric_profile.json"
    json_data = json.load(open(input_json_file))

    if field == "apiVersion":
        json_data.pop("apiVersion", None)
    elif field == "kind":
        json_data.pop("kind", None)
    elif field == "metadata":
        json_data.pop("metadata", None)
    elif field == "name":
        json_data['metadata'].pop("name", None)
    elif field == "slo":
        json_data.pop("slo", None)

    print("\n*****************************************")
    print(json_data)
    print("*****************************************\n")
    data = json.dumps(json_data)
    with open(json_file, 'w') as file:
        file.write(data)

    response = delete_metric_profile(input_json_file)
    print("delete metric profile = ", response.status_code)
    response = create_metric_profile(json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == expected_status_code, \
        f"Mandatory field check failed for {field} actual - {response.status_code} expected - {expected_status_code}"
    assert data['status'] == expected_status

    response = delete_metric_profile(input_json_file)
    print("delete metric profile = ", response.status_code)
