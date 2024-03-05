package com.autotune.common.datasource;

import com.autotune.common.data.dataSourceDetails.*;
import com.autotune.common.data.dataSourceQueries.PromQLDataSourceQueries;
import com.autotune.utils.KruizeConstants;
import com.google.gson.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;

/**
 * DataSourceDetailsOperator is an abstraction with CRUD operations to manage DataSourceDetailsInfo Object
 * representing JSON for a given data source
 *
 * Currently Supported Implementations:
 *  - createDataSourceDetails
 *  TODO -
 *  object is currently stored in memory moving forward need to store cluster details in Kruize DB
 */
public class DataSourceDetailsOperator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceDetailsOperator.class);
    private static final DataSourceDetailsOperator dataSourceDetailsOperatorInstance = new DataSourceDetailsOperator();
    private DataSourceDetailsInfo dataSourceDetailsInfo;
    private DataSourceDetailsOperator() { this.dataSourceDetailsInfo = null; }
    public static DataSourceDetailsOperator getInstance() { return dataSourceDetailsOperatorInstance; }

    /**
     * Creates and populates details for a data source based on the provided DataSourceInfo object.
     *
     * Currently supported DataSourceProvider - Prometheus
     *
     * @param dataSourceInfo The DataSourceInfo object containing information about the data source.
     * TODO - support multiple data sources
     */
    public void createDataSourceDetails(DataSourceInfo dataSourceInfo) {

        DataSourceDetailsHelper dataSourceDetailsHelper = new DataSourceDetailsHelper();
        /**
         * Get DataSourceOperatorImpl instance on runtime based on dataSource provider
         */
        DataSourceOperatorImpl op = DataSourceOperatorImpl.getInstance().getOperator(dataSourceInfo.getProvider());

        if (null == op) {
            return;
        }

        /**
         * For the "prometheus" data source, fetches and processes data related to namespaces, workloads, and containers,
         * creating a comprehensive DataSourceDetailsInfo object that is then added to a list.
         * TODO - Process cluster metadata using a custom query
         */
        try {
            JsonArray namespacesDataResultArray =  op.getResultArrayForQuery(dataSourceInfo.getUrl().toString(), PromQLDataSourceQueries.NAMESPACE_QUERY);
            if (false == op.validateResultArray(namespacesDataResultArray)){
                dataSourceDetailsInfo = dataSourceDetailsHelper.createDataSourceDetailsInfoObject(dataSourceInfo.getName(), null);
                return;
            }

            /**
             * Key: Name of namespace
             * Value: DataSourceNamespace object corresponding to a namespace
             */
            HashMap<String, DataSourceNamespace> datasourceNamespaces = dataSourceDetailsHelper.getActiveNamespaces(namespacesDataResultArray);
            dataSourceDetailsInfo = dataSourceDetailsHelper.createDataSourceDetailsInfoObject(dataSourceInfo.getName(), datasourceNamespaces);

            /**
             * Outer map:
             * Key: Name of namespace
             * <p>
             * Inner map:
             * Key: Name of workload
             * Value: DataSourceWorkload object matching the name
             * TODO -  get workload metadata for a given namespace
             */
            HashMap<String, HashMap<String, DataSourceWorkload>> datasourceWorkloads = new HashMap<>();
            JsonArray workloadDataResultArray = op.getResultArrayForQuery(dataSourceInfo.getUrl().toString(), PromQLDataSourceQueries.WORKLOAD_QUERY);
            if (true == op.validateResultArray(workloadDataResultArray)) {
                datasourceWorkloads = dataSourceDetailsHelper.getWorkloadInfo(workloadDataResultArray);
            }
            dataSourceDetailsHelper.updateWorkloadDataSourceDetailsInfoObject(dataSourceInfo.getName(), dataSourceDetailsInfo, datasourceWorkloads);

            /**
             * Outer map:
             * Key: Name of workload
             * <p>
             * Inner map:
             * Key: Name of container
             * Value: DataSourceContainer object matching the name
             * TODO - get container metadata for a given workload
             */
            HashMap<String, HashMap<String, DataSourceContainer>> datasourceContainers = new HashMap<>();
            JsonArray containerDataResultArray = op.getResultArrayForQuery(dataSourceInfo.getUrl().toString(), PromQLDataSourceQueries.CONTAINER_QUERY);
            if (true == op.validateResultArray(containerDataResultArray)) {
                datasourceContainers = dataSourceDetailsHelper.getContainerInfo(containerDataResultArray);
            }
            dataSourceDetailsHelper.updateContainerDataSourceDetailsInfoObject(dataSourceInfo.getName(), dataSourceDetailsInfo, datasourceWorkloads, datasourceContainers);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Retrieves DataSourceDetailsInfo object.
     * @return DataSourceDetailsInfo containing details about the data source if found, otherwise null.
     */
    public DataSourceDetailsInfo getDataSourceDetailsInfo(DataSourceInfo dataSource) {
        try {
            if (null == dataSourceDetailsInfo) {
                LOGGER.debug(KruizeConstants.DataSourceConstants.DataSourceDetailsErrorMsgs.DATASOURCE_DETAILS_INFO_NOT_AVAILABLE);
                return null;
            }
            String clusterGroupName = dataSource.getName();
            HashMap<String, DataSourceClusterGroup> clusterGroupHashMap = dataSourceDetailsInfo.getDataSourceClusterGroupHashMap();

            if (null == clusterGroupHashMap || !clusterGroupHashMap.containsKey(clusterGroupName)) {
                LOGGER.debug(KruizeConstants.DataSourceConstants.DataSourceDetailsErrorMsgs.DATASOURCE_DETAILS_CLUSTER_GROUP_NOT_AVAILABLE + clusterGroupName);
                return null;
            }

            DataSourceClusterGroup targetClusterGroup = clusterGroupHashMap.get(clusterGroupName);
            HashMap<String, DataSourceClusterGroup> targetClusterGroupHashMap = new HashMap<>();
            targetClusterGroupHashMap.put(clusterGroupName, targetClusterGroup);
            return new DataSourceDetailsInfo(targetClusterGroupHashMap);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return null;
        }
    }

    /*
    TODO - Implement methods to support update and delete operations for periodic update of DataSourceDetailsInfo
    public DataSourceDetailsInfo updateDataSourceDetails(DataSourceInfo dataSource) {

    }

    public void deleteDataSourceDetails(DataSourceInfo dataSource) {

    }

     */
}
