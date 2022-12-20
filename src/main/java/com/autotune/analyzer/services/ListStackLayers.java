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

import com.autotune.analyzer.application.ApplicationDeployment;
import com.autotune.analyzer.application.ApplicationServiceStack;
import com.autotune.analyzer.deployment.KruizeDeployment;
import com.autotune.common.k8sObjects.AutotuneConfig;
import com.autotune.common.k8sObjects.KruizeObject;
import com.autotune.utils.AnalyzerConstants;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.autotune.analyzer.deployment.KruizeDeployment.deploymentMap;
import static com.autotune.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;
import static com.autotune.utils.AnalyzerErrorConstants.AutotuneServiceMessages.*;
import static com.autotune.analyzer.utils.ServiceHelpers.addExperimentDetails;
import static com.autotune.analyzer.utils.ServiceHelpers.addLayerDetails;

public class ListStackLayers extends HttpServlet {
    /**
     * Returns the list of application stacks monitored by autotune along with layers detected in the applications.
     * <p>
     * Request:
     * `GET /listStackLayers` returns the list of all application stacks monitored by autotune, and their layers.
     * <p>
     * `GET /listStackLayers?experiment_name=<EXP_NAME>` returns the layers detected for the pods specific to the given experiment.
     * <p>
     * Example JSON:
     * [
     * {
     * "experiment_name": "autotune-max-http-throughput",
     * "experiment_id": "94f76772f43339f860e0d5aad8bebc1abf50f461712d4c5d14ea7aada280e8f3",
     * "objective_function": "request_count",
     * "hpo_algo_impl": "optuna_tpe",
     * "deployments": [
     * {
     * "name": "autotune",
     * "namespace": "monitoring",
     * "stacks": [
     * {
     * "layers": [{
     * "layer_id": "af07fd998199bf2d57f95dc18f2cc2311b72f6de11e7e949b566fcdc5ecb443b",
     * "layer_level": 0,
     * "layer_name": "container",
     * "layer_details": "generic container tunables"
     * }],
     * "stack_name": "dinogun/hpo:0.0.5"
     * },
     * {
     * "layers": [{
     * "layer_id": "af07fd998199bf2d57f95dc18f2cc2311b72f6de11e7e949b566fcdc5ecb443b",
     * "layer_level": 0,
     * "layer_name": "container",
     * "layer_details": "generic container tunables"
     * }],
     * "stack_name": "dinogun/autotune_operator:0.0.5"
     * }
     * ]
     * }
     * ]
     * "slo_class": "throughput",
     * "direction": "maximize"
     * },
     * {
     * "experiment_name": "galaxies-autotune-min-http-response-time",
     * "experiment_id": "3bc579e7b1c29eb547809348c2a452e96cfd9ed9d3489d644f5fa4d3aeaa3c9f",
     * "objective_function": "request_sum/request_count",
     * "hpo_algo_impl": "optuna_tpe",
     * "deployments": [
     * {
     * "name": "autotune",
     * "namespace": "monitoring",
     * "stacks": [{
     * "layers": [
     * {
     * "layer_id": "af07fd998199bf2d57f95dc18f2cc2311b72f6de11e7e949b566fcdc5ecb443b",
     * "layer_level": 0,
     * "layer_name": "container",
     * "layer_details": "generic container tunables"
     * },
     * {
     * "layer_id": "63f4bd430913abffaa6c41a5e05015d5fea23134c99826470c904a7cfe56b40c",
     * "layer_level": 1,
     * "layer_name": "hotspot",
     * "layer_details": "hotspot tunables"
     * },
     * {
     * "layer_id": "3ec648860dd10049b2488f19ca6d80fc5b50acccdf4aafaedc2316c6eea66741",
     * "layer_level": 2,
     * "layer_name": "quarkus",
     * "layer_details": "quarkus tunables"
     * }
     * ],
     * "stack_name": "dinogun/galaxies:1.2-jdk-11.0.10_9"
     * }]
     * }
     * ]
     * "slo_class": "response_time",
     * "direction": "minimize"
     * }
     * ]
     *
     * @param request
     * @param response
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);

        JSONArray outputJsonArray = new JSONArray();
        // Check if there are any experiments running at all ?
        if (KruizeDeployment.autotuneObjectMap.isEmpty()) {
            outputJsonArray.put(AUTOTUNE_OBJECTS_NOT_FOUND);
            response.getWriter().println(outputJsonArray.toString(4));
            return;
        }

        String experimentName = request.getParameter(AnalyzerConstants.ServiceConstants.EXPERIMENT_NAME);
        String layerName = request.getParameter(AnalyzerConstants.AutotuneConfigConstants.LAYER_NAME);
        // If experiment name is not null, try to find it in the hashmap
        if (experimentName != null) {
            KruizeObject kruizeObject = KruizeDeployment.autotuneObjectMap.get(experimentName);
            if (kruizeObject != null) {
                addAppLayersToResponse(outputJsonArray, experimentName, kruizeObject, layerName);
            }
        } else {
            // Print all the experiments
            for (String autotuneObjectKey : KruizeDeployment.autotuneObjectMap.keySet()) {
                KruizeObject kruizeObject = KruizeDeployment.autotuneObjectMap.get(autotuneObjectKey);
                addAppLayersToResponse(outputJsonArray, autotuneObjectKey, kruizeObject, layerName);
            }
        }

        if (outputJsonArray.isEmpty()) {
            outputJsonArray.put(ERROR_EXPERIMENT_NAME + experimentName + NOT_FOUND);
        }
        response.getWriter().println(outputJsonArray.toString(4));

    }

    private void addAppLayersToResponse(JSONArray outputJsonArray, String autotuneObjectKey, KruizeObject kruizeObject, String layerName) {
        JSONObject experimentJson = new JSONObject();
        addExperimentDetails(experimentJson, kruizeObject);

        JSONArray deploymentArray = new JSONArray();
        if (KruizeDeployment.autotuneObjectMap.isEmpty()
                || KruizeDeployment.autotuneObjectMap.get(autotuneObjectKey) == null) {
            experimentJson.put(AnalyzerConstants.ServiceConstants.DEPLOYMENTS, deploymentArray);
            outputJsonArray.put(experimentJson);
            return;
        }

        for (String deploymentName : deploymentMap.get(autotuneObjectKey).keySet()) {
            JSONObject deploymentJson = new JSONObject();
            ApplicationDeployment applicationDeployment = deploymentMap.get(autotuneObjectKey).get(deploymentName);
            deploymentJson.put(AnalyzerConstants.ServiceConstants.DEPLOYMENT_NAME, applicationDeployment.getDeploymentName());
            deploymentJson.put(AnalyzerConstants.ServiceConstants.NAMESPACE, applicationDeployment.getNamespace());
            JSONArray stackArray = new JSONArray();
            if (!applicationDeployment.getApplicationServiceStackMap().isEmpty()) {
                for (String stackName : applicationDeployment.getApplicationServiceStackMap().keySet()) {
                    ApplicationServiceStack applicationServiceStack = applicationDeployment.getApplicationServiceStackMap().get(stackName);
                    JSONObject stackJson = new JSONObject();
                    stackJson.put(AnalyzerConstants.ServiceConstants.STACK_NAME, stackName);
                    stackJson.put(AnalyzerConstants.ServiceConstants.CONTAINER_NAME, applicationServiceStack.getContainerName());
                    JSONArray layersArray = new JSONArray();
                    if (layerName != null) {
                        if (applicationServiceStack.getApplicationServiceStackLayers().containsKey(layerName)) {
                            JSONObject layerJson = new JSONObject();
                            AutotuneConfig autotuneConfig = applicationServiceStack.getApplicationServiceStackLayers().get(layerName);
                            addLayerDetails(layerJson, autotuneConfig);
                            layersArray.put(layerJson);
                        }
                    } else {
                        if (!applicationServiceStack.getApplicationServiceStackLayers().keySet().isEmpty()) {
                            for (String layer : applicationServiceStack.getApplicationServiceStackLayers().keySet()) {
                                JSONObject layerJson = new JSONObject();
                                AutotuneConfig autotuneConfig = applicationServiceStack.getApplicationServiceStackLayers().get(layer);
                                addLayerDetails(layerJson, autotuneConfig);
                                layersArray.put(layerJson);
                            }
                        }
                    }
                    stackJson.put(AnalyzerConstants.ServiceConstants.LAYERS, layersArray);
                    stackArray.put(stackJson);
                }
            }
            deploymentJson.put(AnalyzerConstants.ServiceConstants.STACKS, stackArray);
            deploymentArray.put(deploymentJson);
        }

        experimentJson.put(AnalyzerConstants.ServiceConstants.DEPLOYMENTS, deploymentArray);
        outputJsonArray.put(experimentJson);
    }
}
