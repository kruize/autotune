#!/bin/bash
#
# Copyright (c) 2020, 2024 Red Hat, IBM Corporation and others.
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
##### Common routines used in the tests #####
#

CURRENT_DIR="$(dirname "$(realpath "$0")")"
KRUIZE_REPO="${CURRENT_DIR}/.."

# variables to keep track of overall tests performed
TOTAL_TESTS_FAILED=0
TOTAL_TESTS_PASSED=0
TOTAL_TEST_SUITES=0
TOTAL_TESTS=0

# variables to keep track of tests performed for each test suite
TESTS_FAILED=0
TESTS_PASSED=0
TESTS=0

TEST_SUITE_ARRAY=("remote_monitoring_tests"
"local_monitoring_tests"
"authentication_tests")

KRUIZE_DOCKER_IMAGE="quay.io/kruizehub/autotune-test-image:mvp_demo"
total_time=0
matched=0
setup=1
skip_setup=0

cleanup_prometheus=0

target="crc"

#   Clone git Repos
function clone_repos() {
	repo_name=$1
	echo
	echo "#######################################"
	echo "Cloning ${repo_name} git repos"
	if [ ! -d ${repo_name} ]; then
		git clone git@github.com:kruize/${repo_name}.git >/dev/null 2>/dev/null
		if [ $? -ne 0 ]; then
			git clone https://github.com/kruize/${repo_name}.git 2>/dev/null
		fi
		check_err "ERROR: git clone of kruize/${repo_name} failed."
	fi

	echo "done"
	echo "#######################################"
	echo
}


#   Cleanup git Repos
function delete_repos() {
	app_name=$1
	echo "Deleting ${app_name} git repos"
	rm -rf ${app_name} benchmarks
}

# checks if the previous command is executed successfully
# input:Return value of previous command
# output:Prompts the error message if the return value is not zero and exits
function err_exit() {
	err=$?
	if [ ${err} -ne 0 ]; then
		echo "$*"
		exit -1
	fi
}

# checks if the previous command is executed successfully
# input:Return value of previous command
# output:Prompts the error message if the return value is not zero
function check_err() {
	err=$?
	if [ ${err} -ne 0 ]; then
		echo "$*"
	fi
}

# Check if jq is installed
function check_prereq() {
	echo
	echo "Info: Checking prerequisites..."
	# check if jq exists
	if ! [ -x "$(command -v jq)" ]; then
		echo "Error: jq is not installed."
		exit 1
	fi
}

# get date in format
function get_date() {
	date "+%Y-%m-%d %H:%M:%S"
}

function increment_timestamp_by_days() {
        initial_start_date=$1
        days_to_add=$2

        # Extract the date, time, and timezone parts from the initial date string
        date_part=$(echo "$initial_start_date" | cut -d'T' -f1)
        time_part=$(echo "$initial_start_date" | cut -d'T' -f2 | cut -d'.' -f1)
        timezone_part=$(echo "$initial_start_date" | awk -F'T' '{print $2}' | cut -d'.' -f2)

        # Remove trailing zeros from timezone part (e.g., 000000000Z to 000Z)
        trimmed_timezone=$(echo "$timezone_part" | sed 's/0*$//')

        # Use date command to increment the date by the specified days
        incremented_date=$(date -u -d "$date_part + $days_to_add days" +%Y-%m-%dT$time_part.${trimmed_timezone})

        echo "$incremented_date"
}

function time_diff() {
	ssec=`date --utc --date "$1" +%s`
	esec=`date --utc --date "$2" +%s`

	diffsec=$(($esec-$ssec))
	echo $diffsec
}

# Set up autotune
function setup() {
	KRUIZE_POD_LOG=$1

	# remove the existing autotune objects
	autotune_cleanup ${TEST_SUITE_DIR}

	# Wait for 5 seconds to terminate the autotune pod
	sleep 5

	# Check if jq is installed
	check_prereq

	# Deploy autotune
	echo "Deploying autotune..."
	
	deploy_autotune  "${cluster_type}" "${KRUIZE_DOCKER_IMAGE}" "${KRUIZE_POD_LOG}"
	echo "Deploying autotune...Done"

	case "${cluster_type}" in
		minikube)
			NAMESPACE="monitoring"
			;;
		openshift)
			NAMESPACE="openshift-tuning"
			;;
	esac
}

# Check if the prometheus is already deployed , if not invoke the script to deploy prometheus on minikube
function setup_prometheus() {
	kubectl_cmd="kubectl"
	prometheus_pod_running=$(${kubectl_cmd} get pods --all-namespaces | grep "prometheus-k8s-1")
	if [ "${prometheus_pod_running}" == "" ]; then
		./scripts/prometheus_on_minikube.sh -as
	fi
}

