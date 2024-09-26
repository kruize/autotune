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

import com.autotune.analyzer.exceptions.FetchMetricsError;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.recommendations.engine.RecommendationEngine;
import com.autotune.analyzer.serviceObjects.ContainerAPIObject;
import com.autotune.analyzer.serviceObjects.Converters;
import com.autotune.analyzer.serviceObjects.ListRecommendationsAPIObject;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.analyzer.utils.GsonUTCDateAdapter;
import com.autotune.common.data.result.ContainerData;
import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.MetricsConfig;
import com.autotune.utils.Utils;
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
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

/**
 *
 */
@WebServlet(asyncSupported = true)
public class UpdateRecommendations extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateRecommendations.class);
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
        LOGGER.debug(String.format(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.UPDATE_RECOMMENDATIONS_COUNT, calCount));
        String statusValue = KruizeConstants.APIMessages.FAILURE;
        Timer.Sample timerBUpdateRecommendations = Timer.start(MetricsConfig.meterRegistry());
        // Set the character encoding of the request to UTF-8
        request.setCharacterEncoding(CHARACTER_ENCODING);
        // Get the values from the request parameters
        String experiment_name = request.getParameter(KruizeConstants.JSONKeys.EXPERIMENT_NAME);
        String intervalEndTimeStr = request.getParameter(KruizeConstants.JSONKeys.INTERVAL_END_TIME);

        String intervalStartTimeStr = request.getParameter(KruizeConstants.JSONKeys.INTERVAL_START_TIME);
        Timestamp interval_end_time;
        Timestamp interval_start_time = null;
        if (KruizeDeploymentInfo.log_http_req_resp)
            LOGGER.info(String.format(KruizeConstants.APIMessages.UPDATE_RECOMMENDATIONS_INPUT_PARAMS, experiment_name, intervalStartTimeStr, intervalEndTimeStr));
        try {
            // create recommendation engine object
            RecommendationEngine recommendationEngine = new RecommendationEngine(experiment_name, intervalEndTimeStr, intervalStartTimeStr);
            // validate and create KruizeObject if successful
            String validationMessage = recommendationEngine.validate();
            if (validationMessage.isEmpty()) {
                KruizeObject kruizeObject = recommendationEngine.prepareRecommendations(calCount);
                if (kruizeObject.getValidation_data().isSuccess()) {
                    LOGGER.debug(String.format(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.UPDATE_RECOMMENDATIONS_SUCCESS_COUNT, calCount));
                    interval_end_time = Utils.DateUtils.getTimeStampFrom(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT,
                            intervalEndTimeStr);
                    SimpleDateFormat sdf = new SimpleDateFormat(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, Locale.ROOT);
                    sdf.setTimeZone(TimeZone.getTimeZone(KruizeConstants.TimeUnitsExt.TimeZones.UTC));
                    LOGGER.info(String.format(KruizeConstants.APIMessages.UPDATE_RECOMMENDATIONS_SUCCESS, experiment_name,
                            sdf.format(interval_end_time)));
                    sendSuccessResponse(response, kruizeObject, interval_end_time);
                    statusValue = KruizeConstants.APIMessages.SUCCESS;
                } else {
                    LOGGER.error(String.format(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.UPDATE_RECOMMENDATIONS_FAILED_COUNT, calCount));
                    sendErrorResponse(response, null, kruizeObject.getValidation_data().getErrorCode(), kruizeObject.getValidation_data().getMessage(), experiment_name, intervalEndTimeStr);
                }
            } else {
                sendErrorResponse(response, null, HttpServletResponse.SC_BAD_REQUEST, validationMessage, experiment_name, intervalEndTimeStr);
            }

        } catch (FetchMetricsError e) {
            sendErrorResponse(response, new Exception(e), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage(), experiment_name, intervalEndTimeStr);
        } catch (Exception e) {
            LOGGER.error(String.format(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.UPDATE_RECOMMENDATIONS_FAILED_COUNT, calCount));
            e.printStackTrace();
            sendErrorResponse(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage(), experiment_name, intervalEndTimeStr);
        } finally {
            LOGGER.debug(String.format(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.UPDATE_RECOMMENDATIONS_COMPLETED_COUNT, calCount));
            if (null != timerBUpdateRecommendations) {
                MetricsConfig.timerUpdateRecomendations = MetricsConfig.timerBUpdateRecommendations.tag(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.STATUS, statusValue).register(MetricsConfig.meterRegistry());
                timerBUpdateRecommendations.stop(MetricsConfig.timerUpdateRecomendations);
            }
        }
    }

    private void sendSuccessResponse(HttpServletResponse response, KruizeObject ko, Timestamp interval_end_time) throws IOException {
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        response.setStatus(HttpServletResponse.SC_CREATED);
        List<ListRecommendationsAPIObject> recommendationList = new ArrayList<>();              //TODO: Executing two identical SQL SELECT queries against the database instead of just one is causing a performance issue. set 'showSQL' flag is set to true to debug.
        try {
            LOGGER.debug(ko.getKubernetes_objects().toString());
            ListRecommendationsAPIObject listRecommendationsAPIObject = Converters.KruizeObjectConverters.
                    convertKruizeObjectToListRecommendationSO(
                            ko,
                            false,
                            false,
                            interval_end_time);
            recommendationList.add(listRecommendationsAPIObject);
        } catch (Exception e) {
            LOGGER.error(String.format(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.GENERATE_RECOMMENDATION_FAILURE,
                    ko.getExperimentName(), e.getMessage()));
        }
        ExclusionStrategy strategy = new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes field) {
                return field.getDeclaringClass() == ContainerData.class && (field.getName().equals(KruizeConstants.JSONKeys.RESULTS))
                        || (field.getDeclaringClass() == ContainerAPIObject.class && (field.getName().equals(KruizeConstants.JSONKeys.METRICS)));
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        };
        String gsonStr = "[]";
        if (!recommendationList.isEmpty()) {
            Gson gsonObj = new GsonBuilder()
                    .disableHtmlEscaping()
                    .setPrettyPrinting()
                    .enableComplexMapKeySerialization()
                    .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                    .setExclusionStrategies(strategy)
                    .create();
            gsonStr = gsonObj.toJson(recommendationList);
        }
        if (KruizeDeploymentInfo.log_http_req_resp)
            LOGGER.info(String.format(KruizeConstants.APIMessages.UPDATE_RECOMMENDATIONS_RESPONSE, new Gson().toJson(JsonParser.parseString(gsonStr))));
        response.getWriter().println(gsonStr);
        response.getWriter().close();
    }

    public void sendErrorResponse(HttpServletResponse response, Exception e, int httpStatusCode, String errorMsg,
                                  String experiment_name, String intervalEndTimeStr) throws IOException {
        if (null != e) {
            e.printStackTrace();
            LOGGER.error(e.toString());
            if (null == errorMsg) errorMsg = e.getMessage();
        }
        LOGGER.error(String.format(KruizeConstants.APIMessages.UPDATE_RECOMMENDATIONS_FAILURE_MSG, experiment_name, intervalEndTimeStr, errorMsg));
        response.sendError(httpStatusCode, errorMsg);
    }
}
