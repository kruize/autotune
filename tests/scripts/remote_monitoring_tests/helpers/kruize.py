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

from . json_validate import validate_exp_input_json
import subprocess
import requests
import json
import os
import time

def form_kruize_url(cluster_type):
    global URL
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
# Input Parameters: experiment input json
def create_experiment(input_json_file, invalid_header = False):

    json_file = open(input_json_file, "r")
    input_json = json.loads(json_file.read())
    print("\n************************************************************")
    print(input_json)
    print("\n************************************************************")

    # Validate the json
    # print("\nValidating the input json...")
    #  isInvalid = validate_exp_input_json(input_json)
    isInvalid = ""
    if isInvalid:
        print(isInvalid)
        print("Input Json is invalid")
        exit(1)
    else:
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

        print(response)
        print("Response status code = ", response.status_code)
        return response

# Description: This function validates the result json and posts the experiment results using updateResults API to Kruize Autotune
# Input Parameters: experiment input json
def update_results(result_json_file):

    # read the json
    json_file = open(result_json_file, "r")
    result_json = json.loads(json_file.read())
    print("\n************************************************************")
    print(result_json)
    print("\n************************************************************")

    # TO DO: Validate the result json

    print("\nUpdating the results...")
    url = URL + "/updateResults"
    print("URL = ", url)

    response = requests.post(url, json=result_json)
    print("Response status code = ", response.status_code)
    print(response.text)
    return response

# Description: This function obtains the recommendations from Kruize Autotune using listRecommendations API
# Input Parameters: experiment input json
def list_recommendations():

    print("\nListing the recommendations...")
    url = URL + "/listRecommendations"
    print("URL = ", url)

    response = requests.get(url)
    print("Response status code = ", response.status_code)


    print("\n************************************************************")
    print(response.text)
    print("\n************************************************************")
    return response

# Description: This function deletes the experiment and posts the experiment using createExperiment API to Kruize Autotune
# Input Parameters: experiment input json
def delete_experiment(input_json_file, invalid_header = False):

    json_file = open(input_json_file, "r")
    input_json = json.loads(json_file.read())

    # Validate the json
    print("\nValidating the input json...")
 #  isInvalid = validate_exp_input_json(input_json)
    isInvalid = ""
    if isInvalid:
        print(isInvalid)
        print("Input Json is invalid")
        exit(1)
    else:
        # read the json
        print("\nDeleting the experiment...")

        url = URL + "/createExperiment"
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


