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
import com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment;
import com.autotune.dependencyAnalyzer.k8sObjects.AutotuneConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ListAutotuneTunables extends HttpServlet
{
	/**
	 * Get the tunables supported by autotune for the SLA.
	 *
	 * Request:
	 * `GET /listAutotuneTunables` gives all tunables for all layers in the cluster
	 *
	 * `GET /listAutotuneTunables?sla_class=<SLA_CLASS>` gives all tunables for the SLA class
	 *
	 * `GET /listAutotuneTunables?sla_class=<SLA_CLASS>&layer=<LAYER>` gives tunables for the SLA class and the layer
	 *
	 * Example JSON:
	 * [
	 *   {
	 *     "layer_name": "container",
	 *     "layer_level": 0,
	 *     "layer_details": "generic container tunables",
	 *     "tunables": [
	 *       {
	 *         "name": "memoryLimit",
	 *         "value_type": "double",
	 *         "lower_bound": "150M",
	 *         "upper_bound": "300M"
	 *       },
	 *       {
	 *         "name": "memoryRequests",
	 *         "value_type": "double",
	 *         "lower_bound": "150M",
	 *         "upper_bound": "300M"
	 *       },
	 *       {
	 *         "name": "cpuLimit",
	 *         "value_type": "double",
	 *         "lower_bound": "2.0",
	 *         "upper_bound": "4.0"
	 *       },
	 *       {
	 *         "name": "cpuRequest",
	 *         "value_type": "double",
	 *         "lower_bound": "1.0",
	 *         "upper_bound": "3.0"
	 *       }
	 *     ]
	 *   },
	 *   {
	 *     "layer_name": "openj9",
	 *     "layer_level": 1,
	 *     "layer_details": "java openj9 tunables",
	 *     "tunables": [
	 *       {
	 *         "name": "javaHeap",
	 *         "value_type": "double",
	 *         "lower_bound": "100M",
	 *         "upper_bound": "250M"
	 *       }
	 *     ]
	 *   }
	 * ]
	 * @param req
	 * @param resp
	 * @throws ServletException
	 * @throws IOException
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		JSONArray outputJsonArray = new JSONArray();
		resp.setContentType("application/json");

		String slaClass = req.getParameter("sla_class");
		String layerName = req.getParameter("layer_name");

		//No layer parameter was passed in the request
		if (layerName == null) {
			for (String autotuneConfigName : AutotuneDeployment.autotuneConfigMap.keySet()) {
				addLayerTunablesToResponse(outputJsonArray, slaClass, autotuneConfigName);
			}
		} else {
			addLayerTunablesToResponse(outputJsonArray, slaClass, layerName);
		}
		resp.getWriter().println(outputJsonArray.toString(4));
	}

	private void addLayerTunablesToResponse(JSONArray outputJsonArray, String slaClass, String autotuneConfigName) {
		AutotuneConfig autotuneConfig;
		JSONObject autotuneConfigJson = new JSONObject();

		if (AutotuneDeployment.autotuneConfigMap.containsKey(autotuneConfigName))
			autotuneConfig = AutotuneDeployment.autotuneConfigMap.get(autotuneConfigName);
		else
			return;

		autotuneConfigJson.put("layer_name", autotuneConfig.getName());
		autotuneConfigJson.put("layer_level", autotuneConfig.getLevel());
		autotuneConfigJson.put("layer_details", autotuneConfig.getDetails());

		JSONArray tunablesArray = new JSONArray();
		for (Tunable tunable : autotuneConfig.getTunables()) {
			//If no slaClass parameter was passed in the request, or if the argument matches the slaClassList for the Tunable
			if (slaClass == null || tunable.slaClassList.contains(slaClass)) {
				JSONObject tunablesJson = new JSONObject();
				tunablesJson.put("name", tunable.getName());
				tunablesJson.put("value_type", tunable.getValueType());
				tunablesJson.put("lower_bound", tunable.getLowerBound());
				tunablesJson.put("upper_bound", tunable.getUpperBound());

				tunablesArray.put(tunablesJson);
			}
		}
		autotuneConfigJson.put("tunables", tunablesArray);
		outputJsonArray.put(autotuneConfigJson);
	}
}
