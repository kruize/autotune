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
@pytest.mark.parametrize("datasource_name", ["", "null", "xyz"])
def test_list_datasources_invalid_datasorce_name(datasource_name, cluster_type):
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

    assert response.status_code == ERROR_STATUS_CODE

