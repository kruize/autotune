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

import com.autotune.analyzer.exceptions.K8sTypeNotSupportedException;
import com.autotune.analyzer.exceptions.MonitoringAgentNotFoundException;
import com.autotune.analyzer.exceptions.MonitoringAgentNotSupportedException;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.datasource.DataSourceFactory;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.utils.KruizeConstants;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Get the deployment information from the config map and initialize
 */
public class InitializeDeployment
{   
	private static final Logger LOGGER = LoggerFactory.getLogger(InitializeDeployment.class);
	private InitializeDeployment() { }
    
	
	public static void populateDatasourceMapWithCongfig (HashMap<String,DataSourceInfo> datasourceMap){
		HashMap<String, Integer>  keyFlag = new HashMap<String, Integer>();
        String configFile = System.getenv(KruizeConstants.DataSourceConstants.CONFIG_FILE);
        try {
            InputStream is = new FileInputStream(configFile);
            String jsonTxt = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            JSONObject jsonObj = new JSONObject(jsonTxt);
            JSONArray datasourceArr = jsonObj.getJSONArray("datasource");
            for (int i = 0; i < datasourceArr.length(); i++) {
                JSONObject datasourceObj = datasourceArr.getJSONObject(i);
                String name = datasourceObj.getString("name");
                String source = datasourceObj.getString("source");
                String urlString = datasourceObj.getString("url");
                URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                continue;
            }  
            if(url==null){
                LOGGER.error("Invalid datasource URL");
            }else if(keyFlag.containsKey(name)){
				//If two datasource object have same name then throw error
				LOGGER.error("Datasource with similar name Exists");
			}
			else
			  { 
				keyFlag.put(name,(keyFlag.getOrDefault(name,0))+1);
                DataSourceInfo tempDatasourceObj = new DataSourceInfo(name, source, url);
                datasourceMap.put(name,tempDatasourceObj);
              }
            }        
            
        } catch (FileNotFoundException fileNotFoundException) {
            LOGGER.error("Config file not available!");
        }catch (Exception e) {
            e.printStackTrace();
        }
	}

	public static void setup_deployment_info() throws Exception, K8sTypeNotSupportedException, MonitoringAgentNotSupportedException, MonitoringAgentNotFoundException {
		String k8S_type = System.getenv(AnalyzerConstants.K8S_TYPE);
		String auth_type = System.getenv(AnalyzerConstants.AUTH_TYPE);
		String auth_token = System.getenv(AnalyzerConstants.AUTH_TOKEN);
		String cluster_type = System.getenv(AnalyzerConstants.CLUSTER_TYPE);
		String logging_level = System.getenv(AnalyzerConstants.LOGGING_LEVEL);
		String root_logging_level = System.getenv(AnalyzerConstants.ROOT_LOGGING_LEVEL);
		String monitoring_agent = System.getenv(AnalyzerConstants.MONITORING_AGENT);
		String monitoring_agent_service = System.getenv(AnalyzerConstants.MONITORING_SERVICE);
		String monitoring_agent_endpoint = System.getenv(AnalyzerConstants.MONITORING_AGENT_ENDPOINT);
		HashMap<String, DataSourceInfo>  dataSourceMap = new HashMap<String, DataSourceInfo>();

		KruizeDeploymentInfo.setClusterType(cluster_type);
		KruizeDeploymentInfo.setKubernetesType(k8S_type);
		KruizeDeploymentInfo.setAuthType(auth_type);
		KruizeDeploymentInfo.setMonitoringAgent(monitoring_agent);
		KruizeDeploymentInfo.setAuthToken(auth_token);
		KruizeDeploymentInfo.setMonitoringAgentService(monitoring_agent_service);
		KruizeDeploymentInfo.setLoggingLevel(logging_level);
		KruizeDeploymentInfo.setRootLoggingLevel(root_logging_level);
         
		
		//If no endpoint was specified in the configmap
		if (monitoring_agent_endpoint == null || monitoring_agent_endpoint.isEmpty()) {
			if (monitoring_agent == null || monitoring_agent_service == null) {
				throw new MonitoringAgentNotFoundException();
			} else {
				// Fetch endpoint from service cluster IP
				monitoring_agent_endpoint = DataSourceFactory.getDataSource(monitoring_agent).getDataSourceURL();
			}
		}
		KruizeDeploymentInfo.setMonitoringAgentEndpoint(monitoring_agent_endpoint);
        
		//Populate the datasourceinfo hasmap and validate it.
        populateDatasourceMapWithCongfig(dataSourceMap);
		KruizeDeploymentInfo.setDatasourceMap(dataSourceMap);

		KruizeDeploymentInfo.setLayerTable();

		KruizeDeploymentInfo.initiateEventLogging();

		KruizeDeploymentInfo.logDeploymentInfo();
	}

}
