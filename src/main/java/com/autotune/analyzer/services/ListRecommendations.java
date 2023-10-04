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
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.serviceObjects.ContainerAPIObject;
import com.autotune.analyzer.serviceObjects.Converters;
import com.autotune.analyzer.serviceObjects.ListRecommendationsAPIObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.analyzer.utils.GsonUTCDateAdapter;
import com.autotune.analyzer.utils.ServiceHelpers;
import com.autotune.common.data.result.ContainerData;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.MetricsConfig;
import com.autotune.utils.Utils;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

/**
 * Rest API used to recommend right configuration.
 */
@WebServlet(asyncSupported = true)
public class ListRecommendations extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ListRecommendations.class);

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String statusValue = "failure";
        Timer.Sample timerListRec = Timer.start(MetricsConfig.meterRegistry());
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        response.setStatus(HttpServletResponse.SC_OK);
        // Set the character encoding of the request to UTF-8
        request.setCharacterEncoding(CHARACTER_ENCODING);
        String experimentName = request.getParameter(AnalyzerConstants.ServiceConstants.EXPERIMENT_NAME);
        String clusterName = request.getParameter(KruizeConstants.JSONKeys.CLUSTER_NAME);
        String latestRecommendation = request.getParameter(AnalyzerConstants.ServiceConstants.LATEST);
        String monitoringEndTime = request.getParameter(KruizeConstants.JSONKeys.MONITORING_END_TIME);
        Timestamp monitoringEndTimestamp = null;
        Map<String, KruizeObject> mKruizeExperimentMap = new ConcurrentHashMap<String, KruizeObject>();
        ;

        boolean getLatest = true;
        boolean checkForTimestamp = false;
        boolean error = false;
        if (null != latestRecommendation
                && !latestRecommendation.isEmpty()
                && latestRecommendation.equalsIgnoreCase(AnalyzerConstants.BooleanString.FALSE)
        ) {
            getLatest = false;
        }
        List<KruizeObject> kruizeObjectList = new ArrayList<>();
        try {
            // Check if experiment name is passed
            if (null != experimentName) {
                // trim the experiment name to remove whitespaces
                experimentName = experimentName.trim();
                try {
                    new ExperimentDBService().loadExperimentAndRecommendationsFromDBByName(mKruizeExperimentMap, experimentName);
                } catch (Exception e) {
                    LOGGER.error("Loading saved experiment {} failed: {} ", experimentName, e.getMessage());
                }
                // Check if experiment exists
                if (mKruizeExperimentMap.containsKey(experimentName)) {
                    // Check if timestamp is passed
                    if (null != monitoringEndTime && !monitoringEndTime.isEmpty()) {
                        monitoringEndTime = monitoringEndTime.trim();
                        if (Utils.DateUtils.isAValidDate(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, monitoringEndTime)) {
                            Date mEndTime = Utils.DateUtils.getDateFrom(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, monitoringEndTime);
                            monitoringEndTimestamp = new Timestamp(mEndTime.getTime());
                            // Check if timestamp exists in recommendations
                            boolean timestampExists = ServiceHelpers.KruizeObjectOperations.checkRecommendationTimestampExists(mKruizeExperimentMap.get(experimentName), monitoringEndTime);
                            if (timestampExists) {
                                checkForTimestamp = true;
                            } else {
                                error = true;
                                sendErrorResponse(
                                        response,
                                        new Exception(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.RECOMMENDATION_DOES_NOT_EXIST_EXCPTN),
                                        HttpServletResponse.SC_BAD_REQUEST,
                                        String.format(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.RECOMMENDATION_DOES_NOT_EXIST_MSG, monitoringEndTime)
                                );
                            }
                        } else {
                            error = true;
                            sendErrorResponse(
                                    response,
                                    new Exception(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_TIMESTAMP_EXCPTN),
                                    HttpServletResponse.SC_BAD_REQUEST,
                                    String.format(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_TIMESTAMP_MSG, monitoringEndTime)
                            );
                        }
                    }
                    kruizeObjectList.add(mKruizeExperimentMap.get(experimentName));
                } else {
                    error = true;
                    sendErrorResponse(
                            response,
                            new Exception(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_EXPERIMENT_NAME_EXCPTN),
                            HttpServletResponse.SC_BAD_REQUEST,
                            String.format(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_EXPERIMENT_NAME_MSG, experimentName)
                    );
                }
            } else if (null != clusterName) {
                try {
                    new ExperimentDBService().loadExperimentsAndRecommendationsFromDBByClusterName(mKruizeExperimentMap, clusterName);
                } catch (Exception e) {
                    LOGGER.error("Loading saved cluster {} failed: {} ", clusterName, e.getMessage());
                }
                // Check if cluster exists
                if (!mKruizeExperimentMap.isEmpty()) {
                    // Check if timestamp is passed
                    if (null != monitoringEndTime && !monitoringEndTime.isEmpty()) {
                        monitoringEndTime = monitoringEndTime.trim();
                        if (Utils.DateUtils.isAValidDate(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, monitoringEndTime)) {
                            Date mEndTime = Utils.DateUtils.getDateFrom(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, monitoringEndTime);
                            monitoringEndTimestamp = new Timestamp(mEndTime.getTime());
                            // Check if timestamp exists in recommendations
                            for (String expName : mKruizeExperimentMap.keySet()) {
                                boolean timestampExists = ServiceHelpers.KruizeObjectOperations.checkRecommendationTimestampExists(mKruizeExperimentMap.get(expName), monitoringEndTime);
                                if (timestampExists) {
                                    kruizeObjectList.add(mKruizeExperimentMap.get(expName));
                                    checkForTimestamp = true;
                                }
                            }
                            if (kruizeObjectList.isEmpty()) {
                                error = true;
                                sendErrorResponse(
                                        response,
                                        new Exception(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.RECOMMENDATION_DOES_NOT_EXIST_EXCPTN),
                                        HttpServletResponse.SC_BAD_REQUEST,
                                        String.format(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.RECOMMENDATION_DOES_NOT_EXIST_MSG, monitoringEndTime)
                                );
                            }
                        } else {
                            error = true;
                            sendErrorResponse(
                                    response,
                                    new Exception(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_TIMESTAMP_EXCPTN),
                                    HttpServletResponse.SC_BAD_REQUEST,
                                    String.format(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_TIMESTAMP_MSG, monitoringEndTime)
                            );
                        }
                    } else {
                        // Add all experiments to list
                        kruizeObjectList.addAll(mKruizeExperimentMap.values());
                    }
                } else {
                    error = true;
                    sendErrorResponse(
                            response,
                            new Exception(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_CLUSTER_NAME_EXCPTN),
                            HttpServletResponse.SC_BAD_REQUEST,
                            String.format(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_CLUSTER_NAME_MSG, clusterName)
                    );
                }
            } else {
                try {
                    new ExperimentDBService().loadAllExperimentsAndRecommendations(mKruizeExperimentMap);
                } catch (Exception e) {
                    LOGGER.error("Loading saved experiment {} failed: {} ", experimentName, e.getMessage());
                }
                if (null != monitoringEndTime && !monitoringEndTime.isEmpty()) {
                    monitoringEndTime = monitoringEndTime.trim();
                    if (Utils.DateUtils.isAValidDate(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, monitoringEndTime)) {
                        Date mEndTime = Utils.DateUtils.getDateFrom(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, monitoringEndTime);
                        monitoringEndTimestamp = new Timestamp(mEndTime.getTime());
                        for (String expName : mKruizeExperimentMap.keySet()) {
                            boolean timestampExists = ServiceHelpers.KruizeObjectOperations.checkRecommendationTimestampExists(mKruizeExperimentMap.get(expName), monitoringEndTime);
                            if (timestampExists) {
                                kruizeObjectList.add(mKruizeExperimentMap.get(expName));
                                checkForTimestamp = true;
                            }
                        }
                        if (kruizeObjectList.isEmpty()) {
                            error = true;
                            sendErrorResponse(
                                    response,
                                    new Exception(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.RECOMMENDATION_DOES_NOT_EXIST_EXCPTN),
                                    HttpServletResponse.SC_BAD_REQUEST,
                                    String.format(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.RECOMMENDATION_DOES_NOT_EXIST_MSG, monitoringEndTime)
                            );
                        }
                    } else {
                        error = true;
                        sendErrorResponse(
                                response,
                                new Exception(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_TIMESTAMP_EXCPTN),
                                HttpServletResponse.SC_BAD_REQUEST,
                                String.format(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_TIMESTAMP_MSG, monitoringEndTime)
                        );
                    }
                } else {
                    // Add all experiments to list
                    kruizeObjectList.addAll(mKruizeExperimentMap.values());
                }
            }
            if (!error) {
                List<ListRecommendationsAPIObject> recommendationList = new ArrayList<>();
                for (KruizeObject ko : kruizeObjectList) {
                    try {
                        LOGGER.debug(ko.getKubernetes_objects().toString());
                        ListRecommendationsAPIObject listRecommendationsAPIObject = Converters.KruizeObjectConverters.
                                convertKruizeObjectToListRecommendationSO(
                                        ko,
                                        getLatest,
                                        checkForTimestamp,
                                        monitoringEndTimestamp);
                        recommendationList.add(listRecommendationsAPIObject);
                        statusValue = "success";
                    } catch (Exception e) {
                        LOGGER.error("Not able to generate recommendation for expName : {} due to {}", ko.getExperimentName(), e.getMessage());
                    }
                }

                ExclusionStrategy strategy = new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes field) {
                        return field.getDeclaringClass() == ContainerData.class && (field.getName().equals("results"))
                                || (field.getDeclaringClass() == ContainerAPIObject.class && (field.getName().equals("metrics")));
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
        } catch (Exception e) {
            LOGGER.error("Exception: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } finally {
            if (null != timerListRec) {
                MetricsConfig.timerListRec = MetricsConfig.timerBListRec.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                timerListRec.stop(MetricsConfig.timerListRec);
            }
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
