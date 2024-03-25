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
##### Functional tests for autotune #####
#

CURRENT_DIR="$(dirname "$(realpath "$0")")"
SCRIPTS_DIR="${CURRENT_DIR}"
# Source the common functions scripts
. ${SCRIPTS_DIR}/common/common_functions.sh

# Source the test suite scripts
. ${SCRIPTS_DIR}/da/da_app_autotune_yaml_tests.sh
. ${SCRIPTS_DIR}/da/da_kruize_layer_yaml_tests.sh
. ${SCRIPTS_DIR}/da/da_basic_api_tests.sh
. ${SCRIPTS_DIR}/da/modify_kruize_layer_tests.sh
. ${SCRIPTS_DIR}/da/configmap_yaml_tests.sh
. ${SCRIPTS_DIR}/da/autotune_id_tests.sh
. ${SCRIPTS_DIR}/da/kruize_layer_id_tests.sh
. ${SCRIPTS_DIR}/em/em_standalone_tests.sh
. ${SCRIPTS_DIR}/remote_monitoring_tests/remote_monitoring_tests.sh
. ${SCRIPTS_DIR}/local_monitoring_tests/local_monitoring_tests.sh

# Iterate through the commandline options
while getopts i:o:r:-: gopts
do
	case ${gopts} in
	-)
		case "${OPTARG}" in
			cluster_type=*)
				cluster_type=${OPTARG#*=}
				;;
			tctype=*)
				tctype=${OPTARG#*=}
				;;
			testmodule=*)
				testmodule=${OPTARG#*=}
				;;
			testsuite=*)
				testsuite=${OPTARG#*=}
				;;
			testcase=*)
				testcase=${OPTARG#*=}
				;;
			resultsdir=*)
				resultsdir=${OPTARG#*=}
				;;
			skipsetup)
				skip_setup=1
				;;
		esac
		;;
	i)
		AUTOTUNE_DOCKER_IMAGE="${OPTARG}"
		;;
	r)
		APP_REPO="${OPTARG}"
		;;
	esac
done

# Set the root for result directory
if [ -z "${resultsdir}" ]; then
	RESULTS_ROOT_DIR="${PWD}/kruize_test_results"
else
	RESULTS_ROOT_DIR="${resultsdir}/kruize_test_results"
fi
mkdir -p ${RESULTS_ROOT_DIR}

# create the result directory with a time stamp
RESULTS="${RESULTS_ROOT_DIR}/kruize_$(date +%Y%m%d:%T)"
mkdir -p "${RESULTS}"

SETUP_LOG="${TEST_DIR}/setup.log"

if [[ "$testsuite" != "remote_monitoring_tests" && "$testsuite" != "local_monitoring_tests" ]];  then
	CONFIGMAP="${RESULTS}/test_configmap"
	mkdir ${CONFIGMAP}

	# Replace configmap logging level to debug for testing purpose
	find="info"
	replace="debug"
	config_yaml="${CONFIGMAP}/${cluster_type}-config.yaml"
	cp "${configmap}/${cluster_type}-config.yaml" "${config_yaml}"

	# Update the config map yaml with specified field
	update_yaml ${find} ${replace} ${config_yaml}
fi

# Set of functional tests to be performed
# input: Result directory to store the functional test results
# output: Perform the set of functional tests
function functional_test() {
	if [ "${sanity}" -eq "1" ]; then
		testcase=""
		# perform the basic api tests
		basic_api_tests > >(tee "${RESULTS}/basic_api_tests.log") 2>&1
	else
		execute_da_testsuites
		execute_em_testsuites
	fi
}

# Execute all tests for DA (Dependency Analyzer) module
function execute_da_testsuites() {
	# perform the application autotune yaml tests
	app_autotune_yaml_tests > >(tee "${RESULTS}/app_autotune_yaml_tests.log") 2>&1

	testcase=""
	# perform the autotune config yaml tests
	kruize_layer_yaml_tests > >(tee "${RESULTS}/kruize_layer_yaml_tests.log") 2>&1

	testcase=""
	# perform the basic api tests
	basic_api_tests > >(tee "${RESULTS}/basic_api_tests.log") 2>&1

	testcase=""
	# Modify existing autotuneconfig yamls and check for API results
	modify_kruize_layer_tests > >(tee "${RESULTS}/modify_kruize_layer_tests.log") 2>&1

	testcase=""
	# perform the configmap yaml tests
	configmap_yaml_tests > >(tee "${RESULTS}/configmap_yaml_tests.log") 2>&1

	testcase=""
	# validate the autotune object id
	autotune_id_tests > >(tee "${RESULTS}/autotune_id_tests.log") 2>&1

	testcase=""
	# validate the autotune config object id
	kruize_layer_id_tests > >(tee "${RESULTS}/kruize_layer_id_tests.log") 2>&1
}

# Execute all tests for EM (Experiment Manager) module
function execute_em_testsuites() {
        testcase=""
        # perform the EM API tests
        em_standalone_tests > >(tee "${RESULTS}/em_standalone_tests.log") 2>&1
}

# Execute all tests for Remote monitoring
function execute_remote_monitoring_testsuites() {
        testcase=""
        # perform the Remote monitoring tests
        remote_monitoring_tests > >(tee "${RESULTS}/remote_monitoring_tests.log") 2>&1
}

# Execute all tests for Local monitoring
function execute_local_monitoring_testsuites() {
        testcase=""
        # perform the Remote monitoring tests
        local_monitoring_tests > >(tee "${RESULTS}/local_monitoring_tests.log") 2>&1
}

# Perform the specific testsuite if specified
if [ ! -z "${testmodule}" ]; then
	case "${testmodule}" in
	da)
		# Execute tests for Dependency Analyzer Module
		execute_da_testsuites
		;;
	em)
		# Execute tests for Experiment Manager (EM) Module
		execute_em_testsuites
		;;
	esac
elif [ ! -z "${testsuite}" ]; then
	if [ "${testsuite}" == "sanity" ]; then
		sanity=1
		functional_test
	else
		${testsuite} > >(tee "${RESULTS}/${testsuite}.log") 2>&1
	fi
elif [[ -z "${testcase}" && -z "${testsuite}" && -z "${testmodule}" ]]; then
	functional_test
fi

echo ""
echo "*********************************************************************************"
echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Overall summary of the tests ~~~~~~~~~~~~~~~~~~~~~~~"
overallsummary  ${FAILED_TEST_SUITE}
echo ""
echo "*********************************************************************************"

if [ "${TOTAL_TESTS_FAILED}" -ne "0" ]; then
	exit 1
else
	exit 0
fi
