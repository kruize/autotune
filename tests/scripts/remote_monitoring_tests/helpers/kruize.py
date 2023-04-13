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

import subprocess
import requests
import json
import os
import time
from helpers.utils import *

def form_kruize_url(cluster_type, SERVER_IP = None):
    global URL

    if SERVER_IP != None:
        URL = "http://" + str(SERVER_IP)
        print ("\nKRUIZE AUTOTUNE URL = ", URL)
        return

    if (cluster_type == "minikube"):
        port = subprocess.run(['kubectl -n monitoring get svc kruize --no-headers -o=custom-columns=PORT:.spec.ports[*].nodePort'], shell=True, stdout=subprocess.PIPE)

        AUTOTUNE_PORT=port.stdout.decode('utf-8').strip('\n')

        ip = subprocess.run(['minikube ip'], shell=True, stdout=subprocess.PIPE)
        SERVER_IP=ip.stdout.decode('utf-8').strip('\n')

    elif (cluster_type == "openshift"):
        port = subprocess.run(['kubectl -n openshift-tuning get svc kruize --no-headers -o=custom-columns=PORT:.spec.ports[*].nodePort'], shell=True, stdout=subprocess.PIPE)

        AUTOTUNE_PORT=port.stdout.decode('utf-8').strip('\n')
        print("PORT = ", AUTOTUNE_PORT)

        ip = subprocess.run(['kubectl get pods -l=app=kruize -o wide -n openshift-tuning -o=custom-columns=NODE:.spec.nodeName --no-headers'], shell=True, stdout=subprocess.PIPE)
        SERVER_IP=ip.stdout.decode('utf-8').strip('\n')
        print("IP = ", SERVER_IP)

    URL = "http://" + str(SERVER_IP) + ":" + str(AUTOTUNE_PORT)
    print ("\nKRUIZE AUTOTUNE URL = ", URL)


# Description: This function validates the input json and posts the experiment using createExperiment API to Kruize Autotune
# Input Parameters: experiment input json, flag to indicate if json data or file was sent, flag for turning off debug log
def create_experiment(input_json_file, debug_log = True, is_json_file = True, invalid_header = False):

    if is_json_file == True:
        json_file = open(input_json_file, "r")
        input_json = json.loads(json_file.read())
    else:
        input_json = input_json_file

    url = URL + "/createExperiment"
    if debug_log == True:
        print("\n************************************************************")
        print(input_json)
        print("\n************************************************************")

        # read the json
        print("\nCreating the experiment...")
    
        print("URL = ", url)
    
    headers = {'content-type': 'application/xml'}

    start_time = time.time()
    if invalid_header:
        print("Invalid header")
        response = requests.post(url, json=input_json, headers=headers)
    else:
        response = requests.post(url, json=input_json)
       
    end_time = time.time()
    elapsed_time = end_time - start_time
    print(f"createExperiment elapsed_time = {elapsed_time}")
    log_time_taken('createExperiment', elapsed_time)

    if debug_log == True:
        print(response)
        print("Response status code = ", response.status_code)
    return response

# Description: This function validates the result json and posts the experiment results using updateResults API to Kruize Autotune
# Input Parameters: experiment input json, flag to indicate if json data or file was sent, flag for turning off debug log
def update_results(result_json_file, debug_log = True, is_json_file = True):

    if is_json_file == True:
        # read the json file
        print("opening result json ")
        json_file = open(result_json_file, "r")
        print("loading result json...")
        result_json = json.loads(json_file.read())
        print("loading result json...done")
    else:
        result_json = result_json_file

    url = URL + "/updateResults"
    print("**** URL = ", url)
    if debug_log == True:
        print("\n************************************************************")
        print(result_json)
        print("\n************************************************************")

        print("\nUpdating the results...")
        print("URL = ", url)

    print("Posting request...")
    start_time = time.time()

    response = requests.post(url, json=result_json)

    end_time = time.time()
    elapsed_time = end_time - start_time
    print(f"updateResults elapsed_time = {elapsed_time}")
    log_time_taken('updateResults', elapsed_time)
    print("Posting request...done")

    if debug_log == True:
        print("Response status code = ", response.status_code)
        print(response.text)
    return response

# Description: This function obtains the recommendations from Kruize Autotune using listRecommendations API
# Input Parameters: experiment name, flag to indicate latest recommendation, monitoring end time, flag to turn off debug log
def list_recommendations(experiment_name = None, latest = None, monitoring_end_time = None, debug_log = True):
    PARAMS = ""

    url = URL + "/listRecommendations"
    if debug_log == True:
        print("\nListing the recommendations...")
        print("URL = ", url)

    if experiment_name == None:
        if latest == None and monitoring_end_time == None:
            response = requests.get(url)
        elif latest != None:
            PARAMS = {'latest' : latest}
        elif monitoring_end_time != None:
            PARAMS = {'monitoring_end_time' : monitoring_end_time}
    else:
        if latest == None and monitoring_end_time == None:
            PARAMS = {'experiment_name': experiment_name}
        elif latest != None:
            PARAMS = {'experiment_name': experiment_name, 'latest' : latest}
        elif monitoring_end_time != None:
            PARAMS = {'experiment_name': experiment_name, 'monitoring_end_time' : monitoring_end_time}
            
        if debug_log == True:
            print("PARAMS = ", PARAMS)

    start_time = time.time()
    response = requests.get(url = url, params = PARAMS)
    end_time = time.time()
    elapsed_time = end_time - start_time
    print(f"listRecommendations elapsed_time = {elapsed_time}")
    log_time_taken('listRecommendations', elapsed_time)

    if debug_log == True:
        print("Response status code = ", response.status_code)
        print("\n************************************************************")
        print(response.text)
        print("\n************************************************************")
    return response

# Description: This function deletes the experiment and posts the experiment using createExperiment API to Kruize Autotune
# Input Parameters: experiment input json
def delete_experiment(input_json_file, debug_log = True, invalid_header = False):

    json_file = open(input_json_file, "r")
    input_json = json.loads(json_file.read())
    url = URL + "/createExperiment"
    if debug_log == True:
        print("\nDeleting the experiment...")
        print("URL = ", url)

    experiment_name = input_json[0]['experiment_name']

    delete_json = [{
        "experiment_name": experiment_name
    }]

    headers = {'content-type': 'application/xml'}
    if invalid_header:
        print("Invalid header")
        response = requests.delete(url, json=delete_json, headers=headers)
    else:
        response = requests.delete(url, json=delete_json)
    
    if debug_log == True:
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

