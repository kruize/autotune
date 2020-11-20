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
        "application_name": "app1",
        "sla": "response_time",
        "namespace" :"NAMESPACE",
        "constraints": {
            "include": "",
            "exclude": "",
            "direction": ""
        },
        "application_tunables": [
            {
                "layer": "container",
                "level": "0",
                "layer_details": "generic container optimizations",
                "layer_tunables": [
                    {
                        "label": "CURRENT_CPU_VALUE",
                        "query": "cpu_process_time_seconds(app=\"app1\")",
                        "details": "CPU constraint information",
                        "value_type": "double",
                        "tunables": [
                            {
                                "name": "cpuRequest",
                                "upper_bound": "NA",
                                "lower_bound": "NA"        
                            },
                            {
                                "name": "cpuThrottling",
                                "upper_bound": "60ms",
                                "lower_bound": "30ms"        
                            },
                            {
                                "name": "cpuLimit",
                                "upper_bound": "NA",
                                "lower_bound": "NA"        
                            }
                        ]
                    }
                ]
            },
            {
                "layer": "runtime",
                "level": "1",
                "layer_details": "java",
                "layer_tunables": [
                    {
                        "label": "GC_POLICY",
                        "query": "jvm_gc_policy",
                        "details": "GC policy for the application",
                        "value_type": "string",
                        "tunables": [
                            {
                                "name": "gcPolicy",
                                "upper_bound": "NA",
                                "lower_bound": "NA"
                            }
                        ]
                    },
                    {
                        "query": "jvm_memory_heap",
                        "details": "heap size of the java application",
                        "value_type": "double",
                        "tunables": [
                            {
                                "name": "javaHeap",
                                "upper_bound": "NA",
                                "lower_bound": "NA"
                            },
                            {
                                "name": "gcPolicy",
                                "upper_bound": "NA",
                                "lower_bound": "NA"
                            }
                        ]
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
        "name": "acmeair-autotuning",
        "sla": "SLA",
        "services":[
            {
               "name": "msA",
               "layer1": "container",
               "layer2": "openj9",
               "layer3": ""

            },
            {
                "name": "msB",
                "layer1": "container",
                "layer2": "hotspot",
                "layer3": "spring"
 
            },
            {
                "name": "dbA",
                "layer1": "container",
                "layer2": "",
                "layer3": ""
 
            }
        ]
    },
    {
        "name": "petclinic-autotuning",
        "sla": "SLA",
        "services":[
            {
               "name": "msC",
               "layer1": "container",
               "layer2": "hotspot",
               "layer3": "openliberty"

            },
            {
                "name": "msB",
                "layer1": "container",
                "layer2": "hotspot",
                "layer3": "spring"
 
            },
            {
                "name": "dbA",
                "layer1": "container",
                "layer2": "",
                "layer3": ""
 
            }
        ]
    }
]
```
Tuning set A:
```
[
    {
        "name": "acmeair-autotuning",
        "sla": "SLA",
        "namespace" :"NAMESPACE", `if not accessible, fail`
        "constraints": {
            "include": "",
            "exclude": "",
            "direction": ""
        },
        "services":[
            "msA",
            "msB",
            "msC",
            "msD",
            "dbA",
            "dbB"
        ]
    },
    {
        "name": "petclinic-autotuning",
        "sla": "SLA",
        "services":[
            "msA",
            "msB",
            "msC",
            "msD",
            "dbA",
            "dbB"
        ]
    }
]
```
##  ListTunables
Get the tunables supported by autotune for the SLA.

**Request**
`GET /listTunables?sla=<SLA>` gives all tunables for the SLA

`GET /listTunables?sla=<SLA>&layer=<LAYER>` gives tunables for the SLA and the layer

`GET /listTunables?sla=<SLA>&layer=<LAYER>&type=<TYPE>` gives tunables for the SLA and the type for the layer.

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listTunables?sla=<SLA>`

**Response**
```
{
    "sla": "SLA",
    "application_tunables": [
        {
            "layer": "container",
            "level": "0",
            "layer_tunables": [
                {
                    "type": "container",
                    "tunables": [
                        {
                            "tunable": "CPU_Request",
                            "details": "CPU requests of the application"
                        },
                        {
                            "tunable": "CPU_Limit",
                            "details": "CPU limit of the application"
                        }        
                    ]
                }
            ]
        },
        {
            "layer": "runtime",
            "level": "1",
            "layer_tunables":  [
                {
                    "type": "java",
                    "tunables": [
                        {
                            "tunable": "HEAP_SIZE",
                            "details": "Java heap size of the application"
                        }        
                    ]
                },
                {
                    "type": "python",
                    "tunables": [
                        {
                            "tunable": "NUMBER_OF_THREADS",
                            "details": ""
                        }
                    ]
                }
            ]
        }
    ] 
}
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
