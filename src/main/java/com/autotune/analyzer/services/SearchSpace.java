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

import com.autotune.analyzer.Experimentator;
import com.autotune.analyzer.application.ApplicationServiceStack;
import com.autotune.analyzer.application.Tunable;
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

import static com.autotune.utils.SearchSpaceHelpers.getSearchSpaceJSONArray;

public class SearchSpace extends HttpServlet
{
    /**
     * Generates the search space used for the analysis.
     *
     * Request:
     * `GET /searchSpace` gives the search space for all applications monitored.
     *
     * `GET /searchSpace?application_name=<APPLICATION>` gives the search space for a specific application.
     *
     * Example JSON:
     * [
     *     {
     *         "experiment_name": "app1_autotune",
     *         "objective_function": "request_count",
     *         "hpo_algo_impl": "optuna_tpe",
     *         "tunables": [
     *             {
     *                 "value_type": "double",
     *                 "lower_bound": 150,
     *                 "name": "memoryRequest",
     *                 "step": 1,
     *                 "upper_bound": 300
     *             },
     *             {
     *                 "value_type": "double",
     *                 "lower_bound": 1,
     *                 "name": "cpuRequest",
     *                 "step": 0.01,
     *                 "upper_bound": 3
     *             }
     *         ],
     *         "sla_class": "throughput",
     *         "direction": "maximize"
     *     }
     * ]
     * @param request
     * @param response
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            String experimentId = request.getParameter(AnalyzerConstants.AutotuneObjectConstants.ID);
            JSONArray searchSpaceJsonArray = new JSONArray();
            getSearchSpaceJSONArray(searchSpaceJsonArray, experimentId);

            if (searchSpaceJsonArray.isEmpty()) {
                if (Experimentator.experimentsMap.isEmpty())
                    searchSpaceJsonArray.put("Error: No Experiments underway!");
                else
                    searchSpaceJsonArray.put("Error: Experiment " + experimentId + " not found!");
            }

            response.getWriter().println(searchSpaceJsonArray.toString(4));
            response.getWriter().close();
        } catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void addApplicationToSearchSpace(JSONArray outputJsonArray, String autotuneObjectKey, AutotuneObject autotuneObject, String experimentName) {
        JSONObject applicationJson = new JSONObject();
        ApplicationServiceStack applicationServiceStack = AutotuneDeployment.applicationServiceStackMap
                .get(autotuneObjectKey).get(experimentName);

        applicationJson.put(AnalyzerConstants.ServiceConstants.EXPERIMENT_NAME, experimentName);
        applicationJson.put(AnalyzerConstants.AutotuneObjectConstants.OBJECTIVE_FUNCTION, autotuneObject.getSloInfo().getObjectiveFunction());
        applicationJson.put(AnalyzerConstants.AutotuneObjectConstants.SLO_CLASS, autotuneObject.getSloInfo().getSloClass());
        applicationJson.put(AnalyzerConstants.AutotuneObjectConstants.DIRECTION, autotuneObject.getSloInfo().getDirection());
        applicationJson.put(AnalyzerConstants.AutotuneObjectConstants.HPO_ALGO_IMPL, autotuneObject.getSloInfo().getHpoAlgoImpl());

        JSONArray tunablesJsonArray = new JSONArray();
        for (String autotuneConfigName : applicationServiceStack.getApplicationServiceStackLayers().keySet()) {
            AutotuneConfig autotuneConfig = applicationServiceStack.getApplicationServiceStackLayers().get(autotuneConfigName);
            for (Tunable tunable : autotuneConfig.getTunables()) {
                JSONObject tunableJson = new JSONObject();
                tunableJson.put(AnalyzerConstants.AutotuneConfigConstants.NAME, tunable.getName());
                tunableJson.put(AnalyzerConstants.AutotuneConfigConstants.UPPER_BOUND, tunable.getUpperBound());
                tunableJson.put(AnalyzerConstants.AutotuneConfigConstants.LOWER_BOUND, tunable.getLowerBound());
                tunableJson.put(AnalyzerConstants.AutotuneConfigConstants.VALUE_TYPE, tunable.getValueType());
                tunableJson.put(AnalyzerConstants.AutotuneConfigConstants.STEP, tunable.getStep());

                tunablesJsonArray.put(tunableJson);
            }
        }

        applicationJson.put(AnalyzerConstants.AutotuneConfigConstants.TUNABLES, tunablesJsonArray);
        outputJsonArray.put(applicationJson);
    }
}