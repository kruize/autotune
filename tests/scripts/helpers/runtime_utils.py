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
import os
import shutil
import tempfile
import time
from pathlib import Path

from helpers.kruize import *
from helpers.kruize import delete_layer_from_db
from helpers.list_metadata_json_validate import *
from helpers.list_reco_json_local_monitoring_schema import list_reco_json_local_monitoring_schema
from helpers.list_reco_json_validate import validate_list_reco_json
from helpers.utils import (
    SUCCESS_STATUS_CODE,
    SUCCESS_200_STATUS_CODE,
    SUCCESS_STATUS,
    ERROR_409_STATUS_CODE,
    CREATE_LAYER_SUCCESS_MSG,
    CREATE_EXP_SUCCESS_MSG,
    CONTAINER_EXPERIMENT_TYPE,
    clone_repo,
    benchmarks_install,
    get_metric_profile_dir,
    get_layer_dir,
    get_metadata_profile_dir,
    validate_local_monitoring_recommendation_data_present,
)

metric_profile_dir = get_metric_profile_dir()
metadata_profile_dir = get_metadata_profile_dir()
layer_dir = get_layer_dir()

RUNTIMES_RECOMMENDATIONS_NOT_AVAILABLE = "Runtimes recommendations are unavailable for the provided datasource."
RUNTIMES_RECOMMENDATIONS_AVAILABLE = "Runtimes Recommendations Available"

# Kruize Recommendations Notification codes
NOTIFICATION_CODE_FOR_RUNTIMES_RECOMMENDATIONS_AVAILABLE = "112104"

# Expected env names for JVM runtime recommendations
JDK_JAVA_OPTIONS = "JDK_JAVA_OPTIONS"
JAVA_OPTIONS = "JAVA_OPTIONS"
QUARKUS_THREAD_POOL_CORE_THREADS = "QUARKUS_THREAD_POOL_CORE_THREADS"

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

EXPECTED_ENV_VALUES = (
            JDK_JAVA_OPTIONS,
            JAVA_OPTIONS,
            QUARKUS_THREAD_POOL_CORE_THREADS,
            )

def _has_runtime_env_value(value):
    """
    Check if an environment variable value contains GC-related JVM options.
    
    This function validates whether a given string contains any of the known
    Garbage Collection (GC) flags for either Hotspot JVM or Semeru/OpenJ9 JVM.
    
    Args:
        value: The environment variable value to check. Expected to be a string
               containing JVM options.
    
    Returns:
        bool: True if the value contains any GC-related pattern from either
              HOTSPOT_GC_PATTERNS or SEMERU_GC_PATTERNS, False otherwise.
              Returns False if value is None, empty, or not a string.
    
    Examples:
        >>> _has_runtime_env_value("-XX:+UseG1GC -Xmx512m")
        True
        >>> _has_runtime_env_value("-Xgcpolicy:gencon")
        True
        >>> _has_runtime_env_value("-Xmx512m")
        False
    """
    if not value or not isinstance(value, str):
        return False
    for pattern in HOTSPOT_GC_PATTERNS + SEMERU_GC_PATTERNS:
        if pattern in value:
            return True
    return False


