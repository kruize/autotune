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
##### Tests for autotune config yaml #####
#

CURRENT_DIR="$(dirname "$(realpath "$0")")"

# Source the common functions scripts
. ${CURRENT_DIR}/da/constants/kruize_layer_constants.sh

# application autotune yaml tests
# output: Run the test cases for autotune config yaml
function kruize_layer_yaml_tests() {
	start_time=$(get_date)
	# create the result directory for given testsuite
	TEST_SUITE_DIR="${RESULTS}/${FUNCNAME}"
	mkdir ${TEST_SUITE_DIR}
	AUTOTUNE_SETUP_LOG="${TEST_SUITE_DIR}/setup.log"

	if [ ! -z "${testcase}" ]; then
		check_test_case kruize_layer
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
		testtorun=${kruize_layer_tests[@]}
	else
		testtorun=${testcase}
	fi

	# create autotune setup
	echo -n "Deploying autotune..."| tee -a ${LOG}
	setup >> ${AUTOTUNE_SETUP_LOG} 2>&1
	echo "done"| tee -a ${LOG}

	# perform the tests for autotuneconfig yamls
	run_test "${testtorun}" autotuneconfig ${yaml_path}

	end_time=$(get_date)
	elapsed_time=$(time_diff "${start_time}" "${end_time}")

	# Summary of the test suite
	testsuitesummary ${FUNCNAME} ${elapsed_time} ${FAILED_CASES}

	# Check if any test failed in the testsuite if so add the testsuite to FAILED_TEST_SUITE array
	if [ "${TESTS_FAILED}" -ne "0" ]; then
		FAILED_TEST_SUITE+=(${FUNCNAME})
	fi
}
