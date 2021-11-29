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

import com.autotune.analyzer.application.ApplicationServiceStack;
import com.autotune.analyzer.deployment.AutotuneDeployment;
import com.autotune.analyzer.k8sObjects.AutotuneConfig;
import com.autotune.analyzer.k8sObjects.AutotuneObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.autotune.analyzer.deployment.AutotuneDeployment.applicationServiceStackMap;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;
import static com.autotune.analyzer.utils.AnalyzerErrorConstants.AutotuneServiceMessages.*;
import static com.autotune.analyzer.utils.ServiceHelpers.*;

public class ListAppTunables extends HttpServlet
{
	/**
	 * Returns the list of applications monitored by autotune along with their tunables
	 *
	 * Request:
	 * `GET /listAppTunables` gives the tunables and layer information for all the applications monitored by autotune.
	 *
	 * `GET /listAppTunables?experiment_name=<EXP_NAME>` for getting the tunables information of a specific application.
	 *
	 * `GET /listAppTunables?experiment_name=<EXP_NAME>&layer_name='<LAYER>' for getting tunables of a specific layer for the application.
	 *
	 * Example JSON:
	 * {
	 *       "experiment_name": "app1_autotune",
	 *       “objective_function”: “transaction_response_time”,
	 *       "slo_class": "response_time",
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
		resp.setContentType(JSON_CONTENT_TYPE);

		// Check if there are any experiments running at all ?
		if (AutotuneDeployment.autotuneObjectMap.isEmpty()) {
			outputJsonArray.put(AUTOTUNE_OBJECTS_NOT_FOUND);
			resp.getWriter().println(outputJsonArray.toString(4));
			return;
		}

		String experimentName = req.getParameter(AnalyzerConstants.ServiceConstants.EXPERIMENT_NAME);
		String layerName = req.getParameter(AnalyzerConstants.AutotuneConfigConstants.LAYER_NAME);
		String sloClass = req.getParameter(AnalyzerConstants.AutotuneObjectConstants.SLO_CLASS);

		// If experiment name is not null, try to find it in the hashmap
		if (experimentName != null) {
			AutotuneObject autotuneObject = AutotuneDeployment.autotuneObjectMap.get(experimentName);
			if (autotuneObject != null) {
				addAppLayersToResponse(outputJsonArray, experimentName, autotuneObject, layerName, sloClass);
			}
		} else {
			// Print all the experiments
			for (String autotuneObjectKey : AutotuneDeployment.autotuneObjectMap.keySet()) {
				AutotuneObject autotuneObject = AutotuneDeployment.autotuneObjectMap.get(autotuneObjectKey);
				addAppLayersToResponse(outputJsonArray, autotuneObjectKey, autotuneObject, layerName, sloClass);
			}
		}

		if (outputJsonArray.isEmpty()) {
			outputJsonArray.put(ERROR_EXPERIMENT_NAME + experimentName + NOT_FOUND);
		}
		resp.getWriter().println(outputJsonArray.toString(4));
	}

	private void addAppLayersToResponse(JSONArray outputJsonArray, String autotuneObjectKey, AutotuneObject autotuneObject, String layerName, String sloClass) {
		JSONObject experimentJson = new JSONObject();
		addExperimentDetails(experimentJson, autotuneObject);
		addFunctionVariablesDetails(experimentJson, autotuneObject);

		JSONArray stackArray = new JSONArray();
		for (String containerImageName : applicationServiceStackMap.get(autotuneObjectKey).keySet()) {
			JSONObject stackJson = new JSONObject();
			stackJson.put(AnalyzerConstants.ServiceConstants.STACK_NAME, containerImageName);
			JSONArray layersArray = new JSONArray();
			ApplicationServiceStack applicationServiceStack = applicationServiceStackMap.get(autotuneObjectKey).get(containerImageName);
			if (layerName != null) {
				if (applicationServiceStack.getApplicationServiceStackLayers().containsKey(layerName)) {
					addLayersAndTunablesToResponse(layersArray, applicationServiceStack, layerName, sloClass);
				}
			} else {
				for (String layer : applicationServiceStack.getApplicationServiceStackLayers().keySet()) {
					addLayersAndTunablesToResponse(layersArray, applicationServiceStack, layer, sloClass);
				}
			}
			stackJson.put(AnalyzerConstants.ServiceConstants.LAYERS, layersArray);
			stackArray.put(stackJson);
		}
		experimentJson.put(AnalyzerConstants.ServiceConstants.STACKS, stackArray);

		/*
		if (layersArray.isEmpty()) {
			// No autotuneconfig objects currently being monitored.
			if (layerName == null)
				outputJsonArray.put(LAYER_NOT_FOUND);
			else
				outputJsonArray.put(ERROR_LAYER + layerName + NOT_FOUND);
			return;
		}
		*/
		outputJsonArray.put(experimentJson);
	}

	private void addLayersAndTunablesToResponse(JSONArray layersArray, ApplicationServiceStack applicationServiceStack, String layerName, String sloClass) {
		JSONObject layerJson = new JSONObject();
		AutotuneConfig autotuneConfig = applicationServiceStack.getApplicationServiceStackLayers().get(layerName);
		addLayerDetails(layerJson, autotuneConfig);
		JSONArray tunablesArray = new JSONArray();
		addLayerTunableDetails(tunablesArray, autotuneConfig, sloClass);
		if (!tunablesArray.isEmpty()) {
			layerJson.put(AnalyzerConstants.AutotuneConfigConstants.TUNABLES, tunablesArray);
			layersArray.put(layerJson);
		}
	}
}
