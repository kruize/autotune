#!/bin/bash
#
# Copyright (c) 2025, 2025 IBM Corporation, RedHat and others.
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
### Script to run all the kruize demos ###

CURRENT_DIR="$(dirname "$(realpath "$0")")"
KRUIZE_DEMO_REPO="${CURRENT_DIR}/kruize-demos"

# Source the common functions scripts
. ${CURRENT_DIR}/../common/common_functions.sh

RESULTS_DIR=/tmp/kruize_demos_test_results
CLUSTER_TYPE=minikube
NAMESPACE=monitoring
demo=all

target="crc"
KRUIZE_IMAGE="quay.io/kruizehub/autotune-test-image:mvp_demo"
KRUIZE_OPERATOR_IMAGE=""
KRUIZE_OPERATOR=1
failed=0

KRUIZE_DEMOS_REPO="https://github.com/kruize/kruize-demos.git"
KRUIZE_DEMOS_BRANCH="main"
SETUP=0
WAIT_TIME=1800

function usage() {
	echo
	echo "Usage: -c cluster_type[minikube|openshift] [-i Kruize image] [-o Kruize operator image] [ -t demo ] [-r <resultsdir path>] [-a Kruize demos git repo URL] [-b Kruize demos branch] [-k]"
	echo "c = supports minikube, kind and openshift cluster-type"
	echo "i = kruize image. Default - quay.io/kruizehub/autotune-test-image:mvp_demo"
	echo "o = Kruize operator image. Default - It will use the latest kruize operator image"
	echo "a = Kruize demos git repo URL. Default - https://github.com/kruize/kruize-demos.git"
	echo "b = Kruize demos git repo branch. Default - main"
	echo "t = Kruize demo to run. Default - all (valid values - all/local_monitoring/remote_monitoring/bulk/vpa)"
	echo "r = Kruize results dir path. Default - /tmp/kruize_demos_test_results"
	echo "k = Disable operator and install kruize using deploy scripts instead."
	echo "w = Wait time for metrics to be available before recommedations are generated in bulk demo on a fresh cluster setup. Default 1800s"
	exit 1
}

function get_kruize_pod_log() {
	log=$1

	# Fetch the kruize pod log
	echo "" | tee -a ${LOG}
	echo "Fetch the kruize pod logs and store in ${log}..." | tee -a ${LOG}
	
	# Get the kruize pod name
	kruize_pod=$(kubectl get pods -n ${NAMESPACE} -l app=kruize -o jsonpath='{.items[0].metadata.name}')

	# Check if kruize_pod is empty
	if [ -z "${kruize_pod}" ]; then
		error_msg="Error: No kruize pod found in namespace ${NAMESPACE}, unable to fetch kruize pod log"
		echo "${error_msg}" | tee -a ${LOG}
	else
		echo "kubectl logs -f ${kruize_pod} -n ${NAMESPACE} > ${log} 2>&1 &" | tee -a ${LOG}
		kubectl logs -f ${kruize_pod} -n ${NAMESPACE} > ${log} 2>&1 &
	fi
}

function local_monitoring_demo() {
	demo_name="local_monitoring"
	demo_dir="local_monitoring"
	run_demo "${demo_name}" "${demo_dir}"
}

function bulk_demo() {
	demo_name="bulk"
	demo_dir="local_monitoring/bulk_demo"
	run_demo "${demo_name}" "${demo_dir}"
}

function vpa_demo() {
	demo_name="vpa"
	demo_dir="local_monitoring/vpa_demo"
	run_demo "${demo_name}" "${demo_dir}"
}

function runtimes_demo() {
	demo_name="runtimes"
	demo_dir="local_monitoring/runtimes_demo"
	run_demo "${demo_name}" "${demo_dir}"
}

function remote_monitoring_demo() {
	demo_name="remote_monitoring"
	demo_dir="remote_monitoring_demo"
	run_demo "${demo_name}" "${demo_dir}"

}

function all_demos() {
	local_monitoring_demo
	vpa_demo
	bulk_demo
	runtimes_demo
	if [ "${KRUIZE_OPERATOR}" == 0 ]; then
		# Include demos supported with manifests
		remote_monitoring_demo
	fi
}

