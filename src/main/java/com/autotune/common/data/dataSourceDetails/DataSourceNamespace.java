package com.autotune.common.data.dataSourceDetails;

import java.util.List;

public class DataSourceNamespace {
    private String namespace;
    private List<DataSourceWorkload> workloads;
    public String getDataSourceNamespaceName() {
        return namespace;
    }

    public void setDataSourceNamespaceName(String namespace) {
        this.namespace = namespace;
    }

    public List<DataSourceWorkload> getDataSourceWorkloads() {
        return workloads;
    }

    public void setDataSourceWorkloads(List<DataSourceWorkload> workloads) {
        this.workloads = workloads;
    }

    @Override
    public String toString(){
        return "DataSourceNamespaces{" +
                "namespace ='" + namespace + '\'' +
                ", workloads ='" + workloads.toString() + '\'' +
                '}';
    }
}
