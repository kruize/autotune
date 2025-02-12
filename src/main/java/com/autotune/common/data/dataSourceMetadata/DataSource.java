package com.autotune.common.data.dataSourceMetadata;

import com.autotune.utils.KruizeConstants;
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
    private String dataSourceName;

    /**
     * Key: Cluster name
     * Value: Associated DataSourceCluster object
     */
    @SerializedName(KruizeConstants.DataSourceConstants.DataSourceMetadataInfoJSONKeys.CLUSTERS)
    private HashMap<String, DataSourceCluster> clusterHashMap;

    public DataSource(String dataSourceName, HashMap<String, DataSourceCluster> clusterHashMap) {
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
        if (null == clusterHashMap) {
            LOGGER.debug(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.SET_CLUSTER_MAP_ERROR + "{}", dataSourceName);
        }
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
                "datasource_name='" + dataSourceName + '\'' +
                ", clusters=" + clusterHashMap +
                '}';
    }
}
