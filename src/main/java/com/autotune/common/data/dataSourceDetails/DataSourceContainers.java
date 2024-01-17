package com.autotune.common.data.dataSourceDetails;

public class DataSourceContainers {
    private String container_name;
    private String container_image_name;

    public String getDataSourceContainerName() { return container_name;}
    public void setDataSourceContainerName(String container_name) {this.container_name = container_name;}
    public String getDataSourceContainerImageName() { return container_image_name;}
    public void setDataSourceContainerImageName(String container_image_name) {this.container_image_name = container_image_name;}

    @Override
    public String toString(){
        return "DataSourceContainers{" +
                "container_name ='" + container_name + '\'' +
                ", container_image_name ='" + container_image_name + '\'' +
                '}';
    }
}
