#!/bin/bash
#
# Copyright (c) 2024, 2024 Red Hat, IBM Corporation and others.
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
CURRENT_DIR="$(dirname "$(realpath "$0")")"
LOCAL_MONITORING_TEST_DIR="${CURRENT_DIR}/local_monitoring_tests"

# Source the common functions scripts
. ${LOCAL_MONITORING_TEST_DIR}/../common/common_functions.sh

APP_DEPLOYMENT="kruize"

# Define token scenarios
declare -A tokens
tokens=(
  ["valid"]="/var/run/secrets/kubernetes.io/serviceaccount/token"
  ["expired"]="EXPIRED_TOKEN"
  ["invalid"]="/var/run/secrets/kubernetes.io/serviceaccount/token2"
  ["empty"]=""
)
# Tests to validate authentication types in Kruize
function authentication_tests() {
	start_time=$(get_date)
	FAILED_CASES=()
	TESTS=0
	failed=0
	((TOTAL_TEST_SUITES++))

  TEST_SUITE_DIR="${RESULTS}/authentication_tests"
  mkdir -p "${TEST_SUITE_DIR}" 2>&1
  KRUIZE_SETUP_LOG="${TEST_SUITE_DIR}/kruize_setup.log"
 	KRUIZE_POD_LOG="${TEST_SUITE_DIR}/kruize_pod.log"
 	target="crc"
  echo ""
  echo "Setting up kruize..." | tee -a ${LOG}
		echo "${KRUIZE_SETUP_LOG}"
		setup "${KRUIZE_POD_LOG}" >> "${KRUIZE_SETUP_LOG}" 2>&1
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
	fi
  kubectl_cmd="kubectl -n ${NAMESPACE}"

  echo ""
	echo "******************* Executing test suite ${FUNCNAME} ****************"
	echo ""
  for token_type in "${!tokens[@]}";
  do
		echo ""
		echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
		echo "                    Running Test ${token_type}-token"
		echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"

    deploy_and_check_pod "$token_type"

#	 check for success and failed cases here
		if [ "${TESTS_FAILED}" -ne "0" ]; then
			FAILED_CASES+=(${test})
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

	# Remove the duplicates
	FAILED_CASES=( $(printf '%s\n' "${FAILED_CASES[@]}" | uniq ) )

	# print the testsuite summary
	testsuitesummary ${FUNCNAME} "${elapsed_time}" ${FAILED_CASES}
}

# Deploy app and check pod status
deploy_and_check_pod() {
  local token_type=$1
  POD_LOG="${TEST_SUITE_DIR}/${token_type}-pod.log"

  # Update the yaml with the appropriate token
  echo "*************************************"
  echo "Updating the yaml with $token_type token and restarting kruize..."
  echo "*************************************"
  update_yaml_with_token "${tokens[$token_type]}"
  echo ""

  # re-apply the yaml to update the auth config
  $kubectl_cmd apply -f "$YAML_FILE" > /dev/null
	# get the kruize pod name
	POD_NAME=$($kubectl_cmd get pods | grep 'kruize' | grep -v -E 'kruize-db|kruize-ui' | awk 'NR==1{print $1}')
	# Check if POD_NAME is not empty
	if [ -n "$POD_NAME" ]; then
		# Delete the pod
		$kubectl_cmd delete pod "$POD_NAME"
	else
		echo "No matching pod found to delete."
	fi

  # Wait for the new pod to be ready or fail
  $kubectl_cmd wait --for=condition=Ready pod -l app=$APP_DEPLOYMENT --timeout=120s > /dev/null
 # Check pod logs for errors
  echo "Checking logs for the pod..."
 	POD_NAME=$($kubectl_cmd get pods | grep 'kruize' | grep -v -E 'kruize-db|kruize-ui' | awk 'NR==1{print $1}')
  echo "$kubectl_cmd logs -f ${POD_NAME} > ${POD_LOG} 2>&1 &"
	$kubectl_cmd logs -f "${POD_NAME}" > "${POD_LOG}" 2>&1 &
  sleep 10
	echo ""
  # Determine the test outcome based on logs
	if [[ $(grep -i "Datasource connection refused or timed out" ${POD_LOG}) ]]; then
    if [ "$token_type" == "valid" ]; then
      echo "$token_type token: Unexpected failure detected in logs."
      ((TESTS_FAILED++)) # Increment the global TESTS_FAILED
    else
      echo "$token_type token: Failure detected in logs (as expected for invalid tokens)."
      ((TESTS_PASSED++)) # Increment the global TESTS_PASSED
    fi
  else
    if [ "$token_type" == "valid" ]; then
      echo "$token_type token: No failure detected in logs (as expected for valid tokens)."
      ((TESTS_PASSED++)) # Increment the global TESTS_PASSED
    else
      echo "$token_type token: Unexpected success detected in logs."
      ((TESTS_FAILED++)) # Increment the global TESTS_FAILED
    fi
  fi
   # Restore original YAML file
  mv "${YAML_FILE}".token.bak "$YAML_FILE"
  mv "${YAML_FILE}".image.bak "$YAML_FILE"
}

# Update the YAML file with the token
update_yaml_with_token() {
  local token_value=$1

  # Escape special characters in the new token to avoid sed issues
  new_token_escaped=$(printf '%s\n' "$token_value" | sed -e 's/[\/&]/\\&/g')

  # Update the tokenFilePath
  sed -i.token.bak 's/\("tokenFilePath": \)"[^"]*"/\1"'"$new_token_escaped"'"/' "$YAML_FILE"
  echo "Token updated"

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
