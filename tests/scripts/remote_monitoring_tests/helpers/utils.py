
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
ERROR_409_STATUS_CODE = 409

SUCCESS_STATUS = "SUCCESS"
ERROR_STATUS = "ERROR"
UPDATE_RESULTS_SUCCESS_MSG = "Results added successfully! View saved results at /listExperiments."
CREATE_EXP_SUCCESS_MSG = "Experiment registered successfully with Kruize. View registered experiments at /listExperiments"

NOT_ENOUGH_DATA_MSG = "There is not enough data available to generate a recommendation."
EXP_EXISTS_MSG = "Experiment name already exists: "
INVALID_DEPLOYMENT_TYPE_MSG = "Invalid deployment type: xyz"
INVALID_INTERVAL_DURATION_MSG = "Interval duration cannot be less than or greater than measurement_duration by more than 30 seconds"

createExperimentCount_STATUS = "Metric Category: API, Name: createExperiment_count, Value: 10.0"
createExperimentSum_STATUS = "Metric Category: API, Name: createExperiment_sum, Value: 0.228229311"
listRecommendationsCount_STATUS = "Metric Category: API, Name: listRecommendations_count, Value: 11.0"
listRecommendationsSum_STATUS = "Metric Category: API, Name: listRecommendations_sum, Value: 0.349422898"
listExperimentsCount_STATUS = "Metric Category: API, Name: listExperiments_count, Value: 0.0"
listExperimentsSum_STATUS = "Metric Category: API, Name: listExperiments_sum, Value: 0.0"
updateResultsCount_STATUS = "Metric Category: API, Name: updateResults_count, Value: 1000.0"
updateResultsSum_STATUS = "Metric Category: API, Name: updateResults_sum, Value: 51.24196982"
addRecommendationToDBCount_STATUS = "Metric Category: DB, Name: addRecommendationToDB_count, Value: 50.0"
addRecommendationToDBSum_STATUS = "Metric Category: DB, Name: addRecommendationToDB_sum, Value: 0.11283727"
addResultsToDBCount_STATUS = "Metric Category: DB, Name: addResultsToDB_count, Value: 1000.0"
addResultsToDBSum_STATUS = "Metric Category: DB, Name: addResultsToDB_sum, Value: 3.513978451"
loadAllRecommendationsCount_STATUS = "Metric Category: DB, Name: loadAllRecommendations_count, Value: 0.0"
loadAllRecommendationsSum_STATUS = "Metric Category: DB, Name: loadAllRecommendations_sum, Value: 0.0"
loadAllExperimentsCount_STATUS = "Metric Category: DB, Name: loadAllExperiments_count, Value: 0.0"
loadAllExperimentsSum_STATUS = "Metric Category: DB, Name: loadAllExperiments_sum, Value: 0.0"
addExperimentToDBCount_STATUS = "Metric Category: DB, Name: addExperimentToDB_count, Value: 10.0"
addExperimentToDBSum_STATUS = "Metric Category: DB, Name: addExperimentToDB_sum, Value: 0.132267294"
loadResultsByExperimentNameCount_STATUS = "Metric Category: DB, Name: loadResultsByExperimentName_count, Value: 1000.0"
loadResultsByExperimentNameSum_STATUS = "Metric Category: DB, Name: loadResultsByExperimentName_sum, Value: 16.355101603"
loadExperimentByNameCount_STATUS = "Metric Category: DB, Name: loadExperimentByName_count, Value: 1031.0"
loadExperimentByNameSum_STATUS = "Metric Category: DB, Name: loadExperimentByName_sum, Value: 1.665818586"
loadAllResultsCount_STATUS = "Metric Category: DB, Name: loadAllResults_count, Value: 0.0"
loadAllResultsSum_STATUS = "Metric Category: DB, Name: loadAllResults_sum, Value: 0.0"
loadRecommendationsByExperimentNameCount_STATUS = "Metric Category: DB, Name: loadRecommendationsByExperimentName_count, Value: 11.0"
loadRecommendationsByExperimentNameSum_STATUS = "Metric Category: DB, Name: loadRecommendationsByExperimentName_sum, Value: 0.090203608"

