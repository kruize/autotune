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

import com.autotune.analyzer.experiment.KruizeExperiment;
import com.autotune.analyzer.application.ApplicationDeployment;
import com.autotune.analyzer.application.ApplicationSearchSpace;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import org.json.JSONArray;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

import static com.autotune.analyzer.experiment.Experimentator.experimentsMap;
import static com.autotune.operator.KruizeOperator.autotuneObjectMap;
import static com.autotune.operator.KruizeOperator.deploymentMap;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;
import static com.autotune.analyzer.utils.AnalyzerErrorConstants.AutotuneServiceMessages.*;
import static com.autotune.analyzer.utils.ServiceHelpers.addApplicationToSearchSpace;

/**
 * Generates the search space used for the analysis.
 * <p>
 * Request:
 * `GET /searchSpace` gives the search space for all applications monitored.
 * <p>
 * `GET /searchSpace?experiment_name=<EXP_NAME>` gives the search space for a specific application.
 * <p>
 * Example JSON:
 * [
 * {
 * "experiment_name": "galaxies-autotune-min-http-response-time",
 * "experiment_id": "7c07cf4db16adcf76bad79394c9e7df2f3b8d8e6942cfa3f7b254b5aec1299b0",
 * "objective_function":
 *  {
 *  "function_type": "expression",
 *  "expression": "request_sum/request_count"
 *  },
 * "hpo_algo_impl": "optuna_tpe",
 * "tunables": [
 * {
 * "value_type": "double",
 * "lower_bound": "150.0Mi",
 * "name": "memoryRequest",
 * "step": 1,
 * "upper_bound": "300.0Mi"
 * },
 * {
 * "value_type": "double",
 * "lower_bound": "1.0",
 * "name": "cpuRequest",
 * "step": 0.01,
 * "upper_bound": "3.0"
 * },
 * {
 * "value_type": "integer",
 * "lower_bound": "9",
 * "name": "MaxInlineLevel",
 * "step": 1,
 * "upper_bound": "50"
 * },
 * {
 * "value_type": "integer",
 * "lower_bound": "1",
 * "name": "quarkus.thread-pool.core-threads",
 * "step": 1,
 * "upper_bound": "10"
 * },
 * {
 * "value_type": "integer",
 * "lower_bound": "1",
 * "name": "quarkus.thread-pool.queue-size",
 * "step": 1,
 * "upper_bound": "100"
 * },
 * {
 * "value_type": "integer",
 * "lower_bound": "1",
 * "name": "quarkus.hibernate-orm.jdbc.statement-fetch-size",
 * "step": 1,
 * "upper_bound": "50"
 * }
 * ],
 * "direction": "minimize"
 * }
 * ]
 */
public class SearchSpace extends HttpServlet {
    /**
     * Generates the search space used for the analysis.
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
        JSONArray searchSpaceJsonArray = new JSONArray();

        String experimentName = request.getParameter(AnalyzerConstants.ServiceConstants.EXPERIMENT_NAME);
        String deploymentName = request.getParameter(AnalyzerConstants.ServiceConstants.DEPLOYMENT_NAME);
        String containerImageName = request.getParameter(AnalyzerConstants.ServiceConstants.STACK_NAME);

        KruizeExperiment kruizeExperiment = null;
        ApplicationSearchSpace applicationSearchSpace = null;
        if (null != experimentName) {
            KruizeObject kruizeObject = autotuneObjectMap.get(experimentName);
            if (null != kruizeObject) {
                Map<String, ApplicationDeployment> depMap = deploymentMap.get(experimentName);
                if (!depMap.isEmpty()) {
                    if (null != deploymentName) {
                        ApplicationDeployment applicationDeployment = depMap.get(deploymentName);
                        if (null != applicationDeployment) {
                            kruizeExperiment = experimentsMap.get(deploymentName);
                            if (null != kruizeExperiment) {
                                applicationSearchSpace = kruizeExperiment.getApplicationSearchSpace();
                                addApplicationToSearchSpace(searchSpaceJsonArray, applicationSearchSpace);
                            }
                        }
                    } else {
                        for (String depName : depMap.keySet()) {
                            kruizeExperiment = experimentsMap.get(depName);
                            if (null != kruizeExperiment) {
                                applicationSearchSpace = kruizeExperiment.getApplicationSearchSpace();
                                addApplicationToSearchSpace(searchSpaceJsonArray, applicationSearchSpace);
                            }
                        }
                    }
                }
            }
        } else if (null != deploymentName) {
            kruizeExperiment = experimentsMap.get(deploymentName);
            if (null != kruizeExperiment) {
                applicationSearchSpace = kruizeExperiment.getApplicationSearchSpace();
                addApplicationToSearchSpace(searchSpaceJsonArray, applicationSearchSpace);
            }
        } else {
            // No experiment name parameter, generate search space for all experiments
            for (String expName : experimentsMap.keySet()) {
                kruizeExperiment = experimentsMap.get(expName);
                applicationSearchSpace = kruizeExperiment.getApplicationSearchSpace();
                addApplicationToSearchSpace(searchSpaceJsonArray, applicationSearchSpace);
            }
        }

        if (searchSpaceJsonArray.isEmpty()) {
            if (null != containerImageName) {
                searchSpaceJsonArray.put(ERROR_STACK_NAME + containerImageName + NOT_FOUND);
            } else if (null != deploymentName) {
                searchSpaceJsonArray.put(ERROR_DEPLOYMENT_NAME + deploymentName + NOT_FOUND);
            } else {
                searchSpaceJsonArray.put(ERROR_EXPERIMENT_NAME + experimentName + NOT_FOUND);
            }
        }
        response.getWriter().println(searchSpaceJsonArray.toString(4));
        response.getWriter().close();
    }
}
