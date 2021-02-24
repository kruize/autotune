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
pushd ${CURRENT_DIR}/.. > /dev/null

TEST_DIR=${PWD}
pushd ${TEST_DIR}/..  > /dev/null

AUTOTUNE_REPO="${PWD}"
SETUP_LOG="${TEST_DIR}/setup.log"
# variables to keep track of overall tests performed
TOTAL_TESTS_FAILED=0
TOTAL_TESTS_PASSED=0
TOTAL_TEST_SUITES=0
TOTAL_TESTS=0

# variables to keep track of tests performed for each test suite
TESTS_FAILED=0
TESTS_PASSED=0
TESTS=0

TEST_SUITE_ARRAY=("app_autotune_yaml_tests" "autotune_config_yaml_tests" "basic_api_tests")
AUTOTUNE_IMAGE="kruize/autotune:test"
matched=0
setup=1

# checks if the previous command is executed successfully
# input:Return value of previous command
# output:Prompts the error message if the return value is not zero 
function err_exit() {
	err=$?
	if [ ${err} -ne 0 ]; then
		echo "$*"
		exit -1
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

# Set up the autotune 
function setup() {
	# remove the existing autotune objects
	autotune_cleanup ${cluster_type}
	
	# Wait for 5 seconds to terminate the autotune pod
	sleep 5
	
	# Check if jq is installed
	check_prereq
	
	# Deploy autotune 
	echo "Deploying autotune..."
	deploy_autotune  ${cluster_type} ${AUTOTUNE_DOCKER_IMAGE}
	echo "Deploying autotune...Done"
	
	case "${cluster_type}" in
		minikube)
			NAMESPACE="monitoring"
			;;
		openshift)
			NAMESPACE="openshift-monitoring"
			;;
	esac
}

# Deploy autotune
# input: cluster type , autotune image
# output: Deploy autotune based on the parameter passed
function deploy_autotune() {
	cluster_type=$1
	AUTOTUNE_IMAGE=$2
	pushd ${AUTOTUNE_REPO} > /dev/null
	echo "Deploying autotune"
	if [ -z "${AUTOTUNE_IMAGE}" ]; then
		cmd="./deploy.sh -c ${cluster_type}"
	else
		cmd="./deploy.sh -c ${cluster_type} -i ${AUTOTUNE_IMAGE}"
	fi	
	echo "CMD= ${cmd}"
	${cmd}
}

# Remove the autotune setup
# output: Remove all the autotune dependencies
function autotune_cleanup() {
	pushd ${AUTOTUNE_REPO} > /dev/null
	pushd autotune/ > /dev/null
	echo "${PWD}"
	cmd="./deploy.sh -c ${cluster_type} -t"
	echo "CMD= ${cmd}"
	${cmd}
	popd
}

# list of test cases supported 
# input: testsuite
# ouput: print the testcases supported for specified testsuite
function test_case_usage() {
	checkfor=$1
	typeset -n da_tests="${checkfor}_tests"
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
	FAILED_CASES=$2
	echo 
	echo "########### Results Summary of the test suite ${TEST_SUITE_NAME} ##########"
	echo "Number of tests performed ${TESTS}"
	echo "Number of tests passed ${TESTS_PASSED}"
	echo "Number of tests failed ${TESTS_FAILED}"
	echo ""
	if [ "${TESTS_FAILED}" -ne "0" ]; then
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
	set_app_folder ${app_name}
	echo
	echo "Starting ${app_name} jmeter workload..."
	# Invoke the jmeter load script
	${APP_REPO}/${APP_FOLDER}/scripts/${app_name}-load.sh -c ${cluster_type} -i ${num_instances} --iter=${MAX_LOOP} 
}

# Remove the application setup
# input: application name
# output: Remove the instances of specified application 
function app_cleanup() {
	app_name=$1
	set_app_folder ${app_name}
	echo
	echo "Removing ${app_name} app..."
	${APP_REPO}/${APP_FOLDER}/scripts/${app_name}-cleanup.sh ${cluster_type}
	echo "done"
}

