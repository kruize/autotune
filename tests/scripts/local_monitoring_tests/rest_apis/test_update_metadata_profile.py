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
import os
import shutil

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
def test_update_metadata_profile(cluster_type):
    """
    Test Description: This test validates the response status code of updateMetadataProfile API by passing a
    valid input for the json
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

    update_json_file = "../json_files/update_cluster_metadata_local_monitoring.json"

    dsmetadata_json_file = "../json_files/import_metadata.json"
    import_metadata_list_and_validate(dsmetadata_json_file, verbose="true")

    # Update metadata profile using the specified json
    response = update_metadata_profile(update_json_file, name=metadata_profile_name)
    data = response.json()

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == UPDATE_METADATA_PROFILE_SUCCESS_MSG % metadata_profile_name

    dsmetadata_json_file = "../json_files/import_metadata.json"

    if cluster_type == "openshift":
        import_metadata_list_and_validate(dsmetadata_json_file, verbose="true", validate_workload="true", 
                                        namespace="openshift-tuning", workload="kruize", container="kruize")
    else :
        import_metadata_list_and_validate(dsmetadata_json_file, verbose="true", validate_workload="true", 
                                        namespace="monitoring", workload="kruize", container="kruize")

    response = delete_metadata_profile(metadata_profile_name)
    print("delete metadata profile = ", response.status_code)


@pytest.mark.extended
@pytest.mark.parametrize("field, expected_status_code, expected_status", mandatory_fields)
def test_update_metadata_profiles_mandatory_fields(cluster_type, field, expected_status_code, expected_status):
    """
    Test Description: This test validates the update API of metadata profile by missing the mandatory fields and validating
    the error message and status code
    """

    form_kruize_url(cluster_type)

    delete_and_create_metadata_profile()

    # Create metadata profile using the specified json
    json_file = "/tmp/create_metadata_profile.json"
    input_json_file = "../json_files/update_cluster_metadata_local_monitoring.json"
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

    data = json.dumps(json_data, indent=2)
    print("\n*****************************************")
    print(data)
    print("*****************************************\n")
    with open(json_file, 'w') as file:
        file.write(data)

    # Update metadata profile using the specified json
    response = update_metadata_profile(json_file, name=metadata_profile_name)
    data = response.json()

    assert response.status_code == expected_status_code, \
        f"Mandatory field check failed for {field} actual - {response.status_code} expected - {expected_status_code}"
    assert data['status'] == expected_status

    if response.status_code == ERROR_500_STATUS_CODE:
        assert data['message'] == UPDATE_METADATA_PROFILE_MISSING_MANDATORY_FIELD_MSG % field
    else:
        assert data['message'] == UPDATE_METADATA_PROFILE_MISSING_MANDATORY_PARAMETERS_MSG % field

    response = delete_metadata_profile(metadata_profile_name)
    print("delete metadata profile = ", response.status_code)


@pytest.mark.negative
@pytest.mark.parametrize("test_name, expected_status_code, name",
                         [
                             ("blank_name", 400, ""),
                             ("null_name", 400, "null"),
                             ("invalid_name", 400, "xyz")
                         ]
                         )
def test_update_metadata_profile_invalid_profile_name(test_name, expected_status_code, name, cluster_type):
    """
    Test Description: This test validates the response status code of updateMetadataProfile API by passing
    invalid name query parameter like blank, null and invalid values
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

    update_json_file = "../json_files/update_cluster_metadata_local_monitoring.json"

    dsmetadata_json_file = "../json_files/import_metadata.json"
    import_metadata_list_and_validate(dsmetadata_json_file)

    # Update metadata profile using the specified json
    response = update_metadata_profile(update_json_file, name=name)
    data = response.json()

    if test_name == "blank_name":
        errorMsg = MISSING_METADATA_PROFILE_NAME_PARAMETER
    else:
        errorMsg = INVALID_NAME_PARAMETER_METADATA_PROFILE % name

    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == errorMsg

    response = delete_metadata_profile(metadata_profile_name)
    print("delete metadata profile = ", response.status_code)


