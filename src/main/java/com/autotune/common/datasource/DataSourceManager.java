package com.autotune.common.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;

/**
 * DataSourceManager is an interface to manage (create and update) experiments
 * from collection of data sources
 *
 *
 * Currently Supported Implementations:
 *  - createExperimentsFromDataSource
 */

public class DataSourceManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceManager.class);
    DataSourceDetailsOperator dataSourceDetailsOperator = new DataSourceDetailsOperator();
    HashMap<String, DataSourceInfo> dataSources = DataSourceCollection.getInstance().getDataSourcesCollection();

    /**
     * Creates experiments for each data source using associated DataSourceInfo and DataSourceDetailsOperator.
     */
    public void createExperimentsFromDataSource() {

        for(String name : dataSources.keySet()) {
            DataSourceInfo dataSource = dataSources.get(name);
            dataSourceDetailsOperator.createDataSourceDetails(dataSource);

        }
    }

    /*
    TODO
    private static updateExperimentsFromDS() {

        for(String name : dataSources.keySet()) {
            DataSourceInfo dataSource = dataSources.get(name);
            dataSourceDetailsOperator.updateDataSourceDetails(dataSource);

        }
    }
     */


}
