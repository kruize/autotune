import argparse
import copy
import datetime
import json
import time

import requests
import sys
sys.path.append("../../")

from helpers.utils import get_metric_profile_dir

def loadData():
    json_file = open("./json_files/create_exp.json", "r")
    createdata = json.loads(json_file.read())

    json_file = open("./json_files/results.json", "r")
    data = json.loads(json_file.read())

    performance_profile_dir = get_metric_profile_dir()
    profile_json_path = performance_profile_dir / 'resource_optimization_openshift.json'

    with open(profile_json_path, 'r', encoding='utf-8') as profile_file:
        profile_data = json.load(profile_file)

    return (data, createdata, profile_data)

def updateRecommendation(experiment_name, endDate):
    try:
        # Send the request with the payload
        payloadRecommendationURL = "%s?experiment_name=%s&interval_end_time=%s" % (
            updateRecommendationURL, experiment_name, endDate.strftime('%Y-%m-%dT%H:%M:%S.%fZ')[:-4] + 'Z')
        response = requests.post(payloadRecommendationURL, data={}, headers=headers, timeout=timeout)
        # Check the response
        if response.status_code == 201:
            #data = response.json()
            #print('experiment_name %s  : %s' % (experiment_name , data[0]['kubernetes_objects'][0]['containers'][0]['recommendations']['notifications']['112101'][
            #    'message'] ))
            pass
        else:
            print(
                f'{payloadRecommendationURL} Request failed with status code {response.status_code}: {response.text}')
            #requests.post(createProfileURL, data=profile_json_payload, headers=headers)
    except requests.exceptions.Timeout:
        print('updateRecommendation Timeout occurred while connecting to')
    except requests.exceptions.RequestException as e:
        print('updateRecommendation Timeout occurred while connecting to', e)

def postResultsInBulk(expName, bulkData):
    json_payload = json.dumps(bulkData)
    try:
        # Send the request with the payload
        response = requests.post(updateExpURL, data=json_payload, headers=headers, timeout=timeout)
        # Check the response
        if response.status_code == 201:
            pass
        else:
            print(f'Request failed with status code {expName} {response.status_code}: {response.text}')
            #requests.post(createProfileURL, data=profile_json_payload, headers=headers)
    except requests.exceptions.Timeout:
        print('Timeout occurred while connecting to')
    except requests.exceptions.RequestException as e:
        print('An error occurred while connecting to', e)

