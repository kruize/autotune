#!/bin/bash
#
# Copyright (c) 2020, 2021 Red Hat, IBM Corporation and others.
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
AUTOTUNE_REPO="${CURRENT_DIR}/../.."

# variables to keep track of overall tests performed
TOTAL_TESTS_FAILED=0
TOTAL_TESTS_PASSED=0
TOTAL_TEST_SUITES=0
TOTAL_TESTS=0

# variables to keep track of tests performed for each test suite
TESTS_FAILED=0
TESTS_PASSED=0
TESTS=0

# By default do not start HPO service as it is started in the autotune docker image
HPO_SERVICE=0

TEST_MODULE_ARRAY=("da" "em")

TEST_SUITE_ARRAY=("app_autotune_yaml_tests"
"kruize_layer_yaml_tests"
"basic_api_tests"
"modify_kruize_layer_tests"
"sanity"
"configmap_yaml_tests"
"autotune_id_tests"
"kruize_layer_id_tests"
"em_standalone_tests"
"remote_monitoring_tests")

modify_kruize_layer_tests=("add_new_tunable"
"apply_null_tunable"
"remove_tunable"
"change_bound"
"multiple_tunables")

AUTOTUNE_IMAGE="kruize/autotune_operator:test"
total_time=0
matched=0
sanity=0
setup=1
skip_setup=0

cleanup_prometheus=0

target="autotune"

# Path to the directory containing yaml files
MANIFESTS="${AUTOTUNE_REPO}/tests/autotune_test_yamls/manifests"
api_yaml="api_test_yamls"
module="da"
api_yaml_path="${MANIFESTS}/${module}/${api_yaml}"

# Path to the directory containing yaml files
configmap="${AUTOTUNE_REPO}/manifests/autotune/configmaps"

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

function time_diff() {
	ssec=`date --utc --date "$1" +%s`
	esec=`date --utc --date "$2" +%s`

	diffsec=$(($esec-$ssec))
	echo $diffsec
}

# Update the config map yaml with specified field
# input: String to find and string to replace it with
function update_yaml() {
	find=$1
	replace=$2
	config_yaml=$3
	sed -i "s/${find}/${replace}/g" ${config_yaml}
}

