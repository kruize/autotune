import argparse
import copy
import datetime
import json
import requests
import time


# Define a decorator function to measure the execution time of a function
def timing_decorator(func):
    def wrapper(*args, **kwargs):
        start_time = time.time()
        result = func(*args, **kwargs)
        end_time = time.time()
        print(f"\033[{10}C{func.__name__} took {end_time - start_time:.4f} seconds to execute.", end="\r")
        return result

    return wrapper


def loadData():
    createdata = {"version": "1.0", "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_10",
                  "cluster_name": "cluster-one-division-bell", "performance_profile": "resource-optimization-openshift",
                  "mode": "monitor", "target_cluster": "remote", "kubernetes_objects": [
            {"type": "deployment", "name": "tfb-qrh-deployment_5", "namespace": "default_5",
             "containers": [{"container_image_name": "kruize/tfb-db:1.15", "container_name": "tfb-server-0"},
                            {"container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
                             "container_name": "tfb-server-1"}]}], "trial_settings": {"measurement_duration": "15min"},
                  "recommendation_settings": {"threshold": "0.1"}}
    data = {"version": "3.0", "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_4",
            "interval_start_time": "2023-01-01T00:00:00.000Z", "interval_end_time": "2023-01-01T00:00:00.000Z",
            "kubernetes_objects": [{"type": "deployment", "name": "tfb-qrh-deployment_5", "namespace": "default_5",
                                    "containers": [
                                        {"container_image_name": "kruize/tfb-db:1.15", "container_name": "tfb-server-0",
                                         "metrics": [{"name": "cpuRequest", "results": {
                                             "aggregation_info": {"sum": 1, "avg": 0, "format": "cores"}}},
                                                     {"name": "cpuLimit", "results": {
                                                         "aggregation_info": {"sum": 1, "avg": 0,
                                                                              "format": "cores"}}}, {"name": "cpuUsage",
                                                                                                     "results": {
                                                                                                         "aggregation_info": {
                                                                                                             "min": 0,
                                                                                                             "max": 0,
                                                                                                             "sum": 0,
                                                                                                             "avg": 0,
                                                                                                             "format": "cores"}}},
                                                     {"name": "cpuThrottle", "results": {
                                                         "aggregation_info": {"sum": 0, "max": 0, "avg": 0,
                                                                              "format": "cores"}}},
                                                     {"name": "memoryRequest", "results": {
                                                         "aggregation_info": {"sum": 260.85, "avg": 50.21,
                                                                              "format": "MiB"}}},
                                                     {"name": "memoryLimit", "results": {
                                                         "aggregation_info": {"sum": 700, "avg": 100,
                                                                              "format": "MiB"}}},
                                                     {"name": "memoryUsage", "results": {
                                                         "aggregation_info": {"min": 50.6, "max": 198.5, "sum": 298.5,
                                                                              "avg": 40.1, "format": "MiB"}}},
                                                     {"name": "memoryRSS", "results": {
                                                         "aggregation_info": {"min": 50.6, "max": 523.6, "sum": 123.6,
                                                                              "avg": 31.91, "format": "MiB"}}}]},
                                        {"container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
                                         "container_name": "tfb-server-1", "metrics": [{"name": "cpuRequest",
                                                                                        "results": {
                                                                                            "aggregation_info": {
                                                                                                "sum": 4.4, "avg": 1.1,
                                                                                                "format": "cores"}}},
                                                                                       {"name": "cpuLimit", "results": {
                                                                                           "aggregation_info": {
                                                                                               "sum": 2.0, "avg": 0.5,
                                                                                               "format": "cores"}}},
                                                                                       {"name": "cpuUsage", "results": {
                                                                                           "aggregation_info": {
                                                                                               "min": 0.14, "max": 0.84,
                                                                                               "sum": 0.84, "avg": 0.12,
                                                                                               "format": "cores"}}},
                                                                                       {"name": "cpuThrottle",
                                                                                        "results": {
                                                                                            "aggregation_info": {
                                                                                                "sum": 0.19,
                                                                                                "max": 0.09,
                                                                                                "avg": 0.045,
                                                                                                "format": "cores"}}},
                                                                                       {"name": "memoryRequest",
                                                                                        "results": {
                                                                                            "aggregation_info": {
                                                                                                "sum": 250.85,
                                                                                                "avg": 50.21,
                                                                                                "format": "MiB"}}},
                                                                                       {"name": "memoryLimit",
                                                                                        "results": {
                                                                                            "aggregation_info": {
                                                                                                "sum": 500, "avg": 100,
                                                                                                "format": "MiB"}}},
                                                                                       {"name": "memoryUsage",
                                                                                        "results": {
                                                                                            "aggregation_info": {
                                                                                                "min": 50.6,
                                                                                                "max": 198.5,
                                                                                                "sum": 198.5,
                                                                                                "avg": 40.1,
                                                                                                "format": "MiB"}}},
                                                                                       {"name": "memoryRSS",
                                                                                        "results": {
                                                                                            "aggregation_info": {
                                                                                                "min": 50.6,
                                                                                                "max": 123.6,
                                                                                                "sum": 123.6,
                                                                                                "avg": 31.91,
                                                                                                "format": "MiB"}}}]}]}]}
    profile_data = {"name": "resource-optimization-openshift", "profile_version": 1, "k8s_type": "openshift",
                    "slo": {"slo_class": "resource_usage", "direction": "minimize",
                            "objective_function": {"function_type": "expression", "expression": "cpuRequest"},
                            "function_variables": [
                                {"name": "cpuRequest", "datasource": "prometheus", "value_type": "double",
                                 "kubernetes_object": "container",
                                 "query": "kube_pod_container_resource_requests{pod=~'$DEPLOYMENT_NAME$-[^-]*-[^-]*$', container='$CONTAINER_NAME$', namespace='$NAMESPACE', resource='cpu', unit='core'}",
                                 "aggregation_functions": [{"function": "avg",
                                                            "query": "avg(kube_pod_container_resource_requests{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE\", resource=\"cpu\", unit=\"core\"})"}]}]}}
    return (data, createdata, profile_data)


