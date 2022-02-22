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
##### Common routines for experiment manager #####


declare -A expected_tunable_values
declare -A actual_tunable_values
declare -A trial_metrics_value


# Get the configuration of the deployed application
function get_config() {
	app_=$1
	namespace=$2

	if [ -z ${namespace} ]; then
		namespace="default"
	fi

	app_deploy_config="${TEST_DIR}/${app_}_deploy_config.json"
	echo "Deployment name = ${app_}"
	echo "app deploy config = ${app_deploy_config}"

	echo "Fetching the updated config from the deployment ${app_} in namespace ${namespace}..."
	echo "kubectl get deployment ${app_} -o json -n ${namespace} > ${app_deploy_config}"
	kubectl get deployment ${app_} -o json -n ${namespace} > ${app_deploy_config}
	if [ $? != 0 ]; then
		echo "Getting the deployment json failed"
	fi
}


function form_em_curl_cmd {
	API=$1

	# Form the curl command based on the cluster type
        case $cluster_type in
           openshift) ;;
           minikube)
		NAMESPACE="monitoring"   
		echo "NAMESPACE = ${NAMESPACE}"
                AUTOTUNE_PORT=$(kubectl -n ${NAMESPACE} get svc autotune --no-headers -o=custom-columns=PORT:.spec.ports[*].nodePort)
                SERVER_IP=$(minikube ip)
                AUTOTUNE_URL="http://${SERVER_IP}";;
           docker) ;;
           *);;
        esac

        if [ $cluster_type == "openshift" ]; then
                em_curl_cmd="curl -s -H 'Content-Type: application/json' ${AUTOTUNE_URL}/${API}"
        else
                em_curl_cmd="curl -s -H 'Content-Type: application/json' ${AUTOTUNE_URL}:${AUTOTUNE_PORT}/${API}"
        fi
}

function list_trial_status() {
	run_id=$1

	echo "Forming the curl command to list the status of the experiment..."
	form_em_curl_cmd "listTrialStatus"
	echo "em_curl_cmd = ${em_curl_cmd}"

	cmd="${em_curl_cmd}?runId=${run_id}"
	echo "list trial status command  = ${cmd}"

	exp_status_json=$(${cmd})
	echo "exp_status_json = ${exp_status_json}"
	exp_status=$(echo ${exp_status_json} | jq '.status')

	echo "Status of ${run_id} is ${exp_status}"
}

