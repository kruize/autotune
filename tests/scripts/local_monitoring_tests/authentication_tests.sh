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

NAMESPACE="openshift-tuning"
APP_DEPLOYMENT="kruize"
DB_DEPLOYMENT="kruize-db-deployment"
DB_PVC="kruize-db-pv-claim"
SECRET_NAME="custom-token-secret" # TODO: to be updated
AUTOTUNE_IMAGE="quay.io/kruize/autotune_operator:0.0.25_mvp"
# Configuration
AUTH_TOKEN_PATH="/var/run/secrets/kubernetes.io/serviceaccount/token"
DATASOURCE_URL="https://prometheus-k8s.openshift-monitoring.svc.cluster.local:9091"
YAML_FILE="${LOCAL_MONITORING_TEST_DIR}/../../../manifests/crc/default-db-included-installation/openshift/kruize-crc-openshift.yaml"

# Tests to validate authentication types in Kruize
function authentication_tests() {
  TEST_SUITE_DIR="${RESULTS}/authentication_tests"
  mkdir -p ${TEST_SUITE_DIR} 2>&1
  for token_type in "${!tokens[@]}"; do
    deploy_and_check_pod $token_type
  done

# Define token scenarios
declare -A tokens
tokens=(
  ["valid"]="/var/run/secrets/kubernetes.io/serviceaccount/token"
  ["expired"]="EXPIRED_TOKEN"
  ["invalid"]="random-invalid-token-string"
  ["empty"]=""
)


# Update the YAML file with the token
update_yaml_with_token() {
  local token_value=$1
  # Escape special characters in the new token to avoid sed issues
  new_token_escaped=$(printf '%s\n' "$new_token" | sed -e 's/[\/&]/\\&/g')

  sed -i.bak 's/\("tokenFilePath": \)"[^"]*"/\1"'"$new_token_escaped"'"/' $YAML_FILE
}

# Deploy app and check pod status
deploy_and_check_pod() {
  local token_type=$1
  echo "**********************************"
  echo "Testing with $token_type token..."
  echo "**********************************"

  LOG="${TEST_SUITE_DIR}/${token_type}.log"
  echo "***********************************"
  echo "Terminating any existing instance of kruize..."
  echo "***********************************"
  kruize_terminate > /dev/null
  sleep 10

  # Update the secret with the appropriate token
  echo "*************************************"
  echo "Updating the yaml with $token_type token..."
  echo "*************************************"
  update_yaml_with_token "${tokens[$token_type]}"
  echo ""

  # Restart the app and db pod (if it's already running)
#  kubectl rollout restart deployment/$APP_DEPLOYMENT -n $NAMESPACE :TODO: to be used once the code is fixed
  # Run the deployment script again
  echo "**********************"
  echo "Redeploying kruize..."
  echo "**********************"
  ${LOCAL_MONITORING_TEST_DIR}/../../../deploy.sh -c ${cluster_type} -i ${AUTOTUNE_IMAGE} -m crc > /dev/null
  # Wait for the pod to be ready or fail
  kubectl wait --for=condition=Ready pod -l app=$APP_DEPLOYMENT -n $NAMESPACE --timeout=120s >> ${LOG} #2> /dev/null
  local pod_status=$?
  # Check pod logs for errors
  if [ $pod_status -ne 0 ]; then
    echo "$token_type token: Pod failed to start as expected."
    kubectl logs -l app=$APP_DEPLOYMENT -n $NAMESPACE --tail=20
  else
    echo "$token_type token: Pod started successfully (unexpected for invalid tokens)."
    kubectl logs -l app=$APP_DEPLOYMENT -n $NAMESPACE --tail=20
  fi

 # Check pod logs for errors
  echo "Checking logs for the pod..."
  pod_logs=$(kubectl logs -l app=$APP_DEPLOYMENT -n $NAMESPACE --tail=100)

  # Check if the log contains the error message
  if echo "$pod_logs" | grep -q "Datasource is not serviceable."; then
    echo "$token_type token: Failure detected in logs (as expected for invalid tokens)."
  else
    echo "$token_type token: No failure detected in logs (as expected for valid tokens)."
  fi
   # Restore original YAML file
  mv ${YAML_FILE}.bak $YAML_FILE

}

function kruize_terminate() {
    ${LOCAL_MONITORING_TEST_DIR}/../../../deploy.sh -c ${cluster_type} -i ${AUTOTUNE_IMAGE} -m crc -t
    # Wait for the pod to terminate
    while true; do
      # Get the status of the pod
      pod_name=$(kubectl get pod -l app=kruize -o jsonpath="{.items[0].metadata.name}" 2>/dev/null)
      namespace_status=$(kubectl get namespace $NAMESPACE --no-wait -o jsonpath='{.status.phase}' 2>/dev/null)


      # Check if the pod exists
      if [ -z "$pod_name" ]; then
        echo "Pod has fully terminated."
        break
      fi

      # Get the pod phase (Running, Succeeded, Failed, etc.)
      pod_phase=$(kubectl get pod $pod_name -o jsonpath='{.status.phase}' 2>/dev/null)

      # Check the pod phase
      if [ "$pod_phase" == "Succeeded" ] || [ "$pod_phase" == "Failed" ]; then
        echo "Pod has terminated with status: $pod_phase."
        break
      fi

      echo "Waiting for pod to terminate..."
      sleep 5
    done
}
