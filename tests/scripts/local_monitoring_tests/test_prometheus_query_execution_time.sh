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

declare -A default_queries
declare -A individual_queries_by_pod
declare -A grouped_queries_by_owner_workload
declare -A metadata_queries

# List of Prometheus queries
default_queries=(
    [cpu_request_avg]='avg by(container, namespace) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    [cpu_request_sum]='sum by(container, namespace) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    [cpu_request_max]='max by(container, namespace) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    [cpu_request_min]='min by(container, namespace) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    [cpu_limits_avg]='avg by(container, namespace) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    [cpu_limits_sum]='sum by(container, namespace) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    [cpu_limits_max]='max by(container, namespace) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    [cpu_limits_min]='min by(container, namespace) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    [cpu_usage_avg]='avg by(container, namespace) (avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    [cpu_usage_sum]='sum by(container, namespace) (avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    [cpu_usage_max]='max by(container, namespace) (max_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    [cpu_usage_min]='min by(container, namespace) (min_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    [cpu_throttle_avg]='avg by(container, namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    [cpu_throttle_sum]='sum by(container, namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    [cpu_throttle_max]='max by(container, namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    [cpu_throttle_min]='min by(container, namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    [memory_request_avg]='avg by(container, namespace) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    [memory_request_sum]='sum by(container, namespace) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    [memory_request_max]='max by(container, namespace) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    [memory_request_min]='min by(container, namespace) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    [memory_limits_avg]='avg by(container, namespace) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    [memory_limits_sum]='sum by(container, namespace) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    [memory_limits_max]='max by(container, namespace) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    [memory_limits_min]='min by(container, namespace) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    [memory_usage_avg]='avg by(container, namespace) (avg_over_time(container_memory_working_set_bytes{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    [memory_usage_sum]='sum by(container, namespace) (avg_over_time(container_memory_working_set_bytes{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    [memory_usage_max]='max by(container, namespace) (max_over_time(container_memory_working_set_bytes{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    [memory_usage_min]='min by(container, namespace) (min_over_time(container_memory_working_set_bytes{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    [memory_rss_avg]='avg by(container, namespace) (avg_over_time(container_memory_rss{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    [memory_rss_sum]='sum by(container, namespace) (avg_over_time(container_memory_rss{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    [memory_rss_max]='max by(container, namespace) (max_over_time(container_memory_rss{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    [memory_rss_min]='min by(container, namespace) (min_over_time(container_memory_rss{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
)

individual_queries_by_pod=(
    [cpu_request_avg]='avg by(container, pod, namespace) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    [cpu_request_sum]='sum by(container, pod, namespace) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    [cpu_request_max]='max by(container, pod, namespace) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    [cpu_request_min]='min by(container, pod, namespace) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    [cpu_limits_avg]='avg by(container, pod, namespace) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    [cpu_limits_sum]='sum by(container, pod, namespace) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    [cpu_limits_max]='max by(container, pod, namespace) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    [cpu_limits_min]='min by(container, pod, namespace) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    [cpu_usage_avg]='avg by(container, pod, namespace) (avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    [cpu_usage_sum]='sum by(container, pod, namespace) (avg_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    [cpu_usage_max]='max by(container, pod, namespace) (max_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    [cpu_usage_min]='min by(container, pod, namespace) (min_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    [cpu_throttle_avg]='avg by(container, pod, namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    [cpu_throttle_sum]='sum by(container, pod, namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    [cpu_throttle_max]='max by(container, pod, namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    [cpu_throttle_min]='min by(container, pod, namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    [memory_request_avg]='avg by(container, pod, namespace) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    [memory_request_sum]='sum by(container, pod, namespace) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    [memory_request_max]='max by(container, pod, namespace) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    [memory_request_min]='min by(container, pod, namespace) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    [memory_limits_avg]='avg by(container, pod, namespace) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    [memory_limits_sum]='sum by(container, pod, namespace) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    [memory_limits_max]='max by(container, pod, namespace) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    [memory_limits_min]='min by(container, pod, namespace) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}))'
    [memory_usage_avg]='avg by(container, pod, namespace) (avg_over_time(container_memory_working_set_bytes{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    [memory_usage_sum]='sum by(container, pod, namespace) (avg_over_time(container_memory_working_set_bytes{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    [memory_usage_max]='max by(container, pod, namespace) (max_over_time(container_memory_working_set_bytes{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    [memory_usage_min]='min by(container, pod, namespace) (min_over_time(container_memory_working_set_bytes{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    [memory_rss_avg]='avg by(container, pod, namespace) (avg_over_time(container_memory_rss{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    [memory_rss_sum]='sum by(container, pod, namespace) (avg_over_time(container_memory_rss{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    [memory_rss_max]='max by(container, pod, namespace) (max_over_time(container_memory_rss{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    [memory_rss_min]='min by(container, pod, namespace) (min_over_time(container_memory_rss{container!="", container!="POD", pod!="",namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]))'
    [image_owners]='(max_over_time(kube_pod_container_info{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m])) * on(pod, namespace) group_left(owner_kind, owner_name) max by(pod, namespace, owner_kind, owner_name) (max_over_time(kube_pod_owner{container!="", container!="POD", pod!="", namespace="$NAMESPACE"}[15m]))'
    [image_workloads]='(max_over_time(kube_pod_container_info{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m])) * on(pod, namespace) group_left(workload, workload_type) max by(pod, namespace, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!="", namespace="$NAMESPACE"}[15m]))'
)

grouped_queries_by_owner_workload=(
    [cpu_request_avg]='sum by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
     * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
     * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
    ))'
    [cpu_request_sum]='avg by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
      * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
      * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
    ))'
    [cpu_request_max]='max by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
      * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
      * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
    ))'
    [cpu_request_min]='min by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
      * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
      * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
    ))'
    [cpu_limits_avg]='avg by(container, namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
       * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
       * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
    ))'
    [cpu_limits_sum]='sum by(container, namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
        * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
        * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
    ))'
    [cpu_limits_max]='max by(container, namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
       * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
       * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
    ))'
    [cpu_limits_min]='min by(container, namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
     * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
     * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
    ))'
    [cpu_usage_avg]='avg_over_time(
    avg by(namespace,container,workload,workload_type,owner_kind) (
      (node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"})
     * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
     * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
    )[15m:])'
    [cpu_usage_min]='min_over_time(
      min by(namespace,container,workload,workload_type,owner_kind) (
        (node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"})
       * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
       * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
     )[15m:] )'
    [cpu_usage_max]='max_over_time(
      max by(namespace,container,workload,workload_type,owner_kind) (
        (node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"})
       * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
       * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
     )[15m:] )'
    [cpu_usage_sum]='avg_over_time(
      sum by(namespace,container,workload,workload_type,owner_kind) (
        (node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"})
       * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
       * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
     )[15m:] )'
    [cpu_throttle_avg]='avg_over_time(
      avg by(namespace,container,workload,workload_type,owner_kind) (
        (rate(container_cpu_cfs_throttled_seconds_total{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]) )
       * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
       * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
     )[15m:] )'
    [cpu_throttle_max]='max_over_time(
      max by(namespace,container,workload,workload_type,owner_kind) (
         (rate(container_cpu_cfs_throttled_seconds_total{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]) )
         * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
         * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
      )[15m:] )'
    [cpu_throttle_min]='min_over_time(
      min by(namespace,container,workload,workload_type,owner_kind) (
        (rate(container_cpu_cfs_throttled_seconds_total{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]) )
       * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
       * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
    )[15m:] )'
    [cpu_throttle_sum]='avg_over_time(
      sum by(namespace,container,workload,workload_type,owner_kind) (
        (rate(container_cpu_cfs_throttled_seconds_total{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]) )
       * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
       * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
    )[15m:] )'
    [memory_request_avg]='avg by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
     * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
     * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
     ))'
     [memory_request_sum]='sum by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
      * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
      * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
      ))'
     [memory_request_max]='max by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
      * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
      * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
      ))'
      [memory_request_min]='min by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
      * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
      * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
      ))'
     [memory_limits_avg]='avg by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
       * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
       * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
     ))'
     [memory_limits_sum]='sum by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
      * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
      * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
      ))'
      [memory_limits_max]='max by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
         * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
         * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
      ))'
      [memory_limits_min]='min by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
         * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
         * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
      ))'
      [memory_usage_avg]='avg_over_time(
        avg by(namespace,container,workload,workload_type,owner_kind) (
          container_memory_working_set_bytes{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
         * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
         * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
      )[15m:] )'
      [memory_usage_min]='min_over_time(
        min by(namespace,container,workload,workload_type,owner_kind) (
          container_memory_working_set_bytes{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
         * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
         * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
      )[15m:] )'
      [memory_usage_max]='max_over_time(
        max by(namespace,container,workload,workload_type,owner_kind) (
          container_memory_working_set_bytes{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
         * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
         * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
      )[15m:])'
      [memory_usage_sum]='avg_over_time(
         sum by(namespace,container,workload,workload_type,owner_kind) (
           container_memory_working_set_bytes{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
           * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
           * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
      )[15m:] )'
      [memory_rss_avg]='avg_over_time(
        avg by(namespace,container,workload,workload_type,owner_kind) (
          container_memory_rss{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
         * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
         * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
      )[15m:] )'
      [memory_rss_min]='min_over_time(
        min by(namespace,container,workload,workload_type,owner_kind) (
          container_memory_rss{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
         * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
         * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
      )[15m:] )'
      [memory_rss_max]='max_over_time(
        max by(namespace,container,workload,workload_type,owner_kind) (
          container_memory_rss{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
          * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
          * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
      )[15m:] )'
      [memory_rss_sum]='avg_over_time(
        sum by(namespace,container,workload,workload_type,owner_kind) (
          container_memory_rss{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}
         * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))
         * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))
      )[15m:] )'
)

