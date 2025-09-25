"""
Copyright (c) 2022, 2025 IBM Corporation and others.

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
from helpers.utils import *


@pytest.mark.perf_profile
def test_create_performance_profile(cluster_type):
    """
    Test Description: This test validates the response status code of createPerformanceProfile API by passing a
    valid input for the json
    """
    form_kruize_url(cluster_type)
    # Create the performance profile
    perf_profile_dir = get_metric_profile_dir()
    perf_profile_json_file = perf_profile_dir / 'resource_optimization_openshift.json'

    response = create_performance_profile(perf_profile_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert CREATE_PERF_PROFILE_SUCCESS_MSG in data['message']


@pytest.mark.perf_profile
def test_create_duplicate_performance_profile(cluster_type):
    """
    Test Description: This test validates the response status code of performance profile API by specifying the
    same profile name
    """
    form_kruize_url(cluster_type)
    # Create the performance profile
    perf_profile_dir = get_metric_profile_dir()
    perf_profile_json_file = perf_profile_dir / 'resource_optimization_openshift.json'
    response = create_performance_profile(perf_profile_json_file)

    json_data = json.load(open(perf_profile_json_file))
    perf_profile_name = json_data['name']

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert CREATE_PERF_PROFILE_SUCCESS_MSG in data['message']

    # Create performance profile again using the same json
    response = create_performance_profile(perf_profile_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == ERROR_409_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == CREATE_PERF_PROFILE_ERROR_MSG + perf_profile_name


@pytest.mark.perf_profile
def test_create_performance_profile_with_deprecated_version(cluster_type):
    """
    Test Description: This test validates the response message of performance profile API by specifying the
    deprecated version
    """
    form_kruize_url(cluster_type)
    # Create the performance profile
    perf_profile_json_file = "../json_files/resource_optimization_openshift_v1.json"
    response = create_performance_profile(perf_profile_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == DEPRECATED_PERF_PROFILE_VERSION
