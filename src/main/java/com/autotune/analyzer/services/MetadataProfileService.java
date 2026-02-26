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
import com.autotune.database.dao.ExperimentDAOImpl;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.KruizeSupportedTypes;
import com.autotune.utils.MetricsConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
                    metadataProfilesMap.put(String.valueOf(metadataProfile.getMetadata().get(KruizeConstants.JSONKeys.NAME)), metadataProfile);
                    getServletContext().setAttribute(AnalyzerConstants.MetadataProfileConstants.METADATA_PROFILE_MAP, metadataProfilesMap);
                    LOGGER.debug(KruizeConstants.MetadataProfileAPIMessages.ADD_METADATA_PROFILE_TO_DB_WITH_VERSION,
                            metadataProfile.getMetadata().get(KruizeConstants.JSONKeys.NAME).asText(), metadataProfile.getProfile_version());
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
            sendErrorResponse(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, KruizeConstants.MetadataProfileConstants.METADATA_PROFILE_VALIDATION_FAILURE + e.getMessage());
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
        boolean verbose = Boolean.parseBoolean(request.getParameter(AnalyzerConstants.ServiceConstants.VERBOSE));

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

                try {
                    loadMetadataProfilesFromCollection(metadataProfilesMap, metadataProfileName);

                    // Check if metadata profile exists
                    if (null != metadataProfileName && !metadataProfilesMap.containsKey(metadataProfileName)) {
                        sendErrorResponse(
                                response,
                                new Exception(AnalyzerErrorConstants.APIErrors.ListMetadataProfileAPI.INVALID_METADATA_PROFILE_NAME_EXCPTN),
                                HttpServletResponse.SC_BAD_REQUEST,
                                String.format(AnalyzerErrorConstants.APIErrors.ListMetadataProfileAPI.INVALID_METADATA_PROFILE_NAME_MSG, metadataProfileName)
                        );
                    } else if (null == metadataProfileName && metadataProfilesMap.isEmpty()) {
                        sendErrorResponse(
                                response,
                                new Exception(AnalyzerErrorConstants.APIErrors.ListMetadataProfileAPI.NO_METADATA_PROFILES_EXCPTN),
                                HttpServletResponse.SC_BAD_REQUEST,
                                AnalyzerErrorConstants.APIErrors.ListMetadataProfileAPI.NO_METADATA_PROFILES
                        );
                    } else {
                        Collection<MetadataProfile> values = metadataProfilesMap.values();
                        // create Gson Object
                        Gson gsonObj = createGsonObject();

                        if(null != metadataProfileName || verbose) {
                            gsonStr = gsonObj.toJson(values);
                        } else {
                            Collection<JsonObject> filteredValues = new ArrayList<>();
                            for(MetadataProfile metadataProfile : values) {
                                JsonObject jsonObject = new JsonObject();
                                jsonObject.addProperty(KruizeConstants.JSONKeys.NAME, metadataProfile.getMetadata().get(KruizeConstants.JSONKeys.NAME).asText());
                                filteredValues.add(jsonObject);
                            }
                            gsonStr = gsonObj.toJson(filteredValues);
                        }
                        response.getWriter().println(gsonStr);
                        response.getWriter().close();
                    }
                } catch (Exception e) {
                    LOGGER.error(AnalyzerErrorConstants.APIErrors.ListMetadataProfileAPI.LOAD_METADATA_PROFILE_ERROR, e.getMessage());
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
            LOGGER.error(AnalyzerErrorConstants.APIErrors.ListMetadataProfileAPI.LOAD_METADATA_PROFILE_ERROR, e.getMessage());
            sendErrorResponse(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Update Metadata Profile
     * Handles the PUT request for updating metadata profile - PUT /updateMetadataProfile
     * <p>
     * Supported Query Parameters -
     * name	- metadata profile name to be updated(required)
     *
     * @param request
     * @param response
     * @throws IOException
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String statusValue = "failure";
        Timer.Sample timerUpdateMetadataProfile = Timer.start(MetricsConfig.meterRegistry());

        Map<String, MetadataProfile> metadataProfilesMap = new ConcurrentHashMap<>();
        String metadataProfileName = request.getParameter(AnalyzerConstants.MetadataProfileConstants.METADATA_PROFILE_NAME);

        String inputData = request.getReader().lines().collect(Collectors.joining());

        if (null == metadataProfileName || metadataProfileName.isEmpty()) {
            sendErrorResponseMessage(
                    response,
                    new Exception(AnalyzerErrorConstants.APIErrors.UpdateMetadataProfileAPI.MISSING_METADATA_PROFILE_NAME_EXCPTN),
                    HttpServletResponse.SC_BAD_REQUEST,
                    String.format(AnalyzerErrorConstants.APIErrors.UpdateMetadataProfileAPI.MISSING_METADATA_PROFILE_NAME_MSG)
            );
            return;
        }

        if (inputData.isEmpty()) {
            sendErrorResponseMessage(
                    response,
                    new Exception(AnalyzerErrorConstants.APIErrors.UpdateMetadataProfileAPI.MISSING_INPUT_PAYLOAD),
                    HttpServletResponse.SC_BAD_REQUEST,
                    AnalyzerErrorConstants.APIErrors.UpdateMetadataProfileAPI.MISSING_INPUT_PAYLOAD
            );
            return;
        }

        Set<String> invalidParams = new HashSet<>();
        for (String param : request.getParameterMap().keySet()) {
            if (!KruizeSupportedTypes.UPDATE_METADATA_PROFILES_QUERY_PARAMS_SUPPORTED.contains(param)) {
                invalidParams.add(param);
            }
        }

        try {
            if (invalidParams.isEmpty()) {
                try {
                    // Fetch metadata profiles based on the query parameter using the in-memory storage collection
                    loadMetadataProfilesFromCollection(metadataProfilesMap, metadataProfileName);

                    // Check if metadata profile exists
                    if (!metadataProfilesMap.containsKey(metadataProfileName)) {
                        sendErrorResponseMessage(
                                response,
                                new Exception(AnalyzerErrorConstants.APIErrors.UpdateMetadataProfileAPI.INVALID_METADATA_PROFILE_NAME_EXCPTN),
                                HttpServletResponse.SC_BAD_REQUEST,
                                String.format(AnalyzerErrorConstants.APIErrors.UpdateMetadataProfileAPI.INVALID_METADATA_PROFILE_NAME_MSG, metadataProfileName)
                        );
                    } else {
                        MetadataProfile metadataProfile = Converters.KruizeObjectConverters.convertInputJSONToCreateMetadataProfile(inputData);
                        String inputMetadataProfileName =  metadataProfile.getMetadata().get(KruizeConstants.JSONKeys.NAME).asText();

                        if (!inputMetadataProfileName.equals(metadataProfileName)) {
                            sendErrorResponseMessage(response, null, HttpServletResponse.SC_BAD_REQUEST,
                                    String.format(AnalyzerErrorConstants.APIErrors.UpdateMetadataProfileAPI.METADATA_PROFILE_NAMES_MISMATCH,
                                            metadataProfileName, inputMetadataProfileName));
                            return;
                        }

                        ValidationOutputData validationOutputData = MetadataProfileUtil.validateMetadataProfile(metadataProfilesMap, metadataProfile);
                        if (validationOutputData.isSuccess()) {
                            ValidationOutputData updateMetadataProfileToDB = new ExperimentDBService().updateMetadataProfileToDB(metadataProfile);
                            if (updateMetadataProfileToDB.isSuccess()) {
                                // Update metadata profile in-memory collection
                                MetadataProfileCollection.getInstance().updateMetadataProfileToCollection(metadataProfileName, metadataProfile);

                                metadataProfilesMap.put(inputMetadataProfileName, metadataProfile);
                                getServletContext().setAttribute(AnalyzerConstants.MetadataProfileConstants.METADATA_PROFILE_MAP, metadataProfilesMap);
                                LOGGER.debug(KruizeConstants.MetadataProfileAPIMessages.UPDATE_METADATA_PROFILE_TO_DB_WITH_VERSION,
                                        metadataProfile.getMetadata().get(KruizeConstants.JSONKeys.NAME).asText(), metadataProfile.getProfile_version());

                                statusValue = "success";
                                sendSuccessResponse(response, String.format(KruizeConstants.MetadataProfileAPIMessages.UPDATE_METADATA_PROFILE_SUCCESS_MSG, metadataProfile.getMetadata().get("name").asText()));
                            } else {
                                sendErrorResponseMessage(response, null, HttpServletResponse.SC_BAD_REQUEST, updateMetadataProfileToDB.getMessage());
                            }
                        } else {
                            sendErrorResponseMessage(response, null, validationOutputData.getErrorCode(), validationOutputData.getMessage());
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error(AnalyzerErrorConstants.APIErrors.UpdateMetadataProfileAPI.UPDATE_METADATA_PROFILE_ERROR, e.getMessage());
                    e.printStackTrace();
                    sendErrorResponseMessage(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                }
            } else {
                sendErrorResponseMessage(
                        response,
                        new Exception(AnalyzerErrorConstants.APIErrors.UpdateMetadataProfileAPI.INVALID_QUERY_PARAM),
                        HttpServletResponse.SC_BAD_REQUEST,
                        String.format(AnalyzerErrorConstants.APIErrors.UpdateMetadataProfileAPI.INVALID_QUERY_PARAM, invalidParams)
                );
            }
        } catch (Exception e) {
            LOGGER.error(AnalyzerErrorConstants.APIErrors.UpdateMetadataProfileAPI.UPDATE_METADATA_PROFILE_ERROR, e.getMessage());
            sendErrorResponseMessage(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } finally {
            if (null != timerUpdateMetadataProfile) {
                MetricsConfig.timerUpdateMetadataProfile = MetricsConfig.timerBUpdateMetadataProfile.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerUpdateMetadataProfile.stop(MetricsConfig.timerUpdateMetadataProfile);
            }
        }
    }

    /**
     * Delete MetadataProfile
     * Handles the DELETE request for deleting metadata profile - DELETE /deleteMetadataProfile
     *
     * Supported Query Parameters -
     * name	- metadata profile name to be deleted(required)
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        response.setStatus(HttpServletResponse.SC_OK);

        ConcurrentHashMap<String, MetadataProfile> metadataProfilesMap = new ConcurrentHashMap<>();
        String metadataProfileName = request.getParameter(AnalyzerConstants.MetadataProfileConstants.METADATA_PROFILE_NAME);

        if (null == metadataProfileName || metadataProfileName.isEmpty()) {
            sendErrorResponseMessage(
                    response,
                    new Exception(AnalyzerErrorConstants.APIErrors.DeleteMetadataProfileAPI.MISSING_METADATA_PROFILE_NAME_EXCPTN),
                    HttpServletResponse.SC_BAD_REQUEST,
                    String.format(AnalyzerErrorConstants.APIErrors.DeleteMetadataProfileAPI.MISSING_METADATA_PROFILE_NAME_MSG)
            );
            return;
        }

        try {
            // load specified metadata profile
            loadMetadataProfilesFromCollection(metadataProfilesMap, metadataProfileName);

            // Check if metadata profile exists
            if (metadataProfilesMap.containsKey(metadataProfileName)) {
                try {
                    // Deletes database and in-memory metadata profile object stored
                    deleteMetadataProfile(metadataProfileName);
                    metadataProfilesMap.remove(metadataProfileName);
                } catch (Exception e) {
                    sendErrorResponseMessage(
                            response,
                            e,
                            HttpServletResponse.SC_BAD_REQUEST,
                            e.getMessage());
                    return;
                }
            } else {
                sendErrorResponseMessage(
                        response,
                        new Exception(AnalyzerErrorConstants.APIErrors.DeleteMetadataProfileAPI.INVALID_METADATA_PROFILE_NAME_EXCPTN),
                        HttpServletResponse.SC_BAD_REQUEST,
                        String.format(AnalyzerErrorConstants.APIErrors.DeleteMetadataProfileAPI.INVALID_METADATA_PROFILE_NAME_MSG, metadataProfileName)
                );
                return;
            }

            sendSuccessResponse(response, String.format(KruizeConstants.MetadataProfileAPIMessages.DELETE_METADATA_PROFILE_SUCCESS_MSG, metadataProfileName));

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            sendErrorResponseMessage(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
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
                                HttpServletResponse.SC_CREATED, "", AnalyzerConstants.SUCCESS_STATUS)
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
            } else {
                metadataProfilesMap.putAll(metadataProfileCollection.getMetadataProfileCollection());
            }
        } catch (Exception e) {
            LOGGER.error(AnalyzerErrorConstants.APIErrors.ListMetadataProfileAPI.LOAD_METADATA_PROFILE_ERROR, e.getMessage());
        }

    }

    /**
     * This method deletes specified profile name from database and MetadataProfileCollection
     * @param metadataProfileName Name of the metadata profile to be deleted
     */
    private void deleteMetadataProfile(String metadataProfileName) {
        try {
            // delete the metadata profile from DB
            ValidationOutputData deletedMetadataProfileFromDB = new ExperimentDAOImpl().deleteKruizeLMMetadataProfileEntryByName(metadataProfileName);
            if (deletedMetadataProfileFromDB.isSuccess()) {
                // remove in-memory metadata profile
                MetadataProfileCollection.getInstance().getMetadataProfileCollection().remove(metadataProfileName);
                LOGGER.debug(KruizeConstants.MetadataProfileAPIMessages.DELETE_METADATA_PROFILE_FROM_DB_SUCCESS_MSG);
            } else {
                LOGGER.error(AnalyzerErrorConstants.APIErrors.DeleteMetadataProfileAPI.DELETE_METADATA_PROFILE_FROM_DB_FAILURE_MSG, deletedMetadataProfileFromDB.getMessage());
            }
        }  catch (Exception e) {
            LOGGER.error(AnalyzerErrorConstants.APIErrors.DeleteMetadataProfileAPI.DELETE_METADATA_PROFILE_FAILURE_MSG, e.getMessage());
        }
    }

    /**
     * Sends a formatted JSON error response with HTTP status, error message, and error details.
     * Specifically used for DELETE request as by default the content-length is set to 0 and no response message is
     * being returned in case of 400 error status
     *
     * @param response
     * @param e
     * @param httpStatusCode
     * @param errorMsg
     * @throws IOException
     */
    public void sendErrorResponseMessage(HttpServletResponse response, Exception e, int httpStatusCode, String errorMsg) throws IOException {
        if (e != null) {
            LOGGER.error(e.toString());
            if (errorMsg == null) {
                errorMsg = e.getMessage();
            }
        }
        response.setStatus(httpStatusCode);
        response.setContentType(AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE);
        response.setCharacterEncoding(AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING);

        PrintWriter out = response.getWriter();
        out.append(
                new Gson().toJson(
                        new MetadataProfileResponse(errorMsg,
                                httpStatusCode,
                                "",
                                AnalyzerConstants.ERROR_STATUS)
                )
        );
        out.flush();
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
                            if (objectNode.has(KruizeConstants.JSONKeys.NAME)) {
                                metadataJson.addProperty(KruizeConstants.JSONKeys.NAME, objectNode.get(KruizeConstants.JSONKeys.NAME).asText());
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
                                f.getName().equals(AnalyzerConstants.TRIAL_RESULT_SUMMARY)
                                        || f.getName().equals(AnalyzerConstants.CYCLE_DATA_MAP)
                        ) ||
                                f.getDeclaringClass() == ContainerData.class && (
                                        f.getName().equalsIgnoreCase(AnalyzerConstants.METRICS)
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
