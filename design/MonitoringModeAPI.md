# Remote Monitoring Mode

This article describes how to quickly get started with the Remote Monitoring Mode use case REST API using curl command.
Documentation still in progress stay tuned.

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
    "performanceProfile": "resource_optimization",
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
           "general_info": {
             "sum": 4.4,
             "avg": 1.1,
             "units": "cores"
           }
         }
       },
       "cpuLimit": {
         "results": {
           "general_info": {
             "sum": 2.0,
             "avg": 0.5,
             "units": "cores"
           }
         }
       },
       "cpuUsage": {
         "results": {
           "general_info": {
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
           "general_info": {
             "sum": 0.19,
             "max": 0.09,
             "avg": 0.045,
             "units": "cores"
           }
         }
       },
       "memoryRequest": {
         "results": {
           "general_info": {
             "sum": 250.85,
             "avg": 50.21,
             "units": "MiB"
           }
         }
       },
       "memoryLimit": {
         "results": {
           "general_info": {
             "sum": 500,
             "avg": 100,
             "units": "MiB"
           }
         }
       },
       "memoryUsage": {
         "results": {
           "general_info": {
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
           "general_info": {
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
        "2023-01-15 17:53:40.498": {
          "Short Term": {
            "monitoringStartTime": "jan 01, 2023, 5:53:40 PM",
            "monitoringEndTime": "Jan 02, 2023, 12:24:04 AM",
            "podsCount": 0,
            "confidence_level": 0.0,
            "config": {
              "max": {
                "memory": {
                  "amount": 0.0
                },
                "cpu": {
                  "amount": 0.0
                }
              },
              "capacity": {
                "memory": {
                  "amount": 0.0
                },
                "cpu": {
                  "amount": 0.0
                }
              }
            }
          },
          "Medium Term": {
            "monitoringStartTime": "jan 01, 2023, 5:53:40 PM",
            "monitoringEndTime": "jan 07, 2023, 12:24:04 AM",
            "podsCount": 0,
            "confidence_level": 0.0,
            "config": {
              "max": {
                "memory": {
                  "amount": 0.0
                },
                "cpu": {
                  "amount": 0.0
                }
              },
              "capacity": {
                "memory": {
                  "amount": 0.0
                },
                "cpu": {
                  "amount": 0.0
                }
              }
            }
          },
          "Long Term": {
            "monitoringStartTime": "jan 01, 2023, 5:53:40 PM",
            "monitoringEndTime": "jan 15, 2023, 12:24:04 AM",
            "podsCount": 0,
            "confidence_level": 0.0,
            "config": {
              "max": {
                "memory": {
                  "amount": 0.0
                },
                "cpu": {
                  "amount": 0.0
                }
              },
              "capacity": {
                "memory": {
                  "amount": 0.0
                },
                "cpu": {
                  "amount": 0.0
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
        "2029-10-26 17:53:40.498": {
          "Short Term": {
            "monitoringStartTime": "jan 01, 2023, 5:53:40 PM",
            "monitoringEndTime": "jan 01, 2023, 12:24:04 AM",
            "podsCount": 0,
            "confidence_level": 0.0,
            "config": {
              "max": {
                "memory": {
                  "amount": 0.0
                },
                "cpu": {
                  "amount": 0.0
                }
              },
              "capacity": {
                "memory": {
                  "amount": 0.0
                },
                "cpu": {
                  "amount": 0.0
                }
              }
            }
          },
          "Mid Term": {
            "monitoringStartTime": "jan 01, 2023, 5:53:40 PM",
            "monitoringEndTime": "jan 07, 2023, 12:24:04 AM",
            "podsCount": 0,
            "confidence_level": 0.0,
            "config": {
              "max": {
                "memory": {
                  "amount": 0.0
                },
                "cpu": {
                  "amount": 0.0
                }
              },
              "capacity": {
                "memory": {
                  "amount": 0.0
                },
                "cpu": {
                  "amount": 0.0
                }
              }
            }
          },
          "Long Term": {
            "monitoringStartTime": "jan 01, 2023, 5:53:40 PM",
            "monitoringEndTime": "jan 30, 2023, 12:24:04 AM",
            "podsCount": 0,
            "confidence_level": 0.0,
            "config": {
              "max": {
                "memory": {
                  "amount": 0.0
                },
                "cpu": {
                  "amount": 0.0
                }
              },
              "capacity": {
                "memory": {
                  "amount": 0.0
                },
                "cpu": {
                  "amount": 0.0
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
