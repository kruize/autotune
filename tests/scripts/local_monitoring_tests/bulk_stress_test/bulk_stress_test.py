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

def fetch_bulk_recommendations(job_status_json, logger):
    logger.info("Fetching processed experiments...")
    exp_list = list(job_status_json["experiments"].keys())

    # List recommendations for the experiments for which recommendations are available
    if exp_list:
        reco_failures = 0
        for exp_name in exp_list:
            logger.info(f"Fetching recommendations for {exp_name}...")
            reco_response = job_status_json['experiments'][exp_name]['apis']['recommendations']['response']
            if reco_response:
                recommendations = reco_response[0]['kubernetes_objects'][0]['containers'][0]['recommendations']
                notifications_list = list(recommendations['notifications'].keys())
                for notification_code in notifications_list:
                    if notification_code == "111000":
                        reco_available_msg = recommendations['notifications'][notification_code]['message']
                        if reco_available_msg != "Recommendations Are Available":
                            reco_failures += 1
                            logger.info(f"Bulk recommendations failed for the experiment - {exp_name}!")
                            continue
                        else:
                            if prometheus == 0:
                                logger.info(f"Recommendations is not available for the experiment - {exp_name}!")
                                reco_failures += 1
                    elif notification_code == "120001" and prometheus == 0:
                        reco_failures += 1
                        logger.info(f"Recommendations is not available for the experiment - {exp_name}!")
            else:
                if prometheus == 0:
                    logger.info("Recommendations is not available!")
                    reco_failures += 1

        if reco_failures != 0:
            logger.info(
                f"Bulk recommendations failed for some of the experiments, check the logs for details!")
            return -1
        else:
            return 0
    else:
        logger.error("Something went wrong! There are no experiments with recommendations!")
        return -1

def fetch_bulk_job_status(job_id, worker_number, logger):
    # Get the bulk job status using the job id
    include = "summary"
    bulk_job_response = get_bulk_job_status(job_id, include, logger)
    job_status_json = bulk_job_response.json()

    # Loop until job status is COMPLETED
    job_status = job_status_json['summary']['status']

    while job_status != "COMPLETED":
        bulk_job_response = get_bulk_job_status(job_id, include, logger)
        job_status_json = bulk_job_response.json()
        job_status = job_status_json['summary']['status']
        total_exps = job_status_json['summary']['total_experiments']
        processed_exps = job_status_json['summary']['processed_experiments']
        logger.info(f"Total_experiments / Processed experiments - {total_exps} / {processed_exps}")
        if job_status == "FAILED":
            logger.info("Job FAILED!")
            break
        sleep(5)

    logger.info(f"worker number - {worker_number} job id - {job_id} job status - {job_status}")
    
    # Fetch Job status with experiments
    include = "summary,experiments"
    bulk_job_response = get_bulk_job_status(job_id, include, logger)
    job_status_json = bulk_job_response.json()

    # Dump the job status json into a file
    job_status_dir = results_dir + "/job_status_jsons"
    os.makedirs(job_status_dir, exist_ok=True)

    job_file = job_status_dir + "/job_status" + str(worker_number) + ".json"
    logger.info(f"Storing job status in {job_file}")
    with open(job_file, 'w') as f:
        json.dump(job_status_json, f, indent=4)

    # Fetch the list of experiments for which recommendations are available
    if job_status != "FAILED":
        #status = fetch_bulk_recommendations(job_status_json, worker_number, logger)
        status = fetch_bulk_recommendations(job_status_json, logger)
        return status
    else:
        logger.info(f"Check {job_file} for job status")
        return -1

def invoke_bulk(worker_number, start_time=None, end_time=None):
    try:
        stress_log_dir = results_dir + "/stress_logs"
        os.makedirs(stress_log_dir, exist_ok=True)

        log_file = f"{stress_log_dir}/worker_{worker_number}.log"
        logger = setup_logger(f"logger_{worker_number}", log_file)

        # Update the bulk json with start & end time
        logger.info(f"worker number = {worker_number}")
        logger.info(f"start time = {start_time}")
        logger.info(f"end time = {end_time}")

        if test == "time_range" or test == "time_range_split":
            bulk_json_file = "../json_files/bulk_input_timerange.json"
            json_file = open(bulk_json_file, "r")
            bulk_json = json.loads(json_file.read())

            bulk_json['time_range']['start'] = start_time
            bulk_json['time_range']['end'] = end_time
        elif test == "no_config":
            bulk_json_file = "../json_files/bulk_input.json"

            json_file = open(bulk_json_file, "r")
            bulk_json = json.loads(json_file.read())

        if prometheus == 1:
            logger.info("Datasource - prometheus-1")
            bulk_json['datasource'] = "prometheus-1"
            logger.info(bulk_json)
        else:
            logger.info("Datasource - thanos")
            logger.info(bulk_json)

        bulk_response = post_bulk_api(bulk_json, logger)

        # Obtain the job id from the response from bulk service
        job_id_json = bulk_response.json()

        job_id = job_id_json['job_id']
        logger.info(f"worker number - {worker_number} job id - {job_id}")

        # Get the bulk job status using the job id
        return_status = fetch_bulk_job_status(job_id, worker_number, logger)
        return return_status
    except Exception as e:
        print("Exception occurred:", repr(e))
        return {'error': str(e)}


