package com.autotune.common.datasource;

import com.autotune.common.exceptions.DataSourceAlreadyExist;
import com.autotune.common.exceptions.DataSourceMissingRequiredFiled;
import com.autotune.common.exceptions.DataSourceNotServiceable;
import com.autotune.common.exceptions.UnsupportedDataSourceProvider;
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
                    }else{
                        throw new DataSourceNotServiceable(name + ": " + KruizeConstants.ErrorMsgs.DataSourceErrorMsgs.DATASOURCE_NOT_SERVICEABLE);
                    }
                }else{
                    throw new DataSourceAlreadyExist(name + ": " + KruizeConstants.ErrorMsgs.DataSourceErrorMsgs.DATASOURCE_ALREADY_EXIST);
                }

            }
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage());
        } catch (DataSourceMissingRequiredFiled e) {
            LOGGER.error(e.getMessage());
        } catch (UnsupportedDataSourceProvider e) {
            LOGGER.error(e.getMessage());
        } catch (DataSourceNotServiceable e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        } catch (DataSourceAlreadyExist e) {
            LOGGER.error(e.getMessage());
        }
    }
}
