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
import datetime
import json

import pytest
import sys

sys.path.append("../../")

from helpers.all_terms_list_reco_json_schema import all_terms_list_reco_json_schema
from helpers.fixtures import *
from helpers.generate_rm_jsons import *
from helpers.kruize import *
from helpers.list_reco_json_schema import *
from helpers.medium_and_long_term_list_reco_json_schema import medium_and_long_term_list_reco_json_schema
from helpers.medium_term_list_reco_json_schema import *
from helpers.long_term_list_reco_json_schema import *
from helpers.list_reco_json_validate import *
from helpers.short_and_long_term_list_reco_json_schema import short_and_long_term_list_reco_json_schema
from helpers.short_and_medium_term_list_reco_json_schema import short_and_medium_term_list_reco_json_schema
from helpers.short_term_list_reco_json_schema import short_term_list_reco_json_schema
from helpers.utils import *
from helpers.list_reco_json_local_monitoring_schema import list_reco_namespace_json_local_monitoring_schema

reco_term_input = [
    ("short_term_test_latest_true", 1, list_reco_json_schema, SHORT_TERM_DURATION_IN_HRS_MAX, True, True),
    ("short_term_test_latest_false", 1, list_reco_json_schema, SHORT_TERM_DURATION_IN_HRS_MAX, False, True),
    ("medium_term_test_true", 7, medium_term_list_reco_json_schema, MEDIUM_TERM_DURATION_IN_HRS_MAX, True, False),
    ("medium_term_test_false", 7, medium_term_list_reco_json_schema, MEDIUM_TERM_DURATION_IN_HRS_MAX, False, False),
    ("long_term_test_true", 15, long_term_list_reco_json_schema, LONG_TERM_DURATION_IN_HRS_MAX, True, False),
    ("long_term_test_false", 15, long_term_list_reco_json_schema, LONG_TERM_DURATION_IN_HRS_MAX, False, False),
]

term_input = [
    # test_name, num_res, reco_json_schema, expected_duration_in_hours, increment_end_time_by
    ("short_term_test_2_data_points", 2, list_reco_json_schema, 0.5, 0, True),
    ("medium_term_test_192_data_points", 192, medium_term_list_reco_json_schema, 48.0, 0, False),
    ("long_term_test_768_data_points", 768, long_term_list_reco_json_schema, 192.0, 0, False),
    ("long_term_test_769_data_points", 769, long_term_list_reco_json_schema, 192.25, 0, False),
    ("long_term_test_1440_data_points", 1440, long_term_list_reco_json_schema, 360.0, 0, False),
    ("short_term_test_2_data_points_non_contiguous", 2, list_reco_json_schema, 0.5, 0, True),
    ("medium_term_test_192_data_points_non_contiguous", 192, medium_term_list_reco_json_schema, 48.0, 0, False),
    ("long_term_test_768_data_points_non_continguous", 768, long_term_list_reco_json_schema, 192.0, 0, False),
    # Uncomment below in future when monitoring_end_time to updateRecommendations need not have result uploaded with the same end_time
    # ("short_term_test_2_data_points_end_time_after_1hr", 2, list_reco_json_schema, 0.5, 60),
    # ("medium_term_test_192_data_points_end_time_after_1hr", 192, medium_term_list_reco_json_schema, 48, 60),
    # ("long_term_test_768_data_points_end_time_after_1hr", 768, long_term_list_reco_json_schema, 192, 60),
    # ("long_term_test_769_data_points_end_time_after_1hr", 769, long_term_list_reco_json_schema, 192.25, 60),
]
term_input_for_missing_terms = [
    # test_name, num_res, reco_json_schema, expected_duration_in_hours, increment_end_time_by, logging,
    # long_term_present, medium_term_present, short_term_present
    # contiguous scenarios
    ("no_term_min_recomm", 1, None, 0, 0, True, False, False, False),
    ("all_terms_min_recomm", 768, all_terms_list_reco_json_schema, 192.0, 0, False, True, True, True),
    ("only_short_term_min_recomm", 2, short_term_list_reco_json_schema, 0.5, 0, True, False, False, True),
    ("only_medium_term_min_recomm", 192, medium_term_list_reco_json_schema, 48.0, 0, False, False, True, False),
    ("only_long_term_min_recomm", 768, long_term_list_reco_json_schema, 192.0, 0, False, True, False, False),
    ("short_term_and_medium_term_min_recomm", 192, short_and_medium_term_list_reco_json_schema, 24.0, 0, False, False,
     True, True),
    ("short_term_and_long_term_min_recomm", 768, short_and_long_term_list_reco_json_schema, 24.0, 0, False, True, False,
     True),
    ("medium_term_and_long_term_min_recomm", 768, medium_and_long_term_list_reco_json_schema, 168.0, 0, False, True,
     True, False)
]

term_input_for_missing_terms_non_contiguous = [
    # test_name, num_res, reco_json_schema, expected_duration_in_hours, increment_end_time_by
    # non-contiguous scenarios
    ("all_terms_min_recomm_non_contiguous", 768, all_terms_list_reco_json_schema, 192, 15, False, True, True, True),
    ("only_short_term_min_recomm_non_contiguous", 2, short_term_list_reco_json_schema, 0.5, 15, True, False, False,
     True),
    ("only_medium_term_min_recomm_non_contiguous", 192, medium_term_list_reco_json_schema, 48.0, 15, False, False, True,
     False),
    ("only_long_term_min_recomm_non_contiguous", 768, long_term_list_reco_json_schema, 192.0, 15, False, True, False,
     False),
    ("short_term_and_medium_term_min_recomm_non_contiguous", 192, short_and_medium_term_list_reco_json_schema, 24.0, 15,
     False, False, True, True),
    ("short_term_and_long_term_min_recomm_non_contiguous", 768, short_and_long_term_list_reco_json_schema, 24.0, 15,
     False, True, False, True),
    ("medium_term_and_long_term_min_recomm_non_contiguous", 768, medium_and_long_term_list_reco_json_schema, 168.0, 15,
     False, True, True, False)
]

invalid_term_input = [
    ("short_term_test_no_data_point", 0, list_reco_json_schema, 0, True),
    ("short_term_test_1_data_point", 1, list_reco_json_schema, 0.25, True),
    ("medium_term_test_191_data_points", 191, medium_term_list_reco_json_schema, 47.8, False),
    ("long_term_test_true", 767, long_term_list_reco_json_schema, 191.8, False),
]

term_input_exceeding_limit = [
    ("short_term_test_non_contiguous_2_data_points_exceeding_24_hours", 2, list_reco_json_schema, 0.5, 1440, True),
    ("medium_term_test_non_contiguous_192_data_points_exceeding_7_days", 192, medium_term_list_reco_json_schema, 48.0,
     420, False),
    (
        "long_term_test_non_contiguous_768_data_points_exceeding_15_days", 768, long_term_list_reco_json_schema, 192.0,
        360,
        False)
]

