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
##### EM A/B validation tests for experiment manager #####


# EM A/B validation test for Experiment manager (EM). This test deploys an application and creates an experiment with the provided config
# and validates the rolling update config against the provided config. It also validates which version of the application performs better

function validate_em_genperfexp_test() {
	app="tfb-qrh"
	instances=1
	MAX_LOOP=3

	test_name_=${FUNCNAME}
	input_json="${TEST_DIR_}/resources/em_input_json/GeneralPerfExp.json"
	echo  "************** input json = ${input_json}"

	# Deploy the application with the specified number of instances	
	deploy_app ${APP_REPO} ${app} ${instances}
	
	# Sleep for sometime for application pods to be up
	sleep 5

	# Post the input json to /createExperimentTrial API
	post_experiment_json "${input_json}"

	sleep 10
	deployment_name=$(cat ${input_json} | jq '.[0].resource.deployment_name')
	experiment_name=$(cat ${input_json} | jq '.[0].experiment_name')

	echo "deployment_name = ${deployment_name}"
	echo "experiment_name = ${experiment_name}"
	deployment_name=$(echo ${deployment_name} | sed -e "s/\"//g")

	echo "Status of the deployment is ${exp_status}"
	# Get the config of the new deployment
	get_config "${deployment_name}"

	# Validate the tunable values
	trial_num="0"
	validate_tunable_values "${test_name_}" "${input_json}" "${trial_num}" "${deployment_name}"

	# Obtain the status of the experiment
	list_trial_status "${experiment_name}" "${trial_num}"
	expected_exp_status="\"WAITING_FOR_LOAD\""

#	timeout 120s bash -c 'while [ ${exp_status} != ${expected_exp_status} ]; do sleep 5;  list_trial_status "${experiment_name}";  done'

	counter=1
	while [ "${exp_status}" != "${expected_exp_status}" ]
	do
		sleep 1
		list_trial_status "${experiment_name}" "${trial_num}"
		counter=$((counter+1))

		if [ ${counter} == "5" ]; then
			echo "Status of the experiment is not as expected (WAITING_FOR_LOAD)!"
			break
		fi
	done

	# Start the load
	start_load "${app}" "${instances}" "${MAX_LOOP}"

	# Check if the metrics has been gathered
	expected_exp_status="\"COMPLETED\""
	counter=1
	while [ ${exp_status} != ${expected_exp_status} ]
       	do
		sleep 60
		list_trial_status "${experiment_name}" "${trial_num}"
		$(( counter++ ))

		if [ ${counter} == "20" ]; then
			echo "Status of the experiment is not as expected (COMPLETED)!"
			exit 1
		fi
	done

	echo "Status of the deployment is ${exp_status}"

	# Stop the load
	echo "Stopping load..."
	stop_load "${app}" 

	# Validate the metrics  
	validate_exp_trial_result "${experiment_name}" "${trial_num}"

	# Cleanup the deployed application
#	app_cleanup ${app}
	echo "----------------------------------------------------------------------------------------------"
}

