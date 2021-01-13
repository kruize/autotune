# Autotune REST API
The Autotune REST API design is proposed as follows:

##  listApplications
Get the list of applications monitored by autotune.

**Request**
`GET /listApplications`

`GET /listApplications?application_name=<APP_NAME>`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listApplications?application_name=<APP_NAME>`

**Response**
```
[
    {
      "application_name": "app1",
      “objective_function”: “transaction_response_time”,
      "sla_class": "response_time",
      “direction”: “minimize”
    },
    {
      "application_name": "app2",
      “objective_function”: “performedChecks_total”,
      "sla_class": "throughput",
      “direction”: “maximize”
    }
]
```

## listAppLayers
Returns the list of applications monitored by autotune along with layers detected in the applications.

**Request**
`GET /listAppLayers`

`GET /listAppLayers?application_name=<APP_NAME>`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listAppLayers?application_name=<APP_NAME>`

**Response**
```
[
    {
        "application_name": "app1",
        “objective_function”: “transaction_response_time”,
        "sla_class": "response_time",
        “direction”: “minimize”
        "layers": [
          {
            "layer_level": 0,
            "layer_name": "container",
            "layer_details": "generic container tunables"
          },
          {
            "layer_level": 1,
            "layer_name": "openj9",
            "layer_details": "java openj9 tunables"
          }
        ]
    },
    {
        "application_name": "app2",
        “objective_function”: “performedChecks_total”,
        "sla_class": "throughput",
        “direction”: “maximize”
        "layers": [
          {
            "layer_level": 0,
            "layer_name": "container",
            "layer_details": "generic container tunables"
          },
          {
            "layer_level": 1,
            "layer_name": "hotspot",
            "layer_details": "java hotspot tunables"
          }
        ]
    }
]
```

##  listAppTunables
Returns the list of applications monitored by autotune along with their tunables.
**Request**
`GET /listAppTunables` gives the tunables and layer information for all the applications monitored by autotune.

`GET /listAppTunables?application_name=<APPLICATION_NAME>` for getting the tunables information of a specific application.

`GET /listAppTunables?application_name=<APPLICATION_NAME>&layer_name='<LAYER>'` for getting tunables of a specific layer for the application.

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listAppTunables?application_name=<APPLICATION_NAME>&layer_name='<LAYER>'`

**Response**
```
[
  {
      "application_name": "app1",
      “objective_function”: “transaction_response_time”,
      "sla_class": "response_time",
      “direction”: “minimize”
      "layers": [
        {
          "layer_level": 0,
          "layer_name": "container",
          "layer_details": "generic container tunables"
          "tunables": [
            {
                "name": "memoryRequest",
                "upper_bound": "300M",
                "lower_bound": "150M",
                "value_type": "double",
                "query_url": "http://prometheus:9090/container_memory_working_set_bytes{container=\"\", pod_name=\"petclinic-deployment-6d4c8678d4-jmz8x\"}"
            },
            {
                "name": "cpuRequest",
                "upper_bound": "3.0",
                "lower_bound": "1.0",
                "value_type": "double",
                "query_url": "http://prometheus:9090/(container_cpu_usage_seconds_total{container!=\"POD\", pod_name=\"petclinic-deployment-6d4c8678d4-jmz8x\"}[1m])"
            }
          ]
        },
        {
          "layer_level": 1,
          "layer_name": "openj9",
          "layer_details": "java openj9 tunables",
          "tunables": [
            {
              "name": "javaHeap",
              "upper_bound": "250M",
              "lower_bound": "100M",
              "value_type": "double",
              "query_url": "http://prometheus:9090/jvm_memory_used_bytes{area=\"heap\",id=\"nursery-allocate\", pod=petclinic-deployment-6d4c8678d4-jmz8x}"
            }
          ]
      }
    ]
  }
]
```
##  listAutotuneTunables
Get the tunables supported by autotune for the SLA.

**Request**
`GET /listAutotuneTunables` gives all tunables for all layers in the cluster

`GET /listAutotuneTunables?sla_class=<SLA_CLASS>` gives all tunables for the SLA class

`GET /listAutotuneTunables?sla_class=<SLA_CLASS>&layer_name=<LAYER>` gives tunables for the SLA class and the layer

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listAutotuneTunables?sla_class=<SLA_CLASS>`

**Response**
```
[
  {
    "layer_name": "container",
    "layer_level": 0,
    "layer_details": "generic container tunables",
    "tunables": [
      {
        "name": "memoryLimit",
        "value_type": "double",          
        "lower_bound": "150M",
        "upper_bound": "300M"
      },
      {
        "name": "memoryRequests",
        "value_type": "double",          
        "lower_bound": "150M",
        "upper_bound": "300M"
      },
      {
        "name": "cpuLimit",
        "value_type": "double",          
        "lower_bound": "2.0",
        "upper_bound": "4.0"
      },
      {
        "name": "cpuRequest",
        "value_type": "double",          
        "lower_bound": "1.0",
        "upper_bound": "3.0"
      }
    ]
  },
  {
    "layer_name": "openj9",
    "layer_level": 1,
    "layer_details": "java openj9 tunables",
    "tunables": [
      {
        "name": "javaHeap",
        "value_type": "double",          
        "lower_bound": "100M",
        "upper_bound": "250M"
      }
    ]
  }
]
```
##  SearchSpace
Generates the search space used for the analysis.

**Request**
`GET /searchSpace` gives the search space for all applications monitored.

`GET /searchSpace?application_name=<APPLICATION>` gives the search space for a specific application.

`curl -H 'Accept: application/json' http://<URL>:<PORT>/searchSpace`

**Response**

```
[
  {
    "application_name": "petclinic-deployment-6d4c8678d4-jmz8x",
    "objective_function": "transaction_response_time",
    "sla_class": "response_time",
    "direction": "minimize"
    "tunables": [
      {
        "name": "memoryLimit",
        "upper_bound": "300M",
        "lower_bound": "150M",
        "value_type": "double"
      },
      {
        "name": "memoryRequests",
        "upper_bound": "300M",
        "lower_bound": "150M",
        "value_type": "double"
      },
      {
        "name": "cpuLimit",
        "upper_bound": "4.0",
        "lower_bound": "2.0",
        "value_type": "double"
      },
      {
        "name": "cpuRequest",
        "upper_bound": "3.0",
        "lower_bound": "1.0",
        "value_type": "double"
      },
      {
        "name": "javaHeap",
        "upper_bound": "250M",
        "lower_bound": "100M",
        "value_type": "double"
      }
    ]
  }
]
```

##  Health
Get the status of autotune.

**Request**
`GET /health`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/health`

**Response**

```
Healthy
```
