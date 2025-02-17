package com.autotune.common.data.dataSourceMetadata;

import com.autotune.utils.KruizeConstants;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * DataSource object represents a data source, and it's associated clusters
 * used to store hashmap of DataSourceCluster objects representing cluster metadata
 */
public class DataSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSource.class);
    @SerializedName(KruizeConstants.DataSourceConstants.DataSourceMetadataInfoJSONKeys.DATASOURCE_NAME)
    @JsonProperty(KruizeConstants.DataSourceConstants.DataSourceMetadataInfoJSONKeys.DATASOURCE_NAME)
    private String dataSourceName;
    /**
     * Key: Cluster name
     * Value: Associated DataSourceCluster object
     */
    private HashMap<String, DataSourceCluster> clusters;

    public DataSource() {
    }

    public DataSource(String dataSourceName, HashMap<String, DataSourceCluster> clusters) {
        this.dataSourceName = dataSourceName;
        this.clusters = clusters;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public HashMap<String, DataSourceCluster> getClusters() {
        return clusters;
    }

    public void setClusters(HashMap<String, DataSourceCluster> clusterHashMap) {
        if (null == clusterHashMap) {
            LOGGER.debug(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.SET_CLUSTER_MAP_ERROR + "{}", dataSourceName);
        }
        this.clusters = clusterHashMap;
    }

    public DataSourceCluster getDataSourceClusterObject(String clusterName) {
        if (null != clusters && clusters.containsKey(clusterName)) {
            return clusters.get(clusterName);
        }
        return null;
    }

    @Override
    public String toString() {
        return "DataSource{" +
                "datasource_name='" + dataSourceName + '\'' +
                ", clusters=" + clusters +
                '}';
    }
}
