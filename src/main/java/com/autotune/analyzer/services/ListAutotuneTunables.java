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
package com.autotune.analyzer.services;

import com.autotune.analyzer.application.Tunable;
import com.autotune.analyzer.deployment.AutotuneDeployment;
import com.autotune.analyzer.k8sObjects.AutotuneConfig;
import com.autotune.analyzer.utils.AnalyzerConstants;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ListAutotuneTunables extends HttpServlet
{
	/**
	 * Get the tunables supported by autotune for the SLO.
	 *
	 * Request:
	 * `GET /listAutotuneTunables` gives all tunables for all layers in the cluster
	 *
	 * `GET /listAutotuneTunables?slo_class=<SLO_CLASS>` gives all tunables for the SLO class
	 *
	 * `GET /listAutotuneTunables?slo_class=<SLO_CLASS>&layer=<LAYER>` gives tunables for the SLO class and the layer
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
	 * @throws IOException
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		JSONArray outputJsonArray = new JSONArray();
		resp.setContentType("application/json");

		String sloClass = req.getParameter(AnalyzerConstants.AutotuneObjectConstants.SLO_CLASS);
		String layerName = req.getParameter(AnalyzerConstants.AutotuneConfigConstants.LAYER_NAME);

		//No layer parameter was passed in the request
		if (layerName == null) {
			if (!AutotuneDeployment.autotuneConfigMap.isEmpty()) {
				for (String autotuneConfigName : AutotuneDeployment.autotuneConfigMap.keySet()) {
					addLayerTunablesToResponse(outputJsonArray, sloClass, autotuneConfigName);
				}
			} else {
				outputJsonArray.put("Error: No AutotuneConfig objects found!");
			}
		} else {
			addLayerTunablesToResponse(outputJsonArray, sloClass, layerName);
		}
		resp.getWriter().println(outputJsonArray.toString(4));
	}

	private void addLayerTunablesToResponse(JSONArray outputJsonArray, String sloClass, String autotuneConfigName) {
		AutotuneConfig autotuneConfig;
		JSONObject autotuneConfigJson = new JSONObject();

		if (AutotuneDeployment.autotuneConfigMap.containsKey(autotuneConfigName)) {
			autotuneConfig = AutotuneDeployment.autotuneConfigMap.get(autotuneConfigName);
		} else {
			outputJsonArray.put("Error: AutotuneConfig " + autotuneConfigName + " not found!");
			return;
		}

		autotuneConfigJson.put(AnalyzerConstants.AutotuneConfigConstants.ID, autotuneConfig.getId());
		autotuneConfigJson.put(AnalyzerConstants.AutotuneConfigConstants.LAYER_NAME, autotuneConfig.getLayerName());
		autotuneConfigJson.put(AnalyzerConstants.AutotuneConfigConstants.LAYER_LEVEL, autotuneConfig.getLevel());
		autotuneConfigJson.put(AnalyzerConstants.AutotuneConfigConstants.LAYER_DETAILS, autotuneConfig.getDetails());

		JSONArray tunablesArray = new JSONArray();
		for (Tunable tunable : autotuneConfig.getTunables()) {
			//If no sloClass parameter was passed in the request, or if the argument matches the sloClassList for the Tunable
			if (sloClass == null || tunable.sloClassList.contains(sloClass)) {
				JSONObject tunablesJson = new JSONObject();
				tunablesJson.put(AnalyzerConstants.AutotuneConfigConstants.NAME, tunable.getName());
				tunablesJson.put(AnalyzerConstants.AutotuneConfigConstants.VALUE_TYPE, tunable.getValueType());
				tunablesJson.put(AnalyzerConstants.AutotuneConfigConstants.LOWER_BOUND, tunable.getLowerBound());
				tunablesJson.put(AnalyzerConstants.AutotuneConfigConstants.UPPER_BOUND, tunable.getUpperBound());
				tunablesJson.put(AnalyzerConstants.AutotuneConfigConstants.STEP, tunable.getStep());

				tunablesArray.put(tunablesJson);
			}
		}
		if (tunablesArray.isEmpty()) {
			outputJsonArray.put("Error: Tunables matching slo_class " + sloClass + " not found");
			return;
		}
		autotuneConfigJson.put(AnalyzerConstants.AutotuneConfigConstants.TUNABLES, tunablesArray);
		outputJsonArray.put(autotuneConfigJson);
	}
}