profile_notifications = [
    ("cpu_zero_test", 1, True, [
        {"cpuRequest": {'sum': 5, "avg": 5, "min": 5, "max": 5, "format": "cores"}},
        {"cpuLimit": {'sum': 5, "avg": 5, "min": 5, "max": 5, "format": "cores"}},
        {"cpuUsage": {'sum': 0, "avg": 0, "min": 0, "max": 0, "format": "cores"}},
        {"cpuThrottle": {'sum': 0, "avg": 0, "min": 0, "max": 0, "format": "cores"}}
    ],
     NOTIFICATION_CODE_FOR_CPU_RECORDS_ARE_ZERO, NOTIFICATION_CODE_FOR_CPU_RECORDS_ARE_ZERO_MESSAGE
     ),
    ("cpu_usage_less_than_millicore_test", 1, True, [
        {"cpuRequest": {'sum': 5, "avg": 5, "min": 5, "max": 5, "format": "cores"}},
        {"cpuLimit": {'sum': 5, "avg": 5, "min": 5, "max": 5, "format": "cores"}},
        {"cpuUsage": {'sum': 0.000001, "avg": 0.000001, "min": 0.000001, "max": 0.000001, "format": "cores"}},
        {"cpuThrottle": {'sum': 0.000001, "avg": 0.000001, "min": 0.000001, "max": 0.000001, "format": "cores"}}
    ],
     NOTIFICATION_CODE_FOR_CPU_RECORDS_ARE_IDLE, NOTIFICATION_CODE_FOR_CPU_RECORDS_ARE_IDLE_MESSAGE
     ),
    ("memory_zero_test", 1, True, [
        {"memoryRequest": {'sum': 100, "avg": 100, "min": 100, "max": 100, "format": "MiB"}},
        {"memoryLimit": {'sum': 100, "avg": 100, "min": 100, "max": 100, "format": "MiB"}},
        {"memoryUsage": {'sum': 0, "avg": 0, "min": 0, "max": 0, "format": "MiB"}},
        {"memoryRSS": {'sum': 0, "avg": 0, "min": 0, "max": 0, "format": "MiB"}}
    ],
     NOTIFICATION_CODE_FOR_MEMORY_RECORDS_ARE_ZERO, NOTIFICATION_CODE_FOR_MEMORY_RECORDS_ARE_ZERO_MESSAGE
     )
    ,
    ("cpu_memory_zero_test", 1, True, [
        {"memoryRequest": {'sum': 100, "avg": 100, "min": 100, "max": 100, "format": "MiB"}},
        {"memoryLimit": {'sum': 100, "avg": 100, "min": 100, "max": 100, "format": "MiB"}},
        {"memoryUsage": {'sum': 0, "avg": 0, "min": 0, "max": 0, "format": "MiB"}},
        {"memoryRSS": {'sum': 0, "avg": 0, "min": 0, "max": 0, "format": "MiB"}},
        {"cpuRequest": {'sum': 5, "avg": 5, "min": 5, "max": 5, "format": "cores"}},
        {"cpuLimit": {'sum': 5, "avg": 5, "min": 5, "max": 5, "format": "cores"}},
        {"cpuUsage": {'sum': 0, "avg": 0, "min": 0, "max": 0, "format": "cores"}},
        {"cpuThrottle": {'sum': 0, "avg": 0, "min": 0, "max": 0, "format": "cores"}}
    ],
     NOTIFICATION_CODE_FOR_CPU_RECORDS_ARE_ZERO, NOTIFICATION_CODE_FOR_CPU_RECORDS_ARE_ZERO_MESSAGE
     )
    ,
    ("memory_cpu_zero_test", 1, True, [
        {"memoryRequest": {'sum': 100, "avg": 100, "min": 100, "max": 100, "format": "MiB"}},
        {"memoryLimit": {'sum': 100, "avg": 100, "min": 100, "max": 100, "format": "MiB"}},
        {"memoryUsage": {'sum': 0, "avg": 0, "min": 0, "max": 0, "format": "MiB"}},
        {"memoryRSS": {'sum': 0, "avg": 0, "min": 0, "max": 0, "format": "MiB"}},
        {"cpuRequest": {'sum': 5, "avg": 5, "min": 5, "max": 5, "format": "cores"}},
        {"cpuLimit": {'sum': 5, "avg": 5, "min": 5, "max": 5, "format": "cores"}},
        {"cpuUsage": {'sum': 0, "avg": 0, "min": 0, "max": 0, "format": "cores"}},
        {"cpuThrottle": {'sum': 0, "avg": 0, "min": 0, "max": 0, "format": "cores"}}
    ],
     NOTIFICATION_CODE_FOR_MEMORY_RECORDS_ARE_ZERO, NOTIFICATION_CODE_FOR_MEMORY_RECORDS_ARE_ZERO_MESSAGE
     )
]


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

    response = list_recommendations(experiment_name, rm=True)

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
    response = update_results(result_json_file, False)

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
    response = list_recommendations(experiment_name, rm=True)

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

    response = list_recommendations(experiment_name, rm=True)

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

    response = list_recommendations(experiment_name, rm=True)

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
    response = update_results(result_json_file, False)

    # Get the experiment name
    json_data = json.load(open(input_json_file))
    experiment_name = json_data[0]['experiment_name']
    end_time = "2023-04-14T23:59:20.982Z"

    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

    response = update_recommendations(experiment_name, None, end_time)
    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data[0]['experiment_name'] == experiment_name
    assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications'][
               NOTIFICATION_CODE_FOR_RECOMMENDATIONS_AVAILABLE]['message'] == RECOMMENDATIONS_AVAILABLE

    response = list_recommendations(experiment_name, rm=True)

    list_reco_json = response.json()
    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json against the json schema
    errorMsg = validate_list_reco_json(list_reco_json, list_reco_json_schema)
    assert errorMsg == ""

    # Validate the json values
    create_exp_json = read_json_data_from_file(input_json_file)

    expected_duration_in_hours = SHORT_TERM_DURATION_IN_HRS_MAX

    update_results_json = []
    result_json_arr = read_json_data_from_file(result_json_file)
    update_results_json.append(result_json_arr[len(result_json_arr) - 1])
    validate_reco_json(create_exp_json[0], update_results_json, list_reco_json[0], expected_duration_in_hours)

    # Delete the experiment
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)


@pytest.mark.sanity
@pytest.mark.parametrize("memory_format_type",
                         ["bytes", "Bytes", "KiB", "MiB", "GiB", "TiB", "PiB", "EiB", "Ki", "Mi", "Gi", "Ti", "Pi",
                          "Ei", "kB", "KB", "MB", "GB", "TB", "PB", "EB", "K", "k", "M", "G", "T", "P", "E"])
@pytest.mark.parametrize("cpu_format_type", ["cores", "m"])
def test_list_recommendations_supported_metric_formats(memory_format_type, cpu_format_type, cluster_type):
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

    # Update the memory format and cpu format
    result_json = read_json_data_from_file(result_json_file)

    for result in result_json:
        metrics = result['kubernetes_objects'][0]['containers'][0]['metrics']

        for metric in metrics:
            if "cpu" in metric['name']:
                metric['results']['aggregation_info']['format'] = cpu_format_type

            if "memory" in metric['name']:
                metric['results']['aggregation_info']['format'] = memory_format_type

    tmp_update_results_json_file = "/tmp/update_results_metric" + "_" + memory_format_type + "_" + cpu_format_type + ".json"
    write_json_data_to_file(tmp_update_results_json_file, result_json)

    # Update the results
    response = update_results(tmp_update_results_json_file, False)

    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

    # Get the experiment name
    json_data = json.load(open(input_json_file))
    experiment_name = json_data[0]['experiment_name']
    end_time = "2023-04-14T23:59:20.982Z"

    response = update_recommendations(experiment_name, None, end_time)
    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data[0]['experiment_name'] == experiment_name
    assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications'][
               NOTIFICATION_CODE_FOR_RECOMMENDATIONS_AVAILABLE]['message'] == RECOMMENDATIONS_AVAILABLE

    response = list_recommendations(experiment_name, rm=True)

    list_reco_json = response.json()
    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json against the json schema
    errorMsg = validate_list_reco_json(list_reco_json, list_reco_json_schema)
    assert errorMsg == ""

    # Validate the json values
    create_exp_json = read_json_data_from_file(input_json_file)

    expected_duration_in_hours = SHORT_TERM_DURATION_IN_HRS_MAX

    update_results_json = []
    result_json_arr = read_json_data_from_file(tmp_update_results_json_file)
    update_results_json.append(result_json_arr[len(result_json_arr) - 1])

    validate_reco_json(create_exp_json[0], update_results_json, list_reco_json[0], expected_duration_in_hours)


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
        assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications'][
                   NOTIFICATION_CODE_FOR_RECOMMENDATIONS_AVAILABLE][
                   'message'] == RECOMMENDATIONS_AVAILABLE

        # Invoke list recommendations for the specified experiment
        response = list_recommendations(experiment_name, rm=True)
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
    response = update_results(result_json_file, False)
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
    all_dates = [
        item['interval_start_time'] for item in sorted_data
    ]
    # Get the experiment name
    json_data = json.load(open(input_json_file))
    experiment_name = json_data[0]['experiment_name']

    for dateStr in all_dates:
        update_recommendations(experiment_name, None,
                               dateStr.strftime("%Y-%m-%dT%H:%M:%S.%fZ")[:-4] + "Z")

    response = update_recommendations(experiment_name, None,
                                      end_time.strftime("%Y-%m-%dT%H:%M:%S.%fZ")[:-4] + "Z")
    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data[0]['experiment_name'] == experiment_name
    assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications'][
               NOTIFICATION_CODE_FOR_RECOMMENDATIONS_AVAILABLE][
               'message'] == RECOMMENDATIONS_AVAILABLE

    response = list_recommendations(experiment_name, latest, rm=True)

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
        expected_duration_in_hours = SHORT_TERM_DURATION_IN_HRS_MIN
        print(f"len update results json {len(update_results_json)}")
        # Recommendations are generated only when 24h results are present
        total_num_results = len(result_json_arr)
        num_results_without_recos = int(SHORT_TERM_DURATION_IN_HRS_MIN * 4 - 1)
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
    response = list_recommendations(experiment_name, latest, monitoring_end_time, rm=True)
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
    assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications'][
               NOTIFICATION_CODE_FOR_RECOMMENDATIONS_AVAILABLE][
               'message'] == RECOMMENDATIONS_AVAILABLE

    latest = None
    response = list_recommendations(experiment_name, latest, monitoring_end_time, rm=True)

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

        response = list_recommendations(experiment_name, rm=True)

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
@pytest.mark.parametrize("test_name, num_days, reco_json_schema, expected_duration_in_hours, latest, logging",
                         reco_term_input)
