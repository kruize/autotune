#!/bin/bash
#
# Copyright (c) 2024, 2024 IBM Corporation, RedHat and others.
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
### Script to run scale test with Kruize in local monitoring mode ##
#

CURRENT_DIR="$(dirname "$(realpath "$0")")"
KRUIZE_REPO="${CURRENT_DIR}/../../../../"


# Source the common functions scripts
. ${CURRENT_DIR}/../../common/common_functions.sh

RESULTS_DIR=kruize_bulk_scale_test_results
APP_NAME=kruize
CLUSTER_TYPE=openshift

NAMESPACE=openshift-tuning
workers=5
days_of_res=15
interval_hours=6
initial_end_date="2024-12-10T11:50:00.000Z"
org_ids=10
cluster_ids=10

skip_setup=0
prometheus_ds=0
replicas=3

ds_url="http://thanos-query-frontend.thanos-bench.svc.cluster.local:9090/"

target="crc"
KRUIZE_IMAGE="quay.io/kruize/autotune:mvp_demo"

function usage() {
	echo
	echo "Usage: [-i Kruize image] [-w No. of parallel workers (default - 10)] [-d No. of days of res (default - 15)] [-t interval hours (default - 2)] [-s Initial end date of tsdb block (default - 2024-11-11T00:00:00.000Z)]"
	echo "[-a kruize replicas (default - 3)][-r <resultsdir path>] [--skipsetup skip kruize setup] [ -z to test with prometheus datasource]"
	echo "[--url Datasource url (default - ${ds_url}] [-o No. of orgs (default - 10)] [-c No. of clusters / org (default - 10)]"
	exit 1
}

function get_kruize_pod_log() {
	log_dir=$1

	# Fetch the kruize pod log

	echo ""
	echo "Fetch the kruize pod logs..."

	pod_list=$(kubectl get pods -n ${NAMESPACE} -l app=kruize --output=jsonpath='{.items[*].metadata.name}')
	echo $pod_list
	mkdir -p "${log_dir}/pod_logs"
	for pod in $pod_list; do
		kubectl logs -n ${NAMESPACE} $pod > "${log_dir}/pod_logs/$pod.log" 2>&1 &
	done
}

function get_kruize_service_log() {
        log=$1

        # Fetch the kruize service log

        echo ""
        echo "Fetch the kruize service logs and store in ${log}..."
        kruize_pod="svc/kruize"
        kubectl logs -f ${kruize_pod} -n ${NAMESPACE} > ${log} 2>&1 &
}

function kruize_local_thanos_patch() {
        CRC_DIR="./manifests/crc/default-db-included-installation"
        KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT="${CRC_DIR}/openshift/kruize-crc-openshift.yaml"

	sed -i 's/"name": "prometheus-1"/"name": "thanos"/' ${KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT}
	sed -i 's/"serviceName": "prometheus-k8s"/"serviceName": ""/' ${KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT}
        sed -i 's/"namespace": "openshift-monitoring"/"namespace": ""/' ${KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT}
	sed -i 's#"url": ""#"url": "'"${ds_url}"'"#' ${KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT}

	sed -i 's/\([[:space:]]*\)\(storage:\)[[:space:]]*[0-9]\+Mi/\1\2 1Gi/' ${KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT}
	sed -i 's/\([[:space:]]*\)\(memory:\)[[:space:]]*".*"/\1\2 "2Gi"/; s/\([[:space:]]*\)\(cpu:\)[[:space:]]*".*"/\1\2 "2"/' ${KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT}
}


while getopts r:i:d:s:t:a:o:c:zh:-: gopts
do
	case ${gopts} in
	-)
		case "${OPTARG}" in
		  url=*)
				ds_url=${OPTARG#*=}
				;;
			skipsetup)
				skip_setup=1
				;;
			*)
	                        echo "Unknown option: --${OPTARG}"
                                exit 1
                    ;;
		esac
		;;
	r)
		RESULTS_DIR="${OPTARG}"		
		;;
	i)
		KRUIZE_IMAGE="${OPTARG}"		
		;;
  w)
		workers="${OPTARG}"
		;;
	d)
		days_of_res="${OPTARG}"
		;;
	s)
		initial_end_date="${OPTARG}"
		;;
	t)
		interval_hours="${OPTARG}"		
		;;
	a)
		replicas="${OPTARG}"
		;;
  o)
		org_ids="${OPTARG}"
		;;
  c)
    cluster_ids="${OPTARG}"
		;;
	z)
		prometheus_ds=1
		;;
	h)
		usage
		;;
	esac
