package com.autotune.common.data.dataSourceMetadata;


/**
 * DataSourceContainer object represents the container metadata for a workload
 */
public class DataSourceContainer {
    // TODO - add KruizeConstants for attributes
    private String containerName;
    private String containerImageName;

    public DataSourceContainer(String containerName, String containerImageName) {
        this.containerName = containerName;
        this.containerImageName = containerImageName;
    }

    public String getDataSourceContainerName() { return containerName;}
    public String getDataSourceContainerImageName() { return containerImageName;}

    @Override
    public String toString() {
        return "DataSourceContainer{" +
                "containerName='" + containerName + '\'' +
                ", containerImageName='" + containerImageName + '\'' +
                '}';
    }
}
