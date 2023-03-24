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
import com.autotune.analyzer.serviceObjects.ListRecommendationsSO;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.analyzer.utils.GsonUTCDateAdapter;
import com.autotune.analyzer.utils.ServiceHelpers;
import com.autotune.common.k8sObjects.ContainerObject;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.Utils;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

/**
 * Rest API used to recommend right configuration.
 */
@WebServlet(asyncSupported = true)
public class ListRecommendation extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ListRecommendation.class);
    private ConcurrentHashMap<String, KruizeObject> mainKruizeExperimentMap;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.mainKruizeExperimentMap = (ConcurrentHashMap<String, KruizeObject>) getServletContext().getAttribute(AnalyzerConstants.EXPERIMENT_MAP);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        response.setStatus(HttpServletResponse.SC_OK);
        String experimentName = request.getParameter(AnalyzerConstants.ServiceConstants.EXPERIMENT_NAME);
        String latestRecommendation = request.getParameter(AnalyzerConstants.ServiceConstants.LATEST);
        String monitoringTimestamp = request.getParameter(KruizeConstants.JSONKeys.MONITORING_END_TIME);
        boolean getLatest = true;
        boolean checkForTimestamp = false;
        boolean error = false;
        if (null != latestRecommendation
                && !latestRecommendation.isEmpty()
                && latestRecommendation.equalsIgnoreCase(AnalyzerConstants.BooleanString.FALSE)
        ) {
            getLatest = false;
        }
        List<KruizeObject> kruizeObjectList =  new ArrayList<>();
        // Check if experiment name is passed
        if (null != experimentName) {
            // trim the experiment name to remove whitespaces
            experimentName = experimentName.trim();
            // Check if experiment exists
            if (this.mainKruizeExperimentMap.containsKey(experimentName)) {
                // Check if timestamp is passed
                if (null != monitoringTimestamp && !monitoringTimestamp.isEmpty()) {
                    monitoringTimestamp = monitoringTimestamp.trim();
                    if (Utils.DateUtils.isAValidDate(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, monitoringTimestamp)) {
                        // Check if timestamp exists in recommendations
                        boolean timestampExists = ServiceHelpers.KruizeObjectOperations.checkRecommendationTimestampExists(this.mainKruizeExperimentMap.get(experimentName), monitoringTimestamp);
                        if (timestampExists) {
                            checkForTimestamp = true;
                        } else {
                            error = true;
                            sendErrorResponse(
                                    response,
                                    new Exception(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.RECOMMENDATION_DOES_NOT_EXIST_EXCPTN),
                                    HttpServletResponse.SC_BAD_REQUEST,
                                    String.format(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.RECOMMENDATION_DOES_NOT_EXIST_MSG, monitoringTimestamp)
                            );
                        }
                    } else {
                        error = true;
                        sendErrorResponse(
                                response,
                                new Exception(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_TIMESTAMP_EXCPTN),
                                HttpServletResponse.SC_BAD_REQUEST,
                                String.format(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_TIMESTAMP_MSG, monitoringTimestamp)
                        );
                    }
                }
                kruizeObjectList.add(this.mainKruizeExperimentMap.get(experimentName));
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
            if (null != monitoringTimestamp && !monitoringTimestamp.isEmpty()) {
                monitoringTimestamp = monitoringTimestamp.trim();
                if (Utils.DateUtils.isAValidDate(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, monitoringTimestamp)) {
                    for (String expName : this.mainKruizeExperimentMap.keySet()) {
                        boolean timestampExists = ServiceHelpers.KruizeObjectOperations.checkRecommendationTimestampExists(this.mainKruizeExperimentMap.get(expName), monitoringTimestamp);
                        if (timestampExists) {
                            kruizeObjectList.add(this.mainKruizeExperimentMap.get(expName));
                            checkForTimestamp = true;
                        }
                    }
                    if (kruizeObjectList.isEmpty()) {
                        error = true;
                        sendErrorResponse(
                                response,
                                new Exception(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.RECOMMENDATION_DOES_NOT_EXIST_EXCPTN),
                                HttpServletResponse.SC_BAD_REQUEST,
                                String.format(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.RECOMMENDATION_DOES_NOT_EXIST_MSG, monitoringTimestamp)
                        );
                    }
                } else {
                    error = true;
                    sendErrorResponse(
                            response,
                            new Exception(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_TIMESTAMP_EXCPTN),
                            HttpServletResponse.SC_BAD_REQUEST,
                            String.format(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_TIMESTAMP_MSG, monitoringTimestamp)
                    );
                }
            } else {
                // Add all experiments to list
                kruizeObjectList.addAll(this.mainKruizeExperimentMap.values());
            }
        }
        if (!error) {
            List<ListRecommendationsSO> recommendationList = new ArrayList<ListRecommendationsSO>();
            for (KruizeObject ko : kruizeObjectList) {
                try {
                    LOGGER.debug(ko.getDeployment_name());
                    LOGGER.debug(ko.getDeployments().toString());
                    ListRecommendationsSO listRecommendationsSO = ServiceHelpers.Converters.KruizeObjectConverters.
                                                                    convertKruizeObjectToListRecommendationSO(
                                                                        ko,
                                                                        getLatest,
                                                                        checkForTimestamp,
                                                                        monitoringTimestamp);
                    recommendationList.add(listRecommendationsSO);
                } catch (Exception e) {
                    LOGGER.error("Not able to generate recommendation for expName : {} due to {}", ko.getExperimentName(), e.getMessage());
                }
            }

            ExclusionStrategy strategy = new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes field) {
                    if (field.getDeclaringClass() == ContainerObject.class && (field.getName().equals("results") || field.getName().equalsIgnoreCase("metrics"))) {
                        return true;
                    }
                    return false;
                }

                @Override
                public boolean shouldSkipClass(Class<?> clazz) {
                    return false;
                }
            };
            String gsonStr = "[]";
            if (recommendationList.size() > 0) {
                Gson gsonObj = new GsonBuilder()
                        .disableHtmlEscaping()
                        .setPrettyPrinting()
                        .enableComplexMapKeySerialization()
                        .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                        .setExclusionStrategies(strategy)
                        .create();
                gsonStr = gsonObj.toJson(recommendationList);
            }
            response.getWriter().println(gsonStr);
            response.getWriter().close();
        }
    }

    private void sendSuccessResponse(HttpServletResponse response) throws IOException {
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        response.setStatus(HttpServletResponse.SC_CREATED);
        PrintWriter out = response.getWriter();
        out.append(
                new Gson().toJson(
                        new KruizeResponse("Updated metrics results successfully with Autotune. View update results at /listExperiments \"results\" section.", HttpServletResponse.SC_CREATED, "", "SUCCESS")
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
