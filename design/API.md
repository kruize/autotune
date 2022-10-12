# Autotune REST API

The Autotune REST API design is proposed as follows:

## listStacks

Get the list of application stacks monitored by autotune.

**Request**
`GET /listStacks`

`GET /listStacks?experiment_name=<EXPERIMENT_NAME>`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listStacks?experiment_name=<EXPERIMENT_NAME>`

**Response**

```
[
    {
        "experiment_name": "galaxies-autotune-min-http-response-time",
        "experiment_id": "7c07cf4db16adcf76bad79394c9e7df2f3b8d8e6942cfa3f7b254b5aec1299b0",
        "objective_function": "request_sum/request_count",
        "hpo_algo_impl": "optuna_tpe",
        "stacks": ["dinogun/galaxies:1.2-jdk-11.0.10_9"],
        "slo_class": "response_time",
        "direction": "minimize"
    },
    {
        "experiment_name": "petclinic-autotune-max-http-throughput",
        "experiment_id": "629ad1c575dd81576c98142aa6de9ddc241de0a9f008586923f200b3a6bc83c6",
        "objective_function": "request_count",
        "hpo_algo_impl": "optuna_tpe",
        "stacks": ["kruize/spring_petclinic:2.2.0-jdk-11.0.8-openj9-0.21.0"],
        "slo_class": "throughput",
        "direction": "maximize"
    }
]
```

## listStackLayers

Returns the list of application stacks monitored by autotune along with layers detected in the stacks.

**Request**
`GET /listStackLayers`

`GET /listStackLayers?experiment_name=<EXPERIMENT_NAME>`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listStackLayers?experiment_name=<EXPERIMENT_NAME>`

**Response**

```
[
    {
        "experiment_name": "galaxies-autotune-min-http-response-time",
        "experiment_id": "7c07cf4db16adcf76bad79394c9e7df2f3b8d8e6942cfa3f7b254b5aec1299b0",
        "objective_function": "request_sum/request_count",
        "hpo_algo_impl": "optuna_tpe",
        "stacks": [{
            "layers": [
                {
                    "layer_id": "af07fd998199bf2d57f95dc18f2cc2311b72f6de11e7e949b566fcdc5ecb443b",
                    "layer_level": 0,
                    "layer_name": "container",
                    "layer_details": "generic container tunables"
                },
                {
                    "layer_id": "63f4bd430913abffaa6c41a5e05015d5fea23134c99826470c904a7cfe56b40c",
                    "layer_level": 1,
                    "layer_name": "hotspot",
                    "layer_details": "hotspot tunables"
                },
                {
                    "layer_id": "3ec648860dd10049b2488f19ca6d80fc5b50acccdf4aafaedc2316c6eea66741",
                    "layer_level": 2,
                    "layer_name": "quarkus",
                    "layer_details": "quarkus tunables"
                }
            ],
            "stack_name": "dinogun/galaxies:1.2-jdk-11.0.10_9"
        }],
        "slo_class": "response_time",
        "direction": "minimize"
    },
    {
        "experiment_name": "petclinic-autotune-max-http-throughput",
        "experiment_id": "629ad1c575dd81576c98142aa6de9ddc241de0a9f008586923f200b3a6bc83c6",
        "objective_function": "request_count",
        "hpo_algo_impl": "optuna_tpe",
        "stacks": [{
            "layers": [
                {
                    "layer_id": "af07fd998199bf2d57f95dc18f2cc2311b72f6de11e7e949b566fcdc5ecb443b",
                    "layer_level": 0,
                    "layer_name": "container",
                    "layer_details": "generic container tunables"
                }
            ],
            "stack_name": "kruize/spring_petclinic:2.2.0-jdk-11.0.8-openj9-0.21.0"
        }],
        "slo_class": "throughput",
        "direction": "maximize"
    }
]
```

## listStackTunables

Returns the list of application stacks monitored by autotune along with their tunables.
**Request**
`GET /listStackTunables` gives the tunables and layer information for all the application stacks monitored by autotune.

`GET /listStackTunables?experiment_name=<EXPERIMENT_NAME>` for getting the tunables information of a specific
application.