function check_log() {
	log=$1
	echo ""
	echo "Checking $log for exceptions/failed messages..." | tee -a ${LOG}
	if grep -Eqi "exception|failed" "${log}"; then
		echo "Exception/Failed messages found in ${log}" | tee -a ${LOG}
		failed=1
	fi
}

function validate_sysbench_reco() {
	# Obtain the kruize recommendations for the vpa optimize-sysbench
	DEMO_LOG_DIR=$1
	vpa_json="${DEMO_LOG_DIR}/vpa.json"
	kubectl get vpa "optimize-sysbench" -o json > "${vpa_json}"
	if [ ! -e "${vpa_json}" ]; then
		echo "VPA optimize-sysbench not created"
		failed=1
	fi

	CONTAINER="sysbench"

	# Extract the cpu / memory limits and requests
	readarray -t VPA_RECOS < <(
		jq -r --arg name "${CONTAINER}" '
	        .status.recommendation.containerRecommendations[] |
        	select(.containerName == $name) |
        	[
            		.lowerBound.cpu,
			.lowerBound.memory,
			.upperBound.cpu,
			.upperBound.memory
		] |
	     	.[]' "${vpa_json}"
	)

	# Check if reco array is empty
	if [ ${#VPA_RECOS[@]} -eq 0 ]; then
		echo "Could not find vpa recommendations for container ${CONTAINER}"
		failed=1
	fi

	for vpa in "${VPA_RECOS[@]}"; do
		echo "${vpa}"
	done

	# Obtain the resource settings for the sysbench pod and check if the vpa recommendations are applied
	sysbench_json="${DEMO_LOG_DIR}/sysbench.json"
	kubectl get pods -l app=sysbench -n default -o json > "${sysbench_json}"

	if [ ! -e "${sysbench_json}" ]; then
                echo "Unable to fetch sysbench pod json"
                failed=1
        fi

	CONTAINER="sysbench"

	# Extract the cpu / memory limits and requests
	readarray -t SYSBENCH_RECOS < <(
		jq -r --arg name "${CONTAINER}" '
	        .items[0].spec.containers[0] |
        	select(.name == $name) |
        	[
            		.resources.requests.cpu,
			.resources.requests.memory,
			.resources.limits.cpu,
			.resources.limits.memory
		] |
	     	.[]' "${sysbench_json}"
	)

	# Check if reco array is empty
	if [ ${#SYSBENCH_RECOS[@]} -eq 0 ]; then
		echo "Could not find sysbench recommendations for container ${CONTAINER}"
		failed=1
	fi
	
	for sysbench in "${SYSBENCH_RECOS[@]}"; do
		echo "${sysbench}"
	done

	no_match=0
	for ((i=0; i<${#VPA_RECOS[@]}; i++)); do
		if [ "${SYSBENCH_RECOS[i]}" != "${VPA_RECOS[i]}" ]; then
			echo "sysbench - ${SYSBENCH_RECOS[i]} vpa - ${VPA_RECOS[i]}"
			no_match=1
		fi
	done

	if [ "${no_match}" == 1 ]; then
		echo "VPA recommendations not applied to Sysbench app"
		failed=1
	fi
}

function run_demo() {
	DEMO_NAME=$1
	DEMO_DIR=$2

	if [[ "${DEMO_NAME}" == "local_monitoring" ]]; then
		CMD=(./local_monitoring_demo.sh -c ${CLUSTER_TYPE} -i ${KRUIZE_IMAGE})
		JSONS=(container_experiment_local_recommendation.json namespace_experiment_local_recommendation.json)
		if [[ "${CLUSTER_TYPE}" == minikube || "${CLUSTER_TYPE}" == "kind" ]]; then
			if [[ "${SETUP}" == 0 ]]; then
				JSONS=(container_experiment_sysbench_recommendation.json namespace_experiment_sysbench_recommendation.json)
			fi
		fi
	elif [[ "${DEMO_NAME}" == "remote_monitoring" ]]; then
		CMD=(./remote_monitoring_demo.sh -c ${CLUSTER_TYPE} -o ${KRUIZE_IMAGE})
		JSONS=(recommendations_data.json)
	elif [[ "${DEMO_NAME}" == "bulk" ]]; then
		CMD=(./bulk_service_demo.sh -c ${CLUSTER_TYPE} -i ${KRUIZE_IMAGE})
		JSONS=(recommendations_data.json)
	elif [[ "${DEMO_NAME}" == "vpa" ]]; then
		CMD=(./vpa_demo.sh -c ${CLUSTER_TYPE} -i ${KRUIZE_IMAGE})
		JSONS=(container_vpa_experiment_sysbench_recommendation.json)
	elif [[ "${DEMO_NAME}" == "runtimes" ]]; then
		CMD=(./runtimes_demo.sh -c ${CLUSTER_TYPE} -i ${KRUIZE_IMAGE})
		JSONS=(create_tfb-db_exp_recommendation.json create_tfb_exp_recommendation.json create_petclinic_openj9_exp_recommendation.json)
		if [[ "${CLUSTER_TYPE}" == "openshift" ]]; then
			JSONS=(create_tfb-db_exp_ocp_recommendation.json create_tfb_exp_ocp_recommendation.json create_petclinic_openj9_exp_ocp_recommendation.json)
		fi
	fi

	if [ "${KRUIZE_OPERATOR}" == 1 ]; then
		if [ "${KRUIZE_OPERATOR_IMAGE}" != "" ]; then
			if [[ "${DEMO_NAME}" != "remote_monitoring" && "${DEMO_NAME}" != "bulk" ]]; then
				CMD+=( -o ${KRUIZE_OPERATOR_IMAGE})
			fi
		fi
	fi

	if [[ "${CLUSTER_TYPE}" == minikube || "${CLUSTER_TYPE}" == "kind" ]]; then
		# Remote monitoring doesn't support -f option, remove this check once it is fixed
		if [ "${DEMO_NAME}" != "remote_monitoring" ]; then
			# Use -f to do the cluster and prometheus setup if not done already
			if [[ "${SETUP}" == 0 ]]; then
				CMD+=( -f)
				SETUP=1
			fi
		fi
		if [ "${DEMO_NAME}" == "bulk" ]; then
			CMD+=( -w ${WAIT_TIME})
		fi
	fi

	if [ "${KRUIZE_OPERATOR}" == 0 ]; then
		if [ "${DEMO_NAME}" != "remote_monitoring" ]; then
			CMD+=( -k)
		fi
	fi

	CLEANUP_CMD="${CMD[@]} -t"

	DEMO_LOG_DIR="${LOG_DIR}/${DEMO_NAME}"

	mkdir -p "${DEMO_LOG_DIR}"
	demo_log="${DEMO_LOG_DIR}/${DEMO_NAME}_kruize-demo.log"

	{
		echo
		echo "********************************************************************"
		echo "Running ${DEMO_NAME} demo..." 
		echo "********************************************************************"
		echo
	
		echo "DEMO_NAME = $DEMO_NAME"
		echo "DEMO_DIR = $DEMO_DIR"
		echo "CMD = ${CMD[@]}"
		echo "EXPECTED JSONS = ${JSONS[@]}"
		echo
	} | tee -a ${LOG}
	pushd "${DEMO_DIR}" > /dev/null
		# Cleanup before running the test
		# For kruize operator to be cleaned up clone the repo
		if [[ "${KRUIZE_OPERATOR}" -eq 1 ]]; then
			if [ "${DEMO_NAME}" != "remote_monitoring" ]; then
				pwd
				clone_repos "kruize-operator"
			fi
		fi
		pwd
		#${CLEANUP_CMD[@]} | tee -a ${LOG}

		# Sleep for a few seconds for cleanup to complete
		sleep 20

		${CMD[@]} | tee -a ${LOG}

		# Copy the demo log to results dir
		if [ "${DEMO_NAME}" == "bulk" ]; then
			log_name="kruize-bulk-demo.log"
		else
			log_name="kruize-demo.log"
		fi

		if [ -e "${log_name}" ]; then
			cp "${log_name}" "${demo_log}"
		else
			echo "Missing ${demo_log}" | tee -a ${LOG}
			failed=1
		fi

		# Copy the recommendation jsons
		for reco_json in ${JSONS[@]}; do
			if [ -e "${reco_json}" ]; then
				cp "${reco_json}" "${DEMO_LOG_DIR}/${reco_json}"
				if ! grep -qi "Recommendations Are Available" ${reco_json}; then
					echo "Recommendations not found, check ${DEMO_LOG_DIR}/${reco_json}" | tee -a ${LOG}
					failed=1
				fi
			else
				echo "Missing ${reco_json}" | tee -a ${LOG}
				failed=1
			fi
		done

		# If demo is VPA check for vpa recommendations
		if [ "${DEMO_NAME}" == "vpa" ]; then
			# sleep for the vpa to be created
			sleep 60
			echo "Validating vpa recommendations..." | tee -a ${LOG}
			validate_sysbench_reco "${DEMO_LOG_DIR}" | tee -a ${LOG}
			echo "Validating vpa recommendations...Done" | tee -a ${LOG}

		fi

		KRUIZE_POD_LOG="${DEMO_LOG_DIR}/${DEMO_NAME}_kruize_pod.log"
		get_kruize_pod_log "${KRUIZE_POD_LOG}"
		sleep 5
		check_log "${KRUIZE_POD_LOG}"
		sleep 2

	popd > /dev/null 2>&1
	{
		if [ "${failed}" -ne 0 ]; then
			echo "Kruize ${DEMO_NAME} failed! Check the logs for details"
		else
			echo "Kruize ${DEMO_NAME} passed!"
		fi
		echo 
		echo "********************************************************************"
		echo "Running ${DEMO_NAME} demo...Done"
		echo "********************************************************************"
		echo
	} | tee -a ${LOG}
}

while getopts c:w:r:i:o:a:b:t:kh gopts
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
	w)
		WAIT_TIME="${OPTARG}"
		;;
	o)
		KRUIZE_OPERATOR_IMAGE="${OPTARG}"		
		;;
	a)
		KRUIZE_DEMOS_REPO="${OPTARG}"		
		;;
	b)
		KRUIZE_DEMOS_BRANCH="${OPTARG}"		
		;;
	t)
		demo="${OPTARG}"		
		;;
	k)
		KRUIZE_OPERATOR=0
      		;;
	h)
		usage
		;;
	esac
