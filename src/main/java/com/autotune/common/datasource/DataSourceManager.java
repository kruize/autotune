package com.autotune.common.datasource;

import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.exceptions.datasource.DataSourceDoesNotExist;
import com.autotune.common.data.dataSourceMetadata.*;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.utils.KruizeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DataSourceManager is an interface to manage (create and update) metadata
 * of data sources
 *
 *
 * Currently Supported Implementations:
 *  - importMetadataFromDataSource
 *  - getMetadataFromDataSource
 *  TODO - DB integration for update and delete functionalities
 */
public class DataSourceManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceManager.class);
    DataSourceMetadataOperator dataSourceMetadataOperator = DataSourceMetadataOperator.getInstance();

    public DataSourceManager() {
    }

    /**
     * Imports Metadata for a specific data source using associated DataSourceInfo.
     */
    public void importMetadataFromDataSource(DataSourceInfo dataSourceInfo) {
        try {
            if (null == dataSourceInfo) {
                throw new DataSourceDoesNotExist(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.MISSING_DATASOURCE_INFO);
            }
            String dataSourceName = dataSourceInfo.getName();
            if(checkIfDataSourceMetadataExists(dataSourceName)) {
                return;
            }
            dataSourceMetadataOperator.createDataSourceMetadata(dataSourceInfo);
            // save the metadata to DB
            saveMetadataFromDataSourceToDB(dataSourceInfo);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
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
        try {
            if (null == dataSource) {
                throw new DataSourceDoesNotExist(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.MISSING_DATASOURCE_INFO);
            }
            String dataSourceName = dataSource.getName();
            DataSourceMetadataInfo dataSourceMetadataInfo = new ExperimentDBService().loadMetadataFromDBByName(dataSourceName, "true");
            if (null == dataSourceMetadataInfo) {
                LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.DATASOURCE_METADATA_INFO_NOT_AVAILABLE, "for datasource {}" + dataSourceName);
                return null;
            }
            return dataSourceMetadataInfo;
        } catch (DataSourceDoesNotExist e) {
            LOGGER.error(e.getMessage());
        }catch (Exception e) {
            LOGGER.error("Loading saved datasource metadata failed: {} ", e.getMessage());
        }
        return null;
    }

    /**
     * Updates metadata of the specified data source and metadata object
     * @param dataSource The information about the data source to be updated.
     * @param dataSourceMetadataInfo The existing DataSourceMetadataInfo object containing the current
     *                             metadata information of the data source.
     */
    public void updateMetadataFromDataSource(DataSourceInfo dataSource, DataSourceMetadataInfo dataSourceMetadataInfo) {
        try {
            if (null == dataSource) {
                throw new DataSourceDoesNotExist(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.MISSING_DATASOURCE_INFO);
            }
            if (null == dataSourceMetadataInfo) {
                throw new DataSourceDoesNotExist(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.DATASOURCE_METADATA_INFO_NOT_AVAILABLE);
            }
            dataSourceMetadataOperator.updateDataSourceMetadata(dataSource, dataSourceMetadataInfo);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Deletes metadata of the specified data source
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
     * retrieves and adds Metadata object from the specified data source
     * @param dataSourceInfo DataSourceInfo object
     */
    public void saveMetadataFromDataSourceToDB(DataSourceInfo dataSourceInfo) {
        try {
            DataSourceMetadataInfo dataSourceMetadataInfo = dataSourceMetadataOperator.getDataSourceMetadataInfo(dataSourceInfo);
            if (null == dataSourceMetadataInfo) {
                LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.DATASOURCE_METADATA_INFO_NOT_AVAILABLE, "for datasource {}" + dataSourceInfo.getName());
                return;
            }
            // add the metadata to DB
            addMetadataToDB(dataSourceMetadataInfo);

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Adds Metadata object to DB
     * @param dataSourceMetadataInfo DataSourceMetadataInfo object
     */
    public void addMetadataToDB(DataSourceMetadataInfo dataSourceMetadataInfo) {
        ValidationOutputData addedToDB = null;
        try {
            // add the data source to DB
            addedToDB = new ExperimentDBService().addMetadataToDB(dataSourceMetadataInfo);
            if (addedToDB.isSuccess()) {
                LOGGER.debug("Metadata added to the DB successfully.");
            } else {
                LOGGER.error("Failed to add metadata to DB: {}", addedToDB.getMessage());
            }
        } catch (Exception e) {
            LOGGER.error("Exception occurred while adding metadata : {} ", e.getMessage());
        }

    }

    private boolean checkIfDataSourceMetadataExists(String dataSourceName) {
        boolean isPresent = false;
        try {
            DataSourceMetadataInfo dataSourceMetadataInfo = new ExperimentDBService().loadMetadataFromDBByName(dataSourceName,"false");
            if (null != dataSourceMetadataInfo) {
                LOGGER.error("Metadata already exists for datasource: {}!", dataSourceName);
                isPresent = true;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load metadata for the datasource: {}: {} ", dataSourceName, e.getMessage());
        }
        return isPresent;
    }
}
