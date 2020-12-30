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
        "sla_class": "Response Time",
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
        "sla_class": "Throughput",
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

##  getAllTunables
Returns the JSON array response containing all the applications along with their tunables for the SLA class.

**Request**
`GET /getTunables` gives the tuning set C for all the applications monitored.

`GET /getTunables?set=<A|B|C>` gives the tuning set A for all the applications monitored.

`GET /getTunables?application_name=<APPLICATION_NAME>` for getting the tuning set C of a specific application.

`GET /getTunables?application_name=<APPLICATION_NAME>&layer_name='container'` for getting tunables of a specific layer for the application.

`curl -H 'Accept: application/json' http://<URL>:<PORT>/getTunables?application_name=<APPLICATION_NAME>&layer_name='container'`

**Response**
Tuning Set C:
```
[
  {
    "application_name": "petclinic-deployment-6d4c8678d4-jmz8x",
    "namespace": "default",
    "sla_class": "response_time",
    "application_tunables": [
      {
        "layer_name": "container",
        "layer_level": 0,
        "layer_details": "generic container tunables",
        "tunables": [
          {
            "name": "memoryLimit",
            "upper_bound": "300M",
            "lower_bound": "150M",
            "value_type": "double",
            "query_url": "http://prometheus:9090/container_memory_working_set_bytes{container=\"\", pod_name=\"petclinic-deployment-6d4c8678d4-jmz8x\"}"
          },
          {
            "name": "memoryRequests",
            "upper_bound": "300M",
            "lower_bound": "150M",
            "value_type": "double",
            "query_url": "http://prometheus:9090/container_memory_working_set_bytes{container=\"\", pod_name=\"petclinic-deployment-6d4c8678d4-jmz8x\"}"
          },
          {
            "name": "cpuLimit",
            "upper_bound": "4.0",
            "lower_bound": "2.0",
            "value_type": "double",
            "query_url": "http://prometheus:9090/(container_cpu_usage_seconds_total{container!=\"POD\", pod_name=\"petclinic-deployment-6d4c8678d4-jmz8x\"}[1m])"
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
        "layer__name": "openj9",
        "layer_level": 1,
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
Tuning Set B:
```
[
  {
    "name": "petclinic-autotune",
    "namespace": "default",
    "sla_class": "response_time",
    "direction": "minimize",
    "services": [
      {
        "name": "petclinic-deployment-6d4c8678d4-jmz8x",
        "layers": [
          {
            "layer_name": "container",
            "layer_level": 0,
            "layer_details": "generic container tunables"
          },
          {
            "layer_name": "openj9",
            "layer_level": 1,
            "layer_details": "java openj9 tunables"
          }
        ]
      }
    ]
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
    "direction": "minimize",
    "services": [
      "petclinic-deployment-6d4c8678d4-jmz8x"
    ]
  }
]
```
##  ListTunables
Get the tunables supported by autotune for the SLA.

**Request**
`GET /listTunables?sla_class=<SLA_CLASS>` gives all tunables for the SLA class

`GET /listTunables?sla_class=<SLA_CLASS>&layer=<LAYER>` gives tunables for the SLA class and the layer

`GET /listTunables?sla_class=<SLA_CLASS>&layer_level=<LEVEL>` gives tunables for the SLA class and the level.

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listTunables?sla_class=<SLA_CLASS>`

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
    "application": "petclinic-deployment-6d4c8678d4-jmz8x",
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
Get the status of the dependency analyzer.

**Request**
`GET /health`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/health`

**Response**

```
Healthy
```
