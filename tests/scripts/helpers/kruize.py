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

import json
import requests
import subprocess


def get_kruize_url():
    return URL


def form_kruize_url(cluster_type, SERVER_IP=None):
    global URL

    if SERVER_IP != None:
        URL = "http://" + str(SERVER_IP)
        print("\nKRUIZE AUTOTUNE URL = ", URL)
        return

    if (cluster_type == "local"):
        AUTOTUNE_PORT = 8080
        SERVER_IP = '127.0.0.1'
        URL = "http://" + str(SERVER_IP) + ":" + str(AUTOTUNE_PORT)
    if (cluster_type == "minikube"):
        port = subprocess.run(
            ['kubectl -n monitoring get svc kruize --no-headers -o=custom-columns=PORT:.spec.ports[*].nodePort'],
            shell=True, stdout=subprocess.PIPE)

        AUTOTUNE_PORT = port.stdout.decode('utf-8').strip('\n')

        ip = subprocess.run(['minikube ip'], shell=True, stdout=subprocess.PIPE)
        SERVER_IP = ip.stdout.decode('utf-8').strip('\n')
        URL = "http://" + str(SERVER_IP) + ":" + str(AUTOTUNE_PORT)

    elif (cluster_type == "openshift"):

        subprocess.run(['oc expose svc/kruize -n openshift-tuning'], shell=True, stdout=subprocess.PIPE)
        ip = subprocess.run(
            [
                'oc status -n openshift-tuning | grep "kruize" | grep -v "kruize-ui" | grep -v "kruize-db" | grep port | cut -d " " -f1 | cut -d "/" -f3'],
            shell=True,
            stdout=subprocess.PIPE)
        SERVER_IP = ip.stdout.decode('utf-8').strip('\n')
        print("IP = ", SERVER_IP)
        URL = "http://" + str(SERVER_IP)

    print("\nKRUIZE AUTOTUNE URL = ", URL)


# Description: This function validates the input json and posts the experiment using createExperiment API to Kruize Autotune
# Input Parameters: experiment input json
def create_experiment(input_json_file, invalid_header=False):
    json_file = open(input_json_file, "r")
    input_json = json.loads(json_file.read())
    print("\n************************************************************")
    pretty_json_str = json.dumps(input_json, indent=4)
    print(pretty_json_str)
    print("\n************************************************************")

    # read the json
    print("\nCreating the experiment...")

    url = URL + "/createExperiment"
    print("URL = ", url)

    headers = {'content-type': 'application/xml'}
    if invalid_header:
        print("Invalid header")
        response = requests.post(url, json=input_json, headers=headers)
    else:
        response = requests.post(url, json=input_json)

    print("Response status code = ", response.status_code)
    try:
        # Parse the response content as JSON into a Python dictionary
        response_json = response.json()

        # Check if the response_json is a valid JSON object or array
        if isinstance(response_json, (dict, list)):
            # Convert the response_json back to a JSON-formatted string with double quotes and pretty print it
            pretty_response_json_str = json.dumps(response_json, indent=4)

            # Print the JSON string
            print(pretty_response_json_str)
        else:
            print("Invalid JSON format in the response.")
            print(response.text)  # Print the response text as-is
    except json.JSONDecodeError:
        print("Response content is not valid JSON.")
        print(response.text)  # Print the response text as-is
    return response


# Description: This function validates the result json and posts the experiment results using updateResults API to Kruize Autotune
# Input Parameters: experiment input json
def update_results(result_json_file, logging=True):
    # read the json
    json_file = open(result_json_file, "r")
    result_json = json.loads(json_file.read())
    if logging:
        print("\n************************************************************")
        print(result_json)
        print("\n************************************************************")

    print("\nUpdating the results...")
    url = URL + "/updateResults"
    print("URL = ", url)

    response = requests.post(url, json=result_json)
    print("Response status code = ", response.status_code)
    print(response.text)
    return response