# Deploy autotune
# input: cluster type , autotune image
# output: Deploy autotune based on the parameter passed
function deploy_autotune() {
	cluster_type=$1
	KRUIZE_IMAGE=$2
	KRUIZE_POD_LOG=$3

	# Check if the cluster_type is minikube., if so deploy prometheus
	if [ "${cluster_type}" == "minikube" ]; then
		echo "Installing Prometheus on minikube" >>/dev/stderr
		setup_prometheus >> ${KRUIZE_SETUP_LOG} 2>&1
	fi

	echo "Deploying autotune"
	cmd="./deploy.sh -c ${cluster_type} -i ${KRUIZE_IMAGE} -m ${target}"
	echo "Kruize deploy command - ${cmd}"
	${cmd}

	status="$?"
	# Check if autotune is deployed.
	if [[ "${status}" -eq "1" ]]; then
		echo "Error deploying autotune" >>/dev/stderr
		echo "See ${KRUIZE_SETUP_LOG}" >>/dev/stderr
		exit -1
	fi

	sleep 30

	if [[ ${cluster_type} == "minikube" || ${cluster_type} == "openshift" ]]; then
		sleep 2
		echo "Capturing Autotune service log into ${KRUIZE_POD_LOG}"
		namespace="openshift-tuning"
		if [ ${cluster_type} == "minikube" ]; then
			namespace="monitoring"
		fi
		echo "Namespace = $namespace"
		service="kruize"
		kruize_pod=$(kubectl get pod -n ${namespace} | grep ${service} | grep -v kruize-ui | grep -v kruize-db | cut -d " " -f1)
		echo "kruize_pod = $kruize_pod"
		echo "kubectl -n ${namespace} logs -f ${kruize_pod} > "${KRUIZE_POD_LOG}" 2>&1 &"
		kubectl -n ${namespace} logs -f ${kruize_pod} > "${KRUIZE_POD_LOG}" 2>&1 &
	fi
}

# Remove the prometheus setup
# output: Remove all the prometheus dependencies
function prometheus_cleanup() {
	kubectl_cmd="kubectl"
	prometheus_pod_running=$(${kubectl_cmd} get pods --all-namespaces | grep "prometheus-k8s-1"| awk '{print $4}')
	if [ "${prometheus_pod_running}" == "Running" ]; then
		./scripts/prometheus_on_minikube.sh -t
	fi
}

# Remove the autotune setup
# output: Remove all the autotune dependencies
function autotune_cleanup() {
	RESULTS_LOG=$1

	# If autotune cleanup is invoke through -t option then setup.log will inside the given result directory
	if [ ! -z "${RESULTS_LOG}" ]; then
		KRUIZE_SETUP_LOG="${RESULTS_LOG}/kruize_setup.log"
		pushd ${KRUIZE_REPO} > /dev/null
	else
		KRUIZE_SETUP_LOG="kruize_setup.log"
		pushd ${KRUIZE_REPO}/autotune > /dev/null
	fi

	echo  "Removing Autotune dependencies..."
	cmd="./deploy.sh -c ${cluster_type} -m ${target} -t"
	echo "CMD = ${cmd}"
	${cmd} >> ${KRUIZE_SETUP_LOG} 2>&1
	# Remove the prometheus setup
	if [ "${cleanup_prometheus}" -eq "1" ]; then
		prometheus_cleanup
	fi
	popd > /dev/null
	echo "done"
}

# Restore DB from the file passed as input
function restore_db() {
	db_backup_file=$1
    	db_restore_log=$2

	echo ""
	echo "Restoring DB..."
	kruize_db_pod=$(kubectl get pods -o=name -n ${NAMESPACE} | grep kruize-db | cut -d '/' -f2)
	db_file=$(basename ${db_backup_file})

	echo "oc cp ${db_backup_file} ${NAMESPACE}/${kruize_db_pod}:/"
	oc cp ${db_backup_file} ${NAMESPACE}/${kruize_db_pod}:/

	echo "kubectl exec -it ${kruize_db_pod} -n ${NAMESPACE} -- psql -U admin -d kruizeDB -f ${db_file} > ${db_restore_log}"
	kubectl exec -it ${kruize_db_pod} -n ${NAMESPACE} -- psql -U admin -d kruizeDB -f ${db_file} > ${db_restore_log}
	echo "Restoring DB...done"
	echo ""
}

# list of test cases supported
# input: testsuite
# ouput: print the testcases supported for specified testsuite
function test_case_usage() {
	checkfor=$1
	typeset -n da_tests="${checkfor}_tests"
	echo
	echo "Supported Test cases are:"
	for tests in "${da_tests[@]}"
	do
		echo "		           ${tests}"
	done
}

# Check if the given test case is supported
# input: testsuite
# output: check if the specified testcase is supported if not then call test_case_usage
function check_test_case() {
	checkfor=$1
	typeset -n da_tests=${checkfor}_tests
	for test in ${da_tests[@]}
	do
		if [ "${testcase}" == "${test}" ]; then
			testcase_matched=1
		fi
	done

	if [ "${testcase}" == "help" ]; then
		test_case_usage ${checkfor}
		exit -1
	fi

	if [ "${testcase_matched}" -eq "0" ]; then
		echo ""
		echo "Error: Invalid testcase **${testcase}** "
		test_case_usage ${checkfor}
		exit -1
	fi
}