metadata_queries=(
  [namespaces_across_cluster]='sum by (namespace) (avg_over_time(kube_namespace_status_phase{namespace!=""}[15m]))'
  [workloads_across_cluster]='sum by (namespace, workload, workload_type) (avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{workload!=""}[15m]))'
  [containers_across_cluster]='sum by (container, image, workload, workload_type, namespace) (avg_over_time(kube_pod_container_info{container!=""}[15m]) * on (pod, namespace) group_left(workload, workload_type) avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{workload!=""}[15m]))'
)

declare -n grouped_queriesByDuration="grouped_queries_by_owner_workload"
queries_collection=( default_queries individual_queries_by_pod grouped_queries_by_owner_workload metadata_queries grouped_queriesByDuration )

# Function to return the correct query set as an array reference
get_queries() {
    local query_set="$1"
    local -n return_array="$2"

    case "$query_set" in
        "default_queries")
            return_array=("default_queries")
            ;;
        "individual_queries")
            return_array=("individual_queries_by_pod")
            ;;
        "grouped_queries")
            return_array=("grouped_queries_by_owner_workload")
            ;;
        "grouped_queriesByDuration")
            return_array=("grouped_queries_by_owner_workload")
            ;;
        "metadata_queries")
            return_array=("metadata_queries")
            ;;
        *)
            echo "Invalid query set. Available sets are: default_queries, individual_queries, grouped_queries, grouped_queriesByDuration, metadata_queries."
            exit 0  # Return error
            ;;
    esac
}