time_log_csv = "/tmp/time_log.csv"

# DURATION - No. of days * 24.0 hrs
SHORT_TERM_DURATION_IN_HRS_MAX = 1 * 24.0
MEDIUM_TERM_DURATION_IN_HRS_MAX = 7 * 24.0
LONG_TERM_DURATION_IN_HRS_MAX = 15 * 24.0

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

# version, experiment_name,interval_start_time,interval_end_time,type,name,namespace,container_image_name,container_name,cpuRequest_name,cpuRequest_sum,cpuRequest_avg,cpuRequest_format,cpuLimit_name,cpuLimit_sum,cpuLimit_avg,cpuLimit_format,cpuUsage_name,cpuUsage_sum,cpuUsage_max,cpuUsage_avg,cpuUsage_min,cpuUsage_format,cpuThrottle_name,cpuThrottle_sum,cpuThrottle_max,cpuThrottle_avg,cpuThrottle_format,memoryRequest_name,memoryRequest_sum,memoryRequest_avg,memoryRequest_format,memoryLimit_name,memoryLimit_sum,memoryLimit_avg,memoryLimit_format,memoryUsage_name,memoryUsage_sum,memoryUsage_max,memoryUsage_avg,memUsage_min,memoryUsage_format,memoryRSS_name,memoryRSS_sum,memoryRSS_max,memoryRSS_avg,memoryRSS_min,memoryRSS_format
update_results_test_data = {
        "version": "\"1.0\"",
        "experiment_name": "\"quarkus-resteasy-kruize-min-http-response-time-db\"",
        "interval_start_time": "\"2022-01-23T18:25:43.511Z\"",
        "interval_end_time": "\"2022-01-23T18:40:43.511Z\"",
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

def get_num_lines_in_csv(csv_filename):
    reader = csv.reader(open(csv_filename))
    num_lines= len(list(reader))
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

        find = "2022-01-23T18:40:43.570Z"
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

def increment_timestamp_by_given_mins(input_timestamp, minutes):
    input_date = datetime.strptime(input_timestamp, "%Y-%m-%dT%H:%M:%S.%fZ")
    output_date = input_date + timedelta(minutes=minutes)
    timestamp = output_date.strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3]+'Z'

    return timestamp

def get_datetime():
    my_datetime = datetime.today()
    time_str = my_datetime.isoformat(timespec = 'milliseconds')
    time_str = time_str + "Z"
    return time_str

def term_based_start_time(input_date_str, term):
    duration = {"short_term": 1, "medium_term": 7, "long_term": 15}
    input_date = datetime.strptime(input_date_str, "%Y-%m-%dT%H:%M:%S.%fZ")

    output_date = input_date - timedelta(days=duration[term])
    output_date_str = output_date.strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3]+'Z'

    return output_date_str

def validate_reco_json(create_exp_json, update_results_json, list_reco_json, expected_duration_in_hours = None, test_name = None):

    # Validate experiment
    assert create_exp_json["version"] == list_reco_json["version"]
    assert create_exp_json["experiment_name"] == list_reco_json["experiment_name"]
    assert create_exp_json["cluster_name"] == list_reco_json["cluster_name"]

    # Validate kubernetes objects
    if update_results_json != None:
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

def validate_kubernetes_obj(create_exp_kubernetes_obj, update_results_kubernetes_obj, update_results_json, list_reco_kubernetes_obj, expected_duration_in_hours, test_name):

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
            if list_reco_kubernetes_obj["containers"][j]["container_name"] == create_exp_kubernetes_obj["containers"][i]["container_name"]:
                update_results_container = create_exp_kubernetes_obj["containers"][i]
                list_reco_container = list_reco_kubernetes_obj["containers"][j]
                validate_container(update_results_container, update_results_json, list_reco_container, expected_duration_in_hours)

