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

import com.autotune.common.datasource.DataSourceFactory;
import com.autotune.analyzer.exceptions.K8sTypeNotSupportedException;
import com.autotune.analyzer.exceptions.MonitoringAgentNotFoundException;
import com.autotune.analyzer.exceptions.MonitoringAgentNotSupportedException;
import com.autotune.analyzer.utils.AnalyzerConstants;

/**
 * Get the deployment information from the config map and initialize
 */
public class InitializeDeployment
{
	private InitializeDeployment() { }

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
		int DEFAULT_BULKUPLOAD_LIMIT = 1;
		AnalyzerConstants.PersistenceType DEFAULT_PERSISTENCE_TYPE = AnalyzerConstants.PersistenceType.LOCAL;
		int bulkupload_createexperiment_limit = System.getenv(AnalyzerConstants.BULKUPLOAD_CREATEEXPERIMENT_LIMIT) != null ?
				Integer.parseInt(System.getenv(AnalyzerConstants.BULKUPLOAD_CREATEEXPERIMENT_LIMIT)) :
				DEFAULT_BULKUPLOAD_LIMIT;
		AnalyzerConstants.PersistenceType PERSISTENCE_TYPE = System.getenv(AnalyzerConstants.PERSISTANCE_STORAGE) != null ?
				AnalyzerConstants.PersistenceType.valueOf(System.getenv(AnalyzerConstants.PERSISTANCE_STORAGE)) :
				DEFAULT_PERSISTENCE_TYPE;

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

		KruizeDeploymentInfo.setLayerTable();

		KruizeDeploymentInfo.initiateEventLogging();

		KruizeDeploymentInfo.logDeploymentInfo();

		KruizeDeploymentInfo.setBulkupload_createexperiment_limit(bulkupload_createexperiment_limit);
		KruizeDeploymentInfo.setPersistence_type(PERSISTENCE_TYPE);

	}
}
