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
import com.autotune.operator.KruizeDeploymentInfo;
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
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
    private ConcurrentHashMap<String, PerformanceProfile> performanceProfilesMap;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        performanceProfilesMap = (ConcurrentHashMap<String, PerformanceProfile>) getServletContext()
                .getAttribute(AnalyzerConstants.PerformanceProfileConstants.PERF_PROFILE_MAP);
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
            ValidationOutputData validationOutputData = PerformanceProfileUtil.validateAndAddProfile(performanceProfilesMap, performanceProfile);
            if (validationOutputData.isSuccess()) {
                ValidationOutputData addedToDB = new ExperimentDBService().addPerformanceProfileToDB(performanceProfile);
                if (addedToDB.isSuccess()) {
                    performanceProfilesMap.put(performanceProfile.getName(), performanceProfile);
                    getServletContext().setAttribute(AnalyzerConstants.PerformanceProfileConstants.PERF_PROFILE_MAP, performanceProfilesMap);
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
        // Fetch all profiles from the DB
        try {
            new ExperimentDBService().loadAllPerformanceProfiles(performanceProfilesMap);
        } catch (Exception e) {
            LOGGER.error("Failed to load saved experiment data: {} ", e.getMessage());
        }
        if (performanceProfilesMap.size() > 0) {
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
            // Parse incoming JSON
            String inputData = request.getReader().lines().collect(Collectors.joining());
            PerformanceProfile incomingPerfProfile = Converters.KruizeObjectConverters.convertInputJSONToCreatePerfProfile(inputData);
            String profileName = incomingPerfProfile.getName();

            // Fetch existing profile
            try {
                new ExperimentDBService().loadPerformanceProfileFromDBByName(performanceProfilesMap, profileName);
            }  catch (Exception e) {
                throw new Exception("Failed to load performance profile from the DB: {} "+ e.getMessage());
            }
            // Return 404 if the profile is not present
            if (null ==  performanceProfilesMap.get(profileName)) {
                LOGGER.debug(AnalyzerErrorConstants.AutotuneObjectErrors.NO_PERF_PROFILE);
                sendErrorResponse(
                        response,
                        null,
                        HttpServletResponse.SC_NOT_FOUND,
                        String.format("Performance Profile '%s' not found. Use POST to create a new profile.", profileName)
                );
                return;
            }
            // Compare version — only allow update if the version is matching with the current supported one
            if (incomingPerfProfile.getProfile_version() != KruizeDeploymentInfo.perf_profile_supported_version) {
                LOGGER.debug("Version not supported");
                sendErrorResponse(
                        response,
                        null,
                        HttpServletResponse.SC_CONFLICT,
                        String.format(
                                "Update rejected: the provided version (%.1f) is older than the current version (%.1f) for profile '%s'.",
                                incomingPerfProfile.getProfile_version(),
                                KruizeDeploymentInfo.perf_profile_supported_version,
                                profileName
                        )
                );
                return;
            }
            // validate the entries present in the incoming profile
            ValidationOutputData validationOutputData = PerformanceProfileUtil.validateAndAddProfile(performanceProfilesMap, incomingPerfProfile);
            if (validationOutputData.isSuccess()) {
                // Perform update
                ValidationOutputData updatedInDB = new ExperimentDBService().updatePerformanceProfileInDB(incomingPerfProfile);

                if (updatedInDB.isSuccess()) {
                    LOGGER.info("Updated Performance Profile '{}' to version {}", profileName, incomingPerfProfile.getProfile_version());

                    performanceProfilesMap.put(incomingPerfProfile.getName(), incomingPerfProfile);
                    getServletContext().setAttribute(AnalyzerConstants.PerformanceProfileConstants.PERF_PROFILE_MAP, performanceProfilesMap);
                    LOGGER.debug("Updated Performance Profile : {} into the DB with version: {}",
                            incomingPerfProfile.getName(), incomingPerfProfile.getProfile_version());
                    sendSuccessResponse(
                            response,
                            String.format("Performance Profile '%s' updated successfully to version %.1f.",
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
     * TODO: Need to implement
     * Delete Performance profile
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
                                " View Performance Profiles at /listPerformanceProfiles",
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
