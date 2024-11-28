"""
Copyright (c) 2024, 2024 Red Hat, IBM Corporation and others.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

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
import logging
from datetime import datetime, timedelta

def setup_logger(name, log_file, level=logging.INFO):

    handler = logging.FileHandler(log_file)
    formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
    handler.setFormatter(formatter)

    logger = logging.getLogger(name)
    logger.setLevel(level)
    logger.addHandler(handler)

    return logger

def invoke_bulk(worker_number, resultsdir):
    try:

        scale_log_dir = resultsdir + "/scale_logs"
        os.makedirs(scale_log_dir, exist_ok=True)

        log_file = f"{scale_log_dir}/worker_{worker_number}.log"
        logger = setup_logger(f"logger_{worker_number}", log_file)

        bulk_response = bulk("./bulk_input.json")

        # Obtain the job id from the response from bulk service
        job_id_json = bulk_response.json()

        job_id = job_id_json['job_id']
        logger.info(f"worker number - {worker_number} job id - {job_id}")

        # Get the bulk job status using the job id
        verbose = "true"
        bulk_job_response = get_bulk_job_status(job_id, verbose)
        job_status_json = bulk_job_response.json()

        # Loop until job status is COMPLETED
        job_status = job_status_json['status']

        while job_status != "COMPLETED":
                bulk_job_response = get_bulk_job_status(job_id, verbose)
                job_status_json = bulk_job_response.json()
                job_status = job_status_json['status']
                if job_status == "FAILED":
                    logger.info("Job FAILED!")
                    break
                sleep(5)

        logger.info(f"worker number - {worker_number} job id - {job_id} job status - {job_status}")

        # Dump the job status json into a file
        job_status_dir = results_dir + "/job_status_jsons"
        os.makedirs(job_status_dir, exist_ok=True)

        job_file = job_status_dir + "/job_status" + str(worker_number) + ".json"
        logger.info(f"Storing job status in {job_file}")
        with open(job_file, 'w') as f:
            json.dump(job_status_json, f, indent=4)

       # Fetch the list of experiments for which recommendations are available
        logger.info(f"Fetching processed experiments...")
        exp_list = list(job_status_json["experiments"].keys())

        logger.info(f"List of processed experiments")
        logger.info(f"**************************************************")
        logger.info(exp_list)
        logger.info(f"**************************************************")

        # List recommendations for the experiments for which recommendations are available
        recommendations_json_arr = []

        if exp_list != "":
            list_reco_failures = 0
            for exp_name in exp_list:

                logger.info(f"Fetching recommendations for {exp_name}...")
                list_reco_response = list_recommendations(exp_name)
                if list_reco_response.status_code != 200:
                    list_reco_failures = list_reco_failures + 1
                    logger.info(f"List recommendations failed for the experiment - {exp_name}!")
                    reco = list_reco_response.json()
                    logger.info(reco)
                    continue
                else:
                    logger.info(f"Fetched recommendations for {exp_name} - Done")

                reco = list_reco_response.json()
                recommendations_json_arr.append(reco)

            # Dump the recommendations into a json file
            reco_dir = results_dir + "/recommendation_jsons"
            os.makedirs(reco_dir, exist_ok=True)
            reco_file = reco_dir + "/recommendations" + str(worker_number) + ".json"
            with open(reco_file, 'w') as f:
                json.dump(recommendations_json_arr, f, indent=4)

            if list_reco_failures != 0:
                logger.info("List recommendations failed for some of the experiments, check the log {log_file} for details!")
                return -1
            else:
                return 0
        else:
            logger.error("Something went wrong! There are no experiments with recommendations!")
            return -1
    except Exception as e:
        return {'error': str(e)}


def invoke_bulk_with_time_range(worker_number, resultsdir, start_time, end_time):
    try:

        scale_log_dir = resultsdir + "/scale_logs"
        os.makedirs(scale_log_dir, exist_ok=True)

        log_file = f"{scale_log_dir}/worker_{worker_number}.log"
        logger = setup_logger(f"logger_{worker_number}", log_file)

        # Update the bulk json with start & end time
        logger.info(f"worker number = {worker_number}")
        logger.info(f"start time = {start_time}")
        logger.info(f"end time = {end_time}")

        # Update time range in the bulk input json
        bulk_input_json_file = "./bulk_input.json"
        data = json.load(open(bulk_input_json_file))
        data['time_range']['start'] = start_time
        data['time_range']['end'] = end_time

        logger.info(data)

        tmp_file = "/tmp/bulk_input_" + str(worker_number) + ".json"
        with open(tmp_file, 'w') as f:
             json.dump(data, f)

        # Invoke the bulk service
        bulk_response = bulk(tmp_file)

        # Obtain the job id from the response from bulk service
        job_id_json = bulk_response.json()

        job_id = job_id_json['job_id']
        logger.info(f"worker number - {worker_number} job id - {job_id}")

        # Get the bulk job status using the job id
        verbose = "true"
        bulk_job_response = get_bulk_job_status(job_id, verbose)
        job_status_json = bulk_job_response.json()

        # Loop until job status is COMPLETED
        job_status = job_status_json['status']
        print(job_status)
        while job_status != "COMPLETED":
                bulk_job_response = get_bulk_job_status(job_id, verbose)
                job_status_json = bulk_job_response.json()
                job_status = job_status_json['status']
                if job_status == "FAILED":
                    logger.info("Job FAILED!")
                    break
                sleep(5)

        logger.info(f"worker number - {worker_number} job id - {job_id} job status - {job_status}")

        # Dump the job status json into a file
        job_status_dir = results_dir + "/job_status_jsons"
        os.makedirs(job_status_dir, exist_ok=True)

        job_file = job_status_dir + "/job_status" + str(worker_number) + ".json"
        logger.info(f"Storing job status in {job_file}")
        with open(job_file, 'w') as f:
            json.dump(job_status_json, f, indent=4)

        # Fetch the list of experiments for which recommendations are available
        logger.info("Fetching processed experiments...")
        if job_status_json['status'] == "COMPLETED":
            exp_list = list(job_status_json["experiments"].keys())
        else:
            exp_list = ""
            logger.info(f"No processed experiments, job status - {jpb_status_json['status']}")

        logger.info("List of processed experiments")
        logger.info("**************************************************")
        logger.info(exp_list)
        logger.info("**************************************************")

        # List recommendations for the experiments for which recommendations are available
        recommendations_json_arr = []

        if exp_list != "":
            list_reco_failures = 0
            for exp_name in exp_list:
                logger.info(f"Fetching recommendations for {exp_name}...")
                list_reco_response = list_recommendations(exp_name)
                if list_reco_response.status_code != 200:
                   list_reco_failures = list_reco_failures + 1
                   logger.info(f"List recommendations failed for the experiment - {exp_name}!")
                   reco = list_reco_response.json()
                   logger.info(reco)
                   continue
                else:
                    logger.info(f"Fetched recommendations for {exp_name} - Done")

                reco = list_reco_response.json()
                recommendations_json_arr.append(reco)

            reco_dir = results_dir + "/recommendation_jsons"
            os.makedirs(reco_dir, exist_ok=True)
            reco_file = reco_dir + "/recommendations" + str(worker_number) + ".json"
            with open(reco_file, 'w') as f:
                json.dump(recommendations_json_arr, f, indent=4)

            if list_reco_failures != 0:
                logger.info(
                    "List recommendations failed for some of the experiments, check the log {log_file} for details!")
                return -1
            else:
                return 0
        else:
            logger.error("Something went wrong! There are no experiments with recommendations!")
            return -1
    except Exception as e:
        return {'error': str(e)}

def parallel_requests_to_bulk(workers, resultsdir):
    results = []
    with ThreadPoolExecutor(max_workers=workers) as executor:
        # Submit all the tasks to the executor
        futures = [
            executor.submit(invoke_bulk, worker_number, resultsdir)
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

def parallel_requests_with_time_range(max_workers, resultsdir, initial_end_time, interval_hours, days_of_res):
    results = []

    print(f"days_of_res - {days_of_res}")
    print(f"interval_hours - {interval_hours}")
    actual_max_workers = int((days_of_res * 24) / interval_hours)

    print(f"max_workers - {max_workers} actual_max_workers - {actual_max_workers}")
    if max_workers > actual_max_workers:
        print(f"Actual max workers is capped at {actual_max_workers}")

    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        # Submit all the tasks to the executor
        futures = []
        current_end_time = initial_end_time
        for worker_number in range(1, max_workers+1):
                current_start_time = datetime.strptime(current_end_time, '%Y-%m-%dT%H:%M:%S.%fZ') - timedelta(hours=interval_hours)
                current_end_time = datetime.strptime(current_end_time, '%Y-%m-%dT%H:%M:%S.%fZ')
                print(current_start_time)
                current_start_time = current_start_time.strftime('%Y-%m-%dT%H:%M:%S.000Z')
                current_end_time = current_end_time.strftime('%Y-%m-%dT%H:%M:%S.000Z')
                executor.submit(invoke_bulk_with_time_range, worker_number, resultsdir, current_start_time, current_end_time)
                current_end_time = current_start_time

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
    days_of_res = 15
    results_dir = "."
    initial_end_date = ""
    interval_hours = 6

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
    print(metric_profile_json_file)
    create_metric_profile(metric_profile_json_file)

    # List datasources
    datasource_name = None
    list_response = list_datasources(datasource_name)

    # Import datasource metadata
    input_json_file = "./import_metadata.json"
    meta_response = import_metadata(input_json_file)
    metadata_json = meta_response.json()
    print(metadata_json)
    if meta_response.status_code != 201:
        print("Importing metadata from the datasource failed!")
        sys.exit(1)

    start_time = time.time()
    #responses = parallel_requests_to_bulk(max_workers, results_dir)
    responses = parallel_requests_with_time_range(max_workers, results_dir, initial_end_date, interval_hours, days_of_res)

    # Print the results
    for i, response in enumerate(responses):
        print(f"Response {i+1}: {json.dumps(response, indent=2)}")

    end_time = time.time()
    exec_time = end_time - start_time
    print(f"Execution time: {exec_time} seconds")
