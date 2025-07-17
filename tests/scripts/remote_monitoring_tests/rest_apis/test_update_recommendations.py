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
from helpers.list_reco_json_validate import *
from helpers.utils import *
from helpers.list_reco_json_local_monitoring_schema import list_reco_namespace_json_local_monitoring_schema
from helpers.long_term_list_reco_json_schema import long_term_namespace_reco_json_schema
from helpers.medium_term_list_reco_json_schema import medium_term_namespace_reco_json_schema

reco_term_input = [
    ("short_term_test_latest_true", 1, list_reco_namespace_json_local_monitoring_schema, SHORT_TERM_DURATION_IN_HRS_MAX, True),
    ("short_term_test_latest_false", 1, list_reco_namespace_json_local_monitoring_schema, SHORT_TERM_DURATION_IN_HRS_MAX, True),
    ("medium_term_test_true", 7, medium_term_namespace_reco_json_schema, MEDIUM_TERM_DURATION_IN_HRS_MAX, False),
    ("medium_term_test_false", 7, medium_term_namespace_reco_json_schema, MEDIUM_TERM_DURATION_IN_HRS_MAX, False),
    ("long_term_test_true", 15, long_term_namespace_reco_json_schema, LONG_TERM_DURATION_IN_HRS_MAX, False),
    ("long_term_test_false", 15, long_term_namespace_reco_json_schema, LONG_TERM_DURATION_IN_HRS_MAX, False),
]


