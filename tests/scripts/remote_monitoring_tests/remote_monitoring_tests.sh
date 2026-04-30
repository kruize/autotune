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
##### Script to perform remote monitoring tests #####


# Get the absolute path of current directory
REMOTE_MONITORING_TEST_DIR="${KRUIZE_REPO}/tests/scripts/remote_monitoring_tests"
PERF_PROFILE_DIR="${KRUIZE_REPO}/manifests/autotune/performance-profiles"

# Source the common functions scripts
#. ${KRUIZE_REPO}/tests/scripts/common/common_functions.sh

# Tests to validate Remote monitoring mode in Kruize 
function remote_monitoring_tests() {
	start_time=$(get_date)
	FAILED_CASES=()
	TESTS_FAILED=0
	TESTS_PASSED=0
	TESTS=0
	failed=0
	marker_options=""
	((TOTAL_TEST_SUITES++))

	python3 --version >/dev/null 2>/dev/null
	err_exit "ERROR: python3 not installed"

	target="crc"
	perf_profile_json="${PERF_PROFILE_DIR}/resource_optimization_openshift.json"
	perf_profile_json_v1="${REMOTE_MONITORING_TEST_DIR}/json_files/resource_optimization_openshift_v1.json"

	remote_monitoring_tests=("test_e2e" "perf_profile" "sanity" "negative" "extended" "simulate_prod")

	# check if the test case is supported
	if [ ! -z "${testcase}" ]; then
		check_test_case "remote_monitoring"
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
		echo "${KRUIZE_SETUP_LOG}"
		echo "Removing isROSEnabled=false and local=true"
		pwd
		pushd "${KRUIZE_REPO}" > /dev/null
			kruize_remote_patch
			echo "Removing isROSEnabled=false and local=true...done"
		popd > /dev/null
	else
		echo "Skipping kruize setup..." | tee -a ${LOG}
	fi

	# If testcase is not specified run all tests	
	if [ -z "${testcase}" ]; then
		testtorun=("${remote_monitoring_tests[@]}")
	else
		testtorun=${testcase}
	fi
	
	# create the result directory for given testsuite
	echo ""
	mkdir -p ${TEST_SUITE_DIR}

	PIP_INSTALL_LOG="${TEST_SUITE_DIR}/pip_install.log"

	echo ""
	echo "Installing the required python modules..."
	echo "python3 -m pip install --user -r "${REMOTE_MONITORING_TEST_DIR}/requirements.txt" > ${PIP_INSTALL_LOG}"
	python3 -m pip install --user -r "${REMOTE_MONITORING_TEST_DIR}/requirements.txt" > ${PIP_INSTALL_LOG} 2>&1
	err_exit "ERROR: Installing python modules for the test run failed!"

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
	
		pushd "${KRUIZE_REPO}" > /dev/null
			TEMP_KRUIZE_YAML=""
			ORIGINAL_KRUIZE_YAML_BACKUP=""
			if [ ${skip_setup} -eq 0 ]; then
				setup "${KRUIZE_POD_LOG}" >> ${KRUIZE_SETUP_LOG} 2>&1
				echo "Setting up kruize...Done" | tee -a ${LOG}

				sleep 60
				# Scale up Kruize deployment to 3 replicas for simulate_prod test
				if [ "${test}" == "simulate_prod" ]; then
					# Determine namespace based on cluster type
					if [ "${cluster_type}" == "openshift" ]; then
						KRUIZE_NAMESPACE="openshift-tuning"
					else
						KRUIZE_NAMESPACE="monitoring"
					fi

					# create performance profile v1
          create_performance_profile "${perf_profile_json_v1}"

					echo "Scaling Kruize deployment to 3 replicas for production-like setup in namespace ${KRUIZE_NAMESPACE}..." | tee -a ${LOG}
					kubectl scale deployment kruize --replicas=3 -n ${KRUIZE_NAMESPACE} 2>&1 | tee -a ${LOG}
					err_exit "ERROR: Failed to scale Kruize deployment to 3 replicas"

					# Wait for all 3 pods to be ready
					echo "Waiting for all 3 Kruize pods to be ready..." | tee -a ${LOG}
					kubectl wait --for=condition=ready pod -l app=kruize -n ${KRUIZE_NAMESPACE} --timeout=300s 2>&1 | tee -a ${LOG}
					err_exit "ERROR: Kruize pods did not become ready in time"

					# Verify 3 pods are running
					KRUIZE_POD_COUNT=$(kubectl get pods -n ${KRUIZE_NAMESPACE} -l app=kruize --no-headers 2>/dev/null | wc -l | tr -d ' ')
					if [ "${KRUIZE_POD_COUNT}" -ne "3" ]; then
						echo "ERROR: Expected 3 Kruize pods but found ${KRUIZE_POD_COUNT}" | tee -a ${LOG}
						kubectl get pods -n ${KRUIZE_NAMESPACE} -l app=kruize 2>&1 | tee -a ${LOG}
						exit 1
					fi
					echo "✓ Successfully scaled Kruize deployment to 3 replicas" | tee -a ${LOG}
					kubectl get pods -n ${KRUIZE_NAMESPACE} -l app=kruize 2>&1 | tee -a ${LOG}
				fi

				# create performance profile(skip for simulate-prod test as it's called with older version)
				if [ "${test}" != "simulate_prod" ]; then
					create_performance_profile ${perf_profile_json}
				fi
			fi
		popd > /dev/null

		pushd ${REMOTE_MONITORING_TEST_DIR}/rest_apis > /dev/null
			echo "pytest -m ${test} --junitxml=${TEST_DIR}/report-${test}.xml --html=${TEST_DIR}/report-${test}.html --cluster_type ${cluster_type}"
			pytest -m ${test} --junitxml=${TEST_DIR}/report-${test}.xml --html=${TEST_DIR}/report-${test}.html --cluster_type ${cluster_type} | tee -a ${LOG}
			err_exit "ERROR: Running the test using pytest failed, check ${LOG} for details!"

		popd > /dev/null

		pushd "${KRUIZE_REPO}" > /dev/null
			# Scale down Kruize deployment back to 1 replica after simulate_prod test
			if [ "${test}" == "simulate_prod" ]; then
				# Determine namespace based on cluster type
				if [ "${cluster_type}" == "openshift" ]; then
					KRUIZE_NAMESPACE="openshift-tuning"
				else
					KRUIZE_NAMESPACE="monitoring"
				fi
				
				echo "Scaling Kruize deployment back to 1 replica in namespace ${KRUIZE_NAMESPACE}..." | tee -a ${LOG}
				kubectl scale deployment kruize --replicas=1 -n ${KRUIZE_NAMESPACE} 2>&1 | tee -a ${LOG}
				echo "✓ Kruize deployment scaled back to 1 replica" | tee -a ${LOG}
			fi
		popd > /dev/null

		passed=$(grep -o -E '[0-9]+ passed' ${TEST_DIR}/report-${test}.html | cut -d' ' -f1)
		failed=$(grep -o -E 'check the boxes to filter the results.*' ${TEST_DIR}/report-${test}.html | grep -o -E '[0-9]+ failed' | cut -d' ' -f1)
		errors=$(grep -o -E '[0-9]+ errors' ${TEST_DIR}/report-${test}.html | cut -d' ' -f1)

		TESTS_PASSED=$(($TESTS_PASSED + $passed))
		TESTS_FAILED=$(($TESTS_FAILED + $failed))

		if [ "${errors}" -ne "0" ]; then
			echo "Tests did not execute there were errors, check the logs"
			exit 1
		fi

		if [ "${TESTS_FAILED}" -ne "0" ]; then
			FAILED_CASES+=(${test})
		fi

	done

	TESTS=$(($TESTS_PASSED + $TESTS_FAILED))
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

