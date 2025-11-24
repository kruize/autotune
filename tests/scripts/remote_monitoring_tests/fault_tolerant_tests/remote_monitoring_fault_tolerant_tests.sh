#!/bin/bash
#
# Copyright (c) 2023, 2023 IBM Corporation, RedHat and others.
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
### Script to run fault tolerant tests with Kruize in remote monitoring mode ##
#

CURRENT_DIR="$(dirname "$(realpath "$0")")"
KRUIZE_REPO="${CURRENT_DIR}/../../../../"
PERFORMANCE_PROFILE_DIR="${REMOTE_MONITORING_TEST_DIR}/../../../manifests/autotune/performance-profiles"

# Source the common functions scripts
. ${CURRENT_DIR}/../../common/common_functions.sh


RESULTS_DIR=/tmp/kruize_fault_tolerant_test_results
APP_NAME=kruize
CLUSTER_TYPE=minikube
NAMESPACE=monitoring
num_exps=3

RESOURCE_OPTIMIZATION_JSON="${PERFORMANCE_PROFILE_DIR}/resource_optimization_openshift.json"

target="crc"
KRUIZE_IMAGE="kruize/autotune_operator:test"
iterations=2
num_exps=1

function usage() {
	echo
	echo "Usage: -c cluster_tyep[minikube|openshift] [-i Kruize image] [-u No. of experiments (default - 1)] [ -d no. of iterations to test restart (default - 2)] [-r <resultsdir path>]"
	exit -1
}

function get_kruize_pod_log() {
	log=$1

	# Fetch the kruize pod log

	echo ""
	echo "Fetch the kruize pod logs and store in ${log}..."
	kruize_pod=$(kubectl get pod -n ${NAMESPACE} | grep kruize | grep -v kruize-ui | grep -v kruize-db | cut -d " " -f1)
	kubectl logs -f ${kruize_pod} -n ${NAMESPACE} > ${log} 2>&1 &
}

while getopts c:r:i:u:d:t:h gopts
do
	case ${gopts} in
	c)
		CLUSTER_TYPE=${OPTARG}
		;;
	r)
		RESULTS_DIR="${OPTARG}"		
		;;
	i)
		KRUIZE_IMAGE="${OPTARG}"		
		;;
	u)
		num_exps="${OPTARG}"		
		;;
	d)
		iterations="${OPTARG}"
		;;
	h)
		usage
		;;
	esac
done

if [ -z "${CLUSTER_TYPE}" ]; then
	usage
fi

start_time=$(get_date)
LOG_DIR="${RESULTS_DIR}/remote-monitoring-fault-tolerant-test-$(date +%Y%m%d%H%M)"
mkdir -p ${LOG_DIR}

LOG="${LOG_DIR}/remote-monitoring-fault-tolerant-test.log"

prometheus_pod_running=$(kubectl get pods --all-namespaces | grep "prometheus-k8s-0")
if [ "${prometheus_pod_running}" == "" ]; then
	echo "Install prometheus required to fetch the resource usage metrics for kruize"
	exit 1

fi

KRUIZE_SETUP_LOG="${LOG_DIR}/kruize_setup.log"

# Setup kruize
echo "Setting up kruize..." | tee -a ${LOG}
cluster_type=${CLUSTER_TYPE}
pushd ${KRUIZE_REPO} > /dev/null
	kruize_remote_patch
        echo "./deploy.sh -c ${CLUSTER_TYPE} -i ${KRUIZE_IMAGE} -m ${target} -t >> ${KRUIZE_SETUP_LOG}" | tee -a ${LOG}
        ./deploy.sh -c ${CLUSTER_TYPE} -i ${KRUIZE_IMAGE} -m ${target} -t >> ${KRUIZE_SETUP_LOG} 2>&1

        sleep 20
        echo "./deploy.sh -c ${CLUSTER_TYPE} -i ${KRUIZE_IMAGE} -m ${target} >> ${KRUIZE_SETUP_LOG}" | tee -a ${LOG}
        ./deploy.sh -c ${CLUSTER_TYPE} -i ${KRUIZE_IMAGE} -m ${target} >> ${KRUIZE_SETUP_LOG} 2>&1
        sleep 20
