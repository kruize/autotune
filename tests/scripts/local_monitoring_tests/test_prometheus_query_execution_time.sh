#!/bin/bash

# Variables
PROMETHEUS_NAMESPACE="openshift-monitoring"
SERVICE_NAME="prometheus-k8s"

export PROMETHEUS_ROUTE=$(oc get route $SERVICE_NAME -n $PROMETHEUS_NAMESPACE --no-headers -o wide -o=custom-columns=NODE:.spec.host)
echo $PROMETHEUS_ROUTE

oc -n $PROMETHEUS_NAMESPACE annotate route $SERVICE_NAME --overwrite haproxy.router.openshift.io/timeout=200s

PROMETHEUS_URL="https://${PROMETHEUS_ROUTE}"

echo $PROMETHEUS_URL

export TOKEN=$(oc whoami --show-token)

# List of Prometheus queries
default_queries=(
    'avg by(container, namespace) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    'sum by(container, namespace) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    'max by(container, namespace) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    'min by(container, namespace) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    'avg by(container, namespace) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    'sum by(container, namespace) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    'max by(container, namespace) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    'min by(container, namespace) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    'avg by(container, namespace) (avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    'sum by(container, namespace) (avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    'max by(container, namespace) (max_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    'min by(container, namespace) (min_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    'avg by(container, namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    'sum by(container, namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    'max by(container, namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    'min by(container, namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    'avg by(container, namespace) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    'sum by(container, namespace) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    'max by(container, namespace) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    'min by(container, namespace) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    'avg by(container, namespace) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    'sum by(container, namespace) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    'max by(container, namespace) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    'min by(container, namespace) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    'avg by(container, namespace) (avg_over_time(container_memory_working_set_bytes{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    'sum by(container, namespace) (avg_over_time(container_memory_working_set_bytes{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    'max by(container, namespace) (max_over_time(container_memory_working_set_bytes{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    'min by(container, namespace) (min_over_time(container_memory_working_set_bytes{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    'avg by(container, namespace) (avg_over_time(container_memory_rss{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    'sum by(container, namespace) (avg_over_time(container_memory_rss{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    'max by(container, namespace) (max_over_time(container_memory_rss{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    'min by(container, namespace) (min_over_time(container_memory_rss{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
)

individual_queries_by_pod=(
    'avg by(container, pod, namespace) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    'sum by(container, pod, namespace) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    'max by(container, pod, namespace) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    'min by(container, pod, namespace) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    'avg by(container, pod, namespace) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    'sum by(container, pod, namespace) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    'max by(container, pod, namespace) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    'min by(container, pod, namespace) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    'avg by(container, pod, namespace) (avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    'sum by(container, pod, namespace) (avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    'max by(container, pod, namespace) (max_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    'min by(container, pod, namespace) (min_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    'avg by(container, pod, namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    'sum by(container, pod, namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    'max by(container, pod, namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    'min by(container, pod, namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    'avg by(container, pod, namespace) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    'sum by(container, pod, namespace) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    'max by(container, pod, namespace) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    'min by(container, pod, namespace) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    'avg by(container, pod, namespace) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    'sum by(container, pod, namespace) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    'max by(container, pod, namespace) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    'min by(container, pod, namespace) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    'avg by(container, pod, namespace) (avg_over_time(container_memory_working_set_bytes{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    'sum by(container, pod, namespace) (avg_over_time(container_memory_working_set_bytes{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    'max by(container, pod, namespace) (max_over_time(container_memory_working_set_bytes{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    'min by(container, pod, namespace) (min_over_time(container_memory_working_set_bytes{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    'avg by(container, pod, namespace) (avg_over_time(container_memory_rss{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    'sum by(container, pod, namespace) (avg_over_time(container_memory_rss{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    'max by(container, pod, namespace) (max_over_time(container_memory_rss{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    'min by(container, pod, namespace) (min_over_time(container_memory_rss{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    '(max_over_time(kube_pod_container_info{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m])) * on(pod, namespace) group_left(owner_kind, owner_name) max by(pod, namespace, owner_kind, owner_name) (max_over_time(kube_pod_owner{container!="", container!="POD", pod!="", namespace="$NAMESPACE"}[15m]))'
    '(max_over_time(kube_pod_container_info{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m])) * on(pod, namespace) group_left(workload, workload_type) max by(pod, namespace, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!="", namespace="$NAMESPACE"}[15m]))'
)

grouped_queries_by_owner_workload=(
    'sum by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
     * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
     * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
    ))'
    'avg by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
      * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
      * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
    ))'
    'max by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
      * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
      * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
    ))'
    'min by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
      * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
      * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
    ))'
    'avg by(container, namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
       * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
       * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
    ))'
    'sum by(container, namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
        * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
        * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
    ))'
    'max by(container, namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
       * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
       * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
    ))'
    'min by(container, namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
     * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
     * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
    ))'
    'avg_over_time(
    avg by(namespace,container,workload,workload_type,owner_kind) (
      (node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"})
     * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
     * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
    )[15m:])'
    'min_over_time(
      min by(namespace,container,workload,workload_type,owner_kind) (
        (node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"})
       * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
       * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
     )[15m:] )'
    'max_over_time(
      max by(namespace,container,workload,workload_type,owner_kind) (
        (node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"})
       * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
       * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
     )[15m:] )'
    'avg_over_time(
      sum by(namespace,container,workload,workload_type,owner_kind) (
        (node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"})
       * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
       * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
     )[15m:] )'
    'avg_over_time(
      avg by(namespace,container,workload,workload_type,owner_kind) (
        (rate(container_cpu_cfs_throttled_seconds_total{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]) )
       * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
       * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
     )[15m:] )'
    'max_over_time(
      max by(namespace,container,workload,workload_type,owner_kind) (
         (rate(container_cpu_cfs_throttled_seconds_total{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]) )
         * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
         * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
      )[15m:] )'
    'min_over_time(
      min by(namespace,container,workload,workload_type,owner_kind) (
        (rate(container_cpu_cfs_throttled_seconds_total{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]) )
       * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
       * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
    )[15m:] )'
    'avg_over_time(
      sum by(namespace,container,workload,workload_type,owner_kind) (
        (rate(container_cpu_cfs_throttled_seconds_total{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]) )
       * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
       * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
    )[15m:] )'
    'avg by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
     * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
     * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
     ))'
     'sum by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
      * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
      * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
      ))'
     'max by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
      * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
      * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
      ))'
      'min by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
      * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
      * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
      ))'
     'avg by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
       * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
       * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
     ))'
     'sum by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
      * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
      * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
      ))'
      'max by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
         * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
         * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
      ))'
      'min by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
         * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
         * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
      ))'
      'avg_over_time(
        avg by(namespace,container,workload,workload_type,owner_kind) (
          container_memory_working_set_bytes{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
         * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
         * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
      )[15m:] )'
      'min_over_time(
        min by(namespace,container,workload,workload_type,owner_kind) (
          container_memory_working_set_bytes{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
         * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
         * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
      )[15m:] )'
      'max_over_time(
        max by(namespace,container,workload,workload_type,owner_kind) (
          container_memory_working_set_bytes{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
         * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
         * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
      )[15m:])'
      'avg_over_time(
         sum by(namespace,container,workload,workload_type,owner_kind) (
           container_memory_working_set_bytes{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
           * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
           * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
      )[15m:] )'
      'avg_over_time(
        avg by(namespace,container,workload,workload_type,owner_kind) (
          container_memory_rss{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
         * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
         * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
      )[15m:] )'
      'min_over_time(
        min by(namespace,container,workload,workload_type,owner_kind) (
          container_memory_rss{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
         * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
         * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
      )[15m:] )'
      'max_over_time(
        max by(namespace,container,workload,workload_type,owner_kind) (
          container_memory_rss{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
          * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
          * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
      )[15m:] )'
      'avg_over_time(
        sum by(namespace,container,workload,workload_type,owner_kind) (
          container_memory_rss{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
         * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
         * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
      )[15m:] )'
)

