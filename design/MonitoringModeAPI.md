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
 "info": {"trial_info": {
   "trial_number": 98,
   "trial_timestamp": "yyyymmddhhmmss"
 }},
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
             "mean": 1.1,
             "units": "cores"
           }
         }
       },
       "cpuLimit": {
         "results": {
           "general_info": {
             "sum": 2.0,
             "mean": 0.5,
             "units": "cores"
           }
         }
       },
       "cpuUsage": {
         "results": {
           "general_info": {
             "max": 0.84,
             "mean": 0.12,
             "units": "cores"
           }
         }
       },
       "cpuThrottle": {
         "results": {
           "general_info": {
             "max": 0.09,
             "mean": 0.045,
             "units": "cores"
           }
         }
       },
       "memoryRequest": {
         "results": {
           "general_info": {
             "sum": 250.85,
             "mean": 50.21,
             "units": "MiB"
           }
         }
       },
       "memoryLimit": {
         "results": {
           "general_info": {
             "sum": 500,
             "mean": 100,
             "units": "MiB"
           }
         }
       },
       "memoryUsage": {
         "results": {
           "general_info": {
             "max": 198.50,
             "mean": 40.1,
             "units": "MiB"
           }
         }
       },
       "memoryRSS": {
         "results": {
           "general_info": {
             "max": 123.6,
             "mean": 31.91,
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
  "quarkus-resteasy-autotune-min-http-response-time-db": {
    "timestamp": "2022-01-23T18:25:43.511Z",
    "config": {
      "container_name": "tfb-server",
      "deployment_name": "tfb-qrh-deployment",
      "namespace": "default",
      "confidence_level": 0.88,
      "requests": {
        "memory": {
          "amount": "210.0",
          "format": "Mi",
          "additionalProperties": {
          }
        },
        "cpu": {
          "amount": "2.26",
          "format": "",
          "additionalProperties": {
          }
        }
      },
      "env": [
            {
              "name": "JDK_JAVA_OPTIONS",
              "additionalProperties": {},
              "value": " -XX:MaxRAMPercentage=70 -XX:+AllowParallelDefineClass -XX:MaxInlineLevel=21 -XX:+UseZGC -XX:+TieredCompilation -Dquarkus.thread-pool.queue-size=27 -Dquarkus.thread-pool.core-threads=9"
            }
      ],
      "limits": {
        "memory": {
          "amount": "210.0",
          "format": "Mi",
          "additionalProperties": {

          }
        },
        "cpu": {
          "amount": "2.26",
          "format": "",
          "additionalProperties": {
          }
        }
      }
    }
  }
}
```
