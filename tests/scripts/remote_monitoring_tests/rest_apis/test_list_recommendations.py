import requests
import pytest
from jinja2 import Environment, FileSystemLoader
from helpers.list_reco_json_validate import *
from helpers.list_reco_json_schema import *
from helpers.utils import *
from helpers.generate_rm_jsons import *
from helpers.kruize import *
from helpers.fixtures import *
import time
import json
import shutil

@pytest.mark.sanity
def test_list_recommendations_single_result(cluster_type):
    """
    Test Description: This test validates listRecommendations by passing a valid experiment name
    and updating a single result
    """
    input_json_file="../json_files/create_exp.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

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

    time.sleep(1)

    # Get the experiment name
    json_data = json.load(open(input_json_file))
    experiment_name = json_data[0]['experiment_name']

    response = list_recommendations(experiment_name)

    list_reco_json = response.json()
    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json values
    create_exp_json = read_json_data_from_file(input_json_file)
    update_results_json = read_json_data_from_file(result_json_file)

    validate_reco_json(create_exp_json[0], update_results_json, list_reco_json[0])

    # Delete the experiment
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

@pytest.mark.sanity
def test_list_recommendations_without_parameters(cluster_type):
    """
    Test Description: This test validates listRecommendations API without parameters
    """
    input_json_file="../json_files/create_exp.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG

    # Update results for the same experiment
    result_json_file="../json_files/multiple_results_single_exp.json"

    result_json_arr = read_json_data_from_file(result_json_file)
    for result_json in result_json_arr:
        single_json_arr = []
        json_file = "/tmp/update_results.json"
        single_json_arr.append(result_json)
        write_json_data_to_file(json_file, single_json_arr)

        response = update_results(json_file)

        data = response.json()
        print(data['message'])

        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

    time.sleep(1)

    # Get the experiment name
    experiment_name = None
    response = list_recommendations(experiment_name)

    list_reco_json = response.json()
    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json against the json schema
    errorMsg = validate_list_reco_json(list_reco_json, list_reco_json_schema)
    assert errorMsg == ""

    # Validate the json values
    create_exp_json = read_json_data_from_file(input_json_file)
    update_results_json = []
    update_results_json.append(result_json_arr[len(result_json_arr)-1])

    # Expected duration in hours is 24h as for short term only 24h plus or minus 30s of data is considered to generate recommendations
    expected_duration_in_hours = SHORT_TERM_DURATION_IN_HRS_MAX
    validate_reco_json(create_exp_json[0], update_results_json, list_reco_json[0], expected_duration_in_hours)

    # Delete the experiment
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

@pytest.mark.negative
def test_list_recommendations_invalid_exp(cluster_type):
    """
    Test Description: This test validates listRecommendations by passing an invalid experiment name
    """
    input_json_file="../json_files/create_exp.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

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

    # Get the experiment name
    experiment_name = "xyz"

    response = list_recommendations(experiment_name)

    data = response.json()
    print(data)
    assert response.status_code == ERROR_STATUS_CODE
    INVALID_EXP_NAME_MSG = "Given experiment name - \" " + experiment_name + " \" is not valid"
    assert data['message'] == INVALID_EXP_NAME_MSG, f"expected - {INVALID_EXP_NAME_MSG}, actual - {data['message']}"

    # Delete the experiment
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

@pytest.mark.sanity
def test_list_recommendations_without_results(cluster_type):
    """
    Test Description: This test validates listRecommendations when there was no updation of results
    """
    input_json_file="../json_files/create_exp.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG

    time.sleep(1)

    # Get the experiment name
    json_data = json.load(open(input_json_file))
    experiment_name = json_data[0]['experiment_name']

    response = list_recommendations(experiment_name)

    list_reco_json = response.json()
    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json values
    create_exp_json = read_json_data_from_file(input_json_file)

    # Validate recommendation message
    update_results_json = None
    validate_reco_json(create_exp_json[0], update_results_json, list_reco_json[0])

    # Delete the experiment
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

