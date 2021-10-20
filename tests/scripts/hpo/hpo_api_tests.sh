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
#
##### Script for validating HPO (Hyper Parameter Optimization) /experiment_trials API #####

# Get the absolute path of current directory
CURRENT_DIR="$(dirname "$(realpath "$0")")"
SCRIPTS_DIR="${CURRENT_DIR}/hpo"
SEARCH_SPACE_JSON="${CURRENT_DIR}/../resources/searchspace_jsons/searchspace.json"

# Source the common functions scripts
. ${SCRIPTS_DIR}/constants/hpo_api_constants.sh

# Tests to validate the HPO APIs
function hpo_api_tests() {
	start_time=$(get_date)
	FAILED_CASES=()
	TESTS_FAILED=0
	TESTS_PASSED=0
	TESTS=0
	((TOTAL_TEST_SUITES++))

	hpo_api_tests=("hpo_post_experiment"  "hpo_get_trial_json" "hpo_post_exp_result")

	# check if the test case is supported
	if [ ! -z "${testcase}" ]; then
		check_test_case "hpo_api"
	fi

	# create the result directory for given testsuite
	echo ""
	TEST_SUITE_DIR="${RESULTS}/hpo_api_tests"
	mkdir -p ${TEST_SUITE_DIR}

	# If testcase is not specified run all tests	
	if [ -z "${testcase}" ]; then
		testtorun=("${hpo_api_tests[@]}")
	else
		testtorun=${testcase}
	fi

	for test in "${testtorun[@]}"
	do
		TEST_DIR="${TEST_SUITE_DIR}/${test}"
		mkdir ${TEST_DIR}
		SETUP="${TEST_DIR}/setup.log"
		AUTOTUNE_LOG="${TEST_DIR}/${test}_autotune.log"
		LOG="${TEST_SUITE_DIR}/${test}.log"

		echo ""
		echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" | tee -a ${LOG}
		echo "                    Running Test ${test}" | tee -a ${LOG}
		echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"| tee -a ${LOG}

		echo " " | tee -a ${LOG}
		echo "Test description: ${hpo_api_test_description[$test]}" | tee -a ${LOG}
		echo " " | tee -a ${LOG}

		# Perform the test
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

# Post a JSON object to HPO(Hyper Parameter Optimization) module
# input: JSON object
# output: Create the Curl command with given JSON and get the result
function post_experiment_json() {
	json_array_=$1

	content='curl -H "Content-Type: application/json"'
	post_cmd=$(curl -H "Content-Type: application/json" -d "${json_array_}"  http://localhost:8085/experiment_trials -w '\n%{http_code}' 2>&1)
	# Example curl command: curl -H "Content-Type: application/json" -d {"id" : "a123", "url" : "http://localhost:8080/searchSpace", "operation" : "EXP_TRIAL_GENERATE_NEW"}  http://localhost:8085/experiment_trials -w n%{http_code}
	post_experiment_cmd="${content} -d ${json_array_}  http://localhost:8085/experiment_trials -w '\n%{http_code}'"

	echo "" | tee -a ${LOG_} ${LOG}
	echo "Command used to post the experiment= ${post_experiment_cmd}" | tee -a ${LOG_} ${LOG}
	echo "" | tee -a ${LOG_} ${LOG}

	echo "${post_cmd}" >> ${LOG_} ${LOG}

	http_code=$(tail -n1 <<< "${post_cmd}")
	response=$(echo -e "${post_cmd}" | tail -2 | head -1)

	echo "Response is ${response}" >> ${LOG_} ${LOG}
}

# Check if the servers have started
function check_server_status() {
	searchspace_log_msg="Starting server at http://localhost:8080"
	service_log_msg="Starting server at http://localhost:8085"

	echo ""
	if grep -q "${searchspace_log_msg}" "${TESTS_}/searchspace.log" ; then
		echo "Searchspace service started successfully..." | tee -a ${LOG_} ${LOG}
	else
		echo "Error Starting the searchspace service..." | tee -a ${LOG_} ${LOG}
		echo "See ${TESTS_}/searchspace.log for more details" | tee -a ${LOG_} ${LOG}
		exit 0
	fi

	if grep -q "${service_log_msg}" "${TESTS_}/service.log" ; then
		echo "HPO REST API service started successfully..." | tee -a ${LOG_} ${LOG}
	else
		echo "Error Starting the HPO REST API service..." | tee -a ${LOG_} ${LOG}
		echo "See ${TESTS_}/service.log for more details" | tee -a ${LOG_} ${LOG}
		exit 0
	fi
}

# The test does the following:
# In case of hpo_post_experiment test, Post valid and invalid experiments to HPO /experiment_trials API and validate the reslut
# In case of hpo_post_exp_result test, Post valid and invalid experiments results to HPO /experiment_trials API and validate the result
# input: Test name
function run_post_tests(){
	hpo_test_name=$1
	
	if [ "${hpo_test_name}" == "hpo_post_experiment" ]; then
		exp_tests=("${run_post_experiment_tests[@]}")
	else
		exp_tests=("${run_post_exp_result_tests[@]}")
	fi
	
	for post_test in "${exp_tests[@]}"
	do
		TESTS_="${TEST_DIR}/${post_test}"
		mkdir -p ${TESTS_}
		LOG_="${TEST_DIR}/${post_test}.log"

		echo "************************************* ${post_test} Test ****************************************" | tee -a ${LOG_} ${LOG}
		echo "" | tee -a ${LOG_} ${LOG}

		case "${post_test}" in
			invalid-searchspace)
				exp="valid-experiment"
				sed 's/"slo_class": "response_time"/"slo_class": "xyz"/g' ${SEARCH_SPACE_JSON} > ${TESTS_}/invalid_searchspace.json
				echo "Searchspace JSON" | tee -a ${LOG_} ${LOG}
				cat ${TESTS_}/invalid_searchspace.json | tee -a ${LOG_} ${LOG}
				echo "" | tee -a ${LOG_} ${LOG}
				# Start the HPO servers
				${SCRIPTS_DIR}/start_hpo_servers.sh -p ${TESTS_} -j ${TESTS_}/invalid_searchspace.json | tee -a ${LOG_} ${LOG}
				;;
			*)
				exp="${post_test}"	
				# Start the HPO servers
				${SCRIPTS_DIR}/start_hpo_servers.sh -p ${TESTS_} -j ${SEARCH_SPACE_JSON} | tee -a ${LOG_} ${LOG}
		esac

		# Sleep for few seconds to reduce the ambiguity
		sleep 2

		# Check if the servers have started
		check_server_status

		# Get the id from search space JSON
		current_id=$(cat ${SEARCH_SPACE_JSON} | jq .[].id | tr -d '""')

		if [ "${hpo_test_name}" == "hpo_post_exp_result" ]; then
			exp="valid-experiment"
			# Post the experiment JSON to HPO /experiment_trials API
			create_post_exp_json_array "${current_id}"
			post_experiment_json "${hpo_post_experiment_json[$exp]}"
			trial_num="${response}"
			
			# Post the experiment result to HPO /experiment_trials API
			create_post_exp_result_json_array "${current_id}" "${trial_num}"
			post_experiment_result_json "${hpo_post_exp_result_json[$post_test]}"
			expected_log_msg="${hpo_exp_result_error_messages[$post_test]}"
		else
			# Post the experiment JSON to HPO /experiment_trials API
			create_post_exp_json_array "${current_id}"
			post_experiment_json "${hpo_post_experiment_json[$exp]}"
			expected_log_msg="${hpo_error_messages[$post_test]}"
		fi

		if [[ "${post_test}" == valid* ]]; then
			expected_result_="200"
			expected_behaviour="RESPONSE_CODE = 200 OK"
		else
			expected_result_="^4[0-9][0-9]"
			expected_behaviour="RESPONSE_CODE = 4XX BAD REQUEST"
		fi

		actual_result="${http_code}"
		if [[ "${http_code}" -eq "000" ]]; then
			if grep -q "${expected_log_msg}" "${TESTS_}/service.log" ; then
				failed=0 
			else
				failed=1
			fi
			((TOTAL_TESTS++))
			((TESTS++))
			error_message "${failed}"
		else
			compare_result "${hpo_test_name}" "${expected_result_}" "${expected_behaviour}"
		fi
		echo ""

		# Stop the HPO servers
		${SCRIPTS_DIR}/start_hpo_servers.sh -t | tee -a ${LOG_} ${LOG}

		# Sleep for few seconds to reduce the ambiguity
		sleep 5

		echo "" | tee -a ${LOG_} ${LOG}

	done
	
	echo "*********************************************************************************************************" | tee -a ${LOG_} ${LOG}
}