# Deploy the specified number of instances of given application on a given cluster type
# input: application name, number of instances
# ouput: Deploy the benchmark application based on the input
function deploy_app() {
	APP_REPO=$1
	app_name=$2
	num_instances=$3
	
	set_app_folder ${app_name}	
	
	if [ ${num_instances} == 1 ]; then
		echo "Deploying ${num_instances} instance of ${app_name} app..."
	else
		echo "Deploying ${num_instances} instances of ${app_name} app..."
	fi

	# Invoke the deploy script from app benchmark
	if [ ${cluster_type} == "openshift" ]; then
		${APP_REPO}/${APP_FOLDER}/scripts/${app_name}-deploy-openshift.sh -s ${kurl} -i ${num_instances}  >> ${SETUP_LOG}
	else
		${APP_REPO}/${APP_FOLDER}/scripts/${app_name}-deploy-${cluster_type}.sh -i ${num_instances}  >> ${SETUP_LOG}
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
				error_message  ${failed} 
			else
				failed=1
				error_message  ${failed} 
			fi
		else
			echo "${object} object ${testcase} did not get created" | tee -a ${LOG}
			failed=1
			error_message ${failed}
		fi
	else
		if [ ! -z "${status}" ]; then
			echo "${object} object ${testcase} got created" | tee -a ${LOG}
			failed=1
			error_message ${failed} 
		else	
			echo "${object} object ${testcase} did not get created" | tee -a ${LOG}
			if grep -q "${expected_log_msg}" "kubectl.log" ; then
				failed=0
				error_message ${failed}  
			else
				failed=1
				error_message ${failed} 
			fi
		fi 
	fi
}

# run the specified testcase
# input: object(autotune/autotuneconfig), testcase and yaml
# output: run the testcase and display the summary of the testcase
function run_test_case() {
	LOG="${LOG_DIR}/${testcase}.log"
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
	echo "*******----------- Running test for ${testcase} ----------*******"| tee  ${LOG}
	
	# create autotune setup
	echo -n "Deploying autotune..."| tee  ${LOG}
	setup >> ${SETUP_LOG} 2>&1
	echo "done"| tee  ${LOG}
	
	# Apply the yaml file 
	if [ "${object}" == "autotuneconfig" ]; then
		kubectl_cmd="kubectl apply -f ${yaml}.yaml -n ${NAMESPACE}"
	else
		kubectl_cmd="kubectl apply -f ${yaml}.yaml" 
	fi
	echo "CMD=${kubectl_cmd}">>${LOG}
	kubectl_log_msg=$(${kubectl_cmd} 2>&1) 
	err_exit "Error: Issue in deploying ${object} object"
	echo "${kubectl_log_msg}" > kubectl.log
	echo "${kubectl_log_msg}" >> "${LOG}"
	
	# get the log of the autotune pod
	autotune_pod=$(kubectl get pod -n ${NAMESPACE} | grep autotune | cut -d " " -f1)
	pod_log_msg=$(kubectl logs ${autotune_pod} -n ${NAMESPACE})
	echo "${pod_log_msg}" >> "${AUTOTUNE_LOG}"
	
	# check if autotune/autotuneconfig object has got created
	if [ "${object}" == "autotuneconfig" ]; then
		status=$(kubectl get ${object} -n ${NAMESPACE} | grep "${testcase}" | cut -d " " -f1) >> ${SETUP_LOG} 2>&1
	else
		status=$(kubectl get ${object} | grep "${testcase}" | cut -d " " -f1) >> ${SETUP_LOG} 2>&1
	fi
	
	# check if the expected message is matching with the actual message
	validate_yaml
	
	rm kubectl.log
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
		for testcase in ${var[@]}
		do 
			yaml=${path}/${test}/${testcase}
			typeset -n autotune_object="${test}_autotune_objects[${testcase}]"
			typeset -n expected_log_msg="${test}_expected_log_msgs[${testcase}]"
			run_test_case ${object} ${testcase} ${yaml} 
			echo
		done
		echo ""
	done
	
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
		run_test_case ${object} ${testcase} ${yaml} 
		echo
	done
	echo ""
}

# Form the curl command based on the cluster type
function form_curl_cmd() {
	# Form the curl command based on the cluster type
	case $cluster_type in
	   openshift) ;;
	   minikube)
		AUTOTUNE_PORT=$(kubectl -n ${NAMESPACE} get svc autotune --no-headers -o=custom-columns=PORT:.spec.ports[*].nodePort)
		SERVER_IP=$(minikube ip)
		AUTOTUNE_URL="http://${SERVER_IP}";;
	   docker) ;;
	   *);;
	esac

	if [ $cluster_type == "openshift" ]; then
		curl_cmd="curl -s -H 'Accept: application/json' ${AUTOTUNE_URL}"
	else
		curl_cmd="curl -s -H 'Accept: application/json' ${AUTOTUNE_URL}:${AUTOTUNE_PORT}"
	fi

	echo "**** curl_cmd = ${curl_cmd}"
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


