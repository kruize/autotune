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

# Create a service account for autotune to be deployed into and setup the proper RBAC for it
function openshift_first() {
	autotune_ns="openshift-monitoring"
	oc_cmd="oc -n ${autotune_ns}"
	kubectl_cmd="kubectl -n ${autotune_ns}"
	# Login to the cluster
	echo "Info: Logging in to OpenShift cluster..."
	#echo " kurl: ${kurl} ,  user:${user} ,  password: ${password}"
	if [ ${non_interactive} == 1 ]; then
		oc login ${kurl} -n ${autotune_ns} 
	elif [ ! -z ${kurl} ]; then
		oc login ${kurl}
	else
		oc login
	fi
	check_err "Error: oc login failed."

	# Check if the service account already exists
	sa_exists=$(${oc_cmd} get sa | grep ${AUTOTUNE_SA_NAME})
	if [ "${sa_exists}" != "" ]; then
		return;
	fi
	echo "Info: One time setup - Create a service account to deploy autotune"
	sed -ie "s/{{ AUTOTUNE_NAMESPACE }}/${autotune_ns}/" ${AUTOTUNE_SA_MANIFEST}
	${oc_cmd} apply -f ${AUTOTUNE_SA_MANIFEST}
	check_err "Error: Failed to create service account and RBAC"

	${oc_cmd} apply -f ${AUTOTUNE_CRD_MANIFEST}
	check_err "Error: Failed to create autotune CRD"

	sed -ie "s/{{ AUTOTUNE_NAMESPACE }}/${autotune_ns}/" ${AUTOTUNE_ROLE_MANIFEST}
	${kubectl_cmd} apply -f ${AUTOTUNE_ROLE_MANIFEST}
	check_err "Error: Failed to create role"

	sed -ie "s/{{ AUTOTUNE_NAMESPACE }}/${autotune_ns}/" ${AUTOTUNE_RB_MANIFEST}
	${kubectl_cmd} apply -f ${AUTOTUNE_RB_MANIFEST}
	check_err "Error: Failed to create role binding"
	
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

	sed -ie "s/{{ BEARER_AUTH_TOKEN }}/${br_token}/" ${OPENSHIFT_ENV}
	sed -ie "s/{{ MONITORING_SERVICE }}/${pservice}/" ${OPENSHIFT_ENV}
	sed -ie "s|{{ MONITORING_AGENT_ENDPOINT }}|${purl}|" ${OPENSHIFT_ENV}
}

# Deploy to the openshift-monitoring namespace for OpenShift
function openshift_deploy() {
	echo
	is_config_exist=$(${oc_cmd} get cm autotune-config | grep autotune-config)

	if [$("${is_config_exist}") != ""]; then
		echo "Deleting already existing autotune-config configmap !"
		${oc_cmd} delete cm autotune-config
	fi	
	echo "Creating environment variable in openshift cluster using configMap"
	${oc_cmd} create cm autotune-config --from-file=autotune-config-key=${OPENSHIFT_ENV}
	echo "Info: Deploying autotune yaml to OpenShift cluster"
	# Deploy into the "openshift-monitoring" namespace/project
	oc project ${autotune_ns}
	sed -ie "s/{{ AUTOTUNE_NAMESPACE }}/${autotune_ns}/" ${AUTOTUNE_DEPLOY_MANIFEST}
	${oc_cmd} apply -f ${AUTOTUNE_DEPLOY_MANIFEST}
	sleep 2
	check_running autotune
	# Indicate deploy failed on error
	if [ "${err}" != "0" ]; then
		exit 1
	fi
}

function openshift_start() {
	echo
	echo "###   Installing Autotune for OpenShift"
	echo
	echo "WARNING: This will create a Autotune ServiceMonitor object in the openshift-monitoring namespace"
	echo "WARNING: This is currently not recommended for production"
	echo

	if [ ${non_interactive} == 0 ]; then
		echo -n "Create ServiceMonitor object and continue installation?(y/n)? "
		read inst
		linst=$(echo ${inst} | tr A-Z a-z)
		if [ ${linst} == "n" ]; then
			echo "Info: Autotune not installed"
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
	echo "Terminating autotune..."

	echo
	echo "Removing autotune service account"
	${oc_cmd} delete -f ${AUTOTUNE_SA_MANIFEST} 2>/dev/null

	echo
	echo "Removing autotune crd"
	${oc_cmd} delete -f ${AUTOTUNE_CRD_MANIFEST} 2>/dev/null

	echo
	echo "Removing autotune role"
	${oc_cmd} delete -f ${AUTOTUNE_ROLE_MANIFEST} 2>/dev/null

	echo
	echo "Removing autotune role binding"
	${oc_cmd} delete -f ${AUTOTUNE_RB_MANIFEST} 2>/dev/null

	echo
	echo "Removing autotune deployment from Openshift cluster"
	${oc_cmd} delete -f ${AUTOTUNE_DEPLOY_MANIFEST} 2>/dev/null

	echo
	echo "Removing autotune serviceMonitor"
	${oc_cmd} delete -f ${SERVICE_MONITOR_MANIFEST} 2>/dev/null

	echo
	echo "Removing generated manifest files"
	echo "done"
}

###############################  ^ OpenShift ^ ################################
