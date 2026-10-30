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
workers=20
days_of_res=15
interval_hours=6
initial_end_date="2024-12-10T11:50:00.000Z"
configs_file="configs.json"

skip_setup=0
prometheus_ds=0
replicas=3

ds_url="http://thanos-query-frontend-example-query.thanos-operator-system.svc.cluster.local:9090/"

target="crc"
KRUIZE_IMAGE="quay.io/kruizehub/autotune-test-image:mvp_demo"

function usage() {
	echo
	echo "Usage: [-i Kruize image] [-w No. of parallel workers (default - 20)] [-d No. of days of res (default - 15)] [-t interval hours (default - 6)] [-s Initial end date of tsdb block (default - 2024-12-10T11:50:00.000Z)]"
	echo "[-a kruize replicas (default - 3)] [-r <resultsdir path>] [--skipsetup skip kruize setup] [ -z to test with prometheus datasource]"
	echo "[--url Datasource url (default - ${ds_url})] [-k <configs json file> (default - configs.json)]"
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

# Portable in-place sed (works with both GNU and BSD/macOS sed)
function sed_inplace() {
	local expr=$1
	local file=$2
	sed -e "${expr}" "${file}" > "${file}.tmp" && mv "${file}.tmp" "${file}"
}

function kruize_local_config_patch() {

	CRC_DIR="./manifests/crc/default-db-included-installation"
	KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT="${CRC_DIR}/openshift/kruize-crc-openshift.yaml"

	if [[ "${prometheus_ds}" == 0 ]]; then
		sed_inplace 's#"experimentNameFormat" : "%datasource%|%clustername%|%namespace%|%workloadname%(%workloadtype%)|%containername%"#"experimentNameFormat" : "%label:org_id%|%label:cluster_id%|%datasource%|%clustername%|%namespace%|%workloadname%(%workloadtype%)|%containername%"#g' "${KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT}"
		sed_inplace 's/"name": "prometheus-1"/"name": "thanos"/' "${KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT}"
		sed_inplace 's/"serviceName": "prometheus-k8s"/"serviceName": ""/' "${KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT}"
		sed_inplace 's/"namespace": "openshift-monitoring"/"namespace": ""/' "${KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT}"
		sed_inplace 's#"url": ""#"url": "'"${ds_url}"'"#' "${KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT}"
	fi

	# Replace memory requests and limits for Kruize to 4Gi and 8Gi respectively for scale test
	sed_inplace '/- name: kruize$/,/ports:/{/requests:/,/limits:/{/memory:/s/"[^"]*"/"4Gi"/}; /limits:/,/ports:/{/memory:/s/"[^"]*"/"8Gi"/}}' "${KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT}"

	# Replace memory requests and limits for Kruize DB to 10Gi and 16 Gi respectively for scale test
	sed_inplace '/- name: kruize-db$/,/ports:/{/requests:/,/limits:/{/memory:/s/"[^"]*"/"10Gi"/}; /limits:/,/ports:/{/memory:/s/"[^"]*"/"16Gi"/}}' "${KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT}"

	sed_inplace 's/\([[:space:]]*\)\(cpu:\)[[:space:]]*".*"/\1\2 "2"/' "${KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT}"
    cp "${KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT}" "${LOG_DIR}"
	    
	#sed_inplace 's/"bulkapilimit"[[:space:]]*:[[:space:]]*[0-9][0-9]*/"bulkapilimit" : 10000/' "${KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT}"
	sed_inplace 's/\([[:space:]]*\)\(storage:\)[[:space:]]*[0-9][0-9]*Mi/\1\2 1Gi/' "${KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT}"
}


while getopts r:i:d:w:s:t:a:k:zh-: gopts
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
			configs=*)
				configs_file=${OPTARG#*=}
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
	k)
		configs_file="${OPTARG}"
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
		# Update kruize config
		kruize_local_config_patch

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
configs_cmd=""
if [ -n "${configs_file}" ]; then
	configs_cmd="--configs ${configs_file}"
fi

echo "python3 bulk_scale_test.py --workers ${workers} --days_of_res ${days_of_res} --enddate ${initial_end_date} --interval ${interval_hours} --resultsdir ${LOG_DIR} --prometheus ${prometheus_ds} ${configs_cmd}" | tee -a ${LOG}
python3 bulk_scale_test.py --workers "${workers}" --days_of_res "${days_of_res}" --enddate "${initial_end_date}" --interval "${interval_hours}" --resultsdir "${LOG_DIR}" --prometheus "${prometheus_ds}" ${configs_cmd} | tee -a ${LOG}
status=$?

end_time=$(get_date)
elapsed_time=$(time_diff "${start_time}" "${end_time}")
echo ""
echo "Test took ${elapsed_time} seconds to complete" | tee -a ${LOG}

echo ""
echo "###########################################################################"
echo "Summary of the test run"
exp_count=$(kubectl exec `kubectl get pods -o=name -n openshift-tuning | grep kruize-db` -n openshift-tuning -- psql -U admin -d kruizeDB -c "SELECT count(*) from public.kruize_lm_experiments ;" | tail -3 | head -1 | tr -d '[:space:]')

reco_count=$(kubectl exec `kubectl get pods -o=name -n openshift-tuning | grep kruize-db` -n openshift-tuning -- psql -U admin -d kruizeDB -c "SELECT count(*) from public.kruize_lm_recommendations ;" | tail -3 | head -1 | tr -d '[:space:]')

echo "exp_count / reco_count = ${exp_count} / ${reco_count}"

db_size=$(kubectl exec `kubectl get pods -o=name -n openshift-tuning | grep kruize-db` -n openshift-tuning -- psql -U admin -d kruizeDB -c "SELECT pg_database_size('kruizeDB') AS database_size_bytes;" | tail -3 | head -1 | tr -d '[:space:]')

db_size_mb=$((db_size / (1024 * 1024) + 1))

echo "Postgres DB size in MB = ${db_size_mb}"

# This should be a computed value
total_recos_count = 300000
#echo "python3 parse_metrics.py -d "${RESULTS_DIR}/results" -r "${total_recos_count}""
#python3 parse_metrics.py -d "${RESULTS_DIR}/results" -r "${total_recos_count}"

echo "###########################################################################"
echo ""
echo ""

if [[ $(grep -i "error\|exception" ${KRUIZE_SERVICE_LOG}) ]]; then
	echo "Bulk scale test failed! Check the logs for details" | tee -a ${LOG}
	exit 1
else
	if [[ ${status} == 1 ]]; then
		echo "Bulk scale test failed! Check the logs for details" | tee -a ${LOG}
		exit 1
	else
		echo "Bulk scale test completed! Check the logs for details" | tee -a ${LOG}
		exit 0
	fi
fi

