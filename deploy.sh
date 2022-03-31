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

ROOT_DIR="${PWD}"
SCRIPTS_DIR="${ROOT_DIR}/scripts"

AUTOTUNE_OPERATOR_CRD="manifests/autotune-operator-crd.yaml"
AUTOTUNE_CONFIG_CRD="manifests/autotune-config-crd.yaml"
AUTOTUNE_QUERY_VARIABLE_CRD="manifests/autotune-query-variable-crd.yaml"
AUTOTUNE_DEPLOY_MANIFEST_TEMPLATE="manifests/autotune-operator-deployment.yaml_template"
AUTOTUNE_DEPLOY_OPENSHIFT_MANIFEST_TEMPLATE="manifests/autotune-operator-openshift-deployment.yaml_template"
AUTOTUNE_DEPLOY_MANIFEST="manifests/autotune-operator-deployment.yaml"
AUTOTUNE_RB_MANIFEST_TEMPLATE="manifests/autotune-operator-rolebinding.yaml_template"
AUTOTUNE_RB_MANIFEST="manifests/autotune-operator-rolebinding.yaml"
AUTOTUNE_ROLE_MANIFEST="manifests/autotune-operator-role.yaml"
AUTOTUNE_SA_MANIFEST="manifests/autotune-operator-sa.yaml"
SERVICE_MONITOR_MANIFEST="manifests/servicemonitor/autotune-service-monitor.yaml"
AUTOTUNE_OPENSHIFT_NAMESPACE="openshift-tuning"
AUTOTUNE_SA_NAME="autotune-sa"
AUTOTUNE_CONFIGMAPS="manifests/configmaps"
AUTOTUNE_CONFIGS="manifests/autotune-configs"
AUTOTUNE_QUERY_VARIABLES_MANIFEST_TEMPLATE="manifests/autotune-query-variables/query-variable.yaml_template"
AUTOTUNE_QUERY_VARIABLES_MANIFEST="manifests/autotune-query-variables/query-variable.yaml"

AUTOTUNE_PORT="8080"
AUTOTUNE_DOCKER_REPO="kruize/autotune_operator"
OPTUNA_DOCKER_REPO="kruize/autotune_optuna"
#Fetch autotune version from the pom.xml file.
AUTOTUNE_VERSION="$(grep -A 1 "autotune" "${ROOT_DIR}"/pom.xml | grep version | awk -F '>' '{ split($2, a, "<"); print a[1] }')"
AUTOTUNE_DOCKER_IMAGE=${AUTOTUNE_DOCKER_REPO}:${AUTOTUNE_VERSION}
OPTUNA_DOCKER_IMAGE=${OPTUNA_DOCKER_REPO}:${AUTOTUNE_VERSION}

# source all the helpers scripts
. ${SCRIPTS_DIR}/minikube-helpers.sh
. ${SCRIPTS_DIR}/openshift-helpers.sh
. ${SCRIPTS_DIR}/common_utils.sh

# Defaults for the script
# minikube is the default cluster type
cluster_type="minikube"
# Call setup by default (and not terminate)
setup=1
# Default mode is interactive
non_interactive=0
autotune_ns=""
# docker: loop timeout is turned off by default
timeout=-1

function ctrlc_handler() {
	# Check if cluster type is docker
	if [[ "$cluster_type" == "docker" ]]; then
		# Terminate the containers [autotune && grafana && prometheus && cadvisor]
		docker_terminate
	fi
	# Exiting gracefully
	exit 2
}

# Handle SIGHUP(1), SIGINT(2), SIGQUIT(3) for cleaning up containers in docker case
trap "ctrlc_handler" 1 2 3

function usage() {
	echo
	echo "Usage: $0 [-a] [-k url] [-c [docker|minikube|openshift]] [-i autotune docker image] [-o optuna docker image] [-n namespace] [-d configmaps-dir ] [--timeout=x, x in seconds, for docker only]"
	echo "       -s = start(default), -t = terminate"
	echo " -s: Deploy autotune [Default]"
	echo " -t: Terminate autotune deployment"
	echo " -c: kubernetes cluster type. At present we support only minikube [Default - minikube]"
	echo " -i: build with specific autotune operator docker image name [Default - kruize/autotune_operator:<version from pom.xml>]"
	echo " -o: build with specific optuna docker image name [Default - kruize/autotune_optuna:<version from pom.xml>]"
	echo " -n: Namespace to which autotune is deployed [Default - monitoring namespace for cluster type minikube]"
	echo " -d: Config maps directory [Default - manifests/configmaps]"
	exit -1
}

# Check if the cluster_type is one of icp or openshift
function check_cluster_type() {
	case "${cluster_type}" in
	docker|minikube|openshift)
		;;
	*)
		echo "Error: unsupported cluster type: ${cluster_type}"
		exit -1
	esac
}

# Iterate through the commandline options
while getopts ac:d:i:k:n:o:p:stu:-: gopts
do
	case ${gopts} in
	-)
		case "${OPTARG}" in
			timeout=*)
				timeout=${OPTARG#*=}
				if [ -z "${timeout}" ]; then
					usage
				fi
				;;
			*)
				if [ "${OPTERR}" == 1 ] && [ "${OPTSPEC:0:1}" != ":" ]; then
					echo "Unknown option --${OPTARG}" >&2
					usage
				fi
				;;
		esac
		;;
	a)
		non_interactive=1
		;;
	c)
		cluster_type="${OPTARG}"
		check_cluster_type
		;;
	d)
		AUTOTUNE_CONFIGMAPS="${OPTARG}"
		;;
	i)
		AUTOTUNE_DOCKER_IMAGE="${OPTARG}"
		;;
	k)
		kurl="${OPTARG}"
		;;
	n)
		autotune_ns="${OPTARG}"
		;;
	o)
		OPTUNA_DOCKER_IMAGE="${OPTARG}"
		;;
	s)
		setup=1
		;;
	t)
		setup=0
		;;
	[?])
		usage
	esac
done

# Call the proper setup function based on the cluster_type
if [ ${setup} == 1 ]; then
	${cluster_type}_start
else
	${cluster_type}_terminate
fi
