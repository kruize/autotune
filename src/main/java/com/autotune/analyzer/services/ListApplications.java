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
import com.autotune.analyzer.k8sObjects.AutotuneObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;
import static com.autotune.analyzer.utils.AnalyzerErrorConstants.AutotuneServiceMessages.*;
import static com.autotune.analyzer.utils.ServiceHelpers.addExperimentDetails;
import static com.autotune.analyzer.utils.ServiceHelpers.addStackDetails;

public class ListApplications extends HttpServlet
{
    /**
     * Get the list of applications monitored by autotune.
     *
     * Request:
     * `GET /listApplications` gives list of applications monitored by autotune.
     *
     * Example JSON:
     * [
     *     {
     *       "experiment_name": "app1_autotune",
     *       “objective_function”: “transaction_response_time”,
     *       "slo_class": "response_time",
     *       “direction”: “minimize”
     *     },
     *     {
     *       "experiment_name": "app2_autotune",
     *       “objective_function”: “performedChecks_total”,
     *       "slo_class": "throughput",
     *       “direction”: “maximize”
     *     }
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
        // If experiment name is not null, try to find it in the hashmap
        if (experimentName != null) {
            AutotuneObject autotuneObject = AutotuneDeployment.autotuneObjectMap.get(experimentName);
            if (autotuneObject != null) {
                JSONObject experimentJson = new JSONObject();
                addExperimentDetails(experimentJson, autotuneObject);
                addStackDetails(experimentJson, autotuneObject);
                outputJsonArray.put(experimentJson);
            }
        } else {
            // Print all the experiments
            for (String autotuneObjectKey : AutotuneDeployment.autotuneObjectMap.keySet()) {
                AutotuneObject autotuneObject = AutotuneDeployment.autotuneObjectMap.get(autotuneObjectKey);
                JSONObject experimentJson = new JSONObject();
                addExperimentDetails(experimentJson, autotuneObject);
                addStackDetails(experimentJson, autotuneObject);
                outputJsonArray.put(experimentJson);
            }
        }

        if (outputJsonArray.isEmpty()) {
            outputJsonArray.put(ERROR_EXPERIMENT_NAME + experimentName + NOT_FOUND);
        }
        resp.getWriter().println(outputJsonArray.toString(4));
    }
}
