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

	CPU_USAGE("%s by(namespace,container,workload,workload_type,owner_kind)(%s_over_time(( node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!='', container!='POD', pod!='',namespace=\"%s\",container=\"%s\"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!="",workload=\"%s\",workload_type=\"%s\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m])))[%sm:]))"),
        CPU_THROTTLE("%s by(container,namespace, workload, workload_type, owner_kind)(%s_over_time((rate(container_cpu_cfs_throttled_seconds_total{container!='', container!='POD', pod!='',namespace=\"%s\",container=\"%s\"}[15m]))* on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!="",workload=\"%s\",workload_type=\"%s\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m])))[%sm:])"),
	CPU_LIMIT("%s by(container,namespace,workload,workload_type,owner_kind) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='cpu', unit='core',namespace=\"%s\",container=\"%s\"} * on(pod, namespace) group_left max by (container,pod, namespace) (kube_pod_status_phase{phase='Running'}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!="",workload=\"%s\",workload_type=\"%s\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m])))"),
	CPU_REQUEST("%s by(container,namespace,workload,workload_type,owner_kind) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='cpu', unit='core',namespace=\"%s\",container=\"%s\"} * on(pod, namespace) group_left max by (container,pod, namespace) (kube_pod_status_phase{phase='Running'}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!="",workload=\"%s\",workload_type=\"%s\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m])))"),
	MEMORY_USAGE("%s by(namespace,container,workload,workload_type,owner_kind) (min_over_time(( container_memory_working_set_bytes{container!='', container!='POD', pod!='',namespace=\"%s\",container=\"%s\"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!=""}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!="",workload=\"%s\",workload_type=\"%s\"}[15m])))[%sm:]))"),
	MEMORY_RSS("%s by(namespace,container,workload,workload_type,owner_kind) (min_over_time((container_memory_rss{container!='', container!='POD', pod!='',namespace=\"%s\",container=\"%s\"} * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!="",workload=\"%s\",workload_type=\"%s\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m])))[%sm:]))"),
	MEMORY_LIMIT("%s by(container,namespace,workload,workload_type,owner_kind) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='memory', unit='core',namespace=\"%s\",container=\"%s\"} * on(pod, namespace) group_left max by (container,pod, namespace) (kube_pod_status_phase{phase='Running'}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!="",workload=\"%s\",workload_type=\"%s\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m])))"),
	MEMORY_REQUEST("%s by(container,namespace,workload,workload_type,owner_kind) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='memory', unit='core',namespace=\"%s\",container=\"%s\"} * on(pod, namespace) group_left max by (container,pod, namespace) (kube_pod_status_phase{phase='Running'}) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!="",workload=\"%s\",workload_type=\"%s\"}[15m])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15m])))"),
        MAX_DATE("last_over_time(container_cpu_usage_seconds_total{container=\"%s\",namespace=\"%s\"}[15d:]) * on(pod) group_left(workload, workload_type) max by (pod, workload, workload_type) (max_over_time(namespace_workload_pod:kube_pod_owner:relabel{pod!="",workload=\"%s\",workload_type=\"%s\"}[15d])) * on(pod) group_left(owner_kind) max by (pod, owner_kind) (max_over_time(kube_pod_owner{pod!=""}[15d]))");
        private final String query;

        PromQLQuery(String query) {
            this.query = query;
        }

        public String getQuery() {
            return query;
        }
    }
}
