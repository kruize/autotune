package com.autotune.common.data.dataSourceDetails;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataSourceDetailsInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceDetailsInfo.class);
    private String version;
    private DataSourceClusterGroup cluster_group;

    public DataSourceDetailsInfo(String version, DataSourceClusterGroup cluster_group) {
        this.version = version;

        if (cluster_group == null) {
            LOGGER.info("No meta data found for cluster_group: " + cluster_group);
        }
        this.cluster_group = cluster_group;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public DataSourceClusterGroup getDataSourceClusterGroup() {
        return cluster_group;
    }

    public void setDataSourceClusterGroup(DataSourceClusterGroup cluster_group) {
        this.cluster_group = cluster_group;
    }

    @Override
    public String toString() {
        return "DataSourceDetailsInfo{" +
                "version ='" + version + '\'' +
                ", cluster_group ='" + cluster_group.toString() + '\'' +
                '}';
    }
}
