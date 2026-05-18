#!/bin/bash
#
# Copyright (c) 2026 Red Hat, IBM Corporation and others.
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
##### Script to perform recommendation API tests #####


# Get the absolute path of current directory
RECOMMENDATION_TEST_DIR="${KRUIZE_REPO}/tests/scripts/recommendation_tests"
PERF_PROFILE_DIR="${KRUIZE_REPO}/manifests/autotune/performance-profiles"

# Source the common functions scripts
. "${KRUIZE_REPO}"/tests/scripts/common/common_functions.sh

# Test descriptions
declare -A recommendation_test_description
recommendation_test_description["remote"]="remote monitoring tests for recommendation API v1.0"
recommendation_test_description["local"]="local monitoring tests for recommendation API v1.0"

# Tests to validate Recommendation APIs in Kruize (applicable to both local and remote monitoring)
function recommendation_tests() {
	start_time=$(get_date)
	FAILED_CASES=()
	TESTS_FAILED=0
	TESTS_PASSED=0
	TESTS=0
	failed=0
	((TOTAL_TEST_SUITES++))

	python3 --version >/dev/null 2>/dev/null
	err_exit "ERROR: python3 not installed"

	target="crc"
	perf_profile_json="${PERF_PROFILE_DIR}/resource_optimization_openshift.json"

	recommendation_tests=("remote" "local")

	# check if the test case is supported
	if [ ! -z "${testcase}" ]; then
		check_test_case "recommendation"
	fi
	
	# create the result directory for given testsuite
	echo ""
	TEST_SUITE_DIR="${RESULTS}/recommendation_tests"
	Kruize_SETUP_LOG="${TEST_SUITE_DIR}/Kruize_setup.log"
	Kruize_POD_LOG="${TEST_SUITE_DIR}/Kruize_pod.log"

	mkdir -p "${TEST_SUITE_DIR}"

	# If testcase is not specified run all tests	
	if [ -z "${testcase}" ]; then
		test_to_run=("${recommendation_tests[@]}")
	else
		test_to_run=("${testcase}")
	fi
	
	# create the result directory for given testsuite
	echo ""
	mkdir -p "${TEST_SUITE_DIR}"

	PIP_INSTALL_LOG="${TEST_SUITE_DIR}/pip_install.log"

	echo ""
	echo "Installing the required python modules..."
	echo "python3 -m pip install --user -r "${RECOMMENDATION_TEST_DIR}/requirements.txt" > ${PIP_INSTALL_LOG}"
	python3 -m pip install --user -r "${RECOMMENDATION_TEST_DIR}/requirements.txt" > ${PIP_INSTALL_LOG} 2>&1
	err_exit "ERROR: Installing python modules for the test run failed!"

	echo ""
	echo "******************* Executing test suite ${FUNCNAME} ****************"
	echo ""
	
	for test in "${test_to_run[@]}"
	do
		TEST_DIR="${TEST_SUITE_DIR}/${test}"
		mkdir "${TEST_DIR}"
		LOG="${TEST_DIR}/${test}.log"

    pushd "${KRUIZE_REPO}" > /dev/null || exit
      # Setup Kruize
	    if [ "${skip_setup}" -eq 0 ]; then
        if [ -n "${test}" ] && [ "${test}" == "remote" ]; then
            echo "Applying remote monitoring patch (local=false) for remote tests"
            kruize_remote_patch
            echo "Remote monitoring patch applied...done"
          else
            echo "Using default local monitoring configuration (local=true)"
            # check for 'servicename' and 'datasource_namespace' input variables
            kruize_local_datasource_manifest_patch
            # increase cpu/memory resources, PV storage for openshift
            kruize_local_patch
        fi

        setup "${Kruize_POD_LOG}" >> "${Kruize_SETUP_LOG}" 2>&1
        echo "Setting up Kruize...Done" | tee -a "${LOG}"
        sleep 60

        if [ "${test}" == "remote" ]; then
          # create performance profile
          create_performance_profile "${perf_profile_json}"
        fi
      else
        echo "Skipping Kruize setup..." | tee -a ${LOG}
      fi
		popd > /dev/null || exit

		echo ""
		echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" | tee -a "${LOG}"
		echo "                    Running Test ${test}" | tee -a ${LOG}
		echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"| tee -a "${LOG}"

		echo " " | tee -a ${LOG}
		echo "Test description: ${recommendation_test_description[$test]}" | tee -a "${LOG}"
		echo " " | tee -a "${LOG}"

		pushd ${RECOMMENDATION_TEST_DIR}/rest_apis > /dev/null || exit
			echo "pytest -m ${test} --junitxml=${TEST_DIR}/report-${test}.xml --html=${TEST_DIR}/report-${test}.html --cluster_type ${cluster_type}"
			pytest -m "${test}" --junitxml=${TEST_DIR}/report-${test}.xml --html=${TEST_DIR}/report-${test}.html --cluster_type ${cluster_type} | tee -a "${LOG}"
			err_exit "ERROR: Running the test using pytest failed, check ${LOG} for details!"
    popd > /dev/null || exit

		passed=$(grep -o -E '[0-9]+ passed' ${TEST_DIR}/report-${test}.html | cut -d' ' -f1)
		failed=$(grep -o -E 'check the boxes to filter the results.*' ${TEST_DIR}/report-${test}.html | grep -o -E '[0-9]+ failed' | cut -d' ' -f1)
		errors=$(grep -o -E '[0-9]+ errors' ${TEST_DIR}/report-${test}.html | cut -d' ' -f1)

		# Set default values if grep returns empty
		passed=${passed:-0}
		failed=${failed:-0}
		errors=${errors:-0}

		TESTS_PASSED=$((TESTS_PASSED + passed))
		TESTS_FAILED=$((TESTS_FAILED + failed))

		if [ "${errors}" -ne "0" ]; then
			echo "Tests did not execute there were errors, check the logs"
			exit 1
		fi

		if [ "${TESTS_FAILED}" -ne "0" ]; then
			FAILED_CASES+=(${test})
		fi

		# Reverse patches applied for this test to ensure next test starts with clean state
		if [ "${skip_setup}" -eq 0 ]; then
			pushd "${KRUIZE_REPO}" > /dev/null || exit
				if [ -n "${test}" ] && [ "${test}" == "remote2" ]; then
					echo "Reversing remote monitoring patch for test ${test}"
					kruize_remote_patch_reverse
					echo "Remote monitoring patch reversed...done"
				else
					echo "Reversing local monitoring patches for test ${test}"
					# Reverse datasource manifest patch if it was applied
					kruize_local_datasource_manifest_patch_reverse
					# Reverse local patch (cpu/memory/storage changes)
					kruize_local_patch_reverse
					echo "Local monitoring patches reversed...done"
				fi
			popd > /dev/null || exit
		fi

	done

	TESTS=$((TESTS_PASSED + TESTS_FAILED))
	TOTAL_TESTS_FAILED=${TESTS_FAILED}
	TOTAL_TESTS_PASSED=${TESTS_PASSED}
	TOTAL_TESTS=${TESTS}

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
