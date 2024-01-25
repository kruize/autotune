package com.autotune.common.datasource;

import com.autotune.common.data.dataSourceDetails.*;
import com.autotune.common.data.dataSourceQueries.PromQLDataSourceQueries;
import com.google.gson.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * DataSourceDetailsOperator is an abstraction with CRUD operations to manage DataSourceDetailsInfo Object
 * representing JSON for a given data source
 *
 *
 * Currently Supported Implementations:
 *  - createDataSourceDetails
 *
 *  TODO -
 *  object is currently stored in memory moving forward need to store cluster details in Kruize DB
 */
public class DataSourceDetailsOperator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceDetailsOperator.class);
    private static DataSourceDetailsOperator dataSourceOperatorInstance = new DataSourceDetailsOperator();
    private List<DataSourceDetailsInfo> dataSourceDetailsInfoList;

    private DataSourceDetailsOperator() { this.dataSourceDetailsInfoList = new ArrayList<>(); }
    public static DataSourceDetailsOperator getInstance() { return dataSourceOperatorInstance; }
    public List<DataSourceDetailsInfo> getDataSourceDetailsInfoList() { return dataSourceDetailsInfoList; }
    public void createDataSourceDetails(DataSourceInfo dataSourceInfo){

        KruizeDataSourceOperator kruizeDataSourceOperator = DataSourceOperator.getOperator(dataSourceInfo.getProvider());

        DataSourceDetailsHelper dataSourceDetailsHelper = new DataSourceDetailsHelper();

        if (dataSourceInfo.getProvider().equals("prometheus")) {

            PrometheusDataSource prometheusDataSource = new PrometheusDataSource(dataSourceInfo.getName(),dataSourceInfo.getProvider(),dataSourceInfo.getServiceName(),dataSourceInfo.getNamespace());

            JsonArray namespacesDataObject =  kruizeDataSourceOperator.getPrometheusDataResultArray(prometheusDataSource.getDataSourceURL(), PromQLDataSourceQueries.NAMESPACE_QUERY);
            List<String> datasourceNamespaces = dataSourceDetailsHelper.parseActiveNamespaces(namespacesDataObject);

            JsonArray workloadDataObject =  kruizeDataSourceOperator.getPrometheusDataResultArray(prometheusDataSource.getDataSourceURL(), PromQLDataSourceQueries.WORKLOAD_QUERY);
            HashMap<String, List<DataSourceWorkload>> datasourceWorkloads = dataSourceDetailsHelper.parseWorkloadInfo(workloadDataObject);

            JsonArray containerDataObject = kruizeDataSourceOperator.getPrometheusDataResultArray(prometheusDataSource.getDataSourceURL(), PromQLDataSourceQueries.CONTAINER_QUERY);
            HashMap<String, List<DataSourceContainers>> datasourceContainers = dataSourceDetailsHelper.parseContainerInfo(containerDataObject);

            DataSourceDetailsInfo dataSourceDetailsInfo = dataSourceDetailsHelper.createDataSourceDetailsInfoObject(datasourceNamespaces, datasourceWorkloads, datasourceContainers);
            dataSourceDetailsInfoList.add(dataSourceDetailsInfo);

        }
    }

    /*
    TODO - Implement methods to support CRUD operations for periodic update of DataSourceDetailsInfo

    private static retrieveDataSourceDetails(DataSourceInfo d) {
    }

    private static deleteDataSourceDetails(DataSourceInfo d) {
    }

    private static updateDataSourceDetails(DataSourceInfo d) {
    }
    */
}
