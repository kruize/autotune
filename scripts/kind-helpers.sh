#!/bin/bash
#
# Copyright (c) 2025 IBM Corporation and others.
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

###############################  v Kind v #################################

# Deploy kruize in remote monitoring mode
function check_prometheus_installation_on_kind() {
	echo
	echo "Info: Checking pre requisites for Kind..."
	check_kustomize

	kubectl_cmd="kubectl"
	prometheus_pod_running=$(${kubectl_cmd} get pods --all-namespaces | grep "prometheus-k8s-1")
	if [ "${prometheus_pod_running}" == "" ]; then
		echo "Prometheus is not running, use 'scripts/prometheus_on_kind.sh' to install."
		exit 1
	fi
	echo "Prometheus is installed and running."
}

function kind_crc_start() {
  echo
  echo "#######################################"
  echo "Checking if kind is installed"
  check_kind

	# If autotune_ns was not set by the user
	if [ -z "$autotune_ns" ]; then
		autotune_ns="monitoring"
	fi
	kubectl create namespace "${autotune_ns}"

	check_prometheus_installation_on_kind

	CRC_MANIFEST_FILE=${KRUIZE_CRC_DEPLOY_MANIFEST_MINIKUBE}

	kruize_crc_start
}

function kind_crc_terminate() {
	# If autotune_ns was not set by the user
	if [ -z "$autotune_ns" ]; then
		autotune_ns="monitoring"
	fi
	kubectl_cmd="kubectl -n ${autotune_ns}"
	CRC_MANIFEST_FILE=${KRUIZE_CRC_DEPLOY_MANIFEST_MINIKUBE}

	echo -n "###   Removing Kruize for kind"
	echo
	${kubectl_cmd} delete -f ${CRC_MANIFEST_FILE} 2>/dev/null
}

###########################################
#   Check if kind is installed
###########################################
function check_kind() {
	if ! which kind >/dev/null 2>/dev/null; then
		echo "ERROR: Please install kind and try again"
		exit 1
	fi
	echo "✅ 'kind' is installed."

  # 2. Check if a kind cluster is running
  # (Matches default 'kind' or any user-defined kind cluster name)
  KIND_CLUSTER_COUNT=$(kind get clusters 2>/dev/null | wc -l)

  if [ "$KIND_CLUSTER_COUNT" -eq 0 ]; then
    echo "❌ ERROR: No running Kind cluster found!"
    echo "👉 Please start a cluster before running this script, e.g.:"
    echo "   kind create cluster"
    exit 1
  fi

  echo "✅ A Kind cluster is running:"
  kind get clusters
  echo
}