@pytest.mark.sanity
def test_list_recommendations_single_exp_multiple_results(cluster_type):
    """
    Test Description: This test validates listRecommendations by updating multiple results for a single experiment
    """
    input_json_file="../json_files/create_exp.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

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
    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == "Bulk entries are currently unsupported!"

    time.sleep(1)

    # Get the experiment name
    json_data = json.load(open(input_json_file))
    experiment_name = json_data[0]['experiment_name']

    response = list_recommendations(experiment_name)

    list_reco_json = response.json()
    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json against the json schema
    errorMsg = validate_list_reco_json(list_reco_json, list_reco_json_schema)
    assert errorMsg == ""

    # Validate the json values
    create_exp_json = read_json_data_from_file(input_json_file)

    # Uncomment the below lines when bulk entries are allowed
    # update_results_json = read_json_data_from_file(result_json_file)

    # Since bulk entries are not supported passing None for update results json
    update_results_json = None
    validate_reco_json(create_exp_json[0], update_results_json, list_reco_json[0])

    # Delete the experiment
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

@pytest.mark.extended
def test_list_recommendations_multiple_exps_from_diff_json_files_2(cluster_type):
    """
    Test Description: This test validates list recommendations for multiple experiments posted using different json files
    """
    num_exps = 6
    num_res = 120

    split = False
    split_count = 1

    metrics_csv = "../csv_data/tfb_data.csv"
    exp_jsons_dir = "/tmp/exp_jsons"
    result_jsons_dir = "/tmp/result_jsons"

    # Create the create experiment jsons
    create_exp_jsons(split, split_count, exp_jsons_dir, num_exps)

    # Create the update result jsons
    create_update_results_jsons(metrics_csv, split, split_count, result_jsons_dir, num_exps, num_res)

    # Form the Kruize service URL
    form_kruize_url(cluster_type)

    for i in range(num_exps):

        # Create experiment using the specified json
        create_exp_json_file = exp_jsons_dir + "/create_exp_" + str(i) + ".json"

        response = delete_experiment(create_exp_json_file)
        print("delete exp = ", response.status_code)

        response = create_experiment(create_exp_json_file)

        data = response.json()
        json_data = json.load(open(create_exp_json_file))
        obj_type = json_data[0]['kubernetes_objects'][0]["type"]

        print(f"kubernetes_object_type = {obj_type}")

        print("message = ", data['message'])

        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == CREATE_EXP_SUCCESS_MSG

        result_json_arr = []
        for j in range(num_res):
            # Update results for the experiment
            result_json_file = result_jsons_dir + "/result_" + str(i) + "_" + str(j) + ".json"

            response = update_results(result_json_file)
            data = response.json()

            print("message = ", data['message'])
            assert response.status_code == SUCCESS_STATUS_CODE
            assert data['status'] == SUCCESS_STATUS
            assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG, f"expected message = {UPDATE_RESULTS_SUCCESS_MSG} actual message = {data['message']}"

            result_json_data = read_json_data_from_file(result_json_file)
            result_json_arr.append(result_json_data[0])

        # Get the experiment name
        json_data = json.load(open(create_exp_json_file))
        experiment_name = json_data[0]['experiment_name']

        # Invoke list recommendations for the specified experiment
        response = list_recommendations(experiment_name)
        assert response.status_code == SUCCESS_200_STATUS_CODE

        list_reco_json = response.json()

        # Validate the json against the json schema
        errorMsg = validate_list_reco_json(list_reco_json, list_reco_json_schema)
        assert errorMsg == ""

        create_exp_json = read_json_data_from_file(create_exp_json_file)
        update_results_json = []
        update_results_json.append(result_json_arr[len(result_json_arr)-1])

        # Expected duration in hours is 24h as for short term only 24h plus or minus 30s of data is considered to generate recommendations
        expected_duration_in_hours = SHORT_TERM_DURATION_IN_HRS_MAX
        validate_reco_json(create_exp_json[0], update_results_json, list_reco_json[0], expected_duration_in_hours)

    # Delete the experiments
    for i in range(num_exps):
        create_exp_json_file = exp_jsons_dir + "/create_exp_" + str(i) + ".json"

        response = delete_experiment(create_exp_json_file)
        print("delete exp = ", response.status_code)


