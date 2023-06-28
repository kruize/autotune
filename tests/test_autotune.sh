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
##### Script to test autotune #####
#

CURRENT_DIR="$(dirname "$(realpath "$0")")"
SCRIPTS_DIR="${CURRENT_DIR}/scripts"
# Source the common functions scripts
. ${SCRIPTS_DIR}/common/common_functions.sh

resultsdir="${CURRENT_DIR}"

# usage of the test script
function usage() { 
	echo ""
	echo "Usage: $0 -c [minikube] [-t terminate or cleanup kruize] -r [location of benchmarks] [-i autotune image] [--tctype=functional|system] [--testmodule=Autotune module to be tested] [--testsuite=Group of tests that you want to perform] [--testcase=Particular test case that you want to check] [-u user] [-p password] [-n namespace] [--resultsdir=results directory] [--skipsetup specifying this flag skips autotune & application setup] [--cleanup_prometheus specifying this flag along with -t option cleans up prometheus setup]"
	echo ""
	echo "Example: $0 -c minikube --tctype=functional --testsuite=app_autotune_yaml_tests --testcase=slo_class -r /home/benchmarks --resultsdir=/home/results"
	echo "Example: $0 -c minikube --testmodule=da -r /home/benchmarks --resultsdir=/home/results"
	echo ""
	test_module_usage
	test_suite_usage
	echo ""
	exit -1
}

# List of testmodules supported
# output: Display the names of the supported test module
function test_module_usage() {
	echo "Supported Test modules are:"
	for array in "${TEST_MODULE_ARRAY[@]}"
	do
		echo "		           ${array}"
	done
	exit -1
}

# List of testsuites supported
# output: Display the names of the supported test suite
function test_suite_usage() {
	echo "Supported Test suites are:"
	for array in "${TEST_SUITE_ARRAY[@]}"
	do
		echo "		           ${array}"
	done
	exit -1
}

# Check if the cluster_type is one of kubernetes clusters
# input: cluster type
# output: If cluster type is not supported then print the usage
function check_cluster_type() {
	if [ -z "${cluster_type}" ]; then
		echo
		usage
	fi
	case "${cluster_type}" in
	minikube|openshift)
		;;
	*)
		echo "Error: Cluster type **${cluster_type}** is not supported  "
		usage
	esac
}

# check if the specified testmodule type exists
# input: testmodule
# output: if the specified test module is supported or not 
function check_testmodule_type() {
	for tm in ${TEST_MODULE_ARRAY[@]}
	do
		if [ "${testmodule}" == "${tm}" ]; then
			matched=1
		fi
	done
	
	if [ "${testmodule}" == "help" ]; then
		test_module_usage
	fi
	
	if [ "${matched}" -eq "0" ]; then
		echo "Error: Invalid testmodule **${testmodule}** "
		test_module_usage
	fi
}

# check if the specified testsuite type exists
# input: testsuite
# output: if the given test suite is not supported print the spported testsuite
function check_testsuite_type() {
	for ts in ${TEST_SUITE_ARRAY[@]}
	do
		if [ "${testsuite}" == "${ts}" ]; then
			matched=1
		fi
	done
	
	if [ "${testsuite}" == "help" ]; then
		test_suite_usage
	fi
	
	if [ "${matched}" -eq "0" ]; then
		echo "Error: Invalid testsuite **${testsuite}** "
		test_suite_usage
	fi
}

# Check the test type 
# input: test type
# output: If test type is not supported then stop the test
function check_testcase_type() {
	case "${tctype}" in
	functional|system)
		;;
	*)
		echo "Error: Test case type **${tctype}** is not supported"
		exit -1
	esac
}

