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

import com.autotune.dependencyAnalyzer.application.ApplicationServiceStack;
import com.autotune.dependencyAnalyzer.application.Tunable;
import com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment;
import com.autotune.dependencyAnalyzer.k8sObjects.AutotuneConfig;
import com.autotune.dependencyAnalyzer.k8sObjects.AutotuneObject;
import com.autotune.dependencyAnalyzer.util.DAConstants;
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

        String applicationName = req.getParameter(DAConstants.ServiceConstants.APPLICATION_NAME);

        for (String autotuneObjectKey : AutotuneDeployment.applicationServiceStackMap.keySet()) {
            AutotuneObject autotuneObject = AutotuneDeployment.autotuneObjectMap.get(autotuneObjectKey);

            if (applicationName == null) {
                //No application parameter, generate search space for all applications
                for (String application : AutotuneDeployment.applicationServiceStackMap.get(autotuneObjectKey).keySet()) {
                    addApplicationToSearchSpace(outputJsonArray, autotuneObjectKey, autotuneObject, application);
                }
            } else {
                if (AutotuneDeployment.applicationServiceStackMap.get(autotuneObjectKey).containsKey(applicationName)) {
                    addApplicationToSearchSpace(outputJsonArray, autotuneObjectKey, autotuneObject, applicationName);
                }
            }
        }
        resp.getWriter().println(outputJsonArray.toString(4));
    }

    private void addApplicationToSearchSpace(JSONArray outputJsonArray, String autotuneObjectKey, AutotuneObject autotuneObject, String application) {
        JSONObject applicationJson = new JSONObject();
        ApplicationServiceStack applicationServiceStack = AutotuneDeployment.applicationServiceStackMap
                .get(autotuneObjectKey).get(application);

        applicationJson.put(DAConstants.ServiceConstants.APPLICATION_NAME, application);
        applicationJson.put(DAConstants.AutotuneObjectConstants.OBJECTIVE_FUNCTION, autotuneObject.getSlaInfo().getObjectiveFunction());
        applicationJson.put(DAConstants.AutotuneObjectConstants.SLA_CLASS, autotuneObject.getSlaInfo().getSlaClass());
        applicationJson.put(DAConstants.AutotuneObjectConstants.DIRECTION, autotuneObject.getSlaInfo().getDirection());

        JSONArray tunablesJsonArray = new JSONArray();
        for(AutotuneConfig autotuneConfig : applicationServiceStack.getStackLayers()) {
            for (Tunable tunable : autotuneConfig.getTunables()) {
                JSONObject tunableJson = new JSONObject();
                tunableJson.put(DAConstants.AutotuneConfigConstants.NAME, tunable.getName());
                tunableJson.put(DAConstants.AutotuneConfigConstants.UPPER_BOUND, tunable.getUpperBound());
                tunableJson.put(DAConstants.AutotuneConfigConstants.LOWER_BOUND, tunable.getLowerBound());
                tunableJson.put(DAConstants.AutotuneConfigConstants.VALUE_TYPE, tunable.getValueType());

                tunablesJsonArray.put(tunableJson);
            }
        }

        applicationJson.put(DAConstants.AutotuneConfigConstants.TUNABLES, tunablesJsonArray);
        outputJsonArray.put(applicationJson);
    }
}
