package com.autotune.common.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /*
    TODO - Implement methods to support update and delete operations for periodic update of DataSourceMetadataInfo
    public DataSourceMetadataInfo updateDataSourceMetadata(DataSourceInfo dataSource) {

    }

    public void deleteDataSourceMetadata(DataSourceInfo dataSource) {

    }

     */
}