# Each query is sequentially executed dividing the 15days duration into three, 5 days window
run_query_across_duration_windows() {
    local query="$1"
    local namespace="$NAMESPACE"
    local container="$CONTAINER"
    local start_timestamp="$4"
    local end_timestamp="$5"
    local metric_name="$6"
    local duration=${7:-5}         # Default to 5 days if no duration is passed
    local partitions=${8:-3}       # Default to 3 partitions if not specified

    # Convert duration to seconds
    local DURATION_IN_SECONDS=$((duration * 24 * 60 * 60))

    # Loop to calculate start and end times for the specified number of partitions
    local current_start_time=$start_timestamp

    for ((i = 1; i <= partitions; i++)); do
        local current_end_time=$((current_start_time + DURATION_IN_SECONDS))

        measure_query_time "$query" "$namespace" "$container" "$current_start_time" "$current_end_time" "$metric_name"

        # Update start time for the next period
        current_start_time=$current_end_time
    done
}


# Function to send a Prometheus query and measure the time taken
measure_query_time() {
    local query="$1"
    local namespace="$2"
    local container="$3"
    local start_timestamp="$4"
    local end_timestamp="$5"
    local query_name="$6"

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
    echo "$query" >> "${RESPONSE_LOG_FILE}"
    echo "$response" >> "$RESPONSE_LOG_FILE"

    end_time=$(date +%s.%N)

    time_taken=$(echo "$end_time - $start_time" | bc)

    status=$(echo "$response" | jq -r '.status')

     if [[ "$status" == "success" ]]; then
        echo "Success; ${time_taken}; ${start_timestamp}; ${end_timestamp}; ${namespace}; ${container}; ${query_name}; ${query}" >> "$OUTPUT_FILE"
     else
        error_type=$(echo "$response" | jq -r '.errorType')
        error_message=$(echo "$response" | jq -r '.error')
        echo "Failed | ErrorType: $error_type | Error: $error_message; ${time_taken}; ${start_timestamp}; ${end_timestamp}; ${namespace}; ${container}; ${query}" >> "$OUTPUT_FILE"
     fi
}

