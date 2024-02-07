package com.autotune.common.data.dataSourceDetails;

import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;

public class DataSourceDetailsInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceDetailsInfo.class);
    private String version;
    // key = cluster_group_name
    @SerializedName("cluster_groups")
    private HashMap<String, DataSourceClusterGroup> clusterGroupHashMap;

    public DataSourceDetailsInfo(String version) {
        this.version = version;
    }

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

    public HashMap<String, DataSourceClusterGroup> getDataSourceClusterGroup() {
        return clusterGroupHashMap;
    }

    public void setDataSourceClusterGroup(HashMap<String, DataSourceClusterGroup> clusterGroupHashMap) {
        this.clusterGroupHashMap = clusterGroupHashMap;
    }

    @Override
    public String toString() {
        return "DataSourceDetailsInfo{" +
                "version ='" + version + '\'' +
                ", cluster_group ='" + clusterGroupHashMap.toString() + '\'' +
                '}';
    }
}
