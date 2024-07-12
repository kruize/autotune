# Metric Profile

The metric profile contains a list of queries used to retrieve metrics such as CPU usage, throttling, memory
usage, and more. Users can create metric profiles based on their cluster or datasource provider, such as Prometheus or
Thanos. These profiles can be tagged to create experiment APIs, which will then fetch metrics according to the metric
profile to generate recommendations.

This article describes how to add and list Metric Profiles with REST APIs using curl command.
Documentation still in progress stay tuned.

# Attributes

- **apiVersion** \
  A string representing version of the Kubernetes API to create metric profile
- **kind** \
  A string representing type of kubernetes object
- **metadata** \
  A JSON object containing Data that helps to uniquely identify the metric profile, including a name string
    - **name** \
      A unique string name for identifying each metric profile.
- **profile_version** \
  a double value specifying the current version of the profile.
- **slo** \
  Service Level Objective containing the _direction_, _objective_function_ and _function_variables_
    - **slo_class** \
      a standard slo "bucket" defined by Kruize. Can be "_resource_usage_", "_throughput_" or "_response_time_"
    - **direction** \
      based on the slo_class, it can be '_maximize_' or '_minimize_'
    - **objective_function** \
      Define the performance objective here.
        - **function_type** \
          can be specified as '_source_' (a java file) or as an '_expression_'(algebraic). If it's an expression, it needs to defined below.
        - **expression** \
          an algebraic expression that details the calculation using function variables. Only valid if the "_function_type_" is "expression"
    - **function_variables** \
      Define the variables used in the _objective_function_
        - **name** \
          name of the variable
        - **datasource** \
          datasource of the query
        - **value_type** \
          can be double or integer
        - **query** \
          one of the query or _aggregation_functions_ is mandatory. Both can be present.
        - **kubernetes_object** \
          k8s object that this query is tied to: "_deployment_", "_pod_" or "_container_"
        - **aggregation_functions** \
          aggregate functions associated with this variable
            - **function** \
              can be '_avg_', '_sum_', '_min_', '_max_'
            - **query** \
              corresponding query
            - **versions** \
              Any specific versions that this query is tied to


## CreateMetricProfile

This is quick guide instructions to create metric profile using input JSON as follows.

**Request**
`POST /createMetricProfile`

`curl -H 'Accept: application/json' -X POST --data 'copy paste below JSON' http://<URL>:<PORT>/createMetricProfile`

