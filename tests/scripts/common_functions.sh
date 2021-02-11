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
pushd ${CURRENT_DIR}/.. >> setup.log

TEST_DIR=${PWD}
pushd ${TEST_DIR}/..  >> setup.log

AUTOTUNE_REPO="${PWD}"

# variables to keep track of overall tests performed
TOTAL_TESTS_FAILED=0
TOTAL_TESTS_PASSED=0
TOTAL_TEST_SUITES=0
TOTAL_TESTS=0

# variables to keep track of tests performed for each test suite
TESTS_FAILED=0
TESTS_PASSED=0
TESTS=0

case "${cluster_type}" in
	minikube)
		NAMESPACE="monitoring"
		;;
	openshift)
		NAMESPACE="openshift-monitoring"
		;;
esac

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
	./deploy.sh -t
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
	if [ "${testcase_matched}" -eq "0" ]; then
		echo ""
		echo "Error: Invalid testcase **${testcase}** "
		test_case_usage ${checkfor}
		exit -1
	fi
}

# Check the status of autotune pod
# input: cluster type
# output: check if the autotune operator got deployed
function check_autotune_operator() {
	sleep 20
	case ${cluster_type} in 
		docker)
			status=$(docker ps | grep "autotune" | cut -d " " -f1)
			;;
		minikube)
			status=$(kubectl get pod -n monitoring | grep "Running" | cut -d " " -f1 | grep autotune)  
			;;
		openshift)
			status=$(oc get pod -n ${NAMESPACE} | grep "Running" | cut -d " " -f1 | grep autotune)
			;;
	esac
	
	if [ -z "${status}" ]; then
		echo "Autotune deployed successfully"
		return 0
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
	${APP_REPO}/${APP_FOLDER}/scripts/${app_name}-cleanup.sh -c ${cluster_type}
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
		${APP_REPO}/${APP_FOLDER}/scripts/${app_name}-deploy-openshift.sh -s ${kurl} -i ${num_instances}  >> setup.log
	else
		${APP_REPO}/${APP_FOLDER}/scripts/${app_name}-deploy-${cluster_type}.sh -i ${num_instances}  >> setup.log
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
	
	# create autotune setup
	setup >> setup.log 2>&1
	
	# Run the test	
	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">> ${LOG}
	echo "                    Running Testcase ${test}">> ${LOG}
	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~">> ${LOG}
	echo "*******----------- Running test for ${testcase} ----------*******"| tee  ${LOG}
	
	# Apply the yaml file 
	if [ "${object}" == "autotuneconfig" ]; then
		kubectl_cmd="kubectl apply -f ${yaml}.yaml -n monitoring"
	else
		kubectl_cmd="kubectl apply -f ${yaml}.yaml" 
	fi
	echo "CMD=${kubectl_cmd}">>${LOG}
	kubectl_log_msg=$(${kubectl_cmd} 2>&1) 
	echo "${kubectl_log_msg}" > kubectl.log
	echo "${kubectl_log_msg}" >> "${LOG}"
	
	# get the log of the autotune pod
	autotune_pod=$(kubectl get pod -n monitoring | grep autotune | cut -d " " -f1)
	pod_log_msg=$(kubectl logs ${autotune_pod} -n monitoring)
	echo "${pod_log_msg}" >> "${AUTOTUNE_LOG}"
	
	# check if autotune/autotuneconfig object has got created
	if [ "${object}" == "autotuneconfig" ]; then
		status=$(kubectl get ${object} -n monitoring | grep "${testcase}" | cut -d " " -f1) >> setup.log 2>&1
	else
		status=$(kubectl get ${object} | grep "${testcase}" | cut -d " " -f1) >> setup.log 2>&1
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
	testcase=$1
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