# Create the expected search space json
# Input: application name
function create_expected_searchspace_json() {
	count=0
	index=0
	app_name=$1
	file_name="${LOG_DIR}/expected_searchspace.json"

	# check if the application name is passed , if not the consider all the applications
	if [ -z "${app_name}" ]; then
		app_name=${app_pod_names}
	fi

	# count the number of application for which recommendations are required
	for app in ${app_name[@]}
	do
		((index++))
	done

	printf '[' > ${file_name}
	for app in ${app_name[@]}
	do
		autotune_json="${AUTOTUNE_JSONS_DIR}/${autotune_names[count]}.json"
		((index--))
		printf '\n  {\n  "application_name": "'${app}'",' >> ${file_name}
		printf '\n  "objective_function": '$(cat ${autotune_json} | jq '.spec.sla.objective_function')',' >> ${file_name}
		deploy=${deployments[count]}
		layer_name=${layer_configs[$deploy]}
		for layer in ${layer_name[@]}
		do
			((layercount++))
		done

		for layer in "${layer_name[@]}"
		do
			layer_json="${AUTOTUNE_CONFIG_JSONS_DIR}/${layer}.json"
			((layercount--))
			printf '\n  "tunables": [' >> ${file_name}
			length=$(cat ${layer_json} | jq .tunables | jq length) >> ${file_name}
			while [ "${length}" -ne 0 ]
			do
				((length--))
				printf '\n {\n\t\t"value_type": '$(cat ${layer_json} | jq .tunables[${length}].value_type)',' >> ${file_name}
				printf '\n\t\t"lower_bound": '$(cat ${layer_json} | jq .tunables[${length}].lower_bound)',' >> ${file_name}
				printf '\n\t\t"name": '$(cat ${layer_json} | jq .tunables[${length}].name)',' >> ${file_name}
				printf '\n\t\t"upper_bound": '$(cat ${layer_json} | jq .tunables[${length}].upper_bound)'' >> ${file_name}
				if [ "${length}" -ne 0 ]; then
					printf '\n\t }, \n' >> ${file_name}
				else
					printf '\n\t }\n], \n' >> ${file_name}
				fi
			done
		done
		printf '\n  "sla_class": '$(cat ${autotune_json} | jq '.spec.sla.sla_class')',' >> ${file_name}
		printf '\n  "direction": '$(cat ${autotune_json} | jq '.spec.sla.direction')'' >> ${file_name}
		if [ "${index}" -eq 0 ]; then
			printf '\n } \n' >> ${file_name}
		else
			printf '\n }, \n' >> ${file_name}
		fi
		((count++))
	done
	printf ']' >> ${file_name}
	echo "expected json"  | tee -a ${LOG}
	cat ${file_name}  | tee -a ${LOG}
	echo "" | tee -a ${LOG}
}


# Run the curl command passed and capture the json output in a file
# Input: curl command, json file name
function run_curl_cmd() {
	cmd=$1
	json_file=$2

	echo "***** Curl cmd = ${cmd} json file = ${json_file}"
	echo "Curl cmd=${cmd}" | tee -a ${LOG}
	${cmd} > ${json_file}
	echo "actual json" | tee -a ${LOG}
	cat ${json_file} | tee -a ${LOG}
	echo "" | tee -a ${LOG}
}

# Get the actual search space json
# Input: application name
function get_searchspace_json() {
	app_names=$1
	if [ -z "${app_names}" ]; then
		cmd="${curl_cmd}/searchSpace"
	else
		cmd="${curl_cmd}/searchSpace?application_name=${app_names}"
	fi

	json_file="${LOG_DIR}/actual_searchspace.json"
	run_curl_cmd "${cmd}" ${json_file}
}

# Tests the searchSpace Autotune API
# Input: application name
function searchspace_test() {
	app_name=$1
	test_name=$FUNCNAME

	if [ ! -z "${app_name}" ]; then
		test_name="searchspace_app_name_test"
	fi
	LOG_DIR="${TEST_SUITE_DIR}/${test_name}"

	# check if the directory exists
	if [ ! -d ${LOG_DIR} ]; then
		mkdir ${LOG_DIR}
	fi

	LOG="${LOG_DIR}/${test_name}.log"
	AUTOTUNE_LOG="${LOG_DIR}/${test_name}_autotune.log"

	autotune_pod=$(kubectl get pod -n ${NAMESPACE} | grep autotune | cut -d " " -f1)
	pod_log_msg=$(kubectl logs ${autotune_pod} -n ${NAMESPACE})
	echo "${pod_log_msg}" >> "${AUTOTUNE_LOG}"

	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" | tee  -a ${LOG}
	echo "                    Running Testcase ${test_name}" | tee  -a ${LOG}
	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" | tee -a ${LOG}

	if [ -z "${app_name}" ]; then
		get_searchspace_json
		create_expected_searchspace_json
	else
		get_searchspace_json ${app_name}
		create_expected_searchspace_json ${app_name}
	fi
	compare_json ${LOG_DIR}/actual_searchspace.json ${LOG_DIR}/expected_searchspace.json ${test_name}
	echo "--------------------------------------------------------------" | tee -a ${LOG}
}

