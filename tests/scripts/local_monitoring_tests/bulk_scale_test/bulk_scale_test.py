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

def build_bulk_configs(workers, configs_file):
    """
    Build the list of bulk configurations. Each configuration has a "name", "labels",
    and optional custom filter / datasource to be included in the bulk input json.
    - configs_file: json file containing a list of {"name": <optional>, "labels": {...}, "filter": {...}}
    - prometheus: labels are not used, so one configuration per worker
    """
    if prometheus != 0:
        return [{"name": f"default-{i}", "labels": {}} for i in range(1, workers + 1)]

    if not configs_file:
        configs_file = "configs.json"

    if os.path.exists(configs_file):
        with open(configs_file, "r") as f:
            configs = json.load(f)
        return [{
            "name": c.get("name", f"config-{i}"),
            "labels": c.get("labels", {}),
            "filter": c.get("filter", {}),
            "datasource": c.get("datasource")
        } for i, c in enumerate(configs, start=1)]

    print(f"Error: Configurations file '{configs_file}' not found!")
    sys.exit(1)

def update_bulk_config(config, current_start_time, current_end_time):
    # Update time range in the bulk input json
    bulk_json_file = "../json_files/bulk_input_timerange.json"

    with open(bulk_json_file, "r") as json_file:
        bulk_json = json.load(json_file)

    bulk_json['time_range']['start'] = current_start_time
    bulk_json['time_range']['end'] = current_end_time
    if prometheus == 0:
        if 'labels' in config and config['labels']:
            bulk_json['filter']['include']['labels'].update(config['labels'])
        if 'filter' in config and config['filter']:
            for filter_type in ['include', 'exclude']:
                if filter_type in config['filter']:
                    for key in ['namespace', 'workload', 'containers']:
                        if key in config['filter'][filter_type]:
                            bulk_json['filter'][filter_type][key] = config['filter'][filter_type][key]
                    if 'labels' in config['filter'][filter_type]:
                        bulk_json['filter'][filter_type]['labels'].update(config['filter'][filter_type]['labels'])
        if config.get('datasource'):
            bulk_json['datasource'] = config['datasource']
    else:
        bulk_json['datasource'] = "prometheus-1"

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

        # Evaluate the status only after all the experiments are checked
        if reco_failures != 0:
            logger.info("Bulk recommendations failed for some of the experiments, check the logs for details!")
            return -1
        return 0
    else:
        logger.error("Something went wrong! There are no experiments with recommendations!")
        return -1

def fetch_bulk_job_status(job_id, log_id, logger):
    # Get the bulk job status using the job id
    include = "summary,experiments"
    deadline = time.time() + job_timeout

    # Loop until job status is COMPLETED / FAILED or the timeout expires
    while True:
        bulk_job_response = get_bulk_job_status(job_id, include, logger)
        job_status_json = bulk_job_response.json()
        job_status = job_status_json['summary']['status']
        total_exps = job_status_json['summary']['total_experiments']
        processed_exps = job_status_json['summary']['processed_experiments']
        logger.info(f"Total_experiments / Processed experiments - {total_exps} / {processed_exps}")
        if job_status == "COMPLETED":
            break
        if job_status == "FAILED":
            logger.info("Job FAILED!")
            break
        if time.time() > deadline:
            logger.error(f"Job {job_id} did not complete within {job_timeout} seconds!")
            job_status = "FAILED"
            break
        sleep(5)

    logger.info(f"log id - {log_id} job id - {job_id} job status - {job_status}")

    # Dump the job status json into a file
    job_status_dir = results_dir + "/job_status_jsons"
    os.makedirs(job_status_dir, exist_ok=True)

    job_file = job_status_dir + "/job_status_" + str(log_id) + ".json"
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

def collect_kruize_metrics(resultsdir, worker_number, server):
    # Collect a single kruize metrics data point into the worker's own csv (one csv per worker,
    # a row is appended every tsdb block). kruize_metrics.py prefixes the file name with increase_ / total_
    script_path = "../../../../scripts/kruize_metrics.py"
    cluster = "openshift"

    timeout = "300m"
    csv_file = f"kruizeMetrics-worker-{worker_number}.csv"
    kruize_metrics_res_dir = f"{resultsdir}/results"

    params = ["python3", script_path, "-c", cluster, "-s", server, "-t", timeout, "-e", kruize_metrics_res_dir,
              "-r", csv_file]

    try:
        subprocess.run(params, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=600)
    except Exception as e:
        print(f"Failed to collect kruize metrics for worker {worker_number}: {e}")

def invoke_bulk_with_time_range_labels(resultsdir, config, current_start_time, current_end_time, worker_number, tsdb_id):
    try:
        scale_log_dir = resultsdir + "/scale_logs"
        os.makedirs(scale_log_dir, exist_ok=True)

        bulk_json = update_bulk_config(config, current_start_time, current_end_time)

        log_id = "tsdb-" + str(tsdb_id) + "_worker-" + str(worker_number) + "_" + config['name']

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

