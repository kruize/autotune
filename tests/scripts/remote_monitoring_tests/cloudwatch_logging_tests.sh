#!/bin/bash
#
# Copyright (c) 2026 IBM Corporation and others.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


##### Integration tests for CloudWatch logging configuration in Kruize #####
#
# Scenarios tested:
#
#   credentials-absent  — CloudWatch credentials not set in kruizeconfigjson.
#                         Kruize must start successfully and log the skip message.
#                         (This is the test cluster condition that masked the jdk.net bug.)
#
#   credentials-present — Valid CloudWatch credentials set in kruizeconfigjson.
#                         Kruize must start successfully and log the success message:
#                         "CloudWatch logging enabled — region: ..."
#                         (This is the stage cluster condition that triggered the crash.)
#
# Prerequisites (credentials-present scenario):
#   The following env vars must be exported before running:
#     CLOUDWATCH_ACCESS_KEY_ID      — IAM access key for the dev CloudWatch log group
#     CLOUDWATCH_SECRET_ACCESS_KEY  — IAM secret access key
#     CLOUDWATCH_REGION             — AWS region (e.g. us-east-1)
#     CLOUDWATCH_LOG_GROUP          — CloudWatch log group name (e.g. kruize-dev-logs)
#     CLOUDWATCH_LOG_STREAM         — CloudWatch log stream name (e.g. kruize-dev-stream)
#
# Usage:
#   export CLOUDWATCH_ACCESS_KEY_ID=...
#   export CLOUDWATCH_SECRET_ACCESS_KEY=...
#   export CLOUDWATCH_REGION=us-east-1
#   export CLOUDWATCH_LOG_GROUP=kruize-dev-logs
#   export CLOUDWATCH_LOG_STREAM=kruize-dev-stream
#   ./cloudwatch_logging_tests.sh
#
# Or via the remote_monitoring_tests.sh runner (set testcase=cloudwatch_logging).

REMOTE_MONITORING_TEST_DIR="${KRUIZE_REPO}/tests/scripts/remote_monitoring_tests"
APP_DEPLOYMENT="kruize"

# Expected log messages — must match exactly what CloudWatchAppender emits
CLOUDWATCH_SKIP_MSG="AWS access details are not provided. Skipping sending logs to CloudWatch."
CLOUDWATCH_SUCCESS_MSG="CloudWatch logging enabled"

function cloudwatch_logging_tests() {
	start_time=$(get_date)
	FAILED_CASES=()
	TESTS=0
	TESTS_FAILED=0
	TESTS_PASSED=0
	((TOTAL_TEST_SUITES++))

	TEST_SUITE_DIR="${RESULTS}/cloudwatch_logging_tests"
	mkdir -p "${TEST_SUITE_DIR}" 2>&1
	KRUIZE_SETUP_LOG="${TEST_SUITE_DIR}/kruize_setup.log"

	if [ "$cluster_type" == "openshift" ]; then
		NAMESPACE="openshift-tuning"
		YAML_FILE="${REMOTE_MONITORING_TEST_DIR}/../../../manifests/crc/default-db-included-installation/openshift/kruize-crc-openshift.yaml"
	elif [ "$cluster_type" == "minikube" ] || [ "$cluster_type" == "kind" ]; then
		NAMESPACE="monitoring"
		YAML_FILE="${REMOTE_MONITORING_TEST_DIR}/../../../manifests/crc/default-db-included-installation/minikube/kruize-crc-minikube.yaml"
	else
		echo "Invalid cluster type found: ${cluster_type}"
		return
	fi

	kubectl_cmd="kubectl -n ${NAMESPACE}"

	echo ""
	echo "******************* Executing test suite ${FUNCNAME} ****************"
	echo ""

	# --- Scenario 1: credentials absent ---
	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
	echo " Running scenario: credentials-absent"
	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
	run_cloudwatch_scenario "credentials-absent"
	if [ "${TESTS_FAILED}" -ne "0" ]; then
		FAILED_CASES+=("credentials-absent")
	fi

	# --- Scenario 2: credentials present (requires env vars) ---
	if [ -z "${CLOUDWATCH_ACCESS_KEY_ID}" ] || [ -z "${CLOUDWATCH_SECRET_ACCESS_KEY}" ] || [ -z "${CLOUDWATCH_REGION}" ]; then
		echo ""
		echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
		echo " Skipping scenario: credentials-present"
		echo " Reason: CLOUDWATCH_ACCESS_KEY_ID / CLOUDWATCH_SECRET_ACCESS_KEY /"
		echo "         CLOUDWATCH_REGION env vars are not set."
		echo " Set them to run the full credentials-present integration test."
		echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
	else
		echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
		echo " Running scenario: credentials-present"
		echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
		run_cloudwatch_scenario "credentials-present"
		if [ "${TESTS_FAILED}" -ne "0" ]; then
			FAILED_CASES+=("credentials-present")
		fi
	fi

	TESTS=$(($TESTS_PASSED + $TESTS_FAILED))
	TOTAL_TESTS_FAILED=${TESTS_FAILED}
	TOTAL_TESTS_PASSED=${TESTS_PASSED}
	TOTAL_TESTS=${TESTS}

	if [ "${TESTS_FAILED}" -ne "0" ]; then
		FAILED_TEST_SUITE+=(${FUNCNAME})
	fi

	end_time=$(get_date)
	elapsed_time=$(time_diff "${start_time}" "${end_time}")

	FAILED_CASES=( $(printf '%s\n' "${FAILED_CASES[@]}" | uniq ) )
	testsuitesummary ${FUNCNAME} "${elapsed_time}" ${FAILED_CASES}
}