if __name__ == "__main__":
    debug = False
    # create an ArgumentParser object
    parser = argparse.ArgumentParser()

    # add the named arguments
    parser.add_argument('--ip', type=str, help='specify kruize  ip')
    parser.add_argument('--port', type=int, help='specify port')
    parser.add_argument('--name', type=str, help='specify experiment name')
    parser.add_argument('--count', type=str,
                        help='specify input the number of experiments and corresponding results, separated by commas.')
    parser.add_argument('--startdate', type=str, help='Specify start date and time in  "%Y-%m-%dT%H:%M:%S.%fZ" format.')
    parser.add_argument('--minutesjump', type=int,
                        help='specify the time difference between the start time and end time of the interval.')

    # parse the arguments from the command line
    args = parser.parse_args()
    if args.port != 0:
        createExpURL = 'http://%s:%s/createExperiment' % (args.ip, args.port)
        updateExpURL = 'http://%s:%s/updateResults' % (args.ip, args.port)
        createProfileURL = 'http://%s:%s/createPerformanceProfile' % (args.ip, args.port)
        updateExpURL = 'http://%s:%s/updateResults' % (args.ip, args.port)
        updateRecommendationURL = 'http://%s:%s/updateRecommendations' % (args.ip, args.port)
    else:
        createExpURL = 'http://%s/createExperiment' % (args.ip)
        updateExpURL = 'http://%s/updateResults' % (args.ip)
        createProfileURL = 'http://%s/createPerformanceProfile' % (args.ip)
        updateExpURL = 'http://%s/updateResults' % (args.ip)
        updateRecommendationURL = 'http://%s/updateRecommendations' % (args.ip)

    expnameprfix = args.name
    expcount = int(args.count.split(',')[0])
    rescount = int(args.count.split(',')[1])
    minutesjump = args.minutesjump
    headers = {
        'Content-Type': 'application/json'
    }
    timeout = (60, 60)
    data, createdata, profile_data = loadData()

    if args.startdate:
        data['interval_end_time'] = args.startdate

    if debug:
        print(createExpURL)
        print(updateExpURL)
        print(createProfileURL)
        print("experiment_name : %s " % (expnameprfix))
        print("Number of experiments to create : %s" % (expcount))
        print("Number of results to create : %s" % (rescount))
        print("startdate : %s" % (data['interval_end_time']))
        print("minutes jump : %s" % (minutesjump))

    #Create a performance profile
    profile_json_payload = json.dumps(profile_data)
    response = requests.post(createProfileURL, data=profile_json_payload, headers=headers)
    if response.status_code == 201:
        if debug: print('Request successful!')
        if expcount > 10 : time.sleep(5)
    else:
        if debug: print(f'Request failed with status code {response.status_code}: {response.text}')

    createExp_time = 0.0
    bulkDataPost_time = 0.0
    updateRec_time = 0.0

    #Create experiment and post results
    start_time = time.time()
    for i in range(1, expcount + 1):
        try:
            successfulCnt = 0
            experiment_name = "%s_%s" % (expnameprfix, i)
            createdata['experiment_name'] = experiment_name
            create_json_payload = json.dumps([createdata])
            #Create experiment
            #requests.post(createProfileURL, data=profile_json_payload, headers=headers)
            createExp_start_time = time.time()
            response = requests.post(createExpURL, data=create_json_payload, headers=headers, timeout=timeout)
            createExp_elapsed_time = time.time() -createExp_start_time
            j = 0
            if args.startdate:
                data['interval_end_time'] = args.startdate
            if response.status_code == 201 or response.status_code == 409 or response.status_code == 400:
                bulkdata = []
                totalResultDates = []
                for j in range(rescount):
                    interval_start_time = datetime.datetime.strptime(data['interval_end_time'], '%Y-%m-%dT%H:%M:%S.%fZ')
                    interval_end_time = datetime.datetime.strptime(data['interval_end_time'],
                                                                   '%Y-%m-%dT%H:%M:%S.%fZ') + datetime.timedelta(
                        minutes=minutesjump)
                    data['interval_end_time'] = interval_end_time.strftime('%Y-%m-%dT%H:%M:%S.%fZ')
                    totalResultDates.append(interval_end_time)
                    data['experiment_name'] = experiment_name
                    data['interval_start_time'] = interval_start_time.strftime('%Y-%m-%dT%H:%M:%S.%fZ')
                    data['interval_end_time'] = interval_end_time.strftime('%Y-%m-%dT%H:%M:%S.%fZ')
                    bulkdata.append(copy.deepcopy(data))
                bulkDataPost_start_time = time.time()
                postResultsInBulk(experiment_name, bulkdata)
                bulkDataPost_elapsed_time = time.time() - bulkDataPost_start_time
                # Get the maximum datetime object
                max_datetime = max(totalResultDates)
                updateRec_start_time = time.time()
                updateRecommendation(experiment_name, max_datetime,)
                updateRec_elapsed_time = time.time() - updateRec_start_time
            else:
                print(f'Request failed with status code {response.status_code}: {response.text}')
        except requests.exceptions.Timeout:
            print('Timeout occurred while connecting to')
        except requests.exceptions.RequestException as e:
            print('An error occurred while connecting to', e)
        except Exception as e:
            print('An error occurred ', e)
        createExp_time += createExp_elapsed_time
        bulkDataPost_time += bulkDataPost_elapsed_time
        updateRec_time += updateRec_elapsed_time

    elapsed_time = time.time() - start_time
    hours, rem = divmod(elapsed_time, 3600)
    minutes, seconds = divmod(rem, 60)
    print("Time elapsed: {:0>2}:{:0>2}:{:05.2f}".format(int(hours), int(minutes), seconds))
    hours, rem = divmod(createExp_time, 3600)
    minutes, seconds = divmod(rem, 60)
    print("createExp elapsed time: {:0>2}:{:0>2}:{:05.2f}".format(int(hours), int(minutes), seconds))
    hours, rem = divmod(bulkDataPost_time, 3600)
    minutes, seconds = divmod(rem, 60)
    print("bulkDataPost elapsed time: {:0>2}:{:0>2}:{:05.2f}".format(int(hours), int(minutes), seconds))
    hours, rem = divmod(updateRec_time, 3600)
    minutes, seconds = divmod(rem, 60)
    print("updateRec elapsed time: {:0>2}:{:0>2}:{:05.2f}".format(int(hours), int(minutes), seconds))
