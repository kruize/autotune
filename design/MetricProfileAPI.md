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
                        "query": "avg by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD ', pod!='', resource='cpu', unit='core' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
                    },
                    {
                        "function": "sum",
                        "query": "sum by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='cpu', unit='core' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
                    },
                    {
                        "function": "min",
                        "query": "min by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='cpu', unit='core' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
                    },
                    {
                        "function": "max",
                        "query": "max by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='cpu', unit='core' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
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
                        "query": "avg by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='cpu', unit='core',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
                    },
                    {
                        "function": "sum",
                        "query": "sum by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='cpu', unit='core',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
                    },
                    {
                        "function": "max",
                        "query": "max by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='cpu', unit='core',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
                    },
                    {
                        "function": "min",
                        "query": "min by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='cpu', unit='core',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
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
                        "query": "avg by(container, namespace)(avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\" }[$MEASUREMENT_DURATION_IN_MIN$m]))"

                    },
                    {
                        "function": "min",
                        "query": "min by(container, namespace)(min_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\" }[$MEASUREMENT_DURATION_IN_MIN$m]))"

                    },
                    {
                        "function": "max",
                        "query": "max by(container, namespace)(max_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\" }[$MEASUREMENT_DURATION_IN_MIN$m]))"

                    },
                    {
                        "function": "sum",
                        "query": "sum by(container, namespace)(avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\" }[$MEASUREMENT_DURATION_IN_MIN$m]))"
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
                        "query": "avg by(container,namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
                    },
                    {
                        "function": "max",
                        "query": "max by(container,namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
                    },
                    {
                        "function": "min",
                        "query": "min by(container,namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
                    },
                    {
                        "function": "sum",
                        "query": "sum by(container,namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
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
                        "query": "avg by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='memory', unit='byte' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
                    },
                    {
                        "function": "sum",
                        "query": "sum by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='memory', unit='byte' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
                    },
                    {
                        "function": "max",
                        "query": "max by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='memory', unit='byte' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
                    },
                    {
                        "function": "min",
                        "query": "min by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='memory', unit='byte' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
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
                        "query": "avg by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='memory', unit='byte',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
                    },
                    {
                        "function": "sum",
                        "query": "sum by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='memory', unit='byte',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
                    },
                    {
                        "function": "max",
                        "query": "max by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='memory', unit='byte',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
                    },
                    {
                        "function": "min",
                        "query": "min by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='memory', unit='byte',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
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
                        "query": "avg by(container, namespace) (avg_over_time(container_memory_working_set_bytes{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
                    },
                    {
                        "function": "min",
                        "query": "min by(container, namespace) (min_over_time(container_memory_working_set_bytes{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\" }[$MEASUREMENT_DURATION_IN_MIN$m]))"
                    },
                    {
                        "function": "max",
                        "query": "max by(container, namespace) (max_over_time(container_memory_working_set_bytes{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
                    },
                    {
                        "function": "sum",
                        "query": "sum by(container, namespace) (avg_over_time(container_memory_working_set_bytes{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
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
                        "query": "avg by(container, namespace) (avg_over_time(container_memory_rss{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
                    },
                    {
                        "function": "min",
                        "query": "min by(container, namespace) (min_over_time(container_memory_rss{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
                    },
                    {
                        "function": "max",
                        "query": "max by(container, namespace) (max_over_time(container_memory_rss{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
                    },
                    {
                        "function": "sum",
                        "query": "sum by(container, namespace) (avg_over_time(container_memory_rss{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
                    }
                ]
            },
            {
                "name": "maxDate",
                "datasource": "prometheus",
                "value_type": "double",
                "kubernetes_object": "container",
                "aggregation_functions": [
                    {
                        "function": "max",
                        "query": "max by(namespace,container) (last_over_time((timestamp(container_cpu_usage_seconds_total{namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"} > 0))[15d:]))"
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
                        "query": "sum by (namespace) (kube_resourcequota{namespace=\"$NAMESPACE$\", resource=\"requests.cpu\", type=\"hard\"})",
                        "version": ""
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
                        "query": "sum by (namespace) (kube_resourcequota{namespace=\"$NAMESPACE$\", resource=\"limits.cpu\", type=\"hard\"})",
                        "version": ""
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
                        "query": "sum by (namespace) (kube_resourcequota{namespace=\"$NAMESPACE$\", resource=\"requests.memory\", type=\"hard\"})",
                        "version": ""
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
                        "query": "sum by (namespace) (kube_resourcequota{namespace=\"$NAMESPACE$\", resource=\"limits.memory\", type=\"hard\"})",
                        "version": ""
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
                        "query": "avg_over_time(sum by(namespace) (node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{namespace=\"$NAMESPACE$\", container!='', container!='POD', pod!=''})[$MEASUREMENT_DURATION_IN_MIN$m:])",
                        "version": ""
                    },
                    {
                        "function": "max",
                        "query": "max_over_time(sum by(namespace) (node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{namespace=\"$NAMESPACE$\", container!='', container!='POD', pod!=''})[$MEASUREMENT_DURATION_IN_MIN$m:])",
                        "version": ""
                    },
                    {
                        "function": "min",
                        "query": "min_over_time(sum by(namespace) (node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{namespace=\"$NAMESPACE$\", container!='', container!='POD', pod!=''})[$MEASUREMENT_DURATION_IN_MIN$m:])",
                        "version": ""
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
                        "query": "avg_over_time(sum by(namespace) (rate(container_cpu_cfs_throttled_seconds_total{namespace=\"$NAMESPACE$\", container!='', container!='POD', pod!=''}[5m]))[$MEASUREMENT_DURATION_IN_MIN$m:])",
                        "version": ""
                    },
                    {
                        "function": "max",
                        "query": "max_over_time(sum by(namespace) (rate(container_cpu_cfs_throttled_seconds_total{namespace=\"$NAMESPACE$\", container!='', container!='POD', pod!=''}[5m]))[$MEASUREMENT_DURATION_IN_MIN$m:])",
                        "version": ""
                    },
                    {
                        "function": "min",
                        "query": "min_over_time(sum by(namespace) (rate(container_cpu_cfs_throttled_seconds_total{namespace=\"$NAMESPACE$\", container!='', container!='POD', pod!=''}[5m]))[$MEASUREMENT_DURATION_IN_MIN$m:])",
                        "version": ""
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
                        "query": "avg_over_time(sum by(namespace) (container_memory_working_set_bytes{namespace=\"$NAMESPACE$\", container!='', container!='POD', pod!=''})[$MEASUREMENT_DURATION_IN_MIN$m:])",
                        "version": ""
                    },
                    {
                        "function": "max",
                        "query": "max_over_time(sum by(namespace) (container_memory_working_set_bytes{namespace=\"$NAMESPACE$\", container!='', container!='POD', pod!=''})[$MEASUREMENT_DURATION_IN_MIN$m:])",
                        "version": ""
                    },
                    {
                        "function": "min",
                        "query": "min_over_time(sum by(namespace) (container_memory_working_set_bytes{namespace=\"$NAMESPACE$\", container!='', container!='POD', pod!=''})[$MEASUREMENT_DURATION_IN_MIN$m:])",
                        "version": ""
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
                        "query": "avg_over_time(sum by(namespace) (container_memory_rss{namespace=\"$NAMESPACE$\", container!='', container!='POD', pod!=''})[$MEASUREMENT_DURATION_IN_MIN$m:])",
                        "version": ""
                    },
                    {
                        "function": "max",
                        "query": "max_over_time(sum by(namespace) (container_memory_rss{namespace=\"$NAMESPACE$\", container!='', container!='POD', pod!=''})[$MEASUREMENT_DURATION_IN_MIN$m:])",
                        "version": ""
                    },
                    {
                        "function": "min",
                        "query": "min_over_time(sum by(namespace) (container_memory_rss{namespace=\"$NAMESPACE$\", container!='', container!='POD', pod!=''})[$MEASUREMENT_DURATION_IN_MIN$m:])",
                        "version": ""
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
                        "function": "sum",
                        "query": "sum(count(kube_pod_status_phase{namespace=\"$NAMESPACE$\"}))",
                        "version": ""
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
                        "function": "sum",
                        "query": "sum(count(kube_pod_status_phase{namespace=\"$NAMESPACE$\", phase=\"Running\"}))",
                        "version": ""
                    }
                ]
            },
            {
                "name": "gpuCoreUsage",
                "datasource": "prometheus",
                "value_type": "double",
                "kubernetes_object": "container",
                "aggregation_functions": [
                    {
                        "function": "avg",
                        "query": "avg by (Hostname,device,modelName,UUID,exported_container,exported_namespace) (avg_over_time(DCGM_FI_DEV_GPU_UTIL{exported_namespace=\"$NAMESPACE$\",exported_container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))",
                        "version": ""
                    },
                    {
                        "function": "min",
                        "query": "min by (Hostname,device,modelName,UUID,exported_container,exported_namespace) (min_over_time(DCGM_FI_DEV_GPU_UTIL{exported_namespace=\"$NAMESPACE$\",exported_container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))",
                        "version": ""
                    },
                    {
                        "function": "max",
                        "query": "max by (Hostname,device,modelName,UUID,exported_container,exported_namespace) (max_over_time(DCGM_FI_DEV_GPU_UTIL{exported_namespace=\"$NAMESPACE$\",exported_container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))",
                        "version": ""
                    }
                ]
            },
            {
                "name": "gpuMemoryUsage",
                "datasource": "prometheus",
                "value_type": "double",
                "kubernetes_object": "container",
                "aggregation_functions": [
                    {
                        "function": "avg",
                        "query": "avg by (Hostname,device,modelName,UUID,exported_container,exported_namespace) (avg_over_time(DCGM_FI_DEV_MEM_COPY_UTIL{exported_namespace=\"$NAMESPACE$\",exported_container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))",
                        "version": ""
                    },
                    {
                        "function": "min",
                        "query": "min by (Hostname,device,modelName,UUID,exported_container,exported_namespace) (min_over_time(DCGM_FI_DEV_MEM_COPY_UTIL{exported_namespace=\"$NAMESPACE$\",exported_container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))",
                        "version": ""
                    },
                    {
                        "function": "max",
                        "query": "max by (Hostname,device,modelName,UUID,exported_container,exported_namespace) (max_over_time(DCGM_FI_DEV_MEM_COPY_UTIL{exported_namespace=\"$NAMESPACE$\",exported_container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))",
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
        "name": "resource-optimization-openshift"
    }
]
```

**Request**
`GET /listMetricProfiles`

`curl -H 'Accept: application/json' http://<URL>:<PORT>/listMetricProfiles?name=resource-optimization-openshift`


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
            "function_variables": [
                {
                    "name": "cpuRequest",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "container",
                    "aggregation_functions": {
                        "avg": {
                            "function": "avg",
                            "query": "avg by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD ', pod!='', resource='cpu', unit='core' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
                        },
                        "min": {
                            "function": "min",
                            "query": "min by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='cpu', unit='core' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
                        },
                        "max": {
                            "function": "max",
                            "query": "max by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='cpu', unit='core' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
                        },
                        "sum": {
                            "function": "sum",
                            "query": "sum by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='cpu', unit='core' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
                        }
                    }
                },
                {
                    "name": "cpuLimit",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "container",
                    "aggregation_functions": {
                        "avg": {
                            "function": "avg",
                            "query": "avg by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='cpu', unit='core',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
                        },
                        "min": {
                            "function": "min",
                            "query": "min by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='cpu', unit='core',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
                        },
                        "max": {
                            "function": "max",
                            "query": "max by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='cpu', unit='core',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
                        },
                        "sum": {
                            "function": "sum",
                            "query": "sum by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='cpu', unit='core',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
                        }
                    }
                },
                {
                    "name": "cpuUsage",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "container",
                    "aggregation_functions": {
                        "avg": {
                            "function": "avg",
                            "query": "avg by(container, namespace)(avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\" }[$MEASUREMENT_DURATION_IN_MIN$m]))"
                        },
                        "min": {
                            "function": "min",
                            "query": "min by(container, namespace)(min_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\" }[$MEASUREMENT_DURATION_IN_MIN$m]))"
                        },
                        "max": {
                            "function": "max",
                            "query": "max by(container, namespace)(max_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\" }[$MEASUREMENT_DURATION_IN_MIN$m]))"
                        },
                        "sum": {
                            "function": "sum",
                            "query": "sum by(container, namespace)(avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\" }[$MEASUREMENT_DURATION_IN_MIN$m]))"
                        }
                    }
                },
                {
                    "name": "cpuThrottle",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "container",
                    "aggregation_functions": {
                        "avg": {
                            "function": "avg",
                            "query": "avg by(container,namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
                        },
                        "min": {
                            "function": "min",
                            "query": "min by(container,namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
                        },
                        "max": {
                            "function": "max",
                            "query": "max by(container,namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
                        },
                        "sum": {
                            "function": "sum",
                            "query": "sum by(container,namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
                        }
                    }
                },
                {
                    "name": "memoryRequest",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "container",
                    "aggregation_functions": {
                        "avg": {
                            "function": "avg",
                            "query": "avg by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='memory', unit='byte' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
                        },
                        "min": {
                            "function": "min",
                            "query": "min by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='memory', unit='byte' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
                        },
                        "max": {
                            "function": "max",
                            "query": "max by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='memory', unit='byte' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
                        },
                        "sum": {
                            "function": "sum",
                            "query": "sum by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='memory', unit='byte' ,namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
                        }
                    }
                },
                {
                    "name": "memoryLimit",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "container",
                    "aggregation_functions": {
                        "avg": {
                            "function": "avg",
                            "query": "avg by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='memory', unit='byte',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
                        },
                        "min": {
                            "function": "min",
                            "query": "min by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='memory', unit='byte',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
                        },
                        "max": {
                            "function": "max",
                            "query": "max by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='memory', unit='byte',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
                        },
                        "sum": {
                            "function": "sum",
                            "query": "sum by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='memory', unit='byte',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"})"
                        }
                    }
                },
                {
                    "name": "memoryUsage",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "container",
                    "aggregation_functions": {
                        "avg": {
                            "function": "avg",
                            "query": "avg by(container, namespace) (avg_over_time(container_memory_working_set_bytes{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
                        },
                        "min": {
                            "function": "min",
                            "query": "min by(container, namespace) (min_over_time(container_memory_working_set_bytes{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\" }[$MEASUREMENT_DURATION_IN_MIN$m]))"
                        },
                        "max": {
                            "function": "max",
                            "query": "max by(container, namespace) (max_over_time(container_memory_working_set_bytes{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
                        },
                        "sum": {
                            "function": "sum",
                            "query": "sum by(container, namespace) (avg_over_time(container_memory_working_set_bytes{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
                        }
                    }
                },
                {
                    "name": "memoryRSS",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "container",
                    "aggregation_functions": {
                        "avg": {
                            "function": "avg",
                            "query": "avg by(container, namespace) (avg_over_time(container_memory_rss{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
                        },
                        "min": {
                            "function": "min",
                            "query": "min by(container, namespace) (min_over_time(container_memory_rss{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
                        },
                        "max": {
                            "function": "max",
                            "query": "max by(container, namespace) (max_over_time(container_memory_rss{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
                        },
                        "sum": {
                            "function": "sum",
                            "query": "sum by(container, namespace) (avg_over_time(container_memory_rss{container!='', container!='POD', pod!='',namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))"
                        }
                    }
                },
                {
                    "name": "maxDate",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "container",
                    "aggregation_functions": {
                        "max": {
                            "function": "max",
                            "query": "max by(namespace,container) (last_over_time((timestamp(container_cpu_usage_seconds_total{namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"} > 0))[15d:]))"
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
                            "query": "sum by (namespace) (kube_resourcequota{namespace=\"$NAMESPACE$\", resource=\"requests.cpu\", type=\"hard\"})",
                            "version": ""
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
                            "query": "sum by (namespace) (kube_resourcequota{namespace=\"$NAMESPACE$\", resource=\"limits.cpu\", type=\"hard\"})",
                            "version": ""
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
                            "query": "sum by (namespace) (kube_resourcequota{namespace=\"$NAMESPACE$\", resource=\"requests.memory\", type=\"hard\"})",
                            "version": ""
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
                            "query": "sum by (namespace) (kube_resourcequota{namespace=\"$NAMESPACE$\", resource=\"limits.memory\", type=\"hard\"})",
                            "version": ""
                        }
                    }
                },
                {
                    "name": "namespaceCpuUsage",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "namespace",
                    "aggregation_functions": {
                        "avg": {
                            "function": "avg",
                            "query": "avg_over_time(sum by(namespace) (node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{namespace=\"$NAMESPACE$\", container!='', container!='POD', pod!=''})[$MEASUREMENT_DURATION_IN_MIN$m:])",
                            "version": ""
                        },
                        "min": {
                            "function": "min",
                            "query": "min_over_time(sum by(namespace) (node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{namespace=\"$NAMESPACE$\", container!='', container!='POD', pod!=''})[$MEASUREMENT_DURATION_IN_MIN$m:])",
                            "version": ""
                        },
                        "max": {
                            "function": "max",
                            "query": "max_over_time(sum by(namespace) (node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{namespace=\"$NAMESPACE$\", container!='', container!='POD', pod!=''})[$MEASUREMENT_DURATION_IN_MIN$m:])",
                            "version": ""
                        }
                    }
                },
                {
                    "name": "namespaceCpuThrottle",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "namespace",
                    "aggregation_functions": {
                        "avg": {
                            "function": "avg",
                            "query": "avg_over_time(sum by(namespace) (rate(container_cpu_cfs_throttled_seconds_total{namespace=\"$NAMESPACE$\", container!='', container!='POD', pod!=''}[5m]))[$MEASUREMENT_DURATION_IN_MIN$m:])",
                            "version": ""
                        },
                        "min": {
                            "function": "min",
                            "query": "min_over_time(sum by(namespace) (rate(container_cpu_cfs_throttled_seconds_total{namespace=\"$NAMESPACE$\", container!='', container!='POD', pod!=''}[5m]))[$MEASUREMENT_DURATION_IN_MIN$m:])",
                            "version": ""
                        },
                        "max": {
                            "function": "max",
                            "query": "max_over_time(sum by(namespace) (rate(container_cpu_cfs_throttled_seconds_total{namespace=\"$NAMESPACE$\", container!='', container!='POD', pod!=''}[5m]))[$MEASUREMENT_DURATION_IN_MIN$m:])",
                            "version": ""
                        }
                    }
                },
                {
                    "name": "namespaceMemoryUsage",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "namespace",
                    "aggregation_functions": {
                        "avg": {
                            "function": "avg",
                            "query": "avg_over_time(sum by(namespace) (container_memory_working_set_bytes{namespace=\"$NAMESPACE$\", container!='', container!='POD', pod!=''})[$MEASUREMENT_DURATION_IN_MIN$m:])",
                            "version": ""
                        },
                        "min": {
                            "function": "min",
                            "query": "min_over_time(sum by(namespace) (container_memory_working_set_bytes{namespace=\"$NAMESPACE$\", container!='', container!='POD', pod!=''})[$MEASUREMENT_DURATION_IN_MIN$m:])",
                            "version": ""
                        },
                        "max": {
                            "function": "max",
                            "query": "max_over_time(sum by(namespace) (container_memory_working_set_bytes{namespace=\"$NAMESPACE$\", container!='', container!='POD', pod!=''})[$MEASUREMENT_DURATION_IN_MIN$m:])",
                            "version": ""
                        }
                    }
                },
                {
                    "name": "namespaceMemoryRSS",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "namespace",
                    "aggregation_functions": {
                        "avg": {
                            "function": "avg",
                            "query": "avg_over_time(sum by(namespace) (container_memory_rss{namespace=\"$NAMESPACE$\", container!='', container!='POD', pod!=''})[$MEASUREMENT_DURATION_IN_MIN$m:])",
                            "version": ""
                        },
                        "min": {
                            "function": "min",
                            "query": "min_over_time(sum by(namespace) (container_memory_rss{namespace=\"$NAMESPACE$\", container!='', container!='POD', pod!=''})[$MEASUREMENT_DURATION_IN_MIN$m:])",
                            "version": ""
                        },
                        "max": {
                            "function": "max",
                            "query": "max_over_time(sum by(namespace) (container_memory_rss{namespace=\"$NAMESPACE$\", container!='', container!='POD', pod!=''})[$MEASUREMENT_DURATION_IN_MIN$m:])",
                            "version": ""
                        }
                    }
                },
                {
                    "name": "namespaceTotalPods",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "namespace",
                    "aggregation_functions": {
                        "sum": {
                            "function": "sum",
                            "query": "sum(count(kube_pod_status_phase{namespace=\"$NAMESPACE$\"}))",
                            "version": ""
                        }
                    }
                },
                {
                    "name": "namespaceRunningPods",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "namespace",
                    "aggregation_functions": {
                        "sum": {
                            "function": "sum",
                            "query": "sum(count(kube_pod_status_phase{namespace=\"$NAMESPACE$\", phase=\"Running\"}))",
                            "version": ""
                        }
                    }
                },
                {
                    "name": "gpuCoreUsage",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "container",
                    "aggregation_functions": {
                        "avg": {
                            "function": "avg",
                            "query": "avg by (Hostname,device,modelName,UUID,exported_container,exported_namespace) (avg_over_time(DCGM_FI_DEV_GPU_UTIL{exported_namespace=\"$NAMESPACE$\",exported_container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))",
                            "version": ""
                        },
                        "min": {
                            "function": "min",
                            "query": "min by (Hostname,device,modelName,UUID,exported_container,exported_namespace) (min_over_time(DCGM_FI_DEV_GPU_UTIL{exported_namespace=\"$NAMESPACE$\",exported_container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))",
                            "version": ""
                        },
                        "max": {
                            "function": "max",
                            "query": "max by (Hostname,device,modelName,UUID,exported_container,exported_namespace) (max_over_time(DCGM_FI_DEV_GPU_UTIL{exported_namespace=\"$NAMESPACE$\",exported_container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))",
                            "version": ""
                        }
                    }
                },
                {
                    "name": "gpuMemoryUsage",
                    "datasource": "prometheus",
                    "value_type": "double",
                    "kubernetes_object": "container",
                    "aggregation_functions": {
                        "avg": {
                            "function": "avg",
                            "query": "avg by (Hostname,device,modelName,UUID,exported_container,exported_namespace) (avg_over_time(DCGM_FI_DEV_MEM_COPY_UTIL{exported_namespace=\"$NAMESPACE$\",exported_container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))",
                            "version": ""
                        },
                        "min": {
                            "function": "min",
                            "query": "min by (Hostname,device,modelName,UUID,exported_container,exported_namespace) (min_over_time(DCGM_FI_DEV_MEM_COPY_UTIL{exported_namespace=\"$NAMESPACE$\",exported_container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))",
                            "version": ""
                        },
                        "max": {
                            "function": "max",
                            "query": "max by (Hostname,device,modelName,UUID,exported_container,exported_namespace) (max_over_time(DCGM_FI_DEV_MEM_COPY_UTIL{exported_namespace=\"$NAMESPACE$\",exported_container=\"$CONTAINER_NAME$\"}[$MEASUREMENT_DURATION_IN_MIN$m]))",
                            "version": ""
                        }
                    }
                }
            ]
        }
    }
]
```