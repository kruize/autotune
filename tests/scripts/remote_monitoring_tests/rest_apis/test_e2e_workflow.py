import copy
import json

import pytest
from helpers.fixtures import *
from helpers.generate_rm_jsons import *
from helpers.kruize import *
from helpers.list_reco_json_schema import *
from helpers.list_reco_json_validate import *
from helpers.utils import *


@pytest.mark.test_e2e
def test_list_recommendations_multiple_exps_from_diff_json_files(cluster_type):
    """
    Test Description: This test validates list recommendations for multiple experiments posted using different json files
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
    num_exps = 10
    num_res = 100
    for i in range(num_exps):
        create_exp_json_file = "/tmp/create_exp_" + str(i) + ".json"
        generate_json(find, input_json_file, create_exp_json_file, i)

        # Delete the experiment
        response = delete_experiment(create_exp_json_file)
        print("delete exp = ", response.status_code)

        # Create the experiment
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

        # Define the batch size
        batch_size = 96
        # Loop to fetch elements in batches
        current_index = 0
        while current_index < len(result_json_arr):
            print(f"{current_index} -- {len(result_json_arr)}")
            # Get the current batch
            batch = result_json_arr[current_index:current_index + batch_size]
            batch_deep_copy = copy.deepcopy(batch)
            file_path = '/tmp/result_%s_to_%s.json' % (current_index, batch_size)
            with open(file_path, 'w') as json_file:
                json.dump(batch, json_file)
            response = update_results(file_path)
            data = response.json()
            assert response.status_code == SUCCESS_STATUS_CODE
            assert data['status'] == SUCCESS_STATUS
            assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG
            # Update the current index for the next batch
            current_index += batch_size
            # Convert UTC strings to datetime objects
            for item in batch_deep_copy:
                item['interval_start_time'] = datetime.strptime(item['interval_start_time'], "%Y-%m-%dT%H:%M:%S.%fZ")
                item['interval_end_time'] = datetime.strptime(item['interval_end_time'], "%Y-%m-%dT%H:%M:%S.%fZ")
            end_time = max(batch_deep_copy, key=lambda x: x['interval_end_time'])['interval_end_time']
            start_time = min(batch_deep_copy, key=lambda x: x['interval_start_time'])['interval_start_time']
            response = update_recommendations(experiment_name, None,
                                              end_time.strftime("%Y-%m-%dT%H:%M:%S.%fZ")[:-4] + "Z")
            data = response.json()
            assert response.status_code == SUCCESS_STATUS_CODE
            assert data[0]['experiment_name'] == experiment_name
            assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications']['112101'][
                       'message'] == 'Cost Recommendations Available'
            response = list_recommendations(experiment_name)
            if response.status_code == SUCCESS_200_STATUS_CODE:
                recommendation_json = response.json()
                recommendation_section = recommendation_json[0]["kubernetes_objects"][0]["containers"][0][
                    "recommendations"]
                high_level_notifications = recommendation_section["notifications"]
                # Check if duration
                assert INFO_COST_RECOMMENDATIONS_AVAILABLE_CODE in high_level_notifications
                data_section = recommendation_section["data"]
                short_term_recommendation = \
                    data_section[end_time.strftime("%Y-%m-%dT%H:%M:%S.%fZ")[:-4] + "Z"]["cost"]["short_term"]
                short_term_notifications = short_term_recommendation["notifications"]
                for notification in short_term_notifications.values():
                    assert notification["type"] != "error"

        # Invoke list recommendations for the specified experiment
        response = list_recommendations(experiment_name)
        assert response.status_code == SUCCESS_200_STATUS_CODE
        list_reco_json = response.json()

        # Validate the json against the json schema
        errorMsg = validate_list_reco_json(list_reco_json, list_reco_json_schema)
        assert errorMsg == ""

        # Validate the json values
        create_exp_json = read_json_data_from_file(create_exp_json_file)
        update_results_json = []
        update_results_json.append(result_json_arr[len(result_json_arr) - 1])

        expected_duration_in_hours = SHORT_TERM_DURATION_IN_HRS_MAX
        validate_reco_json(create_exp_json[0], update_results_json, list_reco_json[0], expected_duration_in_hours)

    # Invoke list recommendations for a non-existing experiment
    experiment_name = "Non-existing-exp"
    response = list_recommendations(experiment_name)
    assert response.status_code == ERROR_STATUS_CODE

    data = response.json()

    INVALID_EXP_NAME_MSG = "Given experiment name - \" " + experiment_name + " \" is not valid"
    assert data['message'] == INVALID_EXP_NAME_MSG, f"expected - {INVALID_EXP_NAME_MSG}, actual - {data['message']}"

    # Delete all the experiments
    for i in range(num_exps):
        json_file = "/tmp/create_exp_" + str(i) + ".json"
        response = delete_experiment(json_file)
        print("delete exp = ", response.status_code)
        assert response.status_code == SUCCESS_STATUS_CODE