def validate_runtime_recommendations_if_present(recommendations_json):
    """
    Validate runtime recommendations when present in the recommendations JSON.
    
    This function performs comprehensive validation of runtime recommendations,
    ensuring that:
    1. Runtime recommendations appear as environment variable entries
       (JDK_JAVA_OPTIONS, JAVA_OPTIONS, or QUARKUS_THREAD_POOL_CORE_THREADS)
    2. GC flags are present in JAVA options
    3. Notification codes and messages are correctly set in both cost and
       performance recommendation engines
    4. Environment variable values are non-empty and contain expected patterns
    5. When runtime notification is present, at least one runtime env must exist
    
    The function traverses the recommendations JSON structure through:
    kubernetes_objects -> containers -> recommendations -> data ->
    recommendation_terms -> recommendation_engines -> config -> env
    
    Args:
        recommendations_json (list): A list containing recommendation objects.
                                    Expected to have at least one element with
                                    experiment_type, kubernetes_objects, etc.
    
    Returns:
        None: This function performs assertions and validates all runtime
              recommendations without returning early.
    
    Raises:
        AssertionError: If any validation check fails, including:
            - Missing runtime notification codes
            - Incorrect notification messages
            - Invalid environment variable names
            - Empty environment variable values
            - Missing GC flags in JAVA options
            - Runtime notification present but no runtime env variables found
    
    Note:
        - Only validates container experiment types
        - Returns early without error if recommendations_json is empty or None
        - Returns early if no kubernetes_objects are present
        - Validates ALL runtime recommendations, not just the first one found
        - Fails if runtime notification is present but no runtime envs are found
    """
    if not recommendations_json or len(recommendations_json) == 0:
        return

    rec = recommendations_json[0]
    if rec.get("experiment_type") != CONTAINER_EXPERIMENT_TYPE:
        return

    kubernetes_objects = rec.get("kubernetes_objects", [])
    if not kubernetes_objects:
        return

    has_runtime_notification = False
    runtime_env_vars_found = []

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
                        if NOTIFICATION_CODE_FOR_RUNTIMES_RECOMMENDATIONS_AVAILABLE in cost_notifications:
                            has_runtime_notification = True
                            assert cost_notifications[NOTIFICATION_CODE_FOR_RUNTIMES_RECOMMENDATIONS_AVAILABLE].get("message") == RUNTIMES_RECOMMENDATIONS_AVAILABLE, \
                                f"Runtime recommendations notification message mismatch in cost engine notifications"

                    # Check performance engine has runtime notification (code and message)
                    if "performance" in engines:
                        perf_notifications = engines["performance"].get("notifications", {})
                        if NOTIFICATION_CODE_FOR_RUNTIMES_RECOMMENDATIONS_AVAILABLE in perf_notifications:
                            has_runtime_notification = True
                            assert perf_notifications[NOTIFICATION_CODE_FOR_RUNTIMES_RECOMMENDATIONS_AVAILABLE].get("message") == RUNTIMES_RECOMMENDATIONS_AVAILABLE, \
                                f"Runtime recommendations notification message mismatch in performance engine notifications"
                    
                    for _engine_name, engine_obj in engines.items():
                        config = engine_obj.get("config", {})
                        env_list = config.get("env")
                        if not env_list or not isinstance(env_list, list):
                            continue

                        # verify all expected env vars are present in the config
                        if env_list:
                            found_env_names = {env_item.get("name") for env_item in env_list}
                            missing_env_vars = set(EXPECTED_ENV_VALUES) - found_env_names
                            assert not missing_env_vars, (
                                f"Expected runtime environment variables are missing from config: "
                                f"{', '.join(sorted(missing_env_vars))}. "
                                f"Found: {', '.join(sorted(found_env_names))}"
                            )

                        for env_item in env_list:
                            name = env_item.get("name")
                            value = env_item.get("value")

                            # GC flags are only expected in JAVA options, not in thread-pool tunables
                            if name in (JDK_JAVA_OPTIONS, JAVA_OPTIONS):
                                assert value, f"Runtime env {name} has empty value"
                                assert _has_runtime_env_value(value), (
                                    f"Runtime env {name} should contain GC flags, got: {value}"
                                )
                                runtime_env_vars_found.append(name)

                            if name == QUARKUS_THREAD_POOL_CORE_THREADS:
                                # For thread-pool tunables, ensure we have a non-empty value
                                assert value, f"Runtime env {name} has empty value"
                                runtime_env_vars_found.append(name)

    # If runtime notification is present, we must have at least one runtime env variable
    if has_runtime_notification:
        assert runtime_env_vars_found, (
            f"Runtime recommendations notification code {NOTIFICATION_CODE_FOR_RUNTIMES_RECOMMENDATIONS_AVAILABLE} "
            f"is present, but no runtime environment variables ({JDK_JAVA_OPTIONS}, {JAVA_OPTIONS}, "
            f"{QUARKUS_THREAD_POOL_CORE_THREADS}) were found in the recommendations"
        )


