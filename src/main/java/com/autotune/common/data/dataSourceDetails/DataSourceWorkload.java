package com.autotune.common.data.dataSourceDetails;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DataSourceWorkload {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceWorkload.class);
    private String workload_name;
    private String workload_type;
    private List<DataSourceContainers> containers;

    public DataSourceWorkload(String workload_name, String workload_type) {
        this.workload_name = workload_name;
        this.workload_type = workload_type;
        this.containers = new ArrayList<>();
    }
    public DataSourceWorkload(String workload_name, String workload_type, List<DataSourceContainers> containers) {
        this.workload_name = workload_name;
        this.workload_type = workload_type;
        this.containers = containers;
    }

    public String getDataSourceWorkloadName() {return workload_name;}

    public void setDataSourceWorkloadName(String workload_name) {this.workload_name = workload_name;}

    public String getDataSourceWorkloadType() {return workload_type;}

    public void setDataSourceWorkloadType(String workload_type) {this.workload_type = workload_type;}

    public List<DataSourceContainers> getDataSourceContainers() {
        return containers;
    }

    public void setDataSourceContainers(List<DataSourceContainers> containers) {
        if (containers == null || containers.isEmpty()) {
            LOGGER.info("No containers found for workload: "+ workload_name);
        }
        this.containers = containers;
    }

    @Override
    public String toString() {
        return "DataSourceWorkload{" +
                "workload_name ='" + workload_name + '\'' +
                ", workload_type ='" + workload_type + '\'' +
                ", containers ='" + containers.toString() + '\'' +
                '}';
    }
}
