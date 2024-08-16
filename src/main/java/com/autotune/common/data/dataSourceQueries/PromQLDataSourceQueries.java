package com.autotune.common.data.dataSourceQueries;

/**
 * This class provides static constants for PromQL queries related to a Kubernetes cluster.
 * It utilizes the queries defined in the DataSourceQueries class.
 */
public class PromQLDataSourceQueries {
    public static final String NAMESPACE_QUERY = DataSourceQueries.PromQLQuery.NAMESPACE_QUERY.getQuery();
    public static final String WORKLOAD_QUERY = DataSourceQueries.PromQLQuery.WORKLOAD_INFO_QUERY.getQuery();
    public static final String CONTAINER_QUERY = DataSourceQueries.PromQLQuery.CONTAINER_INFO_QUERY.getQuery();
}