# Create the expected listApplicationTunables json
# Input: application name, layer name
function create_expected_listapptunables_json() {
	count=0
	index=0
	app_name=$1
	layer_name=$2
	file_name="${LOG_DIR}/expected_listapptunables.json"

	# check if the application name is passed , if not the consider all the applications
	if [ -z "${app_name}" ]; then
		app_name=${app_pod_names}
	fi

	# count the number of application for which recommendations are required
	for app in ${app_name[@]}
	do
		((index++))
	done

	printf '[' > ${file_name}
	for app in ${app_name[@]}
	do
		autotune_json="${AUTOTUNE_JSONS_DIR}/${autotune_names[count]}.json"
		((index--))
		printf '{\n  "application_name": "'${app}'",' >> ${file_name}
		printf '\n  "objective_function": '$(cat ${autotune_json} | jq '.spec.sla.objective_function')',' >> ${file_name}
		printf '\n  "function_variables": [{' >> ${file_name}
		printf '\n      "value_type": '$(cat ${autotune_json} | jq '.spec.sla.function_variables[].value_type')','  >> ${file_name}
		printf '\n      "name": '$(cat ${autotune_json} | jq '.spec.sla.function_variables[].name')','  >> ${file_name}

		url=$(kubectl get svc -n ${NAMESPACE} | grep prometheus-k8s | awk {'print $3'})
		query_url="http://${url}:9090/api/v1/query?query="
		fn_query=$(cat ${autotune_json} | jq '.spec.sla.function_variables[].query')
		fn_query=$(echo "${fn_query}" | tr -d '"')
		printf '\n      "query_url": "'${query_url}''${fn_query}'"'  >> ${file_name}
		printf '\n }],'  >> ${file_name}

		# Expected layers
		deploy=${deployments[count]}
		if [ -z "${layer_name}" ]; then
			layer_name=${layer_configs[$deploy]}
		fi
		IFS=',' read -r -a layer_name <<<  ${layer_name}

		for layer in ${layer_name[@]}
		do
			((layercount++))
		done

		printf '\n  "layers": [' >> 	${file_name}
		for layer in "${layer_name[@]}"
		do
			layer_json="${AUTOTUNE_CONFIG_JSONS_DIR}/${layer}.json"
			((layercount--))
			printf '{\n         "layer_level": '$(cat ${layer_json} | jq .layer_level)','  >> ${file_name}
			# Expected tunables
			printf '\n         "tunables": [' >> ${file_name}
			length=$(cat ${layer_json} | jq .tunables | jq length) >> ${file_name}
			while [ "${length}" -ne 0 ]
			do
			((length--))
				printf '{\n\t\t"value_type": '$(cat ${layer_json} | jq .tunables[${length}].value_type)',' >> ${file_name}
				printf '\n\t\t"lower_bound": '$(cat ${layer_json} | jq .tunables[${length}].lower_bound)',' >> ${file_name}
				printf '\n\t\t"name": '$(cat ${layer_json} | jq .tunables[${length}].name)',\n' >> ${file_name}
				query=$(cat ${layer_json} |jq .tunables[${length}].queries.datasource[].query)
				query=$(echo ${query} | sed 's/","/,/g; s/^"\|"$//g')
				query=$(echo "${query/\$CONTAINER_LABEL$/${layer}}")
				query=$(echo "${query/\$POD_LABEL$/pod_name}")
				query=$(echo "${query/\$POD$/${app}}")
				echo '                "query_url": "'${query_url}''${query}'",'  >> ${file_name}
				printf '\t\t"upper_bound": '$(cat ${layer_json} | jq .tunables[${length}].upper_bound)'' >> ${file_name}
				if [ "${length}" -ne 0 ]; then
					printf '\n }, \n' >> ${file_name}
				else
					printf '\n }], \n' >> ${file_name}
				fi
			done
			printf '\n         "layer_name": '$(cat ${layer_json} | jq .layer_name)','  >> ${file_name}
			printf '\n' >> ${file_name}
			echo '         "layer_details": '$(cat ${layer_json} | jq .details)''  >> ${file_name}
			if [ "${layercount}" -eq 0 ]; then
				printf '} \n ],' >> ${file_name}
			else
				printf '}, \n' >> ${file_name}
			fi
		done

		printf '\n  "sla_class": '$(cat ${autotune_json} | jq '.spec.sla.sla_class')',' >> ${file_name}
		if [ "${index}" -eq 0 ]; then
			printf '\n  "direction": '$(cat ${autotune_json} | jq '.spec.sla.direction')'\n}]' >> ${file_name}
		else
			printf '\n  "direction": '$(cat ${autotune_json} | jq '.spec.sla.direction')'\n},\n' >> ${file_name}
		fi
		((count++))
	done
	echo "expected json" | tee -a ${LOG}
	cat ${file_name} | tee -a ${LOG}
	echo ""  | tee -a ${LOG}
}

