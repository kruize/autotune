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

import com.autotune.analyzer.exceptions.KruizeResponse;
import com.autotune.analyzer.serviceObjects.Converters;
import com.autotune.analyzer.serviceObjects.UpdateResultsAPIObject;
import com.autotune.analyzer.experiment.ExperimentInitiator;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

/**
 * REST API used to receive Experiment metric results .
 */
@WebServlet(asyncSupported = true)
public class UpdateResults extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateResults.class);
    Map<String, KruizeObject> mainKruizeExperimentMap;
    Map<String, PerformanceProfile> performanceProfilesMap;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.mainKruizeExperimentMap = (ConcurrentHashMap<String, KruizeObject>) getServletContext().getAttribute(AnalyzerConstants.EXPERIMENT_MAP);
        this.performanceProfilesMap = (HashMap<String, PerformanceProfile>) getServletContext()
                .getAttribute(AnalyzerConstants.PerformanceProfileConstants.PERF_PROFILE_MAP);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            String inputData = request.getReader().lines().collect(Collectors.joining());
            List<ExperimentResultData> experimentResultDataList = new ArrayList<>();
            List<UpdateResultsAPIObject> updateResultsAPIObjects = Arrays.asList(new Gson().fromJson(inputData, UpdateResultsAPIObject[].class));
            // check for bulk entries and respond accordingly
            if (updateResultsAPIObjects.size() > 1) {
                LOGGER.error(AnalyzerErrorConstants.AutotuneObjectErrors.UNSUPPORTED_EXPERIMENT);
                sendErrorResponse(response, null, HttpServletResponse.SC_BAD_REQUEST, AnalyzerErrorConstants.AutotuneObjectErrors.UNSUPPORTED_EXPERIMENT );
            } else {
                for (UpdateResultsAPIObject updateResultsAPIObject : updateResultsAPIObjects) {
                    experimentResultDataList.add(Converters.KruizeObjectConverters.convertUpdateResultsAPIObjToExperimentResultData(updateResultsAPIObject));
                }
                LOGGER.debug(experimentResultDataList.toString());
                new ExperimentInitiator().validateAndUpdateResults(mainKruizeExperimentMap, experimentResultDataList, performanceProfilesMap);
                ExperimentResultData invalidKExperimentResultData = experimentResultDataList.stream().filter((rData) -> (!rData.getValidationOutputData().isSuccess())).findAny().orElse(null);
                if (null == invalidKExperimentResultData) {
                    sendSuccessResponse(response, AnalyzerConstants.ServiceConstants.RESULT_SAVED);
                } else {
                    LOGGER.error("Failed to update results: " + invalidKExperimentResultData.getValidationOutputData().getMessage());
                    sendErrorResponse(response, null, invalidKExperimentResultData.getValidationOutputData().getErrorCode(), invalidKExperimentResultData.getValidationOutputData().getMessage());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Exception: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private void sendSuccessResponse(HttpServletResponse response, String message) throws IOException {
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        response.setStatus(HttpServletResponse.SC_CREATED);
        PrintWriter out = response.getWriter();
        out.append(
                new Gson().toJson(
                        new KruizeResponse(message, HttpServletResponse.SC_CREATED, "", "SUCCESS")
                )
        );
        out.flush();
    }

    public void sendErrorResponse(HttpServletResponse response, Exception e, int httpStatusCode, String errorMsg) throws
            IOException {
        if (null != e) {
            LOGGER.error(e.toString());
            e.printStackTrace();
            if (null == errorMsg) errorMsg = e.getMessage();
        }
        response.sendError(httpStatusCode, errorMsg);
    }
}
