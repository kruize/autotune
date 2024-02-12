package com.autotune.common.data.dataSourceDetails;

import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;
import java.util.HashMap;

/**
 * DataSourceCluster object represents the cluster of a data source, and it's associated namespaces
 * used to store hashmap of DataSourceNamespace objects representing namespace metadata
 */
public class DataSourceCluster {
    @SerializedName(KruizeConstants.DataSourceConstants.DataSourceDetailsInfoJSONKeys.CLUSTER_NAME)
    private String clusterName;

    /**
     * Key: Namespace
     * Value: Associated DataSourceNamespace object
     */
    @SerializedName(KruizeConstants.DataSourceConstants.DataSourceDetailsInfoJSONKeys.NAMESPACES)
    private HashMap<String, DataSourceNamespace> namespaceHashMap;

    public DataSourceCluster(String clusterName, HashMap<String, DataSourceNamespace> dataSourceNamespaceHashMap) {
        this.clusterName = clusterName;
        this.namespaceHashMap = dataSourceNamespaceHashMap;
    }

    public String getDataSourceClusterName() {
        return clusterName;
    }

    public HashMap<String, DataSourceNamespace> getDataSourceNamespaceHashMap() {
        return namespaceHashMap;
    }

    public DataSourceNamespace getDataSourceNamespaceObject(String namespace) {
        if  (null != namespaceHashMap && namespaceHashMap.containsKey(namespace)) {
            return namespaceHashMap.get(namespace);
        }
        return null;
    }

    @Override
    public String toString() {
        return "DataSourceCluster{" +
                "cluster_name='" + clusterName + '\'' +
                ", namespaces=" + namespaceHashMap +
                '}';
    }
}