# Do a post on experiment_trials for the same id again with "operation: EXP_TRIAL_GENERATE_NEW" and check if experiments have started from the beginning
function post_duplicate_experiments() {
	create_post_exp_json_array "${current_id}"
	post_experiment_json "${hpo_post_experiment_json[$exp]}"
	
	if [ "${http_code}" == "200" ]; then
		failed=0
	
		# Post the json with same Id having "operation: EXP_TRIAL_GENERATE_NEW"
		echo "Post the json with same Id having operation: EXP_TRIAL_GENERATE_NEW" | tee -a ${LOG_} ${LOG}
		
		# Sleep for few seconds to reduce the ambiguity
		sleep 2
		
		post_experiment_json "${hpo_post_experiment_json[$exp]}"
		
		actual_result="${http_code}"
		expected_result_="^4[0-9][0-9]"
		expected_behaviour="RESPONSE_CODE = 4XX BAD REQUEST"
	
		compare_result "${hpo_test_name}" "${expected_result_}" "${expected_behaviour}"
	else
		failed=1
		expected_behaviour="RESPONSE_CODE = 200 OK"
		echo "Posting valid experiment failed"
		display_result "${expected_behaviour}" "${hpo_test_name}" "${failed}"
	fi
}

# Do a post on experiment_trials for the same id again with "operation: EXP_TRIAL_GENERATE_SUBSEQUENT" and check if same experiment continues
function operation_generate_subsequent() {
	create_post_exp_json_array "${current_id}"
	post_experiment_json "${hpo_post_experiment_json[$exp]}"
	trial_num="${response}"

	# Sleep for few seconds to reduce the ambiguity
	sleep 5

	# Post a valid experiment result to HPO /experiment_trials API.
	echo -n "Post a valid experiment result to HPO..." | tee -a ${LOG_} ${LOG}
	experiment_result="valid-experiment-result"
	create_post_exp_result_json_array "${current_id}" "${trial_num}"
	post_experiment_result_json "${hpo_post_exp_result_json[$experiment_result]}"

	# Sleep for few seconds to reduce the ambiguity
	sleep 5

	# Post the json with same Id having "operation: EXP_TRIAL_GENERATE_SUBSEQUENT"
	echo "Post the json with same Id having operation: EXP_TRIAL_GENERATE_SUBSEQUENT" | tee -a ${LOG_} ${LOG}
	exp="generate-subsequent"
	post_experiment_json "${hpo_post_experiment_json[$exp]}"

	actual_result="${response}"
	echo "actual_result ${actual_result}"
	expected_result_=$(($trial_num+1))
	expected_behaviour="trial_number = '${expected_result_}'"

	compare_result "${hpo_test_name}" "${expected_result_}" "${expected_behaviour}"
}

