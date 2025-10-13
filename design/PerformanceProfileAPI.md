# Performance Profile

This article describes how to add/update Performance Profiles with REST APIs using curl command.
Documentation still in progress stay tuned.

## CreatePerformanceProfile

This is quick guide instructions to create performance profile using input JSON as follows. For a more detailed guide,
see [Create Performance Profile](/design/PerformanceProfile.md)

**Request**
`POST /createPerformanceProfile`

`curl -H 'Accept: application/json' -X POST --data 'copy paste below JSON' http://<URL>:<PORT>/createPerformanceProfile`

```
{
  "name": "resource-optimization-openshift",
  "profile_version": 1,
  "slo": {
    "direction": "minimize",
    "objective_function": {
        "function_type": "expression",
        "expression": "request_sum/request_count"
    },
    "function_variables": [
      {
        "name": "cpuRequest",
        "datasource": "prometheus",
        "value_type": "double",
        "query": "kube_pod_container_resource_requests{pod=~'$DEPLOYMENT_NAME$-[^-]*-[^-]*$', container='$CONTAINER_NAME$', namespace='$NAMESPACE', resource='cpu', unit='core'}",
        "aggregation_functions": [
          {
            "function": "avg",
            "query": "avg(kube_pod_container_resource_requests{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE\", resource=\"cpu\", unit=\"core\"})"
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
    "message": "Performance Profile : <name> created successfully. View all performance profiles at /listPerformanceProfiles",
    "httpcode": 201,
    "documentationLink": "",
    "status": "SUCCESS"
}
```
#### Note: One of query or aggregation_functions is mandatory. Both can be present together.

## List Performance Profiles

List performance profiles output JSON as follows.

