/*******************************************************************************
 * Copyright (c) 2026 Red Hat, IBM Corporation and others.
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

import com.autotune.analyzer.adapters.DeviceDetailsAdapter;
import com.autotune.analyzer.adapters.MetricMetadataAdapter;
import com.autotune.analyzer.adapters.RecommendationItemAdapter;
import com.autotune.analyzer.exceptions.FetchMetricsError;
import com.autotune.analyzer.exceptions.KruizeResponse;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.recommendations.engine.RecommendationEngine;
import com.autotune.analyzer.serviceObjects.ContainerAPIObject;
import com.autotune.analyzer.serviceObjects.Converters;
import com.autotune.analyzer.serviceObjects.ListRecommendationsAPIObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.analyzer.utils.GsonUTCDateAdapter;
import com.autotune.analyzer.utils.ServiceHelpers;
import com.autotune.common.data.metrics.MetricMetadata;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.system.info.device.DeviceDetails;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.MetricsConfig;
import com.autotune.utils.Utils;
import com.google.gson.*;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.autotune.analyzer.utils.AnalyzerConstants.REMOTE;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;
import static com.autotune.utils.KruizeConstants.KRUIZE_BULK_API.JOB_ID;

/**
 * REST API v1 endpoint for getting recommendations with the new schema.
 * This endpoint supports the new recommendation schema with replicas, nested resources,
 * and metrics_info containing pod_count metrics.
 * 
 * Endpoint: /kruize/api/v1/recommendations
 * Query Parameters:
 *   - experiment_name: Name of the experiment (optional)
 *   - interval_end_time: Specific timestamp for recommendations (optional)
 */
