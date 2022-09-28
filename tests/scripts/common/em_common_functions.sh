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
		exit -1
	fi

	if [ -z "${app_deploy_config}" ]; then
		echo "Deployment json is empty! New deployment has failed!"
		exit -1
	fi
}


function form_em_curl_cmd {
	API=$1

	# Form the curl command based on the cluster type
        case $cluster_type in
           openshift)
		NAMESPACE="openshift-tuning"
		echo "NAMESPACE = ${NAMESPACE}"
		SERVER_IP=$(${kubectl_cmd} get pods -l=app=autotune -o wide -n ${NAMESPACE} -o=custom-columns=NODE:.spec.nodeName --no-headers)
                AUTOTUNE_PORT=$(oc -n ${NAMESPACE} get svc autotune --no-headers -o=custom-columns=PORT:.spec.ports[*].nodePort)
	 	;;
           minikube)
		NAMESPACE="monitoring"   
		echo "NAMESPACE = ${NAMESPACE}"
                AUTOTUNE_PORT=$(kubectl -n ${NAMESPACE} get svc autotune --no-headers -o=custom-columns=PORT:.spec.ports[*].nodePort)
                SERVER_IP=$(minikube ip)
                AUTOTUNE_URL="http://${SERVER_IP}"
		;;
           docker) ;;
           *);;
        esac

	AUTOTUNE_URL="http://${SERVER_IP}"
	echo "AUTOTUNE_URL = $AUTOTUNE_URL"
	em_curl_cmd="curl -s -H 'Content-Type: application/json' ${AUTOTUNE_URL}:${AUTOTUNE_PORT}/${API}"
}

function list_trial_status() {
	experiment_name=$1
	trial_num=$2

	echo "Forming the curl command to list the status of the experiment..."
	form_em_curl_cmd "listTrialStatus"
	echo "em_curl_cmd = ${em_curl_cmd}"

	cmd="${em_curl_cmd}"
	echo "list trial status command  = ${cmd}"

	exp_status_json=$(${cmd})

	exp_status=$(echo ${exp_status_json} | jq '.'${experiment_name}'.'${trial_num}'.status')

	echo "Status of ${experiment_name} is ${exp_status}"
}

function start_load() {
        app_name=$1
        num_instances=$2
        MAX_LOOP=$3

        set_app_folder ${app_name}
        echo
        echo "Starting ${app_name} workload..."
        # Invoke the jmeter load script
	if [ ${app_name} == "tfb-qrh" ]; then
		${APP_REPO}/${APP_FOLDER}/scripts/tfb-load.sh --clustertype=${cluster_type} -i ${num_instances} &
	fi
}


function stop_load() {
        app_name=$1
	if [ ${app_name} == "tfb-qrh" ]; then
	        ps -ef | grep tfb-load.sh | grep -v grep | awk '{print $2}' | xargs kill -9
	fi
}

function post_experiment_json() {
	exp_json=$1

	echo "Forming the curl command to post the experiment..."
	form_em_curl_cmd "createExperimentTrial"

	echo "em_curl_cmd = ${em_curl_cmd}"
	em_curl_cmd="${em_curl_cmd} -d @${exp_json}"

	echo "em_curl_cmd = ${em_curl_cmd}"

	exp_status_json=$(${em_curl_cmd})
	echo "Post experiment status = $exp_status_json"

	echo "" | tee -a "${LOG}"
	echo "Command used to post the experiment result= ${em_curl_cmd}" | tee -a "${LOG}"
	echo "" | tee -a "${LOG}"

	# Add validation to check the status of the post experiment

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
		display_result "${expected_behaviour}" "${test_name_}" "${failed}"
	else
		failed=0
		display_result "${expected_behaviour}" "${test_name_}" "${failed}"
		for (( i=0; i<"${expected_tunable_no}"; i++ ))
		#for env in "${expected_env[@]}"
		do
			echo "----------------------------Validate Env tunables ----------------------------"

			# Get the name of the tunable
			#e_name=$(echo ${env} | awk '{print $1}' | tr -d ':')
			e_name=$(echo ${expected_env} | jq '.['${i}'].name')

			echo "e_name = $e_name"
			
			# Get the value of the tunable. Start from front, grep everything after ":"
			#e_value="${env#*:}"
			e_value=$(echo ${expected_env} | jq '.['${i}'].value')
			echo "e_value = $e_value"
			
			search_string="${e_name}"
			
			# Returns the same value if its present
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
	trial_num=$3
	deployment_name=$4

	echo "Validate tunable values..."
	echo "test_name = ${test_name_}"
	get_expected_tunable_values "${input_json}" "${trial_num}"
	get_actual_tunable_values "${deployment_name}"
	validate_resources
	validate_env
}

