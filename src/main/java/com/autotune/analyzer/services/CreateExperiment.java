/*******************************************************************************
 * Copyright (c) 2022 Red Hat, IBM Corporation and others.
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

import com.autotune.analyzer.data.ExperimentInterface;
import com.autotune.analyzer.data.ExperimentInterfaceImpl;
import com.autotune.analyzer.data.ExperimentValidation;
import com.autotune.analyzer.exceptions.AutotuneResponse;
import com.autotune.common.k8sObjects.AutotuneObject;
import com.autotune.common.parallelengine.executor.AutotuneExecutor;
import com.autotune.common.parallelengine.worker.AutotuneWorker;
import com.autotune.common.parallelengine.worker.CallableFactory;
import com.autotune.utils.AnalyzerConstants;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.autotune.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

/**
 * REST API to create experiments to Analyser for monitoring metrics.
 */
@WebServlet(asyncSupported = true)
public class CreateExperiment extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateExperiment.class);
    Map<String, AutotuneObject> mainKruizeExperimentMap;
    AutotuneExecutor analyserExecutor;


    @Override
    public void init(ServletConfig config) throws ServletException {
        LOGGER.info("*********************************************************************************START");
        super.init(config);
        try {
            this.mainKruizeExperimentMap = (ConcurrentHashMap<String, AutotuneObject>) getServletContext().getAttribute(AnalyzerConstants.EXPERIMENT_MAP);
            this.analyserExecutor = (AutotuneExecutor) getServletContext().getAttribute(AnalyzerConstants.AnalyserParallelEngineConfigs.EXECUTOR);

            ScheduledThreadPoolExecutor ses = new ScheduledThreadPoolExecutor(1);
            Runnable checkForNewExperiment = () -> {
                this.mainKruizeExperimentMap.forEach(           //TOdo do pre filter where status=QUEUED before loop
                        (name, ao) -> {
                            if (ao.getStatus().equals(AnalyzerConstants.ExpStatus.QUEUED)) {
                                this.analyserExecutor.submit(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                AutotuneWorker theWorker = new CallableFactory().create(analyserExecutor.getWorker());
                                                theWorker.execute(ao, analyserExecutor, getServletContext());
                                            }
                                        }
                                );
                            }
                        }
                );
            };
            ses.scheduleAtFixedRate(checkForNewExperiment, 5, 5, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("Not able to initiate createExperiment api due to {}", e.getMessage());
        }
        LOGGER.info("*********************************************************************************END");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            String inputData = request.getReader().lines().collect(Collectors.joining());
            try {
                List<AutotuneObject> kruizeExpList = Arrays.asList(new Gson().fromJson(inputData, AutotuneObject[].class));
                ExperimentValidation validationObject = new ExperimentValidation(this.mainKruizeExperimentMap);
                validationObject.validate(kruizeExpList);
                if (validationObject.isSuccess()) {
                    ExperimentInterface experimentInterface = new ExperimentInterfaceImpl();
                    experimentInterface.addExperiments(mainKruizeExperimentMap, validationObject.getValidKruizeExpList());
                    sendSuccessResponse(response);
                } else {
                    sendErrorResponse(response, null, HttpServletResponse.SC_BAD_REQUEST, "Validation failed due to " + validationObject.getErrorMessage());
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendErrorResponse(response, e, HttpServletResponse.SC_BAD_REQUEST, "Validation failed due to " + e.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            LOGGER.error("Exception due to :" + e.getMessage());
            sendErrorResponse(response, e, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendSuccessResponse(HttpServletResponse response) throws IOException {
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        response.setStatus(HttpServletResponse.SC_CREATED);
        PrintWriter out = response.getWriter();
        out.append(
                new Gson().toJson(
                        new AutotuneResponse("Experiment registered successfully with Autotune. View registered experiments at /listExperiments", HttpServletResponse.SC_CREATED, "", "SUCCESS")
                )
        );
        out.flush();
    }

    public void sendErrorResponse(HttpServletResponse response, Exception e, int httpStatusCode, String errorMsg) throws
            IOException {
        if (null != e) {
            LOGGER.error(e.toString());
            e.printStackTrace();
            if (null == errorMsg) errorMsg = e.getMessage();
        }
        response.sendError(httpStatusCode, errorMsg);
    }
}
