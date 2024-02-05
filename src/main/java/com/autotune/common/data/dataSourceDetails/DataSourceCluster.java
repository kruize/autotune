package com.autotune.common.data.dataSourceDetails;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.util.ArrayList;
import java.util.List;

public class DataSourceCluster {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceCluster.class);
    private String cluster_name;
    private List<DataSourceNamespace> namespaces;

    public DataSourceCluster(String cluster_name) {
        this.cluster_name = cluster_name;
        this.namespaces = new ArrayList<>();
    }

    public DataSourceCluster(String cluster_name, List<DataSourceNamespace> namespaces) {
        this.cluster_name = cluster_name;
        this.namespaces = namespaces;
    }

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
        if (namespaces == null || namespaces.isEmpty()) {
            LOGGER.info("No namespaces found for cluster: " + cluster_name);
        }
        this.namespaces = namespaces;
    }

    @Override
    public String toString() {
        return "DataSourceCluster{" +
                "cluster_group_name ='" + cluster_name + '\'' +
                ", namespaces ='" + namespaces.toString() + '\'' +
                '}';
    }
}
