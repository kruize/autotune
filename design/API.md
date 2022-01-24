# Autotune REST API
The Autotune REST API design is proposed as follows:

##  listStacks
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

##  listStackTunables
Returns the list of application stacks monitored by autotune along with their tunables.
**Request**
`GET /listStackTunables` gives the tunables and layer information for all the application stacks monitored by autotune.

`GET /listStackTunables?experiment_name=<EXPERIMENT_NAME>` for getting the tunables information of a specific application.

`GET /listStackTunables?experiment_name=<EXPERIMENT_NAME>&layer_name=<LAYER>` for getting tunables of a specific layer for the application.

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
##  listAutotuneTunables
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
##  SearchSpace
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

##  Health
Get the status of autotune.

**Request**
`GET /health`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/health`

**Response**

```
Healthy
```

##  createExperimentTrial
Launch an experiment for a particular deployment with a config recommended by the Recommendation Manager 

**Request**
`GET /createExperimentTrial`

`curl -H 'Accept: application/json' -d <INPUT JSON> http://<URL>:<PORT>/createExperimentTrial`

*Description:*

The API endpoint `createExperimentTrial` expects the data object of type `JSON` with a structure as shown
below. The Input JSON holds details of the configuration to try, settings to that particular deployment
and the details of the deployment.

*Sections:* 
    
    - Metadata
    - Info
    - Settings
    - Deployments

Metadata section consists of params like `experiment_id`, `name` etc which are top level info points for the experiment

Info section consists of the trial information details like `trial_id`, `trial_num`

Settings section consists of 2 sub-sections 


    - trial_settings
    - deployment_settings


Trial Settings consists of setting related to trial like 

`total_duration` : How long a trial experiment should run for ?
`warmup_cycles` : Number of iterations for waiting for warmup
`warmup_duration` : Time to wait for each iteration
`measurement_cycles` : Number of iterations for measuring the metrics
`measurement_duration` : Time to collect metrics in each iteration

Deployment Settings consists of settings related to deployment like

