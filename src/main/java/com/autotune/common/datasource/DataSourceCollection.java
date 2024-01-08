package com.autotune.common.datasource;

import com.autotune.common.exceptions.DataSourceAlreadyExist;
import com.autotune.common.exceptions.DataSourceNotServiceable;
import com.autotune.common.exceptions.UnsupportedDataSourceProvider;
import com.autotune.common.exceptions.DataSourceMissingRequiredFiled;
import com.autotune.common.utils.CommonUtils;
import com.autotune.utils.KruizeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.net.URL;
import java.util.HashMap;

public class DataSourceCollection {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceCollection.class);
    private HashMap<String, DataSourceInfo> dataSourceCollection;

    public DataSourceCollection() {
        this.dataSourceCollection = new HashMap<>();
    }

    /**
     * Returns the hashmap of data sources
     * @return HashMap containing data sources
     */
    public HashMap<String, DataSourceInfo> getDataSourcesCollection() {
        return dataSourceCollection;
    }

    /**
     * Adds datasource to collection
     * @param datasource DataSourceInfo object containing details of datasource
     */
    public void addDataSource(DataSourceInfo datasource) {
        try {
            if (datasource.getName() == null) {
                throw new DataSourceMissingRequiredFiled(KruizeConstants.ErrorMsgs.DataSourceErrorMsgs.MISSING_DATASOURCE_NAME);
            }
            if (datasource.getProvider() == null) {
                throw new DataSourceMissingRequiredFiled(datasource.getName() + ": " + KruizeConstants.ErrorMsgs.DataSourceErrorMsgs.MISSING_DATASOURCE_PROVIDER);
            }
            if (datasource.getServiceName() == null) {
                throw new DataSourceMissingRequiredFiled(datasource.getName() + ": " + KruizeConstants.ErrorMsgs.DataSourceErrorMsgs.MISSING_DATASOURCE_SERVICENAME);
            }
            if (datasource.getUrl() == null) {
                throw new DataSourceMissingRequiredFiled(datasource.getName() + ": " + KruizeConstants.ErrorMsgs.DataSourceErrorMsgs.MISSING_DATASOURCE_URL);
            }

            if (!dataSourceCollection.containsKey(datasource.getName())) {
                if (datasource.getProvider().equalsIgnoreCase(KruizeConstants.SupportedDatasources.PROMETHEUS)) {
                    PrometheusDataSource promDataSource = new PrometheusDataSource(datasource.getName(), datasource.getProvider(), datasource.getServiceName(), datasource.getUrl());
                    if (promDataSource.isReachable() == CommonUtils.DatasourceReachabilityStatus.REACHABLE) {
                        dataSourceCollection.put(datasource.getName(), promDataSource);
                        LOGGER.info("Added Datasource to Collection: " + promDataSource.getName() + ", " + promDataSource.getServiceName() + ", " + promDataSource.getUrl());
                    } else {
                        throw new DataSourceNotServiceable(promDataSource.getName() + ": " + KruizeConstants.ErrorMsgs.DataSourceErrorMsgs.DATASOURCE_NOT_SERVICEABLE);
                    }
                } else {
                    throw new UnsupportedDataSourceProvider(datasource.getName() + ": " + KruizeConstants.ErrorMsgs.DataSourceErrorMsgs.UNSUPPORTED_DATASOURCE_PROVIDER);
                }
            } else {
                throw new DataSourceAlreadyExist(datasource.getName() + ": " + KruizeConstants.ErrorMsgs.DataSourceErrorMsgs.DATASOURCE_ALREADY_EXIST);
            }
        } catch (DataSourceMissingRequiredFiled e) {
            LOGGER.error(e.getMessage());
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

            InputStream is = new FileInputStream(configFile);
            String jsonTxt = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            configObject = new JSONObject(jsonTxt);
            JSONArray dataSourceArr = configObject.getJSONArray("datasource");

            for (int i = 0; i < dataSourceArr.length(); i++) {
                JSONObject dataSourceObject = dataSourceArr.getJSONObject(i);
                String name = dataSourceObject.getString("name");
                String provider = dataSourceObject.getString("provider");
                String serviceName = dataSourceObject.getString("serviceName");
                String dataSourceURL = dataSourceObject.getString("url");
                DataSourceInfo datasource = new DataSourceInfo(name, provider, serviceName, new URL(dataSourceURL));
                addDataSource(datasource);
            }
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }
}