def test_list_recommendations_for_diff_reco_terms_with_only_latest(test_name, num_days, reco_json_schema,
                                                                   expected_duration_in_hours, latest, logging,
                                                                   cluster_type):
    """
        Test Description: This test validates list recommendations for all the terms for multiple experiments posted using different json files
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
    num_exps = 1
    num_res = 96 * num_days
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
            response = update_results(update_results_json_file, logging)

            data = response.json()
            print("message = ", data['message'])
            assert response.status_code == SUCCESS_STATUS_CODE
            assert data['status'] == SUCCESS_STATUS
            assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

            update_recommendations(experiment_name, None, end_time)

            # Get the experiment name
            json_data = json.load(open(create_exp_json_file))
            experiment_name = json_data[0]['experiment_name']

            response = list_recommendations(experiment_name, rm=True)
            assert response.status_code == SUCCESS_200_STATUS_CODE

        list_of_result_json_arr.append(result_json_arr)

        response = update_recommendations(experiment_name, None, end_time)
        data = response.json()
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data[0]['experiment_name'] == experiment_name
        assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications'][
                   NOTIFICATION_CODE_FOR_RECOMMENDATIONS_AVAILABLE][
                   'message'] == RECOMMENDATIONS_AVAILABLE
    experiment_name = None
    response = list_recommendations(experiment_name, latest, rm=True)

    list_reco_json = response.json()
    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json against the json schema
    errorMsg = validate_list_reco_json(list_reco_json, reco_json_schema)
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

            write_json_data_to_file(update_results_json_file, result_json)
            result_json_arr.append(result_json[0])
            response = update_results(update_results_json_file)

            data = response.json()
            print("message = ", data['message'])
            assert response.status_code == SUCCESS_STATUS_CODE
            assert data['status'] == SUCCESS_STATUS
            assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

            response = update_recommendations(experiment_name, None, end_time)
            data = response.json()
            assert response.status_code == SUCCESS_STATUS_CODE
            assert data[0]['experiment_name'] == experiment_name

            # Get the experiment name
            json_data = json.load(open(create_exp_json_file))
            experiment_name = json_data[0]['experiment_name']

            response = list_recommendations(experiment_name, rm=True)
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
            if j == 0:
                assert NOTIFICATION_CODE_FOR_NOT_ENOUGH_DATA in high_level_notifications
                assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications'][
                           NOTIFICATION_CODE_FOR_NOT_ENOUGH_DATA]['message'] == NOT_ENOUGH_DATA_MSG
            else:
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
                short_term_recommendation_variation = short_term_recommendation["recommendation_engines"]["cost"][
                    "variation"]

                response = update_recommendations(experiment_name, None, end_time)
                data = response.json()
                assert response.status_code == SUCCESS_STATUS_CODE
                assert data[0]['experiment_name'] == experiment_name
                assert \
                    data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications'][
                        NOTIFICATION_CODE_FOR_RECOMMENDATIONS_AVAILABLE]['message'] == RECOMMENDATIONS_AVAILABLE
                assert short_term_recommendation['notifications'][NOTIFICATION_CODE_FOR_COST_RECOMMENDATIONS_AVAILABLE][
                           'message'] == COST_RECOMMENDATIONS_AVAILABLE
                assert \
                    short_term_recommendation['notifications'][
                        NOTIFICATION_CODE_FOR_PERFORMANCE_RECOMMENDATIONS_AVAILABLE][
                        'message'] == PERFORMANCE_RECOMMENDATIONS_AVAILABLE
                validate_variation(recommendation_current, short_term_recommendation_config,
                                   short_term_recommendation_variation)

    # Delete the experiments
    for i in range(num_exps):
        json_file = "/tmp/create_exp_" + str(i) + ".json"

        response = delete_experiment(json_file)
        print("delete exp = ", response.status_code)


def validate_error_msgs(j: int, status_message, cname, experiment_name):
    if j == 96:
        assert status_message == UPDATE_RESULTS_INVALID_METRIC_VALUE_ERROR_MSG + CPU_REQUEST + CONTAINER_AND_EXPERIMENT_NAME % (
            cname, experiment_name)
    elif j == 97:
        assert status_message == UPDATE_RESULTS_INVALID_METRIC_VALUE_ERROR_MSG + MEMORY_REQUEST + CONTAINER_AND_EXPERIMENT_NAME % (
            cname, experiment_name)
    elif j == 98:
        assert status_message == UPDATE_RESULTS_INVALID_METRIC_VALUE_ERROR_MSG + CPU_LIMIT + CONTAINER_AND_EXPERIMENT_NAME % (
            cname, experiment_name)
    elif j == 99:
        assert status_message == UPDATE_RESULTS_INVALID_METRIC_VALUE_ERROR_MSG + MEMORY_LIMIT + CONTAINER_AND_EXPERIMENT_NAME % (
            cname, experiment_name)
    elif j > 100:
        assert status_message == UPDATE_RESULTS_INVALID_METRIC_FORMAT_ERROR_MSG + CONTAINER_AND_EXPERIMENT_NAME % (
            cname, experiment_name)


@pytest.mark.negative
def test_invalid_list_recommendations_notification_codes(cluster_type: str):
    """
        Test Description: This test validates list recommendations for multiple experiments posted using different json files
                          and pass different update results with incorrect values to test the notifications provided
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

            CPU_REQUEST_INDEX = get_index_of_metric(container_metrics, CPU_REQUEST)
            CPU_LIMIT_INDEX = get_index_of_metric(container_metrics, CPU_LIMIT)
            CPU_THROTTLE_INDEX = get_index_of_metric(container_metrics, CPU_THROTTLE)

            MEMORY_REQUEST_INDEX = get_index_of_metric(container_metrics, MEMORY_REQUEST)
            MEMORY_LIMIT_INDEX = get_index_of_metric(container_metrics, MEMORY_LIMIT)

            if j == 96:
                # 96th update result  - Invalid Amount in CPU Request
                if CPU_REQUEST_INDEX is not None:
                    cpu_request_entry = container_metrics[CPU_REQUEST_INDEX]
                    aggre_info = cpu_request_entry["results"]["aggregation_info"]
                    aggre_info["avg"] = -1
                    aggre_info["sum"] = -1
            elif j == 97:
                # 97th update result  - Invalid Amount in Memory Request
                if MEMORY_REQUEST_INDEX is not None:
                    memory_request_entry = container_metrics[MEMORY_REQUEST_INDEX]
                    aggre_info = memory_request_entry["results"]["aggregation_info"]
                    aggre_info["avg"] = -100
                    aggre_info["sum"] = -100
            elif j == 98:
                # 98th update result  - Invalid Amount in CPU Limit
                if CPU_LIMIT_INDEX is not None:
                    cpu_limit_entry = container_metrics[CPU_LIMIT_INDEX]
                    aggre_info = cpu_limit_entry["results"]["aggregation_info"]
                    aggre_info["avg"] = -1
                    aggre_info["sum"] = -1
            elif j == 99:
                # 99th update result  - Invalid Amount in Memory Limit
                if MEMORY_LIMIT_INDEX is not None:
                    memory_limit_entry = container_metrics[MEMORY_LIMIT_INDEX]
                    aggre_info = memory_limit_entry["results"]["aggregation_info"]
                    aggre_info["avg"] = -100
                    aggre_info["sum"] = -100
            elif j == 100:
                # 100th update result  - Invalid Format in CPU Request
                if CPU_REQUEST_INDEX is not None:
                    cpu_request_entry = container_metrics[CPU_REQUEST_INDEX]
                    aggre_info = cpu_request_entry["results"]["aggregation_info"]
                    aggre_info["format"] = None
            elif j == 101:
                # 101st update result  - Invalid Format in Memory Request
                if MEMORY_REQUEST_INDEX is not None:
                    memory_request_entry = container_metrics[MEMORY_REQUEST_INDEX]
                    aggre_info = memory_request_entry["results"]["aggregation_info"]
                    aggre_info["format"] = None
            elif j == 102:
                # 102nd update result  - Invalid Format in CPU Limit
                if CPU_LIMIT_INDEX is not None:
                    cpu_limit_entry = container_metrics[CPU_LIMIT_INDEX]
                    aggre_info = cpu_limit_entry["results"]["aggregation_info"]
                    aggre_info["format"] = None
            elif j == 103:
                # 103rd update result  - Invalid Format in Memory Limit
                if MEMORY_LIMIT_INDEX is not None:
                    memory_limit_entry = container_metrics[MEMORY_LIMIT_INDEX]
                    aggre_info = memory_limit_entry["results"]["aggregation_info"]
                    aggre_info["format"] = None
            elif j == 104:
                # 104th update result - miss all the non-mandatory fields
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
            elif j == 105:
                # 105th update result   - misses Cpu Request
                if CPU_REQUEST_INDEX is not None:
                    container_metrics.pop(CPU_REQUEST_INDEX)
            elif j == 106:
                # 106th update result   - misses Cpu Limit
                if CPU_LIMIT_INDEX is not None:
                    container_metrics.pop(CPU_LIMIT_INDEX)
            elif j == 107:
                # 107th update result  - misses Memory Request
                if MEMORY_REQUEST_INDEX is not None:
                    container_metrics.pop(MEMORY_REQUEST_INDEX)
            elif j == 108:
                # 108th update result  - misses Memory Limit
                if MEMORY_LIMIT_INDEX is not None:
                    container_metrics.pop(MEMORY_LIMIT_INDEX)
            elif j == 109:
                # 109th update result - Skip CPU Request entry to check if variation is available
                if CPU_REQUEST_INDEX is not None:
                    container_metrics.pop(CPU_REQUEST_INDEX)
            elif j == 110:
                # 110th update result - Skip CPU Limit entry to check if variation is available
                if CPU_LIMIT_INDEX is not None:
                    container_metrics.pop(CPU_LIMIT_INDEX)
            elif j == 111:
                # 111th update result - Skip Memory Request entry to check if variation is available
                if MEMORY_REQUEST_INDEX is not None:
                    container_metrics.pop(MEMORY_REQUEST_INDEX)
            elif j == 112:
                # 112th update result - Skip Memory Limit entry to check if variation is available
                if MEMORY_LIMIT_INDEX is not None:
                    container_metrics.pop(MEMORY_LIMIT_INDEX)

            write_json_data_to_file(update_results_json_file, result_json)
            result_json_arr.append(result_json[0])
            response = update_results(update_results_json_file)

            data = response.json()
            print("message = ", data['message'])
            if j in range(96, 104):
                assert response.status_code == ERROR_STATUS_CODE
                assert data['status'] == ERROR_STATUS
                validate_error_msgs(j, data['data'][0]['errors'][0]['message'], container_name_to_update,
                                    experiment_name)
            else:
                assert response.status_code == SUCCESS_STATUS_CODE
                assert data['status'] == SUCCESS_STATUS
                assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

            if j > 103:
                response = update_recommendations(experiment_name, None, end_time)
                data = response.json()
                assert response.status_code == SUCCESS_STATUS_CODE
                assert data[0]['experiment_name'] == experiment_name

                # Get the experiment name
                json_data = json.load(open(create_exp_json_file))
                experiment_name = json_data[0]['experiment_name']

                response = list_recommendations(experiment_name, rm=True)
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
                short_term_recommendation_variation = short_term_recommendation["recommendation_engines"]["cost"][
                    "variation"]

                if j == 104:
                    response = update_recommendations(experiment_name, None, end_time)
                    data = response.json()
                    assert response.status_code == SUCCESS_STATUS_CODE
                    assert data[0]['experiment_name'] == experiment_name
                    assert \
                        data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications'][
                            NOTIFICATION_CODE_FOR_RECOMMENDATIONS_AVAILABLE]['message'] == RECOMMENDATIONS_AVAILABLE
                    # Expected notifications in short term recommendation
                    # WARNING_CPU_LIMIT_NOT_SET_CODE = "423001"
                    # CRITICAL_CPU_REQUEST_NOT_SET_CODE = "523001"
                    # CRITICAL_MEMORY_REQUEST_NOT_SET_CODE = "524001"
                    # CRITICAL_MEMORY_LIMIT_NOT_SET_CODE = "524002"
                    assert WARNING_CPU_LIMIT_NOT_SET_CODE in timestamp_level_notifications
                    assert CRITICAL_CPU_REQUEST_NOT_SET_CODE in timestamp_level_notifications
                    assert CRITICAL_MEMORY_REQUEST_NOT_SET_CODE in timestamp_level_notifications
                    assert CRITICAL_MEMORY_LIMIT_NOT_SET_CODE in timestamp_level_notifications
                elif j == 105:
                    # Expected notifications in short term recommendation
                    # CRITICAL_CPU_REQUEST_NOT_SET_CODE = "523001"
                    assert CRITICAL_CPU_REQUEST_NOT_SET_CODE in timestamp_level_notifications
                elif j == 106:
                    # Expected notifications in short term recommendation
                    # WARNING_CPU_LIMIT_NOT_SET_CODE = "423001"
                    assert WARNING_CPU_LIMIT_NOT_SET_CODE in timestamp_level_notifications
                elif j == 107:
                    # Expected notifications in short term recommendation
                    # CRITICAL_MEMORY_REQUEST_NOT_SET_CODE = "524001"
                    assert CRITICAL_MEMORY_REQUEST_NOT_SET_CODE in timestamp_level_notifications
                elif j == 108:
                    # Expected notifications in short term recommendation
                    # CRITICAL_MEMORY_LIMIT_NOT_SET_CODE = "524002"
                    assert CRITICAL_MEMORY_LIMIT_NOT_SET_CODE in timestamp_level_notifications
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


