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

import com.autotune.analyzer.exceptions.KruizeResponse;
import com.autotune.analyzer.experiment.ExperimentInitiator;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.serviceObjects.Converters;
import com.autotune.analyzer.serviceObjects.CreateExperimentAPIObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.database.dao.ExperimentDAO;
import com.autotune.database.dao.ExperimentDAOImpl;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.utils.MetricsConfig;
import com.autotune.utils.Utils;
import com.google.gson.Gson;
import io.micrometer.core.instrument.Timer;
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

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
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
        Timer.Sample timerCreateExp = Timer.start(MetricsConfig.meterRegistry());
        Map<String, KruizeObject> mKruizeExperimentMap = new ConcurrentHashMap<String, KruizeObject>();;
        try {
            String inputData = request.getReader().lines().collect(Collectors.joining());
            List<CreateExperimentAPIObject> createExperimentAPIObjects = Arrays.asList(new Gson().fromJson(inputData, CreateExperimentAPIObject[].class));
            // check for bulk entries and respond accordingly
            if (createExperimentAPIObjects.size() > 1) {
                LOGGER.error(AnalyzerErrorConstants.AutotuneObjectErrors.UNSUPPORTED_EXPERIMENT);
                sendErrorResponse(response, null, HttpServletResponse.SC_BAD_REQUEST, AnalyzerErrorConstants.AutotuneObjectErrors.UNSUPPORTED_EXPERIMENT);
            } else {
                List<KruizeObject> kruizeExpList = new ArrayList<>();
                for (CreateExperimentAPIObject createExperimentAPIObject : createExperimentAPIObjects) {
                    createExperimentAPIObject.setExperiment_id(Utils.generateID(createExperimentAPIObject.toString()));
                    createExperimentAPIObject.setStatus(AnalyzerConstants.ExperimentStatus.IN_PROGRESS);
                    KruizeObject kruizeObject = Converters.KruizeObjectConverters.convertCreateExperimentAPIObjToKruizeObject(createExperimentAPIObject);
                    if (null != kruizeObject)
                        kruizeExpList.add(kruizeObject);
                }
                new ExperimentInitiator().validateAndAddNewExperiments(mKruizeExperimentMap, kruizeExpList);
                //TODO: UX needs to be modified - Handle response for the multiple objects
                KruizeObject invalidKruizeObject = kruizeExpList.stream().filter((ko) -> (!ko.getValidation_data().isSuccess())).findAny().orElse(null);
                if (null == invalidKruizeObject) {
                    ValidationOutputData addedToDB = null;  // TODO savetoDB should move to queue and bulk upload not considered here
                    for (KruizeObject ko : kruizeExpList) {
                        CreateExperimentAPIObject validAPIObj = createExperimentAPIObjects.stream()
                                .filter(createObj -> ko.getExperimentName().equals(createObj.getExperimentName()))
                                .findAny()
                                .orElse(null);
                        validAPIObj.setValidationData(ko.getValidation_data());
                        ExperimentDAO experimentDAO = new ExperimentDAOImpl();
                        addedToDB = new ExperimentDBService().addExperimentToDB(validAPIObj);
                    }
                    if (addedToDB.isSuccess())
                        sendSuccessResponse(response, "Experiment registered successfully with Kruize.");
                    else {
                        sendErrorResponse(response, null, HttpServletResponse.SC_BAD_REQUEST, addedToDB.getMessage());
                    }
                } else {
                    LOGGER.error("Failed to create experiment: {}", invalidKruizeObject.getValidation_data().getMessage());
                    sendErrorResponse(response, null, invalidKruizeObject.getValidation_data().getErrorCode(), invalidKruizeObject.getValidation_data().getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("Unknown exception caught: " + e.getMessage());
            sendErrorResponse(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error: " + e.getMessage());
        } finally {
            if (null != timerCreateExp) timerCreateExp.stop(MetricsConfig.timerCreateExp);
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
        Map<String, KruizeObject> mKruizeExperimentMap = new ConcurrentHashMap<String, KruizeObject>();

        try {
            String inputData = request.getReader().lines().collect(Collectors.joining());
            CreateExperimentAPIObject[] createExperimentAPIObjects = new Gson().fromJson(inputData, CreateExperimentAPIObject[].class);
            if (createExperimentAPIObjects.length > 1) {
                LOGGER.error(AnalyzerErrorConstants.AutotuneObjectErrors.UNSUPPORTED_EXPERIMENT);
                sendErrorResponse(response, null, HttpServletResponse.SC_BAD_REQUEST, AnalyzerErrorConstants.AutotuneObjectErrors.UNSUPPORTED_EXPERIMENT);
            } else {
                for (CreateExperimentAPIObject ko : createExperimentAPIObjects) {
                    try {
                        new ExperimentDBService().loadExperimentFromDBByName(mKruizeExperimentMap, ko.getExperimentName());
                    } catch (Exception e) {
                        LOGGER.error("Loading saved experiment {} failed: {} ", ko.getExperimentName(), e.getMessage());
                    }
                }

                for (CreateExperimentAPIObject ko : createExperimentAPIObjects) {
                    String expName = ko.getExperimentName();
                    if (!mKruizeExperimentMap.isEmpty() && mKruizeExperimentMap.containsKey(expName)) {
                        ValidationOutputData validationOutputData = new ExperimentDAOImpl().deleteKruizeExperimentEntryByName(expName);
                        if (validationOutputData.isSuccess()) {
                            mKruizeExperimentMap.remove(ko.getExperimentName());
                        } else {
                            throw new Exception("Experiment not deleted due to : " + validationOutputData.getMessage());
                        }
                    } else
                        throw new Exception("Experiment not found!");
                }
                sendSuccessResponse(response, "Experiment deleted successfully.");
            }
        } catch (Exception e) {
            sendErrorResponse(response, e, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
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