@pytest.mark.sanity
@pytest.mark.parametrize("latest", ["true", "false"])
def test_list_recommendations_exp_name_and_latest(latest, cluster_type):
    """
    Test Description: This test validates listRecommendations by passing a valid experiment name and latest as true or false
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

    # Update results for the same experiment
    result_json_file="../json_files/multiple_results_single_exp.json"

    result_json_arr = read_json_data_from_file(result_json_file)
    for result_json in result_json_arr:
        single_json_arr = []
        json_file = "/tmp/update_results.json"
        single_json_arr.append(result_json)
        write_json_data_to_file(json_file, single_json_arr)

        response = update_results(json_file)

        data = response.json()
        print(data['message'])

        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

    time.sleep(1)
    # Get the experiment name
    json_data = json.load(open(input_json_file))
    experiment_name = json_data[0]['experiment_name']

    response = list_recommendations(experiment_name, latest)

    list_reco_json = response.json()
    assert response.status_code == SUCCESS_200_STATUS_CODE

    update_results_json = []
    if latest == "true":
        update_results_json.append(result_json_arr[len(result_json_arr)-1])
        # Expected duration in hours is 24h as for short term only 24h plus or minus 30s of data is considered to generate recommendations
        expected_duration_in_hours = SHORT_TERM_DURATION_IN_HRS_MAX

        # Expected no. of recommendations is 1 as there would be only one recommendation with latest = true
        expected_num_recos = 1
    elif latest == "false":
        expected_duration_in_hours = SHORT_TERM_DURATION_IN_HRS_MAX
        print(f"len update results json {len(update_results_json)}")
        # Recommendations are generated only when 24h results are present
        total_num_results = len(result_json_arr)
        num_results_without_recos = int(SHORT_TERM_DURATION_IN_HRS_MAX * 4 - 1)
        expected_num_recos = total_num_results - num_results_without_recos

        # Fetch only the results corresponding to the recommendations generated 
        for i in range(num_results_without_recos, total_num_results):
            update_results_json.append(result_json_arr[i])

    data = list_reco_json[0]["kubernetes_objects"][0]["containers"][0]["recommendations"]["data"]
    actual_num_recos = len(data)
    assert actual_num_recos == expected_num_recos, f"Number of recommendations when latest is {latest} should be {expected_num_recos} but was {actual_num_recos}"

    # Validate the json against the json schema
    errorMsg = validate_list_reco_json(list_reco_json, list_reco_json_schema)
    assert errorMsg == ""

    # Validate the json values
    create_exp_json = read_json_data_from_file(input_json_file)

    validate_reco_json(create_exp_json[0], update_results_json, list_reco_json[0], expected_duration_in_hours)

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

@pytest.mark.negative
@pytest.mark.parametrize("monitoring_end_time", ["2022-12-20T21:10:11", "20220211"])
def test_list_recommendations_exp_name_and_monitoring_end_time_invalid(monitoring_end_time, cluster_type):
    """
    Test Description: This test validates listRecommendations by passing a valid experiment name and an invalid monitoring end time value
    """
    input_json_file="../json_files/create_exp.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

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

    # Get the experiment name
    json_data = json.load(open(input_json_file))
    experiment_name = json_data[0]['experiment_name']

    latest = None
    response = list_recommendations(experiment_name, latest, monitoring_end_time)
    list_reco_json = response.json()

    print(list_reco_json['message'])
    ERROR_MSG = "Given timestamp - \" " + monitoring_end_time + " \" is not a valid timestamp format"
    assert response.status_code == ERROR_STATUS_CODE
    assert list_reco_json['message'] == ERROR_MSG

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

@pytest.mark.sanity
@pytest.mark.parametrize("test_name, monitoring_end_time", \
            [("valid_monitoring_end_time", "2023-04-14T22:59:20.982Z"), ("invalid_monitoring_end_time","2018-12-20T23:40:15.000Z")])
def test_list_recommendations_exp_name_and_monitoring_end_time(test_name, monitoring_end_time, cluster_type):
    """
    Test Description: This test validates listRecommendations by passing a valid experiment name
                      and a valid monitoring end time and an invalid monitoring end time
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

    # Update results for the same experiment
    result_json_file="../json_files/multiple_results_single_exp.json"

    result_json_arr = read_json_data_from_file(result_json_file)
    for result_json in result_json_arr:
        single_json_arr = []
        json_file = "/tmp/update_results.json"
        single_json_arr.append(result_json)
        write_json_data_to_file(json_file, single_json_arr)

        response = update_results(json_file)

        data = response.json()
        print(data['message'])

        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

    time.sleep(1)
    # Get the experiment name
    json_data = json.load(open(input_json_file))
    experiment_name = json_data[0]['experiment_name']

    latest = None
    response = list_recommendations(experiment_name, latest, monitoring_end_time)

    list_reco_json = response.json()

    update_results_json = []
    if test_name == "valid_monitoring_end_time":
        assert response.status_code == SUCCESS_200_STATUS_CODE
        for result in result_json_arr:
            if result['interval_end_time'] == monitoring_end_time:
                update_results_json.append(result)
                expected_duration_in_hours = SHORT_TERM_DURATION_IN_HRS_MAX
        # Validate the json against the json schema
        errorMsg = validate_list_reco_json(list_reco_json, list_reco_json_schema)
        assert errorMsg == ""

        # Validate the json values
        create_exp_json = read_json_data_from_file(input_json_file)

        validate_reco_json(create_exp_json[0], update_results_json, list_reco_json[0], expected_duration_in_hours, test_name)
    elif test_name == "invalid_monitoring_end_time":
        print(list_reco_json)
        assert response.status_code == ERROR_STATUS_CODE
        ERROR_MSG = "Recommendation for timestamp - \" " + monitoring_end_time + " \" does not exist"
        assert list_reco_json['message'] == ERROR_MSG

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

