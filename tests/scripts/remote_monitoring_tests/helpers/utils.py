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
import subprocess
import time
from datetime import datetime, timedelta

SUCCESS_STATUS_CODE = 201
SUCCESS_200_STATUS_CODE = 200
ERROR_STATUS_CODE = 400
ERROR_409_STATUS_CODE = 409

SUCCESS_STATUS = "SUCCESS"
ERROR_STATUS = "ERROR"
UPDATE_RESULTS_SUCCESS_MSG = "Results added successfully! View saved results at /listExperiments."
UPDATE_RESULTS_DATE_PRECEDE_ERROR_MSG = "The Start time should precede the End time!"
UPDATE_RESULTS_INVALID_METRIC_VALUE_ERROR_MSG = "Performance profile: avg cannot be negative or blank for the metric variable: "
UPDATE_RESULTS_INVALID_METRIC_FORMAT_ERROR_MSG = "Performance profile:  Format value should be among these values: [cores, MiB]"
CREATE_EXP_SUCCESS_MSG = "Experiment registered successfully with Kruize. View registered experiments at /listExperiments"
CREATE_EXP_BULK_ERROR_MSG = "At present, the system does not support bulk entries!"
UPDATE_RECOMMENDATIONS_MANDATORY_DEFAULT_MESSAGE = 'experiment_name is mandatory'
UPDATE_RECOMMENDATIONS_MANDATORY_INTERVAL_END_DATE = 'interval_end_time is mandatory'
UPDATE_RECOMMENDATIONS_DATA_NOT_FOUND = 'Data not found!'
UPDATE_RECOMMENDATIONS_START_TIME_PRECEDE_END_TIME = 'The Start time should precede the End time!'
UPDATE_RECOMMENDATIONS_START_TIME_END_TIME_GAP_ERROR = 'The gap between the interval_start_time and interval_end_time must be within a maximum of 15 days!'
UPDATE_RECOMMENDATIONS_INVALID_DATE_TIME_FORMAT = "Given timestamp - \" %s \" is not a valid timestamp format"
RECOMMENDATIONS_AVAILABLE = "Recommendations Are Available"
COST_RECOMMENDATIONS_AVAILABLE = "Cost Recommendations Available"
PERFORMANCE_RECOMMENDATIONS_AVAILABLE = "Performance Recommendations Available"

# Kruize Recommendations Notification codes
NOTIFICATION_CODE_FOR_RECOMMENDATIONS_AVAILABLE = "111000"
NOTIFICATION_CODE_FOR_COST_RECOMMENDATIONS_AVAILABLE = "112101"
NOTIFICATION_CODE_FOR_PERFORMANCE_RECOMMENDATIONS_AVAILABLE = "112102"
NOTIFICATION_CODE_FOR_NOT_ENOUGH_DATA = "120001"
NOTIFICATION_CODE_FOR_CPU_RECORDS_ARE_IDLE = "323001"
NOTIFICATION_CODE_FOR_CPU_RECORDS_ARE_ZERO = "323002"
NOTIFICATION_CODE_FOR_CPU_RECORDS_NOT_AVAILABLE = "323003"
NOTIFICATION_CODE_FOR_MEMORY_RECORDS_ARE_ZERO = "324001"
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

CPU_REQUEST = "cpuRequest"
CPU_LIMIT = "cpuLimit"
CPU_USAGE = "cpuUsage"
CPU_THROTTLE = "cpuThrottle"

MEMORY_REQUEST = "memoryRequest"
MEMORY_LIMIT = "memoryLimit"
MEMORY_USAGE = "memoryUsage"
MEMORY_RSS = "memoryRSS"

NOT_ENOUGH_DATA_MSG = "There is not enough data available to generate a recommendation."
EXP_EXISTS_MSG = "Experiment name already exists: "
INVALID_DEPLOYMENT_TYPE_MSG = "Invalid deployment type: xyz"
INVALID_INTERVAL_DURATION_MSG = "Interval duration cannot be less than or greater than measurement_duration by more than 30 seconds"

time_log_csv = "/tmp/time_log.csv"

# DURATION - No. of days * 24.0 hrs
SHORT_TERM_DURATION_IN_HRS_MAX = 1 * 24.0
MEDIUM_TERM_DURATION_IN_HRS_MAX = 7 * 24.0
LONG_TERM_DURATION_IN_HRS_MAX = 15 * 24.0

SHORT_TERM = "short_term"
MEDIUM_TERM = "medium_term"
LONG_TERM = "long_term"

