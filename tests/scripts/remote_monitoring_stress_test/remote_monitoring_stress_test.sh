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
KRUIZE_REPO="${CURRENT_DIR}/../../../"


# Source the common functions scripts
. ${CURRENT_DIR}/../common/common_functions.sh

ITER=1
TIMEOUT=10
RESULTS_DIR=/tmp/kruize_scale_test_results
BENCHMARK_SERVER=localhost
APP_NAME=autotune
CLUSTER_TYPE=minikube
DEPLOYMENT_NAME=autotune
CONTAINER_NAME=autotune
NAMESPACE=monitoring

target="crc"
KRUIZE_IMAGE="kruize/autotune_operator:0.0.8_mvp"

jmx_file="jmx/kruize_remote_monitoring_stress.jmx"

function usage() {
	echo
	echo "Usage: -c CLUSTER_TYPE[docker|minikube|openshift] [-i Kruize image] [--iter=MAX_LOOP] [-n NAMESPACE] [-a SERVER_IP_ADDR] [-r <Specify resultsdir path> ]"
	exit -1
}

function jmeter_setup() {
	JMETER_VERSION="5.5"

	echo "Downloading jmeter..." | tee -a ${LOG}
	#wget https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-$JMETER_VERSION.tgz
	#tar -xzf apache-jmeter-$JMETER_VERSION.tgz
	#rm apache-jmeter-$JMETER_VERSION.tgz
	export JMETER_HOME=${CURRENT_DIR}/apache-jmeter-$JMETER_VERSION
	export PATH=$JMETER_HOME/bin:$PATH 

}

while getopts c:a:rn:i:-: gopts
do
	case ${gopts} in
	-)
		case "${OPTARG}" in
			iter=*)
				MAX_LOOP=${OPTARG#*=}
				;;
		esac
		;;
	c)
		CLUSTER_TYPE=${OPTARG}
		;;
	a)
		SERVER_IP_ADDR="${OPTARG}"		
		;;
	n)
		NAMESPACE="${OPTARG}"		
		;;
	r)
		RESULTS_DIR="${OPTARG}"		
		;;
	i)
		KRUIZE_IMAGE="${OPTARG}"		
		;;
	esac
done

if [ -z "${CLUSTER_TYPE}" ]; then
	usage
fi

if [ -z "${MAX_LOOP}" ]; then
	MAX_LOOP=1
fi

if [ -z "${NAMESPACE}" ]; then
	NAMESPACE="${DEFAULT_NAMESPACE}"
fi

LOG_DIR="${RESULTS_DIR}/remote-monitoring-stress-$(date +%Y%m%d%H%M)"
mkdir -p ${LOG_DIR}

LOG="${LOG_DIR}/remote-monitoring-stress.log"
METRICS_LOG_DIR="${LOG_DIR}/resource_usage"
mkdir -p ${METRICS_LOG_DIR}

JMETER_LOG_DIR="${LOG_DIR}/jmeter_logs" 
mkdir -p ${JMETER_LOG_DIR}

echo "Setting up kruize..." | tee -a ${LOG}
pushd ${KRUIZE_REPO} > /dev/null
	./deploy.sh -c ${CLUSTER_TYPE} -i ${KRUIZE_IMAGE} -m ${target} >> ${LOG_DIR}/kruize_setup.log 2>&1

echo "Setting up kruize...Done" | tee -a ${LOG}
popd > /dev/null


case ${CLUSTER_TYPE} in
	docker)
		if [ -z "${SERVER_IP_ADDR}" ]; then
			get_ip
		fi
		err_exit "Error: Unable to load the jmeter image" | tee -a ${LOG}
	
		;;
	icp|minikube)
		if [ -z "${SERVER_IP_ADDR}" ]; then
			SERVER_IP_ADDR=$(minikube ip)
			echo "Port forward prometheus..." | tee -a ${LOG}
			#kubectl port-forward svc/prometheus-k8s 9090:9090 -n monitoring &
			echo "Port forward prometheus...done" | tee -a ${LOG}
		fi
		;;
	openshift)
		NAMESPACE="openshift-tuning"
		if [ -z "${SERVER_IP_ADDR}" ]; then
			oc expose svc/kruize -n ${NAMESPACE}
			SERVER_IP_ADDR=($(oc status --namespace=${NAMESPACE} | grep "kruize" | grep port | cut -d " " -f1 | cut -d "/" -f3))
			echo "************ SERVER_IP_ADDR = $SERVER_IP_ADDR"
		fi
		;;
	*)
		err_exit "Error: Cluster type $CLUSTER_TYPE is not supported" | tee -a ${LOG}
		;;
