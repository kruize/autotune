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
. ${SCRIPTS_DIR}/remote_monitoring_tests/remote_monitoring_tests.sh
. ${SCRIPTS_DIR}/local_monitoring_tests/local_monitoring_tests.sh
. ${SCRIPTS_DIR}/local_monitoring_tests/authentication_tests.sh

# Iterate through the commandline options
while getopts i:-: gopts
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
			skipsetup)
				skip_setup=1
				;;
		esac
		;;
	i)
		AUTOTUNE_DOCKER_IMAGE="${OPTARG}"
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

# Perform the specific testsuite if specified
if [ ! -z "${testsuite}" ]; then
	${testsuite} > >(tee "${RESULTS}/${testsuite}.log") 2>&1
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
