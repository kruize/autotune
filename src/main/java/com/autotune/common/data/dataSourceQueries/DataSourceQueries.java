package com.autotune.common.data.dataSourceQueries;

/**
 * This class contains PromQL queries as enum constants for various metrics related to Kubernetes clusters.
 * TODO - Add a custom PromQL query to fetch Cluster info
 *      - Refactor PromQL queries into a separate YAML file utilizing Custom Resource Definitions (CRD).
 */
public class DataSourceQueries {
    public enum PromQLQuery {
        NAMESPACE_QUERY("sum by (namespace) (kube_namespace_status_phase{phase=\"Active\"})"),
        WORKLOAD_INFO_QUERY("sum by (namespace, workload, workload_type) (namespace_workload_pod:kube_pod_owner:relabel)"),
        CONTAINER_INFO_QUERY("sum by (container, image, workload) (kube_pod_container_info * on(pod) group_left(workload, workload_type) (namespace_workload_pod:kube_pod_owner:relabel))"),
        CPU_USAGE("%s by(container, namespace, workload, workload_type, owner_kind)(%s_over_time(( node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!=\"\", container!=\"POD\", pod!=\"\",namespace=\"%s\",container=\"%s\" } * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=\"\",workload=\"%s\",workload_type=\"%s\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=\"\"}[15m])))[%sm:]))"),
        CPU_THROTTLE("%s by(container,namespace, workload, workload_type, owner_kind) ((rate(container_cpu_cfs_throttled_seconds_total{container!='', container!='POD', pod!='',namespace=\"%s\",container=\"%s\"}[15m]))* on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!='',workload=\"%s\",workload_type=\"%s\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=''}[%sm])))"),
        CPU_LIMIT("%s by(container,namespace, workload, workload_type, owner_kind) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='cpu', unit='core',namespace=\"%s\",container=\"%s\"} * on(pod, namespace) group_left max by (container,pod, namespace) (kube_pod_status_phase{phase='Running'}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!='',workload=\"%s\",workload_type=\"%s\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=''}[15m])))"),
        CPU_REQUEST("%s by(container, namespace, workload, workload_type, owner_kind) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='cpu', unit='core' ,namespace=\"%s\",container=\"%s\"} * on(pod, namespace) group_left max by (container, pod, namespace) (kube_pod_status_phase{phase='Running'}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!='',workload=\"%s\",workload_type=\"%s\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=''}[15m])))"),
        MEMORY_USAGE("%s by(container, namespace, workload, workload_type, owner_kind) (%s_over_time(( container_memory_working_set_bytes{container!='', container!='POD', pod!='',namespace=\"%s\",container=\"%s\" } * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=''}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!='',workload=\"%s\",workload_type=\"%s\"}[15m])))[%sm:]))"),
        MEMORY_RSS("%s by(container, namespace, workload, workload_type, owner_kind) (%s_over_time((container_memory_rss{container!='', container!='POD', pod!='',namespace=\"%s\",container=\"%s\"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!='',workload=\"%s\",workload_type=\"%s\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=''}[15m])))[%sm:]))"),
        MEMORY_LIMIT("%s by(container,namespace, workload, workload_type, owner_kind) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='memory', unit='byte', namespace=\"%s\",container=\"%s\" } * on(pod, namespace) group_left max by (container, pod, namespace) (kube_pod_status_phase{phase='Running'}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!='',workload=\"%s\",workload_type=\"%s\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=''}[15m])))"),
        MEMORY_REQUEST("%s by(container,namespace, workload, workload_type, owner_kind) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='memory', unit='byte',namespace=\"%s\",container=\"%s\"} * on(pod, namespace) group_left max by (container, pod, namespace) (kube_pod_status_phase{phase='Running'}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!='',workload=\"%s\",workload_type=\"%s\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=''}[15m])))"),
        MAX_DATE("last_over_time(container_cpu_usage_seconds_total{container=\"%s\",namespace=\"%s\"}[15d:]) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!='',workload=\"%s\",workload_type=\"%s\"}[15d])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=''}[15d]))"),
        NAMESPACE_CPU_USAGE("%s_over_time(sum by(namespace) (node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{namespace=\"%s\", container!='', container!='POD', pod!=''})[%sm:])"),
        NAMESPACE_CPU_THROTTLE("%s_over_time(sum by(namespace) (rate(container_cpu_cfs_throttled_seconds_total{namespace=\"%s\", container!='', container!='POD', pod!=''}[5m]))[%sm:])"),
        NAMESPACE_CPU_LIMIT("%s by (namespace) (kube_resourcequota{namespace=\"%s\", resource=\"limits.cpu\", type=\"hard\"})"),
        NAMESPACE_CPU_REQUEST("%s by (namespace) (kube_resourcequota{namespace=\"%s\", resource=\"requests.cpu\", type=\"hard\"})"),
        NAMESPACE_MEMORY_USAGE("%s_over_time(sum by(namespace) (container_memory_working_set_bytes{namespace=\"%s\", container!='', container!='POD', pod!=''})[%sm:])"),
        NAMESPACE_MEMORY_RSS("%s_over_time(sum by(namespace) (container_memory_rss{namespace=\"%s\", container!='', container!='POD', pod!=''})[%sm:])"),
        NAMESPACE_MEMORY_LIMIT("%s by(namespace) (kube_resourcequota{namespace=\"%s\", resource=\"limits.memory\", type=\"hard\"})"),
        NAMESPACE_MEMORY_REQUEST("%s by (namespace) (kube_resourcequota{namespace=\"%s\", resource=\"requests.memory\", type=\"hard\"})"),
        NAMESPACE_MAX_DATE("last_over_time(timestamp((sum by (namespace) (container_cpu_usage_seconds_total{namespace=\"%s\"})) > 0 )[15d:])"),
        NAMESPACE_TOTAL_PODS("count(kube_pod_status_phase{namespace=\"%s\"})"),
        NAMESPACE_TOTAL_RUNNING_PODS("count(kube_pod_status_phase{namespace=\"%s\", phase=\"Running\"})");
        private final String query;


        PromQLQuery(String query) {
            this.query = query;
        }

        public String getQuery() {
            return query;
        }
    }
}
