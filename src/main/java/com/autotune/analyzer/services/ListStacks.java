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

import com.autotune.operator.KruizeOperator;
import com.autotune.common.k8sObjects.KruizeObject;
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
import static com.autotune.analyzer.utils.ServiceHelpers.addExperimentDetails;
import static com.autotune.analyzer.utils.ServiceHelpers.addDeploymentDetails;

public class ListStacks extends HttpServlet {
    /**
     * Get the list of applications monitored by autotune.
     * <p>
     * Request:
     * `GET /listStacks` gives list of application stacks monitored by autotune.
     * <p>
     * Example JSON:
     * [
     * {
     * "experiment_name": "autotune-max-http-throughput",
     * "experiment_id": "94f76772f43339f860e0d5aad8bebc1abf50f461712d4c5d14ea7aada280e8f3",
     * "objective_function": {
     * "function_type": "expression",
     * "expression": "request_sum/request_count"
     *  },
     * "slo_class": "throughput",
     * "direction": "maximize",
     * "hpo_algo_impl": "optuna_tpe",
     * "deployments": [
     * {
     * "name": "autotune",
     * "namespace": "monitoring",
     * "stacks": [
     * "dinogun/hpo:0.0.5",
     * "dinogun/autotune_operator:0.0.5"
     * ]
     * }
     * ]
     * },
     * {
     * "experiment_name": "galaxies-autotune-min-http-response-time",
     * "experiment_id": "3bc579e7b1c29eb547809348c2a452e96cfd9ed9d3489d644f5fa4d3aeaa3c9f",
     * "objective_function":
     *  {
     *  "function_type": "expression",
     *  "expression": "request_count"
     *  },
     * "slo_class": "response_time",
     * "direction": "minimize",
     * "hpo_algo_impl": "optuna_tpe",
     * "deployments": [
     * {
     * "name": "galaxies-sample",
     * "namespace": "default",
     * "stacks": ["dinogun/galaxies:1.2-jdk-11.0.10_9"]
     * }
     * ]
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
        String experimentName = request.getParameter(AnalyzerConstants.ServiceConstants.EXPERIMENT_NAME);
        // If experiment name is not null, try to find it in the hashmap
        if (experimentName != null) {
            KruizeObject kruizeObject = KruizeOperator.autotuneObjectMap.get(experimentName);
            if (kruizeObject != null) {
                JSONObject experimentJson = new JSONObject();
                addExperimentDetails(experimentJson, kruizeObject);
                addDeploymentDetails(experimentJson, kruizeObject);
                outputJsonArray.put(experimentJson);
            }
        } else {
            // Print all the experiments
            for (String autotuneObjectKey : KruizeOperator.autotuneObjectMap.keySet()) {
                KruizeObject kruizeObject = KruizeOperator.autotuneObjectMap.get(autotuneObjectKey);
                JSONObject experimentJson = new JSONObject();
                addExperimentDetails(experimentJson, kruizeObject);
                addDeploymentDetails(experimentJson, kruizeObject);
                outputJsonArray.put(experimentJson);
            }
        }
        if (outputJsonArray.isEmpty()) {
            outputJsonArray.put(ERROR_EXPERIMENT_NAME + experimentName + NOT_FOUND);
        }
        response.getWriter().println(outputJsonArray.toString(4));
    }
}