```
{
    "apiVersion": "recommender.com/v1",
    "kind": "KruizePerformanceProfile",
    "metadata": {
        "name": "resource-optimization-openshift"
    },
    "profile_version": 1,
    "k8s_type": "openshift",
    "slo": {
        "sloClass": "resource_usage",
        "objective_function": {
            "function_type": "source"
        },
        "direction": "minimize",
        "hpoAlgoImpl": "optuna_tpe",
        "function_variables": [
            {
                "name": "cpuRequest",
                "query": "",
                "datasource": "prometheus",
                "value_type": "double",
                "kubernetes_object": "container",
                "aggregation_functions": [
                    {
                        "function": "avg",
                        "query": "avg(kube_pod_container_resource_requests{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE\", resource=\"cpu\", unit=\"core\"})",
                        "version": ""
                    },
                    {
                        "function": "sum",
                        "query": "sum(kube_pod_container_resource_requests{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE\", resource=\"cpu\", unit=\"core\"})",
                        "version": ""
                    }
                ]
            },
            {
                "name": "cpuLimit",
                "query": "",
                "datasource": "prometheus",
                "value_type": "double",
                "kubernetes_object": "container",
                "aggregation_functions": [
                    {
                        "function": "avg",
                        "query": "avg(kube_pod_container_resource_limits{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE\", resource=\"cpu\", unit=\"core\"})",
                        "version": ""
                    },
                    {
                        "function": "sum",
                        "query": "sum(kube_pod_container_resource_limits{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\", resource=\"cpu\", unit=\"core\"})",
                        "version": ""
                    }
                ]
            },
            {
                "name": "cpuUsage",
                "query": "",
                "datasource": "prometheus",
                "value_type": "double",
                "kubernetes_object": "container",
                "aggregation_functions": [
                    {
                        "function": "avg",
                        "query": "avg(avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_rate{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=\"$NAMESPACE$\", container=”$CONTAINER_NAME$”}[15m]))",
                        "version": "<=4.8"
                    },
                    {
                        "function": "avg",
                        "query": "avg(avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=\"$NAMESPACE$\", container=”$CONTAINER_NAME$”}[15m]))",
                        "version": ">4.9"
                    },
                    {
                        "function": "min",
                        "query": "min(min_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_rate{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=\"$NAMESPACE$\", container=\"$CONTAINER_NAME$\"}[15m]))",
                        "version": "<=4.8"
                    },
                    {
                        "function": "min",
                        "query": "min(min_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=\"$NAMESPACE$\", container=\"$CONTAINER_NAME$\"}[15m]))",
                        "version": ">4.9"
                    },
                    {
                        "function": "max",
                        "query": "max(max_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_rate{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=\"$NAMESPACE$\", container=\"$CONTAINER_NAME$\"}[15m]))",
                        "version": "<=4.8"
                    },
                    {
                        "function": "max",
                        "query": "max(max_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=\"$NAMESPACE$\", container=\"$CONTAINER_NAME$\"}[15m]))",
                        "version": ">4.9"
                    },
                    {
                        "function": "sum",
                        "query": "sum(avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_rate{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=\"$NAMESPACE$\", container=\"$CONTAINER_NAME$\"}[15m]))",
                        "version": "<=4.8"
                    },
                    {
                        "function": "sum",
                        "query": "sum(avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=\"$NAMESPACE$\", container=\"$CONTAINER_NAME$\"}[15m]))",
                        "version": ">4.9"
                    }
                ]
            },
            {
                "name": "cpuThrottle",
                "query": "",
                "datasource": "prometheus",
                "value_type": "double",
                "kubernetes_object": "container",
                "aggregation_functions": [
                    {
                        "function": "avg",
                        "query": "avg(rate(container_cpu_cfs_throttled_seconds_total{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=\"$NAMESPACE$\", container=”$CONTAINER_NAME$”}[15m]))",
                        "version": ""
                    },
                    {
                        "function": "max",
                        "query": "max(rate(container_cpu_cfs_throttled_seconds_total{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=\"$NAMESPACE$\", container=”$CONTAINER_NAME$”}[15m]))",
                        "version": ""
                    },
                    {
                        "function": "sum",
                        "query": "sum(rate(container_cpu_cfs_throttled_seconds_total{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=\"$NAMESPACE$\", container=”$CONTAINER_NAME$”}[15m]))",
                        "version": ""
                    }
                ]
            },
            {
                "name": "memoryRequest",
                "query": "",
                "datasource": "prometheus",
                "value_type": "double",
                "kubernetes_object": "container",
                "aggregation_functions": [
                    {
                        "function": "avg",
                        "query": "avg(kube_pod_container_resource_requests{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", container=”$CONTAINER_NAME$”, namespace=”$NAMESPACE”, resource=\"memory\", unit=\"byte\"})",
                        "version": ""
                    },
                    {
                        "function": "sum",
                        "query": "sum(kube_pod_container_resource_requests{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", container=”$CONTAINER_NAME$”, namespace=”$NAMESPACE”, resource=\"memory\", unit=\"byte\"})",
                        "version": ""
                    }
                ]
            },
            {
                "name": "memoryLimit",
                "query": "",
                "datasource": "prometheus",
                "value_type": "double",
                "kubernetes_object": "container",
                "aggregation_functions": [
                    {
                        "function": "avg",
                        "query": "avg(kube_pod_container_resource_limits{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE\", resource=\"memory\", unit=\"byte\"})",
                        "version": ""
                    },
                    {
                        "function": "sum",
                        "query": "sum(kube_pod_container_resource_limits{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", container=”$CONTAINER_NAME$”, namespace=”$NAMESPACE”, resource=\"memory\", unit=\"byte\"})",
                        "version": ""
                    }
                ]
            },
            {
                "name": "memoryUsage",
                "query": "",
                "datasource": "prometheus",
                "value_type": "double",
                "kubernetes_object": "container",
                "aggregation_functions": [
                    {
                        "function": "avg",
                        "query": "avg(avg_over_time(container_memory_working_set_bytes{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=$NAMESPACE$, container=”$CONTAINER_NAME$”}[15m]))",
                        "version": ""
                    },
                    {
                        "function": "min",
                        "query": "min(min_over_time(container_memory_working_set_bytes{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=$NAMESPACE$, container=\"$CONTAINER_NAME$\"}[15m]))",
                        "version": ""
                    },
                    {
                        "function": "max",
                        "query": "max(max_over_time(container_memory_working_set_bytes{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=$NAMESPACE$, container=\"$CONTAINER_NAME$\"}[15m]))",
                        "version": ""
                    },
                    {
                        "function": "sum",
                        "query": "sum(avg_over_time(container_memory_working_set_bytes{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=$NAMESPACE$, container=\"$CONTAINER_NAME$\"}[15m]))",
                        "version": ""
                    }
                ]
            },
            {
                "name": "memoryRSS",
                "query": "",
                "datasource": "prometheus",
                "value_type": "double",
                "kubernetes_object": "container",
                "aggregation_functions": [
                    {
                        "function": "avg",
                        "query": "avg(avg_over_time(container_memory_rss{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=$NAMESPACE$, container=”$CONTAINER_NAME$”}[15m]))",
                        "version": ""
                    },
                    {
                        "function": "min",
                        "query": "min(min_over_time(container_memory_rss{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=$NAMESPACE$, container=\"$CONTAINER_NAME$\"}[15m]))",
                        "version": ""
                    },
                    {
                        "function": "max",
                        "query": "max(max_over_time(container_memory_rss{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=$NAMESPACE$, container=\"$CONTAINER_NAME$\"}[15m]))",
                        "version": ""
                    },
                    {
                        "function": "sum",
                        "query": "sum(avg_over_time(container_memory_rss{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=$NAMESPACE$, container=”$CONTAINER_NAME$”}[15m]))",
                        "version": ""
                    }
                ]
            }
        ]
    }
}
```

