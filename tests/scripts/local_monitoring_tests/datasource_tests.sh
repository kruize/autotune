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

# Get the absolute path of current directory
LOCAL_MONITORING_TEST_DIR="${KRUIZE_REPO}/tests/scripts/local_monitoring_tests"

# Source the common functions scripts
. ${LOCAL_MONITORING_TEST_DIR}/../common/common_functions.sh

APP_DEPLOYMENT="kruize"

# Datasource serviceName overrides to simulate reachability
declare -A datasource_scenarios
datasource_scenarios=(
  ["both-invalid"]="invalid invalid"
  ["both-valid"]="prometheus-k8s thanos-querier"
  ["prom-valid-thanos-invalid"]="prometheus-k8s invalid-thanos"
  ["prom-invalid-thanos-valid"]="invalid-prometheus thanos-querier"
)
datasource_scenario_order=(
  "both-invalid"
  "prom-invalid-thanos-valid"
  "prom-valid-thanos-invalid"
  "both-valid"
)

function datasource_tests() {
	start_time=$(get_date)
	FAILED_CASES=()
	TESTS=0
	TESTS_FAILED=0
	TESTS_PASSED=0
	((TOTAL_TEST_SUITES++))

	TEST_SUITE_DIR="${RESULTS}/datasource_tests"
	mkdir -p "${TEST_SUITE_DIR}" 2>&1
	KRUIZE_SETUP_LOG="${TEST_SUITE_DIR}/kruize_setup.log"
	KRUIZE_POD_LOG="${TEST_SUITE_DIR}/kruize_pod.log"
  target="crc"
	echo ""
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
	echo "Setting up kruize..." | tee -a ${LOG}
	echo "${KRUIZE_SETUP_LOG}"
	cleanup_datasources_from_yaml
	pushd "${KRUIZE_REPO}" > /dev/null
    setup "${KRUIZE_POD_LOG}" >> "${KRUIZE_SETUP_LOG}" 2>&1
    echo "Setting up kruize...Done" | tee -a ${LOG}
    sleep 10
	popd > /dev/null
	# restore the yaml once the setup is done
	mv "${YAML_FILE}.pre_setup.bak" "$YAML_FILE"

	kubectl_cmd="kubectl -n ${NAMESPACE}"

	echo ""
	echo "******************* Executing test suite ${FUNCNAME} ****************"
	echo ""

  suffix=1
	for scenario in "${datasource_scenario_order[@]}"; do
		echo ""
		echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
		echo " Running datasource scenario: ${scenario}"
		echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
		run_datasource_scenario "$scenario" "$suffix"

		if [ "${TESTS_FAILED}" -ne "0" ]; then
			FAILED_CASES+=("${scenario}")
		fi
		((suffix++))
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

	# Remove the duplicates
	FAILED_CASES=( $(printf '%s\n' "${FAILED_CASES[@]}" | uniq ) )

	# print the testsuite summary
	testsuitesummary ${FUNCNAME} "${elapsed_time}" ${FAILED_CASES}
}

run_datasource_scenario() {
	local scenario=$1
	suffix=$2
	POD_LOG="${TEST_SUITE_DIR}/${scenario}-pod.log"

	read PROM_SERVICE THANOS_SERVICE <<< "${datasource_scenarios[$scenario]}"

	echo "Updating YAML:"
	echo "  Prometheus serviceName = ${PROM_SERVICE}"
	echo "  Thanos serviceName     = ${THANOS_SERVICE}"

	update_yaml_with_datasources "${PROM_SERVICE}" "${THANOS_SERVICE}" "${suffix}"

	$kubectl_cmd apply -f "$YAML_FILE" > /dev/null
  $kubectl_cmd rollout restart deployment kruize
  sleep 10

	if $kubectl_cmd wait --for=condition=Ready pod -l app=$APP_DEPLOYMENT --timeout=120s > /dev/null 2>&1; then
		echo "Kruize Pod is Ready"
		POD_NAME=$($kubectl_cmd get pods | grep 'kruize' | grep -v -E 'kruize-db|kruize-ui' | awk 'NR==1{print $1}')
		sleep 5
		$kubectl_cmd logs "$POD_NAME" > "$POD_LOG" 2>&1

		if [[ "$scenario" == "both-invalid" ]]; then
		  echo "inside both-invalid"
      if grep -i "No datasource could be added or are serviceable" "$POD_LOG"; then
        echo "Expected failure detected (both datasources invalid)"
        ((TESTS_PASSED++))
      else
        echo "Expected failure NOT detected"
        ((TESTS_FAILED++))
      fi
    else
      if grep -i "No datasource could be added" "$POD_LOG"; then
        echo "Unexpected startup failure"
        ((TESTS_FAILED++))
      else
        echo "Startup succeeded as expected"
        ((TESTS_PASSED++))
      fi
    fi
	else
		echo "Kruize Pod failed to come up"
	fi

	restore_yaml
}

cleanup_datasources_from_yaml() {
  echo "Cleaning up datasources from YAML before setup..."

  # Backup once
  cp "$YAML_FILE" "${YAML_FILE}.pre_setup.bak"

  # Remove the entire "datasource" block
  sed -i '
  /"datasource"[[:space:]]*:[[:space:]]*\[/,/]/d
  ' "$YAML_FILE"
}

update_yaml_with_datasources() {
	local prom_service=$1
	local thanos_service=$2
	# create unique datasource name for each scenario
	SUFFIX=$3
	PROM_DS_NAME="prometheus-${SUFFIX}"
	THANOS_DS_NAME="thanos-${SUFFIX}"

	echo "Using datasource names:"
	echo "  Prometheus: ${PROM_DS_NAME}"
	echo "  Thanos:     ${THANOS_DS_NAME}"

	# Backup once
	cp "$YAML_FILE" "${YAML_FILE}.ds.bak"

	sed -i '
	/"name": *"prometheus-1"/,/}/{
		s/"name": *"[^"]*"/"name": "'"$PROM_DS_NAME"'"/
		s/"serviceName": *"[^"]*"/"serviceName": "'"$prom_service"'"/
	}
	/"name": *"thanos-1"/,/}/{
		s/"name": *"[^"]*"/"name": "'"$THANOS_DS_NAME"'"/
		s/"serviceName": *"[^"]*"/"serviceName": "'"$thanos_service"'"/
	}
	' "$YAML_FILE"

	# Update the image in the Deployment YAML
  sed -i.image.bak '
	/kind: Deployment/,/kind:/{
		/name: kruize$/,/containers:/{
			/^        - name: kruize$/{
				n
				s|image: .*|image: '"$AUTOTUNE_IMAGE"'|
			}
		}
	}' "$YAML_FILE"
  echo "Updated image in YAML to $AUTOTUNE_IMAGE"
}

restore_yaml() {
	mv "${YAML_FILE}".ds.bak "$YAML_FILE"
	mv "${YAML_FILE}".image.bak "$YAML_FILE"
}
