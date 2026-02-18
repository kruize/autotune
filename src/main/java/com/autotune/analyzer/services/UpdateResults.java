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

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.autotune.analyzer.adapters.DeviceDetailsAdapter;
import com.autotune.analyzer.adapters.MetricMetadataAdapter;
import com.autotune.analyzer.adapters.RecommendationItemAdapter;
import com.autotune.analyzer.exceptions.KruizeResponse;
import com.autotune.analyzer.experiment.ExperimentInitiator;
import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.analyzer.serviceObjects.FailedUpdateResultsAPIObject;
import com.autotune.analyzer.serviceObjects.UpdateResultsAPIObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.metrics.MetricMetadata;
import com.autotune.common.data.system.info.device.DeviceDetails;
import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.utils.MetricsConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import io.micrometer.core.instrument.Timer;

/**
 * REST API used to receive Experiment metric results .
 */
@WebServlet(asyncSupported = true)
public class UpdateResults extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateResults.class);
    private static final Gson SIMPLE_GSON = new Gson();
    public static ConcurrentHashMap<String, PerformanceProfile> performanceProfilesMap = new ConcurrentHashMap<>();
    private static int requestCount = 0;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int calCount = ++requestCount;
        String requestId = String.valueOf(calCount);
        
        LOGGER.info("Starting updateResults API processing - requestId: {}, remoteAddr: {}", 
                requestId, request.getRemoteAddr());
        LOGGER.debug("updateResults API request count: {}", calCount);
        
        String statusValue = "failure";
        Timer.Sample timerUpdateResults = Timer.start(MetricsConfig.meterRegistry());
        String inputData = "";
        int bulkCount = 0;
        int duplicateCount = 0;
        int successfullyAdded = 0;
        List<UpdateResultsAPIObject> actualFailures = null;
        try {
            // Set the character encoding of the request to UTF-8
            request.setCharacterEncoding(CHARACTER_ENCODING);
            inputData = request.getReader().lines().collect(Collectors.joining());
            List<UpdateResultsAPIObject> updateResultsAPIObjects;
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Double.class, new CustomNumberDeserializer())
                    .registerTypeAdapter(Integer.class, new CustomNumberDeserializer())
                    .registerTypeAdapter(AnalyzerConstants.RecommendationItem.class, new RecommendationItemAdapter())
                    .registerTypeAdapter(DeviceDetails.class, new DeviceDetailsAdapter())
                    .registerTypeAdapter(MetricMetadata.class, new MetricMetadataAdapter())
                    .create();
            LOGGER.debug("updateResults API request payload size for requestID {}: {} characters", 
                    requestId, inputData.length());
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("updateResults API request payload for requestID {}: {}", requestId, inputData);
            }
            
            try {
                updateResultsAPIObjects = Arrays.asList(gson.fromJson(inputData, UpdateResultsAPIObject[].class));
                LOGGER.debug("Successfully parsed JSON for requestID {}", requestId);
            } catch (JsonParseException e) {
                LOGGER.error("JSON parsing failed for requestID {}: {} - Error: {}", 
                        requestId, AnalyzerErrorConstants.AutotuneObjectErrors.JSON_PARSING_ERROR, e.getMessage());
                sendErrorResponse(inputData, request, response, null, HttpServletResponse.SC_BAD_REQUEST, AnalyzerErrorConstants.AutotuneObjectErrors.JSON_PARSING_ERROR);
                return;
            } catch (NumberFormatException e) {
                LOGGER.error("Number format validation failed for requestID {}: {}", requestId, e.getMessage());
                sendErrorResponse(inputData, request, response, null, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
                return;
            }
            bulkCount = updateResultsAPIObjects.size();
            LOGGER.info("updateResults API requestID {} processing {} experiment results", requestId, bulkCount);
            
            // check for bulk entries and respond accordingly
            if (bulkCount > KruizeDeploymentInfo.bulk_update_results_limit) {
                LOGGER.error("Bulk limit exceeded for requestID {}: {} results exceed limit of {}", 
                        requestId, bulkCount, KruizeDeploymentInfo.bulk_update_results_limit);
                sendErrorResponse(inputData, request, response, null, HttpServletResponse.SC_BAD_REQUEST, AnalyzerErrorConstants.AutotuneObjectErrors.UNSUPPORTED_EXPERIMENT_RESULTS);
                return;
            } else if (bulkCount > KruizeDeploymentInfo.bulk_update_results_limit * 0.8) {
                LOGGER.warn("Approaching bulk limit for requestID {}: {} results (limit: {})", 
                        requestId, bulkCount, KruizeDeploymentInfo.bulk_update_results_limit);
            }
            LOGGER.debug("Starting experiment validation and processing for requestID {}", requestId);
            ExperimentInitiator experimentInitiator = new ExperimentInitiator();
            experimentInitiator.validateAndAddExperimentResults(updateResultsAPIObjects);
            
            List<UpdateResultsAPIObject> failureAPIObjs = experimentInitiator.getFailedUpdateResultsAPIObjects();
            List<FailedUpdateResultsAPIObject> jsonObjectList = new ArrayList<>();
            actualFailures = new ArrayList<>();
            
            for (UpdateResultsAPIObject failObj : failureAPIObjs) {
                boolean isDuplicate = isDuplicateError(failObj);
                        
                if (isDuplicate) {
                    duplicateCount++;
                    LOGGER.warn("Duplicate data point detected for experiment '{}' (start: {}, end: {})", 
                            failObj.getExperimentName(), failObj.getStartTimestamp(), failObj.getEndTimestamp());
                } else {
                    actualFailures.add(failObj);
                }
            }
            
            successfullyAdded = bulkCount - actualFailures.size() - duplicateCount;
            
            if (!actualFailures.isEmpty()) {
                LOGGER.warn("Partial failure in requestID {}: {} newly added, {} failed, {} duplicates skipped, {} total records", 
                        requestId, successfullyAdded, actualFailures.size(), duplicateCount, bulkCount);
                        
                actualFailures.forEach(failObj -> jsonObjectList.add(
                        new FailedUpdateResultsAPIObject(
                                failObj.getApiVersion(),
                                failObj.getExperimentName(),
                                failObj.getStartTimestamp(),
                                failObj.getEndTimestamp(),
                                failObj.getErrors()
                        )
                ));
                request.setAttribute("data", jsonObjectList);
                String errorMessage = String.format("Out of a total of %s records, %s failed to save (excluding %s duplicates)", 
                        bulkCount, actualFailures.size(), duplicateCount);
                LOGGER.error("updateResults API requestID {} completed with partial failures: {}", requestId, errorMessage);
                sendErrorResponse(inputData, request, response, null, HttpServletResponse.SC_BAD_REQUEST, errorMessage);
            } else {
                String successMessage;
                if (duplicateCount > 0) {
                    successMessage = String.format("Results saved successfully. %d duplicate records were skipped.", duplicateCount);
                    LOGGER.info("updateResults API requestID {} completed successfully - {} newly added, {} duplicates skipped, {} total records", 
                            requestId, successfullyAdded, duplicateCount, bulkCount);
                } else {
                    successMessage = AnalyzerConstants.ServiceConstants.RESULT_SAVED;
                    LOGGER.info("updateResults API requestID {} completed successfully - {} experiment results processed", 
                            requestId, bulkCount);
                }
                
                if (KruizeDeploymentInfo.log_http_req_resp && LOGGER.isDebugEnabled()) {
                    LOGGER.debug("updateResults API requestID {} success response payload: {}", 
                            requestId, SIMPLE_GSON.toJson(JsonParser.parseString(inputData)));
                }
                sendSuccessResponse(response, successMessage);
                statusValue = "success";
            }
        } catch (Exception e) {
            LOGGER.error("updateResults API requestID {} failed with unexpected error: {} - {}", 
                    requestId, e.getClass().getSimpleName(), e.getMessage(), e);
            sendErrorResponse(inputData, request, response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } finally {
            LOGGER.info("updateResults API requestID {} completed with status: {} (processed: {} items)", 
                    requestId, statusValue, bulkCount);
            
            if (null != timerUpdateResults) {
                MetricsConfig.timerUpdateResults = MetricsConfig.timerBUpdateResults.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerUpdateResults.stop(MetricsConfig.timerUpdateResults);
            }
            
            if (MetricsConfig.timerBUpdateResultsAdded != null) {
                MetricsConfig.timerBUpdateResultsAdded.register(MetricsConfig.meterRegistry()).increment(successfullyAdded);
            }
            if (MetricsConfig.timerBUpdateResultsDuplicates != null) {
                MetricsConfig.timerBUpdateResultsDuplicates.register(MetricsConfig.meterRegistry()).increment(duplicateCount);
            }
            if (actualFailures != null && !actualFailures.isEmpty() && MetricsConfig.timerBUpdateResultsFailed != null) {
                MetricsConfig.timerBUpdateResultsFailed.register(MetricsConfig.meterRegistry()).increment(actualFailures.size());
            }
        }
    }

    private void sendSuccessResponse(HttpServletResponse response, String message) throws IOException {
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        response.setStatus(HttpServletResponse.SC_CREATED);
        PrintWriter out = response.getWriter();
        String successOutput = SIMPLE_GSON.toJson(
                new KruizeResponse(message, HttpServletResponse.SC_CREATED, "", "SUCCESS")
        );
        LOGGER.debug("Sending success response - status: {}, message: {}", 
                HttpServletResponse.SC_CREATED, message);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Update Results API success response payload: {}", successOutput);
        }
        out.append(successOutput);
        out.flush();
    }

    public void sendErrorResponse(String inputPayload, HttpServletRequest request, HttpServletResponse response, Exception e, int httpStatusCode, String errorMsg) throws
            IOException {
        String clientInfo = String.format("client=%s, userAgent=%s", 
                request.getRemoteAddr(), 
                request.getHeader("User-Agent"));
        
        if (null != e) {
            LOGGER.error("Sending error response - status: {}, error: {} ({})", 
                    httpStatusCode, errorMsg != null ? errorMsg : e.getMessage(), clientInfo, e);
            if (null == errorMsg) errorMsg = e.getMessage();
        } else {
            LOGGER.warn("Sending error response - status: {}, error: {} ({})", 
                    httpStatusCode, errorMsg, clientInfo);
        }
        
        if (KruizeDeploymentInfo.log_http_req_resp && LOGGER.isDebugEnabled()) {
            LOGGER.debug("UpdateResults error response - input payload size: {} chars", 
                    inputPayload != null ? inputPayload.length() : 0);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("UpdateResults error response - input payload: {}", 
                        inputPayload != null ? SIMPLE_GSON.toJson(JsonParser.parseString(inputPayload)) : "null");
            }
        }
        response.sendError(httpStatusCode, errorMsg);
    }

    private boolean isDuplicateError(UpdateResultsAPIObject failObj) {
        if (failObj.getErrors() == null) {
            return false;
        }
        
        for (var error : failObj.getErrors()) {
            if (error != null && 
                error.getMessage() != null && 
                error.getMessage().equals(AnalyzerErrorConstants.APIErrors.updateResultsAPI.RESULTS_ALREADY_EXISTS)) {
                return true;
            }
        }
        return false;
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
