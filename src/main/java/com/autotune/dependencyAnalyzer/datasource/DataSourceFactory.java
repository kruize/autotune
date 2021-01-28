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
package com.autotune.dependencyAnalyzer.datasource;

import com.autotune.dependencyAnalyzer.util.DAConstants;
import com.autotune.dependencyAnalyzer.exceptions.MonitoringAgentNotFoundException;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Creates and configures the datasource class for the specified datasource string
 */
public class DataSourceFactory
{
	public static DataSource getDataSource(String dataSource) throws MonitoringAgentNotFoundException {
		String monitoringAgentEndpoint = getMonitoringAgentEndpoint();
		String token = System.getenv(DAConstants.AUTH_TOKEN);

		if (dataSource.equals(DAConstants.PROMETHEUS_DATA_SOURCE))
		{
			return new PrometheusDataSource(monitoringAgentEndpoint, token);
		}

		return null;
	}

	/**
	 * Gets the monitoring agent endpoint for the datasource through the env, or by the cluster IP
	 * of the service.
	 *
	 * @return Endpoint of the monitoring agent.
	 * @throws MonitoringAgentNotFoundException
	 */
	private static String getMonitoringAgentEndpoint() throws MonitoringAgentNotFoundException {
		String monitoringAgentEndpoint = System.getenv(DAConstants.MONITORING_AGENT_ENDPOINT);
		if (monitoringAgentEndpoint != null)
			return monitoringAgentEndpoint;

		//No URL was provided, find the endpoint from the service.
		KubernetesClient client = new DefaultKubernetesClient();
		ServiceList serviceList = client.services().inAnyNamespace().list();
		String monitoringAgentService = System.getenv(DAConstants.MONITORING_SERVICE);

		if (monitoringAgentService == null)
			throw new MonitoringAgentNotFoundException();

		for (Service service : serviceList.getItems())
		{
			String serviceName = service.getMetadata().getName();
			if (serviceName.toLowerCase().equals(monitoringAgentService))
			{
				try {
					String clusterIP = service.getSpec().getClusterIP();
					int port = service.getSpec().getPorts().get(0).getPort();
					return DAConstants.HTTP_PROTOCOL + clusterIP + ":" + port;
				} catch (Exception e) {
					throw new MonitoringAgentNotFoundException();
				}
			}
		}
		return null;
	}

	/**
	 * @param jsonObj The JSON that needs to be parsed
	 * @param key The key to search in the JSON
	 * @param values ArrayList to hold the key values in the JSON
	 * @param level Level of recursion
	 */
	static void parseJsonForKey(JSONObject jsonObj, String key, ArrayList<String> values, int level) {
		level += 1;

		if (level > 30)
			return;

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
