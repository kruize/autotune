package com.autotune.common.datasource;

import com.autotune.common.datasource.promql.PrometheusDataSource;
import com.autotune.common.exceptions.*;
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
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.net.URL;
import java.util.HashMap;

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
        final String name = datasource.getName();
        final String provider = datasource.getProvider();
        final String serviceName = datasource.getServiceName();
        final String namespace = datasource.getNamespace();
        String url = "";
        if (datasource.getUrl() != null) {
            url = datasource.getUrl().toString();
        }
        LOGGER.info(KruizeConstants.DataSourceConstants.AddingDataSource + name);
        try {
            if (validateInput(name, provider, serviceName, url, namespace)) {
                if (!dataSourceCollection.containsKey(name)) {
                    if (provider.equalsIgnoreCase(KruizeConstants.SupportedDatasources.PROMETHEUS)) {
                        PrometheusDataSource prometheusDataSource = null;
                        if(serviceName.isEmpty()) {
                            prometheusDataSource = new PrometheusDataSource(name, provider, new URL(url));
                        } else {
                            prometheusDataSource = new PrometheusDataSource(name, provider, serviceName, namespace);
                        }
                        LOGGER.info(KruizeConstants.DataSourceConstants.VerifyingDataSourceReachability + name);
                        if (prometheusDataSource.isReachable() == CommonUtils.DatasourceReachabilityStatus.REACHABLE) {
                            LOGGER.info(KruizeConstants.DataSourceConstants.VerifyingDataSourceReachability + name + ": " + CommonUtils.DatasourceReachabilityStatus.REACHABLE);
                            dataSourceCollection.put(name, prometheusDataSource);
                            LOGGER.info(KruizeConstants.DataSourceConstants.DataSourceAdded + name + ", " + serviceName + ", " + url);
                        } else {
                            throw new DataSourceNotServiceable(name + KruizeConstants.ErrorMsgs.DataSourceErrorMsgs.DATASOURCE_NOT_SERVICEABLE);
                        }
                    } else {
                        throw new UnsupportedDataSourceProvider(name + KruizeConstants.ErrorMsgs.DataSourceErrorMsgs.UNSUPPORTED_DATASOURCE_PROVIDER);
                    }
                } else {
                    throw new DataSourceAlreadyExist(name + KruizeConstants.ErrorMsgs.DataSourceErrorMsgs.DATASOURCE_ALREADY_EXIST);
                }
            }
        } catch (UnsupportedDataSourceProvider e) {
            LOGGER.error(e.getMessage());
        } catch (DataSourceNotServiceable e) {
            LOGGER.error(e.getMessage());
        } catch (DataSourceAlreadyExist e) {
            LOGGER.error(e.getMessage());
        } catch (MalformedURLException e) {
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
            JSONArray dataSourceArr = configObject.getJSONArray(KruizeConstants.DataSourceConstants.KruizeDataSource);

            for (Object dataSourceObj: dataSourceArr) {
                JSONObject dataSourceObject = (JSONObject) dataSourceObj;
                String name = dataSourceObject.getString(KruizeConstants.DataSourceConstants.DataSourceName);
                String provider = dataSourceObject.getString(KruizeConstants.DataSourceConstants.DataSourceProvider);
                String serviceName = dataSourceObject.getString(KruizeConstants.DataSourceConstants.DataSourceServiceName);
                String namespace = dataSourceObject.getString(KruizeConstants.DataSourceConstants.DataSourceServiceNamespace);
                String dataSourceURL = dataSourceObject.getString(KruizeConstants.DataSourceConstants.DataSourceUrl);
                DataSourceInfo datasource = null;
                if (!validateInput(name, provider, serviceName, dataSourceURL, namespace)) {
                    continue;
                }
                if (dataSourceURL.isEmpty()) {
                    datasource = new DataSourceInfo(name, provider, serviceName, namespace);
                } else {
                    datasource = new DataSourceInfo(name, provider, new URL(dataSourceURL));
                }
                addDataSource(datasource);
            }
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(KruizeConstants.ErrorMsgs.DataSourceErrorMsgs.DATASOURCE_INVALID_URL);
        }
    }

    public boolean validateInput(String name, String provider, String servicename, String url, String namespace) {
        try {
            if (name.isEmpty()) {
                throw new DataSourceMissingRequiredFiled(KruizeConstants.ErrorMsgs.DataSourceErrorMsgs.MISSING_DATASOURCE_NAME);
            }
            if (provider.isEmpty()) {
                throw new DataSourceMissingRequiredFiled(name + KruizeConstants.ErrorMsgs.DataSourceErrorMsgs.MISSING_DATASOURCE_PROVIDER);
            }
            if (namespace.isEmpty() && !servicename.isEmpty()) {
                throw new DataSourceMissingRequiredFiled(name + KruizeConstants.ErrorMsgs.DataSourceErrorMsgs.MISSING_DATASOURCE_NAMESPACE);
            }
            if (servicename.isEmpty() && url.isEmpty()) {
                throw new DataSourceMissingRequiredFiled(name + KruizeConstants.ErrorMsgs.DataSourceErrorMsgs.MISSING_DATASOURCE_SERVICENAME_AND_URL);
            }
            if (!servicename.isEmpty() && !url.isEmpty()) {
                throw new DataSourceMissingRequiredFiled(name + KruizeConstants.ErrorMsgs.DataSourceErrorMsgs.DATASOURCE_URL_SERVICENAME_SET);
            }
            return true;
        } catch (DataSourceMissingRequiredFiled e) {
            LOGGER.error(e.getMessage());
            return false;
        }
    }

    public void deleteDataSource(String name) {
        try {
            if (name == null) {
                throw new DataSourceMissingRequiredFiled(KruizeConstants.ErrorMsgs.DataSourceErrorMsgs.MISSING_DATASOURCE_NAME);
            }
            if (dataSourceCollection.containsKey(name)) {
                dataSourceCollection.remove(name);
            } else {
                throw new DataSourceNotExist(name + ": " + KruizeConstants.ErrorMsgs.DataSourceErrorMsgs.DATASOURCE_NOT_EXIST);
            }
        } catch (DataSourceMissingRequiredFiled e) {
            LOGGER.error(e.getMessage());
        } catch (DataSourceNotExist e) {
            LOGGER.error(e.getMessage());
        }
    }

    public void updateDataSource(String name, DataSourceInfo newDataSource) {
        try {
            if (dataSourceCollection.containsKey(name)) {
                dataSourceCollection.remove(name);
                addDataSource(newDataSource);
            } else {
                throw new DataSourceNotExist(name + ": " + KruizeConstants.ErrorMsgs.DataSourceErrorMsgs.DATASOURCE_NOT_EXIST);
            }
        } catch (DataSourceNotExist e) {
            LOGGER.error(e.getMessage());
        }
    }
}
