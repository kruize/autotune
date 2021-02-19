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
. ${SCRIPTS_DIR}/common_functions.sh

# Source the test suite scripts
. ${SCRIPTS_DIR}/da_app_autotune_yaml_tests.sh
. ${SCRIPTS_DIR}/da_autotune_config_yaml_tests.sh
. ${SCRIPTS_DIR}/da_basic_api_tests.sh

# Iterate through the commandline options
while getopts i:r:-: gopts
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

# Set of functional tests to be performed 
# input: Result directory to store the functional test results
# output: Perform the set of functional tests
function functional_test() {
	# perform the application autotune yaml tests 
	app_autotune_yaml_tests > >(tee "${RESULTS}/app_autotune_yaml_tests.log") 2>&1

	# perform the autotune config yaml tests
	autotune_config_yaml_tests > >(tee "${RESULTS}/autotune_config_yaml_tests.log") 2>&1

	# perform the basic api tests
	basic_api_tests > >(tee "${RESULTS}/basic_api_tests.log") 2>&1
}

# If testsuite is not specified perform the set of functional tests
if [ -z "${testsuite}" ]; then
	testsuite=functional_test
fi

# Perform the specific testsuite if specified 
if [ ! -z "${testsuite}" ]; then
	${testsuite} > >(tee "${RESULTS}/${testsuite}.log") 2>&1
elif [[ -z "${testcase}" && -z "${testsuite}"  ]]; then
	functional_test
fi

echo ""
echo "*********************************************************************************"
echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Overall summary of the tests ~~~~~~~~~~~~~~~~~~~~~~~"
overallsummary  ${FAILED_TEST_SUITE} 
echo ""
echo "*********************************************************************************"