# Function to return the correct query set as an array reference
get_queries() {
    local query_set="$1"
    local -n return_array="$2"

    case "$query_set" in
        "default_queries")
            return_array=("${default_queries[@]}")
            ;;
        "individual_queries")
            return_array=("${individual_queries_by_pod[@]}")
            ;;
        "grouped_queries")
            return_array=("${grouped_queries_by_owner_workload[@]}")
            ;;
        "grouped_queriesBy5days")
            return_array=("${grouped_queries_by_owner_workload[@]}")
            ;;
        *)
            echo "Invalid query set. Available sets are: individual_queries, grouped_queries, grouped_queriesBy5days."
            return 1  # Return error
            ;;
    esac
}

# Each query is sequentially executed dividing the 15days duration into three, 5 days window
run_query_across_5day_windows() {
    local query="$1"
    local namespace="$NAMESPACE"
    local container="$CONTAINER"
    local start_timestamp="$4"
    local end_timestamp="$5"

    FIVE_DAYS_IN_SECONDS=$((5 * 24 * 60 * 60))  # 5 days in seconds


    FIRST_END_TIME=$((start_timestamp + FIVE_DAYS_IN_SECONDS))

    SECOND_START_TIME=$((FIRST_END_TIME))
    SECOND_END_TIME=$((SECOND_START_TIME + FIVE_DAYS_IN_SECONDS))

    THIRD_START_TIME=$((SECOND_END_TIME))
    THIRD_END_TIME=$((THIRD_START_TIME + FIVE_DAYS_IN_SECONDS))

    # Sequentially run the query across the 3 windows
    measure_query_time "$query" "$namespace" "$container" "$START_TIME" "$FIRST_END_TIME"

    measure_query_time "$query" "$namespace" "$container" "$SECOND_START_TIME" "$SECOND_END_TIME"

    measure_query_time "$query" "$namespace" "$container" "$THIRD_START_TIME" "$THIRD_END_TIME"
}


