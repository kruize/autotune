"""
Copyright (c) 2026 IBM Corporation and others.

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
import json
import pytest
import requests
import sys

sys.path.append("../../")

from helpers.fixtures import *
from helpers.generate_rm_jsons import *
from helpers.kruize import *
from helpers.utils import *


@pytest.mark.test_metrics
def test_metrics_endpoint_is_accessible(cluster_type):
    """Test that the /metrics endpoint returns a 200 response."""
    form_kruize_url(cluster_type)
    response = scrape_metrics()
    assert response.status_code == SUCCESS_200_STATUS_CODE, \
        f"/metrics returned {response.status_code}: {response.text[:200]}"


@pytest.mark.test_metrics
def test_metrics_no_seconds_suffix(cluster_type):
    """
    Test that Timer metric names do NOT have a _seconds suffix.
    After the micrometer 1.17.0 upgrade, NamingConvention.dot must be set
    before metric registration to prevent _seconds from being appended.
    """
    form_kruize_url(cluster_type)

    input_json_file = "../json_files/create_exp.json"
    result_json_file = "../json_files/update_results.json"

    find = []
    json_data = json.load(open(input_json_file))
    find.append(json_data[0]['experiment_name'])
    find.append(json_data[0]['kubernetes_objects'][0]['name'])
    find.append(json_data[0]['kubernetes_objects'][0]['namespace'])

    create_exp_json_file = "/tmp/create_exp_metrics_test.json"
    generate_json(find, input_json_file, create_exp_json_file, 9999)

    delete_experiment(create_exp_json_file)

    response = create_experiment(create_exp_json_file)
    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS

    update_results_json_file = "/tmp/update_results_metrics_test.json"
    generate_json(find, result_json_file, update_results_json_file, 9999, True)
    result_json = read_json_data_from_file(update_results_json_file)

    json_data = json.load(open(create_exp_json_file))
    experiment_name = json_data[0]['experiment_name']
    start_time = get_datetime()
    result_json[0]['interval_start_time'] = start_time
    end_time = increment_timestamp_by_given_mins(start_time, 15)
    result_json[0]['interval_end_time'] = end_time
    write_json_data_to_file(update_results_json_file, result_json)

    response = update_results(update_results_json_file)
    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE

    response = scrape_metrics()
    assert response.status_code == SUCCESS_200_STATUS_CODE
    metrics_text = response.text

    incorrect_names = [
        "kruizeAPI_seconds_count",
        "kruizeAPI_seconds_sum",
        "kruizeAPI_seconds_max",
        "kruizeDB_seconds_count",
        "kruizeDB_seconds_sum",
        "kruizeDB_seconds_max",
        "KruizeMethod_seconds_count",
        "KruizeMethod_seconds_sum",
        "KruizeMethod_seconds_max",
    ]
    metric_names = parse_metric_names(metrics_text)
    for bad_name in incorrect_names:
        assert bad_name not in metric_names, \
            f"Found metric with _seconds suffix: {bad_name}. " \
            f"NamingConvention.dot must be set before metric registration."

    delete_experiment(create_exp_json_file)


@pytest.mark.test_metrics
def test_metrics_exposed_after_api_calls(cluster_type):
    """
    Test that kruizeAPI and kruizeDB timer metrics are exposed on /metrics
    after creating an experiment and updating results.
    """
    form_kruize_url(cluster_type)

    input_json_file = "../json_files/create_exp.json"
    result_json_file = "../json_files/update_results.json"

    find = []
    json_data = json.load(open(input_json_file))
    find.append(json_data[0]['experiment_name'])
    find.append(json_data[0]['kubernetes_objects'][0]['name'])
    find.append(json_data[0]['kubernetes_objects'][0]['namespace'])

    create_exp_json_file = "/tmp/create_exp_metrics_test.json"
    generate_json(find, input_json_file, create_exp_json_file, 9998)

    delete_experiment(create_exp_json_file)

    response = create_experiment(create_exp_json_file)
    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS

    update_results_json_file = "/tmp/update_results_metrics_test.json"
    generate_json(find, result_json_file, update_results_json_file, 9998, True)
    result_json = read_json_data_from_file(update_results_json_file)

    json_data = json.load(open(create_exp_json_file))
    experiment_name = json_data[0]['experiment_name']
    start_time = get_datetime()
    result_json[0]['interval_start_time'] = start_time
    end_time = increment_timestamp_by_given_mins(start_time, 15)
    result_json[0]['interval_end_time'] = end_time
    write_json_data_to_file(update_results_json_file, result_json)

    response = update_results(update_results_json_file)
    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE

    response = scrape_metrics()
    assert response.status_code == SUCCESS_200_STATUS_CODE
    metric_names = parse_metric_names(response.text)

    for expected in EXPECTED_API_TIMER_METRICS:
        assert expected in metric_names, \
            f"Expected metric '{expected}' not found on /metrics endpoint. " \
            f"Found: {sorted([m for m in metric_names if 'kruize' in m.lower()])}"

    for expected in EXPECTED_DB_TIMER_METRICS:
        assert expected in metric_names, \
            f"Expected metric '{expected}' not found on /metrics endpoint. " \
            f"Found: {sorted([m for m in metric_names if 'kruize' in m.lower()])}"

    delete_experiment(create_exp_json_file)


@pytest.mark.test_metrics
def test_metrics_gauge_registered_on_startup(cluster_type):
    """
    Test that the kruizeAPI_active_jobs_count gauge is registered on startup,
    even without any API calls (it's registered in the MetricsConfig constructor).
    """
    form_kruize_url(cluster_type)

    response = scrape_metrics()
    assert response.status_code == SUCCESS_200_STATUS_CODE
    metric_names = parse_metric_names(response.text)

    for expected in EXPECTED_GAUGE_METRICS:
        assert expected in metric_names, \
            f"Expected gauge metric '{expected}' not found on /metrics. " \
            f"This metric should be registered on startup."


@pytest.mark.test_metrics
def test_metrics_contain_application_tag(cluster_type):
    """
    Test that all Kruize metrics include the application='Kruize' tag.
    """
    form_kruize_url(cluster_type)

    input_json_file = "../json_files/create_exp.json"
    result_json_file = "../json_files/update_results.json"

    find = []
    json_data = json.load(open(input_json_file))
    find.append(json_data[0]['experiment_name'])
    find.append(json_data[0]['kubernetes_objects'][0]['name'])
    find.append(json_data[0]['kubernetes_objects'][0]['namespace'])

    create_exp_json_file = "/tmp/create_exp_metrics_test.json"
    generate_json(find, input_json_file, create_exp_json_file, 9997)

    delete_experiment(create_exp_json_file)

    response = create_experiment(create_exp_json_file)
    assert response.status_code == SUCCESS_STATUS_CODE

    update_results_json_file = "/tmp/update_results_metrics_test.json"
    generate_json(find, result_json_file, update_results_json_file, 9997, True)
    result_json = read_json_data_from_file(update_results_json_file)

    start_time = get_datetime()
    result_json[0]['interval_start_time'] = start_time
    end_time = increment_timestamp_by_given_mins(start_time, 15)
    result_json[0]['interval_end_time'] = end_time
    write_json_data_to_file(update_results_json_file, result_json)

    response = update_results(update_results_json_file)
    assert response.status_code == SUCCESS_STATUS_CODE

    response = scrape_metrics()
    assert response.status_code == SUCCESS_200_STATUS_CODE

    kruize_lines = [
        line for line in response.text.split("\n")
        if not line.startswith("#") and line.strip()
        and ("kruizeAPI_" in line or "kruizeDB_" in line or "KruizeMethod_" in line)
    ]

    for line in kruize_lines:
        assert 'application="Kruize"' in line, \
            f"Missing application='Kruize' tag in metric line: {line}"

    delete_experiment(create_exp_json_file)
