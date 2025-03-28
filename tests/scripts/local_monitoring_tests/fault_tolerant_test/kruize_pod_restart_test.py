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

import getopt
import json
import os
import sys
import time

sys.path.append("../../")
from helpers.kruize import *
from helpers.utils import *
from helpers.generate_rm_jsons import *


def main(argv):
    cluster_type = "minikube"
    results_dir = "."
    failed = 0
    try:
        opts, args = getopt.getopt(argv, "h:c:a:u:r:")
    except getopt.GetoptError:
        print("kruize_pod_restart_test.py -c <cluster type> -a <openshift kruize route> -r <results dir>")
        print("Note: -a option is required only on openshift when kruize service is exposed")
        sys.exit(2)
    for opt, arg in opts:
        if opt == '-h':
            print("kruize_pod_restart_test.py -c <cluster type> -a <openshift kruize route> -r <results dir>")
            sys.exit(0)
        elif opt == '-c':
            cluster_type = arg
        elif opt == '-a':
            server_ip_addr = arg
        elif opt == '-r':
            results_dir = arg

    print(f"Cluster type = {cluster_type}")
    print(f"Results dir = {results_dir}")

    # Form the kruize url
    if cluster_type == "minikube":
        form_kruize_url(cluster_type)
        namespace = "monitoring"
    else:
        form_kruize_url(cluster_type, server_ip_addr)
        namespace = "openshift-tuning"

    # Create the metric profile
    metric_profile_dir = get_metric_profile_dir()
    metric_profile_json_file = metric_profile_dir / 'resource_optimization_local_monitoring.json'
    create_metric_profile(metric_profile_json_file)

    # Create the metadata profile
    delete_and_create_metadata_profile()

    list_reco_json_dir = results_dir + "/list_reco_jsons"
    list_datasources_json_dir = results_dir + "/list_datasources_jsons"
    list_metadata_json_dir = results_dir + "/list_metadata_jsons"
    list_exp_json_dir = results_dir + "/list_exp_jsons"

    os.mkdir(list_exp_json_dir)
    os.mkdir(list_reco_json_dir)
    os.mkdir(list_datasources_json_dir)
    os.mkdir(list_metadata_json_dir)

    reco_json_dir = results_dir + "/reco_jsons"
    os.mkdir(reco_json_dir)

    # Import Datasource metadata
    input_json_file = "../json_files/import_metadata.json"

    response = delete_metadata(input_json_file)
    print("delete metadata = ", response.status_code)

    # Import metadata using the specified json
    response = import_metadata(input_json_file)
    metadata_json = response.json()

    print(metadata_json)

    # Create the experiment 
    create_exp_json_file = "../json_files/create_tfb_exp.json"
    create_experiment(create_exp_json_file)

    # Obtain the experiment name
    json_data = json.load(open(create_exp_json_file))

    experiment_name = json_data[0]['experiment_name']
    print(f"experiment_name = {experiment_name}")

    # Fetch the recommendations for all the experiments
    reco = generate_recommendations(experiment_name)
    filename = reco_json_dir + '/generate_reco.json'
    write_json_data_to_file(filename, reco.json())

    # Fetch listExperiments
    list_exp_json_file_before = list_exp_json_dir + "/list_exp_json_before.json"
    # assign params to be passed in listExp
    results = "false"
    recommendations = "true"
    latest = "false"
    experiment_name = None
    response = list_experiments(results, recommendations, latest, experiment_name)
    if response.status_code == SUCCESS_200_STATUS_CODE:
        list_exp_json = response.json()
    else:
        print(f"listExperiments failed!")
        failed = 1
        sys.exit(1)

    write_json_data_to_file(list_exp_json_file_before, list_exp_json)

    # Fetch the recommendations for all the experiments
    experiment_name = None
    latest = "false"
    interval_end_time = None
    response = list_recommendations(experiment_name, latest, interval_end_time)
    if response.status_code == SUCCESS_200_STATUS_CODE:
        list_reco_json_file_before = list_reco_json_dir + '/list_reco_json_before.json'
        write_json_data_to_file(list_reco_json_file_before, response.json())
    else:
        print(f"listRecommendations for experiment name - ${experiment_name} failed!")
        failed = 1
        sys.exit(1)

    # Fetch the datasources
    name = None
    response = list_datasources(name)
    if response.status_code == SUCCESS_200_STATUS_CODE:
        list_datasources_json_file_before = list_datasources_json_dir + '/list_datasources_json_before.json'
        write_json_data_to_file(list_datasources_json_file_before, response.json())
    else:
        print(f"list Datasources failed!")
        failed = 1
        sys.exit(1)

    # Fetch the datasource metadata
    json_data = json.load(open(input_json_file))
    datasource_name = json_data['datasource_name']
    cluster_name = None
    ns = None
    verbose = "true"

    response = list_metadata(datasource_name, cluster_name, ns, verbose)

    if response.status_code == SUCCESS_200_STATUS_CODE:
        list_metadata_json_file_before = list_metadata_json_dir + '/list_metadata_json_before.json'
        write_json_data_to_file(list_metadata_json_file_before, response.json())
    else:
        print(f"list metadata failed!")
        failed = 1
        sys.exit(1)

    # Delete the kruize pod
    delete_kruize_pod(namespace)

    # Check if the kruize pod is running
    pod_name = get_kruize_pod(namespace)
    result = check_pod_running(namespace, pod_name)

    # Sleep for a while
    time.sleep(60)

    if result == False:
        print("Restarting kruize failed!")
        failed = 1
        sys.exit(failed)

    # Fetch listExperiments
    list_exp_json_file_after = list_exp_json_dir + "/list_exp_json_after.json"
    results = "false"
    recommendations = "true"
    latest = "false"
    experiment_name = None
    response = list_experiments(results, recommendations, latest, experiment_name)
    if response.status_code == SUCCESS_200_STATUS_CODE:
        list_exp_json = response.json()
    else:
        print(f"listExperiments failed!")
        failed = 1
        sys.exit(1)

    write_json_data_to_file(list_exp_json_file_after, list_exp_json)

    # Fetch the recommendations for all the experiments
    latest = "false"
    interval_end_time = None
    response = list_recommendations(experiment_name, latest, interval_end_time)
    if response.status_code == SUCCESS_200_STATUS_CODE:
        list_reco_json_file_after = list_reco_json_dir + '/list_reco_json_after.json'
        write_json_data_to_file(list_reco_json_file_after, response.json())
    else:
        print(f"listRecommendations for experiment name - ${experiment_name} failed")
        failed = 1
        sys.exit(1)

    # Fetch the datasources
    name = None
    response = list_datasources(name)
    if response.status_code == SUCCESS_200_STATUS_CODE:
        list_datasources_json_file_after = list_datasources_json_dir + '/list_datasources_json_after.json'
        write_json_data_to_file(list_datasources_json_file_after, response.json())
    else:
        print(f"list Datasources failed!")
        failed = 1
        sys.exit(1)

    # Fetch the datasource metadata
    response = list_metadata(datasource_name, cluster_name, ns, verbose)
    if response.status_code == SUCCESS_200_STATUS_CODE:
        list_metadata_json_file_after = list_metadata_json_dir + '/list_metadata_json_after.json'
        write_json_data_to_file(list_metadata_json_file_after, response.json())
    else:
        print(f"list metadata failed!")
        failed = 1
        sys.exit(1)

    # Compare the listExperiments before and after kruize pod restart
    result = compare_json_files(list_exp_json_file_before, list_exp_json_file_after)
    if result == True:
        print("Passed! listExperiments before and after kruize pod restart are same!")
    else:
        failed = 1
        print("Failed! listExperiments before and after kruize pod restart are not same!")

    # Compare the listRecommendations before and after kruize pod restart
    result = compare_json_files(list_reco_json_file_before, list_reco_json_file_after)
    if result == True:
        print("Passed! listRecommendations before and after kruize pod restart are same!")
    else:
        failed = 1
        print("Failed! listRecommendations before and after kruize pod restart are not same!")

    # Compare the listDatasource before and after kruize pod restart
    result = compare_json_files(list_datasources_json_file_before, list_datasources_json_file_after)
    if result == True:
        print("Passed! listDatasources before and after kruize pod restart are same!")
    else:
        failed = 1
        print("Failed! listDatasources before and after kruize pod restart are not same!")

    # Compare the list Datasource Metadata before and after kruize pod restart
    result = compare_json_files(list_metadata_json_file_before, list_metadata_json_file_after)
    if result == True:
        print("Passed! list datasource metadata before and after kruize pod restart are same!")
    else:
        failed = 1
        print("Failed! list datasource metadata before and after kruize pod restart are not same!")

    # sleep for a while to mimic the availability of next set of results
    time.sleep(1)

    # Delete the experiment
    delete_experiment(create_exp_json_file, rm=False)

    if failed == 1:
        print("Test Failed! Check the logs for test failures")
        sys.exit(1)
    else:
        print("Test Passed! Check the logs for details")
        sys.exit(0)


if __name__ == '__main__':
    main(sys.argv[1:])
