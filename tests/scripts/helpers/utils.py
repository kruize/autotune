"""
Copyright (c) 2022, 2024 Red Hat, IBM Corporation and others.

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
import subprocess
import time
import math
import docker
from helpers.kruize import *
from datetime import datetime, timedelta
from kubernetes import client, config
from pathlib import Path
from helpers.kruize import get_bulk_job_status

SUCCESS_STATUS_CODE = 201
SUCCESS_200_STATUS_CODE = 200
ERROR_STATUS_CODE = 400
ERROR_409_STATUS_CODE = 409
DUPLICATE_RECORDS_COUNT = 5
ERROR_500_STATUS_CODE = 500

SUCCESS_STATUS = "SUCCESS"
ERROR_STATUS = "ERROR"
UPDATE_RESULTS_SUCCESS_MSG = "Results added successfully! View saved results at /listExperiments."
UPDATE_RESULTS_DATE_PRECEDE_ERROR_MSG = "The Start time should precede the End time!"
UPDATE_RESULTS_INVALID_METRIC_VALUE_ERROR_MSG = "Performance profile: [avg cannot be negative or blank for the metric variable: "
UPDATE_RESULTS_INVALID_METRIC_FORMAT_ERROR_MSG = "Performance profile: [ Format value should be among these values: [GiB, Gi, Ei, KiB, E, MiB, G, PiB, K, TiB, M, P, Bytes, cores, T, Ti, MB, KB, Pi, GB, EB, k, m, TB, PB, bytes, kB, Mi, Ki, EiB]"
UPDATE_RESULTS_FAILED_RECORDS_MSG = f"Out of a total of 100 records, {DUPLICATE_RECORDS_COUNT} failed to save"
FAILED_RECORDS_MSG = "Out of a total of 1 records, 1 failed to save"
THREE_FAILED_RECORDS_MSG = "Out of a total of 3 records, 3 failed to save"
DUPLICATE_RECORDS_MSG = "An entry for this record already exists!"
CREATE_EXP_SUCCESS_MSG = "Experiment registered successfully with Kruize. View registered experiments at /listExperiments"
CREATE_EXP_BULK_ERROR_MSG = "At present, the system does not support bulk entries!"
CREATE_EXP_CONTAINER_EXP_CONTAINS_NAMESPACE = "Can not specify namespace data for container experiment"
CREATE_EXP_NAMESPACE_EXP_CONTAINS_CONTAINER = "Can not specify container data for namespace experiment"
CREATE_EXP_NAMESPACE_EXP_NOT_SUPPORTED_FOR_VPA_MODE = "Auto or recreate mode is not supported for namespace experiment."
CREATE_EXP_VPA_NOT_SUPPORTED_FOR_REMOTE = "Auto or recreate mode is not supported for remote monitoring use case."
CREATE_EXP_INVALID_KUBERNETES_OBJECT_FOR_VPA = "Kubernetes object type is not supported for auto or recreate mode."
UPDATE_RECOMMENDATIONS_MANDATORY_DEFAULT_MESSAGE = 'experiment_name is mandatory'
UPDATE_RECOMMENDATIONS_MANDATORY_INTERVAL_END_DATE = 'interval_end_time is mandatory'
UPDATE_RECOMMENDATIONS_EXPERIMENT_NOT_FOUND = 'Not Found: experiment_name does not exist: '
UPDATE_RECOMMENDATIONS_METRICS_NOT_FOUND = 'No metrics available from '
UPDATE_RECOMMENDATIONS_START_TIME_PRECEDE_END_TIME = 'The Start time should precede the End time!'
UPDATE_RECOMMENDATIONS_START_TIME_END_TIME_GAP_ERROR = 'The gap between the interval_start_time and interval_end_time must be within a maximum of 15 days!'
UPDATE_RECOMMENDATIONS_INVALID_DATE_TIME_FORMAT = "Given timestamp - \" %s \" is not a valid timestamp format"
RECOMMENDATIONS_AVAILABLE = "Recommendations Are Available"
COST_RECOMMENDATIONS_AVAILABLE = "Cost Recommendations Available"
PERFORMANCE_RECOMMENDATIONS_AVAILABLE = "Performance Recommendations Available"
CONTAINER_AND_EXPERIMENT_NAME = " for container : %s for experiment: %s.]"
LIST_DATASOURCES_ERROR_MSG = "Given datasource name - %s either does not exist or is not valid"
LIST_METADATA_DATASOURCE_NAME_ERROR_MSG = "Metadata for a given datasource name - %s either does not exist or is not valid"
LIST_METADATA_ERROR_MSG = ("Metadata for a given datasource - %s, cluster name - %s, namespace - %s "
                           "either does not exist or is not valid")
LIST_METADATA_DATASOURCE_NAME_CLUSTER_NAME_ERROR_MSG = "Metadata for a given datasource name - %s, cluster_name - %s either does not exist or is not valid"
LIST_METADATA_MISSING_DATASOURCE = "datasource is mandatory"
IMPORT_METADATA_DATASOURCE_CONNECTION_FAILURE_MSG = "Metadata cannot be imported, datasource connection refused or timed out"
CREATE_METRIC_PROFILE_SUCCESS_MSG = "Metric Profile : %s created successfully. View Metric Profiles at /listMetricProfiles"
METRIC_PROFILE_EXISTS_MSG = "Validation failed: Metric Profile already exists: %s"
METRIC_PROFILE_NOT_FOUND_MSG = "No metric profiles found!"
INVALID_LIST_METRIC_PROFILE_INPUT_QUERY = "The query param(s) - [%s] is/are invalid"
LIST_METRIC_PROFILES_INVALID_NAME = "Given metric profile name - %s either does not exist or is not valid"
CREATE_METRIC_PROFILE_MISSING_MANDATORY_FIELD_MSG = "Validation failed: JSONObject[\"%s\"] not found."
CREATE_METRIC_PROFILE_MISSING_MANDATORY_PARAMETERS_MSG = "Validation failed: Missing mandatory parameters: [%s] "
CREATE_METADATA_PROFILE_SUCCESS_MSG = "Metadata Profile : %s created successfully. View Metadata Profiles at /listMetadataProfiles"
METADATA_PROFILE_EXISTS_MSG = "Validation failed: Metadata Profile already exists: %s"
METADATA_PROFILE_NOT_FOUND_MSG = "No metadata profiles found!"
INVALID_LIST_METADATA_PROFILE_INPUT_QUERY = "The query param(s) - [%s] is/are invalid"
LIST_METADATA_PROFILES_INVALID_NAME = "Given metadata profile name - %s either does not exist or is not valid"
CREATE_METADATA_PROFILE_MISSING_MANDATORY_FIELD_MSG = "Validation failed: JSONObject[\"%s\"] not found."
CREATE_METADATA_PROFILE_MISSING_MANDATORY_PARAMETERS_MSG = "Validation failed: Missing mandatory parameters: [%s] "


EMPTY_MODELS_TERMS = "Empty term or model value."
INVALID_TERMS = "Term name is not supported. Use short, medium or long term."
INVALID_MODELS = "Model name is not supported. Use cost or performance."
MULTIPLE_TERMS = "Multiple terms are currently not supported for auto or recreate mode."
MULTIPLE_MODELS = "Multiple models are currently not supported for auto or recreate mode."

# Kruize Recommendations Notification codes
NOTIFICATION_CODE_FOR_RECOMMENDATIONS_AVAILABLE = "111000"
NOTIFICATION_CODE_FOR_COST_RECOMMENDATIONS_AVAILABLE = "112101"
NOTIFICATION_CODE_FOR_PERFORMANCE_RECOMMENDATIONS_AVAILABLE = "112102"
NOTIFICATION_CODE_FOR_SHORT_TERM_RECOMMENDATIONS_AVAILABLE = "111101"
NOTIFICATION_CODE_FOR_MEDIUM_TERM_RECOMMENDATIONS_AVAILABLE = "111102"
NOTIFICATION_CODE_FOR_LONG_TERM_RECOMMENDATIONS_AVAILABLE = "111103"
NOTIFICATION_CODE_FOR_NOT_ENOUGH_DATA = "120001"
NOTIFICATION_CODE_FOR_CPU_RECORDS_ARE_IDLE = "323001"
NOTIFICATION_CODE_FOR_CPU_RECORDS_ARE_IDLE_MESSAGE = "CPU Usage is less than a millicore, No CPU Recommendations can be generated"
NOTIFICATION_CODE_FOR_CPU_RECORDS_ARE_ZERO = "323002"
NOTIFICATION_CODE_FOR_CPU_RECORDS_ARE_ZERO_MESSAGE = "CPU usage is zero, No CPU Recommendations can be generated"
NOTIFICATION_CODE_FOR_CPU_RECORDS_NOT_AVAILABLE = "323003"
NOTIFICATION_CODE_FOR_MEMORY_RECORDS_ARE_ZERO = "324001"
NOTIFICATION_CODE_FOR_MEMORY_RECORDS_ARE_ZERO_MESSAGE = "Memory Usage is zero, No Memory Recommendations can be generated"
NOTIFICATION_CODE_FOR_MEMORY_RECORDS_NOT_AVAILABLE = "324002"
NOTIFICATION_CODE_FOR_CPU_REQUEST_NOT_SET = "523001"
NOTIFICATION_CODE_FOR_CPU_LIMIT_NOT_SET = "423001"
NOTIFICATION_CODE_FOR_MEMORY_REQUEST_NOT_SET = "524001"
NOTIFICATION_CODE_FOR_MEMORY_LIMIT_NOT_SET = "524002"

AMOUNT_MISSING_IN_CPU_SECTION_CODE = "223001"
INVALID_AMOUNT_IN_CPU_SECTION_CODE = "223002"
FORMAT_MISSING_IN_CPU_SECTION_CODE = "223003"
INVALID_FORMAT_IN_CPU_SECTION_CODE = "223004"

AMOUNT_MISSING_IN_MEMORY_SECTION_CODE = "224001"
INVALID_AMOUNT_IN_MEMORY_SECTION_CODE = "224002"
FORMAT_MISSING_IN_MEMORY_SECTION_CODE = "224003"
INVALID_FORMAT_IN_MEMORY_SECTION_CODE = "224004"

WARNING_CPU_LIMIT_NOT_SET_CODE = "423001"
CRITICAL_CPU_REQUEST_NOT_SET_CODE = "523001"
CRITICAL_MEMORY_REQUEST_NOT_SET_CODE = "524001"
CRITICAL_MEMORY_LIMIT_NOT_SET_CODE = "524002"

INFO_COST_RECOMMENDATIONS_AVAILABLE_CODE = "112101"
INFO_PERFORMANCE_RECOMMENDATIONS_AVAILABLE_CODE = "112102"
INFO_RECOMMENDATIONS_AVAILABLE_CODE = "111000"
INFO_SHORT_TERM_RECOMMENDATIONS_AVAILABLE_CODE = "111101"

CPU_REQUEST_OPTIMISED_CODE = "323004"
CPU_LIMIT_OPTIMISED_CODE = "323005"
MEMORY_REQUEST_OPTIMISED_CODE = "324003"
MEMORY_LIMIT_OPTIMISED_CODE = "324004"

CPU_REQUEST = "cpuRequest"
CPU_LIMIT = "cpuLimit"
CPU_USAGE = "cpuUsage"
CPU_THROTTLE = "cpuThrottle"

MEMORY_REQUEST = "memoryRequest"
MEMORY_LIMIT = "memoryLimit"
MEMORY_USAGE = "memoryUsage"
MEMORY_RSS = "memoryRSS"

OPTIMISED_CPU = 3
OPTIMISED_MEMORY = 300

NOT_ENOUGH_DATA_MSG = "There is not enough data available to generate a recommendation."
EXP_EXISTS_MSG = "Experiment name already exists: "
INVALID_DEPLOYMENT_TYPE_MSG = "Invalid deployment type: xyz"
INVALID_INTERVAL_DURATION_MSG = "Interval duration cannot be less than or greater than measurement_duration by more than 30 seconds"

time_log_csv = "/tmp/time_log.csv"

# DURATION - No. of days * 24.0 hrs
SHORT_TERM_DURATION_IN_HRS_MIN = 1 * 0.5
SHORT_TERM_DURATION_IN_HRS_MAX = 1 * 24.0
MEDIUM_TERM_DURATION_IN_HRS_MIN = 2 * 24.0
MEDIUM_TERM_DURATION_IN_HRS_MAX = 7 * 24.0
LONG_TERM_DURATION_IN_HRS_MIN = 8 * 24.0
LONG_TERM_DURATION_IN_HRS_MAX = 15 * 24.0

SHORT_TERM = "short_term"
MEDIUM_TERM = "medium_term"
LONG_TERM = "long_term"
NO_TERM = "no_term"
SHORT_AND_MEDIUM = "short_term_and_medium_term"
SHORT_AND_LONG = "short_term_and_long_term"
MEDIUM_AND_LONG = "medium_term_and_long_term"
ONLY_LONG = "only_long_term"
ONLY_MEDIUM = "only_medium_term"
SHORT_TERM_TEST = "short_term_test"
MEDIUM_TERM_TEST = "medium_term_test"
LONG_TERM_TEST = "long_term_test"

PLOTS = "plots"
DATA_POINTS = "datapoints"
PLOTS_DATA = "plots_data"

TERMS_NOTIFICATION_CODES = {
    SHORT_TERM: NOTIFICATION_CODE_FOR_SHORT_TERM_RECOMMENDATIONS_AVAILABLE,
    MEDIUM_TERM: NOTIFICATION_CODE_FOR_MEDIUM_TERM_RECOMMENDATIONS_AVAILABLE,
    LONG_TERM: NOTIFICATION_CODE_FOR_LONG_TERM_RECOMMENDATIONS_AVAILABLE,
}

NAMESPACE_EXPERIMENT_TYPE = "namespace"
CONTAINER_EXPERIMENT_TYPE = "container"

# version,experiment_name,cluster_name,performance_profile,mode,target_cluster,type,name,namespace,container_image_name,container_name,measurement_duration,threshold
create_exp_test_data = {
    "version": "v2.0",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db",
    "cluster_name": "cluster-one-division-bell",
    "performance_profile": "resource-optimization-openshift",
    "mode": "monitor",
    "target_cluster": "remote",
    "experiment_type": "container",
    "type": "deployment",
    "name": "tfb-qrh-sample",
    "namespace": "default",
    "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
    "container_name": "tfb-server",
    "measurement_duration": "15min",
    "threshold": "0.1"
}

# version, experiment_name,interval_start_time,interval_end_time,type,name,namespace,container_image_name,container_name,cpuRequest_name,cpuRequest_sum,cpuRequest_avg,cpuRequest_format,cpuLimit_name,cpuLimit_sum,cpuLimit_avg,cpuLimit_format,cpuUsage_name,cpuUsage_sum,cpuUsage_max,cpuUsage_avg,cpuUsage_min,cpuUsage_format,cpuThrottle_name,cpuThrottle_sum,cpuThrottle_max,cpuThrottle_avg,cpuThrottle_format,memoryRequest_name,memoryRequest_sum,memoryRequest_avg,memoryRequest_format,memoryLimit_name,memoryLimit_sum,memoryLimit_avg,memoryLimit_format,memoryUsage_name,memoryUsage_sum,memoryUsage_max,memoryUsage_avg,memUsage_min,memoryUsage_format,memoryRSS_name,memoryRSS_sum,memoryRSS_max,memoryRSS_avg,memoryRSS_min,memoryRSS_format
update_results_test_data = {
    "version": "v2.0",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db",
    "interval_start_time": "2022-01-23T18:25:43.511Z",
    "interval_end_time": "2022-01-23T18:40:43.511Z",
    "type": "deployment",
    "name": "tfb-qrh-deployment",
    "namespace": "default",
    "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
    "container_name": "tfb-server",
    "cpuRequest_name": "cpuRequest",
    "cpuRequest_sum": 4.4,
    "cpuRequest_avg": 1.1,
    "cpuRequest_format": "cores",
    "cpuLimit_name": "cpuLimit",
    "cpuLimit_sum": 5.4,
    "cpuLimit_avg": 22.1,
    "cpuLimit_format": "cores",
    "cpuUsage_name": "cpuUsage",
    "cpuUsage_sum": 3.4,
    "cpuUsage_max": 2.4,
    "cpuUsage_avg": 1.5,
    "cpuUsage_min": 0.5,
    "cpuUsage_format": "cores",
    "cpuThrottle_name": "cpuThrottle",
    "cpuThrottle_sum": 1.09,
    "cpuThrottle_max": 0.09,
    "cpuThrottle_avg": 0.045,
    "cpuThrottle_format": "cores",
    "memoryRequest_name": "memoryRequest",
    "memoryRequest_sum": 250.85,
    "memoryRequest_avg": 51.1,
    "memoryRequest_format": "MiB",
    "memoryLimit_name": "memoryLimit",
    "memoryLimit_sum": 500,
    "memoryLimit_avg": 100,
    "memoryLimit_format": "MiB",
    "memoryUsage_name": "memoryUsage",
    "memoryUsage_sum": 298.5,
    "memoryUsage_max": 198.4,
    "memoryUsage_avg": 41.5,
    "memoryUsage_min": 21.5,
    "memoryUsage_format": "MiB",
    "memoryRSS_name": "memoryRSS",
    "memoryRSS_sum": 225.64,
    "memoryRSS_max": 125.54,
    "memoryRSS_avg": 46.5,
    "memoryRSS_min": 26.5,
    "memoryRSS_format": "MiB"
}

# version, datasource_name
import_metadata_test_data = {
    "version": "v1.0",
    "datasource_name": "prometheus-1",
    "metadata_profile": "cluster-metadata-local-monitoring",
    "measurement_duration": "15min"
}

test_type = {"blank": "", "null": "null", "invalid": "xyz"}

aggr_info_keys_to_skip = ["cpuRequest_sum", "cpuRequest_avg", "cpuLimit_sum", "cpuLimit_avg", "cpuUsage_sum", "cpuUsage_max",
                          "cpuUsage_avg", "cpuUsage_min", "cpuThrottle_sum", "cpuThrottle_max", "cpuThrottle_avg",
                          "memoryRequest_sum", "memoryRequest_avg", "memoryLimit_sum", "memoryRequest_avg",
                          "memoryLimit_sum", "memoryLimit_avg", "memoryUsage_sum", "memoryUsage_max", "memoryUsage_avg",
                          "memoryUsage_min", "memoryRSS_sum", "memoryRSS_max", "memoryRSS_avg", "memoryRSS_min"]

MIG_PATTERN = r"nvidia\.com/mig-[1-4|7]g\.(5|10|20|40|80)gb"


def generate_test_data(csvfile, test_data, api_name):
    if os.path.isfile(csvfile):
        os.remove(csvfile)
    with open(csvfile, 'a') as f:
        writer = csv.writer(f)

        for key in test_data:
            for t in test_type:
                data = []
                # skip checking the invalid container name and container image name
                if key == "container_image_name" or (key == "container_name" and t == "invalid"):
                    continue
                #  skip checking the aggregation info values
                if key in aggr_info_keys_to_skip and t == "null":
                    continue

                test_name = t + "_" + key
                status_code = 400
                if api_name == "create_exp" and (test_name == "invalid_experiment_name" or test_name == "invalid_cluster_name"
                                                 or test_name == "null_experiment_type"):
                    status_code = 201

                if api_name == "import_metadata" and (test_name == "invalid_version" or test_name == "blank_version" or
                test_name == "invalid_measurement_duration" or test_name == "blank_measurement_duration" or test_name == "null_measurement_duration"):
                    status_code = 201

                data.append(test_name)
                data.append(status_code)
                for k in test_data:
                    if k != key:
                        data.append(test_data[k])
                    else:
                        if any(re.findall(r'invalid.*sum|invalid.*max|invalid.*min|invalid.*avg', test_name,
                                          re.IGNORECASE)):
                            data.append(-1)
                        elif any(re.findall(r'blank.*sum|blank.*max|blank.*min|blank.*avg', test_name, re.IGNORECASE)):
                            data.append("\"\"")
                        else:
                            data.append(test_type[t])

                writer.writerow(data)

    f.close()
    test_data = read_test_data_from_csv(csvfile)
    return test_data


def get_num_lines_in_csv(csv_filename):
    reader = csv.reader(open(csv_filename))
    num_lines = len(list(reader))
    print(num_lines)
    return num_lines


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
        # next(data)  # skip header row
        for row in data:
            test_data.append(row)

    return test_data


def update_metrics_json(find_arr, json_file, filename, i, update_metrics,update_timestamps=False ):
    with open(json_file, 'r') as file:
        data = file.read()

    for find in find_arr:
        replace = find + "_" + str(i)
        data = data.replace(find, replace)

    if update_timestamps == True:
        find = "2022-01-23T18:25:43.511Z"
        replace = increment_timestamp(find, i)
        data = data.replace(find, replace)

        find = "2022-01-23T18:40:43.570Z"
        replace = increment_timestamp(find, i)
        data = data.replace(find, replace)

    data = json.loads(data)

    if update_metrics != None:
        containers = data[0]['kubernetes_objects'][0]['containers']
        for container in containers:
            for metric in container['metrics']:
                for metric_dict in update_metrics:
                    for key, value in metric_dict.items():
                        if key == metric['name']:
                            metric['results']['aggregation_info']=value

    data = json.dumps(data)
    with open(filename, 'w') as file:
        file.write(data)



def generate_json(find_arr, json_file, filename, i, update_timestamps=False):
    with open(json_file, 'r') as file:
        data = file.read()

    for find in find_arr:
        replace = find + "_" + str(i)
        data = data.replace(find, replace)

    if update_timestamps == True:
        find = "2022-01-23T18:25:43.511Z"
        replace = increment_timestamp(find, i)
        data = data.replace(find, replace)

        find = "2022-01-23T18:40:43.570Z"
        replace = increment_timestamp(find, i)
        data = data.replace(find, replace)

    with open(filename, 'w') as file:
        file.write(data)


def increment_timestamp(input_timestamp, step):
    input_date = datetime.strptime(input_timestamp, "%Y-%m-%dT%H:%M:%S.%fZ")
    minutes = 50 * step + 3600
    output_date = input_date + timedelta(minutes=minutes)
    timestamp = output_date.strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + 'Z'

    return timestamp


def increment_timestamp_by_given_mins(input_timestamp, minutes):
    input_date = datetime.strptime(input_timestamp, "%Y-%m-%dT%H:%M:%S.%fZ")
    output_date = input_date + timedelta(minutes=minutes)
    timestamp = output_date.strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + 'Z'

    return timestamp


def get_datetime():
    my_datetime = datetime.today()
    time_str = my_datetime.isoformat(timespec='milliseconds')
    time_str = time_str + "Z"
    return time_str


def term_based_start_time(input_date_str, term):
    duration = {"short_term": 1, "medium_term": 7, "long_term": 15}
    input_date = datetime.strptime(input_date_str, "%Y-%m-%dT%H:%M:%S.%fZ")

    output_date = input_date - timedelta(days=duration[term])
    output_date_str = output_date.strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + 'Z'

    return output_date_str

def validate_reco_json(create_exp_json, update_results_json, list_reco_json, expected_duration_in_hours=None,
                       test_name=None):
    # Validate experiment
    assert create_exp_json["version"] == list_reco_json["version"]
    assert create_exp_json["experiment_name"] == list_reco_json["experiment_name"]
    assert create_exp_json["cluster_name"] == list_reco_json["cluster_name"]
    experiment_type = create_exp_json.get("experiment_type")

    # Validate kubernetes objects
    if update_results_json is not None and len(update_results_json) > 0:
        length = len(update_results_json[0]["kubernetes_objects"])
        for i in range(length):
            update_results_kubernetes_obj = update_results_json[0]["kubernetes_objects"][i]
            create_exp_kubernetes_obj = create_exp_json["kubernetes_objects"][i]
            list_reco_kubernetes_obj = list_reco_json["kubernetes_objects"][i]
            validate_kubernetes_obj(create_exp_kubernetes_obj, update_results_kubernetes_obj, update_results_json,
                                    list_reco_kubernetes_obj, expected_duration_in_hours, test_name, experiment_type)
    else:
        update_results_kubernetes_obj = None
        create_exp_kubernetes_obj = create_exp_json["kubernetes_objects"][0]
        list_reco_kubernetes_obj = list_reco_json["kubernetes_objects"][0]
        validate_kubernetes_obj(create_exp_kubernetes_obj, update_results_kubernetes_obj, update_results_json,
                                list_reco_kubernetes_obj, expected_duration_in_hours, test_name, experiment_type)

def validate_local_monitoring_reco_json(create_exp_json, list_reco_json, expected_duration_in_hours=None, test_name=None):
    # Validate experiment
    assert create_exp_json["version"] == list_reco_json["version"]
    assert create_exp_json["experiment_name"] == list_reco_json["experiment_name"]
    assert create_exp_json["cluster_name"] == list_reco_json["cluster_name"]

    # Validate kubernetes objects
    create_exp_kubernetes_obj = create_exp_json["kubernetes_objects"][0]
    list_reco_kubernetes_obj = list_reco_json["kubernetes_objects"][0]
    experiment_type = create_exp_json.get("experiment_type")
    create_exp_recommendation_settings = create_exp_json["recommendation_settings"]
    validate_local_monitoring_kubernetes_obj(create_exp_recommendation_settings, create_exp_kubernetes_obj, list_reco_kubernetes_obj, expected_duration_in_hours,
                                             test_name, experiment_type)

def validate_list_exp_results_count(expected_results_count, list_exp_json):

    # Get the count of objects in all results arrays
    list_exp_results_count = count_results_objects(list_exp_json)
    print("results_count = ", expected_results_count)
    print("list_exp_results_count = ", list_exp_results_count)

    assert expected_results_count == list_exp_results_count


# Function to count objects in results arrays
def count_results_objects(list_exp_json):
    count = 0
    container_count = 1
    for k8s_object in list_exp_json.get("kubernetes_objects", []):
        container_count = len(k8s_object.get("containers"))
        for container in k8s_object.get("containers", {}).values():
            results = container.get("results", {})
            count += len(results)

    return count/container_count


def validate_kubernetes_obj(create_exp_kubernetes_obj, update_results_kubernetes_obj, update_results_json,
                            list_reco_kubernetes_obj, expected_duration_in_hours, test_name, experiment_type):

    if experiment_type == NAMESPACE_EXPERIMENT_TYPE:
        # validate the containers list, should be empty
        containers = list_reco_kubernetes_obj["containers"]
        assert containers == [] or isinstance(containers, list), f"'containers' object should be empty"

        assert list_reco_kubernetes_obj["namespaces"]["namespace"] == create_exp_kubernetes_obj["namespaces"]["namespace"]
        update_results_namespace = create_exp_kubernetes_obj["namespaces"]
        list_reco_namespace = list_reco_kubernetes_obj["namespaces"]
        validate_namespace(update_results_namespace, update_results_json, list_reco_namespace, expected_duration_in_hours, test_name, experiment_type)
    else:
        # Validate type, name, namespace
        assert list_reco_kubernetes_obj["type"] == create_exp_kubernetes_obj["type"]
        assert list_reco_kubernetes_obj["name"] == create_exp_kubernetes_obj["name"]
        assert list_reco_kubernetes_obj["namespace"] == create_exp_kubernetes_obj["namespace"]

        # Validate type, name, namespace
        if update_results_kubernetes_obj == None:
            assert list_reco_kubernetes_obj["type"] == create_exp_kubernetes_obj["type"]
            assert list_reco_kubernetes_obj["name"] == create_exp_kubernetes_obj["name"]
            assert list_reco_kubernetes_obj["namespace"] == create_exp_kubernetes_obj["namespace"]

        else:
            assert list_reco_kubernetes_obj["type"] == update_results_kubernetes_obj["type"]
            assert list_reco_kubernetes_obj["name"] == update_results_kubernetes_obj["name"]
            assert list_reco_kubernetes_obj["namespace"] == update_results_kubernetes_obj["namespace"]


        exp_containers_length = len(create_exp_kubernetes_obj["containers"])
        list_reco_containers_length = len(list_reco_kubernetes_obj["containers"])

        # Validate the count of containers
        assert list_reco_containers_length == exp_containers_length, \
            f"list reco containers size not same as update results containers size - list_reco = {list_reco_containers_length} \
              create_exp = {exp_containers_length}"

        # Validate if all the containers are present
        for i in range(exp_containers_length):
            list_reco_container = None

            for j in range(list_reco_containers_length):
                if list_reco_kubernetes_obj["containers"][j]["container_name"] == \
                        create_exp_kubernetes_obj["containers"][i]["container_name"]:
                    update_results_container = create_exp_kubernetes_obj["containers"][i]
                    list_reco_container = list_reco_kubernetes_obj["containers"][j]
                    validate_container(update_results_container, update_results_json, list_reco_container,
                                       expected_duration_in_hours, test_name, experiment_type)


def validate_local_monitoring_kubernetes_obj(create_exp_recommendation_settings, create_exp_kubernetes_obj,
                            list_reco_kubernetes_obj, expected_duration_in_hours, test_name, experiment_type):
    if experiment_type == NAMESPACE_EXPERIMENT_TYPE:
        assert list_reco_kubernetes_obj["namespaces"]["namespace"] == create_exp_kubernetes_obj["namespaces"]["namespace"]
        list_reco_namespace = list_reco_kubernetes_obj["namespaces"]
        create_exp_namespace = create_exp_kubernetes_obj["namespaces"]
        validate_local_monitoring_namespace( create_exp_recommendation_settings,create_exp_namespace, list_reco_namespace, expected_duration_in_hours, test_name)
    else:
        # Validate type, name, namespace
        assert list_reco_kubernetes_obj["type"] == create_exp_kubernetes_obj["type"]
        assert list_reco_kubernetes_obj["name"] == create_exp_kubernetes_obj["name"]
        assert list_reco_kubernetes_obj["namespace"] == create_exp_kubernetes_obj["namespace"]

        exp_containers_length = len(create_exp_kubernetes_obj["containers"])
        list_reco_containers_length = len(list_reco_kubernetes_obj["containers"])


        # Validate if all the containers are present
        for i in range(exp_containers_length):
            list_reco_container = None

            for j in range(list_reco_containers_length):
                if list_reco_kubernetes_obj["containers"][j]["container_name"] == \
                        create_exp_kubernetes_obj["containers"][i]["container_name"]:
                    list_reco_container = list_reco_kubernetes_obj["containers"][j]
                    create_exp_container = create_exp_kubernetes_obj["containers"][i]
                    validate_local_monitoring_container(create_exp_container, list_reco_container, expected_duration_in_hours, test_name)

def validate_container(update_results_container, update_results_json, list_reco_container, expected_duration_in_hours,
                       test_name, experiment_type):
    # Validate container image name and container name
    if update_results_container != None and list_reco_container != None:
        assert list_reco_container["container_image_name"] == update_results_container["container_image_name"], \
            f"Container image names did not match! Actual -  {list_reco_container['container_image_name']} Expected - {update_results_container['container_image_name']}"

        assert list_reco_container["container_name"] == update_results_container["container_name"], \
            f"Container names did not match! Acutal = {list_reco_container['container_name']} Expected - {update_results_container['container_name']}"

    # default term value
    term = SHORT_TERM
    # Validate timestamps
    if update_results_json != None:
        if expected_duration_in_hours == None:
            duration_in_hours = 0.0
        else:
            duration_in_hours = expected_duration_in_hours

        for update_results in update_results_json:
            interval_end_time = update_results["interval_end_time"]
            interval_start_time = update_results["interval_start_time"]
            print(f"interval_end_time = {interval_end_time} interval_start_time = {interval_start_time}")

            # Obtain the metrics
            metrics = ""
            containers = update_results['kubernetes_objects'][0]['containers']
            for container in containers:
                if update_results_container["container_image_name"] == container["container_image_name"]:
                    metrics = container["metrics"]

            if check_if_recommendations_are_present(list_reco_container["recommendations"]):
                terms_obj = list_reco_container["recommendations"]["data"][interval_end_time]["recommendation_terms"]
                current_config = list_reco_container["recommendations"]["data"][interval_end_time]["current"]

                duration_terms = {'short_term': 4, 'medium_term': 7, 'long_term': 15}

                if 'term_settings' in create_exp_recommendation_settings:
                    duration_terms = get_duration_terms(create_exp_recommendation_settings)

                for term in duration_terms.keys():
                    if check_if_recommendations_are_present(terms_obj[term]):
                        print(f"reco present for term {term}")
                        # Validate timestamps [deprecated as monitoring end time is moved to higher level]
                        # assert cost_obj[term]["monitoring_end_time"] == interval_end_time, \
                        #    f"monitoring end time {cost_obj[term]['monitoring_end_time']} did not match end timestamp {interval_end_time}"

                        # Validate the precision of the valid duration
                        duration = terms_obj[term]["duration_in_hours"]
                        assert validate_duration_in_hours_decimal_precision(duration), f"The value '{duration}' for " \
                                                                                       f"'{term}' has more than two decimal places"

                        monitoring_start_time = term_based_start_time(interval_end_time, term)
                        assert terms_obj[term]["monitoring_start_time"] == monitoring_start_time, \
                            f"actual = {terms_obj[term]['monitoring_start_time']} expected = {monitoring_start_time}"

                        # Validate duration in hrs
                        if expected_duration_in_hours is None:
                            duration_in_hours = set_duration_based_on_terms(duration_in_hours, term,
                                                                            interval_start_time, interval_end_time)

                        if test_name is not None:

                            if MEDIUM_TERM_TEST in test_name and term == MEDIUM_TERM:
                                assert terms_obj[term]["duration_in_hours"] == duration_in_hours, \
                                    f"Duration in hours did not match! Actual = {terms_obj[term]['duration_in_hours']} expected = {duration_in_hours}"
                            elif SHORT_TERM_TEST in test_name and term == SHORT_TERM:
                                assert terms_obj[term]["duration_in_hours"] == duration_in_hours, \
                                    f"Duration in hours did not match! Actual = {terms_obj[term]['duration_in_hours']} expected = {duration_in_hours}"
                            elif LONG_TERM_TEST in test_name and term == LONG_TERM:
                                assert terms_obj[term]["duration_in_hours"] == duration_in_hours, \
                                    f"Duration in hours did not match! Actual = {terms_obj[term]['duration_in_hours']} expected = {duration_in_hours}"
                        else:
                            print(
                                f"Actual = {terms_obj[term]['duration_in_hours']} expected = {duration_in_hours}")
                            assert terms_obj[term]["duration_in_hours"] == duration_in_hours, \
                                f"Duration in hours did not match! Actual = {terms_obj[term]['duration_in_hours']} expected = {duration_in_hours}"
                            duration_in_hours = set_duration_based_on_terms(duration_in_hours, term, interval_start_time,
                                                                            interval_end_time)

                        # Get engine objects
                        if "model_settings" in create_exp_recommendation_settings:
                            engines_list = create_exp_recommendation_settings["model_settings"]["models"]
                        else :
                            engines_list = ["cost", "performance"]

                        # Extract recommendation engine objects
                        recommendation_engines_object = None
                        if "recommendation_engines" in terms_obj[term]:
                            recommendation_engines_object = terms_obj[term]["recommendation_engines"]
                        if recommendation_engines_object is not None:
                            for engine_entry in engines_list:
                                if engine_entry in terms_obj[term]["recommendation_engines"]:
                                    engine_obj = terms_obj[term]["recommendation_engines"][engine_entry]
                                    validate_config(engine_obj["config"], metrics, experiment_type)
                                    validate_variation(current_config, engine_obj["config"], engine_obj["variation"])
                        # validate Plots data
                        validate_plots(terms_obj, duration_terms, term)
                    # verify that plots isn't generated in case of no recommendations
                    else:
                        assert PLOTS not in terms_obj[term], f"Expected plots to be absent in case of no recommendations"
            else:
                data = list_reco_container["recommendations"]["data"]
                assert len(data) == 0, f"Data is not empty! Length of data - Actual = {len(data)} expected = 0"

    else:
        print("Checking for recommendation notifications message...")
        result = check_if_recommendations_are_present(list_reco_container["recommendations"])
        assert result == False, f"Recommendations notifications does not contain the expected message - {NOT_ENOUGH_DATA_MSG}"

<<<<<<< HEAD

#TODO: Extract out the common part from this method which matches with container one to remove redundancy
def validate_namespace(update_results_namespace, update_results_json, list_reco_namespace, expected_duration_in_hours,
                       test_name, experiment_type):
    # Validate container image name and container name
    if update_results_namespace != None and list_reco_namespace != None:
        assert list_reco_namespace["namespace"] == update_results_namespace["namespace"], \
            f"Namespace names did not match! Actual -  {list_reco_namespace['namespace']} Expected - {update_results_namespace['namespace']}"

    # default term value
    term = SHORT_TERM
    # Validate timestamps
    if update_results_json != None:
        if expected_duration_in_hours == None:
            duration_in_hours = 0.0
        else:
            duration_in_hours = expected_duration_in_hours

        for update_results in update_results_json:
            interval_end_time = update_results["interval_end_time"]
            interval_start_time = update_results["interval_start_time"]
            print(f"interval_end_time = {interval_end_time} interval_start_time = {interval_start_time}")

            # Obtain the metrics
            metrics = ""
            namespaces = update_results['kubernetes_objects'][0]['namespaces']
            if update_results_namespace["namespace"] == namespaces['namespace']:
                metrics = namespaces["metrics"]
            if check_if_recommendations_are_present(list_reco_namespace["recommendations"]):
                terms_obj = list_reco_namespace["recommendations"]["data"][interval_end_time]["recommendation_terms"]
                current_config = list_reco_namespace["recommendations"]["data"][interval_end_time]["current"]

                duration_terms = {'short_term': 4, 'medium_term': 7, 'long_term': 15}
                for term in duration_terms.keys():
                    if check_if_recommendations_are_present(terms_obj[term]):
                        print(f"reco present for term {term}")
                        # Validate timestamps [deprecated as monitoring end time is moved to higher level]
                        # assert cost_obj[term]["monitoring_end_time"] == interval_end_time, \
                        #    f"monitoring end time {cost_obj[term]['monitoring_end_time']} did not match end timestamp {interval_end_time}"

                        # Validate the precision of the valid duration
                        duration = terms_obj[term]["duration_in_hours"]
                        assert validate_duration_in_hours_decimal_precision(duration), f"The value '{duration}' for " \
                                                                                       f"'{term}' has more than two decimal places"

                        monitoring_start_time = term_based_start_time(interval_end_time, term)
                        assert terms_obj[term]["monitoring_start_time"] == monitoring_start_time, \
                            f"actual = {terms_obj[term]['monitoring_start_time']} expected = {monitoring_start_time}"

                        # Validate duration in hrs
                        if expected_duration_in_hours is None:
                            duration_in_hours = set_duration_based_on_terms(duration_in_hours, term,
                                                                            interval_start_time, interval_end_time)

                        if test_name is not None:

                            if MEDIUM_TERM_TEST in test_name and term == MEDIUM_TERM:
                                assert terms_obj[term]["duration_in_hours"] == duration_in_hours, \
                                    f"Duration in hours did not match! Actual = {terms_obj[term]['duration_in_hours']} expected = {duration_in_hours}"
                            elif SHORT_TERM_TEST in test_name and term == SHORT_TERM:
                                assert terms_obj[term]["duration_in_hours"] == duration_in_hours, \
                                    f"Duration in hours did not match! Actual = {terms_obj[term]['duration_in_hours']} expected = {duration_in_hours}"
                            elif LONG_TERM_TEST in test_name and term == LONG_TERM:
                                assert terms_obj[term]["duration_in_hours"] == duration_in_hours, \
                                    f"Duration in hours did not match! Actual = {terms_obj[term]['duration_in_hours']} expected = {duration_in_hours}"
                        else:
                            print(
                                f"Actual = {terms_obj[term]['duration_in_hours']} expected = {duration_in_hours}")
                            assert terms_obj[term]["duration_in_hours"] == duration_in_hours, \
                                f"Duration in hours did not match! Actual = {terms_obj[term]['duration_in_hours']} expected = {duration_in_hours}"
                            duration_in_hours = set_duration_based_on_terms(duration_in_hours, term, interval_start_time,
                                                                            interval_end_time)

                        # Get engine objects
                        engines_list = ["cost", "performance"]

                        # Extract recommendation engine objects
                        recommendation_engines_object = None
                        if "recommendation_engines" in terms_obj[term]:
                            recommendation_engines_object = terms_obj[term]["recommendation_engines"]
                        if recommendation_engines_object is not None:
                            for engine_entry in engines_list:
                                if engine_entry in terms_obj[term]["recommendation_engines"]:
                                    engine_obj = terms_obj[term]["recommendation_engines"][engine_entry]
                                    validate_config(engine_obj["config"], metrics, experiment_type)
                                    validate_variation(current_config, engine_obj["config"], engine_obj["variation"])
                        # TODO: validate Plots data for namespace experiment_type
                        # validate_plots(terms_obj, duration_terms, term)
                    # verify that plots isn't generated in case of no recommendations
                    else:
                        assert PLOTS not in terms_obj[term], f"Expected plots to be absent in case of no recommendations"
            else:
                data = list_reco_namespace["recommendations"]["data"]
                assert len(data) == 0, f"Data is not empty! Length of data - Actual = {len(data)} expected = 0"

    else:
        print("Checking for recommendation notifications message...")
        result = check_if_recommendations_are_present(list_reco_namespace["recommendations"])
        assert result == False, f"Recommendations notifications does not contain the expected message - {NOT_ENOUGH_DATA_MSG}"


def validate_local_monitoring_container(create_exp_container, list_reco_container, expected_duration_in_hours, test_name):
=======
def validate_local_monitoring_container(create_exp_recommendation_settings, create_exp_container, list_reco_container, expected_duration_in_hours, test_name):
>>>>>>> 4bca0493 (makes duration terms dynamic)
    # Validate container image name and container name
    if create_exp_container != None and list_reco_container != None:
        assert list_reco_container["container_image_name"] == create_exp_container["container_image_name"], \
            f"Container image names did not match! Actual -  {list_reco_container['container_image_name']} Expected - {create_exp_container['container_image_name']}"

        assert list_reco_container["container_name"] == create_exp_container["container_name"], \
            f"Container names did not match! Acutal = {list_reco_container['container_name']} Expected - {create_exp_container['container_name']}"


    if expected_duration_in_hours == None:
        duration_in_hours = 0.0
    else:
        duration_in_hours = expected_duration_in_hours

    if check_if_recommendations_are_present(list_reco_container["recommendations"]):
        interval_end_time = list(list_reco_container['recommendations']['data'].keys())[0]
        print(f"interval_end_time = {interval_end_time}")

        terms_obj = list_reco_container["recommendations"]["data"][interval_end_time]["recommendation_terms"]
        current_config = list_reco_container["recommendations"]["data"][interval_end_time]["current"]

        duration_terms = {'short_term': 4, 'medium_term': 7, 'long_term': 15}

        if 'term_settings' in create_exp_recommendation_settings:
            duration_terms = get_duration_terms(create_exp_recommendation_settings)

        for term in duration_terms.keys():
            if check_if_recommendations_are_present(terms_obj[term]):
                print(f"reco present for term {term}")
                # Validate timestamps [deprecated as monitoring end time is moved to higher level]
                # assert cost_obj[term]["monitoring_end_time"] == interval_end_time, \
                #    f"monitoring end time {cost_obj[term]['monitoring_end_time']} did not match end timestamp {interval_end_time}"

                interval_start_time = list_reco_container['recommendations']['data'][interval_end_time]['recommendation_terms'][term]['monitoring_start_time']
                # Validate the precision of the valid duration
                duration = terms_obj[term]["duration_in_hours"]
                assert validate_duration_in_hours_decimal_precision(duration), f"The value '{duration}' for " \
                                                                               f"'{term}' has more than two decimal places"

                monitoring_start_time = term_based_start_time(interval_end_time, term)
                assert terms_obj[term]["monitoring_start_time"] == monitoring_start_time, \
                    f"actual = {terms_obj[term]['monitoring_start_time']} expected = {monitoring_start_time}"

                # Validate duration in hrs
                if expected_duration_in_hours is None:
                    duration_in_hours = set_duration_based_on_terms(duration_in_hours, term,
                                                                    interval_start_time, interval_end_time)

                if test_name is not None:

                    if MEDIUM_TERM_TEST in test_name and term == MEDIUM_TERM:
                        assert terms_obj[term]["duration_in_hours"] == duration_in_hours, \
                            f"Duration in hours did not match! Actual = {terms_obj[term]['duration_in_hours']} expected = {duration_in_hours}"
                    elif SHORT_TERM_TEST in test_name and term == SHORT_TERM:
                        assert terms_obj[term]["duration_in_hours"] == duration_in_hours, \
                            f"Duration in hours did not match! Actual = {terms_obj[term]['duration_in_hours']} expected = {duration_in_hours}"
                    elif LONG_TERM_TEST in test_name and term == LONG_TERM:
                        assert terms_obj[term]["duration_in_hours"] == duration_in_hours, \
                            f"Duration in hours did not match! Actual = {terms_obj[term]['duration_in_hours']} expected = {duration_in_hours}"
                else:
                    print(
                        f"Actual = {terms_obj[term]['duration_in_hours']} expected = {duration_in_hours}")
                    assert terms_obj[term]["duration_in_hours"] == duration_in_hours, \
                        f"Duration in hours did not match! Actual = {terms_obj[term]['duration_in_hours']} expected = {duration_in_hours}"
                    duration_in_hours = set_duration_based_on_terms(duration_in_hours, term, interval_start_time,
                                                                    interval_end_time)

                # Get engine objects
                if "model_settings" in create_exp_recommendation_settings:
                     engines_list = create_exp_recommendation_settings["model_settings"]["models"]
                else :
                     engines_list = ["cost", "performance"]

                # Extract recommendation engine objects
                recommendation_engines_object = None
                if "recommendation_engines" in terms_obj[term]:
                    recommendation_engines_object = terms_obj[term]["recommendation_engines"]
                if recommendation_engines_object is not None:
                    for engine_entry in engines_list:
                        if engine_entry in terms_obj[term]["recommendation_engines"]:
                            engine_obj = terms_obj[term]["recommendation_engines"][engine_entry]
                            validate_config_local_monitoring(engine_obj["config"])
                            validate_variation_local_monitoring(current_config, engine_obj["config"], engine_obj["variation"], engine_obj)
                # validate Plots data
                validate_plots(terms_obj, duration_terms, term)
            # verify that plots isn't generated in case of no recommendations
            else:
                assert PLOTS not in terms_obj[term], f"Expected plots to be absent in case of no recommendations"
    else:
        notifications = list_reco_container["recommendations"]["notifications"]
        if NOTIFICATION_CODE_FOR_NOT_ENOUGH_DATA in notifications:
            assert notifications[NOTIFICATION_CODE_FOR_NOT_ENOUGH_DATA]["message"] == NOT_ENOUGH_DATA_MSG

        data = list_reco_container["recommendations"]["data"]
        assert len(data) == 0, f"Data is not empty! Length of data - Actual = {len(data)} expected = 0"


def validate_local_monitoring_namespace(create_exp_recommendation_settings, create_exp_namespace, list_reco_namespace, expected_duration_in_hours, test_name):
    # Validate namespace name
    if create_exp_namespace != None and list_reco_namespace != None:
        assert create_exp_namespace["namespace"] == list_reco_namespace["namespace"], \
            f"Namespace names did not match! Actual -  {list_reco_namespace['namespace']} Expected - {create_exp_namespace['namespace']}"

    if expected_duration_in_hours == None:
        duration_in_hours = 0.0
    else:
        duration_in_hours = expected_duration_in_hours

    if check_if_recommendations_are_present(list_reco_namespace["recommendations"]):
        interval_end_time = list(list_reco_namespace['recommendations']['data'].keys())[0]
        print(f"interval_end_time = {interval_end_time}")

        terms_obj = list_reco_namespace["recommendations"]["data"][interval_end_time]["recommendation_terms"]
        current_config = list_reco_namespace["recommendations"]["data"][interval_end_time]["current"]

        term_mapping = {"short": "short_term", "medium": "medium_term", "long": "long_term"}

        if 'term_settings' in create_exp_recommendation_settings:
            duration_terms = create_exp_recommendation_settings["term_settings"]["terms"]
        else :
            duration_terms = ['short', 'medium', 'long']
        for terms in duration_terms:
            term = term_mapping.get(terms)
            if check_if_recommendations_are_present(terms_obj[term]):
                print(f"reco present for term {term}")

                interval_start_time = list_reco_namespace['recommendations']['data'][interval_end_time]['recommendation_terms'][term]['monitoring_start_time']
                # Validate the precision of the valid duration
                duration = terms_obj[term]["duration_in_hours"]
                assert validate_duration_in_hours_decimal_precision(duration), f"The value '{duration}' for " \
                                                                               f"'{term}' has more than two decimal places"

                monitoring_start_time = term_based_start_time(interval_end_time, term)
                assert terms_obj[term]["monitoring_start_time"] == monitoring_start_time, \
                    f"actual = {terms_obj[term]['monitoring_start_time']} expected = {monitoring_start_time}"

                # Validate duration in hrs
                if expected_duration_in_hours is None:
                    duration_in_hours = set_duration_based_on_terms(duration_in_hours, term,
                                                                    interval_start_time, interval_end_time)

                if test_name is not None:

                    if MEDIUM_TERM_TEST in test_name and term == MEDIUM_TERM:
                        assert terms_obj[term]["duration_in_hours"] == duration_in_hours, \
                            f"Duration in hours did not match! Actual = {terms_obj[term]['duration_in_hours']} expected = {duration_in_hours}"
                    elif SHORT_TERM_TEST in test_name and term == SHORT_TERM:
                        assert terms_obj[term]["duration_in_hours"] == duration_in_hours, \
                            f"Duration in hours did not match! Actual = {terms_obj[term]['duration_in_hours']} expected = {duration_in_hours}"
                    elif LONG_TERM_TEST in test_name and term == LONG_TERM:
                        assert terms_obj[term]["duration_in_hours"] == duration_in_hours, \
                            f"Duration in hours did not match! Actual = {terms_obj[term]['duration_in_hours']} expected = {duration_in_hours}"
                else:
                    print(
                        f"Actual = {terms_obj[term]['duration_in_hours']} expected = {duration_in_hours}")
                    assert terms_obj[term]["duration_in_hours"] == duration_in_hours, \
                        f"Duration in hours did not match! Actual = {terms_obj[term]['duration_in_hours']} expected = {duration_in_hours}"
                    duration_in_hours = set_duration_based_on_terms(duration_in_hours, term, interval_start_time,
                                                                    interval_end_time)

                # Get engine objects
                if "model_settings" in create_exp_recommendation_settings:
                    engines_list = create_exp_recommendation_settings["model_settings"]["models"]
                else :
                    engines_list = ["cost", "performance"]

                # Extract recommendation engine objects
                recommendation_engines_object = None
                if "recommendation_engines" in terms_obj[term]:
                    recommendation_engines_object = terms_obj[term]["recommendation_engines"]
                if recommendation_engines_object is not None:
                    for engine_entry in engines_list:
                        if engine_entry in terms_obj[term]["recommendation_engines"]:
                            engine_obj = terms_obj[term]["recommendation_engines"][engine_entry]
                            validate_config_local_monitoring(engine_obj["config"])
                            validate_variation_local_monitoring(current_config, engine_obj["config"], engine_obj["variation"], engine_obj)
    else:
        notifications = list_reco_namespace["recommendations"]["notifications"]
        if NOTIFICATION_CODE_FOR_NOT_ENOUGH_DATA in notifications:
            assert notifications[NOTIFICATION_CODE_FOR_NOT_ENOUGH_DATA]["message"] == NOT_ENOUGH_DATA_MSG

        data = list_reco_namespace["recommendations"]["data"]
        assert len(data) == 0, f"Data is not empty! Length of data - Actual = {len(data)} expected = 0"


def validate_plots(terms_obj, duration_terms, term):
    plots = terms_obj[term][PLOTS]
    datapoint = plots[DATA_POINTS]
    plots_data = plots[PLOTS_DATA]

    assert plots is not None, f"Expected plots to be available"
    assert datapoint is not None, f"Expected datapoint to be available"
    # validate the count of data points for the specific term
    assert datapoint == duration_terms[term], f"datapoint Expected: {duration_terms[term]}, Obtained: {datapoint}"
    assert len(plots_data) == duration_terms[term], f"plots_data size Expected: {duration_terms[term]}, Obtained: {len(plots_data)}"
    # TODO: validate the datapoint JSON objects
    # TODO: validate the actual JSONs present, how many are empty for each term, this should be passed as an input
    # TODO: validate the format value against the results metrics


def set_duration_based_on_terms(duration_in_hours, term, interval_start_time, interval_end_time):
    diff = time_diff_in_hours(interval_start_time, interval_end_time)
    duration_in_hours += diff
    print(f"duration in hours = {duration_in_hours}")

    if term == "short_term" and duration_in_hours > SHORT_TERM_DURATION_IN_HRS_MAX:
        duration_in_hours = SHORT_TERM_DURATION_IN_HRS_MAX
    elif term == "medium_term" and duration_in_hours > MEDIUM_TERM_DURATION_IN_HRS_MAX:
        duration_in_hours = MEDIUM_TERM_DURATION_IN_HRS_MAX
    elif term == "long_term" and duration_in_hours > LONG_TERM_DURATION_IN_HRS_MAX:
        duration_in_hours = LONG_TERM_DURATION_IN_HRS_MAX

    return duration_in_hours


def validate_config(reco_config, metrics, experiment_type):
    cpu_format_type = ""
    memory_format_type = ""
    # default values corresponds to container experiment type
    cpu_usage = "cpuUsage"
    memory_usage = "memoryUsage"
    filtered_metrics = metrics

    if experiment_type == NAMESPACE_EXPERIMENT_TYPE:
        cpu_usage = "namespaceCpuUsage"
        memory_usage = "namespaceMemoryUsage"
        # filter out metrics which do not have 'format' value
        filtered_metrics = [
            metric for metric in metrics
            if metric["name"] not in ("namespaceRunningPods", "namespaceTotalPods")
        ]

    for metric in filtered_metrics:
        if cpu_usage == metric["name"]:
            cpu_format_type = metric['results']['aggregation_info']['format']

        if memory_usage == metric["name"]:
            memory_format_type = metric['results']['aggregation_info']['format']

    usage_list = ["requests", "limits"]
    for usage in usage_list:
        assert reco_config[usage]["cpu"][
                   "amount"] > 0, f"cpu amount in recommendation config is {reco_config[usage]['cpu']['amount']}"
        assert reco_config[usage]["cpu"][
                   "format"] == cpu_format_type, f"cpu format in recommendation config is {reco_config[usage]['cpu']['format']} instead of {cpu_format_type}"
        assert reco_config[usage]["memory"][
                   "amount"] > 0, f"cpu amount in recommendation config is {reco_config[usage]['memory']['amount']}"
        assert reco_config[usage]["memory"][
                   "format"] == memory_format_type, f"memory format in recommendation config is {reco_config[usage]['memory']['format']} instead of {memory_format_type}"

def validate_config_local_monitoring(reco_config):
    cpu_format_type = "cores"
    memory_format_type = "bytes"

    usage_list = ["requests", "limits"]
    for usage in usage_list:
        if "cpu" in reco_config[usage]:
            assert reco_config[usage]["cpu"][
                       "amount"] > 0, f"cpu amount in recommendation config is {reco_config[usage]['cpu']['amount']}"
            assert reco_config[usage]["cpu"][
                    "format"] == cpu_format_type, f"cpu format in recommendation config is {reco_config[usage]['cpu']['format']} instead of {cpu_format_type}"
        if "memory" in reco_config[usage]:
            assert reco_config[usage]["memory"][
                       "amount"] > 0, f"cpu amount in recommendation config is {reco_config[usage]['memory']['amount']}"
            assert reco_config[usage]["memory"][
                       "format"] == memory_format_type, f"memory format in recommendation config is {reco_config[usage]['memory']['format']} instead of {memory_format_type}"

def check_if_recommendations_are_present(cost_obj):
    notifications = cost_obj["notifications"]
    if NOTIFICATION_CODE_FOR_NOT_ENOUGH_DATA in notifications:
        return False
    return True


def time_diff_in_hours(interval_start_time, interval_end_time):
    start_date = datetime.strptime(interval_start_time, "%Y-%m-%dT%H:%M:%S.%fZ")
    end_date = datetime.strptime(interval_end_time, "%Y-%m-%dT%H:%M:%S.%fZ")
    diff = end_date - start_date
    return round(diff.total_seconds() / 3600, 2)


def strip_double_quotes_for_field(json_file, field, filename):
    find = "\"{{" + field + "}}\""
    replace = "{{" + field + "}}"
    with open(json_file, 'r') as file:
        data = file.read()

        data = data.replace(find, replace)

        with open(filename, 'w') as file:
            file.write(data)


def compare_json_files(json_file1, json_file2):
    with open(json_file1, "r") as f1:
        try:
            json_data1 = json.load(f1)
        except json.JSONDecodeError:
            print("Received JSONDecodeError")
            json_data1 = {}

    with open(json_file2, "r") as f2:
        try:
            json_data2 = json.load(f2)
        except json.JSONDecodeError:
            print("Received JSONDecodeError")
            json_data2 = {}

    if json_data1 and json_data2:
        if json_data1 == json_data2:
            print("The two JSON files are identical!")
            return True
        else:
            print("The two JSON files are different!")
            return False
    else:
        print(f"JSON files are empty! Check the files {json_file1} and {json_file2}")
        return False


def get_kruize_pod(namespace):
    command = f"kubectl get pod -n {namespace} | grep kruize | grep -v kruize-ui | grep -v kruize-db | cut -d ' ' -f1"
    # Execute the command and capture the output
    output = subprocess.check_output(command, shell=True)

    pod_name = output.decode('utf-8')
    print(f"pod name = {pod_name}")
    return pod_name.rstrip()


def delete_kruize_pod(namespace):
    pod_name = get_kruize_pod(namespace)

    command = f"kubectl delete pod {pod_name} -n {namespace}"
    print(command)

    # Execute the command and capture the output
    output = subprocess.check_output(command, shell=True)

    print(output.decode('utf-8'))


def check_pod_running(namespace, pod_name):
    command = f"kubectl get pod -n {namespace} | grep {pod_name}"

    # set the maximum number of retries and the retry interval
    MAX_RETRIES = 12
    RETRY_INTERVAL = 5

    # execute the command and capture the output
    output = subprocess.check_output(command, shell=True)

    # check if the pod is running
    retry_count = 0
    while "Running" not in output.decode('utf-8') and retry_count < MAX_RETRIES:
        time.sleep(RETRY_INTERVAL)
        output = subprocess.check_output(command, shell=True)
        retry_count += 1

    if retry_count == MAX_RETRIES:
        print(f"Kruize Pod {pod_name} did not start within the specified time")
        return False
    else:
        print(f"Kruize Pod {pod_name} is now running")
        return True


def get_index_of_metric(metrics: list, metric_name: str):
    for i, metric in enumerate(metrics):
        if metric["name"] == metric_name:
            return i

    return None


def check_if_dict_has_same_keys(base_dict, test_dict):
    # Return false if the key set is not equal
    if set(base_dict.keys()) != set(test_dict.keys()):
        return False

    for key in base_dict.keys():
        if key not in test_dict:
            return False
        if isinstance(base_dict[key], dict) and isinstance(test_dict[key], dict):
            check_if_dict_has_same_keys(base_dict[key], test_dict[key])
    return True


def validate_variation(current_config: dict, recommended_config: dict, variation_config: dict):
    # Check structure
    assert check_if_dict_has_same_keys(recommended_config, variation_config) == True

    # Create temporary dict if it's none jus to make process easier
    if current_config == None:
        current_config = {}

    # Check values
    REQUESTS_KEY = "requests"
    LIMITS_KEY = "limits"
    CPU_KEY = "cpu"
    MEMORY_KEY = "memory"
    AMOUNT_KEY = "amount"
    FORMAT_KEY = "format"

    # Initialise requests holders
    current_requests: dict = None
    recommended_requests: dict = None
    variation_requests: dict = None

    # Initialise limits holders
    current_limits: dict = None
    recommended_limits: dict = None
    variation_limits: dict = None

    if REQUESTS_KEY in current_config:
        current_requests = current_config[REQUESTS_KEY]
    if LIMITS_KEY in current_config:
        current_limits = current_config[LIMITS_KEY]

    if REQUESTS_KEY in recommended_config:
        recommended_requests = recommended_config[REQUESTS_KEY]
    if LIMITS_KEY in recommended_config:
        recommended_limits = recommended_config[LIMITS_KEY]

    if REQUESTS_KEY in variation_config:
        variation_requests = variation_config[REQUESTS_KEY]
    if LIMITS_KEY in variation_config:
        variation_limits = variation_config[LIMITS_KEY]

    if recommended_requests is not None:
        current_cpu_value = 0
        current_memory_value = 0
        if CPU_KEY in recommended_requests:
            if CPU_KEY in current_requests and AMOUNT_KEY in current_requests[CPU_KEY]:
                current_cpu_value = current_requests[CPU_KEY][AMOUNT_KEY]
            assert variation_requests[CPU_KEY][AMOUNT_KEY] == recommended_requests[CPU_KEY][
                AMOUNT_KEY] - current_cpu_value
            assert variation_requests[CPU_KEY][FORMAT_KEY] == recommended_requests[CPU_KEY][FORMAT_KEY]
        if MEMORY_KEY in recommended_requests:
            if MEMORY_KEY in current_requests and AMOUNT_KEY in current_requests[MEMORY_KEY]:
                current_memory_value = current_requests[MEMORY_KEY][AMOUNT_KEY]
            assert variation_requests[MEMORY_KEY][AMOUNT_KEY] == recommended_requests[MEMORY_KEY][
                AMOUNT_KEY] - current_memory_value
            assert variation_requests[MEMORY_KEY][FORMAT_KEY] == recommended_requests[MEMORY_KEY][FORMAT_KEY]
    if recommended_limits is not None:
        current_cpu_value = 0
        current_memory_value = 0
        if CPU_KEY in recommended_limits:
            if CPU_KEY in current_limits and AMOUNT_KEY in current_limits[CPU_KEY]:
                current_cpu_value = current_limits[CPU_KEY][AMOUNT_KEY]
            assert variation_limits[CPU_KEY][AMOUNT_KEY] == recommended_limits[CPU_KEY][AMOUNT_KEY] - current_cpu_value
            assert variation_limits[CPU_KEY][FORMAT_KEY] == recommended_limits[CPU_KEY][FORMAT_KEY]
        if MEMORY_KEY in recommended_limits:
            if MEMORY_KEY in current_limits and AMOUNT_KEY in current_limits[MEMORY_KEY]:
                current_memory_value = current_limits[MEMORY_KEY][AMOUNT_KEY]
            assert variation_limits[MEMORY_KEY][AMOUNT_KEY] == recommended_limits[MEMORY_KEY][
                AMOUNT_KEY] - current_memory_value
            assert variation_limits[MEMORY_KEY][FORMAT_KEY] == recommended_limits[MEMORY_KEY][FORMAT_KEY]


def validate_variation_local_monitoring(current_config: dict, recommended_config: dict, variation_config: dict, engine_obj):
    # Check structure
    assert check_if_dict_has_same_keys(recommended_config, variation_config) == True

    # Create temporary dict if it's none jus to make process easier
    if current_config == None:
        current_config = {}

    # Check values
    REQUESTS_KEY = "requests"
    LIMITS_KEY = "limits"
    CPU_KEY = "cpu"
    MEMORY_KEY = "memory"
    AMOUNT_KEY = "amount"
    FORMAT_KEY = "format"

    # Initialise requests holders
    current_requests: dict = None
    recommended_requests: dict = None
    variation_requests: dict = None

    # Initialise limits holders
    current_limits: dict = None
    recommended_limits: dict = None
    variation_limits: dict = None

    if REQUESTS_KEY in current_config:
        current_requests = current_config[REQUESTS_KEY]
    if LIMITS_KEY in current_config:
        current_limits = current_config[LIMITS_KEY]

    if REQUESTS_KEY in recommended_config:
        recommended_requests = recommended_config[REQUESTS_KEY]
    if LIMITS_KEY in recommended_config:
        recommended_limits = recommended_config[LIMITS_KEY]

    if REQUESTS_KEY in variation_config:
        variation_requests = variation_config[REQUESTS_KEY]
    if LIMITS_KEY in variation_config:
        variation_limits = variation_config[LIMITS_KEY]

    if recommended_requests is not None:
        current_cpu_value = 0
        current_memory_value = 0
        if CPU_KEY in recommended_requests:
            if current_requests is not None and CPU_KEY in current_requests and AMOUNT_KEY in current_requests[CPU_KEY]:
                current_cpu_value = current_requests[CPU_KEY][AMOUNT_KEY]
            assert variation_requests[CPU_KEY][AMOUNT_KEY] == recommended_requests[CPU_KEY][
                AMOUNT_KEY] - current_cpu_value
            assert variation_requests[CPU_KEY][FORMAT_KEY] == recommended_requests[CPU_KEY][FORMAT_KEY]
        else:
            assert NOTIFICATION_CODE_FOR_CPU_RECORDS_ARE_IDLE in engine_obj["notifications"]
            assert engine_obj["notifications"][NOTIFICATION_CODE_FOR_CPU_RECORDS_ARE_IDLE]["message"] == NOTIFICATION_CODE_FOR_CPU_RECORDS_ARE_IDLE_MESSAGE

        if MEMORY_KEY in recommended_requests:
            if current_requests is not None and MEMORY_KEY in current_requests and AMOUNT_KEY in current_requests[MEMORY_KEY]:
                current_memory_value = current_requests[MEMORY_KEY][AMOUNT_KEY]
            assert variation_requests[MEMORY_KEY][AMOUNT_KEY] == recommended_requests[MEMORY_KEY][
                AMOUNT_KEY] - current_memory_value
            assert variation_requests[MEMORY_KEY][FORMAT_KEY] == recommended_requests[MEMORY_KEY][FORMAT_KEY]
    if recommended_limits is not None:
        current_cpu_value = 0
        current_memory_value = 0
        if CPU_KEY in recommended_limits:
            if current_limits is not None and CPU_KEY in current_limits and AMOUNT_KEY in current_limits[CPU_KEY]:
                current_cpu_value = current_limits[CPU_KEY][AMOUNT_KEY]
            assert variation_limits[CPU_KEY][AMOUNT_KEY] == recommended_limits[CPU_KEY][AMOUNT_KEY] - current_cpu_value
            assert variation_limits[CPU_KEY][FORMAT_KEY] == recommended_limits[CPU_KEY][FORMAT_KEY]
        else:
            assert NOTIFICATION_CODE_FOR_CPU_RECORDS_ARE_IDLE in engine_obj["notifications"]
            assert engine_obj["notifications"][NOTIFICATION_CODE_FOR_CPU_RECORDS_ARE_IDLE]["message"] == NOTIFICATION_CODE_FOR_CPU_RECORDS_ARE_IDLE_MESSAGE

        if MEMORY_KEY in recommended_limits:
            if current_limits is not None and MEMORY_KEY in current_limits and AMOUNT_KEY in current_limits[MEMORY_KEY]:
                current_memory_value = current_limits[MEMORY_KEY][AMOUNT_KEY]
            assert variation_limits[MEMORY_KEY][AMOUNT_KEY] == recommended_limits[MEMORY_KEY][
                AMOUNT_KEY] - current_memory_value
            assert variation_limits[MEMORY_KEY][FORMAT_KEY] == recommended_limits[MEMORY_KEY][FORMAT_KEY]


def check_optimised_codes(cost_notifications, perf_notifications):
    assert CPU_REQUEST_OPTIMISED_CODE in cost_notifications
    assert CPU_REQUEST_OPTIMISED_CODE in perf_notifications

    assert CPU_LIMIT_OPTIMISED_CODE in cost_notifications
    assert CPU_LIMIT_OPTIMISED_CODE in perf_notifications

    assert MEMORY_REQUEST_OPTIMISED_CODE in cost_notifications
    assert MEMORY_REQUEST_OPTIMISED_CODE in perf_notifications

    assert MEMORY_LIMIT_OPTIMISED_CODE in cost_notifications
    assert MEMORY_LIMIT_OPTIMISED_CODE in perf_notifications


def validate_recommendation_for_cpu_mem_optimised(recommendations: dict, current: dict, profile: str):
    assert "variation" in recommendations["recommendation_engines"][profile]
    assert "config" in recommendations["recommendation_engines"][profile]
    assert recommendations["recommendation_engines"][profile]["config"]["requests"]["cpu"]["amount"] == current["requests"]["cpu"]["amount"]
    assert recommendations["recommendation_engines"][profile]["config"]["limits"]["cpu"]["amount"] == current["limits"]["cpu"]["amount"]
    assert recommendations["recommendation_engines"][profile]["config"]["requests"]["memory"]["amount"] == current["requests"]["memory"]["amount"]
    assert recommendations["recommendation_engines"][profile]["config"]["limits"]["memory"]["amount"] == current["limits"]["memory"]["amount"]


def validate_list_metadata_parameters(import_metadata_json, list_metadata_json, cluster_name=None, namespace=None):
    datasources = list_metadata_json.get('datasources', {})

    if len(datasources) != 1:
        return False

    # Loop through the datasources dictionary
    for key, value in datasources.items():
        assert import_metadata_json['datasource_name'] == value.get('datasource_name')

        if cluster_name is not None:
            # Extract clusters from the current datasource
            clusters = value.get('clusters', {})

            for clusters_key, clusters_value in clusters.items():
                assert cluster_name == clusters_value.get('cluster_name'), f"Invalid cluster name: {cluster_name}"

                # If namespace is provided, perform namespace validation
                if namespace is not None:
                    # Extract namespaces from the current cluster
                    namespaces = clusters[cluster_name].get('namespaces', {})

                    assert namespace in [ns.get('namespace') for ns in namespaces.values()], f"Invalid namespace: {namespace}"


def create_namespace(namespace_name):
    # Load kube config
    config.load_kube_config()

    # Create a V1Namespace object
    namespace = client.V1Namespace(
        metadata=client.V1ObjectMeta(name=namespace_name)
    )

    # Create a Kubernetes API client
    api_instance = client.CoreV1Api()

    # Create the namespace
    try:
        api_instance.create_namespace(namespace)

        print(f"Namespace '{namespace_name}' created successfully.")
    except client.exceptions.ApiException as e:
        if e.status == 409:
            print(f"Namespace '{namespace_name}' already exists.")
        else:
            print(f"Error creating namespace: {e}")


def delete_namespace(namespace_name):
    # Load kube config
    config.load_kube_config()

    # Create a Kubernetes API client
    api_instance = client.CoreV1Api()

    # Delete the namespace
    try:
        api_instance.delete_namespace(name=namespace_name)
        print(f"Namespace '{namespace_name}' deleted successfully.")
    except client.exceptions.ApiException as e:
        if e.status == 404:
            print(f"Namespace '{namespace_name}' not found.")
        else:
            print(f"Exception deleting namespace: {e}")


def scale_deployment(namespace, deployment_name, replicas):
    """
    Scale a Kubernetes Deployment to the desired number of replicas.

    This function scales a specified Deployment in a given namespace to a specified number
    of replicas using the Kubernetes Python client library. It achieves this by creating
    a Scale object and using the AppsV1Api to update the Deployment's scale.

    Args:
    - namespace (str): The namespace of the Deployment.
    - deployment_name (str): The name of the Deployment.
    - replicas (int): The desired number of replicas.

    Returns:
    None
    """
    config.load_kube_config()  # Load kube config from default location

    # Create an API client
    apps_v1 = client.AppsV1Api()

    # Define the scale object
    scale = client.V1Scale(
        api_version='autoscaling/v1',
        kind='Scale',
        metadata=client.V1ObjectMeta(name=deployment_name, namespace=namespace),
        spec=client.V1ScaleSpec(replicas=replicas)
    )

    # Scale the deployment
    try:
        response = apps_v1.replace_namespaced_deployment_scale(
            name=deployment_name,
            namespace=namespace,
            body=scale
        )
        print(f"Deployment {deployment_name} scaled to {replicas} replicas successfully.")
    except client.exceptions.ApiException as e:
        print(f"Error scaling deployment {deployment_name}: {e}")


def scale_statefulset(namespace, statefulset_name, replicas):
    """
    Scale a Kubernetes Statefulset to the desired number of replicas.

    This function scales a specified Statefulset in a given namespace to a specified number
    of replicas using the Kubernetes Python client library. It achieves this by creating
    a Scale object and using the AppsV1Api to update the Statefulset's scale.

    Args:
    - namespace (str): The namespace of the Deployment.
    - statefulset_name (str): The name of the Statefulset.
    - replicas (int): The desired number of replicas.

    Returns:
    None
    """
    config.load_kube_config()  # Load kube config from default location

    # Create an API client
    apps_v1 = client.AppsV1Api()

    # Define the scale object
    scale = client.V1Scale(
        api_version='autoscaling/v1',
        kind='Scale',
        metadata=client.V1ObjectMeta(name=statefulset_name, namespace=namespace),
        spec=client.V1ScaleSpec(replicas=replicas)
    )

    # Scale the statefulset
    try:
        response = apps_v1.replace_namespaced_stateful_set_scale(
            name=statefulset_name,
            namespace=namespace,
            body=scale
        )
        print(f"StatefulSet {statefulset_name} scaled to {replicas} replicas successfully.")
    except client.exceptions.ApiException as e:
        print(f"Error scaling statefulset {statefulset_name}: {e}")


# validate duration_in_hours decimal precision
def validate_duration_in_hours_decimal_precision(duration_in_hours):
    """
        Validate that the given value has at most two decimal places.
        :param duration_in_hours: The value to be validated.
        :return: True if the value has at most two decimal places, False otherwise.
    """
    return re.match(r'^\d+\.\d{3,}$', str(duration_in_hours)) is None

# clone GitHub repository
def clone_repo(repo_url, target_dir=None):
    """
    Clone a Git repository without exiting the program.

    Parameters:
    - repo_url: The URL of the Git repository to clone.
    - target_dir: Optional target directory to clone the repository into. If not specified, defaults to the repo name.

    Returns:
    - True if the repository was successfully cloned, False otherwise.
    """
    # Construct the git clone command
    clone_command = ["git", "clone", repo_url]

    # If target_dir is specified, add it to the command
    if target_dir:
        clone_command.append(target_dir)

    try:
        # Run the git clone command
        print(f"Cloning repository from {repo_url}...")
        subprocess.run(clone_command, check=True)
        print("Repository cloned successfully.")
        return True
    except subprocess.CalledProcessError as e:
        print(f"Failed to clone the repository: {e}")
        return False


#  Install Benchmarks
def benchmarks_install(namespace="default", manifests="default_manifests", name="techempower"):

    # Change to the benchmarks directory
    try:
        os.chdir("benchmarks")
    except Exception as e:
        print(f"ERROR: Could not change to 'benchmarks' directory: {e}")

    print(f"Installing {name} benchmark into cluster")

    # Change to the techempower directory
    try:
        if name == "sysbench":
            os.chdir("sysbench")
        else:
            os.chdir("techempower")
    except Exception as e:
        print(f"ERROR: Could not change to {name} directory: {e}")


    # Apply the Kubernetes manifests
    kubectl_command = f"kubectl apply -f manifests/{manifests} -n {namespace}"
    result = subprocess.run(kubectl_command, shell=True)

    # Check for errors
    if subprocess.call("kubectl get pods | grep -iE 'error|fail|crash'", shell=True) == 0:
        print("ERROR: TechEmpower app failed to start, exiting")

    # Navigate back to the original directory
    os.chdir("../..")


def get_urls(namespace, cluster_type):
    kubectl_cmd = f"kubectl -n {namespace}"

    # Get the Techempower port using kubectl
    techempower_port_cmd = f"{kubectl_cmd} get svc tfb-qrh-service --no-headers -o=custom-columns=PORT:.spec.ports[*].nodePort"
    techempower_port = subprocess.check_output(techempower_port_cmd, shell=True).decode('utf-8').strip()

    # Get the Techempower IP using kubectl
    techempower_ip_cmd = f"{kubectl_cmd} get pods -l=app=tfb-qrh-deployment -o wide -o=custom-columns=NODE:.spec.nodeName --no-headers"
    techempower_ip = subprocess.check_output(techempower_ip_cmd, shell=True).decode('utf-8').strip()


    if cluster_type == "minikube":
        # Get the minikube IP using the `minikube ip` command
        minikube_ip = subprocess.check_output("minikube ip", shell=True).decode('utf-8').strip()
        techempower_url = f"http://{minikube_ip}:{techempower_port}"
    elif cluster_type == "openshift":
        # expose service kruize
        subprocess.run(['oc -n openshift-tuning expose service kruize'], shell=True, stdout=subprocess.PIPE,
                       stderr=subprocess.PIPE)

        # annotate kruize route
        subprocess.run(['oc -n openshift-tuning annotate route kruize --overwrite haproxy.router.openshift.io/timeout=60s'],
                       shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

        # expose service tfb-qrh-service
        subprocess.run([f'oc -n {namespace} expose service tfb-qrh-service'], shell=True, stdout=subprocess.PIPE,
                       stderr=subprocess.PIPE)

        # get tfb-qrh-service route
        techempower_url = subprocess.run([f'oc -n {namespace} get route tfb-qrh-service --no-headers -o custom-columns=NODE:.spec.host'],
                                         shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    else:
        raise ValueError("Unsupported CLUSTER_TYPE. Expected 'minikube' or 'openshift'.")

    return techempower_url



def apply_tfb_load(app_namespace, cluster_type):

    print("\n###################################################################")
    print(" Starting 20 min background load against the techempower benchmark ")
    print("###################################################################\n")

    techempower_load_image = "quay.io/kruizehub/tfb_hyperfoil_load:0.25.2"
    load_duration = 1200  # 20 minutes in seconds

    techempower_url = get_urls(app_namespace, cluster_type)

    if cluster_type == "minikube":
        techempower_route = techempower_url
    elif cluster_type == "openshift":
        techempower_route_cmd = ["oc", "get", "route", "-n", app_namespace, "--template={{range .items}}{{.spec.host}}{{\"\\n\"}}{{end}}"]
        techempower_route = subprocess.check_output(techempower_route_cmd).decode("utf-8").strip()


    # Run the docker command with subprocess
    docker_cmd = [
        "docker", "run", "-d", "--rm", "--network=host",
        techempower_load_image,
        "/opt/run_hyperfoil_load.sh", techempower_route,
        "queries?queries=20", str(load_duration), "1024", "8096"
    ]

    # Run the Docker command and get the container ID
    process = subprocess.Popen(docker_cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    container_id = process.stdout.read().decode().strip()

    if not container_id:
        raise RuntimeError(f"Failed to start Docker container. Error: {process.stderr.read().decode().strip()}")

    return container_id

#   Retrieve the status of the Docker container.
def get_container_status(container_id):
    result = subprocess.run(
        ["docker", "inspect", "--format", "{{.State.Status}}", container_id],
        capture_output=True,
        text=True
    )
    status = result.stdout.strip()
    return status

#   Wait until the Docker container completes and get its exit code.
def wait_for_container_to_complete(container_id):

    print("\n########################################################################################################")
    print(f"Waiting for container {container_id} to complete... before generating recommendations")
    print("##########################################################################################################\n")
    result = subprocess.run(
        ["docker", "wait", container_id],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    exit_code = result.stdout.strip()
    print(f"Container {container_id} has completed with exit code {exit_code}.")

def get_metric_profile_dir():
    # Get the current directory
    current_directory = Path(__file__).resolve().parent
    # Navigate up 3 levels and build the path to the 'manifests/app/adreess' directory
    base_dir = current_directory.parents[2]  # (index 2 because it's zero-based)
    metric_profile_dir = base_dir / 'manifests' / 'autotune' / 'performance-profiles'

    return metric_profile_dir

def validate_local_monitoring_recommendation_data_present(recommendations_json):
    if recommendations_json[0]['experiment_type'] == NAMESPACE_EXPERIMENT_TYPE:
        assert recommendations_json[0]['kubernetes_objects'][0]['namespaces']['recommendations']['data'], "Recommendations data is expected, but not present."
        assert recommendations_json[0]['kubernetes_objects'][0]['namespaces']['recommendations']['notifications'][NOTIFICATION_CODE_FOR_RECOMMENDATIONS_AVAILABLE]['message'] == RECOMMENDATIONS_AVAILABLE, "Recommendations notification is expected, but not present."
    if recommendations_json[0]['experiment_type'] == CONTAINER_EXPERIMENT_TYPE:
        list_reco_containers_length = len(recommendations_json[0]['kubernetes_objects'][0]['containers'])

        # Validate if all the containers are present
        for i in range(list_reco_containers_length):
             assert recommendations_json[0]['kubernetes_objects'][0]['containers'][i]['recommendations']['data'], "Recommendations data is expected, but not present."
             assert recommendations_json[0]['kubernetes_objects'][0]['containers'][i]['recommendations']['notifications'][NOTIFICATION_CODE_FOR_RECOMMENDATIONS_AVAILABLE]['message'] == RECOMMENDATIONS_AVAILABLE, "Recommendations notification is expected, but not present."


def validate_limits_map_for_accelerator(limits: dict):
    for resource, resource_obj in limits.items():
        # Check if the key contains "nvidia" and matches the MIG pattern
        if "nvidia" in resource:
            # Assert that the key matches the expected MIG pattern
            assert re.match(MIG_PATTERN, resource), f"Resource '{resource}' does not match the expected MIG pattern."

            # Assert that the amount is 1.0 and format is "cores"
            assert resource_obj.get("amount") == 1.0, f"Resource '{resource}' has an invalid amount: {resource_obj.get('amount')}"
            assert resource_obj.get("format") == "cores", f"Resource '{resource}' has an invalid format: {resource_obj.get('format')}"



def validate_accelerator_recommendations_for_container(recommendations_json):
    if 'experiment_type' in recommendations_json[0]:
        assert recommendations_json[0]['experiment_type'] == CONTAINER_EXPERIMENT_TYPE, "Test is only applicable for container experiment type"

    assert recommendations_json[0]['kubernetes_objects'], "Kubernetes objects expected"
    # Test needs to be changed if we support multiple kubernetes objects
    kubernetes_obj = recommendations_json[0]['kubernetes_objects'][0]
    assert kubernetes_obj["containers"], "Containers array expected"

    containers = kubernetes_obj["containers"]
    assert len(containers) > 0, "Expecting atleast one container"

    for container in containers:
        assert container['recommendations'], "Recommendations object expected"
        recommendations = container['recommendations']

        assert recommendations["data"], "Data object expected"
        data = recommendations["data"]

        assert len(data) > 0, "Data object cannot be empty"

        for timestamp, interval_recommendation_obj in data.items():
            assert interval_recommendation_obj["recommendation_terms"], "Term based recommendations expected"
            terms = interval_recommendation_obj["recommendation_terms"]

            assert len(terms) > 0, "Atleast one term is expected"

            for term_name, term_obj in terms.items():
                term_notifications = term_obj["notifications"]

                if NOTIFICATION_CODE_FOR_COST_RECOMMENDATIONS_AVAILABLE in term_notifications:
                    cost_limits_map = term_obj["recommendation_engines"]["cost"]["config"]["limits"]
                    validate_limits_map_for_accelerator(cost_limits_map)

                if NOTIFICATION_CODE_FOR_PERFORMANCE_RECOMMENDATIONS_AVAILABLE in term_notifications:
                    perf_limits_map = term_obj["recommendation_engines"]["performance"]["config"]["limits"]
                    validate_limits_map_for_accelerator(perf_limits_map)

#@pytest.mark.skip(reason="Not a test function")
def validate_job_status(job_id, base_url, caplog):
    # Common keys expected in both responses
    common_keys = {
        "status", "total_experiments", "processed_experiments", "job_id", "job_start_time", "job_end_time"
    }

    # Extra keys expected when verbose=true
    verbose_keys = {
        "experiments"
    }

    # Make the GET request without verbose
    response_basic = get_bulk_job_status(job_id,False)
    # Verify common keys in the basic response
    assert common_keys.issubset(
        response_basic.json()['summary'].keys()), f"Missing keys in response: {common_keys - response_basic.json()['summary'].keys()}"

    response_verbose = get_bulk_job_status(job_id,include="summary,experiments")
    # Verify common and verbose keys in the verbose response
    assert common_keys.issubset(
        response_verbose.json()['summary'].keys()), f"Missing keys in verbose response: {common_keys - response_verbose.json()['summary'].keys()}"
    assert verbose_keys.issubset(
        response_verbose.json().keys()), f"Missing verbose keys in response: {verbose_keys - response_verbose.json().keys()}"


def get_metadata_profile_dir():
    # Get the current directory
    current_directory = Path(__file__).resolve().parent
    # Navigate up 3 levels
    base_dir = current_directory.parents[2]  # (index 2 because it's zero-based)
    metadata_profile_dir = base_dir / 'manifests' / 'autotune' / 'metadata-profiles'

    return metadata_profile_dir


def delete_and_create_metadata_profile():
    metadata_profile_dir = get_metadata_profile_dir()

    metadata_profile_json_file = metadata_profile_dir / 'bulk_cluster_metadata_local_monitoring.json'
    json_data = json.load(open(metadata_profile_json_file))
    metadata_profile_name = json_data['metadata']['name']

    response = delete_metadata_profile(metadata_profile_name)
    print("delete metadata profile = ", response.status_code)

    # Create metadata profile using the specified json
    response = create_metadata_profile(metadata_profile_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_METADATA_PROFILE_SUCCESS_MSG % metadata_profile_name


def delete_and_create_metric_profile():
    metric_profile_dir = get_metric_profile_dir()

    metric_profile_json_file = metric_profile_dir / 'resource_optimization_local_monitoring.json'
    json_data = json.load(open(metric_profile_json_file))
    metric_profile_name = json_data['metadata']['name']

    response = delete_metric_profile(metric_profile_json_file)
    print("delete metric profile = ", response.status_code)

    # Create metric profile using the specified json
    response = create_metric_profile(metric_profile_json_file)

    data = response.json()
    print(data['message'])

    assert response.status_code == SUCCESS_STATUS_CODE
    assert data['status'] == SUCCESS_STATUS
    assert data['message'] == CREATE_METRIC_PROFILE_SUCCESS_MSG % metric_profile_name
