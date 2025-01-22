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
export STEP=900
#15d=1296000s
export STEP_15DAYS=1296000

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
    [cpu_request_avg]='sum by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}* on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))* on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))))'
    [cpu_request_sum]='avg by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}* on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))* on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))))'
    [cpu_request_max]='max by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}* on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))* on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))))'
    [cpu_request_min]='min by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}* on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))* on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))))'
    [cpu_limits_avg]='avg by(container, namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}* on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))* on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))))'
    [cpu_limits_sum]='sum by(container, namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}* on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))* on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))))'
    [cpu_limits_max]='max by(container, namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"}* on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))))'
    [cpu_limits_min]='min by(container, namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="cpu", unit="core", namespace="$NAMESPACE",container="$CONTAINER_NAME"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))))'
    [cpu_usage_avg]='avg_over_time(avg by(namespace,container,workload,workload_type,owner_kind) ((node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m])))[15m:])'
    [cpu_usage_min]='min_over_time(min by(namespace,container,workload,workload_type,owner_kind) ((node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m])))[15m:])'
    [cpu_usage_max]='max_over_time(max by(namespace,container,workload,workload_type,owner_kind) ((node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m])))[15m:] )'
    [cpu_usage_sum]='avg_over_time(sum by(namespace,container,workload,workload_type,owner_kind) ((node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m])))[15m:] )'
    [cpu_throttle_avg]='avg_over_time(avg by(namespace,container,workload,workload_type,owner_kind) ((rate(container_cpu_cfs_throttled_seconds_total{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]) ) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m])))[15m:] )'
    [cpu_throttle_max]='max_over_time(max by(namespace,container,workload,workload_type,owner_kind) ((rate(container_cpu_cfs_throttled_seconds_total{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m])) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))* on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m])))[15m:])'
    [cpu_throttle_min]='min_over_time(min by(namespace,container,workload,workload_type,owner_kind) ((rate(container_cpu_cfs_throttled_seconds_total{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m]) ) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m])))[15m:])'
    [cpu_throttle_sum]='avg_over_time(sum by(namespace,container,workload,workload_type,owner_kind) ((rate(container_cpu_cfs_throttled_seconds_total{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"}[15m])) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m])))[15m:] )'
    [memory_request_avg]='avg by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"}* on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))))'
    [memory_request_sum]='sum by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))))'
    [memory_request_max]='max by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m]))* on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))))'
    [memory_request_min]='min by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_requests{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))))'
    [memory_limits_avg]='avg by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))))'
    [memory_limits_sum]='sum by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))))'
    [memory_limits_max]='max by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))))'
    [memory_limits_min]='min by(container,namespace,workload,workload_type,owner_kind) ((kube_pod_container_resource_limits{container!="", container!="POD", pod!="", resource="memory", unit="byte", namespace="$NAMESPACE",container="$CONTAINER_NAME"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m]))))'
    [memory_usage_avg]='avg_over_time(avg by(namespace,container,workload,workload_type,owner_kind) (container_memory_working_set_bytes{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m])))[15m:])'
    [memory_usage_min]='min_over_time(min by(namespace,container,workload,workload_type,owner_kind) (container_memory_working_set_bytes{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m])))[15m:])'
    [memory_usage_max]='max_over_time(max by(namespace,container,workload,workload_type,owner_kind) (container_memory_working_set_bytes{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m])))[15m:])'
    [memory_usage_sum]='avg_over_time(sum by(namespace,container,workload,workload_type,owner_kind) (container_memory_working_set_bytes{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m])))[15m:])'
    [memory_rss_avg]='avg_over_time(avg by(namespace,container,workload,workload_type,owner_kind) (container_memory_rss{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m])))[15m:])'
    [memory_rss_min]='min_over_time(min by(namespace,container,workload,workload_type,owner_kind) (container_memory_rss{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m])))[15m:])'
    [memory_rss_max]='max_over_time(max by(namespace,container,workload,workload_type,owner_kind) (container_memory_rss{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m])))[15m:])'
    [memory_rss_sum]='avg_over_time(sum by(namespace,container,workload,workload_type,owner_kind) (container_memory_rss{container!="", container!="POD", pod!="", namespace="$NAMESPACE",container="$CONTAINER_NAME"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m])))[15m:])'
)