# Description: This function generates recommendation for the given experiment_name , start time and end time .
def update_recommendations(experiment_name, startTime, endTime):
    print("\n************************************************************")
    print("\nUpdating the recommendation \n for %s for dates Start-time: %s and End-time: %s..." % (
        experiment_name, startTime, endTime))
    queryString = "?"
    if experiment_name:
        queryString = queryString + "&experiment_name=%s" % (experiment_name)
    if endTime:
        queryString = queryString + "&interval_end_time=%s" % (endTime)
    if startTime:
        queryString = queryString + "&interval_start_time=%s" % (startTime)

    url = URL + "/updateRecommendations?%s" % (queryString)
    print("URL = ", url)
    response = requests.post(url, )
    print("Response status code = ", response.status_code)
    print(response.text)
    print("\n************************************************************")
    return response


# Description: This function obtains the recommendations from Kruize Autotune using listRecommendations API
# Input Parameters: experiment name, flag indicating latest result and monitoring end time
def list_recommendations(experiment_name=None, latest=None, monitoring_end_time=None, rm=False):
    PARAMS = ""
    print("\nListing the recommendations...")
    url = URL + "/listRecommendations"
    if rm:
        url += "?rm=true"
    print("URL = ", url)

    if experiment_name == None:
        if latest == None and monitoring_end_time == None:
            response = requests.get(url)
        elif latest != None:
            PARAMS = {'latest': latest}
        elif monitoring_end_time != None:
            PARAMS = {'monitoring_end_time': monitoring_end_time}
    else:
        if latest == None and monitoring_end_time == None:
            PARAMS = {'experiment_name': experiment_name}
        elif latest != None:
            PARAMS = {'experiment_name': experiment_name, 'latest': latest}
        elif monitoring_end_time != None:
            PARAMS = {'experiment_name': experiment_name, 'monitoring_end_time': monitoring_end_time}

    print("PARAMS = ", PARAMS)
    response = requests.get(url=url, params=PARAMS)

    print("Response status code = ", response.status_code)
    print("\n************************************************************")
    print(response.text)
    print("\n************************************************************")
    return response


# Description: This function deletes the experiment and posts the experiment using createExperiment API to Kruize Autotune
# Input Parameters: experiment input json
def delete_experiment(input_json_file, rm=True):
    json_file = open(input_json_file, "r")
    input_json = json.loads(json_file.read())

    print("\nDeleting the experiment...")
    url = URL + "/createExperiment"
    PARAMS = {'rm': True}

    print("URL = ", url)

    experiment_name = input_json[0]['experiment_name']

    delete_json = [{
        "experiment_name": experiment_name
    }]

    headers = {'content-type': 'application/xml'}
    if invalid_header:
        print("Invalid header")
        response = requests.delete(url, json=delete_json, headers=headers, PARAMS=PARAMS)
    else:
        response = requests.delete(url, json=delete_json, PARAMS=PARAMS)

    print(response)
    print("Response status code = ", response.status_code)
    return response


def delete_experiment_local(input_json_file):
    json_file = open(input_json_file, "r")
    input_json = json.loads(json_file.read())

    print("\nDeleting the experiment...")
    url = URL + "/createExperiment"
    print("URL = ", url)

    experiment_name = input_json[0]['experiment_name']

    delete_json = [{
        "experiment_name": experiment_name
    }]

    headers = {'content-type': 'application/xml'}
    if invalid_header:
        print("Invalid header")
        response = requests.delete(url, json=delete_json, headers=headers, PARAMS=False)
    else:
        response = requests.delete(url, json=delete_json, PARAMS=False)

    print(response)
    print("Response status code = ", response.status_code)
    return response


# Description: This function creates a performance profile using the Kruize createPerformanceProfile API
# Input Parameters: performance profile json
def create_performance_profile(perf_profile_json_file):
    json_file = open(perf_profile_json_file, "r")
    perf_profile_json = json.loads(json_file.read())

    print("\nCreating performance profile...")
    url = URL + "/createPerformanceProfile"
    print("URL = ", url)

    response = requests.post(url, json=perf_profile_json)
    print("Response status code = ", response.status_code)
    print(response.text)
    return response


