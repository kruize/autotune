"""
Copyright (c) 2024 Red Hat, IBM Corporation and others.

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
import sys
sys.path.append("../../")

from helpers.fixtures import *
from helpers.kruize import *
from helpers.utils import *
from jinja2 import Environment, FileSystemLoader

@pytest.mark.sanity
@pytest.mark.parametrize("test_name, expected_status_code, version, experiment_name, cluster_name, performance_profile, mode, target_cluster, datasource, experiment_type, kubernetes_obj_type, name, namespace, namespace_name, container_image_name, container_name, measurement_duration, threshold",
    [
        ("valid_namespace_exp_with_exp_type", SUCCESS_STATUS_CODE, "v2.0", "tfb-workload-namespace", "default", "resource-optimization-local-monitoring", "monitor", "local", "prometheus-1", "namespace", None, None, None, "default", None, None, "15min", "0.1"),
        ("valid_container_exp_without_exp_type", SUCCESS_STATUS_CODE, "v2.0", "tfb-workload-namespace", "default", "resource-optimization-local-monitoring", "monitor", "local", "prometheus-1", None, "deployment", "tfb-qrh-sample", "default", None, "kruize/tfb-qrh:1.13.2.F_et17", "tfb-server", "15min", "0.1"),
        ("valid_container_exp_with_exp_type", SUCCESS_STATUS_CODE, "v2.0", "tfb-workload-namespace", "default", "resource-optimization-local-monitoring", "monitor", "local", "prometheus-1", "container", "deployment", "tfb-qrh-sample", "default", None, "kruize/tfb-qrh:1.13.2.F_et17", "tfb-server", "15min", "0.1"),
    ]
)
def test_create_exp_valid_tests(test_name, expected_status_code, version, experiment_name, cluster_name, performance_profile, mode, target_cluster, datasource, experiment_type, kubernetes_obj_type, name, namespace, namespace_name, container_image_name, container_name, measurement_duration, threshold, cluster_type):
    """
    Test Description: This test validates the response status code of createExperiment API
    for namespace experiment by passing a valid input for the json
    """
    # Generate a temporary JSON filename
    tmp_json_file = "/tmp/create_exp_" + test_name + ".json"
    print("tmp_json_file = ", tmp_json_file)

    # Load the Jinja2 template
    environment = Environment(loader=FileSystemLoader("../json_files/"))
    template = environment.get_template("create_exp_template.json")

    # In case of test_name with "null", strip the specific fields
    if "null" in test_name:
         field = test_name.replace("null_", "")
         json_file = "../json_files/create_exp_template.json"
         filename = "/tmp/create_exp_template.json"
         strip_double_quotes_for_field(json_file, field, filename)
         environment = Environment(loader=FileSystemLoader("/tmp/"))
         template = environment.get_template("create_exp_template.json")

    # Render the JSON content from the template
    content = template.render(
        version=version,
        experiment_name=experiment_name,
        cluster_name=cluster_name,
        performance_profile=performance_profile,
        mode=mode,
        target_cluster=target_cluster,
        datasource=datasource,
        experiment_type=experiment_type,
        kubernetes_obj_type=kubernetes_obj_type,
        name=name,
        namespace=namespace,
        namespace_name=namespace_name,
        container_image_name=container_image_name,
        container_name=container_name,
        measurement_duration=measurement_duration,
        threshold=threshold
    )

    # Convert rendered content to a dictionary
    json_content = json.loads(content)

    if json_content[0]["kubernetes_objects"][0]["type"] == "None":
        json_content[0]["kubernetes_objects"][0].pop("type")
    if json_content[0]["kubernetes_objects"][0]["name"] == "None":
        json_content[0]["kubernetes_objects"][0].pop("name")
    if json_content[0]["kubernetes_objects"][0]["namespace"] == "None":
        json_content[0]["kubernetes_objects"][0].pop("namespace")
    if json_content[0]["kubernetes_objects"][0]["containers"][0]["container_image_name"] == "None":
        json_content[0]["kubernetes_objects"][0].pop("containers")
    if json_content[0]["kubernetes_objects"][0]["namespaces"]["namespace_name"] == "None":
        json_content[0]["kubernetes_objects"][0].pop("namespaces")
    if json_content[0]["experiment_type"] == "None":
        json_content[0].pop("experiment_type")

    # Write the final JSON to the temp file
    with open(tmp_json_file, mode="w", encoding="utf-8") as message:
        json.dump(json_content, message, indent=4)

    input_json_file = tmp_json_file
    form_kruize_url(cluster_type)

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)


@pytest.mark.negative
@pytest.mark.parametrize("test_name, expected_status_code, expected_error_msg, version, experiment_name, cluster_name, performance_profile, mode, target_cluster, datasource, experiment_type, kubernetes_obj_type, name, namespace, namespace_name, container_image_name, container_name, measurement_duration, threshold",
    [
        ("invalid_namespace_exp_without_exp_type", ERROR_STATUS_CODE, CREATE_EXP_CONTAINER_EXP_CONTAINS_NAMESPACE, "v2.0", "tfb-workload-namespace", "default", "resource-optimization-local-monitoring", "monitor", "local", "prometheus-1", None, None, None, None, "default", None, None, "15min", "0.1"),
        ("invalid_both_container_and_namespace_without_exp_type", ERROR_STATUS_CODE, CREATE_EXP_CONTAINER_EXP_CONTAINS_NAMESPACE,"v2.0", "tfb-workload-namespace", "default", "resource-optimization-local-monitoring", "monitor", "local", "prometheus-1", None, "deployment", "tfb-qrh-sample", "default", "default", "kruize/tfb-qrh:1.13.2.F_et17", "tfb-server", "15min", "0.1"),
        ("invalid_both_container_and_namespace_namespace_exp_type", ERROR_STATUS_CODE, CREATE_EXP_NAMESPACE_EXP_CONTAINS_CONTAINER,"v2.0", "tfb-workload-namespace", "default", "resource-optimization-local-monitoring", "monitor", "local", "prometheus-1", "namespace", "deployment", "tfb-qrh-sample", "default", "default", "kruize/tfb-qrh:1.13.2.F_et17", "tfb-server", "15min", "0.1"),
        ("invalid_both_container_and_namespace_container_exp_type", ERROR_STATUS_CODE, CREATE_EXP_CONTAINER_EXP_CONTAINS_NAMESPACE,"v2.0", "tfb-workload-namespace", "default", "resource-optimization-local-monitoring", "monitor", "local", "prometheus-1", "container", "deployment", "tfb-qrh-sample", "default", "default", "kruize/tfb-qrh:1.13.2.F_et17", "tfb-server", "15min", "0.1"),
        ("invalid_namespace_exp_type_with_only_containers", ERROR_STATUS_CODE, CREATE_EXP_NAMESPACE_EXP_CONTAINS_CONTAINER,"v2.0", "tfb-workload-namespace", "default", "resource-optimization-local-monitoring", "monitor", "local", "prometheus-1", "namespace", "deployment", "tfb-qrh-sample", "default", None, "kruize/tfb-qrh:1.13.2.F_et17", "tfb-server", "15min", "0.1"),
        ("invalid_container_exp_type_with_only_namespace", ERROR_STATUS_CODE, CREATE_EXP_CONTAINER_EXP_CONTAINS_NAMESPACE,"v2.0", "tfb-workload-namespace", "default", "resource-optimization-local-monitoring", "monitor", "local", "prometheus-1", "container", None, None, None, "default", None, None, "15min", "0.1"),
        ("invalid_namespace_exp_with_remote_cluster", ERROR_STATUS_CODE, CREATE_EXP_NAMESPACE_EXP_NOT_SUPPORTED_FOR_REMOTE,"v2.0", "tfb-workload-namespace", "default", "resource-optimization-local-monitoring", "monitor", "remote", "prometheus-1", "namespace", None, None, None, "default", None, None, "15min", "0.1")
    ]
)
def test_create_exp_invalid_tests(test_name, expected_status_code, expected_error_msg, version, experiment_name, cluster_name, performance_profile, mode, target_cluster, datasource, experiment_type, kubernetes_obj_type, name, namespace, namespace_name, container_image_name, container_name, measurement_duration, threshold, cluster_type):
    """
    Test Description: This test validates the response status code of createExperiment API
    for namespace experiment by passing a valid input for the json
    """
    # Generate a temporary JSON filename
    tmp_json_file = "/tmp/create_exp_" + test_name + ".json"
    print("tmp_json_file = ", tmp_json_file)

    # Load the Jinja2 template
    environment = Environment(loader=FileSystemLoader("../json_files/"))
    template = environment.get_template("create_exp_template.json")

    # In case of test_name with "null", strip the specific fields
    if "null" in test_name:
         field = test_name.replace("null_", "")
         json_file = "../json_files/create_exp_template.json"
         filename = "/tmp/create_exp_template.json"
         strip_double_quotes_for_field(json_file, field, filename)
         environment = Environment(loader=FileSystemLoader("/tmp/"))
         template = environment.get_template("create_exp_template.json")

    # Render the JSON content from the template
    content = template.render(
        version=version,
        experiment_name=experiment_name,
        cluster_name=cluster_name,
        performance_profile=performance_profile,
        mode=mode,
        target_cluster=target_cluster,
        datasource=datasource,
        experiment_type=experiment_type,
        kubernetes_obj_type=kubernetes_obj_type,
        name=name,
        namespace=namespace,
        namespace_name=namespace_name,
        container_image_name=container_image_name,
        container_name=container_name,
        measurement_duration=measurement_duration,
        threshold=threshold
    )

    # Convert rendered content to a dictionary
    json_content = json.loads(content)

    if json_content[0]["kubernetes_objects"][0]["type"] == "None":
        json_content[0]["kubernetes_objects"][0].pop("type")
    if json_content[0]["kubernetes_objects"][0]["name"] == "None":
        json_content[0]["kubernetes_objects"][0].pop("name")
    if json_content[0]["kubernetes_objects"][0]["namespace"] == "None":
        json_content[0]["kubernetes_objects"][0].pop("namespace")
    if json_content[0]["kubernetes_objects"][0]["containers"][0]["container_image_name"] == "None":
        json_content[0]["kubernetes_objects"][0].pop("containers")
    if json_content[0]["kubernetes_objects"][0]["namespaces"]["namespace_name"] == "None":
            json_content[0]["kubernetes_objects"][0].pop("namespaces")
    if json_content[0]["experiment_type"] == "None":
        json_content[0].pop("experiment_type")

    # Write the final JSON to the temp file
    with open(tmp_json_file, mode="w", encoding="utf-8") as message:
        json.dump(json_content, message, indent=4)

    input_json_file = tmp_json_file
    form_kruize_url(cluster_type)

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == expected_status_code
    assert data['status'] == ERROR_STATUS
    assert data['message'] == expected_error_msg

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)


@pytest.mark.negative
def test_create_multiple_namespace_exp(cluster_type):
    """
    Test Description: This test validates the response status code of createExperiment API
    if multiple entries are presnet in create experiment json
    """
    input_json_file = "../json_files/create_multiple_namespace_exp.json"
    form_kruize_url(cluster_type)

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    # validate error message
    assert data['message'] == CREATE_EXP_BULK_ERROR_MSG

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)
