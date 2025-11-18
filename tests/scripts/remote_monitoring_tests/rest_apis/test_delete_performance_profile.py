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
mandatory_fields = [
    ("name", ERROR_STATUS_CODE, ERROR_STATUS),
    ("profile_version", ERROR_STATUS_CODE, ERROR_STATUS),
    ("sloInfo", ERROR_STATUS_CODE, ERROR_STATUS),
    ("direction", ERROR_STATUS_CODE, ERROR_STATUS),
    ("objective_function", ERROR_STATUS_CODE, ERROR_STATUS),
    ("function_type", ERROR_STATUS_CODE, ERROR_STATUS),
    ("function_variables", ERROR_STATUS_CODE, ERROR_STATUS),
    ("metric_name", ERROR_STATUS_CODE, ERROR_STATUS),
    ("datasource", ERROR_STATUS_CODE, ERROR_STATUS),
    ("value_type", ERROR_STATUS_CODE, ERROR_STATUS),
    ("aggregation_functions", ERROR_STATUS_CODE, ERROR_STATUS),
    ("function", ERROR_STATUS_CODE, ERROR_STATUS),
    ("query", ERROR_STATUS_CODE, ERROR_STATUS)
]

@pytest.mark.perf_profile
def test_delete_performance_profile(cluster_type):
    """
    Test Description: This test validates the response status code of deletePerformanceProfile API by passing a
    valid input for the json
    """
    # Form the kruize url
    form_kruize_url(cluster_type)
    perf_profile_json_file = perf_profile_dir / 'resource_optimization_openshift.json'
    json_file_modified = "/tmp/create_performance_profile.json"
    # Rename the performance profile name to create a new performance profile
    with open(perf_profile_json_file, "r") as f:
        data = json.load(f)

    # Update only the name
    data["name"] = PERF_PROFILE_NAME

    # Write out the modified JSON to a new file
    with open(json_file_modified, "w") as f:
        json.dump(data, f, indent=2)

    # Create the performance profile
    response = create_performance_profile(json_file_modified)
    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert CREATE_PERF_PROFILE_SUCCESS_MSG % PERF_PROFILE_NAME in data['message']

    # Delete the profile
    response = delete_performance_profile(json_file_modified)
    print("delete API status code = ", response.status_code)
    data = response.json()
    print("delete API status message  = ", data["message"])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == DELETE_PERF_PROFILE_SUCCESS_MSG % PERF_PROFILE_NAME


@pytest.mark.perf_profile
@pytest.mark.parametrize(
    "invalid_name, expected_message",
    [
        ("__missing__", DELETE_PERF_PROFILE_MISSING_NAME_ERROR),         # missing
        ("", DELETE_PERF_PROFILE_MISSING_NAME_ERROR),                    # blank
        (None, DELETE_PERF_PROFILE_MISSING_NAME_ERROR),                  # null
        ("__non_existent_profile__", DELETE_PERF_PROFILE_NON_EXISTENT_NAME_ERROR), # name not present
    ],
)
def test_delete_performance_profiles_negative_cases(cluster_type, invalid_name, expected_message):
    """
    Tests deletePerformanceProfile API against all invalid or edge-case name values.
    """

    # Form the kruize URL
    form_kruize_url(cluster_type)

    perf_profile_json_file = perf_profile_dir / "resource_optimization_openshift.json"
    tmp_json = Path("/tmp/create_performance_profile.json")

    # Load original JSON
    with open(perf_profile_json_file, "r") as f:
        data = json.load(f)

    # ----- Case: Name missing entirely -----
    if invalid_name == "__missing__":
        data.pop("name", None)
    else:
        data["name"] = invalid_name

    # Save modified JSON
    with open(tmp_json, "w") as f:
        json.dump(data, f, indent=2)

    # Call delete API
    response = delete_performance_profile(tmp_json)
    json_response = response.json()

    print("status:", response.status_code)
    print("resp:", json_response)

    assert response.status_code == ERROR_STATUS_CODE
    assert json_response["status"] == ERROR_STATUS
    assert json_response["message"] == expected_message


@pytest.mark.perf_profile
def test_delete_performance_profile_when_associated_with_experiment(cluster_type):
    """
    Test: Deleting a performance profile must fail if it is already associated
    with one or more experiments.
    """

    form_kruize_url(cluster_type)
    profile_name = "perf-profile-associated-test"

    # Load base JSON & override the name
    perf_profile_file = perf_profile_dir / "resource_optimization_openshift.json"
    tmp_profile_file = "/tmp/perf_profile_associated.json"
    with open(perf_profile_file, "r") as f:
        data = json.load(f)
    data["name"] = profile_name

    with open(tmp_profile_file, "w") as f:
        json.dump(data, f, indent=2)

    # Create the performance profile
    resp = create_performance_profile(tmp_profile_file)
    assert resp.status_code in (200, 201)

    # Create an experiment using this profile
    experiment_name = "exp-using-" + profile_name
    input_json_file = "../json_files/create_exp.json"
    tmp_exp_file = "/tmp/experiment_associated.json"

    with open(input_json_file, "r") as f:
        data = json.load(f)
    data["performance_profile"] = profile_name

    with open(tmp_exp_file, "w") as f:
        json.dump(data, f, indent=2)

    response = create_experiment(tmp_exp_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG

    # Verify via listExperiments that the experiment exists
    list_resp = list_experiments()
    assert list_resp.status_code == 200

    experiments = list_resp.json().get("experiments", [])
    associated_count = sum(
        1 for e in experiments if e.get("performance_profile") == profile_name
    )

    assert associated_count >= 1, "Experiment should be associated with the profile"

    # Attempt to delete the performance profile
    response = delete_performance_profile(tmp_profile_file)
    print("delete API status code = ", response.status_code)
    data = response.json()
    print("delete API status message  = ", data["message"])

    assert response.status_code == ERROR_STATUS_CODE
    assert data["status"] == ERROR_STATUS

    expected_msg = DELETE_PERF_PROFILE_EXPERIMENT_ASSOCIATION_ERROR.format(
        profile_name, associated_count, "s" if associated_count > 1 else ""
    )
    assert data["message"] == expected_msg