# Get listAppTunables json
# Input: application name, layer name
function get_listapptunables_json() {
	app_name=$1
	layer_name=$2

	if [[ -z "${app_name}" && -z "${layer_name}" ]]; then
		cmd="${curl_cmd}/listAppTunables"
	elif [[ ! -z "${app_name}" && ! -z "${layer_name}" ]]; then
		cmd="${curl_cmd}/listAppTunables?application_name=${app_name}&layer_name=${layer_name}"
	elif [[ ! -z "${app_name}" && -z "${layer_name}" ]];then
		cmd="${curl_cmd}/listAppTunables?application_name=${app_name}"
	fi

	json_file="${LOG_DIR}/actual_listapptunables.json"
	run_curl_cmd "${cmd}" ${json_file}
}

# Test listAppTunables Autotune API
# Input: application name, layer name
function listapptunables_test() {
	app_name=$1
	layer_name=$2
	test_name=${FUNCNAME}

	if [[ -z ${app_name} && -z "${layer_name}" ]]; then
		test_name=${FUNCNAME}
	elif [[ ! -z "${app_name}" && -z "${layer_name}" ]]; then
		test_name="listapptunables_app_name_test"
	elif [[ ! -z "${app_name}" && ! -z "${layer_name}" ]]; then
		test_name="listapptunables_app_name_layer_name_test"
	fi

	LOG_DIR="${TEST_SUITE_DIR}/${test_name}"

	# check if the directory exists
	if [ ! -d ${LOG_DIR} ]; then
		mkdir ${LOG_DIR}
	fi

	LOG="${LOG_DIR}/${test_name}.log"
	AUTOTUNE_LOG="${LOG_DIR}/${test_name}_autotune.log"
	autotune_pod=$(kubectl get pod -n ${NAMESPACE} | grep autotune | cut -d " " -f1)
	pod_log_msg=$(kubectl logs ${autotune_pod} -n ${NAMESPACE})
	echo "${pod_log_msg}" >> "${AUTOTUNE_LOG}"

	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" | tee -a ${LOG}
	echo "                    Running Testcase ${FUNCNAME}" | tee -a ${LOG}
	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" | tee -a ${LOG}
	if [[ -z ${app_name} && -z "${layer_name}" ]]; then
		echo "*******----------- ${FUNCNAME} for all application----------*******" | tee -a ${LOG}
		get_listapptunables_json
		create_expected_listapptunables_json
	elif [[ ! -z ${app_name} && ! -z "${layer_name}" ]]; then
		echo "*******----------- ${FUNCNAME} for specific application and specific layer----------*******" | tee -a ${LOG}
		get_listapptunables_json ${app_name} ${layer_name}
		create_expected_listapptunables_json ${app_name} ${layer_name}
	elif [[ ! -z ${app_name} && -z "${layer_name}" ]]; then
		echo "*******----------- ${FUNCNAME} for specified application----------*******" | tee -a ${LOG}
		get_listapptunables_json ${app_name}
		create_expected_listapptunables_json ${app_name}
	fi

	compare_json ${LOG_DIR}/actual_listapptunables.json ${LOG_DIR}/expected_listapptunables.json ${test_name}
	echo "--------------------------------------------------------------" | tee -a ${LOG}
}

# Create expected listAutotuneTunables json
# Input: sla class, layer name
function create_expected_listautotunetunables_json() {
	sla_class=$1
	layer_name=$2
	file_name="${LOG_DIR}/expected_list_tunables.json"

	if [ -z "${sla_class}" ]; then
		sla_class=("response_time" "throughput" "resource_usage")
	fi

	if [ -z "${layer_name}" ]; then
		layer_name=("${autotune_config_names[@]}")
	fi

	count="${#layer_name[@]}"

	if [ -z "${sla_class}" ]; then
		sla_class=("response_time" "throughput" "resource_usage")
	fi

	printf '[' > 	${file_name}
	for layer in "${layer_name[@]}"
	do
		layer_json="${AUTOTUNE_CONFIG_JSONS_DIR}/${layer}.json"
		((count--))
		printf '{\n         "layer_level": '$(cat ${layer_json} | jq .layer_level)','  >> ${file_name}
		printf '\n         "tunables": [' >> ${file_name}
		length=$(cat ${layer_json} | jq .tunables | jq length) >> ${file_name}
		while [ "${length}" -ne 0 ]
		do
			((length--))
			sla_count=0
			sla=$(cat ${layer_json} | jq .tunables[${length}].sla_class[])
			readarray -t sla <<<  ${sla}
			for s in "${sla[@]}"
			do
				s=$(echo "${s}" | tr -d '"')
				if [[ "${sla_class[sla_count]}" == "${s}" ]]; then
					printf '{\n\t\t"value_type": '$(cat ${layer_json} | jq .tunables[${length}].value_type)',' >> ${file_name}
					printf '\n\t\t"lower_bound": '$(cat ${layer_json} | jq .tunables[${length}].lower_bound)',' >> ${file_name}
					printf '\n\t\t"name": '$(cat ${layer_json} | jq .tunables[${length}].name)',' >> ${file_name}
					printf '\n\t\t"upper_bound": '$(cat ${layer_json} | jq .tunables[${length}].upper_bound)'' >> ${file_name}
					if [ "${length}" -ne 0 ]; then
						printf '\n }, \n' >> ${file_name}
					else
						printf '\n }], \n' >> ${file_name}
					fi
				fi
			done
		done
		printf '\n         "layer_name": '$(cat ${layer_json} | jq .layer_name)','  >> 	${file_name}
		printf '\n' >> ${file_name}
		echo '         "layer_details": '$(cat ${layer_json} | jq .details)''  >> ${file_name}
		if [ "${count}" -eq 0 ]; then
			printf '}' >> ${file_name}
		else
			printf '}, \n' >> ${file_name}
		fi
	done

	printf ']\n' >> ${file_name}

	echo "expectd json" | tee -a ${LOG}
	cat ${file_name} | tee -a ${LOG}
	echo "" | tee -a ${LOG}
}