def validate_container(update_results_container, update_results_json, list_reco_container, expected_duration_in_hours):
    # Validate container image name and container name
    if update_results_container != None and list_reco_container != None:
        assert list_reco_container["container_image_name"] == update_results_container["container_image_name"], \
            f"Container image names did not match! Actual -  {list_reco_container['container_image_name']} Expected - {update_results_container['container_image_name']}"

        assert list_reco_container["container_name"] == update_results_container["container_name"],\
            f"Container names did not match! Acutal = {list_reco_container['container_name']} Expected - {update_results_container['container_name']}"

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
            if check_if_recommendations_are_present(list_reco_container["recommendations"]):
                duration_based_obj = list_reco_container["recommendations"]["data"][interval_end_time]["duration_based"]

                duration_terms = ["short_term", "medium_term", "long_term"]
                for term in duration_terms:
                    if check_if_recommendations_are_present(duration_based_obj[term]):
                        # Validate timestamps
                        assert duration_based_obj[term]["monitoring_end_time"] == interval_end_time,\
                            f"monitoring end time {duration_based_obj[term]['monitoring_end_time']} did not match end timestamp {interval_end_time}"

                        monitoring_start_time = term_based_start_time(interval_end_time, term)
                        assert duration_based_obj[term]["monitoring_start_time"] == monitoring_start_time,\
                            f"actual = {duration_based_obj[term]['monitoring_start_time']} expected = {monitoring_start_time}"

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

                        print(f"Actual = {duration_based_obj[term]['duration_in_hours']} expected = {duration_in_hours}")
                        assert duration_based_obj[term]["duration_in_hours"] == duration_in_hours,\
                            f"Duration in hours did not match! Actual = {duration_based_obj[term]['duration_in_hours']} expected = {duration_in_hours}"
                        
                        # Validate recommendation config
                        validate_config(duration_based_obj[term]["config"])
            else:
                data = list_reco_container["recommendations"]["data"]
                assert len(data) == 0, f"Data is not empty! Length of data - Actual = {len(data)} expected = 0"

    else:
        print("Checking for recommendation notifications message...")
        result = check_if_recommendations_are_present(list_reco_container["recommendations"])
        assert result == False, f"Recommendations notifications does not contain the expected message - {NOT_ENOUGH_DATA_MSG}"

def validate_config(reco_config):
    usage_list = ["requests", "limits"]
    for usage in usage_list:
        assert reco_config[usage]["cpu"]["amount"] > 0, f"cpu amount in recommendation config is {reco_config[usage]['cpu']['amount']}"
        assert reco_config[usage]["cpu"]["format"] == "cores", f"cpu format in recommendation config is {reco_config[usage]['cpu']['format']}"
        assert reco_config[usage]["memory"]["amount"] > 0, f"cpu amount in recommendation config is {reco_config[usage]['memory']['amount']}"
        assert reco_config[usage]["memory"]["format"] == "MiB", f"memory format in recommendation config is {reco_config[usage]['cpu']['format']}"

def check_if_recommendations_are_present(duration_based_obj):
    for notification in duration_based_obj["notifications"]:
        if notification["message"] == NOT_ENOUGH_DATA_MSG:
            return False
    return True

def time_diff_in_hours(interval_start_time, interval_end_time):
    start_date = datetime.strptime(interval_start_time, "%Y-%m-%dT%H:%M:%S.%fZ")
    end_date = datetime.strptime(interval_end_time, "%Y-%m-%dT%H:%M:%S.%fZ")
    diff = end_date - start_date
    return round(diff.total_seconds() / 3600, 2)

