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

**Eg:** after updating the results for time stamp `2022-01-23T18:25:43.511Z` you cannot add results previous to that
timestamp

* Mandatory parameters in the input JSON:

 ```
 cpuUsage, memoryUsage, memoryRSS
 ```

* Note: If the parameters `cpuRequest`, `cpuLimit`, `memoryRequest` and `memoryLimit` are missing, then they are assumed
  to not have been set for the container in question.

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

| Param             | Possible options | Defaults | Description                                                                                                                                 | 
|-------------------|------------------|----------|---------------------------------------------------------------------------------------------------------------------------------------------|
| `results`         | `true`, `false`  | `false`  | Passing results = true as the parameter to the API returns the latest results.                                                              |
| `latest`          | `true`, `false`  | `true`   | Gets you the latest available results or recommendation if true, else returns all the results or recommendations                            |
| `recommendations` | `true`, `false`  | `false`  | Passing recommendations = true as the parameter to the API returns the latest recommendations and no results                                |
| `experiment_name` | Any string       | None     | Passing Experiment Name as the parameter to the API returns the results or recommendation or both of the particular experiment if it exists |

**Request without Parameter**

`GET /listExperiments`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listExperiments`

If no parameter is passed API returns all the experiment details.

**Response**

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
  },
  ...
  ...
  ...
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

Returns the experiment details of the specified experiment
<br><br><br>
**Request with results set to true**

`GET /listExperiments`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listExperiments?results=true`

Returns the latest result of all the experiments