# Function to fetch long running namespace and container using Prometheus
fetch_namespace_and_container(){

  local start_timestamp="$1"
  local end_timestamp="$2"

  local query='topk(1,
                 (time() - container_start_time_seconds{container!="POD", container!=""})
                 * on(pod, container, namespace)
                 group_left(workload, workload_type) (
                 max(kube_pod_container_info{container!="", container!="POD", pod!=""}) by (pod, container, namespace)
                 )
                 * on(pod, namespace) group_left(workload, workload_type) (
                 max(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}) by (pod, namespace, workload, workload_type)
                 )
                 )'

  response=$(curl -G -kH "Authorization: Bearer ${TOKEN}" \
                      --data-urlencode "query=${query}" \
                      --data-urlencode "start=${start_timestamp}" \
                      --data-urlencode "end=${end_timestamp}" \
                      --data-urlencode "step=900" \
                      "${PROMETHEUS_URL}/api/v1/query_range")

  # Check if the result is empty
  if [[ -z "$response" || "$response" == "null" ]]; then
    echo "Error: No data returned from Prometheus query to create experiments. Exiting!"
    exit 1
  fi

  namespace=$(echo "$response" | jq -r '.data.result[].metric.namespace')
  container=$(echo "$response" | jq -r '.data.result[].metric.container')

  # Return namespace and container as an array
  echo "$namespace $container"
}

# Function to capture resource metrics (CPU and memory) of Prometheus pods
capture_prometheus_resource_metrics() {
    echo -e "\n===== Prometheus Pod Resource Metrics (CPU & Memory) =====" >> "$RESPONSE_LOG_FILE"
    # Get resource usage for Prometheus pods in the given namespace
    kubectl top pod -n "$PROMETHEUS_NAMESPACE" | grep "prometheus" >> "$RESPONSE_LOG_FILE"
    echo -e "=========================================================\n" >> "$RESPONSE_LOG_FILE"
}

DEFAULT_END_TIME=$(date +%s)
DEFAULT_START_TIME=$(date -d "15 days ago" +%s)
ALL_QUERIES=0
DEFAULT_DURATION_IN_DAYS=5
DEFAULT_PARTITIONS=3

function usage() {
	echo "Usage: $0 [-n namespace] [-c container-name] [-q query_set] [-s start_timestamp] [-e end_timestamp] [-d duration in days] [-p no. of partitions] [-a all query sets]"
	echo "n = namespace"
	echo "c = container"
	echo "q = set of queries to be executed for eg. default_queries, individual_queries, grouped_queries_by_owner_workload, grouped_queriesByDuration, metadata_queries"
	echo "s = start time in epoch"
	echo "e = end timestamp in epoch, (if start and end timestamp are not specified 15 days is the default time range)"
	echo "d = duration for equally dividing the time range for eg. dividing 15 days into 5 days duration and executing the grouped_queries"
	echo "p = partitions in time range for eg. dividing 15 days into 5 days duration with 3 partitions"
	echo "a = Flag to run all the query sets to capture the time taken"
	echo "h = help"

	exit 1
}