metadata_queries=(
  [namespaces_across_cluster]='sum by (namespace) (avg_over_time(kube_namespace_status_phase{namespace!=""}[15d]))'
  [workloads_across_cluster]='sum by (namespace, workload, workload_type) (avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{workload!=""}[15d]))'
  [containers_across_cluster]='sum by (container, image, workload, workload_type, namespace) (avg_over_time(kube_pod_container_info{container!=""}[15d]) * on (pod, namespace) group_left(workload, workload_type) avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{workload!=""}[15d]))'
)

declare -n grouped_queriesByDuration="grouped_queries_by_owner_workload"
queries_collection=( default_queries individual_queries_by_pod grouped_queries_by_owner_workload grouped_queriesByDuration metadata_queries )

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
    local namespace="$2"
    local container="$3"
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

    local step
    if echo "$query" | grep -q "15d"; then
      step=${STEP_15DAYS}
    else
      step=${STEP}
    fi

    start_time=$(date +%s.%N)

    response=$(curl -G -kH "Authorization: Bearer ${TOKEN}" \
            --data-urlencode "query=${query}" \
            --data-urlencode "start=${start_timestamp}" \
            --data-urlencode "end=${end_timestamp}" \
            --data-urlencode "step=${step}" \
            "${PROMETHEUS_URL}/api/v1/query_range")

    echo "Query: $query" >> "${RESPONSE_LOG_FILE}"
    echo "$response" >> "$RESPONSE_LOG_FILE"

    end_time=$(date +%s.%N)

    time_taken=$(echo "$end_time $start_time" | awk '{print $1 - $2}')

    status=$(echo "$response" | jq -r '.status')

     if [[ "$status" == "success" ]]; then
        echo "Success;${time_taken};${start_timestamp};${end_timestamp};${namespace};${container};${query_name};${query}" >> "$OUTPUT_FILE"
     else
        error_type=$(echo "$response" | jq -r '.errorType')
        error_message=$(echo "$response" | jq -r '.error')
        echo "Failed | ErrorType: $error_type | Error: $error_message;${time_taken};${start_timestamp};${end_timestamp};${namespace};${container};${query}" >> "$OUTPUT_FILE"
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
    local response_log_file=$1
    echo -e "\n===== Prometheus Pod Resource Metrics (CPU & Memory) =====" >> "$response_log_file"
    # Get resource usage for Prometheus pods in the given namespace
    kubectl top pod -n "$PROMETHEUS_NAMESPACE" | grep "prometheus" >> "$response_log_file"
    echo -e "=========================================================\n" >> "$response_log_file"
}

# Declare associative arrays for summing total time for each query type
declare -A total_time_default_sum
declare -A total_time_individual_sum
declare -A total_time_grouped_sum
declare -A total_time_grouped_duration_sum

# Declare arrays for tracking start_time and end_time
declare -A start_time_sum
declare -A end_time_sum

sum_float() {
    echo "$1 $2" | awk '{printf "%.9f", $1 + $2}'
}

process_file() {
    local file=$1
    local query_type=$2

    # Read the CSV file line by line (skipping the header)
    while IFS=';' read -r status time_taken start_time end_time local_namespace local_container metric_name query; do
        # Create a unique key based on status, namespace, container, and metric_name
        key="$status,$local_namespace,$local_container,$metric_name"

        # Sum the time based on the query type
        case "$query_type" in
            default_queries)
                total_time_default_sum[$key]="$time_taken"
                ;;
            individual_queries_by_pod)
                total_time_individual_sum[$key]="$time_taken"
                ;;
            grouped_queries_by_owner_workload)
                total_time_grouped_sum[$key]="$time_taken"
                ;;
            grouped_queriesByDuration)
                if [ -z "${total_time_grouped_duration_sum[$key]}" ]; then
                    total_time_grouped_duration_sum[$key]=0
                fi

                total_time_grouped_duration_sum[$key]=$(sum_float "${total_time_grouped_duration_sum[$key]}" "$time_taken")
        esac

        # Track start_time and end_time for each key
        if [[ -z "${start_time_sum[$key]}" || "${start_time_sum[$key]}" -gt "$start_time" ]]; then
            start_time_sum[$key]=$start_time
        fi

        if [[ -z "${end_time_sum[$key]}" || "${end_time_sum[$key]}" -lt "$end_time" ]]; then
            end_time_sum[$key]=$end_time
        fi

    done < <(tail -n +2 "$file")
}

