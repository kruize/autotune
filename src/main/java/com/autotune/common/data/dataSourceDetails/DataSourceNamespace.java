package com.autotune.common.data.dataSourceDetails;

import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;

/**
 * DataSourceNamespace object represents the namespace of a cluster, and it's associated workloads
 * used to store hashmap of DataSourceWorkload objects representing workload metadata
 */
public class DataSourceNamespace {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceNamespace.class);
    private String namespace;

    // key = workload_name
    @SerializedName("workloads")
    private HashMap<String, DataSourceWorkload> workloadHashMap ;
    public DataSourceNamespace(String namespace) {
        this.namespace = namespace;
    }

    public DataSourceNamespace(String namespace, HashMap<String, DataSourceWorkload> workloadHashMap) {
        this.namespace = namespace;
        this.workloadHashMap = workloadHashMap;
    }

    public String getDataSourceNamespaceName() {
        return namespace;
    }

    public void setDataSourceNamespaceName(String namespace) {
        this.namespace = namespace;
    }

    public HashMap<String, DataSourceWorkload> getDataSourceWorkloads() {
        return workloadHashMap;
    }

    public void setDataSourceWorkloadHashMap(HashMap<String, DataSourceWorkload> workloadHashMap) {
       this.workloadHashMap = workloadHashMap;
    }

    @Override
    public String toString() {
        return "DataSourceNamespace{" +
                "namespace='" + namespace + '\'' +
                ", workloads=" + workloadHashMap +
                '}';
    }
}
