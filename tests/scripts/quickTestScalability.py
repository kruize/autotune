# This is a handy tool for developers to check scalability. By running the "quickTestScalability.py" script with the specified parameters,
#
# python3 quickTestScalability.py --ip master-1.kruizevin.lab.psi.pnq2.redhat.com --port 31620 --name firstEXP --count 2,10 --minutesjump=15
#
# above script creates default performance profile
# followed by two experiments with the names "firstEXP_1" and "firstEXP_2" are generated.
# Each experiment will have 10 results,
# and the start and end times for each result will be auto-incremented by 15 minutes.

# --ip specify kruize ip address
# --port specify kruize port
# --name name of the experiment
# --count comma seprated first part number of experiment , second part number of results under each experiment
#          if value is 2,10  then 2 experiments and 10 results for each
# --minutesjump diff b/w endtime and starttime

import json
import datetime
import requests
import argparse
import multiprocessing
import time
from multiprocessing import Manager

def postResult(expName,startDate,endDate):
    if args.debug: print("Posting results for %s - %s "%(startDate,endDate))
    # update the JSON data with the new interval times
    data['experiment_name'] = expName
    data['interval_start_time'] = startDate
    data['interval_end_time'] = endDate
    # Convert payload to JSON string
    json_payload = json.dumps([data])
    try:
    # Send the request with the payload
        response = requests.post(updateExpURL, data=json_payload, headers=headers, timeout=timeout)
    # Check the response
        if response.status_code == 201:
            pass
        else:
            print(f'Request failed with status code {response.status_code}: {response.text}')
            requests.post(createProfileURL, data=profile_json_payload, headers=headers)
    except requests.exceptions.Timeout:
        print('Timeout occurred while connecting to')
    except requests.exceptions.RequestException as e:
        print('An error occurred while connecting to',  e)
    finally:
        completedResultDatesList.append( datetime.datetime.strptime( endDate ,  '%Y-%m-%dT%H:%M:%S.%fZ'))

def updateRecommendation(experiment_name,endDate):
    try:
        # Send the request with the payload
        payloadRecommendationURL = "%s?experiment_name=%s&interval_end_time=%s"%(updateRecommendationURL,experiment_name,endDate.strftime('%Y-%m-%dT%H:%M:%S.%fZ')[:-4] + 'Z')
        if args.debug: print(payloadRecommendationURL)
        response = requests.post(payloadRecommendationURL, data={}, headers=headers, timeout=timeout)
        # Check the response
        if response.status_code == 201:
            pass
        else:
            if args.debug: print(f'{payloadRecommendationURL} Request failed with status code {response.status_code}: {response.text}')
            requests.post(createProfileURL, data=profile_json_payload, headers=headers)
    except requests.exceptions.Timeout:
        print('updateRecommendation Timeout occurred while connecting to')
    except requests.exceptions.RequestException as e:
        print('updateRecommendation Timeout occurred while connecting to',  e)

def validateRecommendation():
    totalResultDates.sort()
    completedResultDatesList.sort()
    while len(completedRecommendation) < len(totalResultDates):
        for completedDate in completedResultDatesList:
            if completedDate not in completedRecommendation:
                subTotalResulutDates = totalResultDates[:totalResultDates.index(completedDate)]
                if(all(x in completedResultDatesList for x in subTotalResulutDates)):
                    if args.debug: print("You can generate recommendation for completedDate %s \n due to subTotalResulutDates %s  \n are subset of completedResultSet %s" %(completedDate,subTotalResulutDates,completedResultDatesList))
                    completedRecommendation.append(completedDate)
                    updateRecommendation(createdata['experiment_name'],completedDate)
                else:
                    if args.debug: print("You CANNOT generate recommendation for completedDate %s \n due to subTotalResulutDates %s  \n are not subset of completedResultSet %s" %(completedDate,subTotalResulutDates,completedResultDatesList))
                    pass
                if args.debug: print('*************************')
        time.sleep(2)

