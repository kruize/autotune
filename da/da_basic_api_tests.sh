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
pushd ${CURRENT_DIR}/.. >> /dev/null

# Path to the directory containing yaml files
MANIFESTS="${PWD}/autotune_test_yamls/manifests"
api_autotune_yaml="api_test_yamls/basic_api_tests"
module="da"
YAML_PATH=${MANIFESTS}/${module}/${api_autotune_yaml}

# Main function that calls all the other tests to test the Autotune APIs
function basic_api_tests() {
	start_time=$(get_date)
	FAILED_CASES=()
	TESTS_FAILED=0
	TESTS_PASSED=0
	TESTS=0

	# check if the test case is supported
	basic_api_tests=("liststacklayers" "liststacktunables" "liststacks" "searchspace" "list_kruize_tunables")

	if [ ! -z "${testcase}" ]; then
		check_test_case "basic_api"
	fi

	# create the result directory for given testsuite
	echo ""
	TEST_SUITE_DIR="${RESULTS}/basic_api_tests"
	AUTOTUNE_YAMLS_DIR="${TEST_SUITE_DIR}/autotune_yamls"
	AUTOTUNE_JSONS_DIR="${TEST_SUITE_DIR}/autotune_jsons"
	KRUIZE_LAYER_JSONS_DIR="${TEST_SUITE_DIR}/autotuneconfig_jsons"
	AUTOTUNE_SETUP_LOG="${TEST_SUITE_DIR}/setup.log"

	mkdir -p ${TEST_SUITE_DIR}
	mkdir -p ${AUTOTUNE_YAMLS_DIR}
	mkdir -p ${AUTOTUNE_JSONS_DIR}
	mkdir -p ${KRUIZE_LAYER_JSONS_DIR}
	echo ""
	((TOTAL_TEST_SUITES++))

	declare -A layer_configs=([petclinic]="container" [galaxies]="container hotspot quarkus")

	echo ""
	echo "******************* Executing test suite ${FUNCNAME} ****************"
	echo ""

	# create the autotune setup
	echo "Setting up autotune..."
	setup >> ${AUTOTUNE_SETUP_LOG} 2>&1
	echo "Setting up autotune...Done"

	# Giving a sleep for autotune pod to be up and running
	sleep 10

	# Get the autotune config names applied by default
	kruize_layer_names=$(kubectl get autotuneconfig -n ${NAMESPACE} --no-headers=true | cut -d " " -f1 | tr "\n" " ")
	IFS=' ' read -r -a kruize_layer_names <<<  ${kruize_layer_names}

	# form the curl command based on the cluster type
	form_curl_cmd

	# Deploy petclinic application instances
	appln="petclinic"
	instances="3"
	deploy_app "${APP_REPO}" "${appln}" "${instances}"

	# Sleep for sometime for application pods to be up
	sleep 5

	# Get the application pods
	app_pod_names=$(kubectl get pod | grep petclinic | cut -d " " -f1)

	# Add label to your application pods for autotune to monitor
	label_names=("petclinic-deployment-0" "petclinic-deployment-1" "petclinic-deployment-2")
	label_pods app_pod_names label_names

	# Apply Autotune yamls
	apply_autotune_yamls "${APP_REPO}" "${appln}" "${instances}" "${AUTOTUNE_YAMLS_DIR}"

	# Get the autotune jsons and autotune config jsons
	get_autotune_jsons "${AUTOTUNE_JSONS_DIR}"
	get_kruize_layer_jsons "${KRUIZE_LAYER_JSONS_DIR}"

	# If testcase is not specified run all tests
	if [ -z "${testcase}" ]; then
		testtorun="all"
	else
		testtorun=${testcase}
	fi

	case "$testtorun" in

	   liststacks|all)
		# Test listStacks API for a specific application
		exp_name="${autotune_names[0]}"
		liststacks_test "${exp_name}"

		# Test listStacks API for all applications
		liststacks_test
		;;&
	   liststacklayers|all)
		# Test listStackLayer API for a specific application
		exp_name="${autotune_names[1]}"
		liststacklayers_test "${appln}" "${exp_name}"

		# Test listStackLayers API for all applications
		liststacklayers_test "${appln}"
		;;&
	   searchspace|all)
		# Test searchSpace API for a specific application
		exp_name="${autotune_names[0]}"
		searchspace_test "${appln}" "${exp_name}"

		# Test searchSpace API for all applications
		searchspace_test "${appln}"
		;;&
	    list_kruize_tunables|all)
		# Test listKruizeTunables API for a specific slo_class and layer
		slo_class="response_time"
		layer="container"
		list_kruize_tunables_test "${slo_class}" "${layer}"

		# Test listKruizeTunables API for a specific slo_class
		slo_class="response_time"
		list_kruize_tunables_test "${slo_class}"

		# Test listKruizeTunables API for all layers
		list_kruize_tunables_test
		;;&
	    liststacktunables|all)
		# Test listStackTunables API for a specific application and a specific layer
		layer="container"
		exp_name="${autotune_names[2]}"
		liststacktunables_test "${appln}" "${exp_name}" "${layer}"

		# Test listStackTunables API for a specific application
		liststacktunables_test "${appln}" "${exp_name}"

		# Test listStackTunables API for all applications
		liststacktunables_test "${appln}"
		;;&
	esac


	if [ "${TESTS_FAILED}" -ne "0" ]; then
		FAILED_TEST_SUITE+=(${FUNCNAME})
	fi

	# Cleanup the deployed apps
	app_cleanup "${appln}"

	# Cleanup autotune
	autotune_cleanup

	end_time=$(get_date)
	elapsed_time=$(time_diff "${start_time}" "${end_time}")

	# print the testsuite summary
	testsuitesummary "${FUNCNAME}" "${elapsed_time}" "${FAILED_CASES}"
}
