/*******************************************************************************
 * Copyright (c) 2020, 2021 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.autotune.common.datasource;

import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.data.dataSourceMetadata.DataSource;
import com.autotune.common.data.dataSourceMetadata.DataSourceCluster;
import com.autotune.common.data.dataSourceMetadata.DataSourceMetadataInfo;
import com.autotune.common.exceptions.datasource.DataSourceDoesNotExist;
import com.autotune.database.dao.ExperimentDAOImpl;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.MetricsConfig;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import static com.autotune.utils.KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.*;
import static com.autotune.utils.KruizeConstants.DataSourceConstants.DataSourceMetadataSuccessMsgs.METADATA_ADDED;

/**
 * DataSourceManager is an interface to manage (create and update) metadata
 * of data sources
 * <p>
 * <p>
 * Currently Supported Implementations:
 * - importMetadataFromDataSource
 * - getMetadataFromDataSource
 *  TODO - DB integration for update and delete functionalities
 */
public class DataSourceManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceManager.class);
    DataSourceMetadataOperator dataSourceMetadataOperator = DataSourceMetadataOperator.getInstance();

    public DataSourceManager() {
    }

    /**
     * Imports Metadata for a specific data source using associated DataSourceInfo.
     *
     * @param dataSourceInfo
     * @param uniqueKey        this is used as labels in query example container="xyz" namespace="abc"
     * @param startTime        Get metadata from starttime to endtime
     * @param endTime          Get metadata from starttime to endtime
     * @param steps            the interval between data points in a range query
     * @param includeResources
     * @param excludeResources
     * @return
     */
    public DataSourceMetadataInfo importMetadataFromDataSource(DataSourceInfo dataSourceInfo, String uniqueKey, long startTime, long endTime, int steps, Map<String, String> includeResources,
                                                               Map<String, String> excludeResources) throws DataSourceDoesNotExist, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        String statusValue = "failure";
        io.micrometer.core.instrument.Timer.Sample timerImportMetadata = Timer.start(MetricsConfig.meterRegistry());
        try {
            if (null == dataSourceInfo) {
                throw new DataSourceDoesNotExist(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.MISSING_DATASOURCE_INFO);
            }
            DataSourceMetadataInfo dataSourceMetadataInfo = dataSourceMetadataOperator.createDataSourceMetadata(dataSourceInfo, uniqueKey, startTime, endTime, steps, includeResources, excludeResources);
            if (null == dataSourceMetadataInfo) {
                LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.DATASOURCE_METADATA_INFO_NOT_AVAILABLE, "for datasource {}" + dataSourceInfo.getName());
                return null;
            }
            statusValue = "success";
            return dataSourceMetadataInfo;
        } finally {
            if (null != timerImportMetadata) {
                MetricsConfig.timerImportMetadata = MetricsConfig.timerBImportMetadata.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerImportMetadata.stop(MetricsConfig.timerImportMetadata);
            }
        }
    }

    /**
     * Retrieves Metadata from the specified data source information.
     *
     * @param dataSource The information about the data source to retrieve data from.
     * @return DataSourceMetadataInfo containing details about the data source, or null if not found.
     * @throws DataSourceDoesNotExist Thrown when the provided data source information is null.
     */
    public DataSourceMetadataInfo getMetadataFromDataSource(DataSourceInfo dataSource) {
        String statusValue = "failure";
        io.micrometer.core.instrument.Timer.Sample timerGetMetadata = Timer.start(MetricsConfig.meterRegistry());
        try {
            if (null == dataSource) {
                throw new DataSourceDoesNotExist(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.MISSING_DATASOURCE_INFO);
            }
            String dataSourceName = dataSource.getName();
            DataSourceMetadataInfo dataSourceMetadataInfo = dataSourceMetadataOperator.getDataSourceMetadataInfo(dataSource);
            if (null == dataSourceMetadataInfo) {
                LOGGER.error(DATASOURCE_METADATA_INFO_NOT_AVAILABLE, "for datasource {}" + dataSourceName);
                return null;
            }
            statusValue = "success";
            return dataSourceMetadataInfo;
        } catch (DataSourceDoesNotExist e) {
            LOGGER.error(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Loading saved datasource metadata failed: {} ", e.getMessage());
        } finally {
            if (null != timerGetMetadata) {
                MetricsConfig.timerGetMetadata = MetricsConfig.timerBGetMetadata.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerGetMetadata.stop(MetricsConfig.timerGetMetadata);
            }
        }
        return null;
    }

    /**
     * Updates metadata of the specified data source and metadata object
     *
     * @param dataSource             The information about the data source to be updated.
     * @param dataSourceMetadataInfo The existing DataSourceMetadataInfo object containing the current
     *                               metadata information of the data source.
     */
    public void updateMetadataFromDataSource(DataSourceInfo dataSource, DataSourceMetadataInfo dataSourceMetadataInfo) {
        try {
            if (null == dataSource) {
                throw new DataSourceDoesNotExist(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.MISSING_DATASOURCE_INFO);
            }
            if (null == dataSourceMetadataInfo) {
                throw new DataSourceDoesNotExist(DATASOURCE_METADATA_INFO_NOT_AVAILABLE);
            }
            dataSourceMetadataOperator.updateDataSourceMetadata(dataSource, "", 0, 0, 0, null, null);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Deletes metadata of the specified data source
     *
     * @param dataSource The metadata associated with the specified data source to be deleted.
     */
    public void deleteMetadataFromDataSource(DataSourceInfo dataSource) {

        try {
            if (null == dataSource) {
                throw new DataSourceDoesNotExist(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.MISSING_DATASOURCE_INFO);
            }
            dataSourceMetadataOperator.deleteDataSourceMetadata(dataSource);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Adds Metadata object to DB
     *
     * @param dataSourceMetadataInfo DataSourceMetadataInfo object
     *                               Note - It's assumed that metadata will be added to database after validating dataSourceMetadataInfo object
     */
    public void addMetadataToDB(DataSourceMetadataInfo dataSourceMetadataInfo) {
        ValidationOutputData addedToDB = null;
        try {
            // add the data source to DB
            addedToDB = new ExperimentDBService().addMetadataToDB(dataSourceMetadataInfo);
            if (addedToDB.isSuccess()) {
                LOGGER.debug(METADATA_ADDED);
            } else {
                LOGGER.error(LOAD_DATASOURCE_METADATA_TO_DB_ERROR, addedToDB.getMessage());
            }
        } catch (Exception e) {
            LOGGER.error("Exception occurred while adding metadata : {} ", e.getMessage());
        }

    }

    private boolean checkIfDataSourceMetadataExists(String dataSourceName) {
        boolean isPresent = false;
        try {
            DataSourceMetadataInfo dataSourceMetadataInfo = new ExperimentDBService().loadMetadataFromDBByName(dataSourceName, "false");
            if (null != dataSourceMetadataInfo) {
                LOGGER.error(METADATA_EXIST, dataSourceName);
                isPresent = true;
            }
        } catch (Exception e) {
            LOGGER.error(METADATA_LOAD_FROM_DB, dataSourceName, e.getMessage());
        }
        return isPresent;
    }

    /**
     * Fetches and deletes DataSourceMetadata of the specified datasource from Database
     *
     * @param dataSourceInfo DataSourceInfo object
     */
    public void deleteMetadataFromDBByDataSource(DataSourceInfo dataSourceInfo) {
        try {
            if (null == dataSourceInfo) {
                throw new DataSourceDoesNotExist(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.MISSING_DATASOURCE_INFO);
            }
            String dataSourceName = dataSourceInfo.getName();
            DataSourceMetadataInfo dataSourceMetadataInfo = fetchDataSourceMetadataFromDBByName(dataSourceName, "false");
            if (null == dataSourceMetadataInfo) {
                LOGGER.debug(String.format(AnalyzerErrorConstants.APIErrors.DSMetadataAPI.DATASOURCE_METADATA_DELETE_ERROR_MSG, dataSourceName));
                return;
            }
            // delete metadata from DB
            deleteMetadataFromDB(dataSourceName);

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Deletes DataSourceMetadata entry from Database
     *
     * @param dataSourceName datasource name
     */
    public void deleteMetadataFromDB(String dataSourceName) {
        ValidationOutputData deletedFromDB = null;
        try {
            // add the data source to DB
            deletedFromDB = new ExperimentDAOImpl().deleteKruizeDSMetadataEntryByName(dataSourceName);
            if (deletedFromDB.isSuccess()) {
                LOGGER.debug("Metadata deleted successfully from the DB.");
            } else {
                LOGGER.error("Failed to delete metadata from DB: {}", deletedFromDB.getMessage());
            }
        } catch (Exception e) {
            LOGGER.error("Exception occurred while deleting metadata : {} ", e.getMessage());
        }

    }

    /**
     * Fetches Datasource details from Database by name
     *
     * @param dataSourceName Name of the datasource to be fetched
     * @return DataSourceInfo object of the specified datasource name
     */
    public DataSourceInfo fetchDataSourceFromDBByName(String dataSourceName) {
        try {
            if (null == dataSourceName || dataSourceName.isEmpty()) {
                throw new Exception(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.MISSING_DATASOURCE_NAME);
            }
            DataSourceInfo datasource = new ExperimentDBService().loadDataSourceFromDBByName(dataSourceName);
            return datasource;
        } catch (Exception e) {
            LOGGER.error(String.format(LOAD_DATASOURCE_FROM_DB_ERROR, dataSourceName, e.getMessage()));
        }
        return null;
    }

    /**
     * Fetches Datasource metadata details from Database by name
     *
     * @param dataSourceName Name of the datasource to be fetched
     * @param verbose        Flag indicating granularity of metadata to be fetched
     * @return DataSourceMetadataInfo object of the specified datasource name
     */
    public DataSourceMetadataInfo fetchDataSourceMetadataFromDBByName(String dataSourceName, String verbose) {
        try {
            if (null == dataSourceName || dataSourceName.isEmpty()) {
                throw new Exception(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.MISSING_DATASOURCE_NAME);
            }
            DataSourceMetadataInfo metadataInfo = new ExperimentDBService().loadMetadataFromDBByName(dataSourceName, verbose);
            return metadataInfo;
        } catch (Exception e) {
            LOGGER.error(String.format(LOAD_DATASOURCE_METADATA_FROM_DB_ERROR, dataSourceName, e.getMessage()));
        }
        return null;
    }

    /**
     * Filters the given metadata object to retain only the cluster details.
     * This method processes the provided metadata includes only the datasource
     * names and their associated cluster names, pruning all other details.
     *
     * @param dataSourceName         Datasource name
     * @param dataSourceMetadataInfo DataSourceMetadataInfo object containing granular metadata
     * @return A new DataSourceMetadataInfo object containing only the cluster details.
     * <p>
     * Note - It's assumed that Cluster view will be requested after validating dataSourceMetadataInfo object
     */
    public DataSourceMetadataInfo DataSourceMetadataClusterView(String dataSourceName, DataSourceMetadataInfo dataSourceMetadataInfo) {
        try {
            HashMap<String, DataSource> filteredDataSourceHashMap = new HashMap<>();

            DataSource dataSource = dataSourceMetadataInfo.getDataSourceHashMap().get(dataSourceName);

            HashMap<String, DataSourceCluster> filteredClusterHashMap = new HashMap<>();

            for (Map.Entry<String, DataSourceCluster> clusterEntry : dataSource.getDataSourceClusterHashMap().entrySet()) {
                String clusterName = clusterEntry.getKey();
                DataSourceCluster cluster = clusterEntry.getValue();

                // Create a new DataSourceCluster object with only the cluster name
                DataSourceCluster filteredCluster = new DataSourceCluster(cluster.getDataSourceClusterName(), null);
                filteredClusterHashMap.put(clusterName, filteredCluster);
            }

            // Create a new DataSource object with filtered clusters
            DataSource filteredDataSource = new DataSource(dataSource.getDataSourceName(), filteredClusterHashMap);
            filteredDataSourceHashMap.put(dataSourceName, filteredDataSource);

            return new DataSourceMetadataInfo(filteredDataSourceHashMap);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return null;
        }
    }
}
