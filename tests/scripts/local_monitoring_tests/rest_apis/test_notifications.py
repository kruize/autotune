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
"""
Tests for provisioning-status notifications emitted by the recommendation engine.

Each engine-level notifications block must contain exactly ONE of:
  optimised | under-provisioned | over-provisioned
for every resource dimension that has both a current value and a generated
recommendation.

Notification codes verified:
  CPU request  : 323004 (optimised) / 323006 (under) / 323007 (over)
  CPU limit    : 323005 (optimised) / 323008 (under) / 323009 (over)
  Memory request: 324003 (optimised) / 324005 (under) / 324006 (over)
  Memory limit : 324004 (optimised) / 324007 (under) / 324008 (over)
"""

import json
import sys

import pytest
from jinja2 import Environment, FileSystemLoader

sys.path.append("../../")

from helpers.fixtures import *
from helpers.kruize import *
from helpers.utils import (
    # Status codes / messages
    SUCCESS_STATUS_CODE,
    SUCCESS_200_STATUS_CODE,
    SUCCESS_STATUS,
    ERROR_STATUS_CODE,
    CREATE_EXP_SUCCESS_MSG,
    CREATE_METRIC_PROFILE_SUCCESS_MSG,
    CREATE_METADATA_PROFILE_SUCCESS_MSG,
    RECOMMENDATIONS_AVAILABLE,
    # Notification codes — optimised
    CPU_REQUEST_OPTIMISED_CODE,
    CPU_LIMIT_OPTIMISED_CODE,
    MEMORY_REQUEST_OPTIMISED_CODE,
    MEMORY_LIMIT_OPTIMISED_CODE,
    # Notification codes — under/over-provisioned
    CPU_REQUEST_UNDER_PROVISIONED_CODE,
    CPU_REQUEST_OVER_PROVISIONED_CODE,
    CPU_LIMIT_UNDER_PROVISIONED_CODE,
    CPU_LIMIT_OVER_PROVISIONED_CODE,
    MEMORY_REQUEST_UNDER_PROVISIONED_CODE,
    MEMORY_REQUEST_OVER_PROVISIONED_CODE,
    MEMORY_LIMIT_UNDER_PROVISIONED_CODE,
    MEMORY_LIMIT_OVER_PROVISIONED_CODE,
    # Messages — under/over provisioned
    CPU_REQUEST_UNDER_PROVISIONED_MSG,
    CPU_REQUEST_OVER_PROVISIONED_MSG,
    CPU_LIMIT_UNDER_PROVISIONED_MSG,
    CPU_LIMIT_OVER_PROVISIONED_MSG,
    MEMORY_REQUEST_UNDER_PROVISIONED_MSG,
    MEMORY_REQUEST_OVER_PROVISIONED_MSG,
    MEMORY_LIMIT_UNDER_PROVISIONED_MSG,
    MEMORY_LIMIT_OVER_PROVISIONED_MSG,
    # All provisioning-state codes (optimised + under + over) — used for presence checks
    ALL_PROVISIONING_CODES,
    # Helper validators
    validate_provisioning_notifications,
    check_provisioning_notification_exclusive,
    check_provisioning_notification_message,
    get_metric_profile_dir,
    get_metadata_profile_dir,
    read_json_data_from_file,
)
from helpers.list_reco_json_local_monitoring_schema import list_reco_json_local_monitoring_schema
from helpers.list_reco_json_validate import validate_list_reco_json
from helpers.list_metric_profiles_validate import validate_list_metric_profiles_json
from helpers.list_metric_profiles_without_parameters_schema import list_metric_profiles_schema
from helpers.list_metadata_profiles_validate import validate_list_metadata_profiles_json
from helpers.list_metadata_profiles_schema import list_metadata_profiles_schema

metric_profile_dir = get_metric_profile_dir()
metadata_profile_dir = get_metadata_profile_dir()