***Note : When we don't pass `latest` in the query URL, it takes as `true` by default.***

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
                  "memoryLimit": {
                    "name": "memoryLimit",
                    "aggregation_info": {
                      "avg": 100.0,
                      "sum": 500.0,
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
                  "cpuRequest": {
                    "name": "cpuRequest",
                    "aggregation_info": {
                      "avg": 5.37,
                      "sum": 16.11,
                      "format": "cores"
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
  },
  ...
  ...
  ...
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

<br>

**Request with results set to true and with experiment name parameter**

`GET /listExperiments`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listExperiments?results=true&experiment_name=<experiment_name>`

Returns the latest result of the specified experiment
<br><br>

**Request with results set to true and latest set to false**

`GET /listExperiments`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listExperiments?results=true&latest=false`

Returns all the results of all the experiments

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
                  "memoryLimit": {
                    "name": "memoryLimit",
                    "aggregation_info": {
                      "avg": 100,
                      "sum": 500,
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
                  "cpuRequest": {
                    "name": "cpuRequest",
                    "aggregation_info": {
                      "avg": 5.37,
                      "sum": 16.11,
                      "format": "cores"
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
                  }
                },
                "interval_start_time": "2023-04-02T13:15:00.433Z",
                "interval_end_time": "2023-04-02T13:30:00.680Z",
                "duration_in_minutes": 15.004116666666667,
                "duration_in_seconds": 900
              },
              ...
              ...
              ...
              "2022-12-20T15:55:03.000Z": {
                "metrics": {
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
                  "memoryLimit": {
                    "name": "memoryLimit",
                    "aggregation_info": {
                      "avg": 100,
                      "sum": 500,
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
                  "cpuRequest": {
                    "name": "cpuRequest",
                    "aggregation_info": {
                      "avg": 5.37,
                      "sum": 16.11,
                      "format": "cores"
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
                  }
                },
                "interval_start_time": "2022-12-20T15:40:02.000Z",
                "interval_end_time": "2022-12-20T15:55:03.000Z",
                "duration_in_minutes": 15.016666666666667,
                "duration_in_seconds": 901
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

<br>

**Request with results set to true, latest set to false and with experiment name parameter**

`GET /listExperiments`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listExperiments?results=true&latest=false&experiment_name=<experiment_name>`

Returns all the results of the specific experiment
<br><br>
**Request with recommendations set to true**

`GET /listExperiments`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listExperiments?recommendations=true`

Returns the latest recommendations of all the experiments

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
              "version" : "1.0",
              "notifications": {
                "112101": {
                    "type": "info",
                    "message": "Duration Based Recommendations Available",
                    "code": 112101
                }
              },
              "data": {
                "2023-04-02T08:00:00.680Z": {
                  "duration_based": {
                    "short_term": {
                      "monitoring_start_time": "2023-04-01T06:45:00.000Z",
                      "monitoring_end_time": "2023-04-02T08:00:00.680Z",
                      "duration_in_hours": 24.0,
                      "pods_count": 27,
                      "confidence_level": 0.0,
                      "current": {
                          "requests": {
                              "memory": {
                                  "amount": 490.93,
                                  "format": "MiB"
                              },
                              "cpu": {
                                  "amount": 1.46,
                                  "format": "cores"
                              }
                          },
                          "limits": {
                              "memory": {
                                  "amount": 712.21,
                                  "format": "MiB"
                              },
                              "cpu": {
                                  "amount": 1.54,
                                  "format": "cores"
                              }
                          }
                      },
                      "config": {
                          "requests": {
                              "memory": {
                                  "amount": 1197.9840000000002,
                                  "format": "MiB"
                              },
                              "cpu": {
                                  "amount": 7.68,
                                  "format": "cores"
                              }
                          },
                          "limits": {
                              "memory": {
                                  "amount": 1197.9840000000002,
                                  "format": "MiB"
                              },
                              "cpu": {
                                  "amount": 7.68,
                                  "format": "cores"
                              }
                          }
                      },
                      "variation": {
                          "requests": {
                              "memory": {
                                  "amount": 707.0540000000001,
                                  "format": "MiB"
                              },
                              "cpu": {
                                  "amount": 6.22,
                                  "format": "cores"
                              }
                          },
                          "limits": {
                              "memory": {
                                  "amount": 485.7740000000001,
                                  "format": "MiB"
                              },
                              "cpu": {
                                  "amount": 6.14,
                                  "format": "cores"
                              }
                          }
                      },
                      "notifications": {}
                    },
                    "medium_term": {
                      "pods_count": 0,
                      "confidence_level": 0.0,
                      "notifications": {
                        "120001": {
                            "type": "info",
                            "message": "There is not enough data available to generate a recommendation.",
                            "code": 120001
                        }
                      }
                    },
                    "long_term": {
                      "pods_count": 0,
                      "confidence_level": 0.0,
                      "notifications": {
                        "120001": {
                            "type": "info",
                            "message": "There is not enough data available to generate a recommendation.",
                            "code": 120001
                        }
                      }
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
              "version" : "1.0",
              "notifications": {
                "120001": {
                    "type": "info",
                    "message": "There is not enough data available to generate a recommendation.",
                    "code": 120001
                }
              },
              "data": {}
            }
          }
        }
      }
    ]
  },
  ...
  ...
  ...
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
              "version" : "1.0",
              "notifications": {
                "120001": {
                    "type": "info",
                    "message": "There is not enough data available to generate a recommendation.",
                    "code": 120001
                }
              },
              "data": {}
            }
          },
          "tfb-server-0": {
            "container_image_name": "kruize/tfb-db:1.15",
            "container_name": "tfb-server-0",
            "recommendations": {
              "version" : "1.0",
              "notifications": {
                "120001": {
                    "type": "info",
                    "message": "There is not enough data available to generate a recommendation.",
                    "code": 120001
                }
              },
              "data": {}
            }
          }
        }
      }
    ]
  }
]
```

<br><br>
**Request with recommendations set to true with experiment name parameter**

`GET /listExperiments`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listExperiments?recommendations=true&experiment_name=<experiment_name>`

Returns the latest recommendations of the specified experiment with no results
<br><br>

**Request with recommendations set to true and latest set to false**