# Parse command-line arguments
while getopts ":n:c:q:s:e:d:p:a" opt; do
  case "${opt}" in
    n)
      NAMESPACE="$OPTARG"
      ;;
    c)
      CONTAINER="$OPTARG"
      ;;
    q)
      QUERY_SET="$OPTARG"
      ;;
    s)
      START_TIME="$OPTARG"
      ;;
    e)
      END_TIME="$OPTARG"
      ;;
    a)
      ALL_QUERIES=1
      ;;
    d)
      DURATION_IN_DAYS="$OPTARG"
      ;;
    p)
      DURATION_PARTITIONS="$OPTARG"
      ;;
    *)
      usage
      ;;
  esac
done

# Set default values if not provided via command-line
START_TIME=${START_TIME:-$DEFAULT_START_TIME}
END_TIME=${END_TIME:-$DEFAULT_END_TIME}
DURATION_IN_DAYS=${DURATION_IN_DAYS:-$DEFAULT_DURATION_IN_DAYS}
DURATION_PARTITIONS=${DURATION_PARTITIONS:-$DEFAULT_PARTITIONS}

if [ -z "${NAMESPACE}" ] && [ -z "${CONTAINER}" ]; then
  echo "Finding a long running container"
  result=($(fetch_namespace_and_container "$START_TIME" "$END_TIME"))

  # Access the namespace and container from the array
  NAMESPACE="${result[0]}"
  CONTAINER="${result[1]}"

  # Use the namespace and container values
  echo "Namespace: $NAMESPACE"
  echo "Container: $CONTAINER"
fi

if [ ${ALL_QUERIES} -eq 1 ]; then
  for i in "${!queries_collection[@]}"; do
          query_name=${queries_collection[i]}
          declare -n current_queries="${queries_collection[i]}"
          echo $query_name

          # Output file to store the results
          OUTPUT_FILE="prometheus_${query_name}_${NAMESPACE}_${CONTAINER}_stats.csv"
          RESPONSE_LOG_FILE="${query_name}_${NAMESPACE}_${CONTAINER}_response.log"

          # Clear the output file before starting
          > "$OUTPUT_FILE"
          > "$RESPONSE_LOG_FILE"

          echo "status; time_taken(s); start_time; end_time; namespace; container; metric_name; query" > "$OUTPUT_FILE"

          for key in "${!current_queries[@]}"; do
            if [[ $query_name == "grouped_queriesByDuration" ]]; then
              run_query_across_duration_windows "${current_queries[$key]}" "$NAMESPACE" "$CONTAINER" "$START_TIME" "$END_TIME" "$key" "$DURATION_IN_DAYS" "$DURATION_PARTITIONS"
            else
              measure_query_time "${current_queries[$key]}" "$NAMESPACE" "$CONTAINER" "$START_TIME" "$END_TIME" "$key"
            fi
          done

          echo "Results have been written to $OUTPUT_FILE"
          echo "Query output have been written to $RESPONSE_LOG_FILE"
          capture_prometheus_resource_metrics
  done
else
  queries=()  # Declare an empty array to store the returned queries

  # Get the query set
  get_queries "$QUERY_SET" queries

  # Output file to store the results
  OUTPUT_FILE="prometheus_${QUERY_SET}_${NAMESPACE}_${CONTAINER}_stats.csv"
  RESPONSE_LOG_FILE="${QUERY_SET}_${NAMESPACE}_${CONTAINER}_response.log"

  # Clear the output file before starting
  > "$OUTPUT_FILE"
  > "$RESPONSE_LOG_FILE"

  echo "status; time_taken(s); start_time; end_time; namespace; container; metric_name; query" > "$OUTPUT_FILE"

  declare -n query_set=$queries
  if [[ "$QUERY_SET" == "grouped_queriesByDuration" ]]; then
      for key in "${!query_set[@]}"; do
              run_query_across_duration_windows "${query_set[$key]}" "$NAMESPACE" "$CONTAINER" "$START_TIME" "$END_TIME" "$key" "$DURATION_IN_DAYS" "$DURATION_PARTITIONS"
      done
  else
      for key in "${!query_set[@]}"; do
              measure_query_time "${query_set[$key]}" "$NAMESPACE" "$CONTAINER" "$START_TIME" "$END_TIME" "$key"
      done
  fi

  echo "Results have been written to $OUTPUT_FILE"
  echo "Query output have been written to $RESPONSE_LOG_FILE"
  capture_prometheus_resource_metrics
fi

exit 0