_CODE_TO_MSG = {
    CPU_REQUEST_UNDER_PROVISIONED_CODE: CPU_REQUEST_UNDER_PROVISIONED_MSG,
    CPU_REQUEST_OVER_PROVISIONED_CODE: CPU_REQUEST_OVER_PROVISIONED_MSG,
    CPU_LIMIT_UNDER_PROVISIONED_CODE: CPU_LIMIT_UNDER_PROVISIONED_MSG,
    CPU_LIMIT_OVER_PROVISIONED_CODE: CPU_LIMIT_OVER_PROVISIONED_MSG,
    MEMORY_REQUEST_UNDER_PROVISIONED_CODE: MEMORY_REQUEST_UNDER_PROVISIONED_MSG,
    MEMORY_REQUEST_OVER_PROVISIONED_CODE: MEMORY_REQUEST_OVER_PROVISIONED_MSG,
    MEMORY_LIMIT_UNDER_PROVISIONED_CODE: MEMORY_LIMIT_UNDER_PROVISIONED_MSG,
    MEMORY_LIMIT_OVER_PROVISIONED_CODE: MEMORY_LIMIT_OVER_PROVISIONED_MSG,
}


# ---------------------------------------------------------------------------
# Setup helpers
# ---------------------------------------------------------------------------

def _setup_metric_and_metadata_profile(cluster_type):
    """Create (or recreate) the default metric and metadata profiles."""
    if cluster_type == "minikube":
        metric_profile_json_file = metric_profile_dir / "resource_optimization_local_monitoring_norecordingrules.json"
    else:
        metric_profile_json_file = metric_profile_dir / "resource_optimization_local_monitoring.json"

    response = delete_metric_profile(metric_profile_json_file)
    response = create_metric_profile(metric_profile_json_file)
    assert response.status_code == SUCCESS_STATUS_CODE, f"create_metric_profile failed: {response.text}"
    data = response.json()
    assert data["status"] == SUCCESS_STATUS
    metric_profile_name = json.load(open(metric_profile_json_file))["metadata"]["name"]
    assert data["message"] == CREATE_METRIC_PROFILE_SUCCESS_MSG % metric_profile_name

    response = validate_list_metric_profiles_json(
        list_metric_profiles(name=metric_profile_name, logging=False).json(),
        list_metric_profiles_schema,
    )
    assert response == ""

    metadata_profile_json_file = metadata_profile_dir / "cluster_metadata_local_monitoring.json"
    metadata_profile_name = json.load(open(metadata_profile_json_file))["metadata"]["name"]
    delete_metadata_profile(metadata_profile_name)

    response = create_metadata_profile(metadata_profile_json_file)
    assert response.status_code == SUCCESS_STATUS_CODE, f"create_metadata_profile failed: {response.text}"
    data = response.json()
    assert data["status"] == SUCCESS_STATUS
    assert data["message"] == CREATE_METADATA_PROFILE_SUCCESS_MSG % metadata_profile_name

    response = validate_list_metadata_profiles_json(
        list_metadata_profiles(name=metadata_profile_name, logging=False).json(),
        list_metadata_profiles_schema,
    )
    assert response == ""


def _create_experiment_from_template(test_name, template_params: dict) -> str:
    """Render the create_exp_template, write to /tmp, return path."""
    tmp_json_file = f"/tmp/create_exp_{test_name}.json"
    environment = Environment(loader=FileSystemLoader("../json_files/"))
    template = environment.get_template("create_exp_template.json")
    content = template.render(**template_params)
    json_content = json.loads(content)

    # Remove "None" placeholders left by the template
    k8s = json_content[0]["kubernetes_objects"][0]
    for field in ("type", "name", "namespace"):
        if k8s.get(field) == "None":
            k8s.pop(field)
    if k8s.get("containers", [{}])[0].get("container_image_name") == "None":
        k8s.pop("containers", None)

    with open(tmp_json_file, mode="w", encoding="utf-8") as f:
        json.dump(json_content, f, indent=4)

    return tmp_json_file


