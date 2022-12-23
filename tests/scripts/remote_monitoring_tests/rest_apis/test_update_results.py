import requests
import pytest
from jinja2 import Environment, FileSystemLoader
from helpers.utils import *
from helpers.kruize import *
from helpers.fixtures import *
import time

csvfile = "/tmp/update_results_test_data.csv"

@pytest.mark.negative
@pytest.mark.parametrize("test_name, expected_status_code, experiment_name, deployment_name, start_timestamp, end_timestamp, namespace, image, container_name, cpuRequest_sum, cpuRequest_mean, cpuRequest_units, cpuLimit_sum, cpuLimit_mean, cpuLimit_units, cpuUsage_max, cpuUsage_mean, cpuUsage_units, cpuThrottle_max, cpuThrottle_mean, cpuThrottle_units, memoryRequest_sum, memoryRequest_mean, memoryRequest_units, memoryLimit_sum, memoryLimit_mean, memoryLimit_units, memoryUsage_max, memoryUsage_mean, memoryUsage_units, memoryRSS_max, memoryRSS_mean, memoryRSS_units", generate_test_data(csvfile, update_results_test_data))
def test_update_results_invalid_tests(test_name, expected_status_code, experiment_name, deployment_name, start_timestamp, end_timestamp, namespace, image, container_name, cpuRequest_sum, cpuRequest_mean, cpuRequest_units, cpuLimit_sum, cpuLimit_mean, cpuLimit_units, cpuUsage_max, cpuUsage_mean, cpuUsage_units, cpuThrottle_max, cpuThrottle_mean, cpuThrottle_units, memoryRequest_sum, memoryRequest_mean, memoryRequest_units, memoryLimit_sum, memoryLimit_mean, memoryLimit_units, memoryUsage_max, memoryUsage_mean, memoryUsage_units, memoryRSS_max, memoryRSS_mean, memoryRSS_units, cluster_type):

    print("\n*******************************************************")
    print("Test - ", test_name)
    print("*******************************************************\n")
    input_json_file="../json_files/create_exp.json"

    form_kruize_url(cluster_type)
    
    response = delete_experiment(input_json_file)
    print("delet exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    print(data['message'])
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG

    # Create experiment using the specified json
    result_json_file="../json_files/update_results_template.json"
    tmp_json_file="/tmp/update_result_" + test_name + ".json"

    environment = Environment(loader=FileSystemLoader("../json_files/"))
    template = environment.get_template("update_results_template.json")

    filename = f"/tmp/update_result_{test_name}.json"
    content = template.render(
        experiment_name=experiment_name,
        deployment_name=deployment_name,
        start_timestamp=start_timestamp,
        end_timestamp=end_timestamp,
        namespace=namespace,
        image=image,
        container_name = container_name,
        cpuRequest_sum = cpuRequest_sum,
        cpuRequest_mean = cpuRequest_mean,
        cpuRequest_units = cpuRequest_units,
        cpuLimit_sum = cpuLimit_sum,
        cpuLimit_mean = cpuLimit_mean,
        cpuLimit_units = cpuLimit_units,
        cpuUsage_max = cpuUsage_max,
        cpuUsage_mean = cpuUsage_mean,
        cpuUsage_units = cpuUsage_units,
        cpuThrottle_max = cpuThrottle_max,
        cpuThrottle_mean = cpuThrottle_mean,
        cpuThrottle_units = cpuThrottle_units,
        memoryRequest_sum = memoryRequest_sum,
        memoryRequest_mean = memoryRequest_mean,
        memoryRequest_units = memoryRequest_units,
        memoryLimit_sum = memoryLimit_sum,
        memoryLimit_mean = memoryLimit_mean,
        memoryLimit_units = memoryLimit_units,
        memoryUsage_max = memoryUsage_max,
        memoryUsage_mean = memoryUsage_mean,
        memoryUsage_units = memoryUsage_units,
        memoryRSS_max = memoryRSS_max,
        memoryRSS_mean = memoryRSS_mean,
        memoryRSS_units = memoryRSS_units
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
    #assert response.headers["Content-Type"] == "application/json"

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

    assert response.status_code == SUCCESS_STATUS_CODE
    data = response.json()
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

        assert response.status_code == SUCCESS_STATUS_CODE
        data = response.json()
        assert data['status'] == SUCCESS_STATUS

        print("content type = ",response.headers["Content-Type"])


@pytest.mark.sanity
def test_update_results_multiple_exps_from_same_json_file(cluster_type):
    """
    Test Description: This test validates the response status code of updateResults API by posting
    results of multiple experiments in the same json file. 
    """
    input_json_file="../json_files/multiple_exps.json"

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

    assert data['status'] == SUCCESS_STATUS
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

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
    find.append(json_data[0]['deployment_name'])
    find.append(json_data[0]['namespace'])

    form_kruize_url(cluster_type)

    # Create experiment using the specified json
    num_exps = 10
    for i in range(num_exps):
        json_file = "/tmp/create_exp.json"
        generate_json(find, input_json_file, json_file, i)

        response = delete_experiment(json_file)
        print("delet exp = ", response.status_code)

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

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file)
    print("delet exp = ", response.status_code)

    # Create experiment using the specified json
    result_json_file="../json_files/update_results.json"
    response = update_results(result_json_file)

    assert response.status_code == ERROR_STATUS_CODE
    data = response.json()
    assert data['status'] == ERROR_STATUS


