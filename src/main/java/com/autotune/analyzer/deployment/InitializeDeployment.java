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
package com.autotune.analyzer.deployment;

import com.autotune.analyzer.datasource.DataSourceFactory;
import com.autotune.analyzer.exceptions.K8sTypeNotSupportedException;
import com.autotune.analyzer.exceptions.MonitoringAgentNotFoundException;
import com.autotune.analyzer.exceptions.MonitoringAgentNotSupportedException;
import com.autotune.analyzer.utils.AnalyzerConstants;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;

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
		String monitoring_agent = System.getenv(AnalyzerConstants.MONITORING_AGENT);
		String monitoring_agent_service = System.getenv(AnalyzerConstants.MONITORING_SERVICE);
		String monitoring_agent_endpoint = System.getenv(AnalyzerConstants.MONITORING_AGENT_ENDPOINT);

		DeploymentInfo.setClusterType(cluster_type);
		DeploymentInfo.setKubernetesType(k8S_type);
		DeploymentInfo.setAuthType(auth_type);
		DeploymentInfo.setMonitoringAgent(monitoring_agent);
		DeploymentInfo.setAuthToken(auth_token);
		DeploymentInfo.setMonitoringAgentService(monitoring_agent_service);
		DeploymentInfo.setLoggingLevel(logging_level);

		//If no endpoint was specified in the configmap
		if (monitoring_agent_endpoint == null || monitoring_agent_endpoint.isEmpty()) {
			if (monitoring_agent == null || monitoring_agent_service == null) {
				throw new MonitoringAgentNotFoundException();
			} else {
				// Fetch endpoint from service cluster IP
				monitoring_agent_endpoint = DataSourceFactory.getDataSource(monitoring_agent).getDataSourceURL();
			}
		}
		DeploymentInfo.setMonitoringAgentEndpoint(monitoring_agent_endpoint);

		/* Update logging level from the env */
		Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.toLevel(DeploymentInfo.getLoggingLevel()));

		DeploymentInfo.logDeploymentInfo();
	}
}
