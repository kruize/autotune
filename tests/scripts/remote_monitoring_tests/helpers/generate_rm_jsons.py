import csv
import json
import sys
import os
import datetime
import random
from datetime import datetime, timedelta, timezone
from helpers.utils import *

total_exps = 10

exp_name = "quarkus-exp"
kubernetes_object_name = "tfb-qrh-deployment"
kubernetes_object_namespace = "default"
container_image_name = "kruize/tfb-qrh:1.13.2.F_et17"
container_name = "tfb-server"

db_container_image_name = "kruize/kruize/tfb-db:1.15"
db_container_name = "tfb-db"

cluster_name = "cluster-one-division-bell"
mode = "monitor"
target_cluster = "remote"
performance_profile = "resource-optimization-openshift"

kubernetes_object_type = ["deployment", "deploymentConfig", "statefulset", "daemonset", "replicaset", "replicationController"]
num_obj_types = len(kubernetes_object_type)

def convert_date_format(input_date_str):
    input_date = datetime.datetime.strptime(input_date_str, "%a %b %d %H:%M:%S UTC %Y")
    output_date_str = input_date.strftime("%Y-%m-%dT%H:%M:%S.000Z")
    return output_date_str

def create_exp_jsons(split = False, split_count = 1, exp_json_dir = "/tmp/exp_jsons", total_exps = 10):
    complete_json_data = []
    single_json_data = []
    multi_json_data = []

    isExist = os.path.exists(exp_json_dir)
    if not isExist:
        os.mkdir(exp_json_dir)

    j = 0
    type_index = 0
    for i in range(total_exps):

        exp_num = i

        # Create a dictionary for trial settings
        trial_settings = {
            "measurement_duration": "15min"
        }

        # Create a dictionary for recommendation settings
        recommendation_settings = {
            "threshold": "0.1"
        }

        # Create a list to hold the containers
        containers = []

        # Create a dictionary to hold the container information
        container1 = {
            "container_image_name": container_image_name + "_" + str(exp_num),
            "container_name": container_name + "_" + str(exp_num),
        }

        containers.append(container1)

        container2 = {
            "container_image_name": db_container_image_name + "_" + str(exp_num),
            "container_name": db_container_name + "_" + str(exp_num),
        }

        containers.append(container2)
            
        # Create a dictionary to hold the deployment information
        kubernetes_objects = [{
            "type": kubernetes_object_type[type_index],
            "name": kubernetes_object_name + "_" + str(exp_num),
            "namespace": kubernetes_object_namespace + "_" + str(exp_num),
            "containers": containers
        }]

        # Create a dictionary to hold the experiment data
        experiment = {
            "version": "1.0",
            "experiment_name": exp_name + "_" + str(exp_num),
            "cluster_name": cluster_name + "_" + str(exp_num),
            "performance_profile": performance_profile,
            "mode": mode,
            "target_cluster": target_cluster,
            "kubernetes_objects": kubernetes_objects,
            "trial_settings": trial_settings,
            "recommendation_settings": recommendation_settings
        }

        complete_json_data.append(experiment)
        if split == True:
            if i % split_count != 0:
                multi_json_data.append(experiment)
            else:
                multi_json_data.append(experiment)
                exp_json_file = exp_json_dir + "/create_exp_" + str(j) + ".json"
                with open(exp_json_file, "w") as json_file:
                    json.dump(multi_json_data, json_file, indent=4)

                multi_json_data = []
                j += 1
        else:
            single_json_data.append(experiment)
            # Write the final JSON data to the output file
            exp_json_file = exp_json_dir + "/create_exp_" + str(j) + ".json"
            j += 1
            with open(exp_json_file, "w") as json_file:
                json.dump(single_json_data, json_file, indent=4)

            single_json_data = []

        if type_index < num_obj_types-1:
            type_index += 1
        else:
            type_index = 0


    # Write the final JSON data to the output file
    with open("/tmp/exp_complete.json", "w") as json_file:
        json.dump(complete_json_data, json_file, indent=4)