`GET /listStackTunables?experiment_name=<EXPERIMENT_NAME>&layer_name=<LAYER>` for getting tunables of a specific layer
for the application.

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listStackTunables?experiment_name=<EXPERIMENT_NAME>`

**Response**

```
[
    {
        "experiment_name": "petclinic-autotune-max-http-throughput",
        "experiment_id": "629ad1c575dd81576c98142aa6de9ddc241de0a9f008586923f200b3a6bc83c6",
        "objective_function": "request_count",
        "hpo_algo_impl": "optuna_tpe",
        "stacks": [{
            "layers": [
                {
                    "layer_id": "af07fd998199bf2d57f95dc18f2cc2311b72f6de11e7e949b566fcdc5ecb443b",
                    "layer_level": 0,
                    "tunables": [
                        {
                            "value_type": "double",
                            "lower_bound": "150.0Mi",
                            "name": "memoryRequest",
                            "step": 1,
                            "query_url": "http://10.111.106.208:9090/api/v1/query?query=container_memory_working_set_bytes{container=\"\", pod=\"kruize/spring_petclinic:2.2.0-jdk-11.0.8-openj9-0.21.0\"}",
                            "upper_bound": "300.0Mi"
                        },
                        {
                            "value_type": "double",
                            "lower_bound": "1.0",
                            "name": "cpuRequest",
                            "step": 0.01,
                            "query_url": "http://10.111.106.208:9090/api/v1/query?query=(container_cpu_usage_seconds_total{container!=\"POD\", pod=\"kruize/spring_petclinic:2.2.0-jdk-11.0.8-openj9-0.21.0\"}[1m])",
                            "upper_bound": "3.0"
                        }
                    ],
                    "layer_name": "container",
                    "layer_details": "generic container tunables"
                }
            ],
            "stack_name": "kruize/spring_petclinic:2.2.0-jdk-11.0.8-openj9-0.21.0"
        }],
        "function_variables": [{
            "value_type": "double",
            "name": "request_count",
            "query_url": "http://10.111.106.208:9090/api/v1/query?query=rate(http_server_requests_seconds_count{exception=\"None\",method=\"GET\",outcome=\"SUCCESS\",status=\"200\",uri=\"/webjars/**\",}[1m])"
        }],
        "slo_class": "throughput",
        "direction": "maximize"
    }
]
```

## listAutotuneTunables

Get the tunables supported by autotune for the SLO.

**Request**
`GET /listAutotuneTunables` gives all tunables for all layers in the cluster

`GET /listAutotuneTunables?slo_class=<SLO_CLASS>` gives all tunables for the SLO class

`GET /listAutotuneTunables?slo_class=<SLO_CLASS>&layer_name=<LAYER>` gives tunables for the SLO class and the layer

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listAutotuneTunables?slo_class=<SLO_CLASS>`

**Response**

```
[
    {
        "layer_id": "af07fd998199bf2d57f95dc18f2cc2311b72f6de11e7e949b566fcdc5ecb443b",
        "layer_level": 0,
        "tunables": [
            {
                "value_type": "double",
                "lower_bound": "150.0Mi",
                "name": "memoryRequest",
                "step": 1,
                "query_url": "http://10.111.106.208:9090/api/v1/query?query=container_memory_working_set_bytes{container=\"\", pod=\"$POD$\"}",
                "upper_bound": "300.0Mi"
            },
            {
                "value_type": "double",
                "lower_bound": "1.0",
                "name": "cpuRequest",
                "step": 0.01,
                "query_url": "http://10.111.106.208:9090/api/v1/query?query=(container_cpu_usage_seconds_total{container!=\"POD\", pod=\"$POD$\"}[1m])",
                "upper_bound": "3.0"
            }
        ],
        "layer_name": "container",
        "layer_details": "generic container tunables"
    },
    {
        "layer_id": "63f4bd430913abffaa6c41a5e05015d5fea23134c99826470c904a7cfe56b40c",
        "layer_level": 1,
        "tunables": [{
            "value_type": "integer",
            "lower_bound": "9",
            "name": "MaxInlineLevel",
            "step": 1,
            "query_url": "http://10.111.106.208:9090/api/v1/query?query=jvm_memory_used_bytes{area=\"heap\", container=\"\", pod=\"$POD$\"}",
            "upper_bound": "50"
        }],
        "layer_name": "hotspot",
        "layer_details": "hotspot tunables"
    },
    {
        "layer_id": "3ec648860dd10049b2488f19ca6d80fc5b50acccdf4aafaedc2316c6eea66741",
        "layer_level": 2,
        "tunables": [
            {
                "value_type": "integer",
                "lower_bound": "1",
                "name": "quarkus.thread-pool.core-threads",
                "step": 1,
                "query_url": "none",
                "upper_bound": "10"
            },
            {
                "value_type": "integer",
                "lower_bound": "1",
                "name": "quarkus.thread-pool.queue-size",
                "step": 1,
                "query_url": "none",
                "upper_bound": "100"
            },
            {
                "value_type": "integer",
                "lower_bound": "1",
                "name": "quarkus.hibernate-orm.jdbc.statement-fetch-size",
                "step": 1,
                "query_url": "none",
                "upper_bound": "50"
            }
        ],
        "layer_name": "quarkus",
        "layer_details": "quarkus tunables"
    }
]
```

