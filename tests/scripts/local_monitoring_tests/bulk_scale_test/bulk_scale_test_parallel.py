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

def update_bulk_config(org_id, cluster_id, current_start_time, current_end_time):
    org_value = "org-" + str(org_id)
    cluster_value = "eu-" + str(org_id) + "-" + str(cluster_id)

    new_labels = {
        "org_id": org_value,
        "cluster_id": cluster_value
    }

    # Update time range in the bulk input json
    bulk_json_file = "../json_files/bulk_input_timerange.json"

    json_file = open(bulk_json_file, "r")
    bulk_json = json.loads(json_file.read())

    bulk_json['time_range']['start'] = current_start_time
    bulk_json['time_range']['end'] = current_end_time
    bulk_json['filter']['include']['labels'].update(new_labels)

    return bulk_json

def invoke_bulk_with_time_range_labels(resultsdir, chunk, current_start_time, current_end_time):
    try:
        for org_id, cluster_id in chunk:
            #time.sleep(delay)

            scale_log_dir = resultsdir + "/scale_logs"
            os.makedirs(scale_log_dir, exist_ok=True)

            bulk_json = update_bulk_config(org_id, cluster_id, current_start_time, current_end_time)

            log_id = str(org_id) + "-" + str(cluster_id)
            log_file = f"{scale_log_dir}/worker_{log_id}.log"

            logger = setup_logger(f"logger_{log_id}", log_file)

            # Invoke the bulk service
            logger.info("Invoking bulk service with bulk json")
            logger.info(bulk_json)
            bulk_response = post_bulk_api(bulk_json, logger)

            # Obtain the job id from the response from bulk service
            job_id_json = bulk_response.json()

            job_id = job_id_json['job_id']
            logger.info(f"log_id - {log_id} job id - {job_id}")

            # Get the bulk job status using the job id
            verbose = "true"
            bulk_job_response = get_bulk_job_status(job_id, verbose, logger)
            job_status_json = bulk_job_response.json()

            # Loop until job status is COMPLETED
            job_status = job_status_json['status']

            while job_status != "COMPLETED":
                bulk_job_response = get_bulk_job_status(job_id, verbose, logger)
                job_status_json = bulk_job_response.json()
                job_status = job_status_json['status']
                if job_status == "FAILED":
                    logger.info("Job FAILED!")
                    break
                sleep(5)

            logger.info(f"worker number - {log_id} job id - {job_id} job status - {job_status}")

            # Dump the job status json into a file
            job_status_dir = results_dir + "/job_status_jsons"
            os.makedirs(job_status_dir, exist_ok=True)

            job_file = job_status_dir + "/job_status" + log_id + ".json"
            logger.info(f"Storing job status in {job_file}")
            with open(job_file, 'w') as f:
                json.dump(job_status_json, f, indent=4)

            # Fetch the list of experiments for which recommendations are available
            if job_status != "FAILED":
                logger.info("Fetching processed experiments...")
                exp_list = list(job_status_json["experiments"].keys())

                logger.info("List of processed experiments")
                logger.info("**************************************************")
                logger.info(exp_list)
                logger.info("**************************************************")

                # List recommendations for the experiments for which recommendations are available
                recommendations_json_arr = []

                if exp_list:
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
                    reco_file = reco_dir + "/recommendations" + log_id + ".json"
                    with open(reco_file, 'w') as f:
                        json.dump(recommendations_json_arr, f, indent=4)

                    if list_reco_failures != 0:
                        logger.info(
                            f"List recommendations failed for some of the experiments, check the log {log_file} for details!")
                        return -1
                    else:
                        return 0
                else:
                    logger.error("Something went wrong! There are no experiments with recommendations!")
                    return -1
            else:
                logger.info(f"Check {job_file} for job status")
                return -1
    except Exception as e:
        return {'error': str(e)}

