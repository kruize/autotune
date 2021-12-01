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

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;
import static com.autotune.analyzer.utils.AnalyzerErrorConstants.AutotuneServiceMessages.*;
import static com.autotune.analyzer.utils.ServiceHelpers.addApplicationToSearchSpace;

public class SearchSpace extends HttpServlet
{
    /**
     * Generates the search space used for the analysis.
     *
     * Request:
     * `GET /searchSpace` gives the search space for all applications monitored.
     *
     * `GET /searchSpace?experiment_name=<EXP_NAME>` gives the search space for a specific application.
     *
     * Example JSON:
     * [
     *    {
     *         "experiment_name": "galaxies-autotune-min-http-response-time",
     *         "experiment_id": "7c07cf4db16adcf76bad79394c9e7df2f3b8d8e6942cfa3f7b254b5aec1299b0",
     *         "objective_function": "request_sum/request_count",
     *         "hpo_algo_impl": "optuna_tpe",
     *         "tunables": [
     *             {
     *                 "value_type": "double",
     *                 "lower_bound": "150.0Mi",
     *                 "name": "memoryRequest",
     *                 "step": 1,
     *                 "upper_bound": "300.0Mi"
     *             },
     *             {
     *                 "value_type": "double",
     *                 "lower_bound": "1.0",
     *                 "name": "cpuRequest",
     *                 "step": 0.01,
     *                 "upper_bound": "3.0"
     *             },
     *             {
     *                 "value_type": "integer",
     *                 "lower_bound": "9",
     *                 "name": "MaxInlineLevel",
     *                 "step": 1,
     *                 "upper_bound": "50"
     *             },
     *             {
     *                 "value_type": "integer",
     *                 "lower_bound": "1",
     *                 "name": "quarkus.thread-pool.core-threads",
     *                 "step": 1,
     *                 "upper_bound": "10"
     *             },
     *             {
     *                 "value_type": "integer",
     *                 "lower_bound": "1",
     *                 "name": "quarkus.thread-pool.queue-size",
     *                 "step": 1,
     *                 "upper_bound": "100"
     *             },
     *             {
     *                 "value_type": "integer",
     *                 "lower_bound": "1",
     *                 "name": "quarkus.hibernate-orm.jdbc.statement-fetch-size",
     *                 "step": 1,
     *                 "upper_bound": "50"
     *             }
     *         ],
     *         "direction": "minimize"
     *     }
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

        String experimentName = req.getParameter(AnalyzerConstants.ServiceConstants.EXPERIMENT_NAME);
        String containerImageName = req.getParameter(AnalyzerConstants.ServiceConstants.STACK_NAME);

        if (AutotuneDeployment.autotuneObjectMap.isEmpty()) {
            outputJsonArray.put(AUTOTUNE_OBJECTS_NOT_FOUND);
            resp.getWriter().println(outputJsonArray.toString(4));
            return;
        }

        for (String autotuneObjectKey : AutotuneDeployment.applicationServiceStackMap.keySet()) {
            AutotuneObject autotuneObject = AutotuneDeployment.autotuneObjectMap.get(autotuneObjectKey);

            if (containerImageName == null) {
                // No application parameter, generate search space for all applications
                for (String imageName : AutotuneDeployment.applicationServiceStackMap.get(autotuneObjectKey).keySet()) {
                    addApplicationToSearchSpace(outputJsonArray, autotuneObjectKey, autotuneObject, imageName);
                }
            } else {
                if (AutotuneDeployment.applicationServiceStackMap.get(autotuneObjectKey).containsKey(containerImageName)) {
                    addApplicationToSearchSpace(outputJsonArray, autotuneObjectKey, autotuneObject, containerImageName);
                }
            }
        }

        if (outputJsonArray.isEmpty()) {
            if (containerImageName != null) {
                outputJsonArray.put(ERROR_STACK_NAME + containerImageName + NOT_FOUND);
            } else {
                outputJsonArray.put(ERROR_EXPERIMENT_NAME + experimentName + NOT_FOUND);
            }
        }
        resp.getWriter().println(outputJsonArray.toString(4));
    }
}
