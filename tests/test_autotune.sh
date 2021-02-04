#!/bin/bash
#
# Copyright (c) 2020, 2021 RedHat, IBM Corporation and others.
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

TEST_SUITE_ARRAY=("app_autotune_yaml_tests")
CURRENT_DIR="$(dirname "$(realpath "$0")")"
SCRIPTS_DIR="${CURRENT_DIR}/scripts"
matched=0
setup=1

# Source the common functions scripts
. ${SCRIPTS_DIR}/common_functions.sh

# usage of the test script
function usage() { 
	echo ""
	echo "Usage: $0 -c docker|minikube|openshift -k kurl -r [location of benchmarks] -y [autotune yaml path] [-i autotune image] [--tctype=functional|system] [--testsuite=Group of tests that you want to perform] [--testcase=Particular test case that you want to check] [-u user] [-p password] [-n namespace] [--resultsdir=results directory] "
	echo ""
	echo "Example: $0 -c minikube --tctype=functional --testsuite=app_autotune_yaml_tests --testcase=sla_class -r /home/benchmarks --resultsdir=/home/results"
	echo ""
	test_suite_usage
	echo ""
	exit -1
}

# List of testsuites supported
# ouput: Display the names of the supported test suite
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
# ouput: If cluster type is not supported then print the usage
function check_cluster_type() {
	if [ -z "${cluster_type}" ]; then
		echo
		usage
	fi
	case "${cluster_type}" in
	docker|minikube|openshift)
		;;
	*)
		echo "Error: Cluster type **${cluster_type}** is not supported  "
		usage
	esac
}

# check if the specified testsuite type exists
# input: testsuite
# ouput: if the given test suite is not supported print the spported testsuite
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
while getopts c:ti:k:n:p:su:r:y:-: gopts
do
	case ${gopts} in
	-)
		case "${OPTARG}" in
			tctype=*)
				tctype=${OPTARG#*=}
				check_testcase_type
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
	y)
		AUTOTUNE_YAML_PATH="${OPTARG}"
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

if [ "${setup}" -ne "0" ]; then
	# Call the proper setup function based on the cluster_type
	echo -n "############# Performing ${tctype} test for autotune #############"
	${SCRIPTS_DIR}/${tctype}_tests.sh --cluster_type=${cluster_type} --tctype=${tctype} --testsuite=${testsuite} --testcase=${testcase} --resultsdir=${resultsdir} -i ${AUTOTUNE_DOCKER_IMAGE} -r ${APP_REPO}
	echo "########################################################################"
	echo ""
else
	autotune_cleanup
fi