def invoke_bulk_with_time_range(worker_number, start_time, end_time):
    try:
        stress_log_dir = results_dir + "/stress_logs"
        os.makedirs(stress_log_dir, exist_ok=True)

        log_file = f"{stress_log_dir}/worker_{worker_number}.log"
        logger = setup_logger(f"logger_{worker_number}", log_file)

        # Update the bulk json with start & end time
        logger.info(f"worker number = {worker_number}")
        logger.info(f"start time = {start_time}")
        logger.info(f"end time = {end_time}")

        # Update time range in the bulk input json
        bulk_json_file = "../json_files/bulk_input_timerange.json"

        json_file = open(bulk_json_file, "r")
        bulk_json = json.loads(json_file.read())

        bulk_json['time_range']['start'] = start_time
        bulk_json['time_range']['end'] = end_time

        if prometheus == 1:
            bulk_json['datasource'] = "prometheus-1"

        # Invoke the bulk service
        logger.info("Invoking bulk service with bulk json")
        bulk_response = post_bulk_api(bulk_json, logger)

        logger.info(bulk_json)

        # Obtain the job id from the response from bulk service
        job_id_json = bulk_response.json()

        job_id = job_id_json['job_id']
        logger.info(f"worker number - {worker_number} job id - {job_id}")

        # Get the bulk job status using the job id
        return_status = fetch_bulk_job_status(job_id, worker_number, logger)
        return return_status
    except Exception as e:
        return {'error': str(e)}

def parallel_requests_to_bulk():
    results = []

    current_start_time = datetime.strptime(initial_end_time, '%Y-%m-%dT%H:%M:%S.%fZ') - timedelta(hours=interval_hours)
    current_end_time = datetime.strptime(initial_end_time, '%Y-%m-%dT%H:%M:%S.%fZ')

    current_start_time = current_start_time.strftime('%Y-%m-%dT%H:%M:%S.000Z')
    current_end_time = current_end_time.strftime('%Y-%m-%dT%H:%M:%S.000Z')
    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        # Submit all the tasks to the executor
        futures = [
            executor.submit(invoke_bulk, worker_number, current_start_time, current_end_time) if test == "time_range" else executor.submit(invoke_bulk, worker_number)
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

def parallel_requests_with_time_range_split(max_workers):
    results = []
    print(f"days_of_res - {days_of_res}")
    print(f"interval_hours - {interval_hours}")
    actual_max_workers = int((days_of_res * 24) / interval_hours)

    print(f"max_workers - {max_workers} actual_max_workers - {actual_max_workers}")
    if max_workers > actual_max_workers:
        print(f"Max workers is capped at {actual_max_workers}")

    max_workers = actual_max_workers

    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        # Submit all the tasks to the executor
        futures = []
        current_end_time = initial_end_time
        for worker_number in range(1, max_workers+1):
                current_start_time = datetime.strptime(current_end_time, '%Y-%m-%dT%H:%M:%S.%fZ') - timedelta(hours=interval_hours)
                current_end_time = datetime.strptime(current_end_time, '%Y-%m-%dT%H:%M:%S.%fZ')

                current_start_time = current_start_time.strftime('%Y-%m-%dT%H:%M:%S.000Z')
                current_end_time = current_end_time.strftime('%Y-%m-%dT%H:%M:%S.000Z')

                executor.submit(invoke_bulk, worker_number, current_start_time, current_end_time)

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
    initial_end_time = ""
    interval_hours = 2
    test = ""
    prometheus = 0

    parser = argparse.ArgumentParser()

    # add the named arguments
    parser.add_argument('--test', type=str, help='specify the test to be run')
    parser.add_argument('--workers', type=str, help='specify the number of workers')
    parser.add_argument('--days_of_res', type=str, help='specify the number of days of results')
    parser.add_argument('--enddate', type=str, help='Specify end date and time of tsdb blocks in "%Y-%m-%dT%H:%M:%S.%fZ" format.')
    parser.add_argument('--interval', type=str, help='specify the interval hours')
    parser.add_argument('--resultsdir', type=str, help='specify the results dir')
    parser.add_argument('--prometheus', type=str, help='specify the value as 1 for prometheus, default is 0')

    # parse the arguments from the command line
    args = parser.parse_args()

    if args.test:
        test = args.test

    if args.workers:
        max_workers = int(args.workers)

    if args.workers:
        days_of_res = int(args.days_of_res)

    if args.enddate:
        initial_end_time = args.enddate

    if args.interval:
        interval_hours = int(args.interval)

    if args.resultsdir:
        results_dir = args.resultsdir

    if args.prometheus:
        prometheus = int(args.prometheus)

    form_kruize_url(cluster_type)

    # Create the metric profile
    metric_profile_dir = get_metric_profile_dir()
    metric_profile_json_file = metric_profile_dir / 'resource_optimization_local_monitoring.json'
    print(metric_profile_json_file) 
    create_metric_profile(metric_profile_json_file)

    # List datasources
    datasource_name = None
    list_response = list_datasources(datasource_name)

    # Create the metadata profile
    delete_and_create_metadata_profile()

    # Import datasource metadata
    input_json_file = "../json_files/thanos_import_metadata.json"
    if prometheus == 1:
        input_json_file = "../json_files/import_metadata.json"

    meta_response = import_metadata(input_json_file)
    metadata_json = meta_response.json()
    print(metadata_json)
    if meta_response.status_code != 201:
        print("Importing metadata from the datasource failed!")
        sys.exit(1)

    start_time = time.time()

    if test == "time_range" or test == "no_config":
        responses = parallel_requests_to_bulk()

    if test == "time_range_split":
        responses = parallel_requests_with_time_range_split(max_workers)

    # Print the results
    for i, response in enumerate(responses):
        print(f"Response {i+1}: {json.dumps(response, indent=2)}")

    end_time = time.time()
    exec_time = end_time - start_time
    print(f"Execution time: {exec_time} seconds")
