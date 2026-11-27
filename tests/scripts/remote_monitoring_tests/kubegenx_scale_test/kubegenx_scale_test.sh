#!/bin/bash
#
# Copyright (c) 2026 IBM Corporation and others.
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
### Script to run KubeGenX-based scale test with Kruize in remote monitoring mode ##
#
# Two-phase test:
#   Phase 1: Fetch data from KubeGenX APIs and save as Kruize-compatible JSONs
#   Phase 2: Post the saved data to Kruize with parallel clients
#
# Usage: ./kubegenx_scale_test.sh --kubegenx-url <URL> --dataset <DATASET> --session-id <SESSION_ID> [options]
#

CURRENT_DIR="$(dirname "$(realpath "$0")")"
KRUIZE_REPO_PATH="${CURRENT_DIR}/../../../.."

# Source the common functions scripts
. ${CURRENT_DIR}/../../common/common_functions.sh

# Defaults
RESULTS_DIR=kruize_kubegenx_scale_test_results
APP_NAME=kruize
CLUSTER_TYPE=openshift
NAMESPACE=openshift-tuning

num_clients=20
replicas=3
chunk_size=24

kruize_setup=true
target="crc"
KRUIZE_IMAGE="quay.io/kruizehub/autotune-test-image:mvp_demo"
api_version="legacy"
insecure_flag=""
limit_namespaces=0
limit_workloads=0
fetch_only=false
post_only=false
query_db_interval=5
custom_data_dir=""
exp_type="container"
limit_experiments=0

# KubeGenX parameters (required)
kubegenx_url=""
dataset=""
session_id=""

function usage() {
	echo
	echo "Usage: ./kubegenx_scale_test.sh --kubegenx-url <URL> --dataset <DATASET> --session-id <SESSION_ID> [options]"
	echo
	echo "Required:"
	echo "  --kubegenx-url <URL>       KubeGenX base URL"
	echo "  --dataset <DATASET>        KubeGenX dataset name"
	echo "  --session-id <SESSION_ID>  KubeGenX session ID"
	echo
	echo "Options:"
	echo "  -r <results_dir>           Results directory (default: ${RESULTS_DIR})"
	echo "  -i <kruize_image>          Kruize image (default: ${KRUIZE_IMAGE})"
	echo "  -n <num_clients>           Number of parallel clients (default: ${num_clients})"
	echo "  -g <replicas>              Number of Kruize replicas (default: ${replicas})"
	echo "  -b <true|false>            Kruize setup (default: ${kruize_setup})"
	echo "  -q <interval_mins>         DB query interval in minutes (default: ${query_db_interval})"
	echo "  --chunk-size <N>           Results per updateResults POST, 24=6hrs of 15min data (default: ${chunk_size})"
	echo "  --insecure                 Skip TLS verification for KubeGenX"
	echo "  --limit-namespaces <N>     Limit namespaces to fetch (default: 0 = all)"
	echo "  --limit-workloads <N>      Limit workloads per namespace (default: 0 = all)"
	echo "  --fetch-only               Only fetch data, do not post to Kruize"
	echo "  --post-only                Only post data (assumes data already fetched)"
	echo "  --data-dir <DIR>           Use existing data dir with --post-only (skip timestamp lookup)"
	echo "  --api-version <v1|legacy>  Kruize API version (default: legacy)"
	echo "  --limit-experiments <N>    Limit total experiments to post (default: 0 = all)"
	echo "  -c <type>                  Experiment type: container (default), namespace, container_ns"
	echo "  -h                         Show this help"
	echo
	exit 1
}

function query_db() {
	while true; do
		exp_count=$(kubectl exec $(kubectl get pods -o=name -n ${NAMESPACE} | grep kruize-db) -n ${NAMESPACE} -- psql -U admin -d kruizeDB -c "SELECT count(*) from public.kruize_experiments ;" | tail -3 | head -1 | tr -d '[:space:]')
		results_count=$(kubectl exec $(kubectl get pods -o=name -n ${NAMESPACE} | grep kruize-db) -n ${NAMESPACE} -- psql -U admin -d kruizeDB -c "SELECT count(*) from public.kruize_results ;" | tail -3 | head -1 | tr -d '[:space:]')
		echo "DB status: Experiments = ${exp_count}  Results = ${results_count}"

		sleep_time=$((query_db_interval * 60))
		sleep ${sleep_time}
	done
}

