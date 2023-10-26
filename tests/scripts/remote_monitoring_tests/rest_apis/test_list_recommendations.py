import datetime
import json

import pytest
from helpers.fixtures import *
from helpers.generate_rm_jsons import *
from helpers.kruize import *
from helpers.list_reco_json_schema import *
from helpers.list_reco_json_validate import *
from helpers.utils import *


@pytest.mark.sanity
def test_list_recommendations_single_result(cluster_type):
    """
    Test Description: This test validates listRecommendations by passing a valid experiment name
    and updating a single result
    """
    input_json_file = "../json_files/create_exp.json"

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
    result_json_file = "../json_files/update_results.json"
    json_data = json.load(open(result_json_file))
    experiment_name = json_data[0]['experiment_name']
    result_json = read_json_data_from_file(result_json_file)
    end_time = result_json[0]['interval_end_time']
    response = update_results(result_json_file)

    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

    # Update recommendations for the experiment
    response = update_recommendations(experiment_name, None, end_time)
    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data[0]['experiment_name'] == experiment_name
    assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications']['120001'][
               'message'] == 'There is not enough data available to generate a recommendation.'

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
    input_json_file = "../json_files/create_exp.json"

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
    result_json_file = "../json_files/multiple_results_single_exp.json"

    result_json_arr = read_json_data_from_file(result_json_file)
    response = update_results(result_json_file)

    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

    start_time = '2023-04-13T22:59:20.982Z'
    end_time = '2023-04-14T23:59:20.982Z'
    # Get the experiment name
    json_data = json.load(open(input_json_file))
    experiment_name = json_data[0]['experiment_name']
    response = update_recommendations(experiment_name, None, end_time)
    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data[0]['experiment_name'] == experiment_name
    assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications']['111000'][
               'message'] == 'Recommendations Are Available'

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
    update_results_json.append(result_json_arr[len(result_json_arr) - 1])

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
    input_json_file = "../json_files/create_exp.json"

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
    result_json_file = "../json_files/update_results.json"
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
    input_json_file = "../json_files/create_exp.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG

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
    input_json_file = "../json_files/create_exp.json"

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
    result_json_file = "../json_files/multiple_results_single_exp.json"
    response = update_results(result_json_file)

    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

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
        start_time = None
        end_time = None
        for j in range(num_res):
            # Update results for the experiment
            result_json_file = result_jsons_dir + "/result_" + str(i) + "_" + str(j) + ".json"
            result_json = read_json_data_from_file(result_json_file)
            if start_time is None:
                start_time = result_json[0]['interval_start_time']
            end_time = result_json[0]['interval_end_time']

            response = update_results(result_json_file)
            data = response.json()

            print("message = ", data['message'])
            assert response.status_code == SUCCESS_STATUS_CODE
            assert data['status'] == SUCCESS_STATUS
            assert data[
                       'message'] == UPDATE_RESULTS_SUCCESS_MSG, f"expected message = {UPDATE_RESULTS_SUCCESS_MSG} actual message = {data['message']}"

            result_json_data = read_json_data_from_file(result_json_file)
            result_json_arr.append(result_json_data[0])

        # Get the experiment name
        json_data = json.load(open(create_exp_json_file))
        experiment_name = json_data[0]['experiment_name']

        # Update Recommendations
        response = update_recommendations(experiment_name, None, end_time)
        data = response.json()
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data[0]['experiment_name'] == experiment_name
        assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications']['112101'][
                   'message'] == 'Cost Recommendations Available'

        # Invoke list recommendations for the specified experiment
        response = list_recommendations(experiment_name)
        assert response.status_code == SUCCESS_200_STATUS_CODE

        list_reco_json = response.json()

        # Validate the json against the json schema
        errorMsg = validate_list_reco_json(list_reco_json, list_reco_json_schema)
        assert errorMsg == ""

        create_exp_json = read_json_data_from_file(create_exp_json_file)
        update_results_json = []
        update_results_json.append(result_json_arr[len(result_json_arr) - 1])

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

    # Update results for the same experiment
    result_json_file = "../json_files/multiple_results_single_exp.json"
    result_json_arr = read_json_data_from_file(result_json_file)
    response = update_results(result_json_file)
    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

    # update Recommendations
    with open(result_json_file, 'r') as file:
        data = json.load(file)

    # Step 2: Convert UTC strings to datetime objects
    for item in data:
        item['interval_start_time'] = datetime.strptime(item['interval_start_time'], "%Y-%m-%dT%H:%M:%S.%fZ")
        item['interval_end_time'] = datetime.strptime(item['interval_end_time'], "%Y-%m-%dT%H:%M:%S.%fZ")

    # Step 3: Find minimum start_time and maximum end_time
    start_time = min(data, key=lambda x: x['interval_start_time'])['interval_start_time']
    end_time = max(data, key=lambda x: x['interval_end_time'])['interval_end_time']

    sorted_data = sorted(data, key=lambda x: x['interval_end_time'], reverse=True)
    top_5_records = sorted_data[:5]
    top_5_dates = [
        item['interval_start_time'] for item in sorted_data[:5]
    ]
    print(f"{top_5_dates}")
    # Get the experiment name
    json_data = json.load(open(input_json_file))
    experiment_name = json_data[0]['experiment_name']

    for dateStr in top_5_dates:
        update_recommendations(experiment_name, None,
                               dateStr.strftime("%Y-%m-%dT%H:%M:%S.%fZ")[:-4] + "Z")

    response = update_recommendations(experiment_name, None,
                                      end_time.strftime("%Y-%m-%dT%H:%M:%S.%fZ")[:-4] + "Z")
    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data[0]['experiment_name'] == experiment_name
    assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications']['111000'][
               'message'] == 'Recommendations Are Available'

    response = list_recommendations(experiment_name, latest)

    list_reco_json = response.json()
    assert response.status_code == SUCCESS_200_STATUS_CODE

    update_results_json = []
    if latest == "true":
        update_results_json.append(result_json_arr[len(result_json_arr) - 1])
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
        print(f"total_num_results {total_num_results}")
        print(f"num_results_without_recos {num_results_without_recos}")
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
    input_json_file = "../json_files/create_exp.json"

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
    result_json_file = "../json_files/update_results.json"
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
                         [("valid_monitoring_end_time", "2023-04-14T22:59:20.982Z"),
                          ("invalid_monitoring_end_time", "2018-12-20T23:40:15.000Z")])
