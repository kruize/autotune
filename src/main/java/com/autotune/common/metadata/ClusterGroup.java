package com.autotune.common.metadata;

import java.util.Map;

public class ClusterGroup {
    private String clusterGroupName;
    private Map<String, Cluster> clusters;

    public String getClusterGroupName() {
        return clusterGroupName;
    }

    public void setClusterGroupName(String clusterGroupName) {
        this.clusterGroupName = clusterGroupName;
    }

    public Map<String, Cluster> getClusters() {
        return clusters;
    }

    public void setClusters(Map<String, Cluster> clusters) {
        this.clusters = clusters;
    }
}
