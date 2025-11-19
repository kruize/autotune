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

import sys

import pytest

sys.path.append("../../")

from helpers.utils import *

perf_profile_dir = get_metric_profile_dir()

@pytest.mark.perf_profile
def test_list_performance_profiles_empty(cluster_type):
    """
    Validates that listPerformanceProfiles returns an empty list when no profiles are present.
    """

    # Form the kruize url
    form_kruize_url(cluster_type)
    perf_profile_json_file = perf_profile_dir / 'resource_optimization_openshift.json'
    # Delete any existing profile
    response = delete_performance_profile(perf_profile_json_file)
    print("delete API status code = ", response.status_code)
    data = response.json()
    print("delete API status message  = ", data["message"])

    # get the list profiles response
    response = list_performance_profiles()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == LIST_PERF_PROFILE_NO_PROFILES_MSG


@pytest.mark.perf_profile
def test_list_performance_profiles_single(cluster_type):
    """
    Validates listPerformanceProfiles when exactly one profile exists.
    """

    form_kruize_url(cluster_type)

    # Create one profile
    profile_name = "single-profile-test"
    tmp_json = "/tmp/perf_profile_single.json"

    base_file = perf_profile_dir / "resource_optimization_openshift.json"
    with open(base_file) as f:
        data = json.load(f)
    data["name"] = profile_name

    with open(tmp_json, "w") as f:
        json.dump(data, f, indent=2)

    # Create performance profile using the specified json
    response = create_performance_profile(tmp_json)

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert CREATE_PERF_PROFILE_SUCCESS_MSG % profile_name in data['message']


    # Call listProfiles
    response = list_performance_profiles()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS

    profiles = response.json()
    print("List response:", profiles)

    assert isinstance(profiles, list)
    assert len(profiles) >= 1

    # Verify the profile exists
    assert any(p["name"] == profile_name for p in profiles)


@pytest.mark.perf_profile
def test_list_performance_profiles_multiple(cluster_type):
    """
    Validates that listPerformanceProfiles returns all profiles when multiple exist.
    """

    form_kruize_url(cluster_type)

    names = ["profile-A", "profile-B", "profile-C"]

    base_file = perf_profile_dir / "resource_optimization_openshift.json"

    # Create 3 profiles
    for name in names:
        tmp_file = f"/tmp/{name}.json"
        with open(base_file) as f:
            data = json.load(f)
        data["name"] = name
        with open(tmp_file, "w") as f:
            json.dump(data, f, indent=2)

        response = create_performance_profile(tmp_file)

        data = response.json()
        print(data['message'])
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert CREATE_PERF_PROFILE_SUCCESS_MSG % name in data['message']

    # List all profiles
    response = list_performance_profiles()
    profiles = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE
    assert profiles['status'] == SUCCESS_STATUS

    print("Profiles returned:", profiles)

    assert isinstance(profiles, list)
    assert len(profiles) >= 3

    # Verify all created profiles are present
    returned_names = [p["name"] for p in profiles]
    for n in names:
        assert n in returned_names
