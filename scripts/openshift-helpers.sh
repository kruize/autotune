#!/bin/bash
#
# Copyright (c) 2021, 2022 Red Hat, IBM Corporation and others.
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

###############################  v OPENSHIFT v #################################

function check_openshift_prometheus_installation() {
	echo
	echo "Info: Checking pre requisites for openshift..."
	check_kustomize
	kubectl_cmd="kubectl"
	prometheus_pod_running=$(${kubectl_cmd} get pods --all-namespaces | grep "prometheus-k8s")
	if [ "${prometheus_pod_running}" == "" ]; then
		echo "Prometheus is not running."
		exit 1
	fi
	echo "Prometheus is installed and running."
}

function openshift_first() {
	#Create a namespace
	echo "Create autotune namespace ${autotune_ns}"
	kubectl create namespace ${autotune_ns}

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

	sed -e "s|{{ AUTOTUNE_NAMESPACE }}|${autotune_ns}|" ${AUTOTUNE_RB_MANIFEST_TEMPLATE} >${AUTOTUNE_RB_MANIFEST}
	${kubectl_cmd} apply -f ${AUTOTUNE_RB_MANIFEST}
	check_err "Error: Failed to create role binding"

	sed -e "s|{{ AUTOTUNE_NAMESPACE }}|${autotune_ns}|" -e "s|{{ CLUSTER_TYPE }}|openshift|" ${AUTOTUNE_QUERY_VARIABLES_MANIFEST_TEMPLATE} >${AUTOTUNE_QUERY_VARIABLES_MANIFEST}
	${kubectl_cmd} apply -f ${AUTOTUNE_QUERY_VARIABLES_MANIFEST}
	check_err "Error: Failed to create query variables"

	${kubectl_cmd} apply -f ${SERVICE_MONITOR_MANIFEST}
	check_err "Error: Failed to create service monitor for Prometheus"
}

# You can deploy using kubectl
function openshift_deploy() {
	echo
	echo "Creating environment variable in openshift cluster using configMap"
	${kubectl_cmd} apply -f ${AUTOTUNE_CONFIGMAPS}/${cluster_type}-config.yaml

	echo
	echo "Deploying AutotuneConfig objects"
	${kubectl_cmd} apply -f ${AUTOTUNE_CONFIGS}

	echo "Info: Deploying autotune yaml to openshift cluster"

	# Replace autotune docker image in deployment yaml
	sed -e "s|{{ AUTOTUNE_IMAGE }}|${AUTOTUNE_DOCKER_IMAGE}|" ${AUTOTUNE_DEPLOY_MANIFEST_TEMPLATE} >${AUTOTUNE_DEPLOY_MANIFEST}
	sed -i "s|{{ HPO_IMAGE }}|${HPO_DOCKER_IMAGE}|" ${AUTOTUNE_DEPLOY_MANIFEST}

	${kubectl_cmd} apply -f ${AUTOTUNE_DEPLOY_MANIFEST}
	sleep 2
	check_running autotune ${autotune_ns}
	if [ "${err}" != "0" ]; then
		# Indicate deploy failed on error
		exit 1
	fi

	# Get the Autotune application port in openshift
	OPENSHIFT_IP=$(${kubectl_cmd} get pods -l=app=autotune -o wide -n ${autotune_ns} -o=custom-columns=NODE:.spec.nodeName --no-headers)
	AUTOTUNE_PORT=$(${kubectl_cmd} get svc autotune --no-headers -o=custom-columns=PORT:.spec.ports[*].nodePort)
	echo "Info: Access Autotune at http://$OPENSHIFT_IP:${AUTOTUNE_PORT}/listAutotuneTunables"
	echo
}

function openshift_start() {
	echo
	echo "###   Installing autotune for openshift"
	echo

	# If autotune_ns was not set by the user
	if [ -z "$autotune_ns" ]; then
		autotune_ns=${AUTOTUNE_OPENSHIFT_NAMESPACE}
	fi

	check_openshift_prometheus_installation
	openshift_first
	openshift_deploy
}

function openshift_terminate() {
	# If autotune_ns was not set by the user
	if [ -z "$autotune_ns" ]; then
		autotune_ns=${AUTOTUNE_OPENSHIFT_NAMESPACE}
	fi

	echo -n "###   Removing autotune for openshift"

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
	${kubectl_cmd} delete -f ${AUTOTUNE_QUERY_VARIABLES_MANIFEST} 2>/dev/null

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

	echo
	echo "Removing Autotune namespace"
	kubectl delete ns ${autotune_ns}
}