def validate_term_recommendations(data, end_time, term):
    assert \
        data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['data'][end_time]['recommendation_terms'][
            term]['notifications'][NOTIFICATION_CODE_FOR_COST_RECOMMENDATIONS_AVAILABLE][
            'message'] == COST_RECOMMENDATIONS_AVAILABLE
    assert \
        data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['data'][end_time]['recommendation_terms'][
            term]['notifications'][NOTIFICATION_CODE_FOR_PERFORMANCE_RECOMMENDATIONS_AVAILABLE][
            'message'] == PERFORMANCE_RECOMMENDATIONS_AVAILABLE


@pytest.mark.sanity
@pytest.mark.parametrize(
    "test_name, num_res, reco_json_schema, expected_duration_in_hours, increment_end_time_by, logging",
    term_input)
def test_list_recommendations_term_min_data_threshold(test_name, num_res, reco_json_schema, expected_duration_in_hours,
                                                      increment_end_time_by, logging, cluster_type):
    """
        Test Description: This test validates if recommendations are generated when minimum data threshold of data is uploaded for all the terms
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
        complete_result_json_arr = []
        total_res = 0
        bulk_res = 0

        # Get the experiment name
        json_data = json.load(open(create_exp_json_file))
        experiment_name = json_data[0]['experiment_name']
        interval_start_time = get_datetime()

        for j in range(num_res):
            update_timestamps = True
            generate_json(find, result_json_file, update_results_json_file, i, update_timestamps)
            result_json = read_json_data_from_file(update_results_json_file)

            if "non_contiguous" in test_name:
                if j == 0:
                    start_time = interval_start_time
                elif j == 1 or j % 10 == 0:
                    start_time = increment_timestamp_by_given_mins(end_time, 15)
                else:
                    start_time = end_time
            else:
                if j == 0:
                    start_time = interval_start_time
                else:
                    start_time = end_time

            result_json[0]['interval_start_time'] = start_time
            end_time = increment_timestamp_by_given_mins(start_time, 15)
            result_json[0]['interval_end_time'] = end_time

            result_json_arr.append(result_json[0])
            complete_result_json_arr.append(result_json[0])

            result_json_arr_len = len(result_json_arr)

            if result_json_arr_len == 100:
                print(f"Updating {result_json_arr_len} results...")
                # Update results for every 100 results
                bulk_update_results_json_file = "/tmp/bulk_update_results_" + str(i) + "_" + str(bulk_res) + ".json"

                write_json_data_to_file(bulk_update_results_json_file, result_json_arr)
                response = update_results(bulk_update_results_json_file, logging)
                data = response.json()

                assert response.status_code == SUCCESS_STATUS_CODE
                assert data['status'] == SUCCESS_STATUS
                assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG
                result_json_arr = []
                total_res = total_res + result_json_arr_len
                bulk_res = bulk_res + 1

        result_json_arr_len = len(result_json_arr)

        if result_json_arr_len != 0:
            print(f"Updating {result_json_arr_len} results...")
            bulk_update_results_json_file = "/tmp/bulk_update_results_" + str(i) + "_" + str(bulk_res) + ".json"
            write_json_data_to_file(bulk_update_results_json_file, result_json_arr)

            response = update_results(bulk_update_results_json_file, logging)

            data = response.json()

            assert response.status_code == SUCCESS_STATUS_CODE
            assert data['status'] == SUCCESS_STATUS
            assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG
            total_res = total_res + result_json_arr_len

        print(f"Total results updated = {total_res}")
        print(f"interval_start_time = {interval_start_time}")
        print(f"end_time = {end_time}")

        end_time = increment_timestamp_by_given_mins(end_time, increment_end_time_by)

        # Get the experiment name
        json_data = json.load(open(create_exp_json_file))
        experiment_name = json_data[0]['experiment_name']

        list_of_result_json_arr.append(complete_result_json_arr)

        response = update_recommendations(experiment_name, None, end_time)
        data = response.json()
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data[0]['experiment_name'] == experiment_name

        if SHORT_TERM in test_name:
            assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications'][
                       NOTIFICATION_CODE_FOR_RECOMMENDATIONS_AVAILABLE][
                       'message'] == RECOMMENDATIONS_AVAILABLE
            validate_term_recommendations(data, end_time, SHORT_TERM)

            assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['data'][end_time][
                       'recommendation_terms'][
                       MEDIUM_TERM]['notifications'][NOTIFICATION_CODE_FOR_NOT_ENOUGH_DATA][
                       'message'] == NOT_ENOUGH_DATA_MSG
            assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['data'][end_time][
                       'recommendation_terms'][
                       LONG_TERM]['notifications'][NOTIFICATION_CODE_FOR_NOT_ENOUGH_DATA][
                       'message'] == NOT_ENOUGH_DATA_MSG
        elif MEDIUM_TERM in test_name:
            assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications'][
                       NOTIFICATION_CODE_FOR_RECOMMENDATIONS_AVAILABLE][
                       'message'] == RECOMMENDATIONS_AVAILABLE

            validate_term_recommendations(data, end_time, SHORT_TERM)
            validate_term_recommendations(data, end_time, MEDIUM_TERM)

            assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['data'][end_time][
                       'recommendation_terms'][
                       LONG_TERM]['notifications'][NOTIFICATION_CODE_FOR_NOT_ENOUGH_DATA][
                       'message'] == NOT_ENOUGH_DATA_MSG
        elif LONG_TERM in test_name:
            assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications'][
                       NOTIFICATION_CODE_FOR_RECOMMENDATIONS_AVAILABLE][
                       'message'] == RECOMMENDATIONS_AVAILABLE
            validate_term_recommendations(data, end_time, SHORT_TERM)
            validate_term_recommendations(data, end_time, MEDIUM_TERM)
            validate_term_recommendations(data, end_time, LONG_TERM)

    experiment_name = None
    latest = True
    response = list_recommendations(experiment_name, latest, rm=True)

    list_reco_json = response.json()
    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json against the json schema
    errorMsg = validate_list_reco_json(list_reco_json, reco_json_schema)
    assert errorMsg == ""
    for i in range(num_exps):
        create_exp_json_file = "/tmp/create_exp_" + str(i) + ".json"
        update_results_json_file = "/tmp/update_results_" + str(i) + ".json"

        # Validate the json values
        create_exp_json = read_json_data_from_file(create_exp_json_file)

        update_results_json = []
        print(len(list_of_result_json_arr[i]))
        update_results_json.append(list_of_result_json_arr[i][len(list_of_result_json_arr[i]) - 1])

        print(update_results_json)

        exp_found = False
        for list_reco in list_reco_json:
            if create_exp_json[0]['experiment_name'] == list_reco['experiment_name']:
                print(f"expected_duration_in_hours = {expected_duration_in_hours}")
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


@pytest.mark.negative
@pytest.mark.parametrize("test_name, num_res, reco_json_schema, expected_duration_in_hours, logging",
                         invalid_term_input)
def test_list_recommendations_invalid_term_min_data_threshold(test_name, num_res, reco_json_schema,
                                                              expected_duration_in_hours, logging, cluster_type):
    """
        Test Description: This test validates list recommendations for all the terms with minimum data threshold not present
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
        complete_result_json_arr = []
        total_res = 0
        bulk_res = 0

        # Get the experiment name
        json_data = json.load(open(create_exp_json_file))
        experiment_name = json_data[0]['experiment_name']
        interval_start_time = get_datetime()
        end_time = ""
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

            result_json_arr.append(result_json[0])

            result_json_arr_len = len(result_json_arr)

            if result_json_arr_len == 100:
                print(f"Updating {result_json_arr_len} results...")
                # Update results for every 100 results
                bulk_update_results_json_file = "/tmp/bulk_update_results_" + str(i) + "_" + str(bulk_res) + ".json"

                write_json_data_to_file(bulk_update_results_json_file, result_json_arr)
                response = update_results(bulk_update_results_json_file, logging)
                data = response.json()

                assert response.status_code == SUCCESS_STATUS_CODE
                assert data['status'] == SUCCESS_STATUS
                assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG
                result_json_arr = []
                total_res = total_res + result_json_arr_len
                bulk_res = bulk_res + 1

            complete_result_json_arr.append(result_json_arr)

        result_json_arr_len = len(result_json_arr)

        if result_json_arr_len != 0:
            print(f"Updating {result_json_arr_len} results...")
            bulk_update_results_json_file = "/tmp/bulk_update_results_" + str(i) + "_" + str(bulk_res) + ".json"
            write_json_data_to_file(bulk_update_results_json_file, result_json_arr)

            response = update_results(bulk_update_results_json_file, logging)

            data = response.json()
            assert response.status_code == SUCCESS_STATUS_CODE
            assert data['status'] == SUCCESS_STATUS
            assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG
            total_res = total_res + result_json_arr_len

        print(f"Total results updated = {total_res}")

        # Get the experiment name
        json_data = json.load(open(create_exp_json_file))
        experiment_name = json_data[0]['experiment_name']

        list_of_result_json_arr.append(complete_result_json_arr)

        response = update_recommendations(experiment_name, None, end_time)
        data = response.json()
        if num_res == 0:
            assert response.status_code == ERROR_STATUS_CODE
            assert data['message'] == UPDATE_RECOMMENDATIONS_MANDATORY_INTERVAL_END_DATE
        else:
            assert response.status_code == SUCCESS_STATUS_CODE
            assert data[0]['experiment_name'] == experiment_name
            if SHORT_TERM in test_name:
                assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications'][
                           NOTIFICATION_CODE_FOR_NOT_ENOUGH_DATA][
                           'message'] == NOT_ENOUGH_DATA_MSG
            elif MEDIUM_TERM in test_name:
                assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications'][
                           NOTIFICATION_CODE_FOR_RECOMMENDATIONS_AVAILABLE][
                           'message'] == RECOMMENDATIONS_AVAILABLE

                validate_term_recommendations(data, end_time, SHORT_TERM)
                assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['data'][end_time][
                           'recommendation_terms'][
                           'long_term']['notifications'][NOTIFICATION_CODE_FOR_NOT_ENOUGH_DATA][
                           'message'] == NOT_ENOUGH_DATA_MSG
            elif LONG_TERM in test_name:
                assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications'][
                           NOTIFICATION_CODE_FOR_RECOMMENDATIONS_AVAILABLE][
                           'message'] == RECOMMENDATIONS_AVAILABLE

                validate_term_recommendations(data, end_time, SHORT_TERM)
                validate_term_recommendations(data, end_time, MEDIUM_TERM)
                assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['data'][end_time][
                           'recommendation_terms'][
                           'long_term']['notifications'][NOTIFICATION_CODE_FOR_NOT_ENOUGH_DATA][
                           'message'] == NOT_ENOUGH_DATA_MSG

    experiment_name = None
    latest = True
    response = list_recommendations(experiment_name, latest, rm=True)

    list_reco_json = response.json()
    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json against the json schema
    errorMsg = validate_list_reco_json(list_reco_json, reco_json_schema)
    assert errorMsg == ""
    for i in range(num_exps):
        create_exp_json_file = "/tmp/create_exp_" + str(i) + ".json"
        update_results_json_file = "/tmp/update_results_" + str(i) + ".json"

        # Validate the json values
        create_exp_json = read_json_data_from_file(create_exp_json_file)

        update_results_json = []

        if latest == "true":
            update_results_json.append(list_of_result_json_arr[i][len(list_of_result_json_arr[i]) - 1])
        elif latest == "false":
            total_num_results = len(list_of_result_json_arr[i])
            # Recommendations will be generated when 0.5h/48h/192h of results are available
            num_results_without_recos = int(expected_duration_in_hours * 4 - 1)
            for j in range(num_results_without_recos, total_num_results):
                update_results_json.append(list_of_result_json_arr[i][j])

        exp_found = False
        for list_reco in list_reco_json:
            if create_exp_json[0]['experiment_name'] == list_reco['experiment_name']:
                print(f"expected_duration_in_hours = {expected_duration_in_hours}")
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