esac	


echo "Invoking jmeter setup" | tee -a ${LOG}
jmeter_setup

echo "Invoking jmeter setup...done" | tee -a ${LOG}

if [ "${CLUSTER_TYPE}" == "openshift" ]; then
	DEPLOYMENT_NAME="kruize"
	echo "./monitor-metrics-promql.sh ${ITER} ${TIMEOUT} ${METRICS_LOG_DIR} ${SERVER_IP_ADDR} ${APP_NAME} ${CLUSTER_TYPE} ${DEPLOYMENT_NAME} ${CONTAINER_NAME} ${NAMESPACE} &" 
	./monitor-metrics-promql.sh ${ITER} ${TIMEOUT} ${METRICS_LOG_DIR} ${SERVER_IP_ADDR} ${APP_NAME} ${CLUSTER_TYPE} ${DEPLOYMENT_NAME} ${CONTAINER_NAME} ${NAMESPACE} &
else
	echo "./monitor-metrics-promql.sh ${ITER} ${TIMEOUT} ${METRICS_LOG_DIR} localhost ${APP_NAME} ${CLUSTER_TYPE} ${DEPLOYMENT_NAME} ${CONTAINER_NAME} ${NAMESPACE} &"
	./monitor-metrics-promql.sh ${ITER} ${TIMEOUT} ${METRICS_LOG_DIR} localhost ${APP_NAME} ${CLUSTER_TYPE} ${DEPLOYMENT_NAME} ${CONTAINER_NAME} ${NAMESPACE} &
fi


for iter in `seq 1 ${MAX_LOOP}`
do
	echo | tee -a ${LOG}
	echo "#########################################################################################" | tee -a ${LOG}
	echo "                             Starting Iteration ${iter}                                  " | tee -a ${LOG}
	echo "#########################################################################################" | tee -a ${LOG}
	echo | tee -a ${LOG}

	kruize_stats="${JMETER_LOG_DIR}/jmeter_kruize.stats"
	kruize_log="${JMETER_LOG_DIR}/jmeter_kruize.log"
	
	users=10000
	rampup=120
	loop=1
	host=${SERVER_IP_ADDR}

	# Run the jmeter load
	if [ "${CLUSTER_TYPE}" == "openshift" ]; then
		echo "Running jmeter load for kruize ${inst} with the following parameters" | tee -a ${LOG}
		jmx_file="jmx/kruize_remote_monitoring_stress_openshift.jmx"
		echo "jmeter -n -t ${jmx_file} -j ${kruize_stats} -l ${kruize_log} -Jhost=$host -Jusers=$users -Jlogdir=${JMETER_LOG_DIR} -Jrampup=$rampup -Jloop=$loop > ${LOG_DIR}/jmeter-${iter}.log" | tee -a ${LOG}
		exec jmeter -n -t ${jmx_file} -j ${kruize_stats} -l ${kruize_log} -Jport="" -Jhost=$host -Jusers=$users -Jlogdir=${JMETER_LOG_DIR} -Jrampup=$rampup -Jloop=$loop > ${LOG_DIR}/jmeter-${iter}.log
	else
		port=$(kubectl -n monitoring get svc autotune --no-headers -o=custom-columns=PORT:.spec.ports[*].nodePort)
		echo "port = $port" | tee -a ${LOG}
		if [ "${port}" == "" ]; then
			echo "Failed to get the Kruize port, Check if kruize is runnning!" | tee -a ${LOG}
			exit -1
		fi
		echo "Running jmeter load for kruize ${inst} with the following parameters" | tee -a ${LOG}
		echo "jmeter -n -t ${jmx_file} -j ${kruize_stats} -l ${kruize_log} -Jhost=$host -Jport=$port -Jusers=$users -Jlogdir=${JMETER_LOG_DIR} -Jrampup=$rampup -Jloop=$loop > ${LOG_DIR}/jmeter-${iter}.log" | tee -a ${LOG}
		sleep 200
		exec jmeter -n -t ${jmx_file} -j ${kruize_stats} -l ${kruize_log} -Jhost=$host -Jport=$port -Jusers=$users -Jlogdir=${JMETER_LOG_DIR} -Jrampup=$rampup -Jloop=$loop > ${LOG_DIR}/jmeter-${iter}.log
	fi
		
done	
