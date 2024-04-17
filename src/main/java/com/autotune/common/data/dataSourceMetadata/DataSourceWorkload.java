package com.autotune.common.data.dataSourceMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * DataSourceWorkload object represents the workload of a namespace, and it's associated containers
 * used to store hashmap of DataSourceContainer objects representing container metadata
 */
public class DataSourceWorkload {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceWorkload.class);
    // TODO - add KruizeConstants for attributes
    private String workloadName;
    private String workloadType;

    /**
     * Key: Container name
     * Value: Associated DataSourceContainer object
     */
    private HashMap<String, DataSourceContainer> containerHashMap;

    public DataSourceWorkload(String workloadName, String workloadType, HashMap<String, DataSourceContainer> containerHashMap) {
        this.workloadName = workloadName;
        this.workloadType = workloadType;
        this.containerHashMap = containerHashMap;
    }

    public String getDataSourceWorkloadName() {return workloadName;}

    public String getDataSourceWorkloadType() {return workloadType;}

    public HashMap<String, DataSourceContainer> getDataSourceContainerHashMap() {
        return containerHashMap;
    }

    public void setDataSourceContainerHashMap(HashMap<String, DataSourceContainer> containerHashMap) {
        // TODO: Validate input before setting the containerHashMap
        this.containerHashMap = containerHashMap;
    }

    @Override
    public String toString() {
        return "DataSourceWorkload{" +
                "workloadName='" + workloadName + '\'' +
                ", workloadType='" + workloadType + '\'' +
                ", containerHashMap=" + containerHashMap +
                '}';
    }
}
