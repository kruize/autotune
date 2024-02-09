package com.autotune.common.data.dataSourceDetails;

import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;

public class DataSourceDetailsInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceDetailsInfo.class);
    private String version;

    /**
     * Key: Cluster group name
     * Value: Associated DataSourceClusterGroup object
     */
    @SerializedName("cluster_groups")
    private HashMap<String, DataSourceClusterGroup> clusterGroupHashMap;

    public DataSourceDetailsInfo(String version, HashMap<String, DataSourceClusterGroup> clusterGroupHashMap) {
        this.version = version;
        this.clusterGroupHashMap = clusterGroupHashMap;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public HashMap<String, DataSourceClusterGroup> getDataSourceClusterGroupHashMap() {
        return clusterGroupHashMap;
    }

    public void setDataSourceClusterGroupHashMap(HashMap<String, DataSourceClusterGroup> clusterGroupHashMap) {
        if (null == clusterGroupHashMap) {
            LOGGER.error("No cluster groups found");
        }
        this.clusterGroupHashMap = clusterGroupHashMap;
    }

    public DataSourceClusterGroup getDataSourceClusterGroupObject(String clusterGroupName) {
        if (null != clusterGroupHashMap && clusterGroupHashMap.containsKey(clusterGroupName)) {
            return clusterGroupHashMap.get(clusterGroupName);
        }
        return null;
    }

    @Override
    public String toString() {
        return "DataSourceDetailsInfo{" +
                "version ='" + version + '\'' +
                ", cluster_group ='" + clusterGroupHashMap.toString() + '\'' +
                '}';
    }
}
