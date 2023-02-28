#!/bin/bash
#
# Copyright (c) 2023, 2023 Red Hat, IBM Corporation and others.
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

current_dir="$(dirname "$0")"
source ${current_dir}/common_utils.sh

cluster_type="minikube"
log_dir="${current_dir}"

function usage() {
	echo
	echo "Usage: $0 [-c [minikube|openshift]] [-d log directory path] [-m [autotune|crc]] [-n namespace]"
	echo " -c: kubernetes cluster type [Default - minikube]"
	echo " -d: Log directory path [Default - current directory]"
	echo " -m: Target mode [Default - autotune]"
	echo " -n: Namespace where kruize is deployed [Default - monitoring]"
	exit -1
}

function ffdc() {
	echo "Obtaining first failure data capture logs..."
	echo "Cluster_type = $cluster_type"
	echo "Namespace = $namespace"
	echo "Service = $service"
	echo "Target = $target"

	pod_log="${log_dir}/kruize_pod_log.txt"
	describe_log="${log_dir}/kruize_describe_pod_log.txt"

	kruize_pod=$(kubectl get pod -n $namespace | grep $service | cut -d " " -f1)

	kubectl describe pod ${kruize_pod} -n ${namespace} > ${describe_log} 2>&1
	check_err "Error getting kubectl describe kruize pod log! Check ${describe_log} for details!"
	
	if [ $target == "crc" ]; then
		kubectl logs ${kruize_pod} -n ${namespace} > ${pod_log} 2>&1
		check_err "Error getting kruize pod log! Check ${pod_log} for details!"
	else
		kubectl logs ${kruize_pod} -n ${namespace} -c ${service} > ${pod_log} 2>&1
		check_err "Error getting autotune pod log! Check ${pod_log} for details!"
	fi

	echo "Obtaining first failure data capture logs...done!"
}


# Iterate through the commandline options
while getopts d:c:m:n: gopts
do
	case ${gopts} in
		d)
			log_dir="${OPTARG}"
			;;
		c)
			cluster_type="${OPTARG}"
			;;
		m)
			target="${OPTARG}"
			;;
		n)
			namespace="${OPTARG}"
			;;
		[?])
			usage
			;;
	esac
done

if [ ${cluster_type} == "minikube" ] || [ ${cluster_type} == "openshift" ]; then
	if [ -z "${namespace}" ]; then
		if [ ${cluster_type} == "minikube" ]; then
			namespace="monitoring"
		elif [ ${cluster_type} == "openshift" ]; then
			namespace="openshift-tuning"
		fi
	fi

	if [ -z "${target}" ]; then
		target="autotune"
	fi

	if [ ${target} == "crc" ]; then
		service="kruize"
	elif [ ${target} == "autotune" ]; then
		service="autotune"
	else
		echo "Unsupported target mode!"
		exit -1
	fi

	# Capture the first failure data logs
	ffdc
else
	echo "Cluster type is not supported!"
	exit -1
fi