`deployment_info` : Metadata about the deployment like name of the deployment (will be ignored and warned if doesn't match in case of rollingUpdate), target environment (dev / qa / prod) 
`deployment_policy` : What's the type of policy ? (newDeployment / rollingUpdate)
`deployment_tracking` : Which deployments to track (either prod or training or both)

Deployments section consists of the deployments to track and also their respective configs

`deployment_name` : This is name of the target deployment to apply the config.

*Sample Input JSON:*

```
{
  "settings": {
    "trial_settings": {
      "measurement_cycles": "3",
      "warmup_duration": "1min",
      "warmup_cycles": "3",
      "measurement_duration": "1min",
      "iterations": "3"
    },
    "deployment_settings": {
      "deployment_tracking": {"trackers": ["training"]},
      "deployment_info": {
        "agent": "EM",
        "target_env": "qa"
      },
      "deployment_policy": {
        "type": "rollingUpdate"
      }
    }
  },
  "experiment_name": "galaxies-autotune-min-http-response-time",
  "deployments": [{
    "deployment_name": "galaxies-sample",
    "namespace": "default",
    "containers": [{
      "image_name": "dinogun/galaxies:1.2-jdk-11.0.10_9",
      "container_name": "galaxies",
      "config": [
        {
          "name": "update requests and limits",
          "spec": {"template": {"spec": {"container": {"resources": {
            "requests": {
              "memory": "213.00Mi",
              "cpu": "1.04"
            },
            "limits": {
              "memory": "213.00Mi",
              "cpu": "1.04"
            }
          }}}}}
        },
        {
          "name": "update env",
          "spec": {"template": {"spec": {"container": {"env": {"JDK_JAVA_OPTIONS": " -Dquarkus.hibernate-orm.jdbc.statement-fetch-size=47 -Dquarkus.thread-pool.queue-size=4 -Dquarkus.thread-pool.core-threads=5 -server -XX:+UseG1GC -XX:MaxRAMPercentage=70 -XX:MaxInlineLevel=43"}}}}}
        }
      ],
      "container_metrics": [
        {
          "datasource": "prometheus",
          "query": "rate(http_server_requests_seconds_sum{method=\"GET\",outcome=\"SUCCESS\",status=\"200\",uri=\"/galaxies\",}[1m])",
          "name": "request_sum"
        },
        {
          "datasource": "prometheus",
          "query": "rate(http_server_requests_seconds_count{method=\"GET\",outcome=\"SUCCESS\",status=\"200\",uri=\"/galaxies\",}[1m])",
          "name": "request_count"
        }
      ]
    }],
    "pod_metrics": [
      {
        "datasource": "prometheus",
        "query": "rate(http_server_requests_seconds_sum{method=\"GET\",outcome=\"SUCCESS\",status=\"200\",uri=\"/galaxies\",}[1m])",
        "name": "request_sum"
      },
      {
        "datasource": "prometheus",
        "query": "rate(http_server_requests_seconds_count{method=\"GET\",outcome=\"SUCCESS\",status=\"200\",uri=\"/galaxies\",}[1m])",
        "name": "request_count"
      },
      {
        "datasource": "prometheus",
        "query": "(container_cpu_usage_seconds_total{$CONTAINER_LABEL$!=\"POD\", $POD_LABEL$=\"$POD$\"}[1m])",
        "name": "cpuRequest"
      },
      {
        "datasource": "prometheus",
        "query": "jvm_memory_used_bytes{area=\"heap\", $CONTAINER_LABEL$=\"\", $POD_LABEL$=\"$POD$\"}",
        "name": "MaxInlineLevel"
      },
      {
        "datasource": "prometheus",
        "query": "container_memory_working_set_bytes{$CONTAINER_LABEL$=\"\", $POD_LABEL$=\"$POD$\"}",
        "name": "memoryRequest"
      }
    ],
    "type": "training"
  }],
  "experiment_id": "7671cf10f52b288d48e3f60806af0740ec09a09360ed1da3a6e24df4cfd27256",
  "app-version": "v1",
  "info": {
    "trial_info": {
      "trial_id": "",
      "trial_num": 0,
      "trial_result_url": "trail data return url"
    },
    "datasource_info": [
      {
        "name": "prometheus",
        "url": "prom url"
      }
    ]
  }
} 
```

**Response**

The Endpoint returns the `runId` (Which is specific to EM) of the experiment
which we can use to track the status of the experiment

```
{
    'runId' : 'ccffab17-0ce5-43bc-96a5-5d9ef5fca075'
}
```

##  ListTrialStatus
Get the status of the experiment by passing `runId` to the endpoint.

**Request**
`GET /listTrialStatus?runId=<RUN_ID>&completeStatus=true&summary=true`

```
runId - Required
completeStatus - Optional
summary - Optional
```

`curl http://<URL>:<PORT>/listTrialStatus?runId=<RUN_ID>`

*Description:*

The `listTrialStatus` endpoint expects a input param `runId` which holds the `runId` and returns the status of a particular experiment run if the id is valid.

**Response**

##### With only `runId`

```
{
    'status' : <STATUS OF EXPERIMENT [CREATED | IN_PROGRESS | COMPLETED]>
}
```

##### With `runId` and `completeStatus` (Under design yet to be implemented)

```
{
  "experiment_id": "2190310A384BC90EF",
  "name": "petclinic-autotune",
  "status": "COMPLETED",
  "info": {
    "trial_id": "",
    "trial_num": 1,
    "trial_result_url": "http://analyser.com/receive/results?expId=2190310A384BC90EF"
  },
  "settings": {
    "trial_settings": {
      "total_duration": "7 mins",
      "warmup_cycles": 2,
      "warmup_duration": "1 min",
      "measurement_cycles": 5,
      "measurement_duration": "1 min"
    },
    "deployment_settings": {
      "deployment_info": {
        "deployment_name" : "petclinic-sample",
        "target_env" : "qa"
      },
      "deployment_policy" : {
        "type" : "rollingUpdate"
      },
      "deployment_tracking": {
        "trackers": [
          "training"
        ]
      }
    }
  },
  "deployments": [
    {
      "type" : "training",
      "deployment_name": "petclinic-sample",
      "namespace" : "default",
      "state": "",
      "result": "",
      "result_info": "",
      "result_error": "",
      "metrics": [
        {
          "name": "request_sum",
          "query": "request_sum_query",
          "datasource": "prometheus",
          "value_type": "double",
          "metric_results": {
            "warmup_results": {
              "cycles": 2,
              "duration": "1 min",
              "results": [
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                },
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                }
              ]
            },
            "measurement_results": {
              "cycles": 5,
              "duration": "1min",
              "results": [
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                },
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                },
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                },
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                },
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                }
              ]
            }
          }
        },
        {
          "name": "request_count",
          "query": "request_count_query",
          "datasource": "prometheus",
          "value_type": "double",
          "metric_results": {
            "warmup_results": {
              "cycles": 2,
              "duration": "1 min",
              "results": [
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                },
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                }
              ]
            },
            "measurement_results": {
              "cycles": 5,
              "duration": "1min",
              "results": [
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                },
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                },
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                },
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                },
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                }
              ]
            }
          }
        },
        {
          "name": "hotspot_function",
          "query": "hotspot_function_query",
          "datasource": "prometheus",
          "value_type": "double",
          "metric_results": {
            "warmup_results": {
              "cycles": 2,
              "duration": "1 min",
              "results": [
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                },
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                }
              ]
            },
            "measurement_results": {
              "cycles": 5,
              "duration": "1min",
              "results": [
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                },
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                },
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                },
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                },
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                }
              ]
            }
          }
        },
        {
          "name": "cpuRequest",
          "query": "cpuRequest_query",
          "datasource": "prometheus",
          "value_type": "double",
          "metric_results": {
            "warmup_results": {
              "cycles": 2,
              "duration": "1 min",
              "results": [
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                },
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                }
              ]
            },
            "measurement_results": {
              "cycles": 5,
              "duration": "1min",
              "results": [
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                },
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                },
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                },
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                },
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                }
              ]
            }
          }
        },
        {
          "name": "memRequest",
          "query": "memRequest_query",
          "datasource": "prometheus",
          "value_type": "double",
          "metric_results": {
            "warmup_results": {
              "cycles": 2,
              "duration": "1 min",
              "results": [
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                },
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                }
              ]
            },
            "measurement_results": {
              "cycles": 5,
              "duration": "1min",
              "results": [
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                },
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                },
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                },
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                },
                {
                  "score": "",
                  "Error": "",
                  "mean": "",
                  "mode": "",
                  "95.0": "",
                  "99.0": "",
                  "99.9": "",
                  "99.99": "",
                  "99.999": "",
                  "99.9999": "",
                  "100.0": "",
                  "spike": ""
                }
              ]
            }
          }
        }
      ],
      "config": [
        {
          "name": "update requests and limits",
          "spec": {
            "template": {
              "spec": {
                "container": {
                  "resources": {
                    "requests": {
                      "cpu": 2,
                      "memory": "512Mi"
                    },
                    "limits": {
                      "cpu": 3,
                      "memory": "1024Mi"
                    }
                  }
                }
              }
            }
          }
        },
        {
          "name": "update env",
          "spec": {
            "template": {
              "spec": {
                "container": {
                  "env": {
                    "JDK_JAVA_OPTIONS": "-XX:MaxInlineLevel=23"
                  }
                }
              }
            }
          }
        }
      ]
    }
  ]
}
```

##### With `runId` and `summary`

```
Yet to be finalised
```

### Note

`completeStatus` and `summary` cannot be clubbed together in a request. If both are sent `summary` takes the precedence and the API returns response of `summary`

