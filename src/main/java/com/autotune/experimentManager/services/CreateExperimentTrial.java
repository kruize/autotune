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

import com.autotune.analyzer.exceptions.KruizeResponse;
import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.common.parallelengine.executor.KruizeExecutor;
import com.autotune.common.parallelengine.worker.KruizeWorker;
import com.autotune.common.parallelengine.worker.CallableFactory;
import com.autotune.common.target.kubernetes.service.KubernetesServices;
import com.autotune.common.target.kubernetes.service.impl.KubernetesServicesImpl;
import com.autotune.experimentManager.data.ExperimentDetailsMap;
import com.autotune.experimentManager.data.TrialInterface;
import com.autotune.experimentManager.data.TrialInterfaceImpl;
import com.autotune.experimentManager.utils.EMConstants;
import com.autotune.experimentManager.utils.EMConstants.ParallelEngineConfigs;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncContext;
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
import java.util.stream.Collectors;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;


/**
 * RestAPI Servlet used to load experiment trial in JSON format using POST method.
 * JSON format sample can be found here autotune/examples/createExperimentTrial.json
 */

@WebServlet(asyncSupported = true)
public class CreateExperimentTrial extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateExperimentTrial.class);
    KruizeExecutor emExecutor;
    ExperimentDetailsMap<String, ExperimentTrial> EMExperimentTrialMap;

    public CreateExperimentTrial() {
        super();
    }

    /**
     * Get the instance of Task manager executor which helps in executing experiments asynchronously.
     *
     * @param config
     * @throws ServletException
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.emExecutor = (KruizeExecutor) getServletContext().getAttribute(ParallelEngineConfigs.EM_EXECUTOR);
        this.EMExperimentTrialMap = (ExperimentDetailsMap<String, ExperimentTrial>) getServletContext().getAttribute(EMConstants.EMKeys.EM_STORAGE_CONTEXT_KEY);
        KubernetesServices kubernetesServices = new KubernetesServicesImpl();
        getServletContext().setAttribute(EMConstants.EMKeys.EM_KUBERNETES_SERVICE, kubernetesServices);
    }

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
        try {
            String inputData = request.getReader().lines().collect(Collectors.joining());
            List<ExperimentTrial> experimentTrialList = Arrays.asList(new Gson().fromJson(inputData, ExperimentTrial[].class));
            TrialInterface experimentAccess = new TrialInterfaceImpl(this.EMExperimentTrialMap);
            experimentAccess.addExperiments(experimentTrialList);
            if (null == experimentAccess.getErrorMessage()) {
                submitTask();
            } else {
                sendErrorResponse(response, null, experimentAccess.getHttpResponseCode(), experimentAccess.getErrorMessage());
            }
            sendSuccessResponse(response);
        } catch (JsonParseException e) {
            sendErrorResponse(response, e, HttpServletResponse.SC_BAD_REQUEST, "Not able to parse JSON: " + e.getMessage());
        } catch (Exception e) {
            sendErrorResponse(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
        } finally {
            ac.complete();
        }
    }

    private void sendSuccessResponse(HttpServletResponse response) throws IOException {
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        response.setStatus(HttpServletResponse.SC_CREATED);
        PrintWriter out = response.getWriter();
        out.append(
                new Gson().toJson(
                        new KruizeResponse(null, HttpServletResponse.SC_CREATED, "", "SUCCESS")
                )
        );
        out.flush();
    }

    public void sendErrorResponse(HttpServletResponse response, Exception e, int httpStatusCode, String errorMsg) throws IOException {
        if (null != e) {
            LOGGER.error(e.toString());
            e.printStackTrace();
            if (null == errorMsg) errorMsg = e.getMessage();
        }
        response.sendError(httpStatusCode, errorMsg);
    }

    public void submitTask() {
        for (Object obj : EMExperimentTrialMap.values()) {
            try {
                /**
                 * Asynchronous task gets initiated, and it will spawn iteration manger for each experiment.
                 */
                //ToDo  Make sure new Experiments having identical deployments not get executed in parallel.
                ExperimentTrial experimentTrial = (ExperimentTrial) obj;
                this.emExecutor.submit(
                        new Runnable() {
                            @Override
                            public void run() {
                                KruizeWorker theWorker = new CallableFactory().create(emExecutor.getWorker());
                                theWorker.execute(null, experimentTrial, emExecutor, getServletContext());
                            }
                        }
                );
            } catch (Exception e) {
                throw e;
                //sendErrorResponse(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
            }
        }
    }

    /**
     * Shutdown executor when server gets shutdown.
     */
    @Override
    public void destroy() {
        this.emExecutor.shutdown();
        super.destroy();
    }
}
