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
import static com.autotune.analyzer.utils.ServiceHelpers.*;

public class ListStackTunables extends HttpServlet {
    /**
     * Returns the list of application stacks monitored by autotune along with their tunables
     * <p>
     * Request:
     * `GET /listStackTunables` gives the tunables and layer information for all the application stacks monitored by autotune.
     * <p>
     * `GET /listStackTunables?experiment_name=<EXP_NAME>` for getting the tunables information of a specific application stack/
     * <p>
     * `GET /listStackTunables?experiment_name=<EXP_NAME>&layer_name='<LAYER>' for getting tunables of a specific layer for the application stack.
     * <p>
     * Example JSON:
     * {
     * {
     * "experiment_name": "galaxies-autotune-min-http-response-time",
     * "experiment_id": "3bc579e7b1c29eb547809348c2a452e96cfd9ed9d3489d644f5fa4d3aeaa3c9f",
     * "objective_function":
     *  {
     *  "type": "source"
     *  },
     * "hpo_algo_impl": "optuna_tpe",
     * "stacks": [{
     * "layers": [
     * {
     * "layer_id": "af07fd998199bf2d57f95dc18f2cc2311b72f6de11e7e949b566fcdc5ecb443b",
     * "layer_level": 0,
     * "tunables": [
     * {
     * "value_type": "double",
     * "lower_bound": "150.0Mi",
     * "name": "memoryRequest",
     * "step": 1,
     * "query_url": "http://10.111.106.208:9090/api/v1/query?query=container_memory_working_set_bytes{container=\"\", pod=\"dinogun/galaxies:1.2-jdk-11.0.10_9\"}",
     * "upper_bound": "300.0Mi"
     * },
     * {
     * "value_type": "double",
     * "lower_bound": "1.0",
     * "name": "cpuRequest",
     * "step": 0.01,
     * "query_url": "http://10.111.106.208:9090/api/v1/query?query=(container_cpu_usage_seconds_total{container!=\"POD\", pod=\"dinogun/galaxies:1.2-jdk-11.0.10_9\"}[1m])",
     * "upper_bound": "3.0"
     * }
     * ],
     * "layer_name": "container",
     * "layer_details": "generic container tunables"
     * },
     * {
     * "layer_id": "63f4bd430913abffaa6c41a5e05015d5fea23134c99826470c904a7cfe56b40c",
     * "layer_level": 1,
     * "tunables": [{
     * "value_type": "integer",
     * "lower_bound": "9",
     * "name": "MaxInlineLevel",
     * "step": 1,
     * "query_url": "http://10.111.106.208:9090/api/v1/query?query=jvm_memory_used_bytes{area=\"heap\", container=\"\", pod=\"dinogun/galaxies:1.2-jdk-11.0.10_9\"}",
     * "upper_bound": "50"
     * }],
     * "layer_name": "hotspot",
     * "layer_details": "hotspot tunables"
     * },
     * {
     * "layer_id": "3ec648860dd10049b2488f19ca6d80fc5b50acccdf4aafaedc2316c6eea66741",
     * "layer_level": 2,
     * "tunables": [
     * {
     * "value_type": "integer",
     * "lower_bound": "1",
     * "name": "quarkus.thread-pool.core-threads",
     * "step": 1,
     * "query_url": "none",
     * "upper_bound": "10"
     * },
     * {
     * "value_type": "integer",
     * "lower_bound": "1",
     * "name": "quarkus.thread-pool.queue-size",
     * "step": 1,
     * "query_url": "none",
     * "upper_bound": "100"
     * },
     * {
     * "value_type": "integer",
     * "lower_bound": "1",
     * "name": "quarkus.hibernate-orm.jdbc.statement-fetch-size",
     * "step": 1,
     * "query_url": "none",
     * "upper_bound": "50"
     * }
     * ],
     * "layer_name": "quarkus",
     * "layer_details": "quarkus tunables"
     * }
     * ],
     * "stack_name": "dinogun/galaxies:1.2-jdk-11.0.10_9"
     * }],
     * "function_variables": [
     * {
     * "value_type": "double",
     * "name": "request_sum",
     * "query_url": "http://10.111.106.208:9090/api/v1/query?query=rate(http_server_requests_seconds_sum{method=\"GET\",outcome=\"SUCCESS\",status=\"200\",uri=\"/galaxies\",}[1m])"
     * },
     * {
     * "value_type": "double",
     * "name": "request_count",
     * "query_url": "http://10.111.106.208:9090/api/v1/query?query=rate(http_server_requests_seconds_count{method=\"GET\",outcome=\"SUCCESS\",status=\"200\",uri=\"/galaxies\",}[1m])"
     * }
     * ],
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
        String sloClass = request.getParameter(AnalyzerConstants.AutotuneObjectConstants.SLO_CLASS);

        // If experiment name is not null, try to find it in the hashmap
        if (experimentName != null) {
            KruizeObject kruizeObject = KruizeDeployment.autotuneObjectMap.get(experimentName);
            if (kruizeObject != null) {
                addAppLayersToResponse(outputJsonArray, experimentName, kruizeObject, layerName, sloClass);
            }
        } else {
            // Print all the experiments
            for (String autotuneObjectKey : KruizeDeployment.autotuneObjectMap.keySet()) {
                KruizeObject kruizeObject = KruizeDeployment.autotuneObjectMap.get(autotuneObjectKey);
                addAppLayersToResponse(outputJsonArray, autotuneObjectKey, kruizeObject, layerName, sloClass);
            }
        }

        if (outputJsonArray.isEmpty()) {
            outputJsonArray.put(ERROR_EXPERIMENT_NAME + experimentName + NOT_FOUND);
        }
        response.getWriter().println(outputJsonArray.toString(4));

    }

    private void addAppLayersToResponse(JSONArray outputJsonArray, String autotuneObjectKey, KruizeObject kruizeObject, String layerName, String sloClass) {
        JSONObject experimentJson = new JSONObject();
        addExperimentDetails(experimentJson, kruizeObject);
        addFunctionVariablesDetails(experimentJson, kruizeObject);

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
                            addLayersAndTunablesToResponse(layersArray, applicationServiceStack, layerName, sloClass);
                        }
                    } else {
                        if (!applicationServiceStack.getApplicationServiceStackLayers().isEmpty()) {
                            for (String layer : applicationServiceStack.getApplicationServiceStackLayers().keySet()) {
                                addLayersAndTunablesToResponse(layersArray, applicationServiceStack, layer, sloClass);
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
