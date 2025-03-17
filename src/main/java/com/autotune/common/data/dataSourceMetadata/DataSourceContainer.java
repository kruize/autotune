package com.autotune.common.data.dataSourceMetadata;

import com.autotune.utils.KruizeConstants;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

/**
 * DataSourceContainer object represents the container metadata for a workload
 */
public class DataSourceContainer {
    @SerializedName(KruizeConstants.DataSourceConstants.DataSourceMetadataInfoJSONKeys.CONTAINER_NAME)
    @JsonProperty(KruizeConstants.DataSourceConstants.DataSourceMetadataInfoJSONKeys.CONTAINER_NAME)
    private String containerName;
    @SerializedName(KruizeConstants.DataSourceConstants.DataSourceMetadataInfoJSONKeys.CONTAINER_IMAGE_NAME)
    @JsonProperty(KruizeConstants.DataSourceConstants.DataSourceMetadataInfoJSONKeys.CONTAINER_IMAGE_NAME)
    private String containerImageName;

    public DataSourceContainer() {
    }

    public DataSourceContainer(String containerName, String containerImageName) {
        this.containerName = containerName;
        this.containerImageName = containerImageName;
    }

    public String getContainerName() {
        return containerName;
    }

    public String getContainerImageName() {
        return containerImageName;
    }

    @Override
    public String toString() {
        return "DataSourceContainer{" +
                "container_name='" + containerName + '\'' +
                ", container_image_name='" + containerImageName + '\'' +
                '}';
    }
}
