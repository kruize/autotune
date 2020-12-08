#!/bin/bash
#
# Copyright (c) 2020, 2020 Red Hat, IBM Corporation and others.
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
	
	autotune_ns="monitoring"
	kubectl_cmd="kubectl -n ${autotune_ns}"

	# Check if the service account already exists
	sa_exists=$(${kubectl_cmd} get sa | grep ${AUTOTUNE_SA_NAME})
	if [ "${sa_exists}" != "" ]; then
		return;
	fi
	echo
	echo "Info: One time setup - Create a service account to deploy autotune"
	

	sed -ie "s/{{ AUTOTUNE_NAMESPACE }}/${autotune_ns}/" ${AUTOTUNE_SA_MANIFEST}
	${kubectl_cmd} apply -f ${AUTOTUNE_SA_MANIFEST}
	check_err "Error: Failed to create service account and RBAC"

	${kubectl_cmd} apply -f ${AUTOTUNE_CRD_MANIFEST}
	check_err "Error: Failed to create autotune CRD"

	sed -ie "s/{{ AUTOTUNE_NAMESPACE }}/${autotune_ns}/" ${AUTOTUNE_ROLE_MANIFEST}
	${kubectl_cmd} apply -f ${AUTOTUNE_ROLE_MANIFEST}
	check_err "Error: Failed to create role"

	sed -ie "s/{{ AUTOTUNE_NAMESPACE }}/${autotune_ns}/" ${AUTOTUNE_RB_MANIFEST}
	${kubectl_cmd} apply -f ${AUTOTUNE_RB_MANIFEST}
	check_err "Error: Failed to create role binding"

	${kubectl_cmd} apply -f ${SERVICE_MONITOR_MANIFEST}
	check_err "Error: Failed to create service monitor for Prometheus"
}

# You can deploy using kubectl
function minikube_deploy() {
	echo
	echo "clearning existing autotune-config map"
	${kubectl_cmd} delete cm autotune-config 

	echo
	echo "Creating environment variable in minikube cluster using configMap"
	${kubectl_cmd} create cm autotune-config --from-file=autotune-config-key=${MINIKUBE_ENV}
	
	
	echo "Info: Deploying autotune yaml to minikube cluster"

	sed -ie "s/{{ AUTOTUNE_NAMESPACE }}/${autotune_ns}/" ${AUTOTUNE_DEPLOY_MANIFEST}
	${kubectl_cmd} apply -f ${AUTOTUNE_DEPLOY_MANIFEST}
	sleep 2
	check_running autotune
	if [ "${err}" == "0" ]; then
		grafana_pod=$(${kubectl_cmd} get pods | grep grafana | awk '{ print $1 }')
		echo "Info: Access grafana dashboard to see autotune recommendations at http://localhost:3000"
		echo "Info: Run the following command first to access grafana port"
		echo "      $ kubectl port-forward -n monitoring ${grafana_pod} 3000:3000"
		echo
	else
		# Indicate deploy failed on error
		exit 1
	fi
}

function minikube_start() {
	echo
	echo "###   Installing autotune for minikube"
	echo
	check_install_required_tools
	minikube_first
	minikube_deploy
}

function minikube_terminate() {
	echo -n "###   Removing autotune for minikube"

	autotune_ns="monitoring"
	kubectl_cmd="kubectl -n ${autotune_ns}"

	echo
	echo "Removing autotune"
	${kubectl_cmd} delete -f ${AUTOTUNE_DEPLOY_MANIFEST} 2>/dev/null

	echo
	echo "Removing autotune service account"
	${kubectl_cmd} delete -f ${AUTOTUNE_SA_MANIFEST} 2>/dev/null

	echo
	echo "Removing autotune serviceMonitor"
	${kubectl_cmd} delete -f ${SERVICE_MONITOR_MANIFEST} 2>/dev/null

	pushd minikube_downloads > /dev/null
		echo
		echo "Removing cadvisor"
		pushd cadvisor/deploy/kubernetes/base > /dev/null
		kubectl kustomize . | kubectl delete -f-
		popd > /dev/null
		
		echo
		echo "Removing prometheus"
		pushd kube-prometheus/manifests > /dev/null
		kubectl delete -f . 2>/dev/null
		kubectl delete -f setup 2>/dev/null
		popd > /dev/null
	popd > /dev/null

	rm ${AUTOTUNE_DEPLOY_MANIFEST}
	rm ${AUTOTUNE_SA_MANIFEST}
	
	rm -rf minikube_downloads
}
