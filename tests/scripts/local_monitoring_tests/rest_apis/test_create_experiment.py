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
import pytest
import sys
sys.path.append("../../")

from helpers.fixtures import *
from helpers.kruize import *
from helpers.utils import *
from jinja2 import Environment, FileSystemLoader

@pytest.mark.sanity
@pytest.mark.parametrize("test_name, expected_status_code, version, experiment_name, cluster_name, performance_profile, metadata_profile, mode, target_cluster, datasource, experiment_type, kubernetes_obj_type, name, namespace, namespace_name, container_image_name, container_name, measurement_duration, threshold",
    [
        ("valid_namespace_exp_with_exp_type", SUCCESS_STATUS_CODE, "v2.0", "tfb-workload-namespace", "default", "resource-optimization-local-monitoring", "cluster-metadata-local-monitoring", "monitor", "local", "prometheus-1", "namespace", None, None, None, "default", None, None, "15min", "0.1"),
        ("valid_container_exp_without_exp_type", SUCCESS_STATUS_CODE, "v2.0", "tfb-workload-namespace", "default", "resource-optimization-local-monitoring", "cluster-metadata-local-monitoring", "monitor", "local", "prometheus-1", None, "deployment", "tfb-qrh-sample", "default", None, "kruize/tfb-qrh:1.13.2.F_et17", "tfb-server", "15min", "0.1"),
        ("valid_container_exp_with_exp_type", SUCCESS_STATUS_CODE, "v2.0", "tfb-workload-namespace", "default", "resource-optimization-local-monitoring", "cluster-metadata-local-monitoring", "monitor", "local", "prometheus-1", "container", "deployment", "tfb-qrh-sample", "default", None, "kruize/tfb-qrh:1.13.2.F_et17", "tfb-server", "15min", "0.1"),
        ("valid_auto_mode_exp_with_exp_type", SUCCESS_STATUS_CODE, "v2.0", "tfb-auto-container-exp", "default", "resource-optimization-local-monitoring", "cluster-metadata-local-monitoring", "auto", "local", "prometheus-1", None, "deployment", "tfb-qrh-sample", "default", None, "kruize/tfb-qrh:1.13.2.F_et17", "tfb-server", "15min", "0.1"),
        ("valid_recreate_mode_exp_with_exp_type", SUCCESS_STATUS_CODE, "v2.0", "tfb-recreate-container-exp", "default", "resource-optimization-local-monitoring", "cluster-metadata-local-monitoring", "recreate", "local", "prometheus-1", "container", "deployment", "tfb-qrh-sample", "default", None, "kruize/tfb-qrh:1.13.2.F_et17", "tfb-server", "15min", "0.1"),
        ("valid_auto_mode_exp_without_exp_type", SUCCESS_STATUS_CODE, "v2.0", "tfb-auto", "default", "resource-optimization-local-monitoring", "cluster-metadata-local-monitoring", "auto", "local", "prometheus-1", None, "deployment", "tfb-qrh-sample", "default", None, "kruize/tfb-qrh:1.13.2.F_et17", "tfb-server", "15min", "0.1"),
        ("valid_recreate_mode_exp_without_exp_type", SUCCESS_STATUS_CODE, "v2.0", "tfb-recreate", "default", "resource-optimization-local-monitoring", "cluster-metadata-local-monitoring", "recreate", "local", "prometheus-1", "container", "deployment", "tfb-qrh-sample", "default", None, "kruize/tfb-qrh:1.13.2.F_et17", "tfb-server", "15min", "0.1")
    ]
)
def test_create_exp_valid_tests(test_name, expected_status_code, version, experiment_name, cluster_name, performance_profile, metadata_profile, mode, target_cluster, datasource, experiment_type, kubernetes_obj_type, name, namespace, namespace_name, container_image_name, container_name, measurement_duration, threshold, cluster_type):
    """
    Test Description: This test validates the response status code of createExperiment API
    for namespace experiment by passing a valid input for the json
    """
    # Generate a temporary JSON filename
    tmp_json_file = "/tmp/create_exp_" + test_name + ".json"
    print("tmp_json_file = ", tmp_json_file)

    # Load the Jinja2 template
    environment = Environment(loader=FileSystemLoader("../json_files/"))
    template = environment.get_template("create_exp_template.json")

    # In case of test_name with "null", strip the specific fields
    if "null" in test_name:
         field = test_name.replace("null_", "")
         json_file = "../json_files/create_exp_template.json"
         filename = "/tmp/create_exp_template.json"
         strip_double_quotes_for_field(json_file, field, filename)
         environment = Environment(loader=FileSystemLoader("/tmp/"))
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
    if json_content[0]["kubernetes_objects"][0]["namespaces"]["namespace"] == "None":
        json_content[0]["kubernetes_objects"][0].pop("namespaces")
    if json_content[0]["experiment_type"] == "None":
        json_content[0].pop("experiment_type")

    # Write the final JSON to the temp file
    with open(tmp_json_file, mode="w", encoding="utf-8") as message:
        json.dump(json_content, message, indent=4)

    input_json_file = tmp_json_file
    form_kruize_url(cluster_type)

    delete_and_create_metadata_profile()

    response = delete_experiment(input_json_file, rm=False)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_EXP_SUCCESS_MSG

    response = delete_experiment(input_json_file, rm=False)
    print("delete exp = ", response.status_code)