# get the summary of each test suite
# input: Test suite name for which you want to get the summary and the failed test cases
# output: summary of the specified test suite
function testsuitesummary() {
	TEST_SUITE_NAME=$1
	elapsed_time=$2
	FAILED_CASES=$3
	((total_time=total_time+elapsed_time))
	echo
	echo "########### Results Summary of the test suite ${TEST_SUITE_NAME} ##########"
	echo "${TEST_SUITE_NAME} took ${elapsed_time} seconds"
	echo "Number of tests performed ${TESTS}"
	echo "Number of tests passed ${TESTS_PASSED}"
	echo "Number of tests failed ${TESTS_FAILED}"
	echo ""
	if [[ "${TESTS_FAILED}" -ne "0" || "${TESTS_PASSED}" -eq "0" ]]; then
		echo "~~~~~~~~~~~~~~~~~~~~~~~ ${TEST_SUITE_NAME} failed ~~~~~~~~~~~~~~~~~~~~~~~~~~"
		echo "Failed cases are :"
		for fails in "${FAILED_CASES[@]}"
		do
			echo "		  ${fails}"
		done
		echo
		echo "Check Log Directory: ${TEST_SUITE_DIR} for failed cases "
		echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
	else
		echo "~~~~~~~~~~~~~~~~~~~~~~ ${TEST_SUITE_NAME} passed ~~~~~~~~~~~~~~~~~~~~~~~~~~"
	fi
	echo ""
	echo "************************************** done *************************************"
}

# get the overall summary of the test
# input: failed test suites
# output: summary of the overall tests performed
function overallsummary(){
	FAILED_TEST_SUITES=$1
	echo "Total time taken to perform the test ${total_time} seconds"
	echo "Total Number of test suites performed ${TOTAL_TEST_SUITES}"
	echo "Total Number of tests performed ${TOTAL_TESTS}"
	echo "Total Number of tests passed ${TOTAL_TESTS_PASSED}"
	echo "Total Number of tests failed ${TOTAL_TESTS_FAILED}"
	if [ "${TOTAL_TESTS_FAILED}" -ne "0" ]; then
		echo ""
		echo "Check below logs for failed test cases:"
		for fails in "${FAILED_TEST_SUITE[@]}"
		do
			echo "		                        ${fails}"
		done
	fi
}

# Set app folder according to specific application
# input: application name
# output: set the benchmark application folder based on the application name passed
function set_app_folder() {
	app_name=$1
	if [ "${app_name}" == "petclinic" ]; then
		APP_FOLDER="spring-petclinic"
	elif [ "${app_name}" == "tfb-qrh" ]; then
                APP_FOLDER="techempower"
	else
		APP_FOLDER="${app_name}"
	fi
}

# Run jmeter load for acmeair application
# input: application name, number of instances, number of iterations
# output: Apply the jmeter load based on the input
function run_jmeter_load() {
	app_name=$1
	num_instances=$2
	MAX_LOOP=2
	set_app_folder "${app_name}"
	echo
	echo "Starting ${app_name} jmeter workload..."
	# Invoke the jmeter load script
	if [ ${app_name} == "tfb-qrh" ]; then
		${APP_REPO}/${APP_FOLDER}/scripts/tfb-load.sh -c ${cluster_type} -i ${num_instances} --iter=${MAX_LOOP}
	else
		${APP_REPO}/${APP_FOLDER}/scripts/${app_name}-load.sh -c ${cluster_type} -i ${num_instances} --iter=${MAX_LOOP}
	fi
}

# Remove the application setup
# input: application name
# output: Remove the instances of specified application
function app_cleanup() {
	app_name=$1
	set_app_folder "${app_name}"
	echo
	echo -n "Removing ${app_name} app..."
	if [ ${app_name} == "tfb-qrh" ]; then
		${APP_REPO}/${APP_FOLDER}/scripts/tfb-cleanup.sh -c ${cluster_type} >> ${KRUIZE_SETUP_LOG} 2>&1
	else
		${APP_REPO}/${APP_FOLDER}/scripts/${app_name}-cleanup.sh -c ${cluster_type} >> ${KRUIZE_SETUP_LOG} 2>&1
	fi
	echo "done"
}