## SearchSpace

Generates the search space used for the analysis.

**Request**
`GET /searchSpace` gives the search space for all application stacks monitored.

`GET /searchSpace?experiment_name=<EXPERIMENT_NAME>` gives the search space for a specific application stack.

`curl -H 'Accept: application/json' http://<URL>:<PORT>/searchSpace`

**Response**

```
[
    {
        "experiment_name": "galaxies-autotune-min-http-response-time",
        "experiment_id": "7c07cf4db16adcf76bad79394c9e7df2f3b8d8e6942cfa3f7b254b5aec1299b0",
        "objective_function": "request_sum/request_count",
        "hpo_algo_impl": "optuna_tpe",
        "tunables": [
            {
                "value_type": "double",
                "lower_bound": "150.0Mi",
                "name": "memoryRequest",
                "step": 1,
                "upper_bound": "300.0Mi"
            },
            {
                "value_type": "double",
                "lower_bound": "1.0",
                "name": "cpuRequest",
                "step": 0.01,
                "upper_bound": "3.0"
            },
            {
                "value_type": "integer",
                "lower_bound": "9",
                "name": "MaxInlineLevel",
                "step": 1,
                "upper_bound": "50"
            },
            {
                "value_type": "integer",
                "lower_bound": "1",
                "name": "quarkus.thread-pool.core-threads",
                "step": 1,
                "upper_bound": "10"
            },
            {
                "value_type": "integer",
                "lower_bound": "1",
                "name": "quarkus.thread-pool.queue-size",
                "step": 1,
                "upper_bound": "100"
            },
            {
                "value_type": "integer",
                "lower_bound": "1",
                "name": "quarkus.hibernate-orm.jdbc.statement-fetch-size",
                "step": 1,
                "upper_bound": "50"
            }
        ],
        "direction": "minimize"
    }
]
```

## Health

Get the status of autotune.

**Request**
`GET /health`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/health`

**Response**

```
Healthy
```

## CreateExperimentTrial

Create experiment trials using input JSON provided by Analyser module.

**Request**
`POST /createExperimentTrial`

`curl -H 'Accept: application/json' -X POST --data 'copy paste below JSON' http://<URL>:<PORT>/createExperimentTrial`

