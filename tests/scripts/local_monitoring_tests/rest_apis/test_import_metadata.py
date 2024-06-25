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
    metadata_json = response.json()

    # Validate the json against the json schema
    errorMsg = validate_import_metadata_json(metadata_json, import_metadata_json_schema)
    assert errorMsg == ""

    response = delete_metadata(input_json_file)
    print("delete metadata = ", response.status_code)


@pytest.mark.negative
@pytest.mark.parametrize(
    "test_name, expected_status_code, version, datasource_name",
    generate_test_data(csvfile, import_metadata_test_data, "import_metadata"))
def test_import_metadata_invalid_test(test_name, expected_status_code, version, datasource_name, cluster_type):
    """
    Test Description: This test validates the response status code of POST dsmtedata API against
    invalid input (blank, null, empty) for the json parameters.
    """
    print("\n****************************************************")
    print("Test - ", test_name)
    print("****************************************************\n")
    tmp_json_file = "/tmp/import_metadata_" + test_name + ".json"

    print("tmp_json_file = ", tmp_json_file)

    form_kruize_url(cluster_type)

    environment = Environment(loader=FileSystemLoader("../json_files/"))
    template = environment.get_template("import_metadata_template.json")
    if "null" in test_name:
        field = test_name.replace("null_", "")
        json_file = "../json_files/import_metadata_template.json"
        filename = "/tmp/import_metadata_template.json"

        strip_double_quotes_for_field(json_file, field, filename)
        environment = Environment(loader=FileSystemLoader("/tmp/"))
        template = environment.get_template("import_metadata_template.json")

    content = template.render(
        version=version,
        datasource_name=datasource_name,
    )
    with open(tmp_json_file, mode="w", encoding="utf-8") as message:
        message.write(content)

    response = delete_metadata(tmp_json_file)
    print("delete metadata = ", response.status_code)

    # Import metadata using the specified json
    response = import_metadata(tmp_json_file)
    metadata_json = response.json()

    # temporarily moved this up to avoid failures in the subsequent tests
    response_delete_metadata = delete_metadata(tmp_json_file)
    print("delete metadata = ", response_delete_metadata.status_code)

    assert response.status_code == int(expected_status_code)


@pytest.mark.extended
@pytest.mark.parametrize("field, expected_status_code, expected_status", mandatory_fields)
def test_import_metadata_mandatory_fields(cluster_type, field, expected_status_code, expected_status):
    form_kruize_url(cluster_type)

    # Import metadata using the specified json
    json_file = "/tmp/import_metadata.json"
    input_json_file = "../json_files/import_metadata_mandatory.json"
    json_data = json.load(open(input_json_file))

    if field == 'version':
        json_data.pop("version", None)
    else:
        json_data.pop("datasource_name", None)

    print("\n*****************************************")
    print(json_data)
    print("*****************************************\n")
    data = json.dumps(json_data)
    with open(json_file, 'w') as file:
        file.write(data)

    response = delete_metadata(json_file)
    print("delete metadata = ", response.status_code)

    # Import metadata using the specified json
    response = import_metadata(json_file)
    metadata_json = response.json()

    assert response.status_code == expected_status_code, \
        f"Mandatory field check failed for {field} actual - {response.status_code} expected - {expected_status_code}"
    assert metadata_json['status'] == expected_status

    response = delete_metadata(json_file)
    print("delete metadata = ", response.status_code)


