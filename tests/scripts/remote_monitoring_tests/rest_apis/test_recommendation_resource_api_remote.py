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

import sys

import pytest
import requests
from jinja2 import Environment, FileSystemLoader

from helpers import v1_list_reco_json_local_monitoring_schema
from helpers.fixtures import *
from helpers.list_reco_json_validate import validate_list_reco_json

sys.path.append("../../")
from helpers.utils import *


@pytest.mark.recommendation
def test_get_recommendations_v1_remote_e2e_workflow(cluster_type):
    """
    Test POST /kruize/api/v1/recommendations API with new schema for container and namespace experiments
    follows below steps:
    - Create container and namespace experiments
    - Update results for container and namespace experiments
    - call recommendations API and validates response structure with the new schema
    - validates the presence of replicas field
    - validates nested resources structure
    - validates Pod count metrics
    """
    input_json_file_container = "../json_files/create_exp.json"
    input_json_file_namespace = "../json_files/create_exp_namespace.json"
    result_json_file_container = "../json_files/multiple_results_single_exp.json"
    result_json_file_namespace = "../json_files/multiple_results_single_exp_namespace.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file_container, rm=True)
    print("delete container exp = ", response.status_code)
    response = delete_experiment(input_json_file_namespace, rm=True)
    print("delete namespace exp = ", response.status_code)

    try:
        # Create container experiment
        response = create_experiment(input_json_file_container)
        data = response.json()
        print("container exp response = ", data['message'])
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == CREATE_EXP_SUCCESS_MSG
        # Create namespace experiment
        response = create_experiment(input_json_file_namespace)
        data = response.json()
        print("namespace exp response = ", data['message'])
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == CREATE_EXP_SUCCESS_MSG

        # Update results for the container experiment
        response = update_results(result_json_file_container, False)

        data = response.json()
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

        # Get the experiment name
        json_data = json.load(open(input_json_file_container))
        container_experiment_name = json_data[0]['experiment_name']
        end_time = "2023-04-14T23:59:20.982Z"

        # Generate recommendations for container using POST - this returns the recommendations in v1.0 format
        print("container_experiment_name = ", container_experiment_name)
        response = generate_recommendations_v1(container_experiment_name, interval_end_time=end_time)
        data = response.json()
        print("recommendations response = ", data)
        assert response.status_code == SUCCESS_200_STATUS_CODE
        assert data[0]['experiment_name'] == container_experiment_name
        assert data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications'][
                   NOTIFICATION_CODE_FOR_RECOMMENDATIONS_AVAILABLE]['message'] == RECOMMENDATIONS_AVAILABLE

        response = list_recommendations_v1(container_experiment_name, rm=True)
        list_reco_json_container = response.json()
        assert response.status_code == SUCCESS_200_STATUS_CODE

        # Validate the json against the json schema
        error_msg = validate_list_reco_json(list_reco_json_container, v1_list_reco_json_local_monitoring_schema.v1_list_reco_json_local_monitoring_schema)
        assert error_msg == ""

        # Validate the json values
        create_exp_json = read_json_data_from_file(input_json_file_container)
        expected_duration_in_hours = SHORT_TERM_DURATION_IN_HRS_MAX

        update_results_json = []
        result_json_arr = read_json_data_from_file(result_json_file_container)
        update_results_json.append(result_json_arr[len(result_json_arr) - 1])
        validate_reco_json(create_exp_json[0], update_results_json, list_reco_json_container[0],
                           expected_duration_in_hours, v1=True)


        # Update results for the namespace experiment
        response = update_results(result_json_file_namespace, False)
        data = response.json()
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == UPDATE_RESULTS_SUCCESS_MSG

        end_time = "2022-01-24T19:25:43.511Z"
        json_data = json.load(open(input_json_file_namespace))
        namespace_experiment_name = json_data[0]['experiment_name']
        # Generate recommendations for namespace using POST - this returns the recommendations in v1.0 format
        response = generate_recommendations_v1(namespace_experiment_name, interval_end_time=end_time)
        data = response.json()
        print("recommendations response = ", data)
        assert response.status_code == SUCCESS_200_STATUS_CODE
        assert data[0]['experiment_name'] == namespace_experiment_name
        assert data[0]['kubernetes_objects'][0]['namespaces']['recommendations']['notifications'][
                   INFO_RECOMMENDATIONS_AVAILABLE_CODE][
                   'message'] == RECOMMENDATIONS_AVAILABLE

        response = list_recommendations_v1(namespace_experiment_name, rm=True)
        list_reco_json_namespace = response.json()
        assert response.status_code == SUCCESS_200_STATUS_CODE

        # Validate the json against the json schema
        error_msg = validate_list_reco_json(list_reco_json_namespace, v1_list_reco_json_local_monitoring_schema.v1_list_reco_namespace_json_local_monitoring_schema)
        assert error_msg == ""

        # Validate the json values
        create_exp_json = read_json_data_from_file(input_json_file_namespace)
        expected_duration_in_hours = SHORT_TERM_DURATION_IN_HRS_MAX

        result_json_arr = read_json_data_from_file(result_json_file_namespace)
        update_results_json = [result_json_arr[len(result_json_arr) - 1]]
        validate_reco_json(create_exp_json[0], update_results_json, list_reco_json_namespace[0],
                           expected_duration_in_hours, v1=True)
    finally:
        # Cleanup: Delete the experiments
        # response = delete_experiment(input_json_file_container, rm=True)
        # print("delete container exp = ", response.status_code)
        # response = delete_experiment(input_json_file_namespace, rm=True)
        print("delete namespace exp = ", response.status_code)


