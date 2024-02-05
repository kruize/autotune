package com.autotune.common.data.dataSourceDetails;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

public class DataSourceNamespace {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceNamespace.class);
    private String namespace;
    private List<DataSourceWorkload> workloads;
    public DataSourceNamespace(String namespace) {
        this.namespace = namespace;
        this.workloads = new ArrayList<>();

    }
    public DataSourceNamespace(String namespace, List<DataSourceWorkload> dataSourceWorkloadList) {
        this.namespace = namespace;
        this.workloads = dataSourceWorkloadList;
    }

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
        if (workloads.isEmpty()) {
            LOGGER.info("No workloads found for namespace: " + namespace);
        }
        this.workloads = workloads;
    }

    @Override
    public String toString() {
        return "DataSourceNamespaces{" +
                "namespace ='" + namespace + '\'' +
                ", workloads ='" + workloads.toString() + '\'' +
                '}';
    }
}
