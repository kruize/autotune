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
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        LOGGER.info("ListRecommendationsV1 GET request received");
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
        LOGGER.info("ListRecommendationsV1 POST request received");
    }

}
