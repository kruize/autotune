"""
Copyright (c) 2025, 2025 Red Hat, IBM Corporation and others.

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
from helpers.list_metadata_profiles_validate import *
from helpers.list_metadata_profiles_schema import *
from helpers.list_metadata_profiles_without_parameters_schema import *

metadata_profile_dir = get_metadata_profile_dir()

mandatory_fields = [
    ("apiVersion", ERROR_500_STATUS_CODE, ERROR_STATUS),
    ("kind", ERROR_500_STATUS_CODE, ERROR_STATUS),
    ("metadata", ERROR_500_STATUS_CODE, ERROR_STATUS),
    ("name", ERROR_500_STATUS_CODE, ERROR_STATUS),
    ("datasource", ERROR_500_STATUS_CODE, ERROR_STATUS),
    ("query_variables", ERROR_500_STATUS_CODE, ERROR_STATUS),
    ("name", ERROR_500_STATUS_CODE, ERROR_STATUS),
    ("value_type", ERROR_500_STATUS_CODE, ERROR_STATUS),
    ("aggregation_functions", ERROR_500_STATUS_CODE, ERROR_STATUS),
    ("function", ERROR_500_STATUS_CODE, ERROR_STATUS),
    ("query", ERROR_500_STATUS_CODE, ERROR_STATUS)
]


@pytest.mark.sanity
def test_create_metadata_profile(cluster_type):
    """
    Test Description: This test validates the response status code of createMetadataProfile API by passing a
    valid input for the json
    """
    input_json_file = metadata_profile_dir / 'cluster_metadata_local_monitoring.json'
    form_kruize_url(cluster_type)

    json_data = json.load(open(input_json_file))
    metadata_profile_name = json_data['metadata']['name']

    response = delete_metadata_profile(metadata_profile_name)
    print("delete metadata profile = ", response.status_code)

    # Create metadata profile using the specified json
    response = create_metadata_profile(input_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_METADATA_PROFILE_SUCCESS_MSG % metadata_profile_name

    response = list_metadata_profiles(name=metadata_profile_name)
    metadata_profile_json = response.json()

    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json against the json schema
    errorMsg = validate_list_metadata_profiles_json(metadata_profile_json, list_metadata_profiles_schema)
    assert errorMsg == ""

    response = delete_metadata_profile(metadata_profile_name)
    print("delete metadata profile = ", response.status_code)


@pytest.mark.sanity
def test_create_duplicate_metadata_profile(cluster_type):
    """
    Test Description: This test validates the response status code of createMetadataProfile API by specifying the
    same metadata profile name
    """
    input_json_file = metadata_profile_dir / 'cluster_metadata_local_monitoring.json'
    json_data = json.load(open(input_json_file))

    metadata_profile_name = json_data['metadata']['name']
    print("name = ", metadata_profile_name)

    form_kruize_url(cluster_type)

    response = delete_metadata_profile(metadata_profile_name)
    print("delete metadata profile = ", response.status_code)

    # Create metadata profile using the specified json
    response = create_metadata_profile(input_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_METADATA_PROFILE_SUCCESS_MSG % metadata_profile_name

    # Create metadata profile using the specified json
    response = create_metadata_profile(input_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == ERROR_409_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == METADATA_PROFILE_EXISTS_MSG % metadata_profile_name

    response = delete_metadata_profile(metadata_profile_name)
    print("delete metadata profile = ", response.status_code)


@pytest.mark.sanity
def test_create_multiple_metadata_profiles(cluster_type):
    """
    Test Description: This test validates the creation of multiple metadata profiles using different json files
    """

    input_json_file = metadata_profile_dir / 'cluster_metadata_local_monitoring.json'
    output_json_file = "/tmp/create_metadata_profile.json"
    temp_json_file = "/tmp/temp_profile.json"

    input_json_data = json.load(open(input_json_file, 'r'))

    form_kruize_url(cluster_type)

    metadata_profiles = []

    input_metadata_profile_name = input_json_data['metadata']['name']

    # Create metadata profile using the specified json
    num_metadata_profiles = 100
    for i in range(num_metadata_profiles):
        json_data = copy.deepcopy(input_json_data)
        # Modify the name for each profile
        metadata_profile_name = f"{input_metadata_profile_name}_{i}"
        json_data['metadata']['name'] = metadata_profile_name

        # Write the modified profile to a temporary file
        with open(temp_json_file, 'w') as file:
            json.dump(json_data, file, indent=4)

        response = delete_metadata_profile(metadata_profile_name)
        print("delete metadata profile = ", response.status_code)

        response = create_metadata_profile(temp_json_file)

        data = response.json()
        print(data['message'])

        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == CREATE_METADATA_PROFILE_SUCCESS_MSG % metadata_profile_name

        response = list_metadata_profiles(name=metadata_profile_name, logging=False)
        metadata_profile_json = response.json()

        assert response.status_code == SUCCESS_200_STATUS_CODE

        # Validate the json against the json schema
        errorMsg = validate_list_metadata_profiles_json(metadata_profile_json, list_metadata_profiles_schema)
        assert errorMsg == ""

        metadata_profiles.append(copy.deepcopy(json_data))


    # list all the metadata profile names created
    response = list_metadata_profiles()
    list_metadata_profiles_json = response.json()

    assert len(list_metadata_profiles_json) == num_metadata_profiles, f"Expected {num_metadata_profiles} metadata profiles in response, but got {len(list_metadata_profiles_json)}"
    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json against the json schema
    errorMsg = validate_list_metadata_profiles_json(list_metadata_profiles_json, list_metadata_profiles_without_parameters_schema)
    assert errorMsg == ""

    # Validate all the metadata profiles names present in /listMetadataProfiles JSON
    list_metadata_profile_names = {obj.get("name") for obj in list_metadata_profiles_json}

    for metadata_profile in metadata_profiles:
        profile_name = metadata_profile['metadata']['name']

        assert profile_name in list_metadata_profile_names, f"Metadata profile name '{profile_name}' is not found in the /listMetadataProfiles JSON!"

    # Write the profiles to the output file
    with open(output_json_file, 'w') as file:
        json.dump(metadata_profiles, file, indent=4)

    for i in range(num_metadata_profiles):
        metadata_profile = metadata_profiles[i]
        metadata_profile_name = metadata_profile['metadata']['name']

        with open(temp_json_file, 'w') as file:
            json.dump(metadata_profile, file, indent=4)

        response = delete_metadata_profile(metadata_profile_name)
        print("delete metadata profile = ", response.status_code)


@pytest.mark.extended
@pytest.mark.parametrize("field, expected_status_code, expected_status", mandatory_fields)
def test_create_metadata_profiles_mandatory_fields(cluster_type, field, expected_status_code, expected_status):
    """
    Test Description: This test validates the creation of metadata profile by missing the mandatory fields and validating
    the error message and status code
    """

    form_kruize_url(cluster_type)

    # Create metadata profile using the specified json
    json_file = "/tmp/create_metadata_profile.json"
    input_json_file = metadata_profile_dir / 'cluster_metadata_local_monitoring.json'
    json_data = json.load(open(input_json_file))
    metadata_profile_name = json_data['metadata']['name']

    if field == "apiVersion":
        json_data.pop("apiVersion", None)
    elif field == "kind":
        json_data.pop("kind", None)
    elif field == "metadata":
        json_data.pop("metadata", None)
    elif field == "name":
        json_data['metadata'].pop("name", None)
    elif field == "datasource":
        json_data.pop("datasource", None)
    elif field == "query_variables":
        json_data.pop("query_variables", None)
    elif field == "name":
        json_data['query_variables'].pop("name", None)
    elif field == "value_type":
        json_data['query_variables'][0].pop("value_type", None)
    elif field == "aggregation_functions":
        json_data['query_variables'][0].pop("aggregation_functions", None)
    elif field == "function":
        json_data['query_variables'][0]['aggregation_functions'][0].pop("function", None)
    elif field == "query":
        json_data['query_variables'][0]['aggregation_functions'][0].pop("query", None)

    print("\n*****************************************")
    print(json_data)
    print("*****************************************\n")
    data = json.dumps(json_data)
    with open(json_file, 'w') as file:
        file.write(data)

    response = delete_metadata_profile(metadata_profile_name)
    print("delete metadata profile = ", response.status_code)
    response = create_metadata_profile(json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == expected_status_code, \
        f"Mandatory field check failed for {field} actual - {response.status_code} expected - {expected_status_code}"
    assert data['status'] == expected_status

    if response.status_code == ERROR_500_STATUS_CODE:
        assert data['message'] == CREATE_METADATA_PROFILE_MISSING_MANDATORY_FIELD_MSG % field
    else:
        assert data['message'] == CREATE_METADATA_PROFILE_MISSING_MANDATORY_PARAMETERS_MSG % field

    response = delete_metadata_profile(metadata_profile_name)
    print("delete metadata profile = ", response.status_code)