# Description: This function obtains the experiments from Kruize Autotune using listExperiments API
# Input Parameters: None
def list_experiments(results=None, recommendations=None, latest=None, experiment_name=None, rm=False):
    print("\nListing the experiments...")
    query_params = {}

    if experiment_name is not None:
        query_params['experiment_name'] = experiment_name
    if latest is not None:
        query_params['latest'] = latest
    if results is not None:
        query_params['results'] = results
    if recommendations is not None:
        query_params['recommendations'] = recommendations

    query_string = "&".join(f"{key}={value}" for key, value in query_params.items())

    url = URL + "/listExperiments"
    if rm:
        url += "?rm=true"
        if query_string:
            url += "&" + query_string
    else:
        url += "?" + query_string
    print("URL = ", url)
    response = requests.get(url)
    print("Response status code = ", response.status_code)
    return response


# Description: This function obtains the list of datasources from Kruize Autotune using datasources API
# Input Parameters: None
def list_datasources(name=None):
    print("\nListing the datasources...")
    query_params = {}

    if name is not None:
        query_params['name'] = name

    query_string = "&".join(f"{key}={value}" for key, value in query_params.items())

    url = URL + "/datasources"
    if query_string:
        url += "?" + query_string
    print("URL = ", url)
    response = requests.get(url)

    print("PARAMS = ", query_params)
    print("Response status code = ", response.status_code)
    print("\n************************************************************")
    print(response.text)
    print("\n************************************************************")
    return response


# Description: This function validates the input json and imports metadata using POST dsmetadata API to Kruize Autotune
# Input Parameters: datasource input json
def import_metadata(input_json_file, invalid_header=False):
    json_file = open(input_json_file, "r")
    input_json = json.loads(json_file.read())
    print("\n************************************************************")
    pretty_json_str = json.dumps(input_json, indent=4)
    print(pretty_json_str)
    print("\n************************************************************")

    # read the json
    print("\nImporting the metadata...")

    url = URL + "/dsmetadata"
    print("URL = ", url)

    headers = {'content-type': 'application/xml'}
    if invalid_header:
        print("Invalid header")
        response = requests.post(url, json=input_json, headers=headers)
    else:
        response = requests.post(url, json=input_json)

    print("Response status code = ", response.status_code)
    try:
        # Parse the response content as JSON into a Python dictionary
        response_json = response.json()

        # Check if the response_json is a valid JSON object or array
        if isinstance(response_json, (dict, list)):
            # Convert the response_json back to a JSON-formatted string with double quotes and pretty print it
            pretty_response_json_str = json.dumps(response_json, indent=4)

            # Print the JSON string
            print(pretty_response_json_str)
        else:
            print("Invalid JSON format in the response.")
            print(response.text)  # Print the response text as-is
    except json.JSONDecodeError:
        print("Response content is not valid JSON.")
        print(response.text)  # Print the response text as-is
    return response


# Description: This function deletes the metadata and posts the metadata using dsmetadata API to Kruize Autotune
# Input Parameters: datasource input json
def delete_metadata(input_json_file, invalid_header=False):
    json_file = open(input_json_file, "r")
    input_json = json.loads(json_file.read())

    print("\nDeleting the metadata...")

    url = URL + "/dsmetadata"
    print("URL = ", url)

    headers = {'content-type': 'application/xml'}
    if invalid_header:
        print("Invalid header")
        response = requests.delete(url, json=input_json, headers=headers)
    else:
        response = requests.delete(url, json=input_json)

    print(response)
    print("Response status code = ", response.status_code)
    return response


# Description: This function obtains the metadata from Kruize Autotune using GET dsmetadata API
# Input Parameters: datasource name, cluster name, namespace, verbose - flag indicating granularity of data to be listed
def list_metadata(datasource=None, cluster_name=None, namespace=None, verbose=None, logging=True):
    print("\nListing the metadata...")

    query_params = {}

    if datasource is not None:
        query_params['datasource'] = datasource
        if cluster_name is not None:
            query_params['cluster_name'] = cluster_name
        if namespace is not None:
            query_params['namespace'] = namespace
        if verbose is not None:
            query_params['verbose'] = verbose

    query_string = "&".join(f"{key}={value}" for key, value in query_params.items())

    url = URL + "/dsmetadata"
    if query_string:
        url += "?" + query_string
    print("URL = ", url)
    print("PARAMS = ", query_params)
    response = requests.get(url)

    print("Response status code = ", response.status_code)
    if logging:
        print("\n************************************************************")
        print(response.text)
        print("\n************************************************************")
    return response