**Request**
`GET /listPerformanceProfiles`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listPerformanceProfiles`

**Response**

```
[
    {
        "name": "resource-optimization-openshift",
        "profile_version": 2.0,
        "k8s_type": "openshift",
        "slo": {
            "sloClass": "resource_usage",
            "objective_function": {
                "function_type": "source"
            },
            "direction": "minimize",
            "function_variables": [
                {
                    "name": "cpuRequest",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "container",
                    "aggregation_functions": {
                        "sum": {
                            "function": "sum",
                            "query": "sum by(container, namespace)(kube_pod_container_resource_requests{container!=\"\", container!=\"POD\", pod!=\"\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE\", resource=\"cpu\", unit=\"core\"})"
                        }
                    }
                },
                {
                    "name": "cpuLimit",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "container",
                    "aggregation_functions": {
                        "sum": {
                            "function": "sum",
                            "query": "sum by(container, namespace)(kube_pod_container_resource_limits{container!=\"\", container!=\"POD\", pod!=\"\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\", resource=\"cpu\", unit=\"core\"})"
                        }
                    }
                },
                {
                    "name": "cpuUsage",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "container",
                    "aggregation_functions": {
                        "sum": {
                            "function": "sum",
                            "query": "sum by(container, namespace)(avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!=\"\", container!=\"POD\", pod!=\"\", namespace=\"$NAMESPACE$\", container=\"$CONTAINER_NAME$\"}[15m]))"
                        }
                    }
                },
                {
                    "name": "cpuThrottle",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "container",
                    "aggregation_functions": {
                        "sum": {
                            "function": "sum",
                            "query": "sum by(container,namespace)(rate(container_cpu_cfs_throttled_seconds_total{container!=\"\", container!=\"POD\", pod!=\"\", namespace=\"$NAMESPACE$\", container=”$CONTAINER_NAME$”}[15m]))"
                        }
                    }
                },
                {
                    "name": "memoryRequest",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "container",
                    "aggregation_functions": {
                        "sum": {
                            "function": "sum",
                            "query": "sum by(container,namespace)(kube_pod_container_resource_requests{container!=\"\", container!=\"POD\", pod!=\"\", container=”$CONTAINER_NAME$”, namespace=”$NAMESPACE”, resource=\"memory\", unit=\"byte\"})"
                        }
                    }
                },
                {
                    "name": "memoryLimit",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "container",
                    "aggregation_functions": {
                        "sum": {
                            "function": "sum",
                            "query": "sum by(container,namespace)(kube_pod_container_resource_limits{container!=\"\", container!=\"POD\", pod!=\"\", container=”$CONTAINER_NAME$”, namespace=”$NAMESPACE”, resource=\"memory\", unit=\"byte\"})"
                        }
                    }
                },
                {
                    "name": "memoryUsage",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "container",
                    "aggregation_functions": {
                        "sum": {
                            "function": "sum",
                            "query": "sum by(container,namespace)(avg_over_time(container_memory_working_set_bytes{container!=\"\", container!=\"POD\", pod!=\"\", namespace=$NAMESPACE$, container=\"$CONTAINER_NAME$\"}[15m]))"
                        }
                    }
                },
                {
                    "name": "memoryRSS",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "container",
                    "aggregation_functions": {
                        "sum": {
                            "function": "sum",
                            "query": "sum by(container,namespace)(avg_over_time(container_memory_rss{container!=\"\", container!=\"POD\", pod!=\"\", namespace=$NAMESPACE$, container=”$CONTAINER_NAME$”}[15m]))"
                        }
                    }
                },
                {
                    "name": "namespaceCpuRequest",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "namespace",
                    "aggregation_functions": {
                        "sum": {
                            "function": "sum",
                            "query": "sum by (namespace) (kube_resourcequota{namespace=\"$NAMESPACE$\", resource=\"requests.cpu\", type=\"hard\"})"
                        }
                    }
                },
                {
                    "name": "namespaceCpuLimit",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "namespace",
                    "aggregation_functions": {
                        "sum": {
                            "function": "sum",
                            "query": "sum by (namespace) (kube_resourcequota{namespace=\"$NAMESPACE$\", resource=\"limits.cpu\", type=\"hard\"})"
                        }
                    }
                },
                {
                    "name": "namespaceMemoryRequest",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "namespace",
                    "aggregation_functions": {
                        "sum": {
                            "function": "sum",
                            "query": "sum by (namespace) (kube_resourcequota{namespace=\"$NAMESPACE$\", resource=\"requests.memory\", type=\"hard\"})"
                        }
                    }
                },
                {
                    "name": "namespaceMemoryLimit",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "namespace",
                    "aggregation_functions": {
                        "sum": {
                            "function": "sum",
                            "query": "sum by (namespace) (kube_resourcequota{namespace=\"$NAMESPACE$\", resource=\"limits.memory\", type=\"hard\"})"
                        }
                    }
                },
                {
                    "name": "namespaceCpuUsage",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "namespace",
                    "aggregation_functions": {
                        "min": {
                            "function": "min",
                            "query": "min_over_time(sum by(namespace) (node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{namespace=\"$NAMESPACE$\", container!=\"\", container!=\"POD\", pod!=\"\"})[15m:])"
                        }
                    }
                },
                {
                    "name": "namespaceCpuThrottle",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "namespace",
                    "aggregation_functions": {
                        "min": {
                            "function": "min",
                            "query": "min_over_time(sum by(namespace) (rate(container_cpu_cfs_throttled_seconds_total{namespace=\"$NAMESPACE$\", container!=\"\", container!=\"POD\", pod!=\"\"}[5m]))[15m:])"
                        }
                    }
                },
                {
                    "name": "namespaceMemoryUsage",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "namespace",
                    "aggregation_functions": {
                        "min": {
                            "function": "min",
                            "query": "min_over_time(sum by(namespace) (container_memory_working_set_bytes{namespace=\"$NAMESPACE$\", container!=\"\", container!=\"POD\", pod!=\"\"})[15m:])"
                        }
                    }
                },
                {
                    "name": "namespaceMemoryRSS",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "namespace",
                    "aggregation_functions": {
                        "min": {
                            "function": "min",
                            "query": "min_over_time(sum by(namespace) (container_memory_rss{namespace=\"$NAMESPACE$\", container!=\"\", container!=\"POD\", pod!=\"\"})[15m:])"
                        }
                    }
                },
                {
                    "name": "namespaceTotalPods",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "namespace",
                    "aggregation_functions": {
                        "avg": {
                            "function": "avg",
                            "query": "avg_over_time(sum by(namespace) ((kube_pod_info{namespace=\"$NAMESPACE$\"}))[15m:])"
                        }
                    }
                },
                {
                    "name": "namespaceRunningPods",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "namespace",
                    "aggregation_functions": {
                        "avg": {
                            "function": "avg",
                            "query": "avg_over_time(sum by(namespace) ((kube_pod_status_phase{phase=\"Running\"}))[15m:])"
                        }
                    }
                },
                {
                    "name": "namespaceMaxDate",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "namespace",
                    "aggregation_functions": {
                        "max": {
                            "function": "max",
                            "query": "max(last_over_time(timestamp((sum by (namespace) (container_cpu_usage_seconds_total{namespace=\"$NAMESPACE$\"})) > 0 )[15d:]))"
                        }
                    }
                }
            ]
        }
    }
]
```


## UpdatePerformanceProfile

This is quick guide instructions to update performance profile using input JSON as follows.

**Request**
`POST /updatePerformanceProfile`

`curl -H 'Accept: application/json' -X POST --data 'copy paste below JSON' http://<URL>:<PORT>/updatePerformanceProfile`

