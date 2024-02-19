package com.autotune.common.data.dataSourceDetails;

import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

/**
 * DataSourceContainers object represents the container metadata for a workload
 */
public class DataSourceContainer {
    @SerializedName(KruizeConstants.DataSourceConstants.DataSourceDetailsInfoJSONKeys.CONTAINER_NAME)
    private String containerName;
    @SerializedName(KruizeConstants.DataSourceConstants.DataSourceDetailsInfoJSONKeys.CONTAINER_IMAGE_NAME)
    private String containerImageName;

    public DataSourceContainer(String containerName, String container_image_name) {
        this.containerName = containerName;
        this.containerImageName = container_image_name;
    }

    public String getDataSourceContainerName() { return containerName;}
    public String getDataSourceContainerImageName() { return containerImageName;}

    @Override
    public String toString() {
        return "DataSourceContainer{" +
                "container_name ='" + containerName + '\'' +
                ", container_image_name ='" + containerImageName + '\'' +
                '}';
    }
}
