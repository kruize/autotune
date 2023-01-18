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

import com.autotune.analyzer.exceptions.PerformanceProfileResponse;
import com.autotune.analyzer.utils.PerformanceProfileValidation;
import com.autotune.common.data.ValidationResultData;
import com.autotune.common.performanceProfiles.PerformanceProfile;
import com.autotune.utils.AnalyzerConstants;
import com.google.gson.Gson;
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
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.autotune.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

/**
 * REST API to create performance profile .
 */
@WebServlet(asyncSupported = true)
public class PerformanceProfileService extends HttpServlet {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceProfileService.class);
    Map<String, PerformanceProfile> performanceProfilesMap;


    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.performanceProfilesMap = (HashMap<String, PerformanceProfile>) getServletContext()
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
            String inputData = request.getReader().lines().collect(Collectors.joining());
            PerformanceProfile performanceProfile = new Gson().fromJson(inputData, PerformanceProfile.class);
            ValidationResultData validationResultData = new PerformanceProfileValidation(performanceProfilesMap).validate(performanceProfilesMap,performanceProfile);
            if (validationResultData.isSuccess()) {
                LOGGER.debug("Added Performance Profile : {} into the map with version: {}",
                        performanceProfile.getName(), performanceProfile.getProfile_version());
                sendSuccessResponse(response, "Performance Profile : "+performanceProfile.getName()+" created successfully.");
            }
            else
                sendErrorResponse(response, null, HttpServletResponse.SC_BAD_REQUEST,validationResultData.getMessage());
        } catch (Exception e) {
            sendErrorResponse(response, e, HttpServletResponse.SC_BAD_REQUEST,"Validation failed due to " + e.getMessage());
        }
    }

    /**TODO: Need to implement
     * Get List of Performance Profiles
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
        // TODO: Will be updated later

    }

    /**TODO: Need to implement
     * Update Performance Profile
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPut(req, resp);
    }

    /**TODO: Need to implement
     * Delete Performance profile
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