def _generate_and_list_recommendations_for_tfb(
    cluster_type,
    *,
    metric_profile_json_modifier=None,
    metadata_profile_filename="cluster_metadata_local_monitoring.json",
    metadata_profile_json_modifier=None,
    layer_filter=None,
):
    """
    Generate and list recommendations for TechEmpower Framework Benchmarks (TFB).
    
    This is a comprehensive end-to-end test helper function that orchestrates the
    complete workflow for generating runtime recommendations for TechEmpower
    Quarkus JVM workload. The workflow includes:
    
    1. Clone the benchmarks repository
    2. Install the workload
    3. Create and configure metric profiles (with JVM runtime metrics)
    4. Create and configure metadata profiles
    5. Create runtime layers (Hotspot, Semeru, Quarkus, etc.)
    6. Create experiment using TFB configuration
    7. Generate recommendations
    8. List and validate recommendations
    9. Clean up all created resources
    
    Args:
        cluster_type (str): The type of cluster to test against. Supported values:
                           - "minikube": Uses local monitoring without recording rules
                           - Other values: Uses standard local monitoring with thanos-1
        metric_profile_json_modifier (callable, optional): A function that takes
                                                          the metric profile JSON
                                                          and returns a modified version.
                                                          Used for test-specific customization.
        metadata_profile_filename (str, optional): Name of the metadata profile JSON
                                                  file to use. Defaults to
                                                  "cluster_metadata_local_monitoring.json"
        metadata_profile_json_modifier (callable, optional): A function that takes
                                                            the metadata profile JSON
                                                            and returns a modified version.
        layer_filter (callable, optional): A function that takes a Path object and
                                          returns True/False to filter which layer
                                          JSON files to create. If None, all layers
                                          are created.
    
    Returns:
        dict: The parsed listRecommendations JSON response containing the generated
              recommendations with runtime tuning parameters.
    
    Raises:
        AssertionError: If any step in the workflow fails, including:
            - Profile creation failures
            - Layer creation failures
            - Experiment creation failures
            - Recommendation generation failures
            - JSON schema validation failures
    
    Note:
        - Uses temporary files for modified profiles to avoid affecting original files
        - Automatically cleans up all resources in the finally block
        - Deletes experiments, profiles, layers, and temporary files
        - Removes the cloned benchmarks directory
        - For non-minikube clusters, updates datasource from prometheus-1 to thanos-1
    
    Example:
        >>> def filter_hotspot_only(path):
        ...     return "hotspot" in path.name.lower()
        >>> recommendations = _generate_and_list_recommendations_for_tfb(
        ...     "minikube",
        ...     layer_filter=filter_hotspot_only
        ... )
    """
    input_json_path = (Path(__file__).resolve().parents[1]/ "local_monitoring_tests"/ "json_files"/ "create_tfb_exp.json")
    input_json_file = str(input_json_path)
    with open(input_json_path) as f:
        input_json = json.load(f)

    temp_input_json_file = None
    temp_metric_profile_file = None
    temp_metadata_profile_file = None

    form_kruize_url(cluster_type)

    # Install metric profile
    metric_profile_json_file = metric_profile_dir / "resource_optimization_local_monitoring.json"
    if cluster_type == "openshift":
        # Update datasource from prometheus-1 to thanos-1 before using in the test
        for exp in input_json:
            if exp.get("datasource") == "prometheus-1":
                exp["datasource"] = "thanos-1"
                break
        with tempfile.NamedTemporaryFile(mode="w", suffix=".json", delete=False) as tf:
            json.dump(input_json, tf, indent=2)
            temp_input_json_file = tf.name
            input_json_file = temp_input_json_file

    # Point the metric profile JSON to a temp file to avoid using the original one
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

    created_layer_names = []
    for layer_input_json_file in layer_json_files:
        with open(layer_input_json_file, "r") as json_file:
            layer_json = json.load(json_file)
            layer_name = layer_json["layer_name"]

        response = create_layer(layer_input_json_file)
        data = response.json()

        # Handle both success (201) and conflict (409 - layer already exists)
        if response.status_code == SUCCESS_STATUS_CODE:
            assert data["status"] == SUCCESS_STATUS
            assert data["message"] == CREATE_LAYER_SUCCESS_MSG % layer_name
            created_layer_names.append(layer_name)
            print(f"✓ Layer '{layer_name}' created successfully")
        elif response.status_code == ERROR_409_STATUS_CODE:
            # Layer already exists, log and continue
            print(f"ℹ Layer '{layer_name}' already exists, skipping creation")
            created_layer_names.append(layer_name)
        else:
            # Unexpected status code, fail the test
            assert False, f"Unexpected status code {response.status_code} for layer '{layer_name}': {data}"

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

        # Delete metadata profile to avoid cross-test interference
        response = delete_metadata_profile(metadata_profile_name)
        print("delete metadata profile = ", response.status_code)

        # Delete created layers to avoid accumulated state
        for layer_name in created_layer_names:
            delete_layer_from_db(layer_name)
            print(f"delete layer '{layer_name}' = done")

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

