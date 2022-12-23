#!/bin/bash
#
# Copyright (c) 2023, 2023 Red Hat, IBM Corporation and others.
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
REMOTE_MONITORING_TEST_DIR="${CURRENT_DIR}/remote_monitoring_tests"

# Source the common functions scripts
. ${REMOTE_MONITORING_TEST_DIR}/../common/common_functions.sh

# Tests to validate Remote monitoring mode in Kruize 
function remote_monitoring_tests() {
	start_time=$(get_date)
	FAILED_CASES=()
	TESTS_FAILED=0
	TESTS_PASSED=0
	TESTS=0
	failed=0
	((TOTAL_TEST_SUITES++))

	target="crc"
	perf_profile_json="${REMOTE_MONITORING_TEST_DIR}/json_files/resource_optimization_openshift.json"

	remote_monitoring_tests=("test_create_experiment" "test_update_results")
	#remote_monitoring_tests=("test_create_experiment")
	
	# check if the test case is supported
	if [ ! -z "${testcase}" ]; then
		check_test_case "remote_monitoring_tests"
	fi
	
	# create the result directory for given testsuite
	echo ""
	TEST_SUITE_DIR="${RESULTS}/remote_monitoring_tests"
	KRUIZE_SETUP_LOG="${TEST_SUITE_DIR}/kruize_setup.log"
	KRUIZE_POD_LOG="${TEST_SUITE_DIR}/kruize_pod.log"

	mkdir -p ${TEST_SUITE_DIR}

	# Setup kruize
	if [ ${skip_setup} -eq 0 ]; then
		echo "Setting up kruize..." | tee -a ${LOG}
		setup "${KRUIZE_POD_LOG}" >> ${KRUIZE_SETUP_LOG} 2>&1
	        echo "Setting up kruize...Done" | tee -a ${LOG}
	else
		echo "Skipping kruize setup..." | tee -a ${LOG}
	fi

	sleep 60
	# If testcase is not specified run all tests	
	if [ -z "${testcase}" ]; then
		testtorun=("${remote_monitoring_tests[@]}")
	else
		testtorun=${testcase}
	fi
	
	# create the result directory for given testsuite
	echo ""
	mkdir -p ${TEST_SUITE_DIR}

	# create performance profile
	create_performance_profile ${perf_profile_json}

	echo ""
	echo "******************* Executing test suite ${FUNCNAME} ****************"
	echo ""
	
	for test in "${testtorun[@]}"
	do
		TEST_DIR="${TEST_SUITE_DIR}/${test}"
		mkdir ${TEST_DIR}
		LOG="${TEST_DIR}/${test}.log"

		echo ""
		echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" | tee -a ${LOG}
		echo "                    Running Test ${test}" | tee -a ${LOG}
		echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"| tee -a ${LOG}

		echo " " | tee -a ${LOG}
		echo "Test description: ${remote_monitoring_test_description[$test]}" | tee -a ${LOG}
		echo " " | tee -a ${LOG}
	
		echo "pytest -s -m sanity ${REMOTE_MONITORING_TEST_DIR}/rest_apis/${test}.py --cluster_type ${cluster_type}" | tee -a ${LOG}
		pushd ${REMOTE_MONITORING_TEST_DIR}/rest_apis > /dev/null 
			pytest -s -m sanity --html=${TEST_DIR}/report.html ${test}.py --cluster_type ${cluster_type} | tee -a ${LOG}
		popd > /dev/null
		if  grep -q "AssertionError" "${LOG}" ; then
			failed=1
			failed_count=$(cat ${LOG} | grep AssertionError | wc -l)
			TESTS_FAILED=$(($TESTS_FAILED + $failed_count))
			((TOTAL_TESTS_FAILED++))
			FAILED_CASES+=(${test})
		else
			((TESTS_PASSED++))
			((TOTAL_TESTS_PASSED++))
		fi
		((TESTS++))
		((TOTAL_TESTS++))

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

