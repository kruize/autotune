
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
ERROR_STATUS_CODE = 400

SUCCESS_STATUS = "SUCCESS"
ERROR_STATUS = "ERROR"
UPDATE_RESULTS_SUCCESS_MSG = "Results added successfully! View saved results at /listExperiments."
CREATE_EXP_SUCCESS_MSG = "Experiment registered successfully with Kruize. View registered experiments at /listExperiments"

# experiment_name,deployment_name,namespace,performanceProfile,slo_class,direction,mode,targetCluster,image,container_name,measurement_duration,threshold,matchLabel,matchLabelValue
create_exp_test_data = {
        "experiment_name": "\"quarkus-resteasy-kruize-min-http-response-time-db\"",
        "deployment_name": "\"tfb-qrh-sample\"",
        "namespace": "\"default\"",
        "performanceProfile": "\"resource-optimization-openshift\"",
        "slo_class": "\"resource_usage\"",
        "direction": "\"minimize\"",
        "mode": "\"monitor\"",
        "targetCluster": "\"remote\"",
        "image": "\"kruize/tfb-qrh:1.13.2.F_et17\"",
        "container_name": "\"tfb-server\"",
        "measurement_duration": "\"15min\"",
        "threshold": "\"0.1\"",
        "matchLabel": "\"app.kubernetes.io/name\"",
        "matchLabelvalue": "\"tfb-qrh-deployment\""
}

# experiment_name,start_timestamp,end_timestamp,deployment_name,namespace,image,container_name,cpuRequest_sum,cpuRequest_mean,cpuRequest_units,cpuLimit_sum,cpuLimit_mean,cpuLimit_units,cpuUsage_max,cpuUsage_mean,# cpuUsage_units,cpuThrottle_max,cpuThrottle_mean,cpuThrottle_units,memoryRequest_sum,memoryRequest_mean,memoryRequest_units,memoryLimit_sum,memoryLimit_mean,memoryLimit_units,memoryUsage_sum,memoryUsage_mean,
# memoryUsage_units,memoryRSS_max,memoryRSS_mean,memoryRSS_units
update_results_test_data = {
        "experiment_name": "\"quarkus-resteasy-kruize-min-http-response-time-db\"",
        "start_timestamp": "\"2022-01-23T18:25:43.511Z\"",
        "end_timestamp": "\"2022-01-23T18:40:43.511Z\"",
        "deployment_name": "\"tfb-qrh-sample\"",
        "namespace": "\"default\"",
        "image": "\"kruize/tfb-qrh:1.13.2.F_et17\"",
        "container_name": "\"tfb-server\"",
        "cpuRequest_sum": "4.4",
        "cpuRequest_mean": "1.1",
        "cpuRequest_units": "\"cores\"",
        "cpuLimit_sum": "5.4",
        "cpuLimit_mean": "22.1",
        "cpuLimit_units": "\"cores\"",
        "cpuUsage_max": "2.4",
        "cpuUsage_mean": "1.5",
        "cpuUsage_units": "\"cores\"",
        "cpuThrottle_max": "0.09",
        "cpuThrottle_mean": "0.045",
        "cpuThrottle_units": "\"cores\"",
        "memoryRequest_sum": "250.85",
        "memoryRequest_mean": "51.1",
        "memoryRequest_units": "\"MiB\"",
        "memoryLimit_sum": "500",
        "memoryLimit_mean": "100",
        "memoryLimit_units": "\"MiB\"",
        "memoryUsage_max": "198.4",
        "memoryUsage_mean": "41.5",
        "memoryUsage_units": "\"MiB\"",
        "memoryRSS_max": "125.54",
        "memoryRSS_mean": "46.5",
        "memoryRSS_units": "\"MiB\""
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
    return time_str


