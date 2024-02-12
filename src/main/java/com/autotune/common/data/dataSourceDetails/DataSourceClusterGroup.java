package com.autotune.common.data.dataSourceDetails;

import com.autotune.utils.KruizeConstants;
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
    @SerializedName(KruizeConstants.DataSourceConstants.DataSourceDetailsInfoJSONKeys.CLUSTER_GROUP_NAME)
    private String clusterGroupName;

    /**
     * Key: Cluster name
     * Value: Associated DataSourceCluster object
     */
    @SerializedName(KruizeConstants.DataSourceConstants.DataSourceDetailsInfoJSONKeys.CLUSTERS)
    private HashMap<String, DataSourceCluster> clusterHashMap;

    public DataSourceClusterGroup(String clusterGroupName, HashMap<String,DataSourceCluster> clusters) {
        this.clusterGroupName = clusterGroupName;
        this.clusterHashMap = clusters;
    }

    public String getDataSourceClusterGroupName() {
        return clusterGroupName;
    }

    public HashMap<String, DataSourceCluster> getDataSourceClusterHashMap() {
        return clusterHashMap;
    }

    public void setDataSourceClusterHashMap(HashMap<String, DataSourceCluster> clusters) {
        if (null == clusters) {
            LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceDetailsErrorMsgs.SET_CLUSTER_MAP_ERROR + clusterGroupName);
        }
        this.clusterHashMap = clusters;
    }

    public DataSourceCluster getDataSourceClusterObject(String clusterName) {
        if (null != clusterHashMap && clusterHashMap.containsKey(clusterName)) {
            return clusterHashMap.get(clusterName);
        }
        return null;
    }

    @Override
    public String toString() {
        return "DataSourceClusterGroup{" +
                "cluster_group_name='" + clusterGroupName + '\'' +
                ", clusters=" + clusterHashMap +
                '}';
    }
}