```
[
    {
      "experiment_name": "quarkus-resteasy-autotune-min-http-response-time-db",
      "resource": {
        "namespace": "default",
        "deployment_name": "tfb-qrh-sample"
      },
      "datasource_info": {
        "prometheus1": {
          "url": "http://10.101.144.137:9090",
          "provider": "prometheus"
        }
      },
      "settings": {
        "do_experiment": true,
        "do_monitoring": true,
        "wait_for_load": true,
        "trial_settings": {
          "measurement_cycles": "3",
          "warmup_duration": "1min",
          "warmup_cycles": "3",
          "measurement_duration": "1min",
          "iterations": "3"
        },
        "deployment_settings": {
          "deployment_policy": {
            "type": "rollingUpdate"
          }
        }
      },
      "pod_metrics": {
        "request_sum": {
          "datasource": "prometheus1",
          "query": "rate(http_server_requests_seconds_sum{method=\"GET\",outcome=\"SUCCESS\",status=\"200\",uri=\"/db\",}[1m])",
          "name": "request_sum"
        },
        "request_count": {
          "datasource": "prometheus1",
          "query": "rate(http_server_requests_seconds_count{method=\"GET\",outcome=\"SUCCESS\",status=\"200\",uri=\"/db\",}[1m])",
          "name": "request_count"
        }
      },
      "container_metrics": {
        "container_name": {
          "memoryRequest": {
            "name": "memoryRequest",
            "query": "container_memory_working_set_bytes{$CONTAINER_LABEL$=\"\", $POD_LABEL$=\"$POD$\"}",
            "datasource": "prometheus",
            "valueType": "double"
          },
          "gc": {
            "name": "gc",
            "query": "jvm_memory_used_bytes{area=\"heap\", $CONTAINER_LABEL$=\"\", $POD_LABEL$=\"$POD$\"}",
            "datasource": "prometheus",
            "valueType": "categorical"
          },
          "cpuRequest": {
            "name": "cpuRequest",
            "query": "(container_cpu_usage_seconds_total{$CONTAINER_LABEL$!=\"POD\", $POD_LABEL$=\"$POD$\"}[1m])",
            "datasource": "prometheus",
            "valueType": "double"
          }
        }
      },
      "trials": {
        "0": {
          "config": {
            "image": "kruize/tfb-qrh:2.9.0.F_mm",
            "container_name": "tfb-server",
            "requests": {
              "cpu": {
                "amount": "2.11",
                "format": "",
                "additionalProperties": {}
              },
              "memory": {
                "amount": "160.0",
                "format": "",
                "additionalProperties": {}
              }
            },
            "limits": {
              "cpu": {
                "amount": "2.11",
                "format": "",
                "additionalProperties": {}
              },
              "memory": {
                "amount": "160.0",
                "format": "",
                "additionalProperties": {}
              }
            },
            "env": [
              {
                "name": "JDK_JAVA_OPTIONS",
                "additionalProperties": {},
                "value": " -server -XX:MaxRAMPercentage=70 -XX:+AllowParallelDefineClass -XX:MaxInlineLevel=21 -XX:+UseZGC -XX:+TieredCompilation -Dquarkus.thread-pool.queue-size=27 -Dquarkus.thread-pool.core-threads=9"
              }
            ]
          }
        }
      },
      "trial_result_url": "http://localhost:8080/listExperiments?experiment_name=quarkus-resteasy-autotune-min-http-response-time-db"
    }
]
```

**Response**

On Success

```
{
    "httpcode": 201,
    "documentationLink": "",
    "status": "SUCCESS"
}
```

On Failure

```
{
    "message": "Following trials : [A, B] Already exist for Experiment :  Compare performance objective of A/B software releases",
    "httpcode": 400,
    "documentationLink": "",
    "status": "ERROR"
}
```

### List Trial Status

**Request**
`GET /listTrialStatus` Gives the status of the Trial(s)

`GET /listTrialStatus` Gives the list of experiments with their last trial updated status

```
{
    "quarkus-resteasy-autotune-min-http-response-time-db": {
        "Status": "COMPLETED"
    },
    "quarkus-resteasy-autotune-max-throughput-api": {
        "Status": "WAITING_FOR_LOAD"
    },
}

```

`GET /listTrialStatus?experiment_name=<experiment name>` Gives the Trial status of the trials in a particular experiment

Example for `experiment_name` set to `quarkus-resteasy-autotune-min-http-response-time-db`
**Response**