@pytest.mark.recommendation
def test_get_recommendations_v1_invalid_experiment(cluster_type):
    """
    Test GET /kruize/api/v1/recommendations API with non-existing experiment
    Expected: 400 Bad Request with proper error message
    """
    form_kruize_url(cluster_type)

    # Try to get recommendations for non-existing experiment
    response = list_recommendations_v1("non_existing_experiment_12345")
    
    # Validate error response
    validate_error_response(response, expected_status_code=ERROR_STATUS_CODE)


@pytest.mark.recommendation
def test_get_recommendations_v1_invalid_timestamp(cluster_type):
    """
    Test GET /kruize/api/v1/recommendations API with invalid timestamp format
    Expected: 400 Bad Request with proper error message
    """
    input_json_file = "../json_files/create_exp.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file, rm=True)
    print("delete exp = ", response.status_code)

    try:
        # Create experiment
        response = create_experiment(input_json_file)
        data = response.json()
        print(data['message'])

        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == CREATE_EXP_SUCCESS_MSG
        # Get experiment name
        json_data = json.load(open(input_json_file))
        experiment_name = json_data[0]['experiment_name']

        # Try to get recommendations with invalid timestamp
        response = list_recommendations_v1(experiment_name, interval_end_time="invalid-timestamp")

        # Validate error response
        validate_error_response(response, expected_status_code=ERROR_STATUS_CODE)
    finally:
        # Cleanup: Delete the experiment
        response = delete_experiment(input_json_file, rm=True)
        print("delete exp = ", response.status_code)


@pytest.mark.recommendation
def test_post_recommendations_v1_without_experiment_name(cluster_type):
    """
    Test POST /kruize/api/v1/recommendations API without experiment_name
    Expected: 400 Bad Request with proper error message
    """
    form_kruize_url(cluster_type)

    # Try to generate recommendations without experiment name
    url = get_kruize_url()
    api_url = f"{url}{RECOMMENDATIONS_API_V1}"
    response = requests.post(api_url, json={})
    
    # Validate error response with status message
    validate_error_response(response, expected_status_code=ERROR_STATUS_CODE)


@pytest.mark.recommendation
def test_post_recommendations_v1_without_interval_end_time(cluster_type):
    """
    Test POST /kruize/api/v1/recommendations API without interval_end_time for remote target
    Expected: 400 Bad Request with proper error message
    """
    input_json_file = "../json_files/create_exp.json"

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file, rm=True)
    print("delete exp = ", response.status_code)

    # Create experiment
    response = create_experiment(input_json_file)
    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG
    # Get experiment name
    json_data = json.load(open(input_json_file))
    experiment_name = json_data[0]['experiment_name']

    # Try to generate recommendations without interval_end_time
    url = get_kruize_url()
    api_url = f"{url}{RECOMMENDATIONS_API_V1}?experiment_name={experiment_name}"
    response = requests.post(api_url, json={})
    
    # Validate error response with status message
    validate_error_response(response, expected_status_code=ERROR_STATUS_CODE)
