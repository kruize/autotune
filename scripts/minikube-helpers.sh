#!/bin/bash
#
# Copyright (c) 2020, 2021 Red Hat, IBM Corporation and others.
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

###############################  v MiniKube v #################################

function minikube_first() {

	kubectl_cmd="kubectl -n ${autotune_ns}"
	echo "Info: One time setup - Create a service account to deploy autotune"
	
	${kubectl_cmd} apply -f ${AUTOTUNE_SA_MANIFEST}
	check_err "Error: Failed to create service account and RBAC"

	${kubectl_cmd} apply -f ${AUTOTUNE_OPERATOR_CRD}
	check_err "Error: Failed to create autotune CRD"

	${kubectl_cmd} apply -f ${AUTOTUNE_CONFIG_CRD}
	check_err "Error: Failed to create autotuneconfig CRD"

	${kubectl_cmd} apply -f ${AUTOTUNE_QUERY_VARIABLE_CRD}
	check_err "Error: Failed to create autotunequeryvariable CRD"

	${kubectl_cmd} apply -f ${AUTOTUNE_ROLE_MANIFEST}
	check_err "Error: Failed to create role"

	sed -e "s|{{ AUTOTUNE_NAMESPACE }}|${autotune_ns}|" ${AUTOTUNE_RB_MANIFEST_TEMPLATE} > ${AUTOTUNE_RB_MANIFEST}
	${kubectl_cmd} apply -f ${AUTOTUNE_RB_MANIFEST}
	check_err "Error: Failed to create role binding"

	${kubectl_cmd} apply -f ${SERVICE_MONITOR_MANIFEST}
	check_err "Error: Failed to create service monitor for Prometheus"
}

# You can deploy using kubectl
function minikube_deploy() {
	echo
	echo "Creating environment variable in minikube cluster using configMap"
	${kubectl_cmd} apply -f ${AUTOTUNE_CONFIGMAPS}/${cluster_type}-config.yaml

	echo
	echo "Deploying AutotuneConfig objects"
	${kubectl_cmd} apply -f ${AUTOTUNE_CONFIGS}

	echo
	echo "Deploying AutotuneQueryVariable objects"
	${kubectl_cmd} apply -f ${AUTOTUNE_QUERY_VARIABLES}
	
	echo "Info: Deploying autotune yaml to minikube cluster"

	# Replace autotune docker image in deployment yaml
	sed -e "s|{{ AUTOTUNE_IMAGE }}|${AUTOTUNE_DOCKER_IMAGE}|" ${AUTOTUNE_DEPLOY_MANIFEST_TEMPLATE} > ${AUTOTUNE_DEPLOY_MANIFEST}

	${kubectl_cmd} apply -f ${AUTOTUNE_DEPLOY_MANIFEST}
	sleep 2
	check_running autotune
	if [ "${err}" != "0" ]; then
		# Indicate deploy failed on error
		exit 1
	fi

	# Get the Autotune application port in minikube
	MINIKUBE_IP=$(minikube ip)
	AUTOTUNE_PORT=$(${kubectl_cmd} get svc autotune --no-headers -o=custom-columns=PORT:.spec.ports[*].nodePort)
	echo "Info: Access Autotune at http://${MINIKUBE_IP}:${AUTOTUNE_PORT}/listAutotuneTunables"
	echo
}

function minikube_start() {
	echo
	echo "###   Installing autotune for minikube"
	echo

	# If autotune_ns was not set by the user
	if [ -z "$autotune_ns" ]; then
		autotune_ns="monitoring"
	fi

	check_prometheus_installation
	minikube_first
	minikube_deploy
}

function check_prometheus_installation() {
	echo
	echo "Info: Checking pre requisites for minikube..."
	kubectl_tool=$(which kubectl)
	check_err "Error: Please install the kubectl tool"
	# Check to see if kubectl supports kustomize
	kubectl kustomize --help >/dev/null 2>/dev/null
	check_err "Error: Please install a newer version of kubectl tool that supports the kustomize option (>=v1.12)"

	kubectl_cmd="kubectl"
	prometheus_pod_running=$(${kubectl_cmd} get pods --all-namespaces | grep "prometheus-k8s-1")
	if [ "${prometheus_pod_running}" == "" ]; then
		echo "Prometheus is not running, use 'scripts/prometheus_on_minikube.sh' to install."
		exit 1
	fi
	echo "Prometheus is installed and running."
}


function minikube_terminate() {
	# If autotune_ns was not set by the user
	if [ -z "$autotune_ns" ]; 	then
		autotune_ns="monitoring"
	fi

	echo -n "###   Removing autotune for minikube"

	kubectl_cmd="kubectl -n ${autotune_ns}"

	echo
	echo "Removing autotune"
	${kubectl_cmd} delete -f ${AUTOTUNE_DEPLOY_MANIFEST} 2>/dev/null

	echo
	echo "Removing autotune service account"
	${kubectl_cmd} delete -f ${AUTOTUNE_SA_MANIFEST} 2>/dev/null

	echo
	echo "Removing autotune role"
	${kubectl_cmd} delete -f ${AUTOTUNE_ROLE_MANIFEST} 2>/dev/null

	echo
	echo "Removing autotune rolebinding"
	${kubectl_cmd} delete -f ${AUTOTUNE_RB_MANIFEST} 2>/dev/null

	echo
	echo "Removing autotune serviceMonitor"
	${kubectl_cmd} delete -f ${SERVICE_MONITOR_MANIFEST} 2>/dev/null

	echo
	echo "Removing AutotuneConfig objects"
	${kubectl_cmd} delete -f ${AUTOTUNE_CONFIGS} 2>/dev/null

	echo
	echo "Removing AutotuneQueryVariable objects"
	${kubectl_cmd} delete -f ${AUTOTUNE_QUERY_VARIABLES} 2>/dev/null
	
	echo
	echo "Removing Autotune configmap"
	${kubectl_cmd} delete -f ${AUTOTUNE_CONFIGMAPS}/${cluster_type}-config.yaml 2>/dev/null

	echo
	echo "Removing Autotune CRD"
	${kubectl_cmd} delete -f ${AUTOTUNE_OPERATOR_CRD} 2>/dev/null

	echo
	echo "Removing AutotuneConfig CRD"
	${kubectl_cmd} delete -f ${AUTOTUNE_CONFIG_CRD} 2>/dev/null

	echo
	echo "Removing AutotuneQueryVariables CRD"
	${kubectl_cmd} delete -f ${AUTOTUNE_QUERY_VARIABLE_CRD} 2>/dev/null

	rm ${AUTOTUNE_DEPLOY_MANIFEST}
	rm ${AUTOTUNE_RB_MANIFEST}
}