@pytest.mark.sanity
def test_list_recommendations_multiple_exps_with_missing_metrics(cluster_type):
    """
    Test Description: This test validates list recommendations for multiple experiments posted using different json files
                      with some of the mandatory metrics missing in the results
    """

    input_json_file="../json_files/create_exp.json"
    result_json_file="../json_files/update_results.json"

    find = []
    json_data = json.load(open(input_json_file))

    find.append(json_data[0]['experiment_name'])
    find.append(json_data[0]['kubernetes_objects'][0]['name'])
    find.append(json_data[0]['kubernetes_objects'][0]['namespace'])

    form_kruize_url(cluster_type)

    drop_metrics = {"cpuRequest":0, "cpuLimit":1, "cpuThrottle":3, "memoryRequest":4, "memoryLimit":5}
    keys = list(drop_metrics.keys())
    j = 0
    num_exps = 10
    for i in range(num_exps):
        create_exp_json_file = "/tmp/create_exp_" + str(i) + ".json"
        generate_json(find, input_json_file, create_exp_json_file, i)

        response = delete_experiment(create_exp_json_file)
        print("delete exp = ", response.status_code)

        response = create_experiment(create_exp_json_file)

        data = response.json()
        print("message = ", data['message'])
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == CREATE_EXP_SUCCESS_MSG

        # Update results for the experiment
        update_results_json_file = "/tmp/update_results_" + str(i) + ".json"
        update_timestamps = True
        generate_json(find, result_json_file, update_results_json_file, i, update_timestamps)

        result_json = read_json_data_from_file(update_results_json_file)
        if i % 2 == 0:
            result_json[0]['kubernetes_objects'][0]['containers'][0]['metrics'].pop(drop_metrics[keys[j]])
            tmp_update_results_json_file = "/tmp/update_results_metric" + str(drop_metrics[keys[j]]) + ".json"
            write_json_data_to_file(tmp_update_results_json_file, result_json)
            response = update_results(tmp_update_results_json_file)
            j += 1
        else:
            response = update_results(update_results_json_file)

        data = response.json()
        print("message = ", data['message'])
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

        time.sleep(2)

        # Get the experiment name
        json_data = json.load(open(create_exp_json_file))
        experiment_name = json_data[0]['experiment_name']

        response = list_recommendations(experiment_name)

        list_reco_json = response.json()
        assert response.status_code == SUCCESS_200_STATUS_CODE

        # Validate the json against the json schema
        errorMsg = validate_list_reco_json(list_reco_json, list_reco_json_schema)
        assert errorMsg == ""

        # Validate the json values
        create_exp_json = read_json_data_from_file(create_exp_json_file)
        update_results_json = read_json_data_from_file(update_results_json_file)

        validate_reco_json(create_exp_json[0], update_results_json, list_reco_json[0])

    # Delete the experiments
    for i in range(num_exps):
        create_exp_json_file = "/tmp/create_exp_" + str(i) + ".json"
        response = delete_experiment(create_exp_json_file)
        print("delete exp = ", response.status_code)

