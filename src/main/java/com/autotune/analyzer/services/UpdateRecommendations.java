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
import com.autotune.analyzer.serviceObjects.ContainerAPIObject;
import com.autotune.analyzer.serviceObjects.Converters;
import com.autotune.analyzer.serviceObjects.ListRecommendationsAPIObject;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.analyzer.utils.GsonUTCDateAdapter;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.operator.KruizeDeploymentInfo;
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

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

/**
 *
 */
@WebServlet(asyncSupported = true)
public class UpdateRecommendations extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateRecommendations.class);
    private static final BlockingQueue queue = new ArrayBlockingQueue<>(20000);
    private static ScheduledExecutorService executorService = Executors.newScheduledThreadPool(20);

    static {
        executorService.scheduleAtFixedRate(UpdateRecommendations::newEvent, 0, 0, TimeUnit.MILLISECONDS);
    }

    private static void newEvent() {
        ArrayList clients = new ArrayList<AsyncContext>(queue.size());
        queue.drainTo(clients);
        clients.parallelStream().forEach((object) ->
        {
            String statusValue = "failure";
            AsyncContext ac = (AsyncContext) object;
            ServletRequest request = ac.getRequest();
            ServletResponse response = ac.getResponse();
            Timer.Sample timerBUpdateRecommendations = Timer.start(MetricsConfig.meterRegistry());
            try {
                // Get the values from the request parameters
                String experiment_name = request.getParameter(KruizeConstants.JSONKeys.EXPERIMENT_NAME);
                String intervalEndTimeStr = request.getParameter(KruizeConstants.JSONKeys.INTERVAL_END_TIME);
                System.out.println(experiment_name);

                String intervalStartTimeStr = request.getParameter(KruizeConstants.JSONKeys.INTERVAL_START_TIME);
                Timestamp interval_end_time = null;
                Timestamp interval_start_time = null;

                // Check if experiment_name is provided
                if (experiment_name == null || experiment_name.isEmpty()) {
                    sendErrorResponse((HttpServletResponse) response, null, HttpServletResponse.SC_BAD_REQUEST, AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.EXPERIMENT_NAME_MANDATORY);
                    return;
                }

                // Check if interval_end_time is provided
                if (intervalEndTimeStr == null || intervalEndTimeStr.isEmpty()) {
                    sendErrorResponse((HttpServletResponse) response, null, HttpServletResponse.SC_BAD_REQUEST, AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.INTERVAL_END_TIME_MANDATORY);
                    return;
                }
                if (!Utils.DateUtils.isAValidDate(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, intervalEndTimeStr)) {
                    sendErrorResponse(
                            (HttpServletResponse) response,
                            new Exception(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_TIMESTAMP_EXCPTN),
                            HttpServletResponse.SC_BAD_REQUEST,
                            String.format(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_TIMESTAMP_MSG, intervalEndTimeStr)
                    );
                    return;
                } else {
                    interval_end_time = Utils.DateUtils.getTimeStampFrom(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, intervalEndTimeStr);
                }


                // Check if interval_start_time is provided
                if (intervalStartTimeStr != null) {
                    if (!Utils.DateUtils.isAValidDate(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, intervalStartTimeStr)) {
                        sendErrorResponse(
                                (HttpServletResponse) response,
                                new Exception(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_TIMESTAMP_EXCPTN),
                                HttpServletResponse.SC_BAD_REQUEST,
                                String.format(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_TIMESTAMP_MSG, intervalStartTimeStr)
                        );
                        return;
                    } else {
                        interval_start_time = Utils.DateUtils.getTimeStampFrom(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, intervalStartTimeStr);
                        int comparisonResult = interval_start_time.compareTo(interval_end_time);
                        if (comparisonResult >= 0) {
                            // interval_start_time is after interval_end_time
                            sendErrorResponse((HttpServletResponse) response, null, HttpServletResponse.SC_BAD_REQUEST, AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.TIME_COMPARE);
                            return;
                        } else {
                            // calculate difference between two dates
                            long differenceInMillis = interval_end_time.getTime() - interval_start_time.getTime();
                            long differenceInDays = TimeUnit.MILLISECONDS.toDays(differenceInMillis);
                            if (differenceInDays > KruizeDeploymentInfo.generate_recommendations_date_range_limit_in_days) {
                                sendErrorResponse((HttpServletResponse) response, null, HttpServletResponse.SC_BAD_REQUEST, AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.TIME_GAP_LIMIT);
                                return;
                            }
                        }
                    }
                }

                LOGGER.debug("experiment_name : {} and interval_start_time : {} and interval_end_time : {} ", experiment_name, intervalStartTimeStr, intervalEndTimeStr);

                List<ExperimentResultData> experimentResultDataList = new ArrayList<>();
                ExperimentResultData experimentResultData = null;
                Map<String, KruizeObject> mainKruizeExperimentMAP = new ConcurrentHashMap<>();
                try {
                    String clusterName = null;
                    if (mainKruizeExperimentMAP.containsKey(experiment_name)) {
                        clusterName = mainKruizeExperimentMAP.get(experiment_name).getClusterName();
                    } else {
                        new ExperimentDBService().loadExperimentFromDBByName(mainKruizeExperimentMAP, experiment_name);
                        if (null != mainKruizeExperimentMAP.get(experiment_name)) {
                            clusterName = mainKruizeExperimentMAP.get(experiment_name).getClusterName();
                        }
                    }
                    LOGGER.debug(clusterName);
                    if (null != clusterName)
                        experimentResultDataList = new ExperimentDBService().getExperimentResultData(experiment_name, clusterName, interval_start_time, interval_end_time);   // Todo this object is not required
                } catch (Exception e) {
                    sendErrorResponse((HttpServletResponse) response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                    return;
                }

                if (experimentResultDataList.size() > 0) {
                    //generate recommendation
                    try {
                        KruizeObject kruizeObject = mainKruizeExperimentMAP.get(experiment_name);
                        new ExperimentInitiator().generateAndAddRecommendations(kruizeObject, experimentResultDataList, interval_start_time, interval_end_time);    // TODO: experimentResultDataList not required
                        ValidationOutputData validationOutputData = new ExperimentDBService().addRecommendationToDB(mainKruizeExperimentMAP, experimentResultDataList);
                        if (validationOutputData.isSuccess()) {
                            sendSuccessResponse((HttpServletResponse) response, kruizeObject, interval_end_time);
                            statusValue = "success";
                        } else {
                            sendErrorResponse((HttpServletResponse) response, null, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, validationOutputData.getMessage());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        LOGGER.error("Failed to create recommendation for experiment: {} and interval_start_time: {} and interval_end_time: {}",
                                experiment_name,
                                interval_start_time,
                                interval_end_time);
                        sendErrorResponse((HttpServletResponse) response, null, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                        return;
                    }
                } else {
                    sendErrorResponse((HttpServletResponse) response, null, HttpServletResponse.SC_BAD_REQUEST, AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.DATA_NOT_FOUND);
                    return;
                }
            } catch (Exception e) {
                LOGGER.error("Exception: " + e.getMessage());
                e.printStackTrace();
                try {
                    sendErrorResponse((HttpServletResponse) response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            } finally {
                if (null != timerBUpdateRecommendations) {
                    MetricsConfig.timerUpdateRecomendations = MetricsConfig.timerBUpdateRecommendations.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                    timerBUpdateRecommendations.stop(MetricsConfig.timerUpdateRecomendations);
                }
            }

            ac.complete();
        });

    }

    public static void addToWaitingList(AsyncContext c) {
        queue.add(c);
    }

    private static void sendSuccessResponse(HttpServletResponse response, KruizeObject ko, Timestamp interval_end_time) throws IOException {
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

    public static void sendErrorResponse(HttpServletResponse response, Exception e, int httpStatusCode, String errorMsg) throws
            IOException {
        if (null != e) {
            LOGGER.error(e.toString());
            if (null == errorMsg) errorMsg = e.getMessage();
        }
        response.sendError(httpStatusCode, errorMsg);
    }

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
        String statusValue = "failure";
        addToWaitingList(request.startAsync());
    }
}