# Get the expected values from input JSON
function get_expected_tunable_values() {
        input_json=$1
	trial_num=$2

	echo "Fetching expected tunable values from the input json..."
        config=".[0].trials.${trial_num}.config"
	echo "config = $config"

	requests=$(cat ${input_json} | jq ''${config}'.requests')
	echo "requests = $requests"
	if [ ${requests} != null ]; then
	        expected_tunable_values[mem_request]=$(cat ${input_json} | jq ''${config}'.requests.memory.amount')
	        expected_tunable_values[cpu_request]=$(cat ${input_json} | jq ''${config}'.requests.cpu.amount')

		expected_tunable_values[cpu_request]=$(echo ${expected_tunable_values[cpu_request]} | sed 's/"//g')
        	echo "CPU request = ${expected_tunable_values[cpu_request]}"
		# Convert the expected cpu values to millicores and in the format as in the new deployment
		const="1000"
		expected_tunable_values[cpu_request]=$(bc <<< "${const} * ${expected_tunable_values[cpu_request]}")
		expected_tunable_values[cpu_request]=\""${expected_tunable_values[cpu_request]%.*}m"\"

	        echo "Memory request = ${expected_tunable_values[mem_request]}"
        	echo "CPU request = ${expected_tunable_values[cpu_request]}"
	fi

	limits=$(cat ${input_json} | jq ''${config}'.limits')
	echo "limits = $limits"
	if [ ${limits} != null ]; then
        	expected_tunable_values[mem_limit]=$(cat ${input_json} | jq ''${config}'.limits.memory.amount')
        	expected_tunable_values[cpu_limit]=$(cat ${input_json} | jq ''${config}'.limits.cpu.amount')

		expected_tunable_values[cpu_limit]=$(echo ${expected_tunable_values[cpu_limit]} | sed 's/"//g')
        	echo "CPU limit = ${expected_tunable_values[cpu_limit]}"
		# Convert the expected cpu values to millicores and in the format as in the new deployment
		const="1000"
		expected_tunable_values[cpu_request]=$(bc <<< "${const} * ${expected_tunable_values[cpu_request]}")
		expected_tunable_values[cpu_limit]=$(bc <<< "${const} * ${expected_tunable_values[cpu_limit]}")
		expected_tunable_values[cpu_limit]=\""${expected_tunable_values[cpu_limit]%.*}m"\"

        	echo "Memory limit = ${expected_tunable_values[mem_limit]}"
        	echo "CPU limit = ${expected_tunable_values[cpu_limit]}"
	fi


	expected_env=$(cat ${input_json} | jq ''${config}'.env')
	echo "expected_env = $expected_env"
	if [ ${expected_env} != null ]; then
		expected_tunable_no=$(cat ${input_json} | jq ${config}.env | jq '. | length')

	        echo "expected env tunables count = ${expected_tunable_no}"
        	echo "expected envs = ${expected_env[@]}"
	fi

	echo "Fetching expected tunable values from the input json...done"
}

