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

import com.autotune.analyzer.experiment.ExperimentInitiator;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.serviceObjects.ContainerAPIObject;
import com.autotune.analyzer.serviceObjects.Converters;
import com.autotune.analyzer.serviceObjects.ListRecommendationsAPIObject;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.analyzer.utils.GsonUTCDateAdapter;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.database.service.ExperimentDBService;
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
import java.sql.Timestamp;
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

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    /**
     * Generates recommendations
     *
     * @param request  an {@link HttpServletRequest} object that
     *                 contains the request the client has made
     *                 of the servlet
     * @param response an {@link HttpServletResponse} object that
     *                 contains the response the servlet sends
     *                 to the client
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Get the values from the request parameters
        String experiment_name = request.getParameter(KruizeConstants.JSONKeys.EXPERIMENT_NAME);
        String intervalEndTimeStr = request.getParameter(KruizeConstants.JSONKeys.INTERVAL_END_TIME);

        // Check if experiment_name is provided
        if (experiment_name == null || experiment_name.isEmpty()) {
            sendErrorResponse(response, null, HttpServletResponse.SC_BAD_REQUEST, AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.EXPERIMENT_NAME_MANDATORY);
            return;
        }

        // Check if interval_end_time is provided
        if (intervalEndTimeStr == null || intervalEndTimeStr.isEmpty()) {
            sendErrorResponse(response, null, HttpServletResponse.SC_BAD_REQUEST, AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.INTERVAL_END_TIME_MANDATORY);
            return;
        }

        // Convert interval_endtime to UTC date format
        Timestamp interval_end_time = null;
        if (intervalEndTimeStr != null) {
            if (!Utils.DateUtils.isAValidDate(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, intervalEndTimeStr)) {
                sendErrorResponse(
                        response,
                        new Exception(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_TIMESTAMP_EXCPTN),
                        HttpServletResponse.SC_BAD_REQUEST,
                        String.format(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_TIMESTAMP_MSG, intervalEndTimeStr)
                );
                return;
            }
            //Check if data exist
            interval_end_time = Utils.DateUtils.getTimeStampFrom(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, intervalEndTimeStr);
        }

        ExperimentResultData experimentResultData = null;
        try {
            experimentResultData = new ExperimentDBService().getExperimentResultData(experiment_name, interval_end_time);
        } catch (Exception e) {
            sendErrorResponse(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            return;
        }

        if (null != experimentResultData) {
            //Load KruizeObject and generate recommendation
            Map<String, KruizeObject> mainKruizeExperimentMAP = new HashMap<>();
            try {
                //Load KruizeObject
                ExperimentDBService experimentDBService = new ExperimentDBService();
                experimentDBService.loadExperimentFromDBByName(mainKruizeExperimentMAP, experiment_name);
                KruizeObject kruizeObject = mainKruizeExperimentMAP.get(experiment_name);
                /*
                    To restrict the number of rows in the result set, the Load results operation involves locating the appropriate method and configuring the desired limitation.
                    It's important to note that in order for the Limit rows feature to function correctly,
                    the CreateExperiment API must adhere strictly to the trail settings' measurement duration and should not allow arbitrary values
                 */
                int limitRows = (int) ((
                        KruizeConstants.RecommendationEngineConstants.DurationBasedEngine.DurationAmount.LONG_TERM_DURATION_DAYS *
                                KruizeConstants.DateFormats.MINUTES_FOR_DAY)
                        / kruizeObject.getTrial_settings().getMeasurement_durationMinutes_inDouble());
                experimentDBService.loadResultsFromDBByName(mainKruizeExperimentMAP, experiment_name, interval_end_time, limitRows);
                boolean recommendationCheck = new ExperimentInitiator().generateAndAddRecommendations(mainKruizeExperimentMAP, Collections.singletonList(experimentResultData));
                if (!recommendationCheck)
                    LOGGER.error("Failed to create recommendation for experiment: %s and interval_end_time: %s",
                            experimentResultData.getExperiment_name(),
                            experimentResultData.getIntervalEndTime());
                else {
                    boolean success = new ExperimentDBService().addRecommendationToDB(mainKruizeExperimentMAP, Collections.singletonList(experimentResultData));
                    if (success)
                        sendSuccessResponse(response, kruizeObject, interval_end_time);
                    else {
                        sendErrorResponse(response, null, HttpServletResponse.SC_BAD_REQUEST, RecommendationConstants.RecommendationNotificationMsgConstant.NOT_ENOUGH_DATA);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            sendErrorResponse(response, null, HttpServletResponse.SC_BAD_REQUEST, AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.DATA_NOT_FOUND);
            return;
        }
    }

    private void sendSuccessResponse(HttpServletResponse response, KruizeObject ko, Timestamp interval_end_time) throws IOException {
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        response.setStatus(HttpServletResponse.SC_CREATED);
        List<ListRecommendationsAPIObject> recommendationList = new ArrayList<>();
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
                    .setExclusionStrategies(strategy)
                    .create();
            gsonStr = gsonObj.toJson(recommendationList);
        }
        response.getWriter().println(gsonStr);
        response.getWriter().close();
    }

    public void sendErrorResponse(HttpServletResponse response, Exception e, int httpStatusCode, String errorMsg) throws
            IOException {
        if (null != e) {
            LOGGER.error(e.toString());
            if (null == errorMsg) errorMsg = e.getMessage();
        }
        response.sendError(httpStatusCode, errorMsg);
    }
}
