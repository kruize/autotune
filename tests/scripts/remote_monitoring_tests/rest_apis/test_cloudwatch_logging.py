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

Integration tests for CloudWatch logging configuration in Kruize.

These tests replicate the manual verification performed on the dev OpenShift
cluster after the jdk.net fix was applied to the container image.

Two scenarios are covered:

  credentials-absent  — CloudWatch credentials are empty in kruizeconfigjson.
                        Kruize must start cleanly and emit the skip message.
                        This is the condition on the test cluster that masked
                        the jdk.net bug (credentials were never set there).

  credentials-present — Valid CloudWatch credentials are set.
                        Kruize must start cleanly and emit:
                          "CloudWatch logging enabled — region: ..."
                        Before the fix, this crashed with:
                          NoClassDefFoundError: jdk/net/Sockets
                        because the jdk.net module was absent from the
                        jlink-built JRE in the container image.

Prerequisites (credentials-present tests):
  The following environment variables must be set:
    CLOUDWATCH_ACCESS_KEY_ID      — IAM access key for the dev CloudWatch log group
    CLOUDWATCH_SECRET_ACCESS_KEY  — IAM secret access key
    CLOUDWATCH_REGION             — AWS region (e.g. us-east-1)
    CLOUDWATCH_LOG_GROUP          — CloudWatch log group  (e.g. kruize-dev-logs)
    CLOUDWATCH_LOG_STREAM         — CloudWatch log stream (e.g. kruize-dev-stream)
  Tests that require these variables are skipped automatically when they are absent.
