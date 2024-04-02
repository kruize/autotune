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
        CONTAINER_INFO_QUERY("sum by (container, image, workload) (kube_pod_container_info * on(pod) group_left(workload, workload_type) (namespace_workload_pod:kube_pod_owner:relabel))");
        private final String query;

        PromQLQuery(String query) {
            this.query = query;
        }

        public String getQuery() {
            return query;
        }
    }
}