# Description: This function creates a metric profile using the Kruize createMetricProfile API
# Input Parameters: metric profile json
def create_metric_profile(metric_profile_json_file):
    json_file = open(metric_profile_json_file, "r")
    metric_profile_json = json.loads(json_file.read())

    print("\nCreating metric profile...")
    url = URL + "/createMetricProfile"
    print("URL = ", url)

    response = requests.post(url, json=metric_profile_json)
    print("Response status code = ", response.status_code)
    print(response.text)
    return response


# Description: This function deletes the metric profile
# Input Parameters: metric profile input json
def delete_metric_profile(input_json_file, invalid_header=False):
    json_file = open(input_json_file, "r")
    input_json = json.loads(json_file.read())

    print("\nDeleting the metric profile...")
    url = URL + "/deleteMetricProfile"

    metric_profile_name = input_json['metadata']['name']
    query_string = f"name={metric_profile_name}"

    if query_string:
        url += "?" + query_string
    print("URL = ", url)

    headers = {'content-type': 'application/xml'}
    if invalid_header:
        print("Invalid header")
        response = requests.delete(url, headers=headers)
    else:
        response = requests.delete(url)

    print(response)
    print("Response status code = ", response.status_code)
    return response


# Description: This function lists the metric profile from Kruize Autotune using GET listMetricProfiles API
# Input Parameters: metric profile name and verbose - flag indicating granularity of data to be listed
def list_metric_profiles(name=None, verbose=None, logging=True):
    print("\nListing the metric profiles...")

    query_params = {}

    if name is not None:
        query_params['name'] = name
    if verbose is not None:
        query_params['verbose'] = verbose

    query_string = "&".join(f"{key}={value}" for key, value in query_params.items())

    url = URL + "/listMetricProfiles"
    if query_string:
        url += "?" + query_string
    print("URL = ", url)
    print("PARAMS = ", query_params)
    response = requests.get(url)

    print("Response status code = ", response.status_code)
    if logging:
        print("\n************************************************************")
        print(response.text)
        print("\n************************************************************")
    return response


# Description: This function generates recommendation for the given experiment_name
def generate_recommendations(experiment_name):
    print("\n************************************************************")
    print("\nGenerating the recommendation \n for %s..." % (
        experiment_name))
    queryString = "?"
    if experiment_name:
        queryString = queryString + "experiment_name=%s" % (experiment_name)

    url = URL + "/generateRecommendations%s" % (queryString)
    print("URL = ", url)
    response = requests.post(url, )
    print("Response status code = ", response.status_code)
    print(response.text)
    print("\n************************************************************")
    return response


def post_bulk_api(input_json_file):
    print("\n************************************************************")
    print("Sending POST request to URL: ", f"{URL}/bulk")
    print("Request Payload: ", input_json_file)
    curl_command = f"curl -X POST {URL}/bulk -H 'Content-Type: application/json' -d '{json.dumps(input_json_file)}'"
    print("Equivalent cURL command: ", curl_command)

    # Send the POST request
    response = requests.post(f"{URL}/bulk", json=input_json_file)
    print("Response Status Code: ", response.status_code)
    print("Response JSON: ", response.json())
    return response


def get_bulk_job_status(job_id, verbose=False):
    print("\n************************************************************")
    url_basic = f"{URL}/bulk?job_id={job_id}"
    url_verbose = f"{URL}/bulk?job_id={job_id}&verbose={verbose}"
    getJobIDURL = url_basic
    if verbose:
        getJobIDURL = url_verbose
    print("Sending GET request to URL ( verbose=", verbose, " ): ", getJobIDURL)
    curl_command_verbose = f"curl -X GET '{getJobIDURL}'"
    print("Equivalent cURL command : ", curl_command_verbose)
    response = requests.get(url_verbose)

    print("Verbose GET Response Status Code: ", response.status_code)
    print("Verbose GET Response JSON: ", response.json())
    return response
