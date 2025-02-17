package com.autotune.common.data.dataSourceMetadata;

import com.autotune.utils.KruizeConstants;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * DataSourceWorkload object represents the workload of a namespace, and it's associated containers
 * used to store hashmap of DataSourceContainer objects representing container metadata
 */
public class DataSourceWorkload {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceWorkload.class);
    @SerializedName(KruizeConstants.DataSourceConstants.DataSourceMetadataInfoJSONKeys.WORKLOAD_NAME)
    @JsonProperty(KruizeConstants.DataSourceConstants.DataSourceMetadataInfoJSONKeys.WORKLOAD_NAME)
    private String workloadName;
    @SerializedName(KruizeConstants.DataSourceConstants.DataSourceMetadataInfoJSONKeys.WORKLOAD_TYPE)
    @JsonProperty(KruizeConstants.DataSourceConstants.DataSourceMetadataInfoJSONKeys.WORKLOAD_TYPE)
    private String workloadType;
    /**
     * Key: Container name
     * Value: Associated DataSourceContainer object
     */
    private HashMap<String, DataSourceContainer> containers;

    public DataSourceWorkload() {
    }

    public DataSourceWorkload(String workloadName, String workloadType, HashMap<String, DataSourceContainer> containers) {
        this.workloadName = workloadName;
        this.workloadType = workloadType;
        this.containers = containers;
    }

    public String getWorkloadName() {
        return workloadName;
    }

    public String getWorkloadType() {
        return workloadType;
    }

    public HashMap<String, DataSourceContainer> getContainers() {
        return containers;
    }

    public void setContainers(HashMap<String, DataSourceContainer> containerHashMap) {
        if (containerHashMap == null) {
            LOGGER.debug(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.SET_CONTAINER_MAP_ERROR + "{}", workloadName);
        }
        this.containers = containerHashMap;
    }

    @Override
    public String toString() {
        return "DataSourceWorkload{" +
                "workload_name='" + workloadName + '\'' +
                ", workload_type='" + workloadType + '\'' +
                ", conatiners=" + containers +
                '}';
    }
}