@pytest.mark.sanity
def test_repeated_metadata_import(cluster_type):
    """
    Test Description: This test validates the response status code of /dsmetadata API by specifying the
    same datasource name
    """
    input_json_file = "../json_files/import_metadata.json"
    json_data = json.load(open(input_json_file))

    datasource_name = json_data['datasource_name']
    print("datasource_name = ", datasource_name)

    form_kruize_url(cluster_type)

    response = delete_metadata(input_json_file)
    print("delete metadata = ", response.status_code)

    create_namespace("testing")
    time.sleep(30)

    # Import metadata using the specified json
    response = import_metadata(input_json_file)
    metadata_json = response.json()

    assert response.status_code == SUCCESS_STATUS_CODE

    # Validate the json against the json schema
    errorMsg = validate_import_metadata_json(metadata_json, import_metadata_json_schema)
    assert errorMsg == ""

    json_data = json.load(open(input_json_file))
    datasource = json_data['datasource_name']
    # Currently only default cluster_name is supported by kruize
    cluster_name = "default"
    response = list_metadata(datasource=datasource, cluster_name=cluster_name)

    list_metadata_json = response.json()
    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json values
    import_metadata_json = read_json_data_from_file(input_json_file)
    validate_list_metadata_parameters(import_metadata_json, list_metadata_json, cluster_name=cluster_name, namespace="testing")

    delete_namespace("testing")
    time.sleep(15)

    create_namespace("repeated-metadata-import")
    create_namespace("local-monitoring-test")
    time.sleep(30)

    # Import metadata using the specified json
    response = import_metadata(input_json_file)
    metadata_json = response.json()

    assert response.status_code == SUCCESS_STATUS_CODE

    # Validate the json against the json schema
    errorMsg = validate_import_metadata_json(metadata_json, import_metadata_json_schema)
    assert errorMsg == ""

    json_data = json.load(open(input_json_file))
    datasource = json_data['datasource_name']
    # Currently only default cluster_name is supported by kruize
    cluster_name = "default"
    response = list_metadata(datasource=datasource, cluster_name=cluster_name)

    list_metadata_json = response.json()
    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json values
    import_metadata_json = read_json_data_from_file(input_json_file)
    validate_list_metadata_parameters(import_metadata_json, list_metadata_json, cluster_name=cluster_name, namespace="repeated-metadata-import")
    validate_list_metadata_parameters(import_metadata_json, list_metadata_json, cluster_name=cluster_name, namespace="local-monitoring-test")

    #validate namespaces
    response = delete_metadata(input_json_file)
    print("delete metadata = ", response.status_code)

    delete_namespace("repeated-metadata-import")
    delete_namespace("local-monitoring-test")


@pytest.mark.negative
def test_repeated_metadata_import_without_datasource_connection(cluster_type):
    """
    Test Description: This test validates the response status code of POST /dsmetadata API by specifying the
    same datasource name with repeated metadata imports by bringing down prometheus server instance
    """
    input_json_file = "../json_files/import_metadata.json"
    json_data = json.load(open(input_json_file))

    datasource_name = json_data['datasource_name']
    print("datasource_name = ", datasource_name)

    form_kruize_url(cluster_type)

    response = delete_metadata(input_json_file)
    print("delete metadata = ", response.status_code)

    # Import metadata using the specified json
    response = import_metadata(input_json_file)
    metadata_json = response.json()

    assert response.status_code == SUCCESS_STATUS_CODE

    # Validate the json against the json schema
    errorMsg = validate_import_metadata_json(metadata_json, import_metadata_json_schema)
    assert errorMsg == ""

    json_data = json.load(open(input_json_file))
    datasource = json_data['datasource_name']
    # Currently only default cluster_name is supported by kruize
    cluster_name = "default"
    response = list_metadata(datasource=datasource, cluster_name=cluster_name)

    list_metadata_json = response.json()
    assert response.status_code == SUCCESS_200_STATUS_CODE

    if cluster_type == "minikube":
        namespace = "monitoring"
    elif cluster_type == "openshift":
        namespace = "openshift-monitoring"

    # Validate the json values
    import_metadata_json = read_json_data_from_file(input_json_file)
    validate_list_metadata_parameters(import_metadata_json, list_metadata_json, cluster_name=cluster_name, namespace=namespace)


    # Scaling down prometheus deployment and statefulset to zero replicas to bring down prometheus datasource connection
    scale_deployment(namespace, "prometheus-operator", 0)
    scale_deployment(namespace, "prometheus-adapter", 0)
    scale_statefulset(namespace, "prometheus-k8s", 0)
    time.sleep(10)

    # Repeated Import metadata using the specified json
    response = import_metadata(input_json_file)
    metadata_json = response.json()

    assert response.status_code == ERROR_STATUS_CODE
    assert metadata_json['message'] == IMPORT_METADATA_DATASOURCE_CONNECTION_FAILURE_MSG

    json_data = json.load(open(input_json_file))
    datasource = json_data['datasource_name']
    # Currently only default cluster_name is supported by kruize
    cluster_name = "default"
    response = list_metadata(datasource=datasource, cluster_name=cluster_name)

    list_metadata_json = response.json()
    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json values
    import_metadata_json = read_json_data_from_file(input_json_file)
    validate_list_metadata_parameters(import_metadata_json, list_metadata_json, cluster_name=cluster_name, namespace=namespace)

    #validate namespaces
    response = delete_metadata(input_json_file)
    print("delete metadata = ", response.status_code)

    scale_deployment(namespace, "prometheus-operator", 1)
    scale_deployment(namespace, "prometheus-adapter", 2)
    scale_statefulset(namespace, "prometheus-k8s", 2)
    time.sleep(90)
