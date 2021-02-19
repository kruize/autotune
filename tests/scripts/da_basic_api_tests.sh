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
##### Script for testing the Autotune APIs #####

# Get the absolute path of current directory
CURRENT_DIR="$(dirname "$(realpath "$0")")"
pushd ${CURRENT_DIR}/.. >> setup.log

# Path to the directory containing yaml files
MANIFESTS="${PWD}/autotune_test_yamls/manifests"
api_autotune_yaml="api_test_yamls/basic_api_tests"
module="da"
YAML_PATH=${MANIFESTS}/${module}/${api_autotune_yaml}

# Main function that calls all the other tests to test the Autotune APIs 
function basic_api_tests() {
	FAILED_CASES=()
	TESTS_FAILED=0
	TESTS_PASSED=0
	TESTS=0

	# check if the test case is supported
	basic_api_tests=("listapplayer" "listapptunables" "listapplications" "searchspace" "list_autotune_tunables")

	if [ ! -z "${testcase}" ]; then
		check_test_case "basic_api"
	fi

	# create the result directory for given testsuite
	echo ""
	TEST_SUITE_DIR="${RESULTS}/basic_api_tests"
	AUTOTUNE_JSONS_DIR="${TEST_SUITE_DIR}/autotune_jsons"
	AUTOTUNE_CONFIG_JSONS_DIR="${TEST_SUITE_DIR}/autotuneconfig_jsons"

	mkdir -p ${TEST_SUITE_DIR}
	mkdir -p ${AUTOTUNE_JSONS_DIR}
	mkdir -p ${AUTOTUNE_CONFIG_JSONS_DIR}
	echo ""
	((TOTAL_TEST_SUITES++))
	
	declare -A layer_configs=([petclinic-deployment-0]="container" [petclinic-deployment-1]="container" [petclinic-deployment-2]="container")
	deployments=("petclinic-deployment-0" "petclinic-deployment-1" "petclinic-deployment-2")
	autotune_names=("petclinic-autotune-0"  "petclinic-autotune-1" "petclinic-autotune-2")

	
	echo ""
	echo "******************* Executing test suite ${FUNCNAME} ****************"
	echo ""
	
	# create the autotune setup
	echo "Setting up autotune..."
	setup >> ${TEST_SUITE_DIR}/setup.log 2>&1
	echo "Setting up autotune...Done"
	
	# Giving a sleep for autotune pod to be up and running
	sleep 10

	# Get the autotune config names applied by default
	autotune_config_names=$(kubectl get autotuneconfig -n ${NAMESPACE} --no-headers=true | cut -d " " -f1 | tr "\n" " ")
	IFS=' ' read -r -a autotune_config_names <<<  ${autotune_config_names}

	# form the curl command based on the cluster type
	form_curl_cmd

	# Deploy petclinic application instances	
	deploy_app ${APP_REPO} petclinic 3

	# Sleep for sometime for application pods to be up
	sleep 5

	# Get the application pods
	app_name=$(kubectl get pod | grep petclinic-sample-0 | awk '{print $1}')
	app_pod_names=$(kubectl get pod | grep petclinic | cut -d " " -f1)
	
	# Add label to your application pods for autotune to monitor
	label_names=("petclinic-deployment-0" "petclinic-deployment-1" "petclinic-deployment-2")
	label_pods app_pod_names label_names
	
	# Get the autotune jsons and autotune config jsons
	get_autotune_jsons ${AUTOTUNE_JSONS_DIR} ${YAML_PATH} ${autotune_names[@]}
	get_autotune_config_jsons ${AUTOTUNE_CONFIG_JSONS_DIR} ${autotune_config_names[@]}

	# If testcase is not specified run all tests	
	if [ -z "${testcase}" ]; then
		testtorun="all"
	else
		testtorun=${testcase}
	fi
	
	case "$testtorun" in

	   listapplications|all) 
		# test listapplication API for specific application
		listapplications_test ${app_name} 
	
		# test listapplication API for all applications
		listapplications_test
		;;&	
	   listapplayer|all)
		# test listapplayer API for specific application
		listapplayer_test ${app_name}
	
		# test listapplayer API for all applications
		listapplayer_test
		;;&
	   searchspace|all)
		# test searchSpace API for specific application
		searchspace_test ${app_name}
	
		# test searchSpace API for all applications
		searchspace_test
		;;&
	    list_autotune_tunables|all)
		# test listAutotuneTunables API for specific sla_class and layer
		sla_class="response_time"
		layer="container"
		list_autotune_tunables_test ${sla_class} ${layer}

		# test listAutotuneTunables API for specific sla_class
		sla_class="response_time"
		list_autotune_tunables_test ${sla_class} 
	
		# test listAutotuneTunables API for all layers
		list_autotune_tunables_test
		;;&
	    listapptunables|all)
		# test listapptunables API for specific application and specific layer
		layer="container"
		listapptunables_test ${app_name} ${layer}

		# test listapptunables API for specific application
		listapptunables_test ${app_name}

		# test listapptunables API for all applications	
		listapptunables_test
		;;&
	esac


	if [ "${TESTS_FAILED}" -ne "0" ]; then
		FAILED_TEST_SUITE+=(${FUNCNAME})
	fi 

	# Cleanup the deployed apps
	app_cleanup "petclinic"

	# Cleanup autotune
	autotune_cleanup ${cluster_type}

	# print the testsuite summary
	testsuitesummary ${FUNCNAME} ${FAILED_CASES}
}