**Response**

* Success
```
{
    "message": "Metric Profile : <name> created successfully. View all metric profiles at /listMetricProfiles",
    "httpcode": 201,
    "documentationLink": "",
    "status": "SUCCESS"
}
```

* Failure
    * Duplicate Metric Profile name.
  ```
  {
      "message": "Metric Profile already exists",
      "httpcode": 409,
      "documentationLink": "",
      "status": "ERROR"
  }
  ```
    * Mandatory parameters are missing.
  ```
  {
      "message": "Missing mandatory parameters",
      "httpcode": 400,
      "documentationLink": "",
      "status": "ERROR"
  }
  ```
    * Any unknown exception on server side
  ```
  {
      "message": "Internal Server Error",
      "httpcode": 500,
      "documentationLink": "",
      "status": "ERROR"
  }
  ```
#### Note: One of query or aggregation_functions is mandatory. Both can be present together.

## List Metric Profiles

List metric profiles output JSON as follows.

**Request**
`GET /listMetricProfiles`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listMetricProfiles`

**Response**

```
[
    {
        "apiVersion": "recommender.com/v1",
        "kind": "KruizePerformanceProfile",
        "metadata": {
            "name": "resource-optimization-openshift"
        },
        "profile_version": 1.0,
        "k8s_type": "openshift",
        "slo": {
            "sloClass": "resource_usage",
            "objective_function": {
                "function_type": "source"
            },
            "direction": "minimize",
            "hpoAlgoImpl": "optuna_tpe",
            "function_variables": [
                {
                    "name": "cpuRequest",
                    "query": "",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "container",
                    "aggregation_functions": [
                        {
                            "function": "avg",
                            "query": "avg(kube_pod_container_resource_requests{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE\", resource=\"cpu\", unit=\"core\"})",
                            "version": ""
                        },
                        {
                            "function": "sum",
                            "query": "sum(kube_pod_container_resource_requests{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE\", resource=\"cpu\", unit=\"core\"})",
                            "version": ""
                        }
                    ]
                },
                {
                    "name": "cpuLimit",
                    "query": "",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "container",
                    "aggregation_functions": [
                        {
                            "function": "avg",
                            "query": "avg(kube_pod_container_resource_limits{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE\", resource=\"cpu\", unit=\"core\"})",
                            "version": ""
                        },
                        {
                            "function": "sum",
                            "query": "sum(kube_pod_container_resource_limits{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\", resource=\"cpu\", unit=\"core\"})",
                            "version": ""
                        }
                    ]
                },
                {
                    "name": "cpuUsage",
                    "query": "",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "container",
                    "aggregation_functions": [
                        {
                            "function": "avg",
                            "query": "avg(avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_rate{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=\"$NAMESPACE$\", container=”$CONTAINER_NAME$”}[15m]))",
                            "version": "<=4.8"
                        },
                        {
                            "function": "avg",
                            "query": "avg(avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=\"$NAMESPACE$\", container=”$CONTAINER_NAME$”}[15m]))",
                            "version": ">4.9"
                        },
                        {
                            "function": "min",
                            "query": "min(min_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_rate{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=\"$NAMESPACE$\", container=\"$CONTAINER_NAME$\"}[15m]))",
                            "version": "<=4.8"
                        },
                        {
                            "function": "min",
                            "query": "min(min_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=\"$NAMESPACE$\", container=\"$CONTAINER_NAME$\"}[15m]))",
                            "version": ">4.9"
                        },
                        {
                            "function": "max",
                            "query": "max(max_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_rate{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=\"$NAMESPACE$\", container=\"$CONTAINER_NAME$\"}[15m]))",
                            "version": "<=4.8"
                        },
                        {
                            "function": "max",
                            "query": "max(max_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=\"$NAMESPACE$\", container=\"$CONTAINER_NAME$\"}[15m]))",
                            "version": ">4.9"
                        },
                        {
                            "function": "sum",
                            "query": "sum(avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_rate{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=\"$NAMESPACE$\", container=\"$CONTAINER_NAME$\"}[15m]))",
                            "version": "<=4.8"
                        },
                        {
                            "function": "sum",
                            "query": "sum(avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=\"$NAMESPACE$\", container=\"$CONTAINER_NAME$\"}[15m]))",
                            "version": ">4.9"
                        }
                    ]
                },
                {
                    "name": "cpuThrottle",
                    "query": "",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "container",
                    "aggregation_functions": [
                        {
                            "function": "avg",
                            "query": "avg(rate(container_cpu_cfs_throttled_seconds_total{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=\"$NAMESPACE$\", container=”$CONTAINER_NAME$”}[15m]))",
                            "version": ""
                        },
                        {
                            "function": "max",
                            "query": "max(rate(container_cpu_cfs_throttled_seconds_total{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=\"$NAMESPACE$\", container=”$CONTAINER_NAME$”}[15m]))",
                            "version": ""
                        },
                        {
                            "function": "sum",
                            "query": "sum(rate(container_cpu_cfs_throttled_seconds_total{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=\"$NAMESPACE$\", container=”$CONTAINER_NAME$”}[15m]))",
                            "version": ""
                        }
                    ]
                },
                {
                    "name": "memoryRequest",
                    "query": "",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "container",
                    "aggregation_functions": [
                        {
                            "function": "avg",
                            "query": "avg(kube_pod_container_resource_requests{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", container=”$CONTAINER_NAME$”, namespace=”$NAMESPACE”, resource=\"memory\", unit=\"byte\"})",
                            "version": ""
                        },
                        {
                            "function": "sum",
                            "query": "sum(kube_pod_container_resource_requests{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", container=”$CONTAINER_NAME$”, namespace=”$NAMESPACE”, resource=\"memory\", unit=\"byte\"})",
                            "version": ""
                        }
                    ]
                },
                {
                    "name": "memoryLimit",
                    "query": "",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "container",
                    "aggregation_functions": [
                        {
                            "function": "avg",
                            "query": "avg(kube_pod_container_resource_limits{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE\", resource=\"memory\", unit=\"byte\"})",
                            "version": ""
                        },
                        {
                            "function": "sum",
                            "query": "sum(kube_pod_container_resource_limits{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", container=”$CONTAINER_NAME$”, namespace=”$NAMESPACE”, resource=\"memory\", unit=\"byte\"})",
                            "version": ""
                        }
                    ]
                },
                {
                    "name": "memoryUsage",
                    "query": "",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "container",
                    "aggregation_functions": [
                        {
                            "function": "avg",
                            "query": "avg(avg_over_time(container_memory_working_set_bytes{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=$NAMESPACE$, container=”$CONTAINER_NAME$”}[15m]))",
                            "version": ""
                        },
                        {
                            "function": "min",
                            "query": "min(min_over_time(container_memory_working_set_bytes{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=$NAMESPACE$, container=\"$CONTAINER_NAME$\"}[15m]))",
                            "version": ""
                        },
                        {
                            "function": "max",
                            "query": "max(max_over_time(container_memory_working_set_bytes{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=$NAMESPACE$, container=\"$CONTAINER_NAME$\"}[15m]))",
                            "version": ""
                        },
                        {
                            "function": "sum",
                            "query": "sum(avg_over_time(container_memory_working_set_bytes{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=$NAMESPACE$, container=\"$CONTAINER_NAME$\"}[15m]))",
                            "version": ""
                        }
                    ]
                },
                {
                    "name": "memoryRSS",
                    "query": "",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "container",
                    "aggregation_functions": [
                        {
                            "function": "avg",
                            "query": "avg(avg_over_time(container_memory_rss{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=$NAMESPACE$, container=”$CONTAINER_NAME$”}[15m]))",
                            "version": ""
                        },
                        {
                            "function": "min",
                            "query": "min(min_over_time(container_memory_rss{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=$NAMESPACE$, container=\"$CONTAINER_NAME$\"}[15m]))",
                            "version": ""
                        },
                        {
                            "function": "max",
                            "query": "max(max_over_time(container_memory_rss{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=$NAMESPACE$, container=\"$CONTAINER_NAME$\"}[15m]))",
                            "version": ""
                        },
                        {
                            "function": "sum",
                            "query": "sum(avg_over_time(container_memory_rss{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=$NAMESPACE$, container=”$CONTAINER_NAME$”}[15m]))",
                            "version": ""
                        }
                    ]
                }
            ]
        }
    }
]
```