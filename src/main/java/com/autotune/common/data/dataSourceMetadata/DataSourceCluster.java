package com.autotune.common.data.dataSourceMetadata;

import com.autotune.utils.KruizeConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * DataSourceCluster object represents the cluster of a data source, and it's associated namespaces
 * used to store hashmap of DataSourceNamespace objects representing namespace metadata
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataSourceCluster {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceCluster.class);
    @SerializedName(KruizeConstants.DataSourceConstants.DataSourceMetadataInfoJSONKeys.CLUSTER_NAME)
    @JsonProperty(KruizeConstants.DataSourceConstants.DataSourceMetadataInfoJSONKeys.CLUSTER_NAME)
    private String clusterName;

    /**
     * Key: Namespace
     * Value: Associated DataSourceNamespace object
     */
    private HashMap<String, DataSourceNamespace> namespaces;

    public DataSourceCluster() {
    }

    public DataSourceCluster(String clusterName, HashMap<String, DataSourceNamespace> namespaces) {
        this.clusterName = clusterName;
        this.namespaces = namespaces;
    }

    public String getDataSourceClusterName() {
        return clusterName;
    }

    public HashMap<String, DataSourceNamespace> getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(HashMap<String, DataSourceNamespace> namespaceHashMap) {
        if (null == namespaceHashMap) {
            LOGGER.debug(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.SET_NAMESPACE_MAP_ERROR + "{}", clusterName);
        }
        this.namespaces = namespaceHashMap;
    }

    public DataSourceNamespace getDataSourceNamespaceObject(String namespace) {
        if (null != namespaces && namespaces.containsKey(namespace)) {
            return namespaces.get(namespace);
        }
        return null;
    }

    @Override
    public String toString() {
        return "DataSourceCluster{" +
                "cluster_name='" + clusterName + '\'' +
                ", namespaces=" + namespaces +
                '}';
    }
}
