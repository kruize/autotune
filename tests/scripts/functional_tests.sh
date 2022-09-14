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
. ${SCRIPTS_DIR}/da/da_autotune_config_yaml_tests.sh
. ${SCRIPTS_DIR}/da/da_basic_api_tests.sh
. ${SCRIPTS_DIR}/da/modify_autotune_config_tests.sh
. ${SCRIPTS_DIR}/da/configmap_yaml_tests.sh
. ${SCRIPTS_DIR}/da/autotune_id_tests.sh
. ${SCRIPTS_DIR}/da/autotune_layer_config_id_tests.sh
. ${SCRIPTS_DIR}/em/em_standalone_tests.sh

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
	RESULTS_ROOT_DIR="${PWD}/autotune_test_results"
else
	RESULTS_ROOT_DIR="${resultsdir}/autotune_test_results"
fi
mkdir -p ${RESULTS_ROOT_DIR}

# create the result directory with a time stamp
RESULTS_DIR="${RESULTS_ROOT_DIR}/autotune_$(date +%Y%m%d:%T)"
mkdir -p "${RESULTS_DIR}"

# create the result directory for functional tests
RESULTS="${RESULTS_DIR}/${tctype}"
mkdir ${RESULTS}

SETUP_LOG="${TEST_DIR}/setup.log"

CONFIGMAP="${RESULTS}/test_configmap"
mkdir ${CONFIGMAP}

# Replace configmap logging level to debug for testing purpose
find="info"
replace="debug"
config_yaml="${CONFIGMAP}/${cluster_type}-config.yaml"
cp "${configmap}/${cluster_type}-config.yaml" "${config_yaml}"

# Update the config map yaml with specified field
update_yaml ${find} ${replace} ${config_yaml}

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
	autotune_config_yaml_tests > >(tee "${RESULTS}/autotune_config_yaml_tests.log") 2>&1

	testcase=""
	# perform the basic api tests
	basic_api_tests > >(tee "${RESULTS}/basic_api_tests.log") 2>&1
		
	testcase=""
	# Modify existing autotuneconfig yamls and check for API results
	modify_autotune_config_tests > >(tee "${RESULTS}/modify_autotune_config_tests.log") 2>&1
	
	testcase=""
	# perform the configmap yaml tests
	configmap_yaml_tests > >(tee "${RESULTS}/configmap_yaml_tests.log") 2>&1
		
	testcase=""
	# validate the autotune object id
	autotune_id_tests > >(tee "${RESULTS}/autotune_id_tests.log") 2>&1

	testcase=""	
	# validate the autotune config object id
	autotune_layer_config_id_tests > >(tee "${RESULTS}/autotune_layer_config_id_tests.log") 2>&1
}

# Execute all tests for EM (Experiment Manager) module
function execute_em_testsuites() {
        testcase=""
        # perform the EM API tests
        em_standalone_tests > >(tee "${RESULTS}/em_standalone_tests.log") 2>&1
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