# Get listAutotuneTunables json
# Input: sla class, layer name
function get_list_autotune_tunables_json() {
	sla_class=$1
	layer_name=$2

	if [[ -z "${sla_class}" && -z "${layer_name}" ]]; then
		cmd="${curl_cmd}/listAutotuneTunables"
	elif [[ ! -z "${sla_class}" && -z "${layer_name}" ]]; then
		cmd="${curl_cmd}/listAutotuneTunables?sla_class=${sla_class}"
	elif [[ ! -z "${sla_class}" && ! -z "${layer_name}" ]]; then
		cmd="${curl_cmd}/listAutotuneTunables?sla_class=${sla_class}&layer_name=${layer_name}"
	fi

	json_file="${LOG_DIR}/actual_list_tunables.json"
	run_curl_cmd "${cmd}" ${json_file}
}

# Test listAutotuneTunables Autotune API
# Input: sla class, layer name
function list_autotune_tunables_test() {
	sla_class=$1
	layer_name=$2
	test_name=${FUNCNAME}

	if [[ -z "${sla_class}" && -z "${layer_name}" ]]; then
		test_name=${FUNCNAME}
	elif [[ ! -z "${sla_class}" && -z "${layer_name}" ]]; then
		test_name="list_autotune_tunables_sla_class_test"
	elif [[ ! -z "${sla_class}" && ! -z "${layer_name}" ]]; then
		test_name="list_autotune_tunables_sla_class_layer_name_test"
	fi

	LOG_DIR="${TEST_SUITE_DIR}/${test_name}"

	# check if the directory exists
	if [ ! -d ${LOG_DIR} ]; then
		mkdir ${LOG_DIR}
	fi

	LOG="${LOG_DIR}/${test_name}.log"
	AUTOTUNE_LOG="${LOG_DIR}/${test_name}_autotune.log"

	autotune_pod=$(kubectl get pod -n ${NAMESPACE} | grep autotune | cut -d " " -f1)
	pod_log_msg=$(kubectl logs ${autotune_pod} -n ${NAMESPACE})
	echo "${pod_log_msg}" >> "${AUTOTUNE_LOG}"
	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" | tee -a ${LOG}
	echo "                    Running Testcase ${test_name}" | tee -a ${LOG}
	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" | tee -a ${LOG}
	if [[ -z "${sla_class}" && -z "${layer_name}" ]]; then
		echo "*******----------- ${FUNCNAME} for all applications ----------*******" | tee -a ${LOG}
		get_list_autotune_tunables_json
		create_expected_listautotunetunables_json
	elif [[ ! -z "${sla_class}" && -z "${layer_name}" ]]; then
		echo "*******----------- ${FUNCNAME} for specified sla ----------*******" | tee -a ${LOG}
		get_list_autotune_tunables_json ${sla_class}
		create_expected_listautotunetunables_json ${sla_class}
	elif [[ ! -z "${sla_class}" && ! -z "${layer_name}" ]]; then
		echo "*******----------- ${FUNCNAME} for specified sla and specific layer ----------*******" | tee -a ${LOG}
		get_list_autotune_tunables_json ${sla_class} ${layer_name}
		create_expected_listautotunetunables_json ${sla_class} ${layer_name}
	fi
	compare_json ${LOG_DIR}/actual_list_tunables.json ${LOG_DIR}/expected_list_tunables.json ${test_name}
	echo "--------------------------------------------------------------" | tee -a ${LOG}
}