# @timing_decorator
def updateRecommendation(experiment_name, endDate):
    try:
        # Send the request with the payload
        payloadRecommendationURL = "%s?experiment_name=%s&interval_end_time=%s" % (
            updateRecommendationURL, experiment_name, endDate.strftime('%Y-%m-%dT%H:%M:%S.%fZ')[:-4] + 'Z')
        response = requests.post(payloadRecommendationURL, data={}, headers=headers, timeout=timeout)
        # Check the response
        if response.status_code == 201:
            pass
        else:
            print(
                f'{payloadRecommendationURL} Request failed with status code {response.status_code}: {response.text}')
    except requests.exceptions.Timeout:
        print('updateRecommendation Timeout occurred while connecting to')
        time.sleep(5)
    except requests.exceptions.RequestException as e:
        print('updateRecommendation Timeout occurred while connecting to', e)
        time.sleep(5)


# @timing_decorator
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
            # requests.post(createProfileURL, data=profile_json_payload, headers=headers)
    except requests.exceptions.Timeout:
        print('Timeout occurred while connecting to postResultsInBulk')
        time.sleep(5)
    except requests.exceptions.RequestException as e:
        print('An error occurred while connecting to postResultsInBulk', e)
        time.sleep(5)


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
    timeout = (300, 300)
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

    # Create a performance profile
    profile_json_payload = json.dumps(profile_data)
    response = requests.post(createProfileURL, data=profile_json_payload, headers=headers)
    if response.status_code == 201:
        if debug: print('Request successful!')
        if expcount > 10: time.sleep(5)
    else:
        if debug: print(f'Request failed with status code {response.status_code}: {response.text}')

    # Create experiment and post results
    start_time = time.time()
    for i in range(1, expcount + 1):
        try:
            successfulCnt = 0
            experiment_name = "%s_%s" % (expnameprfix, i)
            createdata['experiment_name'] = experiment_name
            createdata['cluster_name'] = "%s_cluster" % (experiment_name)
            create_json_payload = json.dumps([createdata])
            # Create experiment
            response = requests.post(createExpURL, data=create_json_payload, headers=headers, timeout=timeout)
            # time.sleep(2)
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
                postResultsInBulk(experiment_name, bulkdata)
                # Get the maximum datetime object
                max_datetime = max(totalResultDates)
                updateRecommendation(experiment_name, max_datetime, )
            else:
                print(f'Request failed with status code {response.status_code}: {response.text}')
            print("  %s/%s      " % (i, expcount), end="\r")
        except requests.exceptions.Timeout:
            print('Timeout occurred while connecting to createExperiment')
            time.sleep(5)
        except requests.exceptions.RequestException as e:
            print('An error occurred while connecting to createExperiment', e)
            time.sleep(5)
        except Exception as e:
            print('An error occurred ', e)

    elapsed_time = time.time() - start_time
    hours, rem = divmod(elapsed_time, 3600)
    minutes, seconds = divmod(rem, 60)
    print("\nTime elapsed: {:0>2}:{:0>2}:{:05.2f}\n".format(int(hours), int(minutes), seconds))
