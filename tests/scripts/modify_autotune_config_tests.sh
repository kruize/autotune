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
##### modify autotune config tests #####
#

CURRENT_DIR="$(dirname "$(realpath "$0")")"
SCRIPTS_DIR="${CURRENT_DIR}" 

# Modify the existing autotuneconfig and check for API results
function modify_autotune_config_tests() {
	start_time=$(get_date)
	FAILED_CASES=()
	TESTS_FAILED=0
	TESTS_PASSED=0
	TESTS=0

	if [ ! -z "${testcase}" ]; then
		check_test_case modify_autotune_config
	fi
	
	TESTS_DIR="${RESULTS}/${FUNCNAME}"
	AUTOTUNE_CONFIG_JSONS_DIR="${TESTS_DIR}/autotuneconfig_jsons"
	mkdir -p ${AUTOTUNE_CONFIG_JSONS_DIR}
	AUTOTUNE_SETUP_LOG="${TESTS_DIR}/setup.log"
	AUTOTUNE_LOG="${TESTS_DIR}/${FUNCNAME}_autotune.log"
	YAML="${api_yaml_path}/${FUNCNAME}"
	((TOTAL_TEST_SUITES++))
	
	echo " "
	echo "******************* Executing test suite ${FUNCNAME} ****************"
	echo "" 

	# Set up the autotune
	echo -n "Deploying autotune..." | tee -a ${LOG}
	setup >> ${AUTOTUNE_SETUP_LOG} 2>&1
	echo "done" | tee -a ${LOG}

	# Giving a sleep for autotune pod to be up and running
	sleep 10
	
	# get autotune pod log
	autotune_pod=$(kubectl get pod -n monitoring | grep autotune | cut -d " " -f1)
	pod_log_msg=$(kubectl logs ${autotune_pod} -n monitoring)
	echo "${pod_log_msg}" >> "${AUTOTUNE_LOG}"
	
	# form the curl command based on the cluster type
	form_curl_cmd
	
	# If testcase is not specified run all tests	
	if [ -z "${testcase}" ]; then
		testtorun=("${modify_autotune_config_tests[@]}")
	else
		testtorun=${testcase}
	fi
	
	for test in "${testtorun[@]}"
	do
		${test}	
	done
	
	if [ "${TESTS_FAILED}" -ne "0" ]; then
		FAILED_TEST_SUITE+=(${FUNCNAME})
	fi 
	
	# Cleanup autotune
	autotune_cleanup  | tee -a ${LOG}
	
	end_time=$(get_date)
	elapsed_time=$(time_diff "${start_time}" "${end_time}")
	
	FAILED=()
	for fail in ${FAILED_CASES[@]}
	do
		if [[ "${fail}" != list* ]]; then 
			FAILED+=(${fail})		
		fi
	done
	FAILED_CASES=("${FAILED[@]}")
	
	# print the testsuite summary
	testsuitesummary ${FUNCNAME} ${elapsed_time} ${FAILED_CASES}
}

# Perform the tests for Modify existing autotuneconfig 
# input: Test name
# ouput: create the required setup and perform the test based on test name
function perform_test() {
	api_test_name=$1
	
	TEST_SUITE_DIR="${TESTS_DIR}/${api_test_name}"
	mkdir ${TEST_SUITE_DIR}
	LOG="${TESTS_DIR}/${api_test_name}.log"
	
	test=$(echo "${api_test_name}" | tr '_' '-')
	
	echo ""
	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" | tee -a ${LOG}
	echo "                    Running Test ${api_test_name}" | tee -a ${LOG}
	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"| tee -a ${LOG}
	kubectl apply -f ${YAML}/${test}.yaml -n ${NAMESPACE} >> ${LOG}
	
	# Get the autotune config names 
	autotune_config_names=$(kubectl get autotuneconfig -n ${NAMESPACE} --no-headers=true | cut -d " " -f1 | tr "\n" " ")
	IFS=' ' read -r -a autotune_config_names <<<  ${autotune_config_names}
	
	# Get the autotune config jsons
	get_autotune_config_jsons ${AUTOTUNE_CONFIG_JSONS_DIR} ${autotune_config_names[@]}
	
	validate_list_autotune_tunables > >(tee "${LOG}") 2>&1
	echo " " | tee -a ${LOG}
	
	if [ "${TESTS_FAILED}" -ne "0" ]; then
		FAILED_CASES+=(${api_test_name})
	fi
}

# Test to add new tunable to existing autotuneconfig
function add_new_tunable() {
	perform_test ${FUNCNAME} 
}

# Test to replace tunable name by null value to exiting autotuneconfig
function apply_null_tunable() {
	perform_test ${FUNCNAME}
}

# Test to remove tunable from exiting autotuneconfig and check for API result
function remove_tunable() {
	perform_test ${FUNCNAME}
}

# Test to change tunable bound of exiting autotuneconfig and check for API result
function change_bound() {
	perform_test ${FUNCNAME}
}

# Test to add multiple tunables to exiting autotuneconfig and check for API result
function multiple_tunables() {
	perform_test ${FUNCNAME}
}

# Validate listAutotuneTunabels API 
function validate_list_autotune_tunables() {
	# test listAutotuneTunables API for specific sla_class and layer
	sla_class="response_time"
	layer="container"
	list_autotune_tunables_test ${sla_class} ${layer}

	# test listAutotuneTunables API for specific sla_class
	list_autotune_tunables_test ${sla_class} 
	
	# test listautotunetunables API for all layers
	list_autotune_tunables_test
}
