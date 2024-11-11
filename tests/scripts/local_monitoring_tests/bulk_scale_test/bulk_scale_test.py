import requests
import argparse
import json
import subprocess
from time import sleep
from concurrent.futures import ThreadPoolExecutor, as_completed
import sys
sys.path.append("../../")
from helpers.kruize import *
from helpers.utils import *
import time

def invoke_bulk(worker_number, results_dir):
    try:
        response = bulk("./bulk_input.json")

        # Obtain the job id from the response from bulk service
        job_id_json = response.json()
        print(job_id_json)
        job_id = job_id_json['job_id']
        print(job_id)

        # Get the bulk job status using the job id
        verbose = "true"
        response = get_bulk_job_status(job_id, verbose)
        job_status_json = response.json()

        # Loop until job status is COMPLETED
        job_status = job_status_json['status']
        print(job_status)
        while job_status != "COMPLETED":
                response = get_bulk_job_status(job_id, verbose)
                job_status_json = response.json()
                job_status = job_status_json['status']
                sleep(5)

        print(f"worker number - {worker_number}")

        # Dump the job status json into a file
        job_file = results_dir + "/job_status" + str(worker_number) + ".json"
        with open(job_file, 'w') as f:
            json.dump(job_status_json, f, indent=4)

        print(job_status)

        # Fetch the list of experiments for which recommendations are available
        exp_list = job_status_json['data']['recommendations']['data']['processed']

        # List recommendations for the experiments for which recommendations are available
        recommendations_json_arr = []

        if exp_list != "":
            for exp_name in exp_list:
                response = list_recommendations(exp_name)
                reco = response.json()
                recommendations_json_arr.append(reco)

                # Dump the recommendations into a json file
                reco_file = results_dir + "/recommendations" + str(worker_number) + ".json"
                with open(reco_file, 'w') as f:
                    json.dump(recommendations_json_arr, f, indent=4)
            return 0
        else:
            print("Something went wrong! There are no experiments with recommendations!")
            return -1
    except Exception as e:
        return {'error': str(e)}

def parallel_requests_to_bulk(max_workers, results_dir):
    results = []
    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        # Submit all the tasks to the executor
        futures = [
            executor.submit(invoke_bulk, worker_number, results_dir)
            for worker_number in range(1, max_workers+1)
        ]
        
        # Process the results as they complete
        for future in as_completed(futures):
            try:
                result = future.result()
                results.append(result)
            except Exception as e:
                results.append({'error': str(e)})
    
    return results

if __name__ == '__main__':
    cluster_type = "openshift"
    max_workers = 5

    parser = argparse.ArgumentParser()

    # add the named arguments
    parser.add_argument('--workers', type=str, help='specify the number of workers')
    parser.add_argument('--startdate', type=str, help='Specify start date and time in  "%Y-%m-%dT%H:%M:%S.%fZ" format.')
    parser.add_argument('--interval', type=str, help='specify the interval hours')
    parser.add_argument('--resultsdir', type=str, help='specify the results dir')

    # parse the arguments from the command line
    args = parser.parse_args()

    if args.workers:
        max_workers = int(args.workers)

    if args.startdate:
        initial_end_date = args.startdate

    if args.interval:
        interval_hours = int(args.interval)

    if args.resultsdir:
        results_dir = args.resultsdir

    form_kruize_url(cluster_type)

    # Create the metric profile
    metric_profile_dir = get_metric_profile_dir()
    metric_profile_json_file = metric_profile_dir / 'resource_optimization_local_monitoring.json'
    create_metric_profile(metric_profile_json_file)

    start_time = time.time()
    responses = parallel_requests_to_bulk(max_workers, results_dir)

    # Print the results
    for i, response in enumerate(responses):
        print(f"Response {i+1}: {json.dumps(response, indent=2)}")

    end_time = time.time()
    exec_time = end_time - start_time
    print(f"Execution time: {exec_time} seconds")