done

start_time=$(get_date)
LOG_DIR="${RESULTS_DIR}/bulk-scale-test-$(date +%Y%m%d%H%M)"
mkdir -p ${LOG_DIR}

LOG="${LOG_DIR}/bulk-scale-test.log"

prometheus_pod_running=$(kubectl get pods --all-namespaces | grep "prometheus-k8s-0")
if [ "${prometheus_pod_running}" == "" ]; then
	echo "Install prometheus required to fetch the resource usage metrics for kruize"
	exit 1

fi

KRUIZE_SETUP_LOG="${LOG_DIR}/kruize_setup.log"
KRUIZE_SERVICE_LOG="${LOG_DIR}/kruize_service.log"

# Setup kruize
if [ ${skip_setup} -eq 0 ]; then
	echo "Setting up kruize..." | tee -a ${LOG}
	echo "$KRUIZE_REPO"
	pushd ${KRUIZE_REPO} > /dev/null
		# Update datasource
		if [ ${prometheus_ds} == 0 ]; then
			kruize_local_thanos_patch
		fi

        	echo "./deploy.sh -c ${CLUSTER_TYPE} -i ${KRUIZE_IMAGE} -m ${target} -t >> ${KRUIZE_SETUP_LOG}" | tee -a ${LOG}
		./deploy.sh -c ${CLUSTER_TYPE} -i ${KRUIZE_IMAGE} -m ${target} -t >> ${KRUIZE_SETUP_LOG} 2>&1

        	sleep 30
	        echo "./deploy.sh -c ${CLUSTER_TYPE} -i ${KRUIZE_IMAGE} -m ${target} >> ${KRUIZE_SETUP_LOG}" | tee -a ${LOG}
        	./deploy.sh -c ${CLUSTER_TYPE} -i ${KRUIZE_IMAGE} -m ${target} >> ${KRUIZE_SETUP_LOG} 2>&1 &
	        sleep 60

		# scale kruize pods
		echo "Scaling kruize replicas to ${replicas}..." | tee -a ${LOG}
		echo "kubectl scale deployments/kruize -n ${NAMESPACE} --replicas=${replicas}" | tee -a ${LOG}
		kubectl scale deployments/kruize -n ${NAMESPACE} --replicas=${replicas} | tee -a ${LOG}
		sleep 30

		echo "List the pods..." | tee -a ${LOG} | tee -a ${LOG}
		kubectl get pods -n ${NAMESPACE} | tee -a ${LOG}

    oc expose svc/kruize -n ${NAMESPACE}

	popd > /dev/null
	echo "Setting up kruize...Done" | tee -a ${LOG}
fi

if [ -z "${SERVER_IP_ADDR}" ]; then
  SERVER_IP_ADDR=($(oc status --namespace=${NAMESPACE} | grep "kruize" | grep port | cut -d " " -f1 | cut -d "/" -f3))
  port=0
	echo "SERVER_IP_ADDR = ${SERVER_IP_ADDR} " | tee -a ${LOG}
fi

echo | tee -a ${LOG}

get_kruize_pod_log ${LOG_DIR}
get_kruize_service_log ${KRUIZE_SERVICE_LOG}

export PYTHONUNBUFFERED=1
# Run the scale test
echo ""
echo "Running scale test for kruize on ${CLUSTER_TYPE}" | tee -a ${LOG}
echo ""
python3 bulk_scale_test.py --workers ${workers} --org_ids ${org_ids} --cluster_ids ${cluster_ids} --days_of_res ${days_of_res} --enddate ${initial_end_date} --interval ${interval_hours} --resultsdir ${LOG_DIR} | tee -a ${LOG}

end_time=$(get_date)
elapsed_time=$(time_diff "${start_time}" "${end_time}")
echo ""
echo "Test took ${elapsed_time} seconds to complete" | tee -a ${LOG}

if [[ $(grep -i "error\|exception" ${KRUIZE_SERVICE_LOG}) ]]; then
		echo "Bulk scale test failed! Check the logs for details" | tee -a ${LOG}
		exit 1
else
		echo "Bulk scale test completed! Check the logs for details" | tee -a ${LOG}
		exit 0
fi

