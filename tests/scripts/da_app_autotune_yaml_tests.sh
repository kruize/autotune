#!/bin/bash
#
# Copyright (c) 2020, 2021 RedHat, IBM Corporation and others.
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
##### Tests for application autotune yaml #####
#

CURRENT_DIR="$(dirname "$(realpath "$0")")"
SCRIPTS_DIR="${CURRENT_DIR}"

# Source the common functions scripts
. ${SCRIPTS_DIR}/app_autotune_yaml_constants.sh

# application autotune yaml tests
# output: Run the test cases for application autotune yaml
function app_autotune_yaml_tests() {
	# create the result directory for given testsuite
	TEST_SUITE_DIR="${RESULTS}/${FUNCNAME}"
	mkdir ${TEST_SUITE_DIR}
	
	if [ ! -z "${testcase}" ]; then
		check_test_case app_autotune
	fi
	
	echo ""
	((TOTAL_TEST_SUITES++))
	FAILED_CASES=()
	TESTS_FAILED=0
	TESTS_PASSED=0
	TESTS=0
	echo ""
	echo "******************* Executing test suite ${FUNCNAME} ****************"
	echo ""
	
	if [ -z "${testcase}" ]; then
		testtorun=${app_autotune_tests[@]}
	else
		testtorun=${testcase}
	fi
	
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
			yaml=${MANIFESTS}/${module}/${App_Autotune_yaml}/${test}/${testcase}
			typeset -n autotune_object="${test}_autotune_objects[${testcase}]"
			typeset -n log_msg="${test}_expected_log_msgs[${testcase}]"
			run_test_case autotune ${testcase} ${yaml} 
			echo
		done
		echo ""
	done
	
	testcase=incomplete-autotune
	test=incomplete_yaml 
	yaml=${MANIFESTS}/${module}/${App_Autotune_yaml}/${testcase}
	typeset -n autotune_object="${test}_autotune_objects[${testcase}]"
	typeset -n log_msg="${test}_expected_log_msgs[${testcase}]"
	run_test_case autotune ${testcase} ${yaml} 
	
	# Summary of the test suite
	testsuitesummary ${FUNCNAME} ${FAILED_CASES} 
	
	# Check if any test failed in the testsuite if so add the testsuite to FAILED_TEST_SUITE array
	if [ "${TESTS_FAILED}" -ne "0" ]; then
		FAILED_TEST_SUITE+=(${FUNCNAME})
	fi
} 

