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

import com.autotune.analyzer.serviceObjects.Converters;
import com.autotune.operator.KruizeOperator;
import com.autotune.analyzer.exceptions.KruizeResponse;
import com.autotune.analyzer.serviceObjects.CreateExperimentSO;
import com.autotune.analyzer.experiment.ExperimentInitiator;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.utils.Utils;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

/**
 * REST API to create experiments to Analyser for monitoring metrics.
 */
@WebServlet(asyncSupported = true)
public class CreateExperiment extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateExperiment.class);
    Map<String, KruizeObject> mainKruizeExperimentMap;


    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.mainKruizeExperimentMap = (ConcurrentHashMap<String, KruizeObject>) getServletContext().getAttribute(AnalyzerConstants.EXPERIMENT_MAP);
    }

    /**
     * It reads the input data from the request, converts it into a List of "KruizeObject" objects using the GSON library.
     * It then calls the validateAndAddNewExperiments method of the "ExperimentInitiator" class, passing in the mainKruizeExperimentMap and kruizeExpList as arguments.
     * If the validateAndAddNewExperiments method returns an ValidationOutputData object with the success flag set to true, it sends a success response to the client with a message "Experiment registered successfully with Kruize."
     * Otherwise, it sends an error response to the client with the appropriate error message.
     * If an exception is thrown, it prints the stack trace and sends an error response to the client with the appropriate error message.
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            String inputData = request.getReader().lines().collect(Collectors.joining());
            List<CreateExperimentSO> experimentSOList = Arrays.asList(new Gson().fromJson(inputData, CreateExperimentSO[].class));
            if (experimentSOList.size() > 1) {
                LOGGER.error(AnalyzerErrorConstants.AutotuneObjectErrors.UNSUPPORTED_EXPERIMENT);
                sendErrorResponse(response, null, HttpServletResponse.SC_BAD_REQUEST, AnalyzerErrorConstants.AutotuneObjectErrors.UNSUPPORTED_EXPERIMENT );
            } else {
                List<KruizeObject> kruizeExpList = new ArrayList<>();
                for (CreateExperimentSO createExperimentSO : experimentSOList) {
                    KruizeObject kruizeObject = Converters.KruizeObjectConverters.convertCreateExperimentSOToKruizeObject(createExperimentSO);
                    if (null != kruizeObject) {
                        kruizeExpList.add(kruizeObject);
                    }
                }
                new ExperimentInitiator().validateAndAddNewExperiments(mainKruizeExperimentMap, kruizeExpList);
                //TODO: UX needs to be modified - Handle response for the multiple objects
                KruizeObject invalidKruizeObject = kruizeExpList.stream().filter((ko) -> (!ko.getValidationData().isSuccess())).findAny().orElse(null);
                if (null == invalidKruizeObject) {
                    sendSuccessResponse(response, "Experiment registered successfully with Kruize.");
                } else {
                    LOGGER.error("Failed to create experiment: {}", invalidKruizeObject.getValidationData().getMessage());
                    sendErrorResponse(response, null, HttpServletResponse.SC_BAD_REQUEST, invalidKruizeObject.getValidationData().getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("Unknown exception caught: " + e.getMessage());
            sendErrorResponse(response, e, HttpServletResponse.SC_BAD_REQUEST, "Validation failed: " + e.getMessage());
        }
    }

    /**
     * TODO temp solution to delete experiments, Need to evaluate use cases
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            String inputData = request.getReader().lines().collect(Collectors.joining());
            List<KruizeObject> kruizeExpList = Arrays.asList(new Gson().fromJson(inputData, KruizeObject[].class));
            for (KruizeObject ko : kruizeExpList) {
                mainKruizeExperimentMap.remove(ko.getExperimentName());
                KruizeOperator.deploymentMap.remove(ko.getExperimentName());
            }
            sendSuccessResponse(response, "Experiment deleted successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(response, e, HttpServletResponse.SC_BAD_REQUEST, "Validation failed: " + e.getMessage());
        }
    }

    private void sendSuccessResponse(HttpServletResponse response, String message) throws IOException {
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        response.setStatus(HttpServletResponse.SC_CREATED);
        PrintWriter out = response.getWriter();
        out.append(
                new Gson().toJson(
                        new KruizeResponse(message + " View registered experiments at /listExperiments", HttpServletResponse.SC_CREATED, "", "SUCCESS")
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