# Iterate through the commandline options
while getopts c:ti:k:n:p:su:r:y:o:-: gopts
do
	case ${gopts} in
	-)
		case "${OPTARG}" in
			tctype=*)
				tctype=${OPTARG#*=}
				check_testcase_type
				;;
			testmodule=*)
				testmodule=${OPTARG#*=}
				check_testmodule_type
				;;
			testsuite=*)
				testsuite=${OPTARG#*=}
				check_testsuite_type
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
			cleanup_prometheus)
				cleanup_prometheus=1
				;;

		esac
		;;
	c)
		cluster_type="${OPTARG}"
		;;
	t)
		setup=0
		;;
	i)
		AUTOTUNE_DOCKER_IMAGE="${OPTARG}"		
		;;
	k)
		kurl="${OPTARG}"
		;;
	n)
		autotune_ns="${OPTARG}"
		;;
	p)
		password="${OPTARG}"
		;;
	s)
		setup=1
		;;
	u)
		user="${OPTARG}"
		;;
	r)
		APP_REPO="${OPTARG}"
		;;
	[?])
		usage
	esac
done

# Check if the cluster type is supported
check_cluster_type

# Set the testcase type to default if it is not specified 
if [ -z "${tctype}" ]; then
	tctype="functional"
fi

# It is necessary to pass testsuite name when testcase is specified
if [ ! -z "${testcase}" ]; then
	if [ -z "${testsuite}" ]; then
		echo "Error: Do specify the testsuite name"
		exit -1
	fi
fi

#if [ -z "${AUTOTUNE_DOCKER_IMAGE}" ]; then
#	if [ ${testsuite} != "remote_monitoring_tests" ]; then
#		AUTOTUNE_DOCKER_IMAGE="${AUTOTUNE_IMAGE}"
#	fi
#fi

# check for benchmarks directory path
if [ ! "${testsuite}" == "remote_monitoring_tests" ]; then
	if [ -z "${APP_REPO}" ]; then
		echo "Error: Do specify the benchmarks directory path"
		usage
	else
		if [ ! -d "${APP_REPO}" ]; then
			echo "Error: benchmark directory does not exists"
			usage
		fi
	fi
else
	APP_REPO="NA"
fi

if [ "${setup}" -ne "0" ]; then
	# Call the proper setup function based on the cluster_type
	echo -n "############# Performing ${tctype} test for autotune #############"
	if [ ${skip_setup} -eq 1 ]; then
		#if [ ${testsuite} == "remote_monitoring_tests" ]; then
		if [ -z "${AUTOTUNE_DOCKER_IMAGE}" ]; then
			${SCRIPTS_DIR}/${tctype}_tests.sh --cluster_type=${cluster_type} --tctype=${tctype} --testmodule=${testmodule} --testsuite=${testsuite} --testcase=${testcase} --resultsdir=${resultsdir} -r ${APP_REPO} --skipsetup
		else
			${SCRIPTS_DIR}/${tctype}_tests.sh --cluster_type=${cluster_type} --tctype=${tctype} --testmodule=${testmodule} --testsuite=${testsuite} --testcase=${testcase} --resultsdir=${resultsdir} -i ${AUTOTUNE_DOCKER_IMAGE} -r ${APP_REPO} --skipsetup
		fi
	else
		if [ -z "${AUTOTUNE_DOCKER_IMAGE}" ]; then
		#if [ ${testsuite} == "remote_monitoring_tests" ]; then
			${SCRIPTS_DIR}/${tctype}_tests.sh --cluster_type=${cluster_type} --tctype=${tctype} --testmodule=${testmodule} --testsuite=${testsuite} --testcase=${testcase} --resultsdir=${resultsdir} -r ${APP_REPO}
		else
			${SCRIPTS_DIR}/${tctype}_tests.sh --cluster_type=${cluster_type} --tctype=${tctype} --testmodule=${testmodule} --testsuite=${testsuite} --testcase=${testcase} --resultsdir=${resultsdir} -i ${AUTOTUNE_DOCKER_IMAGE} -r ${APP_REPO}
		fi
	fi

	TEST_RESULT=$?
	echo "########################################################################"
	echo ""
	if [ "${TEST_RESULT}" -ne "0" ]; then
		exit 1
	else
		exit 0
	fi
else
	if [ ${testsuite} == "remote_monitoring_tests" ]; then
		target="crc"
	else
		target="autotune"
	fi
	autotune_cleanup
fi
