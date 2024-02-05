package com.autotune.common.data.dataSourceDetails;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataSourceClusterGroup {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceClusterGroup.class);
    private String cluster_group_name;
    private DataSourceCluster cluster;

    public DataSourceClusterGroup(String cluster_group_name, DataSourceCluster cluster) {
        this.cluster_group_name = cluster_group_name;

        if (cluster == null) {
            LOGGER.info("No clusters found for cluster_group: " + cluster_group_name);
        }
        this.cluster = cluster;
    }

    public String getDataSourceClusterGroupName() {
        return cluster_group_name;
    }

    public void setDataSourceClusterGroupName(String cluster_group_name) {
        this.cluster_group_name = cluster_group_name;
    }

    public DataSourceCluster getDataSourceCluster() {
        return cluster;
    }

    public void setDataSourceCluster(DataSourceCluster cluster) {
        this.cluster = cluster;
    }

    @Override
    public String toString() {
        return "DataSourceClusterGroup{" +
                "cluster_group_name ='" + cluster_group_name + '\'' +
                ", cluster ='" + cluster.toString() + '\'' +
                '}';
    }
}