popd > /dev/null
echo "Setting up kruize...Done" | tee -a ${LOG}

case ${CLUSTER_TYPE} in
	minikube)
		if [ -z "${SERVER_IP_ADDR}" ]; then
			SERVER_IP_ADDR=$(minikube ip)
			echo "Port forward prometheus..." | tee -a ${LOG}
			kubectl port-forward svc/prometheus-k8s 9090:9090 -n ${NAMESPACE} > /dev/null 2>/dev/null &
			echo "Port forward prometheus...done" | tee -a ${LOG}
			port=$(kubectl -n ${NAMESPACE} get svc ${APP_NAME} --no-headers -o=custom-columns=PORT:.spec.ports[*].nodePort)
			if [ "${port}" == "" ]; then
				echo "Failed to get the Kruize port, Check if kruize is runnning!" | tee -a ${LOG}
				exit -1
			fi
			echo "SERVER_IP_ADDR = ${SERVER_IP_ADDR} port = ${port}" | tee -a ${LOG}
		fi
		;;
	openshift)
		NAMESPACE="openshift-tuning"
		if [ -z "${SERVER_IP_ADDR}" ]; then
			oc expose svc/kruize -n ${NAMESPACE}

			SERVER_IP_ADDR=($(oc status --namespace=${NAMESPACE} | grep "kruize" | grep port | cut -d " " -f1 | cut -d "/" -f3))
			port=""
			echo "SERVER_IP_ADDR = ${SERVER_IP_ADDR}" | tee -a ${LOG}
		fi
		;;
	*)
		err_exit "Error: Cluster type ${CLUSTER_TYPE} is not supported" | tee -a ${LOG}
		;;
esac	

echo | tee -a ${LOG}

KRUIZE_POD_LOG_BEFORE="${LOG_DIR}/kruize_pod_before.log"
get_kruize_pod_log ${KRUIZE_POD_LOG_BEFORE}

# Run the test
TEST_LOG="${LOG_DIR}/kruize_pod_restart_test.log"
echo ""
echo "Running fault tolerant test for kruize on ${CLUSTER_TYPE}" | tee -a ${LOG}
if [ "${CLUSTER_TYPE}" == "openshift" ]; then
	echo "python3 kruize_pod_restart_test.py -c ${CLUSTER_TYPE} -a ${SERVER_IP_ADDR} -u ${num_exps} -d ${iterations} -r ${LOG_DIR} | tee -a  ${TEST_LOG}" | tee -a ${LOG}
	python3 kruize_pod_restart_test.py -c ${CLUSTER_TYPE} -a ${SERVER_IP_ADDR} -u ${num_exps} -d ${iterations} -r "${LOG_DIR}" | tee -a  ${TEST_LOG}
	exit_code=$?
	echo "exit_code = $exit_code"

else
	echo "python3 kruize_pod_restart_test.py -c ${CLUSTER_TYPE} -u ${num_exps} -d ${iterations} -r ${LOG_DIR} | tee -a  ${TEST_LOG}" | tee -a ${LOG}
	python3 kruize_pod_restart_test.py -c ${CLUSTER_TYPE} -u ${num_exps} -d ${iterations} -r "${LOG_DIR}" | tee -a  ${TEST_LOG}
	exit_code=$?
	echo "exit_code = $exit_code"
fi

KRUIZE_POD_LOG_AFTER="${LOG_DIR}/kruize_pod_after.log"
get_kruize_pod_log ${KRUIZE_POD_LOG_AFTER}

end_time=$(get_date)
elapsed_time=$(time_diff "${start_time}" "${end_time}")
echo "Test took ${elapsed_time} seconds to complete" | tee -a ${LOG}

if [ "${exit_code}" -ne 0 ]; then
	echo "Fault tolerant test failed! Check the log for details" | tee -a ${LOG}
	exit 1
else
	if [[ $(grep -i "error\|exception" ${KRUIZE_POD_LOG_BEFORE}) || $(grep -i "error\|exception" ${KRUIZE_POD_LOG_AFTER}) ]]; then
		echo "Fault tolerant test failed! Check the logs for details" | tee -a ${LOG}
		exit 1
	else
		echo "Fault tolerant test passed!" | tee -a ${LOG}
		exit 0
	fi
fi