def test_list_recommendations_exp_name_and_monitoring_end_time(test_name, monitoring_end_time, cluster_type):
    """
    Test Description: This test validates listRecommendations by passing a valid experiment name
                      and a valid monitoring end time and an invalid monitoring end time
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

    # Update results for the same experiment
    result_json_file = "../json_files/multiple_results_single_exp.json"
    result_json_arr = read_json_data_from_file(result_json_file)
    response = update_results(result_json_file)
    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG
    # update Recommendations
    with open(result_json_file, 'r') as file:
        data = json.load(file)

    # Get the experiment name
    json_data = json.load(open(input_json_file))
    experiment_name = json_data[0]['experiment_name']

    # Step 2: Convert UTC strings to datetime objects
    for item in data:
        item['interval_start_time'] = datetime.strptime(item['interval_start_time'], "%Y-%m-%dT%H:%M:%S.%fZ")
        item['interval_end_time'] = datetime.strptime(item['interval_end_time'], "%Y-%m-%dT%H:%M:%S.%fZ")
        update_recommendations(experiment_name, None,
                               item['interval_end_time'].strftime("%Y-%m-%dT%H:%M:%S.%fZ")[:-4] + "Z")

    # Step 3: Find minimum start_time and maximum end_time
    start_time = min(data, key=lambda x: x['interval_start_time'])['interval_start_time']
    end_time = max(data, key=lambda x: x['interval_end_time'])['interval_end_time']

    response = update_recommendations(experiment_name, None,
                                      end_time.strftime("%Y-%m-%dT%H:%M:%S.%fZ")[:-4] + "Z")
    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data[0]['experiment_name'] == experiment_name
    assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications']['111000'][
               'message'] == 'Recommendations Are Available'

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

        validate_reco_json(create_exp_json[0], update_results_json, list_reco_json[0], expected_duration_in_hours,
                           test_name)
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

    input_json_file = "../json_files/create_exp.json"
    result_json_file = "../json_files/update_results.json"

    find = []
    json_data = json.load(open(input_json_file))

    find.append(json_data[0]['experiment_name'])
    find.append(json_data[0]['kubernetes_objects'][0]['name'])
    find.append(json_data[0]['kubernetes_objects'][0]['namespace'])

    form_kruize_url(cluster_type)

    drop_metrics = {"cpuRequest": 0, "cpuLimit": 1, "cpuThrottle": 3, "memoryRequest": 4, "memoryLimit": 5}
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

    input_json_file = "../json_files/create_exp.json"
    result_json_file = "../json_files/update_results.json"

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
        # Get the experiment name
        json_data = json.load(open(create_exp_json_file))
        experiment_name = json_data[0]['experiment_name']
        interval_start_time = get_datetime()
        for j in range(num_res):
            update_timestamps = True
            generate_json(find, result_json_file, update_results_json_file, i, update_timestamps)
            result_json = read_json_data_from_file(update_results_json_file)
            if j == 0:
                start_time = interval_start_time
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

            update_recommendations(experiment_name, None, end_time)

            # Get the experiment name
            json_data = json.load(open(create_exp_json_file))
            experiment_name = json_data[0]['experiment_name']

            response = list_recommendations(experiment_name)
            assert response.status_code == SUCCESS_200_STATUS_CODE

        list_of_result_json_arr.append(result_json_arr)

        response = update_recommendations(experiment_name, None, end_time)
        data = response.json()
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data[0]['experiment_name'] == experiment_name
        assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications']['112101'][
                   'message'] == 'Cost Recommendations Available'
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
            update_results_json.append(list_of_result_json_arr[i][len(list_of_result_json_arr[i]) - 1])
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


@pytest.mark.sanity
def test_list_recommendations_notification_codes(cluster_type: str):
    """
        Test Description: This test validates list recommendations for multiple experiments posted using different json files
                          and pass different update results to test the notifications provided
        """

    input_json_file = "../json_files/create_exp.json"
    result_json_file = "../json_files/update_results.json"

    find = []
    json_data = json.load(open(input_json_file))

    find.append(json_data[0]['experiment_name'])
    find.append(json_data[0]['kubernetes_objects'][0]['name'])
    find.append(json_data[0]['kubernetes_objects'][0]['namespace'])

    form_kruize_url(cluster_type)

    # Create experiment using the specified json
    num_exps = 1
    num_res = 120
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
        # Get the experiment name
        json_data = json.load(open(create_exp_json_file))
        experiment_name = json_data[0]['experiment_name']
        interval_start_time = get_datetime()
        for j in range(num_res):
            update_timestamps = True
            generate_json(find, result_json_file, update_results_json_file, i, update_timestamps)
            result_json = read_json_data_from_file(update_results_json_file)
            if j == 0:
                start_time = interval_start_time
            else:
                start_time = end_time

            result_json[0]['interval_start_time'] = start_time
            end_time = increment_timestamp_by_given_mins(start_time, 15)
            result_json[0]['interval_end_time'] = end_time

            # Expecting that metrics exists for container as we read from template
            # Add if exists checks for each key if needed
            container_metrics: list = result_json[0]["kubernetes_objects"][0]["containers"][0]["metrics"]
            container_name_to_update = result_json[0]["kubernetes_objects"][0]["containers"][0]["container_name"]
            num_metrics = len(container_metrics)

            CPU_REQUEST_INDEX = get_index_of_metric(container_metrics, CPU_REQUEST)
            CPU_LIMIT_INDEX = get_index_of_metric(container_metrics, CPU_LIMIT)
            CPU_THROTTLE_INDEX = get_index_of_metric(container_metrics, CPU_THROTTLE)
            CPU_USAGE_INDEX = get_index_of_metric(container_metrics, CPU_USAGE)

            MEMORY_REQUEST_INDEX = get_index_of_metric(container_metrics, MEMORY_REQUEST)
            MEMORY_LIMIT_INDEX = get_index_of_metric(container_metrics, MEMORY_LIMIT)
            MEMORY_USAGE_INDEX = get_index_of_metric(container_metrics, MEMORY_USAGE)
            MEMORY_RSS_INDEX = get_index_of_metric(container_metrics, MEMORY_RSS)

            if j == 96:
                # 97 th update result - miss all the non-mandatory fields
                # [ CpuRequest, CpuLimit, MemoryRequest, MemoryLimit, CpuThrottle ]
                if CPU_REQUEST_INDEX is not None:
                    container_metrics.pop(CPU_REQUEST_INDEX)
                if CPU_LIMIT_INDEX is not None:
                    CPU_LIMIT_INDEX = get_index_of_metric(container_metrics, CPU_LIMIT)
                    container_metrics.pop(CPU_LIMIT_INDEX)
                if CPU_THROTTLE_INDEX is not None:
                    CPU_THROTTLE_INDEX = get_index_of_metric(container_metrics, CPU_THROTTLE)
                    container_metrics.pop(CPU_THROTTLE_INDEX)
                if MEMORY_REQUEST_INDEX is not None:
                    MEMORY_REQUEST_INDEX = get_index_of_metric(container_metrics, MEMORY_REQUEST)
                    container_metrics.pop(MEMORY_REQUEST_INDEX)
                if MEMORY_LIMIT_INDEX is not None:
                    MEMORY_LIMIT_INDEX = get_index_of_metric(container_metrics, MEMORY_LIMIT)
                    container_metrics.pop(MEMORY_LIMIT_INDEX)
            elif j == 97:
                # 98 th update result   - misses Cpu Request
                if CPU_REQUEST_INDEX is not None:
                    container_metrics.pop(CPU_REQUEST_INDEX)
            elif j == 98:
                # 99 th update result   - misses Cpu Limit
                if CPU_LIMIT_INDEX is not None:
                    container_metrics.pop(CPU_LIMIT_INDEX)
            elif j == 99:
                # 100 th update result  - misses Memory Request
                if MEMORY_REQUEST_INDEX is not None:
                    container_metrics.pop(MEMORY_REQUEST_INDEX)
            elif j == 100:
                # 101 st update result  - misses Memory Limit
                if MEMORY_LIMIT_INDEX is not None:
                    container_metrics.pop(MEMORY_LIMIT_INDEX)
            elif j == 101:
                # 102 nd update result  - Invalid Amount in CPU Request
                if CPU_REQUEST_INDEX is not None:
                    cpu_request_entry = container_metrics[CPU_REQUEST_INDEX]
                    aggre_info = cpu_request_entry["results"]["aggregation_info"]
                    aggre_info["avg"] = -1
                    aggre_info["sum"] = -1
            elif j == 102:
                # 103 rd update result  - Invalid Amount in Memory Request
                if MEMORY_REQUEST_INDEX is not None:
                    memory_request_entry = container_metrics[MEMORY_REQUEST_INDEX]
                    aggre_info = memory_request_entry["results"]["aggregation_info"]
                    aggre_info["avg"] = -100
                    aggre_info["sum"] = -100
            elif j == 103:
                # 104 th update result  - Invalid Amount in CPU Limit
                if CPU_LIMIT_INDEX is not None:
                    cpu_limit_entry = container_metrics[CPU_LIMIT_INDEX]
                    aggre_info = cpu_limit_entry["results"]["aggregation_info"]
                    aggre_info["avg"] = -1
                    aggre_info["sum"] = -1
            elif j == 104:
                # 105 th update result  - Invalid Amount in Memory Limit
                if MEMORY_LIMIT_INDEX is not None:
                    memory_limit_entry = container_metrics[MEMORY_LIMIT_INDEX]
                    aggre_info = memory_limit_entry["results"]["aggregation_info"]
                    aggre_info["avg"] = -100
                    aggre_info["sum"] = -100
            elif j == 105:
                # 106 th update result  - Invalid Format in CPU Request
                if CPU_REQUEST_INDEX is not None:
                    cpu_request_entry = container_metrics[CPU_REQUEST_INDEX]
                    aggre_info = cpu_request_entry["results"]["aggregation_info"]
                    aggre_info["format"] = None
            elif j == 106:
                # 107 th update result  - Invalid Format in Memory Request
                if MEMORY_REQUEST_INDEX is not None:
                    memory_request_entry = container_metrics[MEMORY_REQUEST_INDEX]
                    aggre_info = memory_request_entry["results"]["aggregation_info"]
                    aggre_info["format"] = None
            elif j == 107:
                # 108 th update result  - Invalid Format in CPU Limit
                if CPU_LIMIT_INDEX is not None:
                    cpu_limit_entry = container_metrics[CPU_LIMIT_INDEX]
                    aggre_info = cpu_limit_entry["results"]["aggregation_info"]
                    aggre_info["format"] = None
            elif j == 108:
                # 109 th update result  - Invalid Format in Memory Limit
                if MEMORY_LIMIT_INDEX is not None:
                    memory_limit_entry = container_metrics[MEMORY_LIMIT_INDEX]
                    aggre_info = memory_limit_entry["results"]["aggregation_info"]
                    aggre_info["format"] = None
            elif j == 109:
                # 110 th update result - Skip CPU Request entry to check if variation is available
                if CPU_REQUEST_INDEX is not None:
                    container_metrics.pop(CPU_REQUEST_INDEX)
            elif j == 110:
                # 111 th update result - Skip CPU Limit entry to check if variation is available
                if CPU_LIMIT_INDEX is not None:
                    container_metrics.pop(CPU_LIMIT_INDEX)
            elif j == 111:
                # 112 th update result - Skip Memory Request entry to check if variation is available
                if MEMORY_REQUEST_INDEX is not None:
                    container_metrics.pop(MEMORY_REQUEST_INDEX)
            elif j == 112:
                # 113 th update result - Skip Memory Limit entry to check if variation is available
                if MEMORY_LIMIT_INDEX is not None:
                    container_metrics.pop(MEMORY_LIMIT_INDEX)

            write_json_data_to_file(update_results_json_file, result_json)
            result_json_arr.append(result_json[0])
            response = update_results(update_results_json_file)

            data = response.json()
            print("message = ", data['message'])
            assert response.status_code == SUCCESS_STATUS_CODE
            assert data['status'] == SUCCESS_STATUS
            assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

            if j > 95:
                response = update_recommendations(experiment_name, None, end_time)
                data = response.json()
                assert response.status_code == SUCCESS_STATUS_CODE
                assert data[0]['experiment_name'] == experiment_name

                # Get the experiment name
                json_data = json.load(open(create_exp_json_file))
                experiment_name = json_data[0]['experiment_name']

                response = list_recommendations(experiment_name)
                assert response.status_code == SUCCESS_200_STATUS_CODE

                #############################################################################################
                # TODO: Optimise the flow by having everything in the same else if ladder blocks            #
                #############################################################################################
                # This mechanism can be optimised by placing the above content in a function and calling    #
                # recommendations in the same else if ladder above, but parking it for later as Currently   #
                # we are not intended to disturb the flow as of now.                                        #
                #############################################################################################
                recommendation_json = response.json()

                recommendation_section = None

                for containers in recommendation_json[0]["kubernetes_objects"][0]["containers"]:
                    actual_container_name = containers["container_name"]
                    print(
                        f"actual container name = {actual_container_name}  expected container name = {container_name_to_update}")
                    if containers["container_name"] == container_name_to_update:
                        recommendation_section = containers["recommendations"]
                        break

                assert recommendation_section is not None

                high_level_notifications = recommendation_section["notifications"]

                # Check for Recommendation level notifications
                assert INFO_RECOMMENDATIONS_AVAILABLE_CODE in high_level_notifications

                data_section = recommendation_section["data"]
                # Check if recommendation exists
                assert str(end_time) in data_section
                # Check for timestamp level notifications
                timestamp_level_notifications = data_section[str(end_time)]["notifications"]
                assert INFO_SHORT_TERM_RECOMMENDATIONS_AVAILABLE_CODE in timestamp_level_notifications

                # Check for current recommendation
                recommendation_current = None
                if "current" in data_section[str(end_time)]:
                    recommendation_current = data_section[str(end_time)]["current"]

                short_term_recommendation = data_section[str(end_time)]["recommendation_terms"]["short_term"]

                if INFO_COST_RECOMMENDATIONS_AVAILABLE_CODE in short_term_recommendation["notifications"]:
                    assert "variation" in short_term_recommendation["recommendation_engines"]["cost"]
                    assert "config" in short_term_recommendation["recommendation_engines"]["cost"]

                if INFO_PERFORMANCE_RECOMMENDATIONS_AVAILABLE_CODE in short_term_recommendation["notifications"]:
                    assert "variation" in short_term_recommendation["recommendation_engines"]["performance"]
                    assert "config" in short_term_recommendation["recommendation_engines"]["performance"]


                short_term_recommendation_config = short_term_recommendation["recommendation_engines"]["cost"]["config"]
                short_term_recommendation_variation = short_term_recommendation["recommendation_engines"]["cost"]["variation"]

                if j == 96:
                    response = update_recommendations(experiment_name, None, end_time)
                    data = response.json()
                    assert response.status_code == SUCCESS_STATUS_CODE
                    assert data[0]['experiment_name'] == experiment_name
                    assert \
                        data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications']['111000'][
                            'message'] == 'Recommendations Are Available'
                    # Expected notifications in short term recommendation
                    # WARNING_CPU_LIMIT_NOT_SET_CODE = "423001"
                    # CRITICAL_CPU_REQUEST_NOT_SET_CODE = "523001"
                    # CRITICAL_MEMORY_REQUEST_NOT_SET_CODE = "524001"
                    # CRITICAL_MEMORY_LIMIT_NOT_SET_CODE = "524002"
                    assert WARNING_CPU_LIMIT_NOT_SET_CODE in timestamp_level_notifications
                    assert CRITICAL_CPU_REQUEST_NOT_SET_CODE in timestamp_level_notifications
                    assert CRITICAL_MEMORY_REQUEST_NOT_SET_CODE in timestamp_level_notifications
                    assert CRITICAL_MEMORY_LIMIT_NOT_SET_CODE in timestamp_level_notifications
                elif j == 97:
                    # Expected notifications in short term recommendation
                    # CRITICAL_CPU_REQUEST_NOT_SET_CODE = "523001"
                    assert CRITICAL_CPU_REQUEST_NOT_SET_CODE in timestamp_level_notifications
                elif j == 98:
                    # Expected notifications in short term recommendation
                    # WARNING_CPU_LIMIT_NOT_SET_CODE = "423001"
                    assert WARNING_CPU_LIMIT_NOT_SET_CODE in timestamp_level_notifications
                elif j == 99:
                    # Expected notifications in short term recommendation
                    # CRITICAL_MEMORY_REQUEST_NOT_SET_CODE = "524001"
                    assert CRITICAL_MEMORY_REQUEST_NOT_SET_CODE in timestamp_level_notifications
                elif j == 100:
                    # Expected notifications in short term recommendation
                    # CRITICAL_MEMORY_LIMIT_NOT_SET_CODE = "524002"
                    assert CRITICAL_MEMORY_LIMIT_NOT_SET_CODE in timestamp_level_notifications
                elif j == 101:
                    # Expected notifications in short term recommendation
                    # INVALID_AMOUNT_IN_CPU_SECTION_CODE = "223002"
                    assert INVALID_AMOUNT_IN_CPU_SECTION_CODE in timestamp_level_notifications
                elif j == 102:
                    # Expected notifications in short term recommendation
                    # INVALID_AMOUNT_IN_MEMORY_SECTION_CODE = "224002"
                    assert INVALID_AMOUNT_IN_MEMORY_SECTION_CODE in timestamp_level_notifications
                elif j == 103:
                    # Expected notifications in short term recommendation
                    # INVALID_AMOUNT_IN_CPU_SECTION_CODE = "223002"
                    assert INVALID_AMOUNT_IN_CPU_SECTION_CODE in timestamp_level_notifications
                elif j == 104:
                    # Expected notifications in short term recommendation
                    # INVALID_AMOUNT_IN_MEMORY_SECTION_CODE = "224002"
                    assert INVALID_AMOUNT_IN_MEMORY_SECTION_CODE in timestamp_level_notifications
                elif j == 105:
                    # Expected notifications in short term recommendation
                    # INVALID_FORMAT_IN_CPU_SECTION_CODE = "223004"
                    assert FORMAT_MISSING_IN_CPU_SECTION_CODE in timestamp_level_notifications
                elif j == 106:
                    # Expected notifications in short term recommendation
                    # INVALID_FORMAT_IN_MEMORY_SECTION_CODE = "224004"
                    assert FORMAT_MISSING_IN_MEMORY_SECTION_CODE in timestamp_level_notifications
                elif j == 107:
                    # Expected notifications in short term recommendation
                    # INVALID_FORMAT_IN_CPU_SECTION_CODE = "223004"
                    assert FORMAT_MISSING_IN_CPU_SECTION_CODE in timestamp_level_notifications
                elif j == 108:
                    # Expected notifications in short term recommendation
                    # INVALID_FORMAT_IN_MEMORY_SECTION_CODE = "224004"
                    assert FORMAT_MISSING_IN_MEMORY_SECTION_CODE in timestamp_level_notifications
                elif j == 109:
                    # Expecting CPU request variation is available
                    validate_variation(current_config=recommendation_current,
                                       recommended_config=short_term_recommendation_config,
                                       variation_config=short_term_recommendation_variation)
                elif j == 110:
                    # Expecting CPU limit variation is available
                    validate_variation(current_config=recommendation_current,
                                       recommended_config=short_term_recommendation_config,
                                       variation_config=short_term_recommendation_variation)
                elif j == 111:
                    # Expecting Memory request variation is available
                    validate_variation(current_config=recommendation_current,
                                       recommended_config=short_term_recommendation_config,
                                       variation_config=short_term_recommendation_variation)
                elif j == 112:
                    # Expecting Memory limit variation is available
                    validate_variation(current_config=recommendation_current,
                                       recommended_config=short_term_recommendation_config,
                                       variation_config=short_term_recommendation_variation)

    # Delete the experiments
    for i in range(num_exps):
        json_file = "/tmp/create_exp_" + str(i) + ".json"

        response = delete_experiment(json_file)
        print("delete exp = ", response.status_code)
