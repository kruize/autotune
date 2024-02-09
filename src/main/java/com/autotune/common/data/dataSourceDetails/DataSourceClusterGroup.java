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

    /**
     * Key: Cluster name
     * Value: Associated DataSourceCluster object
     */
    @SerializedName("clusters")
    private HashMap<String, DataSourceCluster> dataSourceClusterHashMap;

    public DataSourceClusterGroup(String clusterGroupName, HashMap<String,DataSourceCluster> clusters) {
        this.clusterGroupName = clusterGroupName;
        this.dataSourceClusterHashMap = clusters;
    }

    public String getDataSourceClusterGroupName() {
        return clusterGroupName;
    }

    public HashMap<String, DataSourceCluster> getDataSourceClusterHashMap() {
        return dataSourceClusterHashMap;
    }

    public void setDataSourceClusterHashMap(HashMap<String, DataSourceCluster> clusters) {
        if (null == clusters) {
            LOGGER.error("No clusters found for cluster group: "+ clusterGroupName);
        }
        this.dataSourceClusterHashMap = clusters;
    }

    public DataSourceCluster getDataSourceClusterObject(String clusterName) {
        if (null != dataSourceClusterHashMap && dataSourceClusterHashMap.containsKey(clusterName)) {
            return dataSourceClusterHashMap.get(clusterName);
        }
        return null;
    }

    @Override
    public String toString() {
        return "DataSourceClusterGroup{" +
                "cluster_group_name='" + clusterGroupName + '\'' +
                ", clusters=" + dataSourceClusterHashMap +
                '}';
    }
}
