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
[{
    "experiment_name": "quarkus-resteasy-autotune-min-http-response-time-db",
    "namespace": "default",
    "deployment_name": "tfb-qrh-sample",
    "performanceProfile": "resource-optimization-openshift",
    "mode": "monitor",
    "targetCluster": "remote",
    "containers": [
        {
            "image": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server"
        }
    ],
    "trial_settings": {
        "measurement_duration": "15min"
    },
    "recommendation_settings": {
        "threshold": "0.1"
    }
}]
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

**Request**
`POST /updateResults`

`curl -H 'Accept: application/json' -X POST --data 'copy paste below JSON' http://<URL>:<PORT>/updateResults`

```
[{
 "experiment_name": "quarkus-resteasy-autotune-min-http-response-time-db",
 "start_timestamp": "2022-01-23T18:25:43.511Z",
 "end_timestamp": "2022-01-23T18:25:43.511Z",
 "deployments": [{
   "deployment_name": "tfb-qrh-sample",
   "namespace": "default",
   "pod_metrics": [],
   "containers": [{
     "image_name": "kruize/tfb-qrh:1.13.2.F_et17",
     "container_name": "tfb-server",
     "container_metrics": {
       "cpuRequest" : {
         "results": {
            "value" : 1.1,
            "units": "cores",
            "aggregation_info": {
               "sum": 4.4,
               "avg": 1.1,
               "units": "cores"
            }
         }
       },
       "cpuLimit": {
         "results": {
           "value" : 0.5,
           "units": "cores",
           "aggregation_info": {
             "sum": 2.0,
             "avg": 0.5,
             "units": "cores"
           }
         }
       },
       "cpuUsage": {
         "results": {
           "value" : 0.12,
           "units": "cores",
           "aggregation_info": {
             "min": 0.14,
             "max": 0.84,
             "sum": 0.84,
             "avg": 0.12,
             "units": "cores"
           }
         }
       },
       "cpuThrottle": {
         "results": {
           "value" : 0.045,
           "units": "cores",
           "aggregation_info": {
             "sum": 0.19,
             "max": 0.09,
             "avg": 0.045,
             "units": "cores"
           }
         }
       },
       "memoryRequest": {
         "results": {
           "value" : 50.12,
           "units": "MiB",
           "aggregation_info": {
             "sum": 250.85,
             "avg": 50.21,
             "units": "MiB"
           }
         }
       },
       "memoryLimit": {
         "results": {
           "value" : 100,
           "units": "MiB",
           "aggregation_info": {
             "sum": 500,
             "avg": 100,
             "units": "MiB"
           }
         }
       },
       "memoryUsage": {
         "results": {
           "value" : 40.1,
           "units": "MiB",
           "aggregation_info": {
             "min": 50.6,
             "max": 198.50,
             "sum": 198.50,
             "avg": 40.1,
             "units": "MiB"
           }
         }
       },
       "memoryRSS": {
         "results": {
           "value": 31.91,
           "units": "MiB",
           "aggregation_info": {
             "min": 50.6,
             "max": 123.6,
             "sum": 123.6,
             "avg": 31.91,
             "units": "MiB"
           }
         }
       }
     }
   }]
 }]
}]

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

**Request**
`GET /listRecommendations`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listRecommendations`

**Response**

```
{
  "experimentName": "quarkus-resteasy-autotune-min-http-response-time-db4",
  "namespace": "default",
  "deploymentName": "tfb-qrh-sample",
  "containers": {
    "tfb-server-1": {
      "name": "tfb-server-1",
      "recommendation": {
        "2022-01-23T18:25:43.511Z": {
          "Cost": {
            "monitoringStartTime": "2022-01-22T18:25:43.511Z",
            "monitoringEndTime": "2022-01-23T18:25:43.511Z",
            "podsCount": 4,
            "confidence_level": 0.0,
            "config": {
              "max": {
                "memory": {
                  "amount": 128.8,
                  "units": "MiB"
                },
                "cpu": {
                  "amount": 8.0,
                  "units": "cores"
                }
              },
              "capacity": {
                "memory": {
                  "amount": 100.0,
                  "units": "MiB"
                },
                "cpu": {
                  "amount": 4.0,
                  "units": "cores"
                }
              }
            }
          },
          "Balanced": {
            "monitoringStartTime": "2022-01-16T18:25:43.511Z",
            "monitoringEndTime": "2022-01-23T18:25:43.511Z",
            "podsCount": 0,
            "confidence_level": 0.0,
            "config": {
              "max": {
                "memory": {
                  "amount": 128.8,
                  "units": "MiB"
                },
                "cpu": {
                  "amount": 8.8,
                  "units": "cores"
                }
              },
              "capacity": {
                "memory": {
                  "amount": 1000,
                  "units": "MiB"
                },
                "cpu": {
                  "amount": 8.8,
                  "units": "cores"
                }
              }
            }
          },
          "Performance": {
            "monitoringStartTime": "2022-01-08T18:25:43.511Z",
            "monitoringEndTime": "2022-01-23T18:25:43.511Z",
            "podsCount": 0,
            "confidence_level": 0.0,
            "config": {
              "max": {
                "memory": {
                  "amount": 128.8,
                  "units": "MiB"
                },
                "cpu": {
                  "amount": 8.0,
                  "units": "cores"
                }
              },
              "capacity": {
                "memory": {
                  "amount": 1000.0,
                  "units": "MiB"
                },
                "cpu": {
                  "amount": 8.0,
                  "units": "cores"
                }
              }
            }
          }
        }
      }
    },
    "tfb-server-0": {
      "name": "tfb-server-0",
      "recommendation": {
        "2022-01-23T18:25:43.511Z": {
          "Cost": {
            "monitoringStartTime": "2022-01-22T18:25:43.511Z",
            "monitoringEndTime": "2022-01-23T18:25:43.511Z",
            "podsCount": 0,
            "confidence_level": 0.0,
            "config": {
              "max": {
                "memory": {
                  "amount": 128.8,
                  "units": "MiB"
                },
                "cpu": {
                  "amount": 8.8,
                  "units": "cores"
                }
              },
              "capacity": {
                "memory": {
                  "amount": 1000.0,
                  "units": "MiB"
                },
                "cpu": {
                  "amount": 8.0,
                  "units": "cores"
                }
              }
            }
          },
          "Balanced": {
            "monitoringStartTime": "2022-01-16T18:25:43.511Z",
            "monitoringEndTime": "2022-01-23T18:25:43.511Z",
            "podsCount": 0,
            "confidence_level": 0.0,
            "config": {
              "max": {
                "memory": {
                  "amount": 1000.0,
                  "units": "MiB"
                },
                "cpu": {
                  "amount": 6.0,
                  "units": "cores"
                }
              },
              "capacity": {
                "memory": {
                  "amount": 1000.0,
                  "units": "MiB"
                },
                "cpu": {
                  "amount": 6.0,
                  "units": "cores"
                }
              }
            }
          },
          "Performance": {
            "monitoringStartTime": "2022-01-08T18:25:43.511Z",
            "monitoringEndTime": "2022-01-23T18:25:43.511Z",
            "podsCount": 0,
            "confidence_level": 0.0,
            "config": {
              "max": {
                "memory": {
                  "amount": 1000.0,
                  "units": "MiB"
                },
                "cpu": {
                  "amount": 4.0,
                  "units": "cores"
                }
              },
              "capacity": {
                "memory": {
                  "amount": 1000.0,
                  "units": "MiB"
                },
                "cpu": {
                  "amount": 4.0,
                  "units": "cores"
                }
              }
            }
          }
        }
      }
    }
  }
}
```