# The test does the following: 
# * Post the same experiment again with operation set to "EXP_TRIAL_GENERATE_NEW" and validate the result.
# * Post the same experiment again with the operation set to "EXP_TRIAL_GENERATE_SUBSEQUENT" after we post the result for the previous trial, and check if subsequent trial number is generated
# input: Test name
function other_post_experiment_tests() {
	hpo_test_name=$1
	exp="valid-experiment"
	
	for operation in "${other_post_experiment_tests[@]}"
	do
		TESTS_="${TEST_DIR}/${operation}"
		mkdir -p ${TESTS_}
		LOG_="${TEST_DIR}/${operation}.log"
		echo "************************************* ${operation} Test ****************************************" | tee -a ${LOG_} ${LOG}
		
		# Start the HPO servers
		${SCRIPTS_DIR}/start_hpo_servers.sh -p ${TESTS_} -j ${SEARCH_SPACE_JSON} | tee -a ${LOG_} ${LOG}
		
		# Sleep for few seconds to reduce the ambiguity
		sleep 2
		
		# Check if the servers have started
		check_server_status
				
		# Get the id from search space JSON
		current_id=$(cat ${SEARCH_SPACE_JSON} | jq .[].id | tr -d '""')
		
		operation=$(echo ${operation//-/_})
		${operation}
		echo ""
		
		# Stop the HPO servers
		${SCRIPTS_DIR}/start_hpo_servers.sh -t | tee -a ${LOG_} ${LOG}
		
		# Sleep for few seconds to reduce the ambiguity
		sleep 2
	done
		
	echo "*********************************************************************************************************" | tee -a ${LOG_} ${LOG}	
}

# Generate the curl command based on the test name passed and get the result by querying it.
# input: Test name and Trial number
function run_get_trial_json_test() {
	exp_trial=$1
	trial_num=$2
	curl="curl -H 'Accept: application/json'"
	url="http://localhost:8085/experiment_trials"
	case "${exp_trial}" in
		invalid-id)
			get_trial_json=$(${curl} ''${url}'?id=124365213472&trial_number=0'  -w '\n%{http_code}' 2>&1)
			get_trial_json_cmd="${curl} '${url}?id=124365213472&trial_number=0' -w '\n%{http_code}'"
			;;
		empty-id)
			get_trial_json=$(${curl} ''${url}'?id= &trial_number=0' -w '\n%{http_code}' 2>&1)
			get_trial_json_cmd="${curl} '${url}?id= &trial_number=0' -w '\n%{http_code}'"
			;;
		no-id)
			get_trial_json=$(${curl} ''${url}'?trial_number=0' -w '\n%{http_code}' 2>&1)
			get_trial_json_cmd="${curl} '${url}?trial_number=0' -w '\n%{http_code}'"
			;;
		null-id)
			get_trial_json=$(${curl} ''${url}'?id=null &trial_number=0' -w '\n%{http_code}' 2>&1)
			get_trial_json_cmd="${curl} '${url}?id=null &trial_number=0' -w '\n%{http_code}'"
			;;
		only-valid-id)
			get_trial_json=$(${curl} ''${url}'?id='${current_id}'' -w '\n%{http_code}' 2>&1)
			get_trial_json_cmd="${curl} '${url}?id='${current_id}'' -w '\n%{http_code}'"
			;;
		invalid-trial-number)
			get_trial_json=$(${curl} ''${url}'?id='${current_id}'&trial_number=102yrt' -w '\n%{http_code}' 2>&1)
			get_trial_json_cmd="${curl} '${url}?id='${current_id}'&trial_number=102yrt' -w '\n%{http_code}'"
			;;
		empty-trial-number)
			get_trial_json=$(${curl} ''${url}'?id='${current_id}'&trial_number=' -w '\n%{http_code}' 2>&1)
			get_trial_json_cmd="${curl} '${url}?id='${current_id}'&trial_number=' -w '\n%{http_code}'"
			;;
		no-trial-number)
			get_trial_json=$(${curl} ''${url}'?id='${current_id}'' -w '\n%{http_code}' 2>&1)
			get_trial_json_cmd="${curl} '${url}?id='${current_id}'' -w '\n%{http_code}'"
			;;
		null-trial-number)
			get_trial_json=$(${curl} ''${url}'?id='${current_id}'&trial_number=null' -w '\n%{http_code}' 2>&1)
			get_trial_json_cmd="${curl} '${url}?id='${current_id}'&trial_number=null' -w '\n%{http_code}'"
			;;
		only-valid-trial-number)
			get_trial_json=$(${curl} ''${url}'?trial_number=0' -w '\n%{http_code}' 2>&1)
			get_trial_json_cmd="${curl} '${url}?trial_number=0' -w '\n%{http_code}'"
			;;
		valid-exp-trial)
			get_trial_json=$(${curl} ''${url}'?id=a123&trial_number='${trial_num}'' -w '\n%{http_code}' 2>&1)
			get_trial_json_cmd="${curl} '${url}?id=a123&trial_number=${trial_num}' -w '\n%{http_code}'"
			;;
	esac
	
	echo "command used to query the experiment_trial API = ${get_trial_json_cmd}" | tee -a ${LOG_} ${LOG}
	echo "" | tee -a ${LOG_} ${LOG}
	echo "${get_trial_json}" >> ${LOG_} ${LOG}
	http_code=$(tail -n1 <<< "${get_trial_json}")
	response=$(echo -e "${get_trial_json}" | tail -2 | head -1)
	response=$(echo ${response} | cut -c 4-)
	echo "${response}" > ${result}
}

