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
import itertools
import json

import pytest
import sys

sys.path.append("../../")

from helpers.fixtures import *
from helpers.kruize import *
from helpers.utils import *

# Behaviour tests for recommendation_settings.resource_scope_settings.
#
# The local monitoring suite reads live Prometheus data, so the provisioning state of the workload cannot be forced.
# Every test therefore creates an UNSCOPED reference experiment (all models, cpu and memory scope "all"), derives the
# expected result from it with expected_models_for_scope() and compares it to the scoped experiment.
# A scope that matches nothing in the live data does not fail a test, it is skipped, because there is nothing to prove.

ALL_MODELS = ["cost", "performance"]
SCOPE_ALL_ALL = {"cpu": [SCOPE_ALL], "memory": [SCOPE_ALL]}


def _install_profiles(cluster_type):
    """Installs the metric and metadata profiles and imports the datasource metadata, as the e2e tests do."""
    form_kruize_url(cluster_type)

    metric_profile_dir = get_metric_profile_dir()
    if cluster_type == "minikube":
        metric_profile_json_file = metric_profile_dir / 'resource_optimization_local_monitoring_norecordingrules.json'
    else:
        metric_profile_json_file = metric_profile_dir / 'resource_optimization_local_monitoring.json'

    response = delete_metric_profile(metric_profile_json_file)
    print("delete metric profile = ", response.status_code)
    response = create_metric_profile(metric_profile_json_file)
    assert response.status_code == SUCCESS_STATUS_CODE

    delete_and_create_metadata_profile()

    input_json_file = "../json_files/import_metadata.json"
    response = delete_metadata(input_json_file)
    print("delete metadata = ", response.status_code)
    response = import_metadata(input_json_file)
    assert response.status_code == SUCCESS_STATUS_CODE


def _create_generate_list(experiment_name, experiment_type="container", **exp_kwargs):
    """
    Creates the experiment, generates the recommendations and lists them.
    Returns (input_json_file, listRecommendations json).
    """
    input_json_file = write_create_exp_json(experiment_name, experiment_type=experiment_type, **exp_kwargs)

    response = delete_experiment(input_json_file, rm=False)
    print("delete exp = ", response.status_code)

    response = create_experiment(input_json_file)
    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE, data
    assert data['message'] == CREATE_EXP_SUCCESS_MSG

    response = generate_recommendations(experiment_name)
    assert response.status_code == SUCCESS_STATUS_CODE

    response = list_recommendations(experiment_name)
    assert response.status_code == SUCCESS_200_STATUS_CODE
    return input_json_file, response.json()


def _cleanup(*input_json_files):
    for input_json_file in input_json_files:
        response = delete_experiment(input_json_file, rm=False)
        print("delete exp = ", response.status_code)


def _scoped_vs_unscoped(cluster_type, test_name, resource_scope_settings, experiment_type="container",
                        models=None):
    """
    Runs the unscoped reference and the scoped experiment, asserts the scoped result against the scope and returns
    (unscoped recommendations, scoped recommendations) as {(term, model): engine_object} maps.
    """
    _install_profiles(cluster_type)
    suffix = test_name.replace("_", "-")
    reference_models = ALL_MODELS if models is None else models

    reference_file, reference_json = _create_generate_list("tfb-ref-" + suffix, experiment_type,
                                                           models=reference_models,
                                                           resource_scope_settings=SCOPE_ALL_ALL)
    scoped_file, scoped_json = _create_generate_list("tfb-scoped-" + suffix, experiment_type,
                                                     models=reference_models,
                                                     resource_scope_settings=resource_scope_settings)
    _cleanup(reference_file, scoped_file)

    unscoped = get_model_recommendations(reference_json)
    scoped = get_model_recommendations(scoped_json)
    if not unscoped:
        pytest.skip("No recommendations were generated for the unscoped experiment, nothing to compare")

    # Every returned model must satisfy the scope on its own data
    assert set(scoped.keys()) == expected_models_for_scope(scoped, resource_scope_settings), \
        f"Scoped result contains a model that does not match {resource_scope_settings}"
    # Nothing that matches the scope may be dropped
    expected = expected_models_for_scope(unscoped, resource_scope_settings)
    assert set(scoped.keys()) == expected, \
        f"Expected {sorted(expected)} for {resource_scope_settings}, got {sorted(scoped.keys())}"
    return unscoped, scoped


