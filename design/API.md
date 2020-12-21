# Autotune REST API
The Dependency Analyzer REST API design is proposed as follows:

##  listApplications
Get the list of applications monitored by dependency analyzer, along with the layer information.

**Request**
`GET /listApplications`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listApplications`

**Response**

```
[
    {
        "application_name": "app1",
        "sla": "Response Time",
        "runtime" : "java",
        "framework" : "springboot"
    },
    {
        "application_name": "app2",
        "sla": "Throughput",
        "runtime" : "none",
        "framework" : "none"
    }
]
```

##  getAllTunables
Returns the JSON array response containing all the applications along with their tunables for the SLA.

**Request**
`GET /getTunables` gives the tuning set C for all the applications monitored.

`GET /getTunables?set=<A|B|C>` gives the tuning set A for all the applications monitored.

`GET /getTunables?application_name=<APPLICATION_NAME>` for getting the tuning set C of a specific application.

`GET /getTunables?application_name=<APPLICATION_NAME>&type='container'` for getting tunables of a specific type for the application.

`curl -H 'Accept: application/json' http://<URL>:<PORT>/getTunables?application_name=<APPLICATION_NAME>&type='container'`

**Response**
Tuning Set C:
```
[
  {
    "application_name": "petclinic-deployment-6d4c8678d4-jmz8x",
    "namespace": "default",
    "type": "response_time",
    "application_tunables": [
      {
        "level": 0,
        "layer_tunables": [
          {
            "value_type": "double",
            "query": "container_memory_working_set_bytes{container=\"\", pod_name=\"petclinic-deployment-6d4c8678d4-jmz8x\"}",
            "tunables": [
              {
                "lower_bound": "150M",
                "name": "memoryLimit",
                "upper_bound": "300M"
              },
              {
                "lower_bound": "150M",
                "name": "memoryRequests",
                "upper_bound": "300M"
              }
            ],
            "details": "Current RSS value"
          },
          {
            "value_type": "double",
            "query": "(container_cpu_usage_seconds_total{container!=\"POD\", pod_name=\"petclinic-deployment-6d4c8678d4-jmz8x\"}[1m])",
            "tunables": [
              {
                "lower_bound": "2.0",
                "name": "cpuLimit",
                "upper_bound": "4.0"
              },
              {
                "lower_bound": "1.0",
                "name": "cpuRequest",
                "upper_bound": "3.0"
              }
            ],
            "details": "Current CPU used"
          }
        ],
        "layer": "container",
        "layer_details": "generic container tunables"
      },
      {
        "level": 1,
        "layer_tunables": [
          {
            "value_type": "double",
            "query": "jvm_memory_used_bytes{area=\"heap\",id=\"nursery-allocate\", pod=petclinic-deployment-6d4c8678d4-jmz8x}",
            "tunables": [
              {
                "lower_bound": "100M",
                "name": "javaHeap",
                "upper_bound": "250M"
              }
            ],
            "details": "Current Nursery Heap"
          }
        ],
        "layer": "openj9",
        "layer_details": "java openj9 tunables"
      }
    ]
  }
]
```
Tuning Set B:
```
[
  {
    "name": "petclinic-autotune",
    "namespace": "default",
    "sla_class": "response_time",
    "services": [
      {
        "name": "petclinic-deployment-6d4c8678d4-jmz8x",
        "layers": [
          {
            "level": 0,
            "name": "container",
            "details": "generic container tunables"
          },
          {
            "level": 1,
            "name": "openj9",
            "details": "java openj9 tunables"
          }
        ]
      }
    ],
    "direction": "lower"
  }
]
```
Tuning set A:
```
[
  {
    "name": "petclinic-autotune",
    "namespace": "default",
    "sla_class": "response_time",
    "services": [
      "petclinic-deployment-6d4c8678d4-jmz8x"
    ],
    "direction": "lower"
  }
]
```
##  ListTunables
Get the tunables supported by autotune for the SLA.

**Request**
`GET /listTunables?sla=<SLA>` gives all tunables for the SLA

`GET /listTunables?sla=<SLA>&layer=<LAYER>` gives tunables for the SLA and the layer

`GET /listTunables?sla=<SLA>&layer_level=<LEVEL>` gives tunables for the SLA and the level type.

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listTunables?sla=<SLA>`

**Response**
```
[
  {
    "layer_level": 0,
    "tunables": [
      {
        "name": "memoryLimit",          
        "lower_bound": "150M",
        "upper_bound": "300M"
      },
      {
        "name": "memoryRequests",
        "lower_bound": "150M",
        "upper_bound": "300M"
      },
      {
        "name": "cpuLimit",
        "lower_bound": "2.0",
        "upper_bound": "4.0"
      },
      {
        "name": "cpuRequest",
        "lower_bound": "1.0",
        "upper_bound": "3.0"
      }
    ],
    "details": "generic container tunables",
    "layer_name": "container"
  },
  {
    "layer_level": 1,
    "tunables": [
      {
        "name": "javaHeap",
        "lower_bound": "100M",
        "upper_bound": "250M"
      }
    ],
    "details": "java openj9 tunables",
    "layer_name": "openj9"
  }
]
```
##  ListApplications
Get the list of applications, along with layer information for the application.

**Request**
`GET /listApplications` gives list of applications monitored by autotune, along with the individual detected layers of the application.

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listApplications`

**Response**
```
[
  {
    "application_name": "petclinic-deployment-6d4c8678d4-jmz8x",
    "layers": [
      {
        "level": 0,
        "name": "container",
        "details": "generic container tunables"
      },
      {
        "level": 1,
        "name": "openj9",
        "details": "java openj9 tunables"
      }
    ],
    "type": "response_time"
  }
]
```
##  SearchSpace
Generates the search space used for the analysis.

**Request**
`GET /searchSpace`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/searchSpace`

**Response**

```
[
  {
    "application": "petclinic-deployment-6d4c8678d4-jmz8x",
    "tunables": [
      {
        "value_type": "double",
        "lower_bound": "150M",
        "name": "memoryLimit",
        "upper_bound": "300M",
        "direction": "lower"
      },
      {
        "value_type": "double",
        "lower_bound": "150M",
        "name": "memoryRequests",
        "upper_bound": "300M",
        "direction": "lower"
      },
      {
        "value_type": "double",
        "lower_bound": "2.0",
        "name": "cpuLimit",
        "upper_bound": "4.0",
        "direction": "lower"
      },
      {
        "value_type": "double",
        "lower_bound": "1.0",
        "name": "cpuRequest",
        "upper_bound": "3.0",
        "direction": "lower"
      },
      {
        "value_type": "double",
        "lower_bound": "100M",
        "name": "javaHeap",
        "upper_bound": "250M",
        "direction": "lower"
      }
    ],
    "sla_class": "response_time"
  }
]
```

##  Health
Get the status of the dependency analyzer.

**Request**
`GET /health`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/health`

**Response**

```
Healthy
```
