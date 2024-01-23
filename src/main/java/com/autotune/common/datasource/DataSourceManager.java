package com.autotune.common.datasource;

import com.autotune.common.data.dataSourceDetails.DataSourceDetailsInfo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.List;

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
    DataSourceDetailsOperator dataSourceDetailsOperator = DataSourceDetailsOperator.getInstance();
    List<DataSourceDetailsInfo> dataSourceDetailsInfoList = DataSourceDetailsOperator.getInstance().getDataSourceDetailsInfoList();
    HashMap<String, DataSourceInfo> dataSources = DataSourceCollection.getInstance().getDataSourcesCollection();

    /**
     * Creates experiments for each data source using associated DataSourceInfo and DataSourceDetailsOperator.
     */
    public void createExperimentsFromDataSource() {

        for(String name : dataSources.keySet()) {
            DataSourceInfo dataSource = dataSources.get(name);
            dataSourceDetailsOperator.createDataSourceDetails(dataSource);

        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(dataSourceDetailsInfoList);

        LOGGER.info(jsonOutput);
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
