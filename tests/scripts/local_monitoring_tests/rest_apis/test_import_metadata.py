import pytest
import sys
sys.path.append("../../")
from helpers.fixtures import *
from helpers.kruize import *
from helpers.utils import *
from jinja2 import Environment, FileSystemLoader

mandatory_fields = [
    ("version", ERROR_STATUS_CODE, ERROR_STATUS),
    ("datasource_name", ERROR_STATUS_CODE, ERROR_STATUS)
]

csvfile = "/tmp/import_metadata_test_data.csv"

@pytest.mark.sanity
def test_import_metadata(cluster_type):
    """
    Test Description: This test validates the response status code of dsmetadata API by passing a
    valid input for the json
    """
    input_json_file = "../json_files/import_metadata.json"
    form_kruize_url(cluster_type)

    response = delete_metadata(input_json_file)
    print("delete metadata = ", response.status_code)

    # Import metadata using the specified json
    response = import_metadata(input_json_file)

    data = response.json()
    print("response data: ", str(data))
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == IMPORT_METADATA_SUCCESS_MSG

    response = delete_metadata(input_json_file)
    print("delete exp = ", response.status_code)


@pytest.mark.negative
def test_import_metadata_with_invalid_header(cluster_type):
    """
    Test Description: This test validates the importing of metadata by specifying invalid content type in the header
    """

    input_json_file = "../json_files/import_metadata.json"

    form_kruize_url(cluster_type)

    response = delete_metadata(input_json_file)
    print("delete exp = ", response.status_code)

    # Import metadata using the specified json
    response = import_metadata(input_json_file, invalid_header=True)

    data = response.json()
    print(data['message'])
    print("content type = ", response.headers["Content-Type"])

    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS

    response = delete_metadata(input_json_file)
    print("delete exp = ", response.status_code)