```
{
    "0": {
        "Status": "COMPLETED",
        "experiment_name": "quarkus-resteasy-autotune-min-http-response-time-db",
        "deployments": [{
            "pod_metrics": [
                {
                    "datasource": "prometheus",
                    "summary_results": {
                        "percentile_info": {
                            "99p": 82.59,
                            "97p": 64.75,
                            "95p": 8.94,
                            "50p": 0.63,
                            "99.9p": 93.48,
                            "100p": 30000,
                            "99.99p": 111.5,
                            "99.999p": 198.52
                        },
                        "general_info": {
                            "min": 2.15,
                            "max": 2107.212121,
                            "mean": 31.91
                        }
                    },
                    "name": "request_sum"
                },
                {
                    "datasource": "prometheus",
                    "summary_results": {
                        "percentile_info": {
                            "99p": 82.59,
                            "97p": 64.75,
                            "95p": 8.94,
                            "50p": 0.63,
                            "99.9p": 93.48,
                            "100p": 30000,
                            "99.99p": 111.5,
                            "99.999p": 198.52
                        },
                        "general_info": {
                            "min": 2.15,
                            "max": 2107.212121,
                            "mean": 31.91
                        }
                    },
                    "name": "request_count"
                }
            ],
            "deployment_name": "tfb-qrh-sample",
            "namespace": "default",
            "containers": [{
                "image_name": "kruize/tfb-qrh:1.13.2.F_et17",
                "container_name": "tfb-server",
                "container_metrics": [
                    {
                        "datasource": "prometheus",
                        "summary_results": {
                            "percentile_info": {
                                "99p": 82.59,
                                "97p": 64.75,
                                "95p": 8.94,
                                "50p": 0.63,
                                "99.9p": 93.48,
                                "100p": 30000,
                                "99.99p": 111.5,
                                "99.999p": 198.52
                            },
                            "general_info": {
                                "min": 2.15,
                                "max": 2107.212121,
                                "mean": 31.91
                            }
                        },
                        "name": "memoryRequest"
                    },
                    {
                        "datasource": "prometheus",
                        "summary_results": {
                            "percentile_info": {
                                "99p": 82.59,
                                "97p": 64.75,
                                "95p": 8.94,
                                "50p": 0.63,
                                "99.9p": 93.48,
                                "100p": 30000,
                                "99.99p": 111.5,
                                "99.999p": 198.52
                            },
                            "general_info": {
                                "min": 2.15,
                                "max": 2107.212121,
                                "mean": 31.91
                            }
                        },
                        "name": "cpuRequest"
                    }
                ]
            }],
            "type": "training"
        }],
        "experiment_id": "04c99daec35563782c29f17ebe568ea96065a7b20b93eb47c225a2f2ad769445",
        "deployment_name": "tfb-qrh-sample"
    },
    "1": {
        "Status": "COMPLETED",
        "experiment_name": "quarkus-resteasy-autotune-min-http-response-time-db",
        "deployments": [{
            "pod_metrics": [
                {
                    "datasource": "prometheus",
                    "summary_results": {
                        "percentile_info": {
                            "99p": 82.59,
                            "97p": 64.75,
                            "95p": 8.94,
                            "50p": 0.63,
                            "99.9p": 93.48,
                            "100p": 30000,
                            "99.99p": 111.5,
                            "99.999p": 198.52
                        },
                        "general_info": {
                            "min": 2.15,
                            "max": 2107.212121,
                            "mean": 31.91
                        }
                    },
                    "name": "request_sum"
                },
                {
                    "datasource": "prometheus",
                    "summary_results": {
                        "percentile_info": {
                            "99p": 82.59,
                            "97p": 64.75,
                            "95p": 8.94,
                            "50p": 0.63,
                            "99.9p": 93.48,
                            "100p": 30000,
                            "99.99p": 111.5,
                            "99.999p": 198.52
                        },
                        "general_info": {
                            "min": 2.15,
                            "max": 2107.212121,
                            "mean": 31.91
                        }
                    },
                    "name": "request_count"
                }
            ],
            "deployment_name": "tfb-qrh-sample",
            "namespace": "default",
            "containers": [{
                "image_name": "kruize/tfb-qrh:1.13.2.F_et17",
                "container_name": "tfb-server",
                "container_metrics": [
                    {
                        "datasource": "prometheus",
                        "summary_results": {
                            "percentile_info": {
                                "99p": 82.59,
                                "97p": 64.75,
                                "95p": 8.94,
                                "50p": 0.63,
                                "99.9p": 93.48,
                                "100p": 30000,
                                "99.99p": 111.5,
                                "99.999p": 198.52
                            },
                            "general_info": {
                                "min": 2.15,
                                "max": 2107.212121,
                                "mean": 31.91
                            }
                        },
                        "name": "memoryRequest"
                    },
                    {
                        "datasource": "prometheus",
                        "summary_results": {
                            "percentile_info": {
                                "99p": 82.59,
                                "97p": 64.75,
                                "95p": 8.94,
                                "50p": 0.63,
                                "99.9p": 93.48,
                                "100p": 30000,
                                "99.99p": 111.5,
                                "99.999p": 198.52
                            },
                            "general_info": {
                                "min": 2.15,
                                "max": 2107.212121,
                                "mean": 31.91
                            }
                        },
                        "name": "cpuRequest"
                    }
                ]
            }],
            "type": "training"
        }],
        "experiment_id": "04c99daec35563782c29f17ebe568ea96065a7b20b93eb47c225a2f2ad769445",
        "deployment_name": "tfb-qrh-sample",
        "info": {"trial_info": {
            "trial_id": "",
            "trial_num": 1,
            "trial_result_url": "http://localhost:8080/listExperiments?experiment_name=quarkus-resteasy-autotune-min-http-response-time-db"
        }}
    },
}
```