`GET /listExperiments`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listExperiments?recommendations=true&latest=false`

Returns all the recommendations of all the experiments

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
              "version" : "1.0",
              "notifications": {
                "112101": {
                    "type": "info",
                    "message": "Duration Based Recommendations Available",
                    "code": 112101
                }
              },
              "data": {
                "2023-04-02T06:00:00.770Z": {
                  "duration_based": {
                    "short_term": {
                      "monitoring_start_time": "2023-04-01T04:45:00.000Z",
                      "monitoring_end_time": "2023-04-02T06:00:00.770Z",
                      "duration_in_hours": 24,
                      "pods_count": 27,
                      "confidence_level": 0,
                      "current": {
                          "requests": {
                              "memory": {
                                  "amount": 490.93,
                                  "format": "MiB"
                              },
                              "cpu": {
                                  "amount": 1.46,
                                  "format": "cores"
                              }
                          },
                          "limits": {
                              "memory": {
                                  "amount": 712.21,
                                  "format": "MiB"
                              },
                              "cpu": {
                                  "amount": 1.54,
                                  "format": "cores"
                              }
                          }
                      },
                      "config": {
                          "requests": {
                              "memory": {
                                  "amount": 1197.9840000000002,
                                  "format": "MiB"
                              },
                              "cpu": {
                                  "amount": 7.68,
                                  "format": "cores"
                              }
                          },
                          "limits": {
                              "memory": {
                                  "amount": 1197.9840000000002,
                                  "format": "MiB"
                              },
                              "cpu": {
                                  "amount": 7.68,
                                  "format": "cores"
                              }
                          }
                      },
                      "variation": {
                          "requests": {
                              "memory": {
                                  "amount": 707.0540000000001,
                                  "format": "MiB"
                              },
                              "cpu": {
                                  "amount": 6.22,
                                  "format": "cores"
                              }
                          },
                          "limits": {
                              "memory": {
                                  "amount": 485.7740000000001,
                                  "format": "MiB"
                              },
                              "cpu": {
                                  "amount": 6.14,
                                  "format": "cores"
                              }
                          }
                      },
                      "notifications": {}
                    },
                    "medium_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": {
                        "120001": {
                            "type": "info",
                            "message": "There is not enough data available to generate a recommendation.",
                            "code": 120001
                        }
                      }
                    },
                    "long_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": {
                        "120001": {
                            "type": "info",
                            "message": "There is not enough data available to generate a recommendation.",
                            "code": 120001
                        }
                      }
                    }
                  }
                },
                ...
                ...
                ...
                "2023-04-02T04:30:00.000Z": {
                  "duration_based": {
                    "short_term": {
                      "monitoring_start_time": "2023-04-01T03:15:00.000Z",
                      "monitoring_end_time": "2023-04-02T04:30:00.000Z",
                      "duration_in_hours": 24,
                      "pods_count": 27,
                      "confidence_level": 0,
                      "current": {
                          "requests": {
                              "memory": {
                                  "amount": 490.93,
                                  "format": "MiB"
                              },
                              "cpu": {
                                  "amount": 1.46,
                                  "format": "cores"
                              }
                          },
                          "limits": {
                              "memory": {
                                  "amount": 712.21,
                                  "format": "MiB"
                              },
                              "cpu": {
                                  "amount": 1.54,
                                  "format": "cores"
                              }
                          }
                      },
                      "config": {
                          "requests": {
                              "memory": {
                                  "amount": 1197.9840000000002,
                                  "format": "MiB"
                              },
                              "cpu": {
                                  "amount": 7.68,
                                  "format": "cores"
                              }
                          },
                          "limits": {
                              "memory": {
                                  "amount": 1197.9840000000002,
                                  "format": "MiB"
                              },
                              "cpu": {
                                  "amount": 7.68,
                                  "format": "cores"
                              }
                          }
                      },
                      "variation": {
                          "requests": {
                              "memory": {
                                  "amount": 707.0540000000001,
                                  "format": "MiB"
                              },
                              "cpu": {
                                  "amount": 6.22,
                                  "format": "cores"
                              }
                          },
                          "limits": {
                              "memory": {
                                  "amount": 485.7740000000001,
                                  "format": "MiB"
                              },
                              "cpu": {
                                  "amount": 6.14,
                                  "format": "cores"
                              }
                          }
                      },
                      "notifications": {}
                    },
                    "medium_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": {
                        "120001": {
                            "type": "info",
                            "message": "There is not enough data available to generate a recommendation.",
                            "code": 120001
                        }
                      }
                    },
                    "long_term": {
                      "pods_count": 0,
                      "confidence_level": 0,
                      "notifications": {
                        "120001": {
                            "type": "info",
                            "message": "There is not enough data available to generate a recommendation.",
                            "code": 120001
                        }
                      }
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
              "version" : "1.0",
              "notifications": {
                "120001": {
                    "type": "info",
                    "message": "There is not enough data available to generate a recommendation.",
                    "code": 120001
                }
              },
              "data": {}
            }
          }
        }
      }
    ]
  },
  ...
  ...
  ...
]
```