# validate obtaining trial json from RM-HPO /experiment_trials API for invalid queries
# input: test name 
function get_trial_json_invalid_tests() {
	__test_name__=$1
	IFS=' ' read -r -a get_trial_json_invalid_tests <<<  ${rm_hpo_get_trial_json_tests[$FUNCNAME]}
	for exp_trial in "${get_trial_json_invalid_tests[@]}"
	do
		TESTS_="${TEST_DIR}/${exp_trial}"
		mkdir -p ${TESTS_}
		LOG_="${TEST_DIR}/${exp_trial}.log"
		result="${TESTS_}/${exp_trial}_result.log"
		echo "************************************* ${exp_trial} Test ****************************************" | tee -a ${LOG_} ${LOG}
		
		# Start the HPO servers
		${SCRIPTS_DIR}/start_hpo_servers.sh -p ${TESTS_} | tee -a ${LOG_} ${LOG}
		
		# Sleep for few seconds to reduce the ambiguity
		sleep 2
		
		# Check if the servers have started
		check_server_status

		# Get the id from search space JSON
		current_id=$(cat ${SEARCH_SPACE_JSON} | jq .[].id | tr -d '""')

		create_post_exp_json_array ${current_id}
		
		# Post a valid experiment to RM-HPO /experiment_trials API.
		exp="valid-experiment"
		post_experiment_json "${rm_hpo_post_experiment_json[$exp]}"

		run_get_trial_json_test ${exp_trial}

		actual_result="${http_code}"
		
		expected_result_="^4[0-9][0-9]"
		expected_behaviour="RESPONSE_CODE = 4XX BAD REQUEST"

		compare_result ${__test_name__} ${expected_result_} "${expected_behaviour}"
		echo ""
		
		# Stop the HPO servers
		${SCRIPTS_DIR}/start_hpo_servers.sh -t | tee -a ${LOG_} ${LOG}
	done
	echo "*********************************************************************************************************" | tee -a ${LOG_} ${LOG}
}

