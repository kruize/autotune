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

import com.autotune.analyzer.deployment.AutotuneDeployment;
import com.autotune.analyzer.k8sObjects.AutotuneConfig;
import com.autotune.analyzer.utils.AnalyzerConstants;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;
import static com.autotune.analyzer.utils.AnalyzerErrorConstants.AutotuneServiceMessages.*;
import static com.autotune.analyzer.utils.ServiceHelpers.addLayerDetails;
import static com.autotune.analyzer.utils.ServiceHelpers.addLayerTunableDetails;

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
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(JSON_CONTENT_TYPE);
			response.setCharacterEncoding(CHARACTER_ENCODING);

			JSONArray layersArray = new JSONArray();
			if (AutotuneDeployment.autotuneConfigMap.isEmpty()) {
				layersArray.put(LAYER_NOT_FOUND);
				response.getWriter().println(layersArray.toString(4));
				return;
			}

			String sloClass = request.getParameter(AnalyzerConstants.AutotuneObjectConstants.SLO_CLASS);
			String layerName = request.getParameter(AnalyzerConstants.AutotuneConfigConstants.LAYER_NAME);
			AutotuneConfig autotuneConfig = null;

			if (layerName != null) {
				if (AutotuneDeployment.autotuneConfigMap.containsKey(layerName)) {
					JSONObject layerJson = new JSONObject();
					autotuneConfig = AutotuneDeployment.autotuneConfigMap.get(layerName);
					addLayerDetails(layerJson, autotuneConfig);
					JSONArray tunablesArray = new JSONArray();
					addLayerTunableDetails(tunablesArray, autotuneConfig, sloClass);
					if (!tunablesArray.isEmpty()) {
						layerJson.put(AnalyzerConstants.AutotuneConfigConstants.TUNABLES, tunablesArray);
						layersArray.put(layerJson);
					} else {
						if (sloClass != null) {
							layersArray.put(ERROR_SLO_CLASS + sloClass + NOT_FOUND);
						}
					}
				} else {
					layersArray.put(LAYER_NOT_FOUND);
				}
			} else {
				// No layer parameter was passed in the request
				for (String layer : AutotuneDeployment.autotuneConfigMap.keySet()) {
					JSONObject layerJson = new JSONObject();
					autotuneConfig = AutotuneDeployment.autotuneConfigMap.get(layer);
					addLayerDetails(layerJson, autotuneConfig);
					JSONArray tunablesArray = new JSONArray();
					addLayerTunableDetails(tunablesArray, autotuneConfig, sloClass);
					if (!tunablesArray.isEmpty()) {
						layerJson.put(AnalyzerConstants.AutotuneConfigConstants.TUNABLES, tunablesArray);
						layersArray.put(layerJson);
					}
				}
			}
			response.getWriter().println(layersArray.toString(4));
		}  catch (Exception ex) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
}
