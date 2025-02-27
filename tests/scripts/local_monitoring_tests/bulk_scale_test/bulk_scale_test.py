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

def get_server():
    try:
        # Run the oc status command
        cmd = ["oc", "status", "--namespace=openshift-tuning"]
        result = subprocess.run(cmd, capture_output=True, text=True, check=True)

        lines = [line for line in result.stdout.splitlines() if "kruize" in line and "port" in line]

        server = ""
        for line in lines:
            parts = line.split()
            if len(parts) > 0:
                server = parts[0].split("/")[2].split(".", 2)[-1]
                print(server)
        return server

    except subprocess.CalledProcessError as e:
        print("Error running oc command:", e)
        return server


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

def fetch_bulk_recommendations(job_status_json, logger):
    logger.info("Fetching processed experiments...")
    exp_list = list(job_status_json["experiments"].keys())

    logger.info("List of processed experiments")
    logger.info("**************************************************")
    logger.info(exp_list)
    logger.info("**************************************************")

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
                            logger.info(f"'Recommendations Are Available' message is not found in 1110000 notification code - {exp_name}!")
                            continue
                    elif notification_code == "120001" and prometheus == 0:
                        reco_failures += 1
                        logger.info(f"Not enough data! Recommendations is not available for the experiment - {exp_name}!")
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
    include = "summary,experiments"
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

    # Dump the job status json into a file
    job_status_dir = results_dir + "/job_status_jsons"
    os.makedirs(job_status_dir, exist_ok=True)

    job_file = job_status_dir + "/job_status" + str(worker_number) + ".json"
    logger.info(f"Storing job status in {job_file}")
    with open(job_file, 'w') as f:
        json.dump(job_status_json, f, indent=4)

    # Fetch the list of experiments for which recommendations are available
    if job_status != "FAILED":
        status = fetch_bulk_recommendations(job_status_json, logger)
        return status
    else:
        logger.info(f"Check {job_file} for job status")
        return -1