# Function to send a Prometheus query and measure the time taken
measure_query_time() {
    local query="$1"
    local namespace="$2"
    local container="$3"
    local start_timestamp="$4"
    local end_timestamp="$5"

    # Replace placeholders in the query with the actual namespace and container
    query=${query//\$NAMESPACE/$namespace}
    query=${query//\$CONTAINER_NAME/$container}

    start_time=$(date +%s.%N)

    response=$(curl -G -kH "Authorization: Bearer ${TOKEN}" \
            --data-urlencode "query=${query}" \
            --data-urlencode "start=${start_timestamp}" \
            --data-urlencode "end=${end_timestamp}" \
            --data-urlencode "step=900" \
            "${PROMETHEUS_URL}/api/v1/query_range")
    echo "$response" >> "$RESPONSE_LOG_FILE"

    end_time=$(date +%s.%N)

    time_taken=$(echo "$end_time - $start_time" | bc)

    status=$(echo "$response" | jq -r '.status')

     if [[ "$status" == "success" ]]; then
        echo "Success, ${time_taken}, ${start_timestamp}, ${end_timestamp}, ${query}" >> "$OUTPUT_FILE"
     else
        error_type=$(echo "$response" | jq -r '.errorType')
        error_message=$(echo "$response" | jq -r '.error')
        echo "Failed | ErrorType: $error_type | Error: $error_message", ${time_taken}, ${start_timestamp}, ${end_timestamp}, ${query}>> "$OUTPUT_FILE"
     fi
}

# Function to capture resource metrics (CPU and memory) of Prometheus pods
capture_prometheus_resource_metrics() {
    echo -e "\n===== Prometheus Pod Resource Metrics (CPU & Memory) =====" >> "$RESPONSE_LOG_FILE"
    # Get resource usage for Prometheus pods in the given namespace
    kubectl top pod -n "$PROMETHEUS_NAMESPACE" | grep "prometheus" >> "$RESPONSE_LOG_FILE"
    echo -e "=========================================================\n" >> "$RESPONSE_LOG_FILE"
}


DEFAULT_NAMESPACE="openshift-cloud-controller-manager-operator"
DEFAULT_CONTAINER="config-sync-controllers"
DEFAULT_QUERY_SET="default_queries"
DEFAULT_END_TIME=$(date +%s)
DEFAULT_START_TIME=$(date -d "15 days ago" +%s)

# Parse command-line arguments
while getopts ":n:c:q:s:e:" opt; do
  case $opt in
    n) NAMESPACE="$OPTARG"
    ;;
    c) CONTAINER="$OPTARG"
    ;;
    q) QUERY_SET="$OPTARG"
    ;;
    s) START_TIME="$OPTARG"
    ;;
    e) END_TIME="$OPTARG"
    ;;
    \?) echo "Invalid option -$OPTARG" >&2
        exit 1
    ;;
  esac
done

# Set default values if not provided via command-line
NAMESPACE=${NAMESPACE:-$DEFAULT_NAMESPACE}
CONTAINER=${CONTAINER:-$DEFAULT_CONTAINER}
QUERY_SET=${QUERY_SET:-$DEFAULT_QUERY_SET}
START_TIME=${START_TIME:-$DEFAULT_START_TIME}
END_TIME=${END_TIME:-$DEFAULT_END_TIME}

# Output file to store the results
OUTPUT_FILE="prometheus_${QUERY_SET}_stats.csv"
RESPONSE_LOG_FILE="${QUERY_SET}_response.log"

# Clear the output file before starting
> "$OUTPUT_FILE"
> "$RESPONSE_LOG_FILE"

echo "status, time_taken(s), start_time, end_time, query" > "$OUTPUT_FILE"

queries=()  # Declare an empty array to store the returned queries

# Get the query set
get_queries "$QUERY_SET" queries

if [[ "$QUERY_SET" == "grouped_queriesBy5days" ]]; then
    for query in "${queries[@]}"; do
            run_query_across_5day_windows "$query" "$NAMESPACE" "$CONTAINER" "$START_TIME" "$END_TIME"
    done
else
    for query in "${queries[@]}"; do
            measure_query_time "$query" "$NAMESPACE" "$CONTAINER" "$START_TIME" "$END_TIME"
    done
fi

capture_prometheus_resource_metrics

echo "Results have been written to $OUTPUT_FILE"
echo "Query output have been written to $RESPONSE_LOG_FILE"

exit 0
