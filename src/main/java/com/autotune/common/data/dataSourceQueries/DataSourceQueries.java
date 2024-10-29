package com.autotune.common.data.dataSourceQueries;

/**
 * This class contains PromQL queries as enum constants for various metrics related to Kubernetes clusters.
 * TODO - Add a custom PromQL query to fetch Cluster info
 *      - Refactor PromQL queries into a separate YAML file utilizing Custom Resource Definitions (CRD).
 */
public class DataSourceQueries {
    public enum PromQLQuery {
        NAMESPACE_QUERY("sum by (namespace) ( avg_over_time(kube_namespace_status_phase{namespace!=\"\" ADDITIONAL_LABEL}[15d]))"),
        WORKLOAD_INFO_QUERY("sum by (namespace, workload, workload_type) ( avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{workload!=\"\" ADDITIONAL_LABEL}[15d]))"),
        CONTAINER_INFO_QUERY("sum by (container, image, workload, workload_type, namespace) (" +
                "  avg_over_time(kube_pod_container_info{}[15d]) *" +
                "  on (pod, namespace,prometheus_replica) group_left(workload, workload_type)" +
                "   avg_over_time(namespace_workload_pod:kube_pod_owner:relabel{}[15d])" +
                ")");
        private final String query;

        PromQLQuery(String query) {
            this.query = query;
        }

        public String getQuery() {
            return query;
        }
    }
}
