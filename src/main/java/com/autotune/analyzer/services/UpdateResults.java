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
import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.analyzer.serviceObjects.FailedUpdateResultsAPIObject;
import com.autotune.analyzer.serviceObjects.UpdateResultsAPIObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.utils.MetricsConfig;
import com.google.gson.*;
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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

/**
 * REST API used to receive Experiment metric results .
 */
@WebServlet(asyncSupported = true)
public class UpdateResults extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateResults.class);
    public static ConcurrentHashMap<String, PerformanceProfile> performanceProfilesMap = new ConcurrentHashMap<>();
    private static int requestCount = 0;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int calCount = ++requestCount;
        LOGGER.debug("updateResults API request count: {}", calCount);
        String statusValue = "failure";
        Timer.Sample timerUpdateResults = Timer.start(MetricsConfig.meterRegistry());
        String inputData = "";
        try {
            // Set the character encoding of the request to UTF-8
            request.setCharacterEncoding(CHARACTER_ENCODING);
            inputData = request.getReader().lines().collect(Collectors.joining());
            List<UpdateResultsAPIObject> updateResultsAPIObjects;
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Double.class, new CustomNumberDeserializer())
                    .registerTypeAdapter(Integer.class, new CustomNumberDeserializer())
                    .create();
            LOGGER.debug("updateResults API request payload for requestID {} is {}", calCount, inputData);
            try {
                updateResultsAPIObjects = Arrays.asList(gson.fromJson(inputData, UpdateResultsAPIObject[].class));
            } catch (JsonParseException e) {
                LOGGER.error("{} : {}", AnalyzerErrorConstants.AutotuneObjectErrors.JSON_PARSING_ERROR, e.getMessage());
                sendErrorResponse(inputData, request, response, null, HttpServletResponse.SC_BAD_REQUEST, AnalyzerErrorConstants.AutotuneObjectErrors.JSON_PARSING_ERROR);
                return;
            } catch (NumberFormatException e) {
                LOGGER.error("{}", e.getMessage());
                sendErrorResponse(inputData, request, response, null, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
                return;
            }
            LOGGER.debug("updateResults API request payload for requestID {} bulk count is {}", calCount, updateResultsAPIObjects.size());
            // check for bulk entries and respond accordingly
            if (updateResultsAPIObjects.size() > KruizeDeploymentInfo.bulk_update_results_limit) {
                LOGGER.error(AnalyzerErrorConstants.AutotuneObjectErrors.UNSUPPORTED_EXPERIMENT_RESULTS);
                sendErrorResponse(inputData, request, response, null, HttpServletResponse.SC_BAD_REQUEST, AnalyzerErrorConstants.AutotuneObjectErrors.UNSUPPORTED_EXPERIMENT_RESULTS);
                return;
            }
            ExperimentInitiator experimentInitiator = new ExperimentInitiator();
            experimentInitiator.validateAndAddExperimentResults(updateResultsAPIObjects);
            List<UpdateResultsAPIObject> failureAPIObjs = experimentInitiator.getFailedUpdateResultsAPIObjects();
            List<FailedUpdateResultsAPIObject> jsonObjectList = new ArrayList<>();
            if (failureAPIObjs.size() > 0) {
                failureAPIObjs.forEach(
                        (failObj) -> {
                            FailedUpdateResultsAPIObject failJSONObj = new FailedUpdateResultsAPIObject(
                                    failObj.getApiVersion(),
                                    failObj.getExperimentName(),
                                    failObj.getStartTimestamp(),
                                    failObj.getEndTimestamp(),
                                    failObj.getErrors()
                            );
                            jsonObjectList.add(
                                    failJSONObj
                            );
                        }
                );
                request.setAttribute("data", jsonObjectList);
                String errorMessage = String.format("Out of a total of %s records, %s failed to save", updateResultsAPIObjects.size(), failureAPIObjs.size());
                LOGGER.debug("updateResults API request payload for requestID {} failed", calCount);
                sendErrorResponse(inputData, request, response, null, HttpServletResponse.SC_BAD_REQUEST, errorMessage);
            } else {
                LOGGER.debug("updateResults API request payload for requestID {} success", calCount);
                sendSuccessResponse(response, AnalyzerConstants.ServiceConstants.RESULT_SAVED);
                statusValue = "success";
            }
        } catch (Exception e) {
            LOGGER.debug("updateResults API request payload for requestID {} failed", calCount);
            LOGGER.error("Exception: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(inputData, request, response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } finally {
            LOGGER.debug("updateResults API request payload for requestID {} completed", calCount);
            if (null != timerUpdateResults) {
                MetricsConfig.timerUpdateResults = MetricsConfig.timerBUpdateResults.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerUpdateResults.stop(MetricsConfig.timerUpdateResults);
            }
        }
    }

    private void sendSuccessResponse(HttpServletResponse response, String message) throws IOException {
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        response.setStatus(HttpServletResponse.SC_CREATED);
        PrintWriter out = response.getWriter();
        String successOutput = new Gson().toJson(
                new KruizeResponse(message, HttpServletResponse.SC_CREATED, "", "SUCCESS")
        );
        LOGGER.debug(successOutput);
        out.append(
                successOutput
        );
        out.flush();
    }

    public void sendErrorResponse(String inputPayload, HttpServletRequest request, HttpServletResponse response, Exception e, int httpStatusCode, String errorMsg) throws
            IOException {
        if (null != e) {
            LOGGER.error(e.toString());
            e.printStackTrace();
            if (null == errorMsg) errorMsg = e.getMessage();
        }
        LOGGER.debug("UpdateRequestsAPI  input pay load {} ", inputPayload);
        response.sendError(httpStatusCode, errorMsg);
    }

    public static class CustomNumberDeserializer implements JsonDeserializer<Number> {

        @Override
        public Number deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws NumberFormatException {
            String value = json.getAsString().trim();
            if (value.isEmpty()) {
                throw new NumberFormatException(AnalyzerErrorConstants.AutotuneObjectErrors.AGGREGATION_INFO_INVALID_VALUE);
            } else {
                return Double.parseDouble(value);
            }
        }
    }

}
