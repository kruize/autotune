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
##### Script to start the HPO (Hyperparameter Optimization) REST Service and Search Space Mock server required for testing HPO REST APIs #####

CURRENT_DIR="$(dirname "$(realpath "$0")")"
SCRIPTS_DIR="${CURRENT_DIR}"

PATH_TO_HPO_SERVICE=${SCRIPTS_DIR}/../../hyperparameter_tuning
PATH_TO_SEARCHSPACE=${SCRIPTS_DIR}

SEARCH_SPACE_JSON=${SCRIPTS_DIR}/../resources/searchspace_jsons/searchspace.json

setup=1

while getopts tj:sp:-: gopts
do
	case ${gopts} in
	p)
		log_dir="${OPTARG}"
		;;
	t)
		setup=0
		;;
	j)
		SEARCH_SPACE_JSON="${OPTARG}"
		;;
	s)
		setup=1
		;;
	[?])
		usage
	esac
done

if [ -z "${log_dir}" ]; then
        log_dir="${HOME}"
fi

function usage() {
	echo ""
	echo "Usage: $0 -p log_dir -j <search space json> [-s] [-t]" 
	echo "Where -s = start(default), -t = terminate" 
	echo ""
	exit -1
}

function start_servers() {
	export N_TRIALS=5
	export N_JOBS=1
	export PYTHONUNBUFFERED=TRUE

	# Start the Search Space service
	echo "Starting searchspace service..."
	nohup python3 ${PATH_TO_SEARCHSPACE}/search_space.py ${SEARCH_SPACE_JSON} &> ${log_dir}/searchspace.log &

	# Start the HPO REST API service 
	echo "Starting HPO REST API service..."
	nohup python3 ${PATH_TO_HPO_SERVICE}/service.py  &> ${log_dir}/service.log &
}

function stop_servers() {
	echo "Stopping searchspace service..."
	ps -ef | grep search_space.py | grep -v grep | awk '{print $2}' | xargs kill -9

	echo "Stopping HPO REST API service..."
	ps -ef | grep service.py | grep -v grep | awk '{print $2}' | xargs kill -9
}

if [ "${setup}" -ne "0" ]; then
	start_servers
else
	stop_servers
fi