# Validate if the actual tunable name is matching with the tunable name returned by dependency analyzer
function validate_tunable_name() {
	failed=0
	if [ "${actual_tunable_name}" != "${tunable_name}" ]; then
		failed=1
	fi
	expected_behaviour="Actual Tunable name should match with the tunable name returned by dependency analyzer"
	display_result "${expected_behaviour}" ${__test_name__} ${failed}
}

# Validate if Actual Tunable value is within the given range
function validate_tunable_value(){
	failed=0
	
 	if [[ $(bc <<< "${actual_tunable_value} >= ${lowerbound} && ${actual_tunable_value} <= ${upperbound}") == 0 ]]; then
 		failed=1
	fi
	expected_behaviour="Actual Tunable value should be within the given range"
	display_result "${expected_behaviour}" ${__test_name__} ${failed}
}

# Validate the trial json returned by RM-HPO GET operation
function validate_exp_trial() {
	tunable_count=0
	# Sort the actual json based on tunable name
	echo "$(cat ${result} | jq  'sort_by(.tunable_name)')" > ${result}

	# Sort the json based on tunable name
	echo "$(jq '[.[].tunables[] | {lower_bound: .lower_bound, name: .name, upper_bound: .upper_bound}] | sort_by(.name)' ${SEARCH_SPACE_JSON})" > ${parse_json}

	expected_tunables=$(cat ${parse_json} | jq '. | length')
	actual_tunables=$(cat ${result}  | jq '. | length')
	
	echo "___________________________________ Validate experiment trial __________________________________________" | tee -a ${LOG_} ${LOG}
	echo "" | tee -a ${LOG_} ${LOG}
	
	if [ "${expected_tunables}" -ne "${actual_tunables}" ]; then
		failed=1
		expected_behaviour="Number of expected and actual tunables should be same"
		display_result "${expected_behaviour}" ${__test_name__} ${failed}
		
		echo "" | tee -a ${LOG_} ${LOG}
		echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" | tee -a ${LOG_} ${LOG}
	else
		failed=0
		expected_behaviour="Number of expected and actual tunables should be same"
		display_result "${expected_behaviour}" ${__test_name__} ${failed}
		
		echo "" | tee -a ${LOG_} ${LOG}
		echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" | tee -a ${LOG_} ${LOG}
		
		while [ "${tunable_count}" -lt "${expected_tunables}" ]
		do
			upperbound=$(cat ${parse_json} | jq '.['${tunable_count}'].upper_bound')
			lowerbound=$(cat ${parse_json} | jq '.['${tunable_count}'].lower_bound')
			tunable_name=$(cat ${parse_json} | jq '.['${tunable_count}'].name')
			actual_tunable_name=$(cat ${result} | jq '.['${tunable_count}'].tunable_name')
			actual_tunable_value=$(cat ${result} | jq '.['${tunable_count}'].tunable_value')

			echo "" | tee -a ${LOG_} ${LOG}
			echo "Validating the tunable name ${actual_tunable_name}..." | tee -a ${LOG_} ${LOG}
			# validate the tunable name
			validate_tunable_name
			echo "" | tee -a ${LOG_} ${LOG}

			echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" | tee -a ${LOG_} ${LOG}
			echo "" | tee -a ${LOG_} ${LOG}
			echo "Validating the tunable value for ${actual_tunable_name}..." | tee -a ${LOG_} ${LOG}
			# validate the tunable value
			validate_tunable_value
			echo "" | tee -a ${LOG_} ${LOG}
			echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" | tee -a ${LOG_} ${LOG}
			((tunable_count++))
		done
	fi
	echo ""
}

