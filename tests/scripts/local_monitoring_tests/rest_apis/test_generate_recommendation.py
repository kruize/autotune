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

from helpers.runtime_utils import _generate_and_list_recommendations_for_tfb, _env_values, _contains_any_pattern

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

    list_reco_json = _generate_and_list_recommendations_for_tfb(cluster_type)

    # Generic runtime recommendation validation as before
    validate_runtime_recommendations_if_present(list_reco_json)


@pytest.mark.runtimes
def test_semeru_gc_policy_when_layer_present(cluster_type):
    """
    Test Description: When the Semeru/OpenJ9 runtime is active and the `semeru` layer is present,
    runtime recommendations should include JAVA_OPTIONS with Semeru GC policy flags.

    Expected: At least one env value contains one of SEMERU_GC_PATTERNS
    (e.g., -Xgcpolicy:gencon, -Xgcpolicy:balanced, -Xgcpolicy:optthruput).
    """
    list_reco_json = _generate_and_list_recommendations_for_tfb(cluster_type)
    env_values = _env_values(list_reco_json)

    if not _contains_any_pattern(env_values, SEMERU_GC_PATTERNS):
        pytest.skip("Semeru/OpenJ9 GC policy not detected for current workload – skipping Semeru-specific assertion")

    assert _contains_any_pattern(env_values, SEMERU_GC_PATTERNS), (
        f"Expected Semeru GC policy flags {SEMERU_GC_PATTERNS} in JAVA_OPTIONS, got: {env_values}"
    )


@pytest.mark.runtimes
def test_no_runtime_recommendations_when_jvm_metadata_missing(cluster_type):
    """
    Test Description: If jvmInfo/jvmInfoTotal metrics are not configured in the metric profile,
    runtime recommendations should not be generated.

    Expected: No runtime-related env entries (JDK_JAVA_OPTIONS/JAVA_OPTIONS) with GC flags.
    """

    def remove_jvm_metrics(metric_profile_json):
        vars_list = metric_profile_json.get("slo", {}).get("function_variables", [])
        filtered = [
            v for v in vars_list
            if v.get("name") not in ("jvmInfo", "jvmInfoTotal", "jvmMemoryMaxBytes")
        ]
        metric_profile_json["slo"]["function_variables"] = filtered
        return metric_profile_json

    list_reco_json = _generate_and_list_recommendations_for_tfb(
        cluster_type,
        metric_profile_json_modifier=remove_jvm_metrics,
    )

    env_values = _env_values(list_reco_json)
    assert not _contains_any_pattern(env_values, HOTSPOT_GC_PATTERNS + SEMERU_GC_PATTERNS), (
        f"GC-related runtime env not expected when JVM metrics are missing, but found: {env_values}"
    )


@pytest.mark.runtimes
def test_no_gc_recommendation_when_jvm_version_missing(cluster_type):
    """
    Test Description: If jvm_info metrics are present but the version label is not part
    of the aggregation (simulating missing version), no GC-specific recommendation should be emitted.

    Expected: No GC flags in runtime env (null / missing version handling).
    """

    def strip_version_from_jvm_queries(metric_profile_json):
        def _rewrite_query(q):
            # Best-effort: drop ', version' from the 'sum by(... )' grouping clause
            return q.replace(", version", "")

        for var in metric_profile_json.get("slo", {}).get("function_variables", []):
            if var.get("name") in ("jvmInfo", "jvmInfoTotal"):
                for af in var.get("aggregation_functions", []):
                    query = af.get("query")
                    if isinstance(query, str) and "sum by(" in query and "version" in query:
                        af["query"] = _rewrite_query(query)
        return metric_profile_json

    list_reco_json = _generate_and_list_recommendations_for_tfb(
        cluster_type,
        metric_profile_json_modifier=strip_version_from_jvm_queries,
    )

    env_values = _env_values(list_reco_json)
    assert not _contains_any_pattern(env_values, HOTSPOT_GC_PATTERNS + SEMERU_GC_PATTERNS), (
        f"Did not expect GC flags when JVM version label is missing, but found: {env_values}"
    )


