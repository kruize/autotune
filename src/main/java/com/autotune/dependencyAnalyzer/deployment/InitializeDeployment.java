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
package com.autotune.dependencyAnalyzer.deployment;

import com.autotune.dependencyAnalyzer.datasource.DataSourceFactory;
import com.autotune.dependencyAnalyzer.exceptions.K8sTypeNotSupportedException;
import com.autotune.dependencyAnalyzer.exceptions.MonitoringAgentNotFoundException;
import com.autotune.dependencyAnalyzer.exceptions.MonitoringAgentNotSupportedException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;

public class InitializeDeployment
{
	public static void setup_deployment_info() throws Exception, K8sTypeNotSupportedException, MonitoringAgentNotSupportedException, MonitoringAgentNotFoundException {
		String monitoring_agent_endpoint = System.getenv("MONITORING_AGENT_ENDPOINT");
		String monitoring_agent_service = System.getenv("MONITORING_SERVICE");
		String monitoring_agent = System.getenv("MONITORING_AGENT");
		String logging_level = System.getenv("LOGGING_LEVEL");
		String cluster_type = System.getenv("CLUSTER_TYPE");
		String auth_token = System.getenv("AUTH_TOKEN");
		String auth_type = System.getenv("AUTH_TYPE");
		String k8S_type = System.getenv("K8S_TYPE");

		DeploymentInfo.setClusterType(cluster_type);
		DeploymentInfo.setKubernetesType(k8S_type);
		DeploymentInfo.setAuthType(auth_type);
		DeploymentInfo.setMonitoringAgent(monitoring_agent);
		DeploymentInfo.setAuthToken(auth_token);
		DeploymentInfo.setMonitoringAgentService(monitoring_agent_service);

		//If no endpoint was specified in the configmap
		if (monitoring_agent_endpoint == null) {
			if (monitoring_agent == null || monitoring_agent_service == null) {
				throw new MonitoringAgentNotFoundException();
			} else {
				// Fetch endpoint from service cluster IP
				monitoring_agent_endpoint = DataSourceFactory.getDataSource(monitoring_agent).getDataSourceURL();
			}
		}
		DeploymentInfo.setMonitoringAgentEndpoint(monitoring_agent_endpoint);

		/* Update logging level from the env */
		if (logging_level != null)
			Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.toLevel(logging_level));

		DeploymentInfo.logDeploymentInfo();
	}
}
