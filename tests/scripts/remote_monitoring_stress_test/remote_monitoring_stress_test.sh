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
TIMEOUT=1200
RESULTS_DIR=/tmp/kruize_scale_test_results
BENCHMARK_SERVER=localhost
APP_NAME=kruize
CLUSTER_TYPE=minikube
DEPLOYMENT_NAME=kruize
CONTAINER_NAME=kruize
NAMESPACE=monitoring

target="crc"
KRUIZE_IMAGE="kruize/autotune_operator:test"

jmx_file="jmx/kruize_remote_monitoring_stress.jmx"

function usage() {
	echo
	echo "Usage: -c CLUSTER_TYPE[minikube|openshift] [-i Kruize image] [-r <Specify resultsdir path> ]"
	exit -1
}

function get_kruize_pod_log() {
	log=$1

	# Fetch the kruize pod log

	echo ""
	echo "Fetch the kruize pod logs and store in $log..."
	kruize_pod=$(kubectl get pod -n ${NAMESPACE} | grep kruize | cut -d " " -f1)
	kubectl logs -f ${kruize_pod} -n ${NAMESPACE} > ${log} 2>&1 &
}

function jmeter_setup() {
	JMETER_VERSION="5.5"

	if [ ! -d ${CURRENT_DIR}/apache-jmeter-$JMETER_VERSION ]; then
		echo "Downloading jmeter..." | tee -a ${LOG}
		wget https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-$JMETER_VERSION.tgz
		tar -xzf apache-jmeter-$JMETER_VERSION.tgz
		rm apache-jmeter-$JMETER_VERSION.tgz
	else 
		echo "Skipping jmeter install as it is already present here - ${CURRENT_DIR}/apache-jmeter-$JMETER_VERSION"
	fi
	export JMETER_HOME=${CURRENT_DIR}/apache-jmeter-$JMETER_VERSION
	export PATH=$JMETER_HOME/bin:$PATH 
}

while getopts c:r:i: gopts
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
	esac
done

if [ -z "${CLUSTER_TYPE}" ]; then
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
pushd ${KRUIZE_REPO} > /dev/null
	./deploy.sh -c ${CLUSTER_TYPE} -i ${KRUIZE_IMAGE} -m ${target} -t >> ${LOG_DIR}/kruize_setup.log 2>&1

	sleep 5
	./deploy.sh -c ${CLUSTER_TYPE} -i ${KRUIZE_IMAGE} -m ${target} >> ${LOG_DIR}/kruize_setup.log 2>&1
	sleep 100
popd > /dev/null
echo "Setting up kruize...Done" | tee -a ${LOG}


case ${CLUSTER_TYPE} in
	minikube)
		if [ -z "${SERVER_IP_ADDR}" ]; then
			SERVER_IP_ADDR=$(minikube ip)
			echo "Port forward prometheus..." | tee -a ${LOG}
			kubectl port-forward svc/prometheus-k8s 9090:9090 -n ${NAMESPACE} > /dev/null &
			echo "Port forward prometheus...done" | tee -a ${LOG}
			port=$(kubectl -n ${NAMESPACE} get svc $APP_NAME --no-headers -o=custom-columns=PORT:.spec.ports[*].nodePort)
			if [ "${port}" == "" ]; then
				echo "Failed to get the Kruize port, Check if kruize is runnning!" | tee -a ${LOG}
				exit -1
			fi
			BENCHMARK_SERVER="localhost"
		fi
		;;
	openshift)
		NAMESPACE="openshift-tuning"
		if [ -z "${SERVER_IP_ADDR}" ]; then
			oc expose svc/kruize -n ${NAMESPACE}

			SERVER_IP_ADDR=($(oc status --namespace=${NAMESPACE} | grep "kruize" | grep port | cut -d " " -f1 | cut -d "/" -f3))
			port=""
			BENCHMARK_SERVER=$(echo $SERVER_IP_ADDR | cut -d "." -f3-)

			#SERVER_IP_ADDR=$(oc get pods -l=app=${APP_NAME} -o wide -n ${NAMESPACE} -o=custom-columns=NODE:.spec.nodeName --no-headers)
			#port=$(oc -n ${NAMESPACE} get svc ${APP_NAME} --no-headers -o=custom-columns=PORT:.spec.ports[*].nodePort)
			#BENCHMARK_SERVER=$(echo $SERVER_IP_ADDR | cut -d "." -f2-)
		fi
		;;
	*)
		err_exit "Error: Cluster type $CLUSTER_TYPE is not supported" | tee -a ${LOG}
		;;
esac	
echo "SERVER_IP_ADDR = $SERVER_IP_ADDR BENCHMARK_SERVER = $BENCHMARK_SERVER port = $port"

