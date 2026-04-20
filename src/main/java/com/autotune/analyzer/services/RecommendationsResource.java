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
import com.autotune.analyzer.serviceObjects.ListRecommendationsAPIObject;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.MetricsConfig;
import com.google.gson.*;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
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
public class RecommendationsResource extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(RecommendationsResource.class);
    private static int requestCount = 0;

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
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        LOGGER.info("RecommendationsResource GET request received");
        String statusValue = "failure";
        Timer.Sample timerListRec = Timer.start(MetricsConfig.meterRegistry());
        
        // This endpoint delegates to ListRecommendations but uses V1 converter
        // Call ListRecommendations.doGet() which will handle all the logic
        ListRecommendations listRecommendations = new ListRecommendations();
        HttpServletRequest v1Request = new RecommendationV1RequestWrapper(request);
        try {
            listRecommendations.init(getServletConfig());
            listRecommendations.doGet(v1Request, response);
            statusValue = "success";
        } catch (Exception e) {
            LOGGER.error("Exception in RecommendationsResource GET: {}", e.getMessage());
            e.printStackTrace();
            sendErrorResponse(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } finally {
            if (null != timerListRec) {
                MetricsConfig.timerListRec = MetricsConfig.timerBListRec.tag("status", statusValue)
                        .register(MetricsConfig.meterRegistry());
                timerListRec.stop(MetricsConfig.timerListRec);
            }
        }
    }

    /**
     * Generates recommendations using the V1 schema
     * Supports both remote and local monitoring modes via the "target" query parameter.
     *
     * @param request  an {@link HttpServletRequest} object that contains the request the client has made
     * @param response an {@link HttpServletResponse} object that contains the response the servlet sends to the client
     * @throws ServletException
     * @throws IOException
     */
    @Operation(
            summary = "Update recommendations (V1)",
            description = "Generate recommendations for experiments with new schema including replicas, nested resources, and pod_count metrics. " +
                    "Supports 'target' query parameter: 'remote' (default) for remote monitoring, 'local' for local monitoring."
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
                    description = "Bad request - Invalid parameters or invalid target cluster",
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
        int calCount = ++requestCount;
        LOGGER.info("RecommendationsResource POST request received - count: {}", calCount);
        String statusValue = KruizeConstants.APIMessages.FAILURE;
        Timer.Sample timerBUpdateRecommendations = Timer.start(MetricsConfig.meterRegistry());
        
        try {
            // Get the target parameter (default to "remote" if not specified)
            String target = request.getParameter("target");
            
            // Normalize target value: treat null, empty, or blank as "remote"
            if (target == null || target.trim().isEmpty()) {
                target = "remote";
            } else {
                target = target.trim().toLowerCase();
            }
            
            LOGGER.info("RecommendationsResource POST - target: {}", target);
            
            // Route to appropriate flow based on target
            if ("remote".equals(target)) {
                // Remote monitoring mode - delegate to UpdateRecommendations
                LOGGER.debug("Using remote monitoring mode (UpdateRecommendations flow)");
                UpdateRecommendations updateRecommendations = new UpdateRecommendations();
                HttpServletRequest v1Request = new RecommendationV1RequestWrapper(request);
                updateRecommendations.init(getServletConfig());
                updateRecommendations.doPost(v1Request, response);
                statusValue = KruizeConstants.APIMessages.SUCCESS;
            } else if ("local".equals(target)) {
                // Local monitoring mode - delegate to GenerateRecommendations
                LOGGER.debug("Using local monitoring mode (GenerateRecommendations flow)");
                GenerateRecommendations generateRecommendations = new GenerateRecommendations();
                HttpServletRequest v1Request = new RecommendationV1RequestWrapper(request);
                generateRecommendations.init(getServletConfig());
                generateRecommendations.doPost(v1Request, response);
                statusValue = KruizeConstants.APIMessages.SUCCESS;
            } else {
                // Invalid target value
                String errorMsg = String.format("Invalid target cluster: '%s'. Valid values are 'remote' or 'local'.", target);
                LOGGER.error(errorMsg);
                sendErrorResponse(response, null, HttpServletResponse.SC_BAD_REQUEST, errorMsg);
            }
        } catch (Exception e) {
            LOGGER.error("Exception in RecommendationsResource POST - count: {}", calCount);
            e.printStackTrace();
            sendErrorResponse(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } finally {
            LOGGER.debug("RecommendationsResource POST completed - count: {}", calCount);
            if (null != timerBUpdateRecommendations) {
                MetricsConfig.timerUpdateRecomendations = MetricsConfig.timerBUpdateRecommendations
                        .tag(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.STATUS, statusValue)
                        .register(MetricsConfig.meterRegistry());
                timerBUpdateRecommendations.stop(MetricsConfig.timerUpdateRecomendations);
            }
        }
    }

    private void sendErrorResponse(HttpServletResponse response, Exception e, int httpStatusCode, String errorMsg)
            throws IOException {
        if (null != e) {
            LOGGER.error(e.toString());
            e.printStackTrace();
            if (null == errorMsg) errorMsg = e.getMessage();
        }
        response.sendError(httpStatusCode, errorMsg);
    }

}

/**
 * Request wrapper used by the V1 recommendations endpoint to mark delegated requests
 * as V1-originated without changing the existing servlet method signatures.
 *
 * <p>When {@link RecommendationsResource} forwards a request to the existing
 * recommendation servlets, this wrapper overrides {@code getParameter("useV1Converter")}
 * and returns {@code true}. This allows the downstream servlets to switch to the
 * V1 recommendation converter only for requests coming through the new API, while
 * preserving the default V0/standard behavior for direct calls to the existing APIs.
 */
class RecommendationV1RequestWrapper extends javax.servlet.http.HttpServletRequestWrapper {
    private static final String USE_V1_CONVERTER_PARAM = "useV1Converter";

    public RecommendationV1RequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getParameter(String name) {
        if (USE_V1_CONVERTER_PARAM.equals(name)) {
            return Boolean.TRUE.toString();
        }
        return super.getParameter(name);
    }
}
