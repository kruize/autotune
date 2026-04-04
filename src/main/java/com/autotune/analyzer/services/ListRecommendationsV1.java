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

import com.autotune.analyzer.exceptions.KruizeResponse;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.serviceObjects.Converters;
import com.autotune.analyzer.serviceObjects.ListRecommendationsAPIObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.analyzer.utils.GsonUTCDateAdapter;
import com.autotune.analyzer.utils.ServiceHelpers;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.MetricsConfig;
import com.autotune.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

/**
 * REST API v1 endpoint for listing recommendations with new schema.
 * This endpoint supports the new recommendation schema with replicas, nested resources,
 * and metrics_info containing pod_count metrics.
 * 
 * Endpoint: /kruize/api/v1/recommendations
 * Query Parameters:
 *   - experiment_name: Name of the experiment (optional)
 *   - container_name: Name of the container (optional)
 *   - namespace: Namespace of the experiment (optional)
 *   - latest: Get latest recommendation (default: true)
 *   - monitoring_end_time: Specific timestamp for recommendations (optional)
 */
@WebServlet(asyncSupported = true)
@Tag(name = "Recommendations V1", description = "API v1 for listing recommendations with new schema")
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

        // Extract query parameters
        String experimentName = request.getParameter(AnalyzerConstants.ServiceConstants.EXPERIMENT_NAME);
        String containerName = request.getParameter("container_name");
        String namespace = request.getParameter("namespace");
        String latestRecommendation = request.getParameter(AnalyzerConstants.ServiceConstants.LATEST);
        String monitoringEndTime = request.getParameter(KruizeConstants.JSONKeys.MONITORING_END_TIME);

        Timestamp monitoringEndTimestamp = null;
        Map<String, KruizeObject> mKruizeExperimentMap = new ConcurrentHashMap<>();

        boolean getLatest = true;
        boolean checkForTimestamp = false;
        boolean error = false;

        // Parse latest parameter
        if (null != latestRecommendation
                && !latestRecommendation.isEmpty()
                && latestRecommendation.equalsIgnoreCase(AnalyzerConstants.BooleanString.FALSE)) {
            getLatest = false;
        }

        List<KruizeObject> kruizeObjectList = new ArrayList<>();
        
        try {
            // TODO: Implement the logic to fetch recommendations using new schema
            // This is a skeleton implementation - actual logic will be added in subsequent commits
            
            if (null != experimentName) {
                experimentName = experimentName.trim();
                try {
                    new ExperimentDBService().loadLMExperimentAndRecommendationsFromDBByName(
                            mKruizeExperimentMap, experimentName, null);
                } catch (Exception e) {
                    LOGGER.error("Loading saved experiment {} failed: {} ", experimentName, e.getMessage());
                }

                if (mKruizeExperimentMap.containsKey(experimentName)) {
                    // Handle timestamp validation
                    if (null != monitoringEndTime && !monitoringEndTime.isEmpty()) {
                        monitoringEndTime = monitoringEndTime.trim();
                        if (Utils.DateUtils.isAValidDate(
                                KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, monitoringEndTime)) {
                            Date mEndTime = Utils.DateUtils.getDateFrom(
                                    KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, monitoringEndTime);
                            monitoringEndTimestamp = new Timestamp(mEndTime.getTime());
                            
                            boolean timestampExists = ServiceHelpers.KruizeObjectOperations
                                    .checkRecommendationTimestampExists(
                                            mKruizeExperimentMap.get(experimentName), monitoringEndTime);
                            
                            if (timestampExists) {
                                checkForTimestamp = true;
                            } else {
                                error = true;
                                sendErrorResponse(
                                        response,
                                        new Exception(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.RECOMMENDATION_DOES_NOT_EXIST_EXCPTN),
                                        HttpServletResponse.SC_BAD_REQUEST,
                                        String.format(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.RECOMMENDATION_DOES_NOT_EXIST_MSG, monitoringEndTime)
                                );
                            }
                        } else {
                            error = true;
                            sendErrorResponse(
                                    response,
                                    new Exception(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_TIMESTAMP_EXCPTN),
                                    HttpServletResponse.SC_BAD_REQUEST,
                                    String.format(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_TIMESTAMP_MSG, monitoringEndTime)
                            );
                        }
                    }
                    kruizeObjectList.add(mKruizeExperimentMap.get(experimentName));
                } else {
                    error = true;
                    sendErrorResponse(
                            response,
                            new Exception(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_EXPERIMENT_NAME_EXCPTN),
                            HttpServletResponse.SC_BAD_REQUEST,
                            String.format(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_EXPERIMENT_NAME_MSG, experimentName)
                    );
                }
            } else {
                // Load all experiments
                try {
                    new ExperimentDBService().loadAllLMExperimentsAndRecommendations(mKruizeExperimentMap, null);
                } catch (Exception e) {
                    LOGGER.error("Loading saved experiments failed: {} ", e.getMessage());
                }
                kruizeObjectList.addAll(mKruizeExperimentMap.values());
            }

            if (!error) {
                List<ListRecommendationsAPIObject> recommendationList = new ArrayList<>();
                for (KruizeObject ko : kruizeObjectList) {
                    try {
                        // Convert KruizeObject to ListRecommendationsAPIObject using new schema
                        ListRecommendationsAPIObject listRecommendationsAPIObject = 
                                Converters.KruizeObjectConverters.convertKruizeObjectToListRecommendationSO(
                                        ko,
                                        getLatest,
                                        checkForTimestamp,
                                        monitoringEndTimestamp);
                        recommendationList.add(listRecommendationsAPIObject);
                        statusValue = "success";
                    } catch (Exception e) {
                        LOGGER.error("Not able to generate recommendation for expName : {} due to {}", 
                                ko.getExperimentName(), e.getMessage());
                    }
                }

                String gsonStr = "[]";
                if (recommendationList.size() > 0) {
                    Gson gsonObj = new GsonBuilder()
                            .disableHtmlEscaping()
                            .setPrettyPrinting()
                            .enableComplexMapKeySerialization()
                            .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                            .create();
                    gsonStr = gsonObj.toJson(recommendationList);
                }

                PrintWriter out = response.getWriter();
                out.append(gsonStr);
                out.flush();
            }
        } catch (Exception e) {
            LOGGER.error("Error processing recommendations: {}", e.getMessage());
            sendErrorResponse(
                    response,
                    e,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Internal server error while processing recommendations"
            );
        } finally {
            if (null != timerListRec) {
                MetricsConfig.timerListRec = MetricsConfig.timerBListRec.tag("status", statusValue)
                        .register(MetricsConfig.meterRegistry());
                timerListRec.stop(MetricsConfig.timerListRec);
            }
        }
    }

    private void sendErrorResponse(HttpServletResponse response, Exception e, int httpStatusCode, String errorMsg)
            throws IOException {
        if (null != e) {
            LOGGER.error(e.toString());
            e.printStackTrace();
        }
        response.sendError(httpStatusCode, errorMsg);
    }
}
