"""
Copyright (c) 2023, 2023 Red Hat, IBM Corporation and others.

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

# This is a handy tool for developers to check scalability. By running the "quickTestScalability.py" script with the specified parameters,
#
# python3 quickTestScalability.py --cluster_type minikube --ip master-1.kruizevin.lab.psi.pnq2.redhat.com --port 31620 --name firstEXP --count 2,10 --measurement_mins=15 --move_mins=300
#
# above script creates default performance profile
# followed by two experiments with the names "firstEXP_1" and "firstEXP_2" are generated.
# Each experiment will have 10 results,
# and the start and end times for each result will be auto-incremented by 15 minutes.

# --cluster_type specify clsuter type [minikube|openshift]
# --ip specify kruize ip address
# --port specify kruize port
# --name name of the experiment
# --count comma seprated first part number of experiment , second part number of results under each experiment
#          if value is 2,10  then 2 experiments and 10 results for each
# --measurement_mins diff b/w endtime and starttime
# --move_mins move the interval minutes forward by this amount

import json
import datetime
import requests
import argparse
import sys

# create an ArgumentParser object
parser = argparse.ArgumentParser()

# add the named arguments
parser.add_argument('--cluster_type', type=str, help='enter cluster type')
parser.add_argument('--ip', type=str, help='enter  ip')
parser.add_argument('--port', type=int, help='enter port')
parser.add_argument('--name', type=str, help='enter experiment name')
parser.add_argument('--count', type=str, help='enter experiment_start_count,experiment_end_count,num_results to create separated by , ')
parser.add_argument('--measurement_mins', type=int, help='enter time diff b/w interval_start_time and interval_end_time')
parser.add_argument('--move_mins', type=int, help='move the interval minutes forward by this amount')

# parse the arguments from the command line
args = parser.parse_args()

if args.cluster_type == "minikube":
    createExpURL = 'http://%s:%s/createExperiment'%(args.ip,args.port)
    listRecURL = 'http://%s:%s/listRecommendations'%(args.ip,args.port)
    updateExpURL = 'http://%s:%s/updateResults'%(args.ip,args.port)
    createProfileURL = 'http://%s:%s/createPerformanceProfile'%(args.ip,args.port)
elif args.cluster_type == "openshift":
    createExpURL = 'http://%s/createExperiment'%(args.ip)
    listRecURL = 'http://%s/listRecommendations'%(args.ip)
    updateExpURL = 'http://%s/updateResults'%(args.ip)
    createProfileURL = 'http://%s/createPerformanceProfile'%(args.ip)
else:
    print("Unsupported cluster type")
    sys.exit(1)

expnameprfix = args.name
expstart = int(args.count.split(',')[0])
expend = int(args.count.split(',')[1])
rescount = int(args.count.split(',')[2])
measurement_mins = args.measurement_mins
move_mins = args.move_mins
headers = {
    'Content-Type': 'application/json'
}
timeout = (60, 60)

print(createExpURL)
print(updateExpURL)
print(listRecURL)
print(createProfileURL)
print("experiment_name : %s " %(expnameprfix))
print("Experiment start count : %s" %(expstart))
print("Experiment end count : %s" %(expend))
print("Number of results to create : %s" %(rescount))
print("measurement mins : %s" %(measurement_mins))
print("move minutes forward : %s" %(move_mins))

perf_profile_dir = get_metric_profile_dir()
perf_profile_json = perf_profile_dir / 'resource_optimization_openshift.json'

with open(perf_profile_json, "r") as f:
    profile_data = json.load(f)

profile_json_payload = json.dumps(profile_data)
# Send the request with the payload
response = requests.post(createProfileURL, data=profile_json_payload, headers=headers)
# Check the response
if response.status_code == 201:
    print('CreateProfile Request successful!')
else:
    print(f'CreateProfile Request failed with status code {response.status_code}: {response.text}')

create_exp_json = "../json_files/create_exp.json"
with open(create_exp_json, "r") as f:
    data = json.load(f)
    createdata = data[0]

update_results_json = "../json_files/update_results.json"
with open(update_results_json, "r") as f:
    data = json.load(f)
    resultsdata = data[0]

print(resultsdata)

# calculate the new interval start and end times
new_interval_start_time = datetime.datetime.strptime(resultsdata['interval_start_time'], '%Y-%m-%dT%H:%M:%S.%fZ') + datetime.timedelta(minutes=move_mins)
new_interval_end_time = datetime.datetime.strptime(resultsdata['interval_end_time'], '%Y-%m-%dT%H:%M:%S.%fZ') + datetime.timedelta(minutes=move_mins)

daynum=move_mins/24/60

for i in range(expstart, expend+1):
    try:
        # update the JSON data with the new interval times
        resultsdata['interval_start_time'] = new_interval_start_time.strftime('%Y-%m-%dT%H:%M:%S.%fZ')
        resultsdata['interval_end_time'] = new_interval_end_time.strftime('%Y-%m-%dT%H:%M:%S.%fZ')

        profile_json_payload = json.dumps(profile_data)
        # Send the request with the payload
        response = requests.post(createProfileURL, data=profile_json_payload, headers=headers)
        # Check the response
        if response.status_code == 201:
            print('CreateProfile Request successful!')
        else:
            print(f'CreateProfile Request failed with status code {response.status_code}: {response.text}')

        experiment_name = "%s_%s" %(expnameprfix, i)
        createdata['experiment_name'] = experiment_name
        create_json_payload = json.dumps([createdata])
        # Send the request with the payload
        response = requests.post(createExpURL, data=create_json_payload, headers=headers, timeout=timeout)
        # Check the response
        if response.status_code == 201:
            print('CreateExp Request successful!')
        else:
            print(f'CreateExp Request failed with status code {response.status_code}: {response.text}')

        resultsdata['experiment_name'] = experiment_name
        for j in range(rescount):
            try:
                profile_json_payload = json.dumps(profile_data)
                # Send the request with the payload
                response = requests.post(createProfileURL, data=profile_json_payload, headers=headers)
                # Check the response
                if response.status_code == 201:
                    print('CreateProfile Request successful!')
                #else:
                #    print(f'CreateProfile Request failed with status code {response.status_code}: {response.text}')

                # calculate the new interval start and end times
                interval_start_time = datetime.datetime.strptime(resultsdata['interval_end_time'],  '%Y-%m-%dT%H:%M:%S.%fZ')
                interval_end_time = datetime.datetime.strptime(resultsdata['interval_end_time'], '%Y-%m-%dT%H:%M:%S.%fZ') + datetime.timedelta(minutes=measurement_mins)

                print('Interval_start_time: %s, Interval_end_time: %s' %(interval_start_time, interval_end_time))

                # update the JSON data with the new interval times
                resultsdata['interval_start_time'] = interval_start_time.strftime('%Y-%m-%dT%H:%M:%S.%fZ')
                resultsdata['interval_end_time'] = interval_end_time.strftime('%Y-%m-%dT%H:%M:%S.%fZ')

                # Convert payload to JSON string
                results_json_payload = json.dumps([resultsdata])

                # Send the request with the payload
                response = requests.post(updateExpURL, data=results_json_payload, headers=headers, timeout=timeout)

                # Check the response
                if response.status_code == 201:
                    pass
                else:
                    print(f'UpdateResults Request failed with status code {response.status_code}: {response.text}')
            except requests.exceptions.Timeout:
                print('Timeout occurred while connecting to')
            except requests.exceptions.RequestException as e:
                print('An error occurred while connecting to', e)

            print('### Experiment: %s: Progress: %s/%s  %s/%s' %(experiment_name, i, expend, j, rescount))

        # Fetch recommendations
        print(experiment_name)
        print(listRecURL)
        response = requests.get(listRecURL + "?experiment_name=" + experiment_name)
        # Check the response
        if response.status_code == 200:
            print('ListRecommendations Request successful!')
        else:
            print(f'ListRecommendations Request failed with status code {response.status_code}: {response.text}')

    except requests.exceptions.Timeout:
        print('Timeout occurred while connecting to')
    except requests.exceptions.RequestException as e:
        print('An error occurred while connecting to', e)

    print('Request successful!  Completed: Day: %s for %s-%s  %s/%s'  %(daynum, expstart, expend, j, rescount))

