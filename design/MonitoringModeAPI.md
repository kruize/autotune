# Remote Monitoring Mode

This article describes how to quickly get started with the Remote Monitoring Mode use case REST API using curl command.
Documentation still in progress stay tuned.

**Note :**  The ISO 8601 standard underpins all timestamp formats. An example of a valid timestamp in this format is
2022-01-23T18:25:43.511Z, which represents January 23, 2022, at 18:25:43.511 UTC.

## CreateExperiment

This is quick guide instructions to create experiments using input JSON as follows. For a more detailed guide,
see [Create Experiment](/design/CreateExperiment.md)

**Request**
`POST /createExperiment`

`curl -H 'Accept: application/json' -X POST --data 'copy paste below JSON' http://<URL>:<PORT>/createExperiment`

```
[
  {
    "version": "1.0",
    "experiment_name": "quarkus-resteasy-autotune-min-http-response-time-db",
    "cluster_name": "cluster-one-division-bell",
    "performance_profile": "resource-optimization-openshift",
    "mode": "monitor",
    "target_cluster": "remote",
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment",
        "namespace": "default",
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
]
```

**Response**

```
{
    "message": "Experiment registered successfully with Autotune. View registered experiments at /listExperiments",
    "httpcode": 201,
    "documentationLink": "",
    "status": "SUCCESS"
}
```

## Update Metric Results

Update metric results using input JSON as follows. For a more detailed guide,
see [Update results](/design/UpdateResults.md)

**NOTE:** The update to results of a particular experiment should follow Time Series Order to get valid recommendations.

**Eg:** after updating the results for time stamp `2022-01-23T18:25:43.511Z` you cannot add results previous to that timestamp


* Mandatory parameters in the input JSON:
 ```
 cpuUsage, memoryUsage, memoryRSS
 ```
* Note: If the parameters `cpuRequest`, `cpuLimit`, `memoryRequest` and `memoryLimit` are missing, then they are assumed to not have been set for the container in question.

**Request**
`POST /updateResults`

`curl -H 'Accept: application/json' -X POST --data 'copy paste below JSON' http://<URL>:<PORT>/updateResults`

```
[
  {
    "version": "1.0",
    "experiment_name": "quarkus-resteasy-autotune-min-http-response-time-db",
    "interval_start_time": "2022-01-23T18:25:43.511Z",
    "interval_end_time": "2022-01-23T18:40:43.602Z",
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment",
        "namespace": "default",
        "containers": [
          {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-0",
            "metrics": [
              {
                "name": "cpuRequest",
                "results": {
                  "value": 1.1,
                  "format": "cores",
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
                  "value": 0.5,
                  "format": "cores",
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
                  "value": 0.12,
                  "format": "cores",
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
                  "value": 0.045,
                  "format": "cores",
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
                  "value": 50.12,
                  "format": "MiB",
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
                  "value": 100,
                  "format": "MiB",
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
                  "value": 40.1,
                  "format": "MiB",
                  "aggregation_info": {
                    "min": 50.6,
                    "max": 198.50,
                    "sum": 198.50,
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
          },
          {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "metrics": [
              {
                "name": "cpuRequest",
                "results": {
                  "value": 1.1,
                  "format": "cores",
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
                  "value": 0.5,
                  "format": "cores",
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
                  "value": 0.12,
                  "format": "cores",
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
                  "value": 0.045,
                  "format": "cores",
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
                  "value": 50.12,
                  "format": "MiB",
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
                  "value": 100,
                  "format": "MiB",
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
                  "value": 40.1,
                  "format": "MiB",
                  "aggregation_info": {
                    "min": 50.6,
                    "max": 198.50,
                    "sum": 198.50,
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
]

```

**Response**

```
{
    "message": "Updated metrics results successfully with Autotune. View update results at /listExperiments",
    "httpcode": 201,
    "documentationLink": "",
    "status": "SUCCESS"
}
```

## Experiments

List experiments output JSON as follows:

**Attributes:**

| Param              | Possible options | Defaults | Description                                                                                                              | 
|--------------------|------------------|----------|--------------------------------------------------------------------------------------------------------------------------|
| `results`          | `true`, `false`  | `false`  | Passing results = true as the parameter to the API returns the latest results and no recommendations                     |
| `latest`           | `true`, `false`  | `true`   | Gets you the latest available results or recommendation if true, else returns all the results or recommendations         |
| `recommendations`  | `true`, `false`  | `false`  | Passing recommendations = true as the parameter to the API returns the latest recommendations and no results             |
| `experiment_name`  | Any string       | None     | Passing Experiment Name as the parameter to the API returns the recommendation of the particular experiment if it exists |

**Request without Parameter**

`GET /listExperiments`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listExperiments`

If no parameter is passed API returns all the experiment details without any results or recommendations.

**Response**
```
[
  {
    "version": "1.0",
    "experiment_id": "aecee67abcdea250c7cf93dc4d131d92a116ae46100107e7a08c5e6ea41e9e1a",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_2",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_2",
        "namespace": "default_2",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1"
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0"
          }
        }
      }
    ]
  },
  {
    "version": "1.0",
    "experiment_id": "423c5efa0eb9978db4715cccdc67e03b36921e082f7f325e7ca3be378521bdb1",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_3",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_3",
        "namespace": "default_3",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1"
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0"
          }
        }
      }
    ]
  },
  {
    "version": "1.0",
    "experiment_id": "f0007796e65c999d843bebd447c2fbaa6aaf9127c614da55e333cd6bdb628a74",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_0",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_0",
        "namespace": "default_0",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1"
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0"
          }
        }
      }
    ]
  }
]
```

**Request with experiment name parameter**

`GET /listExperiments`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listExperiments?experiment_name=<experiment_name>`

Returns the experiment details of the specified experiment without any results or recommendations

**Response for experiment name - `quarkus-resteasy-kruize-min-http-response-time-db_0`**
```
[
  {
    "version": "1.0",
    "experiment_id": "f0007796e65c999d843bebd447c2fbaa6aaf9127c614da55e333cd6bdb628a74",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_0",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_0",
        "namespace": "default_0",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1"
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0"
          }
        }
      }
    ]
  }
]
```

**Request with results set to true**