<br><br>
**Request with recommendations set to true, latest set to false and with experiment name parameter**

`GET /listExperiments`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listExperiments?recommendations=true&latest=false&experiment_name=<experiment_name>`

Returns all the recommendations of the specified experiment
<br><br>
**Request with recommendations set to true and also results set to true**

`GET /listExperiments`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listExperiments?recommendations=true&results=true`

Returns the latest recommendations and the results of all the experiments.
<br><br>
**Request with recommendations set to true and also results set to true with latest set to false**

`GET /listExperiments`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listExperiments?recommendations=true&results=true&latest=false`

Returns all the recommendations and all the results of all the experiments.
<br><br>
**Request with recommendations set to true and also results set to true with latest set to false and with experiment
name parameter**

`GET /listExperiments`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listExperiments?recommendations=true&results=true&latest=false`

Returns all the recommendations and all the results of the specified experiment.

## Recommendations

List recommendations output JSON as follows. Some parameters like CPU limit , ENV are optional.

**Attributes:**

| Param                 | Possible options     | Defaults | Description                                                                                                              | 
|-----------------------|----------------------|----------|--------------------------------------------------------------------------------------------------------------------------|
| `experiment_name`     | Any string           | None     | Passing Experiment Name as the parameter to the API returns the recommendation of the particular experiment if it exists |
| `latest`              | `true`, `false`      | `true`   | Gets you the latest available recommendation if true, else returns all the recommendations                               |
| `monitoring_end_time` | Any valid timestamp* | None     | Gets the recommendation of a particular timestamp if it exists                                                           |

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
                            "version" : "1.0",
                            "notifications": {
                                "120001": {
                                    "type": "info",
                                    "message": "There is not enough data available to generate a recommendation.",
                                    "code": 120001
                                }
                            },
                            "data": {}
                        }
                    },
                    {
                        "container_image_name": "kruize/tfb-db:1.15",
                        "container_name": "tfb-server-0",
                        "recommendations": {
                            "version" : "1.0",
                            "notifications": {
                                "120001": {
                                    "type": "info",
                                    "message": "There is not enough data available to generate a recommendation.",
                                    "code": 120001
                                }
                            },
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
                            "version" : "1.0",
                            "notifications": {
                                "112101": {
                                    "type": "info",
                                    "message": "Duration Based Recommendations Available",
                                    "code": 112101
                                }
                            },
                            "data": {
                                "2022-12-21T00:40:17.000Z": {
                                    "duration_based": {
                                        "short_term": {
                                            "monitoring_start_time": "2022-12-20T00:40:17.000Z",
                                            "monitoring_end_time": "2022-12-21T00:40:17.000Z",
                                            "duration_in_hours": 9.0,
                                            "pods_count": 0,
                                            "confidence_level": 0.0,
                                            "current": {
                                                "requests": {
                                                    "memory": {
                                                        "amount": 490.93,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 1.46,
                                                        "format": "cores"
                                                    }
                                                },
                                                "limits": {
                                                    "memory": {
                                                        "amount": 712.21,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 1.54,
                                                        "format": "cores"
                                                    }
                                                }
                                            },
                                            "config": {
                                                "requests": {
                                                    "memory": {
                                                        "amount": 1197.9840000000002,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 7.68,
                                                        "format": "cores"
                                                    }
                                                },
                                                "limits": {
                                                    "memory": {
                                                        "amount": 1197.9840000000002,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 7.68,
                                                        "format": "cores"
                                                    }
                                                }
                                            },
                                            "variation": {
                                                "requests": {
                                                    "memory": {
                                                        "amount": 707.0540000000001,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 6.22,
                                                        "format": "cores"
                                                    }
                                                },
                                                "limits": {
                                                    "memory": {
                                                        "amount": 485.7740000000001,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 6.14,
                                                        "format": "cores"
                                                    }
                                                }
                                            },
                                            "notifications": {}
                                        },
                                        "medium_term": {
                                            "pods_count": 0,
                                            "confidence_level": 0.0,
                                            "notifications": {
                                                "120001": {
                                                    "type": "info",
                                                    "message": "There is not enough data available to generate a recommendation.",
                                                    "code": 120001
                                                }
                                            }
                                        },
                                        "long_term": {
                                            "pods_count": 0,
                                            "confidence_level": 0.0,
                                            "notifications": {
                                                "120001": {
                                                    "type": "info",
                                                    "message": "There is not enough data available to generate a recommendation.",
                                                    "code": 120001
                                                }
                                            }
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
                            "version" : "1.0",
                            "notifications": {
                                "120001": {
                                    "type": "info",
                                    "message": "There is not enough data available to generate a recommendation.",
                                    "code": 120001
                                }
                            },
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
                            "version" : "1.0",
                            "notifications": {
                                "112101": {
                                    "type": "info",
                                    "message": "Duration Based Recommendations Available",
                                    "code": 112101
                                }
                            },
                            "data": {
                                "2022-12-21T00:40:17.000Z": {
                                    "duration_based": {
                                        "short_term": {
                                            "monitoring_start_time": "2022-12-20T00:40:17.000Z",
                                            "monitoring_end_time": "2022-12-21T00:40:17.000Z",
                                            "duration_in_hours": 9.0,
                                            "pods_count": 0,
                                            "confidence_level": 0.0,
                                            "current": {
                                                "requests": {
                                                    "memory": {
                                                        "amount": 490.93,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 1.46,
                                                        "format": "cores"
                                                    }
                                                },
                                                "limits": {
                                                    "memory": {
                                                        "amount": 712.21,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 1.54,
                                                        "format": "cores"
                                                    }
                                                }
                                            },
                                            "config": {
                                                "requests": {
                                                    "memory": {
                                                        "amount": 1197.9840000000002,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 7.68,
                                                        "format": "cores"
                                                    }
                                                },
                                                "limits": {
                                                    "memory": {
                                                        "amount": 1197.9840000000002,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 7.68,
                                                        "format": "cores"
                                                    }
                                                }
                                            },
                                            "variation": {
                                                "requests": {
                                                    "memory": {
                                                        "amount": 707.0540000000001,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 6.22,
                                                        "format": "cores"
                                                    }
                                                },
                                                "limits": {
                                                    "memory": {
                                                        "amount": 485.7740000000001,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 6.14,
                                                        "format": "cores"
                                                    }
                                                }
                                            },
                                            "notifications": {}
                                        },
                                        "medium_term": {
                                            "pods_count": 0,
                                            "confidence_level": 0.0,
                                            "notifications": {
                                                "120001": {
                                                    "type": "info",
                                                    "message": "There is not enough data available to generate a recommendation.",
                                                    "code": 120001
                                                }
                                            }
                                        },
                                        "long_term": {
                                            "pods_count": 0,
                                            "confidence_level": 0.0,
                                            "notifications": {
                                                "120001": {
                                                    "type": "info",
                                                    "message": "There is not enough data available to generate a recommendation.",
                                                    "code": 120001
                                                }
                                            }
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
                            "notifications": {
                                "120001": {
                                    "type": "info",
                                    "message": "There is not enough data available to generate a recommendation.",
                                    "code": 120001
                                }
                            },
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
                            "version" : "1.0",
                            "notifications": {
                                "112101": {
                                    "type": "info",
                                    "message": "Duration Based Recommendations Available",
                                    "code": 112101
                                }
                            },
                            "data": {
                                "2022-12-20T17:55:05.000Z": {
                                    "duration_based": {
                                        "short_term": {
                                            "monitoring_start_time": "2022-12-19T17:55:05.000Z",
                                            "monitoring_end_time": "2022-12-20T17:55:05.000Z",
                                            "duration_in_hours": 2.25,
                                            "pods_count": 0,
                                            "confidence_level": 0.0,
                                            "current": {
                                                "requests": {
                                                    "memory": {
                                                        "amount": 490.93,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 1.46,
                                                        "format": "cores"
                                                    }
                                                },
                                                "limits": {
                                                    "memory": {
                                                        "amount": 712.21,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 1.54,
                                                        "format": "cores"
                                                    }
                                                }
                                            },
                                            "config": {
                                                "requests": {
                                                    "memory": {
                                                        "amount": 1197.9840000000002,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 7.68,
                                                        "format": "cores"
                                                    }
                                                },
                                                "limits": {
                                                    "memory": {
                                                        "amount": 1197.9840000000002,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 7.68,
                                                        "format": "cores"
                                                    }
                                                }
                                            },
                                            "variation": {
                                                "requests": {
                                                    "memory": {
                                                        "amount": 707.0540000000001,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 6.22,
                                                        "format": "cores"
                                                    }
                                                },
                                                "limits": {
                                                    "memory": {
                                                        "amount": 485.7740000000001,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 6.14,
                                                        "format": "cores"
                                                    }
                                                }
                                            },
                                            "notifications": {}
                                        },
                                        "medium_term": {
                                            "pods_count": 0,
                                            "confidence_level": 0.0,
                                            "notifications": {
                                                "120001": {
                                                    "type": "info",
                                                    "message": "There is not enough data available to generate a recommendation.",
                                                    "code": 120001
                                                }
                                            }
                                        },
                                        "long_term": {
                                            "pods_count": 0,
                                            "confidence_level": 0.0,
                                            "notifications": {
                                                "120001": {
                                                    "type": "info",
                                                    "message": "There is not enough data available to generate a recommendation.",
                                                    "code": 120001
                                                }
                                            }
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
                                            "current": {
                                                "requests": {
                                                    "memory": {
                                                        "amount": 490.93,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 1.46,
                                                        "format": "cores"
                                                    }
                                                },
                                                "limits": {
                                                    "memory": {
                                                        "amount": 712.21,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 1.54,
                                                        "format": "cores"
                                                    }
                                                }
                                            },
                                            "config": {
                                                "requests": {
                                                    "memory": {
                                                        "amount": 1197.9840000000002,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 7.68,
                                                        "format": "cores"
                                                    }
                                                },
                                                "limits": {
                                                    "memory": {
                                                        "amount": 1197.9840000000002,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 7.68,
                                                        "format": "cores"
                                                    }
                                                }
                                            },
                                            "variation": {
                                                "requests": {
                                                    "memory": {
                                                        "amount": 707.0540000000001,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 6.22,
                                                        "format": "cores"
                                                    }
                                                },
                                                "limits": {
                                                    "memory": {
                                                        "amount": 485.7740000000001,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 6.14,
                                                        "format": "cores"
                                                    }
                                                }
                                            },
                                            "notifications": {}
                                        },
                                        "medium_term": {
                                            "pods_count": 0,
                                            "confidence_level": 0.0,
                                            "notifications": {
                                                "120001": {
                                                    "type": "info",
                                                    "message": "There is not enough data available to generate a recommendation.",
                                                    "code": 120001
                                                }
                                            }
                                        },
                                        "long_term": {
                                            "pods_count": 0,
                                            "confidence_level": 0.0,
                                            "notifications": {
                                                "120001": {
                                                    "type": "info",
                                                    "message": "There is not enough data available to generate a recommendation.",
                                                    "code": 120001
                                                }
                                            }
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
                            "version" : "1.0",
                            "notifications": {
                                "120001": {
                                    "type": "info",
                                    "message": "There is not enough data available to generate a recommendation.",
                                    "code": 120001
                                }
                            },
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

**Request with experiment name parameter and monitoring end time set to a valid timestamp**

`GET /listRecommendations`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listRecommendations?experiment_name=<experiment_name>&monitoring_end_time=2022-12-20T17:55:05.000Z`

Returns the recommendation at a particular timestamp if it exists

**Response for experiment name - `quarkus-resteasy-kruize-min-http-response-time-db_0` and Monitoring End
Time - `2022-12-20T17:55:05.000Z`**

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
                            "version" : "1.0",
                            "notifications": {
                                "112101": {
                                    "type": "info",
                                    "message": "Duration Based Recommendations Available",
                                    "code": 112101
                                }
                            },
                            "data": {
                                "2022-12-20T17:55:05.000Z": {
                                    "duration_based": {
                                        "short_term": {
                                            "monitoring_start_time": "2022-12-19T17:55:05.000Z",
                                            "monitoring_end_time": "2022-12-20T17:55:05.000Z",
                                            "duration_in_hours": 2.25,
                                            "pods_count": 0,
                                            "confidence_level": 0.0,
                                            "current": {
                                                "requests": {
                                                    "memory": {
                                                        "amount": 490.93,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 1.46,
                                                        "format": "cores"
                                                    }
                                                },
                                                "limits": {
                                                    "memory": {
                                                        "amount": 712.21,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 1.54,
                                                        "format": "cores"
                                                    }
                                                }
                                            },
                                            "config": {
                                                "requests": {
                                                    "memory": {
                                                        "amount": 1197.9840000000002,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 7.68,
                                                        "format": "cores"
                                                    }
                                                },
                                                "limits": {
                                                    "memory": {
                                                        "amount": 1197.9840000000002,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 7.68,
                                                        "format": "cores"
                                                    }
                                                }
                                            },
                                            "variation": {
                                                "requests": {
                                                    "memory": {
                                                        "amount": 707.0540000000001,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 6.22,
                                                        "format": "cores"
                                                    }
                                                },
                                                "limits": {
                                                    "memory": {
                                                        "amount": 485.7740000000001,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 6.14,
                                                        "format": "cores"
                                                    }
                                                }
                                            },
                                            "notifications": {}
                                        },
                                        "medium_term": {
                                            "pods_count": 0,
                                            "confidence_level": 0.0,
                                            "notifications": {
                                                "120001": {
                                                    "type": "info",
                                                    "message": "There is not enough data available to generate a recommendation.",
                                                    "code": 120001
                                                }
                                            }
                                        },
                                        "long_term": {
                                            "pods_count": 0,
                                            "confidence_level": 0.0,
                                            "notifications": {
                                                "120001": {
                                                    "type": "info",
                                                    "message": "There is not enough data available to generate a recommendation.",
                                                    "code": 120001
                                                }
                                            }
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
