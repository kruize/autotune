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
import datetime
import json
import time

import pytest
import sys

sys.path.append("../../")

from helpers.fixtures import *
from helpers.reco_json_schemas import *
from helpers.list_reco_json_validate import *
from helpers.list_metric_profiles_validate import *
from helpers.utils import *
from jinja2 import Environment, FileSystemLoader
from helpers.list_metadata_profiles_validate import *
from helpers.list_metadata_profiles_schema import *


metric_profile_dir = get_metric_profile_dir()
metadata_profile_dir = get_metadata_profile_dir()

@pytest.mark.sanity
@pytest.mark.parametrize("test_name, expected_status_code, version, experiment_name, cluster_name, performance_profile, metadata_profile, mode, target_cluster, datasource, experiment_type, kubernetes_obj_type, name, namespace, namespace_name, container_image_name, container_name, measurement_duration, threshold",
    [
        ("list_reco_default_cluster1", SUCCESS_STATUS_CODE, "v2.0", "test-default-ns", "cluster-1", "resource-optimization-local-monitoring", "cluster-metadata-local-monitoring", "monitor", "local", "prometheus-1", "namespace", None, None, None, "default", None, None, "15min", "0.1"),
        ("list_reco_default_cluster2", SUCCESS_STATUS_CODE, "v2.0", "test-default-ns", "cluster-2", "resource-optimization-local-monitoring", "cluster-metadata-local-monitoring", "monitor", "local", "prometheus-1", "namespace", None, None, None, "default", None, None, "15min", "0.1")
    ]
)
def test_list_recommendations_namespace_single_result(test_name, expected_status_code, version, experiment_name, cluster_name, performance_profile, metadata_profile, mode, target_cluster, datasource, experiment_type, kubernetes_obj_type, name, namespace, namespace_name, container_image_name, container_name, measurement_duration, threshold, cluster_type):
    """test_list_recommendations_namespace_single_result
    Test Description: This test validates listRecommendations by passing a valid
    namespace experiment name
    """
    # Generate a temporary JSON filename
    tmp_json_file = "/tmp/create_exp_" + test_name + ".json"
    print("tmp_json_file = ", tmp_json_file)

    # Load the Jinja2 template
    environment = Environment(loader=FileSystemLoader("../json_files/"))
    template = environment.get_template("create_exp_template.json")

    # Render the JSON content from the template
    content = template.render(
        version=version,
        experiment_name=experiment_name,
        cluster_name=cluster_name,
        performance_profile=performance_profile,
        metadata_profile=metadata_profile,
        mode=mode,
        target_cluster=target_cluster,
        datasource=datasource,
        experiment_type=experiment_type,
        kubernetes_obj_type=kubernetes_obj_type,
        name=name,
        namespace=namespace,
        namespace_name=namespace_name,
        container_image_name=container_image_name,
        container_name=container_name,
        measurement_duration=measurement_duration,
        threshold=threshold
    )

    # Convert rendered content to a dictionary
    json_content = json.loads(content)

    if json_content[0]["kubernetes_objects"][0]["type"] == "None":
        json_content[0]["kubernetes_objects"][0].pop("type")
    if json_content[0]["kubernetes_objects"][0]["name"] == "None":
        json_content[0]["kubernetes_objects"][0].pop("name")
    if json_content[0]["kubernetes_objects"][0]["namespace"] == "None":
        json_content[0]["kubernetes_objects"][0].pop("namespace")
    if json_content[0]["kubernetes_objects"][0]["containers"][0]["container_image_name"] == "None":
        json_content[0]["kubernetes_objects"][0].pop("containers")

    # Write the final JSON to the temp file
    with open(tmp_json_file, mode="w", encoding="utf-8") as message:
        json.dump(json_content, message, indent=4)

    input_json_file = tmp_json_file

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file, rm=False)
    print("delete exp = ", response.status_code)

    #Install default metric profile
    if cluster_type == "minikube":
        metric_profile_json_file = metric_profile_dir / 'resource_optimization_local_monitoring_norecordingrules.json'

    if cluster_type == "openshift":
        metric_profile_json_file = metric_profile_dir / 'resource_optimization_local_monitoring.json'

    response = delete_metric_profile(metric_profile_json_file)
    print("delete metric profile = ", response.status_code)

    # Create metric profile using the specified json
    response = create_metric_profile(metric_profile_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS

    json_file = open(metric_profile_json_file, "r")
    input_json = json.loads(json_file.read())
    metric_profile_name = input_json['metadata']['name']
    assert data['message'] == CREATE_METRIC_PROFILE_SUCCESS_MSG % metric_profile_name

    response = list_metric_profiles(name=metric_profile_name, logging=False)
    metric_profile_json = response.json()

    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json against the json schema
    errorMsg = validate_list_metric_profiles_json(metric_profile_json, list_metric_profiles_schema)
    assert errorMsg == ""

    # Install metadata profile
    metadata_profile_json_file = metadata_profile_dir / 'cluster_metadata_local_monitoring.json'
    json_data = json.load(open(metadata_profile_json_file))
    metadata_profile_name = json_data['metadata']['name']

    response = delete_metadata_profile(metadata_profile_name)
    print("delete metadata profile = ", response.status_code)

    # Create metadata profile using the specified json
    response = create_metadata_profile(metadata_profile_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS

    assert data['message'] == CREATE_METADATA_PROFILE_SUCCESS_MSG % metadata_profile_name

    response = list_metadata_profiles(name=metadata_profile_name, logging=False)
    metadata_profile_json = response.json()

    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json against the json schema
    errorMsg = validate_list_metadata_profiles_json(metadata_profile_json, list_metadata_profiles_schema)
    assert errorMsg == ""

    # Create namespace experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG

    # generate recommendations
    json_file = open(input_json_file, "r")
    input_json = json.loads(json_file.read())
    exp_name = input_json[0]['experiment_name']

    response = generate_recommendations(exp_name)
    assert response.status_code == SUCCESS_STATUS_CODE

    # Invoke list recommendations for the specified experiment
    response = list_recommendations(exp_name)
    assert response.status_code == SUCCESS_200_STATUS_CODE
    list_reco_json = response.json()

    # Validate the json against the json schema
    errorMsg = validate_list_reco_json(list_reco_json, list_reco_namespace_json_local_monitoring_schema)
    assert errorMsg == ""

    # Validate the json values
    validate_local_monitoring_recommendation_data_present(list_reco_json)
    namespace_exp_json = read_json_data_from_file(input_json_file)
    validate_local_monitoring_reco_json(namespace_exp_json[0], list_reco_json[0])

    # Delete experiment
    response = delete_experiment(input_json_file, rm=False)
    print("delete exp = ", response.status_code)
    assert response.status_code == SUCCESS_STATUS_CODE

@pytest.mark.skip(reason="This will be enabled once human eval benchmark setup is included in the run")
@pytest.mark.sanity
@pytest.mark.parametrize(
    "test_name, expected_status_code, version, experiment_name, cluster_name, performance_profile, metadata_profile, mode, target_cluster, datasource, experiment_type, kubernetes_obj_type, name, namespace, namespace_name, container_image_name, container_name, measurement_duration, threshold",
                         [
                             ("list_accelerator_recommendations", SUCCESS_STATUS_CODE, "v2.0", "human_eval_exp", "cluster-1", "resource-optimization-local-monitoring", "cluster-metadata-local-monitoring", "monitor", "local", "prometheus-1", "container", "statefulset", "human-eval-benchmark", "unpartitioned", None, None, "human-eval-benchmark", "15min", "0.1"),
                         ]
                         )
def test_accelerator_recommendation_if_exists(
        test_name,
        expected_status_code,
        version,
        experiment_name,
        cluster_name,
        performance_profile,
        metadata_profile,
        mode,
        target_cluster,
        datasource,
        experiment_type,
        kubernetes_obj_type,
        name,
        namespace,
        namespace_name,
        container_image_name,
        container_name,
        measurement_duration,
        threshold,
        cluster_type):
    """
    Test Description: This test validates listRecommendations by passing a valid
    container experiment name which has gpu usage
    """
    # Generate a temporary JSON filename
    tmp_json_file = "/tmp/create_exp_" + test_name + ".json"
    print("tmp_json_file = ", tmp_json_file)

    # Load the Jinja2 template
    environment = Environment(loader=FileSystemLoader("../json_files/"))
    template = environment.get_template("create_exp_template.json")

    # Render the JSON content from the template
    content = template.render(
        version=version,
        experiment_name=experiment_name,
        cluster_name=cluster_name,
        performance_profile=performance_profile,
        metadata_profile=metadata_profile,
        mode=mode,
        target_cluster=target_cluster,
        datasource=datasource,
        experiment_type=experiment_type,
        kubernetes_obj_type=kubernetes_obj_type,
        name=name,
        namespace=namespace,
        namespace_name=namespace_name,
        container_image_name=container_image_name,
        container_name=container_name,
        measurement_duration=measurement_duration,
        threshold=threshold
    )

    # Convert rendered content to a dictionary
    json_content = json.loads(content)

    if json_content[0]["kubernetes_objects"][0]["type"] == "None":
        json_content[0]["kubernetes_objects"][0].pop("type")
    if json_content[0]["kubernetes_objects"][0]["namespaces"]["namespace"] == "None":
        json_content[0]["kubernetes_objects"][0].pop("namespaces")
    if json_content[0]["kubernetes_objects"][0]["containers"][0]["container_name"] == "None":
        json_content[0]["kubernetes_objects"][0].pop("containers")

    # Write the final JSON to the temp file
    with open(tmp_json_file, mode="w", encoding="utf-8") as message:
        json.dump(json_content, message, indent=4)

    input_json_file = tmp_json_file

    form_kruize_url(cluster_type)
    response = delete_experiment(input_json_file, rm=False)
    print("delete exp = ", response.status_code)

    #Install default metric profile
    if cluster_type == "minikube":
        metric_profile_json_file = metric_profile_dir / 'resource_optimization_local_monitoring_norecordingrules.json'

    if cluster_type == "openshift":
        metric_profile_json_file = metric_profile_dir / 'resource_optimization_local_monitoring.json'

    response = delete_metric_profile(metric_profile_json_file)
    print("delete metric profile = ", response.status_code)

    # Create metric profile using the specified json
    response = create_metric_profile(metric_profile_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS

    json_file = open(metric_profile_json_file, "r")
    input_json = json.loads(json_file.read())
    metric_profile_name = input_json['metadata']['name']
    assert data['message'] == CREATE_METRIC_PROFILE_SUCCESS_MSG % metric_profile_name

    response = list_metric_profiles(name=metric_profile_name, logging=False)
    metric_profile_json = response.json()

    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json against the json schema
    errorMsg = validate_list_metric_profiles_json(metric_profile_json, list_metric_profiles_schema)
    assert errorMsg == ""

    # Install metadata profile
    metadata_profile_json_file = metadata_profile_dir / 'cluster_metadata_local_monitoring.json'
    json_data = json.load(open(metadata_profile_json_file))
    metadata_profile_name = json_data['metadata']['name']

    response = delete_metadata_profile(metadata_profile_name)
    print("delete metadata profile = ", response.status_code)

    # Create metadata profile using the specified json
    response = create_metadata_profile(metadata_profile_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS

    assert data['message'] == CREATE_METADATA_PROFILE_SUCCESS_MSG % metadata_profile_name

    response = list_metadata_profiles(name=metadata_profile_name, logging=False)
    metadata_profile_json = response.json()

    assert response.status_code == SUCCESS_200_STATUS_CODE

    # Validate the json against the json schema
    errorMsg = validate_list_metadata_profiles_json(metadata_profile_json, list_metadata_profiles_schema)
    assert errorMsg == ""

    # Create namespace experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG

    # generate recommendations
    json_file = open(input_json_file, "r")
    input_json = json.loads(json_file.read())
    exp_name = input_json[0]['experiment_name']

    response = generate_recommendations(exp_name)
    assert response.status_code == SUCCESS_STATUS_CODE

    # Invoke list recommendations for the specified experiment
    response = list_recommendations(exp_name)
    assert response.status_code == SUCCESS_200_STATUS_CODE
    list_reco_json = response.json()

    # Validate the json against the json schema
    errorMsg = validate_list_reco_json(list_reco_json, list_reco_json_local_monitoring_schema)
    assert errorMsg == ""

    # Validate accelerator info
    validate_accelerator_recommendations_for_container(list_reco_json)

    # Delete experiment
    response = delete_experiment(input_json_file, rm=False)
    print("delete exp = ", response.status_code)
    assert response.status_code == SUCCESS_STATUS_CODE


# ===========================================================================
# flex + stability recommendation routing tests
# ===========================================================================

# Notification codes
_CODE_FLEX_TERM_AVAILABLE   = NOTIFICATION_CODE_FOR_FLEX_TERM_RECOMMENDATIONS_AVAILABLE   # 111104
_CODE_SHORT_TERM_AVAILABLE  = NOTIFICATION_CODE_FOR_SHORT_TERM_RECOMMENDATIONS_AVAILABLE  # 111101
_CODE_STABILITY_AVAILABLE   = NOTIFICATION_CODE_FOR_STABILITY_RECOMMENDATIONS_AVAILABLE   # 112105
_CODE_NOT_ENOUGH_DATA       = NOTIFICATION_CODE_FOR_NOT_ENOUGH_DATA                       # 120001


def _fs_setup_profiles(cluster_type):
    """Install metric profile + metadata profile exactly as existing tests do."""
    if cluster_type == "minikube":
        metric_profile_json_file = metric_profile_dir / 'resource_optimization_local_monitoring_norecordingrules.json'
    else:
        metric_profile_json_file = metric_profile_dir / 'resource_optimization_local_monitoring.json'

    response = delete_metric_profile(metric_profile_json_file)
    print("delete metric profile = ", response.status_code)

    response = create_metric_profile(metric_profile_json_file)
    data = response.json()
    print(data['message'])
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    metric_profile_name = json.load(open(metric_profile_json_file))['metadata']['name']
    assert data['message'] == CREATE_METRIC_PROFILE_SUCCESS_MSG % metric_profile_name

    response = list_metric_profiles(name=metric_profile_name, logging=False)
    assert response.status_code == SUCCESS_200_STATUS_CODE
    errorMsg = validate_list_metric_profiles_json(response.json(), list_metric_profiles_schema)
    assert errorMsg == ""

    metadata_profile_json_file = metadata_profile_dir / 'cluster_metadata_local_monitoring.json'
    metadata_profile_name = json.load(open(metadata_profile_json_file))['metadata']['name']

    response = delete_metadata_profile(metadata_profile_name)
    print("delete metadata profile = ", response.status_code)

    response = create_metadata_profile(metadata_profile_json_file)
    data = response.json()
    print(data['message'])
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_METADATA_PROFILE_SUCCESS_MSG % metadata_profile_name

    response = list_metadata_profiles(name=metadata_profile_name, logging=False)
    assert response.status_code == SUCCESS_200_STATUS_CODE
    errorMsg = validate_list_metadata_profiles_json(response.json(), list_metadata_profiles_schema)
    assert errorMsg == ""


def _fs_build_exp_json(exp_name, terms, models):
    """
    Render the experiment template with the given terms/models.
    Uses create_exp_template.json Jinja2 template.
    """
    environment = Environment(loader=FileSystemLoader("../json_files/"))
    template = environment.get_template("create_exp_template.json")
    content = template.render(
        version="v2.0",
        experiment_name=exp_name,
        cluster_name="default",
        performance_profile="resource-optimization-local-monitoring",
        metadata_profile="cluster-metadata-local-monitoring",
        mode="monitor",
        target_cluster="local",
        datasource="prometheus-1",
        experiment_type="container",
        kubernetes_obj_type="deployment",
        name="sysbench",
        namespace="default",
        namespace_name=None,
        container_image_name="quay.io/kruizehub/sysbench:latest",
        container_name="sysbench",
        measurement_duration="2min",
        threshold="0.1",
        terms=terms,
        models=models,
    )
    json_content = json.loads(content)
    if "namespaces" in json_content[0]["kubernetes_objects"][0]:
        json_content[0]["kubernetes_objects"][0].pop("namespaces", None)
    return json_content


def _terms_in_reco(list_reco_json):
    found = set()
    for exp in list_reco_json:
        for obj in exp.get("kubernetes_objects", []):
            for c in obj.get("containers", []):
                for ts in (c.get("recommendations", {}).get("data") or {}).values():
                    reco_terms = ts.get("recommendation_terms", {})
                    found.update(k for k in reco_terms if k in ("short_term", "medium_term", "long_term", "flex_term"))
    return found


def _models_in_reco(list_reco_json):
    found = set()
    for exp in list_reco_json:
        for obj in exp.get("kubernetes_objects", []):
            for c in obj.get("containers", []):
                for ts in (c.get("recommendations", {}).get("data") or {}).values():
                    for term_data in (ts.get("recommendation_terms") or {}).values():
                        if isinstance(term_data, dict):
                            engines = term_data.get("recommendation_engines", {})
                            found.update(m for m in ("cost", "performance", "stability") if m in engines)
    return found


def _models_by_term_in_reco(list_reco_json):
    """Returns a dict mapping term_name -> set of engine/model names."""
    by_term = {}
    for exp in list_reco_json:
        for obj in exp.get("kubernetes_objects", []):
            for c in obj.get("containers", []):
                for ts in (c.get("recommendations", {}).get("data") or {}).values():
                    for term_name, term_data in (ts.get("recommendation_terms") or {}).items():
                        if isinstance(term_data, dict):
                            engines = term_data.get("recommendation_engines", {})
                            by_term.setdefault(term_name, set()).update(
                                m for m in ("cost", "performance", "stability") if m in engines
                            )
    return by_term


def _notif_codes_in_reco(list_reco_json):
    codes = set()
    for exp in list_reco_json:
        for obj in exp.get("kubernetes_objects", []):
            for c in obj.get("containers", []):
                reco = c.get("recommendations", {})
                codes.update(str(k) for k in (reco.get("notifications") or {}).keys())
                for ts in (reco.get("data") or {}).values():
                    codes.update(str(k) for k in (ts.get("notifications") or {}).keys())
                    for term_data in (ts.get("recommendation_terms") or {}).values():
                        if isinstance(term_data, dict):
                            codes.update(str(k) for k in (term_data.get("notifications") or {}).keys())
                            for engine_data in (term_data.get("recommendation_engines") or {}).values():
                                if isinstance(engine_data, dict):
                                    codes.update(str(k) for k in (engine_data.get("notifications") or {}).keys())
    return codes


@pytest.mark.sanity
@pytest.mark.parametrize("test_name, terms, models, expected_terms, excluded_terms, expected_models, excluded_models, expected_codes, excluded_codes", [
    (
        "flex_only",
        ["flex"], None,
        [FLEX_TERM], ["short_term", "medium_term", "long_term"],
        ["stability"], ["cost", "performance"],
        [_CODE_FLEX_TERM_AVAILABLE, _CODE_STABILITY_AVAILABLE], [],
    ),
    (
        "stability_only_no_term",
        None, ["stability"],
        [FLEX_TERM], ["short_term", "medium_term", "long_term"],
        ["stability"], ["cost", "performance"],
        [_CODE_FLEX_TERM_AVAILABLE, _CODE_STABILITY_AVAILABLE], [],
    ),
    (
        "flex_and_stability_explicit",
        ["flex"], ["stability"],
        [FLEX_TERM], ["short_term", "medium_term", "long_term"],
        ["stability"], ["cost", "performance"],
        [_CODE_FLEX_TERM_AVAILABLE, _CODE_STABILITY_AVAILABLE], [],
    ),
    (
        "short_only",
        ["short"], None,
        [SHORT_TERM], [FLEX_TERM],
        [], ["stability"],
        [_CODE_SHORT_TERM_AVAILABLE], [_CODE_STABILITY_AVAILABLE],
    ),
    (
        "default_no_settings",
        None, None,
        [], [FLEX_TERM],
        [], ["stability"],
        [], [_CODE_STABILITY_AVAILABLE],
    ),
])
def test_flex_stability_reco_term_model_routing(
        test_name, terms, models,
        expected_terms, excluded_terms,
        expected_models, excluded_models,
        expected_codes, excluded_codes,
        cluster_type):
    """
    Validates that generateRecommendations routes the correct term keys and model
    keys into the listRecommendations response for flex+stability combinations.
    """
    exp_name = f"flex-stab-reco-{test_name}"
    tmp_json_file = f"/tmp/flex_stab_reco_{test_name}.json"
    print("tmp_json_file = ", tmp_json_file)

    # Build and write experiment JSON from template
    json_content = _fs_build_exp_json(exp_name, terms, models)
    with open(tmp_json_file, mode="w", encoding="utf-8") as f:
        json.dump(json_content, f, indent=4)

    input_json_file = tmp_json_file

    form_kruize_url(cluster_type)

    # Delete stale experiment
    response = delete_experiment(input_json_file, rm=False)
    print("delete exp = ", response.status_code)

    # Install metric profile
    _fs_setup_profiles(cluster_type)

    try:
        # Create experiment
        response = create_experiment(input_json_file)
        data = response.json()
        print(data['message'])
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == CREATE_EXP_SUCCESS_MSG

        # Generate recommendations
        response = generate_recommendations(exp_name)
        assert response.status_code == SUCCESS_STATUS_CODE

        # List recommendations
        response = list_recommendations(exp_name)
        assert response.status_code == SUCCESS_200_STATUS_CODE
        list_reco_json = response.json()

        # Validate response schema
        errorMsg = validate_list_reco_json(list_reco_json, list_reco_json_local_monitoring_schema)
        assert errorMsg == ""

        # Validate recommendations data is present
        validate_local_monitoring_recommendation_data_present(list_reco_json)

        # Validate term/model routing
        terms_found  = _terms_in_reco(list_reco_json)
        models_found = _models_in_reco(list_reco_json)
        codes_found  = _notif_codes_in_reco(list_reco_json)

        for t in expected_terms:
            assert t in terms_found,  f"[{test_name}] Expected term {t}, got: {terms_found}"
        for t in excluded_terms:
            assert t not in terms_found, f"[{test_name}] Unexpected term {t} found: {terms_found}"
        for m in expected_models:
            assert m in models_found, f"[{test_name}] Expected model {m}, got: {models_found}"
        for m in excluded_models:
            assert m not in models_found, f"[{test_name}] Unexpected model {m} found: {models_found}"
        for c in expected_codes:
            assert c in codes_found,  f"[{test_name}] Expected code {c}, got: {codes_found}"
        for c in excluded_codes:
            assert c not in codes_found, f"[{test_name}] Unexpected code {c} found: {codes_found}"
    finally:
        # Delete experiment
        response = delete_experiment(input_json_file, rm=False)
        print("delete exp = ", response.status_code)
        assert response.status_code == SUCCESS_STATUS_CODE


@pytest.mark.sanity
def test_mixed_flex_short_routes_models_per_term(cluster_type):
    """
    flex + short (no model) → mixed-flex case:
    flex_term gets stability, short_term gets cost/perf.
    Both term keys and all model keys must appear in the recommendation with correct per-term routing.
    """
    test_name = "mixed_flex_short"
    exp_name  = f"flex-stab-reco-{test_name}"
    tmp_json_file = f"/tmp/flex_stab_reco_{test_name}.json"
    print("tmp_json_file = ", tmp_json_file)

    json_content = _fs_build_exp_json(exp_name, ["flex", "short"], None)
    with open(tmp_json_file, mode="w", encoding="utf-8") as f:
        json.dump(json_content, f, indent=4)

    input_json_file = tmp_json_file
    form_kruize_url(cluster_type)

    response = delete_experiment(input_json_file, rm=False)
    print("delete exp = ", response.status_code)

    _fs_setup_profiles(cluster_type)

    try:
        response = create_experiment(input_json_file)
        data = response.json()
        print(data['message'])
        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == CREATE_EXP_SUCCESS_MSG

        response = generate_recommendations(exp_name)
        assert response.status_code == SUCCESS_STATUS_CODE

        response = list_recommendations(exp_name)
        assert response.status_code == SUCCESS_200_STATUS_CODE
        list_reco_json = response.json()

        errorMsg = validate_list_reco_json(list_reco_json, list_reco_json_local_monitoring_schema)
        assert errorMsg == ""

        validate_local_monitoring_recommendation_data_present(list_reco_json)

        terms_found   = _terms_in_reco(list_reco_json)
        models_by_term = _models_by_term_in_reco(list_reco_json)
        codes_found   = _notif_codes_in_reco(list_reco_json)

        # Check terms
        assert FLEX_TERM in terms_found,  f"flex_term missing, got: {terms_found}"
        assert SHORT_TERM in terms_found, f"short_term missing, got: {terms_found}"

        # Per-term model isolation: flex_term must only have stability
        flex_models = models_by_term.get(FLEX_TERM, set())
        assert "stability" in flex_models, f"stability missing under flex_term: {flex_models}"
        assert "cost" not in flex_models, f"cost unexpectedly found under flex_term: {flex_models}"
        assert "performance" not in flex_models, f"performance unexpectedly found under flex_term: {flex_models}"

        # Per-term model isolation: short_term must have cost/perf and NOT stability
        short_models = models_by_term.get(SHORT_TERM, set())
        assert "stability" not in short_models, f"stability unexpectedly found under short_term: {short_models}"
        assert "cost" in short_models or "performance" in short_models, f"cost/perf missing under short_term: {short_models}"

        # Notification codes
        assert _CODE_FLEX_TERM_AVAILABLE in codes_found,  f"code 111104 missing, got: {codes_found}"
        assert _CODE_SHORT_TERM_AVAILABLE in codes_found, f"code 111101 missing, got: {codes_found}"
    finally:
        response = delete_experiment(input_json_file, rm=False)
        print("delete exp = ", response.status_code)
        assert response.status_code == SUCCESS_STATUS_CODE
