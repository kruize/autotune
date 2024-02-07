package com.autotune.common.data.dataSourceDetails;

import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;

/**
 * DataSourceClusterGroup object represents the cluster group for a given data source, and it's associated clusters
 * used to store hashmap of DataSourceCluster objects representing cluster metadata
 */
public class DataSourceClusterGroup {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceClusterGroup.class);
    @SerializedName("cluster_group_name")
    private String clusterGroupName;

    // key = clusterName
    @SerializedName("clusters")
    private HashMap<String, DataSourceCluster> dataSourceClusterHashMap;

    public DataSourceClusterGroup(String clusterGroupName) {
        this.clusterGroupName = clusterGroupName;
    }

    public DataSourceClusterGroup(String clusterGroupName, HashMap<String,DataSourceCluster> clusters) {
        this.clusterGroupName = clusterGroupName;

        if (clusters == null) {
            LOGGER.info("No clusters found for cluster_group: " + clusterGroupName);
        }
        this.dataSourceClusterHashMap = clusters;
    }

    public String getDataSourceClusterGroupName() {
        return clusterGroupName;
    }

    public void setDataSourceClusterGroupName(String clusterGroupName) {
        this.clusterGroupName = clusterGroupName;
    }

    public HashMap<String, DataSourceCluster> getDataSourceCluster() {
        return dataSourceClusterHashMap;
    }

    public void setDataSourceCluster(HashMap<String, DataSourceCluster> clusters) {
        this.dataSourceClusterHashMap = clusters;
    }

    @Override
    public String toString() {
        return "DataSourceClusterGroup{" +
                "cluster_group_name='" + clusterGroupName + '\'' +
                ", clusters=" + dataSourceClusterHashMap +
                '}';
    }
}