done

if [ -z "${CLUSTER_TYPE}" ]; then
	usage
fi

if [ "${CLUSTER_TYPE}" == "openshift" ]; then
	NAMESPACE="openshift-tuning"
fi

start_time=$(get_date)
LOG_DIR="${RESULTS_DIR}/kruize-demos-test-results-$(date +%Y%m%d%H%M)"
mkdir -p ${LOG_DIR}

LOG="${LOG_DIR}/kruize-demos-test.log"

# Clone kruize-demos repo from the specified branch
repo_name="kruize-demos"

if [ ! -d ${repo_name} ]; then
	echo "Cloning ${repo_name} git repo..." | tee -a ${LOG}
	git clone -b ${KRUIZE_DEMOS_BRANCH} ${KRUIZE_DEMOS_REPO} > /dev/null 2> /dev/null
	check_err "ERROR: git clone of kruize/${repo_name} failed." | tee -a ${LOG}
fi

# Change to demos dir
cd kruize-demos/monitoring

case ${demo} in
	all)
		all_demos
		;;
	local_monitoring)
		local_monitoring_demo
		;;
	remote_monitoring)
		remote_monitoring_demo
		;;
	bulk)
		bulk_demo
		;;
	vpa)
		vpa_demo
		;;
	*)
		err_exit "Error: ${demo} is not supported. Valid demos - all/local_monitoring/remote_monitoring/bulk/vpa" | tee -a ${LOG}
		;;
esac	

cd ../..

end_time=$(get_date)
elapsed_time=$(time_diff "${start_time}" "${end_time}")
echo "Test took ${elapsed_time} seconds to complete" | tee -a ${LOG}

# Clone kruize-demos repo
delete_repos "kruize-demos"

if [ "${failed}" -ne 0 ]; then
	echo "Kruize demos test failed! Check the logs for details" | tee -a ${LOG}
	exit 1
else
	echo "Kruize demos test passed!" | tee -a ${LOG}
	exit 0
fi
