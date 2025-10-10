"""
Copyright (c) 2025 IBM Corporation and others.

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
def test_update_performance_profile(cluster_type):
    """
    Test Description: This test validates the response status code of createPerformanceProfile API by passing a
    valid input for the json
    """
    # Form the kruize url
    if cluster_type == "minikube":
        namespace = "monitoring"
    else:
        namespace = "openshift-tuning"

    form_kruize_url(cluster_type)
    # Delete the kruize pod
    delete_kruize_db_pod(namespace)

    # Check if the kruize pod is running
    pod_name = get_kruize_db_pod(namespace)
    result = check_pod_running(namespace, pod_name)

    if result == False:
        print("Restarting kruize failed!")
        failed = 1
        sys.exit(failed)

    # Create the performance profile
    perf_profile_dir = get_metric_profile_dir()
    perf_profile_json_file = "../json_files/resource_optimization_openshift_v1.json"

    response = create_performance_profile(perf_profile_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert CREATE_PERF_PROFILE_SUCCESS_MSG in data['message']

    # Update the performance profile
    perf_profile_json_file = perf_profile_dir / 'resource_optimization_openshift.json'
    response = update_performance_profile(perf_profile_json_file)

    perf_profile_name = perf_profile_json_file["name"]
    perf_profile_version_v2 = perf_profile_json_file["profile_version"]

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == UPDATE_PERF_PROFILE_SUCCESS_MSG % (perf_profile_name, perf_profile_version_v2)

    # Validate using listPerformanceProfile API
    response = list_performance_profiles()
    perf_profile_version = response.json()[0]["profile_version"]
    assert perf_profile_version == perf_profile_version_v2



@pytest.mark.perf_profile
def test_update_performance_profile_with_missing_profile(cluster_type):
    """
    Test Description: This test validates the response message of update performance profile API by passing the missing profile name
    """
    # Form the kruize url
    if cluster_type == "minikube":
        namespace = "monitoring"
    else:
        namespace = "openshift-tuning"

    form_kruize_url(cluster_type)
    # Delete the kruize pod
    delete_kruize_db_pod(namespace)

    # Check if the kruize pod is running
    pod_name = get_kruize_db_pod(namespace)
    result = check_pod_running(namespace, pod_name)

    if result == False:
        print("Restarting kruize failed!")
        failed = 1
        sys.exit(failed)

    # Update the performance profile
    perf_profile_json_file = "../json_files/resource_optimization_openshift_v1.json"
    response = update_performance_profile(perf_profile_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == ERROR_404_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == UPDATE_PERF_PROFILE_MISSING_PROFILE_ERROR_MSG


@pytest.mark.perf_profile
def test_update_performance_profile_with_unsupported_version(cluster_type):
    """
    Test Description: This test validates the response message of update performance profile API by specifying the
    incorrect version
    """
    # Form the kruize url
    if cluster_type == "minikube":
        namespace = "monitoring"
    else:
        namespace = "openshift-tuning"

    form_kruize_url(cluster_type)
    # Delete the kruize pod
    delete_kruize_db_pod(namespace)

    # Check if the kruize pod is running
    pod_name = get_kruize_db_pod(namespace)
    result = check_pod_running(namespace, pod_name)

    if result == False:
        print("Restarting kruize failed!")
        failed = 1
        sys.exit(failed)

    # Update the performance profile
    perf_profile_json_file = "../json_files/resource_optimization_openshift_v1.json"
    response = update_performance_profile(perf_profile_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == ERROR_404_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == UPDATE_PERF_PROFILE_UNSUPPORTED_VERSION_ERROR_MSG


@pytest.mark.perf_profile
def test_update_performance_profile_with_missing_version(cluster_type):
    """
    Test Description: This test validates the response message of update performance profile API by skipping the version
    """
    # Form the kruize url
    if cluster_type == "minikube":
        namespace = "monitoring"
    else:
        namespace = "openshift-tuning"

    form_kruize_url(cluster_type)
    # Delete the kruize pod
    delete_kruize_db_pod(namespace)

    # Check if the kruize pod is running
    pod_name = get_kruize_db_pod(namespace)
    result = check_pod_running(namespace, pod_name)

    if result == False:
        print("Restarting kruize failed!")
        failed = 1
        sys.exit(failed)

    # Get the performance profile directory and read the file
    perf_profile_dir = get_metric_profile_dir()
    perf_profile_json_file = perf_profile_dir / 'resource_optimization_openshift.json'

    # manually remove the version from the JSON and re-build the file
    data = json.loads(perf_profile_json_file.read_text())
    data.pop("profile_version", None)
    perf_profile_json_file.write_text(json.dumps(data, indent=2))

    # call the updatePerformanceProfile with the modified file
    response = update_performance_profile(perf_profile_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert UPDATE_PERF_PROFILE_MISSING_VERSION_ERROR_MSG in data['message']
