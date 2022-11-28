# Monitoring mode REST API
The Monitoring mode REST API design is proposed as follows:

##  CreateExperiment
Create experiments  using input JSON as follows.

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

##  Update Metric Results
Update metric results using input JSON as follows.

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

##  ListRecommendations
List recommendations output JSON as follows.

**Request**
`GET /listRecommendations`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listRecommendations`

**Response**
```
{
  "quarkus-resteasy-autotune-min-http-response-time-db": {
    "config": {
      "container_name": "tfb-server",
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