# Post a valid experiment to RM-HPO /experiment_trials API, Query it using valid experiment id and trial number and validate the result.
# input: test name
function get_trial_json_valid_tests() {
	__test_name__=$1

	IFS=' ' read -r -a get_trial_json_valid_tests <<<  ${rm_hpo_get_trial_json_tests[$FUNCNAME]}
	for exp_trial in "${get_trial_json_valid_tests[@]}"
	do
		TESTS_="${TEST_DIR}/${FUNCNAME}"
		mkdir -p ${TESTS_}
		LOG_="${TEST_DIR}/${FUNCNAME}.log"
		result="${TESTS_}/${exp_trial}_result.log"
		parse_json="${TESTS_}/${exp_trial}_actual_json.json"
		echo "************************************* ${exp_trial} Test ****************************************" | tee -a ${LOG_} ${LOG}
	
		# Start the HPO servers
		${SCRIPTS_DIR}/start_hpo_servers.sh -p ${TESTS_} | tee -a ${LOG_} ${LOG}
		
		# Sleep for few seconds to reduce the ambiguity
		sleep 2
	
		# Check if the servers have started
		check_server_status
			
		# Get the id from search space JSON
		current_id=$(cat ${SEARCH_SPACE_JSON} | jq .[].id | tr -d '""')

		# Post a valid experiment to RM-HPO /experiment_trials API.
		exp="valid-experiment"
		if [ "${exp_trial}" == "valid-exp-trial" ]; then
			create_post_exp_json_array ${current_id}
			post_experiment_json "${rm_hpo_post_experiment_json[$exp]}"
			trial_num="${response}"
		else
			operation_generate_subsequent
			trial_num="${response}"
		fi
		
		# Query the RM-HPO /experiment_trials API for valid experiment id and trial number and get the result.
		run_get_trial_json_test "valid-exp-trial" "${trial_num}"

		actual_result="${http_code}"
		
		expected_result_="200"
		expected_behaviour="RESPONSE_CODE = 200 OK"

		compare_result ${__test_name__} ${expected_result_} "${expected_behaviour}"
		
		if [ "${failed}" -eq 0 ]; then
			validate_exp_trial
		fi
		
		# Stop the HPO servers
		${SCRIPTS_DIR}/start_hpo_servers.sh -t | tee -a ${LOG_} ${LOG}
		echo "*********************************************************************************************************" | tee -a ${LOG_} ${LOG}
	done
}

