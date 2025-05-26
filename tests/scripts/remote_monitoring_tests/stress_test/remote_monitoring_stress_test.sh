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
### Script to run stress test with Kruize in remote monitoring mode ##
#

CURRENT_DIR="$(dirname "$(realpath "$0")")"
KRUIZE_REPO="${CURRENT_DIR}/../../../../"
PERFORMANCE_PROFILE_DIR="${KRUIZE_REPO}manifests/autotune/performance-profiles"

# Source the common functions scripts
. ${CURRENT_DIR}/../../common/common_functions.sh

ITER=1
TIMEOUT=2000
RESULTS_DIR=/tmp/kruize_stress_test_results
BENCHMARK_SERVER=localhost
APP_NAME=kruize
CLUSTER_TYPE=minikube
DEPLOYMENT_NAME=kruize
CONTAINER_NAME=kruize
NAMESPACE=monitoring
users=100
rampup=200
num_res=30
loop=1
exp_type="container"

RESOURCE_OPTIMIZATION_JSON="${PERFORMANCE_PROFILE_DIR}/resource_optimization_openshift.json"

target="crc"
KRUIZE_IMAGE="quay.io/kruize/autotune:mvp_demo"

jmx_file="jmx/kruize_remote_monitoring_stress.jmx"

function usage() {
	echo
	echo "Usage: -c cluster_type [minikube|openshift] [-i Kruize image] [-u users] [-e No. of results] [-d ramp up time in seconds] [-r <resultsdir path> ] [-t TIMEOUT for metrics script]"
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

function jmeter_setup() {
	JMETER_VERSION="5.5"

	if [ ! -d ${CURRENT_DIR}/apache-jmeter-${JMETER_VERSION} ]; then
		echo "Downloading jmeter..." | tee -a ${LOG}
		wget https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-${JMETER_VERSION}.tgz
		tar -xzf apache-jmeter-${JMETER_VERSION}.tgz
		rm apache-jmeter-${JMETER_VERSION}.tgz
	else 
		echo "Skipping jmeter install as it is already present here - ${CURRENT_DIR}/apache-jmeter-${JMETER_VERSION}"
	fi
	export JMETER_HOME=${CURRENT_DIR}/apache-jmeter-${JMETER_VERSION}
	export PATH=${JMETER_HOME}/bin:${PATH}
}

while getopts c:r:i:u:d:t:e:b: gopts
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
		users="${OPTARG}"		
		;;
	e)
		num_res="${OPTARG}"
		;;
	d)
		rampup="${OPTARG}"		
		;;
	t)
		TIMEOUT="${OPTARG}"		
		;;
	b)
		exp_type="${OPTARG}"
		;;
	esac
done

if [ -z "${CLUSTER_TYPE}" ]; then
	usage
fi

if [[ "${exp_type}" != "container" && "${exp_type}" != "namespace" ]]; then
	echo "-b option values should be container or namespace, if not specified default is container"
	usage
fi

LOG_DIR="${RESULTS_DIR}/remote-monitoring-stress-$(date +%Y%m%d%H%M)"
mkdir -p ${LOG_DIR}

LOG="${LOG_DIR}/remote-monitoring-stress.log"
METRICS_LOG_DIR="${LOG_DIR}/resource_usage"
mkdir -p ${METRICS_LOG_DIR}

prometheus_pod_running=$(kubectl get pods --all-namespaces | grep "prometheus-k8s-0")
if [ "${prometheus_pod_running}" == "" ]; then
	echo "Install prometheus required to fetch the resource usage metrics for kruize"
	exit 1

fi

JMETER_LOG_DIR="${LOG_DIR}/jmeter_logs" 
mkdir -p ${JMETER_LOG_DIR}

echo "Invoking jmeter setup" | tee -a ${LOG}
jmeter_setup
echo "Invoking jmeter setup...done" | tee -a ${LOG}

echo "Setting up kruize..." | tee -a ${LOG}
echo "Removing isROSEnabled=false and local=true"
cluster_type=${CLUSTER_TYPE}
pushd ${KRUIZE_REPO} > /dev/null
	kruize_remote_patch
	echo "./deploy.sh -c ${CLUSTER_TYPE} -i ${KRUIZE_IMAGE} -m ${target} -t >> ${LOG_DIR}/kruize_setup.log"
	./deploy.sh -c ${CLUSTER_TYPE} -i ${KRUIZE_IMAGE} -m ${target} -t >> ${LOG_DIR}/kruize_setup.log 2>&1

	sleep 5
	echo "./deploy.sh -c ${CLUSTER_TYPE} -i ${KRUIZE_IMAGE} -m ${target} >> ${LOG_DIR}/kruize_setup.log"
	./deploy.sh -c ${CLUSTER_TYPE} -i ${KRUIZE_IMAGE} -m ${target} >> ${LOG_DIR}/kruize_setup.log 2>&1
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
			echo "SERVER_IP_ADDR = ${SERVER_IP_ADDR} BENCHMARK_SERVER = ${BENCHMARK_SERVER} port = ${port}"
		fi
		;;
	openshift)
		NAMESPACE="openshift-tuning"
		if [ -z "${SERVER_IP_ADDR}" ]; then
			oc expose svc/kruize -n ${NAMESPACE}

			SERVER_IP_ADDR=($(oc status --namespace=${NAMESPACE} | grep "kruize" | grep port | cut -d " " -f1 | cut -d "/" -f3))
			port=""
			BENCHMARK_SERVER=$(echo ${SERVER_IP_ADDR} | cut -d "." -f3-)
			echo "SERVER_IP_ADDR = ${SERVER_IP_ADDR} BENCHMARK_SERVER = ${BENCHMARK_SERVER}"
		fi
		;;
	*)
		err_exit "Error: Cluster type ${CLUSTER_TYPE} is not supported" | tee -a ${LOG}
		;;