@pytest.mark.negative
@pytest.mark.parametrize(
    "test_name, num_res, reco_json_schema, expected_duration_in_hours, increment_end_time_by, logging",
    term_input_exceeding_limit)
def test_list_recommendations_min_data_threshold_exceeding_max_duration(test_name, num_res, reco_json_schema,
                                                                        expected_duration_in_hours,
                                                                        increment_end_time_by, logging, cluster_type):
    """
           Test Description: This test validates if recommendations generated are exceeding the max duration fixed for
           each term
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
        complete_result_json_arr = []
        total_res = 0
        bulk_res = 0

        interval_start_time = get_datetime()

        for j in range(num_res):
            update_timestamps = True
            generate_json(find, result_json_file, update_results_json_file, i, update_timestamps)
            result_json = read_json_data_from_file(update_results_json_file)

            if j == 0:
                start_time = interval_start_time
            elif j == 1 or j % 10 == 0:
                start_time = increment_timestamp_by_given_mins(end_time, increment_end_time_by)
            else:
                start_time = end_time

            result_json[0]['interval_start_time'] = start_time
            end_time = increment_timestamp_by_given_mins(start_time, 15)
            result_json[0]['interval_end_time'] = end_time

            result_json_arr.append(result_json[0])
            complete_result_json_arr.append(result_json[0])

            result_json_arr_len = len(result_json_arr)

            if result_json_arr_len == 100:
                print(f"Updating {result_json_arr_len} results...")
                # Update results for every 100 results
                bulk_update_results_json_file = "/tmp/bulk_update_results_" + str(i) + "_" + str(bulk_res) + ".json"

                write_json_data_to_file(bulk_update_results_json_file, result_json_arr)
                response = update_results(bulk_update_results_json_file, logging)
                data = response.json()

                assert response.status_code == SUCCESS_STATUS_CODE
                assert data['status'] == SUCCESS_STATUS
                assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG
                result_json_arr = []
                total_res = total_res + result_json_arr_len
                bulk_res = bulk_res + 1

        result_json_arr_len = len(result_json_arr)

        if result_json_arr_len != 0:
            print(f"Updating {result_json_arr_len} results...")
            bulk_update_results_json_file = "/tmp/bulk_update_results_" + str(i) + "_" + str(bulk_res) + ".json"
            write_json_data_to_file(bulk_update_results_json_file, result_json_arr)

            response = update_results(bulk_update_results_json_file, logging)

            data = response.json()

            assert response.status_code == SUCCESS_STATUS_CODE
            assert data['status'] == SUCCESS_STATUS
            assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG
            total_res = total_res + result_json_arr_len

        print(f"Total results updated = {total_res}")
        print(f"interval_start_time = {interval_start_time}")
        print(f"end_time = {end_time}")

        # Get the experiment name
        json_data = json.load(open(create_exp_json_file))
        experiment_name = json_data[0]['experiment_name']

        list_of_result_json_arr.append(complete_result_json_arr)

        response = update_recommendations(experiment_name, None, end_time)
        data = response.json()
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data[0]['experiment_name'] == experiment_name

        if SHORT_TERM in test_name:
            assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications'][
                       NOTIFICATION_CODE_FOR_NOT_ENOUGH_DATA][
                       'message'] == NOT_ENOUGH_DATA_MSG
        elif MEDIUM_TERM in test_name:
            assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications'][
                       NOTIFICATION_CODE_FOR_RECOMMENDATIONS_AVAILABLE][
                       'message'] == RECOMMENDATIONS_AVAILABLE

            validate_term_recommendations(data, end_time, SHORT_TERM)

            assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['data'][end_time][
                       'recommendation_terms'][
                       MEDIUM_TERM]['notifications'][NOTIFICATION_CODE_FOR_NOT_ENOUGH_DATA][
                       'message'] == NOT_ENOUGH_DATA_MSG
            assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['data'][end_time][
                       'recommendation_terms'][
                       LONG_TERM]['notifications'][NOTIFICATION_CODE_FOR_NOT_ENOUGH_DATA][
                       'message'] == NOT_ENOUGH_DATA_MSG
        elif LONG_TERM in test_name:
            assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications'][
                       NOTIFICATION_CODE_FOR_RECOMMENDATIONS_AVAILABLE][
                       'message'] == RECOMMENDATIONS_AVAILABLE
            validate_term_recommendations(data, end_time, SHORT_TERM)
            validate_term_recommendations(data, end_time, MEDIUM_TERM)
            assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['data'][end_time][
                       'recommendation_terms'][
                       LONG_TERM]['notifications'][NOTIFICATION_CODE_FOR_NOT_ENOUGH_DATA][
                       'message'] == NOT_ENOUGH_DATA_MSG

    experiment_name = None
    latest = True
    response = list_recommendations(experiment_name, latest, rm=True)

    list_reco_json = response.json()
    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json against the json schema
    errorMsg = validate_list_reco_json(list_reco_json, reco_json_schema)
    assert errorMsg == ""
    for i in range(num_exps):
        create_exp_json_file = "/tmp/create_exp_" + str(i) + ".json"

        # Validate the json values
        create_exp_json = read_json_data_from_file(create_exp_json_file)

        update_results_json = []
        print(len(list_of_result_json_arr[i]))
        update_results_json.append(list_of_result_json_arr[i][len(list_of_result_json_arr[i]) - 1])

        print(update_results_json)

        exp_found = False
        for list_reco in list_reco_json:
            if create_exp_json[0]['experiment_name'] == list_reco['experiment_name']:
                print(f"expected_duration_in_hours = {expected_duration_in_hours}")
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


@pytest.mark.sanity
@pytest.mark.parametrize("test_name, num_res, reco_json_schema, expected_duration_in_hours, increment_end_time_by, "
                         "logging, long_term_present, medium_term_present, short_term_present",
                         term_input_for_missing_terms)
def test_list_recommendations_for_missing_terms(test_name, num_res, reco_json_schema, expected_duration_in_hours,
                                                increment_end_time_by, logging, long_term_present, medium_term_present,
                                                short_term_present, cluster_type):
    """
        Test Description: This test validates if recommendations are generated when minimum data threshold of data is uploaded for all the terms
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
    list_of_result_json_arr = []
    increment_end_time_for_long = 8640
    increment_end_time_for_medium_and_long = 1440
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
        complete_result_json_arr = []
        total_res = 0
        bulk_res = 0

        interval_start_time = get_datetime()

        for j in range(num_res):
            update_timestamps = True
            generate_json(find, result_json_file, update_results_json_file, i, update_timestamps)
            result_json = read_json_data_from_file(update_results_json_file)

            if j == 0:
                start_time = interval_start_time
            elif SHORT_AND_LONG in test_name:
                if j == num_res - 2:
                    start_time = increment_timestamp_by_given_mins(end_time, increment_end_time_for_long)
                else:
                    start_time = end_time
            elif MEDIUM_AND_LONG in test_name or ONLY_MEDIUM in test_name:
                if j == num_res - 1:
                    start_time = increment_timestamp_by_given_mins(end_time, increment_end_time_for_medium_and_long)
                else:
                    start_time = end_time
            elif ONLY_LONG in test_name:
                if j == num_res - 2:
                    start_time = increment_timestamp_by_given_mins(end_time, increment_end_time_for_long)
                elif j == num_res - 1:
                    start_time = increment_timestamp_by_given_mins(end_time, increment_end_time_for_medium_and_long)
                else:
                    start_time = end_time
            else:
                start_time = end_time

            result_json[0]['interval_start_time'] = start_time
            end_time = increment_timestamp_by_given_mins(start_time, 15)
            result_json[0]['interval_end_time'] = end_time

            result_json_arr.append(result_json[0])
            complete_result_json_arr.append(result_json[0])

            result_json_arr_len = len(result_json_arr)

            if result_json_arr_len == 100:
                print(f"Updating {result_json_arr_len} results...")
                # Update results for every 100 results
                bulk_update_results_json_file = "/tmp/bulk_update_results_" + str(i) + "_" + str(bulk_res) + ".json"

                write_json_data_to_file(bulk_update_results_json_file, result_json_arr)
                response = update_results(bulk_update_results_json_file, logging)
                data = response.json()

                assert response.status_code == SUCCESS_STATUS_CODE
                assert data['status'] == SUCCESS_STATUS
                assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG
                result_json_arr = []
                total_res = total_res + result_json_arr_len
                bulk_res = bulk_res + 1

        result_json_arr_len = len(result_json_arr)

        if result_json_arr_len != 0:
            print(f"Updating {result_json_arr_len} results...")
            bulk_update_results_json_file = "/tmp/bulk_update_results_" + str(i) + "_" + str(bulk_res) + ".json"
            write_json_data_to_file(bulk_update_results_json_file, result_json_arr)

            response = update_results(bulk_update_results_json_file, logging)

            data = response.json()

            assert response.status_code == SUCCESS_STATUS_CODE
            assert data['status'] == SUCCESS_STATUS
            assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG
            total_res = total_res + result_json_arr_len

        print(f"Total results updated = {total_res}")
        print(f"interval_start_time = {interval_start_time}")
        print(f"end_time = {end_time}")

        # Get the experiment name
        json_data = json.load(open(create_exp_json_file))
        experiment_name = json_data[0]['experiment_name']

        list_of_result_json_arr.append(complete_result_json_arr)

        response = update_recommendations(experiment_name, None, end_time)
        data = response.json()
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data[0]['experiment_name'] == experiment_name

        if not short_term_present and not medium_term_present and not long_term_present:
            assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications'][
                       NOTIFICATION_CODE_FOR_NOT_ENOUGH_DATA]['message'] == NOT_ENOUGH_DATA_MSG
        else:
            if not short_term_present:
                assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['data'][end_time][
                           'recommendation_terms'][SHORT_TERM]['notifications'][NOTIFICATION_CODE_FOR_NOT_ENOUGH_DATA][
                           'message'] == NOT_ENOUGH_DATA_MSG

            if not medium_term_present:
                assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['data'][end_time][
                           'recommendation_terms'][MEDIUM_TERM]['notifications'][NOTIFICATION_CODE_FOR_NOT_ENOUGH_DATA][
                           'message'] == NOT_ENOUGH_DATA_MSG

            if not long_term_present:
                assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['data'][end_time][
                           'recommendation_terms'][LONG_TERM]['notifications'][NOTIFICATION_CODE_FOR_NOT_ENOUGH_DATA][
                           'message'] == NOT_ENOUGH_DATA_MSG

        if short_term_present:
            validate_term_recommendations(data, end_time, SHORT_TERM)

        if medium_term_present:
            validate_term_recommendations(data, end_time, MEDIUM_TERM)

        if long_term_present:
            validate_term_recommendations(data, end_time, LONG_TERM)

    experiment_name = None
    latest = True
    response = list_recommendations(experiment_name, latest, rm=True)

    list_reco_json = response.json()
    assert response.status_code == SUCCESS_200_STATUS_CODE

    if reco_json_schema is not None:
        # Validate the json against the json schema
        errorMsg = validate_list_reco_json(list_reco_json, reco_json_schema)
        assert errorMsg == ""
        for i in range(num_exps):
            create_exp_json_file = "/tmp/create_exp_" + str(i) + ".json"
            update_results_json_file = "/tmp/update_results_" + str(i) + ".json"

            # Validate the json values
            create_exp_json = read_json_data_from_file(create_exp_json_file)

            update_results_json = []
            print(len(list_of_result_json_arr[i]))
            update_results_json.append(list_of_result_json_arr[i][len(list_of_result_json_arr[i]) - 1])

            print(update_results_json)

            exp_found = False
            for list_reco in list_reco_json:
                if create_exp_json[0]['experiment_name'] == list_reco['experiment_name']:
                    print(f"expected_duration_in_hours = {expected_duration_in_hours}")
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


