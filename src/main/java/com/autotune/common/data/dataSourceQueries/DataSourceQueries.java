package com.autotune.common.data.dataSourceQueries;

/**
 * This class contains PromQL queries as enum constants for various metrics related to Kubernetes clusters.
 * TODO - Add a custom PromQL query to fetch Cluster info
 *      - Refactor PromQL queries into a separate YAML file utilizing Custom Resource Definitions (CRD).
 */
public class DataSourceQueries {
    public enum PromQLQuery {
        NAMESPACE_QUERY("sum by (namespace) (kube_namespace_status_phase{phase=\"Active\" ADDITIONAL_LABEL })"),
        WORKLOAD_INFO_QUERY("sum by (namespace, workload, workload_type) (namespace_workload_pod:kube_pod_owner:relabel{ADDITIONAL_LABEL})"),
        CONTAINER_INFO_QUERY("sum by (container,image,workload)(kube_pod_container_info{ADDITIONAL_LABEL}*on(pod)group_left(workload,workload_type)(namespace_workload_pod:kube_pod_owner:relabel{workload!='cadvisor' , ADDITIONAL_LABEL}))"),
        CPU_USAGE("%s by(container, namespace)(%s_over_time(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!='', container!='POD', pod!='',namespace=\"%s\",container=\"%s\" }[%sm]))"),
        CPU_THROTTLE("%s by(container,namespace) (rate(container_cpu_cfs_throttled_seconds_total{container!='', container!='POD', pod!='',namespace=\"%s\",container=\"%s\"}[%sm]))"),
        CPU_LIMIT("%s by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='cpu', unit='core',namespace=\"%s\",container=\"%s\"} * on(pod, namespace) group_left max by (container,pod, namespace) (kube_pod_status_phase{phase='Running'}))"),
        CPU_REQUEST("%s by(container, namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='cpu', unit='core' ,namespace=\"%s\",container=\"%s\"} * on(pod, namespace) group_left max by (container, pod, namespace) (kube_pod_status_phase{phase='Running'}))"),
        MEMORY_USAGE("%s by(container, namespace) (%s_over_time(container_memory_working_set_bytes{container!='', container!='POD', pod!='',namespace=\"%s\",container=\"%s\" }[%sm]))"),
        MEMORY_RSS("%s by(container, namespace) (%s_over_time(container_memory_rss{container!='', container!='POD', pod!='',namespace=\"%s\",container=\"%s\"}[%sm]))"),
        MEMORY_LIMIT("%s by(container,namespace) (kube_pod_container_resource_limits{container!='', container!='POD', pod!='', resource='memory', unit='byte', namespace=\"%s\",container=\"%s\" } * on(pod, namespace) group_left max by (container, pod, namespace) (kube_pod_status_phase{phase='Running'}))"),
        MEMORY_REQUEST("%s by(container,namespace) (kube_pod_container_resource_requests{container!='', container!='POD', pod!='', resource='memory', unit='byte',namespace=\"%s\",container=\"%s\"} * on(pod, namespace) group_left max by (container, pod, namespace) (kube_pod_status_phase{phase='Running'}))"),
        MAX_DATE("max(container_cpu_usage_seconds_total{container=\"%s\",namespace=\"%s\"} > 0)");
        private final String query;

        PromQLQuery(String query) {
            this.query = query;
        }

        public String getQuery() {
            return query;
        }
    }
}