# Create the expected listAppLayer json
# Input: application name
function create_expected_listapplayer_json() {
	count=0
	index=0
	layercount=0
	app_name=$1
	file_name="${LOG_DIR}/expected_listapplayer.json"

	# check if the application name is passed , if not the consider all the applications
	if [ -z "${app_name}" ]; then
		app_name=${app_pod_names}
	fi

	# count the number of application for which recommendations are required
	for app in ${app_name[@]}
	do
		((index++))
	done

	printf '[' > ${file_name}
	for app in ${app_name[@]}
	do
		autotune_json="${AUTOTUNE_JSONS_DIR}/${autotune_names[count]}.json"
		((index--))
		printf '{\n    "application_name": "'${app}'",' >> ${file_name}
		# do comparision of actual and expected name
		objectve_function=$(cat ${autotune_json} | jq '.spec.sla.objective_function')
		printf '\n    "objective_function": '$(cat ${autotune_json} | jq '.spec.sla.objective_function')',' >> ${file_name}
		deploy=${deployments[count]}
		layer_names=${layer_configs[$deploy]}
		IFS=',' read -r -a layer_name <<<  ${layer_name}
		for layer in ${layer_names[@]}
		do
			((layercount++))
		done
		printf '\n     "layers": [' >> ${file_name}
		for layer in ${layer_names[@]}
		do
			layer_json="${AUTOTUNE_CONFIG_JSONS_DIR}/${layer}.json"
			((layercount--))
			printf '{\n         "layer_level": '$(cat ${layer_json} | jq .layer_level)',' >> ${file_name}
			printf '\n         "layer_name": '$(cat ${layer_json} | jq .layer_name)',' >> ${file_name}
			printf '\n' >> ${file_name}
			echo '         "layer_details": '$(cat ${layer_json} | jq .details)'' >> ${file_name}
			if [ "${layercount}" -eq 0 ]; then
				printf '     }],' >> ${file_name}
			else
				printf '     },\n' >> ${file_name}
			fi
		done
		printf '\n    "sla_class": '$(cat ${autotune_json} | jq '.spec.sla.sla_class')',' >> ${file_name}
		if [ "${index}" -eq 0 ]; then
			printf '\n    "direction": '$(cat ${autotune_json} | jq '.spec.sla.direction')'\n}' >> ${file_name}
		else
			printf '\n    "direction": '$(cat ${autotune_json} | jq '.spec.sla.direction')'\n},\n' >> ${file_name}
		fi
		((count++))
	done
	printf ']' >> ${file_name}
	echo "expected json" | tee -a ${LOG}
	cat ${file_name} | tee -a ${LOG}
	echo "" | tee -a ${LOG}
}

# Get the listAppLayer json
# Input: application name
function get_listapplayer_json() {
	app_names=$1
	if [ -z "${app_names}" ]; then
		cmd="${curl_cmd}/listAppLayers"
	else
		cmd="${curl_cmd}/listAppLayers?application_name=${app_names}"
	fi


	json_file="${LOG_DIR}/actual_listapplayer.json"
	run_curl_cmd "${cmd}" ${json_file}
}

# Test listAppLayer Autotune API
# input: application name
function listapplayer_test() {
	app_name=$1
	test_name=$FUNCNAME

	if [ ! -z "${app_name}" ]; then
		test_name="listapplayer_app_name_test"
	fi

	LOG_DIR="${TEST_SUITE_DIR}/${test_name}"

	# check if the directory exists
	if [ ! -d ${LOG_DIR} ]; then
		mkdir ${LOG_DIR}
	fi

	LOG="${LOG_DIR}/${test_name}.log"
	AUTOTUNE_LOG="${LOG_DIR}/${test_name}_autotune.log"

	autotune_pod=$(kubectl get pod -n ${NAMESPACE} | grep autotune | cut -d " " -f1)
	pod_log_msg=$(kubectl logs ${autotune_pod} -n ${NAMESPACE})
	echo "${pod_log_msg}" >> "${AUTOTUNE_LOG}"

	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" | tee  -a ${LOG}
	echo "                    Running Testcase ${test_name}" | tee  -a ${LOG}
	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" | tee -a ${LOG}

	if [ -z "${app_name}" ]; then
		get_listapplayer_json
		create_expected_listapplayer_json
	else
		get_listapplayer_json ${app_name}
		create_expected_listapplayer_json  ${app_name}
	fi
	compare_json ${LOG_DIR}/actual_listapplayer.json ${LOG_DIR}/expected_listapplayer.json ${test_name}
	echo "--------------------------------------------------------------" | tee -a ${LOG}
}