# Run a single CloudWatch scenario by patching the YAML, deploying, and checking pod logs.
# input: scenario name — "credentials-absent" or "credentials-present"
run_cloudwatch_scenario() {
	local scenario=$1
	local POD_LOG="${TEST_SUITE_DIR}/${scenario}-pod.log"
	local SETUP_LOG="${TEST_SUITE_DIR}/${scenario}-setup.log"

	echo "Backing up YAML: ${YAML_FILE}"
	cp "${YAML_FILE}" "${YAML_FILE}.cw.bak"

	if [ "${scenario}" == "credentials-absent" ]; then
		patch_cloudwatch_credentials "" "" "" "" ""
	else
		local log_group="${CLOUDWATCH_LOG_GROUP:-kruize-dev-logs}"
		local log_stream="${CLOUDWATCH_LOG_STREAM:-kruize-dev-stream}"
		patch_cloudwatch_credentials \
			"${CLOUDWATCH_ACCESS_KEY_ID}" \
			"${CLOUDWATCH_SECRET_ACCESS_KEY}" \
			"${CLOUDWATCH_REGION}" \
			"${log_group}" \
			"${log_stream}"
	fi

	# Update the image to the build under test
	if [ -n "${AUTOTUNE_IMAGE}" ]; then
		sed -i.image.bak '
		/kind: Deployment/,/kind:/{
			/name: kruize$/,/containers:/{
				/^        - name: kruize$/{
					n
					s|image: .*|image: '"${AUTOTUNE_IMAGE}"'|
				}
			}
		}' "${YAML_FILE}"
		echo "Updated image in YAML to ${AUTOTUNE_IMAGE}"
	fi

	echo "Applying YAML and restarting kruize..."
	${kubectl_cmd} apply -f "${YAML_FILE}" > "${SETUP_LOG}" 2>&1
	${kubectl_cmd} rollout restart deployment kruize >> "${SETUP_LOG}" 2>&1

	# Wait for pod to be ready or fail
	if ${kubectl_cmd} wait --for=condition=Ready pod -l app=${APP_DEPLOYMENT} --timeout=180s > /dev/null 2>&1; then
		echo "Kruize pod is Ready"
		local POD_NAME
		POD_NAME=$(${kubectl_cmd} get pods | grep 'kruize' | grep -v -E 'kruize-db|kruize-ui' | awk 'NR==1{print $1}')
		sleep 10
		${kubectl_cmd} logs "${POD_NAME}" > "${POD_LOG}" 2>&1

		check_cloudwatch_pod_log "${scenario}" "${POD_LOG}"
	else
		echo "ERROR: Kruize pod failed to become Ready for scenario '${scenario}'"
		echo "Check ${SETUP_LOG} and ${POD_LOG} for details"
		((TESTS_FAILED++))
	fi

	# Restore original YAML
	mv "${YAML_FILE}.cw.bak" "${YAML_FILE}"
}

