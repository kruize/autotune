
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
from datetime import datetime

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

test_type = {"blank": "\"\"", "null": "NULL", "invalid": "\"xyz\""}

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


def read_data_from_json(filename):
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

def generate_json(find_arr, json_file, filename, i):

    with open(json_file, 'r') as file:
        data = file.read()

    for find in find_arr:
        replace = find + "_" + str(i)
        data = data.replace(find, replace)

    with open(filename, 'w') as file:
        file.write(data)

def get_datetime():
    my_datetime = datetime.today()
    time_str = my_datetime.isoformat(timespec = 'milliseconds')
    time_str = time_str + "Z"
    return time_str