# Deploy the specified number of instances of given application on a given cluster type
# input: application name, number of instances
# ouput: Deploy the benchmark application based on the input
function deploy_app() {
	APP_REPO=$1
	app_name=$2
	num_instances=$3

	echo "$APP_REPO $app_name $num_instances"
	set_app_folder "${app_name}"

	if [ ${num_instances} == 1 ]; then
		echo "Deploying ${num_instances} instance of ${app_name} app..."
	else
		echo "Deploying ${num_instances} instances of ${app_name} app..."
	fi

	# Invoke the deploy script from app benchmark
	if [ ${cluster_type} == "openshift" ]; then
		if [ ${app_name} == "tfb-qrh" ]; then
			${APP_REPO}/${APP_FOLDER}/scripts/tfb-deploy.sh --clustertype=${cluster_type} -s ${kurl} -i ${num_instances}  >> ${KRUIZE_SETUP_LOG} 2>&1
                else
			${APP_REPO}/${APP_FOLDER}/scripts/${app_name}-deploy-openshift.sh -s ${kurl} -i ${num_instances}  >> ${KRUIZE_SETUP_LOG} 2>&1
		fi
	else
		if [ ${app_name} == "tfb-qrh" ]; then
			${APP_REPO}/${APP_FOLDER}/scripts/tfb-deploy.sh --clustertype=${cluster_type} -s "localhost" -i ${num_instances}  >> ${KRUIZE_SETUP_LOG} 2>&1
		else
			${APP_REPO}/${APP_FOLDER}/scripts/${app_name}-deploy-${cluster_type}.sh -i ${num_instances}  >> ${KRUIZE_SETUP_LOG} 2>&1
		fi
	fi
	echo "done"
}

# Check if the application of kind autotune is deployed
# input: application name
# output: check if the autotune object of specified application is created
function check_app_status() {
	APP=$1
	status=$(kubectl get autotune | grep "${APP}" | cut -d " " -f1)
	if [ "${status}" ]; then
		echo "${APP} application of kind autotune is deployed successfully"
		return 0
	fi
}

# print the message for the test
# input: status
# ouput: based on the status passed print the messages
function error_message() {
	failed=$1
	echo ""
	# check for failed cases
	if [ "${failed}" -eq "0" ]; then
		((TESTS_PASSED++))
		((TOTAL_TESTS_PASSED++))
		echo "Expected message is : ${expected_log_msg}"| tee -a ${LOG}
		echo "Expected message found in the log"
		echo "Test Passed" | tee -a ${LOG}
	else
		((TESTS_FAILED++))
		((TOTAL_TESTS_FAILED++))
		FAILED_CASES+=(${testcase})
		echo "Expected message is : ${expected_log_msg}"| tee -a ${LOG}
		echo "Expected message not found"
		echo "Test failed" | tee -a ${LOG}
	fi
}

# Form the curl command based on the cluster type
function form_curl_cmd() {
	crud_operation=$1
	# Form the curl command based on the cluster type
	service="kruize"
	case $cluster_type in
	   openshift)
		NAMESPACE="openshift-tuning"
		oc expose svc/${service} -n ${NAMESPACE}

		SERVER_IP=($(oc status --namespace=${NAMESPACE} | grep ${service} | grep port | cut -d " " -f1 | cut -d "/" -f3))
	        echo "IP = $SERVER_IP"

		AUTOTUNE_URL="http://${SERVER_IP}"
		;;
	   minikube)
		NAMESPACE="monitoring"

		echo "service = $service namespace = $NAMESPACE"
		AUTOTUNE_PORT=$(kubectl -n ${NAMESPACE} get svc ${service} --no-headers -o=custom-columns=PORT:.spec.ports[*].nodePort)
		SERVER_IP=$(minikube ip)
		echo "SERVER_IP = $SERVER_IP AUTOTUNE_PORT = $AUTOTUNE_PORT"
		AUTOTUNE_URL="http://${SERVER_IP}:${AUTOTUNE_PORT}"
		;;
	   docker) ;;
	   *);;
	esac

	curl_cmd="curl -s -H 'Accept: application/json' ${AUTOTUNE_URL}"
	if [ "${crud_operation}" == "update" ]; then
        	curl_cmd="curl -s -X PUT -H 'Accept: application/json' ${AUTOTUNE_URL}"
        fi

	echo "curl_cmd = ${curl_cmd}"
}

# Compare the actual json and expected jsons
# Input: Acutal json, expected json
function compare_json() {
	((TESTS++))
	((TOTAL_TESTS++))
	actual_json=$1
	expected_json=$2
	testcase=$3

	compared=$(jq --argfile actual ${actual_json} --argfile expected ${expected_json} -n '($actual | (.. | arrays) |= sort) as $actual | ($expected | (.. | arrays) |= sort) as $expected | $actual == $expected')
	if [ "${compared}" == "true" ]; then
		echo "Expected json matched with the actual json" | tee -a ${LOG}
		echo "Test passed" | tee -a ${LOG}
		((TESTS_PASSED++))
		((TOTAL_TESTS_PASSED++))
	else
		echo "Expected json did not match with the actual json" | tee -a ${LOG}
		echo "Test failed" | tee -a ${LOG}
		((TESTS_FAILED++))
		((TOTAL_TESTS_FAILED++))
		FAILED_CASES+=(${testcase})
	fi
}