`GET /listExperiments`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listExperiments?results=true`

Returns the latest result of all the experiments with no recommendations

**Note : When we don't pass `latest` in the query URL, it takes as `true` by default.**

**Response for experiment name - `quarkus-resteasy-kruize-min-http-response-time-db_0`**
```
[
  {
    "version": "1.0",
    "experiment_id": "aecee67abcdea250c7cf93dc4d131d92a116ae46100107e7a08c5e6ea41e9e1a",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_2",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_2",
        "namespace": "default_2",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1"
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0"
          }
        }
      }
    ]
  },
  {
    "version": "1.0",
    "experiment_id": "423c5efa0eb9978db4715cccdc67e03b36921e082f7f325e7ca3be378521bdb1",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_3",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_3",
        "namespace": "default_3",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1"
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0"
          }
        }
      }
    ]
  },
  {
    "version": "1.0",
    "experiment_id": "f0007796e65c999d843bebd447c2fbaa6aaf9127c614da55e333cd6bdb628a74",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_0",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_0",
        "namespace": "default_0",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "results": {
              "2023-04-02T13:30:00.680Z": {
                "metrics": {
                  "memoryRSS": {
                    "name": "memoryRSS",
                    "aggregation_info": {
                      "avg": 31.91,
                      "max": 123.6,
                      "min": 50.6,
                      "sum": 123.6,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "cpuUsage": {
                    "name": "cpuUsage",
                    "aggregation_info": {
                      "avg": 0.12,
                      "max": 0.84,
                      "min": 0.14,
                      "sum": 0.84,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "cpuThrottle": {
                    "name": "cpuThrottle",
                    "aggregation_info": {
                      "avg": 0.045,
                      "max": 0.09,
                      "sum": 0.19,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "cpuRequest": {
                    "name": "cpuRequest",
                    "aggregation_info": {
                      "avg": 5.37,
                      "sum": 16.11,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "memoryUsage": {
                    "name": "memoryUsage",
                    "aggregation_info": {
                      "avg": 40.1,
                      "max": 198.5,
                      "min": 50.6,
                      "sum": 198.5,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "memoryRequest": {
                    "name": "memoryRequest",
                    "aggregation_info": {
                      "avg": 50.21,
                      "sum": 250.85,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "cpuLimit": {
                    "name": "cpuLimit",
                    "aggregation_info": {
                      "avg": 0.5,
                      "sum": 2,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "memoryLimit": {
                    "name": "memoryLimit",
                    "aggregation_info": {
                      "avg": 100,
                      "sum": 500,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  }
                },
                "interval_start_time": "2023-04-02T13:15:00.433Z",
                "interval_end_time": "2023-04-02T13:30:00.680Z",
                "duration_in_minutes": 15.004116666666667,
                "duration_in_seconds": 900
              }
            }
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0"
          }
        }
      }
    ]
  }
]
```

**Request with results set to true and with experiment name parameter**

`GET /listExperiments`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listExperiments?results=true&experiment_name=<experiment_name>`

Returns the latest result of the specified experiment with no recommendations

**Response for experiment name - `quarkus-resteasy-kruize-min-http-response-time-db_0`**
```
[
  {
    "version": "1.0",
    "experiment_id": "f0007796e65c999d843bebd447c2fbaa6aaf9127c614da55e333cd6bdb628a74",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_0",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_0",
        "namespace": "default_0",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "results": {
              "2023-04-02T13:30:00.680Z": {
                "metrics": {
                  "memoryRSS": {
                    "name": "memoryRSS",
                    "aggregation_info": {
                      "avg": 31.91,
                      "max": 123.6,
                      "min": 50.6,
                      "sum": 123.6,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "cpuUsage": {
                    "name": "cpuUsage",
                    "aggregation_info": {
                      "avg": 0.12,
                      "max": 0.84,
                      "min": 0.14,
                      "sum": 0.84,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "cpuThrottle": {
                    "name": "cpuThrottle",
                    "aggregation_info": {
                      "avg": 0.045,
                      "max": 0.09,
                      "sum": 0.19,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "cpuRequest": {
                    "name": "cpuRequest",
                    "aggregation_info": {
                      "avg": 5.37,
                      "sum": 16.11,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "memoryUsage": {
                    "name": "memoryUsage",
                    "aggregation_info": {
                      "avg": 40.1,
                      "max": 198.5,
                      "min": 50.6,
                      "sum": 198.5,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "memoryRequest": {
                    "name": "memoryRequest",
                    "aggregation_info": {
                      "avg": 50.21,
                      "sum": 250.85,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "cpuLimit": {
                    "name": "cpuLimit",
                    "aggregation_info": {
                      "avg": 0.5,
                      "sum": 2.0,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "memoryLimit": {
                    "name": "memoryLimit",
                    "aggregation_info": {
                      "avg": 100.0,
                      "sum": 500.0,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  }
                },
                "interval_start_time": "2023-04-02T13:15:00.433Z",
                "interval_end_time": "2023-04-02T13:30:00.680Z",
                "duration_in_minutes": 15.004116666666667,
                "duration_in_seconds": 900.0
              }
            }
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0"
          }
        }
      }
    ]
  }
]
```

**Request with results set to true and latest set to false**

`GET /listExperiments`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listExperiments?results=true&latest=false`

Returns all the results of all the experiments with no recommendations

**Response for experiment name - `quarkus-resteasy-kruize-min-http-response-time-db_0`**
```
[
  {
    "version": "1.0",
    "experiment_id": "aecee67abcdea250c7cf93dc4d131d92a116ae46100107e7a08c5e6ea41e9e1a",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_2",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_2",
        "namespace": "default_2",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1"
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0"
          }
        }
      }
    ]
  },
  {
    "version": "1.0",
    "experiment_id": "423c5efa0eb9978db4715cccdc67e03b36921e082f7f325e7ca3be378521bdb1",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_3",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_3",
        "namespace": "default_3",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1"
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0"
          }
        }
      }
    ]
  },
  {
    "version": "1.0",
    "experiment_id": "f0007796e65c999d843bebd447c2fbaa6aaf9127c614da55e333cd6bdb628a74",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_0",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_0",
        "namespace": "default_0",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "results": {
              "2023-04-02T11:15:00.770Z": {
                "metrics": {
                  "memoryRSS": {
                    "name": "memoryRSS",
                    "aggregation_info": {
                      "avg": 31.91,
                      "max": 123.6,
                      "min": 50.6,
                      "sum": 123.6,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "cpuUsage": {
                    "name": "cpuUsage",
                    "aggregation_info": {
                      "min": 0.14,
                      "sum": 0.84,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "cpuThrottle": {
                    "name": "cpuThrottle",
                    "aggregation_info": {
                      "sum": 0.19,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "cpuRequest": {
                    "name": "cpuRequest",
                    "aggregation_info": {
                      "avg": 5.37,
                      "sum": 16.11,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "memoryUsage": {
                    "name": "memoryUsage",
                    "aggregation_info": {
                      "avg": 40.1,
                      "max": 198.5,
                      "min": 50.6,
                      "sum": 198.5,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "cpuLimit": {
                    "name": "cpuLimit",
                    "aggregation_info": {
                      "avg": 0.5,
                      "sum": 2,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "memoryLimit": {
                    "name": "memoryLimit",
                    "aggregation_info": {
                      "avg": 100,
                      "sum": 500,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  }
                },
                "interval_start_time": "2023-04-02T11:00:00.770Z",
                "interval_end_time": "2023-04-02T11:15:00.770Z",
                "duration_in_minutes": 15,
                "duration_in_seconds": 900
              },
              "2023-04-02T06:00:00.000Z": {
                "metrics": {
                  "memoryRSS": {
                    "name": "memoryRSS",
                    "aggregation_info": {
                      "avg": 31.91,
                      "max": 123.6,
                      "min": 50.6,
                      "sum": 123.6,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "cpuUsage": {
                    "name": "cpuUsage",
                    "aggregation_info": {
                      "avg": 0.12,
                      "max": 0.84,
                      "min": 0.14,
                      "sum": 0.84,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "cpuThrottle": {
                    "name": "cpuThrottle",
                    "aggregation_info": {
                      "avg": 0.045,
                      "max": 0.09,
                      "sum": 0.19,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "cpuRequest": {
                    "name": "cpuRequest",
                    "aggregation_info": {
                      "avg": 5.37,
                      "sum": 16.11,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "memoryUsage": {
                    "name": "memoryUsage",
                    "aggregation_info": {
                      "avg": 40.1,
                      "max": 198.5,
                      "min": 50.6,
                      "sum": 198.5,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "memoryRequest": {
                    "name": "memoryRequest",
                    "aggregation_info": {
                      "avg": 50.21,
                      "sum": 250.85,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "cpuLimit": {
                    "name": "cpuLimit",
                    "aggregation_info": {
                      "avg": 0.5,
                      "sum": 2,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "memoryLimit": {
                    "name": "memoryLimit",
                    "aggregation_info": {
                      "avg": 100,
                      "sum": 500,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  }
                },
                "interval_start_time": "2023-04-02T05:45:00.000Z",
                "interval_end_time": "2023-04-02T06:00:00.000Z",
                "duration_in_minutes": 15,
                "duration_in_seconds": 900
              },
              "2023-04-02T07:45:00.000Z": {
                "metrics": {
                  "memoryRSS": {
                    "name": "memoryRSS",
                    "aggregation_info": {
                      "avg": 31.91,
                      "max": 123.6,
                      "min": 50.6,
                      "sum": 123.6,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "cpuUsage": {
                    "name": "cpuUsage",
                    "aggregation_info": {
                      "avg": 0.12,
                      "max": 0.84,
                      "min": 0.14,
                      "sum": 0.84,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "cpuThrottle": {
                    "name": "cpuThrottle",
                    "aggregation_info": {
                      "avg": 0.045,
                      "max": 0.09,
                      "sum": 0.19,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "cpuRequest": {
                    "name": "cpuRequest",
                    "aggregation_info": {
                      "avg": 5.37,
                      "sum": 16.11,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "memoryUsage": {
                    "name": "memoryUsage",
                    "aggregation_info": {
                      "avg": 40.1,
                      "max": 198.5,
                      "min": 50.6,
                      "sum": 198.5,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "memoryRequest": {
                    "name": "memoryRequest",
                    "aggregation_info": {
                      "avg": 50.21,
                      "sum": 250.85,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "cpuLimit": {
                    "name": "cpuLimit",
                    "aggregation_info": {
                      "avg": 0.5,
                      "sum": 2,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "memoryLimit": {
                    "name": "memoryLimit",
                    "aggregation_info": {
                      "avg": 100,
                      "sum": 500,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  }
                },
                "interval_start_time": "2023-04-02T07:30:00.000Z",
                "interval_end_time": "2023-04-02T07:45:00.000Z",
                "duration_in_minutes": 15,
                "duration_in_seconds": 900
              }
            }
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0"
          }
        }
      }
    ]
  }
]
```

**Request with results set to true, latest set to false and with experiment name parameter**

`GET /listExperiments`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listExperiments?results=true&latest=false&experiment_name=<experiment_name>`

Returns all the results of the specific experiment with no recommendations

**Response for experiment name - `quarkus-resteasy-kruize-min-http-response-time-db_0`**
```
[
  {
    "version": "1.0",
    "experiment_id": "f0007796e65c999d843bebd447c2fbaa6aaf9127c614da55e333cd6bdb628a74",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_0",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_0",
        "namespace": "default_0",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "results": {
              "2023-04-02T11:15:00.770Z": {
                "metrics": {
                  "memoryRSS": {
                    "name": "memoryRSS",
                    "aggregation_info": {
                      "avg": 31.91,
                      "max": 123.6,
                      "min": 50.6,
                      "sum": 123.6,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "cpuUsage": {
                    "name": "cpuUsage",
                    "aggregation_info": {
                      "min": 0.14,
                      "sum": 0.84,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "cpuThrottle": {
                    "name": "cpuThrottle",
                    "aggregation_info": {
                      "sum": 0.19,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "cpuRequest": {
                    "name": "cpuRequest",
                    "aggregation_info": {
                      "avg": 5.37,
                      "sum": 16.11,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "memoryUsage": {
                    "name": "memoryUsage",
                    "aggregation_info": {
                      "avg": 40.1,
                      "max": 198.5,
                      "min": 50.6,
                      "sum": 198.5,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "cpuLimit": {
                    "name": "cpuLimit",
                    "aggregation_info": {
                      "avg": 0.5,
                      "sum": 2,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "memoryLimit": {
                    "name": "memoryLimit",
                    "aggregation_info": {
                      "avg": 100,
                      "sum": 500,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  }
                },
                "interval_start_time": "2023-04-02T11:00:00.770Z",
                "interval_end_time": "2023-04-02T11:15:00.770Z",
                "duration_in_minutes": 15,
                "duration_in_seconds": 900
              },
              "2023-04-02T06:00:00.000Z": {
                "metrics": {
                  "memoryRSS": {
                    "name": "memoryRSS",
                    "aggregation_info": {
                      "avg": 31.91,
                      "max": 123.6,
                      "min": 50.6,
                      "sum": 123.6,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "cpuUsage": {
                    "name": "cpuUsage",
                    "aggregation_info": {
                      "avg": 0.12,
                      "max": 0.84,
                      "min": 0.14,
                      "sum": 0.84,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "cpuThrottle": {
                    "name": "cpuThrottle",
                    "aggregation_info": {
                      "avg": 0.045,
                      "max": 0.09,
                      "sum": 0.19,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "cpuRequest": {
                    "name": "cpuRequest",
                    "aggregation_info": {
                      "avg": 5.37,
                      "sum": 16.11,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "memoryUsage": {
                    "name": "memoryUsage",
                    "aggregation_info": {
                      "avg": 40.1,
                      "max": 198.5,
                      "min": 50.6,
                      "sum": 198.5,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "memoryRequest": {
                    "name": "memoryRequest",
                    "aggregation_info": {
                      "avg": 50.21,
                      "sum": 250.85,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "cpuLimit": {
                    "name": "cpuLimit",
                    "aggregation_info": {
                      "avg": 0.5,
                      "sum": 2,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "memoryLimit": {
                    "name": "memoryLimit",
                    "aggregation_info": {
                      "avg": 100,
                      "sum": 500,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  }
                },
                "interval_start_time": "2023-04-02T05:45:00.000Z",
                "interval_end_time": "2023-04-02T06:00:00.000Z",
                "duration_in_minutes": 15,
                "duration_in_seconds": 900
              },
              "2023-04-02T07:45:00.000Z": {
                "metrics": {
                  "memoryRSS": {
                    "name": "memoryRSS",
                    "aggregation_info": {
                      "avg": 31.91,
                      "max": 123.6,
                      "min": 50.6,
                      "sum": 123.6,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "cpuUsage": {
                    "name": "cpuUsage",
                    "aggregation_info": {
                      "avg": 0.12,
                      "max": 0.84,
                      "min": 0.14,
                      "sum": 0.84,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "cpuThrottle": {
                    "name": "cpuThrottle",
                    "aggregation_info": {
                      "avg": 0.045,
                      "max": 0.09,
                      "sum": 0.19,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "cpuRequest": {
                    "name": "cpuRequest",
                    "aggregation_info": {
                      "avg": 5.37,
                      "sum": 16.11,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "memoryUsage": {
                    "name": "memoryUsage",
                    "aggregation_info": {
                      "avg": 40.1,
                      "max": 198.5,
                      "min": 50.6,
                      "sum": 198.5,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "memoryRequest": {
                    "name": "memoryRequest",
                    "aggregation_info": {
                      "avg": 50.21,
                      "sum": 250.85,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "cpuLimit": {
                    "name": "cpuLimit",
                    "aggregation_info": {
                      "avg": 0.5,
                      "sum": 2,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "memoryLimit": {
                    "name": "memoryLimit",
                    "aggregation_info": {
                      "avg": 100,
                      "sum": 500,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  }
                },
                "interval_start_time": "2023-04-02T07:30:00.000Z",
                "interval_end_time": "2023-04-02T07:45:00.000Z",
                "duration_in_minutes": 15,
                "duration_in_seconds": 900
              }
            }
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0"
          }
        }
      }
    ]
  }
]
```

**Request with recommendations set to true**

`GET /listExperiments`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listExperiments?recommendations=true`

Returns the latest recommendations of all the experiments with no results

**Response for experiment name - `quarkus-resteasy-kruize-min-http-response-time-db_0`**
```
[
  {
    "version": "1.0",
    "experiment_id": "aecee67abcdea250c7cf93dc4d131d92a116ae46100107e7a08c5e6ea41e9e1a",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_2",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_2",
        "namespace": "default_2",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          }
        }
      }
    ]
  },
  {
    "version": "1.0",
    "experiment_id": "423c5efa0eb9978db4715cccdc67e03b36921e082f7f325e7ca3be378521bdb1",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_3",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_3",
        "namespace": "default_3",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          }
        }
      }
    ]
  },
  {
    "version": "1.0",
    "experiment_id": "f0007796e65c999d843bebd447c2fbaa6aaf9127c614da55e333cd6bdb628a74",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_0",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_0",
        "namespace": "default_0",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "Duration Based Recommendations Available"
                }
              ],
              "data": {
                "2023-04-02T13:30:00.680Z": {
                  "duration_based": {
                    "short_term": {
                      "monitoring_start_time": "2023-04-01T12:15:00.000Z",
                      "monitoring_end_time": "2023-04-02T13:30:00.680Z",
                      "duration_in_hours": 24,
                      "pods_count": 27,
                      "confidence_level": 0,
                      "config": {
                        "requests": {
                          "cpu": {
                            "amount": 0.9299999999999999,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 238.2,
                            "format": "MiB"
                          }
                        },
                        "limits": {
                          "cpu": {
                            "amount": 0.9299999999999999,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 238.2,
                            "format": "MiB"
                          }
                        }
                      },
                      "variation": {
                        "requests": {
                          "cpu": {
                            "amount": -4.44,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 187.98999999999998,
                            "format": "MiB"
                          }
                        },
                        "limits": {
                          "cpu": {
                            "amount": 0.42999999999999994,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 138.2,
                            "format": "MiB"
                          }
                        }
                      },
                      "notifications": []
                    },
                    "medium_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": [
                        {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation."
                        }
                      ]
                    },
                    "long_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": [
                        {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation."
                        }
                      ]
                    }
                  }
                }
              }
            }
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          }
        }
      }
    ]
  }
]
```

**Request with recommendations set to true with experiment name parameter**

`GET /listExperiments`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listExperiments?recommendations=true&experiment_name=<experiment_name>`

Returns the latest recommendations of the specified experiment with no results

**Response for experiment name - `quarkus-resteasy-kruize-min-http-response-time-db_0`**
```
[
  {
    "version": "1.0",
    "experiment_id": "f0007796e65c999d843bebd447c2fbaa6aaf9127c614da55e333cd6bdb628a74",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_0",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_0",
        "namespace": "default_0",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "Duration Based Recommendations Available"
                }
              ],
              "data": {
                "2023-04-02T13:30:00.680Z": {
                  "duration_based": {
                    "short_term": {
                      "monitoring_start_time": "2023-04-01T12:15:00.000Z",
                      "monitoring_end_time": "2023-04-02T13:30:00.680Z",
                      "duration_in_hours": 24.0,
                      "pods_count": 27,
                      "confidence_level": 0.0,
                      "config": {
                        "requests": {
                          "cpu": {
                            "amount": 0.9299999999999999,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 238.2,
                            "format": "MiB"
                          }
                        },
                        "limits": {
                          "cpu": {
                            "amount": 0.9299999999999999,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 238.2,
                            "format": "MiB"
                          }
                        }
                      },
                      "variation": {
                        "requests": {
                          "cpu": {
                            "amount": -4.44,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 187.98999999999998,
                            "format": "MiB"
                          }
                        },
                        "limits": {
                          "cpu": {
                            "amount": 0.42999999999999994,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 138.2,
                            "format": "MiB"
                          }
                        }
                      },
                      "notifications": []
                    },
                    "medium_term": {
                      "pods_count": 0,
                      "confidence_level": 0.0,
                      "notifications": [
                        {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation."
                        }
                      ]
                    },
                    "long_term": {
                      "pods_count": 0,
                      "confidence_level": 0.0,
                      "notifications": [
                        {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation."
                        }
                      ]
                    }
                  }
                }
              }
            }
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          }
        }
      }
    ]
  }
]
```

**Request with recommendations set to true and latest set to false**

`GET /listExperiments`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listExperiments?recommendations=true&latest=false`

Returns all the recommendations of all the experiments with no results

**Response for experiment name - `quarkus-resteasy-kruize-min-http-response-time-db_0`**
```
[
  {
    "version": "1.0",
    "experiment_id": "aecee67abcdea250c7cf93dc4d131d92a116ae46100107e7a08c5e6ea41e9e1a",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_2",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_2",
        "namespace": "default_2",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          }
        }
      }
    ]
  },
  {
    "version": "1.0",
    "experiment_id": "423c5efa0eb9978db4715cccdc67e03b36921e082f7f325e7ca3be378521bdb1",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_3",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_3",
        "namespace": "default_3",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          }
        }
      }
    ]
  },
  {
    "version": "1.0",
    "experiment_id": "f0007796e65c999d843bebd447c2fbaa6aaf9127c614da55e333cd6bdb628a74",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_0",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_0",
        "namespace": "default_0",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "Duration Based Recommendations Available"
                }
              ],
              "data": {
                "2023-04-02T11:15:00.770Z": {
                  "duration_based": {
                    "short_term": {
                      "monitoring_start_time": "2023-04-01T10:00:00.000Z",
                      "monitoring_end_time": "2023-04-02T11:15:00.770Z",
                      "duration_in_hours": 24,
                      "pods_count": 27,
                      "confidence_level": 0,
                      "config": {
                        "requests": {
                          "cpu": {
                            "amount": 0.9299999999999999,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 238.2,
                            "format": "MiB"
                          }
                        },
                        "limits": {
                          "cpu": {
                            "amount": 0.9299999999999999,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 238.2,
                            "format": "MiB"
                          }
                        }
                      },
                      "variation": {
                        "requests": {
                          "cpu": {
                            "amount": -4.44,
                            "format": "cores"
                          }
                        },
                        "limits": {
                          "cpu": {
                            "amount": 0.42999999999999994,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 138.2,
                            "format": "MiB"
                          }
                        }
                      },
                      "notifications": [
                        {
                          "type": "critical",
                          "message": "Memory Request Not Set"
                        }
                      ]
                    },
                    "medium_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": [
                        {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation."
                        }
                      ]
                    },
                    "long_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": [
                        {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation."
                        }
                      ]
                    }
                  }
                },
                "2023-04-02T06:00:00.000Z": {
                  "duration_based": {
                    "short_term": {
                      "monitoring_start_time": "2023-04-01T05:00:00.000Z",
                      "monitoring_end_time": "2023-04-02T06:00:00.000Z",
                      "duration_in_hours": 24,
                      "pods_count": 7,
                      "confidence_level": 0,
                      "config": {
                        "requests": {
                          "cpu": {
                            "amount": 0.9299999999999999,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 238.2,
                            "format": "MiB"
                          }
                        },
                        "limits": {
                          "cpu": {
                            "amount": 0.9299999999999999,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 238.2,
                            "format": "MiB"
                          }
                        }
                      },
                      "variation": {
                        "requests": {
                          "cpu": {
                            "amount": -4.44,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 187.98999999999998,
                            "format": "MiB"
                          }
                        },
                        "limits": {
                          "cpu": {
                            "amount": 0.42999999999999994,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 138.2,
                            "format": "MiB"
                          }
                        }
                      },
                      "notifications": []
                    },
                    "medium_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": [
                        {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation."
                        }
                      ]
                    },
                    "long_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": [
                        {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation."
                        }
                      ]
                    }
                  }
                },
                "2023-04-02T07:45:00.000Z": {
                  "duration_based": {
                    "short_term": {
                      "monitoring_start_time": "2023-04-01T06:30:00.000Z",
                      "monitoring_end_time": "2023-04-02T07:45:00.000Z",
                      "duration_in_hours": 24,
                      "pods_count": 7,
                      "confidence_level": 0,
                      "config": {
                        "requests": {
                          "cpu": {
                            "amount": 0.9299999999999999,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 238.2,
                            "format": "MiB"
                          }
                        },
                        "limits": {
                          "cpu": {
                            "amount": 0.9299999999999999,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 238.2,
                            "format": "MiB"
                          }
                        }
                      },
                      "variation": {
                        "requests": {
                          "cpu": {
                            "amount": -4.44,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 187.98999999999998,
                            "format": "MiB"
                          }
                        },
                        "limits": {
                          "cpu": {
                            "amount": 0.42999999999999994,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 138.2,
                            "format": "MiB"
                          }
                        }
                      },
                      "notifications": []
                    },
                    "medium_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": [
                        {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation."
                        }
                      ]
                    },
                    "long_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": [
                        {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation."
                        }
                      ]
                    }
                  }
                }
              }
            }
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          }
        }
      }
    ]
  }
]
```

**Request with recommendations set to true, latest set to false and with experiment name parameter**

`GET /listExperiments`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listExperiments?recommendations=true&latest=false&experiment_name=<experiment_name>`

Returns all the recommendations of the specified experiment with no results

**Response for experiment name - `quarkus-resteasy-kruize-min-http-response-time-db_0`**
```
[
  {
    "version": "1.0",
    "experiment_id": "f0007796e65c999d843bebd447c2fbaa6aaf9127c614da55e333cd6bdb628a74",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_0",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_0",
        "namespace": "default_0",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "Duration Based Recommendations Available"
                }
              ],
              "data": {
                "2023-04-02T11:15:00.770Z": {
                  "duration_based": {
                    "short_term": {
                      "monitoring_start_time": "2023-04-01T10:00:00.000Z",
                      "monitoring_end_time": "2023-04-02T11:15:00.770Z",
                      "duration_in_hours": 24,
                      "pods_count": 27,
                      "confidence_level": 0,
                      "config": {
                        "requests": {
                          "cpu": {
                            "amount": 0.9299999999999999,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 238.2,
                            "format": "MiB"
                          }
                        },
                        "limits": {
                          "cpu": {
                            "amount": 0.9299999999999999,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 238.2,
                            "format": "MiB"
                          }
                        }
                      },
                      "variation": {
                        "requests": {
                          "cpu": {
                            "amount": -4.44,
                            "format": "cores"
                          }
                        },
                        "limits": {
                          "cpu": {
                            "amount": 0.42999999999999994,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 138.2,
                            "format": "MiB"
                          }
                        }
                      },
                      "notifications": [
                        {
                          "type": "critical",
                          "message": "Memory Request Not Set"
                        }
                      ]
                    },
                    "medium_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": [
                        {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation."
                        }
                      ]
                    },
                    "long_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": [
                        {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation."
                        }
                      ]
                    }
                  }
                },
                "2023-04-02T06:00:00.000Z": {
                  "duration_based": {
                    "short_term": {
                      "monitoring_start_time": "2023-04-01T05:00:00.000Z",
                      "monitoring_end_time": "2023-04-02T06:00:00.000Z",
                      "duration_in_hours": 24,
                      "pods_count": 7,
                      "confidence_level": 0,
                      "config": {
                        "requests": {
                          "cpu": {
                            "amount": 0.9299999999999999,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 238.2,
                            "format": "MiB"
                          }
                        },
                        "limits": {
                          "cpu": {
                            "amount": 0.9299999999999999,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 238.2,
                            "format": "MiB"
                          }
                        }
                      },
                      "variation": {
                        "requests": {
                          "cpu": {
                            "amount": -4.44,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 187.98999999999998,
                            "format": "MiB"
                          }
                        },
                        "limits": {
                          "cpu": {
                            "amount": 0.42999999999999994,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 138.2,
                            "format": "MiB"
                          }
                        }
                      },
                      "notifications": []
                    },
                    "medium_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": [
                        {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation."
                        }
                      ]
                    },
                    "long_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": [
                        {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation."
                        }
                      ]
                    }
                  }
                },
                "2023-04-02T07:45:00.000Z": {
                  "duration_based": {
                    "short_term": {
                      "monitoring_start_time": "2023-04-01T06:30:00.000Z",
                      "monitoring_end_time": "2023-04-02T07:45:00.000Z",
                      "duration_in_hours": 24,
                      "pods_count": 7,
                      "confidence_level": 0,
                      "config": {
                        "requests": {
                          "cpu": {
                            "amount": 0.9299999999999999,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 238.2,
                            "format": "MiB"
                          }
                        },
                        "limits": {
                          "cpu": {
                            "amount": 0.9299999999999999,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 238.2,
                            "format": "MiB"
                          }
                        }
                      },
                      "variation": {
                        "requests": {
                          "cpu": {
                            "amount": -4.44,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 187.98999999999998,
                            "format": "MiB"
                          }
                        },
                        "limits": {
                          "cpu": {
                            "amount": 0.42999999999999994,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 138.2,
                            "format": "MiB"
                          }
                        }
                      },
                      "notifications": []
                    },
                    "medium_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": [
                        {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation."
                        }
                      ]
                    },
                    "long_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": [
                        {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation."
                        }
                      ]
                    }
                  }
                }
              }
            }
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          }
        }
      }
    ]
  }
]
```

**Request with recommendations set to true and also results set to true**

`GET /listExperiments`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listExperiments?recommendations=true&results=true`

Returns the latest recommendations and the results of all the experiments.

**Response for experiment name - `quarkus-resteasy-kruize-min-http-response-time-db_0`**
```
[
  {
    "version": "1.0",
    "experiment_id": "aecee67abcdea250c7cf93dc4d131d92a116ae46100107e7a08c5e6ea41e9e1a",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_2",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_2",
        "namespace": "default_2",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          }
        }
      }
    ]
  },
  {
    "version": "1.0",
    "experiment_id": "423c5efa0eb9978db4715cccdc67e03b36921e082f7f325e7ca3be378521bdb1",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_3",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_3",
        "namespace": "default_3",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          }
        }
      }
    ]
  },
  {
    "version": "1.0",
    "experiment_id": "f0007796e65c999d843bebd447c2fbaa6aaf9127c614da55e333cd6bdb628a74",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_0",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_0",
        "namespace": "default_0",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "results": {
              "2023-04-02T13:30:00.680Z": {
                "metrics": {
                  "memoryRSS": {
                    "name": "memoryRSS",
                    "aggregation_info": {
                      "avg": 31.91,
                      "max": 123.6,
                      "min": 50.6,
                      "sum": 123.6,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "cpuUsage": {
                    "name": "cpuUsage",
                    "aggregation_info": {
                      "avg": 0.12,
                      "max": 0.84,
                      "min": 0.14,
                      "sum": 0.84,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "cpuThrottle": {
                    "name": "cpuThrottle",
                    "aggregation_info": {
                      "avg": 0.045,
                      "max": 0.09,
                      "sum": 0.19,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "cpuRequest": {
                    "name": "cpuRequest",
                    "aggregation_info": {
                      "avg": 5.37,
                      "sum": 16.11,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "memoryUsage": {
                    "name": "memoryUsage",
                    "aggregation_info": {
                      "avg": 40.1,
                      "max": 198.5,
                      "min": 50.6,
                      "sum": 198.5,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "memoryRequest": {
                    "name": "memoryRequest",
                    "aggregation_info": {
                      "avg": 50.21,
                      "sum": 250.85,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "cpuLimit": {
                    "name": "cpuLimit",
                    "aggregation_info": {
                      "avg": 0.5,
                      "sum": 2.0,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "memoryLimit": {
                    "name": "memoryLimit",
                    "aggregation_info": {
                      "avg": 100.0,
                      "sum": 500.0,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  }
                },
                "interval_start_time": "2023-04-02T13:15:00.433Z",
                "interval_end_time": "2023-04-02T13:30:00.680Z",
                "duration_in_minutes": 15.004116666666667,
                "duration_in_seconds": 900.0
              }
            },
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "Duration Based Recommendations Available"
                }
              ],
              "data": {
                "2023-04-02T13:30:00.680Z": {
                  "duration_based": {
                    "short_term": {
                      "monitoring_start_time": "2023-04-01T12:15:00.000Z",
                      "monitoring_end_time": "2023-04-02T13:30:00.680Z",
                      "duration_in_hours": 24.0,
                      "pods_count": 27,
                      "confidence_level": 0.0,
                      "config": {
                        "requests": {
                          "cpu": {
                            "amount": 0.9299999999999999,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 238.2,
                            "format": "MiB"
                          }
                        },
                        "limits": {
                          "cpu": {
                            "amount": 0.9299999999999999,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 238.2,
                            "format": "MiB"
                          }
                        }
                      },
                      "variation": {
                        "requests": {
                          "cpu": {
                            "amount": -4.44,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 187.98999999999998,
                            "format": "MiB"
                          }
                        },
                        "limits": {
                          "cpu": {
                            "amount": 0.42999999999999994,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 138.2,
                            "format": "MiB"
                          }
                        }
                      },
                      "notifications": []
                    },
                    "medium_term": {
                      "pods_count": 0,
                      "confidence_level": 0.0,
                      "notifications": [
                        {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation."
                        }
                      ]
                    },
                    "long_term": {
                      "pods_count": 0,
                      "confidence_level": 0.0,
                      "notifications": [
                        {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation."
                        }
                      ]
                    }
                  }
                }
              }
            }
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          }
        }
      }
    ]
  },
  {
    "version": "1.0",
    "experiment_id": "ab0a31a522cebdde52561482300d078ed1448fa7b75834fa216677d1d9d5cda6",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_1",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_1",
        "namespace": "default_1",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          }
        }
      }
    ]
  },
  {
    "version": "1.0",
    "experiment_id": "ddd4316f32b0fa71e8a77ba59101edc884d382770efc63ab9af111a19e7c9112",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_8",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_8",
        "namespace": "default_8",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          }
        }
      }
    ]
  },
  {
    "version": "1.0",
    "experiment_id": "0b1caa47f1804ac420ea5edca8540cd5e8a6ceeebe2764b61f3a63ddb901faa4",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_9",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_9",
        "namespace": "default_9",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          }
        }
      }
    ]
  },
  {
    "version": "1.0",
    "experiment_id": "225944f23e56c973cbf4ca732b8dd4e64b2d7dda94a8822e756a988e9b5b2fed",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_6",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_6",
        "namespace": "default_6",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          }
        }
      }
    ]
  },
  {
    "version": "1.0",
    "experiment_id": "eb3202bb3c16da2590ecd3a49095b0b0833db2b340ab67727f70692dd0521f94",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_7",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_7",
        "namespace": "default_7",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          }
        }
      }
    ]
  },
  {
    "version": "1.0",
    "experiment_id": "c5e6ead71865bc519140054d992a7b32724cfc2e5f6e6bec8c29ea9b847f13ee",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_4",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_4",
        "namespace": "default_4",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          }
        }
      }
    ]
  },
  {
    "version": "1.0",
    "experiment_id": "d78690d0550fd56d7be735e358ca2c08b1a5165f728e407fdb514fc5cdce7746",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_5",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_5",
        "namespace": "default_5",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          }
        }
      }
    ]
  }
]
```

**Request with recommendations set to true and also results set to true with latest set to false**

`GET /listExperiments`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listExperiments?recommendations=true&results=true&latest=false`

