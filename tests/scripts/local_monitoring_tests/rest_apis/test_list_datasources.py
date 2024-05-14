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
from helpers.list_datasources_json_validate import *


@pytest.mark.sanity
def test_list_datasources_without_parameters(cluster_type):
    """
    Test Description: This test validates datasources API without parameters
    """
    form_kruize_url(cluster_type)

    # Get the datasources name
    datasource_name = None
    response = list_datasources(datasource_name)

    list_datasources_json = response.json()

    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json against the json schema
    errorMsg = validate_list_datasources_json(list_datasources_json, list_datasources_json_schema)
    assert errorMsg == ""


@pytest.mark.sanity
def test_list_datasources_with_name(cluster_type):
    """
    Test Description: This test validates datasources API with 'name' parameter
    """
    form_kruize_url(cluster_type)

    # Get the datasources name
    datasource_name = "prometheus-1"
    response = list_datasources(datasource_name)

    list_datasources_json = response.json()

    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json against the json schema
    errorMsg = validate_list_datasources_json(list_datasources_json, list_datasources_json_schema)
    assert errorMsg == ""


@pytest.mark.negative
@pytest.mark.parametrize("test_name, expected_status_code, datasource_name",
    [
        ("blank_name", 400, ""),
        ("null_name", 400, "null"),
        ("invalid_name", 400, "xyz")
    ]
)
def test_list_datasources_invalid_datasource_name(test_name, expected_status_code, datasource_name, cluster_type):
    """
    Test Description: This test validates the response status code of list datasources API against
    invalid input (blank, null, empty) for the json parameters.
    """
    print("\n****************************************************")
    print("Test datasource_name = ", datasource_name)
    print("****************************************************\n")

    form_kruize_url(cluster_type)

    # Get the datasource name
    name = datasource_name
    response = list_datasources(name)

    list_datasources_json = response.json()
    assert response.status_code == ERROR_STATUS_CODE
    assert list_datasources_json['message'] == LIST_DATASOURCES_ERROR_MSG % name