@pytest.mark.extended
@pytest.mark.parametrize("test_name, resource_scope_settings", [
    ("memory_under_only", {"memory": [UNDER_PROVISIONED]}),
    ("cpu_over_only", {"cpu": [OVER_PROVISIONED]}),
    ("both_over", {"cpu": [OVER_PROVISIONED], "memory": [OVER_PROVISIONED]}),
    ("both_under", {"cpu": [UNDER_PROVISIONED], "memory": [UNDER_PROVISIONED]}),
    ("both_optimised", {"cpu": [OPTIMISED], "memory": [OPTIMISED]}),
    ("multiple_values", {"cpu": [UNDER_PROVISIONED, OVER_PROVISIONED], "memory": [UNDER_PROVISIONED, OPTIMISED]}),
    ("all_all", SCOPE_ALL_ALL),
])
def test_resource_scope_filtering_container(test_name, resource_scope_settings, cluster_type):
    """
    Test Description: This test validates that a container experiment only keeps the models whose cpu and memory
    state match resource_scope_settings (AND across the resources, OR across the values of one resource).
    """
    unscoped, scoped = _scoped_vs_unscoped(cluster_type, "ctr-" + test_name, resource_scope_settings)

    if resource_scope_settings == SCOPE_ALL_ALL:
        assert set(scoped.keys()) == set(unscoped.keys())


@pytest.mark.extended
@pytest.mark.parametrize("test_name, resource_scope_settings", [
    ("memory_under_only", {"memory": [UNDER_PROVISIONED]}),
    ("cpu_over_only", {"cpu": [OVER_PROVISIONED]}),
])
def test_resource_scope_filtering_namespace(test_name, resource_scope_settings, cluster_type):
    """
    Test Description: This test validates the same filtering for a namespace experiment.
    """
    _scoped_vs_unscoped(cluster_type, "ns-" + test_name, resource_scope_settings, experiment_type="namespace",
                        models=["cost", "performance"])


@pytest.mark.extended
def test_resource_scope_default_models_unchanged(cluster_type):
    """
    Test Description: This test validates that cost and performance keep the "all" default, so without
    resource_scope_settings every model that has a recommendation is returned.
    """
    _install_profiles(cluster_type)

    reference_file, reference_json = _create_generate_list("tfb-default-ref", models=["cost", "performance"],
                                                           resource_scope_settings=SCOPE_ALL_ALL)
    default_file, default_json = _create_generate_list("tfb-default-none")
    _cleanup(reference_file, default_file)

    reference = get_model_recommendations(reference_json)
    default = get_model_recommendations(default_json)
    if not reference:
        pytest.skip("No recommendations were generated, nothing to compare")

    assert {model for _, model in default.keys()} <= {"cost", "performance"}
    assert set(default.keys()) == set(reference.keys())


@pytest.mark.extended
def test_resource_scope_all_models_filtered_out(cluster_type):
    """
    Test Description: This test validates that when the scope removes every model, the response has no
    recommendation data and carries the "not enough data" notification.
    """
    _install_profiles(cluster_type)

    reference_file, reference_json = _create_generate_list("tfb-none-ref", models=ALL_MODELS,
                                                           resource_scope_settings=SCOPE_ALL_ALL)
    reference = get_model_recommendations(reference_json)
    if not reference:
        _cleanup(reference_file)
        pytest.skip("No recommendations were generated, nothing to compare")

    # Find a cpu/memory combination that matches nothing in the live data
    states = [UNDER_PROVISIONED, OVER_PROVISIONED, OPTIMISED]
    unmatched_scope = None
    for cpu_state, memory_state in itertools.product(states, states):
        candidate = {"cpu": [cpu_state], "memory": [memory_state]}
        if not expected_models_for_scope(reference, candidate):
            unmatched_scope = candidate
            break
    if unmatched_scope is None:
        _cleanup(reference_file)
        pytest.skip("Every cpu/memory combination matches a model in the live data")

    scoped_file, scoped_json = _create_generate_list("tfb-none-scoped", models=ALL_MODELS,
                                                     resource_scope_settings=unmatched_scope)
    _cleanup(reference_file, scoped_file)

    recommendations = scoped_json[0]["kubernetes_objects"][0]["containers"][0]["recommendations"]
    assert len(recommendations["data"]) == 0
    assert NOTIFICATION_CODE_FOR_NOT_ENOUGH_DATA in recommendations["notifications"]


@pytest.mark.extended
def test_resource_scope_applies_to_repeated_generation(cluster_type):
    """
    Test Description: This test validates that the scope stays applied when recommendations are generated
    again for the same experiment.
    """
    _install_profiles(cluster_type)
    resource_scope_settings = {"memory": [UNDER_PROVISIONED]}

    input_json_file, _ = _create_generate_list("tfb-repeat", models=["cost", "performance"],
                                               resource_scope_settings=resource_scope_settings)
    experiment_name = json.load(open(input_json_file))[0]["experiment_name"]

    response = generate_recommendations(experiment_name)
    assert response.status_code == SUCCESS_STATUS_CODE
    response = list_recommendations(experiment_name)
    assert response.status_code == SUCCESS_200_STATUS_CODE
    scoped = get_model_recommendations(response.json())
    _cleanup(input_json_file)

    assert set(scoped.keys()) == expected_models_for_scope(scoped, resource_scope_settings)