@pytest.mark.sanity
@pytest.mark.parametrize("test_name, num_res, reco_json_schema, expected_duration_in_hours, increment_end_time_by, "
                         "logging, long_term_present, medium_term_present, short_term_present",
                         term_input_for_missing_terms_non_contiguous)
def test_list_recommendations_for_missing_terms_non_contiguous(test_name, num_res, reco_json_schema,
                                                               expected_duration_in_hours, increment_end_time_by,
                                                               logging, long_term_present, medium_term_present,
                                                               short_term_present, cluster_type):
    """
        Test Description: This test validates if recommendations are generated when minimum data threshold of data is uploaded for all the terms
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
    list_of_result_json_arr = []
    increment_end_time_for_long = 7200
    increment_end_time_for_medium_and_long = 1440
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
        complete_result_json_arr = []
        total_res = 0
        bulk_res = 0

        interval_start_time = get_datetime()

        for j in range(num_res):
            update_timestamps = True
            generate_json(find, result_json_file, update_results_json_file, i, update_timestamps)
            result_json = read_json_data_from_file(update_results_json_file)

            if j == 0:
                start_time = interval_start_time
            elif j == 1 or j % 10 == 0:
                start_time = increment_timestamp_by_given_mins(end_time, increment_end_time_by)
            elif SHORT_AND_LONG in test_name:
                if j == num_res - 2:
                    start_time = increment_timestamp_by_given_mins(end_time, increment_end_time_for_long)
                else:
                    start_time = end_time
            elif MEDIUM_AND_LONG in test_name or ONLY_MEDIUM in test_name:
                if j == num_res - 1:
                    start_time = increment_timestamp_by_given_mins(end_time, increment_end_time_for_medium_and_long)
                else:
                    start_time = end_time
            elif ONLY_LONG in test_name:
                if j == num_res - 2:
                    start_time = increment_timestamp_by_given_mins(end_time, increment_end_time_for_long)
                elif j == num_res - 1:
                    start_time = increment_timestamp_by_given_mins(end_time, increment_end_time_for_medium_and_long)
                else:
                    start_time = end_time
            else:
                start_time = end_time

            result_json[0]['interval_start_time'] = start_time
            end_time = increment_timestamp_by_given_mins(start_time, 15)
            result_json[0]['interval_end_time'] = end_time

            result_json_arr.append(result_json[0])
            complete_result_json_arr.append(result_json[0])

            result_json_arr_len = len(result_json_arr)

            if result_json_arr_len == 100:
                print(f"Updating {result_json_arr_len} results...")
                # Update results for every 100 results
                bulk_update_results_json_file = "/tmp/bulk_update_results_" + str(i) + "_" + str(bulk_res) + ".json"

                write_json_data_to_file(bulk_update_results_json_file, result_json_arr)
                response = update_results(bulk_update_results_json_file, logging)
                data = response.json()

                assert response.status_code == SUCCESS_STATUS_CODE
                assert data['status'] == SUCCESS_STATUS
                assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG
                result_json_arr = []
                total_res = total_res + result_json_arr_len
                bulk_res = bulk_res + 1

        result_json_arr_len = len(result_json_arr)

        if result_json_arr_len != 0:
            print(f"Updating {result_json_arr_len} results...")
            bulk_update_results_json_file = "/tmp/bulk_update_results_" + str(i) + "_" + str(bulk_res) + ".json"
            write_json_data_to_file(bulk_update_results_json_file, result_json_arr)

            response = update_results(bulk_update_results_json_file, logging)

            data = response.json()

            assert response.status_code == SUCCESS_STATUS_CODE
            assert data['status'] == SUCCESS_STATUS
            assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG
            total_res = total_res + result_json_arr_len

        print(f"Total results updated = {total_res}")
        print(f"interval_start_time = {interval_start_time}")
        print(f"end_time = {end_time}")

        # Get the experiment name
        json_data = json.load(open(create_exp_json_file))
        experiment_name = json_data[0]['experiment_name']

        list_of_result_json_arr.append(complete_result_json_arr)

        response = update_recommendations(experiment_name, None, end_time)
        data = response.json()
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data[0]['experiment_name'] == experiment_name

        if not short_term_present:
            assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['data'][end_time][
                       'recommendation_terms'][SHORT_TERM]['notifications'][NOTIFICATION_CODE_FOR_NOT_ENOUGH_DATA][
                       'message'] == NOT_ENOUGH_DATA_MSG
        if not medium_term_present:
            assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['data'][end_time][
                       'recommendation_terms'][MEDIUM_TERM]['notifications'][NOTIFICATION_CODE_FOR_NOT_ENOUGH_DATA][
                       'message'] == NOT_ENOUGH_DATA_MSG
        if not long_term_present:
            assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['data'][end_time][
                       'recommendation_terms'][LONG_TERM]['notifications'][NOTIFICATION_CODE_FOR_NOT_ENOUGH_DATA][
                       'message'] == NOT_ENOUGH_DATA_MSG
        if short_term_present:
            validate_term_recommendations(data, end_time, SHORT_TERM)
        if medium_term_present:
            validate_term_recommendations(data, end_time, MEDIUM_TERM)
        if long_term_present:
            validate_term_recommendations(data, end_time, LONG_TERM)

    experiment_name = None
    latest = True
    response = list_recommendations(experiment_name, latest, rm=True)

    list_reco_json = response.json()
    assert response.status_code == SUCCESS_200_STATUS_CODE

    if reco_json_schema is not None:
        # Validate the json against the json schema
        errorMsg = validate_list_reco_json(list_reco_json, reco_json_schema)
        assert errorMsg == ""
        for i in range(num_exps):
            create_exp_json_file = "/tmp/create_exp_" + str(i) + ".json"
            update_results_json_file = "/tmp/update_results_" + str(i) + ".json"

            # Validate the json values
            create_exp_json = read_json_data_from_file(create_exp_json_file)

            update_results_json = []
            print(len(list_of_result_json_arr[i]))
            update_results_json.append(list_of_result_json_arr[i][len(list_of_result_json_arr[i]) - 1])

            print(update_results_json)

            exp_found = False
            for list_reco in list_reco_json:
                if create_exp_json[0]['experiment_name'] == list_reco['experiment_name']:
                    print(f"expected_duration_in_hours = {expected_duration_in_hours}")
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


def assert_notification_presence(data, end_time, term, notification_code):
    assert notification_code in data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['data'][end_time][
        'notifications'], f"{term} data is not present"


def assert_notification_absence(data, end_time, term, notification_code):
    assert notification_code not in data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['data'][
        end_time]['notifications'], f"{term} data is present"


def validate_and_assert_term_recommendations(data, end_time, term):
    assert_notification_presence(data, end_time, term, TERMS_NOTIFICATION_CODES[term])
    validate_term_recommendations(data, end_time, term)


@pytest.mark.sanity
def test_list_recommendations_cpu_mem_optimised(cluster_type: str):
    """
        Test Description: This test loads constant cpu and memory data for 15 days to check if autotune
        sends the recommendation same as current along with notifications
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
    num_res = 1450  # 15 days + 10 entries buffer

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
            sum_field = avg_field = min_field = max_field = value_field = 0.00
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

            CPU_REQUEST_INDEX = get_index_of_metric(container_metrics, CPU_REQUEST)
            CPU_LIMIT_INDEX = get_index_of_metric(container_metrics, CPU_LIMIT)
            CPU_USAGE_INDEX = get_index_of_metric(container_metrics, CPU_USAGE)
            CPU_THROTTLE_INDEX = get_index_of_metric(container_metrics, CPU_THROTTLE)

            MEMORY_REQUEST_INDEX = get_index_of_metric(container_metrics, MEMORY_REQUEST)
            MEMORY_LIMIT_INDEX = get_index_of_metric(container_metrics, MEMORY_LIMIT)
            MEMORY_USAGE_INDEX = get_index_of_metric(container_metrics, MEMORY_USAGE)
            MEMORY_RSS_INDEX = get_index_of_metric(container_metrics, MEMORY_RSS)

            if CPU_REQUEST_INDEX is not None:
                cpu_request_entry = container_metrics[CPU_REQUEST_INDEX]
                aggre_info = cpu_request_entry["results"]["aggregation_info"]
                aggre_info["avg"] = OPTIMISED_CPU
                aggre_info["sum"] = OPTIMISED_CPU

            if CPU_LIMIT_INDEX is not None:
                cpu_limit_entry = container_metrics[CPU_LIMIT_INDEX]
                aggre_info = cpu_limit_entry["results"]["aggregation_info"]
                aggre_info["avg"] = OPTIMISED_CPU
                aggre_info["sum"] = OPTIMISED_CPU

            if CPU_USAGE_INDEX is not None:
                cpu_usage_entry = container_metrics[CPU_USAGE_INDEX]
                aggre_info = cpu_usage_entry["results"]["aggregation_info"]
                aggre_info["avg"] = OPTIMISED_CPU
                aggre_info["sum"] = OPTIMISED_CPU
                aggre_info["min"] = OPTIMISED_CPU
                aggre_info["max"] = OPTIMISED_CPU

            if CPU_THROTTLE_INDEX is not None:
                cpu_throttle_entry = container_metrics[CPU_THROTTLE_INDEX]
                aggre_info = cpu_throttle_entry["results"]["aggregation_info"]
                aggre_info["avg"] = 0.00
                aggre_info["sum"] = 0.00
                aggre_info["max"] = 0.00

            if MEMORY_REQUEST_INDEX is not None:
                memory_request_entry = container_metrics[MEMORY_REQUEST_INDEX]
                aggre_info = memory_request_entry["results"]["aggregation_info"]
                aggre_info["avg"] = OPTIMISED_MEMORY
                aggre_info["sum"] = OPTIMISED_MEMORY

            if MEMORY_LIMIT_INDEX is not None:
                memory_limit_entry = container_metrics[MEMORY_LIMIT_INDEX]
                aggre_info = memory_limit_entry["results"]["aggregation_info"]
                aggre_info["avg"] = OPTIMISED_MEMORY
                aggre_info["sum"] = OPTIMISED_MEMORY

            if MEMORY_USAGE_INDEX is not None:
                memory_usage_entry = container_metrics[MEMORY_USAGE_INDEX]
                aggre_info = memory_usage_entry["results"]["aggregation_info"]
                aggre_info["avg"] = OPTIMISED_MEMORY
                aggre_info["sum"] = OPTIMISED_MEMORY
                aggre_info["min"] = OPTIMISED_MEMORY
                aggre_info["max"] = OPTIMISED_MEMORY

            if MEMORY_RSS_INDEX is not None:
                memory_rss_entry = container_metrics[MEMORY_RSS_INDEX]
                aggre_info = memory_rss_entry["results"]["aggregation_info"]
                aggre_info["avg"] = OPTIMISED_MEMORY
                aggre_info["sum"] = OPTIMISED_MEMORY
                aggre_info["min"] = OPTIMISED_MEMORY
                aggre_info["max"] = OPTIMISED_MEMORY

            write_json_data_to_file(update_results_json_file, result_json)
            result_json_arr.append(result_json[0])
            response = update_results(update_results_json_file)

            data = response.json()
            print("message = ", data['message'])

            if j > 95:
                response = update_recommendations(experiment_name, None, end_time)
                data = response.json()
                assert response.status_code == SUCCESS_STATUS_CODE
                assert data[0]['experiment_name'] == experiment_name

                # Get the experiment name
                json_data = json.load(open(create_exp_json_file))
                experiment_name = json_data[0]['experiment_name']

                response = list_recommendations(experiment_name, rm=True)
                assert response.status_code == SUCCESS_200_STATUS_CODE

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
                medium_term_recommendation = None
                long_term_recommendation = None
                if j > 671:  # 7 days
                    medium_term_recommendation = data_section[str(end_time)]["recommendation_terms"]["medium_term"]
                if j > 1439:  # 15 days
                    long_term_recommendation = data_section[str(end_time)]["recommendation_terms"]["long_term"]

                if INFO_COST_RECOMMENDATIONS_AVAILABLE_CODE in short_term_recommendation["notifications"]:
                    validate_recommendation_for_cpu_mem_optimised(recommendations=short_term_recommendation,
                                                                  current=recommendation_current,
                                                                  profile="cost")

                    if j > 671:
                        validate_recommendation_for_cpu_mem_optimised(recommendations=medium_term_recommendation,
                                                                      current=recommendation_current,
                                                                      profile="cost")
                    if j > 1439:
                        validate_recommendation_for_cpu_mem_optimised(recommendations=long_term_recommendation,
                                                                      current=recommendation_current,
                                                                      profile="cost")

                if INFO_PERFORMANCE_RECOMMENDATIONS_AVAILABLE_CODE in short_term_recommendation["notifications"]:
                    validate_recommendation_for_cpu_mem_optimised(recommendations=short_term_recommendation,
                                                                  current=recommendation_current,
                                                                  profile="performance")

                    if j > 671:
                        validate_recommendation_for_cpu_mem_optimised(recommendations=medium_term_recommendation,
                                                                      current=recommendation_current,
                                                                      profile="performance")
                    if j > 1439:
                        validate_recommendation_for_cpu_mem_optimised(recommendations=long_term_recommendation,
                                                                      current=recommendation_current,
                                                                      profile="performance")

                short_term_recommendation_cost_notifications = \
                    short_term_recommendation["recommendation_engines"]["cost"]["notifications"]
                short_term_recommendation_perf_notifications = \
                    short_term_recommendation["recommendation_engines"]["performance"]["notifications"]

                check_optimised_codes(short_term_recommendation_cost_notifications,
                                      short_term_recommendation_perf_notifications)

                if j > 672:
                    medium_term_recommendation_cost_notifications = \
                        medium_term_recommendation["recommendation_engines"]["cost"]["notifications"]
                    medium_term_recommendation_perf_notifications = \
                        medium_term_recommendation["recommendation_engines"]["performance"]["notifications"]

                    check_optimised_codes(medium_term_recommendation_cost_notifications,
                                          medium_term_recommendation_perf_notifications)

                if j > 1439:
                    long_term_recommendation_cost_notifications = \
                        long_term_recommendation["recommendation_engines"]["cost"]["notifications"]
                    long_term_recommendation_perf_notifications = \
                        long_term_recommendation["recommendation_engines"]["performance"]["notifications"]

                    check_optimised_codes(long_term_recommendation_cost_notifications,
                                          long_term_recommendation_perf_notifications)

    # Delete the experiments
    for i in range(num_exps):
        json_file = "/tmp/create_exp_" + str(i) + ".json"

        response = delete_experiment(json_file)
        print("delete exp = ", response.status_code)


