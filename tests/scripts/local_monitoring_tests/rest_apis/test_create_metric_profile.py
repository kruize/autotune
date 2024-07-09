"""
Copyright (c) 2024, 2024 Red Hat, IBM Corporation and others.

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
import json
import sys

sys.path.append("../../")

from helpers.fixtures import *
from helpers.kruize import *
from helpers.utils import *

@pytest.mark.sanity
def test_create_metric_profile(cluster_type):
    """
    Test Description: This test validates the response status code of createMetricProfile API by passing a
    valid input for the json
    """
    input_json_file = "../json_files/resource_optimization_openshift_metric_profile.json"
    form_kruize_url(cluster_type)

    response = delete_metric_profile(input_json_file)
    print("delete metric profile = ", response.status_code)

    # Create metric profile using the specified json
    response = create_metric_profile(input_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS

    json_file = open(input_json_file, "r")
    input_json = json.loads(json_file.read())
    metric_profile_name = input_json['metadata']['name']
    assert data['message'] == CREATE_METRIC_PROFILE_SUCCESS_MSG % metric_profile_name

    response = delete_metric_profile(input_json_file)
    print("delete metric profile = ", response.status_code)


@pytest.mark.sanity
def test_create_duplicate_metric_profile(cluster_type):
    """
    Test Description: This test validates the response status code of createMetricProfile API by specifying the
    same metric profile name
    """
    input_json_file = "../json_files/resource_optimization_openshift_metric_profile.json"
    json_data = json.load(open(input_json_file))

    metric_profile_name = json_data['metadata']['name']
    print("name = ", metric_profile_name)

    form_kruize_url(cluster_type)

    response = delete_metric_profile(input_json_file)
    print("delete metric profile = ", response.status_code)

    # Create metric profile using the specified json
    response = create_metric_profile(input_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_METRIC_PROFILE_SUCCESS_MSG % metric_profile_name

    # Create metric profile using the specified json
    response = create_metric_profile(input_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == ERROR_409_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == METRIC_PROFILE_EXISTS_MSG % metric_profile_name

    response = delete_metric_profile(input_json_file)
    print("delete metric profile = ", response.status_code)