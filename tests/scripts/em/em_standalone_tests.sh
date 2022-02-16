#!/bin/bash
#
# Copyright (c) 2022, 2022 Red Hat, IBM Corporation and others.
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
#
##### Script to perform basic tests for EM #####


# Get the absolute path of current directory
CURRENT_DIR="$(dirname "$(realpath "$0")")"
EM_DIR="${CURRENT_DIR}/em"

# Tests directory path
TEST_DIR_="${CURRENT_DIR}/.."

# Source the common functions scripts
. ${EM_DIR}/constants/em_standalone_constants.sh
. ${EM_DIR}/em_deployment_tests.sh
. ${EM_DIR}/../common/em_common_functions.sh

# Tests to validate Experiment Manager standalone 
function em_standalone_tests() {
	start_time=$(get_date)
	FAILED_CASES=()
	TESTS_FAILED=0
	TESTS_PASSED=0
	TESTS=0
	((TOTAL_TEST_SUITES++))


	em_standalone_tests=("validate_single_deployment" "validate_single_deployment_diff_configs" "validate_single_deployment_diff_configs_sequentially" "validate_single_deployment_same_config" "validate_single_deployment_nondefault_ns" "validate_incorrect_ns" "invalid_input_json" "validate_no_deployment_exp")
	
	# check if the test case is supported
	if [ ! -z "${testcase}" ]; then
		check_test_case "em_standalone"
	fi
	
	# create the result directory for given testsuite
	echo ""
	TEST_SUITE_DIR="${RESULTS}/em_standalone_tests"
	AUTOTUNE_SETUP_LOG="${TEST_SUITE_DIR}/setup.log"

	mkdir -p ${TEST_SUITE_DIR}

	# create the autotune setup
        echo "Setting up autotune..." | tee -a ${LOG}
        setup >> ${AUTOTUNE_SETUP_LOG} 2>&1
        echo "Setting up autotune...Done" | tee -a ${LOG}


	# If testcase is not specified run all tests	
	if [ -z "${testcase}" ]; then
		testtorun=("${em_standalone_tests[@]}")
	else
		testtorun=${testcase}
	fi
	
	# create the result directory for given testsuite
	echo ""
	TEST_SUITE_DIR="${RESULTS}/em_standalone_tests"
	mkdir -p ${TEST_SUITE_DIR}

	echo ""
	echo "******************* Executing test suite ${FUNCNAME} ****************"
	echo ""
	
	for test in "${testtorun[@]}"
	do
		TEST_DIR="${TEST_SUITE_DIR}/${test}"
		mkdir ${TEST_DIR}
		SETUP="${TEST_DIR}/setup.log"
		LOG="${TEST_SUITE_DIR}/${test}.log"

		echo ""
		echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" | tee -a ${LOG}
		echo "                    Running Test ${test}" | tee -a ${LOG}
		echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"| tee -a ${LOG}

		echo " " | tee -a ${LOG}
		echo "Test description: ${em_standalone_test_description[$test]}" | tee -a ${LOG}
		echo " " | tee -a ${LOG}
		
		${test}
	done

	if [ "${TESTS_FAILED}" -ne "0" ]; then
		FAILED_TEST_SUITE+=(${FUNCNAME})
	fi

	end_time=$(get_date)
	elapsed_time=$(time_diff "${start_time}" "${end_time}")

	# Remove the duplicates 
	FAILED_CASES=( $(printf '%s\n' "${FAILED_CASES[@]}" | uniq ) )

	# print the testsuite summary
	testsuitesummary ${FUNCNAME} ${elapsed_time} ${FAILED_CASES} 
}