function get_trial_status() {
	run_id=$1

	echo "Forming the curl command to get the status of the experiment..."
	form_em_curl_cmd "getTrialStatus"
	echo "em_curl_cmd = ${em_curl_cmd}"
	echo "get trial status command  = ${em_curl_cmd} -d "{\"runId\":\"${run_id}\"}""

	exp_status=$(${em_curl_cmd} -d "{\"runId\":\"${run_id}\"}")

	echo "Status of ${run_id} is ${exp_status}"
}

function post_invalid_experiment_json() {
	exp_json=$1
	echo "Forming the curl command to post the experiment..."
	form_em_curl_cmd "createExperimentTrial"

	echo "em_curl_cmd = ${em_curl_cmd}"
	em_curl_cmd="${em_curl_cmd} -d @${exp_json}"

	_post_=$(${em_curl_cmd} -w '\n%{http_code}' 2>&1)
	post_exp_result_cmd="${em_curl_cmd} -w '\n%{http_code}'"

	echo "" | tee -a "${LOG}"
	echo "Command used to post the experiment result= ${post_exp_result_cmd}" | tee -a "${LOG}"
	echo "" | tee -a "${LOG}"

	echo "${_post_}" >> "${LOG}"

	http_code=$(tail -n1 <<< "${_post_}")
	response=$(echo -e "${_post_}" | tail -2 | head -1)

	echo "http code = ${http_code} response = ${response}"
	echo "Response is ${response}" >> "${LOG}"

	echo "Posting it without -w..."
	runid=$(${em_curl_cmd})
	echo "runid = ${runid}"
}


function post_experiment_json() {
	exp_json=$1

	echo "Forming the curl command to post the experiment..."
	form_em_curl_cmd "createExperimentTrial"

	echo "em_curl_cmd = ${em_curl_cmd}"
	em_curl_cmd="${em_curl_cmd} -d @${exp_json}"

	echo "em_curl_cmd = ${em_curl_cmd}"

	runid=$(${em_curl_cmd})
	echo "runid = ${runid}"

	echo "" | tee -a "${LOG}"
	echo "Command used to post the experiment result= ${em_curl_cmd}" | tee -a "${LOG}"
	echo "" | tee -a "${LOG}"

}

function validate_exp_status() {
	actual_exp_status=$1
	expected_exp_status=$2
	test_name_=$3
	expected_behaviour="Experiment status should be ${expected_exp_status}"

	echo "----------------------------Validate Experiment Status----------------------------"
	echo ""
	
	echo "expected exp status = ${expected_exp_status} actual exp status = ${actual_exp_status}"

	if [ "${expected_exp_status}" != "${actual_exp_status}" ]; then
		failed=1
		echo "expected exp status != actual exp status failed = $failed test name = ${test_name_}"
		display_result "${expected_behaviour}" ${test_name_} ${failed}
	else
		failed=0
		echo "expected exp status == actual exp status failed = $failed test name = ${test_name_}"
		display_result "${expected_behaviour}" ${test_name_} ${failed}
	fi
}


function validate_resources() {
	for tunable in "${!expected_tunable_values[@]}"
	do
		echo "---------------------------------Validate ${tunable}--------------------------------------------"
		echo ""
		if [ "${expected_tunable_values[$tunable]}" != "${actual_tunable_values[$tunable]}" ]; then
			failed=1
		else
			failed=0
		fi
		expected_value="${tunable} = ${expected_tunable_values[$tunable]}"
		display_result "${expected_value}" ${test_name_} ${failed}
	done
}

function validate_env() {
	expected_behaviour="Number of tunables in deployment must be same as input"
	echo "----------------------------Validate number of tunables ----------------------------"
	echo ""
	if [ "${expected_tunable_no}" != "${actual_tunable_no}" ]; then
		failed=1
		display_result "${expected_behaviour}" ${test_name_} ${failed}
	else
		failed=0
		display_result "${expected_behaviour}" ${test_name_} ${failed}
		for env in "${expected_env[@]}"
		do
			echo "----------------------------Validate ${env}----------------------------"
			
			# Get the name of the tunable
			e_name=$(echo ${env} | awk '{print $1}' | tr -d ':')
			
			# Get the value of the tunable. Start from front, grep everything after ":"
			e_value="${env#*:}"
			
			search_string="${e_name}"
			
			# Returns the same value if ts present
			match_name=$(echo "${actual_env[@]:0}" | grep -o "${e_name}")
			match_value=$(echo "${actual_env[@]:0}" | grep -o "${e_value}")
			
			if [[ ! -z "${match_name}" && ! -z "${match_value}" ]]; then
				failed=0
			else
				failed=1
			fi
			display_result "${env}" ${test_name_} ${failed}
		done
	fi
}

# Validate if the expected value is present in the actual config
# input: Expected value, test name
function validate_tunable_values() {
	test_name_=$1
	input_json=$2
	deployment_name=$3

	echo "Validate tunable values..."
	echo "test_name = ${test_name_}"
	get_expected_tunable_values ${input_json}
	get_actual_tunable_values ${deployment_name}
	validate_resources
	validate_env
}

# Get the expected values from input JSON
function get_expected_tunable_values() {
	input_json=$1
	resources=".deployments[0].containers[0].config[0].spec.template.spec.container.resources"

	expected_tunable_values[mem_request]=$(cat ${input_json} | jq ${resources}.requests.memory)
	expected_tunable_values[mem_limit]=$(cat ${input_json} | jq ${resources}.limits.memory)
	expected_tunable_values[cpu_request]=$(cat ${input_json} | jq ${resources}.requests.cpu)
	expected_tunable_values[cpu_limit]=$(cat ${input_json} | jq ${resources}.limits.cpu)
	expected_env=$(cat ${input_json} | jq .deployments[0].containers[0].config[1].spec[].spec[].env | tr '\r\n' ' ' | tr -d '{}')
	IFS=',' read -r -a expected_env <<<  ${expected_env}
	expected_tunable_no="${#expected_env[@]}"


	echo "Expected tunable values..."
	echo "Memory request = ${expected_tunable_values[mem_request]}"
	echo "Memory limit = ${expected_tunable_values[mem_limit]}"
	echo "CPU request = ${expected_tunable_values[cpu_request]}"
	echo "CPU limit = ${expected_tunable_values[cpu_limit]}"

	echo "expected tunable no = ${expected_tunable_no}"

}

function get_actual_tunable_values() {
	app_=$1
	app_deploy_config="${TEST_DIR}/${app_}_deploy_config.json"
	resources_=".spec.template.spec.containers[].resources"
	actual_tunable_values[mem_request]=$(cat ${app_deploy_config} | jq ${resources_}.requests.memory)
	actual_tunable_values[mem_limit]=$(cat ${app_deploy_config} | jq ${resources_}.limits.memory)
	actual_tunable_values[cpu_request]=$(cat ${app_deploy_config} | jq ${resources_}.requests.cpu)
	actual_tunable_values[cpu_limit]=$(cat ${app_deploy_config} | jq ${resources_}.limits.cpu)

	actual_env=$(cat ${app_deploy_config} | jq .spec.template.spec.containers[].env[] | tr '\r\n' ' ' | tr -d '{}')
	IFS=',' read -r -a actual_env <<<  ${actual_env}
	actual_tunable_no="${#actual_env[@]}"
	((actual_tunable_no=actual_tunable_no/2))

	echo "Actual tunable values..."
	echo "Memory request = ${actual_tunable_values[mem_request]}"
	echo "Memory limit = ${actual_tunable_values[mem_limit]}"
	echo "CPU request = ${actual_tunable_values[cpu_request]}"
	echo "CPU limit = ${actual_tunable_values[cpu_limit]}"

	echo "actual tunable no = ${actual_tunable_no}"
}


function generate_input_json() {
	test_=$1
	json="${json_dir}/${test_}.json"
	
	# Keep a copy of the template json to perform the test
	cp -r "${input_json}" "${json}"
	
	# Update the json with specified field		
	echo "*** find = $find replace = $replace"
	sed -i "s/${find}/${replace}/g" ${json}
}


function validate_post_result() {
	__test_name__=$1
	if [[ "${test}" == valid* ]]; then
		expected_result_="200"
		expected_behaviour="RESPONSE_CODE = 200 OK"
	else
		expected_result_="^4[0-9][0-9]"
		expected_behaviour="RESPONSE_CODE = 4XX BAD REQUEST"
	fi

	actual_result="${http_code}"
	
	compare_result ${__test_name__} ${expected_result_} "${expected_behaviour}"
}
