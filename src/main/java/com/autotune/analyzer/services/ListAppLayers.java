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

public class ListAppLayers extends HttpServlet {
    /**
     * Returns the list of applications monitored by autotune along with layers detected in the applications.
     * <p>
     * Request:
     * `GET /listAppLayers` returns the list of all applications monitored by autotune, and their layers.
     * <p>
     * `GET /listAppLayers?experiment_name=<EXP_NAME>` returns the layers detected for the pods specific to the given experiment.
     * <p>
     * Example JSON:
     * [
     * {
     * "experiment_name": "app1_autotune",
     * “objective_function”: “transaction_response_time”,
     * "slo_class": "response_time",
     * “direction”: “minimize”
     * "layers": [
     * {
     * "layer_level": 0,
     * "layer_name": "container",
     * "layer_details": "generic container tunables"
     * },
     * {
     * "layer_level": 1,
     * "layer_name": "openj9",
     * "layer_details": "java openj9 tunables"
     * }
     * ]
     * },
     * {
     * "experiment_name": "app2_autotune",
     * “objective_function”: “performedChecks_total”,
     * "slo_class": "throughput",
     * “direction”: “maximize”
     * "layers": [
     * {
     * "layer_level": 0,
     * "layer_name": "container",
     * "layer_details": "generic container tunables"
     * },
     * {
     * "layer_level": 1,
     * "layer_name": "hotspot",
     * "layer_details": "java hotspot tunables"
     * }
     * ]
     * }
     * ]
     *
     * @param req
     * @param resp
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JSONArray outputJsonArray = new JSONArray();
        resp.setContentType("application/json");

        String experimentName = req.getParameter(AnalyzerConstants.ServiceConstants.EXPERIMENT_NAME);

        for (String autotuneObjectKey : AutotuneDeployment.autotuneObjectMap.keySet()) {
            AutotuneObject autotuneObject = AutotuneDeployment.autotuneObjectMap.get(autotuneObjectKey);

            if (experimentName == null) {
                addLayersToResponse(outputJsonArray, autotuneObjectKey, autotuneObject);
            } else {
                if (autotuneObject.getExperimentName().equals(experimentName)) {
                    addLayersToResponse(outputJsonArray, autotuneObjectKey, autotuneObject);
                }
            }
        }

        if (outputJsonArray.isEmpty()) {
            if (AutotuneDeployment.autotuneObjectMap.isEmpty())
                outputJsonArray.put("Error: No objects of kind Autotune found!");
            else
                outputJsonArray.put("Error: Experiment Name " + experimentName + " not found!");
        }

        resp.getWriter().println(outputJsonArray.toString(4));
    }

    private void addLayersToResponse(JSONArray outputJsonArray, String autotuneObjectKey, AutotuneObject autotuneObject) {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put(AnalyzerConstants.ServiceConstants.EXPERIMENT_NAME, autotuneObject.getExperimentName());
        jsonObject.put(AnalyzerConstants.AutotuneObjectConstants.DIRECTION, autotuneObject.getSloInfo().getDirection());
        jsonObject.put(AnalyzerConstants.AutotuneObjectConstants.OBJECTIVE_FUNCTION, autotuneObject.getSloInfo().getObjectiveFunction());
        jsonObject.put(AnalyzerConstants.AutotuneObjectConstants.SLO_CLASS, autotuneObject.getSloInfo().getSloClass());
        jsonObject.put(AnalyzerConstants.AutotuneObjectConstants.ID, autotuneObject.getExperimentId());
        jsonObject.put(AnalyzerConstants.AutotuneObjectConstants.HPO_ALGO_IMPL, autotuneObject.getSloInfo().getHpoAlgoImpl());

        JSONArray layersArray = new JSONArray();
        for (String applicationServiceStackName : applicationServiceStackMap.get(autotuneObjectKey).keySet()) {
            ApplicationServiceStack applicationServiceStack = applicationServiceStackMap.get(autotuneObjectKey).get(applicationServiceStackName);
            for (String layer : applicationServiceStack.getApplicationServiceStackLayers().keySet()) {
                AutotuneConfig autotuneConfig = applicationServiceStack.getApplicationServiceStackLayers().get(layer);
                JSONObject layerJson = new JSONObject();
                layerJson.put(AnalyzerConstants.AutotuneConfigConstants.LAYER_NAME, autotuneConfig.getLayerName());
                layerJson.put(AnalyzerConstants.AutotuneConfigConstants.LAYER_DETAILS, autotuneConfig.getDetails());
                layerJson.put(AnalyzerConstants.AutotuneConfigConstants.LAYER_LEVEL, autotuneConfig.getLevel());
                layerJson.put(AnalyzerConstants.AutotuneConfigConstants.ID, autotuneConfig.getLayerId());
                layersArray.put(layerJson);
            }
        }
        jsonObject.put(AnalyzerConstants.ServiceConstants.LAYERS, layersArray);
        outputJsonArray.put(jsonObject);
    }
}
