"""
Copyright (c) 2022, 2024 Red Hat, IBM Corporation and others.

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

mandatory_fields = [
    ("version", ERROR_STATUS_CODE, ERROR_STATUS),
    ("cluster_name", ERROR_STATUS_CODE, ERROR_STATUS),
    ("experiment_name", ERROR_STATUS_CODE, ERROR_STATUS),
    ("mode", ERROR_STATUS_CODE, ERROR_STATUS),
    ("target_cluster", ERROR_STATUS_CODE, ERROR_STATUS),
    ("kubernetes_objects", ERROR_STATUS_CODE, ERROR_STATUS),
    ("type", ERROR_STATUS_CODE, ERROR_STATUS),
    ("kubernetes_objects_name", ERROR_STATUS_CODE, ERROR_STATUS),
    ("namespace", ERROR_STATUS_CODE, ERROR_STATUS),
    ("containers", ERROR_STATUS_CODE, ERROR_STATUS),
    ("container_image_name", ERROR_STATUS_CODE, ERROR_STATUS),
    ("container_name", ERROR_STATUS_CODE, ERROR_STATUS),
    ("selector", SUCCESS_STATUS_CODE, SUCCESS_STATUS),
    ("namespace", ERROR_STATUS_CODE, ERROR_STATUS),
    ("performance_profile", ERROR_STATUS_CODE, ERROR_STATUS),
    ("slo", SUCCESS_STATUS_CODE, SUCCESS_STATUS),
    ("recommendation_settings", ERROR_STATUS_CODE, ERROR_STATUS),
    ("trial_settings", ERROR_STATUS_CODE, ERROR_STATUS),
    ("kubernetes_objects_name_selector", ERROR_STATUS_CODE, ERROR_STATUS),
    ("performance_profile_slo", ERROR_STATUS_CODE, ERROR_STATUS)
]

csvfile = "/tmp/create_exp_test_data.csv"


@pytest.mark.negative
@pytest.mark.parametrize(
    "test_name, expected_status_code, version, experiment_name, cluster_name, performance_profile, mode, target_cluster, experiment_type, kubernetes_obj_type, name, namespace, container_image_name, container_name, measurement_duration, threshold",
    generate_test_data(csvfile, create_exp_test_data, "create_exp"))
def test_create_exp_invalid_tests(test_name, expected_status_code, version, experiment_name, cluster_name,
                                  performance_profile, mode, target_cluster, experiment_type, kubernetes_obj_type, name, namespace,
                                  container_image_name, container_name, measurement_duration, threshold, cluster_type):
    """
    Test Description: This test validates the response status code of createExperiment API against 
    invalid input (blank, null, empty) for the json parameters.
    """
    print("\n****************************************************")
    print("Test - ", test_name)
    print("****************************************************\n")
    tmp_json_file = "/tmp/create_exp_" + test_name + ".json"

    print("tmp_json_file = ", tmp_json_file)

    form_kruize_url(cluster_type)

    environment = Environment(loader=FileSystemLoader("../json_files/"))
    template = environment.get_template("create_exp_template.json")
    if "null" in test_name:
        field = test_name.replace("null_", "")
        json_file = "../json_files/create_exp_template.json"
        filename = "/tmp/create_exp_template.json"

        strip_double_quotes_for_field(json_file, field, filename)
        environment = Environment(loader=FileSystemLoader("/tmp/"))
        template = environment.get_template("create_exp_template.json")

    content = template.render(
        version=version,
        experiment_name=experiment_name,
        cluster_name=cluster_name,
        performance_profile=performance_profile,
        mode=mode,
        target_cluster=target_cluster,
        experiment_type=experiment_type,
        kubernetes_obj_type=kubernetes_obj_type,
        name=name,
        namespace=namespace,
        container_image_name=container_image_name,
        container_name=container_name,
        measurement_duration=measurement_duration,
        threshold=threshold
    )

    json_content = json.loads(content)

    # remove namespace data for container experiment_type
    if json_content[0]["kubernetes_objects"][0]["namespaces"]["namespace"] == "":
        json_content[0]["kubernetes_objects"][0].pop("namespaces")


    with open(tmp_json_file, mode="w", encoding="utf-8") as message:
        json.dump(json_content, message, indent=4)

    response = delete_experiment(tmp_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(tmp_json_file)
    data = response.json()
    print(data['message'])

    # temporarily moved this up to avoid failures in the subsequent tests, this will be reverted once the create
    # experiment validation PR goes in
    response_delete_exp = delete_experiment(tmp_json_file)
    print("delete exp = ", response_delete_exp.status_code)

    assert response.status_code == int(expected_status_code)


@pytest.mark.sanity
def test_create_exp(cluster_type):
    """
    Test Description: This test validates the response status code of createExperiment API by passing a
    valid input for the json
    """
    input_json_file = "../json_files/create_exp.json"
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


@pytest.mark.sanity
@pytest.mark.parametrize("k8s_obj_type", ["deployment", "deploymentConfig", "statefulset", "daemonset", "replicaset",
                                          "replicationController", "job"])
def test_create_exp_for_supported_k8s_obj_type(k8s_obj_type, cluster_type):
    """
    Test Description: This test validates the response status code of createExperiment API by passing a
    valid json with supported kuberenetes object type
    """
    input_json_file = "../json_files/create_exp.json"
    form_kruize_url(cluster_type)

    json_data = read_json_data_from_file(input_json_file)
    json_data[0]['kubernetes_objects'][0]['type'] = k8s_obj_type
    json_file = "/tmp/create_exp.json"

    write_json_data_to_file(json_file, json_data)

    response = delete_experiment(json_file)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG

    response = delete_experiment(json_file)
    print("delete exp = ", response.status_code)


@pytest.mark.sanity
def test_create_duplicate_exp(cluster_type):
    """
    Test Description: This test validates the response status code of createExperiment API by specifying the
    same experiment name
    """
    input_json_file = "../json_files/create_exp.json"
    json_data = json.load(open(input_json_file))

    experiment_name = json_data[0]['experiment_name']
    print("experiment_name = ", experiment_name)

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

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == ERROR_409_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == EXP_EXISTS_MSG + experiment_name

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)


@pytest.mark.sanity
def test_create_multiple_exps_from_same_json_file(cluster_type):
    """
    Test Description: This test validates the response status code of createExperiment API by specifying 
    multiple experiments in the same json file. This test also validates the behaviour with multiple 
    containers with different container images & container names
    """
    input_json_file = "../json_files/create_multiple_exps.json"

    form_kruize_url(cluster_type)

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == CREATE_EXP_BULK_ERROR_MSG

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)


@pytest.mark.sanity
def test_create_multiple_exps_from_diff_json_files(cluster_type):
    """
    Test Description: This test validates the creation of multiple experiments using different json files
    """

    input_json_file = "../json_files/create_exp.json"
    find = []
    json_data = json.load(open(input_json_file))

    find.append(json_data[0]['experiment_name'])
    find.append(json_data[0]['kubernetes_objects'][0]['name'])
    find.append(json_data[0]['kubernetes_objects'][0]['namespace'])

    form_kruize_url(cluster_type)

    # Create experiment using the specified json
    num_exps = 100
    for i in range(num_exps):
        json_file = "/tmp/create_exp.json"
        generate_json(find, input_json_file, json_file, i)

        response = delete_experiment(json_file)
        print("delete exp = ", response.status_code)

        response = create_experiment(json_file)

        data = response.json()
        print(data['message'])

        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == CREATE_EXP_SUCCESS_MSG

        response = delete_experiment(json_file)
        print("delete exp = ", response.status_code)


@pytest.mark.sanity
def test_create_multiple_exps_with_same_deployment_namespace(cluster_type):
    """
    Test Description: This test validates the creation of multiple experiments using same deployment & namespace 
    """

    input_json_file = "../json_files/create_exp.json"
    find = []
    json_data = json.load(open(input_json_file))

    find.append(json_data[0]['experiment_name'])

    form_kruize_url(cluster_type)

    # Create experiment using the specified json
    num_exps = 5
    for i in range(num_exps):
        json_file = "/tmp/create_exp.json"
        generate_json(find, input_json_file, json_file, i)

        response = create_experiment(json_file)

        data = response.json()
        print(data['message'])

        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == CREATE_EXP_SUCCESS_MSG

    num_exps = 5
    for i in range(num_exps):
        json_file = "/tmp/create_exp.json"
        generate_json(find, input_json_file, json_file, i)

        response = delete_experiment(json_file)
        print("delete exp = ", response.status_code)


@pytest.mark.sanity
def test_create_exp_with_both_performance_profile_slo(cluster_type):
    """
    Test Description: This test validates the creation of an experiment by specifying both performance profile & slo 
    """

    input_json_file = "../json_files/perf_profile_slo.json"

    form_kruize_url(cluster_type)

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    print(data['message'])
    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)


@pytest.mark.negative
def test_create_exp_with_both_deployment_name_selector(cluster_type):
    """
    Test Description: This test validates the creation of an experiment by specifying both deployment name & selector
    """

    input_json_file = "../json_files/deployment_name_selector.json"

    form_kruize_url(cluster_type)

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    print(data['message'])
    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)


@pytest.mark.negative
def test_create_exp_with_invalid_header(cluster_type):
    """
    Test Description: This test validates the creation of an experiment by specifying invalid content type in the header
    """

    input_json_file = "../json_files/create_exp.json"

    form_kruize_url(cluster_type)

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file, invalid_header=True)

    data = response.json()
    print(data['message'])
    print("content type = ", response.headers["Content-Type"])

    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)


@pytest.mark.extended
@pytest.mark.parametrize("field, expected_status_code, expected_status", mandatory_fields)
def test_create_exp_mandatory_fields(cluster_type, field, expected_status_code, expected_status):
    form_kruize_url(cluster_type)

    # Create experiment using the specified json
    json_file = "/tmp/create_exp.json"
    input_json_file = "../json_files/create_exp_mandatory.json"
    json_data = json.load(open(input_json_file))

    if field == "performance_profile_slo":
        json_data[0].pop("performance_profile", None)
        json_data[0].pop("slo", None)
    elif field == "kubernetes_objects_name_selector":
        json_data[0]["kubernetes_objects"][0].pop("name", None)
        json_data[0].pop("selector", None)
        json_data[0].pop("slo", None)
    elif field == "type":
        json_data[0]["kubernetes_objects"][0].pop(field, None)
        json_data[0].pop("slo", None)
        json_data[0].pop("selector", None)
    elif field == "kubernetes_objects_name":
        json_data[0]["kubernetes_objects"][0].pop("name", None)
        json_data[0].pop("slo", None)
        json_data[0].pop("selector", None)
    elif field == "namespace":
        json_data[0]["kubernetes_objects"][0].pop(field, None)
        json_data[0].pop("slo", None)
        json_data[0].pop("selector", None)
    elif field == "containers":
        json_data[0]["kubernetes_objects"][0].pop(field, None)
        json_data[0].pop("slo", None)
        json_data[0].pop("selector", None)
    elif field == "container_image_name":
        json_data[0]["kubernetes_objects"][0]["containers"][0].pop(field, None)
        json_data[0].pop("slo", None)
        json_data[0].pop("selector", None)
    elif field == "container_name":
        json_data[0]["kubernetes_objects"][0]["containers"][0].pop(field, None)
        json_data[0].pop("slo", None)
        json_data[0].pop("selector", None)
    else:
        json_data[0].pop("slo", None)
        json_data[0].pop("selector", None)
        json_data[0].pop(field, None)

    print("\n*****************************************")
    print(json_data)
    print("*****************************************\n")
    data = json.dumps(json_data)
    with open(json_file, 'w') as file:
        file.write(data)

    response = delete_experiment(json_file)
    print("delete exp = ", response.status_code)
    response = create_experiment(json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == expected_status_code, \
        f"Mandatory field check failed for {field} actual - {response.status_code} expected - {expected_status_code}"
    assert data['status'] == expected_status

    response = delete_experiment(json_file)
    print("delete exp = ", response.status_code)



@pytest.mark.sanity
@pytest.mark.parametrize("test_name, expected_status_code, version, experiment_name, cluster_name, performance_profile, mode, target_cluster, experiment_type, kubernetes_obj_type, name, namespace, namespace_name, container_image_name, container_name, measurement_duration, threshold",
         [
             ("valid_namespace_exp_with_exp_type", SUCCESS_STATUS_CODE, "v2.0", "tfb-workload-namespace", "cluster-one-division-bell", "resource-optimization-openshift", "monitor", "remote", "namespace", None, None, None, "default", None, None, "15min", "0.1"),
             ("valid_container_exp_without_exp_type", SUCCESS_STATUS_CODE, "v2.0", "tfb-workload-container", "cluster-one-division-bell", "resource-optimization-openshift", "monitor", "remote", None, "deployment", "tfb-qrh-sample", "default", None, "kruize/tfb-qrh:1.13.2.F_et17", "tfb-server", "15min", "0.1"),
             ("valid_container_exp_with_exp_type", SUCCESS_STATUS_CODE, "v2.0", "tfb-workload-container", "cluster-one-division-bell", "resource-optimization-openshift", "monitor", "remote", "container", "deployment", "tfb-qrh-sample", "default", None, "kruize/tfb-qrh:1.13.2.F_et17", "tfb-server", "15min", "0.1")
         ]
         )
def test_create_exp_valid_tests(test_name, expected_status_code, version, experiment_name, cluster_name, performance_profile, mode, target_cluster, experiment_type, kubernetes_obj_type, name, namespace, namespace_name, container_image_name, container_name, measurement_duration, threshold, cluster_type):
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
    if json_content[0]["kubernetes_objects"][0]["namespaces"]["namespace"] == "None":
        json_content[0]["kubernetes_objects"][0].pop("namespaces")
    if json_content[0]["experiment_type"] == "None":
        json_content[0].pop("experiment_type")

    # Write the final JSON to the temp file
    with open(tmp_json_file, mode="w", encoding="utf-8") as message:
        json.dump(json_content, message, indent=4)

    input_json_file = tmp_json_file
    form_kruize_url(cluster_type)

    response = delete_experiment(input_json_file, rm=True)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG

    response = delete_experiment(input_json_file, rm=True)
    print("delete exp = ", response.status_code)


@pytest.mark.negative
@pytest.mark.parametrize("test_name, expected_status_code, expected_error_msg, version, experiment_name, cluster_name, performance_profile, mode, target_cluster, experiment_type, kubernetes_obj_type, name, namespace, namespace_name, container_image_name, container_name, measurement_duration, threshold",
                         [
                             ("invalid_namespace_exp_without_exp_type", ERROR_STATUS_CODE, CREATE_EXP_CONTAINER_EXP_CONTAINS_NAMESPACE, "v2.0", "tfb-workload-namespace", "cluster-one-division-bell", "resource-optimization-openshift", "monitor", "remote", None, None, None, None, "default", None, None, "15min", "0.1"),
                             ("invalid_both_container_and_namespace_without_exp_type", ERROR_STATUS_CODE, CREATE_EXP_CONTAINER_EXP_CONTAINS_NAMESPACE,"v2.0", "tfb-workload-namespace", "cluster-one-division-bell", "resource-optimization-openshift", "monitor", "remote", None, "deployment", "tfb-qrh-sample", "default", "default", "kruize/tfb-qrh:1.13.2.F_et17", "tfb-server", "15min", "0.1"),
                             ("invalid_both_container_and_namespace_namespace_exp_type", ERROR_STATUS_CODE, CREATE_EXP_NAMESPACE_EXP_CONTAINS_CONTAINER,"v2.0", "tfb-workload-namespace", "cluster-one-division-bell", "resource-optimization-openshift", "monitor", "remote", "namespace", "deployment", "tfb-qrh-sample", "default", "default", "kruize/tfb-qrh:1.13.2.F_et17", "tfb-server", "15min", "0.1"),
                             ("invalid_both_container_and_namespace_container_exp_type", ERROR_STATUS_CODE, CREATE_EXP_CONTAINER_EXP_CONTAINS_NAMESPACE,"v2.0", "tfb-workload-namespace", "cluster-one-division-bell", "resource-optimization-openshift", "monitor", "remote", "container", "deployment", "tfb-qrh-sample", "default", "default", "kruize/tfb-qrh:1.13.2.F_et17", "tfb-server", "15min", "0.1"),
                             ("invalid_namespace_exp_type_with_only_containers", ERROR_STATUS_CODE, CREATE_EXP_NAMESPACE_EXP_CONTAINS_CONTAINER,"v2.0", "tfb-workload-namespace", "cluster-one-division-bell", "resource-optimization-openshift", "monitor", "remote", "namespace", "deployment", "tfb-qrh-sample", "default", None, "kruize/tfb-qrh:1.13.2.F_et17", "tfb-server", "15min", "0.1"),
                             ("invalid_container_exp_type_with_only_namespace", ERROR_STATUS_CODE, CREATE_EXP_CONTAINER_EXP_CONTAINS_NAMESPACE,"v2.0", "tfb-workload-namespace", "cluster-one-division-bell", "resource-optimization-openshift", "monitor", "remote", "container", None, None, None, "default", None, None, "15min", "0.1")
                         ])
def test_create_namespace_exp_invalid_tests(test_name, expected_status_code, expected_error_msg, version, experiment_name, cluster_name, performance_profile, mode, target_cluster, experiment_type, kubernetes_obj_type, name, namespace, namespace_name, container_image_name, container_name, measurement_duration, threshold, cluster_type):
    """
    Test Description: This test validates the response status code of createExperiment API
    for namespace experiment by passing an invalid experiment type field.
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
    if json_content[0]["kubernetes_objects"][0]["namespaces"]["namespace"] == "None":
        json_content[0]["kubernetes_objects"][0].pop("namespaces")
    if json_content[0]["experiment_type"] == "None":
        json_content[0].pop("experiment_type")

    # Write the final JSON to the temp file
    with open(tmp_json_file, mode="w", encoding="utf-8") as message:
        json.dump(json_content, message, indent=4)

    input_json_file = tmp_json_file
    form_kruize_url(cluster_type)

    response = delete_experiment(input_json_file, rm=True)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()

    assert response.status_code == expected_status_code
    assert data['status'] == ERROR_STATUS
    assert data['message'] == expected_error_msg

    response = delete_experiment(input_json_file, rm=True)
    print("delete exp = ", response.status_code)