def _generate_and_list_recommendations_for_petclinic(
    cluster_type,
    *,
    metric_profile_json_modifier=None,
    metadata_profile_filename="cluster_metadata_local_monitoring.json",
    metadata_profile_json_modifier=None,
    layer_filter=None,
):
    """
    Generate and list recommendations for Spring Petclinic (OpenJ9/Semeru runtime).
    
    This is a comprehensive end-to-end test helper function that orchestrates the
    complete workflow for generating runtime recommendations for Spring Petclinic
    with OpenJ9/Semeru JVM workload. The workflow includes:
    
    1. Clone the benchmarks repository
    2. Install the workload
    3. Create and configure metric profiles (with JVM runtime metrics)
    4. Create and configure metadata profiles
    5. Create runtime layers (Hotspot, Semeru, Quarkus, etc.)
    6. Create experiment using Petclinic configuration
    7. Generate recommendations
    8. List and validate recommendations
    9. Clean up all created resources
    
    Args:
        cluster_type (str): The type of cluster to test against. Supported values:
                           - "minikube": Uses local monitoring without recording rules
                           - Other values: Uses standard local monitoring with thanos-1
        metric_profile_json_modifier (callable, optional): A function that takes
                                                          the metric profile JSON
                                                          and returns a modified version.
                                                          Used for test-specific customization.
        metadata_profile_filename (str, optional): Name of the metadata profile JSON
                                                  file to use. Defaults to
                                                  "cluster_metadata_local_monitoring.json"
        metadata_profile_json_modifier (callable, optional): A function that takes
                                                            the metadata profile JSON
                                                            and returns a modified version.
        layer_filter (callable, optional): A function that takes a Path object and
                                          returns True/False to filter which layer
                                          JSON files to create. If None, all layers
                                          are created.
    
    Returns:
        dict: The parsed listRecommendations JSON response containing the generated
              recommendations with runtime tuning parameters.
    
    Raises:
        AssertionError: If any step in the workflow fails, including:
            - Profile creation failures
            - Layer creation failures
            - Experiment creation failures
            - Recommendation generation failures
            - JSON schema validation failures
    
    Note:
        - Uses temporary files for modified profiles to avoid affecting original files
        - Automatically cleans up all resources in the finally block
        - Deletes experiments, profiles, layers, and temporary files
        - Removes the cloned benchmarks directory
        - For non-minikube clusters, updates datasource from prometheus-1 to thanos-1
    
    Example:
        >>> def filter_semeru_only(path):
        ...     return "semeru" in path.name.lower()
        >>> recommendations = _generate_and_list_recommendations_for_petclinic(
        ...     "openshift",
        ...     layer_filter=filter_semeru_only
        ... )
    """
    # Wait for petclinic pod to be ready and metrics to be available
    # This is crucial for Semeru layer detection as Prometheus needs time to scrape metrics
    print("Waiting for petclinic pod to be ready and metrics to be scraped by Prometheus...")
    time.sleep(30)  # Wait 30 seconds for pod readiness and initial metric scraping
    print("Wait complete, proceeding with experiment creation...")

    input_json_path = (Path(__file__).resolve().parents[1]/ "local_monitoring_tests"/ "json_files"/ "create_petclinic_exp.json")
    input_json_file = str(input_json_path)
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

    response = delete_metric_profile(metric_profile_json_file)
    print("delete metric profile = ", response.status_code)

    response = create_metric_profile(metric_profile_json_file)
    data = response.json()
    print(data["message"])
    assert response.status_code == SUCCESS_STATUS_CODE
    assert data["status"] == SUCCESS_STATUS

    # Install metadata profile
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

    # Create layers for all JSON files in layer_dir
    layer_json_files = sorted(layer_dir.glob("*.json"))
    if layer_filter is not None:
        layer_json_files = [p for p in layer_json_files if layer_filter(p)]

    created_layer_names = []
    for layer_input_json_file in layer_json_files:
        with open(layer_input_json_file, "r") as json_file:
            layer_json = json.load(json_file)
            layer_name = layer_json["layer_name"]

        response = create_layer(layer_input_json_file)
        data = response.json()

        # Handle both success (201) and conflict (409 - layer already exists)
        if response.status_code == SUCCESS_STATUS_CODE:
            assert data["status"] == SUCCESS_STATUS
            assert data["message"] == CREATE_LAYER_SUCCESS_MSG % layer_name
            created_layer_names.append(layer_name)
            print(f"✓ Layer '{layer_name}' created successfully")
        elif response.status_code == ERROR_409_STATUS_CODE:
            # Layer already exists, log and continue
            print(f"ℹ Layer '{layer_name}' already exists, skipping creation")
            created_layer_names.append(layer_name)
        else:
            # Unexpected status code, fail the test
            assert False, f"Unexpected status code {response.status_code} for layer '{layer_name}': {data}"

    # Create experiment using Spring Petclinic OpenJ9 workload
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

        # Delete metadata profile to avoid cross-test interference
        response = delete_metadata_profile(metadata_profile_name)
        print("delete metadata profile = ", response.status_code)

        # Delete created layers to avoid accumulated state
        for layer_name in created_layer_names:
            delete_layer_from_db(layer_name)
            print(f"delete layer '{layer_name}' = done")

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
    Extract all runtime environment variables from recommendations JSON.
    
    This function traverses the listRecommendations JSON structure and extracts
    all environment variable entries from the config.env section of each
    recommendation engine (cost, performance, etc.) for container experiments.
    
    The traversal path is:
    list_reco_json[0] -> kubernetes_objects -> containers -> recommendations ->
    data -> recommendation_terms -> recommendation_engines -> config -> env
    
    Args:
        list_reco_json (list): The listRecommendations JSON response, expected
                              to be a list with at least one recommendation object.
    
    Returns:
        list: A list of environment variable dictionaries, where each dictionary
              contains 'name' and 'value' keys. Returns an empty list if:
              - list_reco_json is None or empty
              - experiment_type is not CONTAINER_EXPERIMENT_TYPE
              - No environment variables are found
    
    Example:
        >>> reco_json = [{"experiment_type": "container", ...}]
        >>> envs = _extract_runtime_envs(reco_json)
        >>> # Returns: [{"name": "JDK_JAVA_OPTIONS", "value": "-XX:+UseG1GC"}, ...]
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
    """
    Extract environment variable values from recommendations JSON.
    
    This is a convenience function that extracts only the 'value' field from
    all environment variables found in the recommendations JSON. It internally
    uses _extract_runtime_envs() to get the full environment variable objects
    and then extracts just the values.
    
    Args:
        list_reco_json (list): The listRecommendations JSON response.
    
    Returns:
        list: A list of environment variable value strings. Returns an empty
              list if no environment variables are found or if values are missing.
              Empty strings are returned for env entries without a 'value' key.
    
    Example:
        >>> reco_json = [{"experiment_type": "container", ...}]
        >>> values = _env_values(reco_json)
        >>> # Returns: ["-XX:+UseG1GC -Xmx512m", "4", ...]
    """
    return [env.get("value", "") for env in _extract_runtime_envs(list_reco_json)]