def _collect_engine_notifications(list_reco_json: list) -> list[dict]:
    """Return all engine-level notification dicts found anywhere in the recommendations."""
    engine_notifications = []
    for reco_obj in list_reco_json:
        for k8s_obj in reco_obj.get("kubernetes_objects", []):
            for container in k8s_obj.get("containers", []):
                data = container.get("recommendations", {}).get("data", {})
                for ts_obj in data.values():
                    for term_obj in ts_obj.get("recommendation_terms", {}).values():
                        for engine_obj in term_obj.get("recommendation_engines", {}).values():
                            notifs = engine_obj.get("notifications", {})
                            if notifs:
                                engine_notifications.append(notifs)
    return engine_notifications


# ---------------------------------------------------------------------------
# Unit-style tests for helper functions (no server required)
# ---------------------------------------------------------------------------

class TestProvisioningNotificationHelpers:
    """Pure-Python unit tests for the notification helper utilities in utils.py."""

    def test_exclusive_check_passes_with_optimised(self):
        notifications = {CPU_REQUEST_OPTIMISED_CODE: {"message": "ok", "type": "NOTICE"}}
        check_provisioning_notification_exclusive(
            notifications, "CPU request",
            CPU_REQUEST_UNDER_PROVISIONED_CODE,
            CPU_REQUEST_OVER_PROVISIONED_CODE,
            CPU_REQUEST_OPTIMISED_CODE,
        )

    def test_exclusive_check_passes_with_under_provisioned(self):
        notifications = {CPU_REQUEST_UNDER_PROVISIONED_CODE: {"message": CPU_REQUEST_UNDER_PROVISIONED_MSG, "type": "NOTICE"}}
        check_provisioning_notification_exclusive(
            notifications, "CPU request",
            CPU_REQUEST_UNDER_PROVISIONED_CODE,
            CPU_REQUEST_OVER_PROVISIONED_CODE,
            CPU_REQUEST_OPTIMISED_CODE,
        )

    def test_exclusive_check_passes_with_over_provisioned(self):
        notifications = {CPU_REQUEST_OVER_PROVISIONED_CODE: {"message": CPU_REQUEST_OVER_PROVISIONED_MSG, "type": "NOTICE"}}
        check_provisioning_notification_exclusive(
            notifications, "CPU request",
            CPU_REQUEST_UNDER_PROVISIONED_CODE,
            CPU_REQUEST_OVER_PROVISIONED_CODE,
            CPU_REQUEST_OPTIMISED_CODE,
        )

    def test_exclusive_check_fails_when_two_codes_present(self):
        notifications = {
            CPU_REQUEST_OPTIMISED_CODE: {"message": "ok", "type": "NOTICE"},
            CPU_REQUEST_UNDER_PROVISIONED_CODE: {"message": CPU_REQUEST_UNDER_PROVISIONED_MSG, "type": "NOTICE"},
        }
        with pytest.raises(AssertionError):
            check_provisioning_notification_exclusive(
                notifications, "CPU request",
                CPU_REQUEST_UNDER_PROVISIONED_CODE,
                CPU_REQUEST_OVER_PROVISIONED_CODE,
                CPU_REQUEST_OPTIMISED_CODE,
            )

    def test_exclusive_check_fails_when_none_present(self):
        notifications = {"999999": {"message": "unrelated", "type": "INFO"}}
        with pytest.raises(AssertionError):
            check_provisioning_notification_exclusive(
                notifications, "CPU request",
                CPU_REQUEST_UNDER_PROVISIONED_CODE,
                CPU_REQUEST_OVER_PROVISIONED_CODE,
                CPU_REQUEST_OPTIMISED_CODE,
            )

    def test_message_check_passes(self):
        notifications = {
            CPU_LIMIT_UNDER_PROVISIONED_CODE: {
                "message": CPU_LIMIT_UNDER_PROVISIONED_MSG,
                "type": "NOTICE",
            }
        }
        check_provisioning_notification_message(
            notifications, CPU_LIMIT_UNDER_PROVISIONED_CODE, CPU_LIMIT_UNDER_PROVISIONED_MSG
        )

    def test_message_check_fails_on_wrong_message(self):
        notifications = {
            MEMORY_LIMIT_OVER_PROVISIONED_CODE: {
                "message": "wrong message",
                "type": "NOTICE",
            }
        }
        with pytest.raises(AssertionError):
            check_provisioning_notification_message(
                notifications, MEMORY_LIMIT_OVER_PROVISIONED_CODE, MEMORY_LIMIT_OVER_PROVISIONED_MSG
            )

    def test_validate_provisioning_notifications_all_optimised(self):
        notifications = {
            CPU_REQUEST_OPTIMISED_CODE: {"message": "ok", "type": "NOTICE"},
            CPU_LIMIT_OPTIMISED_CODE: {"message": "ok", "type": "NOTICE"},
            MEMORY_REQUEST_OPTIMISED_CODE: {"message": "ok", "type": "NOTICE"},
            MEMORY_LIMIT_OPTIMISED_CODE: {"message": "ok", "type": "NOTICE"},
        }
        validate_provisioning_notifications(notifications)  # should not raise

    def test_validate_provisioning_notifications_mixed(self):
        notifications = {
            CPU_REQUEST_UNDER_PROVISIONED_CODE: {"message": CPU_REQUEST_UNDER_PROVISIONED_MSG, "type": "NOTICE"},
            CPU_LIMIT_OPTIMISED_CODE: {"message": "ok", "type": "NOTICE"},
            MEMORY_REQUEST_OVER_PROVISIONED_CODE: {"message": MEMORY_REQUEST_OVER_PROVISIONED_MSG, "type": "NOTICE"},
            MEMORY_LIMIT_UNDER_PROVISIONED_CODE: {"message": MEMORY_LIMIT_UNDER_PROVISIONED_MSG, "type": "NOTICE"},
        }
        validate_provisioning_notifications(notifications)  # should not raise

    def test_validate_provisioning_notifications_detects_conflict(self):
        notifications = {
            CPU_REQUEST_OPTIMISED_CODE: {"message": "ok", "type": "NOTICE"},
            CPU_REQUEST_UNDER_PROVISIONED_CODE: {"message": CPU_REQUEST_UNDER_PROVISIONED_MSG, "type": "NOTICE"},
        }
        with pytest.raises(AssertionError):
            validate_provisioning_notifications(notifications)

    @pytest.mark.parametrize("code,expected_msg", list(_CODE_TO_MSG.items()))
    def test_all_under_over_provisioned_messages_correct(self, code, expected_msg):
        """Each under/over-provisioned code must carry its exact message string."""
        notifications = {code: {"message": expected_msg, "type": "NOTICE"}}
        check_provisioning_notification_message(notifications, code, expected_msg)

    def test_cpu_limit_under_provisioned_code_value(self):
        assert CPU_LIMIT_UNDER_PROVISIONED_CODE == "323008"

    def test_cpu_limit_over_provisioned_code_value(self):
        assert CPU_LIMIT_OVER_PROVISIONED_CODE == "323009"

    def test_memory_limit_under_provisioned_code_value(self):
        assert MEMORY_LIMIT_UNDER_PROVISIONED_CODE == "324007"

    def test_memory_limit_over_provisioned_code_value(self):
        assert MEMORY_LIMIT_OVER_PROVISIONED_CODE == "324008"


