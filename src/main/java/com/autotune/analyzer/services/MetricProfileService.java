/*******************************************************************************
 * Copyright (c) 2024 Red Hat, IBM Corporation and others.
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

import com.autotune.analyzer.exceptions.InvalidValueException;
import com.autotune.analyzer.exceptions.PerformanceProfileResponse;
import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.analyzer.performanceProfiles.utils.PerformanceProfileUtil;
import com.autotune.analyzer.serviceObjects.Converters;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.analyzer.utils.GsonUTCDateAdapter;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.data.metrics.Metric;
import com.autotune.database.service.ExperimentDBService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.*;
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
import java.io.Serial;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

/**
 * REST API to create metric profile .
 */
@WebServlet(asyncSupported = true)
public class MetricProfileService extends HttpServlet {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricProfileService.class);
    private ConcurrentHashMap<String, PerformanceProfile> metricProfilesMap;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        metricProfilesMap = (ConcurrentHashMap<String, PerformanceProfile>) getServletContext()
                .getAttribute(AnalyzerConstants.PerformanceProfileConstants.METRIC_PROFILE_MAP);
    }

    /**
     * Validate and create new Metric Profile.
     *
     * @param request
     * @param response
     * @throws IOException
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Map<String, PerformanceProfile> metricProfilesMap = new ConcurrentHashMap<>();
            String inputData = request.getReader().lines().collect(Collectors.joining());
            PerformanceProfile metricProfile = Converters.KruizeObjectConverters.convertInputJSONToCreateMetricProfile(inputData);
            ValidationOutputData validationOutputData = PerformanceProfileUtil.validateAndAddMetricProfile(metricProfilesMap, metricProfile);
            if (validationOutputData.isSuccess()) {
                ValidationOutputData addedToDB = new ExperimentDBService().addMetricProfileToDB(metricProfile);
                if (addedToDB.isSuccess()) {
                    metricProfilesMap.put(String.valueOf(metricProfile.getMetadata().get("name")), metricProfile);
                    getServletContext().setAttribute(AnalyzerConstants.PerformanceProfileConstants.METRIC_PROFILE_MAP, metricProfilesMap);
                    LOGGER.debug("Added Metric Profile : {} into the DB with version: {}",
                            metricProfile.getMetadata().get("name"), metricProfile.getProfile_version());
                    sendSuccessResponse(response, "Metric Profile : " + metricProfile.getMetadata().get("name") + " created successfully.");
                } else {
                    sendErrorResponse(response, null, HttpServletResponse.SC_BAD_REQUEST, addedToDB.getMessage());
                }
            } else
                sendErrorResponse(response, null, validationOutputData.getErrorCode(), validationOutputData.getMessage());
        } catch (Exception e) {
            sendErrorResponse(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Validation failed: " + e.getMessage());
        } catch (InvalidValueException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get List of Metric Profiles
     *
     * @param req
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        response.setStatus(HttpServletResponse.SC_OK);
        String gsonStr = "[]";
        // Fetch all metric profiles from the DB
        try {
            new ExperimentDBService().loadAllMetricProfiles(metricProfilesMap);
        } catch (Exception e) {
            LOGGER.error("Failed to load saved metric profile data: {} ", e.getMessage());
        }
        if (metricProfilesMap.size() > 0) {
            Collection<PerformanceProfile> values = metricProfilesMap.values();
            Gson gsonObj = new GsonBuilder()
                    .disableHtmlEscaping()
                    .setPrettyPrinting()
                    .enableComplexMapKeySerialization()
                    .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                    // a custom serializer for serializing metadata of JsonNode type.
                    .registerTypeAdapter(JsonNode.class, new JsonSerializer<JsonNode>() {
                        @Override
                        public JsonElement serialize(JsonNode jsonNode, Type typeOfSrc, JsonSerializationContext context) {
                            if (jsonNode instanceof ObjectNode) {
                                ObjectNode objectNode = (ObjectNode) jsonNode;
                                JsonObject metadataJson = new JsonObject();

                                // Extract the "name" field directly if it exists
                                if (objectNode.has("name")) {
                                    metadataJson.addProperty("name", objectNode.get("name").asText());
                                }

                                return metadataJson;
                            }
                            return context.serialize(jsonNode);
                        }
                    })
                    .setExclusionStrategies(new ExclusionStrategy() {
                        @Override
                        public boolean shouldSkipField(FieldAttributes f) {
                            return f.getDeclaringClass() == Metric.class && (
                                    f.getName().equals("trialSummaryResult")
                                            || f.getName().equals("cycleDataMap")
                            );
                        }

                        @Override
                        public boolean shouldSkipClass(Class<?> aClass) {
                            return false;
                        }
                    })
                    .create();
            gsonStr = gsonObj.toJson(values);
        } else {
            LOGGER.debug(AnalyzerErrorConstants.AutotuneObjectErrors.NO_PERF_PROFILE);
        }
        response.getWriter().println(gsonStr);
        response.getWriter().close();
    }

    /**
     * TODO: Need to implement
     * Update Metric Profile
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPut(req, resp);
    }

    /**
     * TODO: Need to implement
     * Delete Metric profile
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doDelete(req, resp);
    }

    /**
     * Send success response in case of no errors or exceptions.
     *
     * @param response
     * @param message
     * @throws IOException
     */
    private void sendSuccessResponse(HttpServletResponse response, String message) throws IOException {
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        response.setStatus(HttpServletResponse.SC_CREATED);
        PrintWriter out = response.getWriter();
        out.append(
                new Gson().toJson(
                        new PerformanceProfileResponse(message +
                                " View Metric Profiles at /listMetricProfiles",
                                HttpServletResponse.SC_CREATED, "", "SUCCESS")
                )
        );
        out.flush();
    }

    /**
     * Send response containing corresponding error message in case of failures and exceptions
     *
     * @param response
     * @param e
     * @param httpStatusCode
     * @param errorMsg
     * @throws IOException
     */
    public void sendErrorResponse(HttpServletResponse response, Exception e, int httpStatusCode, String errorMsg) throws
            IOException {
        if (null != e) {
            LOGGER.error(e.toString());
            if (null == errorMsg)
                errorMsg = e.getMessage();
        }
        response.sendError(httpStatusCode, errorMsg);
    }
}
