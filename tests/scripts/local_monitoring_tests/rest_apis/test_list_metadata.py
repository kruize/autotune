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
from helpers.import_metadata_json_validate import *
from helpers.list_metadata_json_validate import *
from helpers.list_metadata_json_schema import *
from jinja2 import Environment, FileSystemLoader

@pytest.mark.sanity
def test_list_metadata_valid_datasource(cluster_type):
    """
    Test Description: This test validates GET /dsmetadata by passing an valid datasource name
    """
    input_json_file = "../json_files/import_metadata.json"

    form_kruize_url(cluster_type)

    response = delete_metadata(input_json_file)
    print("delete metadata = ", response.status_code)

    # Import metadata using the specified json
    response = import_metadata(input_json_file)
    import_metadata_json = response.json()

    # Validate the json against the json schema
    errorMsg = validate_import_metadata_json(import_metadata_json, import_metadata_json_schema)
    assert errorMsg == ""

    json_data = json.load(open(input_json_file))
    datasource = json_data['datasource_name']

    response = list_metadata(datasource)

    list_metadata_json = response.json()
    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json against the json schema
    errorMsg = validate_list_metadata_json(list_metadata_json, list_metadata_json_schema)
    assert errorMsg == ""

    response = delete_metadata(input_json_file)
    print("delete metadata = ", response.status_code)


@pytest.mark.negative
def test_list_metadata_without_parameters(cluster_type):
    """
    Test Description: This test validates GET /dsmetadata without passing any parameters
    """
    input_json_file = "../json_files/import_metadata.json"

    form_kruize_url(cluster_type)

    response = delete_metadata(input_json_file)
    print("delete metadata = ", response.status_code)

    # Import metadata using the specified json
    response = import_metadata(input_json_file)
    metadata_json = response.json()

    # Validate the json against the json schema
    errorMsg = validate_import_metadata_json(metadata_json, import_metadata_json_schema)
    assert errorMsg == ""

    response = list_metadata()

    list_metadata_json = response.json()
    assert response.status_code == ERROR_STATUS_CODE
    datasource = "null"
    assert list_metadata_json['message'] == LIST_METADATA_DATASOURCE_NAME_ERROR_MSG % datasource


    response = delete_metadata(input_json_file)
    print("delete metadata = ", response.status_code)


@pytest.mark.negative
@pytest.mark.parametrize("test_name, expected_status_code, datasource",
                         [
                             ("blank_datasource", 400, ""),
                             ("null_datasource", 400, "null"),
                             ("invalid_datasource", 400, "xyz")
                         ]
                         )
def test_list_metadata_invalid_datasource(test_name, expected_status_code, datasource, cluster_type):
    """
    Test Description: This test validates the response status code of list metadata API against
    invalid input (blank, null, empty) for the json parameters.
    """
    print("\n****************************************************")
    print("Test datasource_name = ", datasource)
    print("****************************************************\n")

    form_kruize_url(cluster_type)
    input_json_file = "../json_files/import_metadata.json"

    response = delete_metadata(input_json_file)
    print("delete metadata = ", response.status_code)

    # Import metadata using the specified json
    response = import_metadata(input_json_file)
    metadata_json = response.json()

    # Validate the json against the json schema
    errorMsg = validate_import_metadata_json(metadata_json, import_metadata_json_schema)
    assert errorMsg == ""

    response = list_metadata(datasource=datasource)

    list_metadata_json = response.json()
    assert response.status_code == ERROR_STATUS_CODE
    cluster_name = "null"
    namespace = "null"
    assert list_metadata_json['message'] == LIST_METADATA_ERROR_MSG % (datasource, cluster_name, namespace)


    response = delete_metadata(input_json_file)
    print("delete metadata = ", response.status_code)


@pytest.mark.negative
@pytest.mark.parametrize("test_name, expected_status_code, cluster_name",
                         [
                             ("blank_cluster_name", 400, ""),
                             ("null_cluster_name", 400, "null"),
                             ("invalid_cluster_name", 400, "xyz")
                         ]
                         )
def test_list_metadata_datasource_invalid_cluster_name(test_name, expected_status_code, cluster_name, cluster_type):
    """
    Test Description: This test validates the response status code of list metadata API against
    invalid input (blank, null, empty) for the json parameters.
    """
    print("\n****************************************************")
    print("Test cluster_name = ", cluster_name)
    print("****************************************************\n")

    form_kruize_url(cluster_type)
    input_json_file = "../json_files/import_metadata.json"

    response = delete_metadata(input_json_file)
    print("delete metadata = ", response.status_code)

    # Import metadata using the specified json
    response = import_metadata(input_json_file)
    metadata_json = response.json()

    # Validate the json against the json schema
    errorMsg = validate_import_metadata_json(metadata_json, import_metadata_json_schema)
    assert errorMsg == ""

    json_data = json.load(open(input_json_file))
    datasource = json_data['datasource_name']
    namespace = "null"
    response = list_metadata(datasource=datasource, cluster_name=cluster_name)

    list_metadata_json = response.json()
    assert response.status_code == ERROR_STATUS_CODE
    assert list_metadata_json['message'] == LIST_METADATA_ERROR_MSG % (datasource, cluster_name, namespace)


    response = delete_metadata(input_json_file)
    print("delete metadata = ", response.status_code)


@pytest.mark.negative
@pytest.mark.parametrize("test_name, expected_status_code, namespace",
                         [
                             ("blank_namespace", 400, ""),
                             ("null_namespace", 400, "null"),
                             ("invalid_namespace", 400, "xyz")
                         ]
                         )
def test_list_metadata_datasource_cluster_name_invalid_namespace(test_name, expected_status_code, namespace, cluster_type):
    """
    Test Description: This test validates the response status code of list metadata API against
    invalid input (blank, null, empty) for the json parameters.
    """
    print("\n****************************************************")
    print("Test namespace = ", namespace)
    print("****************************************************\n")

    form_kruize_url(cluster_type)
    input_json_file = "../json_files/import_metadata.json"

    response = delete_metadata(input_json_file)
    print("delete metadata = ", response.status_code)

    # Import metadata using the specified json
    response = import_metadata(input_json_file)
    metadata_json = response.json()

    # Validate the json against the json schema
    errorMsg = validate_import_metadata_json(metadata_json, import_metadata_json_schema)
    assert errorMsg == ""

    json_data = json.load(open(input_json_file))
    datasource = json_data['datasource_name']
    # Currently only default cluster_name is supported by kruize
    cluster_name = "default"
    response = list_metadata(datasource=datasource, cluster_name=cluster_name, namespace=namespace)

    list_metadata_json = response.json()
    assert response.status_code == ERROR_STATUS_CODE
    assert list_metadata_json['message'] == LIST_METADATA_ERROR_MSG % (datasource, cluster_name, namespace)


    response = delete_metadata(input_json_file)
    print("delete metadata = ", response.status_code)


@pytest.mark.negative
def test_list_metadata_without_import_action(cluster_type):
    """
    Test Description: This test validates GET /dsmetadata without importing metadata
    """
    input_json_file = "../json_files/import_metadata.json"

    form_kruize_url(cluster_type)

    response = delete_metadata(input_json_file)
    print("delete metadata = ", response.status_code)

    json_data = json.load(open(input_json_file))
    datasource = json_data['datasource_name']
    cluster_name = "null"
    namespace = "null"

    response = list_metadata(datasource=datasource)

    list_metadata_json = response.json()
    assert response.status_code == ERROR_STATUS_CODE
    assert list_metadata_json['message'] == LIST_METADATA_ERROR_MSG % (datasource, cluster_name, namespace)