# Start monitoring metrics
if [ "${CLUSTER_TYPE}" == "openshift" ]; then
	echo ""
	echo "./monitor-metrics-promql.sh ${ITER} ${TIMEOUT} ${METRICS_LOG_DIR} ${BENCHMARK_SERVER} ${APP_NAME} ${CLUSTER_TYPE} ${DEPLOYMENT_NAME} ${CONTAINER_NAME} ${NAMESPACE} &" 
	./monitor-metrics-promql.sh ${ITER} ${TIMEOUT} ${METRICS_LOG_DIR} ${BENCHMARK_SERVER} ${APP_NAME} ${CLUSTER_TYPE} ${DEPLOYMENT_NAME} ${CONTAINER_NAME} ${NAMESPACE} > ${LOG_DIR}/monitor-metrics.log 2>&1 &

	# Create the performance profile
	#cmd="curl http://$SERVER_IP_ADDR:$port/createPerformanceProfile -d @resource_optimization_openshift.json"
	#curl http://$SERVER_IP_ADDR:$port/createPerformanceProfile -d @resource_optimization_openshift.json
	
	cmd="curl http://$SERVER_IP_ADDR/createPerformanceProfile -d @resource_optimization_openshift.json"
	echo ""
	echo "cmd = $cmd"
	curl http://$SERVER_IP_ADDR/createPerformanceProfile -d @resource_optimization_openshift.json
else
	echo ""
	echo "./monitor-metrics-promql.sh ${ITER} ${TIMEOUT} ${METRICS_LOG_DIR} ${BENCHMARK_SERVER} ${APP_NAME} ${CLUSTER_TYPE} ${DEPLOYMENT_NAME} ${CONTAINER_NAME} ${NAMESPACE} &"
	./monitor-metrics-promql.sh ${ITER} ${TIMEOUT} ${METRICS_LOG_DIR} ${BENCHMARK_SERVER} ${APP_NAME} ${CLUSTER_TYPE} ${DEPLOYMENT_NAME} ${CONTAINER_NAME} ${NAMESPACE} &

	# Create the performance profile
	cmd="curl http://$SERVER_IP_ADDR:$port/createPerformanceProfile -d @resource_optimization_openshift.json"
	echo ""
	echo "cmd = $cmd"
	curl http://$SERVER_IP_ADDR:$port/createPerformanceProfile -d @resource_optimization_openshift.json
fi



echo | tee -a ${LOG}

kruize_stats="${JMETER_LOG_DIR}/jmeter_kruize.stats"
kruize_log="${JMETER_LOG_DIR}/jmeter_kruize.log"
	
users=10000
rampup=120
loop=1
host=${SERVER_IP_ADDR}

get_kruize_pod_log ${LOG_DIR}/kruize_pod.log

# sleep for sometime before starting the experiments to capture initial resource usage of kruize
sleep 200

# Run the jmeter load
if [ "${CLUSTER_TYPE}" == "openshift" ]; then
	echo ""
	echo "Running jmeter load for kruize ${inst} with the following parameters" | tee -a ${LOG}
	jmx_file="jmx/kruize_remote_monitoring_stress_openshift.jmx"
	echo "jmeter -n -t ${jmx_file} -j ${kruize_stats} -l ${kruize_log} -Jhost=$host -Jport=$port -Jusers=$users -Jlogdir=${JMETER_LOG_DIR} -Jrampup=$rampup -Jloop=$loop > ${LOG_DIR}/jmeter.log" | tee -a ${LOG}
	exec jmeter -n -t ${jmx_file} -j ${kruize_stats} -l ${kruize_log} -Jport="" -Jhost=$host -Jport=$port -Jusers=$users -Jlogdir=${JMETER_LOG_DIR} -Jrampup=$rampup -Jloop=$loop > ${LOG_DIR}/jmeter.log
else
	echo ""
	echo "Running jmeter load for kruize ${inst} with the following parameters" | tee -a ${LOG}
	echo "jmeter -n -t ${jmx_file} -j ${kruize_stats} -l ${kruize_log} -Jhost=$host -Jport=$port -Jusers=$users -Jlogdir=${JMETER_LOG_DIR} -Jrampup=$rampup -Jloop=$loop > ${LOG_DIR}/jmeter.log" | tee -a ${LOG}
	exec jmeter -n -t ${jmx_file} -j ${kruize_stats} -l ${kruize_log} -Jhost=$host -Jport=$port -Jusers=$users -Jlogdir=${JMETER_LOG_DIR} -Jrampup=$rampup -Jloop=$loop > ${LOG_DIR}/jmeter.log
fi
		
