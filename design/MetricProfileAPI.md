# Metric Profile

This article describes how to add Metric Profiles with REST APIs using curl command.
Documentation still in progress stay tuned.

## CreateMetricProfile

This is quick guide instructions to create metric profile using input JSON as follows. For a more detailed guide,
see [Create Metric Profile](/design/MetricProfile.md)

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

```
{
    "message": "Metric Profile : <name> created successfully. View all metric profiles at /listMetricProfiles",
    "httpcode": 201,
    "documentationLink": "",
    "status": "SUCCESS"
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