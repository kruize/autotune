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
package com.autotune.dependencyAnalyzer.service;

import com.autotune.dependencyAnalyzer.application.Tunable;
import com.autotune.dependencyAnalyzer.datasource.DataSourceFactory;
import com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment;
import com.autotune.dependencyAnalyzer.env.EnvInfo;
import com.autotune.dependencyAnalyzer.exceptions.MonitoringAgentNotFoundException;
import com.autotune.dependencyAnalyzer.k8sObjects.AutotuneConfig;
import com.autotune.dependencyAnalyzer.k8sObjects.AutotuneObject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

public class ListAppTunables extends HttpServlet
{
	/**
	 * Returns the list of applications monitored by autotune along with their tunables
	 *
	 * Request:
	 * `GET /listAppTunables` gives the tunables and layer information for all the applications monitored by autotune.
	 *
	 * `GET /listAppTunables?application_name=<APPLICATION_NAME>` for getting the tunables information of a specific application.
	 *
	 * `GET /listAppTunables?application_name=<APPLICATION_NAME>&layer_name='<LAYER>' for getting tunables of a specific layer for the application.
	 *
	 * Example JSON:
	 * {
	 *       "application_name": "app1",
	 *       “objective_function”: “transaction_response_time”,
	 *       "sla_class": "response_time",
	 *       “direction”: “minimize”
	 *       "layers": [
	 *         {
	 *           "layer_level": 0,
	 *           "layer_name": "container",
	 *           "layer_details": "generic container tunables"
	 *           "tunables": [
	 *             {
	 *                 "name": "memoryRequest",
	 *                 "upper_bound": "300M",
	 *                 "lower_bound": "150M",
	 *                 "value_type": "double",
	 *                 "query_url": "http://prometheus:9090/container_memory_working_set_bytes{container=\"\", pod_name=\"petclinic-deployment-6d4c8678d4-jmz8x\"}"
	 *             },
	 *             {
	 *                 "name": "cpuRequest",
	 *                 "upper_bound": "3.0",
	 *                 "lower_bound": "1.0",
	 *                 "value_type": "double",
	 *                 "query_url": "http://prometheus:9090/(container_cpu_usage_seconds_total{container!=\"POD\", pod_name=\"petclinic-deployment-6d4c8678d4-jmz8x\"}[1m])"
	 *             }
	 *           ]
	 *         },
	 *         {
	 *           "layer_level": 1,
	 *           "layer_name": "openj9",
	 *           "layer_details": "java openj9 tunables",
	 *           "tunables": [
	 *             {
	 *               "name": "javaHeap",
	 *               "upper_bound": "250M",
	 *               "lower_bound": "100M",
	 *               "value_type": "double",
	 *               "query_url": "http://prometheus:9090/jvm_memory_used_bytes{area=\"heap\",id=\"nursery-allocate\", pod=petclinic-deployment-6d4c8678d4-jmz8x}"
	 *             }
	 *           ]
	 *       }
	 *     ]
	 *   }
	 * ]
	 * @param req
	 * @param resp
	 * @throws IOException
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		JSONArray outputJsonArray = new JSONArray();
		resp.setContentType("application/json");

		String applicationName = req.getParameter("application_name");
		String layerName = req.getParameter("layer_name");

		for (String autotuneObjectKey : AutotuneDeployment.applicationServiceStackMap.keySet()) {
			AutotuneObject autotuneObject = AutotuneDeployment.autotuneObjectMap.get(autotuneObjectKey);

			//If no application_name was passed in the request
			if (applicationName == null) {
				for (String application : AutotuneDeployment.applicationServiceStackMap.get(autotuneObjectKey).keySet()) {
					addAppTunablesToResponse(outputJsonArray, autotuneObjectKey, autotuneObject, application, layerName);
				}
			} else {
				addAppTunablesToResponse(outputJsonArray, autotuneObjectKey, autotuneObject, applicationName, layerName);
			}
			resp.getWriter().println(outputJsonArray.toString(4));
		}
	}

	private void addAppTunablesToResponse(JSONArray outputJsonArray, String autotuneObjectKey, AutotuneObject autotuneObject, String application, String layerName) {
		//If no such application is monitored by autotune
		if (!AutotuneDeployment.applicationServiceStackMap.get(autotuneObjectKey).containsKey(application))
			return;

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("application_name", application);
		jsonObject.put("direction", autotuneObject.getSlaInfo().getDirection());
		jsonObject.put("objective_function", autotuneObject.getSlaInfo().getObjectiveFunction());
		jsonObject.put("sla_class", autotuneObject.getSlaInfo().getSlaClass());

		JSONArray layersArray = new JSONArray();
		for (AutotuneConfig autotuneConfig : AutotuneDeployment.applicationServiceStackMap.get(autotuneObjectKey).get(application).getStackLayers()) {
			if (layerName == null || autotuneConfig.getName().equals(layerName)) {
				JSONObject layerJson = new JSONObject();
				layerJson.put("layer_name", autotuneConfig.getName());
				layerJson.put("layer_details", autotuneConfig.getDetails());
				layerJson.put("layer_level", autotuneConfig.getLevel());

				JSONArray tunablesArray = new JSONArray();
				for (Tunable tunable : autotuneConfig.getTunables()) {
					JSONObject tunableJson = new JSONObject();
					tunableJson.put("name", tunable.getName());
					tunableJson.put("upper_bound", tunable.getUpperBound());
					tunableJson.put("lower_bound", tunable.getLowerBound());
					tunableJson.put("value_type", tunable.getValueType());
					try {
						tunableJson.put("query_url", Objects.requireNonNull(DataSourceFactory.getDataSource(EnvInfo.getDataSource())).getDataSourceURL() +
								tunable.getQueries().get(EnvInfo.getDataSource()));
					} catch (MonitoringAgentNotFoundException e) {
						tunableJson.put("query_url", tunable.getQueries().get(EnvInfo.getDataSource()));
					}
					tunablesArray.put(tunableJson);
				}
				layerJson.put("tunables", tunablesArray);
				layersArray.put(layerJson);
			}
		}
		jsonObject.put("layers", layersArray);
		outputJsonArray.put(jsonObject);
	}
}
