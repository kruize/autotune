package com.autotune.common.datasource;

import com.autotune.common.data.dataSourceDetails.*;
import com.autotune.common.data.dataSourceQueries.PromQLDataSourceQueries;
import com.autotune.common.exceptions.DataSourceDetailsInfoCreationException;
import com.autotune.utils.KruizeConstants;
import com.google.gson.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;

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
    private static DataSourceDetailsOperator dataSourceDetailsOperatorInstance = new DataSourceDetailsOperator();
    private DataSourceDetailsInfo dataSourceDetailsInfo;
    private DataSourceDetailsOperator() { this.dataSourceDetailsInfo = null; }
    public static DataSourceDetailsOperator getInstance() { return dataSourceDetailsOperatorInstance; }
    public DataSourceDetailsInfo getDataSourceDetailsInfo() { return dataSourceDetailsInfo; }

    /**
     * Creates and populates details for a data source based on the provided DataSourceInfo object.
     *
     * Currently supported DataSourceProvider - Prometheus
     *
     * @param dataSourceInfo The DataSourceInfo object containing information about the data source.
     * TODO - support multiple data sources
     */
    public void createDataSourceDetails (DataSourceInfo dataSourceInfo) throws DataSourceDetailsInfoCreationException {

        DataSourceDetailsHelper dataSourceDetailsHelper = new DataSourceDetailsHelper();
        DataSourceOperatorImpl op = null;

        /**
         * Get PrometheusDataSourceOperatorImpl instance on runtime based on dataSource provider
         */
        if (dataSourceInfo.getProvider().equals(KruizeConstants.SupportedDatasources.PROMETHEUS)) {
            op = DataSourceOperatorImpl.getInstance().getOperator(dataSourceInfo.getProvider());
        }

        /**
         * For the "prometheus" data source, fetches and processes data related to namespaces, workloads, and containers,
         * creating a comprehensive DataSourceDetailsInfo object that is then added to a list.
         */
        try {
            JsonArray namespacesDataResultArray =  op.getResultArrayForQuery(dataSourceInfo.getUrl().toString(), PromQLDataSourceQueries.NAMESPACE_QUERY);

            if (op.validateResultArray(namespacesDataResultArray)) {
                //populate namespace data
                HashMap<String, DataSourceNamespace> datasourceNamespaces = dataSourceDetailsHelper.getActiveNamespaces(namespacesDataResultArray);
                dataSourceDetailsInfo = dataSourceDetailsHelper.createDataSourceDetailsInfoObject(dataSourceInfo.getProvider(), datasourceNamespaces);

                //populate workload data
                HashMap<String, DataSourceWorkload> datasourceWorkloads = new HashMap<>();
                for (String key : datasourceNamespaces.keySet()) {
                    String namespace = key;
                    /*
                    TODO -  get workload metadata for a given namespace
                     */
                    JsonArray workloadDataResultArray = op.getResultArrayForQuery(dataSourceInfo.getUrl().toString(), PromQLDataSourceQueries.WORKLOAD_QUERY);

                    if (op.validateResultArray(workloadDataResultArray)) {
                        datasourceWorkloads = dataSourceDetailsHelper.getWorkloadInfo(workloadDataResultArray);
                    }
                }
                //dataSourceDetailsHelper.updateWorkloadDataSourceDetailsInfoObject(datasourceNamespaces, datasourceWorkloads);

                //populate container data
                HashMap<String, DataSourceContainer> datasourceContainers = new HashMap<>();

                for (String key : datasourceWorkloads.keySet()) {
                    String workloadName = key;
                    /*
                    TODO - get container metadata for a given workload
                     */
                    JsonArray containerDataResultArray = op.getResultArrayForQuery(dataSourceInfo.getUrl().toString(), PromQLDataSourceQueries.CONTAINER_QUERY);

                    if (op.validateResultArray(containerDataResultArray)) {
                        datasourceContainers = dataSourceDetailsHelper.getContainerInfo(containerDataResultArray);
                    }

                }
                //dataSourceDetailsHelper.updateContainerDataSourceDetailsInfoObject(datasourceWorkloads, datasourceContainers);
            }
        } catch (DataSourceDetailsInfoCreationException e) {
            throw new DataSourceDetailsInfoCreationException(e.getMessage());
        }
    }

    /**
     * Retrieves details for the specified data source information.
     *
     * @param dataSourceInfo The information about the data source to retrieve details for.
     * @return DataSourceDetailsInfo containing details about the data source if found, otherwise null.
     */
    public DataSourceDetailsInfo getDataSourceDetails(DataSourceInfo dataSourceInfo) {

        if (dataSourceDetailsInfo != null) {
            return dataSourceDetailsInfo;
        }
        return null;
    }

    /*
    TODO - Implement methods to support update and delete operations for periodic update of DataSourceDetailsInfo
    public DataSourceDetailsInfo updateDataSourceDetails(DataSourceInfo dataSource) {

    }

    public void deleteDataSourceDetails(DataSourceInfo dataSource) {

    }

     */
}
