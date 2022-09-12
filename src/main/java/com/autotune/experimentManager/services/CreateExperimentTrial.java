/*******************************************************************************
 * Copyright (c) 2021, 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.experimentManager.services;

import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.common.parallelengine.executor.AutotuneExecutor;
import com.autotune.experimentManager.data.ExperimentDetailsMap;
import com.autotune.experimentManager.data.access.ExperimentAccess;
import com.autotune.experimentManager.data.access.ExperimentAccessImpl;
import com.autotune.experimentManager.utils.EMConstants;
import com.autotune.experimentManager.utils.EMConstants.ParallelEngineConfigs;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.autotune.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;


/**
 * RestAPI Servlet used to load experiment trial in JSON format using POST method.
 * JSON format sample can be found here autotune/examples/createExperimentTrial.json
 */

@WebServlet(asyncSupported = true)
public class CreateExperimentTrial extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateExperimentTrial.class);
    private static final String SERVLETCONTEXT_EM_KEY = "EM";

    /**
     * This API supports POST methode which is used to initiate experimental trials.
     * Input payload should be in the format of JSON. Please refer documentation for more details.
     * /createExperimentTrial is API endpoint,
     * HTTP STATUS CODE - 201 is returned if experiment loaded successfully.
     * HTTP STATUS CODE - 400 or 500 is returned for any error.
     *
     * @param request
     * @param response
     * @throws java.io.IOException
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, java.io.IOException {
        AsyncContext ac = request.startAsync();
        Gson gson = new Gson();
        HashMap<String, ExperimentTrial> experimentNameMap = new HashMap<String, ExperimentTrial>();
        try {
            ExperimentDetailsMap<String, ExperimentTrial> existExperimentTrialMap = (ExperimentDetailsMap<String, ExperimentTrial>) getServletContext().getAttribute(EMConstants.EMJSONKeys.EM_STORAGE_CONTEXT_KEY);
            String inputData = request.getReader().lines().collect(Collectors.joining());
            ExperimentTrial[] experimentTrialArray = gson.fromJson(inputData, ExperimentTrial[].class);
            List<ExperimentTrial> experimentTrialList = Arrays.asList(experimentTrialArray);
            ExperimentAccess experimentAccess = new ExperimentAccessImpl(existExperimentTrialMap);
            experimentAccess.addExperiments(experimentTrialList);            AutotuneExecutor emExecutor = (AutotuneExecutor) getServletContext().getAttribute(ParallelEngineConfigs.EM_EXECUTOR);
            if (null == experimentAccess.getErrorMessage()) {
                for (ExperimentTrial experimentTrial : experimentTrialList) {
                    try {
                        /**
                         * Asynchronous task gets initiated, and it will spawn iteration manger for each experiment.
                         */
                        //ToDO  Make sure Trials of same experiments not get executed in parallel.
                        emExecutor.getAutotuneQueue().put(experimentTrial);
                    } catch (InterruptedException e) {
                        LOGGER.debug(e.getMessage());
                        e.printStackTrace();
                        break;
                    }
                }
            } else {
                response.sendError(experimentAccess.getHttpResponseCode(), experimentAccess.getErrorMessage());
            }
            response.setContentType(JSON_CONTENT_TYPE);
            response.setCharacterEncoding(CHARACTER_ENCODING);
            response.setStatus(HttpServletResponse.SC_CREATED);
            PrintWriter out = response.getWriter();
            out.append(new Gson().toJson(experimentAccess.listExperiments()));
            out.flush();
        } catch (Exception e) {
            LOGGER.error(e.toString());
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            ac.complete();
        }
    }
}