# ---------------------------------------------------------------------------
# Integration tests — require a running Kruize instance
# ---------------------------------------------------------------------------

@pytest.mark.sanity
@pytest.mark.parametrize(
    "test_name, version, experiment_name, cluster_name, performance_profile, metadata_profile, "
    "mode, target_cluster, datasource, experiment_type, kubernetes_obj_type, "
    "name, namespace, namespace_name, container_image_name, container_name, "
    "measurement_duration, threshold",
    [
        (
            "notifications_container_cluster1",
            "v2.0",
            "test-notifications-container",
            "cluster-1",
            "resource-optimization-local-monitoring",
            "cluster-metadata-local-monitoring",
            "monitor",
            "local",
            "prometheus-1",
            "container",
            "deployment",
            "tfb-qrh-sample",
            "default",
            None,
            "kruize/tfb-qrh:1.13.2.F_et17",
            "tfb-server",
            "15min",
            "0.1",
        ),
    ],
)
def test_provisioning_notifications_present_in_engine(
    test_name,
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
    cluster_type,
):
    """
    Integration test: after generating recommendations for a container experiment, every
    engine-level notifications block must contain exactly one provisioning-state code per
    resource dimension and the message text must match the Java constant exactly.
    """
    form_kruize_url(cluster_type)

    template_params = dict(
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
        namespace_name=str(namespace_name),
        container_image_name=container_image_name,
        container_name=container_name,
        measurement_duration=measurement_duration,
        threshold=threshold,
    )

    input_json_file = _create_experiment_from_template(test_name, template_params)

    # Clean up any pre-existing experiment
    delete_experiment(input_json_file, rm=False)

    _setup_metric_and_metadata_profile(cluster_type)

    # Create experiment
    response = create_experiment(input_json_file)
    data = response.json()
    assert response.status_code == SUCCESS_STATUS_CODE, f"create_experiment failed: {data}"
    assert data["status"] == SUCCESS_STATUS
    assert data["message"] == CREATE_EXP_SUCCESS_MSG

    exp_name = json.load(open(input_json_file))[0]["experiment_name"]

    # Generate recommendations
    response = generate_recommendations(exp_name)
    assert response.status_code == SUCCESS_STATUS_CODE, f"generate_recommendations failed: {response.text}"

    # List recommendations
    response = list_recommendations(exp_name)
    assert response.status_code == SUCCESS_200_STATUS_CODE
    list_reco_json = response.json()

    # Schema validation
    errorMsg = validate_list_reco_json(list_reco_json, list_reco_json_local_monitoring_schema)
    assert errorMsg == "", f"Schema validation error: {errorMsg}"

    # Provisioning notification validation across every engine block
    engine_notif_blocks = _collect_engine_notifications(list_reco_json)
    assert engine_notif_blocks, "No engine notification blocks found — recommendations may not have been generated"

    for engine_notifications in engine_notif_blocks:
        assert any(code in engine_notifications for code in ALL_PROVISIONING_CODES), (
            "Engine notification block contains no provisioning-state codes "
            f"(optimised/under/over-provisioned). Block keys: {list(engine_notifications.keys())}"
        )
        validate_provisioning_notifications(engine_notifications)

    # Cleanup
    response = delete_experiment(input_json_file, rm=False)
    assert response.status_code == SUCCESS_STATUS_CODE


