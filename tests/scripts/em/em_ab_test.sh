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

function validate_em_ab_workflow() {
	app="tfb-qrh"
	instances=1
	MAX_LOOP=3

	test_name_=${FUNCNAME}
	input_json="${TEST_DIR_}/resources/em_input_json/ABTesting.json"

	echo  "EM input json = ${input_json}"

	# Deploy the application with the specified number of instances	
	if [ ${skip_setup} -eq 0 ]; then
		deploy_app ${APP_REPO} ${app} ${instances}
	else
		echo "Skipping application deployment..."
	fi

	kubectl get pods
	
	# Post the input json to /createExperimentTrial API
	post_experiment_json "${input_json}"

	sleep 5

	deployment_name=$(cat ${input_json} | jq '.[0].resource.deployment_name')
	experiment_name=$(cat ${input_json} | jq '.[0].experiment_name')

	deployment_name=$(echo ${deployment_name} | sed -e "s/\"//g")

	echo ""
	echo "Status of the deployment is ${exp_status}"
	# Get the config of the new deployment
	get_config "${deployment_name}"

	# Validate the metrics for trial A & trial B
	trial_num="A"
	echo ""
	echo "Validating trial A..."
	validate_exp_trial_result "${experiment_name}" "${trial_num}"

	trial_num="B"
	echo ""
	echo "Validating trial B..."
	validate_exp_trial_result "${experiment_name}" "${trial_num}"

	# Cleanup the deployed application
	if [ ${skip_setup} -eq 0 ]; then
		app_cleanup ${app}
	fi
	echo "----------------------------------------------------------------------------------------------"
}