def parallel_requests_with_labels(max_workers, resultsdir, initial_end_time, interval_hours, days_of_res, org_ids, cluster_ids, chunk_size, interval_seconds):
    results = []

    print(f"initial_end_time - {initial_end_time}")
    print(f"days_of_res - {days_of_res}")
    print(f"interval_hours - {interval_hours}")
    num_tsdb_blocks = int((days_of_res * 24) / interval_hours)

    print(f"num_tsdb_blocks - {num_tsdb_blocks}")
    print(f"org_ids - {org_ids}")
    print(f"cluster_ids - {cluster_ids}")
    print(f"chunk_size - {chunk_size}")

    current_end_time = initial_end_time

    for k in range(1, num_tsdb_blocks + 1):

        current_start_time = datetime.strptime(current_end_time, '%Y-%m-%dT%H:%M:%S.%fZ') - timedelta(
            hours=interval_hours)
        current_end_time = datetime.strptime(current_end_time, '%Y-%m-%dT%H:%M:%S.%fZ')

        current_start_time = current_start_time.strftime('%Y-%m-%dT%H:%M:%S.000Z')
        current_end_time = current_end_time.strftime('%Y-%m-%dT%H:%M:%S.000Z')
        print(f"Invoking bulk service with the tsdb time range {current_start_time} and {current_end_time}")

        # Create all tasks
        tasks = [(org_id, cluster_id) for org_id in range(1, org_ids + 1) for cluster_id in range(1, cluster_ids + 1)]

        # Divide tasks into chunks
        chunks = [tasks[i:i + chunk_size] for i in range(0, len(tasks), chunk_size)]
        results = []
        with ThreadPoolExecutor(max_workers=max_workers) as executor:
            futures = []
            for chunk in chunks:
               executor.submit(invoke_bulk_with_time_range_labels, resultsdir, chunk, current_start_time, current_end_time)

            for future in as_completed(futures):
                try:
                    chunk = future.result()
                    results.append(chunk_results)
                except Exception as e:
                    print(f"Error processing chunk: {e}")

        current_end_time = current_start_time

    return results

if __name__ == '__main__':
    cluster_type = "openshift"
    max_workers = 1
    days_of_res = 1
    results_dir = "."
    initial_end_date = "2024-12-10T11:50:00.001Z"
    interval_hours = 6
    org_ids = 10
    cluster_ids = 10
    chunk_size = 10
    rampup_interval_seconds = 2

    parser = argparse.ArgumentParser()

    # add the named arguments
    parser.add_argument('--workers', type=str, help='specify the number of workers')
    parser.add_argument('--enddate', type=str, help='Specify end date and time of the tsdb block in "%Y-%m-%dT%H:%M:%S.%fZ" format.')
    parser.add_argument('--interval', type=str, help='specify the interval hours')
    parser.add_argument('--resultsdir', type=str, help='specify the results dir')
    parser.add_argument('--org_ids', type=str, help='specify the no. of orgs')
    parser.add_argument('--cluster_ids', type=str, help='specify the no. of clusters / org')
    parser.add_argument('--chunk_size', type=str, help='specify the chunk size to be processed parallely')

    # parse the arguments from the command line
    args = parser.parse_args()

    if args.workers:
        max_workers = int(args.workers)

    if args.enddate:
        initial_end_date = args.enddate

    if args.interval:
        interval_hours = int(args.interval)

    if args.resultsdir:
        results_dir = args.resultsdir

    if args.org_ids:
        org_ids = int(args.org_ids)

    if args.cluster_ids:
        org_ids = int(args.cluster_ids)

    if args.chunk_size:
        org_ids = int(args.chunk_size)

    form_kruize_url(cluster_type)

    # Create the metric profile
    metric_profile_dir = get_metric_profile_dir()
    metric_profile_json_file = metric_profile_dir / 'resource_optimization_local_monitoring.json'
    print(metric_profile_json_file) 
    create_metric_profile(metric_profile_json_file)

    # List datasources
    datasource_name = None
    list_response = list_datasources(datasource_name)
    list_response_json = list_response.json()

    if list_response_json['datasources'][0]['name'] != "thanos":
        print("Failed! Thanos datasource is not registered with Kruize!")
        sys.exit(1)

    start_time = time.time()
    responses = parallel_requests_with_labels(max_workers, results_dir, initial_end_date, interval_hours, days_of_res, org_ids, cluster_ids,
                    chunk_size, rampup_interval_seconds)

    # Print the results
    print("\n*************************************************")
    print(responses)
    print("\n*************************************************")
    for i, response in enumerate(responses):
        print(f"Response {i+1}: {json.dumps(response, indent=2)}")

    end_time = time.time()
    exec_time = end_time - start_time
    print(f"Execution time: {exec_time} seconds")
