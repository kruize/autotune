"""
Copyright (c) 2026 Red Hat, IBM Corporation and others.

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
import json
import os
import shutil
import tempfile

import pytest
import sys

sys.path.append("../../")

from helpers.fixtures import *
from helpers.kruize import *
from helpers.list_reco_json_local_monitoring_schema import list_reco_json_local_monitoring_schema
from helpers.list_reco_json_validate import validate_list_reco_json
from helpers.utils import *
from pathlib import Path

layer_dir = get_layer_dir()


metric_profile_dir = get_metric_profile_dir()
metadata_profile_dir = get_metadata_profile_dir()

@pytest.mark.runtimes
def test_runtime_recommendation(cluster_type):
    """
    Test Description: Validates that runtime recommendations (GC policy / JVM options)
    are generated for JVM workloads when jvm_info metrics and layers (hotspot/semeru) are available.

    Flow: create metric profile -> create metadata profile -> create layers -> create experiment -> generate recommendations -> list recommendations
    Asserts: When a JVM layer is present, config.env contains JDK_JAVA_OPTIONS or JAVA_OPTIONS
    with GC flags (e.g. -XX:+UseG1GC or -Xgcpolicy:gencon).
    """

    clone_repo("https://github.com/kruize/benchmarks")
    benchmarks_install()

    input_json_path = str(Path(__file__).parent / "../json_files/create_tfb_exp.json")
    with open(input_json_path) as f:
        input_json = json.load(f)
    # Update datasource from prometheus-1 to thanos-1 before using in the test
    for exp in input_json:
        if exp.get("datasource") == "prometheus-1":
            exp["datasource"] = "thanos-1"
            break
    with tempfile.NamedTemporaryFile(mode="w", suffix=".json", delete=False) as tf:
        json.dump(input_json, tf, indent=2)
        input_json_file = tf.name

    form_kruize_url(cluster_type)

    # Install metric profile (use resource_optimization_local_monitoring with jvmRuntimeInfo/jvmMemoryMaxBytes)
    if cluster_type == "minikube":
        metric_profile_json_file = metric_profile_dir / "resource_optimization_local_monitoring_norecordingrules.json"
    else:
        metric_profile_json_file = metric_profile_dir / "resource_optimization_local_monitoring.json"

    response = delete_metric_profile(metric_profile_json_file)
    print("delete metric profile = ", response.status_code)

    response = create_metric_profile(metric_profile_json_file)
    data = response.json()
    print(data["message"])
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data["status"] == SUCCESS_STATUS

    # Install metadata profile
    metadata_profile_json_file = metadata_profile_dir / "cluster_metadata_local_monitoring.json"
    with open(metadata_profile_json_file) as f:
        metadata_profile_name = json.load(f)["metadata"]["name"]

    response = delete_metadata_profile(metadata_profile_name)
    print("delete metadata profile = ", response.status_code)

    response = create_metadata_profile(metadata_profile_json_file)
    data = response.json()
    print(data["message"])
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data["status"] == SUCCESS_STATUS

    # Create layers for all JSON files in layer_dir
    layer_json_files = sorted(layer_dir.glob("*.json"))
    for layer_input_json_file in layer_json_files:
        with open(layer_input_json_file, "r") as json_file:
            layer_json = json.load(json_file)
            layer_name = layer_json['layer_name']

        response = create_layer(layer_input_json_file)
        data = response.json()

        assert response.status_code == SUCCESS_STATUS_CODE
        assert data['status'] == SUCCESS_STATUS
        assert data['message'] == CREATE_LAYER_SUCCESS_MSG % layer_name

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

    response = generate_recommendations(exp_name)
    assert response.status_code == SUCCESS_STATUS_CODE

    response = list_recommendations(exp_name)
    assert response.status_code == SUCCESS_200_STATUS_CODE
    list_reco_json = response.json()

    error_msg = validate_list_reco_json(list_reco_json, list_reco_json_local_monitoring_schema)
    assert error_msg == ""

    validate_local_monitoring_recommendation_data_present(list_reco_json)
    validate_runtime_recommendations_if_present(list_reco_json)

    # Delete experiment
    response = delete_experiment(input_json_file, rm=False)
    print("delete exp = ", response.status_code)
    assert response.status_code == SUCCESS_STATUS_CODE

    # Delete Metric Profile
    response = delete_metric_profile(metric_profile_json_file)
    print("delete metric profile = ", response.status_code)
    assert response.status_code == SUCCESS_STATUS_CODE

    # Remove benchmarks directory
    shutil.rmtree("benchmarks")

    # Clean up temp experiment JSON file
    if os.path.exists(input_json_file):
        os.unlink(input_json_file)
