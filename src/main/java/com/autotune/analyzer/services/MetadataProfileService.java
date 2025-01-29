/*******************************************************************************
 * Copyright (c) 2025 Red Hat, IBM Corporation and others.
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
import com.autotune.analyzer.adapters.RecommendationItemAdapter;
import com.autotune.analyzer.exceptions.InvalidValueException;
import com.autotune.analyzer.exceptions.MetadataProfileResponse;
import com.autotune.analyzer.metadataProfiles.MetadataProfile;
import com.autotune.analyzer.metadataProfiles.MetadataProfileCollection;
import com.autotune.analyzer.metadataProfiles.utils.MetadataProfileUtil;
import com.autotune.analyzer.serviceObjects.Converters;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.analyzer.utils.GsonUTCDateAdapter;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.data.metrics.Metric;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.system.info.device.DeviceDetails;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.KruizeSupportedTypes;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

@WebServlet(asyncSupported = true)
public class MetadataProfileService extends HttpServlet{
    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataProfileService.class);
    private ConcurrentHashMap<String, MetadataProfile> metadataProfilesMap;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        metadataProfilesMap = (ConcurrentHashMap<String, MetadataProfile>) getServletContext()
                .getAttribute(AnalyzerConstants.MetadataProfileConstants.METADATA_PROFILE_MAP);
    }

    /**
     * Validate and create new metadata Profile.
     *
     * @param request
     * @param response
     * @throws IOException
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Map<String, MetadataProfile> metadataProfilesMap = new ConcurrentHashMap<>();
            String inputData = request.getReader().lines().collect(Collectors.joining());
            MetadataProfile metadataProfile = Converters.KruizeObjectConverters.convertInputJSONToCreateMetadataProfile(inputData);
            ValidationOutputData validationOutputData = MetadataProfileUtil.validateAndAddMetadataProfile(metadataProfilesMap, metadataProfile);
            if (validationOutputData.isSuccess()) {
                ValidationOutputData addedToDB = new ExperimentDBService().addMetadataProfileToDB(metadataProfile);
                if (addedToDB.isSuccess()) {
                    metadataProfilesMap.put(String.valueOf(metadataProfile.getMetadata().get("name")), metadataProfile);
                    getServletContext().setAttribute(AnalyzerConstants.MetadataProfileConstants.METADATA_PROFILE_MAP, metadataProfilesMap);
                    LOGGER.debug(KruizeConstants.MetadataProfileAPIMessages.ADD_METADATA_PROFILE_TO_DB_WITH_VERSION,
                            metadataProfile.getMetadata().get("name").asText(), metadataProfile.getProfile_version());
                    // Store metadata profile in-memory collection
                    MetadataProfileCollection metadataProfileCollection = MetadataProfileCollection.getInstance();
                    metadataProfileCollection.addMetadataProfile(metadataProfile);

                    sendSuccessResponse(response, String.format(KruizeConstants.MetadataProfileAPIMessages.CREATE_METADATA_PROFILE_SUCCESS_MSG, metadataProfile.getMetadata().get("name").asText()));
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
     * Get List of Metadata Profiles
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        response.setStatus(HttpServletResponse.SC_OK);
        String gsonStr = "[]";

        ConcurrentHashMap<String, MetadataProfile> metadataProfilesMap = new ConcurrentHashMap<>();
        String metadataProfileName = request.getParameter(AnalyzerConstants.MetadataProfileConstants.METADATA_PROFILE_NAME);
        String verbose = request.getParameter(AnalyzerConstants.ServiceConstants.VERBOSE);
        String internalVerbose = "false";
        boolean error = false;

        // validate Query params
        Set<String> invalidParams = new HashSet<>();
        for (String param : request.getParameterMap().keySet()) {
            if (!KruizeSupportedTypes.LIST_METADATA_PROFILES_QUERY_PARAMS_SUPPORTED.contains(param)) {
                invalidParams.add(param);
            }
        }

        // Fetch metadata profiles based on the query parameters using the in-memory storage collection
        try {
            if (invalidParams.isEmpty()) {
                if (null != verbose) {
                    internalVerbose = verbose;
                }

                try {
                    if (null != metadataProfileName && !metadataProfileName.isEmpty()) {
                        internalVerbose = "true";
                        loadMetadataProfilesFromCollection(metadataProfilesMap, metadataProfileName);
                    } else {
                        loadAllMetadataProfilesFromCollection(metadataProfilesMap);
                    }

                    // Check if metadata profile exists
                    if (metadataProfileName != null && !metadataProfilesMap.containsKey(metadataProfileName)) {
                        error = true;
                        sendErrorResponse(
                                response,
                                new Exception(AnalyzerErrorConstants.APIErrors.ListMetadataProfileAPI.INVALID_METADATA_PROFILE_NAME_EXCPTN),
                                HttpServletResponse.SC_BAD_REQUEST,
                                String.format(AnalyzerErrorConstants.APIErrors.ListMetadataProfileAPI.INVALID_METADATA_PROFILE_NAME_MSG, metadataProfileName)
                        );
                    } else if (null == metadataProfileName && metadataProfilesMap.isEmpty()) {
                        error = true;
                        sendErrorResponse(
                                response,
                                new Exception(AnalyzerErrorConstants.APIErrors.ListMetadataProfileAPI.NO_METADATA_PROFILES_EXCPTN),
                                HttpServletResponse.SC_BAD_REQUEST,
                                AnalyzerErrorConstants.APIErrors.ListMetadataProfileAPI.NO_METADATA_PROFILES
                        );
                    }

                    if (!error) {
                        Collection<MetadataProfile> values = metadataProfilesMap.values();
                        // create Gson Object
                        Gson gsonObj = createGsonObject();

                        if (internalVerbose.equals("false")) {
                            Collection<JsonObject> filteredValues = new ArrayList<>();
                            for(MetadataProfile metadataProfile : values) {
                                JsonObject jsonObject = new JsonObject();
                                jsonObject.addProperty("name", metadataProfile.getMetadata().get("name").asText());
                                filteredValues.add(jsonObject);
                            }
                            gsonStr = gsonObj.toJson(filteredValues);
                        } else {
                            gsonStr = gsonObj.toJson(values);
                        }
                        response.getWriter().println(gsonStr);
                        response.getWriter().close();
                    }
                } catch (Exception e) {
                    LOGGER.error("Exception: {}", e.getMessage());
                    e.printStackTrace();
                    sendErrorResponse(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                }
            } else {
                sendErrorResponse(
                        response,
                        new Exception(AnalyzerErrorConstants.APIErrors.ListMetadataProfileAPI.INVALID_QUERY_PARAM),
                        HttpServletResponse.SC_BAD_REQUEST,
                        String.format(AnalyzerErrorConstants.APIErrors.ListMetadataProfileAPI.INVALID_QUERY_PARAM, invalidParams)
                );
            }
        } catch (Exception e) {
            LOGGER.error(KruizeConstants.MetadataProfileAPIMessages.LOAD_METADATA_PROFILE_FAILURE, e.getMessage());
            sendErrorResponse(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * TODO: Need to implement
     * Update Metadata Profile
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
                        new MetadataProfileResponse(message +
                                KruizeConstants.MetadataProfileAPIMessages.VIEW_METADATA_PROFILES_MSG,
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

    private void loadMetadataProfilesFromCollection(Map<String, MetadataProfile> metadataProfilesMap, String metadataProfileName) {
        try {
            MetadataProfileCollection metadataProfileCollection = MetadataProfileCollection.getInstance();
            if (null != metadataProfileName && !metadataProfileName.isEmpty()) {
                MetadataProfile metadataProfile = metadataProfileCollection.getMetadataProfileCollection().get(metadataProfileName);
                metadataProfilesMap.put(metadataProfileName, metadataProfile);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load saved metadata profile data: {} ", e.getMessage());
        }
    }

    private void loadAllMetadataProfilesFromCollection(Map<String, MetadataProfile> metadataProfilesMap) {
        try {
            MetadataProfileCollection metadataProfileCollection = MetadataProfileCollection.getInstance();
            metadataProfilesMap.putAll(metadataProfileCollection.getMetadataProfileCollection());
        } catch (Exception e) {
            LOGGER.error("Failed to load all the metadata profiles data: {} ", e.getMessage());
        }
    }

    private Gson createGsonObject() {
        return new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .enableComplexMapKeySerialization()
                .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                .registerTypeAdapter(AnalyzerConstants.RecommendationItem.class, new RecommendationItemAdapter())
                .registerTypeAdapter(DeviceDetails.class, new DeviceDetailsAdapter())
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
                        ) ||
                                f.getDeclaringClass() == ContainerData.class && (
                                        f.getName().equalsIgnoreCase("metrics")
                                );
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> aClass) {
                        return false;
                    }
                })
                .create();
    }
}