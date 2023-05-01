#!/bin/bash

IP=192.168.49.2
PORT=31785
move_mins=0

# Outer loop
#
# Each loops kicks off 10000 experiments and posts results for 1 day
# This runs 15 times to post results for 15 days in all for all 10000 experiments
for oloop in {1..15};
do
	exp_start=1
	exp_end=100
	results_count=96
	logfile=scale_${exp_start}-${exp_end}.log

	# Inner loop
	#
	# Each loop starts 100 threads
	# Each thread creates 100 experiments and posts results for 1 day sequentially
	# thread 1 - exp 1 - 96 results, exp 2 - 96 results ... exp 100 - 96 results
	# thread 2 - exp 101 - 96 results, exp 102 - 96 results ... exp 200 - 96 results
	# ...
	# thread 100 - exp 9901 - 96 results, exp 9902 - 96 results ... exp 10000 - 96 results
	# Wait for all 100 threads to complete
	for iloop in {1..100};
	do
		day=$(((move_mins / 24 / 60) + 1))
		echo "Kicking off experiments: ${exp_start}..${exp_end}: Day: ${day}. Data in ${logfile}"

		nohup time python3 -u quickTestScalability.py --ip "${IP}" --port "${PORT}" --name scaleexp --count ${exp_start},${exp_end},${results_count} --measurement_mins=15 --move_mins=${move_mins} >> ${logfile} 2>&1 &

		exp_start=$((exp_start + 100))
		exp_end=$((exp_start + 99))
		logfile=scale_${exp_start}-${exp_end}.log
	done

	echo
	echo "###########################################################################"
	echo "#                                                                         #"
	echo "#     Kicked off 10000 experiments and data upload for day: ${oloop}            #"
	echo "#                                                                         #"
	echo "###########################################################################"
	echo

	wait

	echo
	echo "###########################################################################"
	echo "#                                                                         #"
	echo "#     Completed 10000 experiments and data upload for day: ${oloop}             #"
	echo "#                                                                         #"
	echo "###########################################################################"
	echo

	move_mins=$((oloop * 24 * 60))
done

echo "After all threads complete"