esac	

# Start monitoring metrics
if [ "${CLUSTER_TYPE}" == "openshift" ]; then
	echo ""
	echo "./monitor-metrics-promql.sh ${ITER} ${TIMEOUT} ${METRICS_LOG_DIR} ${BENCHMARK_SERVER} ${APP_NAME} ${CLUSTER_TYPE} ${DEPLOYMENT_NAME} ${CONTAINER_NAME} ${NAMESPACE} &" | tee -a ${LOG}
	./monitor-metrics-promql.sh ${ITER} ${TIMEOUT} ${METRICS_LOG_DIR} ${BENCHMARK_SERVER} ${APP_NAME} ${CLUSTER_TYPE} ${DEPLOYMENT_NAME} ${CONTAINER_NAME} ${NAMESPACE} > ${LOG_DIR}/monitor-metrics.log 2>&1 &

	# Create the performance profile
	# If kruize service is exposed then do not specify the port	
	cmd="curl http://${SERVER_IP_ADDR}/createPerformanceProfile -d @${RESOURCE_OPTIMIZATION_JSON}"
	echo ""
	echo "cmd = ${cmd}"
	curl http://${SERVER_IP_ADDR}/createPerformanceProfile -d @"${RESOURCE_OPTIMIZATION_JSON}"
else
	echo ""
	echo "./monitor-metrics-promql.sh ${ITER} ${TIMEOUT} ${METRICS_LOG_DIR} ${BENCHMARK_SERVER} ${APP_NAME} ${CLUSTER_TYPE} ${DEPLOYMENT_NAME} ${CONTAINER_NAME} ${NAMESPACE} &" | tee -a ${LOG}
	./monitor-metrics-promql.sh ${ITER} ${TIMEOUT} ${METRICS_LOG_DIR} ${BENCHMARK_SERVER} ${APP_NAME} ${CLUSTER_TYPE} ${DEPLOYMENT_NAME} ${CONTAINER_NAME} ${NAMESPACE} &

	# Create the performance profile
	cmd="curl http://${SERVER_IP_ADDR}:${port}/createPerformanceProfile -d @${RESOURCE_OPTIMIZATION_JSON}"
	echo ""
	echo "cmd = ${cmd}"
	curl http://${SERVER_IP_ADDR}:${port}/createPerformanceProfile -d @"${RESOURCE_OPTIMIZATION_JSON}"
fi

echo | tee -a ${LOG}

kruize_stats="${JMETER_LOG_DIR}/jmeter_kruize.stats"
kruize_log="${JMETER_LOG_DIR}/jmeter_kruize.log"
	
host=${SERVER_IP_ADDR}

get_kruize_pod_log ${LOG_DIR}/kruize_pod.log

# sleep for sometime before starting the experiments to capture initial resource usage of kruize
sleep 200

JMETER_LOG="${LOG_DIR}/jmeter.log"
# Run the jmeter load
if [ "${CLUSTER_TYPE}" == "openshift" ]; then
	echo ""
	echo "Running jmeter load for kruize ${inst} with the following parameters" | tee -a ${LOG}
	jmx_file="jmx/kruize_remote_monitoring_stress_openshift.jmx"
	if [ "${exp_type}" == "namespace" ]; then
		jmx_file="jmx/kruize_ns_remote_monitoring_stress_openshift.jmx"
	fi

	echo "jmeter -n -t ${jmx_file} -j ${kruize_stats} -l ${kruize_log} -Jhost=$host -Jport=${port} -Jusers=${users} -Jnum_res=${num_res} -Jlogdir=${JMETER_LOG_DIR} -Jrampup=${rampup} -Jloop=${loop} > ${JMETER_LOG}" | tee -a ${LOG}
	exec jmeter -n -t ${jmx_file} -j ${kruize_stats} -l ${kruize_log} -Jport="" -Jhost=${host} -Jport=${port} -Jusers=${users} -Jnum_res=${num_res} -Jlogdir=${JMETER_LOG_DIR} -Jrampup=${rampup} -Jloop=${loop} > ${JMETER_LOG}

else
	echo ""
	if [ "${exp_type}" == "namespace" ]; then
		jmx_file="jmx/kruize_ns_remote_monitoring_stress.jmx"
	fi
	echo "Running jmeter load for kruize ${inst} with the following parameters" | tee -a ${LOG}
	echo "jmeter -n -t ${jmx_file} -j ${kruize_stats} -l ${kruize_log} -Jhost=${host} -Jport=${port} -Jusers=${users} -Jnum_res=${num_res} -Jlogdir=${JMETER_LOG_DIR} -Jrampup=${rampup} -Jloop=${loop} > ${JMETER_LOG}" | tee -a ${LOG}
	#exec jmeter -n -t ${jmx_file} -j ${kruize_stats} -l ${kruize_log} -Jhost=${host} -Jport=${port} -Jusers=${users} -Jnum_res=${num_res} -Jlogdir=${JMETER_LOG_DIR} -Jrampup=${rampup} -Jloop=${loop} > ${JMETER_LOG}
	cmd="jmeter -n -t ${jmx_file} -j ${kruize_stats} -l ${kruize_log} -Jhost=${host} -Jport=${port} -Jusers=${users} -Jnum_res=${num_res} -Jlogdir=${JMETER_LOG_DIR} -Jrampup=${rampup} -Jloop=${loop}"
	${cmd} > ${JMETER_LOG}

fi
		
