package com.autotune.common.data.dataSourceDetails;

import java.util.List;

public class DataSourceWorkload {
    private String workload_name;
    private String workload_type;
    private List<DataSourceContainers> containers;

    public String getDataSourceWorkloadName() {return workload_name;}

    public void setDataSourceWorkloadName(String workload_name) {this.workload_name = workload_name;}

    public String getDataSourceWorkloadType() {return workload_type;}

    public void setDataSourceWorkloadType(String workload_type) {this.workload_type = workload_type;}

    public List<DataSourceContainers> getDataSourceContainers() {
        return containers;
    }

    public void setDataSourceContainers(List<DataSourceContainers> containers) {
        this.containers = containers;
    }

    @Override
    public String toString(){
        return "DataSourceWorkload{" +
                "workload_name ='" + workload_name + '\'' +
                ", workload_type ='" + workload_type + '\'' +
                ", containers ='" + containers.toString() + '\'' +
                '}';
    }
}
