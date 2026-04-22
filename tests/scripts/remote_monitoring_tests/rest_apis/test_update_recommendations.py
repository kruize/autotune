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
import re

sys.path.append("../../")
from helpers.fixtures import *
from helpers.kruize import *
from helpers.list_reco_json_validate import *
from helpers.utils import *
from helpers.list_reco_json_local_monitoring_schema import list_reco_namespace_json_local_monitoring_schema
from helpers.long_term_list_reco_json_schema import long_term_namespace_reco_json_schema
from helpers.medium_term_list_reco_json_schema import medium_term_namespace_reco_json_schema

namespace_reco_term_input = [
    ("short_term_test_latest_true", 1, list_reco_namespace_json_local_monitoring_schema, SHORT_TERM_DURATION_IN_HRS_MAX, True, True),
    ("short_term_test_latest_false", 1, list_reco_namespace_json_local_monitoring_schema, SHORT_TERM_DURATION_IN_HRS_MAX, False, True),
    ("medium_term_test_true", 7, medium_term_namespace_reco_json_schema, MEDIUM_TERM_DURATION_IN_HRS_MAX, True, False),
    ("medium_term_test_false", 7, medium_term_namespace_reco_json_schema, MEDIUM_TERM_DURATION_IN_HRS_MAX, False, False),
    ("long_term_test_true", 15, long_term_namespace_reco_json_schema, LONG_TERM_DURATION_IN_HRS_MAX, True, False),
    ("long_term_test_false", 15, long_term_namespace_reco_json_schema, LONG_TERM_DURATION_IN_HRS_MAX, False, False),
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
@pytest.mark.parametrize("test_name, num_days, reco_json_schema, expected_duration_in_hours, latest, logging",
                         namespace_reco_term_input)
def test_update_namespace_recommendations_for_diff_reco_terms_with_only_latest(test_name, num_days, reco_json_schema,
                                                                   expected_duration_in_hours, latest, logging,
                                                                   cluster_type):
    '''
        Test Description: This test validates update_recommendations for namespace experiments for all the terms for multiple experiments posted using different json files
         with both latest=true and latest=false
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
    response = list_recommendations(experiment_name, latest, rm=True)

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

        if latest == "true":
            update_results_json.append(list_of_result_json_arr[i][len(list_of_result_json_arr[i]) - 1])
        elif latest == "false":
            total_num_results = len(list_of_result_json_arr[i])
            # Recommendations will be generated when 24h/672h/1440h results are available
            num_results_without_recos = int(expected_duration_in_hours * 4 - 1)
            for j in range(num_results_without_recos, total_num_results):
                update_results_json.append(list_of_result_json_arr[i][j])

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


# Test for GPU recommendations
# This test checks if kruize provides GPU recommendations for valid GPU's
@pytest.mark.sanity
@pytest.mark.parametrize("gpu_name", SUPPORTED_GPUS)
def test_update_valid_accelerator_recommendations(cluster_type, gpu_name):
    '''
        Creates Experiment +
        update results for GPU workload with 2 records
        update recommendation using start and end time as a parameter
            Expected : recommendation should be available for the timestamp provided
    '''
    input_json_file = "../json_files/create_exp.json"
    result_json_file = "../json_files/update_results_accelerator.json"

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

            # Substitute the valid GPU names
            for kub_obj in result_json[0]["kubernetes_objects"]:
                for container in kub_obj["containers"]:
                    for metric in container["metrics"]:
                        if metric["name"].startswith("accelerator") and "metadata" in metric["results"]:
                            metric["results"]["metadata"]["accelerator_model_name"] = gpu_name
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

                    # Check for GPU recommendations
                    cost_limits = short_term_recommendation["recommendation_engines"]["cost"]["config"].get("limits", {})
                    mig_keys_in_limits = [key for key in cost_limits if re.fullmatch(MIG_PATTERN, key)]
                    assert mig_keys_in_limits, COST_LIMITS_NO_MIG_RECOMMENDATIONS_AVAILABLE_MSG

                    # Check for notification
                    cost_notifications = short_term_recommendation["recommendation_engines"]["cost"]["notifications"]
                    assert INFO_ACCELERATOR_RECOMMENDATIONS_AVAILABLE in cost_notifications
                    perf_notifications = short_term_recommendation["recommendation_engines"]["performance"]["notifications"]
                    assert INFO_ACCELERATOR_RECOMMENDATIONS_AVAILABLE in perf_notifications

        response = update_recommendations(experiment_name, None, end_time)
        data = response.json()
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data[0]['experiment_name'] == experiment_name
        assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications']['111000'][
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

        expected_duration_in_hours = SHORT_TERM_DURATION_IN_HRS_MIN
        validate_reco_json(create_exp_json[0], update_results_json, list_reco_json[0], expected_duration_in_hours)

    # Delete all the experiments
    for i in range(num_exps):
        json_file = "/tmp/create_exp_" + str(i) + ".json"
        response = delete_experiment(json_file)
        print("delete exp = ", response.status_code)
        assert response.status_code == SUCCESS_STATUS_CODE


# This test needs to be updated when kriuze adds the notification codes for accelerator
@pytest.mark.sanity
def test_update_invalid_accelerator_name_recommendations(cluster_type):
    '''
        Creates Experiment +
        update results for GPU workload with 2 records
        update recommendation using start and end time as a parameter
            Expected : recommendation should not be available for the timestamp provided as accelerator is non-MIG
    '''
    input_json_file = "../json_files/create_exp.json"
    result_json_file = "../json_files/update_results_accelerator.json"

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
            for kub_obj in result_json[0]["kubernetes_objects"]:
                for container in kub_obj["containers"]:
                    for metric in container["metrics"]:
                        if metric["name"].startswith("accelerator") and "metadata" in metric["results"]:
                            metric["results"]["metadata"]["accelerator_model_name"] = "Fake-Accelerator-XYZ"
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

                    # Check for GPU recommendations
                    cost_limits = short_term_recommendation["recommendation_engines"]["cost"]["config"].get("limits", {})
                    assert any("cpu" in key.lower() for key in cost_limits), COST_LIMITS_CPU_NO_RECOMMENDATIONS_MSG
                    assert any("memory" in key.lower() for key in cost_limits), COST_LIMITS_MEM_NO_RECOMMENDATIONS_MSG

                    mig_keys_in_limits = [key for key in cost_limits if re.fullmatch(MIG_PATTERN, key)]
                    assert not mig_keys_in_limits, f"Unexpected GPU recommendations found: {mig_keys_in_limits}"

                    # Check for notification
                    cost_notifications = short_term_recommendation["recommendation_engines"]["cost"]["notifications"]
                    assert NOTICE_ACCELERATOR_NOT_SUPPORTED_BY_KRUIZE in cost_notifications
                    perf_notifications = short_term_recommendation["recommendation_engines"]["performance"]["notifications"]
                    assert NOTICE_ACCELERATOR_NOT_SUPPORTED_BY_KRUIZE in perf_notifications

        response = update_recommendations(experiment_name, None, end_time)
        data = response.json()
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data[0]['experiment_name'] == experiment_name
        assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications']['111000'][
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

        expected_duration_in_hours = SHORT_TERM_DURATION_IN_HRS_MIN
        validate_reco_json(create_exp_json[0], update_results_json, list_reco_json[0], expected_duration_in_hours)

    # Delete all the experiments
    for i in range(num_exps):
        json_file = "/tmp/create_exp_" + str(i) + ".json"
        response = delete_experiment(json_file)
        print("delete exp = ", response.status_code)
        assert response.status_code == SUCCESS_STATUS_CODE



@pytest.mark.perf_profile
def test_update_recommendations_with_perf_profile_update(cluster_type):
    """
    This test simulates the scenario using the below steps:
    1. Create performance profile with v1 version
    2. Call updatePerformanceProfile to update it to v2
    3. Call createExperiment
    4. Call updateResults and validate that the performance profile validation is not failing
    5. Call updateRecommendations and validate successful recommendation response
    
    This ensures that performance profile updates are properly handled and that validation uses the updated profile version.
    """
    input_json_file = "../json_files/create_exp.json"
    result_json_file = "../json_files/update_results.json"
    perf_profile_v1_json_file = "../json_files/resource_optimization_openshift_v1.json"
    perf_profile_dir = get_metric_profile_dir()
    perf_profile_v2_json_file = perf_profile_dir / 'resource_optimization_openshift.json'
    
    form_kruize_url(cluster_type)

    # Step 0: Clean up any existing experiment and profile
    print("\n[Step 0] Cleaning up existing resources...")
    response = delete_experiment(input_json_file)
    print(f"Delete experiment response: {response.status_code}")
    
    try:
        response = delete_performance_profile(perf_profile_v1_json_file)
        print(f"Delete performance profile response: {response.status_code}")
    except Exception as e:
        print(f"Performance profile doesn't exist or couldn't be deleted: {e}")
    
    # Step 1: Create the v1 performance profile
    print("\n[Step 1] Creating performance profile v1...")
    response = create_performance_profile(perf_profile_v1_json_file)
    print(f"Create performance profile v1 response: {response.status_code}")
    data = response.json()
    print(f"Response: {data}")
    assert response.status_code == SUCCESS_STATUS_CODE or response.status_code == SUCCESS_200_STATUS_CODE
    assert CREATE_PERF_PROFILE_SUCCESS_MSG % "resource-optimization-openshift" in data.get('message', '')
    print("✓ Performance profile v1 created successfully")
    
    # Step 2: Update the performance profile to v2
    print("\n[Step 2] Updating performance profile to v2...")
    response = update_performance_profile(perf_profile_v2_json_file)
    print(f"Update performance profile v2 response: {response.status_code}")
    data = response.json()
    print(f"Response: {data}")
    assert response.status_code == SUCCESS_STATUS_CODE or response.status_code == SUCCESS_200_STATUS_CODE
    # Check for the update success message with version 2.0
    assert "updated successfully to version 2.0" in data.get('message', '') or \
           UPDATE_PERF_PROFILE_SUCCESS_MSG % ("resource-optimization-openshift", 2.0) in data.get('message', '')
    print("✓ Performance profile updated to v2 successfully")
    
    # Step 3: Create experiment using the updated performance profile
    print("\n[Step 3] Creating experiment with updated performance profile...")
    response = create_experiment(input_json_file)
    data = response.json()
    print(f"Create experiment response: {response.status_code}")
    print(f"Response: {data}")
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG
    
    # Get experiment name for later use
    json_data = json.load(open(input_json_file))
    experiment_name = json_data[0]['experiment_name']
    print(f"✓ Experiment '{experiment_name}' created successfully")
    
    # Step 4: Update results and validate performance profile validation is not failing
    print("\n[Step 4] Updating results with performance profile v2 validation...")
    
    # Generate multiple result updates to simulate real scenario
    num_results = 3
    result_json_arr = []
    interval_start_time = get_datetime()
    
    for i in range(num_results):
        print(f"\n  [4.{i+1}] Updating result #{i+1}...")
        update_results_json_file = f"/tmp/update_results_{i}.json"
        
        # Read and prepare result JSON
        result_json = read_json_data_from_file(result_json_file)
        
        if i == 0:
            start_time = interval_start_time
        else:
            start_time = end_time
        
        result_json[0]['interval_start_time'] = start_time
        end_time = increment_timestamp_by_given_mins(start_time, 15)
        result_json[0]['interval_end_time'] = end_time
        
        write_json_data_to_file(update_results_json_file, result_json)
        result_json_arr.append(result_json[0])
        
        # Update results - this should validate against the UPDATED v2 profile
        response = update_results(update_results_json_file, False)
        data = response.json()
        print(f"  Update results response: {response.status_code}")
        
        # Validate that the update succeeded (performance profile validation passed)
        assert response.status_code == SUCCESS_STATUS_CODE, \
            f"Update results failed with status {response.status_code}: {data}"
        assert data['status'] == SUCCESS_STATUS, \
            f"Update results status is not SUCCESS: {data}"
        assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG, \
            f"Update results message unexpected: {data.get('message')}"
        print(f"  ✓ Result #{i+1} updated successfully - performance profile v2 validation passed")
    
    print("\n✓ All results updated successfully with performance profile v2 validation")
    
    # Step 5: Update recommendations and validate successful response
    print("\n[Step 5] Updating recommendations...")
    response = update_recommendations(experiment_name, None, end_time)
    print(f"Update recommendations response: {response.status_code}")
    data = response.json()
    print(f"Response summary: experiment_name={data[0].get('experiment_name')}")
    
    assert response.status_code == SUCCESS_STATUS_CODE, \
        f"Update recommendations failed with status {response.status_code}: {data}"
    assert data[0]['experiment_name'] == experiment_name, \
        f"Experiment name mismatch: expected {experiment_name}, got {data[0].get('experiment_name')}"
    
    # Validate recommendations are available
    recommendations = data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']
    notifications = recommendations.get('notifications', {})
    assert '111000' in notifications, \
        f"Recommendations notification code 111000 not found in: {notifications}"
    assert notifications['111000']['message'] == RECOMMENDATIONS_AVAILABLE, \
        f"Recommendations message unexpected: {notifications['111000'].get('message')}"
    
    print("✓ Recommendations updated successfully")
    
    # Verify recommendations with list API
    print("\n[Step 5.1] Verifying recommendations via listRecommendations...")
    response = list_recommendations(experiment_name, rm=True)
    assert response.status_code == SUCCESS_200_STATUS_CODE, \
        f"List recommendations failed with status {response.status_code}"
    
    list_reco_json = response.json()
    recommendation_section = list_reco_json[0]["kubernetes_objects"][0]["containers"][0]["recommendations"]
    high_level_notifications = recommendation_section["notifications"]
    
    # Verify recommendations are available
    assert INFO_RECOMMENDATIONS_AVAILABLE_CODE in high_level_notifications, \
        f"Recommendations available code not found in: {high_level_notifications}"
    
    # Verify no errors in short term recommendations
    data_section = recommendation_section["data"]
    short_term_recommendation = data_section[str(end_time)]["recommendation_terms"]["short_term"]
    short_term_notifications = short_term_recommendation["notifications"]
    
    for notification_code, notification in short_term_notifications.items():
        assert notification["type"] != "error", \
            f"Error notification found: {notification_code} - {notification}"
    
    print("✓ Recommendations verified successfully via listRecommendations")
    
    # Clean up
    print("\n[Cleanup] Removing test resources...")
    response = delete_experiment(input_json_file)
    print(f"Delete experiment response: {response.status_code}")
    assert response.status_code == SUCCESS_STATUS_CODE
