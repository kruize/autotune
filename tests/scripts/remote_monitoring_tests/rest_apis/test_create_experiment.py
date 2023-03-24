import requests
import pytest
from jinja2 import Environment, FileSystemLoader
from helpers.utils import *
from helpers.kruize import *
from helpers.fixtures import *

mandatory_fields = [
        ("experiment_name", ERROR_STATUS_CODE, ERROR_STATUS),
        ("kubernetes_objects_name", ERROR_STATUS_CODE, ERROR_STATUS),
        ("selector", SUCCESS_STATUS_CODE, SUCCESS_STATUS),
        ("namespace", ERROR_STATUS_CODE, ERROR_STATUS),
        ("performance_profile", ERROR_STATUS_CODE, ERROR_STATUS),
        ("slo", SUCCESS_STATUS_CODE, SUCCESS_STATUS),
        ("recommendation_settings", ERROR_STATUS_CODE, ERROR_STATUS),
        ("kubernetes_objects_name_selector", ERROR_STATUS_CODE, ERROR_STATUS),
        ("performance_profile_slo", ERROR_STATUS_CODE, ERROR_STATUS)
]

csvfile = "/tmp/create_exp_test_data.csv"

@pytest.mark.negative
@pytest.mark.parametrize("test_name, expected_status_code, version, experiment_name, cluster_name, performance_profile, mode, target_cluster, kubernetes_obj_type, name, namespace, container_image_name, container_name, measurement_duration, threshold", generate_test_data(csvfile, create_exp_test_data))
def test_create_exp_invalid_tests(test_name, expected_status_code, version, experiment_name, cluster_name, performance_profile, mode, target_cluster, kubernetes_obj_type, name, namespace, container_image_name, container_name, measurement_duration, threshold, cluster_type):
    """
    Test Description: This test validates the response status code of createExperiment API against 
    invalid input (blank, null, empty) for the json parameters.
    """
    print("\n****************************************************")
    print("Test - ", test_name)
    print("****************************************************\n")
    tmp_json_file="/tmp/create_exp_" + test_name + ".json"

    print("tmp_json_file = ", tmp_json_file)

    form_kruize_url(cluster_type)

    environment = Environment(loader=FileSystemLoader("../json_files/"))
    template = environment.get_template("create_exp_template.json")

    content = template.render(
        version=version,
        experiment_name=experiment_name,
        cluster_name=cluster_name,
        performance_profile=performance_profile,
        mode=mode,
        target_cluster=target_cluster,
        kubernetes_obj_type=kubernetes_obj_type,
        name=name,
        namespace=namespace,
        container_image_name=container_image_name,
        container_name=container_name,
        measurement_duration=measurement_duration,
        threshold=threshold
    )
    with open(tmp_json_file, mode="w", encoding="utf-8") as message:
        message.write(content)

    response = delete_experiment(tmp_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(tmp_json_file)
    data = response.json()
    print(data['message'])

    assert response.status_code == int(expected_status_code)
    response = delete_experiment(tmp_json_file)
    print("delete exp = ", response.status_code)

@pytest.mark.sanity
def test_create_exp(cluster_type):
    """
    Test Description: This test validates the response status code of createExperiment API by passing a
    valid input for the json
    """
    input_json_file="../json_files/create_exp.json"
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
def test_create_duplicate_exp(cluster_type):
    """
    Test Description: This test validates the response status code of createExperiment API by specifying the
    same experiment name
    """
    input_json_file="../json_files/create_exp.json"
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

    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS

    CREATE_EXP_DUPLICATE_MSG = "Experiment name : " + experiment_name + " is duplicate"
    assert data['message'] == CREATE_EXP_DUPLICATE_MSG

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

@pytest.mark.sanity
def test_create_multiple_exps_from_same_json_file(cluster_type):
    """
    Test Description: This test validates the response status code of createExperiment API by specifying 
    multiple experiments in the same json file. This test also validates the behaviour with multiple 
    containers with different container images & container names
    """
    input_json_file="../json_files/create_multiple_exps.json"

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
def test_create_multiple_exps_from_diff_json_files(cluster_type):
    """
    Test Description: This test validates the creation of multiple experiments using different json files
    """

    input_json_file="../json_files/create_exp.json"
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

    input_json_file="../json_files/create_exp.json"
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

    input_json_file="../json_files/perf_profile_slo.json"

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

@pytest.mark.sanity
def test_create_exp_with_both_deployment_name_selector(cluster_type):
    """
    Test Description: This test validates the creation of an experiment by specifying both deployment name & selector
    """

    input_json_file="../json_files/deployment_name_selector.json"

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

@pytest.mark.sanity
def test_create_exp_with_invalid_header(cluster_type):
    """
    Test Description: This test validates the creation of an experiment by specifying invalid content type in the header
    """

    input_json_file="../json_files/create_exp.json"

    form_kruize_url(cluster_type)

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file, invalid_header = True)

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
    input_json_file="../json_files/mandatory.json"
    json_data = json.load(open(input_json_file))

    if field == "performance_profile_slo":
        json_data[0].pop("performance_profile", None)
        json_data[0].pop("slo", None)
        json_data[0]["kubernetes_objects"][0].pop("name", None)
    elif field == "kubernetes_objects_name_selector":
        json_data[0]["kubernetes_objects"][0].pop("name", None)
        json_data[0].pop("selector", None)
        json_data[0].pop("slo", None)
    elif field == "kubernetes_objects_name":
        json_data[0]["kubernetes_objects"][0].pop("name", None)
    elif field == "namespace":
        json_data[0]["kubernetes_objects"][0].pop("namespace", None)
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

    assert response.status_code == expected_status_code
    assert data['status'] == expected_status

    response = delete_experiment(json_file)
    print("delete exp = ", response.status_code)
