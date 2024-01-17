package com.autotune.common.data.dataSourceDetails;

public class DataSourceClusterGroup {
    private String cluster_group_name;
    private DataSourceCluster cluster;

    public String getDataSourceClusterGroupName() {
        return cluster_group_name;
    }

    public void setDataSourceClusterGroupName(String cluster_group_name) {
        this.cluster_group_name = cluster_group_name;
    }

    public DataSourceCluster getDataSourceCluster() {
        return cluster;
    }

    public void setDataSourceCluster(DataSourceCluster cluster) {
        this.cluster = cluster;
    }


    @Override
    public String toString(){
        return "DataSourceClusterGroup{" +
                "cluster_group_name ='" + cluster_group_name + '\'' +
                ", cluster ='" + cluster.toString() + '\'' +
                '}';
    }

}
