package com.autotune.common.datasource;

import com.autotune.common.data.dataSourceDetails.*;
import com.autotune.common.data.dataSourceQueries.PromQLDataSourceQueries;
import com.autotune.common.datasource.prometheus.PrometheusDataOperatorImpl;
import com.autotune.common.exceptions.InvalidDataSourceQueryData;
import com.autotune.utils.KruizeConstants;
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
    private DataSourceDetailsInfo dataSourceDetailsInfo;
    private DataSourceDetailsOperator() { this.dataSourceDetailsInfo = null; }
    public static DataSourceDetailsOperator getInstance() { return dataSourceOperatorInstance; }
    public DataSourceDetailsInfo getDataSourceDetailsInfo() { return dataSourceDetailsInfo; }

    /**
     * Creates and populates details for a data source based on the provided DataSourceInfo object.
     *
     * Currently supported DataSourceProvider - Prometheus
     *
     * @param dataSourceInfo The DataSourceInfo object containing information about the data source.
     * TODO - support multiple data sources
     */
    public void createDataSourceDetails (DataSourceInfo dataSourceInfo) {

        DataSourceDetailsHelper dataSourceDetailsHelper = new DataSourceDetailsHelper();

        /**
        * For the "prometheus" provider, fetches and processes data related to namespaces, workloads, and containers,
        * creating a comprehensive DataSourceDetailsInfo object that is then added to a list.
        */
        if (dataSourceInfo.getProvider().equals(KruizeConstants.SupportedDatasources.PROMETHEUS)) {
           PrometheusDataOperatorImpl op = (PrometheusDataOperatorImpl) DataSourceOperatorImpl.getInstance().getOperator(dataSourceInfo.getProvider());

           try {
               JsonArray namespacesDataResultArray =  op.getResultArrayForQuery(dataSourceInfo.getUrl().toString(), PromQLDataSourceQueries.NAMESPACE_QUERY);

               if (!op.validateResultArray(namespacesDataResultArray)) {
                   throw new InvalidDataSourceQueryData(KruizeConstants.DataSourceConstants.DataSourceDetailsErrorMsgs.INVALID_NAMESPACE_DATA);
               }
               List<String> datasourceNamespaces = dataSourceDetailsHelper.getActiveNamespaces(namespacesDataResultArray);

               JsonArray workloadDataResultArray = op.getResultArrayForQuery(dataSourceInfo.getUrl().toString(), PromQLDataSourceQueries.WORKLOAD_QUERY);

               if (!op.validateResultArray(workloadDataResultArray)) {
                   throw new InvalidDataSourceQueryData(KruizeConstants.DataSourceConstants.DataSourceDetailsErrorMsgs.INVALID_WORKLOAD_DATA);
               }
               HashMap<String, List<DataSourceWorkload>> datasourceWorkloads = dataSourceDetailsHelper.getWorkloadInfo(workloadDataResultArray);

               JsonArray containerDataResultArray = op.getResultArrayForQuery(dataSourceInfo.getUrl().toString(), PromQLDataSourceQueries.CONTAINER_QUERY);

               if (!op.validateResultArray(containerDataResultArray)) {
                   throw new InvalidDataSourceQueryData(KruizeConstants.DataSourceConstants.DataSourceDetailsErrorMsgs.INVALID_CONTAINER_DATA);
               }
               HashMap<String, List<DataSourceContainers>> datasourceContainers = dataSourceDetailsHelper.getContainerInfo(containerDataResultArray);

               dataSourceDetailsInfo = dataSourceDetailsHelper.createDataSourceDetailsInfoObject(datasourceNamespaces, datasourceWorkloads, datasourceContainers);


           } catch (InvalidDataSourceQueryData e) {
               LOGGER.error(e.getMessage());
           }
        }
    }

    public DataSourceDetailsInfo getDataSourceDetails(DataSourceInfo dataSourceInfo) {

        if (dataSourceDetailsInfo != null && dataSourceDetailsInfo.getDataSourceClusterGroup().getDataSourceClusterGroupName().equals(dataSourceInfo.getProvider())) {
            return dataSourceDetailsInfo;
        }
        return null;
    }

    /*
    TODO - Implement methods to support CRUD operations for periodic update of DataSourceDetailsInfo

    private static deleteDataSourceDetails(DataSourceInfo d) {
    }

    private static updateDataSourceDetails(DataSourceInfo d) {
    }
    */
}
