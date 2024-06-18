#!/bin/bash
#
# Copyright (c) 2023, 2023 IBM Corporation, RedHat and others.
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

cluster_type="openshift"
num_exps=250
num_days_of_res=2
num_clients=10
results_count=24
minutes_jump=15
initial_start_date="2023-08-01T00:00:00.000Z"
interval_hours=6
query_db_interval=5
total_results_count=0

function usage() {
	echo
	echo "Usage: ./run_scalability_test.sh -c cluster_type[minikube|openshift (default - openshift)] [-a IP] [-p PORT] [-u No. of experiments per client (default - 250)]"
	echo "	     [-d No. of days of results (default - 2)] [-n No. of clients] [-m results duration interval in mins (default - 15)] [-i interval hours (default - 6)]"
        echo "       [-s Initial start date] [-q query db interval in mins (default - 5)] [-r <resultsdir path>] [-e total results count already in the DB]"
	exit -1
}

function query_db() {

	while(true); do
		# Obtain the no. of experiments and results from the db
		exp_count=$(kubectl exec `kubectl get pods -o=name -n openshift-tuning | grep postgres` -n openshift-tuning -- psql -U admin -d kruizeDB -c "SELECT count(*) from public.kruize_experiments ;" | tail -3 | head -1 | tr -d '[:space:]')

		results_count=$(kubectl exec `kubectl get pods -o=name -n openshift-tuning | grep postgres` -n openshift-tuning -- psql -U admin -d kruizeDB -c "SELECT count(*) from public.kruize_results ;" | tail -3 | head -1 | tr -d '[:space:]')

		# Print the scalability test progress
		echo "Exps = $exp_count Results = $results_count"
		days_completed=$((${results_count} / (96 * ${num_exps} * ${num_clients})))
		day_in_progress=$(($days_completed + 1))

		if [ ${days_completed} == 0 ]; then
			echo "Day $day_in_progress in progress"
		else
			echo "Day $days_completed completed, Day $day_in_progress in progress"
		fi
		
		sleep_time=$((query_db_interval * 60))
		sleep ${sleep_time}
	done

}

function execution_time() {
	declare -a time_arr

	exec_time_log=$1
	scale_log_dir=$2
	time_arr=(1 4 28 40 60)

	cd $scale_log_dir

	# Capture the execution time taken for uploading the specified number of days of results
	for i in ${time_arr[@]}; do
		time_option="-m${i}"
		echo "" >> ${exec_time_log}
		if [ ${i} == 1 ]; then
			echo "6 hours" > ${exec_time_log}
		else
			j=$((${i}/4))
			if [ ${j} -gt ${num_days_of_res} ]; then
				break;
			fi
			if [ ${i} == 4 ]; then
				echo "${j} day" >> ${exec_time_log}
			else
				echo "${j} days" >> ${exec_time_log}
			fi
		fi
		echo "grep ${time_option} -H 'Time elapsed:' *.log | awk -F '[:.]' '{ sum[$1] += ($4 * 3600) + ($5 * 60) + $6 } END { for (key in sum) { printf "%s: Total time elapsed: %02d:%02d:%02d\n", key, sum[key] / 3600, (sum[key] / 60) % 60, sum[key] % 60 } }' | sort >> ${exec_time_log}"
		echo ""
		grep ${time_option} -H 'Time elapsed:' *.log | awk -F '[:.]' '{ sum[$1] += ($4 * 3600) + ($5 * 60) + $6 } END { for (key in sum) { printf "%s: Total time elapsed: %02d:%02d:%02d\n", key, sum[key] / 3600, (sum[key] / 60) % 60, sum[key] % 60 } }' | sort >> ${exec_time_log}
	done
}


while getopts c:a:p:r:u:n:d:m:i:e:s:q:h gopts
do
	case ${gopts} in
	c)
		cluster_type=${OPTARG}
		;;
	a)
		IP=${OPTARG}
		;;
	p)
		PORT=${OPTARG}
		;;
	r)
		RESULTS_DIR="${OPTARG}"		
		;;
	u)
		num_exps="${OPTARG}"		
		;;
	n)
		num_clients="${OPTARG}"		
		;;
	d)
		num_days_of_res="${OPTARG}"		
		;;
	m)
		minutes_jump="${OPTARG}"		
		;;
	i)
		interval_hours="${OPTARG}"		
		;;
	e)
		total_results_count="${OPTARG}"
		;;
	q)
		query_db_interval="${OPTARG}"
		;;
	s)
		initial_start_date="${OPTARG}"		
		;;
	h)
		usage
		;;
	esac
done

if [ -z "${IP}" ]; then
	usage
fi

SCALE_LOG_DIR="${RESULTS_DIR}/scale_logs"
mkdir -p "${SCALE_LOG_DIR}"
echo "SCALE_LOG_DIR = $SCALE_LOG_DIR"

# Each loops kicks off the specified no. of experiments and posts results for the specified no. of days
prometheus_server=$(echo ${IP} | cut -d "." -f 3- )

