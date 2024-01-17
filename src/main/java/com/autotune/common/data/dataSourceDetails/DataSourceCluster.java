package com.autotune.common.data.dataSourceDetails;

import java.util.List;

public class DataSourceCluster {
    private String cluster_name;
    private List<DataSourceNamespace> namespaces;

    public String getDataSourceClusterName() {
        return cluster_name;
    }

    public void setDataSourceClusterName(String cluster_name) {
        this.cluster_name = cluster_name;
    }

    public List<DataSourceNamespace> getDataSourceNamespaces() {
        return namespaces;
    }

    public void setDataSourceNamespaces(List<DataSourceNamespace> namespaces) {
        this.namespaces = namespaces;
    }

    @Override
    public String toString(){
        return "DataSourceCluster{" +
                "cluster_group_name ='" + cluster_name + '\'' +
                ", namespaces ='" + namespaces.toString() + '\'' +
                '}';
    }
}
