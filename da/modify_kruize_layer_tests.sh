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

# Modify the existing kruizelayer and check for API results
function modify_kruize_layer_tests() {
	start_time=$(get_date)
	FAILED_CASES=()
	TESTS_FAILED=0
	TESTS_PASSED=0
	TESTS=0

	if [ ! -z "${testcase}" ]; then
		check_test_case "modify_kruize_layer"
	fi

	TESTS_DIR="${RESULTS}/${FUNCNAME}"
	KRUIZE_LAYER_JSONS_DIR="${TESTS_DIR}/autotuneconfig_jsons"
	mkdir -p ${KRUIZE_LAYER_JSONS_DIR}
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
	container="autotune"
	autotune_pod=$(kubectl get pod -n ${NAMESPACE} | grep autotune | cut -d " " -f1)
	pod_log_msg=$(kubectl logs ${autotune_pod} -n ${NAMESPACE} -c ${container})
	echo "${pod_log_msg}" >> "${AUTOTUNE_LOG}"

	# form the curl command based on the cluster type
	form_curl_cmd

	# If testcase is not specified run all tests
	if [ -z "${testcase}" ]; then
		testtorun=("${modify_kruize_layer_tests[@]}")
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
	testsuitesummary "${FUNCNAME}" "${elapsed_time}" "${FAILED_CASES}"
}

# Perform the tests for Modify existing kruizelayer
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
	kruize_layer_names=$(kubectl get autotuneconfig -n ${NAMESPACE} --no-headers=true | cut -d " " -f1 | tr "\n" " ")
	IFS=' ' read -r -a kruize_layer_names <<<  ${kruize_layer_names}

	# Get the autotune config jsons
	get_kruize_layer_jsons "${KRUIZE_LAYER_JSONS_DIR}" "${kruize_layer_names[@]}"

	validate_list_kruize_tunables > >(tee "${LOG}") 2>&1
	echo " " | tee -a ${LOG}

	if [ "${TESTS_FAILED}" -ne "0" ]; then
		FAILED_CASES+=(${api_test_name})
	fi
}

# Test to add new tunable to existing kruizelayer
function add_new_tunable() {
	perform_test "${FUNCNAME}"
}

# Test to replace tunable name by null value to exiting kruizelayer
function apply_null_tunable() {
	perform_test "${FUNCNAME}"
}

# Test to remove tunable from exiting kruizelayer and check for API result
function remove_tunable() {
	perform_test "${FUNCNAME}"
}

# Test to change tunable bound of exiting kruizelayer and check for API result
function change_bound() {
	perform_test "${FUNCNAME}"
}

# Test to add multiple tunables to exiting kruizelayer and check for API result
function multiple_tunables() {
	perform_test "${FUNCNAME}"
}

# Validate listKruizeTunables API
function validate_list_kruize_tunables() {
	# test listKruizeTunables API for specific slo_class and layer
	slo_class="response_time"
	layer="container"
	list_kruize_tunables_test "${slo_class}" "${layer}"

	# test listKruizeTunables API for specific slo_class
	list_kruize_tunables_test "${slo_class}"

	# test listKruizeTunables API for all layers
	list_kruize_tunables_test
}
