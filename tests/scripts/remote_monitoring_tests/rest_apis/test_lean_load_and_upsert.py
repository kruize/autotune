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
import re

import pytest
import sys

sys.path.append("../../")
from helpers.fixtures import *
from helpers.list_reco_json_validate import *
from helpers.utils import *
from helpers.kruize import *
from helpers.reco_json_schemas import list_reco_namespace_json_local_monitoring_schema


# ===========================================================================
# A. Lean Load Parity Tests (I-1, I-2, I-3, I-4)
#
# These tests verify that the lean-load (JDBC + stream parser) path produces
# valid recommendations for each experiment type. They create an experiment,
# post enough results for short-term recommendations, call
# updateRecommendations, and validate the response against the JSON schema
# and value-level assertions.
# ===========================================================================


@pytest.mark.sanity
def test_lean_load_parity_container(cluster_type):
    """
    I-1: Create a container experiment, post N intervals of results,
    call updateRecommendations. Validate that recommendations are
    generated correctly with CPU and memory config values present.
    """
    input_json_file = "../json_files/create_exp.json"
    result_json_file = "../json_files/update_results.json"

    json_data = json.load(open(input_json_file))
    find = [
        json_data[0]['experiment_name'],
        json_data[0]['kubernetes_objects'][0]['name'],
        json_data[0]['kubernetes_objects'][0]['namespace'],
    ]

    form_kruize_url(cluster_type)
    create_exp_json_file, experiment_name, run_id = setup_experiment(
        cluster_type, input_json_file, find
    )

    num_results = 2
    result_json_arr, end_time = post_results(
        result_json_file, find, num_results, run_id=run_id
    )

    list_reco_json = generate_and_validate_recommendations(
        experiment_name, end_time, is_namespace=False
    )

    # Validate schema
    errorMsg = validate_list_reco_json(list_reco_json, list_reco_json_schema)
    assert errorMsg == "", f"Schema validation failed: {errorMsg}"

    # Validate values
    create_exp_json = read_json_data_from_file(create_exp_json_file)
    update_results_json = [result_json_arr[-1]]
    expected_duration_in_hours = SHORT_TERM_DURATION_IN_HRS_MIN
    validate_reco_json(create_exp_json[0], update_results_json, list_reco_json[0], expected_duration_in_hours)

    # Verify recommendation engines have CPU and memory config
    short_term = extract_reco_values(list_reco_json, end_time, is_namespace=False)
    cost_config = short_term["recommendation_engines"]["cost"]["config"]
    assert "requests" in cost_config or "limits" in cost_config, \
        "Cost recommendation config should contain requests or limits"

    cleanup_experiment(create_exp_json_file)


@pytest.mark.sanity
def test_lean_load_parity_namespace(cluster_type):
    """
    I-2: Create a namespace experiment, post N intervals of namespace results,
    call updateRecommendations. Validate that namespace recommendations are
    generated correctly.
    """
    input_json_file = "../json_files/create_exp_namespace.json"
    result_json_file = "../json_files/update_results_namespace.json"

    json_data = json.load(open(input_json_file))
    find = [
        json_data[0]['experiment_name'],
        json_data[0]['kubernetes_objects'][0]['namespaces']['namespace'],
    ]

    form_kruize_url(cluster_type)
    create_exp_json_file, experiment_name, run_id = setup_experiment(
        cluster_type, input_json_file, find
    )

    num_results = 2
    result_json_arr, end_time = post_results(
        result_json_file, find, num_results, run_id=run_id
    )

    list_reco_json = generate_and_validate_recommendations(
        experiment_name, end_time, is_namespace=True
    )

    # Validate schema
    errorMsg = validate_list_reco_json(list_reco_json, list_reco_namespace_json_local_monitoring_schema)
    assert errorMsg == "", f"Schema validation failed: {errorMsg}"

    # Validate values
    create_exp_json = read_json_data_from_file(create_exp_json_file)
    update_results_json = [result_json_arr[-1]]
    expected_duration_in_hours = SHORT_TERM_DURATION_IN_HRS_MIN
    validate_reco_json(create_exp_json[0], update_results_json, list_reco_json[0], expected_duration_in_hours)

    # Verify namespace recommendation structure
    ns_reco = list_reco_json[0]["kubernetes_objects"][0]["namespaces"]["recommendations"]
    assert "data" in ns_reco, "Namespace recommendations should have data section"
    assert str(end_time) in ns_reco["data"], \
        f"Namespace recommendations should have entry for end_time {end_time}"

    cleanup_experiment(create_exp_json_file)


