"""
Copyright (c) 2022, 2024 Red Hat, IBM Corporation and others.

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
import csv
import json
import os
import re
import subprocess
import time
import math
import docker
from helpers.kruize import *
from datetime import datetime, timedelta
from kubernetes import client, config
from pathlib import Path
from helpers.kruize import get_bulk_job_status
from helpers.import_metadata_json_validate import *
from helpers.list_metadata_json_validate import *
from helpers.list_metadata_json_schema import *
from helpers.list_metadata_json_verbose_true_schema import *

RUNTIMES_RECOMMENDATIONS_NOT_AVAILABLE = "Runtimes recommendations are unavailable for the provided datasource."
RUNTIMES_RECOMMENDATIONS_AVAILABLE = "Runtimes Recommendations Available"

# Kruize Recommendations Notification codes
NOTIFICATION_CODE_FOR_RUNTIMES_RECOMMENDATIONS_AVAILABLE = "112104"

# Expected env names for JVM runtime recommendations
JDK_JAVA_OPTIONS = "JDK_JAVA_OPTIONS"
JAVA_OPTIONS = "JAVA_OPTIONS"
QUARKUS_CORE_THREADS = "quarkus.thread-pool.core-threads"

# GC flag patterns (Hotspot)
HOTSPOT_GC_PATTERNS = (
    "-XX:+UseG1GC",
    "-XX:+UseSerialGC",
    "-XX:+UseParallelGC",
    "-XX:+UseZGC",
    "-XX:+UseShenandoahGC",
)

# GC policy patterns (Semeru/OpenJ9)
SEMERU_GC_PATTERNS = (
    "-Xgcpolicy:gencon",
    "-Xgcpolicy:balanced",
    "-Xgcpolicy:optthruput",
)

def _has_runtime_env_value(value):
    """Check if env value contains GC-related JVM options."""
    if not value or not isinstance(value, str):
        return False
    for pattern in HOTSPOT_GC_PATTERNS + SEMERU_GC_PATTERNS:
        if pattern in value:
            return True
    return False


def validate_runtime_recommendations_if_present(recommendations_json):
    """
    Validates runtime recommendations when present.
    Runtime recommendations appear as env entries (JDK_JAVA_OPTIONS or JAVA_OPTIONS)
    with GC flags in config of recommendation_engines (cost/performance).
    """
    if not recommendations_json or len(recommendations_json) == 0:
        return

    rec = recommendations_json[0]
    if rec.get("experiment_type") != CONTAINER_EXPERIMENT_TYPE:
        return

    kubernetes_objects = rec.get("kubernetes_objects", [])
    if not kubernetes_objects:
        return

    for k8s_obj in kubernetes_objects:
        containers = k8s_obj.get("containers", [])
        for container in containers:
            recommendations = container.get("recommendations", {})
            data = recommendations.get("data", {})
            if not data:
                continue

            for _timestamp, interval_obj in data.items():
                terms = interval_obj.get("recommendation_terms", {})
                for _term_name, term_obj in terms.items():
                    engines = term_obj.get("recommendation_engines", {})
                    # Check cost engine has runtime notification (code and message)
                    if "cost" in engines:
                        cost_notifications = engines["cost"].get("notifications", {})
                        assert NOTIFICATION_CODE_FOR_RUNTIMES_RECOMMENDATIONS_AVAILABLE in cost_notifications, \
                            f"Runtime recommendations notification code {NOTIFICATION_CODE_FOR_RUNTIMES_RECOMMENDATIONS_AVAILABLE} not found in cost engine notifications"
                        assert cost_notifications[NOTIFICATION_CODE_FOR_RUNTIMES_RECOMMENDATIONS_AVAILABLE].get("message") == RUNTIMES_RECOMMENDATIONS_AVAILABLE, \
                            f"Runtime recommendations notification message mismatch in cost engine notifications"

                    # Check performance engine has runtime notification (code and message)
                    if "performance" in engines:
                        perf_notifications = engines["performance"].get("notifications", {})
                        assert NOTIFICATION_CODE_FOR_RUNTIMES_RECOMMENDATIONS_AVAILABLE in perf_notifications, \
                            f"Runtime recommendations notification code {NOTIFICATION_CODE_FOR_RUNTIMES_RECOMMENDATIONS_AVAILABLE} not found in performance engine notifications"
                        assert perf_notifications[NOTIFICATION_CODE_FOR_RUNTIMES_RECOMMENDATIONS_AVAILABLE].get("message") == RUNTIMES_RECOMMENDATIONS_AVAILABLE, \
                            f"Runtime recommendations notification message mismatch in performance engine notifications"
                    for _engine_name, engine_obj in engines.items():
                        config = engine_obj.get("config", {})
                        env_list = config.get("env")
                        if not env_list or not isinstance(env_list, list):
                            continue

                        for env_item in env_list:
                            name = env_item.get("name")
                            value = env_item.get("value")
                            assert name in (JDK_JAVA_OPTIONS, JAVA_OPTIONS, QUARKUS_CORE_THREADS), (
                                    f"Runtime env {name} should be among {JDK_JAVA_OPTIONS}, {JAVA_OPTIONS}, {QUARKUS_CORE_THREADS}"
                                )
                            assert _has_runtime_env_value(value), f"Runtime values are incorrect"
                            assert value, f"Runtime env {name} has empty value"
                            assert _has_runtime_env_value(value), (
                                    f"Runtime env {name} should contain GC flags, got: {value}"
                            )
                            return  # Found valid runtime recommendation


def _generate_and_list_recommendations_for_tfb(
    cluster_type,
    *,
    metric_profile_json_modifier=None,
    metadata_profile_filename="cluster_metadata_local_monitoring.json",
    metadata_profile_json_modifier=None,
    layer_filter=None,
):
    """
    Helper to run the end-to-end flow:
    clone benchmarks -> install workload -> create metric/metadata profiles -> create layers
    -> create experiment -> generate & list recommendations.
    Returns the parsed listRecommendations JSON.
    """
    clone_repo("https://github.com/kruize/benchmarks")
    benchmarks_install()

    input_json_file = "../json_files/create_tfb_exp.json"
    input_json_path = Path(__file__).parent / "../json_files/create_tfb_exp.json"
    with open(input_json_path) as f:
        input_json = json.load(f)

    temp_input_json_file = None
    temp_metric_profile_file = None
    temp_metadata_profile_file = None

    form_kruize_url(cluster_type)

    # Install metric profile (use resource_optimization_local_monitoring with jvmRuntimeInfo/jvmMemoryMaxBytes)
    if cluster_type == "minikube":
        metric_profile_json_file = metric_profile_dir / "resource_optimization_local_monitoring_norecordingrules.json"
    else:
        metric_profile_json_file = metric_profile_dir / "resource_optimization_local_monitoring.json"
        # Update datasource from prometheus-1 to thanos-1 before using in the test
        for exp in input_json:
            if exp.get("datasource") == "prometheus-1":
                exp["datasource"] = "thanos-1"
                break
        with tempfile.NamedTemporaryFile(mode="w", suffix=".json", delete=False) as tf:
            json.dump(input_json, tf, indent=2)
            temp_input_json_file = tf.name
            input_json_file = temp_input_json_file

    # Optional: tweak metric profile JSON for specific scenarios
    if metric_profile_json_modifier is not None:
        with open(metric_profile_json_file) as f:
            metric_profile_json = json.load(f)
        metric_profile_json = metric_profile_json_modifier(metric_profile_json)
        with tempfile.NamedTemporaryFile(mode="w", suffix=".json", delete=False) as tf:
            json.dump(metric_profile_json, tf, indent=2)
            temp_metric_profile_file = tf.name
            metric_profile_json_file = Path(temp_metric_profile_file)

    response = delete_metric_profile(metric_profile_json_file)
    print("delete metric profile = ", response.status_code)

    response = create_metric_profile(metric_profile_json_file)
    data = response.json()
    print(data["message"])
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data["status"] == SUCCESS_STATUS

    # Install metadata profile (default or overridden filename)
    metadata_profile_json_file = metadata_profile_dir / metadata_profile_filename
    with open(metadata_profile_json_file) as f:
        metadata_profile_json = json.load(f)

    if metadata_profile_json_modifier is not None:
        metadata_profile_json = metadata_profile_json_modifier(metadata_profile_json)
        with tempfile.NamedTemporaryFile(mode="w", suffix=".json", delete=False) as tf:
            json.dump(metadata_profile_json, tf, indent=2)
            temp_metadata_profile_file = tf.name
            metadata_profile_json_file = Path(temp_metadata_profile_file)

    metadata_profile_name = metadata_profile_json["metadata"]["name"]

    response = delete_metadata_profile(metadata_profile_name)
    print("delete metadata profile = ", response.status_code)

    response = create_metadata_profile(metadata_profile_json_file)
    data = response.json()
    print(data["message"])
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data["status"] == SUCCESS_STATUS

    # Create layers for all JSON files in layer_dir (optionally filtered)
    layer_json_files = sorted(layer_dir.glob("*.json"))
    if layer_filter is not None:
        layer_json_files = [p for p in layer_json_files if layer_filter(p)]

    for layer_input_json_file in layer_json_files:
        with open(layer_input_json_file, "r") as json_file:
            layer_json = json.load(json_file)
            layer_name = layer_json["layer_name"]

        response = create_layer(layer_input_json_file)
        data = response.json()

        assert response.status_code == SUCCESS_STATUS_CODE
        assert data["status"] == SUCCESS_STATUS
        assert data["message"] == CREATE_LAYER_SUCCESS_MSG % layer_name

        print(f"✓ Layer '{layer_name}' created successfully")

    # Create experiment using TechEmpower Quarkus JVM workload
    response = delete_experiment(input_json_file, rm=False)
    print("delete exp = ", response.status_code)

    response = create_experiment(input_json_file)
    data = response.json()
    print(data["message"])
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data["status"] == SUCCESS_STATUS
    assert data["message"] == CREATE_EXP_SUCCESS_MSG

    exp_name = input_json[0]["experiment_name"]

    try:
        response = generate_recommendations(exp_name)
        assert response.status_code == SUCCESS_STATUS_CODE

        response = list_recommendations(exp_name)
        assert response.status_code == SUCCESS_200_STATUS_CODE
        list_reco_json = response.json()

        error_msg = validate_list_reco_json(list_reco_json, list_reco_json_local_monitoring_schema)
        assert error_msg == ""

        validate_local_monitoring_recommendation_data_present(list_reco_json)
        return list_reco_json
    finally:
        # Delete experiment
        response = delete_experiment(input_json_file, rm=False)
        print("delete exp = ", response.status_code)
        assert response.status_code == SUCCESS_STATUS_CODE

        # Delete Metric Profile
        response = delete_metric_profile(metric_profile_json_file)
        print("delete metric profile = ", response.status_code)

        # Remove benchmarks directory
        if os.path.isdir("benchmarks"):
            shutil.rmtree("benchmarks")

        # Clean up temp experiment JSON file
        if temp_input_json_file and os.path.exists(temp_input_json_file):
            os.unlink(temp_input_json_file)

        # Clean up temp metric/metadata profile JSON files
        if temp_metric_profile_file and os.path.exists(temp_metric_profile_file):
            os.unlink(temp_metric_profile_file)
        if temp_metadata_profile_file and os.path.exists(temp_metadata_profile_file):
            os.unlink(temp_metadata_profile_file)


def _extract_runtime_envs(list_reco_json):
    """
    Traverse listRecommendations JSON and return all env entries from
    recommendation_engines[*].config.env for container experiments.
    """
    if not list_reco_json:
        return []

    rec = list_reco_json[0]
    if rec.get("experiment_type") != CONTAINER_EXPERIMENT_TYPE:
        return []

    envs = []
    for k8s_obj in rec.get("kubernetes_objects", []):
        for container in k8s_obj.get("containers", []):
            recommendations = container.get("recommendations", {})
            data = recommendations.get("data", {})
            for _ts, interval_obj in data.items():
                terms = interval_obj.get("recommendation_terms", {})
                for _term_name, term_obj in terms.items():
                    engines = term_obj.get("recommendation_engines", {})
                    for _engine_name, engine_obj in engines.items():
                        config = engine_obj.get("config", {})
                        env_list = config.get("env") or []
                        if isinstance(env_list, list):
                            envs.extend(env_list)
    return envs


def _env_values(list_reco_json):
    return [env.get("value", "") for env in _extract_runtime_envs(list_reco_json)]


def _contains_any_pattern(values, patterns):
    for v in values:
        if not isinstance(v, str):
            continue
        for p in patterns:
            if p in v:
                return True
    return False