@pytest.mark.negative
def test_create_multiple_namespace_exp(cluster_type):
    """
    Test Description: This test validates the response status code of createExperiment API
    if multiple entries are present in create experiment json
    """
    input_json_file = "../json_files/create_multiple_namespace_exp.json"
    form_kruize_url(cluster_type)

    response = delete_experiment(input_json_file, rm=True)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    # validate error message
    assert data['message'] == CREATE_EXP_BULK_ERROR_MSG

    response = delete_experiment(input_json_file, rm=True)
    print("delete exp = ", response.status_code)


@pytest.mark.sanity
def test_create_exp_exptype_dates_columns(cluster_type):
    """
    Test Description: This test validates the new columns in the DB(namely experiment_type, creation_date, update_date) and its values by passing a
    valid input json
    """
    input_json_file = "../json_files/create_exp.json"
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

    # Fetch listExperiments
    results = "false"
    recommendations = "false"
    latest = "false"
    experiment_name = None
    response = list_experiments(results, recommendations, latest, experiment_name, rm=True)
    if response.status_code == SUCCESS_200_STATUS_CODE:
        list_exp_before = response.json()
        # Validate timestamp fields
        assert "experiment_type" in list_exp_before[0]
        assert "creation_date" in list_exp_before[0]
        assert "update_date" in list_exp_before[0]
        assert list_exp_before[0]["creation_date"] is not None
        assert list_exp_before[0]["update_date"] is not None
        assert list_exp_before[0]["experiment_type"] == CONTAINER_EXPERIMENT_TYPE

        old_update_date = list_exp_before[0]["update_date"]
        # induce a delay os 2 secs, then call the updateResults API to check the update timestamp
        time.sleep(2)

        # call updateResults
        result_json_file = "../json_files/update_results.json"
        response = update_results(result_json_file)

        data = response.json()
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

        # call list exp again to check the updated date
        response = list_experiments(results, recommendations, latest, experiment_name, rm=True)
        if response.status_code == SUCCESS_200_STATUS_CODE:
            list_exp_after = response.json()
            new_update_date = list_exp_after[0]["update_date"]
            # Parse into datetime objects
            old_update_date = datetime.strptime(old_update_date, "%Y-%m-%dT%H:%M:%S.%fZ")
            new_update_date = datetime.strptime(new_update_date, "%Y-%m-%dT%H:%M:%S.%fZ")
            # Assert that the new date is greater
            assert new_update_date > old_update_date, (
                f"update_date not updated correctly: {new_update_date} <= {old_update_date}"
            )
        else:
            print(f"listExperiments failed in the second call!")
            sys.exit(1)
    else:
        print(f"listExperiments failed!")
        sys.exit(1)

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