# version,experiment_name,cluster_name,performance_profile,mode,target_cluster,type,name,namespace,container_image_name,container_name,measurement_duration,threshold
create_exp_test_data = {
    "version": "v2.0",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db",
    "cluster_name": "cluster-one-division-bell",
    "performance_profile": "resource-optimization-openshift",
    "mode": "monitor",
    "target_cluster": "remote",
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

test_type = {"blank": "", "null": "null", "invalid": "xyz"}


def generate_test_data(csvfile, test_data):
    if os.path.isfile(csvfile):
        os.remove(csvfile)
    with open(csvfile, 'a') as f:
        writer = csv.writer(f)

        for key in test_data:
            for t in test_type:
                data = []

                test_name = t + "_" + key
                status_code = 400

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

    # Validate kubernetes objects
    if update_results_json is not None and len(update_results_json) > 0:
        length = len(update_results_json[0]["kubernetes_objects"])
        for i in range(length):
            update_results_kubernetes_obj = update_results_json[0]["kubernetes_objects"][i]
            create_exp_kubernetes_obj = create_exp_json["kubernetes_objects"][i]
            list_reco_kubernetes_obj = list_reco_json["kubernetes_objects"][i]
            validate_kubernetes_obj(create_exp_kubernetes_obj, update_results_kubernetes_obj, update_results_json, \
                                    list_reco_kubernetes_obj, expected_duration_in_hours, test_name)
    else:
        update_results_kubernetes_obj = None
        create_exp_kubernetes_obj = create_exp_json["kubernetes_objects"][0]
        list_reco_kubernetes_obj = list_reco_json["kubernetes_objects"][0]
        validate_kubernetes_obj(create_exp_kubernetes_obj, update_results_kubernetes_obj, update_results_json, \
                                list_reco_kubernetes_obj, expected_duration_in_hours, test_name)


def validate_kubernetes_obj(create_exp_kubernetes_obj, update_results_kubernetes_obj, update_results_json,
                            list_reco_kubernetes_obj, expected_duration_in_hours, test_name):
    # Validate type, name, namespace
    if update_results_kubernetes_obj == None:
        assert list_reco_kubernetes_obj["type"] == create_exp_kubernetes_obj["type"]
        assert list_reco_kubernetes_obj["name"] == create_exp_kubernetes_obj["name"]
        assert list_reco_kubernetes_obj["namespace"] == create_exp_kubernetes_obj["namespace"]

        exp_containers_length = len(create_exp_kubernetes_obj["containers"])
        list_reco_containers_length = len(list_reco_kubernetes_obj["containers"])

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
                                   expected_duration_in_hours, test_name)


def validate_container(update_results_container, update_results_json, list_reco_container, expected_duration_in_hours, test_name):
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

                duration_terms = ["short_term", "medium_term", "long_term"]
                for term in duration_terms:
                    if check_if_recommendations_are_present(terms_obj[term]):
                        print(f"reco present for term {term}")
                        # Validate timestamps [deprecated as monitoring end time is moved to higher level]
                        # assert cost_obj[term]["monitoring_end_time"] == interval_end_time, \
                        #    f"monitoring end time {cost_obj[term]['monitoring_end_time']} did not match end timestamp {interval_end_time}"

                        monitoring_start_time = term_based_start_time(interval_end_time, term)
                        assert terms_obj[term]["monitoring_start_time"] == monitoring_start_time, \
                            f"actual = {terms_obj[term]['monitoring_start_time']} expected = {monitoring_start_time}"

                        # Validate duration in hrs
                        if expected_duration_in_hours == None:
                            diff = time_diff_in_hours(interval_start_time, interval_end_time)
                            print(f"difference in hours = {diff}")
                            duration_in_hours += diff
                            print(f"duration in hours = {duration_in_hours}")

                            if term == "short_term" and duration_in_hours > SHORT_TERM_DURATION_IN_HRS_MAX:
                                duration_in_hours = SHORT_TERM_DURATION_IN_HRS_MAX
                            elif term == "medium_term" and duration_in_hours > MEDIUM_TERM_DURATION_IN_HRS_MAX:
                                duration_in_hours = MEDIUM_TERM_DURATION_IN_HRS_MAX
                            elif term == "long_term" and duration_in_hours > LONG_TERM_DURATION_IN_HRS_MAX:
                                duration_in_hours = LONG_TERM_DURATION_IN_HRS_MAX

                        print(
                            f"Actual = {terms_obj[term]['duration_in_hours']} expected = {duration_in_hours}")
                        assert terms_obj[term]["duration_in_hours"] == duration_in_hours, \
                            f"Duration in hours did not match! Actual = {terms_obj[term]['duration_in_hours']} expected = {duration_in_hours}"

                        # Get engine objects
                        engines_list = ["cost", "performance"]

                        # Extract recommendation engine objects
                        recommendation_engines_object = None
                        if "recommendation_engines" in terms_obj[term]:
                            recommendation_engines_object = terms_obj[term]["recommendation_engines"]
                        if None != recommendation_engines_object:
                            for engine_entry in engines_list:
                                if engine_entry in terms_obj[term]["recommendation_engines"]:
                                    engine_obj = terms_obj[term]["recommendation_engines"][engine_entry]
                                    validate_config(engine_obj["config"], metrics)
                                    validate_variation(current_config, engine_obj["config"], engine_obj["variation"])

            else:
                data = list_reco_container["recommendations"]["data"]
                assert len(data) == 0, f"Data is not empty! Length of data - Actual = {len(data)} expected = 0"

    else:
        print("Checking for recommendation notifications message...")
        result = check_if_recommendations_are_present(list_reco_container["recommendations"])
        assert result == False, f"Recommendations notifications does not contain the expected message - {NOT_ENOUGH_DATA_MSG}"


def validate_config(reco_config, metrics):
    cpu_format_type = ""
    memory_format_type = ""

    for metric in metrics:
        if "cpuUsage" == metric["name"]:
            cpu_format_type = metric['results']['aggregation_info']['format']

        if "memoryUsage" == metric["name"]:
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
    command = f"kubectl get pod -n {namespace} | grep kruize | grep -v kruize-ui | cut -d ' ' -f1"
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