function get_actual_tunable_values() {
	app_=$1
	app_deploy_config="${TEST_DIR}/${app_}_deploy_config.json"

	echo "Fetching actual tunable values from the new deployment..."

	resources_=".spec.template.spec.containers[].resources"
	requests=$(cat ${app_deploy_config} | jq ${resources_}.requests)
	echo "requests = $requests"
	if [ ${requests} != null ]; then
		actual_tunable_values[mem_request]=$(cat ${app_deploy_config} | jq ${resources_}.requests.memory)
		actual_tunable_values[cpu_request]=$(cat ${app_deploy_config} | jq ${resources_}.requests.cpu)
		echo "Memory request = ${actual_tunable_values[mem_request]}"
		echo "CPU request = ${actual_tunable_values[cpu_request]}"
	fi


	limits=$(cat ${app_deploy_config} | jq ${resources_}.limits)
	echo "limits = $limits"
	if [ ${limits} != null ]; then
		actual_tunable_values[mem_limit]=$(cat ${app_deploy_config} | jq ${resources_}.limits.memory)
		actual_tunable_values[cpu_limit]=$(cat ${app_deploy_config} | jq ${resources_}.limits.cpu)
		echo "Memory limit = ${actual_tunable_values[mem_limit]}"
		echo "CPU limit = ${actual_tunable_values[cpu_limit]}"
	fi

	actual_env=$(cat ${app_deploy_config} | jq .spec.template.spec.containers[].env )
	if [ ${actual_env} != null ]; then
		actual_tunable_no="${#actual_env[@]}"

		actual_tunable_no=$(cat ${actual_env} | jq '. | length')

		actual_tunable_no=$(cat ${app_deploy_config} | jq .spec.template.spec.containers[].env | jq '. | length')
	#	((actual_tunable_no=actual_tunable_no/2))

		echo "actual env tunables count = ${actual_tunable_no}"
		echo "actual env tunables = ${actual_env[@]}"
	fi

	echo "Fetching actual tunable values from the new deployment...done"
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

function get_actual_trial_details() {
	actual_trial_details[experiment_id]=$(cat ${input_json} | jq .[].experiment_id)
#	actual_trial_details[application_name]=$(cat ${input_json} | jq .[].application_name)
	actual_trial_details[trial_num]=$(cat ${input_json} | jq .[].trials[].trial_num)
	actual_trial_details[trial_run]=$(cat ${input_json} | jq .[].trials[].trial_run)
	actual_trial_details[trial_measurement_time]=$(cat ${input_json} | jq .[].trials[].trial_measurement_time)
	actual_trial_details[deployment_name]=$(cat ${input_json} | jq .[].trials[].training.deployment_name)
	
}

function get_expected_trial_details() {
	expected_trial_details[experiment_id]=$(cat ${result} | jq .trials.experiment_id)
	expected_trial_details[trial_num]=$(cat ${result} | jq .trials.trial_num)
	expected_trial_details[trial_run]=$(cat ${result} | jq .trials.trial_run)
	expected_trial_details[trial_measurement_time]=$(cat ${result} | jq .trials.trial_measurement_time)
	expected_trial_details[deployment_name]=$(cat ${result} | jq .trials.deployment_name)
}

function validate_trial_details() {
	trial_details=("experiment_name" "status" "deployment_name")
	declare -A expected_trial_details
	declare -A actual_trial_details
	
	get_actual_trial_details
	get_expected_trial_details
	for trial in "${trial_details[@]}"
	do
		if [ "${actual_trial_details[$trial]}" != "${expected_trial_details[$trial]}" ]; then
			echo "${trial} did not match"
			failed=1
			break
		else
			failed=0
		fi 
	done
	expected_behaviour="Trial details of the result must be same as input"
	display_result "${expected_behaviour}" ${FUNCNAME} ${failed}
}


function get_exp_result() {
	exp_name=$1
	trial_num=$2
	result=$3

	echo "SERVER_IP = ${SERVER_IP}"
	echo "AUTOTUNE_PORT = ${AUTOTUNE_PORT}"

	curl="curl -s -H 'Accept: application/json'"

	echo "exp_name=$exp_name trial_num = $trial_num"
	exp_name=$(echo ${exp_name} | sed 's/\"//g')
	echo "exp_name=$exp_name trial_num = $trial_num"
	echo "curl cmd - ${curl} 'http://${SERVER_IP}:${AUTOTUNE_PORT}/listTrialStatus?experiment_name=${exp_name}&trial_number=${trial_num}'"
	get_result=$(${curl} 'http://'${SERVER_IP}':'${AUTOTUNE_PORT}'/listTrialStatus?experiment_name='${exp_name}'&trial_number='${trial_num}'')
	
	echo "" 
	echo "get_result - ${get_result}" 
	echo "${get_result}" > ${result}
	
}

function validate_percentile_values() {
        test_name=$1

        failed=0
        percentile=('"95p"' '"97p"' '"99p"' '"99.9p"' '"99.99p"' '"99.999p"' '"100p"')
        p_count=0

	echo "Validating percentile values..."
        # Store the percentile values in an array in the given order
        for p in "${percentile[@]}"
        do
		echo "trial metrics value[$p] - ${trial_metrics_value[${p}]}"
                percentile_values[$p_count]="${trial_metrics_value[${p}]}"
                ((p_count++))
        done

        # Check if the percentile values are in ascending order
        for ((i=0; i<${p_count}; i++))
        do
                echo "values ${percentile_values[$i]}"
                for ((j=i+1; j<${p_count}; j++))
                do
                        echo "values ${percentile_values[$i]} compare_values ${percentile_values[$j]}"
                        if [[ ${percentile_values[${i}]} > ${percentile_values[${j}]} ]]; then
                                failed=1
                                break
                        fi
                done
                if [ "${failed}" -eq 1 ]; then
                        break
                fi
        done
        expected_behaviour="Values for percentiles has to be in ascending order for 95, 99, ... 99.9999, 100"
        display_result "${expected_behaviour}" ${test_name} ${failed}
}

function validate_mean() {
        test_name=$1
	echo "Validating mean values..."

	echo "trial_metrics_value["mean"] = ${trial_metrics_value['"mean"']}"
	
	if [ 1 -eq "$(echo "${trial_metrics_value['"mean"']} > 0 " | bc)" ]; then
                failed=0
        else
                failed=1
        	expected_behaviour="Mean has to be greater than 0"
	        display_result "${expected_behaviour}" ${test_name} ${failed}
        fi
}

function validate_pod_metrics() {
	pod_metric=$1
	echo "Validating pod metrics"

	pod_metric_name=$(echo ${pod_metric} | jq '.name')
	echo "pod metric name = ${pod_metric_name}"

	pod_metric_datasource=$(echo ${pod_metric} | jq '.datasource')
	echo "pod metric datasource = ${pod_metric_datasource}"

	validate_metrics "${pod_metric}"
}


function validate_metrics() {
	trial_metric=$1

	# For each metric store its corresponding values and validate the metrics
	for metric in $(echo ${trial_metric} | jq '.summary_results.percentile_info | keys | .[]')
	do
		value=$(echo ${trial_metric} | jq '.summary_results.percentile_info['${metric}']')
		echo "value = $value metric = $metric"
		trial_metrics_value[${metric}]="${value}"
		echo "trial_metrics_value = ${trial_metrics_value[$metric]}"
	done

	for metric in $(echo ${trial_metric} | jq '.summary_results.general_info | keys | .[]')
	do
		value=$(echo ${trial_metric} | jq '.summary_results.general_info['${metric}']')
		trial_metrics_value[${metric}]="${value}"
		echo "trial_metrics_value = ${trial_metrics_value[$metric]}"
	done
	
#	validate_metric_details ${FUNCNAME}

	# Validate summary results - percentile info
	validate_percentile_values ${FUNCNAME}

	# Validate summary results - general info
	validate_mean ${FUNCNAME}
}

function validate_deployment() {
	deployment=$1
	trial_num=$2

	echo "Validating deployment"
	name=$(echo ${deployment} | jq '.deployment_name')

	echo "deployment name = $name"

	# Validate pod metrics
	pod_metrics=$(echo ${deployment} | jq '.pod_metrics')
	pod_metrics_count=$(echo ${deployment} | jq '.pod_metrics | length')

#	echo "pod_metrics = ${pod_metrics}"
	echo "pod_metrics_count = ${pod_metrics_count}"

	for (( l=0; l<${pod_metrics_count}; l++ ))
	do
		pod_metric=$(echo ${pod_metrics} | jq '.['${l}']')
		echo "pod_metric = $pod_metric"
		validate_pod_metrics "${pod_metric}"
	done

	# Validate container metrics
	containers=$(echo ${deployment} | jq '.containers')
	containers_count=$(echo ${deployment} | jq '.containers | length')
	echo "containers = ${containers}"
	echo "containers_count = ${containers_count}"
	
	for (( c=0; c<${containers_count}; c++ ))
	do
		container=$(echo ${containers} | jq '.['${c}']')

		# Validate container details
		container_name=$(echo ${container} | jq '.container_name')
		image_name=$(echo ${container} | jq '.image_name')

		echo "container_name = ${container_name}"
		echo "image_name = ${image_name}"

		# Get the container name & image name from the input json		
		trial_num="\"${trial_num}\""
		expected_container_name=$(cat ${input_json} | jq '.[].trials.'"${trial_num}"'.config.container_name')
		expected_image_name=$(cat ${input_json} | jq '.[].trials.'"${trial_num}"'.config.image')

		echo "expected_container_name = ${expected_container_name}"
		echo "expected_image_name = ${expected_image_name}"

		if [[ "${expected_container_name}" != "${container_name}" || "${expected_image_name}" != "${image_name}" ]]; then
			failed=1
			echo "Failed - Container name or image name in the new deployment doesn't match the experiment input trial"
		fi

		container_metrics=$(echo ${container} | jq '.container_metrics')
		container_metrics_count=$(echo ${container} | jq '.container_metrics | length')

		echo "container_metrics = ${container_metrics}"
		echo "container_metrics_count = ${container_metrics_count}"

		for (( l=0; l<${container_metrics_count}; l++ ))
		do
			container_metric=$(echo ${container_metrics} | jq '.['${l}']')
			echo "container_metric = $container_metric"

			# Validate datasource, name
			container_metric_name=$(echo ${container_metric} | jq '.name')
			echo "container metric name = ${container_metric_name}"
	
			container_metric_datasource=$(echo ${container_metric} | jq '.datasource')
			echo "container metric datasource = ${container_metric_datasource}"

			validate_metrics "${container_metric}"
		done
	done
}


function validate_exp_trial_result() {
	experiment_name=$1
	trial_num=$2

	result="${TEST_DIR}/trial_result.json"
	get_exp_result "${experiment_name}" "${trial_num}" "${result}"

#       validate_trial_details

        # Needs to be updated for all deployments
        deployments=$(cat ${result} | jq '.'${experiment_name}'.'${trial_num}'.deployments')
        deployments_count=$(cat ${result} | jq '.'${experiment_name}'.'${trial_num}'.deployments | length')

#        echo "deployments = $deployments"
        echo "deployments_count = $deployments_count"

	for (( i=0; i < ${deployments_count}; i++ ))
	do
		deployment=$(echo ${deployments} | jq '.['${i}']')
#		echo "deployment= $deployment"
		validate_deployment "${deployment}" "${trial_num}"
	done
}

