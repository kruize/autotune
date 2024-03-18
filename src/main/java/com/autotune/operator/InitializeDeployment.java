/*******************************************************************************
 * Copyright (c) 2020, 2021 Red Hat, IBM Corporation and others.
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
package com.autotune.operator;

import com.autotune.analyzer.exceptions.DefaultDataSourceNotFoundException;
import com.autotune.analyzer.exceptions.K8sTypeNotSupportedException;
import com.autotune.common.datasource.*;
import com.autotune.utils.KruizeConstants;
import com.google.gson.Gson;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Get the deployment information from the config map and initialize
 */
public class InitializeDeployment {
    private static final Logger LOGGER = LoggerFactory.getLogger(InitializeDeployment.class);

    private InitializeDeployment() {

    }

    public static void setup_deployment_info() throws Exception, K8sTypeNotSupportedException, DefaultDataSourceNotFoundException {
        setConfigValues(KruizeConstants.CONFIG_FILE, KruizeConstants.KRUIZE_CONFIG_ENV_NAME.class);
        setConfigValues(KruizeConstants.DBConstants.CONFIG_FILE, KruizeConstants.DATABASE_ENV_NAME.class);

        DataSourceCollection dataSourceCollection = DataSourceCollection.getInstance();
        dataSourceCollection.addDataSourcesFromConfigFile(KruizeConstants.CONFIG_FILE);

        KruizeDeploymentInfo.setCluster_type(KruizeDeploymentInfo.cluster_type);
        KruizeDeploymentInfo.setKubernetesType(KruizeDeploymentInfo.k8s_type);
        KruizeDeploymentInfo.setAuth_type(KruizeDeploymentInfo.auth_type);
        KruizeDeploymentInfo.setDefaultDataSource();
        DataSourceInfo defaultDataSource = KruizeDeploymentInfo.defaultDataSource;
        // If no default datasource was specified in the configmap
        if (defaultDataSource == null) {
                LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.DEFAULT_DATASOURCE_NOT_FOUND);
        }
        KruizeDeploymentInfo.setLayerTable();

        KruizeDeploymentInfo.initiateEventLogging();

        KruizeDeploymentInfo.logDeploymentInfo();

        /**
         * Temporarily commenting out the data import process from data sources
         */

        /*
        DataSourceManager dataSourceManager = new DataSourceManager();
        dataSourceManager.importDataFromAllDataSources();
        */
    }

    /**
     * This code sets configuration values based on the provided config file name and the given environment class.
     * First, it tries to retrieve the configuration file path from the corresponding environment variable and parses it into a JSON object.
     * If that fails, it logs a warning message and proceeds to check if the environment variable is set.
     * Next, it retrieves all the String type fields from the given environment class using Java reflection
     * and iterates through them. For each field,
     * it sets the corresponding static variable in the KruizeDeploymentInfo class using its
     * lowercase name as the key and the value obtained from the configuration object or
     * the environment variable, if available.
     * Any errors encountered during this process are logged as warnings.
     */
    private static void setConfigValues(String configFileName, Class envClass) {
        String configFile = System.getenv(configFileName);
        JSONObject configObject = null;
        if (null != configFile) {
            try (InputStream is = new FileInputStream(configFile)) {
                String jsonTxt = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                configObject = new JSONObject(jsonTxt);
            } catch (Exception exception) {
                LOGGER.warn("Failed to set config using file {} due to {}. checking if corresponding environment variable is set.", configFile, exception.getMessage());
            }
        }
        Field[] fields = envClass.getFields();
        for (Field field : fields) {
            try {
                Field deploymentInfoField = KruizeDeploymentInfo.class.getDeclaredField(field.getName().toLowerCase(Locale.ROOT));
                String deploymentInfoFieldValue = getKruizeConfigValue((String) field.get(null), configObject);
                if (null != deploymentInfoFieldValue) {
                    if (deploymentInfoField.getType() == String.class)
                        deploymentInfoField.set(null, deploymentInfoFieldValue);
                    else if (deploymentInfoField.getType() == Boolean.class)
                        deploymentInfoField.set(null, Boolean.parseBoolean(deploymentInfoFieldValue));
                    else if (deploymentInfoField.getType() == Integer.class)
                        deploymentInfoField.set(null, Integer.parseInt(deploymentInfoFieldValue));
                    else
                        throw new IllegalAccessException("Failed to set " + deploymentInfoField + "due to its type " + deploymentInfoField.getType());
                }
            } catch (Exception e) {
                LOGGER.warn("Error while setting config variables : {} : {}", e.getClass(), e.getMessage());
            }
        }
    }

    private static String getKruizeConfigValue(String envName, JSONObject kruizeConfigObject) {
        Object envValue = null;
        String message = "Config variable : " + envName;
        try {
            if (null != kruizeConfigObject) {
                //extract values from jsonObject
                try {
                    String[] parts = envName.split("_");
                    JSONObject primaryObj = kruizeConfigObject;
                    int i = 0;
                    for (i = 0; i < parts.length - 1; i++) {
                        primaryObj = primaryObj.getJSONObject(parts[i]);
                    }
                    envValue = primaryObj.get(parts[i]);
                    message = message + "  :- found in mounted config file";
                } catch (Exception e) {
                    message = message + "  :- Not found in mounted config file";
                }
            } else {
                message = message + "  :- No mount config file";
            }
            //Override if env value set outside kruizeConfigJson
            String sysEnvValue = System.getenv(envName);
            if (null == sysEnvValue && null == envValue) {
                message = message + ", nor not able to set via environment variable and set to null or default.";
            } else {
                if (null != sysEnvValue && envValue == null) {
                    envValue = sysEnvValue;
                    message = message + ", but able to set via environment variable";
                } else if (null != sysEnvValue && envValue != null) {
                    envValue = sysEnvValue;
                    message = message + ", and value override using environment variable";
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to set environment : {} due to {}", envName, e.getMessage());
        }
        LOGGER.info(message);
        if (envValue == null)
            return null;
        else
            return envValue.toString();
    }


}