function get_kruize_pod_log() {
	log_dir=$1
	echo "Fetching kruize pod logs..."
	pod_list=$(kubectl get pods -n ${NAMESPACE} -l app=kruize --output=jsonpath='{.items[*].metadata.name}')
	mkdir -p "${log_dir}/pod_logs"
	for pod in $pod_list; do
		kubectl logs -n ${NAMESPACE} $pod > "${log_dir}/pod_logs/$pod.log" 2>&1 &
	done
}

function kruize_scale_test_remote_patch() {
	CRC_DIR="./manifests/crc/default-db-included-installation"
	KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT="${CRC_DIR}/openshift/kruize-crc-openshift.yaml"

	sed -i -E 's/"isROSEnabled": "false",?\s*//g; s/"local": "true",?\s*//g' "${KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT}"
	sed -i 's/\([[:space:]]*\)\(storage:\)[[:space:]]*[0-9]\+Mi/\1\2 1Gi/' "${KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT}"
	sed -i '/- name: kruize$/,/ports:/{/requests:/,/limits:/{/memory:/s/"[^"]*"/"4Gi"/; /cpu:/s/"[^"]*"/"1"/}; /limits:/,/ports:/{/memory:/s/"[^"]*"/"8Gi"/; /cpu:/s/"[^"]*"/"3"/}}' "${KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT}"
	sed -i '/- name: kruize-db$/,/ports:/{/requests:/,/limits:/{/memory:/s/"[^"]*"/"10Gi"/; /cpu:/s/"[^"]*"/"2"/}; /limits:/,/ports:/{/memory:/s/"[^"]*"/"16Gi"/; /cpu:/s/"[^"]*"/"2"/}}' "${KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT}"
	cp "${KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT}" "${LOG_DIR}"
}

function execution_time() {
	declare -a time_arr

	exec_time_log=$1
	scale_log_dir=$2
	time_arr=(1 4 28 40 60)

	cd $scale_log_dir

	for i in ${time_arr[@]}; do
		time_option="-m${i}"
		echo "" >> ${exec_time_log}
		if [ ${i} == 1 ]; then
			echo "6 hours" > ${exec_time_log}
		else
			j=$((${i}/4))
			if [ ${j} -gt ${num_days_of_res} ]; then
				break;
			fi
			if [ ${i} == 4 ]; then
				echo "${j} day" >> ${exec_time_log}
			else
				echo "${j} days" >> ${exec_time_log}
			fi
		fi
		echo ""
		grep ${time_option} -H 'Time elapsed:' *.log | awk -F '[:.]' '{ sum[$1] += ($4 * 3600) + ($5 * 60) + $6 } END { for (key in sum) { printf "%s: Total time elapsed: %02d:%02d:%02d\n", key, sum[key] / 3600, (sum[key] / 60) % 60, sum[key] % 60 } }' | sort >> ${exec_time_log}
	done
}

# Parse arguments
while [[ $# -gt 0 ]]; do
	case "$1" in
		--kubegenx-url)
			kubegenx_url="$2"; shift 2 ;;
		--dataset)
			dataset="$2"; shift 2 ;;
		--session-id)
			session_id="$2"; shift 2 ;;
		--api-version)
			api_version="$2"; shift 2 ;;
		--chunk-size)
			chunk_size="$2"; shift 2 ;;
		--insecure)
			insecure_flag="--insecure"; shift ;;
		--limit-namespaces)
			limit_namespaces="$2"; shift 2 ;;
		--limit-workloads)
			limit_workloads="$2"; shift 2 ;;
		--fetch-only)
			fetch_only=true; shift ;;
		--post-only)
			post_only=true; shift ;;
		--data-dir)
			custom_data_dir="$2"; shift 2 ;;
		--limit-experiments)
			limit_experiments="$2"; shift 2 ;;
		-r) RESULTS_DIR="$2"; shift 2 ;;
		-i) KRUIZE_IMAGE="$2"; shift 2 ;;
		-n) num_clients="$2"; shift 2 ;;
		-g) replicas="$2"; shift 2 ;;
		-b) kruize_setup="$2"; shift 2 ;;
		-c) exp_type="$2"; shift 2 ;;
		-q) query_db_interval="$2"; shift 2 ;;
		-h) usage ;;
		*) echo "Unknown option: $1"; usage ;;
	esac
done

if [ "${post_only}" != "true" ]; then
	if [[ -z "${kubegenx_url}" || -z "${dataset}" || -z "${session_id}" ]]; then
		echo "Error: --kubegenx-url, --dataset, and --session-id are required"
		usage
	fi