```
{
  "name": "resource-optimization-openshift",
  "profile_version": 2.0,
  "k8s_type": "openshift",
  "slo": {
    "slo_class": "resource_usage",
    "direction": "minimize",
    "objective_function": {
      "function_type": "source"
    },
    "function_variables": [
      {
        "name": "cpuRequest",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "container",
        "aggregation_functions": [
          {
            "function": "avg",
            "query": "avg by(container, namespace)(kube_pod_container_resource_requests{container!=\"\", container!=\"POD\", pod!=\"\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE\", resource=\"cpu\", unit=\"core\"})"
          },
          {
            "function": "sum",
            "query": "sum by(container, namespace)(kube_pod_container_resource_requests{container!=\"\", container!=\"POD\", pod!=\"\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE\", resource=\"cpu\", unit=\"core\"})"
          }
        ]
      },
      {
        "name": "cpuLimit",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "container",
        "aggregation_functions": [
          {
            "function": "avg",
            "query": "avg by(container, namespace)(kube_pod_container_resource_limits{container!=\"\", container!=\"POD\", pod!=\"\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE\", resource=\"cpu\", unit=\"core\"})"
          },
          {
            "function": "sum",
            "query": "sum by(container, namespace)(kube_pod_container_resource_limits{container!=\"\", container!=\"POD\", pod!=\"\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\", resource=\"cpu\", unit=\"core\"})"
          }
        ]
      },
      {
        "name": "cpuUsage",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "container",
        "aggregation_functions": [
          {
            "function": "avg",
            "query": "avg by(container, namespace)(avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_rate{container!=\"\", container!=\"POD\", pod!=\"\", namespace=\"$NAMESPACE$\", container=”$CONTAINER_NAME$”}[15m]))",
            "versions": "<=4.8"
          },
          {
            "function": "avg",
            "query": "avg by(container, namespace)(avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!=\"\", container!=\"POD\", pod!=\"\", namespace=\"$NAMESPACE$\", container=”$CONTAINER_NAME$”}[15m]))",
            "versions": ">4.9"
          },
          {
            "function": "min",
            "query": "min by(container, namespace)(min_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_rate{container!=\"\", container!=\"POD\", pod!=\"\", namespace=\"$NAMESPACE$\", container=\"$CONTAINER_NAME$\"}[15m]))",
            "versions": "<=4.8"
          },
          {
            "function": "min",
            "query": "min by(container, namespace)(min_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!=\"\", container!=\"POD\", pod!=\"\", namespace=\"$NAMESPACE$\", container=\"$CONTAINER_NAME$\"}[15m]))",
            "versions": ">4.9"
          },
          {
            "function": "max",
            "query": "max by(container, namespace)(max_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_rate{container!=\"\", container!=\"POD\", pod!=\"\", namespace=\"$NAMESPACE$\", container=\"$CONTAINER_NAME$\"}[15m]))",
            "versions": "<=4.8"
          },
          {
            "function": "max",
            "query": "max by(container, namespace)(max_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!=\"\", container!=\"POD\", pod!=\"\", namespace=\"$NAMESPACE$\", container=\"$CONTAINER_NAME$\"}[15m]))",
            "versions": ">4.9"
          },
          {
            "function": "sum",
            "query": "sum by(container, namespace)(avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_rate{container!=\"\", container!=\"POD\", pod!=\"\", namespace=\"$NAMESPACE$\", container=\"$CONTAINER_NAME$\"}[15m]))",
            "versions": "<=4.8"
          },
          {
            "function": "sum",
            "query": "sum by(container, namespace)(avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!=\"\", container!=\"POD\", pod!=\"\", namespace=\"$NAMESPACE$\", container=\"$CONTAINER_NAME$\"}[15m]))",
            "versions": ">4.9"
          }
        ]
      },
      {
        "name": "cpuThrottle",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "container",
        "aggregation_functions": [
          {
            "function": "avg",
            "query": "avg by(container,namespace)(rate(container_cpu_cfs_throttled_seconds_total{container!=\"\", container!=\"POD\", pod!=\"\", namespace=\"$NAMESPACE$\", container=”$CONTAINER_NAME$”}[15m]))"
          },
          {
            "function": "max",
            "query": "max by(container,namespace)(rate(container_cpu_cfs_throttled_seconds_total{container!=\"\", container!=\"POD\", pod!=\"\", namespace=\"$NAMESPACE$\", container=”$CONTAINER_NAME$”}[15m]))"
          },
          {
            "function": "sum",
            "query": "sum by(container,namespace)(rate(container_cpu_cfs_throttled_seconds_total{container!=\"\", container!=\"POD\", pod!=\"\", namespace=\"$NAMESPACE$\", container=”$CONTAINER_NAME$”}[15m]))"
          }
        ]
      },
      {
        "name": "memoryRequest",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "container",
        "aggregation_functions": [
          {
            "function": "avg",
            "query": "avg by(container,namespace)(kube_pod_container_resource_requests{container!=\"\", container!=\"POD\", pod!=\"\", container=”$CONTAINER_NAME$”, namespace=”$NAMESPACE”, resource=\"memory\", unit=\"byte\"})"
          },
          {
            "function": "sum",
            "query": "sum by(container,namespace)(kube_pod_container_resource_requests{container!=\"\", container!=\"POD\", pod!=\"\", container=”$CONTAINER_NAME$”, namespace=”$NAMESPACE”, resource=\"memory\", unit=\"byte\"})"
          }
        ]
      },
      {
        "name": "memoryLimit",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "container",
        "aggregation_functions": [
          {
            "function": "avg",
            "query": "avg by(container,namespace)(kube_pod_container_resource_limits{container!=\"\", container!=\"POD\", pod!=\"\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE\", resource=\"memory\", unit=\"byte\"})"
          },
          {
            "function": "sum",
            "query": "sum by(container,namespace)(kube_pod_container_resource_limits{container!=\"\", container!=\"POD\", pod!=\"\", container=”$CONTAINER_NAME$”, namespace=”$NAMESPACE”, resource=\"memory\", unit=\"byte\"})"
          }
        ]
      },
      {
        "name": "memoryUsage",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "container",
        "aggregation_functions": [
          {
            "function": "avg",
            "query": "avg by(container,namespace)(avg_over_time(container_memory_working_set_bytes{container!=\"\", container!=\"POD\", pod!=\"\", namespace=$NAMESPACE$, container=”$CONTAINER_NAME$”}[15m]))"
          },
          {
            "function": "min",
            "query": "min by(container,namespace)(min_over_time(container_memory_working_set_bytes{container!=\"\", container!=\"POD\", pod!=\"\", namespace=$NAMESPACE$, container=\"$CONTAINER_NAME$\"}[15m]))"
          },
          {
            "function": "max",
            "query": "max by(container,namespace)(max_over_time(container_memory_working_set_bytes{container!=\"\", container!=\"POD\", pod!=\"\", namespace=$NAMESPACE$, container=\"$CONTAINER_NAME$\"}[15m]))"
          },
          {
            "function": "sum",
            "query": "sum by(container,namespace)(avg_over_time(container_memory_working_set_bytes{container!=\"\", container!=\"POD\", pod!=\"\", namespace=$NAMESPACE$, container=\"$CONTAINER_NAME$\"}[15m]))"
          }
        ]
      },
      {
        "name": "memoryRSS",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "container",
        "aggregation_functions": [
          {
            "function": "avg",
            "query": "avg by(container,namespace)(avg_over_time(container_memory_rss{container!=\"\", container!=\"POD\", pod!=\"\", namespace=$NAMESPACE$, container=”$CONTAINER_NAME$”}[15m]))"
          },
          {
            "function": "min",
            "query": "min by(container,namespace)(min_over_time(container_memory_rss{container!=\"\", container!=\"POD\", pod!=\"\", namespace=$NAMESPACE$, container=\"$CONTAINER_NAME$\"}[15m]))"
          },
          {
            "function": "max",
            "query": "max by(container,namespace)(max_over_time(container_memory_rss{container!=\"\", container!=\"POD\", pod!=\"\", namespace=$NAMESPACE$, container=\"$CONTAINER_NAME$\"}[15m]))"
          },
          {
            "function": "sum",
            "query": "sum by(container,namespace)(avg_over_time(container_memory_rss{container!=\"\", container!=\"POD\", pod!=\"\", namespace=$NAMESPACE$, container=”$CONTAINER_NAME$”}[15m]))"
          }
        ]
      },
      {
        "name": "namespaceCpuRequest",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "namespace",
        "aggregation_functions": [
          {
            "function": "sum",
            "query": "sum by (namespace) (kube_resourcequota{namespace=\"$NAMESPACE$\", resource=\"requests.cpu\", type=\"hard\"})"
          }
        ]
      },
      {
        "name": "namespaceCpuLimit",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "namespace",
        "aggregation_functions": [
          {
            "function": "sum",
            "query": "sum by (namespace) (kube_resourcequota{namespace=\"$NAMESPACE$\", resource=\"limits.cpu\", type=\"hard\"})"
          }
        ]
      },
      {
        "name": "namespaceMemoryRequest",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "namespace",
        "aggregation_functions": [
          {
            "function": "sum",
            "query": "sum by (namespace) (kube_resourcequota{namespace=\"$NAMESPACE$\", resource=\"requests.memory\", type=\"hard\"})"
          }
        ]
      },
      {
        "name": "namespaceMemoryLimit",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "namespace",
        "aggregation_functions": [
          {
            "function": "sum",
            "query": "sum by (namespace) (kube_resourcequota{namespace=\"$NAMESPACE$\", resource=\"limits.memory\", type=\"hard\"})"
          }
        ]
      },
      {
        "name": "namespaceCpuUsage",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "namespace",
        "aggregation_functions": [
          {
            "function": "avg",
            "query": "avg_over_time(sum by(namespace) (node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{namespace=\"$NAMESPACE$\", container!=\"\", container!=\"POD\", pod!=\"\"})[15m:])"
          },
          {
            "function": "max",
            "query": "max_over_time(sum by(namespace) (node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{namespace=\"$NAMESPACE$\", container!=\"\", container!=\"POD\", pod!=\"\"})[15m:])"
          },
          {
            "function": "min",
            "query": "min_over_time(sum by(namespace) (node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{namespace=\"$NAMESPACE$\", container!=\"\", container!=\"POD\", pod!=\"\"})[15m:])"
          }
        ]
      },
      {
        "name": "namespaceCpuThrottle",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "namespace",
        "aggregation_functions": [
          {
            "function": "avg",
            "query": "avg_over_time(sum by(namespace) (rate(container_cpu_cfs_throttled_seconds_total{namespace=\"$NAMESPACE$\", container!=\"\", container!=\"POD\", pod!=\"\"}[5m]))[15m:])"
          },
          {
            "function": "max",
            "query": "max_over_time(sum by(namespace) (rate(container_cpu_cfs_throttled_seconds_total{namespace=\"$NAMESPACE$\", container!=\"\", container!=\"POD\", pod!=\"\"}[5m]))[15m:])"
          },
          {
            "function": "min",
            "query": "min_over_time(sum by(namespace) (rate(container_cpu_cfs_throttled_seconds_total{namespace=\"$NAMESPACE$\", container!=\"\", container!=\"POD\", pod!=\"\"}[5m]))[15m:])"
          }
        ]
      },
      {
        "name": "namespaceMemoryUsage",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "namespace",
        "aggregation_functions": [
          {
            "function": "avg",
            "query": "avg_over_time(sum by(namespace) (container_memory_working_set_bytes{namespace=\"$NAMESPACE$\", container!=\"\", container!=\"POD\", pod!=\"\"})[15m:])"
          },
          {
            "function": "max",
            "query": "max_over_time(sum by(namespace) (container_memory_working_set_bytes{namespace=\"$NAMESPACE$\", container!=\"\", container!=\"POD\", pod!=\"\"})[15m:])"
          },
          {
            "function": "min",
            "query": "min_over_time(sum by(namespace) (container_memory_working_set_bytes{namespace=\"$NAMESPACE$\", container!=\"\", container!=\"POD\", pod!=\"\"})[15m:])"
          }
        ]
      },
      {
        "name": "namespaceMemoryRSS",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "namespace",
        "aggregation_functions": [
          {
            "function": "avg",
            "query": "avg_over_time(sum by(namespace) (container_memory_rss{namespace=\"$NAMESPACE$\", container!=\"\", container!=\"POD\", pod!=\"\"})[15m:])"
          },
          {
            "function": "max",
            "query": "max_over_time(sum by(namespace) (container_memory_rss{namespace=\"$NAMESPACE$\", container!=\"\", container!=\"POD\", pod!=\"\"})[15m:])"
          },
          {
            "function": "min",
            "query": "min_over_time(sum by(namespace) (container_memory_rss{namespace=\"$NAMESPACE$\", container!=\"\", container!=\"POD\", pod!=\"\"})[15m:])"
          }
        ]
      },
      {
        "name": "namespaceTotalPods",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "namespace",
        "aggregation_functions": [
          {
            "function": "max",
            "query": "max_over_time(sum by(namespace) ((kube_pod_info{namespace=\"$NAMESPACE$\"}))[15m:])"
          },
          {
            "function": "avg",
            "query": "avg_over_time(sum by(namespace) ((kube_pod_info{namespace=\"$NAMESPACE$\"}))[15m:])"
          }
        ]
      },
      {
        "name": "namespaceRunningPods",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "namespace",
        "aggregation_functions": [
          {
            "function": "max",
            "query": "max_over_time(sum by(namespace) ((kube_pod_status_phase{phase=\"Running\"}))[15m:])"
          },
          {
            "function": "avg",
            "query": "avg_over_time(sum by(namespace) ((kube_pod_status_phase{phase=\"Running\"}))[15m:])"
          }
        ]
      },
      {
        "name": "namespaceMaxDate",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "namespace",
        "aggregation_functions": [
          {
            "function": "max",
            "query": "max(last_over_time(timestamp((sum by (namespace) (container_cpu_usage_seconds_total{namespace=\"$NAMESPACE$\"})) > 0 )[15d:]))"
          }
        ]
      },
      {
        "name": "acceleratorCoreUsage",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "container",
        "aggregation_functions": [
          {
            "function": "min",
            "query": "min(min_over_time(DCGM_FI_DEV_GPU_UTIL{exported_namespace != \"\", exported_container != \"\", exported_pod != \"\"}[15m])) by (modelName, exported_container, exported_namespace, exported_pod, Hostname)"
          },
          {
            "function": "max",
            "query": "max(max_over_time(DCGM_FI_DEV_GPU_UTIL{exported_namespace != \"\", exported_container != \"\", exported_pod != \"\"}[15m])) by (modelName, exported_container, exported_namespace, exported_pod, Hostname)"
          },
          {
            "function": "avg",
            "query": "avg(avg_over_time(DCGM_FI_DEV_GPU_UTIL{exported_namespace != \"\", exported_container != \"\", exported_pod != \"\"}[15m])) by (modelName, exported_container, exported_namespace, exported_pod, Hostname)"
          }
        ]
      },
      {
        "name": "acceleratorMemoryUsage",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "container",
        "aggregation_functions": [
          {
            "function": "min",
            "query": "min(min_over_time(DCGM_FI_DEV_MEM_COPY_UTIL{exported_namespace != \"\", exported_container != \"\", exported_pod != \"\"}[15m])) by (modelName, exported_container, exported_namespace, exported_pod, Hostname)"
          },
          {
            "function": "max",
            "query": "max(max_over_time(DCGM_FI_DEV_MEM_COPY_UTIL{exported_namespace != \"\", exported_container != \"\", exported_pod != \"\"}[15m])) by (modelName, exported_container, exported_namespace, exported_pod, Hostname)"
          },
          {
            "function": "avg",
            "query": "avg(avg_over_time(DCGM_FI_DEV_MEM_COPY_UTIL{exported_namespace != \"\", exported_container != \"\", exported_pod != \"\"}[15m])) by (modelName, exported_container, exported_namespace, exported_pod, Hostname)"
          }
        ]
      },
      {
        "name": "acceleratorFrameBufferUsage",
        "datasource": "prometheus",
        "value_type": "double",
        "kubernetes_object": "container",
        "aggregation_functions": [
          {
            "function": "min",
            "query": "min(min_over_time(DCGM_FI_DEV_FB_USED{exported_namespace != \"\", exported_container != \"\", exported_pod != \"\"}[15m])) by (modelName, GPU_I_PROFILE, exported_container, exported_namespace, exported_pod, Hostname)"
          },
          {
            "function": "max",
            "query": "max(max_over_time(DCGM_FI_DEV_FB_USED{exported_namespace != \"\", exported_container != \"\", exported_pod != \"\"}[15m])) by (modelName, GPU_I_PROFILE, exported_container, exported_namespace, exported_pod, Hostname)"
          },
          {
            "function": "avg",
            "query": "avg(avg_over_time(DCGM_FI_DEV_FB_USED{exported_namespace != \"\", exported_container != \"\", exported_pod != \"\"}[15m])) by (modelName, GPU_I_PROFILE, exported_container, exported_namespace, exported_pod, Hostname)"
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
    "message": "Performance Profile : <name> updated successfully to version <new-version>. View all performance profiles at /listPerformanceProfiles",
    "httpcode": 201,
    "documentationLink": "",
    "status": "SUCCESS"
}
```
#### Note: One of query or aggregation_functions is mandatory. Both can be present together.
