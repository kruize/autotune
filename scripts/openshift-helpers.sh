#!/bin/bash
#
# Copyright (c) 2020, 2020 Red Hat Corporation and others.
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

###############################  v OpenShift v ################################

# Check if the oc tool is installed
function openshift_prereq() {
	# Check if oc tool is installed
	echo
	echo -n "Info: Checking pre requisites for OpenShift..."
	oc_tool=$(which oc)
	check_err "Error: Please install the oc tool"
	echo "done"
}

# Create a service account for kruize to be deployed into and setup the proper RBAC for it
function openshift_first() {
	autotune_ns="openshift-monitoring"
	oc_cmd="oc -n ${autotune_ns}"
	kubectl_cmd="kubectl -n ${autotune_ns}"
	# Login to the cluster
	echo "Info: Logging in to OpenShift cluster..."
	if [ ${non_interactive} == 1 ]; then
		oc login ${kurl} -u ${user} -p ${password} -n ${autotune_ns} 
	elif [ ! -z ${kurl} ]; then
		oc login ${kurl}
	else
		oc login
	fi
	check_err "Error: oc login failed."

	# Check if the service account already exists
	sa_exists=$(${oc_cmd} get sa | grep ${SA_NAME})
	if [ "${sa_exists}" != "" ]; then
		return;
	fi
	echo "Info: One time setup - Create a service account to deploy kruize"
	sed -e "s/{{ KRUIZE_NAMESPACE }}/${autotune_ns}/" ${SA_TEMPLATE} > ${SA_MANIFEST}
	${oc_cmd} apply -f ${SA_MANIFEST}
	check_err "Error: Failed to create service account and RBAC"
	${oc_cmd} apply -f ${SERVICE_MONITOR_MANIFEST}
	check_err "Error: Failed to create service monitor for Prometheus"
}

# Update yaml with the current OpenShift instance specific details
function openshift_setup() {
	# Get the bearer token
	br_token=$(oc whoami --show-token)
	br_token="Bearer ${br_token}"
	pservice=""
	prom_path=$(oc get route --all-namespaces=true | grep prometheus-k8s | awk '{ print $3 }')
	purl="https://${prom_path}"
	echo
	echo "Info: Setting Prometheus URL as ${purl}"
	sleep 1

	sed -e "s/{{ K8S_TYPE }}/OpenShift/" ${DEPLOY_TEMPLATE} > ${DEPLOY_MANIFEST}
	sed -ie "s|{{ KRUIZE_DOCKER_IMAGE }}|${KRUIZE_DOCKER_IMAGE}|" ${DEPLOY_MANIFEST}
	sed -ie "s/{{ BEARER_AUTH_TOKEN }}/${br_token}/" ${DEPLOY_MANIFEST}
	sed -ie "s/{{ MONITORING_SERVICE }}/${pservice}/" ${DEPLOY_MANIFEST}
	sed -ie "s|{{ MONITORING_AGENT_ENDPOINT }}|${purl}|" ${DEPLOY_MANIFEST}
}

# Deploy to the openshift-monitoring namespace for OpenShift
function openshift_deploy() {
	echo "Info: Deploying kruize yaml to OpenShift cluster"
	# Deploy into the "openshift-monitoring" namespace/project
	oc project ${autotune_ns}
	${oc_cmd} apply -f ${DEPLOY_MANIFEST}
	sleep 2
	check_running kruize
	# Indicate deploy failed on error
	if [ "${err}" != "0" ]; then
		exit 1
	fi
}

function openshift_start() {
	echo
	echo "###   Installing kruize for OpenShift"
	echo
	echo "WARNING: This will create a Kruize ServiceMonitor object in the openshift-monitoring namespace"
	echo "WARNING: This is currently not recommended for production"
	echo

	if [ ${non_interactive} == 0 ]; then
		echo -n "Create ServiceMonitor object and continue installation?(y/n)? "
		read inst
		linst=$(echo ${inst} | tr A-Z a-z)
		if [ ${linst} == "n" ]; then
			echo "Info: kruize not installed"
			exit 0
		fi
	fi

	openshift_prereq
	openshift_first
	openshift_setup
	openshift_deploy
}

function openshift_terminate() {
	# Add OpenShift cleanup code
	autotune_ns="openshift-monitoring"
	oc_cmd="oc -n ${autotune_ns}"

	echo
	echo "Terminating kruize..."

	echo
	echo "Removing kruize service account"
	${oc_cmd} delete -f ${SA_MANIFEST} 2>/dev/null

	echo
	echo "Removing kruize deployment from Openshift cluster"
	${oc_cmd} delete -f ${DEPLOY_MANIFEST} 2>/dev/null

	echo
	echo "Removing kruize serviceMonitor"
	${oc_cmd} delete -f ${SERVICE_MONITOR_MANIFEST} 2>/dev/null

	echo
	echo "Removing generated manifest files"
	rm ${DEPLOY_MANIFEST}
	rm ${SA_MANIFEST}

	echo "done"
}

###############################  ^ OpenShift ^ ################################
