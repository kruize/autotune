package com.autotune.common.data.dataSourceMetadata;

import com.autotune.utils.KruizeConstants;
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
    private HashMap<String, DataSourceWorkload> workloads;

    public DataSourceNamespace() {
    }

    public DataSourceNamespace(String namespace, HashMap<String, DataSourceWorkload> workloads) {
        this.namespace = namespace;
        this.workloads = workloads;
    }

    public String getNamespace() {
        return namespace;
    }

    public HashMap<String, DataSourceWorkload> getWorkloads() {
        return workloads;
    }

    public void setWorkloads(HashMap<String, DataSourceWorkload> workloadHashMap) {
        if (null == workloadHashMap) {
            LOGGER.debug(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.SET_WORKLOAD_MAP_ERROR + "{}", namespace);
        }
        this.workloads = workloadHashMap;
    }

    public DataSourceWorkload getDataSourceWorkloadObject(String workloadName) {
        if (null != workloads && workloads.containsKey(workloadName)) {
            return workloads.get(workloadName);
        }

        return null;
    }

    @Override
    public String toString() {
        return "DataSourceNamespace{" +
                "namespace='" + namespace + '\'' +
                ", workloads=" + workloads +
                '}';
    }
}