# Create the expected listapplication json
# Input: application name
function create_expected_listapplication_json() {
	count=0
	index=0
	app_name=$1
	file_name="${LOG_DIR}/expected_listapp.json"

	# check if the application name is passed , if not the consider all the applications
	if [ -z "${app_name}" ]; then
		app_name=${app_pod_names}
	fi

	# count the number of application for which recommendations are required
	for app in ${app_name[@]}
	do
		((index++))
	done

	printf '[' > ${file_name}
	for app in ${app_name[@]}
	do
		autotune_json="${AUTOTUNE_JSONS_DIR}/${autotune_names[count]}.json"
		((index--))
		printf '{\n  "application_name": "'${app}'",' >> ${file_name}
		printf '\n  "objective_function": '$(cat ${autotune_json} | jq '.spec.sla.objective_function')',' >> ${file_name}
		printf '\n  "sla_class": '$(cat ${autotune_json} | jq '.spec.sla.sla_class')',' >> ${file_name}
		if [ "${index}" -eq 0 ]; then
			printf '\n  "direction": '$(cat ${autotune_json} | jq '.spec.sla.direction')'\n}' >> ${file_name}
		else
			printf '\n  "direction": '$(cat ${autotune_json} | jq '.spec.sla.direction')'\n},\n' >> ${file_name}
		fi
		((count++))
	done
	printf ']' >> ${file_name}
	echo "expected json" | tee -a ${LOG}
	cat ${file_name} | tee -a ${LOG}
	echo "" | tee -a ${LOG}
}

# Get listApplication json
# Input: application name
function get_listapplication_json() {
	app_names=$1
	if [ -z "${app_names}" ]; then
		cmd="${curl_cmd}/listApplications"
	else
		cmd="${curl_cmd}/listApplications?application_name=${app_names}"
	fi

	json_file="${LOG_DIR}/actual_listapp.json"
	run_curl_cmd "${cmd}" ${json_file}
}

# Test lisApplications Autotune API
# input: application name
function listapplications_test() {
	app_name=$1
	test_name=$FUNCNAME

	if [ ! -z "${app_name}" ]; then
		test_name="listapplications_app_name_test"
	fi

	LOG_DIR="${TEST_SUITE_DIR}/${test_name}"

	# check if the directory exists
	if [ ! -d ${LOG_DIR} ]; then
		mkdir ${LOG_DIR}
	fi

	LOG="${LOG_DIR}/${test_name}.log"
	AUTOTUNE_LOG="${LOG_DIR}/${test_name}_autotune.log"
	autotune_pod=$(kubectl get pod -n ${NAMESPACE} | grep autotune | cut -d " " -f1)
	pod_log_msg=$(kubectl logs ${autotune_pod} -n ${NAMESPACE})
	echo "${pod_log_msg}" >> "${AUTOTUNE_LOG}"

	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" | tee -a ${LOG}
	echo "                    Running Testcase ${test_name}" | tee -a ${LOG}
	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" | tee -a ${LOG}
	if [ -z "${app_name}" ]; then
		echo "*******----------- ${test_name} for all application----------*******" | tee -a ${LOG}
		get_listapplication_json
		create_expected_listapplication_json
	else
		echo "*******----------- ${test_name} for specified application - ${app_name}----------*******" | tee -a ${LOG}
		get_listapplication_json ${app_name}
		create_expected_listapplication_json ${app_name}
	fi
	compare_json ${LOG_DIR}/actual_listapp.json ${LOG_DIR}/expected_listapp.json ${test_name}
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
		echo "Adding label $label to pod $app..."
		# change the label of the pod based on number of instances created
		kubectl label pod ${app} app.kubernetes.io/name=${label}  --overwrite=true
		((inst++))
	done
	echo ""
}

function get_autotune_jsons() {
	AUTOTUNE_JSONS_DIR=$1
	shift
	YAML_PATH=$1
	shift
	autotune_names=("$@")

	echo ""
	echo "AUTOTUNE_JSONS_DIR = ${AUTOTUNE_JSONS_DIR}"
	echo "YAML_PATH = ${YAML_PATH}"

	# Create autotune object
	for autotune in "${autotune_names[@]}"
	do
		echo "Applying autotune yaml $autotune..."
		kubectl apply -f ${YAML_PATH}/${autotune}.yaml >> setup.log
		kubectl get autotune/${autotune} -o json > ${AUTOTUNE_JSONS_DIR}/${autotune}.json

		if [ -z "${AUTOTUNE_JSONS_DIR}/${autotune}.json" ]; then
			echo "Fetching the autotune json for ${autotune} object failed!"
			exit -1
		fi
	done
	echo ""
}

function get_autotune_config_jsons() {
	AUTOTUNE_CONFIG_JSONS_DIR=$1
	shift
	autotune_config_names=("$@")

	echo "AUTOTUNE CONFIG JSONS DIR = ${AUTOTUNE_CONFIG_JSONS_DIR}"
	# Create autotuneconfig object
	for autotuneconfig in "${autotune_config_names[@]}"
	do
		kubectl get autotuneconfig/${autotuneconfig} -o json -n ${NAMESPACE}  > ${AUTOTUNE_CONFIG_JSONS_DIR}/${autotuneconfig}.json
		if [ -z "${AUTOTUNE_CONFIG_JSONS_DIR}/${autotuneconfig}.json" ]; then
			echo "Fetching the autotune config json for ${autotuneconfig} object failed!"
			exit -1
		fi
	done
	echo ""
}