def _contains_any_pattern(values, patterns):
    """
    Check if any value in a list contains any of the specified patterns.
    
    This utility function searches through a list of values (typically strings)
    and checks if any of them contain any of the specified patterns. It's used
    to verify the presence of specific JVM flags or configuration patterns in
    environment variable values.
    
    Args:
        values (list): A list of values to search through. Non-string values
                      are skipped.
        patterns (tuple or list): A collection of string patterns to search for.
                                 Each pattern is checked using substring matching.
    
    Returns:
        bool: True if any value contains any of the patterns, False otherwise.
              Returns False if all values are non-strings or if no matches found.
    
    Example:
        >>> values = ["-XX:+UseG1GC -Xmx512m", "some other value"]
        >>> patterns = ("-XX:+UseG1GC", "-XX:+UseZGC")
        >>> _contains_any_pattern(values, patterns)
        True
        >>> _contains_any_pattern(["no match here"], patterns)
        False
    """
    for v in values:
        if not isinstance(v, str):
            continue
        for p in patterns:
            if p in v:
                return True
    return False


def remove_jvm_metrics(metric_profile_json):
    vars_list = metric_profile_json.get("slo", {}).get("function_variables", [])
    filtered = [
        v for v in vars_list
        if v.get("name") not in ("jvmInfo", "jvmInfoTotal")
    ]
    metric_profile_json["slo"]["function_variables"] = filtered
    return metric_profile_json


def strip_version_from_jvm_queries(metric_profile_json):
    def _rewrite_query(q):
        return q.replace(", version", "")

    for var in metric_profile_json.get("slo", {}).get("function_variables", []):
        if var.get("name") in ("jvmInfo", "jvmInfoTotal"):
            for af in var.get("aggregation_functions", []):
                query = af.get("query")
                if isinstance(query, str) and "sum by(" in query and "version" in query:
                    af["query"] = _rewrite_query(query)
    return metric_profile_json


def no_jvm_layers(path: Path) -> bool:
    """Filter out all JVM-related layers (hotspot, semeru, quarkus)"""
    return not (
        path.name.startswith("hotspot-") or
        path.name.startswith("semeru-") or
        path.name.startswith("quarkus-")
    )
