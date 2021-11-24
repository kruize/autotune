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
import com.autotune.analyzer.datasource.DataSource;
import com.autotune.analyzer.datasource.DataSourceFactory;
import com.autotune.analyzer.deployment.AutotuneDeployment;
import com.autotune.analyzer.deployment.DeploymentInfo;
import com.autotune.analyzer.exceptions.MonitoringAgentNotFoundException;
import com.autotune.analyzer.k8sObjects.AutotuneConfig;
import com.autotune.analyzer.k8sObjects.AutotuneObject;
import com.autotune.analyzer.k8sObjects.FunctionVariable;
import com.autotune.analyzer.utils.AnalyzerConstants;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

import static com.autotune.analyzer.deployment.AutotuneDeployment.applicationServiceStackMap;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;
import static com.autotune.analyzer.utils.AnalyzerErrorConstants.AutotuneServiceMessages.*;
import static com.autotune.analyzer.utils.ServiceHelpers.addExperimentDetails;
import static com.autotune.analyzer.utils.ServiceHelpers.addLayerDetails;

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
        resp.setContentType(JSON_CONTENT_TYPE);

        /* Check if there are any experiments running at all ? */
        if (AutotuneDeployment.autotuneObjectMap.isEmpty()) {
            outputJsonArray.put(AUTOTUNE_OBJECTS_NOT_FOUND);
            resp.getWriter().println(outputJsonArray.toString(4));
            return;
        }

        String experimentName = req.getParameter(AnalyzerConstants.ServiceConstants.EXPERIMENT_NAME);
        String layerName = req.getParameter(AnalyzerConstants.AutotuneConfigConstants.LAYER_NAME);
        /* If experiment name is not null, try to find it in the hashmap */
        if (experimentName != null) {
            AutotuneObject autotuneObject = AutotuneDeployment.autotuneObjectMap.get(experimentName);
            if (autotuneObject != null) {
                addAppLayersToResponse(outputJsonArray, experimentName, autotuneObject, layerName);
            }
        } else {
            /* Print all the experiments */
            for (String autotuneObjectKey : AutotuneDeployment.autotuneObjectMap.keySet()) {
                AutotuneObject autotuneObject = AutotuneDeployment.autotuneObjectMap.get(autotuneObjectKey);
                addAppLayersToResponse(outputJsonArray, autotuneObjectKey, autotuneObject, layerName);
            }
        }

        if (outputJsonArray.isEmpty()) {
            outputJsonArray.put(ERROR_EXPERIMENT_NAME + experimentName + NOT_FOUND);
        }
        resp.getWriter().println(outputJsonArray.toString(4));
    }

    private void addAppLayersToResponse(JSONArray outputJsonArray, String autotuneObjectKey, AutotuneObject autotuneObject, String layerName) {
        JSONObject jsonObject = new JSONObject();
        addExperimentDetails(jsonObject, autotuneObject);

        JSONArray layersArray = new JSONArray();
        for (String applicationServiceStackName : applicationServiceStackMap.get(autotuneObjectKey).keySet()) {
            ApplicationServiceStack applicationServiceStack = applicationServiceStackMap.get(autotuneObjectKey).get(applicationServiceStackName);
            if (layerName != null) {
                if (applicationServiceStack.getApplicationServiceStackLayers().containsKey(layerName)) {
                    JSONObject layerJson = new JSONObject();
                    AutotuneConfig autotuneConfig = applicationServiceStack.getApplicationServiceStackLayers().get(layerName);
                    addLayerDetails(layerJson, autotuneConfig);
                    layersArray.put(layerJson);
                }
            } else {
                for (String layer : applicationServiceStack.getApplicationServiceStackLayers().keySet()) {
                    JSONObject layerJson = new JSONObject();
                    AutotuneConfig autotuneConfig = applicationServiceStack.getApplicationServiceStackLayers().get(layer);
                    addLayerDetails(layerJson, autotuneConfig);
                    layersArray.put(layerJson);
                }
            }
        }

        if (layersArray.isEmpty()) {
            // No autotuneconfig objects currently being monitored.
            if (layerName == null)
                outputJsonArray.put(LAYER_NOT_FOUND);
            else
                outputJsonArray.put(ERROR_LAYER + layerName + NOT_FOUND);
            return;
        }
        jsonObject.put(AnalyzerConstants.ServiceConstants.LAYERS, layersArray);
        outputJsonArray.put(jsonObject);
    }




}