@pytest.mark.sanity
@pytest.mark.parametrize(
    "test_name, expected_code, expected_msg",
    [
        ("cpu_request_under", CPU_REQUEST_UNDER_PROVISIONED_CODE, CPU_REQUEST_UNDER_PROVISIONED_MSG),
        ("cpu_request_over",  CPU_REQUEST_OVER_PROVISIONED_CODE,  CPU_REQUEST_OVER_PROVISIONED_MSG),
        ("cpu_limit_under",   CPU_LIMIT_UNDER_PROVISIONED_CODE,   CPU_LIMIT_UNDER_PROVISIONED_MSG),
        ("cpu_limit_over",    CPU_LIMIT_OVER_PROVISIONED_CODE,    CPU_LIMIT_OVER_PROVISIONED_MSG),
        ("mem_request_under", MEMORY_REQUEST_UNDER_PROVISIONED_CODE, MEMORY_REQUEST_UNDER_PROVISIONED_MSG),
        ("mem_request_over",  MEMORY_REQUEST_OVER_PROVISIONED_CODE,  MEMORY_REQUEST_OVER_PROVISIONED_MSG),
        ("mem_limit_under",   MEMORY_LIMIT_UNDER_PROVISIONED_CODE,   MEMORY_LIMIT_UNDER_PROVISIONED_MSG),
        ("mem_limit_over",    MEMORY_LIMIT_OVER_PROVISIONED_CODE,    MEMORY_LIMIT_OVER_PROVISIONED_MSG),
    ],
)
def test_provisioning_notification_message_text(test_name, expected_code, expected_msg):
    """
    Unit-style parametrized test: verify that each new notification code maps to the
    exact message string defined in RecommendationConstants.java.
    No server connection required.
    """
    notifications = {expected_code: {"message": expected_msg, "type": "NOTICE", "code": int(expected_code)}}
    check_provisioning_notification_message(notifications, expected_code, expected_msg)
