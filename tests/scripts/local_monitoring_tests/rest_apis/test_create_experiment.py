"""
Copyright (c) 2024 Red Hat, IBM Corporation and others.

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
from helpers.utils import *
from jinja2 import Environment, FileSystemLoader

mandatory_fields = [
    ("version", ERROR_STATUS_CODE, ERROR_STATUS),
    ("cluster_name", ERROR_STATUS_CODE, ERROR_STATUS),
    ("experiment_name", ERROR_STATUS_CODE, ERROR_STATUS),
    ("mode", ERROR_STATUS_CODE, ERROR_STATUS),
    ("target_cluster", ERROR_STATUS_CODE, ERROR_STATUS),
    ("kubernetes_objects", ERROR_STATUS_CODE, ERROR_STATUS),
    ("type", ERROR_STATUS_CODE, ERROR_STATUS),
    ("kubernetes_objects_name", ERROR_STATUS_CODE, ERROR_STATUS),
    ("namespace", ERROR_STATUS_CODE, ERROR_STATUS),
    ("containers", ERROR_STATUS_CODE, ERROR_STATUS),
    ("container_image_name", ERROR_STATUS_CODE, ERROR_STATUS),
    ("container_name", ERROR_STATUS_CODE, ERROR_STATUS),
    ("selector", SUCCESS_STATUS_CODE, SUCCESS_STATUS),
    ("namespace", ERROR_STATUS_CODE, ERROR_STATUS),
    ("performance_profile", ERROR_STATUS_CODE, ERROR_STATUS),
    ("slo", SUCCESS_STATUS_CODE, SUCCESS_STATUS),
    ("recommendation_settings", ERROR_STATUS_CODE, ERROR_STATUS),
    ("trial_settings", ERROR_STATUS_CODE, ERROR_STATUS),
    ("kubernetes_objects_name_selector", ERROR_STATUS_CODE, ERROR_STATUS),
    ("performance_profile_slo", ERROR_STATUS_CODE, ERROR_STATUS)
]

csvfile = "/tmp/create_exp_test_data.csv"

@pytest.mark.sanity
def test_create_namespace_exp_with_namespace_type(cluster_type):
    """
    Test Description: This test validates the response status code of createExperiment API
    for namespace experiment by passing a valid input for the json
    """
    input_json_file = "../json_files/create_namespace_exp.json"
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

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

@pytest.mark.negative
def test_create_namespace_exp_without_type(cluster_type):
    """
    Test Description: This test validates the response status code of createExperiment API
    for namespace experiment by passing a invalid input for the json without specifying the experiment type
    """
    input_json_file = "../json_files/create_namespace_exp_without_type.json"
    form_kruize_url(cluster_type)

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == ERROR_500_STATUS_CODE
    assert data['status'] == ERROR_STATUS

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)


@pytest.mark.sanity
def test_create_container_exp_without_type(cluster_type):
    """
    Test Description: This test validates the response status code of createExperiment API
    for containers experiment by passing a valid input for the json without specifying the experiment type
    """
    input_json_file = "../json_files/create_tfb_exp.json"
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

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

@pytest.mark.sanity
def test_create_container_exp_with_container_type(cluster_type):
    """
    Test Description: This test validates the response status code of createExperiment API
    for containers experiment by passing a valid input for the json with specifying the experiment type
    """
    input_json_file = "../json_files/create_tfb_exp_container_type.json"
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

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)


@pytest.mark.negative
def test_create_exp_with_container_namespace_both_without_type(cluster_type):
    """
    Test Description: This test validates the response status code of createExperiment API
    if both container and namespace is passed and experiment type is not passed
    """
    input_json_file = "../json_files/create_exp_namespace_container_both.json"
    form_kruize_url(cluster_type)

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == ERROR_500_STATUS_CODE
    assert data['status'] == ERROR_STATUS

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

@pytest.mark.negative
def test_create_exp_with_container_namespace_both_container_type(cluster_type):
    """
    Test Description: This test validates the response status code of createExperiment API
    if both container and namespace is passed and experiment type is container
    """
    input_json_file = "../json_files/create_exp_namespace_container_both_container_type.json"
    form_kruize_url(cluster_type)

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == ERROR_500_STATUS_CODE
    assert data['status'] == ERROR_STATUS

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

@pytest.mark.negative
def test_create_exp_with_container_namespace_both_namespace_type(cluster_type):
    """
    Test Description: This test validates the response status code of createExperiment API
    if both container and namespace is passed and experiment type is namespace
    """
    input_json_file = "../json_files/create_exp_namespace_container_both_namespace_type.json"
    form_kruize_url(cluster_type)

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == ERROR_500_STATUS_CODE
    assert data['status'] == ERROR_STATUS

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

@pytest.mark.negative
def test_create_namespace_exp_with_containers(cluster_type):
    """
    Test Description: This test validates the response status code of createExperiment API
    if containers array is passed and experiment type is namespace
    """
    input_json_file = "../json_files/create_namespace_exp_with_containers.json"
    form_kruize_url(cluster_type)

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == ERROR_500_STATUS_CODE
    assert data['status'] == ERROR_STATUS

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

@pytest.mark.negative
def test_create_conatiner_exp_with_namespace(cluster_type):
    """
    Test Description: This test validates the response status code of createExperiment API
    if namespaces object is passed and experiment type is container
    """
    input_json_file = "../json_files/create_container_exp_with_namespace.json"
    form_kruize_url(cluster_type)

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == ERROR_500_STATUS_CODE
    assert data['status'] == ERROR_STATUS

    response = delete_experiment(input_json_file)
    print("delete exp = ", response.status_code)

