package com.autotune.common.data.dataSourceDetails;

import com.google.gson.annotations.SerializedName;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.util.HashMap;

/**
 * DataSourceCluster object represents the cluster of a data source, and it's associated namespaces
 * used to store hashmap of DataSourceNamespace objects representing namespace metadata
 */
public class DataSourceCluster {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceCluster.class);
    @SerializedName("cluster_name")
    private String clusterName;

    /**
     * Key: Namespace
     * Value: Associated DataSourceNamespace object
     */
    @SerializedName("namespaces")
    private HashMap<String, DataSourceNamespace> dataSourceNamespaceHashMap;

    public DataSourceCluster(String clusterName, HashMap<String, DataSourceNamespace> dataSourceNamespaceHashMap) {
        this.clusterName = clusterName;
        this.dataSourceNamespaceHashMap = dataSourceNamespaceHashMap;
    }

    public String getDataSourceClusterName() {
        return clusterName;
    }

    public HashMap<String, DataSourceNamespace> getDataSourceNamespaceHashMap() {
        return dataSourceNamespaceHashMap;
    }

    public DataSourceNamespace getDataSourceNamespaceObject(String namespace) {
        if  (null != dataSourceNamespaceHashMap && dataSourceNamespaceHashMap.containsKey(namespace)) {
            return dataSourceNamespaceHashMap.get(namespace);
        }
        return null;
    }

    @Override
    public String toString() {
        return "DataSourceCluster{" +
                "cluster_name='" + clusterName + '\'' +
                ", namespaces=" + dataSourceNamespaceHashMap +
                '}';
    }
}