"""

import os
import time
import pytest
import subprocess

# ---------------------------------------------------------------------------
# Constants — must match exactly what CloudWatchAppender emits
# ---------------------------------------------------------------------------

# Emitted by CloudWatchAppender.configureLoggerForCloudWatchLog() when all
# three mandatory credentials (accessKeyId, secretAccessKey, region) are absent.
CLOUDWATCH_SKIP_MSG = "AWS access details are not provided. Skipping sending logs to CloudWatch."

# Emitted on successful CloudWatch setup (after the jdk.net fix).
# Partial match — the full message includes region/group/stream values.
CLOUDWATCH_SUCCESS_MSG = "CloudWatch logging enabled"

# Emitted when the jdk.net module is absent from the jlink JRE (the production bug).
JDKNET_CRASH_MSG = "NoClassDefFoundError: jdk/net/Sockets"

# Namespace where kruize runs — determined by cluster_type at runtime
NAMESPACE_OPENSHIFT = "openshift-tuning"
NAMESPACE_DEFAULT   = "monitoring"

POD_READY_TIMEOUT   = 180   # seconds to wait for the pod to become Ready
LOG_SETTLE_SECONDS  = 10    # seconds to wait after pod is Ready before reading logs


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _namespace(cluster_type: str) -> str:
    return NAMESPACE_OPENSHIFT if cluster_type == "openshift" else NAMESPACE_DEFAULT


def _get_kruize_pod_name(namespace: str) -> str:
    """Return the name of the running kruize pod (excluding db and ui pods)."""
    result = subprocess.run(
        ["kubectl", "-n", namespace, "get", "pods",
         "--no-headers", "-o", "custom-columns=NAME:.metadata.name"],
        capture_output=True, text=True, check=True
    )
    for line in result.stdout.splitlines():
        name = line.strip()
        if "kruize" in name and "kruize-db" not in name and "kruize-ui" not in name:
            return name
    raise RuntimeError(f"No kruize pod found in namespace '{namespace}'")


def _wait_for_pod_ready(namespace: str, timeout: int = POD_READY_TIMEOUT) -> None:
    """Wait until the kruize pod reaches Ready condition."""
    subprocess.run(
        ["kubectl", "-n", namespace, "wait",
         "--for=condition=Ready", "pod",
         "-l", "app=kruize",
         f"--timeout={timeout}s"],
        check=True
    )


def _get_pod_logs(namespace: str, pod_name: str) -> str:
    """Fetch and return the full log of the given pod."""
    result = subprocess.run(
        ["kubectl", "-n", namespace, "logs", pod_name],
        capture_output=True, text=True, check=True
    )
    return result.stdout


def _restart_kruize(namespace: str) -> None:
    """Trigger a rollout restart and wait for the new pod to be Ready."""
    subprocess.run(
        ["kubectl", "-n", namespace, "rollout", "restart", "deployment/kruize"],
        check=True
    )
    _wait_for_pod_ready(namespace)
    time.sleep(LOG_SETTLE_SECONDS)


# ---------------------------------------------------------------------------
# Scenario 1 — credentials absent
# Simulates the test/dev cluster config that masked the jdk.net bug.
# ---------------------------------------------------------------------------

@pytest.mark.cloudwatch_logging
def test_kruize_starts_and_skips_cloudwatch_when_credentials_absent(cluster_type):
    """
    SCENARIO: credentials-absent

    When CloudWatch credentials are not set in kruizeconfigjson, Kruize must:
      1. Start successfully (pod reaches Ready state).
      2. Log the skip message indicating CloudWatch setup was bypassed.
      3. NOT crash with NoClassDefFoundError: jdk/net/Sockets.

    This mirrors the test-cluster condition where missing credentials caused
    the credential guard in CloudWatchAppender to short-circuit, hiding the
    jdk.net bug from pre-release testing.
    """
    namespace = _namespace(cluster_type)

    _wait_for_pod_ready(namespace)
    time.sleep(LOG_SETTLE_SECONDS)

    pod_name = _get_kruize_pod_name(namespace)
    logs = _get_pod_logs(namespace, pod_name)

    # Must log the skip message — confirms credentials guard fired correctly
    assert CLOUDWATCH_SKIP_MSG in logs, (
        f"Expected skip message not found in pod logs.\n"
        f"Expected: '{CLOUDWATCH_SKIP_MSG}'\n"
        f"This means credentials may have been set unexpectedly, or the "
        f"credential guard in CloudWatchAppender.configureLoggerForCloudWatchLog() "
        f"is not working correctly."
    )

    # Must NOT have hit the jdk.net crash
    assert JDKNET_CRASH_MSG not in logs, (
        f"Pod crashed with '{JDKNET_CRASH_MSG}'.\n"
        f"The 'jdk.net' module is missing from the jlink JRE in the container image.\n"
        f"Ensure jdk.net is included in the --add-modules list in the jlink build step."
    )


# ---------------------------------------------------------------------------
# Scenario 2 — credentials present
# Replicates the stage/prod config that triggered the production crash.
# ---------------------------------------------------------------------------

@pytest.mark.cloudwatch_logging
@pytest.mark.skipif(
    not all([
        os.environ.get("CLOUDWATCH_ACCESS_KEY_ID"),
        os.environ.get("CLOUDWATCH_SECRET_ACCESS_KEY"),
        os.environ.get("CLOUDWATCH_REGION"),
    ]),
    reason=(
        "Skipped: CLOUDWATCH_ACCESS_KEY_ID, CLOUDWATCH_SECRET_ACCESS_KEY, and "
        "CLOUDWATCH_REGION must all be set to run the credentials-present test. "
        "Set them using dev AWS account credentials (not prod). "
        "See tests/scripts/remote_monitoring_tests/cloudwatch_logging_tests.sh for details."
    )
)
def test_kruize_starts_and_enables_cloudwatch_when_credentials_present(cluster_type):
    """
    SCENARIO: credentials-present

    When valid CloudWatch credentials are set in kruizeconfigjson, Kruize must:
      1. Start successfully (pod reaches Ready state).
      2. Log the CloudWatch success message including region, log group, log stream.
      3. NOT crash with NoClassDefFoundError: jdk/net/Sockets.

    BEFORE the fix: this test would fail because the pod crashed at startup with
      java.lang.NoClassDefFoundError: jdk/net/Sockets
        at DefaultHttpClientConnectionOperator.<clinit>
        at Apache5HttpClient.createClient
        at CloudWatchAppender.configureLoggerForCloudWatchLog
        at Autotune.main
      Caused by: java.lang.ClassNotFoundException: jdk.net.Sockets

    Root cause: awssdk-version bumped from 2.42.25 to 2.46.5 in pom.xml switched
    the default HTTP client from apache-client (HC4) to apache5-client (HC5).
    HC5 requires jdk.net.Sockets which was absent from the jlink-built JRE because
    jdk.net was not listed in the --add-modules flag.

    AFTER the fix (jdk.net added to --add-modules): this test passes.
    """
    namespace  = _namespace(cluster_type)
    pod_name   = _get_kruize_pod_name(namespace)
    logs       = _get_pod_logs(namespace, pod_name)

    # Must NOT have hit the production crash — this was the exact failure on stage
    assert JDKNET_CRASH_MSG not in logs, (
        f"PRODUCTION BUG REPRODUCED: Pod crashed with '{JDKNET_CRASH_MSG}'.\n"
        f"This is the exact crash seen on the OpenShift stage cluster after the "
        f"awssdk-version bump from 2.42.25 to 2.46.5 in pom.xml.\n"
        f"Root cause: the 'jdk.net' module is absent from the jlink JRE.\n"
        f"Fix: ensure jdk.net is included in the --add-modules list in the jlink build step."
    )

    # Must log the CloudWatch success message — confirms setup completed
    assert CLOUDWATCH_SUCCESS_MSG in logs, (
        f"CloudWatch success message not found in pod logs.\n"
        f"Expected pattern: '{CLOUDWATCH_SUCCESS_MSG}'\n"
        f"Full expected format: 'CloudWatch logging enabled — region: <r>, "
        f"log group: <g>, log stream: <s>, log level: <l>'\n"
        f"Either the CloudWatch setup failed silently, or the credentials "
        f"in kruizeconfigjson are incorrect. Check the pod logs for errors."
    )

    # Print the actual success line for visibility in test output
    for line in logs.splitlines():
        if CLOUDWATCH_SUCCESS_MSG in line:
            print(f"\n[cloudwatch] {line.strip()}")
            break