def match_metrics(output_metrics):
    api_pattern = re.compile(r'kruizeAPI_seconds_(count|sum){api="(\w+)",application="Kruize",method="(\w+)",}\s+([\d.]+)')
    db_pattern = re.compile(r'kruizeDB_seconds_(count|sum){application="Kruize",method="(\w+)",}\s+([\d.]+)')
    output_metrics = output_metrics.content.decode('utf-8')
    api_matches = api_pattern.findall(output_metrics)
    db_matches = db_pattern.findall(output_metrics)
    match_metrics = []
    for match in api_matches:
        metric_type = match[0]
        api_name = match[1]
        http_method = match[2]
        value = float(match[3])
        match_metrics.append(('API', metric_type, api_name, http_method, value))

    for match in db_matches:
        metric_type = match[0]
        method = match[1]
        value = float(match[2])
        match_metrics.append(('DB', metric_type, method, None, value))

    metrics = []
    for metric in match_metrics:
        metric_category, metric_type, name, http_method, value = metric
        metrics_str = f"Metric Category: {metric_category}, Name: {name}_{metric_type}, Value: {value}"
        metrics.append(metrics_str)

    assertions = [
        ("createExperiment_count", createExperimentCount_STATUS, 0),
        ("createExperiment_sum", createExperimentSum_STATUS, 0.5),
        ("listRecommendations_count", listRecommendationsCount_STATUS, 0),
        ("listRecommendations_sum", listRecommendationsSum_STATUS, 0.5),
        ("listExperiments_count", listExperimentsCount_STATUS, 0),
        ("listExperiments_sum", listExperimentsSum_STATUS, 0),
        ("updateResults_count", updateResultsCount_STATUS, 0),
        ("updateResults_sum", updateResultsSum_STATUS, 30),
        ("addRecommendationToDB_count", addRecommendationToDBCount_STATUS, 0),
        ("addRecommendationToDB_sum", addRecommendationToDBSum_STATUS, 0.5),
        ("addResultsToDB_count", addResultsToDBCount_STATUS, 0),
        ("addResultsToDB_sum", addResultsToDBSum_STATUS, 5),
        ("loadAllRecommendations_count", loadAllRecommendationsCount_STATUS, 0),
        ("loadAllRecommendations_sum", loadAllRecommendationsSum_STATUS, 0),
        ("loadAllExperiments_count", loadAllExperimentsCount_STATUS, 0),
        ("loadAllExperiments_sum", loadAllExperimentsSum_STATUS, 0),
        ("addExperimentToDB_count", addExperimentToDBCount_STATUS, 0),
        ("addExperimentToDB_sum", addExperimentToDBSum_STATUS, 0.5),
        ("loadResultsByExperimentName_count", loadResultsByExperimentNameCount_STATUS, 0),
        ("loadResultsByExperimentName_sum", loadResultsByExperimentNameSum_STATUS, 15),
        ("loadExperimentByName_count", loadExperimentByNameCount_STATUS, 0),
        ("loadExperimentByName_sum", loadExperimentByNameSum_STATUS, 2),
        ("loadAllResults_count", loadAllResultsCount_STATUS, 0),
        ("loadAllResults_sum", loadAllResultsSum_STATUS, 0),
        ("loadRecommendationsByExperimentName_count", loadRecommendationsByExperimentNameCount_STATUS, 0),
        ("loadRecommendationsByExperimentName_sum", loadRecommendationsByExperimentNameSum_STATUS, 0.5)
    ]

    for assertion in assertions:
        variable_name, status_string, tolerance = assertion

        found_metric = None
        for metric in metrics:
            metric_info = metric.split(",")
            for info in metric_info:
                if "Name:" in info:
                    name_value = info.split(":")[-1].strip()
                    if name_value == variable_name:
                        found_metric = metric
                        break

        if found_metric is not None:
            metric_value = float(found_metric.split("Value:")[-1].strip())
            status_value = float(status_string.split("Value:")[-1].strip())
            assert abs(metric_value - status_value) <= tolerance, f"{variable_name} assertion failed. Expected: {status_value}, Actual: {metric_value}, Tolerance: {tolerance}"
