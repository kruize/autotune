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
package com.autotune.dependencyAnalyzer.service;

import com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment;
import com.autotune.dependencyAnalyzer.k8sObjects.AutotuneObject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
     *       "application_name": "app1",
     *       “objective_function”: “transaction_response_time”,
     *       "sla_class": "response_time",
     *       “direction”: “minimize”
     *     },
     *     {
     *       "application_name": "app2",
     *       “objective_function”: “performedChecks_total”,
     *       "sla_class": "throughput",
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
        resp.setContentType("application/json");

        String applicationName = req.getParameter("application_name");

        for (String autotuneObjectKey : AutotuneDeployment.applicationServiceStackMap.keySet()) {
            AutotuneObject autotuneObject = AutotuneDeployment.autotuneObjectMap.get(autotuneObjectKey);

            if (applicationName == null) {
                for (String application : AutotuneDeployment.applicationServiceStackMap.get(autotuneObjectKey).keySet()) {
                    addApplicationToResponse(outputJsonArray, autotuneObject, application);
                }
            } else {
                addApplicationToResponse(outputJsonArray, autotuneObject, applicationName);
            }
        }
        resp.getWriter().println(outputJsonArray.toString(4));
    }

    private void addApplicationToResponse(JSONArray outputJsonArray, AutotuneObject autotuneObject, String application) {
        //Check if application is monitored by autotune
        if (!AutotuneDeployment.applicationServiceStackMap.get(autotuneObject.getName()).containsKey(application))
            return;

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("application_name", application);
        jsonObject.put("direction", autotuneObject.getSlaInfo().getDirection());
        jsonObject.put("objective_function", autotuneObject.getSlaInfo().getObjectiveFunction());
        jsonObject.put("sla_class", autotuneObject.getSlaInfo().getSlaClass());

        outputJsonArray.put(jsonObject);
    }

}
