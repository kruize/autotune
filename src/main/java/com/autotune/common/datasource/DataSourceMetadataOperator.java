package com.autotune.common.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private DataSourceMetadataOperator() {
    }
    public static DataSourceMetadataOperator getInstance() { return dataSourceMetadataOperatorInstance; }

    public void createDataSourceMetadata(DataSourceInfo dataSource) {
        //TODO - Implementation
    }
    public void getDataSourceMetadataInfo(DataSourceInfo dataSource) {
        //TODO - Implementation
    }

}