def loadData():
    createdata = {
                "version": "1.0",
                "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_10",
                "cluster_name": "cluster-one-division-bell",
                "performance_profile": "resource-optimization-openshift",
                "mode": "monitor",
                "target_cluster": "remote",
                "kubernetes_objects": [
                    {
                        "type": "deployment",
                        "name": "tfb-qrh-deployment_5",
                        "namespace": "default_5",
                        "containers": [
                            {
                                "container_image_name": "kruize/tfb-db:1.15",
                                "container_name": "tfb-server-0"
                            },
                            {
                                "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
                                "container_name": "tfb-server-1"
                            }
                        ]
                    }
                ],
                "trial_settings": {
                    "measurement_duration": "15min"
                },
                "recommendation_settings": {
                    "threshold": "0.1"
                }
            }

    data = {
                        "version": "1.0",
                        "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_4",
                        "interval_start_time": "2023-01-01T00:00:00.000Z",
                        "interval_end_time": "2023-01-01T00:00:00.000Z",
                        "kubernetes_objects": [
                            {
                                "type": "deployment",
                                "name": "tfb-qrh-deployment_5",
                                "namespace": "default_5",
                                "containers": [
                                    {
                                        "container_image_name": "kruize/tfb-db:1.15",
                                        "container_name": "tfb-server-0",
                                        "metrics": [
                                            {
                                                "name": "cpuRequest",
                                                "results": {
                                                    "aggregation_info": {
                                                        "sum": None,
                                                        "avg": 0,
                                                        "format": "cores"
                                                    }
                                                }
                                            },
                                            {
                                                "name": "cpuLimit",
                                                "results": {
                                                    "aggregation_info": {
                                                        "sum": None,
                                                        "avg": 0,
                                                        "format": "cores"
                                                    }
                                                }
                                            },
                                            {
                                                "name": "cpuUsage",
                                                "results": {
                                                    "aggregation_info": {
                                                        "min": 0,
                                                        "max": 0,
                                                        "sum": 0,
                                                        "avg": 0,
                                                        "format": "cores"
                                                    }
                                                }
                                            },
                                            {
                                                "name": "cpuThrottle",
                                                "results": {
                                                    "aggregation_info": {
                                                        "sum": 0,
                                                        "max": 0,
                                                        "avg": 0,
                                                        "format": "cores"
                                                    }
                                                }
                                            },
                                            {
                                                "name": "memoryRequest",
                                                "results": {
                                                    "aggregation_info": {
                                                        "sum": 260.85,
                                                        "avg": 50.21,
                                                        "format": "MiB"
                                                    }
                                                }
                                            },
                                            {
                                                "name": "memoryLimit",
                                                "results": {
                                                    "aggregation_info": {
                                                        "sum": 700,
                                                        "avg": 100,
                                                        "format": "MiB"
                                                    }
                                                }
                                            },
                                            {
                                                "name": "memoryUsage",
                                                "results": {
                                                    "aggregation_info": {
                                                        "min": 50.6,
                                                        "max": 198.5,
                                                        "sum": 298.5,
                                                        "avg": 40.1,
                                                        "format": "MiB"
                                                    }
                                                }
                                            },
                                            {
                                                "name": "memoryRSS",
                                                "results": {
                                                    "aggregation_info": {
                                                        "min": 50.6,
                                                        "max": 523.6,
                                                        "sum": 123.6,
                                                        "avg": 31.91,
                                                        "format": "MiB"
                                                    }
                                                }
                                            }
                                        ]
                                    },
                                    {
                                        "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
                                        "container_name": "tfb-server-1",
                                        "metrics": [
                                            {
                                                "name": "cpuRequest",
                                                "results": {
                                                    "aggregation_info": {
                                                        "sum": 4.4,
                                                        "avg": 1.1,
                                                        "format": "cores"
                                                    }
                                                }
                                            },
                                            {
                                                "name": "cpuLimit",
                                                "results": {
                                                    "aggregation_info": {
                                                        "sum": 2.0,
                                                        "avg": 0.5,
                                                        "format": "cores"
                                                    }
                                                }
                                            },
                                            {
                                                "name": "cpuUsage",
                                                "results": {
                                                    "aggregation_info": {
                                                        "min": 0.14,
                                                        "max": 0.84,
                                                        "sum": 0.84,
                                                        "avg": 0.12,
                                                        "format": "cores"
                                                    }
                                                }
                                            },
                                            {
                                                "name": "cpuThrottle",
                                                "results": {
                                                    "aggregation_info": {
                                                        "sum": 0.19,
                                                        "max": 0.09,
                                                        "avg": 0.045,
                                                        "format": "cores"
                                                    }
                                                }
                                            },
                                            {
                                                "name": "memoryRequest",
                                                "results": {
                                                    "aggregation_info": {
                                                        "sum": 250.85,
                                                        "avg": 50.21,
                                                        "format": "MiB"
                                                    }
                                                }
                                            },
                                            {
                                                "name": "memoryLimit",
                                                "results": {
                                                    "aggregation_info": {
                                                        "sum": 500,
                                                        "avg": 100,
                                                        "format": "MiB"
                                                    }
                                                }
                                            },
                                            {
                                                "name": "memoryUsage",
                                                "results": {
                                                    "aggregation_info": {
                                                        "min": 50.6,
                                                        "max": 198.5,
                                                        "sum": 198.5,
                                                        "avg": 40.1,
                                                        "format": "MiB"
                                                    }
                                                }
                                            },
                                            {
                                                "name": "memoryRSS",
                                                "results": {
                                                    "aggregation_info": {
                                                        "min": 50.6,
                                                        "max": 123.6,
                                                        "sum": 123.6,
                                                        "avg": 31.91,
                                                        "format": "MiB"
                                                    }
                                                }
                                            }
                                        ]
                                    }
                                ]
                            }
                        ]
                    }
    profile_data = {
       "name": "resource-optimization-openshift",
       "profile_version": 1,
       "k8s_type": "openshift",
       "slo": {
           "slo_class": "resource_usage",
           "direction": "minimize",
           "objective_function": {
               "function_type": "expression",
               "expression": "cpuRequest"
           },
           "function_variables": [
               {
                   "name": "cpuRequest",
                   "datasource": "prometheus",
                   "value_type": "double",
                   "kubernetes_object": "container",
                   "query": "kube_pod_container_resource_requests{pod=~'$DEPLOYMENT_NAME$-[^-]*-[^-]*$', container='$CONTAINER_NAME$', namespace='$NAMESPACE', resource='cpu', unit='core'}",
                   "aggregation_functions": [
                       {
                           "function": "avg",
                           "query": "avg(kube_pod_container_resource_requests{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE\", resource=\"cpu\", unit=\"core\"})"
                       }
                   ]
               }
           ]
       }
   }
    return (data,createdata,profile_data)




