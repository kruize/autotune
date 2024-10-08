package com.autotune.common.datasource;

import com.autotune.common.data.dataSourceQueries.PromQLDataSourceQueries;
import com.autotune.common.data.dataSourceMetadata.DataSourceMetadataHelper;
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
    public DataSourceMetadataInfo createDataSourceMetadata(DataSourceInfo dataSourceInfo) {
        return processQueriesAndPopulateDataSourceMetadataInfo(dataSourceInfo);
    }

    /**
     * Retrieves DataSourceMetadataInfo object.
     * @return DataSourceMetadataInfo containing metadata about the data source if found, otherwise null.
     */
    public DataSourceMetadataInfo getDataSourceMetadataInfo(DataSourceInfo dataSourceInfo) {
        try {
            if (null == dataSourceMetadataInfo) {
                LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.DATASOURCE_METADATA_INFO_NOT_AVAILABLE);
                return null;
            }
            String dataSourceName = dataSourceInfo.getName();
            HashMap<String, DataSource> dataSourceHashMap = dataSourceMetadataInfo.getDataSourceHashMap();

            if (null == dataSourceHashMap || !dataSourceHashMap.containsKey(dataSourceName)) {
                LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.DATASOURCE_METADATA_DATASOURCE_NOT_AVAILABLE + "{}", dataSourceName);
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

    /**
     * Updates the metadata information of a data source with the provided DataSourceInfo object,
     * while preserving existing metadata information.
     *
     * @param dataSourceInfo      The DataSourceInfo object containing information about the
     *                            data source to be updated.
     *
     *  TODO - Currently Create and Update functions have identical functionalities, based on UI workflow and requirements
     *         need to further enhance updateDataSourceMetadata() to support namespace, workload level granular updates
     */
    public DataSourceMetadataInfo updateDataSourceMetadata(DataSourceInfo dataSourceInfo) {
        return processQueriesAndPopulateDataSourceMetadataInfo(dataSourceInfo);
    }

    /**
     * Deletes the metadata information of a data source with the provided DataSourceInfo object,
     * @param dataSourceInfo      The DataSourceInfo object containing information about the
     *                            metadata to be deleted.
     */
    public void deleteDataSourceMetadata(DataSourceInfo dataSourceInfo) {
        try{
            if (null == dataSourceMetadataInfo) {
                LOGGER.debug(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.DATASOURCE_METADATA_INFO_NOT_AVAILABLE);
                return;
            }
            String dataSourceName = dataSourceInfo.getName();
            HashMap<String, DataSource> dataSourceHashMap = dataSourceMetadataInfo.getDataSourceHashMap();

            if (null == dataSourceHashMap || !dataSourceHashMap.containsKey(dataSourceName)) {
                LOGGER.debug(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.DATASOURCE_METADATA_DATASOURCE_NOT_AVAILABLE + "{}", dataSourceName);
            }

            dataSourceHashMap.remove(dataSourceName);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Fetches and processes metadata related to namespaces, workloads, and containers of a given datasource and populates the
     * DataSourceMetadataInfo object
     *
     * @param dataSourceInfo            The DataSourceInfo object containing information about the data source
     * @return DataSourceMetadataInfo object with populated metadata fields
     */
    public DataSourceMetadataInfo processQueriesAndPopulateDataSourceMetadataInfo(DataSourceInfo dataSourceInfo) {
        DataSourceMetadataHelper dataSourceDetailsHelper = new DataSourceMetadataHelper();
        /**
         * Get DataSourceOperatorImpl instance on runtime based on dataSource provider
         */
        DataSourceOperatorImpl op = DataSourceOperatorImpl.getInstance().getOperator(dataSourceInfo.getProvider());

        if (null == op) {
            LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.DATASOURCE_OPERATOR_RETRIEVAL_FAILURE, dataSourceInfo.getProvider());
            return null;
        }

        /**
         * For the "prometheus" data source, fetches and processes data related to namespaces, workloads, and containers,
         * creating a comprehensive DataSourceMetadataInfo object that is then added to a list.
         * TODO - Process cluster metadata using a custom query
         */
        try {
            String dataSourceName = dataSourceInfo.getName();
            JsonArray namespacesDataResultArray =  op.getResultArrayForQuery(dataSourceInfo, PromQLDataSourceQueries.NAMESPACE_QUERY);
            if (false == op.validateResultArray(namespacesDataResultArray)){
                dataSourceMetadataInfo = dataSourceDetailsHelper.createDataSourceMetadataInfoObject(dataSourceName, null);
                throw new Exception(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.NAMESPACE_QUERY_VALIDATION_FAILED);
            }

            /**
             * Key: Name of namespace
             * Value: DataSourceNamespace object corresponding to a namespace
             */
            HashMap<String, DataSourceNamespace> datasourceNamespaces = dataSourceDetailsHelper.getActiveNamespaces(namespacesDataResultArray);
            dataSourceMetadataInfo = dataSourceDetailsHelper.createDataSourceMetadataInfoObject(dataSourceName, datasourceNamespaces);

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
            JsonArray workloadDataResultArray = op.getResultArrayForQuery(dataSourceInfo,
                    PromQLDataSourceQueries.WORKLOAD_QUERY);

            if (op.validateResultArray(workloadDataResultArray)) {
                datasourceWorkloads = dataSourceDetailsHelper.getWorkloadInfo(workloadDataResultArray);
            }
            dataSourceDetailsHelper.updateWorkloadDataSourceMetadataInfoObject(dataSourceName, dataSourceMetadataInfo,
                    datasourceWorkloads);

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
            JsonArray containerDataResultArray = op.getResultArrayForQuery(dataSourceInfo,
                    PromQLDataSourceQueries.CONTAINER_QUERY);

            if (op.validateResultArray(containerDataResultArray)) {
                datasourceContainers = dataSourceDetailsHelper.getContainerInfo(containerDataResultArray);
            }
            dataSourceDetailsHelper.updateContainerDataSourceMetadataInfoObject(dataSourceName, dataSourceMetadataInfo,
                    datasourceWorkloads, datasourceContainers);

            return getDataSourceMetadataInfo(dataSourceInfo);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        return null;
    }
}
