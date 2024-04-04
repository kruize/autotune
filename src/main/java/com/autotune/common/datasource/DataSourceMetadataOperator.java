package com.autotune.common.datasource;

import com.autotune.common.data.dataSourceQueries.PromQLDataSourceQueries;
import com.autotune.common.utils.DataSourceMetadataHelper;
import com.autotune.common.data.dataSourceMetadata.*;
import com.autotune.utils.KruizeConstants;
import com.google.gson.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * DataSourceMetadataOperator is an abstraction with CRUD operations to manage DataSourceMetadataInfo Object
 * representing JSON for a given data source
 *
 *  TODO -
 *  object is currently stored in memory going forward need to store cluster metadata in Kruize DB
 *  Implement methods to support update and delete operations for periodic update of DataSourceMetadataInfo
 */
public class DataSourceMetadataOperator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceMetadataOperator.class);
    private static final DataSourceMetadataOperator dataSourceMetadataOperatorInstance = new DataSourceMetadataOperator();
    private DataSourceMetadataInfo dataSourceMetadataInfo;
    private DataSourceMetadataOperator() { this.dataSourceMetadataInfo = null; }
    public static DataSourceMetadataOperator getInstance() { return dataSourceMetadataOperatorInstance; }

    /**
     * Creates and populates metadata for a data source based on the provided DataSourceInfo object.
     *
     * Currently supported DataSourceProvider - Prometheus
     *
     * @param dataSourceInfo The DataSourceInfo object containing information about the data source.
     * TODO - support multiple data sources
     */
    public void createDataSourceMetadata(DataSourceInfo dataSourceInfo) {
        DataSourceMetadataHelper dataSourceDetailsHelper = new DataSourceMetadataHelper();
        /**
         * Get DataSourceOperatorImpl instance on runtime based on dataSource provider
         */
        DataSourceOperatorImpl op = DataSourceOperatorImpl.getInstance().getOperator(dataSourceInfo.getProvider());

        if (null == op) {
            return;
        }

        /**
         * For the "prometheus" data source, fetches and processes data related to namespaces, workloads, and containers,
         * creating a comprehensive DataSourceMetadataInfo object that is then added to a list.
         * TODO - Process cluster metadata using a custom query
         */
        try {
            JsonArray namespacesDataResultArray =  op.getResultArrayForQuery(dataSourceInfo.getUrl().toString(), PromQLDataSourceQueries.NAMESPACE_QUERY);
            if (false == op.validateResultArray(namespacesDataResultArray)){
                dataSourceMetadataInfo = dataSourceDetailsHelper.createDataSourceMetadataInfoObject(dataSourceInfo.getName(), null);
                return;
            }

            /**
             * Key: Name of namespace
             * Value: DataSourceNamespace object corresponding to a namespace
             */
            HashMap<String, DataSourceNamespace> datasourceNamespaces = dataSourceDetailsHelper.getActiveNamespaces(namespacesDataResultArray);
            dataSourceMetadataInfo = dataSourceDetailsHelper.createDataSourceMetadataInfoObject(dataSourceInfo.getName(), datasourceNamespaces);

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
            dataSourceDetailsHelper.updateWorkloadDataSourceMetadataInfoObject(dataSourceInfo.getName(), dataSourceMetadataInfo, datasourceWorkloads);

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
            dataSourceDetailsHelper.updateContainerDataSourceMetadataInfoObject(dataSourceInfo.getName(), dataSourceMetadataInfo, datasourceWorkloads, datasourceContainers);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Retrieves DataSourceMetadataInfo object.
     * @return DataSourceMetadataInfo containing metadata about the data source if found, otherwise null.
     */
    public DataSourceMetadataInfo getDataSourceMetadataInfo(DataSourceInfo dataSourceInfo) {
        try {
            if (null == dataSourceMetadataInfo) {
                LOGGER.debug(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.DATASOURCE_METADATA_INFO_NOT_AVAILABLE);
                return null;
            }
            String dataSourceName = dataSourceInfo.getName();
            HashMap<String, DataSource> dataSourceHashMap = dataSourceMetadataInfo.getDataSourceHashMap();

            if (null == dataSourceHashMap || !dataSourceHashMap.containsKey(dataSourceName)) {
                LOGGER.debug(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.DATASOURCE_METADATA_DATASOURCE_NOT_AVAILABLE + dataSourceName);
                return null;
            }

            DataSource targetDataSource = dataSourceHashMap.get(dataSourceName);
            HashMap<String, DataSource> targetDataSourceHashMap = new HashMap<>();
            targetDataSourceHashMap.put(dataSourceName, targetDataSource);
            return new DataSourceMetadataInfo(targetDataSourceHashMap);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return null;
        }
    }

}