@pytest.mark.sanity
@pytest.mark.parametrize("test_name,num_days,logging,update_metrics,code,message", profile_notifications)
def test_list_recommendations_profile_notifications(test_name, num_days, logging, update_metrics, code, message,
                                                    cluster_type: str):
    """
        Test Description: Check if notifications are generated at profile level if cpu_usage is less than millicore
    """
    input_json_file = "../json_files/create_exp.json"
    result_json_file = "../json_files/update_results.json"
    print("Test Name --- %s " % (test_name))
    find = []
    json_data = json.load(open(input_json_file))

    find.append(json_data[0]['experiment_name'])
    find.append(json_data[0]['kubernetes_objects'][0]['name'])
    find.append(json_data[0]['kubernetes_objects'][0]['namespace'])

    form_kruize_url(cluster_type)

    # Create experiment using the specified json
    num_exps = 1
    num_res = 96 * num_days
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
            update_metrics_json(find, result_json_file, update_results_json_file, i, update_metrics, update_timestamps)
            result_json = read_json_data_from_file(update_results_json_file)
            if j == 0:
                start_time = interval_start_time
            else:
                start_time = end_time
            result_json[0]['interval_start_time'] = start_time
            end_time = increment_timestamp_by_given_mins(start_time, 15)
            result_json[0]['interval_end_time'] = end_time
            result_json_arr.append(result_json[0])
        write_json_data_to_file(update_results_json_file, result_json_arr)
        response = update_results(update_results_json_file, logging)

        data = response.json()
        print("message = ", data['message'])
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

        # Get the experiment name
        json_data = json.load(open(create_exp_json_file))
        experiment_name = json_data[0]['experiment_name']

        response = update_recommendations(experiment_name, None, end_time)
        data = response.json()
        assert response.status_code == SUCCESS_STATUS_CODE
        validate_recommendations_notifications(experiment_name, end_time, code, message, data)

        response = list_recommendations(experiment_name, rm=True)
        assert response.status_code == SUCCESS_200_STATUS_CODE
        data = response.json()
        validate_recommendations_notifications(experiment_name, end_time, code, message, data)

    # Delete the experiments
    for i in range(num_exps):
        json_file = "/tmp/create_exp_" + str(i) + ".json"

        response = delete_experiment(json_file)
        print("delete exp = ", response.status_code)


