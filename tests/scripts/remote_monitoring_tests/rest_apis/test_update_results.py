import requests
import pytest
from jinja2 import Environment, FileSystemLoader
from helpers.utils import *
from helpers.kruize import *
from helpers.fixtures import *
import time

csvfile = "/tmp/update_results_test_data.csv"

@pytest.mark.negative
@pytest.mark.parametrize("test_name, expected_status_code, version, experiment_name, start_timestamp, end_timestamp, kubernetes_obj_type, name, namespace, container_image_name, container_name, cpuRequest_name, cpuRequest_sum, cpuRequest_avg, cpuRequest_format, cpuLimit_name, cpuLimit_sum, cpuLimit_avg, cpuLimit_format, cpuUsage_name, cpuUsage_sum, cpuUsage_max, cpuUsage_avg, cpuUsage_min, cpuUsage_format, cpuThrottle_name, cpuThrottle_sum, cpuThrottle_max, cpuThrottle_avg, cpuThrottle_format, memoryRequest_name, memoryRequest_sum, memoryRequest_avg, memoryRequest_format, memoryLimit_name, memoryLimit_sum, memoryLimit_avg, memoryLimit_format, memoryUsage_name, memoryUsage_sum, memoryUsage_max, memoryUsage_avg, memoryUsage_min, memoryUsage_format, memoryRSS_name, memoryRSS_sum, memoryRSS_max, memoryRSS_avg, memoryRSS_min, memoryRSS_format", generate_test_data(csvfile, update_results_test_data))
def test_update_results_invalid_tests(test_name, expected_status_code, version, experiment_name, start_timestamp, end_timestamp, kubernetes_obj_type, name, namespace, container_image_name, container_name, cpuRequest_name, cpuRequest_sum, cpuRequest_avg, cpuRequest_format, cpuLimit_name, cpuLimit_sum, cpuLimit_avg, cpuLimit_format, cpuUsage_name, cpuUsage_sum, cpuUsage_max, cpuUsage_avg, cpuUsage_min, cpuUsage_format, cpuThrottle_name, cpuThrottle_sum, cpuThrottle_max, cpuThrottle_avg, cpuThrottle_format, memoryRequest_name, memoryRequest_sum, memoryRequest_avg, memoryRequest_format, memoryLimit_name, memoryLimit_sum, memoryLimit_avg, memoryLimit_format, memoryUsage_name, memoryUsage_sum, memoryUsage_max, memoryUsage_avg, memoryUsage_min, memoryUsage_format, memoryRSS_name, memoryRSS_sum, memoryRSS_max, memoryRSS_avg, memoryRSS_min, memoryRSS_format, cluster_type):

    print("\n*******************************************************")
    print("Test - ", test_name)
    print("*******************************************************\n")
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

    # Create experiment using the specified json
    result_json_file="../json_files/update_results_template.json"
    tmp_json_file="/tmp/update_results_" + test_name + ".json"

    environment = Environment(loader=FileSystemLoader("../json_files/"))
    template = environment.get_template("update_results_template.json")

    filename = f"/tmp/update_results_{test_name}.json"
    content = template.render(
        version = version,
        experiment_name = experiment_name,
        start_timestamp = start_timestamp,
        end_timestamp = end_timestamp,
        kubernetes_obj_type = kubernetes_obj_type,
        name = name,
        namespace = namespace,
        container_image_name = container_image_name,
        container_name = container_name,
        cpuRequest_name = cpuRequest_name,
        cpuRequest_sum = cpuRequest_sum,
        cpuRequest_avg = cpuRequest_avg,
        cpuRequest_format = cpuRequest_format,
        cpuLimit_name = cpuLimit_name,
        cpuLimit_sum = cpuLimit_sum,
        cpuLimit_avg = cpuLimit_avg,
        cpuLimit_format = cpuLimit_format,
        cpuUsage_name = cpuUsage_name,
        cpuUsage_sum = cpuUsage_sum,
        cpuUsage_max = cpuUsage_max,
        cpuUsage_avg = cpuUsage_avg,
        cpuUsage_min = cpuUsage_min,
        cpuUsage_format = cpuUsage_format,
        cpuThrottle_name = cpuThrottle_name,
        cpuThrottle_sum = cpuThrottle_sum,
        cpuThrottle_max = cpuThrottle_max,
        cpuThrottle_avg = cpuThrottle_avg,
        cpuThrottle_format = cpuThrottle_format,
        memoryRequest_name = memoryRequest_name,
        memoryRequest_sum = memoryRequest_sum,
        memoryRequest_avg = memoryRequest_avg,
        memoryRequest_format = memoryRequest_format,
        memoryLimit_name = memoryLimit_name,
        memoryLimit_sum = memoryLimit_sum,
        memoryLimit_avg = memoryLimit_avg,
        memoryLimit_format = memoryLimit_format,
        memoryUsage_name = memoryUsage_name,
        memoryUsage_sum = memoryUsage_sum,
        memoryUsage_max = memoryUsage_max,
        memoryUsage_avg = memoryUsage_avg,
        memoryUsage_min = memoryUsage_min,
        memoryUsage_format = memoryUsage_format,
        memoryRSS_name = memoryRSS_name,
        memoryRSS_sum = memoryRSS_sum,
        memoryRSS_max = memoryRSS_max,
        memoryRSS_avg = memoryRSS_avg,
        memoryRSS_min = memoryRSS_min,
        memoryRSS_format = memoryRSS_format
    )
    with open(filename, mode="w", encoding="utf-8") as message:
        message.write(content)

    response = update_results(tmp_json_file)

    data = response.json()
    print(data['message'])
    assert response.status_code == int(expected_status_code)