# Function to generate the output file
common_function() {
    local namespace=$1
    local container=$2
    local output_file1=$3
    local output_file2=$4
    total_time_default=0
    total_time_individual=0
    total_time_grouped=0
    total_time_grouped_by_duration=0


    # Output headers to the CSV file
    echo "status;time_default_queries;time_individual_queries;time_grouped_queries;time_grouped_queriesByDuration;start_time;end_time;namespace;container;metric_name" > "$output_file1"

    if [ -z "$(cat "$output_file2")" ]; then
        echo "status;total_time_default_queries;total_time_individual_queries;total_time_grouped_queries;total_time_grouped_queriesByDuration;start_time;end_time;namespace;container" > "$output_file2"
    fi

    for key in "${!total_time_default_sum[@]}"; do

        IFS=',' read -r status target_namespace target_container metric_name <<< "$key"

        # Check if the key matches the target status and namespace
        if [ "$namespace" != "$target_namespace" ] || [ "$container" != "$target_container" ]; then
            continue;
        fi

        # Extract the individual row data for each query type
        time_default=${total_time_default_sum[$key]}
        total_time_default=$(sum_float "${total_time_default}" "$time_default")

        time_individual=${total_time_individual_sum[$key]}
        total_time_individual=$(sum_float "${total_time_individual}" "$time_individual")

        time_grouped=${total_time_grouped_sum[$key]}
        total_time_grouped=$(sum_float "${total_time_grouped}" "$time_grouped")

        time_grouped_duration=${total_time_grouped_duration_sum[$key]}
        total_time_grouped_by_duration=$(sum_float "${total_time_grouped_by_duration}" "$time_grouped_duration")

        start_time=${start_time_sum[$key]:-0}
        end_time=${end_time_sum[$key]:-0}

        IFS=',' read -r status target_namespace target_container metric_name <<< "$key"

        # Write the combined row to the output file
        echo "$status;$time_default;$time_individual;$time_grouped;$time_grouped_duration;$start_time;$end_time;$target_namespace;$target_container;$metric_name" >> "$output_file1"
    done

    echo "$status;$total_time_default;$total_time_individual;$total_time_grouped;$total_time_grouped_by_duration;$start_time;$end_time;$namespace;$container" >> "$output_file2"
}


DEFAULT_END_TIME=$(date +%s)
DEFAULT_START_TIME=$(date -d "15 days ago" +%s)
ALL_QUERIES=0
DEFAULT_DURATION_IN_DAYS=5
DEFAULT_PARTITIONS=3
ALL_NAMESPACES_CONTAINERS=0

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
while getopts ":n:c:q:s:e:d:p:aA" opt; do
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
    A)
      ALL_NAMESPACES_CONTAINERS=1
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

# Function to fetch namespaces using Prometheus
fetch_namespaces() {
    local start_timestamp="$1"
    local end_timestamp="$2"
    # Prometheus query to fetch unique namespaces
    local query="count by (namespace) (kube_pod_container_info)"

    # Fetch namespaces from Prometheus
    response=$(curl -G -kH "Authorization: Bearer ${TOKEN}" \
                --data-urlencode "query=${query}" \
                --data-urlencode "start=${start_timestamp}" \
                --data-urlencode "end=${end_timestamp}" \
                --data-urlencode "step=900" \
                "${PROMETHEUS_URL}/api/v1/query_range")
    # Parse response and extract namespaces
    echo "$response" | jq -r '.data.result[].metric.namespace'
}

# Function to fetch containers for a specific namespace using Prometheus
fetch_containers_for_namespace() {
    local namespace="$1"
    local start_timestamp="$2"
    local end_timestamp="$3"

    # Prometheus query to fetch containers in a namespace
    local query="count by (container) (kube_pod_container_info{namespace='${namespace}'})"

    # Fetch containers from Prometheus
        response=$(curl -G -kH "Authorization: Bearer ${TOKEN}" \
                    --data-urlencode "query=${query}" \
                    --data-urlencode "start=${start_timestamp}" \
                    --data-urlencode "end=${end_timestamp}" \
                    --data-urlencode "step=900" \
                    "${PROMETHEUS_URL}/api/v1/query_range")

    # Parse response and extract container names
    echo "$response" | jq -r '.data.result[].metric.container'
}