@pytest.mark.sanity
def test_lean_load_parity_accelerator(cluster_type):
    """
    I-3: Create a container experiment, post N intervals of GPU/accelerator
    results, call updateRecommendations. Validate that accelerator
    recommendations are generated correctly.
    """
    input_json_file = "../json_files/create_exp.json"
    result_json_file = "../json_files/update_results_accelerator.json"

    json_data = json.load(open(input_json_file))
    find = [
        json_data[0]['experiment_name'],
        json_data[0]['kubernetes_objects'][0]['name'],
        json_data[0]['kubernetes_objects'][0]['namespace'],
    ]

    form_kruize_url(cluster_type)
    create_exp_json_file, experiment_name, run_id = setup_experiment(
        cluster_type, input_json_file, find
    )

    gpu_name = "NVIDIA-A100-SXM4-40GB"
    num_results = 2
    result_json_arr, end_time = post_results(
        result_json_file, find, num_results, run_id=run_id, gpu_name=gpu_name
    )

    list_reco_json = generate_and_validate_recommendations(
        experiment_name, end_time, is_namespace=False
    )

    # Validate schema
    errorMsg = validate_list_reco_json(list_reco_json, list_reco_json_schema)
    assert errorMsg == "", f"Schema validation failed: {errorMsg}"

    # Validate values
    create_exp_json = read_json_data_from_file(create_exp_json_file)
    update_results_json = [result_json_arr[-1]]
    expected_duration_in_hours = SHORT_TERM_DURATION_IN_HRS_MIN
    validate_reco_json(create_exp_json[0], update_results_json, list_reco_json[0], expected_duration_in_hours)

    # Verify accelerator recommendations are present
    short_term = extract_reco_values(list_reco_json, end_time, is_namespace=False)
    cost_notifications = short_term["recommendation_engines"]["cost"]["notifications"]
    assert INFO_ACCELERATOR_RECOMMENDATIONS_AVAILABLE in cost_notifications, \
        "Accelerator recommendation notification should be present"

    cost_limits = short_term["recommendation_engines"]["cost"]["config"].get("limits", {})
    mig_keys = [key for key in cost_limits if re.fullmatch(MIG_PATTERN, key)]
    assert mig_keys, "Cost limits should contain MIG-based GPU recommendations"

    cleanup_experiment(create_exp_json_file)


@pytest.mark.extended
def test_lean_load_parity_mixed(cluster_type):
    """
    I-4: Create both a container and a namespace experiment. Post results for
    each. Verify the lean-load path handles mixed topology correctly --
    recommendations are generated for both experiment types independently.
    """
    # --- Container experiment ---
    container_input = "../json_files/create_exp.json"
    container_result = "../json_files/update_results.json"

    json_data = json.load(open(container_input))
    container_find = [
        json_data[0]['experiment_name'],
        json_data[0]['kubernetes_objects'][0]['name'],
        json_data[0]['kubernetes_objects'][0]['namespace'],
    ]

    form_kruize_url(cluster_type)

    container_exp_file, container_exp_name, container_run_id = setup_experiment(
        cluster_type, container_input, container_find
    )
    container_results, container_end_time = post_results(
        container_result, container_find, 2, run_id=container_run_id
    )

    # --- Namespace experiment ---
    namespace_input = "../json_files/create_exp_namespace.json"
    namespace_result = "../json_files/update_results_namespace.json"

    json_data = json.load(open(namespace_input))
    namespace_find = [
        json_data[0]['experiment_name'],
        json_data[0]['kubernetes_objects'][0]['namespaces']['namespace'],
    ]

    namespace_exp_file, namespace_exp_name, namespace_run_id = setup_experiment(
        cluster_type, namespace_input, namespace_find
    )
    namespace_results, namespace_end_time = post_results(
        namespace_result, namespace_find, 2, run_id=namespace_run_id
    )

    # --- Validate container recommendations ---
    container_reco_json = generate_and_validate_recommendations(
        container_exp_name, container_end_time, is_namespace=False
    )
    errorMsg = validate_list_reco_json(container_reco_json, list_reco_json_schema)
    assert errorMsg == "", f"Container schema validation failed: {errorMsg}"

    create_exp_json = read_json_data_from_file(container_exp_file)
    validate_reco_json(create_exp_json[0], [container_results[-1]],
                       container_reco_json[0], SHORT_TERM_DURATION_IN_HRS_MIN)

    # --- Validate namespace recommendations ---
    namespace_reco_json = generate_and_validate_recommendations(
        namespace_exp_name, namespace_end_time, is_namespace=True
    )
    errorMsg = validate_list_reco_json(namespace_reco_json, list_reco_namespace_json_local_monitoring_schema)
    assert errorMsg == "", f"Namespace schema validation failed: {errorMsg}"

    create_exp_json = read_json_data_from_file(namespace_exp_file)
    validate_reco_json(create_exp_json[0], [namespace_results[-1]],
                       namespace_reco_json[0], SHORT_TERM_DURATION_IN_HRS_MIN)

    # --- Cleanup ---
    cleanup_experiment(container_exp_file)
    cleanup_experiment(namespace_exp_file)