function format_memory() {
	bound=$1
	memory_value=$(echo ${bound} | sed 's/[^0-9]*//g')
	memory_unit=$(echo ${bound} | sed 's/[0-9]*//g')
	memory_unit=$(echo ${memory_unit} | sed 's/ //g')
	memory_unit=$(echo ${memory_unit} | sed 's/^"\|"$//g')
	bound="\"${memory_value}.0${memory_unit}\""
	echo "$bound"
}

# Run the curl command passed and capture the json output in a file
# Input: curl command, json file name
function run_curl_cmd() {
	cmd=$1
	json_file=$2

	echo "Curl cmd=${cmd}" | tee -a ${LOG}
	echo "json file = ${json_file}" | tee -a ${LOG}
	${cmd} > ${json_file}
	echo "actual json" >> ${LOG}
	cat ${json_file} >> ${LOG}
	echo "" >> ${LOG}
}

function label_pods() {
	local -n _app_pod_names=$1
	local -n _label_names=$2
	inst=0

	echo ""
	for app in ${_app_pod_names[@]}
	do
		label=${_label_names[inst]}
		echo "Adding label ${label} to pod ${app}..."
		# change the label of the pod based on number of instances created
		kubectl label pod ${app} app.kubernetes.io/name=${label}  --overwrite=true
		((inst++))
	done
	echo ""
}

function apply_autotune_yamls(){
	APP_REPO=$1
	app_name=$2
	instances=$3
	AUTOTUNE_YAMLS_DIR=$4

	deployment_names=()

	echo "APP_REPO = $APP_REPO app = $app instances = $instances AUTOTUNE_YAMLS_DIR = $AUTOTUNE_YAMLS_DIR"

	set_app_folder ${app_name}
	AUTOTUNE_FILE="autotune-http_resp_time"

	# Copy the autotune yaml from benchmarks repo to resultdir
	cp "${APP_REPO}/${APP_FOLDER}/autotune/${AUTOTUNE_FILE}.yaml" "${AUTOTUNE_YAMLS_DIR}/${AUTOTUNE_FILE}.yaml"

	# Update the autotune yaml for each instance and apply it
	for(( inst=0; inst<${instances}; inst++ ))
	do
		sed 's/petclinic-autotune-min-http-response-time/petclinic-autotune-min-http-response-time-'${inst}'/g' ${AUTOTUNE_YAMLS_DIR}/${AUTOTUNE_FILE}.yaml > ${AUTOTUNE_YAMLS_DIR}/${AUTOTUNE_FILE}-${inst}.yaml
		sed -i 's/petclinic-deployment/petclinic-deployment-'${inst}'/g' ${AUTOTUNE_YAMLS_DIR}/${AUTOTUNE_FILE}-${inst}.yaml
		deployment_names[$inst]="petclinic-sample-${inst}"

		echo "Applying autotune yaml ${autotune}..."
		kubectl apply -f ${AUTOTUNE_YAMLS_DIR}/${AUTOTUNE_FILE}-${inst}.yaml
		check_err "Error: Issue in applying autotune yaml - ${AUTOTUNE_YAMLS_DIR}/${AUTOTUNE_FILE}-${inst}.yaml"
	done

	# Get the container images
	namespace="default"
	deployment="${deployment_names[0]}"
	container_images=$(kubectl get -n ${namespace} deployment ${deployment} -o jsonpath="{.spec.template.spec.containers[*].image}")
	IFS=' ' read -r -a container_images <<<  ${container_images}

	echo "container_images = ${container_images[@]}"
}

function get_autotune_jsons() {
	AUTOTUNE_JSONS_DIR=$1

	autotune_names=$(kubectl get autotune --no-headers=true | cut -d " " -f1 | tr "\n" " ")
	IFS=' ' read -r -a autotune_names <<<  ${autotune_names}
	echo ""
	echo "AUTOTUNE_JSONS_DIR = ${AUTOTUNE_JSONS_DIR}"

	# Create autotune object
	for autotune in "${autotune_names[@]}"
	do
		echo "autotune name = ${autotune}"
		kubectl get autotune/${autotune} -o json > ${AUTOTUNE_JSONS_DIR}/${autotune}.json

		if [ -z "${AUTOTUNE_JSONS_DIR}/${autotune}.json" ]; then
			echo "Fetching the autotune json for ${autotune} object failed!"
			exit -1
		fi
	done
	echo ""
}

function get_kruize_layer_jsons() {
	KRUIZE_LAYER_JSONS_DIR=$1

	kruize_layer_names=$(kubectl get autotuneconfig -n ${NAMESPACE} --no-headers=true | cut -d " " -f1 | tr "\n" " ")
	IFS=' ' read -r -a kruize_layer_names <<<  ${kruize_layer_names}

	echo "AUTOTUNE CONFIG JSONS DIR = ${KRUIZE_LAYER_JSONS_DIR}"
	# Create autotuneconfig object
	for autotuneconfig in "${kruize_layer_names[@]}"
	do
		echo "autotune config = ${autotuneconfig}"
		kubectl get autotuneconfig/${autotuneconfig} -o json -n ${NAMESPACE}  > ${KRUIZE_LAYER_JSONS_DIR}/${autotuneconfig}.json
		if [ -z "${KRUIZE_LAYER_JSONS_DIR}/${autotuneconfig}.json" ]; then
			echo "Fetching the autotune config json for ${autotuneconfig} object failed!"
			exit -1
		fi
	done
	echo ""
}