@pytest.mark.sanity
def test_update_valid_results_after_create_exp(cluster_type):
    """
    Test Description: This test validates update results for a valid experiment
    """
    input_json_file="../json_files/create_exp.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file)
    print("delet exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG

    # Update results for the experiment
    result_json_file="../json_files/update_results.json"
    response = update_results(result_json_file)

    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

    print("content type = ",response.headers["Content-Type"])

@pytest.mark.sanity
def test_update_multiple_valid_results_single_json_after_create_exp(cluster_type):
    """
    Test Description: This test validates update results for a valid experiment by posting multiple results
    """
    input_json_file="../json_files/create_exp.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file)
    print("delet exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG

    # Update results for the experiment
    result_json_file="../json_files/multiple_results_single_exp.json"
    response = update_results(result_json_file)

    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

@pytest.mark.sanity
def test_update_multiple_valid_results_after_create_exp(cluster_type):
    """
    Test Description: This test validates update results for a valid experiment
    """
    input_json_file="../json_files/create_exp.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file)
    print("delet exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS

    # Update results for the experiment
    num_res = 5
    find_start_ts = "2022-01-23T18:25:43.511Z"
    find_end_ts = "2022-01-23T18:40:43.511Z"

    result_json_file="../json_files/update_results.json"
    filename="/tmp/result.json"
    for i in range(num_res):


        with open(result_json_file, 'r') as file:
            data = file.read()

        replace = get_datetime()
        print("replace = ", replace)
        data = data.replace(find_start_ts, replace)

        time.sleep(10)
        replace = get_datetime()
        print("replace = ", replace)
        data = data.replace(find_end_ts, replace)

        with open(filename, 'w') as file:
            file.write(data)
        
        response = update_results(filename)

        data = response.json()
        print("message = ", data['message'])

        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

        print("content type = ",response.headers["Content-Type"])


@pytest.mark.sanity
def test_update_results_multiple_exps_from_same_json_file(cluster_type):
    """
    Test Description: This test validates the response status code of updateResults API by posting
    results of multiple experiments in the same json file. 
    """
    input_json_file="../json_files/create_multiple_exps.json"

    form_kruize_url(cluster_type)

    response = delete_experiment(input_json_file)
    print("delet exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    print("message = ", data['message'])
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG

    # Update results for the experiment
    result_json_file="../json_files/multiple_exps_results.json"
    response = update_results(result_json_file)

    data = response.json()
    print("message = ", data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

@pytest.mark.sanity
def test_update_results_multiple_exps_multiple_containers_from_same_json_file(cluster_type):
    """
    Test Description: This test validates the response status code of updateResults API by posting
    results of multiple experiments with multiple containers in the same json file. 
    """
    input_json_file="../json_files/create_multiple_exps_multiple_containers.json"

    form_kruize_url(cluster_type)

    response = delete_experiment(input_json_file)
    print("delet exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    print("message = ", data['message'])
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG

    # Update results for the experiment
    result_json_file="../json_files/multiple_exps_multiple_containers_results.json"
    response = update_results(result_json_file)

    data = response.json()
    print("message = ", data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

@pytest.mark.sanity
def test_update_results_for_containers_not_present(cluster_type):
    """
    Test Description: This test validates the response status code of updateResults API by posting
    results of multiple experiments with multiple containers in the same json file. 
    """
    input_json_file="../json_files/create_multiple_exps.json"

    form_kruize_url(cluster_type)

    response = delete_experiment(input_json_file)
    print("delet exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    print("message = ", data['message'])
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG

    # Update results for the experiment
    result_json_file="../json_files/multiple_exps_multiple_containers_results.json"
    response = update_results(result_json_file)

    data = response.json()
    print("message = ", data['message'])

    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS

@pytest.mark.sanity
def test_update_results_multiple_exps_from_diff_json_files(cluster_type):
    """
    Test Description: This test validates the updation of results for multiple experiments using different json files
    """

    input_json_file="../json_files/create_exp.json"
    result_json_file="../json_files/update_results.json"

    find = []
    json_data = json.load(open(input_json_file))

    find.append(json_data[0]['experiment_name'])
    find.append(json_data[0]["kubernetes_objects"][0]['name'])
    find.append(json_data[0]["kubernetes_objects"][0]['namespace'])

    form_kruize_url(cluster_type)

    # Create experiment using the specified json
    num_exps = 10
    for i in range(num_exps):
        json_file = "/tmp/create_exp.json"
        generate_json(find, input_json_file, json_file, i)

        response = delete_experiment(json_file)
        print("delete exp = ", response.status_code)

        response = create_experiment(json_file)

        data = response.json()
        print("message = ", data['message'])
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == CREATE_EXP_SUCCESS_MSG

    num_exps = 10
    for i in range(num_exps):
        # Update results for the experiment
        json_file = "/tmp/update_results.json"
        generate_json(find, result_json_file, json_file, i)
        response = update_results(json_file)

        data = response.json()
        print("message = ", data['message'])
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

#@pytest.mark.negative
def test_update_valid_results_without_create_exp(cluster_type):
    """
    Test Description: This test validates the behavior of updateResults API by posting results for a non-existing experiment 
    """
    input_json_file="../json_files/create_exp.json"
    json_data = json.load(open(input_json_file))

    experiment_name = json_data[0]['experiment_name']
    print("experiment_name = ", experiment_name)


    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    result_json_file="../json_files/update_results.json"
    response = update_results(result_json_file)

    data = response.json()
    print("message = ", data['message'])

    EXP_NAME_NOT_FOUND_MSG = "Experiment name : " + experiment_name + " not found"
    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == EXP_NAME_NOT_FOUND_MSG

@pytest.mark.sanity
def test_update_results_with_same_result(cluster_type):
    """
    Test Description: This test validates update results for a valid experiment
    """
    input_json_file="../json_files/create_exp.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file)
    print("delet exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG

    # Update results for the experiment
    result_json_file="../json_files/update_results.json"
    response = update_results(result_json_file)

    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

    # Post the same result again
    response = update_results(result_json_file)

    data = response.json()
    assert response.status_code == ERROR_409_STATUS_CODE
    assert data['status'] == ERROR_STATUS

    exp_json_data = read_json_data_from_file(input_json_file)
    experiment_name = exp_json_data[0]['experiment_name']

    result_json_data = read_json_data_from_file(result_json_file)
    end_timestamp = result_json_data[0]['end_timestamp']

    TIMESTAMP_PRESENT_MSG = "Experiment name : " + experiment_name + " already contains result for timestamp : " + end_timestamp
    assert data['message'] == TIMESTAMP_PRESENT_MSG

    response = delete_experiment(input_json_file)
    print("delet exp = ", response.status_code)
