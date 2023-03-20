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
##### Script to validate the kruize layer object id #####

# Get the absolute path of current directory
CURRENT_DIR="$(dirname "$(realpath "$0")")"

pushd ${CURRENT_DIR}/../.. >> /dev/null
autotune_dir="${PWD}"

# Source the common functions scripts
. ${CURRENT_DIR}/da/constants/id_constants.sh

# validate kruize layer object id for all APIs
function kruize_layer_id_tests() {
	start_time=$(get_date)
	FAILED_CASES=()
	TESTS_FAILED=0
	TESTS_PASSED=0
	TESTS=0
	CONFIG_YAML_DIR="${autotune_dir}/manifests/autotune-configs"
	kruize_layer_id_tests=( "check_uniqueness_test" "re_apply_config_test" "update_layer_config_yaml_test" "new_layer_config_test")

	((TOTAL_TEST_SUITES++))

	if [ ! -z "${testcase}" ]; then
		check_test_case "kruize_layer_id"
	fi

	# create the result directory for given testsuite
	echo ""
	TEST_SUITE_DIR="${RESULTS}/kruize_layer_id_tests"
	mkdir -p ${TEST_SUITE_DIR}

	echo ""
	echo "**************************** Executing test suite ${FUNCNAME} *************************"
	echo ""

	# If testcase is not specified run all tests
	if [ -z "${testcase}" ]; then
		testtorun=("${kruize_layer_id_tests[@]}")
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

	# Print the testsuite summary
	testsuitesummary ${FUNCNAME} ${elapsed_time} ${FAILED_CASES}
}

# Get the kruize layer id form the APIs
# input: json file containing the API result and the test name
# output: Get the kruize layer object ids from the API result
function get_config_id() {
	_json_=$1
	test_name=$2
	declare -A layer_config_id
	length=$(cat ${_json_} | jq '. | length')
	while [ "${length}" -ne 0 ]
	do
		((length--))
		layer_config_id[test_name]+="$(cat ${_json_} | jq .[${length}].layer_id) "
	done
	config_id="${layer_config_id[test_name]}"

	# Convert config_id into an array
	IFS=' ' read -r -a config_id <<<  ${config_id}
}

# query the api and get the result
# input: The API which has to be queried
# output: Query the API and get the autotune object ids
function query_kruize_tunables_api() {
	query_api=$1
	slo_class="response_time"
	layer="container"

	echo " " | tee -a ${LOG}
	case "${query_api}" in
		get_list_kruize_tunables_json_slo_layer)
			# listKruizeTunables for specific slo_class and layer
			get_list_kruize_tunables_json ${slo_class} ${layer}
			;;
		get_list_kruize_tunables_json_slo)
			# listKruizeTunables for specific slo_class
			get_list_kruize_tunables_json ${slo_class}
			;;
		get_list_kruize_tunables_json)
			# listKruizeTunables for all layers
			get_list_kruize_tunables_json
			;;
	esac
	get_config_id ${json_file} ${query_api}
	echo " " | tee -a ${LOG}
}

# check if the old id is matching with the new id after re-apply
# output: set the flag to 1 if the ids are not same
function re_apply_test_() {
	flag=0
	declare -A old_layer_config_id
	old_layer_config_id[test_name]+="${config_id[@]}"
	echo -n "Deleting the application kruize layer objects..." | tee -a ${LOG}
	echo " " >> ${LOG}
	kubectl delete -f ${CONFIG_YAML_DIR} -n ${NAMESPACE} >> ${LOG}
	echo "done" | tee -a ${LOG}

	# sleep for few seconds to reduce the ambiguity
	sleep 2

	echo -n "Re-applying the kruize layer yaml..." | tee -a ${LOG}
	echo " " >> ${LOG}
	kubectl apply -f ${CONFIG_YAML_DIR} -n ${NAMESPACE} >> ${LOG}
	echo "done"

	# sleep for few seconds to reduce the ambiguity
	sleep 2

	query_kruize_tunables_api ${test_name}

	old_id_=("${old_layer_config_id[test_name]}")
	IFS=' ' read -r -a old_id_ <<<  ${old_id_}
	new_id_=("${config_id[@]}")

	match_ids

	if [ "${matched_count}" -ne "${#old_id_[@]}" ]; then
		flag=1
		if [ "${matched_count}" -eq "$(expr ${#old_id_[@]} - 1 )" ]; then
			echo "Kruize layer object id is not same as previous for single layer object"
		else
			echo "Kruize layer object ids are not same as previous for multiple layer object"
		fi
	fi
}