def run_worker(resultsdir, worker_number, worker_configs, current_start_time, current_end_time, tsdb_id, server):
    # A worker invokes the bulk service, one at a time, with each of the configurations assigned to it
    results = [invoke_bulk_with_time_range_labels(resultsdir, config, current_start_time, current_end_time,
                                                  worker_number, tsdb_id)
               for config in worker_configs]

    # Record the kruize metrics in the worker's csv once it is done with the tsdb block
    collect_kruize_metrics(resultsdir, worker_number, server)
    return results

def parallel_requests_with_labels(max_workers, resultsdir, initial_end_time, interval_hours, days_of_res, configs, server):
    results = []

    print(f"initial_end_time - {initial_end_time}")
    print(f"days_of_res - {days_of_res}")
    print(f"interval_hours - {interval_hours}")

    num_tsdb_blocks = int((days_of_res * 24) / interval_hours)

    print(f"num_tsdb_blocks - {num_tsdb_blocks}")

    # One thread per worker, configurations are distributed round-robin across the workers
    num_workers = min(max_workers, len(configs))
    if num_workers < max_workers:
        print(f"WARNING: only {len(configs)} bulk configuration(s) available, using {num_workers} worker(s) instead of {max_workers}")
    worker_configs = [configs[i::num_workers] for i in range(num_workers)]
    for i, wc in enumerate(worker_configs, start=1):
        print(f"worker {i} - {len(wc)} bulk configuration(s)")

    current_end_time = initial_end_time

    for k in range(1, num_tsdb_blocks + 1):

        current_start_time = datetime.strptime(current_end_time, '%Y-%m-%dT%H:%M:%S.%fZ') - timedelta(
            hours=interval_hours)
        current_end_time = datetime.strptime(current_end_time, '%Y-%m-%dT%H:%M:%S.%fZ')
        current_start_time = current_start_time.strftime('%Y-%m-%dT%H:%M:%S.000Z')
        current_end_time = current_end_time.strftime('%Y-%m-%dT%H:%M:%S.000Z')

        with ThreadPoolExecutor(max_workers=num_workers) as executor:
            # Submit one task per worker
            futures = [
                executor.submit(run_worker, resultsdir, worker_number, wc, current_start_time, current_end_time, k, server)
                for worker_number, wc in enumerate(worker_configs, start=1)
            ]

            # Process the results as they complete
            for future in as_completed(futures):
                try:
                    results.extend(future.result())
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
    prometheus = 0
    server = ""
    configs_file = "configs.json"
    job_timeout = 7200

    parser = argparse.ArgumentParser()

    # add the named arguments
    parser.add_argument('--workers', type=str, help='specify the number of parallel workers')
    parser.add_argument('--days_of_res', type=str, help='specify the number of days of results')
    parser.add_argument('--enddate', type=str, help='Specify end date and time of the tsdb block in "%%Y-%%m-%%dT%%H:%%M:%%S.%%fZ" format.')
    parser.add_argument('--interval', type=str, help='specify the interval hours')
    parser.add_argument('--resultsdir', type=str, help='specify the results dir')
    parser.add_argument('--prometheus', type=str, help='specify the value as 1 for prometheus, default is 0')
    parser.add_argument('--configs', type=str, default='configs.json', help='json file with a list of bulk configurations ({"name": ..., "labels": {...}}), distributed across workers. Default: configs.json')
    parser.add_argument('--job_timeout', type=str, help='max seconds to wait for a bulk job to complete, default is 7200')

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

    if args.prometheus:
        prometheus = int(args.prometheus)

    if args.configs:
        configs_file = args.configs

    if args.job_timeout:
        job_timeout = int(args.job_timeout)

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

    if prometheus == 0:
        if list_response_json['datasources'][0]['name'] != "thanos":
            print("Failed! Thanos datasource is not registered with Kruize!")
            sys.exit(1)
    else:
        if list_response_json['datasources'][0]['name'] != "prometheus-1":
            print("Failed! Prometheus-1 datasource is not registered with Kruize!")
            sys.exit(1)

    server = get_server()
    if server == "":
        print("Failed obtaining server details required for kruize_metrics script")
        sys.exit(1)

    start_time = time.time()
    print(f"initial_end_date to parallel requests - {initial_end_date}")
    bulk_configs = build_bulk_configs(max_workers, configs_file)
    print(f"workers - {max_workers}, bulk configurations - {len(bulk_configs)}")
    responses = parallel_requests_with_labels(max_workers, results_dir, initial_end_date, interval_hours, days_of_res, bulk_configs, server)

    # Print the results
    failed = 0
    for i, response in enumerate(responses):
        # print(f"Response {i+1}: {json.dumps(response)}")
        if response != 0:
            failed = 1

    end_time = time.time()
    exec_time = end_time - start_time
    print(f"Execution time: {exec_time} seconds")

       if failed == 0:
        sys.exit(0)
    else:
        sys.exit(1)