# Test to check the uniqueness of the ids
# output: set the flag to 1 if the ids are not unique
function uniqueness_test() {
	test_array=("$@")
	flag=0
	# If element of test_array is not in seen, then store in seen array. If the array elemen is already in seen then do not store it in seen array. UniqueNum is the number of array elements with unique values.
	uniqueNum=$(printf '%s\n' "${test_array[@]}"|awk '!($0 in seen){seen[$0];c++} END {print c}')

	if [ "${uniqueNum}" != "${#test_array[@]}" ]; then
		flag=1
	fi
}

# Display the result based on the actual flag value
# input: Expected behaviour, test name and actual flag value
function display_result() {
	expected_behaviour=$1
	_id_test_name_=$2
	actual_flag=$3
	((TOTAL_TESTS++))
	((TESTS++))
	echo "Expected behaviour: ${expected_behaviour}" | tee -a ${LOG}
	if [ "${actual_flag}" -eq "0" ]; then
		((TESTS_PASSED++))
		((TOTAL_TESTS_PASSED++))
		echo "Expected behaviour found" | tee -a ${LOG}
		echo "Test passed" | tee -a ${LOG}
	else
		((TESTS_FAILED++))
		((TOTAL_TESTS_FAILED++))
		FAILED_CASES+=(${_id_test_name_})
		echo "Expected behaviour not found" | tee -a ${LOG}
		echo "Test failed" | tee -a ${LOG}
	fi
}

# Deploy the application dependencies
# input: id test name
# output: deploy the benchmark application, label the application pods and deploy the required autotune objects.
function deploy_app_dependencies() {
	eval "declare -A app_array="${1#*=}
	yaml_dir=$2
	autotune_names_count=0
	autotune_names=()
	app_pod_names=()
	label_names=()

	# deploy benchmark applications
	for key in "${!app_array[@]}"
	do
		deploy_app ${APP_REPO} ${key} ${app_array[${key}]}

		# Sleep for sometime for application pods to be up
		sleep 10

		inst_count=0
		while [ "${inst_count}" -lt "${app_array[${key}]}" ]
		do
			case "${key}" in
			petclinic)
				FILE_NAME="autotune-http_resp_time"
				AUTOTUNE_YAML="${APP_REPO}/spring-petclinic/autotune/${FILE_NAME}.yaml"
				;;
			galaxies)
				FILE_NAME="autotune-app_resp_time"
				AUTOTUNE_YAML="${APP_REPO}/galaxies/autotune/${FILE_NAME}.yaml"
				;;
			esac

			autotune_name=$(cat $AUTOTUNE_YAML | grep "name: " | grep "${key}" | awk '{print $2}' | sed 's/^"\|"$//g')
			autotune_names+=("${autotune_name}-${inst_count}")
			label_names+=("${key}-deployment-${inst_count}")

			test_yaml="${yaml_dir}/${autotune_names[autotune_names_count]}.yaml"
			sed 's/'${key}'-deployment/'${key}'-deployment-'${inst_count}'/g' ${AUTOTUNE_YAML} > ${test_yaml}
			sed -i 's/'${autotune_name}'/'${autotune_name}'-'${inst_count}'/g' ${test_yaml}
			((inst_count++))
			((autotune_names_count++))
		done
		# Get the application pods
		#experiment_name=$(kubectl get pod | grep galaxies-sample-0 | grep "Running" | awk '{print $1}')
		app_pod_names+=$(kubectl get pod | grep ${key} | grep "Running" | cut -d " " -f1| tr '\n' ' ')
	done

	IFR=' ' read -r -a app_pod_names <<< ${app_pod_names}
	# Add label to your application pods for autotune to monitor
	label_pods app_pod_names label_names

	echo "yaml dir= ${yaml_dir}" | tee -a ${LOG}
	echo -n "Applying application autotune yaml..." | tee -a ${LOG}
	kubectl apply -f ${yaml_dir} >> ${KRUIZE_SETUP_LOG}
	echo "done" | tee -a ${LOG}
}

# Match the old id with the new id
function match_ids() {
	matched_count=0
	new_id_count=0
	for old in "${old_id_[@]}"
	do
		if [ "${old}" == "${new_id_[new_id_count]}" ]; then
			((matched_count++))
		fi
		((new_id_count++))
	done
}