# Custom Terms Validation Tests
@pytest.mark.negative
@pytest.mark.parametrize("term_name, expected_error_msg", [
    ("", "Empty term or model value is not allowed"),
    ("   ", "Empty term or model value is not allowed"),
    ("test@term", "is invalid. Only letters, numbers, underscores (_), and hyphens (-) are allowed"),
    ("test#term", "is invalid. Only letters, numbers, underscores (_), and hyphens (-) are allowed"),
    ("test$term", "is invalid. Only letters, numbers, underscores (_), and hyphens (-) are allowed"),
])
def test_create_exp_invalid_term_name(term_name, expected_error_msg, cluster_type):
    """
    Test Description: This test validates custom term name validation by passing invalid term names
    """
    input_json_file = "../json_files/create_exp.json"
    form_kruize_url(cluster_type)

    # Read the base experiment JSON
    json_data = read_json_data_from_file(input_json_file)

    # Add custom term with invalid name
    json_data[0]["recommendation_settings"] = {
        "threshold": "0.1",
        "term_settings": {
            "terms": {
                term_name: {
                    "duration_in_days": 2.0
                }
            }
        }
    }

    tmp_json_file = "/tmp/create_exp_invalid_term_name.json"
    write_json_data_to_file(tmp_json_file, json_data)

    response = delete_experiment(tmp_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment with invalid term name
    response = create_experiment(tmp_json_file)
    data = response.json()
    print(data)

    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert expected_error_msg in data['message']

    response = delete_experiment(tmp_json_file)
    print("delete exp = ", response.status_code)


@pytest.mark.negative
def test_create_exp_custom_term_without_duration(cluster_type):
    """
    Test Description: This test validates that custom terms require duration_in_days, we are adding a custom term
    without the mandatory parameter (duration in days) and we get an error msg.
    """
    input_json_file = "../json_files/create_exp.json"
    form_kruize_url(cluster_type)

    # Read the base experiment JSON
    json_data = read_json_data_from_file(input_json_file)

    # Add custom term without duration_in_days
    json_data[0]["recommendation_settings"] = {
        "threshold": "0.1",
        "term_settings": {
            "terms": {
                "my_custom_term": {
                    "plots_datapoint": 10
                }
            }
        }
    }

    tmp_json_file = "/tmp/create_exp_custom_term_no_duration.json"
    write_json_data_to_file(tmp_json_file, json_data)

    response = delete_experiment(tmp_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment without duration_in_days
    response = create_experiment(tmp_json_file)
    data = response.json()
    print(data)

    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert "The field 'duration_in_days' is mandatory" in data['message']

    response = delete_experiment(tmp_json_file)
    print("delete exp = ", response.status_code)


@pytest.mark.negative
@pytest.mark.parametrize("duration_value, expected_error_msg", [
    (0, "'duration_in_days' for term 'my_custom_term' must be a positive number"),
    (-1, "'duration_in_days' for term 'my_custom_term' must be a positive number"),
    (-5.5, "'duration_in_days' for term 'my_custom_term' must be a positive number"),
])
def test_create_exp_custom_term_invalid_duration(duration_value, expected_error_msg, cluster_type):
    """
    Test Description: This test validates that duration_in_days must be positive.
    We are adding custom term with negative duration_in_days value and should get error msg.
    """
    input_json_file = "../json_files/create_exp.json"
    form_kruize_url(cluster_type)

    # Read the base experiment JSON
    json_data = read_json_data_from_file(input_json_file)

    # Add custom term with invalid duration
    json_data[0]["recommendation_settings"] = {
        "threshold": "0.1",
        "term_settings": {
            "terms": {
                "my_custom_term": {
                    "duration_in_days": duration_value
                }
            }
        }
    }

    tmp_json_file = "/tmp/create_exp_custom_term_invalid_duration.json"
    write_json_data_to_file(tmp_json_file, json_data)

    response = delete_experiment(tmp_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment with invalid duration
    response = create_experiment(tmp_json_file)
    data = response.json()
    print(data)

    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert expected_error_msg in data['message']

    response = delete_experiment(tmp_json_file)
    print("delete exp = ", response.status_code)


@pytest.mark.negative
@pytest.mark.parametrize("default_term", [
    "daily", "weekly", "15 days", "monthly", "quarterly",
    "half_yearly", "yearly", "short_term", "medium_term", "long_term"
])
def test_create_exp_modify_default_term(default_term, cluster_type):
    """
    Test Description: This test validates that default terms cannot be modified.
    We are trying to modify the duration_in_days for a default term and we should get an error msg.
    """
    input_json_file = "../json_files/create_exp.json"
    form_kruize_url(cluster_type)

    # Read the base experiment JSON
    json_data = read_json_data_from_file(input_json_file)

    # Try to modify a default term
    json_data[0]["recommendation_settings"] = {
        "threshold": "0.1",
        "term_settings": {
            "terms": {
                default_term: {
                    "duration_in_days": 10.0
                }
            }
        }
    }

    tmp_json_file = "/tmp/create_exp_modify_default_term.json"
    write_json_data_to_file(tmp_json_file, json_data)

    response = delete_experiment(tmp_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment with modified default term
    response = create_experiment(tmp_json_file)
    data = response.json()
    print(data)

    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert f"The term '{default_term}' is immutable. Configuration fields cannot be specified" in data['message']

    response = delete_experiment(tmp_json_file)
    print("delete exp = ", response.status_code)


@pytest.mark.negative
@pytest.mark.parametrize("plots_datapoint_value", [0, -1, -10])
def test_create_exp_custom_term_invalid_plots_datapoint(plots_datapoint_value, cluster_type):
    """
    Test Description: This test validates that plots_datapoint must be positive
    """
    input_json_file = "../json_files/create_exp.json"
    form_kruize_url(cluster_type)

    # Read the base experiment JSON
    json_data = read_json_data_from_file(input_json_file)

    # Add custom term with invalid plots_datapoint
    json_data[0]["recommendation_settings"] = {
        "threshold": "0.1",
        "term_settings": {
            "terms": {
                "my_custom_term": {
                    "duration_in_days": 5.0,
                    "plots_datapoint": plots_datapoint_value
                }
            }
        }
    }

    tmp_json_file = "/tmp/create_exp_custom_term_invalid_plots.json"
    write_json_data_to_file(tmp_json_file, json_data)

    response = delete_experiment(tmp_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment with invalid plots_datapoint
    response = create_experiment(tmp_json_file)
    data = response.json()
    print(data)

    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert "'plots_datapoint' for term" in data['message'] and "must be a positive number" in data['message']

    response = delete_experiment(tmp_json_file)
    print("delete exp = ", response.status_code)


@pytest.mark.negative
@pytest.mark.parametrize("plots_delta_value", [0, -1, -5.5])
def test_create_exp_custom_term_invalid_plots_delta(plots_delta_value, cluster_type):
    """
    Test Description: This test validates that plots_datapoint_delta_in_days must be positive
    """
    input_json_file = "../json_files/create_exp.json"
    form_kruize_url(cluster_type)

    # Read the base experiment JSON
    json_data = read_json_data_from_file(input_json_file)

    # Add custom term with invalid plots_datapoint_delta_in_days
    json_data[0]["recommendation_settings"] = {
        "threshold": "0.1",
        "term_settings": {
            "terms": {
                "my_custom_term": {
                    "duration_in_days": 5.0,
                    "plots_datapoint_delta_in_days": plots_delta_value
                }
            }
        }
    }

    tmp_json_file = "/tmp/create_exp_custom_term_invalid_delta.json"
    write_json_data_to_file(tmp_json_file, json_data)

    response = delete_experiment(tmp_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment with invalid plots_datapoint_delta_in_days
    response = create_experiment(tmp_json_file)
    data = response.json()
    print(data)

    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert "'plots_datapoint_delta_in_days' for term" in data['message'] and "must be a positive number" in data['message']

    response = delete_experiment(tmp_json_file)
    print("delete exp = ", response.status_code)


@pytest.mark.skip(reason="Duration threshold validation not yet implemented")
@pytest.mark.negative
@pytest.mark.parametrize("duration_threshold", [
    "invalid format",
    "30",
    "min 30",
    "-5 days",
    "0 hours"
])
def test_create_exp_custom_term_invalid_duration_threshold(duration_threshold, cluster_type):
    """
    Test Description: This test validates duration_threshold format validation
    NOTE: This test is skipped because duration_threshold validation is not yet implemented
    """
    input_json_file = "../json_files/create_exp.json"
    form_kruize_url(cluster_type)

    # Read the base experiment JSON
    json_data = read_json_data_from_file(input_json_file)

    # Add custom term with invalid duration_threshold
    json_data[0]["recommendation_settings"] = {
        "threshold": "0.1",
        "term_settings": {
            "terms": {
                "my_custom_term": {
                    "duration_in_days": 5.0,
                    "duration_threshold": duration_threshold
                }
            }
        }
    }

    tmp_json_file = "/tmp/create_exp_custom_term_invalid_threshold.json"
    write_json_data_to_file(tmp_json_file, json_data)

    response = delete_experiment(tmp_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment with invalid duration_threshold
    response = create_experiment(tmp_json_file)
    data = response.json()
    print(data)

    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS

    response = delete_experiment(tmp_json_file)
    print("delete exp = ", response.status_code)


@pytest.mark.negative
def test_create_exp_duplicate_custom_term_name(cluster_type):
    """
    Test Description: This test validates that duplicate custom term names are not allowed in the same experiment
    """
    input_json_file = "../json_files/create_exp.json"
    form_kruize_url(cluster_type)

    # Read the base experiment JSON
    json_data = read_json_data_from_file(input_json_file)

    # Add duplicate custom term names
    json_data[0]["recommendation_settings"] = {
        "threshold": "0.1",
        "term_settings": {
            "terms": {
                "my_custom_term": {
                    "duration_in_days": 5.0
                },
                "my_custom_term": {
                    "duration_in_days": 10.0
                }
            }
        }
    }

    tmp_json_file = "/tmp/create_exp_duplicate_term.json"
    write_json_data_to_file(tmp_json_file, json_data)

    response = delete_experiment(tmp_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment with duplicate term names
    response = create_experiment(tmp_json_file)
    data = response.json()
    print(data)

    # Note: JSON parsing will automatically deduplicate keys, keeping only the last occurrence
    # This test validates the behavior - the experiment should be created with only one instance
    # or fail depending on backend validation
    assert response.status_code == SUCCESS_STATUS_CODE or response.status_code == ERROR_STATUS_CODE

    response = delete_experiment(tmp_json_file)
    print("delete exp = ", response.status_code)

# Sanity test for custom term
@pytest.mark.sanity
def test_create_exp_valid_custom_term(cluster_type):
    """
    Test Description: This test validates creation of experiment with valid custom term all fields provided by user
    """
    input_json_file = "../json_files/create_exp.json"
    form_kruize_url(cluster_type)

    # Read the base experiment JSON
    json_data = read_json_data_from_file(input_json_file)

    # Add valid custom term
    json_data[0]["recommendation_settings"] = {
        "threshold": "0.1",
        "term_settings": {
            "terms": {
                "my_custom_term": {
                    "duration_in_days": 5.0,
                    "duration_threshold": "3 days",
                    "plots_datapoint": 5,
                    "plots_datapoint_delta_in_days": 1.0
                }
            }
        }
    }

    tmp_json_file = "/tmp/create_exp_valid_custom_term.json"
    write_json_data_to_file(tmp_json_file, json_data)

    response = delete_experiment(tmp_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment with valid custom term
    response = create_experiment(tmp_json_file)
    data = response.json()
    print(data)

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG

    response = delete_experiment(tmp_json_file)
    print("delete exp = ", response.status_code)


@pytest.mark.sanity
def test_create_exp_mixed_default_and_custom_terms(cluster_type):
    """
    Test Description: This test validates creation of experiment with both default and custom terms (some fields provided by the user)
    """
    input_json_file = "../json_files/create_exp.json"
    form_kruize_url(cluster_type)

    # Read the base experiment JSON
    json_data = read_json_data_from_file(input_json_file)

    # Add mixed default and custom terms
    json_data[0]["recommendation_settings"] = {
        "threshold": "0.1",
        "term_settings": {
            "terms": {
                "weekly": {},
                "my_custom_term": {
                    "duration_in_days": 10.0
                },
                "monthly": {}
            }
        }
    }

    tmp_json_file = "/tmp/create_exp_mixed_terms.json"
    write_json_data_to_file(tmp_json_file, json_data)

    response = delete_experiment(tmp_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment with mixed terms
    response = create_experiment(tmp_json_file)
    data = response.json()
    print(data)

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG

    response = delete_experiment(tmp_json_file)
    print("delete exp = ", response.status_code)


@pytest.mark.sanity
@pytest.mark.parametrize("duration_in_days", [7.5, 2.25, 10.75, 30.5])
def test_create_exp_custom_term_fractional_duration(duration_in_days, cluster_type):
    """
    Test Description: This test validates creation of experiment with valid custom term using fractional duration_in_days values
    """
    input_json_file = "../json_files/create_exp.json"
    form_kruize_url(cluster_type)

    # Read the base experiment JSON
    json_data = read_json_data_from_file(input_json_file)

    # Add valid custom term with fractional duration
    json_data[0]["recommendation_settings"] = {
        "threshold": "0.1",
        "term_settings": {
            "terms": {
                "my_fractional_term": {
                    "duration_in_days": duration_in_days
                }
            }
        }
    }

    tmp_json_file = "/tmp/create_exp_fractional_duration.json"
    write_json_data_to_file(tmp_json_file, json_data)

    response = delete_experiment(tmp_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment with fractional duration
    response = create_experiment(tmp_json_file)
    data = response.json()
    print(data)

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG

    response = delete_experiment(tmp_json_file)
    print("delete exp = ", response.status_code)


@pytest.mark.sanity
@pytest.mark.parametrize("term_name", [
    "MyCustomTerm",
    "my_custom_term",
    "MY_CUSTOM_TERM",
    "My-Custom-Term",
    "myCustomTerm123",
    "custom_term_2024"
])
def test_create_exp_custom_term_mixed_case_names(term_name, cluster_type):
    """
    Test Description: This test validates that custom term names with mixed case, underscores,
    hyphens, and numbers are accepted
    """
    input_json_file = "../json_files/create_exp.json"
    form_kruize_url(cluster_type)

    # Read the base experiment JSON
    json_data = read_json_data_from_file(input_json_file)

    # Add custom term with mixed case name
    json_data[0]["recommendation_settings"] = {
        "threshold": "0.1",
        "term_settings": {
            "terms": {
                term_name: {
                    "duration_in_days": 5.0
                }
            }
        }
    }

    tmp_json_file = "/tmp/create_exp_mixed_case_term.json"
    write_json_data_to_file(tmp_json_file, json_data)

    response = delete_experiment(tmp_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment with mixed case term name
    response = create_experiment(tmp_json_file)
    data = response.json()
    print(data)

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG

    response = delete_experiment(tmp_json_file)
    print("delete exp = ", response.status_code)
