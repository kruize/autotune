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
##### Script for validating the autotune object id #####

# Get the absolute path of current directory
CURRENT_DIR="$(dirname "$(realpath "$0")")"
SCRIPTS_DIR="${CURRENT_DIR}"

# Source the common functions scripts
. ${SCRIPTS_DIR}/id_constants.sh

# validate autotune object id for all APIs 
function autotune_id_tests() {
	start_time=$(get_date)
	FAILED_CASES=()
	TESTS_FAILED=0
	TESTS_PASSED=0
	TESTS=0
	autotune_id_tests=( "check_uniqueness" "re_apply" "update_app_autotune_yaml" "multiple_apps")
	declare -A app_array=([galaxies]="3")
	((TOTAL_TEST_SUITES++))
	
	if [ ! -z "${testcase}" ]; then
		check_test_case "autotune_id"
	fi
	
	# create the result directory for given testsuite
	echo ""
	TEST_SUITE_DIR="${RESULTS}/autotune_id_tests"
	mkdir -p ${TEST_SUITE_DIR}
	
	echo ""
	echo "**************************** Executing test suite ${FUNCNAME} *************************"
	echo ""
	
	# If testcase is not specified run all tests	
	if [ -z "${testcase}" ]; then
		testtorun=("${autotune_id_tests[@]}")
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
	
	# Remove the duplicates 
	FAILED_CASES=( $(printf '%s\n' "${FAILED_CASES[@]}" | uniq ) )
	
	# print the testsuite summary
	testsuitesummary ${FUNCNAME} ${elapsed_time} ${FAILED_CASES} 
}

# Get the autotune id form the APIs
# input: json file containing the API result and the test name
# output: Get the autotune object ids from the API result
function get_autotune_id() {
	json_=$1
	test_name=$2
	declare -A autotune_id
	length=$(cat ${json_} | jq '. | length')
	while [ "${length}" -ne 0 ]
	do	
		((length--))
		autotune_id[test_name]+=" $(cat ${json_} | jq .[${length}].id) "
	done
	id_="${autotune_id[test_name]}"
	
	# convert config_id into an array
	IFS=' ' read -r -a id_ <<<  ${id_}
}

# query the api and get the result
# input: The API which has to be queried
# output: Query the API and get the autotune object ids
function query_api() {
	api_to_query=$1
	layer="container"
	
	echo " " | tee -a ${LOG}
	case "${api_to_query}" in
		get_listapplication_json_app)
			# listapplication for specific application
			get_listapplication_json ${application_name}
			;;
		get_listapplication_json)
			# listapplication for all applications
			get_listapplication_json
			;;
		get_listapplayer_json_app)
			# listapplayer for specific application
			get_listapplayer_json ${application_name}
			;;
		get_listapplayer_json)	
			# listapplayer for all applications
			get_listapplayer_json
			;;
		get_listapptunables_json_app_layer)
			# listapptunables for specific application and specific layer
			get_listapptunables_json ${application_name} ${layer}
			;;
		get_listapptunables_json_app)
			# listapptunables for specific application
			get_listapptunables_json ${application_name}
			;;
		get_listapptunables_json)
			# listapptunables for all applications	
			get_listapptunables_json
			;;
		get_searchspace_json_app)
			# test searchSpace API for specific application
			get_searchspace_json ${application_name}
			;;
		get_searchspace_json)
			# test searchSpace API for all applications
			get_searchspace_json
			;;
	esac
	# get the autotune id from the API result
	get_autotune_id ${json_file} ${api_to_query}
	echo " " | tee -a ${LOG}
}

# check if the old id is matching with the new id after re-apply
# output: set the flag to 1 if the ids are not same
function re_apply_test() {
	flag=0
	declare -A old_autotune_id
	old_autotune_id[test_name]+="${id_[@]}"
	
	echo -n "Deleting the application autotune objects..." | tee -a ${LOG}
	echo " " >> ${LOG}
	kubectl delete -f ${yaml_dir} >> ${LOG}
	echo "done" | tee -a ${LOG}
	
	# sleep for few seconds to reduce the ambiguity
	sleep 2
	
	echo -n "Re-applying the application autotune yaml..." | tee -a ${LOG}
	echo " " >> ${LOG}
	kubectl apply -f ${yaml_dir} >> ${LOG}
	echo "done"
	
	# sleep for few seconds to reduce the ambiguity
	sleep 2
	
	query_api ${test_name}
	
	old_id_=("${old_autotune_id[test_name]}")
	IFS=' ' read -r -a old_id_ <<<  ${old_id_}
	new_id_=("${id_[@]}")
	
	match_ids
	
	if [ "${matched_count}" -ne "${#old_id_[@]}" ]; then
		flag=1
		if [ "${matched_count}" -eq "$(expr ${#old_id_[@]} - 1 )" ]; then
			echo "Autotune object id is not same as previous for single instance"
		else
			echo "Autotune object ids are not same as previous for multiple instances"
		fi
	fi
}

