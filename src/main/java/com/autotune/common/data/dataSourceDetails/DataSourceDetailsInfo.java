package com.autotune.common.data.dataSourceDetails;

public class DataSourceDetailsInfo {
    private String version;
    private DataSourceClusterGroup cluster_group;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public DataSourceClusterGroup getDataSourceClusterGroup() {
        return cluster_group;
    }

    public void setDataSourceClusterGroup(DataSourceClusterGroup cluster_group) {
        this.cluster_group = cluster_group;
    }

    @Override
    public String toString(){
        return "DataSourceDetailsInfo{" +
                "version ='" + version + '\'' +
                ", cluster_group ='" + cluster_group.toString() + '\'' +
                '}';
    }
}