def validate_recommendations_notifications(experiment_name, end_time, code, message, data):
    assert data[0]['experiment_name'] == experiment_name
    assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications'][
               NOTIFICATION_CODE_FOR_RECOMMENDATIONS_AVAILABLE][
               'message'] == RECOMMENDATIONS_AVAILABLE
    short_term_recommendation = \
        data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['data'][str(end_time)][
            "recommendation_terms"][
            "short_term"]

    assert short_term_recommendation['notifications'][NOTIFICATION_CODE_FOR_COST_RECOMMENDATIONS_AVAILABLE][
               'message'] == COST_RECOMMENDATIONS_AVAILABLE
    assert short_term_recommendation['notifications'][NOTIFICATION_CODE_FOR_PERFORMANCE_RECOMMENDATIONS_AVAILABLE][
               'message'] == PERFORMANCE_RECOMMENDATIONS_AVAILABLE

    assert short_term_recommendation['recommendation_engines']['cost']['notifications'][code]['message'] == message
    assert short_term_recommendation['recommendation_engines']['performance']['notifications'][code][
               'message'] == message


@pytest.mark.sanity
def test_list_recommendations_job_type_exp(cluster_type):
    """
    Test Description: This test validates listRecommendations by updating multiple results for a single experiment
    """
    input_json_file = "../json_files/create_exp_job_type.json"

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
    result_json_file = "../json_files/multiple_results_single_job_type_exp.json"
    response = update_results(result_json_file, False)

    # Get the experiment name
    json_data = json.load(open(input_json_file))
    experiment_name = json_data[0]['experiment_name']
    end_time = "2023-04-14T23:59:20.982Z"

    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

    response = update_recommendations(experiment_name, None, end_time)
    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data[0]['experiment_name'] == experiment_name
    assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications'][
               NOTIFICATION_CODE_FOR_RECOMMENDATIONS_AVAILABLE]['message'] == RECOMMENDATIONS_AVAILABLE

    response = list_recommendations(experiment_name, rm=True)

    list_reco_json = response.json()
    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json against the json schema
    errorMsg = validate_list_reco_json(list_reco_json, list_reco_json_schema)
    assert errorMsg == ""

    # Validate the json values
    create_exp_json = read_json_data_from_file(input_json_file)

    expected_duration_in_hours = SHORT_TERM_DURATION_IN_HRS_MAX

    update_results_json = []
    result_json_arr = read_json_data_from_file(result_json_file)
    update_results_json.append(result_json_arr[len(result_json_arr) - 1])
    validate_reco_json(create_exp_json[0], update_results_json, list_reco_json[0], expected_duration_in_hours)

    # Delete the experiment
    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)


@pytest.mark.extended
@pytest.mark.parametrize("test_name, num_days, reco_json_schema, expected_duration_in_hours, latest, logging",
                         reco_term_input)
def test_list_recommendations_for_namespace_for_diff_reco_terms_with_only_latest(test_name, num_days, reco_json_schema,
                                                                   expected_duration_in_hours, latest, logging,
                                                                   cluster_type):
    """
        Test Description: This test validates list recommendations for namespace experiments for all the terms for multiple experiments posted using different json files
                          and query with only the parameter latest and with both latest=true and latest=false
    """
    input_json_file = "../json_files/create_exp_namespace.json"
    result_json_file = "../json_files/update_results_namespace.json"

    find = []
    json_data = json.load(open(input_json_file))

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
            response = update_results(update_results_json_file, logging)

            data = response.json()
            print("message = ", data['message'])
            assert response.status_code == SUCCESS_STATUS_CODE
            assert data['status'] == SUCCESS_STATUS
            assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

            update_recommendations(experiment_name, None, end_time)

            # Get the experiment name
            json_data = json.load(open(create_exp_json_file))
            experiment_name = json_data[0]['experiment_name']

            response = list_recommendations(experiment_name, rm=True)
            assert response.status_code == SUCCESS_200_STATUS_CODE

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


@pytest.mark.sanity
def test_list_recommendations_namespace_without_results(cluster_type):
    """
    Test Description: This test validates listRecommendations when there was no updation of results
    """
    input_json_file = "../json_files/create_exp_namespace.json"

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

    response = list_recommendations(experiment_name, rm=True)

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
