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
import pytest
import sys

sys.path.append("../../")

from helpers.fixtures import *
from helpers.kruize import *
from helpers.utils import *

# Tests for recommendation_settings.resource_scope_settings in the createExperiment API.
# No workload data is needed, only the prerequisites of the existing create experiment tests.


def _create_and_cleanup(experiment_name, cluster_type, **exp_kwargs):
    """Creates the experiment from the generated json, deletes it afterwards and returns the create response."""
    input_json_file = write_create_exp_json(experiment_name, **exp_kwargs)
    print("input_json_file = ", input_json_file)
    form_kruize_url(cluster_type)

    delete_and_create_metadata_profile()

    response = delete_experiment(input_json_file, rm=False)
    print("delete exp = ", response.status_code)

    response = create_experiment(input_json_file)
    print(response.json()['message'])

    # Delete the experiment, if it was created
    delete_response = delete_experiment(input_json_file, rm=False)
    print("delete exp = ", delete_response.status_code)
    return response


@pytest.mark.sanity
@pytest.mark.parametrize("test_name, experiment_type, models, resource_scope_settings", [
    ("scope_absent", "container", None, None),
    ("scope_cpu_only", "container", None, {"cpu": [UNDER_PROVISIONED]}),
    ("scope_memory_only", "container", None, {"memory": [OVER_PROVISIONED]}),
    ("scope_cpu_and_memory", "container", None, {"cpu": [UNDER_PROVISIONED, IDLE], "memory": [OVER_PROVISIONED]}),
    ("scope_all_for_both", "container", None, {"cpu": [SCOPE_ALL], "memory": [SCOPE_ALL]}),
    ("scope_all_mixed_with_others", "container", None, {"cpu": [SCOPE_ALL, UNDER_PROVISIONED]}),
    ("scope_idle_for_cpu", "container", None, {"cpu": [IDLE]}),
    ("scope_case_insensitive_and_alias", "container", None, {"cpu": ["Under_Provisioned"], "memory": ["OPTIMIZED"]}),
    ("scope_namespace_experiment", "namespace", None, {"cpu": [OVER_PROVISIONED]}),
])
def test_create_exp_resource_scope_settings_valid(test_name, experiment_type, models, resource_scope_settings, cluster_type):
    """
    Test Description: This test validates that createExperiment accepts valid resource_scope_settings
    for container and namespace experiments.
    """
    response = _create_and_cleanup("tfb-" + test_name.replace("_", "-"), cluster_type,
                                   experiment_type=experiment_type, models=models,
                                   resource_scope_settings=resource_scope_settings)
    data = response.json()

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG


@pytest.mark.negative
@pytest.mark.parametrize("test_name, models, resource_scope_settings, expected_error_msg", [
    ("scope_unknown_model", ["bogus"], None, CREATE_EXP_INVALID_MODEL),
    ("scope_empty_object", None, {}, CREATE_EXP_RESOURCE_SCOPE_EMPTY),
    ("scope_empty_cpu_list", None, {"cpu": []}, CREATE_EXP_RESOURCE_SCOPE_EMPTY_LIST % "cpu"),
    ("scope_empty_memory_list", None, {"memory": []}, CREATE_EXP_RESOURCE_SCOPE_EMPTY_LIST % "memory"),
    ("scope_unknown_cpu_scope", None, {"cpu": ["bogus"]}, CREATE_EXP_INVALID_CPU_SCOPE % "bogus"),
    ("scope_unknown_memory_scope", None, {"memory": ["bogus"]}, CREATE_EXP_INVALID_MEMORY_SCOPE % "bogus"),
    ("scope_idle_for_memory", None, {"memory": [IDLE]}, CREATE_EXP_IDLE_NOT_VALID_FOR_MEMORY),
    ("scope_idle_for_memory_with_valid_cpu", None, {"cpu": [UNDER_PROVISIONED], "memory": [IDLE]},
     CREATE_EXP_IDLE_NOT_VALID_FOR_MEMORY),
    ("scope_valid_cpu_unknown_memory", None, {"cpu": [IDLE], "memory": ["bogus"]},
     CREATE_EXP_INVALID_MEMORY_SCOPE % "bogus"),
    ("scope_blank_cpu_scope", None, {"cpu": [""]}, CREATE_EXP_INVALID_CPU_SCOPE % ""),
])
def test_create_exp_resource_scope_settings_invalid(test_name, models, resource_scope_settings, expected_error_msg, cluster_type):
    """
    Test Description: This test validates that createExperiment rejects an unknown model and invalid
    resource_scope_settings with HTTP 400 and the expected error message.
    """
    response = _create_and_cleanup("tfb-" + test_name.replace("_", "-"), cluster_type,
                                   models=models, resource_scope_settings=resource_scope_settings)
    data = response.json()

    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'] == expected_error_msg


@pytest.mark.negative
def test_create_exp_resource_scope_settings_control_characters(cluster_type):
    """
    Test Description: A scope value containing CR/LF is rejected as an unknown scope with HTTP 400,
    and does not cause a server error.
    """
    scope_value = "x\r\ny"
    response = _create_and_cleanup("tfb-scope-control-characters", cluster_type,
                                   resource_scope_settings={"cpu": [scope_value]})
    data = response.json()

    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    assert data['message'].startswith("Unknown cpu scope")