@WebServlet(asyncSupported = true)
@Tag(name = "Recommendations V1", description = "API v1 for getting recommendations with new schema")
public class ListRecommendationsV1 extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ListRecommendationsV1.class);

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    @Operation(
            summary = "List recommendations (V1)",
            description = "Retrieve recommendations for experiments with new schema including replicas, nested resources, and pod_count metrics"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved recommendations",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ListRecommendationsAPIObject.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid parameters",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = KruizeResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = KruizeResponse.class)
                    )
            )
    })
    @Override
    protected void doGet(
            @Parameter(description = "Name of the experiment", required = false)
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        String statusValue = "failure";
        Timer.Sample timerListRec = Timer.start(MetricsConfig.meterRegistry());
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        response.setStatus(HttpServletResponse.SC_OK);
        request.setCharacterEncoding(CHARACTER_ENCODING);
        
        String experimentName = request.getParameter(AnalyzerConstants.ServiceConstants.EXPERIMENT_NAME);
        String latestRecommendation = request.getParameter(AnalyzerConstants.ServiceConstants.LATEST);
        String monitoringEndTime = request.getParameter(KruizeConstants.JSONKeys.MONITORING_END_TIME);
        String rm = request.getParameter(AnalyzerConstants.ServiceConstants.RM);
        Timestamp monitoringEndTimestamp = null;
        Map<String, KruizeObject> mKruizeExperimentMap = new ConcurrentHashMap<>();

        boolean getLatest = true;
        boolean checkForTimestamp = false;
        boolean error = false;
        boolean rmTable = false;
        
        if (null != latestRecommendation && !latestRecommendation.isEmpty()
                && latestRecommendation.equalsIgnoreCase(AnalyzerConstants.BooleanString.FALSE)) {
            getLatest = false;
        }
        
        List<KruizeObject> kruizeObjectList = new ArrayList<>();
        try {
            // Check if experiment name is passed
            if (null != experimentName) {
                experimentName = experimentName.trim();
                try {
                    new ExperimentDBService().loadExperimentAndRecommendationsFromDBByName(mKruizeExperimentMap, experimentName);
                } catch (Exception e) {
                    LOGGER.error("Loading saved experiment {} failed: {} ", experimentName, e.getMessage());
                }
                
                if (mKruizeExperimentMap.containsKey(experimentName)) {
                    if (null != monitoringEndTime && !monitoringEndTime.isEmpty()) {
                        monitoringEndTime = monitoringEndTime.trim();
                        if (Utils.DateUtils.isAValidDate(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, monitoringEndTime)) {
                            Date mEndTime = Utils.DateUtils.getDateFrom(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, monitoringEndTime);
                            monitoringEndTimestamp = new Timestamp(mEndTime.getTime());
                            boolean timestampExists = ServiceHelpers.KruizeObjectOperations.checkRecommendationTimestampExists(
                                    mKruizeExperimentMap.get(experimentName), monitoringEndTime);
                            if (timestampExists) {
                                checkForTimestamp = true;
                            } else {
                                error = true;
                                sendErrorResponse(response,
                                        new Exception(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.RECOMMENDATION_DOES_NOT_EXIST_EXCPTN),
                                        HttpServletResponse.SC_BAD_REQUEST,
                                        String.format(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.RECOMMENDATION_DOES_NOT_EXIST_MSG, monitoringEndTime));
                            }
                        } else {
                            error = true;
                            sendErrorResponse(response,
                                    new Exception(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_TIMESTAMP_EXCPTN),
                                    HttpServletResponse.SC_BAD_REQUEST,
                                    String.format(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_TIMESTAMP_MSG, monitoringEndTime));
                        }
                    }
                    kruizeObjectList.add(mKruizeExperimentMap.get(experimentName));
                } else {
                    error = true;
                    sendErrorResponse(response,
                            new Exception(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_EXPERIMENT_NAME_EXCPTN),
                            HttpServletResponse.SC_BAD_REQUEST,
                            String.format(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_EXPERIMENT_NAME_MSG, experimentName));
                }
            } else {
                try {
                        new ExperimentDBService().loadAllExperimentsAndRecommendations(mKruizeExperimentMap);
                } catch (Exception e) {
                    LOGGER.error("Loading saved experiments failed: {} ", e.getMessage());
                }
                
                if (null != monitoringEndTime && !monitoringEndTime.isEmpty()) {
                    monitoringEndTime = monitoringEndTime.trim();
                    if (Utils.DateUtils.isAValidDate(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, monitoringEndTime)) {
                        Date mEndTime = Utils.DateUtils.getDateFrom(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, monitoringEndTime);
                        monitoringEndTimestamp = new Timestamp(mEndTime.getTime());
                        for (String expName : mKruizeExperimentMap.keySet()) {
                            boolean timestampExists = ServiceHelpers.KruizeObjectOperations.checkRecommendationTimestampExists(
                                    mKruizeExperimentMap.get(expName), monitoringEndTime);
                            if (timestampExists) {
                                kruizeObjectList.add(mKruizeExperimentMap.get(expName));
                                checkForTimestamp = true;
                            }
                        }
                        if (kruizeObjectList.isEmpty()) {
                            error = true;
                            sendErrorResponse(response,
                                    new Exception(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.RECOMMENDATION_DOES_NOT_EXIST_EXCPTN),
                                    HttpServletResponse.SC_BAD_REQUEST,
                                    String.format(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.RECOMMENDATION_DOES_NOT_EXIST_MSG, monitoringEndTime));
                        }
                    } else {
                        error = true;
                        sendErrorResponse(response,
                                new Exception(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_TIMESTAMP_EXCPTN),
                                HttpServletResponse.SC_BAD_REQUEST,
                                String.format(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_TIMESTAMP_MSG, monitoringEndTime));
                    }
                } else {
                    kruizeObjectList.addAll(mKruizeExperimentMap.values());
                }
            }
            
            if (!error) {
                List<ListRecommendationsAPIObject> recommendationList = new ArrayList<>();
                for (KruizeObject ko : kruizeObjectList) {
                    try {
                        // Use V1 converter for new schema with replicas and pod_count
                        ListRecommendationsAPIObject listRecommendationsAPIObject =
                                Converters.KruizeObjectConverters.convertKruizeObjectToListRecommendationSOV1(
                                        ko, getLatest, checkForTimestamp, monitoringEndTimestamp);
                        recommendationList.add(listRecommendationsAPIObject);
                        statusValue = "success";
                    } catch (Exception e) {
                        LOGGER.error("Not able to generate recommendation for expName : {} due to {}",
                                ko.getExperimentName(), e.getMessage());
                    }
                }

                ExclusionStrategy strategy = new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes field) {
                        return field.getDeclaringClass() == ContainerData.class && (field.getName().equals("results"))
                                || (field.getDeclaringClass() == ContainerAPIObject.class && (field.getName().equals("metrics")));
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        return false;
                    }
                };
                
                String gsonStr = "[]";
                if (!recommendationList.isEmpty()) {
                    Gson gsonObj = new GsonBuilder()
                            .disableHtmlEscaping()
                            .setPrettyPrinting()
                            .enableComplexMapKeySerialization()
                            .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                            .registerTypeAdapter(AnalyzerConstants.RecommendationItem.class, new RecommendationItemAdapter())
                            .registerTypeAdapter(DeviceDetails.class, new DeviceDetailsAdapter())
                            .setExclusionStrategies(strategy)
                            .create();
                    gsonStr = gsonObj.toJson(recommendationList);
                }
                response.getWriter().println(gsonStr);
                response.getWriter().close();
            }
        } catch (Exception e) {
            LOGGER.error("Exception: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } finally {
            MetricsConfig.timerListRec = MetricsConfig.timerBListRec.tag("status", statusValue)
                    .register(MetricsConfig.meterRegistry());
            timerListRec.stop(MetricsConfig.timerListRec);
        }
    }

    /**
     * Generates recommendations using the V1 schema
     *
     * @param request  an {@link HttpServletRequest} object that contains the request the client has made
     * @param response an {@link HttpServletResponse} object that contains the response the servlet sends to the client
     * @throws ServletException
     * @throws IOException
     */
    @Operation(
            summary = "Update recommendations (V1)",
            description = "Generate recommendations for experiments with new schema including replicas, nested resources, and pod_count metrics"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Successfully generated recommendations",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ListRecommendationsAPIObject.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid parameters",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = KruizeResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = KruizeResponse.class)
                    )
            )
    })
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int requestCount = 0;
        int calCount = ++requestCount;
        LOGGER.debug("ListRecommendationsV1.doPost() called - count: {}", calCount);
        String statusValue = KruizeConstants.APIMessages.FAILURE;
        Timer.Sample timerBUpdateRecommendations = Timer.start(MetricsConfig.meterRegistry());
        request.setCharacterEncoding(CHARACTER_ENCODING);
        
        String experiment_name = request.getParameter(KruizeConstants.JSONKeys.EXPERIMENT_NAME);
        String intervalEndTimeStr = request.getParameter(KruizeConstants.JSONKeys.INTERVAL_END_TIME);
        String intervalStartTimeStr = request.getParameter(KruizeConstants.JSONKeys.INTERVAL_START_TIME);
        Timestamp interval_end_time;
        
        if (KruizeDeploymentInfo.log_http_req_resp)
            LOGGER.info("UpdateRecommendations V1 input params - experiment: {}, start: {}, end: {}",
                    experiment_name, intervalStartTimeStr, intervalEndTimeStr);
        
        try {
            // Create recommendation engine object
            RecommendationEngine recommendationEngine = new RecommendationEngine(experiment_name, intervalEndTimeStr, intervalStartTimeStr);
            // Validate and create KruizeObject if successful
            String validationMessage = recommendationEngine.validate();
            if (validationMessage.isEmpty()) {
                KruizeObject kruizeObject = recommendationEngine.prepareRecommendations(calCount, REMOTE, null);
                if (kruizeObject.getValidation_data().isSuccess()) {
                    LOGGER.debug("Update recommendations V1 success - count: {}", calCount);
                    interval_end_time = Utils.DateUtils.getTimeStampFrom(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT,
                            intervalEndTimeStr);
                    SimpleDateFormat sdf = new SimpleDateFormat(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, Locale.ROOT);
                    sdf.setTimeZone(TimeZone.getTimeZone(KruizeConstants.TimeUnitsExt.TimeZones.UTC));
                    LOGGER.info("Update recommendations V1 success for experiment: {}, time: {}",
                            experiment_name, sdf.format(interval_end_time));
                    sendSuccessResponseV1(response, kruizeObject, interval_end_time);
                    statusValue = KruizeConstants.APIMessages.SUCCESS;
                } else {
                    LOGGER.error("Update recommendations V1 failed - count: {}", calCount);
                    sendErrorResponseForPost(response, null, kruizeObject.getValidation_data().getErrorCode(),
                            kruizeObject.getValidation_data().getMessage(), experiment_name, intervalEndTimeStr);
                }
            } else {
                sendErrorResponseForPost(response, null, HttpServletResponse.SC_BAD_REQUEST, validationMessage,
                        experiment_name, intervalEndTimeStr);
            }
        } catch (FetchMetricsError e) {
            sendErrorResponseForPost(response, new Exception(e), HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    e.getMessage(), experiment_name, intervalEndTimeStr);
        } catch (Exception e) {
            LOGGER.error("Update recommendations V1 failed - count: {}", calCount);
            e.printStackTrace();
            sendErrorResponseForPost(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    e.getMessage(), experiment_name, intervalEndTimeStr);
        } finally {
            LOGGER.debug("Update recommendations V1 completed - count: {}", calCount);
            if (null != timerBUpdateRecommendations) {
                MetricsConfig.timerUpdateRecomendations = MetricsConfig.timerBUpdateRecommendations
                        .tag(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.STATUS, statusValue)
                        .register(MetricsConfig.meterRegistry());
                timerBUpdateRecommendations.stop(MetricsConfig.timerUpdateRecomendations);
            }
        }
    }

    private void sendSuccessResponseV1(HttpServletResponse response, KruizeObject ko, Timestamp interval_end_time) throws IOException {
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        response.setStatus(HttpServletResponse.SC_CREATED);
        List<ListRecommendationsAPIObject> recommendationList = new ArrayList<>();
        try {
            LOGGER.debug(ko.getKubernetes_objects().toString());
            // Use V1 converter for new schema with replicas and pod_count
            ListRecommendationsAPIObject listRecommendationsAPIObject =
                    Converters.KruizeObjectConverters.convertKruizeObjectToListRecommendationSOV1(
                            ko, false, false, interval_end_time);
            recommendationList.add(listRecommendationsAPIObject);
        } catch (Exception e) {
            LOGGER.error("Failed to generate V1 recommendation for experiment: {}, error: {}",
                    ko.getExperimentName(), e.getMessage());
        }
        
        ExclusionStrategy strategy = new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes field) {
                return field.getDeclaringClass() == ContainerData.class && (field.getName().equals(KruizeConstants.JSONKeys.RESULTS))
                        || (field.getDeclaringClass() == ContainerAPIObject.class && (field.getName().equals(KruizeConstants.JSONKeys.METRICS)));
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        };
        
        String gsonStr = "[]";
        if (!recommendationList.isEmpty()) {
            Gson gsonObj = new GsonBuilder()
                    .disableHtmlEscaping()
                    .setPrettyPrinting()
                    .enableComplexMapKeySerialization()
                    .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                    .registerTypeAdapter(AnalyzerConstants.RecommendationItem.class, new RecommendationItemAdapter())
                    .registerTypeAdapter(DeviceDetails.class, new DeviceDetailsAdapter())
                    .registerTypeAdapter(MetricMetadata.class, new MetricMetadataAdapter())
                    .setExclusionStrategies(strategy)
                    .create();
            gsonStr = gsonObj.toJson(recommendationList);
        }
        if (KruizeDeploymentInfo.log_http_req_resp)
            LOGGER.info("UpdateRecommendations V1 response: {}", new Gson().toJson(JsonParser.parseString(gsonStr)));
        response.getWriter().println(gsonStr);
        response.getWriter().close();
    }

    private void sendErrorResponse(HttpServletResponse response, Exception e, int httpStatusCode, String errorMsg)
            throws IOException {
        if (null != e) {
            LOGGER.error(e.toString());
            e.printStackTrace();
        }
        response.sendError(httpStatusCode, errorMsg);
    }

    private void sendErrorResponseForPost(HttpServletResponse response, Exception e, int httpStatusCode, String errorMsg,
                                          String experiment_name, String intervalEndTimeStr) throws IOException {
        if (null != e) {
            e.printStackTrace();
            LOGGER.error(e.toString());
            if (null == errorMsg) errorMsg = e.getMessage();
        }
        LOGGER.error("Update recommendations V1 failure for experiment: {}, intervalEndTime: {}, error: {}",
                experiment_name, intervalEndTimeStr, errorMsg);
        response.sendError(httpStatusCode, errorMsg);
    }
}
