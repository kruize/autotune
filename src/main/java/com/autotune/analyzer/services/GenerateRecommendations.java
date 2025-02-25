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
import com.autotune.analyzer.exceptions.FetchMetricsError;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.recommendations.engine.RecommendationEngine;
import com.autotune.analyzer.serviceObjects.ContainerAPIObject;
import com.autotune.analyzer.serviceObjects.Converters;
import com.autotune.analyzer.serviceObjects.ListRecommendationsAPIObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.analyzer.utils.GsonUTCDateAdapter;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.system.info.device.DeviceDetails;
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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;
import static com.autotune.utils.KruizeConstants.KRUIZE_BULK_API.JOB_ID;

/**
 *
 */
@WebServlet(asyncSupported = true)
public class GenerateRecommendations extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateRecommendations.class);
    private static int requestCount = 0;


    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    /**
     * Generates recommendations
     *
     * @param request  an {@link HttpServletRequest} object that
     *                 contains
     *                 the request the client has made
     *                 of the servlet
     * @param response an {@link HttpServletResponse} object that
     *                 contains the response the servlet sends
     *                 to the client
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int calCount = ++requestCount;
        LOGGER.debug("GenerateRecommendations API request count: {}", calCount);
        String statusValue = "failure";
        Timer.Sample timerBUpdateRecommendations = Timer.start(MetricsConfig.meterRegistry());
        try {
            // Set the character encoding of the request to UTF-8
            request.setCharacterEncoding(CHARACTER_ENCODING);
            // Get the values from the request parameters
            String experiment_name = request.getParameter(KruizeConstants.JSONKeys.EXPERIMENT_NAME);
            String intervalEndTimeStr = request.getParameter(KruizeConstants.JSONKeys.INTERVAL_END_TIME);
            String intervalStartTimeStr = request.getParameter(KruizeConstants.JSONKeys.INTERVAL_START_TIME);
            String bulkJobID = request.getParameter(JOB_ID);
            Timestamp interval_end_time, interval_start_time;

            // create recommendation engine object
            RecommendationEngine recommendationEngine = new RecommendationEngine(experiment_name, intervalEndTimeStr, intervalStartTimeStr);
            // validate and create KruizeObject if successful
            String validationMessage = recommendationEngine.validate_local();
            if (validationMessage.isEmpty()) {
                KruizeObject kruizeObject = recommendationEngine.prepareRecommendations(calCount, AnalyzerConstants.LOCAL, bulkJobID);   // todo target cluster is set to LOCAL always
                if (kruizeObject.getValidation_data().isSuccess()) {
                    LOGGER.debug("UpdateRecommendations API request count: {} success", calCount);
                    interval_end_time = Utils.DateUtils.getTimeStampFrom(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT,
                            intervalEndTimeStr);
                    sendSuccessResponse(response, kruizeObject, interval_end_time);
                    statusValue = "success";
                } else {
                    LOGGER.debug("UpdateRecommendations API request count: {} failed", calCount);
                    sendErrorResponse(response, null, kruizeObject.getValidation_data().getErrorCode(), kruizeObject.getValidation_data().getMessage());
                }
            } else {
                LOGGER.error("Validation failed: {}", validationMessage);
                sendErrorResponse(response, null, HttpServletResponse.SC_BAD_REQUEST, validationMessage);
            }
        } catch (FetchMetricsError e) {
            LOGGER.error(AnalyzerErrorConstants.APIErrors.generateRecommendationsAPI.ERROR_FETCHING_METRICS);
            sendErrorResponse(response, new Exception(e), HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Exception occurred while processing request: " + e.getMessage());
            sendErrorResponse(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } finally {
            if (timerBUpdateRecommendations != null) {
                MetricsConfig.timerUpdateRecomendations = MetricsConfig.timerBUpdateRecommendations.tag(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.STATUS, statusValue).register(MetricsConfig.meterRegistry());
                timerBUpdateRecommendations.stop(MetricsConfig.timerUpdateRecomendations);
            }
        }
    }

    private void sendSuccessResponse(HttpServletResponse response, KruizeObject ko, Timestamp interval_end_time) throws IOException {
        LOGGER.debug("sendSuccessResponse");
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        response.setStatus(HttpServletResponse.SC_CREATED);
        List<ListRecommendationsAPIObject> recommendationList = new ArrayList<>();              //TODO: Executing two identical SQL SELECT queries against the database instead of just one is causing a performance issue. set 'showSQL' flag is set to true to debug.
        try {
            //LOGGER.debug(ko.getKubernetes_objects().toString());
            ListRecommendationsAPIObject listRecommendationsAPIObject = Converters.KruizeObjectConverters.
                    convertKruizeObjectToListRecommendationSO(
                            ko,
                            false,
                            false,
                            interval_end_time);
            recommendationList.add(listRecommendationsAPIObject);
        } catch (Exception e) {
            LOGGER.error("Not able to generate recommendation for expName : {} due to {}", ko.getExperimentName(), e.getMessage());
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
                    .registerTypeAdapter(AnalyzerConstants.RecommendationItem.class, new RecommendationItemAdapter())
                    .registerTypeAdapter(DeviceDetails.class, new DeviceDetailsAdapter())
                    .setExclusionStrategies(strategy)
                    .create();
            gsonStr = gsonObj.toJson(recommendationList);
        }
        response.getWriter().println(gsonStr);
        response.getWriter().close();
    }

    public void sendErrorResponse(HttpServletResponse response, Exception e, int httpStatusCode, String errorMsg) throws
            IOException {
        LOGGER.debug("sendErrorResponse");
        if (null != e) {
            e.printStackTrace();
            LOGGER.error(e.toString());
            if (null == errorMsg) errorMsg = e.getMessage();
        }
        response.sendError(httpStatusCode, errorMsg);
    }

}