# ===========================================================================
# B. UPSERT Recommendation Persistence Tests (I-5, I-6)
#
# These tests verify that the native SQL UPSERT for recommendations works
# correctly: calling updateRecommendations twice doesn't create duplicates,
# and updated results are reflected in subsequent recommendations.
# ===========================================================================


@pytest.mark.sanity
def test_recommendation_upsert_idempotent(cluster_type):
    """
    I-5: Call updateRecommendations twice for the same experiment+timestamp.
    The second call should update (not duplicate). Verify listRecommendations
    returns exactly 1 entry for that timestamp.
    """
    input_json_file = "../json_files/create_exp.json"
    result_json_file = "../json_files/update_results.json"

    json_data = json.load(open(input_json_file))
    find = [
        json_data[0]['experiment_name'],
        json_data[0]['kubernetes_objects'][0]['name'],
        json_data[0]['kubernetes_objects'][0]['namespace'],
    ]

    form_kruize_url(cluster_type)
    create_exp_json_file, experiment_name, run_id = setup_experiment(
        cluster_type, input_json_file, find
    )

    num_results = 2
    result_json_arr, end_time = post_results(
        result_json_file, find, num_results, run_id=run_id
    )

    # --- First updateRecommendations call ---
    response1 = update_recommendations(experiment_name, None, end_time, timeout=HTTP_TIMEOUT)
    data1 = response1.json()
    assert response1.status_code == SUCCESS_STATUS_CODE
    assert data1[0]['experiment_name'] == experiment_name

    # --- Second updateRecommendations call (same experiment, same end_time) ---
    response2 = update_recommendations(experiment_name, None, end_time, timeout=HTTP_TIMEOUT)
    data2 = response2.json()
    assert response2.status_code == SUCCESS_STATUS_CODE
    assert data2[0]['experiment_name'] == experiment_name

    # --- Verify both calls produce identical recommendation values ---
    reco1 = data1[0]['kubernetes_objects'][0]['containers'][0]['recommendations']
    reco2 = data2[0]['kubernetes_objects'][0]['containers'][0]['recommendations']
    assert reco1 == reco2, \
        "Duplicate updateRecommendations calls should return identical values"

    # --- Verify no duplicates via listRecommendations ---
    response = list_recommendations(experiment_name, rm=True, timeout=HTTP_TIMEOUT)
    assert response.status_code == SUCCESS_200_STATUS_CODE
    list_reco_json = response.json()

    assert len(list_reco_json) == 1, \
        f"Expected exactly 1 experiment entry, got {len(list_reco_json)}"

    reco_section = list_reco_json[0]["kubernetes_objects"][0]["containers"][0]["recommendations"]
    data_section = reco_section["data"]

    timestamp_entries = [ts for ts in data_section.keys() if ts == str(end_time)]
    assert len(timestamp_entries) == 1, \
        f"Expected exactly 1 recommendation entry for timestamp {end_time}, got {len(timestamp_entries)}"

    # --- Verify the second updateRecommendations call also returns consistent
    #     data via listRecommendations (guards against silent duplicates) ---
    response_check = list_recommendations(experiment_name, rm=True, timeout=HTTP_TIMEOUT)
    assert response_check.status_code == SUCCESS_200_STATUS_CODE
    list_reco_check = response_check.json()
    reco_check = list_reco_check[0]["kubernetes_objects"][0]["containers"][0]["recommendations"]
    assert reco_check == reco2, \
        "listRecommendations should return the same values as the last updateRecommendations call"

    # Validate schema and values
    errorMsg = validate_list_reco_json(list_reco_json, list_reco_json_schema)
    assert errorMsg == "", f"Schema validation failed: {errorMsg}"

    create_exp_json = read_json_data_from_file(create_exp_json_file)
    validate_reco_json(create_exp_json[0], [result_json_arr[-1]],
                       list_reco_json[0], SHORT_TERM_DURATION_IN_HRS_MIN)

    cleanup_experiment(create_exp_json_file)