@pytest.mark.extended
@pytest.mark.parametrize("latest", ["true", "false"])
def test_list_recommendations_with_only_latest(latest, cluster_type):
    """
    Test Description: This test validates list recommendations for multiple experiments posted using different json files
                      and query with only the parameter latest and with both latest=true and latest=false
    """

    input_json_file="../json_files/create_exp.json"
    result_json_file="../json_files/update_results.json"

    find = []
    json_data = json.load(open(input_json_file))

    find.append(json_data[0]['experiment_name'])
    find.append(json_data[0]['kubernetes_objects'][0]['name'])
    find.append(json_data[0]['kubernetes_objects'][0]['namespace'])

    form_kruize_url(cluster_type)

    # Create experiment using the specified json
    num_exps = 3
    num_res = 100
    list_of_result_json_arr = []
    for i in range(num_exps):
        create_exp_json_file = "/tmp/create_exp_" + str(i) + ".json"
        generate_json(find, input_json_file, create_exp_json_file, i)

        response = delete_experiment(create_exp_json_file)
        print("delete exp = ", response.status_code)

        response = create_experiment(create_exp_json_file)

        data = response.json()
        print("message = ", data['message'])
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == CREATE_EXP_SUCCESS_MSG

        # Update results for the experiment
        update_results_json_file = "/tmp/update_results_" + str(i) + ".json"

        result_json_arr = []
        for j in range(num_res):
            update_timestamps = True
            generate_json(find, result_json_file, update_results_json_file, i, update_timestamps)
            result_json = read_json_data_from_file(update_results_json_file)
            if j == 0:
                start_time = get_datetime()
            else:
                start_time = end_time

            result_json[0]['interval_start_time'] = start_time
            end_time = increment_timestamp_by_given_mins(start_time, 15)
            result_json[0]['interval_end_time'] = end_time

            write_json_data_to_file(update_results_json_file, result_json)
            result_json_arr.append(result_json[0])
            response = update_results(update_results_json_file)

            data = response.json()
            print("message = ", data['message'])
            assert response.status_code == SUCCESS_STATUS_CODE
            assert data['status'] == SUCCESS_STATUS
            assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

            time.sleep(2)

            # Get the experiment name
            json_data = json.load(open(create_exp_json_file))
            experiment_name = json_data[0]['experiment_name']

            response = list_recommendations(experiment_name)
            assert response.status_code == SUCCESS_200_STATUS_CODE

        list_of_result_json_arr.append(result_json_arr)

    time.sleep(10)

    experiment_name = None
    response = list_recommendations(experiment_name, latest)

    list_reco_json = response.json()
    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json against the json schema
    errorMsg = validate_list_reco_json(list_reco_json, list_reco_json_schema)
    assert errorMsg == ""
    for i in range(num_exps):
        create_exp_json_file = "/tmp/create_exp_" + str(i) + ".json"
        update_results_json_file = "/tmp/update_results_" + str(i) + ".json"

        # Validate the json values
        create_exp_json = read_json_data_from_file(create_exp_json_file)

        update_results_json = []

        if latest == "true":
            update_results_json.append(list_of_result_json_arr[i][len(list_of_result_json_arr[i])-1])
            # Expected duration in hours is 24h as for short term only 24h plus or minus 30s of data is considered to generate recommendations
            expected_duration_in_hours = SHORT_TERM_DURATION_IN_HRS_MAX
        elif latest == "false":
            expected_duration_in_hours = SHORT_TERM_DURATION_IN_HRS_MAX
            total_num_results = len(list_of_result_json_arr[i])

            # Recommendations will be generated when 24h results is available
            num_results_without_recos = int(SHORT_TERM_DURATION_IN_HRS_MAX * 4 - 1)
            for j in range(num_results_without_recos, total_num_results):
                update_results_json.append(list_of_result_json_arr[i][j])

        exp_found = False
        for list_reco in list_reco_json:
            if create_exp_json[0]['experiment_name'] == list_reco['experiment_name']:
                validate_reco_json(create_exp_json[0], update_results_json, list_reco, expected_duration_in_hours)
                exp_found = True
            continue

        assert exp_found == True, f"Experiment name {create_exp_json[0]['experiment_name']} not found in listRecommendations!"

    # Delete the experiments
    for i in range(num_exps):
        json_file = "/tmp/create_exp_" + str(i) + ".json"

        response = delete_experiment(json_file)
        print("delete exp = ", response.status_code)
