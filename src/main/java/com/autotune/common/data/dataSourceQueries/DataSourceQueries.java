package com.autotune.common.data.dataSourceQueries;

/**
 * This class contains PromQL queries as enum constants for various metrics related to Kubernetes clusters.
 * TODO - Add a custom PromQL query to fetch Cluster info
 *
 */
public class DataSourceQueries {

    public enum PromQLQuery {
        CLUSTER_QUERY("query"),
        NAMESPACE_QUERY("sum by (namespace) (kube_namespace_status_phase{phase=\"Active\", namespace!='', namespace!~'|openshift|openshift-.*'})"),
        WORKLOAD_INFO_QUERY("sum by (namespace, workload, workload_type) (namespace_workload_pod:kube_pod_owner:relabel{container!=' ',container!='POD', pod!=' ', namespace!='', namespace!~'|openshift|openshift-.*'})"),
        CONTAINER_INFO_QUERY("sum by (container, image, workload) (kube_pod_container_info * on(pod) group_left(workload, workload_type) (namespace_workload_pod:kube_pod_owner:relabel{container!=' ',container!='POD', pod!=' ', namespace!='', namespace!~'|openshift|openshift-.*'}))");
        private final String query;

        PromQLQuery(String query) {
            this.query = query;
        }

        public String getQuery() {
            return query;
        }

    }
}