# Set up the autotune
# input: configmap directory and flag which indicates whether or not to do the deployment status check. It has to be set to "1" in case of configmap yaml test
function setup() {
	AUTOTUNE_POD_LOG=$1
	CONFIGMAP_DIR=$2
	ignore_deployment_status_check=$3

	# remove the existing autotune objects
	autotune_cleanup ${TEST_SUITE_DIR}

	# Wait for 5 seconds to terminate the autotune pod
	sleep 5

	# Check if jq is installed
	check_prereq

	# Deploy autotune
	echo "Deploying autotune..."
	deploy_autotune  "${cluster_type}" "${AUTOTUNE_DOCKER_IMAGE}" "${CONFIGMAP_DIR}" "${AUTOTUNE_POD_LOG}"
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
	AUTOTUNE_IMAGE=$2
	CONFIGMAP_DIR=$3
	AUTOTUNE_POD_LOG=$4

	pushd ${AUTOTUNE_REPO} > /dev/null

	# Check if the cluster_type is minikube., if so deploy prometheus
	if [ "${cluster_type}" == "minikube" ]; then
		echo "Installing Prometheus on minikube" >>/dev/stderr
		setup_prometheus >> ${AUTOTUNE_SETUP_LOG} 2>&1
	fi

	echo "Deploying autotune $target"
	# if both autotune image and configmap is not passed then consider the test-configmap(which has logging level as debug)
	if [[ -z "${AUTOTUNE_IMAGE}" && -z "${CONFIGMAP_DIR}" ]]; then
		if [ ${target} == "autotune" ]; then
			cmd="./deploy.sh -c ${cluster_type} -d ${CONFIGMAP} -m ${target}"
		elif [ ${target} == "crc" ]; then
			cmd="./deploy.sh -c ${cluster_type} -m ${target} -b"
		fi
	# if both autotune image and configmap  is passed
	elif [[ ! -z "${AUTOTUNE_IMAGE}" && ! -z "${CONFIGMAP_DIR}" ]]; then
		if [ ${target} == "autotune" ]; then
			cmd="./deploy.sh -c ${cluster_type} -i ${AUTOTUNE_IMAGE} -d ${CONFIGMAP_DIR} -m ${target}"
		elif [ ${target} == "crc" ]; then
			cmd="./deploy.sh -c ${cluster_type} -i ${AUTOTUNE_IMAGE} -m ${target}"
		fi

	# autotune image is passed but configmap is not passed then consider the test-configmap(which has logging level as debug)
	elif [[ ! -z "${AUTOTUNE_IMAGE}" && -z "${CONFIGMAP_DIR}" ]]; then
		if [ ${target} == "autotune" ]; then
			cmd="./deploy.sh -c ${cluster_type} -i ${AUTOTUNE_IMAGE} -d ${CONFIGMAP} -m ${target}"
		elif [ ${target} == "crc" ]; then
			cmd="./deploy.sh -c ${cluster_type} -i ${AUTOTUNE_IMAGE} -m ${target}"
		fi

	fi
	echo "Kruize deploy command - ${cmd}"
	${cmd}

	status="$?"
	# Check if autotune is deployed.
	# Ignore the status check if ignore_deployment_status_check is set to "1".
	# In case of configmap yaml tests we need not check if autotune has deployed properly during the setup since it is done as part of the test.
	if [[ "${status}" -eq "1" && "${ignore_deployment_status_check}" != "1" ]]; then
		echo "Error deploying autotune" >>/dev/stderr
		echo "See ${AUTOTUNE_SETUP_LOG}" >>/dev/stderr
		exit -1
	fi

	sleep 30

	if [[ ${cluster_type} == "minikube" || ${cluster_type} == "openshift" ]]; then
		sleep 2
		echo "Capturing Autotune service log into ${AUTOTUNE_POD_LOG}"
		namespace="openshift-tuning"
		if [ ${cluster_type} == "minikube" ]; then
			namespace="monitoring"
		fi
		echo "Namespace = $namespace"
		if [ ${target} == "crc" ]; then
			service="kruize"
			autotune_pod=$(kubectl get pod -n ${namespace} | grep ${service} | grep -v kruize-ui | cut -d " " -f1)
			echo "autotune_pod = $autotune_pod"
			echo "kubectl -n ${namespace} logs -f ${autotune_pod} > "${AUTOTUNE_POD_LOG}" 2>&1 &"
			kubectl -n ${namespace} logs -f ${autotune_pod} > "${AUTOTUNE_POD_LOG}" 2>&1 &
		else
			service="autotune"
			autotune_pod=$(kubectl get pod -n ${namespace} | grep ${service} | cut -d " " -f1)
			echo "autotune_pod = $autotune_pod"
			echo "kubectl -n ${namespace} logs -f ${autotune_pod} -c autotune > "${AUTOTUNE_POD_LOG}" 2>&1 &"
			kubectl -n ${namespace} logs -f ${autotune_pod} -c autotune > "${AUTOTUNE_POD_LOG}" 2>&1 &
		fi
	fi

	popd > /dev/null
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
		AUTOTUNE_SETUP_LOG="${RESULTS_LOG}/autotune_setup.log"
		pushd ${AUTOTUNE_REPO} > /dev/null
	else
		AUTOTUNE_SETUP_LOG="autotune_setup.log"
		pushd ${AUTOTUNE_REPO}/autotune > /dev/null
	fi

	echo  "Removing Autotune dependencies..."
	cmd="./deploy.sh -c ${cluster_type} -m ${target} -t"
	echo "CMD = ${cmd}"
	${cmd} >> ${AUTOTUNE_SETUP_LOG} 2>&1
	# Remove the prometheus setup
	if [ "${cleanup_prometheus}" -eq "1" ]; then
		prometheus_cleanup
	fi
	popd > /dev/null
	echo "done"
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
		${APP_REPO}/${APP_FOLDER}/scripts/tfb-cleanup.sh -c ${cluster_type} >> ${AUTOTUNE_SETUP_LOG} 2>&1
	else
		${APP_REPO}/${APP_FOLDER}/scripts/${app_name}-cleanup.sh -c ${cluster_type} >> ${AUTOTUNE_SETUP_LOG} 2>&1
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
			${APP_REPO}/${APP_FOLDER}/scripts/tfb-deploy.sh --clustertype=${cluster_type} -s ${kurl} -i ${num_instances}  >> ${AUTOTUNE_SETUP_LOG} 2>&1
                else
			${APP_REPO}/${APP_FOLDER}/scripts/${app_name}-deploy-openshift.sh -s ${kurl} -i ${num_instances}  >> ${AUTOTUNE_SETUP_LOG} 2>&1
		fi
	else
		if [ ${app_name} == "tfb-qrh" ]; then
			${APP_REPO}/${APP_FOLDER}/scripts/tfb-deploy.sh --clustertype=${cluster_type} -s "localhost" -i ${num_instances}  >> ${AUTOTUNE_SETUP_LOG} 2>&1
		else
			${APP_REPO}/${APP_FOLDER}/scripts/${app_name}-deploy-${cluster_type}.sh -i ${num_instances}  >> ${AUTOTUNE_SETUP_LOG} 2>&1
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

# Check if the expected message is matching with the actual message
# output: Check if the expected message is present in the log . If so set failed value to 0 else set failed value to 1 and call the error_message function
function validate_yaml () {
	if [ "${autotune_object}" == "true" ]; then
		if [ ! -z "${status}" ]; then
			echo "${object} object ${testcase} got created" | tee -a ${LOG}
			if  grep -q "${expected_log_msg}" "${AUTOTUNE_LOG}" ; then
				failed=0
				error_message "${failed}"
			else
				failed=1
				error_message "${failed}"
			fi
		else
			echo "${object} object ${testcase} did not get created" | tee -a ${LOG}
			failed=1
			error_message "${failed}"
		fi
	else
		if [ ! -z "${status}" ]; then
			echo "${object} object ${testcase} got created" | tee -a ${LOG}
			failed=1
			error_message "${failed}"
		else
			echo "${object} object ${testcase} did not get created" | tee -a ${LOG}
			if grep -q "${expected_log_msg}" "${LOG_DIR}/kubectl.log" ; then
				failed=0
				error_message "${failed}"
			else
				failed=1
				error_message "${failed}"
			fi
		fi
	fi
}

# run the specified testcase
# input: object(autotune/autotuneconfig), testcase and yaml
# output: run the testcase and display the summary of the testcase
function run_test_case() {
	LOG="${LOG_DIR}/${testcase}.log"
	AUTOTUNE_POD_LOG="${TEST_SUITE_DIR}/autotune.log"
	AUTOTUNE_LOG="${LOG_DIR}/${testcase}-autotune.log"
	((TOTAL_TESTS++))
	((TESTS++))
	object=$1
	testcase=$2
	yaml=$3

	# Run the test
	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">> ${LOG}
	echo "                    Running Testcase ${test}">> ${LOG}
	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">> ${LOG}
	echo "*******----------- Running test for ${testcase} ----------*******"| tee -a ${LOG}

	# Apply the yaml file
	if [ "${object}" == "autotuneconfig" ]; then
		kubectl_cmd="kubectl apply -f ${yaml}.yaml -n ${NAMESPACE}"
	else
		kubectl_cmd="kubectl apply -f ${yaml}.yaml"
	fi

	echo "CMD=${kubectl_cmd}">>${LOG}

	# Expose prometheus as nodeport and get the url
	expose_prometheus

	# Replace PROMETHEUS_URL keyword by actual URL
	sed -i "s|PROMETHEUS_URL|${prometheus_url}|g" ${yaml}.yaml

	# Get autotune pod log
	get_autotune_pod_log ${AUTOTUNE_POD_LOG}

	# Get the length of the pod log before applying the yaml
	log_length_before_test=$(cat ${AUTOTUNE_POD_LOG} | wc -l)

	# Apply the yaml
	kubectl_log_msg=$(${kubectl_cmd} 2>&1)
	check_err "Error: Issue in deploying ${object} object"
	echo "${kubectl_log_msg}" > ${LOG_DIR}/kubectl.log
	echo "${kubectl_log_msg}" >> "${LOG}"

	sed -i "s|${prometheus_url}|PROMETHEUS_URL|g" ${yaml}.yaml

	# Wait for 0.2 seconds to get the complete autotune pod log
	sleep 0.2

	# Get autotune pod log
	get_autotune_pod_log ${AUTOTUNE_POD_LOG}

	# Extract the lines from the pod log after log_length_before_test
	extract_lines=`expr ${log_length_before_test} + 1`
	cat ${AUTOTUNE_POD_LOG} | tail -n +${extract_lines} > ${AUTOTUNE_LOG}

	echo ""
	echo "log_length_before_test ${log_length_before_test}"
	echo "extract_lines ${extract_lines}"
	echo ""

	# check if autotune/autotuneconfig object has got created
	if [ "${object}" == "autotuneconfig" ]; then
		status=$(kubectl get ${object} -n ${NAMESPACE} | grep "${testcase}" | cut -d " " -f1)
	else
		status=$(kubectl get ${object} | grep "${testcase}" | cut -d " " -f1)
	fi

	# check if the expected message is matching with the actual message
	validate_yaml

#	rm kubectl.log
	echo ""
	echo "--------------------------------------------------------------------------------"| tee -a ${LOG}
}

# Perform app_autotune/autotuneconfig yaml tests
# input: testcase, testobject(autotune/autotuneconfig), path to yaml directory
# output: Perform the tests for given test case
function run_test() {
	testtorun=$1
	object=$2
	path=$3

	for test in ${testtorun[@]}
	do
		LOG_DIR="${TEST_SUITE_DIR}/${test}"
		mkdir ${LOG_DIR}
		echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
		echo "                    Running Testcases for ${test}"
		echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
		typeset -n var="${test}_testcases"

		# check if is the sanity test , if so then perform only the sanity bucket list
		test_var=()
		if [ "${sanity}" -eq "1" ]; then
			for testcase in ${var[@]}
			do
				if [[ "${testcase}" == invalid* || "${testcase}" == valid* || "${testcase}" == blank* ]]; then
					test_var+=(${testcase})
				fi
			done
		else
			test_var+=(${var[@]})
		fi

		for testcase in ${test_var[@]}
		do
			yaml=${path}/${test}/${testcase}
			typeset -n autotune_object="${test}_autotune_objects[${testcase}]"
			typeset -n expected_log_msg="${test}_expected_log_msgs[${testcase}]"
			run_test_case "${object}" "${testcase}" "${yaml}"
			echo
		done
		echo ""
	done
	other_tests="${object}_other"
	# perform other test cases
	LOG_DIR="${TEST_SUITE_DIR}/${other_tests}"
	mkdir ${LOG_DIR}
	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
	echo "                    Running Testcases for ${other_tests}"
	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
	typeset -n var="${other_tests}_testcases"
	for testcase in ${var[@]}
	do
		yaml=${path}/${other_tests}/${testcase}
		typeset -n autotune_object="${other_tests}_autotune_objects[${testcase}]"
		typeset -n expected_log_msg="${other_tests}_expected_log_msgs[${testcase}]"
		run_test_case "${object}" "${testcase}" "${yaml}"
		echo
	done
	echo ""

	# Delete the prometheus service
	kubectl delete svc prometheus-test -n ${NAMESPACE}
}

# Form the curl command based on the cluster type
function form_curl_cmd() {
	# Form the curl command based on the cluster type
	service="autotune"
	if [ ${target} == "crc" ]; then
		service="kruize"
	fi
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

# Create the expected search space json
# Input: experiment name
function create_expected_searchspace_json() {
	app_name=$1
	exp_name=$2

	file_name="${LOG_DIR}/expected_searchspace.json"

	# check if the experiment name is passed, if not the consider all the experiments
	if [ -z "${exp_name}" ]; then
		exp_names=(${autotune_names[@]})
	else
		exp_names=(${exp_name})
	fi

	echo "exp name = ${exp_names[@]}"

	arr_size=${#exp_names[@]}

	printf '[' > ${file_name}
	for index in ${!exp_names[@]}
	do
		autotune_json="${AUTOTUNE_JSONS_DIR}/${exp_names[index]}.json"
		printf '\n  {\n  "experiment_name": "'${exp_names[index]}'",' >> ${file_name}
		printf '\n  "objective_function": '$(cat ${autotune_json} | jq '.spec.slo.objective_function')',' >> ${file_name}
		printf '\n  "experiment_id": '$(cat ${json_file} | jq 'sort_by(.experiment_name)' | jq '.['${index}'].experiment_id')',' >> ${file_name}
		hpo_algo_impl=$(cat ${autotune_json} | jq '.spec.slo.hpo_algo_impl')

		if [ ${hpo_algo_impl} == null ]; then
			hpo_algo_impl="optuna_tpe"
		fi

		printf '\n  "hpo_algo_impl":  "'${hpo_algo_impl}'",' >> ${file_name}

		# Pick the expected layers based on the application
		read -r -a layer_names<<<"${layer_configs[$app_name]}"

		layer_names=(${layer_names[@]})

		layercount=${#layer_names[@]}

		for layer in "${layer_names[@]}"
		do

			layer_json="${KRUIZE_LAYER_JSONS_DIR}/${layer}.json"

			((layercount--))
			printf '\n  "tunables": [' >> ${file_name}
			length=$(cat ${layer_json} | jq .tunables | jq length) >> ${file_name}
			while [ "${length}" -ne 0 ]
			do
				((length--))
				printf '\n {\n\t\t"value_type": '$(cat ${layer_json} | jq .tunables[${length}].value_type)',' >> ${file_name}
				printf '\n\t\t"name": '$(cat ${layer_json} | jq .tunables[${length}].name)',' >> ${file_name}

				name=$(cat ${layer_json} | jq .tunables[${length}].name)
				lower_bound=$(cat ${layer_json} | jq .tunables[${length}].lower_bound)

				if [[ "${layer}" == "container"  &&  "${name}" == "\"memoryRequest\"" ]]; then
					lower_bound=$(format_memory "${lower_bound}")
				fi

				printf '\n\t\t"lower_bound": '${lower_bound}',' >> ${file_name}

				printf '\n\t\t"step": '$(cat ${layer_json} | jq .tunables[${length}].step)',' >> ${file_name}


				upper_bound=$(cat ${layer_json} | jq .tunables[${length}].upper_bound)
				if [[ "${layer}" == "container" && "${name}" == "\"memoryRequest\"" ]]; then
					upper_bound=$(format_memory "${upper_bound}")
				fi

				printf '\n\t\t"upper_bound": '${upper_bound}'' >> ${file_name}

				if [ "${length}" -ne 0 ]; then
					printf '\n\t }, \n' >> ${file_name}
				else
					printf '\n\t }], \n' >> ${file_name}
				fi
			done
		done
		printf '\n  "direction": '$(cat ${autotune_json} | jq '.spec.slo.direction')'' >> ${file_name}
		if [ "${index}" -eq $((arr_size-1)) ]; then
			printf '\n } \n' >> ${file_name}
		else
			printf '\n }, \n' >> ${file_name}
		fi
	done
	printf ']' >> ${file_name}
	echo "expected json"  >> ${LOG}
	cat ${file_name}  >> ${LOG}
	echo "" >> ${LOG}
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

# Get the actual search space json
# Input: experiment name
function get_searchspace_json() {
	exp_name=$1
	if [ -z "${exp_name}" ]; then
		cmd="${curl_cmd}/searchSpace"
	else
		cmd="${curl_cmd}/searchSpace?experiment_name=${exp_name}"
	fi

	json_file="${LOG_DIR}/actual_searchspace.json"
	run_curl_cmd "${cmd}" "${json_file}"
}

# Tests the searchSpace Autotune API
# Input: application name, experiment name
function searchspace_test() {
	app_name=$1
	exp_name=$2
	test_name=$FUNCNAME

	if [ ! -z "${exp_name}" ]; then
		test_name="searchspace_exp_name_test"
	fi
	LOG_DIR="${TEST_SUITE_DIR}/${test_name}"

	# check if the directory exists
	if [ ! -d ${LOG_DIR} ]; then
		mkdir ${LOG_DIR}
	fi

	LOG="${LOG_DIR}/${test_name}.log"
	AUTOTUNE_LOG="${LOG_DIR}/${test_name}_autotune.log"

	# get autotune pod log
	get_autotune_pod_log "${AUTOTUNE_LOG}"

	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" | tee  -a ${LOG}
	echo "                    Running Testcase ${test_name}" | tee  -a ${LOG}
	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" | tee -a ${LOG}

	if [ -z "${exp_name}" ]; then
		get_searchspace_json
		create_expected_searchspace_json "${app_name}"
	else
		get_searchspace_json "${exp_name}"
		create_expected_searchspace_json "${app_name}" "${exp_name}"
	fi
	compare_json "${LOG_DIR}/actual_searchspace.json" "${LOG_DIR}/expected_searchspace.json" "${test_name}"
	echo "--------------------------------------------------------------" | tee -a ${LOG}
}

# Create the expected listStackTunables json
# Input: application name, experiment name, layer name
function create_expected_liststacktunables_json() {
	app_name=$1
	exp_name=$2
	layer_name=$3
	file_name="${LOG_DIR}/expected_liststacktunables.json"

	# check if the experiment name is passed, if not the consider all the experiments
	if [ -z "${exp_name}" ]; then
		exp_names=(${autotune_names[@]})
	else
		exp_names=(${exp_name})
	fi

	arr_size=${#exp_names[@]}

	echo "app name = $app_name"
	echo "exp name = ${exp_names[@]}"

	printf '[' > ${file_name}
	for index in ${!exp_names[@]}
	do
		autotune_json="${AUTOTUNE_JSONS_DIR}/${exp_names[index]}.json"
		printf '{\n    "experiment_name": "'${exp_names[index]}'",' >> ${file_name}
		printf '\n    "objective_function": '$(cat ${autotune_json} | jq '.spec.slo.objective_function')',' >> ${file_name}


		hpo_algo_impl=$(cat ${autotune_json} | jq '.spec.slo.hpo_algo_impl')

		if [ ${hpo_algo_impl} == null ]; then
			hpo_algo_impl="optuna_tpe"
		fi

		printf '\n    "hpo_algo_impl":  "'${hpo_algo_impl}'",' >> ${file_name}

		printf '\n    "deployment_name": "'${deployment_names[index]}'",' >> ${file_name}
		printf '\n    "namespace":  '$(cat ${autotune_json} | jq '.metadata.namespace')',' >> ${file_name}
		printf '\n    "function_variables": [' >> ${file_name}
		variables_count=$(cat ${autotune_json} | jq '.spec.slo.function_variables' | jq length)

		for ((i=0 ; i<variables_count ; i++))
		do
			printf '\n     {'  >> ${file_name}
			printf '\n         "value_type": '$(cat ${autotune_json} | jq '.spec.slo.function_variables['${i}'].value_type')','  >> ${file_name}
			printf '\n 	   "name": '$(cat ${autotune_json} | jq '.spec.slo.function_variables['${i}'].name')','  >> ${file_name}

			url=$(kubectl get svc -n ${NAMESPACE} | grep prometheus-k8s | awk {'print $3'})
			fn_query=$(cat ${autotune_json} | jq '.spec.slo.function_variables['${i}'].query')

			fn_query=$(echo "${fn_query}" | sed 's/^"\|"$//g')
			echo -e "\n          \"query_url\": \"${fn_query}\""  >> ${file_name}

			var_count=$((variables_count-1))
			if [[ ${i} == ${var_count} ]]; then
				printf '\n    }'  >> ${file_name}
			else
				printf '\n    },'  >> ${file_name}
			fi
		done
		printf '\n ],'  >> ${file_name}


		if [ -z "${layer_name}" ]; then
			# Pick the expected layers based on the application
			read -r -a layer_names <<< "${layer_configs[$app_name]}"
		else
			read -r -a layer_names <<<  "${layer_name}"
		fi

		layer_names=(${layer_names[@]})

		layercount=${#layer_names[@]}

		images_count=${#container_images[@]}
		printf '\n    "stacks": [{' >> ${file_name}
		for i in ${!container_images[@]}
		do

			printf '\n  "layers": [' >> 	${file_name}
			for layer in "${layer_names[@]}"
			do
				layer_json="${KRUIZE_LAYER_JSONS_DIR}/${layer}.json"
				((layercount--))
				printf '{\n         "layer_level": '$(cat ${layer_json} | jq .layer_level)','  >> ${file_name}
				layer_level=$(cat ${layer_json} | jq .layer_level)

				# Expected tunables
				printf '\n         "tunables": [' >> ${file_name}
				tunables_length=$(cat ${layer_json} | jq .tunables | jq length) >> ${file_name}
				while [ ${tunables_length} -ne 0 ]
				do
					((tunables_length--))
					printf '{\n\t\t"value_type": '$(cat ${layer_json} | jq .tunables[${tunables_length}].value_type)',' >> ${file_name}
					name=$(cat ${layer_json} | jq .tunables[${tunables_length}].name)
					printf '\n\t\t"name": '${name}',\n' >> ${file_name}

					lower_bound=$(cat ${layer_json} | jq .tunables[${tunables_length}].lower_bound)

					if [[ "${name}" == "\"memoryLimit\""  ||  "${name}" == "\"memoryRequest\"" ]]; then
						lower_bound=$(format_memory "${lower_bound}")
					fi
					printf '\t\t"lower_bound": '${lower_bound}',' >> ${file_name}

					printf '\n\t\t"step": '$(cat ${layer_json} | jq .tunables[${tunables_length}].step)',\n' >> ${file_name}
					query=$(cat ${layer_json} |jq .tunables[${tunables_length}].queries.datasource[].query)
					query=$(echo ${query} | sed 's/","/,/g; s/^"\|"$//g')
					query=$(echo "${query/\$CONTAINER_LABEL$/container}")
					query=$(echo "${query/\$POD_LABEL$/pod}")
					query=$(echo "${query/\$POD$/${app}}")
					echo '                "query_url": "'${query}'",'  >> ${file_name}

					upper_bound=$(cat ${layer_json} | jq .tunables[${tunables_length}].upper_bound)
					if [[ "${name}" == "\"memoryLimit\"" || "${name}" == "\"memoryRequest\"" ]]; then
						upper_bound=$(format_memory "${upper_bound}")
					fi
					printf '\t\t"upper_bound": '${upper_bound}'' >> ${file_name}
					if [ "${tunables_length}" -ne 0 ]; then
						printf '\n       }, \n' >> ${file_name}
					else
						printf '\n       }], \n' >> ${file_name}
					fi
				done
				printf '\n         "layer_id": '$(cat ${json_file} | jq 'sort_by(.experiment_name)' | jq '.['${index}'].stacks['${i}'].layers['${layer_level}'].layer_id')',' >> ${file_name}
				printf '\n         "layer_name": '$(cat ${layer_json} | jq .layer_name)','  >> ${file_name}
				printf '\n' >> ${file_name}
				echo '         "layer_details": '$(cat ${layer_json} | jq .details)''  >> ${file_name}
				if [ "${layercount}" -eq 0 ]; then
					printf '} \n  ],' >> ${file_name}
				else
					printf '}, \n' >> ${file_name}
				fi
			done
			printf '\n     "stack_name": "'${container_images[i]}'"' >> ${file_name}
			if [ "${i}" -lt $((images_count-1)) ]; then
				printf '     },' >> ${file_name}
			fi
		done
		printf '\n    }],' >> ${file_name}


		printf '\n  "slo_class": '$(cat ${autotune_json} | jq '.spec.slo.slo_class')',' >> ${file_name}
		printf '\n  "experiment_id": '$(cat ${json_file} | jq 'sort_by(.experiment_name)' | jq '.['${index}'].experiment_id')',' >> ${file_name}
		if [ "${index}" -eq $((arr_size-1)) ]; then
			printf '\n  "direction": '$(cat ${autotune_json} | jq '.spec.slo.direction')'\n}]' >> ${file_name}
		else
			printf '\n  "direction": '$(cat ${autotune_json} | jq '.spec.slo.direction')'\n},\n' >> ${file_name}
		fi
	done
	echo "expected json" >> ${LOG}
	cat ${file_name} >> ${LOG}
	echo ""  >> ${LOG}
}

# Get listStackTunables json
# Input: experiment name, layer name
function get_liststacktunables_json() {
	exp_name=$1
	layer_name=$2

	if [[ -z "${exp_name}" && -z "${layer_name}" ]]; then
		cmd="${curl_cmd}/listStackTunables"
	elif [[ ! -z "${exp_name}" && ! -z "${layer_name}" ]]; then
		cmd="${curl_cmd}/listStackTunables?experiment_name=${exp_name}&layer_name=${layer_name}"
	elif [[ ! -z "${exp_name}" && -z "${layer_name}" ]];then
		cmd="${curl_cmd}/listStackTunables?experiment_name=${exp_name}"
	fi

	json_file="${LOG_DIR}/actual_liststacktunables.json"
	run_curl_cmd "${cmd}" "${json_file}"
}

# Test listStackTunables Autotune API
# Input: application name, experiment name, layer name
function liststacktunables_test() {
	app_name=$1
	exp_name=$2
	layer_name=$3
	test_name=${FUNCNAME}

	if [[ -z ${exp_name} && -z "${layer_name}" ]]; then
		test_name=${FUNCNAME}
	elif [[ ! -z "${exp_name}" && -z "${layer_name}" ]]; then
		test_name="liststacktunables_exp_name_test"
	elif [[ ! -z "${exp_name}" && ! -z "${layer_name}" ]]; then
		test_name="liststacktunables_exp_name_layer_name_test"
	fi

	LOG_DIR="${TEST_SUITE_DIR}/${test_name}"

	# check if the directory exists
	if [ ! -d ${LOG_DIR} ]; then
		mkdir ${LOG_DIR}
	fi

	LOG="${LOG_DIR}/${test_name}.log"
	AUTOTUNE_LOG="${LOG_DIR}/${test_name}_autotune.log"

	# get autotune pod log
	get_autotune_pod_log "${AUTOTUNE_LOG}"

	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" | tee -a ${LOG}
	echo "                    Running Testcase ${FUNCNAME}" | tee -a ${LOG}
	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" | tee -a ${LOG}
	if [[ -z ${exp_name} && -z "${layer_name}" ]]; then
		echo "*******----------- ${FUNCNAME} for all applications----------*******" | tee -a ${LOG}
		get_liststacktunables_json
		create_expected_liststacktunables_json "${app_name}"
	elif [[ ! -z ${exp_name} && ! -z "${layer_name}" ]]; then
		echo "*******----------- ${FUNCNAME} for a specific application and specific layer----------*******" | tee -a ${LOG}
		get_liststacktunables_json "${exp_name}" "${layer_name}"
		create_expected_liststacktunables_json "${app_name}" "${exp_name}" "${layer_name}"
	elif [[ ! -z ${exp_name} && -z "${layer_name}" ]]; then
		echo "*******----------- ${FUNCNAME} for specified application----------*******" | tee -a ${LOG}
		get_liststacktunables_json "${exp_name}"
		create_expected_liststacktunables_json "${app_name}" "${exp_name}"
	fi

	compare_json "${LOG_DIR}/actual_liststacktunables.json" "${LOG_DIR}/expected_liststacktunables.json" "${test_name}"
	echo "--------------------------------------------------------------" | tee -a ${LOG}
}

# Create expected listKruizeTunables json
# Input: slo class, layer name
function create_expected_listkruizetunables_json() {
	slo_class=$1
	layer_name=$2
	file_name="${LOG_DIR}/expected_list_tunables.json"
	layer_count=0

	if [ -z "${slo_class}" ]; then
		slo_class=("response_time" "throughput" "resource_usage")
	fi

	if [ -z "${layer_name}" ]; then
		layer_name=("${kruize_layer_names[@]}")
	fi

	count="${#layer_name[@]}"

	if [ -z "${slo_class}" ]; then
		slo_class=("response_time" "throughput" "resource_usage")
	fi

	printf '[' > 	${file_name}

	for layer in "${layer_name[@]}"
	do
		layer_json="${KRUIZE_LAYER_JSONS_DIR}/${layer}.json"

		((count--))
		printf '{\n         "layer_level": '$(cat ${layer_json} | jq .layer_level)','  >> ${file_name}
		printf '\n         "tunables": [' >> ${file_name}
		length=$(cat ${layer_json} | jq .tunables | jq length) >> ${file_name}
		while [ "${length}" -ne 0 ]
		do
			((length--))
			slo_count=0
			slo=$(cat ${layer_json} | jq .tunables[${length}].slo_class[])
			readarray -t slo <<<  ${slo}
			for s in "${slo[@]}"
			do
				s=$(echo "${s}" | tr -d '"')
				if [[ "${slo_class[slo_count]}" == "${s}" ]]; then
					printf '{\n\t\t"value_type": '$(cat ${layer_json} | jq .tunables[${length}].value_type)',' >> ${file_name}
					name=$(cat ${layer_json} | jq .tunables[${length}].name)
					printf '\n\t\t"name": '${name}',\n' >> ${file_name}

					lower_bound=$(cat ${layer_json} | jq .tunables[${length}].lower_bound)

					if [[ "${name}" == "\"memoryLimit\""  ||  "${name}" == "\"memoryRequest\"" ]]; then
						lower_bound=$(format_memory "${lower_bound}")
					fi
					printf '\n\t\t"lower_bound": '${lower_bound}',' >> ${file_name}

					printf '\n\t\t"step": '$(cat ${layer_json} | jq .tunables[${length}].step)',' >> ${file_name}

					if [[ ${layer} == "container" || ${layer} == "hotspot" ]]; then
						url=$(kubectl get svc -n ${NAMESPACE} | grep prometheus-k8s | awk {'print $3'})
						query=$(cat ${layer_json} |jq .tunables[${length}].queries.datasource[].query)
						query=$(echo ${query} | sed 's/","/,/g; s/^"\|"$//g')
						query=$(echo "${query/\$CONTAINER_LABEL$/container}")
						query=$(echo "${query/\$POD_LABEL$/pod}")
						#query=$(echo "${query/\$POD$/${app}}")
						echo -e '\n                "query_url": "'${query}'",'  >> ${file_name}
					else
						printf '\n\t\t"query_url": "none",' >> ${file_name}

					fi

					upper_bound=$(cat ${layer_json} | jq .tunables[${length}].upper_bound)
					if [[ "${name}" == "\"memoryLimit\"" || "${name}" == "\"memoryRequest\"" ]]; then
						upper_bound=$(format_memory "${upper_bound}")
					fi
					printf '\n\t\t"upper_bound": '${upper_bound}'' >> ${file_name}

					if [ "${length}" -ne 0 ]; then
						printf '\n\t }, \n' >> ${file_name}
					else
						printf '\n\t }], \n' >> ${file_name}
					fi
				fi
			done
		done
		printf '\n         "layer_id": '$(cat ${json_file} | jq '.['${layer_count}'].layer_id')',' >> ${file_name}
		printf '\n         "layer_name": '$(cat ${layer_json} | jq .layer_name)','  >> 	${file_name}
		printf '\n' >> ${file_name}
		echo '         "layer_details": '$(cat ${layer_json} | jq .details)''  >> ${file_name}
		if [ "${count}" -eq 0 ]; then
			printf '}' >> ${file_name}
		else
			printf '}, \n' >> ${file_name}
		fi
		((layer_count++))
	done

	printf ']\n' >> ${file_name}

	echo "expectd json" >> ${LOG}
	cat ${file_name} >> ${LOG}
	echo "" >> ${LOG}
	layer_name=("")
}

# Get listKruizeTunables json
# Input: slo class, layer name
function get_list_kruize_tunables_json() {
	slo_class=$1
	layer_name=$2

	if [[ -z "${slo_class}" && -z "${layer_name}" ]]; then
		cmd="${curl_cmd}/listKruizeTunables"
	elif [[ ! -z "${slo_class}" && -z "${layer_name}" ]]; then
		cmd="${curl_cmd}/listKruizeTunables?slo_class=${slo_class}"
	elif [[ ! -z "${slo_class}" && ! -z "${layer_name}" ]]; then
		cmd="${curl_cmd}/listKruizeTunables?slo_class=${slo_class}&layer_name=${layer_name}"
	fi

	json_file="${LOG_DIR}/actual_list_tunables.json"
	run_curl_cmd "${cmd}" "${json_file}"
}

# Test listKruizeTunables Autotune API
# Input: slo class, layer name
function list_kruize_tunables_test() {
	slo_class=$1
	layer_name=$2
	test_name=${FUNCNAME}

	if [[ -z "${slo_class}" && -z "${layer_name}" ]]; then
		test_name=${FUNCNAME}
	elif [[ ! -z "${slo_class}" && -z "${layer_name}" ]]; then
		test_name="list_kruize_tunables_slo_class_test"
	elif [[ ! -z "${slo_class}" && ! -z "${layer_name}" ]]; then
		test_name="list_kruize_tunables_slo_class_layer_name_test"
	fi

	LOG_DIR="${TEST_SUITE_DIR}/${test_name}"

	# check if the directory exists
	if [ ! -d ${LOG_DIR} ]; then
		mkdir ${LOG_DIR}
	fi

	LOG="${LOG_DIR}/${test_name}.log"
	AUTOTUNE_LOG="${LOG_DIR}/${test_name}_autotune.log"

	# get autotune pod log
	get_autotune_pod_log "${AUTOTUNE_LOG}"

	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" | tee -a ${LOG}
	echo "                    Running Testcase ${test_name}" | tee -a ${LOG}
	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" | tee -a ${LOG}
	if [[ -z "${slo_class}" && -z "${layer_name}" ]]; then
		echo "*******----------- ${FUNCNAME} for all applications ----------*******" | tee -a ${LOG}
		get_list_kruize_tunables_json
		create_expected_listkruizetunables_json
	elif [[ ! -z "${slo_class}" && -z "${layer_name}" ]]; then
		echo "*******----------- ${FUNCNAME} for specified slo ----------*******" | tee -a ${LOG}
		get_list_kruize_tunables_json "${slo_class}"
		create_expected_listkruizetunables_json "${slo_class}"
	elif [[ ! -z "${slo_class}" && ! -z "${layer_name}" ]]; then
		echo "*******----------- ${FUNCNAME} for specified slo and specific layer ----------*******" | tee -a ${LOG}
		get_list_kruize_tunables_json "${slo_class}" "${layer_name}"
		create_expected_listkruizetunables_json "${slo_class}" "${layer_name}"
	fi
	compare_json "${LOG_DIR}/actual_list_tunables.json" "${LOG_DIR}/expected_list_tunables.json" "${test_name}"
	echo "--------------------------------------------------------------" | tee -a ${LOG}
}

# Create the expected listStackLayers json
# Input: application name, experiment name
function create_expected_liststacklayers_json() {
	layercount=0
	app_name=$1
	exp_name=$2

	file_name="${LOG_DIR}/expected_liststacklayers.json"

	# check if the experiment name is passed, if not the consider all the experiments
	if [ -z "${exp_name}" ]; then
		exp_names=(${autotune_names[@]})
	else
		exp_names=("${exp_name}")
	fi

	echo "app_name = $app_name"
	echo "exp names = ${exp_names[@]}"

	arr_size=${#exp_names[@]}

	printf '[' > ${file_name}
	for index in ${!exp_names[@]}
	do
		autotune_json="${AUTOTUNE_JSONS_DIR}/${exp_names[index]}.json"
		printf '{\n    "experiment_name": "'${exp_names[index]}'",' >> ${file_name}
		# do comparision of actual and expected name
		objectve_function=$(cat ${autotune_json} | jq '.spec.slo.objective_function')
		printf '\n    "objective_function": '$(cat ${autotune_json} | jq '.spec.slo.objective_function')',' >> ${file_name}

		hpo_algo_impl=$(cat ${autotune_json} | jq '.spec.slo.hpo_algo_impl')

		if [ ${hpo_algo_impl} == null ]; then
			hpo_algo_impl="optuna_tpe"
		fi

		printf '\n    "hpo_algo_impl":  "'${hpo_algo_impl}'",' >> ${file_name}
		printf '\n    "deployment_name":  "'${deployment_names[index]}'",' >> ${file_name}
		printf '\n    "namespace":  '$(cat ${autotune_json} | jq '.metadata.namespace')',' >> ${file_name}

		images_count=${#container_images[@]}
		printf '\n    "stacks": [{' >> ${file_name}
		for i in ${!container_images[@]}
		do

			# Pick the expected layers based on the application
			read -r -a layer_names<<<"${layer_configs[$app_name]}"

			layer_names=(${layer_names[@]})

			layercount=${#layer_names[@]}

			printf '\n\t     "layers": [' >> ${file_name}
			for layer in ${layer_names[@]}
			do
				layer_json="${KRUIZE_LAYER_JSONS_DIR}/${layer}.json"

				printf '{\n\t        "layer_level": '$(cat ${layer_json} | jq .layer_level)',' >> ${file_name}
				layer_level=$(cat ${layer_json} | jq .layer_level)

				printf '\n\t         "layer_id": '$(cat ${json_file} | jq 'sort_by(.experiment_name)' | jq '.['${index}'].stacks['${i}'].layers['${layer_level}'].layer_id')',' >> ${file_name}
				printf '\n\t         "layer_name": '$(cat ${layer_json} | jq .layer_name)',' >> ${file_name}
				printf '\n\t' >> ${file_name}
				echo '         "layer_details": '$(cat ${layer_json} | jq .details)'' >> ${file_name}
				((layercount--))
				if [ "${layercount}" -eq 0 ]; then
					printf '\t     }],' >> ${file_name}
				else
					printf '\t     },\n' >> ${file_name}
				fi
			done
			printf '\n     "stack_name": "'${container_images[i]}'"' >> ${file_name}
			if [ "${i}" -lt $((images_count-1)) ]; then
				printf '     },' >> ${file_name}
			fi
		done
		printf '\n    }],' >> ${file_name}

		printf '\n    "experiment_id": '$(cat ${json_file} | jq 'sort_by(.experiment_name)' | jq '.['${index}'].experiment_id')',' >> ${file_name}
		printf '\n    "slo_class": '$(cat ${autotune_json} | jq '.spec.slo.slo_class')',' >> ${file_name}
		if [ "${index}" -eq $((arr_size-1)) ]; then
			printf '\n    "direction": '$(cat ${autotune_json} | jq '.spec.slo.direction')'\n}' >> ${file_name}
		else
			printf '\n    "direction": '$(cat ${autotune_json} | jq '.spec.slo.direction')'\n},\n' >> ${file_name}
		fi
	done
	printf ']' >> ${file_name}
	echo "expected json" >> ${LOG}
	cat ${file_name} >> ${LOG}
	echo "" >> ${LOG}
}

# Get the listStackLayers json
# Input: experiment name
function get_liststacklayers_json() {
	exp_name=$1
	if [ -z "${exp_name}" ]; then
		cmd="${curl_cmd}/listStackLayers"
	else
		echo "experiment name = ${exp_name}"
		cmd="${curl_cmd}/listStackLayers?experiment_name=${exp_name}"
	fi


	json_file="${LOG_DIR}/actual_liststacklayers.json"
	run_curl_cmd "${cmd}" "${json_file}"
}

# Test listStackLayers Autotune API
# input: application name, experiment name
function liststacklayers_test() {
	app_name=$1
	exp_name=$2
	test_name=$FUNCNAME

	if [ ! -z "${exp_name}" ]; then
		test_name="liststacklayers_exp_name_test"
	fi

	LOG_DIR="${TEST_SUITE_DIR}/${test_name}"

	# check if the directory exists
	if [ ! -d ${LOG_DIR} ]; then
		mkdir ${LOG_DIR}
	fi

	LOG="${LOG_DIR}/${test_name}.log"
	AUTOTUNE_LOG="${LOG_DIR}/${test_name}_autotune.log"

	# get autotune pod log
	get_autotune_pod_log "${AUTOTUNE_LOG}"

	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" | tee  -a ${LOG}
	echo "                    Running Testcase ${test_name}" | tee  -a ${LOG}
	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" | tee -a ${LOG}

	if [ -z "${exp_name}" ]; then
		get_liststacklayers_json
		create_expected_liststacklayers_json "${app_name}"
	else
		get_liststacklayers_json "${exp_name}"
		create_expected_liststacklayers_json "${app_name}" "${exp_name}"
	fi
	compare_json "${LOG_DIR}/actual_liststacklayers.json" "${LOG_DIR}/expected_liststacklayers.json" "${test_name}"
	echo "--------------------------------------------------------------" | tee -a ${LOG}
}

# Create the expected listStacks json
# Input: experiment name
function create_expected_liststacks_json() {
	exp_name=$1
	file_name="${LOG_DIR}/expected_liststacks.json"

	# check if the experiment name is passed, if not the consider all the experiments
	if [ -z "${exp_name}" ]; then
		exp_names=(${autotune_names[@]})
	else
		exp_names=(${exp_name})
	fi

	echo "exp name = ${exp_names[@]}"

	arr_size=${#exp_names[@]}

	printf '[' > ${file_name}
	for index in ${!exp_names[@]}
	do
		autotune_json="${AUTOTUNE_JSONS_DIR}/${exp_names[index]}.json"
		printf '{\n  "experiment_name": "'${exp_names[index]}'",' >> ${file_name}
		printf '\n  "objective_function": '$(cat ${autotune_json} | jq '.spec.slo.objective_function')',' >> ${file_name}
		printf '\n  "deployment_name": "'${deployment_names[index]}'",' >> ${file_name}
		hpo_algo_impl=$(cat ${autotune_json} | jq '.spec.slo.hpo_algo_impl')

		if [ ${hpo_algo_impl} == null ]; then
			hpo_algo_impl="optuna_tpe"
		fi

		printf '\n  "hpo_algo_impl":  "'${hpo_algo_impl}'",' >> ${file_name}
		printf '\n  "namespace": '$(cat ${autotune_json} | jq '.metadata.namespace')',' >> ${file_name}
		printf '\n  "slo_class": '$(cat ${autotune_json} | jq '.spec.slo.slo_class')',' >> ${file_name}

		images_count=${#container_images[@]}
		printf '\n  "stacks": [' >> ${file_name}
		for i in ${!container_images[@]}
		do
			printf '\n\t\t "'${container_images[i]}'"' >> ${file_name}
			if [ "${i}" -lt $((images_count-1)) ]; then
				printf ',' >> ${file_name}
			fi
		done
		printf '\n            ],' >> ${file_name}
		printf '\n  "experiment_id": '$(cat ${json_file} | jq 'sort_by(.experiment_name)' | jq '.['${index}'].experiment_id')',' >> ${file_name}
		if [ "${index}" -eq $((arr_size-1)) ]; then
			printf '\n  "direction": '$(cat ${autotune_json} | jq '.spec.slo.direction')'\n}' >> ${file_name}
		else
			printf '\n  "direction": '$(cat ${autotune_json} | jq '.spec.slo.direction')'\n},\n' >> ${file_name}
		fi
	done
	printf ']' >> ${file_name}
	echo "expected json" >> ${LOG}
	cat ${file_name} >> ${LOG}
	echo "" >> ${LOG}
}

# Get listStacks json
# Input: experiment name
function get_liststacks_json() {
	exp_name=$1
	if [ -z "${exp_name}" ]; then
		cmd="${curl_cmd}/listStacks"
	else
		cmd="${curl_cmd}/listStacks?experiment_name=${exp_name}"
	fi

	json_file="${LOG_DIR}/actual_liststacks.json"
	run_curl_cmd "${cmd}" "${json_file}"
}

# Test listStacks Autotune API
# input: experiment name
function liststacks_test() {
	exp_name=$1
	test_name=$FUNCNAME

	if [ ! -z "${exp_name}" ]; then
		test_name="liststacks_exp_name_test"
	fi

	LOG_DIR="${TEST_SUITE_DIR}/${test_name}"

	# check if the directory exists
	if [ ! -d ${LOG_DIR} ]; then
		mkdir ${LOG_DIR}
	fi

	LOG="${LOG_DIR}/${test_name}.log"
	AUTOTUNE_LOG="${LOG_DIR}/${test_name}_autotune.log"

	# get autotune pod log
	get_autotune_pod_log "${AUTOTUNE_LOG}"

	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" | tee -a ${LOG}
	echo "                    Running Testcase ${test_name}" | tee -a ${LOG}
	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" | tee -a ${LOG}
	if [ -z "${exp_name}" ]; then
		echo "*******----------- ${test_name} for all experiments ----------*******" | tee -a ${LOG}
		get_liststacks_json
		create_expected_liststacks_json
	else
		echo "*******----------- ${test_name} for specified experiment - ${exp_name}----------*******" | tee -a ${LOG}
		get_liststacks_json "${exp_name}"
		create_expected_liststacks_json "${exp_name}"
	fi
	compare_json "${LOG_DIR}/actual_liststacks.json" "${LOG_DIR}/expected_liststacks.json" "${test_name}"
	echo "--------------------------------------------------------------" | tee -a ${LOG}
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
	kubectl apply -f ${yaml_dir} >> ${AUTOTUNE_SETUP_LOG}
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
	# Fetch the autotune container log
	container="autotune"

	echo "target = $target"
	if [ ${target} == "crc" ]; then
		autotune_pod=$(kubectl get pod -n ${NAMESPACE} | grep kruize | grep -v kruize-ui | cut -d " " -f1)
		pod_log_msg=$(kubectl logs ${autotune_pod} -n ${NAMESPACE})
	else
		autotune_pod=$(kubectl get pod -n ${NAMESPACE} | grep autotune | cut -d " " -f1)
		pod_log_msg=$(kubectl logs ${autotune_pod} -n ${NAMESPACE}  -c ${container})
	fi
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
