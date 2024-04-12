package com.autotune.common.datasource;

import com.autotune.common.exceptions.datasource.KruizeDatasourceDoesNotExist;
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
 *  TODO - Implement update and delete functionalities
 */
public class DataSourceManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceManager.class);
    DataSourceMetadataOperator dataSourceMetadataOperator = DataSourceMetadataOperator.getInstance();

    public DataSourceManager() {
    }

    /**
     * Imports Metadata for a specific data source using associated DataSourceInfo.
     */
    public void importMetadataFromDataSource(DataSourceInfo dataSource) {
        try {
            if (null == dataSource) {
                throw new KruizeDatasourceDoesNotExist(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.MISSING_DATASOURCE_INFO);
            }
            dataSourceMetadataOperator.createDataSourceMetadata(dataSource);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Retrieves Metadata from the specified data source information.
     *
     * @param dataSource The information about the data source to retrieve data from.
     * @return DataSourceMetadataInfo containing details about the data source, or null if not found.
     * @throws KruizeDatasourceDoesNotExist Thrown when the provided data source information is null.
     */
    public void getMetadataFromDataSource(DataSourceInfo dataSource) {
        try {
            if (null == dataSource) {
                throw new KruizeDatasourceDoesNotExist(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.MISSING_DATASOURCE_INFO);
            }
            dataSourceMetadataOperator.getDataSourceMetadataInfo(dataSource);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

}
