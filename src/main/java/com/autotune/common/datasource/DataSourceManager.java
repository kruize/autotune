package com.autotune.common.datasource;

import com.autotune.common.data.dataSourceDetails.DataSourceDetailsInfo;
import com.autotune.common.exceptions.DataSourceNotExist;
import com.autotune.utils.KruizeConstants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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
    List<DataSourceDetailsInfo> dataSourceDetailsInfoList = new ArrayList<>();
    HashMap<String, DataSourceInfo> dataSources = DataSourceCollection.getInstance().getDataSourcesCollection();

    /**
     * Imports Data for each data source using associated DataSourceInfo and DataSourceDetailsOperator.
     */
    public void importDataFromAllDataSources() {

        for(String name : dataSources.keySet()) {
            DataSourceInfo dataSource = dataSources.get(name);
            dataSourceDetailsOperator.createDataSourceDetails(dataSource);
            dataSourceDetailsInfoList.add(dataSourceDetailsOperator.getDataSourceDetailsInfo());
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(dataSourceDetailsInfoList);

        LOGGER.info(jsonOutput);
    }

    /**
     * Imports Data for a specific data source using associated DataSourceInfo.
     */
    public DataSourceDetailsInfo importDataFromDataSource(DataSourceInfo dataSource) {

        try {
            if (null != dataSource) {
                dataSourceDetailsOperator.createDataSourceDetails(dataSource);
                return dataSourceDetailsOperator.getDataSourceDetails(dataSource);
            } else {
                throw new DataSourceNotExist(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.MISSING_DATASOURCE_INFO);
            }
        } catch (DataSourceNotExist e) {
            LOGGER.error(e.getMessage());
        }
        return null;
    }

    /*
    TODO - Implement update and delete functionalities
    private static updateDataFromDataSource() {

        for(String name : dataSources.keySet()) {
            DataSourceInfo dataSource = dataSources.get(name);
            dataSourceDetailsOperator.updateDataSourceDetails(dataSource);

        }
    }
    private static deleteDataFromDataSource() {

        for(String name : dataSources.keySet()) {
            DataSourceInfo dataSource = dataSources.get(name);
            dataSourceDetailsOperator.deleteDataSourceDetails(dataSource);

        }
    }

     */


}
