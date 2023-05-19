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
### Script to run scale test with Kruize in remote monitoring mode ##
#

CURRENT_DIR="$(dirname "$(realpath "$0")")"
KRUIZE_REPO="${CURRENT_DIR}/../../../../"


# Source the common functions scripts
. ${CURRENT_DIR}/../../common/common_functions.sh

METRICS_SCRIPT_DIR="${CURRENT_DIR}/../stress_test"
echo "METRICS_SCRIPT_DIR = ${METRICS_SCRIPT_DIR}"

ITER=1
TIMEOUT=1100000
RESULTS_DIR=/tmp/kruize_scale_test_results
BENCHMARK_SERVER=localhost
APP_NAME=kruize
CLUSTER_TYPE=minikube
DEPLOYMENT_NAME=kruize
CONTAINER_NAME=kruize
NAMESPACE=monitoring
num_exps=10000
num_days_of_res=15


target="crc"
KRUIZE_IMAGE="kruize/autotune_operator:test"
hours=6

function usage() {
	echo
	echo "Usage: -c cluster_type[minikube|openshift] [-i Kruize image] [-u No. of experiments (default - 10000)] [-d No. of days of results (default - 15)] [-r <resultsdir path>] [-t TIMEOUT for metrics script]"
	exit -1
}

function get_kruize_pod_log() {
	log=$1

	# Fetch the kruize pod log

	echo ""
	echo "Fetch the kruize pod logs and store in ${log}..."
	kruize_pod=$(kubectl get pod -n ${NAMESPACE} | grep kruize | cut -d " " -f1)
	kubectl logs -f ${kruize_pod} -n ${NAMESPACE} > ${log} 2>&1 &
}

function get_kruize_service_log() {
        log=$1

        # Fetch the kruize service log

        echo ""
        echo "Fetch the kruize service logs and store in ${log}..."
        kruize_pod="svc/kruize"
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
		num_days_of_res="${OPTARG}"		
		;;
	t)
		TIMEOUT="${OPTARG}"		
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
LOG_DIR="${RESULTS_DIR}/remote-monitoring-scale-test-$(date +%Y%m%d%H%M)"
mkdir -p ${LOG_DIR}

LOG="${LOG_DIR}/remote-monitoring-scale-test.log"
METRICS_LOG_DIR="${LOG_DIR}/resource_usage"
mkdir -p ${METRICS_LOG_DIR}

prometheus_pod_running=$(kubectl get pods --all-namespaces | grep "prometheus-k8s-0")
if [ "${prometheus_pod_running}" == "" ]; then
	echo "Install prometheus required to fetch the resource usage metrics for kruize"
	exit 1

fi

KRUIZE_SETUP_LOG="${LOG_DIR}/kruize_setup.log"
KRUIZE_POD_LOG="${LOG_DIR}/kruize_pod.log"
KRUIZE_SERVICE_LOG="${LOG_DIR}/kruize_service.log"

# Setup kruize
echo "Setting up kruize..." | tee -a ${LOG}
pushd ${KRUIZE_REPO} > /dev/null
        echo "./deploy.sh -c ${CLUSTER_TYPE} -i ${KRUIZE_IMAGE} -m ${target} -t >> ${KRUIZE_SETUP_LOG}" | tee -a ${LOG}
        ./deploy.sh -c ${CLUSTER_TYPE} -i ${KRUIZE_IMAGE} -m ${target} -t >> ${KRUIZE_SETUP_LOG} 2>&1

        sleep 60
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
			BENCHMARK_SERVER="localhost"
			echo "SERVER_IP_ADDR = ${SERVER_IP_ADDR} BENCHMARK_SERVER = ${BENCHMARK_SERVER} port = ${port}" | tee -a ${LOG}
		fi
		;;
	openshift)
		NAMESPACE="openshift-tuning"
		if [ -z "${SERVER_IP_ADDR}" ]; then
			oc expose svc/kruize -n ${NAMESPACE}

			SERVER_IP_ADDR=($(oc status --namespace=${NAMESPACE} | grep "kruize" | grep port | cut -d " " -f1 | cut -d "/" -f3))
			port=9999
			BENCHMARK_SERVER=$(echo ${SERVER_IP_ADDR} | cut -d "." -f3-)
			echo "SERVER_IP_ADDR = ${SERVER_IP_ADDR} BENCHMARK_SERVER = ${BENCHMARK_SERVER}" | tee -a ${LOG}
		fi
		;;
	*)
		err_exit "Error: Cluster type ${CLUSTER_TYPE} is not supported" | tee -a ${LOG}
		;;
esac	

# Start monitoring metrics
if [ "${CLUSTER_TYPE}" == "openshift" ]; then
	echo "" | tee -a ${LOG}
	echo "${METRICS_SCRIPT_DIR}/monitor-metrics-promql.sh ${ITER} ${TIMEOUT} ${METRICS_LOG_DIR} ${BENCHMARK_SERVER} ${APP_NAME} ${CLUSTER_TYPE} ${DEPLOYMENT_NAME} ${CONTAINER_NAME} ${NAMESPACE} &" | tee -a ${LOG}
	${METRICS_SCRIPT_DIR}/monitor-metrics-promql.sh ${ITER} ${TIMEOUT} ${METRICS_LOG_DIR} ${BENCHMARK_SERVER} ${APP_NAME} ${CLUSTER_TYPE} ${DEPLOYMENT_NAME} ${CONTAINER_NAME} ${NAMESPACE} > ${LOG_DIR}/monitor-metrics.log 2>&1 &

else
	echo "" | tee -a ${LOG}
	echo "${METRICS_SCRIPT_DIR}/monitor-metrics-promql.sh ${ITER} ${TIMEOUT} ${METRICS_LOG_DIR} ${BENCHMARK_SERVER} ${APP_NAME} ${CLUSTER_TYPE} ${DEPLOYMENT_NAME} ${CONTAINER_NAME} ${NAMESPACE} &" | tee -a ${LOG}
	${METRICS_SCRIPT_DIR}/monitor-metrics-promql.sh ${ITER} ${TIMEOUT} ${METRICS_LOG_DIR} ${BENCHMARK_SERVER} ${APP_NAME} ${CLUSTER_TYPE} ${DEPLOYMENT_NAME} ${CONTAINER_NAME} ${NAMESPACE} &
fi

echo | tee -a ${LOG}

get_kruize_pod_log ${KRUIZE_POD_LOG}
get_kruize_service_log ${KRUIZE_SERVICE_LOG}

# sleep for sometime before starting the experiments to capture initial resource usage of kruize
sleep 200

# Run the scale demo
SCALE_LOG="${LOG_DIR}/scale_demo.log"
echo ""
echo "Running scale test for kruize on ${CLUSTER_TYPE}" | tee -a ${LOG}
echo "nohup ./run_scalability_test.sh -c "${CLUSTER_TYPE}" -a "${SERVER_IP_ADDR}" -p "${port}" -u "${num_exps}" -d "${num_days_of_res}" -r "${LOG_DIR}" | tee -a ${SCALE_LOG}" | tee -a ${LOG}
nohup ./run_scalability_test.sh -c "${CLUSTER_TYPE}" -a "${SERVER_IP_ADDR}" -p "${port}" -u "${num_exps}" -d "${num_days_of_res}" -r "${LOG_DIR}" | tee -a ${SCALE_LOG}

end_time=$(get_date)
elapsed_time=$(time_diff "${start_time}" "${end_time}")
echo "Test took ${elapsed_time} seconds to complete" | tee -a ${LOG}
