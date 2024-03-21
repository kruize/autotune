import pytest
from helpers.fixtures import *
from helpers.kruize import *
from helpers.list_reco_json_validate import *
from helpers.utils import *


@pytest.mark.sanity
def test_update_valid_recommendations_after_results_after_create_exp(cluster_type):
    '''
    Creates Experiment +
    update results for 24 hrs +
    update recommendation using start and end time as a parameter
        Expected : recommendation should be available for the timestamp provided
    '''
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
            response = update_results(update_results_json_file, False)

            data = response.json()
            print("message = ", data['message'])
            assert response.status_code == SUCCESS_STATUS_CODE
            assert data['status'] == SUCCESS_STATUS
            assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

            # Expecting that we have recommendations
            if j > 96:
                response = update_recommendations(experiment_name, None, end_time)
                data = response.json()
                assert response.status_code == SUCCESS_STATUS_CODE
                assert data[0]['experiment_name'] == experiment_name
                assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications']['111000'][
                           'message'] == 'Recommendations Are Available'
                response = list_recommendations(experiment_name)
                if response.status_code == SUCCESS_200_STATUS_CODE:
                    recommendation_json = response.json()
                    recommendation_section = recommendation_json[0]["kubernetes_objects"][0]["containers"][0][
                        "recommendations"]
                    high_level_notifications = recommendation_section["notifications"]
                    # Check if duration
                    assert INFO_RECOMMENDATIONS_AVAILABLE_CODE in high_level_notifications
                    data_section = recommendation_section["data"]
                    short_term_recommendation = data_section[str(end_time)]["recommendation_terms"]["short_term"]
                    short_term_notifications = short_term_recommendation["notifications"]
                    for notification in short_term_notifications.values():
                        assert notification["type"] != "error"

        response = update_recommendations(experiment_name, None, end_time)
        data = response.json()
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data[0]['experiment_name'] == experiment_name
        assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications']['111000'][
                   'message'] == 'Recommendations Are Available'

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

    # Delete all the experiments
    for i in range(num_exps):
        json_file = "/tmp/create_exp_" + str(i) + ".json"
        response = delete_experiment(json_file)
        print("delete exp = ", response.status_code)
        assert response.status_code == SUCCESS_STATUS_CODE


@pytest.mark.sanity
def test_update_valid_recommendations_just_endtime_input_after_results_after_create_exp(cluster_type):
    '''
        Creates Experiment +
        update results for 24 hrs +
        update recommendation using only end time as a parameter
            Expected : recommendation should be available for the timestamp provided
    '''
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
            response = update_results(update_results_json_file, False)

            data = response.json()
            print("message = ", data['message'])
            assert response.status_code == SUCCESS_STATUS_CODE
            assert data['status'] == SUCCESS_STATUS
            assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

            # Expecting that we have recommendations
            if j > 96:
                response = update_recommendations(experiment_name, None, end_time)
                data = response.json()
                assert response.status_code == SUCCESS_STATUS_CODE
                assert data[0]['experiment_name'] == experiment_name
                assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications'][INFO_RECOMMENDATIONS_AVAILABLE_CODE][
                           'message'] == RECOMMENDATIONS_AVAILABLE
                response = list_recommendations(experiment_name)
                if response.status_code == SUCCESS_200_STATUS_CODE:
                    recommendation_json = response.json()
                    recommendation_section = recommendation_json[0]["kubernetes_objects"][0]["containers"][0][
                        "recommendations"]
                    high_level_notifications = recommendation_section["notifications"]
                    # Check if duration
                    assert INFO_RECOMMENDATIONS_AVAILABLE_CODE in high_level_notifications
                    data_section = recommendation_section["data"]
                    short_term_recommendation = data_section[str(end_time)]["recommendation_terms"]["short_term"]
                    short_term_notifications = short_term_recommendation["notifications"]
                    for notification in short_term_notifications.values():
                        assert notification["type"] != "error"

        response = update_recommendations(experiment_name, None, end_time)
        data = response.json()
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data[0]['experiment_name'] == experiment_name
        assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications'][INFO_RECOMMENDATIONS_AVAILABLE_CODE][
                   'message'] == RECOMMENDATIONS_AVAILABLE

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

    # Delete all the experiments
    for i in range(num_exps):
        json_file = "/tmp/create_exp_" + str(i) + ".json"
        response = delete_experiment(json_file)
        print("delete exp = ", response.status_code)
        assert response.status_code == SUCCESS_STATUS_CODE