Returns all the recommendations and all the results of all the experiments.

**Response for experiment name - `quarkus-resteasy-kruize-min-http-response-time-db_0`**
```
[
  {
    "version": "1.0",
    "experiment_id": "aecee67abcdea250c7cf93dc4d131d92a116ae46100107e7a08c5e6ea41e9e1a",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_2",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_2",
        "namespace": "default_2",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          }
        }
      }
    ]
  },
  {
    "version": "1.0",
    "experiment_id": "423c5efa0eb9978db4715cccdc67e03b36921e082f7f325e7ca3be378521bdb1",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_3",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_3",
        "namespace": "default_3",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          }
        }
      }
    ]
  },
  {
    "version": "1.0",
    "experiment_id": "f0007796e65c999d843bebd447c2fbaa6aaf9127c614da55e333cd6bdb628a74",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_0",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_0",
        "namespace": "default_0",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "results": {
              "2023-04-02T11:15:00.770Z": {
                "metrics": {
                  "memoryRSS": {
                    "name": "memoryRSS",
                    "aggregation_info": {
                      "avg": 31.91,
                      "max": 123.6,
                      "min": 50.6,
                      "sum": 123.6,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "cpuUsage": {
                    "name": "cpuUsage",
                    "aggregation_info": {
                      "min": 0.14,
                      "sum": 0.84,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "cpuThrottle": {
                    "name": "cpuThrottle",
                    "aggregation_info": {
                      "sum": 0.19,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "cpuRequest": {
                    "name": "cpuRequest",
                    "aggregation_info": {
                      "avg": 5.37,
                      "sum": 16.11,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "memoryUsage": {
                    "name": "memoryUsage",
                    "aggregation_info": {
                      "avg": 40.1,
                      "max": 198.5,
                      "min": 50.6,
                      "sum": 198.5,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "cpuLimit": {
                    "name": "cpuLimit",
                    "aggregation_info": {
                      "avg": 0.5,
                      "sum": 2,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "memoryLimit": {
                    "name": "memoryLimit",
                    "aggregation_info": {
                      "avg": 100,
                      "sum": 500,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  }
                },
                "interval_start_time": "2023-04-02T11:00:00.770Z",
                "interval_end_time": "2023-04-02T11:15:00.770Z",
                "duration_in_minutes": 15,
                "duration_in_seconds": 900
              },
              "2023-04-02T06:00:00.000Z": {
                "metrics": {
                  "memoryRSS": {
                    "name": "memoryRSS",
                    "aggregation_info": {
                      "avg": 31.91,
                      "max": 123.6,
                      "min": 50.6,
                      "sum": 123.6,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "cpuUsage": {
                    "name": "cpuUsage",
                    "aggregation_info": {
                      "avg": 0.12,
                      "max": 0.84,
                      "min": 0.14,
                      "sum": 0.84,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "cpuThrottle": {
                    "name": "cpuThrottle",
                    "aggregation_info": {
                      "avg": 0.045,
                      "max": 0.09,
                      "sum": 0.19,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "cpuRequest": {
                    "name": "cpuRequest",
                    "aggregation_info": {
                      "avg": 5.37,
                      "sum": 16.11,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "memoryUsage": {
                    "name": "memoryUsage",
                    "aggregation_info": {
                      "avg": 40.1,
                      "max": 198.5,
                      "min": 50.6,
                      "sum": 198.5,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "memoryRequest": {
                    "name": "memoryRequest",
                    "aggregation_info": {
                      "avg": 50.21,
                      "sum": 250.85,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "cpuLimit": {
                    "name": "cpuLimit",
                    "aggregation_info": {
                      "avg": 0.5,
                      "sum": 2,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "memoryLimit": {
                    "name": "memoryLimit",
                    "aggregation_info": {
                      "avg": 100,
                      "sum": 500,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  }
                },
                "interval_start_time": "2023-04-02T05:45:00.000Z",
                "interval_end_time": "2023-04-02T06:00:00.000Z",
                "duration_in_minutes": 15,
                "duration_in_seconds": 900
              },
              "2023-04-02T07:45:00.000Z": {
                "metrics": {
                  "memoryRSS": {
                    "name": "memoryRSS",
                    "aggregation_info": {
                      "avg": 31.91,
                      "max": 123.6,
                      "min": 50.6,
                      "sum": 123.6,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "cpuUsage": {
                    "name": "cpuUsage",
                    "aggregation_info": {
                      "avg": 0.12,
                      "max": 0.84,
                      "min": 0.14,
                      "sum": 0.84,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "cpuThrottle": {
                    "name": "cpuThrottle",
                    "aggregation_info": {
                      "avg": 0.045,
                      "max": 0.09,
                      "sum": 0.19,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "cpuRequest": {
                    "name": "cpuRequest",
                    "aggregation_info": {
                      "avg": 5.37,
                      "sum": 16.11,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "memoryUsage": {
                    "name": "memoryUsage",
                    "aggregation_info": {
                      "avg": 40.1,
                      "max": 198.5,
                      "min": 50.6,
                      "sum": 198.5,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "memoryRequest": {
                    "name": "memoryRequest",
                    "aggregation_info": {
                      "avg": 50.21,
                      "sum": 250.85,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "cpuLimit": {
                    "name": "cpuLimit",
                    "aggregation_info": {
                      "avg": 0.5,
                      "sum": 2,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "memoryLimit": {
                    "name": "memoryLimit",
                    "aggregation_info": {
                      "avg": 100,
                      "sum": 500,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  }
                },
                "interval_start_time": "2023-04-02T07:30:00.000Z",
                "interval_end_time": "2023-04-02T07:45:00.000Z",
                "duration_in_minutes": 15,
                "duration_in_seconds": 900
              }
            },
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "Duration Based Recommendations Available"
                }
              ],
              "data": {
                "2023-04-02T11:15:00.770Z": {
                  "duration_based": {
                    "short_term": {
                      "monitoring_start_time": "2023-04-01T10:00:00.000Z",
                      "monitoring_end_time": "2023-04-02T11:15:00.770Z",
                      "duration_in_hours": 24,
                      "pods_count": 27,
                      "confidence_level": 0,
                      "config": {
                        "requests": {
                          "cpu": {
                            "amount": 0.9299999999999999,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 238.2,
                            "format": "MiB"
                          }
                        },
                        "limits": {
                          "cpu": {
                            "amount": 0.9299999999999999,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 238.2,
                            "format": "MiB"
                          }
                        }
                      },
                      "variation": {
                        "requests": {
                          "cpu": {
                            "amount": -4.44,
                            "format": "cores"
                          }
                        },
                        "limits": {
                          "cpu": {
                            "amount": 0.42999999999999994,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 138.2,
                            "format": "MiB"
                          }
                        }
                      },
                      "notifications": [
                        {
                          "type": "critical",
                          "message": "Memory Request Not Set"
                        }
                      ]
                    },
                    "medium_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": [
                        {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation."
                        }
                      ]
                    },
                    "long_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": [
                        {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation."
                        }
                      ]
                    }
                  }
                },
                "2023-04-02T06:00:00.000Z": {
                  "duration_based": {
                    "short_term": {
                      "monitoring_start_time": "2023-04-01T05:00:00.000Z",
                      "monitoring_end_time": "2023-04-02T06:00:00.000Z",
                      "duration_in_hours": 24,
                      "pods_count": 7,
                      "confidence_level": 0,
                      "config": {
                        "requests": {
                          "cpu": {
                            "amount": 0.9299999999999999,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 238.2,
                            "format": "MiB"
                          }
                        },
                        "limits": {
                          "cpu": {
                            "amount": 0.9299999999999999,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 238.2,
                            "format": "MiB"
                          }
                        }
                      },
                      "variation": {
                        "requests": {
                          "cpu": {
                            "amount": -4.44,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 187.98999999999998,
                            "format": "MiB"
                          }
                        },
                        "limits": {
                          "cpu": {
                            "amount": 0.42999999999999994,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 138.2,
                            "format": "MiB"
                          }
                        }
                      },
                      "notifications": []
                    },
                    "medium_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": [
                        {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation."
                        }
                      ]
                    },
                    "long_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": [
                        {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation."
                        }
                      ]
                    }
                  }
                },
                "2023-04-02T07:45:00.000Z": {
                  "duration_based": {
                    "short_term": {
                      "monitoring_start_time": "2023-04-01T06:30:00.000Z",
                      "monitoring_end_time": "2023-04-02T07:45:00.000Z",
                      "duration_in_hours": 24,
                      "pods_count": 7,
                      "confidence_level": 0,
                      "config": {
                        "requests": {
                          "cpu": {
                            "amount": 0.9299999999999999,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 238.2,
                            "format": "MiB"
                          }
                        },
                        "limits": {
                          "cpu": {
                            "amount": 0.9299999999999999,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 238.2,
                            "format": "MiB"
                          }
                        }
                      },
                      "variation": {
                        "requests": {
                          "cpu": {
                            "amount": -4.44,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 187.98999999999998,
                            "format": "MiB"
                          }
                        },
                        "limits": {
                          "cpu": {
                            "amount": 0.42999999999999994,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 138.2,
                            "format": "MiB"
                          }
                        }
                      },
                      "notifications": []
                    },
                    "medium_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": [
                        {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation."
                        }
                      ]
                    },
                    "long_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": [
                        {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation."
                        }
                      ]
                    }
                  }
                }
              }
            }
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          }
        }
      }
    ]
  }
]
```

