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
package com.autotune.common.data.datasource;

import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.analyzer.exceptions.MonitoringAgentNotFoundException;
import com.autotune.analyzer.exceptions.TooManyRecursiveCallsException;
import com.autotune.common.target.kubernetes.service.KubernetesServices;
import com.autotune.common.target.kubernetes.service.impl.KubernetesServicesImpl;
import com.autotune.utils.AnalyzerConstants;
import com.autotune.utils.KruizeConstants;
import io.fabric8.kubernetes.api.model.Service;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates and configures the datasource class for the specified datasource string
 */
public class DataSourceFactory
{
	private DataSourceFactory() { }

	private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceFactory.class);

	public static DataSource getDataSource(String dataSource) throws MonitoringAgentNotFoundException {
		String monitoringAgentEndpoint = null;
		if (dataSource.toLowerCase().equals(KruizeDeploymentInfo.getMonitoringAgent()))
			monitoringAgentEndpoint = KruizeDeploymentInfo.getMonitoringAgentEndpoint();

		// Monitoring agent endpoint not set in the configmap
		if (monitoringAgentEndpoint == null || monitoringAgentEndpoint.isEmpty())
			monitoringAgentEndpoint = getMonitoringAgentEndpoint();

		if (dataSource.equals(AnalyzerConstants.PROMETHEUS_DATA_SOURCE))
			return new PrometheusDataSource(monitoringAgentEndpoint);

		LOGGER.error("Datasource " + dataSource + " not supported");
		return null;
	}

	/**
	 * Gets the monitoring agent endpoint for the datasource through the cluster IP
	 * of the service.
	 *
	 * @return Endpoint of the monitoring agent.
	 * @throws MonitoringAgentNotFoundException
	 */
	private static String getMonitoringAgentEndpoint() throws MonitoringAgentNotFoundException {
		//No endpoint was provided in the configmap, find the endpoint from the service.
		KubernetesServices kubernetesServices = new KubernetesServicesImpl();
		List<Service> serviceList = kubernetesServices.getServicelist(null);
		kubernetesServices.shutdownClient();
		String monitoringAgentService = KruizeDeploymentInfo.getMonitoringAgentService();

		if (monitoringAgentService == null)
			throw new MonitoringAgentNotFoundException();

		for (Service service : serviceList) {
			String serviceName = service.getMetadata().getName();
			if (serviceName.toLowerCase().equals(monitoringAgentService)) {
				try {
					String clusterIP = service.getSpec().getClusterIP();
					int port = service.getSpec().getPorts().get(0).getPort();
					LOGGER.info(KruizeDeploymentInfo.getClusterType());
					if (KruizeDeploymentInfo.getKubernetesType().equalsIgnoreCase(KruizeConstants.MINIKUBE)) {
						return AnalyzerConstants.HTTP_PROTOCOL + "://" + clusterIP + ":" + port;
					}
					if (KruizeDeploymentInfo.getKubernetesType().equalsIgnoreCase(KruizeConstants.OPENSHIFT)) {
						return AnalyzerConstants.HTTPS_PROTOCOL + "://" + clusterIP + ":" + port;
					}
				} catch (Exception e) {
					throw new MonitoringAgentNotFoundException();
				}
			}
		}
		LOGGER.error("Monitoring agent endpoint not found");
		return null;
	}

	/**
	 * @param jsonObj The JSON that needs to be parsed
	 * @param key The key to search in the JSON
	 * @param values ArrayList to hold the key values in the JSON
	 * @param level Level of recursion
	 */
	static void parseJsonForKey(JSONObject jsonObj, String key, ArrayList<String> values, int level) throws TooManyRecursiveCallsException {
		level += 1;

		if (level > 30)
			throw new TooManyRecursiveCallsException();

		for (String keyStr : jsonObj.keySet()) {
			Object keyvalue = jsonObj.get(keyStr);

			if (keyStr.equals(key))
				values.add(keyvalue.toString());

			//for nested objects
			if (keyvalue instanceof JSONObject)
				parseJsonForKey((JSONObject) keyvalue, key, values, level);

			//for json array, iterate and recursively get values
			if (keyvalue instanceof JSONArray) {
				JSONArray jsonArray = (JSONArray) keyvalue;
				for (int index = 0; index < jsonArray.length(); index++) {
					Object jsonObject = jsonArray.get(index);
					if (jsonObject instanceof JSONObject) {
						parseJsonForKey((JSONObject) jsonObject, key, values, level);
					}
				}
			}
		}
	}
}
