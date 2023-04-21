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

# create an ArgumentParser object
parser = argparse.ArgumentParser()

# add the named arguments
parser.add_argument('--ip', type=str, help='enter  ip')
parser.add_argument('--port', type=int, help='enter port')
parser.add_argument('--name', type=str, help='enter experiment name')
parser.add_argument('--count', type=str, help='enter number of experiment and results to create separated by , ')
parser.add_argument('--minutesjump', type=int, help='enter time diff b/w interval_start_time and interval_end_time')

# parse the arguments from the command line
args = parser.parse_args()

createExpURL = 'http://%s:%s/createExperiment'%(args.ip,args.port)
updateExpURL = 'http://%s:%s/updateResults'%(args.ip,args.port)
createProfileURL = 'http://%s:%s/createPerformanceProfile'%(args.ip,args.port)
expnameprfix = args.name
expcount = int(args.count.split(',')[0])
rescount = int(args.count.split(',')[1])
minutesjump = args.minutesjump
headers = {
    'Content-Type': 'application/json'
}
timeout = (60, 60)

print(createExpURL)
print(updateExpURL)
print(createProfileURL)
print("experiment_name : %s " %(expnameprfix))
print("Number of experiments to create : %s" %(expcount))
print("Number of results to create : %s" %(rescount))
print("minutes jump : %s" %(minutesjump))

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
profile_json_payload = json.dumps(profile_data)
# Send the request with the payload
response = requests.post(createProfileURL, data=profile_json_payload, headers=headers)
# Check the response
if response.status_code == 201:
    print('Request successful!')
else:
   print(f'Request failed with status code {response.status_code}: {response.text}')


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
                    "interval_start_time": "2022-01-26T14:35:43.511Z",
                    "interval_end_time": "2022-01-26T14:50:50.511Z",
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
                                                    "sum": 3.4,
                                                    "avg": 2.1,
                                                    "format": "cores"
                                                }
                                            }
                                        },
                                        {
                                            "name": "cpuLimit",
                                            "results": {
                                                "aggregation_info": {
                                                    "sum": 3.0,
                                                    "avg": 1.5,
                                                    "format": "cores"
                                                }
                                            }
                                        },
                                        {
                                            "name": "cpuUsage",
                                            "results": {
                                                "aggregation_info": {
                                                    "min": 0.54,
                                                    "max": 0.94,
                                                    "sum": 0.52,
                                                    "avg": 0.12,
                                                    "format": "cores"
                                                }
                                            }
                                        },
                                        {
                                            "name": "cpuThrottle",
                                            "results": {
                                                "aggregation_info": {
                                                    "sum": 0.9,
                                                    "max": 0.09,
                                                    "avg": 0.04,
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


for i in range(expcount):
    try:
        experiment_name = "%s_%s" %(expnameprfix,i)
        createdata['experiment_name'] = experiment_name
        create_json_payload = json.dumps([createdata])
        # Send the request with the payload
        response = requests.post(createExpURL, data=create_json_payload, headers=headers, timeout=timeout)
        # Check the response
        if response.status_code == 201:
            print('Request successful!')
        else:
           print(f'Request failed with status code {response.status_code}: {response.text}')

        data['experiment_name'] = experiment_name
        for j in range(rescount):
            try:
                # calculate the new interval start and end times
                interval_start_time = datetime.datetime.strptime(data['interval_end_time'] ,  '%Y-%m-%dT%H:%M:%S.%fZ')
                interval_end_time = datetime.datetime.strptime(data['interval_end_time'] , '%Y-%m-%dT%H:%M:%S.%fZ' ) + datetime.timedelta(minutes=minutesjump)

                # update the JSON data with the new interval times
                data['interval_start_time'] = interval_start_time.strftime('%Y-%m-%dT%H:%M:%S.%fZ')
                data['interval_end_time'] = interval_end_time.strftime('%Y-%m-%dT%H:%M:%S.%fZ')


                # Convert payload to JSON string
                json_payload = json.dumps([data])

                # Send the request with the payload
                response = requests.post(updateExpURL, data=json_payload, headers=headers, timeout=timeout)

                # Check the response
                if response.status_code == 201:
                    pass
                else:
                    print(f'Request failed with status code {response.status_code}: {response.text}')
            except requests.exceptions.Timeout:
                print('Timeout occurred while connecting to')
            except requests.exceptions.RequestException as e:
                print('An error occurred while connecting to',  e)
    except requests.exceptions.Timeout:
        print('Timeout occurred while connecting to')
    except requests.exceptions.RequestException as e:
        print('An error occurred while connecting to', e)

    print('Request successful!  completed  :  %s/%s  %s/%s '  %(i,expcount,j,rescount ))