echo "Prometheus server = $prometheus_server"
declare -a pid_array=()
for ((loop=1; loop<=num_clients; loop++));
do

	name="scaletest${num_exps}-${loop}"
	logfile="${SCALE_LOG_DIR}/${name}.log"
	echo "logfile = $logfile"

	nohup ./rosSimulationScalabilityWrapper.sh --ip "${IP}" --port "${PORT}" --name ${name} --count ${num_exps},${results_count} --minutesjump ${minutes_jump} --initialstartdate ${initial_start_date} --limitdays ${num_days_of_res} --intervalhours ${interval_hours} --clientthread ${loop}  --prometheusserver ${prometheus_server} --outputdir ${RESULTS_DIR} >> ${logfile} 2>&1 &

	pid_array+=($!)

	echo
	echo "###########################################################################"
	echo "#                                                                         #"
	echo "#  Kicked off ${num_exps} experiments and data upload for client: ${loop} #"
	echo "#                                                                         #"
	echo "###########################################################################"
	echo

	sleep 60
done

query_db &
MYSELF=$!

for pid in "${pid_array[@]}"; do
	wait "$pid"
    	echo "Process with PID $pid has completed."
done

echo "###########################################################################"
echo "				All threads completed!                          "
echo "###########################################################################"

exec_time_log="${RESULTS_DIR}/exec_time.log"
echo "Capturing execution time in ${exec_time_log}..."
pushd $SCALE_LOG_DIR > /dev/null
        execution_time ${exec_time_log} ${SCALE_LOG_DIR}
popd > /dev/null
sleep 5
echo ""
echo "Capturing execution time in ${exec_time_log}...done"

# Compare the expected results count in the db with the actual results count
actual_results_count=$(kubectl exec `kubectl get pods -o=name -n openshift-tuning | grep postgres` -n openshift-tuning -- psql -U admin -d kruizeDB -c "SELECT count(*) from public.kruize_results ;" | tail -3 | head -1 | tr -d '[:space:]')

expected_results_count=$((${num_exps} * ${num_clients} * ${num_days_of_res} * 96))
total_results_count=$((${expected_results_count} + ${total_results_count}))

j=0
while [[ ${total_results_count} != ${actual_results_count} ]]; do
	echo ""
	echo "expected results count = $expected_results_count actual_results_count = $actual_results_count"
	actual_results_count=$(kubectl exec `kubectl get pods -o=name -n openshift-tuning | grep postgres` -n openshift-tuning -- psql -U admin -d kruizeDB -c "SELECT count(*) from public.kruize_results ;" | tail -3 | head -1 | tr -d '[:space:]')

	expected_results_count=$((${num_exps} * ${num_clients} * ${num_days_of_res} * 96))
	total_results_count=$((${expected_results_count} + ${total_results_count}))
	if [ ${j} == 2 ]; then
		break
	else
		sleep 5
	fi
	j=$((${j} + 1))
done

exps_count=$(kubectl exec `kubectl get pods -o=name -n openshift-tuning | grep postgres` -n openshift-tuning -- psql -U admin -d kruizeDB -c "SELECT count(*) from public.kruize_experiments ;" | tail -3 | head -1 | tr -d '[:space:]')

echo ""
echo "###########################################################################"
echo "Scale test completed!"
echo "exps_count = $exps_count results_count = $actual_results_count"
if [ ${total_results_count} != ${actual_results_count} ]; then
	echo "Total expected results count = ${total_results_count} Actual results count = ${actual_results_count}"
	echo "Expected results count not found in kruize_results db table"
fi
echo "###########################################################################"

echo ""
echo ""
echo "###########################################################################"
echo "Summary of the test run"
exp_count=$(kubectl exec `kubectl get pods -o=name -n openshift-tuning | grep postgres` -n openshift-tuning -- psql -U admin -d kruizeDB -c "SELECT count(*) from public.kruize_experiments ;" | tail -3 | head -1 | tr -d '[:space:]')

results_count=$(kubectl exec `kubectl get pods -o=name -n openshift-tuning | grep postgres` -n openshift-tuning -- psql -U admin -d kruizeDB -c "SELECT count(*) from public.kruize_results ;" | tail -3 | head -1 | tr -d '[:space:]')

reco_count=$(kubectl exec `kubectl get pods -o=name -n openshift-tuning | grep postgres` -n openshift-tuning -- psql -U admin -d kruizeDB -c "SELECT count(*) from public.kruize_recommendations ;" | tail -3 | head -1 | tr -d '[:space:]')

echo "exp_count / results_count / reco_count = ${exp_count} / ${results_count} / ${reco_count}"

db_size=$(kubectl exec `kubectl get pods -o=name -n openshift-tuning | grep postgres` -n openshift-tuning -- psql -U admin -d kruizeDB -c "SELECT pg_database_size('kruizeDB') AS database_size_bytes;" | tail -3 | head -1 | tr -d '[:space:]')

db_size_mb=$((db_size / (1024 * 1024) + 1))

echo "Postgres DB size in MB = ${db_size_mb}"

echo "python3 parse_metrics.py -d "${RESULTS_DIR}/results" -r "${expected_results_count}""
python3 parse_metrics.py -d "${RESULTS_DIR}/results" -r "${expected_results_count}"

echo "###########################################################################"
echo ""
echo ""

kill $MYSELF 
wait
