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

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.recommendations.engine.RecommendationEngine;
import com.autotune.analyzer.serviceObjects.ContainerAPIObject;
import com.autotune.analyzer.serviceObjects.Converters;
import com.autotune.analyzer.serviceObjects.ListRecommendationsAPIObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.GsonUTCDateAdapter;
import com.autotune.common.data.dataSourceQueries.PromQLDataSourceQueries;
import com.autotune.common.data.metrics.MetricAggregationInfoResults;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.utils.GenericRestApiClient;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.MetricsConfig;
import com.autotune.utils.Utils;
import com.google.gson.*;
import io.micrometer.core.instrument.Timer;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

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
        LOGGER.debug("UpdateRecommendations API request count: {}", calCount);
        String statusValue = "failure";
        Timer.Sample timerBUpdateRecommendations = Timer.start(MetricsConfig.meterRegistry());
        try {
            // Set the character encoding of the request to UTF-8
            request.setCharacterEncoding(CHARACTER_ENCODING);
            // Get the values from the request parameters
            String experiment_name = request.getParameter(KruizeConstants.JSONKeys.EXPERIMENT_NAME);
            String intervalEndTimeStr = request.getParameter(KruizeConstants.JSONKeys.INTERVAL_END_TIME);
            String intervalStartTimeStr = request.getParameter(KruizeConstants.JSONKeys.INTERVAL_START_TIME);
            Timestamp interval_end_time, interval_start_time;

            // create recommendation engine object
            RecommendationEngine recommendationEngine = new RecommendationEngine(experiment_name, intervalEndTimeStr, intervalStartTimeStr);
            // validate and create KruizeObject if successful
            String validationMessage = recommendationEngine.validate();
            if (validationMessage.isEmpty()) {
                KruizeObject kruizeObject = recommendationEngine.prepareRecommendations(calCount);
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

    public static void fetchMetricsBasedOnDatasource(KruizeObject kruizeObject, Timestamp interval_end_time, Timestamp interval_start_time, DataSourceInfo dataSourceInfo) throws Exception {
        try {
            long interval_end_time_epoc = interval_end_time.getTime() / KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC
                    - ((long) interval_end_time.getTimezoneOffset() * KruizeConstants.TimeConv.NO_OF_SECONDS_PER_MINUTE);
            long interval_start_time_epoc = interval_start_time.getTime() / KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC
                    - ((long) interval_start_time.getTimezoneOffset() * KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC);
            // Get MetricsProfile name and list of promQL to fetch
            Map<AnalyzerConstants.MetricName, String> promQls = new HashMap<>();
            getPromQls(promQls);
            List<String> aggregationMethods = Arrays.asList(KruizeConstants.JSONKeys.SUM, KruizeConstants.JSONKeys.AVG,
                    KruizeConstants.JSONKeys.MAX, KruizeConstants.JSONKeys.MIN);
            Double measurementDurationMinutesInDouble = kruizeObject.getTrial_settings().getMeasurement_durationMinutes_inDouble();
            List<K8sObject> kubernetes_objects = kruizeObject.getKubernetes_objects();
            for (K8sObject k8sObject : kubernetes_objects) {
                String namespace = k8sObject.getNamespace();
                HashMap<String, ContainerData> containerDataMap = k8sObject.getContainerDataMap();
                for (Map.Entry<String, ContainerData> entry : containerDataMap.entrySet()) {
                    ContainerData containerData = entry.getValue();
                    String containerName = containerData.getContainer_name();
                    HashMap<Timestamp, IntervalResults> containerDataResults = new HashMap<>();
                    IntervalResults intervalResults;
                    HashMap<AnalyzerConstants.MetricName, MetricResults> resMap;
                    MetricResults metricResults;
                    MetricAggregationInfoResults metricAggregationInfoResults;
                    for (Map.Entry<AnalyzerConstants.MetricName, String> metricEntry : promQls.entrySet()) {
                        for (String methodName : aggregationMethods) {
                            String promQL = null;
                            String format = null;
                            if (metricEntry.getKey() == AnalyzerConstants.MetricName.cpuUsage) {
                                String secondMethodName = methodName;
                                if (secondMethodName.equals(KruizeConstants.JSONKeys.SUM))
                                    secondMethodName = KruizeConstants.JSONKeys.AVG;
                                promQL = String.format(metricEntry.getValue(), methodName, secondMethodName, namespace, containerName, measurementDurationMinutesInDouble.intValue());
                                format = KruizeConstants.JSONKeys.CORES;
                            } else if (metricEntry.getKey() == AnalyzerConstants.MetricName.cpuThrottle) {
                                promQL = String.format(metricEntry.getValue(), methodName, namespace, containerName, measurementDurationMinutesInDouble.intValue());
                                format = KruizeConstants.JSONKeys.CORES;
                            } else if (metricEntry.getKey() == AnalyzerConstants.MetricName.cpuLimit || metricEntry.getKey() == AnalyzerConstants.MetricName.cpuRequest) {
                                promQL = String.format(metricEntry.getValue(), methodName, namespace, containerName);
                                format = KruizeConstants.JSONKeys.CORES;
                            } else if (metricEntry.getKey() == AnalyzerConstants.MetricName.memoryUsage || metricEntry.getKey() == AnalyzerConstants.MetricName.memoryRSS) {
                                String secondMethodName = methodName;
                                if (secondMethodName.equals(KruizeConstants.JSONKeys.SUM))
                                    secondMethodName = KruizeConstants.JSONKeys.AVG;
                                promQL = String.format(metricEntry.getValue(), methodName, secondMethodName, namespace, containerName, measurementDurationMinutesInDouble.intValue());
                                format = KruizeConstants.JSONKeys.GIBIBYTE;
                            } else if (metricEntry.getKey() == AnalyzerConstants.MetricName.memoryLimit || metricEntry.getKey() == AnalyzerConstants.MetricName.memoryRequest) {
                                promQL = String.format(metricEntry.getValue(), methodName, namespace, containerName);
                                format = KruizeConstants.JSONKeys.GIBIBYTE;
                            }
                            if (promQL != null) {
                                LOGGER.info(promQL);
                                String podMetricsUrl;
                                try {
                                    podMetricsUrl = String.format(KruizeConstants.DataSourceConstants.DATASOURCE_ENDPOINT_WITH_QUERY,
                                            dataSourceInfo.getUrl(),
                                            URLEncoder.encode(promQL, CHARACTER_ENCODING),
                                            interval_start_time_epoc,
                                            interval_end_time_epoc,
                                            measurementDurationMinutesInDouble.intValue() * 60);
                                    LOGGER.info(podMetricsUrl);
                                    JSONObject genericJsonObject = new GenericRestApiClient(podMetricsUrl).fetchMetricsJson("get", "");
                                    JsonObject jsonObject = new Gson().fromJson(genericJsonObject.toString(), JsonObject.class);
                                    JsonArray resultArray = jsonObject.getAsJsonObject(KruizeConstants.JSONKeys.DATA).getAsJsonArray(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.RESULT);
                                    if (null != resultArray && !resultArray.isEmpty()) {
                                        resultArray = jsonObject.getAsJsonObject(KruizeConstants.JSONKeys.DATA).getAsJsonArray(
                                                        KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.RESULT).get(0)
                                                .getAsJsonObject().getAsJsonArray(KruizeConstants.DataSourceConstants
                                                        .DataSourceQueryJSONKeys.VALUES);
                                        SimpleDateFormat sdf = new SimpleDateFormat(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, Locale.ROOT);
                                        sdf.setTimeZone(TimeZone.getTimeZone(KruizeConstants.TimeUnitsExt.TimeZones.UTC));
                                        // Find LongTerm duration  keep Start , End and Step(measurement Duration)
                                        Timestamp sTime = interval_start_time;
                                        for (JsonElement element : resultArray) {
                                            JsonArray valueArray = element.getAsJsonArray();
                                            long epochTime = valueArray.get(0).getAsLong();
                                            double value = valueArray.get(1).getAsDouble();
                                            String timestamp = sdf.format(new Date(epochTime * KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC));
                                            Date date = sdf.parse(timestamp);
                                            Timestamp eTime = new Timestamp(date.getTime());
                                            if (containerDataResults.containsKey(eTime)) {
                                                intervalResults = containerDataResults.get(eTime);
                                                resMap = intervalResults.getMetricResultsMap();
                                            } else {
                                                intervalResults = new IntervalResults();
                                                resMap = new HashMap<>();
                                            }
                                            if (resMap.containsKey(metricEntry.getKey())) {
                                                metricResults = resMap.get(metricEntry.getKey());
                                                metricAggregationInfoResults = metricResults.getAggregationInfoResult();
                                            } else {
                                                metricResults = new MetricResults();
                                                metricAggregationInfoResults = new MetricAggregationInfoResults();
                                            }
                                            Method method = MetricAggregationInfoResults.class.getDeclaredMethod("set" + methodName.substring(0, 1).toUpperCase() + methodName.substring(1), Double.class);
                                            method.invoke(metricAggregationInfoResults, value);
                                            metricAggregationInfoResults.setFormat(format);
                                            metricResults.setAggregationInfoResult(metricAggregationInfoResults);
                                            metricResults.setName(String.valueOf(metricEntry.getKey()));
                                            metricResults.setFormat(format);
                                            resMap.put(metricEntry.getKey(), metricResults);
                                            intervalResults.setMetricResultsMap(resMap);
                                            intervalResults.setIntervalStartTime(sTime);  //Todo this will change
                                            intervalResults.setIntervalEndTime(eTime);
                                            intervalResults.setDurationInMinutes((double) ((eTime.getTime() - sTime.getTime())
                                                    / ((long) KruizeConstants.TimeConv.NO_OF_SECONDS_PER_MINUTE
                                                    * KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC)));
                                            containerDataResults.put(eTime, intervalResults);
                                            sTime = eTime;
                                        }
                                    }
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                    containerData.setResults(containerDataResults);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Exception occurred while fetching metrics from the datasource: "+e.getMessage());
        }
    }

    private static void getPromQls(Map<AnalyzerConstants.MetricName, String> promQls) {
        promQls.put(AnalyzerConstants.MetricName.cpuUsage, PromQLDataSourceQueries.CPU_USAGE);
        promQls.put(AnalyzerConstants.MetricName.cpuThrottle, PromQLDataSourceQueries.CPU_THROTTLE);
        promQls.put(AnalyzerConstants.MetricName.cpuLimit,PromQLDataSourceQueries.CPU_LIMIT);
        promQls.put(AnalyzerConstants.MetricName.cpuRequest,PromQLDataSourceQueries.CPU_REQUEST);
        promQls.put(AnalyzerConstants.MetricName.memoryUsage, PromQLDataSourceQueries.MEMORY_USAGE);
        promQls.put(AnalyzerConstants.MetricName.memoryRSS, PromQLDataSourceQueries.MEMORY_RSS);
        promQls.put(AnalyzerConstants.MetricName.memoryLimit, PromQLDataSourceQueries.MEMORY_LIMIT);
        promQls.put(AnalyzerConstants.MetricName.memoryRequest, PromQLDataSourceQueries.MEMORY_REQUEST);
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
