package com.autotune.common.data.dataSourceMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * DataSourceCluster object represents the cluster of a data source, and it's associated namespaces
 * used to store hashmap of DataSourceNamespace objects representing namespace metadata
 */
public class DataSourceCluster {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceCluster.class);
    // TODO - add KruizeConstants for attributes
    private String clusterName;

    /**
     * Key: Namespace
     * Value: Associated DataSourceNamespace object
     */
    private HashMap<String, DataSourceNamespace> namespaceHashMap;

    public DataSourceCluster(String clusterName, HashMap<String, DataSourceNamespace> namespaceHashMap) {
        this.clusterName = clusterName;
        this.namespaceHashMap = namespaceHashMap;
    }

    public String getDataSourceClusterName() {
        return clusterName;
    }

    public HashMap<String, DataSourceNamespace> getDataSourceNamespaceHashMap() {
        return namespaceHashMap;
    }

    public void setDataSourceNamespaceHashMap(HashMap<String, DataSourceNamespace> namespaceHashMap) {
        // TODO: Validate input before setting the namespaceHashMap
        this.namespaceHashMap = namespaceHashMap;
    }

    @Override
    public String toString() {
        return "DataSourceCluster{" +
                "clusterName='" + clusterName + '\'' +
                ", namespaceHashMap=" + namespaceHashMap +
                '}';
    }
}