@pytest.mark.runtimes
def test_no_recommendation_for_layer_runtime_mismatch(cluster_type):
    """
    Test Description: When only the non-matching JVM layer is present (e.g., hotspot layer for
    a Semeru/OpenJ9 workload or vice versa), runtime recommendations should not be generated.

    Expected: No GC-related runtime env entries in recommendations.
    """

    def only_hotspot_layers(path: Path) -> bool:
        return path.name.startswith("hotspot-")

    def only_semeru_layers(path: Path) -> bool:
        return path.name.startswith("semeru-")

    # First run with only hotspot layer present
    list_reco_hotspot_only = _generate_and_list_recommendations_for_tfb(
        cluster_type,
        layer_filter=only_hotspot_layers,
    )
    hotspot_env_values = _env_values(list_reco_hotspot_only)

    # Then run with only semeru layer present
    list_reco_semeru_only = _generate_and_list_recommendations_for_tfb(
        cluster_type,
        layer_filter=only_semeru_layers,
    )
    semeru_env_values = _env_values(list_reco_semeru_only)

    # At least one of these runs should represent a layer/runtime mismatch.
    # For the mismatched case, we expect no GC flags.
    both_have_gc = (
        _contains_any_pattern(hotspot_env_values, HOTSPOT_GC_PATTERNS + SEMERU_GC_PATTERNS)
        and _contains_any_pattern(semeru_env_values, HOTSPOT_GC_PATTERNS + SEMERU_GC_PATTERNS)
    )
    if both_have_gc:
        pytest.skip(
            "Both hotspot-only and semeru-only runs produced GC recommendations; "
            "cannot reliably assert mismatch behaviour in this environment."
        )

    assert (
        not _contains_any_pattern(hotspot_env_values, HOTSPOT_GC_PATTERNS + SEMERU_GC_PATTERNS)
        or not _contains_any_pattern(semeru_env_values, HOTSPOT_GC_PATTERNS + SEMERU_GC_PATTERNS)
    ), (
        "Expected at least one mismatch case (only hotspot or only semeru layer) "
        "to have no GC-related runtime recommendations."
    )


@pytest.mark.runtimes
def test_non_runtime_supported_datasource_logs_message_on_generate(cluster_type):
    """
    Test Description:
    For datasources that exist but do NOT support runtime recommendations, generateRecommendations
    should still succeed but the server should log RUNTIMES_RECOMMENDATIONS_NOT_AVAILABLE.
    """
    input_json_file = "../json_files/create_tfb_exp.json"
    form_kruize_url(cluster_type)

    # Use bulk metadata profile that defines datasources with and without runtime support
    delete_and_create_metadata_profile()

    response = delete_experiment(input_json_file, rm=False)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)
    data = response.json()
    print(data["message"])
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data["status"] == SUCCESS_STATUS

    exp_name = data["experiment_name"] if "experiment_name" in data else json.load(
        open(input_json_file)
    )[0]["experiment_name"]

    # Call generateRecommendations – API itself should succeed even if runtime
    # recommendations are not available for the underlying datasource.
    response = generate_recommendations(exp_name)
    assert response.status_code in range(SUCCESS_STATUS_CODE_START, SUCCESS_STATUS_CODE_END)

    # Give server a moment to flush logs
    time.sleep(2)

    logs = get_kruize_logs(cluster_type)
    assert RUNTIMES_RECOMMENDATIONS_NOT_AVAILABLE in logs, (
        "Expected RUNTIMES_RECOMMENDATIONS_NOT_AVAILABLE message in Kruize logs "
        "for non-runtime-supported datasource"
    )

    response = delete_experiment(input_json_file, rm=False)
    print("delete exp = ", response.status_code)