@pytest.mark.negative
def test_update_metadata_profile_mismatch_in_name(cluster_type):
    """
    Test Description: This test validates the response status code of updateMetadataProfile API with
    mismatch in profile name in query parameter and JSON payload
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

    dsmetadata_json_file = "../json_files/import_metadata.json"
    import_metadata_list_and_validate(dsmetadata_json_file)

    update_json_file = "../json_files/update_cluster_metadata_local_monitoring.json"
    tmp_file = "../json_files/tmp_file.json"
    shutil.copy(update_json_file, tmp_file)

    update_profile_name = "cluster-metadata-local-monitoring1"
    json_data = json.load(open(tmp_file))
    json_data["metadata"]["name"] = update_profile_name
    data = json.dumps(json_data)
    with open(tmp_file, 'w') as file:
        file.write(data)

    # Update metadata profile using the specified json
    response = update_metadata_profile(tmp_file, name=metadata_profile_name)
    data = response.json()

    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == MISMATCH_IN_METADATA_PROFILE_NAMES % (metadata_profile_name, update_profile_name)

    dsmetadata_json_file = "../json_files/import_metadata.json"
    import_metadata_list_and_validate(dsmetadata_json_file)

    if os.path.exists(tmp_file):
        os.remove(tmp_file)

    response = delete_metadata_profile(metadata_profile_name)
    print("delete metadata profile = ", response.status_code)


@pytest.mark.negative
def test_update_metadata_profile_unsupported_query_parameter(cluster_type):
    """
    Test Description: This test validates the response status code of updateMetadataProfile API by passing
    unsupported query parameter other than "name"
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

    dsmetadata_json_file = "../json_files/import_metadata.json"
    import_metadata_list_and_validate(dsmetadata_json_file)

    update_json_file = "../json_files/update_cluster_metadata_local_monitoring.json"

    profile_name = "profile_name"

    # Update metadata profile using the specified json
    response = update_metadata_profile(update_json_file, name=metadata_profile_name, profile_name=metadata_profile_name)
    data = response.json()

    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == INVALID_QUERY_PARAMETER_UPDATE_METADATA_PROFILE % profile_name

    dsmetadata_json_file = "../json_files/import_metadata.json"
    import_metadata_list_and_validate(dsmetadata_json_file)

    response = delete_metadata_profile(metadata_profile_name)
    print("delete metadata profile = ", response.status_code)


@pytest.mark.sanity
def test_multiple_update_metadata_profile(cluster_type):
    """
    Test Description: This test validates the response status code of multiple updateMetadataProfile API operations
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

    update_json_file = "../json_files/update_cluster_metadata_local_monitoring.json"

    dsmetadata_json_file = "../json_files/import_metadata.json"
    import_metadata_list_and_validate(dsmetadata_json_file, verbose="true")

    # Update metadata profile using the specified json
    response = update_metadata_profile(update_json_file, name=metadata_profile_name)
    data = response.json()

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == UPDATE_METADATA_PROFILE_SUCCESS_MSG % metadata_profile_name

    dsmetadata_json_file = "../json_files/import_metadata.json"
    if cluster_type == "openshift":
        import_metadata_list_and_validate(dsmetadata_json_file, verbose="true", validate_workload="true", 
                                        namespace="openshift-tuning", workload="kruize", container="kruize")
    else :
        import_metadata_list_and_validate(dsmetadata_json_file, verbose="true", validate_workload="true", 
                                        namespace="monitoring", workload="kruize", container="kruize")

    # Update metadata profile using the specified json
    response = update_metadata_profile(update_json_file, name=metadata_profile_name)
    data = response.json()

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == UPDATE_METADATA_PROFILE_SUCCESS_MSG % metadata_profile_name

    dsmetadata_json_file = "../json_files/import_metadata.json"
    if cluster_type == "openshift":
        import_metadata_list_and_validate(dsmetadata_json_file, verbose="true", validate_workload="true", 
                                        namespace="openshift-tuning", workload="kruize", container="kruize")
    else :
        import_metadata_list_and_validate(dsmetadata_json_file, verbose="true", validate_workload="true", 
                                        namespace="monitoring", workload="kruize", container="kruize")

    response = delete_metadata_profile(metadata_profile_name)
    print("delete metadata profile = ", response.status_code)


@pytest.mark.negative
def test_update_metadata_profile_missing_query_parameter(cluster_type):
    """
    Test Description: This test validates the response status code of updateMetadataProfile API by not passing
    required "name" query parameter.
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

    dsmetadata_json_file = "../json_files/import_metadata.json"
    import_metadata_list_and_validate(dsmetadata_json_file)

    update_json_file = "../json_files/update_cluster_metadata_local_monitoring.json"

    # Update metadata profile using the specified json
    response = update_metadata_profile(update_json_file)
    data = response.json()

    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == MISSING_METADATA_PROFILE_NAME_PARAMETER

    dsmetadata_json_file = "../json_files/import_metadata.json"
    import_metadata_list_and_validate(dsmetadata_json_file)

    response = delete_metadata_profile(metadata_profile_name)
    print("delete metadata profile = ", response.status_code)
