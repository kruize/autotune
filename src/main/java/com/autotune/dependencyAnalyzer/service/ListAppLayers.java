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

import com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment;
import com.autotune.dependencyAnalyzer.k8sObjects.AutotuneConfig;
import com.autotune.dependencyAnalyzer.k8sObjects.AutotuneObject;
import com.autotune.dependencyAnalyzer.util.DAConstants;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ListAppLayers extends HttpServlet
{
	/**
	 * Returns the list of applications monitored by autotune along with layers detected in the applications.
	 *
	 * Request:
	 * `GET /listAppLayers` returns the list of all applications monitored by autotune, and their layers.
	 *
	 * `GET /listAppLayers?application_name=<APP_NAME>` returns the layers for the application specified.
	 *
	 * Example JSON:
	 * [
	 *     {
	 *         "application_name": "app1",
	 *         “objective_function”: “transaction_response_time”,
	 *         "sla_class": "response_time",
	 *         “direction”: “minimize”
	 *         "layers": [
	 *           {
	 *             "layer_level": 0,
	 *             "layer_name": "container",
	 *             "layer_details": "generic container tunables"
	 *           },
	 *           {
	 *             "layer_level": 1,
	 *             "layer_name": "openj9",
	 *             "layer_details": "java openj9 tunables"
	 *           }
	 *         ]
	 *     },
	 *     {
	 *         "application_name": "app2",
	 *         “objective_function”: “performedChecks_total”,
	 *         "sla_class": "throughput",
	 *         “direction”: “maximize”
	 *         "layers": [
	 *           {
	 *             "layer_level": 0,
	 *             "layer_name": "container",
	 *             "layer_details": "generic container tunables"
	 *           },
	 *           {
	 *             "layer_level": 1,
	 *             "layer_name": "hotspot",
	 *             "layer_details": "java hotspot tunables"
	 *           }
	 *         ]
	 *     }
	 * ]
	 * @param req
	 * @param resp
	 * @throws IOException
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		JSONArray outputJsonArray = new JSONArray();
		resp.setContentType("application/json");

		String application_name = req.getParameter(DAConstants.ServiceConstants.APPLICATION_NAME);

		for (String autotuneObjectKey : AutotuneDeployment.applicationServiceStackMap.keySet()) {
			AutotuneObject autotuneObject = AutotuneDeployment.autotuneObjectMap.get(autotuneObjectKey);

			//No application_name parameter was passed in the request
			if (application_name == null) {
				for (String application : AutotuneDeployment.applicationServiceStackMap.get(autotuneObjectKey).keySet()) {
					addLayerToResponse(outputJsonArray, autotuneObjectKey, autotuneObject, application);
				}
			} else {
				addLayerToResponse(outputJsonArray, autotuneObjectKey, autotuneObject, application_name);
			}
		}

		resp.getWriter().println(outputJsonArray.toString(4));
	}

	private void addLayerToResponse(JSONArray outputJsonArray, String autotuneObjectKey, AutotuneObject autotuneObject, String application) {
		//Check if application is monitored by autotune
		if (!AutotuneDeployment.applicationServiceStackMap.get(autotuneObjectKey).containsKey(application))
			return;

		JSONObject jsonObject = new JSONObject();
		jsonObject.put(DAConstants.ServiceConstants.APPLICATION_NAME, application);
		jsonObject.put(DAConstants.AutotuneObjectConstants.DIRECTION, autotuneObject.getSlaInfo().getDirection());
		jsonObject.put(DAConstants.AutotuneObjectConstants.OBJECTIVE_FUNCTION, autotuneObject.getSlaInfo().getObjectiveFunction());
		jsonObject.put(DAConstants.AutotuneObjectConstants.SLA_CLASS, autotuneObject.getSlaInfo().getSlaClass());

		JSONArray layersArray = new JSONArray();
		for (AutotuneConfig autotuneConfig : AutotuneDeployment.applicationServiceStackMap.get(autotuneObjectKey).get(application).getStackLayers()) {
			JSONObject layerJson = new JSONObject();
			layerJson.put(DAConstants.AutotuneConfigConstants.LAYER_NAME, autotuneConfig.getName());
			layerJson.put(DAConstants.AutotuneConfigConstants.LAYER_DETAILS, autotuneConfig.getDetails());
			layerJson.put(DAConstants.AutotuneConfigConstants.LAYER_LEVEL, autotuneConfig.getLevel());
			layersArray.put(layerJson);
		}
		jsonObject.put(DAConstants.ServiceConstants.LAYERS, layersArray);
		outputJsonArray.put(jsonObject);
	}
}