if __name__ == "__main__":
    # create an ArgumentParser object
    parser = argparse.ArgumentParser()

    # add the named arguments
    parser.add_argument('--ip', type=str, help='specify kruize  ip')
    parser.add_argument('--port', type=int, help='specify port')
    parser.add_argument('--name', type=str, help='specify experiment name')
    parser.add_argument('--count', type=str, help='specify input the number of experiments and corresponding results, separated by commas.')
    parser.add_argument('--startdate', type=str, help='Specify start date and time in  "%Y-%m-%dT%H:%M:%S.%fZ" format.')
    parser.add_argument('--minutesjump', type=int, help='specify the time difference between the start time and end time of the interval.')
    parser.add_argument('--postresults',  action='store_true' , help='By enabling flag it genrates results and post to updateResults api.')
    parser.add_argument('--parallelresultcount', type=int, help='specify the quantity of processes to execute simultaneously for posting the results.')
    parser.add_argument('--generaterecommendation',  action='store_true', help='execution of recommendation generation.')
    parser.add_argument('--debug', type=bool, help='print debug log.')



    # parse the arguments from the command line
    args = parser.parse_args()

    createExpURL = 'http://%s:%s/createExperiment'%(args.ip,args.port)
    updateExpURL = 'http://%s:%s/updateResults'%(args.ip,args.port)
    createProfileURL = 'http://%s:%s/createPerformanceProfile'%(args.ip,args.port)
    updateExpURL = 'http://%s:%s/updateResults'%(args.ip,args.port)
    updateRecommendationURL = 'http://%s:%s/updateRecommendations'%(args.ip,args.port)
    expnameprfix = args.name
    expcount = int(args.count.split(',')[0])
    rescount = int(args.count.split(',')[1])
    minutesjump = args.minutesjump
    generaterecommendation = args.generaterecommendation
    headers = {
        'Content-Type': 'application/json'
    }
    timeout = (60, 60)
    data,createdata,profile_data = loadData()

    if args.startdate:
        data['interval_end_time'] = args.startdate

    print(createExpURL)
    print(updateExpURL)
    print(createProfileURL)
    print("experiment_name : %s " %(expnameprfix))
    print("Number of experiments to create : %s" %(expcount))
    print("Number of results to create : %s" %(rescount))
    print("startdate : %s" %(data['interval_end_time']))
    print("minutes jump : %s" %(minutesjump))
    print("postresults : %s" %(args.postresults))
    print("generaterecommendation : %s" %(generaterecommendation))


    profile_json_payload = json.dumps(profile_data)
    # Send the request with the payload
    response = requests.post(createProfileURL, data=profile_json_payload, headers=headers)
    # Check the response
    if response.status_code == 201:
        print('Request successful!')
    else:
       print(f'Request failed with status code {response.status_code}: {response.text}')


    # Create a shared list using multiprocessing.Manager()
    manager = Manager()
    completedResultDatesList = manager.list()
    totalResultDates = manager.list()
    completedResultDatesList = manager.list()
    completedRecommendation = manager.list()

    start_time = time.time()
    for i in range(1,expcount+1):
        try:
            successfulCnt = 0
            experiment_name = "%s_%s" %(expnameprfix,i)
            createdata['experiment_name'] = experiment_name
            create_json_payload = json.dumps([createdata])
            # Send the request with the payload
            response = requests.post(createExpURL, data=create_json_payload, headers=headers, timeout=timeout)
            # Check the response
            j = 0
            if response.status_code == 201 or response.status_code == 409:
                print('Create experiment_name %s Request successful!'%(experiment_name))
                timeDeltaList = []
                for j in range(rescount):
                    interval_start_time = datetime.datetime.strptime(data['interval_end_time'] ,  '%Y-%m-%dT%H:%M:%S.%fZ')
                    interval_end_time = datetime.datetime.strptime(data['interval_end_time'] , '%Y-%m-%dT%H:%M:%S.%fZ' ) + datetime.timedelta(minutes=minutesjump)
                    data['interval_end_time'] = interval_end_time.strftime('%Y-%m-%dT%H:%M:%S.%fZ')
                    timeDeltaList.append((experiment_name,interval_start_time.strftime('%Y-%m-%dT%H:%M:%S.%fZ'),interval_end_time.strftime('%Y-%m-%dT%H:%M:%S.%fZ')))
                    totalResultDates.append(interval_end_time)

                if args.postresults and args.generaterecommendation:
                    # Create a pool of processes
                    recommendationProcess = multiprocessing.Process(target=validateRecommendation)
                    recommendationProcess.start()
                    num_processes = args.parallelresultcount
                    pool = multiprocessing.Pool(processes=num_processes)
                    # Start the parallel execution
                    pool.starmap(postResult, timeDeltaList)
                    # Close the pool and wait for the processes to finish
                    recommendationProcess.join()
                    pool.close()
                    pool.join()
                elif args.postresults:
                    num_processes = args.parallelresultcount
                    pool = multiprocessing.Pool(processes=num_processes)
                    # Start the parallel execution
                    pool.starmap(postResult, timeDeltaList)
                    # Close the pool and wait for the processes to finish
                    pool.close()
                    pool.join()
                elif args.generaterecommendation:
                    recommendationDataList = []
                    for i_end_date in totalResultDates:
                        recommendationDataList.append((createdata['experiment_name'],i_end_date))
                    num_processes = args.parallelresultcount
                    pool = multiprocessing.Pool(processes=num_processes)
                    # Start the parallel execution
                    pool.starmap(updateRecommendation, recommendationDataList)
                    # Close the pool and wait for the processes to finish
                    pool.close()
                    pool.join()
                else:
                    print("Invalid choice")
            else:
               print(f'Request failed with status code {response.status_code}: {response.text}')
        except requests.exceptions.Timeout:
            print('Timeout occurred while connecting to')
        except requests.exceptions.RequestException as e:
            print('An error occurred while connecting to', e)
        except Exception as e:
            print('An error occurred ', e)


    print('Request successful!  completed  :  ExperimentCount : %s/%s   Results Count : %s/%s  Recommendation count : %s'  %(i,expcount,len(completedResultDatesList),len(totalResultDates),len(completedRecommendation) ))
    elapsed_time = time.time() - start_time
    hours, rem = divmod(elapsed_time, 3600)
    minutes, seconds = divmod(rem, 60)
    print("Time elapsed: {:0>2}:{:0>2}:{:05.2f}".format(int(hours), int(minutes), seconds))




 #for i in {1..50}; do nohup time python3 -u quickTestScalability.py --ip master-1.kruizevin.lab.psi.pnq2.redhat.com --port 31521 --name 5kexp$i --count 100,1500 --minutesjump=15 > /tmp/5kexp$i.log 2>&1 & done





