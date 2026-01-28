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

import com.autotune.analyzer.adapters.DeviceDetailsAdapter;
import com.autotune.analyzer.adapters.RecommendationItemAdapter;
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
import com.autotune.common.data.system.info.device.DeviceDetails;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.utils.ProfileCache;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.ProfileType;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.autotune.database.dao.ExperimentDAOImpl;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

/**
 * REST API to create performance profile .
 */
@WebServlet(asyncSupported = true)
public class PerformanceProfileService extends HttpServlet {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceProfileService.class);
    private static final Gson gson = new GsonBuilder()
            .disableHtmlEscaping()  // Prevents escaping of quotes
            .setPrettyPrinting()
            .create();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    /**
     * Validate and create new Performance Profile.
     *
     * @param request
     * @param response
     * @throws IOException
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Map<String, PerformanceProfile> performanceProfilesMap = new ConcurrentHashMap<>();
            String inputData = request.getReader().lines().collect(Collectors.joining());
            PerformanceProfile performanceProfile = Converters.KruizeObjectConverters.convertInputJSONToCreatePerfProfile(inputData);
            ValidationOutputData validationOutputData = PerformanceProfileUtil.validateAndAddProfile(performanceProfilesMap, performanceProfile, AnalyzerConstants.OperationType.CREATE);
            if (validationOutputData.isSuccess()) {
                ValidationOutputData addedToDB = new ExperimentDBService().addPerformanceProfileToDB(performanceProfile);
                if (addedToDB.isSuccess()) {
                    LOGGER.debug("Added Performance Profile : {} into the DB with version: {}",
                            performanceProfile.getName(), performanceProfile.getProfile_version());
                    sendSuccessResponse(response, "Performance Profile : " + performanceProfile.getName() + " created successfully.");
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
     * Get List of Performance Profiles
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
        // Fetch all profiles from the cache. Initialize from DB if required.
        Map<String, PerformanceProfile> performanceProfilesMap = ProfileCache.getProfileMap(ProfileType.PERFORMANCE);
        if (!performanceProfilesMap.isEmpty()) {
            Collection<PerformanceProfile> values = performanceProfilesMap.values();
            Gson gsonObj = new GsonBuilder()
                    .disableHtmlEscaping()
                    .setPrettyPrinting()
                    .enableComplexMapKeySerialization()
                    .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                    .registerTypeAdapter(AnalyzerConstants.RecommendationItem.class, new RecommendationItemAdapter())
                    .registerTypeAdapter(DeviceDetails.class, new DeviceDetailsAdapter())
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
     * Update Performance Profile
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Map<String, PerformanceProfile> performanceProfilesMap = new ConcurrentHashMap<>();
            // Parse incoming JSON
            String inputData = request.getReader().lines().collect(Collectors.joining());
            PerformanceProfile incomingPerfProfile = Converters.KruizeObjectConverters.convertInputJSONToCreatePerfProfile(inputData);
            String profileName = incomingPerfProfile.getName();
            // validate the entries present in the incoming profile
            ValidationOutputData validationOutputData = PerformanceProfileUtil.validateAndAddProfile(performanceProfilesMap,
                    incomingPerfProfile, AnalyzerConstants.OperationType.UPDATE);
            if (validationOutputData.isSuccess()) {
                // Perform update
                ValidationOutputData updatedInDB = new ExperimentDBService().updatePerformanceProfileInDB(incomingPerfProfile);
                if (updatedInDB.isSuccess()) {
                    LOGGER.info("{}", String.format(KruizeConstants.APIMessages.PERFORMANCE_PROFILE_UPDATE_SUCCESS,
                            profileName, incomingPerfProfile.getProfile_version()));
                    sendSuccessResponse(
                            response,
                            String.format(KruizeConstants.APIMessages.PERFORMANCE_PROFILE_UPDATE_SUCCESS,
                                    profileName, incomingPerfProfile.getProfile_version())
                    );
                } else {
                    sendErrorResponse(response, null, HttpServletResponse.SC_BAD_REQUEST, updatedInDB.getMessage());
                }
            } else {
                LOGGER.debug(validationOutputData.getMessage());
                sendErrorResponse(response, null, validationOutputData.getErrorCode(), validationOutputData.getMessage());
            }
        } catch (Exception e) {
            sendErrorResponse(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } catch (InvalidValueException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Delete Performance profile
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String perfProfileName = req.getParameter(AnalyzerConstants.ServiceConstants.PERF_PROFILE_NAME);
        if (perfProfileName == null || perfProfileName.isBlank()) {
            sendErrorResponse(resp, null, HttpServletResponse.SC_BAD_REQUEST, AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_PERF_PROFILE_NAME);
            return;
        }
        try {
            // Load profile
            if (!ProfileCache.isExists(perfProfileName, ProfileType.PERFORMANCE)) {
                sendErrorResponse(resp, null, HttpServletResponse.SC_BAD_REQUEST,
                        AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_PERF_PROFILE + perfProfileName);
                return;
            }

            // Check if the profile is associated with any of the existing experiments
            // fetch experiments if any, associated with the mentioned profile name
            Long experimentsCount = new ExperimentDBService().getExperimentsCountFromDBByProfileName(perfProfileName);
            if (experimentsCount != 0) {
                sendErrorResponse(resp, null, HttpServletResponse.SC_BAD_REQUEST,
                        String.format(AnalyzerErrorConstants.AutotuneObjectErrors.PERF_PROFILE_EXPERIMENTS_ERROR,
                                perfProfileName, experimentsCount, experimentsCount > 1 ? "s" : ""));
                return;
            }

            // Delete profile
            ValidationOutputData result = new ExperimentDAOImpl().deletePerformanceProfileByName(perfProfileName);
            if (!result.isSuccess()) {
                sendErrorResponse(resp, null, result.getErrorCode(), result.getMessage());
                return;
            }
            // remove the profile from the local storage as well
            ProfileCache.removeProfile(perfProfileName, ProfileType.PERFORMANCE);
            sendSuccessResponse(resp, String.format(KruizeConstants.APIMessages.PERF_PROFILE_DELETION_SUCCESS, perfProfileName));
        } catch (Exception e) {
            LOGGER.error("{}",String.format(AnalyzerErrorConstants.AutotuneObjectErrors.PERF_PROFILE_DELETION_EXCEPTION, perfProfileName, e.getMessage()));
            sendErrorResponse(resp, null, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Sends a JSON response (for both success and error cases).
     *
     * @param response The servlet response
     * @param message  Message to include in response
     * @param statusCode HTTP status code (e.g., 200, 404, 500)
     * @param status   "SUCCESS" or "FAILURE"
     * @throws IOException
     */
    public static void sendJsonResponse(HttpServletResponse response,
                                        String message,
                                        int statusCode,
                                        String status) throws IOException {

        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        response.setStatus(statusCode);

        PrintWriter out = response.getWriter();
        out.append(
                gson.toJson(
                        new PerformanceProfileResponse(message, statusCode, "", status)
                )
        );
        out.flush();
    }

    /***
     * success response
     * @param response
     * @param message
     * @throws IOException
     */
    public static void sendSuccessResponse(HttpServletResponse response, String message) throws IOException {
        message += " View Performance Profiles at /listPerformanceProfiles";
        sendJsonResponse(response, message, HttpServletResponse.SC_CREATED, "SUCCESS");
    }

    /***
     * Error response
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
        sendJsonResponse(response, errorMsg, httpStatusCode, "ERROR");
    }
}
