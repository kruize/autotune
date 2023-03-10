
"""
Copyright (c) 2022, 2022 Red Hat, IBM Corporation and others.

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
import csv
import json
import os
import re
from datetime import datetime, timedelta

SUCCESS_STATUS_CODE = 201
SUCCESS_200_STATUS_CODE = 200
ERROR_STATUS_CODE = 400

SUCCESS_STATUS = "SUCCESS"
ERROR_STATUS = "ERROR"
UPDATE_RESULTS_SUCCESS_MSG = "Results added successfully! View saved results at /listExperiments."
CREATE_EXP_SUCCESS_MSG = "Experiment registered successfully with Kruize. View registered experiments at /listExperiments"

# version,experiment_name,cluster_name,performance_profile,mode,target_cluster,type,name,namespace,container_image_name,container_name,measurement_duration,threshold
create_exp_test_data = {
        "version": "\"1.0\"",
        "experiment_name": "\"quarkus-resteasy-kruize-min-http-response-time-db\"",
        "cluster_name": "\"cluster-one-division-bell\"",
        "performance_profile": "\"resource-optimization-openshift\"",
        "mode": "\"monitor\"",
        "target_cluster": "\"remote\"",
        "type": "\"deployment\"",
        "name": "\"tfb-qrh-sample\"",
        "namespace": "\"default\"",
        "container_image_name": "\"kruize/tfb-qrh:1.13.2.F_et17\"",
        "container_name": "\"tfb-server\"",
        "measurement_duration": "\"15min\"",
        "threshold": "\"0.1\""
}

# version, experiment_name,start_timestamp,end_timestamp,type,name,namespace,container_image_name,container_name,cpuRequest_name,cpuRequest_sum,cpuRequest_avg,cpuRequest_format,cpuLimit_name,cpuLimit_sum,cpuLimit_avg,cpuLimit_format,cpuUsage_name,cpuUsage_sum,cpuUsage_max,cpuUsage_avg,cpuUsage_min,cpuUsage_format,cpuThrottle_name,cpuThrottle_sum,cpuThrottle_max,cpuThrottle_avg,cpuThrottle_format,memoryRequest_name,memoryRequest_sum,memoryRequest_avg,memoryRequest_format,memoryLimit_name,memoryLimit_sum,memoryLimit_avg,memoryLimit_format,memoryUsage_name,memoryUsage_sum,memoryUsage_max,memoryUsage_avg,memUsage_min,memoryUsage_format,memoryRSS_name,memoryRSS_sum,memoryRSS_max,memoryRSS_avg,memoryRSS_min,memoryRSS_format
update_results_test_data = {
        "version": "\"1.0\"",
        "experiment_name": "\"quarkus-resteasy-kruize-min-http-response-time-db\"",
        "start_timestamp": "\"2022-01-23T18:25:43.511Z\"",
        "end_timestamp": "\"2022-01-23T18:40:43.511Z\"",
        "type": "\"deployment\"",
        "name": "\"tfb-qrh-sample\"",
        "namespace": "\"default\"",
        "container_image_name": "\"kruize/tfb-qrh:1.13.2.F_et17\"",
        "container_name": "\"tfb-server\"",
        "cpuRequest_name": "\"cpuRequest\"",
        "cpuRequest_sum": "4.4",
        "cpuRequest_avg": "1.1",
        "cpuRequest_format": "\"cores\"",
        "cpuLimit_name": "\"cpuLimit\"",
        "cpuLimit_sum": "5.4",
        "cpuLimit_avg": "22.1",
        "cpuLimit_format": "\"cores\"",
        "cpuUsage_name": "\"cpuUsage\"",
        "cpuUsage_sum": "3.4",
        "cpuUsage_max": "2.4",
        "cpuUsage_avg": "1.5",
        "cpuUsage_min": "0.5",
        "cpuUsage_format": "\"cores\"",
        "cpuThrottle_name": "\"cpuThrottle\"",
        "cpuThrottle_sum": "1.09",
        "cpuThrottle_max": "0.09",
        "cpuThrottle_avg": "0.045",
        "cpuThrottle_format": "\"cores\"",
        "memoryRequest_name": "\"memoryRequest\"",
        "memoryRequest_sum": "250.85",
        "memoryRequest_avg": "51.1",
        "memoryRequest_format": "\"MiB\"",
        "memoryLimit_name": "\"memoryLimit\"",
        "memoryLimit_sum": "500",
        "memoryLimit_avg": "100",
        "memoryLimit_format": "\"MiB\"",
        "memoryUsage_name": "\"memoryUsage\"",
        "memoryUsage_sum": "298.5",
        "memoryUsage_max": "198.4",
        "memoryUsage_avg": "41.5",
        "memoryUsage_min": "21.5",
        "memoryUsage_format": "\"MiB\"",
        "memoryRSS_name": "\"memoryRSS\"",
        "memoryRSS_sum": "225.64",
        "memoryRSS_max": "125.54",
        "memoryRSS_avg": "46.5",
        "memoryRSS_min": "26.5",
        "memoryRSS_format": "\"MiB\""
    }

test_type = {"blank": "\"\"", "null": "null", "invalid": "\"xyz\""}

def generate_test_data(csvfile, test_data):
    if os.path.isfile(csvfile):
        os.remove(csvfile)
    f = open(csvfile, "a")

    for key in test_data:
        for t in test_type:
            data_str = ""

            test_name = t + "_" + key
            status_code = "400"

            data_str = "\"" + test_name + "\"," + status_code
            for k in test_data:
                data_str += "," 
                if  k != key :
                        data_str += test_data[k]
                else:
                        if any(re.findall(r'mean|sum|max|min|avg|number', k, re.IGNORECASE)):
                            data_str += "-1"
                        else :
                           data_str += test_type[t]

            data_str += "\n"
            f.write(data_str)

    f.close()
    test_data = read_test_data_from_csv(csvfile)
    return test_data

def write_json_data_to_file(filename, data):
    """
    Helper to read Json file
    """
    try:
        with open(filename, "w") as f:
            json.dump(data, f, indent=4)
        return data
    except:
        return None

def read_json_data_from_file(filename):
    """
    Helper to read Json file
    """
    try:
        with open(filename, "r") as f:
            data = json.load(f)
        return data
    except:
        return None

def read_test_data_from_csv(csv_file):
    test_data = []
    
    with open(csv_file, newline='') as csvfile:
        data = csv.reader(csvfile, delimiter=',')
        #next(data)  # skip header row
        for row in data:
            test_data.append(row)

    return test_data

def generate_json(find_arr, json_file, filename, i, update_timestamps = False):
    with open(json_file, 'r') as file:
        data = file.read()

    for find in find_arr:
        replace = find + "_" + str(i)
        data = data.replace(find, replace)

    if update_timestamps == True:
        find = "2022-01-23T18:25:43.511Z"
        replace = increment_timestamp(find, i) 
        data = data.replace(find, replace)

        find = "2022-01-23T18:55:43.511Z"
        replace = increment_timestamp(find, i) 
        data = data.replace(find, replace)

    with open(filename, 'w') as file:
        file.write(data)

def increment_timestamp(input_timestamp, step):
    input_date = datetime.strptime(input_timestamp, "%Y-%m-%dT%H:%M:%S.%fZ")
    minutes = 50 * step + 3600
    output_date = input_date + timedelta(minutes=minutes)
    timestamp = output_date.strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3]+'Z'

    return timestamp


def get_datetime():
    my_datetime = datetime.today()
    time_str = my_datetime.isoformat(timespec = 'milliseconds')
    time_str = time_str + "Z"
    return time_str

def increment_date_time(input_date_str, term):
    duration = {"short_term": 1, "medium_term": 7, "long_term": 15}
    input_date = datetime.strptime(input_date_str, "%Y-%m-%dT%H:%M:%S.%fZ")

    output_date = input_date - timedelta(days=duration[term])
    output_date_str = output_date.strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3]+'Z'

    return output_date_str

def validate_reco_json(create_exp_json, update_results_json, list_reco_json):

    # Validate experiment
    assert create_exp_json["version"] == list_reco_json["version"]
    assert create_exp_json["experiment_name"] == list_reco_json["experiment_name"]
    assert create_exp_json["cluster_name"] == list_reco_json["cluster_name"]

    # Validate kubernetes objects
    length = len(list_reco_json["kubernetes_objects"])
    for i in range(length):
        validate_kubernetes_obj(create_exp_json["kubernetes_objects"][i], update_results_json, list_reco_json["kubernetes_objects"][i])


def validate_kubernetes_obj(create_exp_kubernetes_obj, update_results_json, list_reco_kubernetes_obj):

    # Validate type, name, namespace
    assert list_reco_kubernetes_obj["type"] == create_exp_kubernetes_obj["type"]
    assert list_reco_kubernetes_obj["name"] == create_exp_kubernetes_obj["name"]
    assert list_reco_kubernetes_obj["namespace"] == create_exp_kubernetes_obj["namespace"]

    # Validate the count of containers
    assert len(list_reco_kubernetes_obj["containers"]) == len(create_exp_kubernetes_obj["containers"])

    container_names = []
    length = len(create_exp_kubernetes_obj["containers"])
    for i in range(length):
        container_names.append(create_exp_kubernetes_obj["containers"][i]["container_name"])

    container_names.sort()
    print(container_names)

    # Validate if all the containers are present
    for i in range(length):
        list_reco_container = None
        update_results_container = None
        for j in range(length):
            if list_reco_kubernetes_obj["containers"][j]["container_name"] == create_exp_kubernetes_obj["containers"][i]["container_name"]:
                create_exp_container = create_exp_kubernetes_obj["containers"][i]
                list_reco_container = list_reco_kubernetes_obj["containers"][j]
                validate_container(create_exp_container, update_results_json, list_reco_container)

def validate_container(create_exp_container, update_results_json, list_reco_container):
    # Validate container image name and container name
    if create_exp_container != None and list_reco_container != None:
        assert list_reco_container["container_image_name"] == create_exp_container["container_image_name"]
        assert list_reco_container["container_name"] == create_exp_container["container_name"]

    # Validate timestamps
    end_timestamp = update_results_json["end_timestamp"]
    start_timestamp = update_results_json["start_timestamp"]
    duration_based_obj = list_reco_container["recommendations"][end_timestamp]["duration_based"]

    duration_terms = ["short_term", "medium_term", "long_term"]
    for term in duration_terms:
        if check_if_recommendations_are_present(duration_based_obj[term]):
            # Validate timestamps
            assert duration_based_obj[term]["monitoring_end_time"] == end_timestamp

            monitoring_start_time = increment_date_time(end_timestamp, term)
            print(f"actual = {duration_based_obj[term]['monitoring_start_time']} expected = {monitoring_start_time}")
            assert duration_based_obj[term]["monitoring_start_time"] == monitoring_start_time

            # Validate duration in hrs
            expected_duration_in_hours = time_diff_in_hours(start_timestamp, end_timestamp)
            assert duration_based_obj[term]["duration_in_hours"] == expected_duration_in_hours
            
            # Validate recommendation config
            validate_config(duration_based_obj[term]["config"])

def validate_config(reco_config):
    usage_list = ["requests", "limits"]
    for usage in usage_list:
        assert reco_config[usage]["cpu"]["amount"] > 0
        assert reco_config[usage]["cpu"]["format"] == "cores"
        assert reco_config[usage]["memory"]["amount"] > 0
        assert reco_config[usage]["memory"]["format"] == "MiB"

def check_if_recommendations_are_present(duration_based_obj):
    no_reco_msg = "There is not enough data available to generate a recommendation."

    for notification in duration_based_obj["notifications"]:
        if notification["message"] == no_reco_msg:
            return False
    return True

def time_diff_in_hours(start_timestamp, end_timestamp):
    start_date = datetime.strptime(start_timestamp, "%Y-%m-%dT%H:%M:%S.%fZ")
    end_date = datetime.strptime(end_timestamp, "%Y-%m-%dT%H:%M:%S.%fZ")
    diff = end_date - start_date
    return diff.total_seconds() / 3600

