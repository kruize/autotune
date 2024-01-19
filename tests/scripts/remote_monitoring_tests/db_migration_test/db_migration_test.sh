#!/bin/bash
#
# Copyright (c) 2024, 2024 IBM Corporation, RedHat and others.
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
### Script to run DB migration test with Kruize in remote monitoring mode ##
#

# This test does the following:
# Deploys the previous release of kruize that is specified and uploads results for 50 exps / 15 days
# Invokes updateRecommendations for all the 50 exps
# Backups DB and deploys the current release of kruize specified along with a new instance of postgres. 
# The backed up DB is restored and new usage metrics or results for 1 day for 50 exps is posted
# Invokes updateRecommendations for all the 50 exps

CURRENT_DIR="$(dirname "$(realpath "$0")")"
KRUIZE_REPO="${CURRENT_DIR}/../../../../"
SCALE_TEST="${CURRENT_DIR}/../scale_test"

# Source the common functions scripts
. ${CURRENT_DIR}/../../common/common_functions.sh

RESULTS_DIR=kruize_scale_test_results
APP_NAME=kruize
CLUSTER_TYPE=openshift
DEPLOYMENT_NAME=kruize
CONTAINER_NAME=kruize
NAMESPACE=openshift-tuning
num_exps=5
num_days_of_res=15
num_clients=10
minutes_jump=15
interval_hours=6
initial_start_date="2023-12-20T00:00:00.000Z"
query_db_interval=10
db_backup_file="./db_backup.sql"

replicas=10

target="crc"
kruize_image_prev="quay.io/kruize/autotune_operator:0.0.19.5_rm"
kruize_image_current="quay.io/kruize/autotune_operator:0.0.20.1_rm"
hours=6

function usage() {
	echo
	echo "Usage: [-i Kruize image previous release] [-j kruize image current release] [-u No. of experiments (default - 5000)] [-d No. of days of results (default - 15)] [-n No. of clients (default - 20)] [-m results duration interval in mins, (default - 15)] [-t interval hours (default - 6)] [-s Initial start date (default - 2023-01-10T00:00:00.000Z)] [-q query db interval in mins, (default - 10)] [-r <resultsdir path>]"
	exit -1
}

while getopts r:i:u:d:t:n:m:s:f:q:h gopts
do
	case ${gopts} in
	r)
		RESULTS_DIR="${OPTARG}"		
		;;
	i)
		kruize_image_prev="${OPTARG}"		
		;;
	j)
		kruize_image_current="${OPTARG}"		
		;;
	u)
		num_exps="${OPTARG}"		
		;;
	d)
		num_days_of_res="${OPTARG}"		
		;;
	n)
		num_clients="${OPTARG}"		
		;;
	m)
		minutes_jump="${OPTARG}"		
		;;
	s)
		initial_start_date="${OPTARG}"		
		;;
	t)
		interval_hours="${OPTARG}"		
		;;
	q)
		query_db_interval="${OPTARG}"		
		;;
	f)
		db_backup_file="${OPTARG}"		
		;;
	h)
		usage
		;;
	esac
done

start_time=$(get_date)
LOG_DIR="${RESULTS_DIR}/db-migration-test-$(date +%Y%m%d%H%M)"
mkdir -p ${LOG_DIR}

LOG="${LOG_DIR}/db-migration-test.log"

# Run scalability test to load 50 exps / 15 days data and update Recommendations with previous release
pushd ${SCALE_TEST} > /dev/null
	echo "Run scalability test to load 50 exps / 15 days data and update Recommendations with ${kruize_image_prev}"
	echo "../remote_monitoring_scale_test_bulk.sh -i ${kruize_image_prev} -u 5 -d 15 -n 10 -t 6 -q 10 -s 2023-12-20T00:00:00.000Z -r ${LOG_DIR}/kruize_scale_test_logs_50_15days"
	./remote_monitoring_scale_test_bulk.sh -i ${kruize_image_prev} -u 5 -d 15 -n 10 -t 6 -q 10 -s 2023-12-20T00:00:00.000Z -r ${LOG_DIR}/kruize_scale_test_logs_50_15days
popd > /dev/null 

# Backup DB
echo ""
echo "Backing up DB..."
db_backup_file="${LOG_DIR}/kruize_scale_test_logs_50_15days/db_backup.sql"
echo "kubectl -n openshift-tuning exec -it `kubectl get pods -o=name -n openshift-tuning | grep postgres` -- pg_dump -U admin -d kruizeDB > ${db_backup_file}"
kubectl -n openshift-tuning exec -it `kubectl get pods -o=name -n openshift-tuning | grep postgres` -- pg_dump -U admin -d kruizeDB > ${db_backup_file}
echo "Backing up DB...Done"
echo ""

sleep 20

# Run scalability test to load 50 exps / 1 day data and update Recommendations after restoring DB with the current release
pushd ${SCALE_TEST} > /dev/null
	echo ""
	echo "Run scalability test to load 50 exps / 1 day data and update Recommendations after restoring DB with ${kruize_image_current}..."
	echo "../remote_monitoring_scale_test_bulk.sh -i ${kruize_image_current} -u 5 -d 1 -n 10 -t 6 -q 10 -s 2024-01-04T00:00:00.000Z -l true -f ${db_backup_file} -r ${LOG_DIR}/kruize_scale_test_logs_50_16days"
	./remote_monitoring_scale_test_bulk.sh -i ${kruize_image_current} -u 5 -d 1 -n 10 -t 6 -q 10 -s 2024-01-04T00:00:00.000Z -l true -f ${db_backup_file} -r ${LOG_DIR}/kruize_scale_test_logs_50_16days

	echo | tee -a ${LOG}
popd > /dev/null 


end_time=$(get_date)
elapsed_time=$(time_diff "${start_time}" "${end_time}")
echo ""
echo "Test took ${elapsed_time} seconds to complete" | tee -a ${LOG}