# Post the experiment result to HPO /experiment_trials API
# input: Experiment result
# output: Create the Curl command with given JSON and get the result
function post_experiment_result_json() {
	exp_result=$1

	content='curl -H "Content-Type: application/json"'
	post_result=$(curl -H "Content-Type: application/json" -d "${exp_result}" http://localhost:8085/experiment_trials -w '\n%{http_code}' 2>&1)
	# Example curl command used to post the experiment result: curl -H "Content-Type: application/json" -d {"id" : null, "trial_number": 0, "trial_result": "success", "result_value_type": "double", "result_value": 98.78, "operation" : "EXP_TRIAL_RESULT"} http://localhost:8085/experiment_trials -w n%{http_code}
	post_exp_result_cmd="${content} -d ${exp_result} http://localhost:8085/experiment_trials -w '\n%{http_code}'"

	echo "" | tee -a ${LOG_} ${LOG}
	echo "Command used to post the experiment result= ${post_exp_result_cmd}" | tee -a ${LOG_} ${LOG}
	echo "" | tee -a ${LOG_} ${LOG}

	echo "${post_result}" >> ${LOG_} ${LOG}

	http_code=$(tail -n1 <<< "${post_result}")
	response=$(echo -e "${post_result}" | tail -2 | head -1)
	echo "Response is ${response}" >> ${LOG_} ${LOG}
}

# Post duplicate experiment results to HPO /experiment_trials API and validate the result
function post_duplicate_exp_result() {
	# Post a valid experiment to HPO /experiment_trials API.
	exp="valid-experiment"
	create_post_exp_json_array "${current_id}"
	post_experiment_json "${hpo_post_experiment_json[$exp]}"
	trial_num="${response}"

	if [ "${http_code}" == "200" ]; then
		failed=0

		# Sleep for few seconds to reduce the ambiguity
		sleep 5

		# Post a valid experiment result to HPO /experiment_trials API.
		experiment_result="valid-experiment-result"
	
		echo -n "Post the experiment result to HPO..."
		create_post_exp_result_json_array "${current_id}" "${trial_num}"
		post_experiment_result_json "${hpo_post_exp_result_json[$experiment_result]}"

		# Sleep for few seconds to reduce the ambiguity
		sleep 5

		# Post the duplicate experiment result to HPO /experiment_trials API.
		echo -n "Post the same experiment result to HPO again for the same id and trial number..."
		create_post_exp_result_json_array "${current_id}" "${trial_num}"
		post_experiment_result_json "${hpo_post_exp_result_json[$experiment_result]}"

		actual_result="${http_code}"
		expected_result_="^4[0-9][0-9]"
		expected_behaviour="RESPONSE_CODE = 4XX BAD REQUEST"

		compare_result "${hpo_test_name}" "${expected_result_}" "${expected_behaviour}"
	else
		failed=1
		expected_behaviour="RESPONSE_CODE = 200 OK"
		echo "Posting valid experiment failed"
		display_result "${expected_behaviour}" "${hpo_test_name}" "${failed}"
	fi
}

# Post different experiment results to HPO /experiment_trials API for the same experiment id and validate the result
function post_same_id_different_exp_result() {
	# Post a valid experiment to HPO /experiment_trials API.
	exp="valid-experiment"
	create_post_exp_json_array "${current_id}"
	post_experiment_json "${hpo_post_experiment_json[$exp]}"
	trial_num="${response}"

	if [ "${http_code}" == "200" ]; then
		failed=0

		# Sleep for few seconds to reduce the ambiguity
		sleep 5

		# Post a valid experiment result to HPO /experiment_trials API.
		experiment_result="valid-experiment-result"
		echo -n "Post the experiment result to HPO..."
		create_post_exp_result_json_array "${current_id}" "${trial_num}"
		post_experiment_result_json "${hpo_post_exp_result_json[$experiment_result]}"

		# Sleep for few seconds to reduce the ambiguity
		sleep 5

		# Post a different valid experiment result for the same id and trial number to HPO /experiment_trials API.
		experiment_result="valid-different-result"
		echo -n "Post the differnt experiment result to HPO again for the same id and trial number..."
		create_post_exp_result_json_array "${current_id}" "${trial_num}"
		post_experiment_result_json "${hpo_post_exp_result_json[$experiment_result]}"

		actual_result="${http_code}"
		expected_result_="^4[0-9][0-9]"
		expected_behaviour="RESPONSE_CODE = 4XX BAD REQUEST"

		compare_result "${hpo_test_name}" "${expected_result_}" "${expected_behaviour}"
	else
		failed=1
		expected_behaviour="RESPONSE_CODE = 200 OK"
		echo "Posting valid experiment failed"
		display_result "${expected_behaviour}" "${hpo_test_name}" "${failed}"
	fi
}

# The test does the following:
# * Post duplicate experiment results to HPO /experiment_trials API and validate the result
# * Post different experiment results to HPO /experiment_trials API for the same experiment id and validate the result
# input: Test name
function other_exp_result_post_tests() {
	hpo_test_name=$1

	for operation in "${other_exp_result_post_tests[@]}"
	do
		TESTS_="${TEST_DIR}/${operation}"
		mkdir -p ${TESTS_}
		LOG_="${TEST_DIR}/${operation}.log"
		echo "************************************* ${operation} Test ****************************************" | tee -a ${LOG_} ${LOG}

		# Start the HPO servers
		${SCRIPTS_DIR}/start_hpo_servers.sh -p ${TESTS_} | tee -a ${LOG_} ${LOG}

		# Sleep for few seconds to reduce the ambiguity
		sleep 2

		# Check if the servers have started
		check_server_status

		# Get the id from search space JSON
		current_id=$(cat ${SEARCH_SPACE_JSON} | jq .[].id | tr -d '""')

		operation=$(echo ${operation//-/_})
		${operation}
		echo ""

		# Stop the HPO servers
		${SCRIPTS_DIR}/start_hpo_servers.sh -t | tee -a ${LOG_} ${LOG}

		# Sleep for few seconds to reduce the ambiguity
		sleep 2

		echo "*********************************************************************************************************" | tee -a ${LOG_} ${LOG}
	done	
}

# Tests for HPO /experiment_trials API POST experiment
function hpo_post_experiment() {
	run_post_tests ${FUNCNAME}
	other_post_experiment_tests ${FUNCNAME}
}

# Tests for RM-HPO GET trial JSON API
function rm_hpo_get_trial_json(){
	for test in "${!rm_hpo_get_trial_json_tests[@]}"
	do
		${test} "${FUNCNAME}"
	done 
}

# Tests for HPO /experiment_trials API POST experiment results
function hpo_post_exp_result() {
	run_post_tests ${FUNCNAME}
	other_exp_result_post_tests ${FUNCNAME}
}