@pytest.mark.sanity
def test_update_valid_recommendations_after_results_after_create_exp(cluster_type):
    '''
    Creates Experiment +
    update results for 24 hrs +
    update recommendation using start and end time as a parameter
        Expected : recommendation should be available for the timestamp provided
        Expected : plots data should be available
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
    num_res = 2
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
            if j > 1:
                response = update_recommendations(experiment_name, None, end_time)
                data = response.json()
                assert response.status_code == SUCCESS_STATUS_CODE
                assert data[0]['experiment_name'] == experiment_name
                assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications']['111000'][
                           'message'] == 'Recommendations Are Available'
                response = list_recommendations(experiment_name, rm=True)
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
        response = list_recommendations(experiment_name, rm=True)
        assert response.status_code == SUCCESS_200_STATUS_CODE
        list_reco_json = response.json()

        # Validate the json against the json schema
        errorMsg = validate_list_reco_json(list_reco_json, list_reco_json_schema)
        assert errorMsg == ""

        # Validate the json values
        create_exp_json = read_json_data_from_file(create_exp_json_file)
        update_results_json = []
        update_results_json.append(result_json_arr[len(result_json_arr) - 1])

        expected_duration_in_hours = SHORT_TERM_DURATION_IN_HRS_MIN
        validate_reco_json(create_exp_json[0], update_results_json, list_reco_json[0], expected_duration_in_hours)

    # Delete all the experiments
    for i in range(num_exps):
        json_file = "/tmp/create_exp_" + str(i) + ".json"
        response = delete_experiment(json_file)
        print("delete exp = ", response.status_code)
        assert response.status_code == SUCCESS_STATUS_CODE


@pytest.mark.sanity
def test_plots_with_no_recommendations_in_some_terms(cluster_type):
    '''
    Creates Experiment +
    update results for 30 mins +
    update recommendation using start and end time as a parameter
        Expected : recommendation should be available for the timestamp provided
        Expected : plots data should not be available for medium and long term
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
    num_res = 2
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
            response = update_results(update_results_json_file)

            data = response.json()
            print("message = ", data['message'])
            assert response.status_code == SUCCESS_STATUS_CODE
            assert data['status'] == SUCCESS_STATUS
            assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

            # Expecting that we have recommendations after minimum of two datapoints
            if j > 1:
                response = update_recommendations(experiment_name, None, end_time)
                data = response.json()
                assert response.status_code == SUCCESS_STATUS_CODE
                assert data[0]['experiment_name'] == experiment_name
                assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications']['111000'][
                           'message'] == 'Recommendations Are Available'
                response = list_recommendations(experiment_name, rm=True)
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
        response = list_recommendations(experiment_name, rm=True)
        assert response.status_code == SUCCESS_200_STATUS_CODE
        list_reco_json = response.json()

        # Validate the json against the json schema
        errorMsg = validate_list_reco_json(list_reco_json, list_reco_json_schema)
        assert errorMsg == ""

        # Validate the json values
        create_exp_json = read_json_data_from_file(create_exp_json_file)
        update_results_json = []
        update_results_json.append(result_json_arr[len(result_json_arr) - 1])

        expected_duration_in_hours = SHORT_TERM_DURATION_IN_HRS_MIN
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
                assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications'][
                           INFO_RECOMMENDATIONS_AVAILABLE_CODE][
                           'message'] == RECOMMENDATIONS_AVAILABLE
                response = list_recommendations(experiment_name, rm=True)
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
        assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications'][
                   INFO_RECOMMENDATIONS_AVAILABLE_CODE][
                   'message'] == RECOMMENDATIONS_AVAILABLE

        # Invoke list recommendations for the specified experiment
        response = list_recommendations(experiment_name, rm=True)
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
        try to update recommendation without experiment name and end time and get 400 status with
        UPDATE_RECOMMENDATIONS_MANDATORY_DEFAULT_MESSAGE and UPDATE_RECOMMENDATIONS_MANDATORY_INTERVAL_END_DATE
    '''
    form_kruize_url(cluster_type)
    response = update_recommendations(None, None, None)
    data = response.json()
    assert response.status_code == ERROR_STATUS_CODE
    assert data['message'] == UPDATE_RECOMMENDATIONS_MANDATORY_DEFAULT_MESSAGE + ", " + \
           UPDATE_RECOMMENDATIONS_MANDATORY_INTERVAL_END_DATE


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
    assert data['message'] == UPDATE_RECOMMENDATIONS_INVALID_DATE_TIME_FORMAT % end_time


@pytest.mark.negative
def test_update_recommendations_with_unknown_experiment_name(cluster_type):
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
def test_update_recommendations_with_unknown_interval_end_time(cluster_type):
    input_json_file = "../json_files/create_exp.json"
    # creating a random end_time timestamp
    end_time = "2023-01-02T00:15:00.000Z"
    # Convert end_time to datetime object
    end_time_dt = datetime.strptime(end_time, "%Y-%m-%dT%H:%M:%S.%fZ")

    # Subtract 15 days from end_time
    start_time_dt = end_time_dt - timedelta(days=15)

    # Format start_time_dt back to string
    start_time = start_time_dt.strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + 'Z'

    find = []
    json_data = json.load(open(input_json_file))

    find.append(json_data[0]['experiment_name'])
    find.append(json_data[0]['kubernetes_objects'][0]['name'])
    find.append(json_data[0]['kubernetes_objects'][0]['namespace'])

    form_kruize_url(cluster_type)

    # Create experiment using the specified json
    create_exp_json_file = "/tmp/create_exp_" + str(0) + ".json"
    generate_json(find, input_json_file, create_exp_json_file, 0)

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

    # Get the experiment name
    json_data = json.load(open(create_exp_json_file))
    experiment_name = json_data[0]['experiment_name']

    response = update_recommendations(experiment_name, None, end_time)
    data = response.json()
    assert response.status_code == ERROR_STATUS_CODE
    assert data['message'] == UPDATE_RECOMMENDATIONS_METRICS_NOT_FOUND + start_time + " to " + end_time


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


@pytest.mark.sanity
def test_update_valid_namespace_recommendations(cluster_type):
    '''
        Creates Namespace Experiment +
        update results for namespace for 30 mins +
        update recommendation using only end time as a parameter
            Expected : namespace recommendation should be available for the timestamp provided
    '''
    input_json_file = "../json_files/create_exp_namespace.json"
    result_json_file = "../json_files/update_results_namespace.json"

    find = []
    json_data = json.load(open(input_json_file))
    print("json_data: ", json_data)

    find.append(json_data[0]['experiment_name'])
    find.append(json_data[0]['kubernetes_objects'][0]['namespaces']['namespace'])

    form_kruize_url(cluster_type)

    # Create experiment using the specified json
    num_exps = 1
    num_res = 2
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
            if j > 1:
                response = update_recommendations(experiment_name, None, end_time)
                data = response.json()
                assert response.status_code == SUCCESS_STATUS_CODE
                assert data[0]['experiment_name'] == experiment_name
                assert data[0]['kubernetes_objects'][0]['namespaces']['recommendations']['notifications'][
                           INFO_RECOMMENDATIONS_AVAILABLE_CODE][
                           'message'] == RECOMMENDATIONS_AVAILABLE
                response = list_recommendations(experiment_name, rm=True)
                if response.status_code == SUCCESS_200_STATUS_CODE:
                    recommendation_json = response.json()
                    recommendation_section = recommendation_json[0]["kubernetes_objects"][0]["namespaces"][
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
        assert data[0]['kubernetes_objects'][0]['namespaces']['recommendations']['notifications'][
                   INFO_RECOMMENDATIONS_AVAILABLE_CODE][
                   'message'] == RECOMMENDATIONS_AVAILABLE

        # Invoke list recommendations for the specified experiment
        response = list_recommendations(experiment_name, rm=True)
        assert response.status_code == SUCCESS_200_STATUS_CODE
        list_reco_json = response.json()

        # Validate the json against the json schema
        error_msg = validate_list_reco_json(list_reco_json, list_reco_namespace_json_local_monitoring_schema)
        assert error_msg == ""

        # Validate the json values
        create_exp_json = read_json_data_from_file(create_exp_json_file)
        update_results_json = [result_json_arr[len(result_json_arr) - 1]]

        expected_duration_in_hours = SHORT_TERM_DURATION_IN_HRS_MIN
        validate_reco_json(create_exp_json[0], update_results_json, list_reco_json[0], expected_duration_in_hours)

    # Delete all the experiments
    for i in range(num_exps):
        json_file = "/tmp/create_exp_" + str(i) + ".json"
        response = delete_experiment(json_file)
        print("delete exp = ", response.status_code)
        assert response.status_code == SUCCESS_STATUS_CODE


@pytest.mark.extended
@pytest.mark.parametrize("test_name, num_days, reco_json_schema, expected_duration_in_hours, logging",
                         reco_term_input)
def test_update_namespace_recommendations_for_diff_reco_terms_with_only_latest(test_name, num_days, reco_json_schema,
                                                                   expected_duration_in_hours, logging,
                                                                   cluster_type):
    '''
        Test Description: This test validates update_recommendations for namespace experiments for all the terms for multiple experiments posted using different json files
    '''
    input_json_file = "../json_files/create_exp_namespace.json"
    result_json_file = "../json_files/update_results_namespace.json"

    find = []
    json_data = json.load(open(input_json_file))
    print("json_data: ", json_data)

    find.append(json_data[0]['experiment_name'])
    find.append(json_data[0]['kubernetes_objects'][0]['namespaces']['namespace'])

    form_kruize_url(cluster_type)

    # Create experiment using the specified json
    num_exps = 1
    num_res = 96 * num_days
    list_of_result_json_arr = []
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

            update_recommendations(experiment_name, None, end_time)

        list_of_result_json_arr.append(result_json_arr)

        response = update_recommendations(experiment_name, None, end_time)
        data = response.json()
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data[0]['experiment_name'] == experiment_name
        assert data[0]['kubernetes_objects'][0]['namespaces']['recommendations']['notifications'][
                   NOTIFICATION_CODE_FOR_RECOMMENDATIONS_AVAILABLE][
                   'message'] == RECOMMENDATIONS_AVAILABLE
    experiment_name = None
    response = list_recommendations(experiment_name, rm=True)

    list_reco_json = response.json()
    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json against the json schema
    errorMsg = validate_list_reco_json(list_reco_json, list_reco_namespace_json_local_monitoring_schema)
    assert errorMsg == ""
    for i in range(num_exps):
        create_exp_json_file = "/tmp/create_exp_" + str(i) + ".json"
        update_results_json_file = "/tmp/update_results_" + str(i) + ".json"

        # Validate the json values
        create_exp_json = read_json_data_from_file(create_exp_json_file)

        update_results_json = []

        if MEDIUM_TERM in test_name:
            expected_duration_in_hours = MEDIUM_TERM_DURATION_IN_HRS_MAX
        elif LONG_TERM in test_name:
            expected_duration_in_hours = LONG_TERM_DURATION_IN_HRS_MAX
        else:
            expected_duration_in_hours = SHORT_TERM_DURATION_IN_HRS_MAX

        # TODO: Need to check if this required
        update_results_json.append(list_of_result_json_arr[i][len(list_of_result_json_arr[i]) - 1])

        exp_found = False
        for list_reco in list_reco_json:
            if create_exp_json[0]['experiment_name'] == list_reco['experiment_name']:
                validate_reco_json(create_exp_json[0], update_results_json, list_reco, expected_duration_in_hours,
                                   test_name)
                exp_found = True
            continue

        assert exp_found == True, f"Experiment name {create_exp_json[0]['experiment_name']} not found in listRecommendations!"

    # Delete the experiments
    for i in range(num_exps):
        json_file = "/tmp/create_exp_" + str(i) + ".json"

        response = delete_experiment(json_file)
        print("delete exp = ", response.status_code)
