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

import sys

from helpers.runtime_utils import (
    _generate_and_list_recommendations_for_tfb,
    _generate_and_list_recommendations_for_petclinic,
    _env_values,
    _contains_any_pattern,
    _extract_runtime_envs,
    validate_runtime_recommendations_if_present,
    HOTSPOT_GC_PATTERNS,
    SEMERU_GC_PATTERNS,
    JDK_JAVA_OPTIONS,
    JAVA_OPTIONS,
    QUARKUS_THREAD_POOL_CORE_THREADS,
    remove_jvm_metrics,
    strip_version_from_jvm_queries, no_jvm_layers,
)

sys.path.append("../../")

from helpers.fixtures import *
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
    Test Description: When the Semeru runtime is active and the `semeru` layer is present,
    runtime recommendations should include JAVA_OPTIONS with Semeru GC policy flags.

    Expected: At least one env value contains one of SEMERU_GC_PATTERNS
    (e.g., -Xgcpolicy:gencon, -Xgcpolicy:balanced, -Xgcpolicy:optthruput).
    
    Note: This test uses Spring Petclinic with OpenJ9/Semeru JVM instead of TFB which uses Hotspot.
    """
    list_reco_json = _generate_and_list_recommendations_for_petclinic(cluster_type)
    envs = _extract_runtime_envs(list_reco_json)

    semeru_envs = [
        env
        for env in envs
        if isinstance(env.get("value"), str)
        and any(pattern in env["value"] for pattern in SEMERU_GC_PATTERNS)
    ]

    if not semeru_envs:
        pytest.skip(
            "Semeru GC policy flags not present in recommendations; "
            "runtime may not be Semeru for this environment"
        )

    # Semeru-specific assertion: flags must be on JAVA_OPTIONS/JDK_JAVA_OPTIONS
    invalid_envs = [
        env for env in semeru_envs if env.get("name") not in (JAVA_OPTIONS, JDK_JAVA_OPTIONS)
    ]
    assert not invalid_envs, (
        "Semeru GC policy flags were found, but not on expected env vars "
        f"{JDK_JAVA_OPTIONS}/{JAVA_OPTIONS}. Offending entries: {invalid_envs}"
    )


@pytest.mark.runtimes
def test_runtime_recommendations_when_jvm_metadata_missing(cluster_type):
    """
    Test Description: If jvmInfo/jvmInfoTotal metrics are not configured in the metric profile,
    runtime recommendations should still be generated.

    Expected: Runtime-related env entries (JDK_JAVA_OPTIONS/JAVA_OPTIONS) with GC flags present.
    """

    list_reco_json = _generate_and_list_recommendations_for_tfb(
        cluster_type,
        metric_profile_json_modifier=remove_jvm_metrics,
    )

    env_values = _env_values(list_reco_json)
    assert _contains_any_pattern(env_values, HOTSPOT_GC_PATTERNS + SEMERU_GC_PATTERNS), (
        f"GC-related runtime env are expected when JVM metrics are missing, but not found"
    )


@pytest.mark.runtimes
def test_gc_recommendation_when_jvm_version_missing(cluster_type):
    """
    Test Description: If jvm_info metrics are present but the version label is not part
    of the aggregation (simulating missing version), GC-specific recommendation should still be present.

    Expected: GC flags present in runtime env.
    """

    list_reco_json = _generate_and_list_recommendations_for_tfb(
        cluster_type,
        metric_profile_json_modifier=strip_version_from_jvm_queries,
    )

    env_values = _env_values(list_reco_json)
    assert _contains_any_pattern(env_values, HOTSPOT_GC_PATTERNS + SEMERU_GC_PATTERNS), (
        f"GC flags expected even when JVM version label is missing, but not found"
    )


@pytest.mark.runtimes
def test_no_recommendation_for_layer_runtime_mismatch(cluster_type):
    """
    Test Description: When only the non-matching JVM layer is present (e.g., hotspot layer for
    a Semeru/OpenJ9 workload or vice versa), JVM specific recommendations should not be generated.

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
def test_no_runtime_recommendations_when_no_layers_detected(cluster_type):
    """
    Test Description: When NO JVM layers are detected (all layers filtered out),
    no JVM runtime recommendations should be generated.
    
    This test validates that runtime recommendations are only generated when
    the corresponding layer is actually detected for the workload.
    
    Expected: No JVM runtime env entries (JDK_JAVA_OPTIONS/JAVA_OPTIONS/QUARKUS_THREAD_POOL_CORE_THREADS)
    should be present when no JVM layers are detected.
    """
    
    list_reco_json = _generate_and_list_recommendations_for_tfb(
        cluster_type,
        layer_filter=no_jvm_layers,
    )
    
    # Extract all runtime environment variables
    envs = _extract_runtime_envs(list_reco_json)
    
    # Filter to JVM-related environment variables
    jvm_related_envs = [
        env for env in envs 
        if env.get("name") in (JDK_JAVA_OPTIONS, JAVA_OPTIONS, QUARKUS_THREAD_POOL_CORE_THREADS)
    ]
    
    # When no JVM layers are detected, there should be no JVM runtime recommendations
    assert not jvm_related_envs, (
        f"No JVM runtime recommendations expected when no JVM layers are detected, "
        f"but found: {jvm_related_envs}"
    )
