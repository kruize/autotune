package com.autotune.common.data.dataSourceMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * DataSource object represents a data source, and it's associated clusters
 * used to store hashmap of DataSourceCluster objects representing cluster metadata
 */
public class DataSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSource.class);
    // TODO - add KruizeConstants for attributes
    private String dataSourceName;

    /**
     * Key: Cluster name
     * Value: Associated DataSourceCluster object
     */
    private HashMap<String, DataSourceCluster> clusterHashMap;

    public DataSource(String dataSourceName, HashMap<String,DataSourceCluster> clusterHashMap) {
        this.dataSourceName = dataSourceName;
        this.clusterHashMap = clusterHashMap;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public HashMap<String, DataSourceCluster> getDataSourceClusterHashMap() {
        return clusterHashMap;
    }

    public void setDataSourceClusterHashMap(HashMap<String, DataSourceCluster> clusterHashMap) {
        // TODO: Validate input before setting the clusterHashMap
        this.clusterHashMap = clusterHashMap;
    }

    public DataSourceCluster getDataSourceClusterObject(String clusterName) {
        if (null != clusterHashMap && clusterHashMap.containsKey(clusterName)) {
            return clusterHashMap.get(clusterName);
        }
        return null;
    }

    @Override
    public String toString() {
        return "DataSource{" +
                "dataSourceName='" + dataSourceName + '\'' +
                ", clusterHashMap=" + clusterHashMap +
                '}';
    }
}
