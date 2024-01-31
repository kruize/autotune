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
    public void importDataFromDataSource(DataSourceInfo dataSource) {

        try {
            if (null != dataSource) {
                dataSourceDetailsOperator.createDataSourceDetails(dataSource);
            } else {
                throw new DataSourceNotExist(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.MISSING_DATASOURCE_INFO);
            }
        } catch (DataSourceNotExist e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Retrieves data from the specified data source information.
     *
     * @param dataSource The information about the data source to retrieve data from.
     * @return DataSourceDetailsInfo containing details about the data source, or null if not found.
     * @throws DataSourceNotExist Thrown when the provided data source information is null.
     */
    public DataSourceDetailsInfo getDataFromDataSource(DataSourceInfo dataSource) {

        try {
            if (null != dataSource) {
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

    public DataSourceDetailsInfo updateDataFromDataSource(DataSourceInfo dataSource) {

        try {
            if (null != dataSource) {
                return dataSourceDetailsOperator.updateDataSourceDetails(dataSource);
            } else {
                throw new DataSourceNotExist(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.MISSING_DATASOURCE_INFO);
            }
        } catch (DataSourceNotExist e) {
            LOGGER.error(e.getMessage());
        }
        return null;
    }
    public void deleteDataFromDataSource(DataSourceInfo dataSource) {

        try {
            if (null != dataSource) {
                dataSourceDetailsOperator.deleteDataSourceDetails(dataSource);
            } else {
                throw new DataSourceNotExist(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.MISSING_DATASOURCE_INFO);
            }
        } catch (DataSourceNotExist e) {
            LOGGER.error(e.getMessage());
        }
    }

     */
}