`GET /listTrialStatus?experiment_name=<experiment name>&trial_number=<trial number>` Gives the Trial status of the
particular trial number in an experiment

Example for `trial_number=1`

**Response**

```
{"1": {
    "Status": "COMPLETED",
    "experiment_name": "quarkus-resteasy-autotune-min-http-response-time-db",
    "deployments": [{
        "pod_metrics": [
            {
                "datasource": "prometheus",
                "summary_results": {
                    "percentile_info": {
                        "99p": 82.59,
                        "97p": 64.75,
                        "95p": 8.94,
                        "50p": 0.63,
                        "99.9p": 93.48,
                        "100p": 30000,
                        "99.99p": 111.5,
                        "99.999p": 198.52
                    },
                    "general_info": {
                        "min": 2.15,
                        "max": 2107.212121,
                        "mean": 31.91
                    }
                },
                "name": "request_sum"
            },
            {
                "datasource": "prometheus",
                "summary_results": {
                    "percentile_info": {
                        "99p": 82.59,
                        "97p": 64.75,
                        "95p": 8.94,
                        "50p": 0.63,
                        "99.9p": 93.48,
                        "100p": 30000,
                        "99.99p": 111.5,
                        "99.999p": 198.52
                    },
                    "general_info": {
                        "min": 2.15,
                        "max": 2107.212121,
                        "mean": 31.91
                    }
                },
                "name": "request_count"
            }
        ],
        "deployment_name": "tfb-qrh-sample",
        "namespace": "default",
        "containers": [{
            "image_name": "kruize/tfb-qrh:1.13.2.F_et17",
            "container_name": "tfb-server",
            "container_metrics": [
                {
                    "datasource": "prometheus",
                    "summary_results": {
                        "percentile_info": {
                            "99p": 82.59,
                            "97p": 64.75,
                            "95p": 8.94,
                            "50p": 0.63,
                            "99.9p": 93.48,
                            "100p": 30000,
                            "99.99p": 111.5,
                            "99.999p": 198.52
                        },
                        "general_info": {
                            "min": 2.15,
                            "max": 2107.212121,
                            "mean": 31.91
                        }
                    },
                    "name": "memoryRequest"
                },
                {
                    "datasource": "prometheus",
                    "summary_results": {
                        "percentile_info": {
                            "99p": 82.59,
                            "97p": 64.75,
                            "95p": 8.94,
                            "50p": 0.63,
                            "99.9p": 93.48,
                            "100p": 30000,
                            "99.99p": 111.5,
                            "99.999p": 198.52
                        },
                        "general_info": {
                            "min": 2.15,
                            "max": 2107.212121,
                            "mean": 31.91
                        }
                    },
                    "name": "cpuRequest"
                }
            ]
        }],
        "type": "training"
    }],
    "experiment_id": "04c99daec35563782c29f17ebe568ea96065a7b20b93eb47c225a2f2ad769445",
    "deployment_name": "tfb-qrh-sample"
}}
```

`GET /listTrialStatus?debug=true` Gives the granular details like status and timestamp of each main step and sub steps
for a given workflow.

Example for `debug=true`

**Response**