# update the application autotune yaml and check if the ids have changed
# output: set the flag to 1 if the ids are not unique
function update_app_autotune() {
	flag=0
	declare -A old_autotune_id
	old_autotune_id[test_name]+="${id_[@]}"
	
	# Update and apply the application autotune yaml
	count=0
	sla=`grep -A3 'sla_class:' ${yaml_dir}/${autotune_names[count]}.yaml | head -n1 | awk '{print $2}' | tr -d '""'`
	direction=`grep -A3 'direction:' ${yaml_dir}/${autotune_names[count]}.yaml | head -n1 | awk '{print $2}' | tr -d '""'`
	
	find_sla="${sla}"
	case "${find_sla}" in
		response_time)
			replace_sla="throughput"
			;;
		throughput)
			replace_sla="response_time"
			;;
	esac
	
	find_direction="${direction}"
	case "${find_direction}" in
		minimize)
			replace_direction="maximize"
			;;
		maximize)
			replace_direction="minimize"
			;;
	esac
	
	echo "Update and apply the autotune yamls..." | tee -a ${LOG}
	for app in "${app_pod_names[@]}"
	do
		test_yaml="${yaml_dir}/${autotune_names[count]}.yaml"
		sed -i 's/'${find_sla}'/'${replace_sla}'/g' ${test_yaml}
		sed -i 's/'${find_direction}'/'${replace_direction}'/g' ${test_yaml}
		echo "Applying autotune yaml ${test_yaml}..." | tee -a ${LOG}
		kubectl apply -f ${test_yaml} | tee -a ${LOG}
		((count++))
	done
	echo "done" | tee -a ${LOG}
	
	# sleep for few seconds to reduce the ambiguity
	sleep 2
	
	query_api ${test_name}
	
	old_id_=("${old_autotune_id[test_name]}")
	IFS=' ' read -r -a old_id_ <<<  ${old_id_}
	new_id_=("${id_[@]}")
	
	match_ids
	
	if [ "${matched_count}" -gt "0" ]; then
		flag=1
		if [ "${matched_count}" -eq "1" ]; then
			echo "Autotune object id is same as previous for single instance"
		else
			echo "Autotune object ids are same as previous for multiple instances"
		fi
	fi
}

# validate the autotune object ids based on the test
# input: id test name
# output: perform the test based on the id test name
function validate_autotune_id() {
	id_=$1
	case "${id_test_name}" in
		check_uniqueness)
			uniqueness_test "${id_[@]}"
			;;
		re_apply)
			re_apply_test
			;;
		update_app_autotune_yaml)
			update_app_autotune
			;;
		multiple_apps)
			uniqueness_test "${id_[@]}"
			;;
	esac
	display_result "${autotune_id_expected_behaviour[$id_test_name]}" "${id_test_name}" ${flag}
	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"| tee -a ${LOG}
}

# query the APIs and store the ids for further tests
# input: id test name
# output: query the APIs, get the autotune object ids and validate those autotune object ids based on the test
function validate_apis() {
	id_test_name=$1
	
	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"| tee -a ${LOG}
	for autotune_id_test in "${autotune_id[@]}"
	do
		echo ""
		echo "**********************************************************************************************"
		echo "					${autotune_id_test}"
		echo "**********************************************************************************************"
		LOG_DIR="${TEST_DIR}/${autotune_id_test}"
		mkdir -p ${LOG_DIR}
		query_api ${autotune_id_test}
		validate_autotune_id "${id_}"
	done
}

# Perform the autotune id tests
# input: test name
# output: deploy the autotune and required benchmarks, query the APIs and validate the autotune object ids
function perform_autotune_id_test() {
	_test_=$1
	
	# sleep for few seconds to reduce the ambiguity
	sleep 10
	TEST_DIR="${TEST_SUITE_DIR}/${_test_}"
	mkdir ${TEST_DIR}
	AUTOTUNE_SETUP_LOG="${TEST_DIR}/setup.log"
	AUTOTUNE_LOG="${TEST_DIR}/${_test_}_autotune.log"
	LOG="${TEST_SUITE_DIR}/${_test_}.log"
	
	echo ""
	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" | tee -a ${LOG}
	echo "                    Running Test ${_test_}" | tee -a ${LOG}
	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"| tee -a ${LOG}
			
	echo " " | tee -a ${LOG}
	echo "Test description: ${autotune_id_test_description[$_test_]}" | tee -a ${LOG}
	echo " " | tee -a ${LOG}
	
	# create the autotune setup
	echo "Setting up autotune..." | tee -a ${LOG}
	setup >> ${AUTOTUNE_SETUP_LOG} 2>&1
	echo "Setting up autotune...Done" | tee -a ${LOG}
	
	# Giving a sleep for autotune pod to be up and running
	sleep 10
	
	# form the curl command based on the cluster type
	form_curl_cmd
	
	# Yaml directory to store the autotune yamls created during the test
	yaml_dir="${TEST_DIR}/yamls"
	mkdir -p ${yaml_dir}
	
	# Deploy benchmarck application and required application autotune yamls
	deploy_app_dependencies "$(declare -p app_array)" ${yaml_dir}
	
	sleep 10
	
	# get autotune pod log
	get_autotune_pod_log ${AUTOTUNE_SETUP_LOG}
	
	sleep 5
	
	# Validate the ids returned by each API
	validate_apis ${_test_}
}

# Test to check the uniqueness of the autotune object ids
function check_uniqueness() {
	perform_autotune_id_test ${FUNCNAME}
}

# Test to check if re-applying the autotune object without modifying yaml, changes the autotune object id
function re_apply() {
	perform_autotune_id_test ${FUNCNAME}
}

# Update and apply the application autotune yaml and compare the ids
function update_app_autotune_yaml() {
	perform_autotune_id_test ${FUNCNAME}
}

# Deploy multiple applications and check if the autotune object ids are unique
function multiple_apps() {
	declare -A app_array=([petclinic]="2" [galaxies]="2")
	perform_autotune_id_test ${FUNCNAME}
	
	# Remove benchmark applications
	for key in "${!app_array[@]}"
	do
		echo "Removing ${key} application instances"
		app_cleanup ${key}
	done
}
