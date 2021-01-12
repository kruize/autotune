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
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ListAppLayers extends HttpServlet
{
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		JSONArray outputJsonArray = new JSONArray();
		resp.setContentType("application/json");

		String application_name = req.getParameter("application_name");

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
		jsonObject.put("application_name", application);
		jsonObject.put("direction", autotuneObject.getSlaInfo().getDirection());
		jsonObject.put("objective_function", autotuneObject.getSlaInfo().getObjectiveFunction());
		jsonObject.put("sla_class", autotuneObject.getSlaInfo().getSlaClass());

		JSONArray layersArray = new JSONArray();
		for (AutotuneConfig autotuneConfig : AutotuneDeployment.applicationServiceStackMap.get(autotuneObjectKey).get(application).getStackLayers()) {
			JSONObject layerJson = new JSONObject();
			layerJson.put("layer_name", autotuneConfig.getName());
			layerJson.put("layer_details", autotuneConfig.getDetails());
			layerJson.put("layer_level", autotuneConfig.getLevel());
			layersArray.put(layerJson);
		}
		jsonObject.put("layers", layersArray);
		outputJsonArray.put(jsonObject);
	}
}