@pytest.mark.extended
def test_recommendation_upsert_updated_results(cluster_type):
    """
    I-6: Post initial results -> updateRecommendations -> post more results
    for a new interval -> updateRecommendations again. Verify the
    recommendation reflects the latest results, not stale data.
    """
    input_json_file = "../json_files/create_exp.json"
    result_json_file = "../json_files/update_results.json"

    json_data = json.load(open(input_json_file))
    find = [
        json_data[0]['experiment_name'],
        json_data[0]['kubernetes_objects'][0]['name'],
        json_data[0]['kubernetes_objects'][0]['namespace'],
    ]

    form_kruize_url(cluster_type)
    create_exp_json_file, experiment_name, run_id = setup_experiment(
        cluster_type, input_json_file, find
    )

    # --- Phase 1: Post 2 results, get initial recommendations ---
    result_json_arr, end_time_1 = post_results(
        result_json_file, find, 2, run_id=run_id
    )

    reco_json_1 = generate_and_validate_recommendations(
        experiment_name, end_time_1, is_namespace=False
    )

    short_term_1 = extract_reco_values(reco_json_1, end_time_1, is_namespace=False)

    # --- Phase 2: Post 2 more results with higher CPU usage values ---
    phase2_cpu_avg = 0.85
    phase2_cpu_max = 1.80
    phase2_cpu_min = 0.60
    update_results_json_file = make_temp_json('update_results_phase2_')

    for j in range(2):
        generate_json(find, result_json_file, update_results_json_file, run_id, update_timestamps=True)
        result_json = read_json_data_from_file(update_results_json_file)

        start_time = end_time_1 if j == 0 else end_time_2
        result_json[0]['interval_start_time'] = start_time
        end_time_2 = increment_timestamp_by_given_mins(start_time, 15)
        result_json[0]['interval_end_time'] = end_time_2

        for kub_obj in result_json[0]["kubernetes_objects"]:
            for container in kub_obj.get("containers", []):
                for metric in container.get("metrics", []):
                    if metric["name"] == "cpuUsage":
                        metric["results"]["aggregation_info"]["avg"] = phase2_cpu_avg
                        metric["results"]["aggregation_info"]["max"] = phase2_cpu_max
                        metric["results"]["aggregation_info"]["min"] = phase2_cpu_min

        write_json_data_to_file(update_results_json_file, result_json)
        result_json_arr.append(result_json[0])
        response = update_results(update_results_json_file, False, timeout=HTTP_TIMEOUT)

        data = response.json()
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

    # --- Update recommendations with new end_time ---
    reco_json_2 = generate_and_validate_recommendations(
        experiment_name, end_time_2, is_namespace=False
    )

    # Verify that the new recommendation timestamp exists
    reco_section = reco_json_2[0]["kubernetes_objects"][0]["containers"][0]["recommendations"]
    data_section = reco_section["data"]

    assert str(end_time_2) in data_section, \
        f"Recommendations should have entry for the latest end_time {end_time_2}"

    short_term_2 = extract_reco_values(reco_json_2, end_time_2, is_namespace=False)

    # --- Verify recommendations changed: the higher CPU usage in phase 2
    #     should produce different CPU request/limit recommendations ---
    cost_config_1 = short_term_1["recommendation_engines"]["cost"]["config"]
    cost_config_2 = short_term_2["recommendation_engines"]["cost"]["config"]
    assert cost_config_1 != cost_config_2, \
        "Recommendations should differ after posting results with different CPU usage " \
        f"(phase1 avg={result_json_arr[1]['kubernetes_objects'][0]['containers'][0]['metrics'][2]['results']['aggregation_info']['avg']}, " \
        f"phase2 avg={phase2_cpu_avg})"

    # Validate schema
    errorMsg = validate_list_reco_json(reco_json_2, list_reco_json_schema)
    assert errorMsg == "", f"Schema validation failed: {errorMsg}"

    # Validate values with latest results (4 intervals x 15 min = 1 hour of data)
    create_exp_json = read_json_data_from_file(create_exp_json_file)
    validate_reco_json(create_exp_json[0], [result_json_arr[-1]],
                       reco_json_2[0], 1.0)

    cleanup_experiment(create_exp_json_file)