# update the application kruize layer yaml and check if the ids have changed
# output: set the flag to 1 if the ids are not unique
function update_layer_config() {
	flag=0
	declare -A old_layer_config_id
	old_layer_config_id[test_name]+="${config_id[@]} "

	# copy the config yamls into the test directory
	test_config_yamls_dir="${config_yaml}/${test_name}"
	mkdir -p ${test_config_yamls_dir}
	cp ${config_yaml}/*.yaml ${test_config_yamls_dir}

	# Update and apply the application autotune yaml
	test_yaml_files=$(ls ${test_config_yamls_dir} | tr "\n" " ")
	IFS=' ' read -r -a test_yaml_files <<<  ${test_yaml_files}

	for test_yaml in "${test_yaml_files[@]}"
	do

		test_layer_config="${test_config_yamls_dir}/${test_yaml}"

		if [[ ${test_layer_config} =~ "container" ]]; then
			find_upper_bound="3.0"
			replace_upper_bound="4.0"
		elif [[ ${test_layer_config} =~ "hotspot" ]]; then
			find_upper_bound="50"
			replace_upper_bound="51"
		elif [[ ${test_layer_config} =~ "quarkus" ]]; then
			find_upper_bound="10"
			replace_upper_bound="11"
		fi

		echo "Updating upper bound and lower bound in ${test_layer_config}"
		sed -i 's/upper_bound: '\'${find_upper_bound}\''/upper_bound: '\'${replace_upper_bound}\''/g' ${test_layer_config}

		echo "Applying kruize layer yaml ${test_layer_config}..." | tee -a ${LOG}
		kubectl apply -f ${test_layer_config} -n ${NAMESPACE} | tee -a ${LOG}
		echo "done" | tee -a ${LOG}
	done

	# sleep for few seconds to reduce the ambiguity
	sleep 2

	query_kruize_tunables_api ${test_name}

	old_id_=("${old_layer_config_id[test_name]}")
	IFS=' ' read -r -a old_id_ <<<  ${old_id_}
	new_id_=("${config_id[@]}")

	echo "old ids = ${old_id_[@]}"
	echo "new ids = ${new_id_[@]}"

	# Compare the old id with the new id
	match_ids

	if [ "${matched_count}" -gt "0" ]; then
		flag=1
		if [ "${matched_count}" -eq "1" ]; then
			echo "Kruize layer object id is same as previous for single layer object"
		else
			echo "Kruize layer object ids are same as previous for multiple layer object"
		fi
	fi
}

# Create and apply new layer config yaml
function new_layer_config() {
	flag=0
	declare -A old_layer_config_id
	old_layer_config_id[test_name]+="${config_id[@]} "
	old_layer_config_id_count="${#config_id[@]}"

	echo "Applying new kruize layer yaml ${new_config_yaml}..."| tee -a ${LOG}
	kubectl apply -f ${new_config_yaml} -n ${NAMESPACE} | tee -a ${LOG}
	echo "done" | tee -a ${LOG}

	# sleep for few seconds to reduce the ambiguity
	sleep 2

	# query the listKruizeTunables API
	query_kruize_tunables_api ${test_name}

	# Check if the new layer config id has been added, If so check if all the ids are unique
	if [ "${old_layer_config_id_count}" -lt "${#config_id[@]}" ]; then
		uniqueness_test "${config_id[@]}"
	else
		flag=1
	fi
}

# validate the autotune object ids based on the test
# input: id test name
# output: perform the test based on the id test name
function validate_layer_config_id() {
	config_id=$1
	case "${config_id_test_name}" in
		check_uniqueness_test)
			uniqueness_test "${config_id[@]}"
			;;
		re_apply_config_test)
			re_apply_test_
			;;
		update_layer_config_yaml_test)
			update_layer_config
			;;
		new_layer_config_test)
			new_layer_config
			;;
	esac
	display_result "${layer_config_id_expected_behaviour[$config_id_test_name]}" "${config_id_test_name}" ${flag}
	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"| tee -a ${LOG}
}

# query the APIs and store the ids for further tests
# input: id test name
# output: query the APIs, get the autotune object ids and validate those autotune object ids based on the test
function validate_kruize_tunables_api() {
	config_id_test_name=$1

	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"| tee -a ${LOG}
	for layer_config_id_test in "${layer_config_id[@]}"
	do
		if [[ "${config_id_test_name}" == "${kruize_layer_id_tests[3]}" && "${layer_config_id_test}" == "${layer_config_id[0]}" ]]; then
			:
		else
			echo ""
			echo "**********************************************************************************************"
			echo "					${layer_config_id_test}"
			echo "**********************************************************************************************"
			LOG_DIR="${TEST_DIR}/${layer_config_id_test}"
			mkdir -p ${LOG_DIR}
			query_kruize_tunables_api ${layer_config_id_test}
			validate_layer_config_id "${config_id}"
			if [ "${config_id_test_name}" == "${kruize_layer_id_tests[3]}" ]; then
				kubectl delete -f ${new_config_yaml} -n ${NAMESPACE} >> ${LOG}
			fi

			echo "Deleting kruize layer yaml ${test_layer_config}..." | tee -a ${LOG}
			kubectl delete -f ${config_yaml} -n ${NAMESPACE} | tee -a ${LOG}
			echo "done" | tee -a ${LOG}

			echo "Applying kruize layer yaml ${test_layer_config}..." | tee -a ${LOG}
			kubectl apply -f ${config_yaml} -n ${NAMESPACE} | tee -a ${LOG}
			echo "done" | tee -a ${LOG}
		fi
	done
}

# Perform the autotune id tests
# input: test name
# output: deploy the autotune and required benchmarks, query the APIs and validate the autotune object ids
function perform_layer_config_id_test() {
	test_=$1

	# sleep for few seconds to reduce the ambiguity
	sleep 10
	TEST_DIR="${TEST_SUITE_DIR}/${test_}"
	if [ ! -d "${TEST_DIR}" ]; then
		mkdir ${TEST_DIR}
	fi
	AUTOTUNE_SETUP_LOG="${TEST_DIR}/setup.log"
	AUTOTUNE_LOG="${TEST_DIR}/${test_}_autotune.log"
	LOG="${TEST_SUITE_DIR}/${test_}.log"

	echo ""
	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" | tee -a ${LOG}
	echo "                    Running Test ${test_}" | tee -a ${LOG}
	echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"| tee -a ${LOG}

	echo " " | tee -a ${LOG}
	echo "Test description: ${layer_config_id_expected_behaviour[$test_]}" | tee -a ${LOG}
	echo " " | tee -a ${LOG}

	# create the autotune setup
	echo "Setting up autotune..." | tee -a ${LOG}
	setup >> ${AUTOTUNE_SETUP_LOG} 2>&1
	echo "Setting up autotune...Done" | tee -a ${LOG}

	# Giving a sleep for autotune pod to be up and running
	sleep 10

	# form the curl command based on the cluster type
	form_curl_cmd

	# get autotune pod log
	get_autotune_pod_log ${AUTOTUNE_SETUP_LOG}

	sleep 5

	# Validate the ids returned by listKruizeTunables API
	validate_kruize_tunables_api ${test_}
}

# Test to check the uniqueness of the kruize layer object ids
function check_uniqueness_test() {
	perform_layer_config_id_test ${FUNCNAME}
}

# Re-apply the layer config without modifying yaml and check if both the ids are same
function re_apply_config_test() {
	perform_layer_config_id_test ${FUNCNAME}
}

# Update and apply the layer config yaml and compare the ids
function update_layer_config_yaml_test() {
	# copy the config yamls
	config_yaml="${TEST_SUITE_DIR}/${FUNCNAME}/yamls"
	mkdir -p ${config_yaml}
	cp ${CONFIG_YAML_DIR}/* ${config_yaml}/ ; rm -r ${config_yaml}/layer-config.yaml_template

	perform_layer_config_id_test ${FUNCNAME}
}

# Apply new layer config and validate the id
function new_layer_config_test() {
	new_config_yaml="${autotune_dir}/tests/autotune_test_yamls/manifests/da/kruize_layer_id_test_yaml/new_layer_config_test/openj9-config.yaml"
	perform_layer_config_id_test ${FUNCNAME}
}
