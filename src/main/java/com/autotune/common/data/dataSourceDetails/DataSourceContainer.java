package com.autotune.common.data.dataSourceDetails;

import com.google.gson.annotations.SerializedName;

/**
 * DataSourceContainers object represents the container metadata for a workload
 */
public class DataSourceContainer {
    @SerializedName("container_name")
    private String containerName;
    @SerializedName("container_image_name")
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
