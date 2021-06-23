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
##### Tests for configmap yaml#####
#

CURRENT_DIR="$(dirname "$(realpath "$0")")"
SCRIPTS_DIR="${CURRENT_DIR}"

# Source the script containing constants 
. ${SCRIPTS_DIR}/configmap_constants.sh

# configmap yaml tests
# output: Run the test cases for application autotune yaml
function configmap_yaml_tests() {
	start_time=$(get_date)
	# create the result directory for given testsuite
	TEST_SUITE_DIR="${RESULTS}/${FUNCNAME}"
	mkdir ${TEST_SUITE_DIR}
	object="configmap"
	
	if [ ! -z "${testcase}" ]; then
		check_test_case configmap
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
		testtorun=${configmap_tests[@]}
	else
		testtorun=${testcase}
	fi
	
	for test in "${testtorun[@]}"
	do	
		# Run the test
		echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
		echo "                    Running Testcase ${test}"
		echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
		
		LOG_DIR="${TEST_SUITE_DIR}/${test}"
		mkdir ${LOG_DIR}
		config_yaml="${LOG_DIR}/${cluster_type}-config.yaml"
		yaml_dir="${LOG_DIR}/yamls"
		mkdir ${yaml_dir}
		
		typeset -n var="${test}_testcases"
		for testcase in ${var[@]}
		do
			LOG="${LOG_DIR}/${testcase}.log"
			AUTOTUNE_LOG="${LOG_DIR}/${testcase}-autotune.log"
			AUTOTUNE_SETUP_LOG="${LOG_DIR}/setup.log"
			((TOTAL_TESTS++))
			((TESTS++))
		
			echo "*******----------- Running test for ${testcase} ----------*******"| tee  -a ${LOG}
			typeset -n find="${test}_find[${testcase}]"
			typeset -n replace="${test}_replace[${testcase}]"
			
			# Copy the configmap yaml 
			cp "${configmap}/${cluster_type}-config.yaml" "${config_yaml}"
			
			# Update the config map yaml with specified field
			update_yaml ${find} ${replace} ${config_yaml}
			
			# Keep a copy of the yaml used for the test
			cp "${config_yaml}" "${yaml_dir}/${testcase}.yaml"
			
			#create autotune setup
			echo -n "Deploying autotune..."| tee -a ${LOG}
			setup ${LOG_DIR} "1" >> ${AUTOTUNE_SETUP_LOG} 2>&1
			echo "done"| tee -a  ${LOG}
		
			# get the log of the autotune pod
			autotune_pod=$(kubectl get pod -n ${NAMESPACE} | grep autotune | cut -d " " -f1)
			pod_log_msg=$(kubectl logs ${autotune_pod} -n ${NAMESPACE})
			echo "${pod_log_msg}" >> "${AUTOTUNE_LOG}"
			
			typeset -n autotune_object="${test}_autotune_objects[${testcase}]"
			typeset -n expected_log_msg="${test}_expected_log_msgs[${testcase}]"
			echo
			
			status=$(kubectl get ${object} -n ${NAMESPACE} | grep "autotune-config" | cut -d " " -f1)
			
			# check if the expected message is matching with the actual message
			validate_yaml
			echo "" | tee -a  ${LOG}
			echo "-------------------------------------------------------------------" | tee -a  ${LOG}
		done
		rm -r ${config_yaml}
	done
	
	end_time=$(get_date)
	elapsed_time=$(time_diff "${start_time}" "${end_time}")
	
	# Summary of the test suite
	testsuitesummary ${FUNCNAME} ${elapsed_time} ${FAILED_CASES}
	
	# Check if any test failed in the testsuite if so add the testsuite to FAILED_TEST_SUITE array
	if [ "${TESTS_FAILED}" -ne "0" ]; then
		FAILED_TEST_SUITE+=(${FUNCNAME})
	fi
} 
