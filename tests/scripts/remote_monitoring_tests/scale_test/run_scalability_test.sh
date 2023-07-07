#!/bin/bash

cluster_type="openshift"
move_mins=0
num_exps=10000
num_days_of_res=15
num_threads=100

function usage() {
	echo
	echo "Usage: ./run_scalability_test.sh -c cluster_type[minikube|openshift (default - openshift)] [-a IP] [-p PORT] [-u No. of experiments (default - 10000)]"
        echo "	     [-d No. of days of results (default - 15)] [-r <resultsdir path>]"
	exit -1
}

while getopts c:a:p:r:u:d:h gopts
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
	d)
		num_days_of_res="${OPTARG}"		
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

# Outer loop
#
# Each loops kicks off 10000 experiments and posts results for 1 day
# This runs 15 times to post results for 15 days in all for all 10000 experiments

num_exps_per_thread=$((num_exps / num_threads))
for ((oloop=1; oloop<=num_days_of_res; oloop++));
do
	exp_start=1
	exp_end=${num_exps_per_thread}
	results_count=96
	logfile="${SCALE_LOG_DIR}/scale_${exp_start}-${exp_end}.log"

	# Inner loop
	#
	# Each loop starts 100 threads
	# Each thread creates 100 experiments and posts results for 1 day sequentially
	# thread 1 - exp 1 - 96 results, exp 2 - 96 results ... exp 100 - 96 results
	# thread 2 - exp 101 - 96 results, exp 102 - 96 results ... exp 200 - 96 results
	# ...
	# thread 100 - exp 9901 - 96 results, exp 9902 - 96 results ... exp 10000 - 96 results
	# Wait for all 100 threads to complete
	for ((iloop=1; iloop<=num_threads; iloop++));
	do
		day=$(((move_mins / 24 / 60) + 1))
		echo "Kicking off experiments: ${exp_start}..${exp_end}: Day: ${day}. Data in ${logfile}"
		echo "nohup time python3 -u quickTestScalability.py --cluster_type "${cluster_type}" --ip "${IP}" --port "${PORT}" --name scaleexp --count ${exp_start},${exp_end},${results_count} --measurement_mins=15 --move_mins=${move_mins} >> ${logfile} 2>&1 &"

		nohup time python3 -u quickTestScalability.py --cluster_type "${cluster_type}" --ip "${IP}" --port "${PORT}" --name scaleexp --count ${exp_start},${exp_end},${results_count} --measurement_mins=15 --move_mins=${move_mins} >> ${logfile} 2>&1 &

		exp_start=$((exp_start + num_exps_per_thread))
		exp_end=$((exp_start + num_exps_per_thread - 1))
		logfile="${SCALE_LOG_DIR}/scale_${exp_start}-${exp_end}.log"
	done

	echo
	echo "###########################################################################"
	echo "#                                                                         #"
	echo "#     Kicked off ${num_exps} experiments and data upload for day: ${oloop}      #"
	echo "#                                                                         #"
	echo "###########################################################################"
	echo

	wait

	echo
	echo "###########################################################################"
	echo "#                                                                         #"
	echo "#     Completed ${num_exps} experiments and data upload for day: ${oloop}       #"
	echo "#                                                                         #"
	echo "###########################################################################"
	echo

	move_mins=$((oloop * 24 * 60))
done

echo "After all threads complete"