# Get the autotune pod log
# input : Log file path to store the pod information
function get_autotune_pod_log() {
	log=$1
	# Fetch the container log

	echo "target = $target"
	kruize_pod=$(kubectl get pod -n ${NAMESPACE} | grep kruize | grep -v kruize-ui | grep -v kruize-db | cut -d " " -f1)
	pod_log_msg=$(kubectl logs ${kruize_pod} -n ${NAMESPACE})
	echo "${pod_log_msg}" > "${log}"
}

# Expose prometheus as nodeport and get the url
function expose_prometheus() {
	if [ "${cluster_type}" == "minikube" ]; then
		exposed=$(kubectl get svc -n ${NAMESPACE} | grep "prometheus-test")

		# Check if the service already exposed, If not then expose the service
		if [ -z "${exposed}" ]; then
			kubectl expose service prometheus-k8s --type=NodePort --target-port=9090 --name=prometheus-test -n ${NAMESPACE} >> ${LOG} 2>&1
		fi

		prometheus_url=$(minikube service list | grep "prometheus-test" | awk '{print $8}')
	fi
}

# Compare the actual result with the expected result
# input: Test name, expected result
function compare_result() {
	failed=0
	__test__=$1
	expected_result=$2
	expected_behaviour=$3

	if [[ ! ${actual_result} =~ ${expected_result} ]]; then
		failed=1
	fi

	display_result "${expected_behaviour}" "${__test__}" "${failed}"
}

