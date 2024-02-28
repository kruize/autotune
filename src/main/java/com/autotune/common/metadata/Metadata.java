package com.autotune.common.metadata;

import java.util.Map;

public class Metadata {
    private String version;
    private Map<String, ClusterGroup> clusterGroups;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, ClusterGroup> getClusterGroups() {
        return clusterGroups;
    }

    public void setClusterGroups(Map<String, ClusterGroup> clusterGroups) {
        this.clusterGroups = clusterGroups;
    }
}