@pytest.mark.negative
@pytest.mark.parametrize("test_name, expected_status_code, expected_error_msg, version, experiment_name, cluster_name, performance_profile, metadata_profile, mode, target_cluster, datasource, experiment_type, kubernetes_obj_type, name, namespace, namespace_name, container_image_name, container_name, measurement_duration, threshold",
    [
        ("invalid_namespace_exp_without_exp_type", ERROR_STATUS_CODE, CREATE_EXP_CONTAINER_EXP_CONTAINS_NAMESPACE, "v2.0", "tfb-workload-namespace", "default", "resource-optimization-local-monitoring", "cluster-metadata-local-monitoring", "monitor", "local", "prometheus-1", None, None, None, None, "default", None, None, "15min", "0.1"),
        ("invalid_both_container_and_namespace_without_exp_type", ERROR_STATUS_CODE, CREATE_EXP_CONTAINER_EXP_CONTAINS_NAMESPACE,"v2.0", "tfb-workload-namespace", "default", "resource-optimization-local-monitoring", "cluster-metadata-local-monitoring", "monitor", "local", "prometheus-1", None, "deployment", "tfb-qrh-sample", "default", "default", "kruize/tfb-qrh:1.13.2.F_et17", "tfb-server", "15min", "0.1"),
        ("invalid_both_container_and_namespace_namespace_exp_type", ERROR_STATUS_CODE, CREATE_EXP_NAMESPACE_EXP_CONTAINS_CONTAINER,"v2.0", "tfb-workload-namespace", "default", "resource-optimization-local-monitoring", "cluster-metadata-local-monitoring", "monitor", "local", "prometheus-1", "namespace", "deployment", "tfb-qrh-sample", "default", "default", "kruize/tfb-qrh:1.13.2.F_et17", "tfb-server", "15min", "0.1"),
        ("invalid_both_container_and_namespace_container_exp_type", ERROR_STATUS_CODE, CREATE_EXP_CONTAINER_EXP_CONTAINS_NAMESPACE,"v2.0", "tfb-workload-namespace", "default", "resource-optimization-local-monitoring", "cluster-metadata-local-monitoring", "monitor", "local", "prometheus-1", "container", "deployment", "tfb-qrh-sample", "default", "default", "kruize/tfb-qrh:1.13.2.F_et17", "tfb-server", "15min", "0.1"),
        ("invalid_namespace_exp_type_with_only_containers", ERROR_STATUS_CODE, CREATE_EXP_NAMESPACE_EXP_CONTAINS_CONTAINER,"v2.0", "tfb-workload-namespace", "default", "resource-optimization-local-monitoring", "cluster-metadata-local-monitoring", "monitor", "local", "prometheus-1", "namespace", "deployment", "tfb-qrh-sample", "default", None, "kruize/tfb-qrh:1.13.2.F_et17", "tfb-server", "15min", "0.1"),
        ("invalid_container_exp_type_with_only_namespace", ERROR_STATUS_CODE, CREATE_EXP_CONTAINER_EXP_CONTAINS_NAMESPACE,"v2.0", "tfb-workload-namespace", "default", "resource-optimization-local-monitoring", "cluster-metadata-local-monitoring", "monitor", "local", "prometheus-1", "container", None, None, None, "default", None, None, "15min", "0.1"),
        ("invalid_namespace_exp_with_auto_mode", ERROR_STATUS_CODE, CREATE_EXP_NAMESPACE_EXP_NOT_SUPPORTED_FOR_VPA_MODE,"v2.0", "tfb-workload-namespace-auto", "default", "resource-optimization-local-monitoring", "cluster-metadata-local-monitoring", "auto", "local", "prometheus-1", "namespace", None, None, None, "default", None, None, "15min", "0.1"),
        ("invalid_namespace_exp_with_recreate_mode", ERROR_STATUS_CODE, CREATE_EXP_NAMESPACE_EXP_NOT_SUPPORTED_FOR_VPA_MODE,"v2.0", "tfb-workload-namespace-recreate", "default", "resource-optimization-local-monitoring", "cluster-metadata-local-monitoring", "recreate", "local", "prometheus-1", "namespace", None, None, None, "default", None, None, "15min", "0.1"),
        ("invalid_auto_mode_exp_with_exp_type_remote_cluster", ERROR_STATUS_CODE, CREATE_EXP_VPA_NOT_SUPPORTED_FOR_REMOTE, "v2.0", "tfb-auto-container-exp-remote", "default", "resource-optimization-local-monitoring", "cluster-metadata-local-monitoring", "auto", "remote", "prometheus-1", None, "deployment", "tfb-qrh-sample", "default", None, "kruize/tfb-qrh:1.13.2.F_et17", "tfb-server", "15min", "0.1"),
        ("invalid_recreate_mode_exp_with_exp_type_remote_cluster", ERROR_STATUS_CODE, CREATE_EXP_VPA_NOT_SUPPORTED_FOR_REMOTE, "v2.0", "tfb-recreate-container-exp-remote", "default", "resource-optimization-local-monitoring", "cluster-metadata-local-monitoring", "recreate", "remote", "prometheus-1", "container", "deployment", "tfb-qrh-sample", "default", None, "kruize/tfb-qrh:1.13.2.F_et17", "tfb-server", "15min", "0.1"),
        ("invalid_auto_mode_exp_without_exp_type_remote_cluster", ERROR_STATUS_CODE, CREATE_EXP_VPA_NOT_SUPPORTED_FOR_REMOTE, "v2.0", "tfb-auto-remote", "default", "resource-optimization-local-monitoring", "cluster-metadata-local-monitoring", "auto", "remote", "prometheus-1", None, "deployment", "tfb-qrh-sample", "default", None, "kruize/tfb-qrh:1.13.2.F_et17", "tfb-server", "15min", "0.1"),
        ("invalid_recreate_mode_exp_without_exp_type_remote_cluster", ERROR_STATUS_CODE, CREATE_EXP_VPA_NOT_SUPPORTED_FOR_REMOTE, "v2.0", "tfb-recreate-remote", "default", "resource-optimization-local-monitoring", "cluster-metadata-local-monitoring", "recreate", "remote", "prometheus-1", "container", "deployment", "tfb-qrh-sample", "default", None, "kruize/tfb-qrh:1.13.2.F_et17", "tfb-server", "15min", "0.1"),
        ("invalid_auto_mode_with_unsupported_object_type", ERROR_STATUS_CODE, CREATE_EXP_INVALID_KUBERNETES_OBJECT_FOR_VPA, "v2.0", "tfb-auto-invalid-object", "default", "resource-optimization-local-monitoring", "cluster-metadata-local-monitoring", "auto", "local", "prometheus-1", "container", "statefulset", "tfb-qrh-sample", "default", None, "kruize/tfb-qrh:1.13.2.F_et17", "tfb-server", "15min", "0.1"),
        ("invalid_recreate_mode_with_unsupported_object_type", ERROR_STATUS_CODE, CREATE_EXP_INVALID_KUBERNETES_OBJECT_FOR_VPA, "v2.0", "tfb-recreate-invalid-object", "default", "resource-optimization-local-monitoring", "cluster-metadata-local-monitoring", "recreate", "local", "prometheus-1", "container", "job", "tfb-qrh-sample", "default", None, "kruize/tfb-qrh:1.13.2.F_et17", "tfb-server", "15min", "0.1"),
        ("invalid_datasource_name", ERROR_STATUS_CODE, CREATE_EXP_INVALID_DATASOURCE % "invalid-ds", "v2.0", "tfb-create-exp-invalid-ds", "default", "resource-optimization-local-monitoring", "cluster-metadata-local-monitoring", "monitor", "local", "invalid-ds", "container", "deployment", "tfb-qrh-sample", "default", "None", "kruize/tfb-qrh:1.13.2.F_et17", "tfb-server", "15min", "0.1")
    ]
)
def test_create_exp_invalid_tests(test_name, expected_status_code, expected_error_msg, version, experiment_name, cluster_name, performance_profile, metadata_profile, mode, target_cluster, datasource, experiment_type, kubernetes_obj_type, name, namespace, namespace_name, container_image_name, container_name, measurement_duration, threshold, cluster_type):
    """
    Test Description: This test validates the response status code of createExperiment API
    for namespace experiment by passing a valid input for the json
    """
    # Generate a temporary JSON filename
    tmp_json_file = "/tmp/create_exp_" + test_name + ".json"
    print("tmp_json_file = ", tmp_json_file)

    # Load the Jinja2 template
    environment = Environment(loader=FileSystemLoader("../json_files/"))
    template = environment.get_template("create_exp_template.json")

    # In case of test_name with "null", strip the specific fields
    if "null" in test_name:
         field = test_name.replace("null_", "")
         json_file = "../json_files/create_exp_template.json"
         filename = "/tmp/create_exp_template.json"
         strip_double_quotes_for_field(json_file, field, filename)
         environment = Environment(loader=FileSystemLoader("/tmp/"))
         template = environment.get_template("create_exp_template.json")

    # Render the JSON content from the template
    content = template.render(
        version=version,
        experiment_name=experiment_name,
        cluster_name=cluster_name,
        performance_profile=performance_profile,
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
    if json_content[0]["kubernetes_objects"][0]["namespaces"]["namespace"] == "None":
            json_content[0]["kubernetes_objects"][0].pop("namespaces")
    if json_content[0]["experiment_type"] == "None":
        json_content[0].pop("experiment_type")

    # Write the final JSON to the temp file
    with open(tmp_json_file, mode="w", encoding="utf-8") as message:
        json.dump(json_content, message, indent=4)

    input_json_file = tmp_json_file
    form_kruize_url(cluster_type)

    delete_and_create_metadata_profile()

    response = delete_experiment(input_json_file, rm=False)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == expected_status_code
    assert data['status'] == ERROR_STATUS
    assert data['message'] == expected_error_msg

    response = delete_experiment(input_json_file, rm=False)
    print("delete exp = ", response.status_code)


@pytest.mark.negative
def test_create_multiple_namespace_exp(cluster_type):
    """
    Test Description: This test validates the response status code of createExperiment API
    if multiple entries are presnet in create experiment json
    """
    input_json_file = "../json_files/create_multiple_namespace_exp.json"
    form_kruize_url(cluster_type)

    delete_and_create_metadata_profile()

    response = delete_experiment(input_json_file, rm=False)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == ERROR_STATUS_CODE
    assert data['status'] == ERROR_STATUS
    # validate error message
    assert data['message'] == CREATE_EXP_BULK_ERROR_MSG

    response = delete_experiment(input_json_file, rm=False)
    print("delete exp = ", response.status_code)


@pytest.mark.skip(reason="This will be enabled once the layer presence logic is implemented")
@pytest.mark.sanity
def test_non_runtime_supported_datasource_logs_message(cluster_type):
    """
    Test Description:
    Creating an experiment with a datasource that exists BUT does not support
    runtime recommendations should NOT fail the API, but the server should log
    'RUNTIMES_RECOMMENDATIONS_NOT_AVAILABLE'.
    """
    input_json_file = "../json_files/create_tfb_exp.json"
    form_kruize_url(cluster_type)

    delete_and_create_metadata_profile()

    response = delete_experiment(input_json_file, rm=False)
    print("delete exp = ", response.status_code)

    # Create experiment using the specified json
    response = create_experiment(input_json_file)

    data = response.json()
    print(data['message'])

    # Give server 1–2 seconds to flush logs
    time.sleep(2)

    # Fetch logs from kruize pod (your helper may differ)
    logs = get_kruize_logs(cluster_type)

    assert RUNTIMES_RECOMMENDATIONS_NOT_AVAILABLE in logs, \
        "Expected log message not found when using non-runtime-supported datasource"

# ===========================================================================
# flex + stability term/model validation tests
# ===========================================================================

def _flex_build_exp_json(experiment_name, terms, models):
    """Render the experiment template and return parsed JSON."""
    environment = Environment(loader=FileSystemLoader("../json_files/"))
    template    = environment.get_template("create_exp_template.json")
    content = template.render(
        version="v2.0",
        experiment_name=experiment_name,
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


def _flex_setup(cluster_type, experiment_name, terms, models):
    """Write temp file, setup metadata profile, clean up stale experiment."""
    json_content = _flex_build_exp_json(experiment_name, terms, models)
    tmp_file = f"/tmp/create_exp_flex_stab_{experiment_name}.json"
    with open(tmp_file, "w", encoding="utf-8") as f:
        json.dump(json_content, f, indent=4)
    form_kruize_url(cluster_type)
    delete_and_create_metadata_profile()
    delete_experiment(tmp_file, rm=False)
    return tmp_file


@pytest.mark.sanity
@pytest.mark.parametrize("test_name, terms, models", [
    ("flex_only_no_model",               ["flex"],                              None),
    ("stability_only_no_term",           None,                                  ["stability"]),
    ("flex_and_stability_explicit",      ["flex"],                              ["stability"]),
    ("stability_cost_no_term",           None,                                  ["stability", "cost"]),
    ("stability_perf_no_term",           None,                                  ["stability", "performance"]),
    ("all_models_no_term",               None,                                  ["stability", "cost", "performance"]),
    ("flex_short_no_model",              ["flex", "short"],                     None),
    ("flex_short_medium_no_model",       ["flex", "short", "medium"],           None),
    ("flex_all_terms_no_model",          ["flex", "short", "medium", "long"],   None),
    ("short_no_model",                   ["short"],                             None),
    ("medium_no_model",                  ["medium"],                            None),
    ("long_no_model",                    ["long"],                              None),
    ("short_cost",                       ["short"],                             ["cost"]),
    ("short_performance",                ["short"],                             ["performance"]),
    ("medium_cost",                      ["medium"],                            ["cost"]),
    ("long_performance",                 ["long"],                              ["performance"]),
    ("no_terms_no_models",               None,                                  None),
])
def test_create_exp_valid_term_model(test_name, terms, models, cluster_type):
    """Valid term/model combinations must be accepted with HTTP 201."""
    exp_name = f"flex-stab-valid-{test_name}"
    tmp_file = _flex_setup(cluster_type, exp_name, terms, models)
    try:
        response = create_experiment(tmp_file)
        data = response.json()
        print(f"[{test_name}] {response.status_code} — {data.get('message')}")
        assert response.status_code == SUCCESS_STATUS_CODE, \
            f"[{test_name}] Expected 201, got {response.status_code}: {data.get('message')}"
        assert data["status"] == SUCCESS_STATUS
        assert data["message"] == CREATE_EXP_SUCCESS_MSG
    finally:
        delete_experiment(tmp_file, rm=False)


@pytest.mark.negative
@pytest.mark.parametrize("test_name, terms, models, expected_error", [
    ("flex_short_with_stability",
     ["flex", "short"], ["stability"],
     FLEX_WITH_OTHER_TERMS_NO_MODEL_ALLOWED),
    ("flex_medium_with_cost",
     ["flex", "medium"], ["cost"],
     FLEX_WITH_OTHER_TERMS_NO_MODEL_ALLOWED),
    ("flex_long_with_performance",
     ["flex", "long"], ["performance"],
     FLEX_WITH_OTHER_TERMS_NO_MODEL_ALLOWED),
    ("flex_short_medium_with_stability",
     ["flex", "short", "medium"], ["stability"],
     FLEX_WITH_OTHER_TERMS_NO_MODEL_ALLOWED),
    ("stability_cost_with_flex_term",
     ["flex"], ["stability", "cost"],
     STABILITY_WITH_OTHER_MODELS_NO_TERM_ALLOWED),
    ("stability_cost_with_short_term",
     ["short"], ["stability", "cost"],
     STABILITY_WITH_OTHER_MODELS_NO_TERM_ALLOWED),
    ("stability_perf_with_medium_term",
     ["medium"], ["stability", "performance"],
     STABILITY_WITH_OTHER_MODELS_NO_TERM_ALLOWED),
    ("stability_cost_perf_with_long_term",
     ["long"], ["stability", "cost", "performance"],
     STABILITY_WITH_OTHER_MODELS_NO_TERM_ALLOWED),
    ("stability_cost_perf_with_flex_term",
     ["flex"], ["stability", "cost", "performance"],
     STABILITY_WITH_OTHER_MODELS_NO_TERM_ALLOWED),
    ("invalid_term_name",
     ["quarterly"], None,
     INVALID_TERM_NAME),
    ("invalid_term_name_garbage",
     ["foo_bar"], None,
     INVALID_TERM_NAME),
    ("invalid_model_name",
     None, ["turbo"],
     INVALID_MODEL_NAME),
    ("invalid_model_name_garbage",
     None, ["cost_and_perf"],
     INVALID_MODEL_NAME),
    ("blank_term",
     [""], None,
     EMPTY_NOT_ALLOWED),
    ("blank_model",
     None, [""],
     EMPTY_NOT_ALLOWED),
])
def test_create_exp_invalid_term_model(test_name, terms, models, expected_error, cluster_type):
    """Invalid term/model combinations must be rejected with HTTP 400 and the exact error message."""
    exp_name = f"flex-stab-invalid-{test_name}"
    tmp_file = _flex_setup(cluster_type, exp_name, terms, models)
    try:
        response = create_experiment(tmp_file)
        data = response.json()
        print(f"[{test_name}] {response.status_code} — {data.get('message')}")
        assert response.status_code == ERROR_STATUS_CODE, \
            f"[{test_name}] Expected 400, got {response.status_code}: {data.get('message')}"
        assert data["status"] == ERROR_STATUS
        assert data["message"] == expected_error, \
            f"[{test_name}]\n  expected: {expected_error}\n  got:      {data.get('message')}"
    finally:
        delete_experiment(tmp_file, rm=False)