function create_performance_profile() {
        perf_profile_json=$1

        echo "Forming the curl command to create the performance profile ..."
        form_curl_cmd

        curl_cmd="${curl_cmd}/createPerformanceProfile -d @${perf_profile_json}"

        echo "curl_cmd = ${curl_cmd}"

        status_json=$($curl_cmd)
        echo "create performance profile status = ${status_json}"

        echo ""
        echo "Command used to create the performance profile = ${curl_cmd}"
        echo ""

        perf_profile_status=$(echo ${status_json} | jq '.status')
        echo "create performance profile status = ${perf_profile_status}"
        if [ "${perf_profile_status}" != \"SUCCESS\" ]; then
                echo "Failed! Create performance profile failed. Status - ${perf_profile_status}"
                exit 1
        fi
}

function update_performance_profile() {
	perf_profile_json=$1
	operation="update"
	
	echo "Forming the curl command to update the performance profile ..."
	form_curl_cmd "${operation}"
	
	curl_cmd="${curl_cmd}/updatePerformanceProfile -d @${perf_profile_json}"
	echo "curl_cmd = ${curl_cmd}"
	
	status_json=$($curl_cmd)
	echo "update performance profile status = ${status_json}"
	echo ""
	echo "Command used to update the performance profile = ${curl_cmd}"
	echo ""
	perf_profile_status=$(echo ${status_json} | jq '.status')
	echo "update performance profile status = ${perf_profile_status}"
	if [ "${perf_profile_status}" != \"SUCCESS\" ]; then
		echo "Failed! Update performance profile failed. Status - ${perf_profile_status}"
		exit 1
	fi
}

function create_metric_profile() {
        metric_profile_json=$1

        echo "Forming the curl command to create the metric profile ..."
        form_curl_cmd

        curl_cmd="${curl_cmd}/createMetricProfile -d @${metric_profile_json}"

        echo "curl_cmd = ${curl_cmd}"

        status_json=$($curl_cmd)
        echo "create metric profile status = ${status_json}"

        echo ""
        echo "Command used to create the metric profile = ${curl_cmd}"
        echo ""

        metric_profile_status=$(echo ${status_json} | jq '.status')
        echo "create metric profile status = ${metric_profile_status}"
        if [ "${metric_profile_status}" != \"SUCCESS\" ]; then
                echo "Failed! Create metric profile failed. Status - ${metric_profile_status}"
                exit 1
        fi
}

#
# This function will be used to test bulk API for ROS use case
# "isROSEnabled" flag is turned on for RM.
# Adds `metadataProfileFilePath` and `metricProfileFilePath` under kruizeconfigjson to mount the respective file paths
#
function kruize_local_ros_patch() {
	CRC_DIR="./manifests/crc/default-db-included-installation"
	KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT="${CRC_DIR}/openshift/kruize-crc-openshift.yaml"
	KRUIZE_CRC_DEPLOY_MANIFEST_MINIKUBE="${CRC_DIR}/minikube/kruize-crc-minikube.yaml"

	if [ ${cluster_type} == "minikube" ]; then
      		if grep -q '"isROSEnabled": "false"' ${KRUIZE_CRC_DEPLOY_MANIFEST_MINIKUBE}; then
      		  	echo "Setting flag 'isROSEnabled' to 'true'"
        		sed -i 's/"isROSEnabled": "false"/"isROSEnabled": "true"/' ${KRUIZE_CRC_DEPLOY_MANIFEST_MINIKUBE}

        		# Use awk to find the 'kruizeconfigjson' block and insert 'metricProfileFilePath' and 'metadataProfileFilePath' before "hibernate"
        		awk '
        		/kruizeconfigjson: \|/ {in_config=1}
        		in_config && /"hibernate":/ {
            			print "      \"metricProfileFilePath\": \"/home/autotune/app/manifests/autotune/performance-profiles/resource_optimization_local_monitoring.json\",";
            			print "      \"metadataProfileFilePath\": \"/home/autotune/app/manifests/autotune/metadata-profiles/bulk_cluster_metadata_local_monitoring.json\",";
            			print
            			next
        		}
        		{print}
        		' "${KRUIZE_CRC_DEPLOY_MANIFEST_MINIKUBE}" > temp.yaml && mv temp.yaml "${KRUIZE_CRC_DEPLOY_MANIFEST_MINIKUBE}"
      		fi
  	elif [ ${cluster_type} == "openshift" ]; then
  	      if grep -q '"isROSEnabled": "false"' ${KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT}; then
  	        	echo "Setting flag 'isROSEnabled' to 'true'"
            		sed -i 's/"isROSEnabled": "false"/"isROSEnabled": "true"/' ${KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT}

            		# Use awk to find the 'kruizeconfigjson' block and insert 'metricProfileFilePath' and 'metadataProfileFilePath' before "hibernate"
            		awk '
            		/kruizeconfigjson: \|/ {in_config=1}
            		in_config && /"hibernate":/ {
                		print "      \"metricProfileFilePath\": \"/home/autotune/app/manifests/autotune/performance-profiles/resource_optimization_local_monitoring.json\",";
                		print "      \"metadataProfileFilePath\": \"/home/autotune/app/manifests/autotune/metadata-profiles/bulk_cluster_metadata_local_monitoring.json\",";
                		print
                		next
            		}
            		{print}
            		' "${KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT}" > temp.yaml && mv temp.yaml "${KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT}"
          	fi
  	fi
}

#
# "isROSEnabled" flag is turned on for RM.
# Restores kruize default cpu/memory resources, PV storage for openshift
#
function kruize_remote_patch() {
	CRC_DIR="./manifests/crc/default-db-included-installation"
	KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT="${CRC_DIR}/openshift/kruize-crc-openshift.yaml"
	KRUIZE_CRC_DEPLOY_MANIFEST_MINIKUBE="${CRC_DIR}/minikube/kruize-crc-minikube.yaml"

	if [ ${cluster_type} == "minikube" ]; then
		sed -i -E 's/"isROSEnabled": "false",?\s*//g; s/"local": "true",?\s*//g'  ${KRUIZE_CRC_DEPLOY_MANIFEST_MINIKUBE}    #this will remove the entry and use default set by java i.e. isROSEnabled=true and local=false
	elif [ ${cluster_type} == "openshift" ]; then
		sed -i -E 's/"isROSEnabled": "false",?\s*//g; s/"local": "true",?\s*//g'  ${KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT}
		sed -i 's/\([[:space:]]*\)\(storage:\)[[:space:]]*[0-9]\+Mi/\1\2 1Gi/' ${KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT}
		sed -i 's/\([[:space:]]*\)\(memory:\)[[:space:]]*".*"/\1\2 "2Gi"/; s/\([[:space:]]*\)\(cpu:\)[[:space:]]*".*"/\1\2 "2"/' ${KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT}
	fi
}

#
# Modify "serviceName" and "namespace" datasource manifest fields based on input parameters
#
function kruize_local_datasource_manifest_patch() {
	CRC_DIR="./manifests/crc/default-db-included-installation"
	KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT="${CRC_DIR}/openshift/kruize-crc-openshift.yaml"
	KRUIZE_CRC_DEPLOY_MANIFEST_MINIKUBE="${CRC_DIR}/minikube/kruize-crc-minikube.yaml"

	if [ ${cluster_type} == "minikube" ]; then
		if [[ ! -z "${servicename}" &&  ! -z "${datasource_namespace}" ]]; then
			sed -i 's/"serviceName": "[^"]*"/"serviceName": "'${servicename}'"/' ${KRUIZE_CRC_DEPLOY_MANIFEST_MINIKUBE}
			sed -i 's/"namespace": "[^"]*"/"namespace": "'${datasource_namespace}'"/' ${KRUIZE_CRC_DEPLOY_MANIFEST_MINIKUBE}
			sed -i 's/"url": ".*"/"url": ""/' ${KRUIZE_CRC_DEPLOY_MANIFEST_MINIKUBE}
		fi
	elif [ ${cluster_type} == "openshift" ]; then
		if [[ ! -z "${servicename}" &&  ! -z "${datasource_namespace}" ]]; then
			sed -i 's/"serviceName": "[^"]*"/"serviceName": "'${servicename}'"/' ${KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT}
			sed -i 's/"namespace": "[^"]*"/"namespace": "'${datasource_namespace}'"/' ${KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT}
			sed -i 's/"url": ".*"/"url": ""/' ${KRUIZE_CRC_DEPLOY_MANIFEST_OPENSHIFT}
		fi
	fi
}
