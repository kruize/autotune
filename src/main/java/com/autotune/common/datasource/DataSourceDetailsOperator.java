package com.autotune.common.datasource;

import com.autotune.common.data.dataSourceDetails.*;
import com.autotune.common.data.dataSourceQueries.PromQLDataSourceQueries;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
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
 */
public class DataSourceDetailsOperator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceDetailsOperator.class);
    List<DataSourceDetailsInfo> dataSourceDetailsInfoList = new ArrayList<>();
    public void createDataSourceDetails(DataSourceInfo dataSourceInfo){

        KruizeDataSourceOperator kruizeDataSourceOperator = DataSourceOperator.getOperator(dataSourceInfo.getProvider());

        DataSourceDetailsHelper dataSourceDetailsHelper = new DataSourceDetailsHelper();

        if (dataSourceInfo.getProvider().equals("prometheus")) {

            PrometheusDataSource prometheusDataSource = new PrometheusDataSource(dataSourceInfo.getName(),dataSourceInfo.getProvider(),dataSourceInfo.getServiceName(),dataSourceInfo.getNamespace());

            JsonObject namespacesDataObject = (JsonObject) kruizeDataSourceOperator.extractDataObject(prometheusDataSource.getDataSourceURL(), PromQLDataSourceQueries.NAMESPACE_QUERY);
            List<String> datasourceNamespaces = dataSourceDetailsHelper.parseActiveNamespaces(namespacesDataObject);

            JsonObject workloadDataObject = (JsonObject) kruizeDataSourceOperator.extractDataObject(prometheusDataSource.getDataSourceURL(), PromQLDataSourceQueries.WORKLOAD_QUERY);
            HashMap<String, List<DataSourceWorkload>> datasourceWorkloads = dataSourceDetailsHelper.parseWorkloadInfo(workloadDataObject);

            JsonObject containerDataObject = (JsonObject) kruizeDataSourceOperator.extractDataObject(prometheusDataSource.getDataSourceURL(), PromQLDataSourceQueries.CONTAINER_QUERY);
            HashMap<String, List<DataSourceContainers>> datasourceContainers = dataSourceDetailsHelper.parseContainerInfo(containerDataObject);

            DataSourceDetailsInfo dataSourceDetailsInfo = dataSourceDetailsHelper.createDataSourceDetailsInfoObject(datasourceNamespaces, datasourceWorkloads, datasourceContainers);
            dataSourceDetailsInfoList.add(dataSourceDetailsInfo);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonOutput = gson.toJson(dataSourceDetailsInfoList);

            LOGGER.info(jsonOutput);

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
