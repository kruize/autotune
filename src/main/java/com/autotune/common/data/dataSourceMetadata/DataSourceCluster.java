package com.autotune.common.data.dataSourceMetadata;

import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * DataSourceCluster object represents the cluster of a data source, and it's associated namespaces
 * used to store hashmap of DataSourceNamespace objects representing namespace metadata
 */
public class DataSourceCluster {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceCluster.class);
    @SerializedName(KruizeConstants.DataSourceConstants.DataSourceMetadataInfoJSONKeys.CLUSTER_NAME)
    private String clusterName;

    /**
     * Key: Namespace
     * Value: Associated DataSourceNamespace object
     */
    @SerializedName(KruizeConstants.DataSourceConstants.DataSourceMetadataInfoJSONKeys.NAMESPACES)
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
        if (null == namespaceHashMap) {
            LOGGER.debug(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.SET_NAMESPACE_MAP_ERROR + "{}", clusterName);
        }
        this.namespaceHashMap = namespaceHashMap;
    }

    public DataSourceNamespace getDataSourceNamespaceObject(String namespace) {
        if (null != namespaceHashMap && namespaceHashMap.containsKey(namespace)) {
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
