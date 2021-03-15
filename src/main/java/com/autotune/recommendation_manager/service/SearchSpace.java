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
package com.autotune.recommendation_manager.service;

import com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment;
import com.autotune.dependencyAnalyzer.util.DAConstants;
import com.autotune.recommendation_manager.ApplicationSearchSpace;
import com.autotune.recommendation_manager.ApplicationTunable;
import com.autotune.recommendation_manager.RecommendationManager;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
     *   {
     *     "application_name": "petclinic-deployment-6d4c8678d4-jmz8x",
     *     "objective_function": "transaction_response_time",
     *     "tunables": [
     *       {
     *         "value_type": "double",
     *         "lower_bound": "150M",
     *         "name": "memoryRequest",
     *         "upper_bound": "300M"
     *       },
     *       {
     *         "value_type": "double",
     *         "lower_bound": "1.0",
     *         "name": "cpuRequest",
     *         "upper_bound": "3.0"
     *       }
     *     ],
     *     "sla_class": "response_time",
     *     "direction": "minimize"
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

        String id = req.getParameter(DAConstants.AutotuneObjectConstants.ID);

        getSearchSpace(outputJsonArray, id);
        resp.getWriter().println(outputJsonArray.toString(4));
    }

    public void getSearchSpace(JSONArray outputJsonArray, String id) {
        if (id == null) {
            //No application parameter, generate search space for all applications
            for (String applicationID : RecommendationManager.applicationSearchSpaceMap.keySet()) {
                addApplicationToSearchSpace(outputJsonArray, applicationID);
            }
        } else {
            if (RecommendationManager.applicationSearchSpaceMap.containsKey(id)) {
                addApplicationToSearchSpace(outputJsonArray, id);
            }
        }

        if (outputJsonArray.isEmpty()) {
            if (AutotuneDeployment.autotuneObjectMap.isEmpty())
                outputJsonArray.put("Error: No objects of kind Autotune found!");
            else
                outputJsonArray.put("Error: Application " + id + " not found!");
        }
    }

    private void addApplicationToSearchSpace(JSONArray outputJsonArray, String id) {
        JSONObject applicationJson = new JSONObject();
        ApplicationSearchSpace applicationSearchSpace = RecommendationManager.applicationSearchSpaceMap.get(id);

        applicationJson.put(DAConstants.ServiceConstants.APPLICATION_NAME, applicationSearchSpace.getApplicationName());
        applicationJson.put(DAConstants.AutotuneObjectConstants.ID, applicationSearchSpace.getId());
        applicationJson.put(DAConstants.AutotuneObjectConstants.VALUE_TYPE, applicationSearchSpace.getValueType());
        applicationJson.put(DAConstants.AutotuneObjectConstants.OBJECTIVE_FUNCTION, applicationSearchSpace.getObjectiveFunction());
        applicationJson.put(DAConstants.AutotuneObjectConstants.DIRECTION, applicationSearchSpace.getDirection());
        applicationJson.put(DAConstants.AutotuneObjectConstants.HPO_ALGO_IMPL, applicationSearchSpace.getHpoAlgoImpl());

        JSONArray tunablesJsonArray = new JSONArray();
        for (ApplicationTunable applicationTunable : applicationSearchSpace.getApplicationTunables()) {
            JSONObject tunableJson = new JSONObject();
            tunableJson.put(DAConstants.AutotuneConfigConstants.NAME, applicationTunable.getName());
            tunableJson.put(DAConstants.AutotuneConfigConstants.UPPER_BOUND, applicationTunable.getUpperBound());
            tunableJson.put(DAConstants.AutotuneConfigConstants.LOWER_BOUND, applicationTunable.getLowerBound());
            tunableJson.put(DAConstants.AutotuneConfigConstants.VALUE_TYPE, applicationTunable.getValueType());
            tunableJson.put(DAConstants.AutotuneConfigConstants.STEP, applicationTunable.getStep());

            tunablesJsonArray.put(tunableJson);
        }

        applicationJson.put(DAConstants.AutotuneConfigConstants.TUNABLES, tunablesJsonArray);
        outputJsonArray.put(applicationJson);
    }
}