run_all_queries() {
  local namespace=$1
  local container=$2

  for i in "${!queries_collection[@]}"; do
    query_name=${queries_collection[i]}
    declare -n current_queries="${queries_collection[i]}"

    # Output file to store the results
    OUTPUT_FILE="prometheus_${query_name}_${namespace}_${container}_stats.csv"
    RESPONSE_LOG_FILE="${query_name}_${namespace}_${container}_response.log"

    # Clear the output file before starting
    > "$OUTPUT_FILE"
    > "$RESPONSE_LOG_FILE"

    echo "status;time_taken(s);start_time;end_time;namespace;container;metric_name;query" > "$OUTPUT_FILE"

    for key in "${!current_queries[@]}"; do
      if [[ $query_name == "grouped_queriesByDuration" ]]; then
        # Calculate the difference in seconds
        TIME_DIFF=$((END_TIME - START_TIME))

        # Convert the difference from seconds to days
        DIFF_IN_DAYS=$((TIME_DIFF / 86400))
        echo "Dividing the ${DIFF_IN_DAYS} days time range into ${DURATION_PARTITIONS} partitions, each with ${DURATION_IN_DAYS} days duration"

        run_query_across_duration_windows "${current_queries[$key]}" "$namespace" "$container" "$START_TIME" "$END_TIME" "$key" "$DURATION_IN_DAYS" "$DURATION_PARTITIONS"
      else
        measure_query_time "${current_queries[$key]}" "$namespace" "$container" "$START_TIME" "$END_TIME" "$key"
      fi
    done

    echo "Results have been written to $OUTPUT_FILE"
    echo "Query output have been written to $RESPONSE_LOG_FILE"
    capture_prometheus_resource_metrics "$RESPONSE_LOG_FILE"
    process_file "$OUTPUT_FILE" "$query_name"
  done

}

if [ -z "${NAMESPACE}" ] && [ -z "${CONTAINER}" ] && [ "${ALL_NAMESPACES_CONTAINERS}" -eq 0 ]; then
  echo "Finding a long running container"
  result=($(fetch_namespace_and_container "$START_TIME" "$END_TIME"))

  # Access the namespace and container from the array
  NAMESPACE="${result[0]}"
  CONTAINER="${result[1]}"

  # Use the namespace and container values
  echo "Namespace: $NAMESPACE"
  echo "Container: $CONTAINER"
fi

TOTAL_TIME_FOR_ALL_CONTAINERS="total_time_for_all_queries.csv"

if [ ${ALL_QUERIES} -eq 1 ]; then

  METRIC_TIME_FILE="metric_time_for_all_queries_${NAMESPACE}_${CONTAINER}.csv"

  run_all_queries "$NAMESPACE" "$CONTAINER"
  common_function "$NAMESPACE" "$CONTAINER" "$METRIC_TIME_FILE" "$TOTAL_TIME_FOR_ALL_CONTAINERS"

  echo "Time taken for each metric by all queries for namespace -$NAMESPACE and container -$CONTAINER have been written to $METRIC_TIME_FILE"
  echo "Total time taken for all the queries have been written to $TOTAL_TIME_FOR_ALL_CONTAINERS"
elif [ ${ALL_NAMESPACES_CONTAINERS} -eq 1 ]; then

  namespaces=$(fetch_namespaces "$START_TIME" "$END_TIME")

  for namespace in $namespaces; do
      containers=$(fetch_containers_for_namespace "$namespace" "$START_TIME" "$END_TIME")

      for container in ${containers[@]}; do
          METRIC_TIME_FILE="metric_time_for_all_queries_${namespace}_${container}.csv"

          run_all_queries "$namespace" "$container"
          common_function "$namespace" "$container" "$METRIC_TIME_FILE" "$TOTAL_TIME_FOR_ALL_CONTAINERS"

          echo "Time taken for each metric by all queries for namespace -$namespace and container -$container have been written to $METRIC_TIME_FILE"
      done
  done
  echo "Total time taken for all the queries have been written to $TOTAL_TIME_FOR_ALL_CONTAINERS"
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

  echo "status;time_taken(s);start_time;end_time;namespace;container;metric_name;query" > "$OUTPUT_FILE"

  declare -n query_set=$queries
  if [[ "$QUERY_SET" == "grouped_queriesByDuration" ]]; then
      TIME_DIFF=$((END_TIME - START_TIME))

      # Convert the difference from seconds to days
      DIFF_IN_DAYS=$((TIME_DIFF / 86400))
      echo "Dividing the ${DIFF_IN_DAYS} days time range into ${DURATION_PARTITIONS} partitions, each with ${DURATION_IN_DAYS} days duration"
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
  capture_prometheus_resource_metrics "$RESPONSE_LOG_FILE"
fi

exit 0
