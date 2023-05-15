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
      "threshold": 0.1
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
