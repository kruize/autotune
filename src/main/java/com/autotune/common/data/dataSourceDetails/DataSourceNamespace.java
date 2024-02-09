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

    /**
     * Key: Workload name
     * Value: Associated DataSourceWorkload object
     */
    @SerializedName("workloads")
    private HashMap<String, DataSourceWorkload> workloadHashMap ;

    public DataSourceNamespace(String namespace, HashMap<String, DataSourceWorkload> workloadHashMap) {
        this.namespace = namespace;
        this.workloadHashMap = workloadHashMap;
    }

    public String getDataSourceNamespaceName() {
        return namespace;
    }

    public HashMap<String, DataSourceWorkload> getDataSourceWorkloadHashMap() {
        return workloadHashMap;
    }

    public void setDataSourceWorkloadHashMap(HashMap<String, DataSourceWorkload> workloadHashMap) {
        if (null == workloadHashMap) {
            LOGGER.error("No workloads found for namespace - "+ namespace);
        }
       this.workloadHashMap = workloadHashMap;
    }

    public DataSourceWorkload getDataSourceWorkloadObject(String workloadName) {
        if (null != workloadHashMap && workloadHashMap.containsKey(workloadName)) {
            return workloadHashMap.get(workloadName);
        }

        return null;
    }
    @Override
    public String toString() {
        return "DataSourceNamespace{" +
                "namespace='" + namespace + '\'' +
                ", workloads=" + workloadHashMap +
                '}';
    }
}
