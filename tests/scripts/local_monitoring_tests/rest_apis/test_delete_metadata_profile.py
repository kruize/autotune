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

sys.path.append("../../")

from helpers.fixtures import *
from helpers.kruize import *
from helpers.utils import *
from helpers.list_metadata_profiles_validate import *
from helpers.list_metadata_profiles_schema import *
from helpers.list_metadata_profiles_without_parameters_schema import *
from helpers.import_metadata_json_validate import *
from helpers.list_metadata_json_schema import *

metadata_profile_dir = get_metadata_profile_dir()

@pytest.mark.sanity
def test_delete_metadata_profile(cluster_type):
    """
    Test Description: This test validates the response status code of deleteMetadataProfile API by passing a
    valid metadata profile name to be deleted
    """
    input_json_file = metadata_profile_dir / 'bulk_cluster_metadata_local_monitoring.json'
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

    data = response.json()

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == DELETE_METADATA_PROFILE_SUCCESS_MSG % metadata_profile_name


@pytest.mark.negative
def test_delete_metadata_profile_missing_profile_name(cluster_type):
    """
    Test Description: This test validates the response status code of updateMetadataProfile API by missing
    required field i.e. profile name to be deleted
    """
    input_json_file = metadata_profile_dir / 'bulk_cluster_metadata_local_monitoring.json'
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

    response = delete_metadata_profile()
    print("delete metadata profile = ", response.status_code)
    data = response.json()

    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == MISSING_METADATA_PROFILE_NAME_PARAMETER

@pytest.mark.negative
@pytest.mark.parametrize("test_name, expected_status_code, name",
                         [
                             ("blank_name", 400, ""),
                             ("null_name", 400, "null"),
                             ("invalid_name", 400, "xyz")
                         ]
                         )
def test_delete_metadata_profile_invalid_profile_name(test_name, expected_status_code, name, cluster_type):
    """
    Test Description: This test validates the response status code of deleteMetadataProfile API by passing
    invalid name query parameter values like blank, null and invalid
    """
    input_json_file = metadata_profile_dir / 'cluster_metadata_local_monitoring.json'
    form_kruize_url(cluster_type)

    json_data = json.load(open(input_json_file))
    metadata_profile_name = json_data['metadata']['name']

    delete_and_create_metadata_profile()

    response = list_metadata_profiles(name=metadata_profile_name)
    metadata_profile_json = response.json()

    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json against the json schema
    errorMsg = validate_list_metadata_profiles_json(metadata_profile_json, list_metadata_profiles_schema)
    assert errorMsg == ""

    response = delete_metadata_profile(name)
    print("delete metadata profile = ", response.status_code)
    data = response.json()

    if test_name == "blank_name":
        errorMsg = MISSING_METADATA_PROFILE_NAME_PARAMETER
    else:
        errorMsg = INVALID_NAME_PARAMETER_METADATA_PROFILE % name

    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == errorMsg


@pytest.mark.negative
def test_multiple_delete_metadata_profile(cluster_type):
    """
    Test Description: This test validates the response status code of deleteMetadataProfile API by trying to delete
    the metadata profile twice and validate the error message
    """
    input_json_file = metadata_profile_dir / 'bulk_cluster_metadata_local_monitoring.json'
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
    data = response.json()
    print("delete metadata profile = ", response.status_code)

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == DELETE_METADATA_PROFILE_SUCCESS_MSG % metadata_profile_name

    response = delete_metadata_profile(metadata_profile_name)
    print("delete metadata profile = ", response.status_code)
    data = response.json()

    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == INVALID_NAME_PARAMETER_METADATA_PROFILE % metadata_profile_name


@pytest.mark.sanity
def test_import_metadata_after_deleting_metadata_profile(cluster_type):
    """
    Test Description: This test validates the response status code of import metadata API after deleting the metadata
    profile using deleteMetadataProfile API. This test ensures that cluster metadata cannot be imported if the metadata
    profile is deleted.
    """
    input_json_file = metadata_profile_dir / 'bulk_cluster_metadata_local_monitoring.json'
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
    data = response.json()

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == DELETE_METADATA_PROFILE_SUCCESS_MSG % metadata_profile_name

    dsmetadata_json_file = "../json_files/import_metadata.json"
    response = delete_metadata(dsmetadata_json_file)
    print("delete metadata = ", response.status_code)

    # Import metadata using the specified json
    response = import_metadata(dsmetadata_json_file)
    data = response.json()

    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == IMPORT_METADATA_INVALID_METADATA_PROFILE_NAME % metadata_profile_name