def invoke_bulk_with_time_range_labels(resultsdir, org_id, cluster_id, current_start_time, current_end_time, worker_number, tsdb_id, server):
    try:
        script_path = "../../../../scripts/kruize_metrics.py"
        cluster = "openshift"

        timeout = "60m"
        csv_file = f"kruizeMetrics-{tsdb_id}.csv"
        kruize_metrics_res_dir = f"{resultsdir}/results"

        params = ["python3", script_path, "-c", cluster, "-s", server, "-t", timeout, "-e", kruize_metrics_res_dir,
                  "-r", csv_file]

        subprocess.Popen(params, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        scale_log_dir = resultsdir + "/scale_logs"
        os.makedirs(scale_log_dir, exist_ok=True)

        bulk_json = update_bulk_config(org_id, cluster_id, current_start_time, current_end_time)

        log_id = "tsdb-" + str(tsdb_id) + "_worker-" + str(worker_number) + "_org-" + str(org_id) + "_cluster-" + str(cluster_id)
        log_file = f"{scale_log_dir}/worker_{log_id}.log"

        logger = setup_logger(f"logger_{log_id}", log_file)
        logger.info(f"log id = {log_id}")

        # Invoke the bulk service
        logger.info("Invoking bulk service with bulk json")
        bulk_response = post_bulk_api(bulk_json, logger)

        # Obtain the job id from the response from bulk service
        job_id_json = bulk_response.json()

        job_id = job_id_json['job_id']

        # Get the bulk job status using the job id
        return_status = fetch_bulk_job_status(job_id, log_id, logger)
        return return_status

    except Exception as e:
        return {'error': str(e)}

def parallel_requests_with_labels(max_workers, resultsdir, initial_end_time, interval_hours, days_of_res, org_ids, cluster_ids, server):
    results = []

    print(f"initial_end_time - {initial_end_time}")
    print(f"days_of_res - {days_of_res}")
    print(f"interval_hours - {interval_hours}")

    num_tsdb_blocks = int((days_of_res * 24) / interval_hours)

    print(f"num_tsdb_blocks - {num_tsdb_blocks}")

    current_end_time = initial_end_time

    for k in range(1, num_tsdb_blocks + 1):

        current_start_time = datetime.strptime(current_end_time, '%Y-%m-%dT%H:%M:%S.%fZ') - timedelta(
            hours=interval_hours)
        current_end_time = datetime.strptime(current_end_time, '%Y-%m-%dT%H:%M:%S.%fZ')
        current_start_time = current_start_time.strftime('%Y-%m-%dT%H:%M:%S.000Z')
        current_end_time = current_end_time.strftime('%Y-%m-%dT%H:%M:%S.000Z')

        with ThreadPoolExecutor(max_workers=max_workers) as executor:
            # Submit all the tasks to the executor
            futures = []

            futures = [
            executor.submit(invoke_bulk_with_time_range_labels, resultsdir, org_id, cluster_id, current_start_time, current_end_time, worker_number, k, server)
                for worker_number, (org_id, cluster_id) in enumerate(
                    ((org_id, cluster_id) for org_id in range(1, org_ids + 1) for cluster_id in
                     range(1, cluster_ids + 1)),
                    start=1
                )
            ]

            # Process the results as they complete
            for future in as_completed(futures):
                try:
                    result = future.result()
                    results.append(result)
                except Exception as e:
                    results.append({'error': str(e)})
        

        current_end_time = current_start_time

    return results

if __name__ == '__main__':
    cluster_type = "openshift"
    max_workers = 10
    days_of_res = 1
    results_dir = "."
    initial_end_date = "2024-12-10T11:50:00.001Z"
    interval_hours = 6
    org_ids = 10
    cluster_ids = 10
    prometheus = 0
    server = ""

    parser = argparse.ArgumentParser()

    # add the named arguments
    parser.add_argument('--workers', type=str, help='specify the number of parallel workers')
    parser.add_argument('--days_of_res', type=str, help='specify the number of days of results')
    parser.add_argument('--enddate', type=str, help='Specify end date and time of the tsdb block in "%Y-%m-%dT%H:%M:%S.%fZ" format.')
    parser.add_argument('--interval', type=str, help='specify the interval hours')
    parser.add_argument('--resultsdir', type=str, help='specify the results dir')
    parser.add_argument('--org_ids', type=str, help='specify the no. of orgs')
    parser.add_argument('--cluster_ids', type=str, help='specify the no. of clusters / org')
    parser.add_argument('--prometheus', type=str, help='specify the value as 1 for prometheus, default is 0')

    # parse the arguments from the command line
    args = parser.parse_args()

    if args.workers:
        max_workers = int(args.workers)

    if args.days_of_res:
        days_of_res = int(args.days_of_res)

    if args.enddate:
        initial_end_date = args.enddate

    if args.interval:
        interval_hours = int(args.interval)

    if args.resultsdir:
        results_dir = args.resultsdir

    if args.org_ids:
        org_ids = int(args.org_ids)

    if args.cluster_ids:
        cluster_ids = int(args.cluster_ids)

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
    list_response_json = list_response.json()

    if list_response_json['datasources'][0]['name'] != "thanos":
        print("Failed! Thanos datasource is not registered with Kruize!")
        sys.exit(1)

    server = get_server()
    if server == "":
        print("Failed obtaining server details required for kruize_metrics script")
        sys.exit(1)

    start_time = time.time()
    print(f"initial_end_date to parallel requests - {initial_end_date}")
    responses = parallel_requests_with_labels(max_workers, results_dir, initial_end_date, interval_hours, days_of_res, org_ids, cluster_ids, server)

    # Print the results
    for i, response in enumerate(responses):
        print(f"Response {i+1}: {json.dumps(response, indent=2)}")

    end_time = time.time()
    exec_time = end_time - start_time
    print(f"Execution time: {exec_time} seconds")