```
{
    "Compare performance objective of A/B software releases": {
        "status": "IN_PROGRESS",
        "creationDate": "Oct 12, 2022, 2:10:25 PM",
        "beginTimeStamp": "Oct 12, 2022, 2:10:25 PM",
        "trialDetails": {
            "A": {
                "creationDate": "Oct 12, 2022, 2:10:25 PM",
                "beginTimestamp": "Oct 12, 2022, 2:10:25 PM",
                "endTimestamp": "Oct 12, 2022, 2:10:26 PM",
                "iterations": {
                    "1": {
                        "beginTimestamp": "Oct 12, 2022, 2:10:25 PM",
                        "endTimestamp": "Oct 12, 2022, 2:10:25 PM",
                        "status": "COMPLETED",
                        "iterationNumber": 1,
                        "workFlow": {
                            "PreValidation": {
                                "stepName": "PreValidation",
                                "beginTimestamp": "Oct 12, 2022, 2:10:25 PM",
                                "endTimestamp": "Oct 12, 2022, 2:10:25 PM",
                                "status": "COMPLETED"
                            },
                            "Deployment": {
                                "stepName": "Deployment",
                                "beginTimestamp": "Oct 12, 2022, 2:10:25 PM",
                                "endTimestamp": "Oct 12, 2022, 2:10:25 PM",
                                "status": "COMPLETED"
                            },
                            "PostValidation": {
                                "stepName": "PostValidation",
                                "beginTimestamp": "Oct 12, 2022, 2:10:25 PM",
                                "endTimestamp": "Oct 12, 2022, 2:10:25 PM",
                                "status": "COMPLETED"
                            },
                            "LoadValidation": {
                                "stepName": "LoadValidation",
                                "beginTimestamp": "Oct 12, 2022, 2:10:25 PM",
                                "endTimestamp": "Oct 12, 2022, 2:10:25 PM",
                                "status": "COMPLETED"
                            },
                            "MetricCollection": {
                                "stepName": "MetricCollection",
                                "beginTimestamp": "Oct 12, 2022, 2:10:25 PM",
                                "endTimestamp": "Oct 12, 2022, 2:10:25 PM",
                                "status": "COMPLETED"
                            },
                            "Summarizer": {
                                "stepName": "Summarizer",
                                "beginTimestamp": "Oct 12, 2022, 2:10:25 PM",
                                "endTimestamp": "Oct 12, 2022, 2:10:25 PM",
                                "status": "COMPLETED"
                            }
                        }
                    },
                    "2": {
                        "beginTimestamp": "Oct 12, 2022, 2:10:25 PM",
                        "endTimestamp": "Oct 12, 2022, 2:10:25 PM",
                        "status": "COMPLETED",
                        "iterationNumber": 2,
                        "workFlow": {
                            "PreValidation": {
                                "stepName": "PreValidation",
                                "beginTimestamp": "Oct 12, 2022, 2:10:25 PM",
                                "endTimestamp": "Oct 12, 2022, 2:10:25 PM",
                                "status": "COMPLETED"
                            },
                            "Deployment": {
                                "stepName": "Deployment",
                                "beginTimestamp": "Oct 12, 2022, 2:10:25 PM",
                                "endTimestamp": "Oct 12, 2022, 2:10:25 PM",
                                "status": "COMPLETED"
                            },
                            "PostValidation": {
                                "stepName": "PostValidation",
                                "beginTimestamp": "Oct 12, 2022, 2:10:25 PM",
                                "endTimestamp": "Oct 12, 2022, 2:10:25 PM",
                                "status": "COMPLETED"
                            },
                            "LoadValidation": {
                                "stepName": "LoadValidation",
                                "beginTimestamp": "Oct 12, 2022, 2:10:25 PM",
                                "endTimestamp": "Oct 12, 2022, 2:10:25 PM",
                                "status": "COMPLETED"
                            },
                            "MetricCollection": {
                                "stepName": "MetricCollection",
                                "beginTimestamp": "Oct 12, 2022, 2:10:25 PM",
                                "endTimestamp": "Oct 12, 2022, 2:10:25 PM",
                                "status": "COMPLETED"
                            },
                            "Summarizer": {
                                "stepName": "Summarizer",
                                "beginTimestamp": "Oct 12, 2022, 2:10:25 PM",
                                "endTimestamp": "Oct 12, 2022, 2:10:25 PM",
                                "status": "COMPLETED"
                            }
                        }
                    },
                    "3": {
                        "beginTimestamp": "Oct 12, 2022, 2:10:25 PM",
                        "endTimestamp": "Oct 12, 2022, 2:10:26 PM",
                        "status": "COMPLETED",
                        "iterationNumber": 3,
                        "workFlow": {
                            "PreValidation": {
                                "stepName": "PreValidation",
                                "beginTimestamp": "Oct 12, 2022, 2:10:25 PM",
                                "endTimestamp": "Oct 12, 2022, 2:10:25 PM",
                                "status": "COMPLETED"
                            },
                            "Deployment": {
                                "stepName": "Deployment",
                                "beginTimestamp": "Oct 12, 2022, 2:10:25 PM",
                                "endTimestamp": "Oct 12, 2022, 2:10:26 PM",
                                "status": "COMPLETED"
                            },
                            "PostValidation": {
                                "stepName": "PostValidation",
                                "beginTimestamp": "Oct 12, 2022, 2:10:26 PM",
                                "endTimestamp": "Oct 12, 2022, 2:10:26 PM",
                                "status": "COMPLETED"
                            },
                            "LoadValidation": {
                                "stepName": "LoadValidation",
                                "beginTimestamp": "Oct 12, 2022, 2:10:26 PM",
                                "endTimestamp": "Oct 12, 2022, 2:10:26 PM",
                                "status": "COMPLETED"
                            },
                            "MetricCollection": {
                                "stepName": "MetricCollection",
                                "beginTimestamp": "Oct 12, 2022, 2:10:26 PM",
                                "endTimestamp": "Oct 12, 2022, 2:10:26 PM",
                                "status": "COMPLETED"
                            },
                            "Summarizer": {
                                "stepName": "Summarizer",
                                "beginTimestamp": "Oct 12, 2022, 2:10:26 PM",
                                "endTimestamp": "Oct 12, 2022, 2:10:26 PM",
                                "status": "COMPLETED"
                            }
                        }
                    }
                },
                "status": "COMPLETED",
                "trialWorkflow": {
                    "Summarizer": {
                        "stepName": "Summarizer",
                        "beginTimestamp": "Oct 12, 2022, 2:10:26 PM",
                        "endTimestamp": "Oct 12, 2022, 2:10:26 PM",
                        "status": "COMPLETED"
                    },
                    "PostResults": {
                        "stepName": "PostResults",
                        "beginTimestamp": "Oct 12, 2022, 2:10:26 PM",
                        "endTimestamp": "Oct 12, 2022, 2:10:26 PM",
                        "status": "COMPLETED"
                    }
                }
            },
            "B": {
                "creationDate": "Oct 12, 2022, 2:10:25 PM",
                "iterations": {
                    "1": {
                        "status": "QUEUED",
                        "iterationNumber": 1,
                        "workFlow": {
                            "PreValidation": {
                                "stepName": "PreValidation",
                                "status": "QUEUED"
                            },
                            "Deployment": {
                                "stepName": "Deployment",
                                "status": "QUEUED"
                            },
                            "PostValidation": {
                                "stepName": "PostValidation",
                                "status": "QUEUED"
                            },
                            "LoadValidation": {
                                "stepName": "LoadValidation",
                                "status": "QUEUED"
                            },
                            "MetricCollection": {
                                "stepName": "MetricCollection",
                                "status": "QUEUED"
                            },
                            "Summarizer": {
                                "stepName": "Summarizer",
                                "status": "QUEUED"
                            }
                        }
                    },
                    "2": {
                        "status": "QUEUED",
                        "iterationNumber": 2,
                        "workFlow": {
                            "PreValidation": {
                                "stepName": "PreValidation",
                                "status": "QUEUED"
                            },
                            "Deployment": {
                                "stepName": "Deployment",
                                "status": "QUEUED"
                            },
                            "PostValidation": {
                                "stepName": "PostValidation",
                                "status": "QUEUED"
                            },
                            "LoadValidation": {
                                "stepName": "LoadValidation",
                                "status": "QUEUED"
                            },
                            "MetricCollection": {
                                "stepName": "MetricCollection",
                                "status": "QUEUED"
                            },
                            "Summarizer": {
                                "stepName": "Summarizer",
                                "status": "QUEUED"
                            }
                        }
                    },
                    "3": {
                        "status": "QUEUED",
                        "iterationNumber": 3,
                        "workFlow": {
                            "PreValidation": {
                                "stepName": "PreValidation",
                                "status": "QUEUED"
                            },
                            "Deployment": {
                                "stepName": "Deployment",
                                "status": "QUEUED"
                            },
                            "PostValidation": {
                                "stepName": "PostValidation",
                                "status": "QUEUED"
                            },
                            "LoadValidation": {
                                "stepName": "LoadValidation",
                                "status": "QUEUED"
                            },
                            "MetricCollection": {
                                "stepName": "MetricCollection",
                                "status": "QUEUED"
                            },
                            "Summarizer": {
                                "stepName": "Summarizer",
                                "status": "QUEUED"
                            }
                        }
                    }
                },
                "status": "QUEUED",
                "trialWorkflow": {
                    "Summarizer": {
                        "stepName": "Summarizer",
                        "status": "QUEUED"
                    },
                    "PostResults": {
                        "stepName": "PostResults",
                        "status": "QUEUED"
                    }
                }
            }
        }
    }
}
```