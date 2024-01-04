package com.autotune.common.datasource;

import com.autotune.common.exceptions.*;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.KruizeSupportedTypes;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataSourceCollection {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceCollection.class);
    private HashMap<String, DataSource> dataSourcesCollection;
    
    public DataSourceCollection(){
        this.dataSourcesCollection = new HashMap<>();
    }

    /**
     * Returns the hashmap of data sources
     * @return HashMap containing data sources
     */
    public HashMap<String, DataSource> getDataSourcesCollection(){
        return dataSourcesCollection;
    }

    /**
     * Returns the of data source based on name
     * @param name String name of the data source
     * @return DataSource object
     */
    public DataSource getDataSource(String name){
        DataSource resultDataSource = null;
        if(dataSourcesCollection.containsKey(name)){
            resultDataSource = dataSourcesCollection.get(name);
        }
        return resultDataSource;
    }

    /**
     * Adds datasource to collection
     * @param name String name of the data source
     * @param provider String provider for data source
     * @param serviceName String name of the service for data source
     * @param dataSourceURL URL of data source
     */
    public void addDataSourceToCollection(String name, String provider, String serviceName, String dataSourceURL){
        try{
            if(name == null){
                throw new DataSourceMissingRequiredFiled(KruizeConstants.ErrorMsgs.DataSourceErrorMsgs.MISSING_DATASOURCE_NAME);
            }
            if(provider == null){
                throw new DataSourceMissingRequiredFiled(name + ": " + KruizeConstants.ErrorMsgs.DataSourceErrorMsgs.MISSING_DATASOURCE_PROVIDER);
            }
            if(serviceName == null){
                throw new DataSourceMissingRequiredFiled(name + ": " + KruizeConstants.ErrorMsgs.DataSourceErrorMsgs.MISSING_DATASOURCE_SERVICENAME);
            }
            if(dataSourceURL == null){
                throw new DataSourceMissingRequiredFiled(name + ": " + KruizeConstants.ErrorMsgs.DataSourceErrorMsgs.MISSING_DATASOURCE_URL);
            }

            if(!dataSourcesCollection.containsKey(name)){
                DataSource dataSource;
                if(provider.equalsIgnoreCase(KruizeConstants.SupportedDatasources.PROMETHEUS)){
                    dataSource = new PrometheusDataSource(name, serviceName, dataSourceURL);
                }else{
                    throw new UnsupportedDataSourceProvider(name + ": " + KruizeConstants.ErrorMsgs.DataSourceErrorMsgs.UNSUPPORTED_DATASOURCE_PROVIDER);
                }

                if(dataSource.isServiceable()){
                    dataSourcesCollection.put(name, dataSource);
                    LOGGER.info("Added Datasource to Collection: " + dataSource.getName() + ", " + dataSource.getServiceName() + ", " + dataSource.getDataSourceURL());
                }else{
                    throw new DataSourceNotServiceable(name + ": " + KruizeConstants.ErrorMsgs.DataSourceErrorMsgs.DATASOURCE_NOT_SERVICEABLE);
                }
            }else{
                throw new DataSourceAlreadyExist(name + ": " + KruizeConstants.ErrorMsgs.DataSourceErrorMsgs.DATASOURCE_ALREADY_EXIST);
            }
        }catch (DataSourceMissingRequiredFiled e) {
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
     * Deletes datasource from collection
     * @param name String name of the data source
     */
    public void deleteDataSourceFromCollection(String name){
        try{
            if(name == null) {
                throw new DataSourceMissingRequiredFiled(KruizeConstants.ErrorMsgs.DataSourceErrorMsgs.MISSING_DATASOURCE_NAME);
            }
            if(dataSourcesCollection.containsKey(name)){
                dataSourcesCollection.remove(name);
            }else{
                throw new DataSourceNotExist(name + ": " + KruizeConstants.ErrorMsgs.DataSourceErrorMsgs.DATASOURCE_NOT_EXIST);
            }
        } catch (DataSourceMissingRequiredFiled e) {
            LOGGER.error(e.getMessage());
        } catch (DataSourceNotExist e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Update existing datasource to new values
     * @param oldDataSource old DataSource Object
     * @param newName String name of the data source
     * @param newServiceName String name of the service for data source
     * @param newDataSourceURL URL of data source
     */
    public void updateDataSource(DataSource oldDataSource, String newName, String newServiceName, String newDataSourceURL){
        try{
            if(dataSourcesCollection.containsKey(oldDataSource.getName())){
                if(newName == null) {
                    newName = oldDataSource.getName();
                }
                if(newServiceName == null){
                    newServiceName = oldDataSource.getServiceName();
                }
                if(newDataSourceURL == null){
                    newDataSourceURL = oldDataSource.getDataSourceURL();
                }

                String provider = oldDataSource.getProvider();

                DataSource updatedDataSource = null;
                if(provider.equalsIgnoreCase(KruizeConstants.SupportedDatasources.PROMETHEUS)){
                    updatedDataSource = new PrometheusDataSource(newName, newServiceName, newDataSourceURL);
                }

                if(updatedDataSource.isServiceable()){
                    dataSourcesCollection.remove(oldDataSource.getName());
                    dataSourcesCollection.put(newName, updatedDataSource);
                    LOGGER.info("Updated Datasource: " + updatedDataSource.getName() + ", " + updatedDataSource.getServiceName() + ", " + updatedDataSource.getDataSourceURL());
                }else{
                    throw new DataSourceNotServiceable(newName + ": " + KruizeConstants.ErrorMsgs.DataSourceErrorMsgs.DATASOURCE_NOT_SERVICEABLE);
                }
            }else{
                throw new DataSourceNotExist(oldDataSource.getName() + ": " + KruizeConstants.ErrorMsgs.DataSourceErrorMsgs.DATASOURCE_NOT_EXIST);
            }
        } catch (DataSourceNotServiceable e) {
            LOGGER.error(e.getMessage());
        } catch (DataSourceNotExist e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Loads the data sources available at installation time
     * @param configFileName name of the config file mounted
     */
    public void addDataSourcesFromConfigFile(String configFileName){
        try{
            String configFile = System.getenv(configFileName);
            JSONObject configObject = null;

            InputStream is = new FileInputStream(configFile);
            String jsonTxt = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            configObject = new JSONObject(jsonTxt);
            JSONArray dataSourceArr = configObject.getJSONArray("datasource");
            
            for(int i = 0; i < dataSourceArr.length(); i++){
                JSONObject dataSourceObject = dataSourceArr.getJSONObject(i);
                String name = dataSourceObject.getString("name");
                String provider = dataSourceObject.getString("provider");
                String serviceName = dataSourceObject.getString("serviceName");
                String dataSourceURL = dataSourceObject.getString("url");

                addDataSourceToCollection(name, provider, serviceName, dataSourceURL);
            }
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }
}
