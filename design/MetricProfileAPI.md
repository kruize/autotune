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
            "query": "avg by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_requests{container!=\"\", container!=\"POD\", pod!=\"\", resource=\"cpu\", unit=\"core\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE\"} * on(pod, namespace) group_left max by (container,pod, namespace) (kube_pod_status_phase{phase=\"Running\"}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m]))))"
          },
          {
            "function": "sum",
            "query": "sum by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_requests{container!=\"\", container!=\"POD\", pod!=\"\", resource=\"cpu\", unit=\"core\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE\"} * on(pod, namespace) group_left max by (container,pod, namespace) (kube_pod_status_phase{phase=\"Running\"}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m]))))"
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
            "query": "avg by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_limits{container!=\"\", container!=\"POD\", pod!=\"\", resource=\"cpu\", unit=\"core\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"} * on(pod, namespace) group_left max by (container,pod, namespace) (kube_pod_status_phase{phase=\"Running\"}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m]))))",
            "version": ""
          },
          {
            "function": "sum",
            "query": "avg by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_limits{container!=\"\", container!=\"POD\", pod!=\"\", resource=\"cpu\", unit=\"core\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"} * on(pod, namespace) group_left max by (container,pod, namespace) (kube_pod_status_phase{phase=\"Running\"}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m]))))",
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
            "query": "avg by(namespace,container,workload,workload_type,owner_kind) (avg_over_time(((irate(container_cpu_usage_seconds_total{container!=\"\", container!=\"POD\", pod!=\"\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"}[5m])) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\", workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
            "version": "<=4.8"
          },
          {
            "function": "avg",
            "query": "avg by(namespace,container,workload,workload_type,owner_kind) (avg_over_time(((node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!=\"\", container!=\"POD\", pod!=\"\",container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
            "version": ">4.9"
          },
          {
            "function": "min",
            "query": "min by(namespace,container,workload,workload_type,owner_kind) (min_over_time(((irate(container_cpu_usage_seconds_total{container!=\"\", container!=\"POD\", pod!=\"\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"}[5m])) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\", workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
            "version": "<=4.8"
          },
          {
            "function": "min",
            "query": "min by(namespace,container,workload,workload_type,owner_kind) (min_over_time(((node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!=\"\", container!=\"POD\", pod!=\"\",container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
            "version": ">4.9"
          },
          {
            "function": "max",
            "query": "max by(namespace,container,workload,workload_type,owner_kind) (max_over_time(((irate(container_cpu_usage_seconds_total{container!=\"\", container!=\"POD\", pod!=\"\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"}[5m])) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\", workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
            "version": "<=4.8"
          },
          {
            "function": "max",
            "query": "max by(namespace,container,workload,workload_type,owner_kind) (max_over_time(((node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!=\"\", container!=\"POD\", pod!=\"\",container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
            "version": ">4.9"
          },
          {
            "function": "sum",
            "query": "sum by(namespace,container,workload,workload_type,owner_kind) (avg_over_time(((irate(container_cpu_usage_seconds_total{container!=\"\", container!=\"POD\", pod!=\"\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"}[5m])) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\", workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
            "version": "<=4.8"
          },
          {
            "function": "sum",
            "query": "sum by(namespace,container,workload,workload_type,owner_kind) (avg_over_time(((node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!=\"\", container!=\"POD\", pod!=\"\",container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
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
            "query": "avg by(namespace,container,workload,workload_type,owner_kind) (avg_over_time((rate(container_cpu_cfs_throttled_seconds_total{container!=\"\", container!=\"POD\", pod!=\"\",container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"}[15m]) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
            "version": ""
          },
          {
            "function": "max",
            "query": "max by(namespace,container,workload,workload_type,owner_kind) (max_over_time((rate(container_cpu_cfs_throttled_seconds_total{container!=\"\", container!=\"POD\", pod!=\"\",container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"}[15m]) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
            "version": ""
          },
          {
            "function": "min",
            "query": "min by(namespace,container,workload,workload_type,owner_kind) (min_over_time((rate(container_cpu_cfs_throttled_seconds_total{container!=\"\", container!=\"POD\", pod!=\"\",container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"}[15m]) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))"
          },
          {
            "function": "sum",
            "query": "sum by(namespace,container,workload,workload_type,owner_kind) (avg_over_time((rate(container_cpu_cfs_throttled_seconds_total{container!=\"\", container!=\"POD\", pod!=\"\",container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"}[15m]) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
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
            "query": "avg by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_requests{container!=\"\", container!=\"POD\", pod!=\"\", resource=\"memory\", unit=\"byte\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"} * on(pod, namespace) group_left max by (container,pod, namespace) (kube_pod_status_phase{phase=\"Running\"}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m]))))",
            "version": ""
          },
          {
            "function": "sum",
            "query": "sum by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_requests{container!=\"\", container!=\"POD\", pod!=\"\", resource=\"memory\", unit=\"byte\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"} * on(pod, namespace) group_left max by (container,pod, namespace) (kube_pod_status_phase{phase=\"Running\"}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m]))))",
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
            "query": "avg by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_limits{container!=\"\", container!=\"POD\", pod!=\"\", resource=\"memory\", unit=\"byte\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"} * on(pod, namespace) group_left max by (container,pod, namespace) (kube_pod_status_phase{phase=\"Running\"}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m]))))",
            "version": ""
          },
          {
            "function": "sum",
            "query": "sum by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_limits{container!=\"\", container!=\"POD\", pod!=\"\", resource=\"memory\", unit=\"byte\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"} * on(pod, namespace) group_left max by (container,pod, namespace) (kube_pod_status_phase{phase=\"Running\"}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m]))))",
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
            "query": "avg by(namespace,container,workload,workload_type,owner_kind) (avg_over_time((container_memory_working_set_bytes{container!=\"\", container!=\"POD\", pod!=\"\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
            "version": ""
          },
          {
            "function": "min",
            "query": "min by(namespace,container,workload,workload_type,owner_kind) (min_over_time((container_memory_working_set_bytes{container!=\"\", container!=\"POD\", pod!=\"\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
            "version": ""
          },
          {
            "function": "max",
            "query": "max by(namespace,container,workload,workload_type,owner_kind) (max_over_time((container_memory_working_set_bytes{container!=\"\", container!=\"POD\", pod!=\"\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
            "version": ""
          },
          {
            "function": "sum",
            "query": "sum by(namespace,container,workload,workload_type,owner_kind) (avg_over_time((container_memory_working_set_bytes{container!=\"\", container!=\"POD\", pod!=\"\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
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
            "query": "avg by(namespace,container,workload,workload_type,owner_kind) (avg_over_time((container_memory_rss{container!=\"\", container!=\"POD\", pod!=\"\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
            "version": ""
          },
          {
            "function": "min",
            "query": "min by(namespace,container,workload,workload_type,owner_kind) (min_over_time((container_memory_rss{container!=\"\", container!=\"POD\", pod!=\"\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
            "version": ""
          },
          {
            "function": "max",
            "query": "max by(namespace,container,workload,workload_type,owner_kind) (max_over_time((container_memory_rss{container!=\"\", container!=\"POD\", pod!=\"\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
            "version": ""
          },
          {
            "function": "sum",
            "query": "sum by(namespace,container,workload,workload_type,owner_kind) (avg_over_time((container_memory_rss{container!=\"\", container!=\"POD\", pod!=\"\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
            "version": ""
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
            "query": "last_over_time((timestamp(container_cpu_usage_seconds_total{container != \"\",  container!=\"POD\", pod!=\"\", namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"} > 0))[15d:]) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!='',workload=\"$WORKLOAD$\",workload_type=\"$WORKLOAD_TYPE$\"}[15d])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15d]))"
          }
        ]
      },
      {
        "name": "namespaceCpuRequest",
        "query": "",
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
        "query": "",
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
        "query": "",
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
        "query": "",
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
        "query": "",
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
        "query": "",
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
        "query": "",
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
        "query": "",
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
        "query": "",
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
        "query": "",
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
            "query": "avg by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_requests{container!=\"\", container!=\"POD\", pod!=\"\", resource=\"cpu\", unit=\"core\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE\"} * on(pod, namespace) group_left max by (container,pod, namespace) (kube_pod_status_phase{phase=\"Running\"}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m]))))"
          },
          {
            "function": "sum",
            "query": "sum by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_requests{container!=\"\", container!=\"POD\", pod!=\"\", resource=\"cpu\", unit=\"core\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE\"} * on(pod, namespace) group_left max by (container,pod, namespace) (kube_pod_status_phase{phase=\"Running\"}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m]))))"
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
            "query": "avg by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_limits{container!=\"\", container!=\"POD\", pod!=\"\", resource=\"cpu\", unit=\"core\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"} * on(pod, namespace) group_left max by (container,pod, namespace) (kube_pod_status_phase{phase=\"Running\"}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m]))))",
            "version": ""
          },
          {
            "function": "sum",
            "query": "avg by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_limits{container!=\"\", container!=\"POD\", pod!=\"\", resource=\"cpu\", unit=\"core\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"} * on(pod, namespace) group_left max by (container,pod, namespace) (kube_pod_status_phase{phase=\"Running\"}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m]))))",
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
            "query": "avg by(namespace,container,workload,workload_type,owner_kind) (avg_over_time(((irate(container_cpu_usage_seconds_total{container!=\"\", container!=\"POD\", pod!=\"\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"}[5m])) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\", workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
            "version": "<=4.8"
          },
          {
            "function": "avg",
            "query": "avg by(namespace,container,workload,workload_type,owner_kind) (avg_over_time(((node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!=\"\", container!=\"POD\", pod!=\"\",container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
            "version": ">4.9"
          },
          {
            "function": "min",
            "query": "min by(namespace,container,workload,workload_type,owner_kind) (min_over_time(((irate(container_cpu_usage_seconds_total{container!=\"\", container!=\"POD\", pod!=\"\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"}[5m])) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\", workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
            "version": "<=4.8"
          },
          {
            "function": "min",
            "query": "min by(namespace,container,workload,workload_type,owner_kind) (min_over_time(((node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!=\"\", container!=\"POD\", pod!=\"\",container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
            "version": ">4.9"
          },
          {
            "function": "max",
            "query": "max by(namespace,container,workload,workload_type,owner_kind) (max_over_time(((irate(container_cpu_usage_seconds_total{container!=\"\", container!=\"POD\", pod!=\"\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"}[5m])) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\", workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
            "version": "<=4.8"
          },
          {
            "function": "max",
            "query": "max by(namespace,container,workload,workload_type,owner_kind) (max_over_time(((node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!=\"\", container!=\"POD\", pod!=\"\",container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
            "version": ">4.9"
          },
          {
            "function": "sum",
            "query": "sum by(namespace,container,workload,workload_type,owner_kind) (avg_over_time(((irate(container_cpu_usage_seconds_total{container!=\"\", container!=\"POD\", pod!=\"\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"}[5m])) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\", workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
            "version": "<=4.8"
          },
          {
            "function": "sum",
            "query": "sum by(namespace,container,workload,workload_type,owner_kind) (avg_over_time(((node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!=\"\", container!=\"POD\", pod!=\"\",container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
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
            "query": "avg by(namespace,container,workload,workload_type,owner_kind) (avg_over_time((rate(container_cpu_cfs_throttled_seconds_total{container!=\"\", container!=\"POD\", pod!=\"\",container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"}[15m]) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
            "version": ""
          },
          {
            "function": "max",
            "query": "max by(namespace,container,workload,workload_type,owner_kind) (max_over_time((rate(container_cpu_cfs_throttled_seconds_total{container!=\"\", container!=\"POD\", pod!=\"\",container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"}[15m]) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
            "version": ""
          },
          {
            "function": "min",
            "query": "min by(namespace,container,workload,workload_type,owner_kind) (min_over_time((rate(container_cpu_cfs_throttled_seconds_total{container!=\"\", container!=\"POD\", pod!=\"\",container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"}[15m]) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))"
          },
          {
            "function": "sum",
            "query": "sum by(namespace,container,workload,workload_type,owner_kind) (avg_over_time((rate(container_cpu_cfs_throttled_seconds_total{container!=\"\", container!=\"POD\", pod!=\"\",container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"}[15m]) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
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
            "query": "avg by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_requests{container!=\"\", container!=\"POD\", pod!=\"\", resource=\"memory\", unit=\"byte\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"} * on(pod, namespace) group_left max by (container,pod, namespace) (kube_pod_status_phase{phase=\"Running\"}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m]))))",
            "version": ""
          },
          {
            "function": "sum",
            "query": "sum by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_requests{container!=\"\", container!=\"POD\", pod!=\"\", resource=\"memory\", unit=\"byte\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"} * on(pod, namespace) group_left max by (container,pod, namespace) (kube_pod_status_phase{phase=\"Running\"}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m]))))",
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
            "query": "avg by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_limits{container!=\"\", container!=\"POD\", pod!=\"\", resource=\"memory\", unit=\"byte\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"} * on(pod, namespace) group_left max by (container,pod, namespace) (kube_pod_status_phase{phase=\"Running\"}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m]))))",
            "version": ""
          },
          {
            "function": "sum",
            "query": "sum by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_limits{container!=\"\", container!=\"POD\", pod!=\"\", resource=\"memory\", unit=\"byte\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"} * on(pod, namespace) group_left max by (container,pod, namespace) (kube_pod_status_phase{phase=\"Running\"}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m]))))",
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
            "query": "avg by(namespace,container,workload,workload_type,owner_kind) (avg_over_time((container_memory_working_set_bytes{container!=\"\", container!=\"POD\", pod!=\"\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
            "version": ""
          },
          {
            "function": "min",
            "query": "min by(namespace,container,workload,workload_type,owner_kind) (min_over_time((container_memory_working_set_bytes{container!=\"\", container!=\"POD\", pod!=\"\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
            "version": ""
          },
          {
            "function": "max",
            "query": "max by(namespace,container,workload,workload_type,owner_kind) (max_over_time((container_memory_working_set_bytes{container!=\"\", container!=\"POD\", pod!=\"\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
            "version": ""
          },
          {
            "function": "sum",
            "query": "sum by(namespace,container,workload,workload_type,owner_kind) (avg_over_time((container_memory_working_set_bytes{container!=\"\", container!=\"POD\", pod!=\"\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
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
            "query": "avg by(namespace,container,workload,workload_type,owner_kind) (avg_over_time((container_memory_rss{container!=\"\", container!=\"POD\", pod!=\"\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
            "version": ""
          },
          {
            "function": "min",
            "query": "min by(namespace,container,workload,workload_type,owner_kind) (min_over_time((container_memory_rss{container!=\"\", container!=\"POD\", pod!=\"\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
            "version": ""
          },
          {
            "function": "max",
            "query": "max by(namespace,container,workload,workload_type,owner_kind) (max_over_time((container_memory_rss{container!=\"\", container!=\"POD\", pod!=\"\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
            "version": ""
          },
          {
            "function": "sum",
            "query": "sum by(namespace,container,workload,workload_type,owner_kind) (avg_over_time((container_memory_rss{container!=\"\", container!=\"POD\", pod!=\"\", container=\"$CONTAINER_NAME$\", namespace=\"$NAMESPACE$\"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"$WORKLOAD$\", workload_type=\"$WORKLOAD_TYPE$\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[15m:]))",
            "version": ""
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
            "query": "last_over_time((timestamp(container_cpu_usage_seconds_total{container != \"\",  container!=\"POD\", pod!=\"\", namespace=\"$NAMESPACE$\",container=\"$CONTAINER_NAME$\"} > 0))[15d:]) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!='',workload=\"$WORKLOAD$\",workload_type=\"$WORKLOAD_TYPE$\"}[15d])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15d]))"
          }
        ]
      },
      {
        "name": "namespaceCpuRequest",
        "query": "",
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
        "query": "",
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
        "query": "",
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
        "query": "",
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
        "query": "",
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
        "query": "",
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
        "query": "",
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
        "query": "",
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
        "query": "",
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
        "query": "",
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
      }
    ]
  }
}
]
```