# Patch the cloudwatch block inside kruizeconfigjson in the deployment YAML.
# inputs: accessKeyId, secretAccessKey, region, logGroup, logStream
# Empty strings produce a no-credentials config (credentials-absent scenario).
patch_cloudwatch_credentials() {
	local access_key_id=$1
	local secret_access_key=$2
	local region=$3
	local log_group=${4:-kruize-logs}
	local log_stream=${5:-kruize-stream}

	echo "Patching CloudWatch credentials in YAML (scenario: ${access_key_id:+present}${access_key_id:-absent})"

	sed -i \
		-e 's|"accessKeyId":[[:space:]]*"[^"]*"|"accessKeyId": "'"${access_key_id}"'"|g' \
		-e 's|"secretAccessKey":[[:space:]]*"[^"]*"|"secretAccessKey": "'"${secret_access_key}"'"|g' \
		-e 's|"region":[[:space:]]*"[^"]*"|"region": "'"${region}"'"|g' \
		-e 's|"logGroup":[[:space:]]*"[^"]*"|"logGroup": "'"${log_group}"'"|g' \
		-e 's|"logStream":[[:space:]]*"[^"]*"|"logStream": "'"${log_stream}"'"|g' \
		"${YAML_FILE}"
}

# Assert the pod log contains the expected message for the given scenario.
check_cloudwatch_pod_log() {
	local scenario=$1
	local pod_log=$2

	echo ""
	if [ "${scenario}" == "credentials-absent" ]; then
		# Positive: pod must start cleanly and log the skip message
		if grep -q "${CLOUDWATCH_SKIP_MSG}" "${pod_log}"; then
			echo "PASS [credentials-absent]: Pod started and logged expected skip message:"
			echo "  '${CLOUDWATCH_SKIP_MSG}'"
			((TESTS_PASSED++))
		else
			echo "FAIL [credentials-absent]: Expected skip message not found in pod log."
			echo "  Expected: '${CLOUDWATCH_SKIP_MSG}'"
			echo "  Check: ${pod_log}"
			((TESTS_FAILED++))
		fi

		# Negative: pod must NOT have crashed with the jdk.net error
		if grep -q "NoClassDefFoundError: jdk/net/Sockets" "${pod_log}"; then
			echo "FAIL [credentials-absent]: Pod crashed with NoClassDefFoundError: jdk/net/Sockets."
			echo "  The 'jdk.net' module is missing from the jlink JRE."
			echo "  Ensure jdk.net is included in the --add-modules list in the jlink build step."
			((TESTS_FAILED++))
		fi

	elif [ "${scenario}" == "credentials-present" ]; then
		# Positive: pod must start cleanly and log the CloudWatch success message
		if grep -q "${CLOUDWATCH_SUCCESS_MSG}" "${pod_log}"; then
			echo "PASS [credentials-present]: Pod started and logged expected CloudWatch success message:"
			grep "${CLOUDWATCH_SUCCESS_MSG}" "${pod_log}"
			((TESTS_PASSED++))
		else
			echo "FAIL [credentials-present]: Expected CloudWatch success message not found in pod log."
			echo "  Expected pattern: '${CLOUDWATCH_SUCCESS_MSG}'"
			echo "  Check: ${pod_log}"
			((TESTS_FAILED++))
		fi

		# Negative: the exact production crash must not appear
		if grep -q "NoClassDefFoundError: jdk/net/Sockets" "${pod_log}"; then
			echo "FAIL [credentials-present]: Pod crashed with NoClassDefFoundError: jdk/net/Sockets."
			echo "  This is the production bug — the 'jdk.net' module is missing from the jlink JRE."
			echo "  Ensure jdk.net is included in the --add-modules list in the jlink build step."
			((TESTS_FAILED++))
		fi
	fi
	echo ""
}
