#!/bin/bash
#
# Copyright (c) 2025, IBM Corporation and others.
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
#

CURRENT_DIR="$(dirname "$(realpath "$0")")"
LOCAL_MONITORING_TEST_DIR="${CURRENT_DIR}/local_monitoring_tests"

. ${LOCAL_MONITORING_TEST_DIR}/../common/common_functions.sh

APP_DEPLOYMENT="kruize"

# Datasource serviceName overrides to simulate reachability
declare -A datasource_scenarios
datasource_scenarios=(
  ["both-valid"]="prometheus-k8s thanos-querier"
  ["prom-valid-thanos-invalid"]="prometheus-k8s invalid-thanos"
  ["prom-invalid-thanos-valid"]="invalid-prometheus thanos-querier"
  ["both-invalid"]="invalid-prometheus invalid-thanos"
)

function datasource_reachability_tests() {
	start_time=$(get_date)
	FAILED_CASES=()
	TESTS=0
	TESTS_FAILED=0
	TESTS_PASSED=0
	((TOTAL_TEST_SUITES++))

	TEST_SUITE_DIR="${RESULTS}/datasource_reachability_tests"
	mkdir -p "${TEST_SUITE_DIR}" 2>&1
	KRUIZE_SETUP_LOG="${TEST_SUITE_DIR}/kruize_setup.log"

	echo ""
	echo "Setting up kruize..." | tee -a ${LOG}
	setup >> "${KRUIZE_SETUP_LOG}" 2>&1
	echo "Setting up kruize...Done" | tee -a ${LOG}
	sleep 15

	if [ "$cluster_type" == "minikube" ] || [ "$cluster_type" == "kind" ]; then
		NAMESPACE="monitoring"
		YAML_FILE="${LOCAL_MONITORING_TEST_DIR}/../../../manifests/crc/default-db-included-installation/minikube/kruize-crc-minikube.yaml"
	elif [ "$cluster_type" == "openshift" ]; then
		NAMESPACE="openshift-tuning"
		YAML_FILE="${LOCAL_MONITORING_TEST_DIR}/../../../manifests/crc/default-db-included-installation/openshift/kruize-crc-openshift.yaml"
	else
		echo "Invalid cluster type found: ${cluster_type}"
		return
	fi

	kubectl_cmd="kubectl -n ${NAMESPACE}"

	echo ""
	echo "******************* Executing test suite ${FUNCNAME} ****************"
	echo ""

	for scenario in "${!datasource_scenarios[@]}"; do
		echo ""
		echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
		echo " Running datasource scenario: ${scenario}"
		echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"

		run_datasource_scenario "$scenario"

		if [ "${TESTS_FAILED}" -ne "0" ]; then
			FAILED_CASES+=("${scenario}")
		fi
	done

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

run_datasource_scenario() {
	local scenario=$1
	POD_LOG="${TEST_SUITE_DIR}/${scenario}-pod.log"

	read PROM_SERVICE THANOS_SERVICE <<< "${datasource_scenarios[$scenario]}"

	echo "Updating YAML:"
	echo "  Prometheus serviceName = ${PROM_SERVICE}"
	echo "  Thanos serviceName     = ${THANOS_SERVICE}"

	update_yaml_with_datasources "${PROM_SERVICE}" "${THANOS_SERVICE}"

	$kubectl_cmd apply -f "$YAML_FILE" > /dev/null

	POD_NAME=$($kubectl_cmd get pods | grep kruize | grep -v -E 'db|ui' | awk 'NR==1{print $1}')
	[ -n "$POD_NAME" ] && $kubectl_cmd delete pod "$POD_NAME"

	if $kubectl_cmd wait --for=condition=Ready pod -l app=$APP_DEPLOYMENT --timeout=120s > /dev/null 2>&1; then
		echo "Kruize Pod is Ready"
		POD_NAME=$($kubectl_cmd get pods | grep kruize | grep -v -E 'db|ui' | awk 'NR==1{print $1}')
		$kubectl_cmd logs "$POD_NAME" > "$POD_LOG" 2>&1
	else
		echo "Kruize Pod failed to come up"
	fi

  if grep -i "No datasources could be added or are serviceable" "$POD_LOG"; then
    echo "Expected failure detected"
    ((TESTS_PASSED++))
  else
    echo "Expected failure NOT detected"
    ((TESTS_FAILED++))
  fi

	restore_yaml
}

update_yaml_with_datasources() {
	local prom_service=$1
	local thanos_service=$2

	sed -i.ds.bak '
	/"name": *"prometheus-1"/,/}/{
		s/"serviceName": *"[^"]*"/"serviceName": "'"$prom_service"'"/
	}
	/"name": *"thanos-1"/,/}/{
		s/"serviceName": *"[^"]*"/"serviceName": "'"$thanos_service"'"/
	}
	' "$YAML_FILE"
}

restore_yaml() {
	mv "${YAML_FILE}".ds.bak "$YAML_FILE"
}