def create_update_results_jsons(csv_file_path, split = False, split_count = 1, json_dir = "/tmp/result_jsons", total_exps = 10, num_res = None, new_timestamp = None):
    # Define the list that will hold the final JSON data
    complete_json_data = []
    single_row_json_data = []
    multi_row_json_data = []

    # Create an empty list to hold the deployments
    mebibyte = 1048576

    isExist = os.path.exists(json_dir)
    if not isExist:
        os.mkdir(json_dir)

    i = 1
    exp_num = 0
    type_index = 0
    j = 0
    data_interval = 15

    if new_timestamp != None:
        interval_start_time = new_timestamp
    else:
        interval_start_time = get_datetime()
    
    # Open the CSV file
    if num_res == None:
        num_res = get_num_lines_in_csv(csv_file_path)
        num_res = num_res - 1
        print(f"Number of results = {num_res}")

    row_counter = 0
    with open(csv_file_path, 'r') as csvfile:
        # Create a CSV reader object
        csvreader = csv.DictReader(csvfile)

        for row in csvreader:
            type_index = 0
            container_metrics = []

            if row["cpu_request_avg_container"]:
                container_metrics.append({
                    "name": "cpuRequest",
                    "results": {
                        "aggregation_info": {
                            "sum": float(row["cpu_request_sum_container"]),
                            "avg": float(row["cpu_request_avg_container"]),
                            "format": "cores"
                        }
                    }
                })

            if row["cpu_limit_avg_container"]:
                container_metrics.append({
                    "name" : "cpuLimit",
                    "results": {
                        "aggregation_info": {
                            "sum": float(row["cpu_limit_sum_container"]),
                            "avg": float(row["cpu_limit_avg_container"]),
                            "format": "cores"
                        }
                    }
                })

            if row["cpu_throttle_max_container"]:
                container_metrics.append({
                    "name" : "cpuThrottle",
                    "results": {
                        "aggregation_info": {
                            "sum": float(row["cpu_throttle_sum_container"]),
                            "max": float(row["cpu_throttle_max_container"]),
                            "avg": float(row["cpu_throttle_avg_container"]),
                            "format": "cores"
                        }
                    }
                })

            container_metrics.append({
                "name" : "cpuUsage",
                "results": {
                    "aggregation_info": {
                        "sum": float(row["cpu_usage_sum_container"]),
                        "min": float(row["cpu_usage_min_container"]),
                        "max": float(row["cpu_usage_max_container"]),
                        "avg": float(row["cpu_usage_avg_container"]),
                        "format": "cores"
                    }
                }
            })

            if row["mem_request_avg_container"]:
                container_metrics.append({
                    "name" : "memoryRequest",
                    "results": {
                        "aggregation_info": {
                            "sum": float(row["mem_request_sum_container"])/mebibyte,
                            "avg": float(row["mem_request_avg_container"])/mebibyte,
                            "format": "MiB"
                        }
                    }
                })

            if row["mem_limit_avg_container"]:
                container_metrics.append({
                    "name" : "memoryLimit",
                    "results": {
                        "aggregation_info": {
                            "sum": float(row["mem_limit_sum_container"])/mebibyte,
                            "avg": float(row["mem_limit_avg_container"])/mebibyte,
                            "format": "MiB"
                        }
                    }
                })

            container_metrics.append({
                "name" : "memoryUsage",
                "results": {
                    "aggregation_info": {
                            "min": float(row["mem_usage_min_container"])/mebibyte,
                            "max": float(row["mem_usage_max_container"])/mebibyte,
                            "sum": float(row["mem_usage_sum_container"])/mebibyte,
                            "avg": float(row["mem_usage_avg_container"])/mebibyte,
                            "format": "MiB"
                        }
                    }
                })

            container_metrics.append({
                "name" : "memoryRSS",
                "results": {
                    "aggregation_info": {
                        "min": float(row["mem_rss_min_container"])/mebibyte,
                        "max": float(row["mem_rss_max_container"])/mebibyte,
                        "sum": float(row["mem_rss_sum_container"])/mebibyte,
                        "avg": float(row["mem_rss_avg_container"])/mebibyte,
                        "format": "MiB"
                    }
                }
            })

            for exp_num in range(total_exps):
                # Create a list to hold the containers
                containers = []

                # Create a dictionary to hold the container information
                container1 = {
                    "container_image_name": container_image_name + "_" + str(exp_num),
                    "container_name": container_name + "_" + str(exp_num),
                    "metrics": container_metrics
                }

                containers.append(container1)

                container2 = {
                    "container_image_name": db_container_image_name + "_" + str(exp_num),
                    "container_name": db_container_name + "_" + str(exp_num),
                    "metrics": container_metrics
                }
            
                containers.append(container2)

                # Create a dictionary to hold the deployment information
                kubernetes_objects = [{
                    "type": kubernetes_object_type[type_index],
                    "name": kubernetes_object_name + "_" + str(exp_num),
                    "namespace": kubernetes_object_namespace + "_" + str(exp_num),
                    "containers": containers
                }]
          
                interval_end_time = increment_timestamp_by_given_mins(interval_start_time, data_interval)
         
                # Create a dictionary to hold the experiment data
                update_results = {
                    "version": "1.0",
                    "experiment_name": exp_name + "_" + str(exp_num),
                    "interval_start_time": interval_start_time,
                    "interval_end_time": interval_end_time,
                    "kubernetes_objects": kubernetes_objects
                }
       
                complete_json_data.append(update_results)
                if split == True:
                    if i % split_count != 0:
                        multi_row_json_data.append(update_results)
                        i += 1
                    else:
                        multi_row_json_data.append(update_results)
                        result_json_file = json_dir + "/result_split" + str(j) + ".json"
                        with open(result_json_file, "w") as json_file:
                            json.dump(multi_row_json_data, json_file, indent=4)

                        multi_row_json_data = []
                        j += 1
                        i += 1

                    if i % 12 == 0:
                        interval_start_time = interval_end_time

                else:
                    single_row_json_data.append(update_results)
                    # Write the final JSON data to the output file
                    result_json_file = json_dir + "/result_" + str(exp_num) + "_" + str(j) + ".json"
                    with open(result_json_file, "w") as json_file:
                        json.dump(single_row_json_data, json_file, indent=4)

                    single_row_json_data = []

                if type_index < num_obj_types-1:
                    type_index += 1
                else:
                    type_index = 0

            j += 1
            interval_start_time = interval_end_time
            if row_counter < num_res-1:
                row_counter += 1
            else:
                break

    # Write the final JSON data to the output file
    with open("/tmp/complete_results.json", "w") as json_file:
        json.dump(complete_json_data, json_file, indent=4)