fi

start_time=$(get_date)
# Make RESULTS_DIR absolute so paths survive pushd/popd
if [[ "${RESULTS_DIR}" != /* ]]; then
	RESULTS_DIR="$(pwd)/${RESULTS_DIR}"
fi
LOG_DIR="${RESULTS_DIR}/kubegenx-scale-test-$(date +%Y%m%d%H%M)"
KUBEGENX_DATA_DIR="${LOG_DIR}/kubegenx_data"
SCALE_LOG_DIR="${LOG_DIR}/scale_logs"
mkdir -p "${LOG_DIR}" "${SCALE_LOG_DIR}"

LOG="${LOG_DIR}/kubegenx-scale-test.log"

echo "============================================================" | tee -a ${LOG}
echo "KubeGenX Scale Test" | tee -a ${LOG}
echo "  KubeGenX URL:   ${kubegenx_url}" | tee -a ${LOG}
echo "  Dataset:        ${dataset}" | tee -a ${LOG}
echo "  Session ID:     ${session_id}" | tee -a ${LOG}
echo "  API Version:    ${api_version}" | tee -a ${LOG}
echo "  Exp Type:       ${exp_type}" | tee -a ${LOG}
echo "  Clients:        ${num_clients}" | tee -a ${LOG}
echo "  Chunk Size:     ${chunk_size}" | tee -a ${LOG}
echo "  Limit Exps:     ${limit_experiments} (0=all)" | tee -a ${LOG}
echo "  Results Dir:    ${LOG_DIR}" | tee -a ${LOG}
echo "============================================================" | tee -a ${LOG}

# ========================
# Phase 1: Fetch data
# ========================
if [ "${post_only}" != "true" ]; then
	echo "" | tee -a ${LOG}
	echo "Phase 1: Fetching data from KubeGenX..." | tee -a ${LOG}

	fetch_cmd="python3 ${CURRENT_DIR}/fetch_kubegenx_data.py --url ${kubegenx_url} --dataset ${dataset} --session-id ${session_id} --output-dir ${KUBEGENX_DATA_DIR} --exp-type ${exp_type} ${insecure_flag}"

	if [ ${limit_namespaces} -gt 0 ]; then
		fetch_cmd="${fetch_cmd} --limit-namespaces ${limit_namespaces}"
	fi
	if [ ${limit_workloads} -gt 0 ]; then
		fetch_cmd="${fetch_cmd} --limit-workloads ${limit_workloads}"
	fi

	echo "${fetch_cmd}" | tee -a ${LOG}
	eval "${fetch_cmd}" 2>&1 | tee -a ${LOG}

	if [ $? -ne 0 ]; then
		echo "Error: Data fetch failed!" | tee -a ${LOG}
		exit 1
	fi

	echo "Phase 1 complete." | tee -a ${LOG}

	if [ "${fetch_only}" == "true" ]; then
		echo "Fetch-only mode. Data saved to ${KUBEGENX_DATA_DIR}" | tee -a ${LOG}
		exit 0
	fi
else
	if [ -n "${custom_data_dir}" ]; then
		if [[ "${custom_data_dir}" != /* ]]; then
			custom_data_dir="$(pwd)/${custom_data_dir}"
		fi
		KUBEGENX_DATA_DIR="${custom_data_dir}"
	fi
	if [ ! -d "${KUBEGENX_DATA_DIR}" ]; then
		echo "Error: Data directory ${KUBEGENX_DATA_DIR} not found. Run without --post-only first or provide --data-dir." | tee -a ${LOG}
		exit 1
	fi
	echo "Post-only mode. Using existing data from ${KUBEGENX_DATA_DIR}" | tee -a ${LOG}
fi

# Read metadata for validation
metadata_file="${KUBEGENX_DATA_DIR}/metadata.json"
total_experiments=$(python3 -c "import json; m=json.load(open('${metadata_file}')); print(m.get('total_experiments', m.get('total_workloads', 0)))")
total_results=$(python3 -c "import json; m=json.load(open('${metadata_file}')); print(m.get('total_results', m.get('total_intervals', 0)))")
max_results_per_exp=$(python3 -c "import json; m=json.load(open('${metadata_file}')); print(m.get('max_results_per_experiment', m.get('max_intervals_per_workload', 0)))")
num_days_of_res=$(( (max_results_per_exp * 15) / (24 * 60) ))
if [ ${num_days_of_res} -eq 0 ]; then
	num_days_of_res=1
fi

echo "" | tee -a ${LOG}
echo "Data summary:" | tee -a ${LOG}
echo "  Total experiments:            ${total_experiments}" | tee -a ${LOG}
echo "  Total results:                ${total_results}" | tee -a ${LOG}
echo "  Max results per experiment:   ${max_results_per_exp}" | tee -a ${LOG}
echo "  Estimated days of results:    ${num_days_of_res}" | tee -a ${LOG}

# ========================
# Kruize Setup
# ========================
if [ "${kruize_setup}" == "true" ]; then
	echo "" | tee -a ${LOG}
	echo "Setting up Kruize..." | tee -a ${LOG}

	prometheus_pod_running=$(kubectl get pods --all-namespaces | grep "prometheus-k8s-0")
	if [ "${prometheus_pod_running}" == "" ]; then
		echo "Error: Prometheus not found. Install prometheus for resource usage metrics." | tee -a ${LOG}
		exit 1
	fi

	KRUIZE_SETUP_LOG="${LOG_DIR}/kruize_setup.log"

	pushd ${KRUIZE_REPO_PATH} > /dev/null
		kruize_scale_test_remote_patch
		echo "./deploy.sh -c ${CLUSTER_TYPE} -i ${KRUIZE_IMAGE} -m ${target} -t >> ${KRUIZE_SETUP_LOG}" | tee -a ${LOG}
		./deploy.sh -c ${CLUSTER_TYPE} -i ${KRUIZE_IMAGE} -m ${target} -t >> ${KRUIZE_SETUP_LOG} 2>&1
		sleep 30
		echo "./deploy.sh -c ${CLUSTER_TYPE} -i ${KRUIZE_IMAGE} -m ${target} >> ${KRUIZE_SETUP_LOG}" | tee -a ${LOG}
		./deploy.sh -c ${CLUSTER_TYPE} -i ${KRUIZE_IMAGE} -m ${target} >> ${KRUIZE_SETUP_LOG} 2>&1 &
		sleep 120

		echo "Scaling kruize to ${replicas} replicas..." | tee -a ${LOG}
		kubectl scale deployments/kruize -n ${NAMESPACE} --replicas=${replicas} | tee -a ${LOG}
		sleep 60

		kubectl get pods -n ${NAMESPACE} | tee -a ${LOG}
	popd > /dev/null

	echo "Kruize setup done." | tee -a ${LOG}
fi

# Get Kruize server address
if [ -z "${SERVER_IP_ADDR}" ]; then
	oc expose svc/kruize -n ${NAMESPACE} 2>/dev/null
	SERVER_IP_ADDR=$(oc status --namespace=${NAMESPACE} | grep "kruize" | grep port | cut -d " " -f1 | cut -d "/" -f3)
	port=0
	echo "SERVER_IP_ADDR = ${SERVER_IP_ADDR}" | tee -a ${LOG}
fi

get_kruize_pod_log ${LOG_DIR}

# ========================
# Phase 2: Post data to Kruize
# ========================
echo "" | tee -a ${LOG}
echo "Phase 2: Posting data to Kruize with ${num_clients} clients..." | tee -a ${LOG}

declare -a pid_array=()
prometheus_server=$(echo ${SERVER_IP_ADDR} | cut -d "." -f 3-)

query_db &
QUERY_DB_PID=$!

for ((client=0; client<num_clients; client++)); do
	client_num=$((client + 1))
	name="kubegenx-client-${client_num}"
	logfile="${SCALE_LOG_DIR}/${name}.log"
	echo "Starting ${name}..." | tee -a ${LOG}

	nohup ${CURRENT_DIR}/kubegenx_scalability_wrapper.sh \
		--ip "${SERVER_IP_ADDR}" \
		--port "${port}" \
		--data-dir "${KUBEGENX_DATA_DIR}" \
		--client-id ${client} \
		--total-clients ${num_clients} \
		--chunk-size ${chunk_size} \
		--limit-experiments ${limit_experiments} \
		--api-version "${api_version}" \
		--prometheusserver "${prometheus_server}" \
		--outputdir "${LOG_DIR}" \
		>> "${logfile}" 2>&1 &

	pid_array+=($!)
	if [ ${num_clients} -gt 1 ]; then
		sleep 10
	fi
done

echo "All ${num_clients} clients launched. Waiting for completion..." | tee -a ${LOG}

for pid in "${pid_array[@]}"; do
	wait "$pid"
	echo "Process with PID $pid has completed." | tee -a ${LOG}
done

echo "" | tee -a ${LOG}
echo "###########################################################################" | tee -a ${LOG}
echo "              All threads completed!                                       " | tee -a ${LOG}
echo "###########################################################################" | tee -a ${LOG}

# ========================
# Execution time tracking
# ========================
exec_time_log="${LOG_DIR}/exec_time.log"
echo "Capturing execution time in ${exec_time_log}..." | tee -a ${LOG}
pushd ${SCALE_LOG_DIR} > /dev/null
	execution_time ${exec_time_log} ${SCALE_LOG_DIR}
popd > /dev/null
sleep 5
echo "Capturing execution time in ${exec_time_log}...done" | tee -a ${LOG}

# ========================
# DB Validation
# ========================
exp_count=$(kubectl exec $(kubectl get pods -o=name -n ${NAMESPACE} | grep kruize-db) -n ${NAMESPACE} -- psql -U admin -d kruizeDB -c "SELECT count(*) from public.kruize_experiments ;" | tail -3 | head -1 | tr -d '[:space:]')
actual_results_count=$(kubectl exec $(kubectl get pods -o=name -n ${NAMESPACE} | grep kruize-db) -n ${NAMESPACE} -- psql -U admin -d kruizeDB -c "SELECT count(*) from public.kruize_results ;" | tail -3 | head -1 | tr -d '[:space:]')

echo "" | tee -a ${LOG}
echo "###########################################################################" | tee -a ${LOG}
echo "Scale test completed!" | tee -a ${LOG}
echo "exps_count = ${exp_count} results_count = ${actual_results_count}" | tee -a ${LOG}
echo "###########################################################################" | tee -a ${LOG}

# ========================
# Summary
# ========================
sleep 10
echo "" | tee -a ${LOG}
echo "###########################################################################" | tee -a ${LOG}
echo "Summary of the test run" | tee -a ${LOG}

exp_count=$(kubectl exec $(kubectl get pods -o=name -n ${NAMESPACE} | grep kruize-db) -n ${NAMESPACE} -- psql -U admin -d kruizeDB -c "SELECT count(*) from public.kruize_experiments ;" | tail -3 | head -1 | tr -d '[:space:]')
results_count=$(kubectl exec $(kubectl get pods -o=name -n ${NAMESPACE} | grep kruize-db) -n ${NAMESPACE} -- psql -U admin -d kruizeDB -c "SELECT count(*) from public.kruize_results ;" | tail -3 | head -1 | tr -d '[:space:]')
reco_count=$(kubectl exec $(kubectl get pods -o=name -n ${NAMESPACE} | grep kruize-db) -n ${NAMESPACE} -- psql -U admin -d kruizeDB -c "SELECT count(*) from public.kruize_recommendations ;" | tail -3 | head -1 | tr -d '[:space:]')

echo "exp_count / results_count / reco_count = ${exp_count} / ${results_count} / ${reco_count}" | tee -a ${LOG}

db_size=$(kubectl exec $(kubectl get pods -o=name -n ${NAMESPACE} | grep kruize-db) -n ${NAMESPACE} -- psql -U admin -d kruizeDB -c "SELECT pg_database_size('kruizeDB') AS database_size_bytes;" | tail -3 | head -1 | tr -d '[:space:]')
db_size_mb=$((db_size / (1024 * 1024) + 1))
echo "Postgres DB size in MB = ${db_size_mb}" | tee -a ${LOG}

if ls "${LOG_DIR}/results"/total_*.csv 1>/dev/null 2>&1; then
	echo "python3 ${CURRENT_DIR}/../scale_test/parse_metrics.py -d \"${LOG_DIR}/results\" -r \"${results_count}\"" | tee -a ${LOG}
	python3 ${CURRENT_DIR}/../scale_test/parse_metrics.py -d "${LOG_DIR}/results" -r "${results_count}" 2>&1 | tee -a ${LOG}
else
	echo "No metrics CSV files found in ${LOG_DIR}/results, skipping parse_metrics" | tee -a ${LOG}
fi

echo "###########################################################################" | tee -a ${LOG}

end_time=$(get_date)
elapsed_time=$(time_diff "${start_time}" "${end_time}")
echo "" | tee -a ${LOG}
echo "Test took ${elapsed_time} seconds to complete" | tee -a ${LOG}
echo "Logs: ${LOG_DIR}" | tee -a ${LOG}

kill ${QUERY_DB_PID} 2>/dev/null
wait 2>/dev/null