@pytest.mark.negative
def test_update_recommendations_without_experiment_name_end_time(cluster_type):
    '''
        try to update recommendation without experiment name and end time and get 400 status with UPDATE_RECOMMENDATIONS_MANDATORY_DEFAULT_MESSAGE
    '''
    form_kruize_url(cluster_type)
    response = update_recommendations(None, None, None)
    data = response.json()
    assert response.status_code == ERROR_STATUS_CODE
    assert data['message'] == UPDATE_RECOMMENDATIONS_MANDATORY_DEFAULT_MESSAGE


@pytest.mark.negative
def test_update_recommendations_without_end_time(cluster_type):
    '''
        try to update recommendation without end time and get 400 status with UPDATE_RECOMMENDATIONS_MANDATORY_INTERVAL_END_DATE
    '''
    form_kruize_url(cluster_type)
    experiment_name = "test123"
    response = update_recommendations(experiment_name, None, None)
    data = response.json()
    assert response.status_code == ERROR_STATUS_CODE
    assert data['message'] == UPDATE_RECOMMENDATIONS_MANDATORY_INTERVAL_END_DATE


@pytest.mark.negative
def test_update_recommendations_with_invalid_date_format_end_time(cluster_type):
    '''
        Update recommendation with invalid end date format.
    '''
    form_kruize_url(cluster_type)
    experiment_name = "test123"
    end_time = "2023-011-02T00:15:00.000Z"
    response = update_recommendations(experiment_name, None, end_time)
    data = response.json()
    assert response.status_code == ERROR_STATUS_CODE
    assert data['message'] == UPDATE_RECOMMENDATIONS_INVALID_DATE_TIME_FORMAT % (end_time)


@pytest.mark.negative
def test_update_recommendations_with_unknown_experiment_name_and_end_time(cluster_type):
    '''
        Update recommendation with unknown experiment name and end date.
    '''
    form_kruize_url(cluster_type)
    experiment_name = "test123"
    end_time = "2023-01-02T00:15:00.000Z"
    response = update_recommendations(experiment_name, None, end_time)
    data = response.json()
    assert response.status_code == ERROR_STATUS_CODE
    assert data['message'] == UPDATE_RECOMMENDATIONS_EXPERIMENT_NOT_FOUND + experiment_name


@pytest.mark.negative
def test_update_recommendations_with_end_time_precede_start_time(cluster_type):
    '''
        Update recommendation with start time precede end time.
    '''
    form_kruize_url(cluster_type)
    experiment_name = "test123"
    start_time = "2023-01-03T00:15:00.000Z"
    end_time = "2023-01-02T00:15:00.000Z"
    response = update_recommendations(experiment_name, None, end_time)
    data = response.json()
    assert response.status_code == ERROR_STATUS_CODE
    assert data['message'] == UPDATE_RECOMMENDATIONS_START_TIME_PRECEDE_END_TIME


@pytest.mark.skip(reason="Not enabled interval_start_time yet")
def test_update_recommendations_with_end_time_precede_start_time(cluster_type):
    '''
        Update recommendation with start time and end time having difference more than 15 days.
    '''
    form_kruize_url(cluster_type)
    experiment_name = "test123"
    start_time = "2023-01-03T00:15:00.000Z"
    end_time = "2023-01-30T00:15:00.000Z"
    response = update_recommendations(experiment_name, None, end_time)
    data = response.json()
    assert response.status_code == ERROR_STATUS_CODE
    assert data['message'] == UPDATE_RECOMMENDATIONS_START_TIME_END_TIME_GAP_ERROR
