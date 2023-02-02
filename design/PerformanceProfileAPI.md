# Performance Profile

This article describes how to add/update Performance Profiles with REST APIs using curl command.
Documentation still in progress stay tuned.

## CreatePerformanceProfile

This is quick guide instructions to create performance profile using input JSON as follows. For a more detailed guide,
see [Create Performance Profile](/design/CreatePerformanceProfile.md)

**Request**
`POST /createPerformanceProfile`

`curl -H 'Accept: application/json' -X POST --data 'copy paste below JSON' http://<URL>:<PORT>/createPerformanceProfile`

```
{
  "apiVersion": "recommender.com/v1",
  "kind": "KruizePerformanceProfile",
  "metadata": {
    "name": "resource-optimization-openshift"
  },
  "profile_version": 1,
  "slo": {
    "direction": "minimize",
    "objective_function": {
        "type": "expression",
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

## Update Performance Profile

Update Performance Profile data using input JSON as follows.

**Request**
`POST /updatePerformanceProfiles`

`curl -H 'Accept: application/json' -X POST --data 'copy paste below JSON' http://<URL>:<PORT>/updatePerformanceProfiles`

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
    "slo_class": "resource_usage",
    "direction": "minimize",
    "objective_function": {
        "type": "expression",
        "expression": "request_sum/request_count"
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
            "query": "avg(kube_pod_container_resource_requests{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE\", resource=\"cpu\", unit=\"core\"})"
          },
          {
            "function": "sum",
            "query": "sum(kube_pod_container_resource_requests{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE\", resource=\"cpu\", unit=\"core\"})"
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
            "query": "avg(kube_pod_container_resource_limits{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE\", resource=\"cpu\", unit=\"core\"})"
          },
          {
            "function": "sum",
            "query": "sum(kube_pod_container_resource_limits{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\", resource=\"cpu\", unit=\"core\"})"
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
            "query": "avg(avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_rate{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=\"$NAMESPACE$\", container=”$CONTAINER_NAME$”}[15m]))",
            "versions": "<=4.8"
          },
          {
            "function": "avg",
            "query": "avg(avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=\"$NAMESPACE$\", container=”$CONTAINER_NAME$”}[15m]))",
            "versions": ">4.9"
          },
          {
            "function": "min",
            "query": "min(min_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_rate{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=\"$NAMESPACE$\", container=\"$CONTAINER_NAME$\"}[15m]))",
            "versions": "<=4.8"
          },
          {
            "function": "min",
            "query": "min(min_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=\"$NAMESPACE$\", container=\"$CONTAINER_NAME$\"}[15m]))",
            "versions": ">4.9"
          },
          {
            "function": "max",
            "query": "max(max_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_rate{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=\"$NAMESPACE$\", container=\"$CONTAINER_NAME$\"}[15m]))",
            "versions": "<=4.8"
          },
          {
            "function": "max",
            "query": "max(max_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=\"$NAMESPACE$\", container=\"$CONTAINER_NAME$\"}[15m]))",
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
            "query": "rate(container_cpu_cfs_throttled_seconds_total{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=\"$NAMESPACE$\", container=”$CONTAINER_NAME$”}[15m])"
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
            "query": "avg(kube_pod_container_resource_requests{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", container=”$CONTAINER_NAME$”, namespace=”$NAMESPACE”, resource=\"memory\", unit=\"byte\"})"
          },
          {
            "function": "sum",
            "query": "sum(kube_pod_container_resource_requests{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", container=”$CONTAINER_NAME$”, namespace=”$NAMESPACE”, resource=\"memory\", unit=\"byte\"})"
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
            "query": "avg(kube_pod_container_resource_limits{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE\", resource=\"memory\", unit=\"byte\"})"
          },
          {
            "function": "sum",
            "query": "sum(kube_pod_container_resource_limits{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", container=”$CONTAINER_NAME$”, namespace=”$NAMESPACE”, resource=\"memory\", unit=\"byte\"})"
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
            "query": "avg(avg_over_time(container_memory_working_set_bytes{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=$NAMESPACE$, container=”$CONTAINER_NAME$”}[15m]))"
          },
          {
            "function": "min",
            "query": "min(min_over_time(container_memory_working_set_bytes{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=$NAMESPACE$, container=\"$CONTAINER_NAME$\"}[15m]))"
          },
          {
            "function": "max",
            "query": "max(max_over_time(container_memory_working_set_bytes{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=$NAMESPACE$, container=\"$CONTAINER_NAME$\"}[15m]))"
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
            "query": "avg(avg_over_time(container_memory_rss{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=$NAMESPACE$, container=”$CONTAINER_NAME$”}[15m]))"
          },
          {
            "function": "min",
            "query": "min(min_over_time(container_memory_rss{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=$NAMESPACE$, container=\"$CONTAINER_NAME$\"}[15m]))"
          },
          {
            "function": "max",
            "query": "max(max_over_time(container_memory_rss{pod=~\"$DEPLOYMENT_NAME$-[^-]*-[^-]*$\", namespace=$NAMESPACE$, container=\"$CONTAINER_NAME$\"}[15m]))"
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
    "message": "Updated performance profile successfully. View updated profile at /listPerformanceProfiles",
    "httpcode": 201,
    "documentationLink": "",
    "status": "SUCCESS"
}
```
