/*******************************************************************************
 * Copyright (c) 2021, 2022 Red Hat, IBM Corporation and others.
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

import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.exceptions.*;
import com.autotune.common.utils.CommonUtils;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.utils.KruizeConstants;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * This DataSourceCollection object is used to store the
 * hashmap of dataSourceInfo objects containing all available data sources,
 * and it also manages the CRUD operations for datasources
 */
public class DataSourceCollection {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceCollection.class);
    private static DataSourceCollection dataSourceCollectionInstance = new DataSourceCollection();
    private HashMap<String, DataSourceInfo> dataSourceCollection;

    private DataSourceCollection() {
        this.dataSourceCollection = new HashMap<>();
    }

    /**
     * Returns the instance of dataSourceCollection class
     * @return DataSourceCollection instance
     */
    public static DataSourceCollection getInstance() {
        return dataSourceCollectionInstance;
    }

    /**
     * Returns the hashmap of data sources
     * @return HashMap containing dataSourceInfo objects
     */
    public HashMap<String, DataSourceInfo> getDataSourcesCollection() {
        return dataSourceCollection;
    }

    /**
     * Adds datasource to collection
     * @param datasource DataSourceInfo object containing details of datasource
     */
    public void addDataSource(DataSourceInfo datasource) {
        final String name = datasource.getName();
        final String provider = datasource.getProvider();
        final String url = datasource.getUrl().toString();

        LOGGER.info(KruizeConstants.DataSourceConstants.DataSourceInfoMsgs.ADDING_DATASOURCE + name);

        try {
            if (dataSourceCollection.containsKey(name)) {
                throw new DataSourceAlreadyExist(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.DATASOURCE_ALREADY_EXIST);
            }

            if (provider.equalsIgnoreCase(KruizeConstants.SupportedDatasources.PROMETHEUS)) {
                LOGGER.info(KruizeConstants.DataSourceConstants.DataSourceInfoMsgs.VERIFYING_DATASOURCE_REACHABILITY + name);
                DataSourceOperatorImpl op = DataSourceOperatorImpl.getInstance().getOperator(KruizeConstants.SupportedDatasources.PROMETHEUS);
                if (op.isServiceable(url) == CommonUtils.DatasourceReachabilityStatus.REACHABLE) {
                    LOGGER.info(KruizeConstants.DataSourceConstants.DataSourceSuccessMsgs.DATASOURCE_SERVICEABLE);
                    dataSourceCollection.put(name, datasource);
                    LOGGER.info(KruizeConstants.DataSourceConstants.DataSourceSuccessMsgs.DATASOURCE_ADDED);
                } else {
                    throw new DataSourceNotServiceable(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.DATASOURCE_NOT_SERVICEABLE);
                }
            } else {
                throw new UnsupportedDataSourceProvider(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.UNSUPPORTED_DATASOURCE_PROVIDER);
            }
        } catch (UnsupportedDataSourceProvider e) {
            LOGGER.error(e.getMessage());
        } catch (DataSourceNotServiceable e) {
            LOGGER.error(e.getMessage());
        } catch (DataSourceAlreadyExist e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Loads the data sources available at installation time
     * @param configFileName name of the config file mounted
     */
    public void addDataSourcesFromConfigFile(String configFileName) {
        try {
            String configFile = System.getenv(configFileName);
            JSONObject configObject = null;
            ValidationOutputData addedToDB = null;

            InputStream is = new FileInputStream(configFile);
            String jsonTxt = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            configObject = new JSONObject(jsonTxt);
            JSONArray dataSourceArr = configObject.getJSONArray(KruizeConstants.DataSourceConstants.KRUIZE_DATASOURCE);

            for (Object dataSourceObj: dataSourceArr) {
                JSONObject dataSourceObject = (JSONObject) dataSourceObj;
                String name = dataSourceObject.getString(KruizeConstants.DataSourceConstants.DATASOURCE_NAME);
                // check the DB if the datasource already exists
                try {
                    DataSourceInfo dataSourceInfo = new ExperimentDBService().loadDataSourceFromDBByName(name);
                    if (null != dataSourceInfo) {
                        LOGGER.error("Datasource: {} already exists!", name);
                        continue;
                    }
                } catch (Exception e) {
                    LOGGER.error("Loading saved datasource {} failed: {} ", name, e.getMessage());
                }
                String provider = dataSourceObject.getString(KruizeConstants.DataSourceConstants.DATASOURCE_PROVIDER);
                String serviceName = dataSourceObject.getString(KruizeConstants.DataSourceConstants.DATASOURCE_SERVICE_NAME);
                String namespace = dataSourceObject.getString(KruizeConstants.DataSourceConstants.DATASOURCE_SERVICE_NAMESPACE);
                String dataSourceURL = dataSourceObject.getString(KruizeConstants.DataSourceConstants.DATASOURCE_URL);
                DataSourceInfo datasource;
                if (!validateInput(name, provider, serviceName, dataSourceURL, namespace)) {
                    continue;
                }
                if (dataSourceURL.isEmpty()) {
                    datasource = new DataSourceInfo(name, provider, serviceName, namespace, null);
                } else {
                    datasource = new DataSourceInfo(name, provider, serviceName, namespace, new URL(dataSourceURL));
                }
                // add the data source to DB
                addedToDB = new ExperimentDBService().addDataSourceToDB(datasource);
                if (addedToDB.isSuccess()) {
                    LOGGER.info("Datasource added to the DB successfully.");
                    // add to local map
                    addDataSource(datasource);
                } else {
                    LOGGER.error("Failed to add datasource to DB: {}", addedToDB.getMessage());
                }
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * validates the input parameters before creating dataSourceInfo objects
     * @param name String containing name of the datasource
     * @param provider String containing provider of the datasource
     * @param servicename String containing service name for the datasource
     * @param url String containing URL of the data source
     * @param namespace String containing namespace for the datasource service
     * @return boolean returns true if validation is successful otherwise return false
     */
    public boolean validateInput(String name, String provider, String servicename, String url, String namespace) {
        try {
            if (name.isEmpty()) {
                throw new DataSourceMissingRequiredFiled(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.MISSING_DATASOURCE_NAME);
            }
            if (provider.isEmpty()) {
                throw new DataSourceMissingRequiredFiled(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.MISSING_DATASOURCE_PROVIDER);
            }
            if (namespace.isEmpty() && !servicename.isEmpty()) {
                throw new DataSourceMissingRequiredFiled(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.MISSING_DATASOURCE_NAMESPACE);
            }
            if (servicename.isEmpty() && url.isEmpty()) {
                throw new DataSourceMissingRequiredFiled(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.MISSING_DATASOURCE_SERVICENAME_AND_URL);
            }
            if (!servicename.isEmpty() && !url.isEmpty()) {
                throw new DataSourceMissingRequiredFiled(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.DATASOURCE_URL_SERVICENAME_BOTH_SET);
            }
            return true;
        } catch (DataSourceMissingRequiredFiled e) {
            LOGGER.error(e.getMessage());
            return false;
        }
    }

    /**
     * deletes the datasource from the Hashmap
     * @param name String containing the name of the datasource to be deleted
     */
    public void deleteDataSource(String name) {
        try {
            if (name == null) {
                throw new DataSourceMissingRequiredFiled(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.MISSING_DATASOURCE_NAME);
            }
            if (dataSourceCollection.containsKey(name)) {
                dataSourceCollection.remove(name);
            } else {
                throw new DataSourceNotExist(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.DATASOURCE_NOT_EXIST);
            }
        } catch (DataSourceMissingRequiredFiled e) {
            LOGGER.error(e.getMessage());
        } catch (DataSourceNotExist e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * updates the existing datasource in the Hashmap
     * @param name String containing the name of the datasource to be updated
     * @param newDataSource DataSourceInfo object with updated values
     */
    public void updateDataSource(String name, DataSourceInfo newDataSource) {
        try {
            if (dataSourceCollection.containsKey(name)) {
                dataSourceCollection.remove(name);
                addDataSource(newDataSource);
            } else {
                throw new DataSourceNotExist(name + ": " + KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.DATASOURCE_NOT_EXIST);
            }
        } catch (DataSourceNotExist e) {
            LOGGER.error(e.getMessage());
        }
    }
}