**Request with recommendations set to true and also results set to true with latest set to false and with experiment name parameter**

`GET /listExperiments`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listExperiments?recommendations=true&results=true&latest=false`

Returns all the recommendations and all the results of all the experiments.

**Response for experiment name - `quarkus-resteasy-kruize-min-http-response-time-db_0`**
```
[
  {
    "version": "1.0",
    "experiment_id": "f0007796e65c999d843bebd447c2fbaa6aaf9127c614da55e333cd6bdb628a74",
    "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_0",
    "cluster_name": "cluster-one-division-bell",
    "mode": "monitor",
    "target_cluster": "remote",
    "status": "IN_PROGRESS",
    "performance_profile": "resource-optimization-openshift",
    "trial_settings": {
      "measurement_duration": "15min"
    },
    "recommendation_settings": {
      "threshold": "0.1"
    },
    "experiment_usecase_type": {
      "remote_monitoring": true,
      "local_monitoring": false,
      "local_experiment": false
    },
    "validation_data": {
      "success": true,
      "message": "Registered successfully with Kruize! View registered experiments at /listExperiments",
      "errorCode": 201
    },
    "kubernetes_objects": [
      {
        "type": "deployment",
        "name": "tfb-qrh-deployment_0",
        "namespace": "default_0",
        "containers": {
          "tfb-server-1": {
            "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server-1",
            "results": {
              "2023-04-02T11:15:00.770Z": {
                "metrics": {
                  "memoryRSS": {
                    "name": "memoryRSS",
                    "aggregation_info": {
                      "avg": 31.91,
                      "max": 123.6,
                      "min": 50.6,
                      "sum": 123.6,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "cpuUsage": {
                    "name": "cpuUsage",
                    "aggregation_info": {
                      "min": 0.14,
                      "sum": 0.84,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "cpuThrottle": {
                    "name": "cpuThrottle",
                    "aggregation_info": {
                      "sum": 0.19,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "cpuRequest": {
                    "name": "cpuRequest",
                    "aggregation_info": {
                      "avg": 5.37,
                      "sum": 16.11,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "memoryUsage": {
                    "name": "memoryUsage",
                    "aggregation_info": {
                      "avg": 40.1,
                      "max": 198.5,
                      "min": 50.6,
                      "sum": 198.5,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "cpuLimit": {
                    "name": "cpuLimit",
                    "aggregation_info": {
                      "avg": 0.5,
                      "sum": 2,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "memoryLimit": {
                    "name": "memoryLimit",
                    "aggregation_info": {
                      "avg": 100,
                      "sum": 500,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  }
                },
                "interval_start_time": "2023-04-02T11:00:00.770Z",
                "interval_end_time": "2023-04-02T11:15:00.770Z",
                "duration_in_minutes": 15,
                "duration_in_seconds": 900
              },
              "2023-04-02T06:00:00.000Z": {
                "metrics": {
                  "memoryRSS": {
                    "name": "memoryRSS",
                    "aggregation_info": {
                      "avg": 31.91,
                      "max": 123.6,
                      "min": 50.6,
                      "sum": 123.6,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "cpuUsage": {
                    "name": "cpuUsage",
                    "aggregation_info": {
                      "avg": 0.12,
                      "max": 0.84,
                      "min": 0.14,
                      "sum": 0.84,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "cpuThrottle": {
                    "name": "cpuThrottle",
                    "aggregation_info": {
                      "avg": 0.045,
                      "max": 0.09,
                      "sum": 0.19,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "cpuRequest": {
                    "name": "cpuRequest",
                    "aggregation_info": {
                      "avg": 5.37,
                      "sum": 16.11,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "memoryUsage": {
                    "name": "memoryUsage",
                    "aggregation_info": {
                      "avg": 40.1,
                      "max": 198.5,
                      "min": 50.6,
                      "sum": 198.5,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "memoryRequest": {
                    "name": "memoryRequest",
                    "aggregation_info": {
                      "avg": 50.21,
                      "sum": 250.85,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "cpuLimit": {
                    "name": "cpuLimit",
                    "aggregation_info": {
                      "avg": 0.5,
                      "sum": 2,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "memoryLimit": {
                    "name": "memoryLimit",
                    "aggregation_info": {
                      "avg": 100,
                      "sum": 500,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  }
                },
                "interval_start_time": "2023-04-02T05:45:00.000Z",
                "interval_end_time": "2023-04-02T06:00:00.000Z",
                "duration_in_minutes": 15,
                "duration_in_seconds": 900
              },
              "2023-04-02T07:45:00.000Z": {
                "metrics": {
                  "memoryRSS": {
                    "name": "memoryRSS",
                    "aggregation_info": {
                      "avg": 31.91,
                      "max": 123.6,
                      "min": 50.6,
                      "sum": 123.6,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "cpuUsage": {
                    "name": "cpuUsage",
                    "aggregation_info": {
                      "avg": 0.12,
                      "max": 0.84,
                      "min": 0.14,
                      "sum": 0.84,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "cpuThrottle": {
                    "name": "cpuThrottle",
                    "aggregation_info": {
                      "avg": 0.045,
                      "max": 0.09,
                      "sum": 0.19,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "cpuRequest": {
                    "name": "cpuRequest",
                    "aggregation_info": {
                      "avg": 5.37,
                      "sum": 16.11,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "memoryUsage": {
                    "name": "memoryUsage",
                    "aggregation_info": {
                      "avg": 40.1,
                      "max": 198.5,
                      "min": 50.6,
                      "sum": 198.5,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "memoryRequest": {
                    "name": "memoryRequest",
                    "aggregation_info": {
                      "avg": 50.21,
                      "sum": 250.85,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  },
                  "cpuLimit": {
                    "name": "cpuLimit",
                    "aggregation_info": {
                      "avg": 0.5,
                      "sum": 2,
                      "format": "cores"
                    },
                    "percentile_results_available": false
                  },
                  "memoryLimit": {
                    "name": "memoryLimit",
                    "aggregation_info": {
                      "avg": 100,
                      "sum": 500,
                      "format": "MiB"
                    },
                    "percentile_results_available": false
                  }
                },
                "interval_start_time": "2023-04-02T07:30:00.000Z",
                "interval_end_time": "2023-04-02T07:45:00.000Z",
                "duration_in_minutes": 15,
                "duration_in_seconds": 900
              }
            },
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "Duration Based Recommendations Available"
                }
              ],
              "data": {
                "2023-04-02T11:15:00.770Z": {
                  "duration_based": {
                    "short_term": {
                      "monitoring_start_time": "2023-04-01T10:00:00.000Z",
                      "monitoring_end_time": "2023-04-02T11:15:00.770Z",
                      "duration_in_hours": 24,
                      "pods_count": 27,
                      "confidence_level": 0,
                      "config": {
                        "requests": {
                          "cpu": {
                            "amount": 0.9299999999999999,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 238.2,
                            "format": "MiB"
                          }
                        },
                        "limits": {
                          "cpu": {
                            "amount": 0.9299999999999999,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 238.2,
                            "format": "MiB"
                          }
                        }
                      },
                      "variation": {
                        "requests": {
                          "cpu": {
                            "amount": -4.44,
                            "format": "cores"
                          }
                        },
                        "limits": {
                          "cpu": {
                            "amount": 0.42999999999999994,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 138.2,
                            "format": "MiB"
                          }
                        }
                      },
                      "notifications": [
                        {
                          "type": "critical",
                          "message": "Memory Request Not Set"
                        }
                      ]
                    },
                    "medium_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": [
                        {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation."
                        }
                      ]
                    },
                    "long_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": [
                        {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation."
                        }
                      ]
                    }
                  }
                },
                "2023-04-02T06:00:00.000Z": {
                  "duration_based": {
                    "short_term": {
                      "monitoring_start_time": "2023-04-01T05:00:00.000Z",
                      "monitoring_end_time": "2023-04-02T06:00:00.000Z",
                      "duration_in_hours": 24,
                      "pods_count": 7,
                      "confidence_level": 0,
                      "config": {
                        "requests": {
                          "cpu": {
                            "amount": 0.9299999999999999,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 238.2,
                            "format": "MiB"
                          }
                        },
                        "limits": {
                          "cpu": {
                            "amount": 0.9299999999999999,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 238.2,
                            "format": "MiB"
                          }
                        }
                      },
                      "variation": {
                        "requests": {
                          "cpu": {
                            "amount": -4.44,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 187.98999999999998,
                            "format": "MiB"
                          }
                        },
                        "limits": {
                          "cpu": {
                            "amount": 0.42999999999999994,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 138.2,
                            "format": "MiB"
                          }
                        }
                      },
                      "notifications": []
                    },
                    "medium_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": [
                        {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation."
                        }
                      ]
                    },
                    "long_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": [
                        {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation."
                        }
                      ]
                    }
                  }
                },
                "2023-04-02T07:45:00.000Z": {
                  "duration_based": {
                    "short_term": {
                      "monitoring_start_time": "2023-04-01T06:30:00.000Z",
                      "monitoring_end_time": "2023-04-02T07:45:00.000Z",
                      "duration_in_hours": 24,
                      "pods_count": 7,
                      "confidence_level": 0,
                      "config": {
                        "requests": {
                          "cpu": {
                            "amount": 0.9299999999999999,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 238.2,
                            "format": "MiB"
                          }
                        },
                        "limits": {
                          "cpu": {
                            "amount": 0.9299999999999999,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 238.2,
                            "format": "MiB"
                          }
                        }
                      },
                      "variation": {
                        "requests": {
                          "cpu": {
                            "amount": -4.44,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 187.98999999999998,
                            "format": "MiB"
                          }
                        },
                        "limits": {
                          "cpu": {
                            "amount": 0.42999999999999994,
                            "format": "cores"
                          },
                          "memory": {
                            "amount": 138.2,
                            "format": "MiB"
                          }
                        }
                      },
                      "notifications": []
                    },
                    "medium_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": [
                        {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation."
                        }
                      ]
                    },
                    "long_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": [
                        {
                          "type": "info",
                          "message": "There is not enough data available to generate a recommendation."
                        }
                      ]
                    }
                  }
                }
              }
            }
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0",
            "recommendations": {
              "notifications": [
                {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation."
                }
              ],
              "data": {}
            }
          }
        }
      }
    ]
  }
]
```

## Recommendations

List recommendations output JSON as follows. Some parameters like CPU limit , ENV are optional.

**Attributes:**

| Param    | Possible options | Defaults | Description | 
| --- | --- | --- | --- |
|`experiment_name`|Any string|None|Passing Experiment Name as the parameter to the API returns the recommendation of the particular experiment if it exists|
|`latest`|`true`, `false`|`true`|Gets you the latest available recommendation if true, else returns all the recommendations|
|`monitoring_end_time`|Any valid timestamp*|None|Gets the recommendation of a particular timestamp if it exists|

`*valid timestamp is the same format as that used by the updateResults API`

**Request without Parameter**

`GET /listRecommendations`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listRecommendations`

If no parameter is passed API returns all the latest recommendations available for each experiment.

**Response**
```
[
    {
        "cluster_name": "cluster-one-division-bell",
        "kubernetes_objects": [
            {
                "type": "deployment",
                "name": "tfb-qrh-deployment_3",
                "namespace": "default_3",
                "containers": [
                    {
                        "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
                        "container_name": "tfb-server-1",
                        "recommendations": {
                            "notifications": [
                                {
                                    "type": "info",
                                    "message": "There is not enough data available to generate a recommendation."
                                }
                            ],
                            "data": {}
                        }
                    },
                    {
                        "container_image_name": "kruize/tfb-db:1.15",
                        "container_name": "tfb-server-0",
                        "recommendations": {
                            "notifications": [
                                {
                                    "type": "info",
                                    "message": "There is not enough data available to generate a recommendation."
                                }
                            ],
                            "data": {}
                        }
                    }
                ]
            }
        ],
        "version": "1.0",
        "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_3"
    },
    {
        "cluster_name": "cluster-one-division-bell",
        "kubernetes_objects": [
            {
                "type": "deployment",
                "name": "tfb-qrh-deployment_0",
                "namespace": "default_0",
                "containers": [
                    {
                        "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
                        "container_name": "tfb-server-1",
                        "recommendations": {
                            "notifications": [
                                {
                                    "type": "info",
                                    "message": "Duration Based Recommendations Available"
                                }
                            ],
                            "data": {
                                "2022-12-21T00:40:17.000Z": {
                                    "duration_based": {
                                        "short_term": {
                                            "monitoring_start_time": "2022-12-20T00:40:17.000Z",
                                            "monitoring_end_time": "2022-12-21T00:40:17.000Z",
                                            "duration_in_hours": 9.0,
                                            "pods_count": 0,
                                            "confidence_level": 0.0,
                                            "config": {
                                                "limits": {
                                                    "memory": {
                                                        "amount": 982.5997506234414,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 6.51,
                                                        "format": "cores"
                                                    }
                                                },
                                                "requests": {
                                                    "memory": {
                                                        "amount": 123.6,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 1.03,
                                                        "format": "cores"
                                                    }
                                                }
                                            },
                                            "notifications": []
                                        },
                                        "medium_term": {
                                            "pods_count": 0,
                                            "confidence_level": 0.0,
                                            "notifications": [
                                                {
                                                    "type": "info",
                                                    "message": "There is not enough data available to generate a recommendation."
                                                }
                                            ]
                                        },
                                        "long_term": {
                                            "pods_count": 0,
                                            "confidence_level": 0.0,
                                            "notifications": [
                                                {
                                                    "type": "info",
                                                    "message": "There is not enough data available to generate a recommendation."
                                                }
                                            ]
                                        }
                                    }
                                }
                            }
                        }
                    },
                    {
                        "container_image_name": "kruize/tfb-db:1.15",
                        "container_name": "tfb-server-0",
                        "recommendations": {
                            "notifications": [
                                {
                                    "type": "info",
                                    "message": "There is not enough data available to generate a recommendation."
                                }
                            ],
                            "data": {}
                        }
                    }
                ]
            }
        ],
        "version": "1.0",
        "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_0"
    }
]
```

**Request with experiment name parameter**

`GET /listRecommendations`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listRecommendations?experiment_name=<experiment_name>`

Returns the latest result of that experiment

**Response for experiment name - `quarkus-resteasy-kruize-min-http-response-time-db_0`**
```
[
    {
        "cluster_name": "cluster-one-division-bell",
        "kubernetes_objects": [
            {
                "type": "deployment",
                "name": "tfb-qrh-deployment_0",
                "namespace": "default_0",
                "containers": [
                    {
                        "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
                        "container_name": "tfb-server-1",
                        "recommendations": {
                            "notifications": [
                                {
                                    "type": "info",
                                    "message": "Duration Based Recommendations Available"
                                }
                            ],
                            "data": {
                                "2022-12-21T00:40:17.000Z": {
                                    "duration_based": {
                                        "short_term": {
                                            "monitoring_start_time": "2022-12-20T00:40:17.000Z",
                                            "monitoring_end_time": "2022-12-21T00:40:17.000Z",
                                            "duration_in_hours": 9.0,
                                            "pods_count": 0,
                                            "confidence_level": 0.0,
                                            "config": {
                                                "limits": {
                                                    "memory": {
                                                        "amount": 982.5997506234414,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 6.51,
                                                        "format": "cores"
                                                    }
                                                },
                                                "requests": {
                                                    "memory": {
                                                        "amount": 123.6,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 1.03,
                                                        "format": "cores"
                                                    }
                                                }
                                            },
                                            "notifications": []
                                        },
                                        "medium_term": {
                                            "pods_count": 0,
                                            "confidence_level": 0.0,
                                            "notifications": [
                                                {
                                                    "type": "info",
                                                    "message": "There is not enough data available to generate a recommendation."
                                                }
                                            ]
                                        },
                                        "long_term": {
                                            "pods_count": 0,
                                            "confidence_level": 0.0,
                                            "notifications": [
                                                {
                                                    "type": "info",
                                                    "message": "There is not enough data available to generate a recommendation."
                                                }
                                            ]
                                        }
                                    }
                                }
                            }
                        }
                    },
                    {
                        "container_image_name": "kruize/tfb-db:1.15",
                        "container_name": "tfb-server-0",
                        "recommendations": {
                            "notifications": [
                                {
                                    "type": "info",
                                    "message": "There is not enough data available to generate a recommendation."
                                }
                            ],
                            "data": {}
                        }
                    }
                ]
            }
        ],
        "version": "1.0",
        "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_0"
    }
]
```

**Request with experiment name parameter and latest set to false**

`GET /listRecommendations`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listRecommendations?experiment_name=<experiment_name>&latest=false`

Returns all the results of that experiment

**Response for experiment name - `quarkus-resteasy-kruize-min-http-response-time-db_0`**

```
[
    {
        "cluster_name": "cluster-one-division-bell",
        "kubernetes_objects": [
            {
                "type": "deployment",
                "name": "tfb-qrh-deployment_0",
                "namespace": "default_0",
                "containers": [
                    {
                        "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
                        "container_name": "tfb-server-1",
                        "recommendations": {
                            "notifications": [
                                {
                                    "type": "info",
                                    "message": "Duration Based Recommendations Available"
                                }
                            ],
                            "data": {
                                "2022-12-20T17:55:05.000Z": {
                                    "duration_based": {
                                        "short_term": {
                                            "monitoring_start_time": "2022-12-19T17:55:05.000Z",
                                            "monitoring_end_time": "2022-12-20T17:55:05.000Z",
                                            "duration_in_hours": 2.25,
                                            "pods_count": 0,
                                            "confidence_level": 0.0,
                                            "config": {
                                                "limits": {
                                                    "memory": {
                                                        "amount": 982.5997506234414,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 6.51,
                                                        "format": "cores"
                                                    }
                                                },
                                                "requests": {
                                                    "memory": {
                                                        "amount": 123.6,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 1.03,
                                                        "format": "cores"
                                                    }
                                                }
                                            },
                                            "notifications": []
                                        },
                                        "medium_term": {
                                            "pods_count": 0,
                                            "confidence_level": 0.0,
                                            "notifications": [
                                                {
                                                    "type": "info",
                                                    "message": "There is not enough data available to generate a recommendation."
                                                }
                                            ]
                                        },
                                        "long_term": {
                                            "pods_count": 0,
                                            "confidence_level": 0.0,
                                            "notifications": [
                                                {
                                                    "type": "info",
                                                    "message": "There is not enough data available to generate a recommendation."
                                                }
                                            ]
                                        }
                                    }
                                },
                                "2022-12-21T00:10:16.000Z": {
                                    "duration_based": {
                                        "short_term": {
                                            "monitoring_start_time": "2022-12-20T00:10:16.000Z",
                                            "monitoring_end_time": "2022-12-21T00:10:16.000Z",
                                            "duration_in_hours": 8.5,
                                            "pods_count": 0,
                                            "confidence_level": 0.0,
                                            "config": {
                                                "limits": {
                                                    "memory": {
                                                        "amount": 982.5997506234414,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 6.51,
                                                        "format": "cores"
                                                    }
                                                },
                                                "requests": {
                                                    "memory": {
                                                        "amount": 123.6,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 1.03,
                                                        "format": "cores"
                                                    }
                                                }
                                            },
                                            "notifications": []
                                        },
                                        "medium_term": {
                                            "pods_count": 0,
                                            "confidence_level": 0.0,
                                            "notifications": [
                                                {
                                                    "type": "info",
                                                    "message": "There is not enough data available to generate a recommendation."
                                                }
                                            ]
                                        },
                                        "long_term": {
                                            "pods_count": 0,
                                            "confidence_level": 0.0,
                                            "notifications": [
                                                {
                                                    "type": "info",
                                                    "message": "There is not enough data available to generate a recommendation."
                                                }
                                            ]
                                        }
                                    }
                                }
                            }
                        }
                    },
                    {
                        "container_image_name": "kruize/tfb-db:1.15",
                        "container_name": "tfb-server-0",
                        "recommendations": {
                            "notifications": [
                                {
                                    "type": "info",
                                    "message": "There is not enough data available to generate a recommendation."
                                }
                            ],
                            "data": {}
                        }
                    }
                ]
            }
        ],
        "version": "1.0",
        "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_0"
    }
]
```

**Request with experiment name parameter and monitoing end time set to a valid timestamp**

`GET /listRecommendations`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listRecommendations?experiment_name=<experiment_name>&monitoring_end_time=2022-12-20T17:55:05.000Z`

Returns the recommendation at a particular timestamp if it exists

**Response for experiment name - `quarkus-resteasy-kruize-min-http-response-time-db_0` and Monitoring End Time - `2022-12-20T17:55:05.000Z`**

```
[
    {
        "cluster_name": "cluster-one-division-bell",
        "kubernetes_objects": [
            {
                "type": "deployment",
                "name": "tfb-qrh-deployment_0",
                "namespace": "default_0",
                "containers": [
                    {
                        "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
                        "container_name": "tfb-server-1",
                        "recommendations": {
                            "notifications": [
                                {
                                    "type": "info",
                                    "message": "Duration Based Recommendations Available"
                                }
                            ],
                            "data": {
                                "2022-12-20T17:55:05.000Z": {
                                    "duration_based": {
                                        "short_term": {
                                            "monitoring_start_time": "2022-12-19T17:55:05.000Z",
                                            "monitoring_end_time": "2022-12-20T17:55:05.000Z",
                                            "duration_in_hours": 2.25,
                                            "pods_count": 0,
                                            "confidence_level": 0.0,
                                            "config": {
                                                "limits": {
                                                    "memory": {
                                                        "amount": 982.5997506234414,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 6.51,
                                                        "format": "cores"
                                                    }
                                                },
                                                "requests": {
                                                    "memory": {
                                                        "amount": 123.6,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 1.03,
                                                        "format": "cores"
                                                    }
                                                }
                                            },
                                            "notifications": []
                                        },
                                        "medium_term": {
                                            "pods_count": 0,
                                            "confidence_level": 0.0,
                                            "notifications": [
                                                {
                                                    "type": "info",
                                                    "message": "There is not enough data available to generate a recommendation."
                                                }
                                            ]
                                        },
                                        "long_term": {
                                            "pods_count": 0,
                                            "confidence_level": 0.0,
                                            "notifications": [
                                                {
                                                    "type": "info",
                                                    "message": "There is not enough data available to generate a recommendation."
                                                }
                                            ]
                                        }
                                    }
                                }
                            }
                        }
                    }
                ]
            }
        ],
        "version": "1.0",
        "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_0"
    }
]
```

### Invalid Scenarios:

**Invalid experiment name**

`experiment_name=stub-experiment`
```
{
    "message": "Given experiment name - \" stub-experiment \" is not valid",
    "httpcode": 400,
    "documentationLink": "",
    "status": "ERROR"
}
```

**Invalid Timestamp format**

`monitoring_end_time=Tony Stark` (Invalid Timestamp)
```
{
    "message": "Given timestamp - \" Tony Stark \" is not a valid timestamp format",
    "httpcode": 400,
    "documentationLink": "",
    "status": "ERROR"
}
```

**Non Existing Timestamp**

`monitoring_end_time=2022-12-20T17:55:07.000Z`
```
{
    "message": "Recommendation for timestamp - \" 2022-12-20T17:55:07.000Z \" does not exist",
    "httpcode": 400,
    "documentationLink": "",
    "status": "ERROR"
}
```
