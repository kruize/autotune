/*******************************************************************************
 * Copyright (c) 2022, 2022 Red Hat, IBM Corporation and others.
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
import com.autotune.analyzer.recommendations.Recommendation;
import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.RecommendationNotification;
import com.autotune.analyzer.recommendations.summary.NotificationsSummary;
import com.autotune.analyzer.recommendations.summary.RecommendationSummary;
import com.autotune.analyzer.serviceObjects.Converters;
import com.autotune.analyzer.serviceObjects.ListRecommendationsAPIObject;
import com.autotune.analyzer.serviceObjects.SummarizeAPIObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.analyzer.utils.GsonUTCDateAdapter;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.KruizeSupportedTypes;
import com.autotune.utils.MetricsConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

public class Summarize extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(Summarize.class);

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Timer.Sample timerListRec = Timer.start(MetricsConfig.meterRegistry());
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        List<ListRecommendationsAPIObject> recommendationList;
        Map<String, KruizeObject> mKruizeExperimentMap = new ConcurrentHashMap<>();
        String clusterName = request.getParameter(KruizeConstants.JSONKeys.CLUSTER_NAME);
        String nsName = request.getParameter(KruizeConstants.JSONKeys.NAMESPACE_NAME);

        // validate Query params
        Set<String> invalidParams = new HashSet<>();
        for (String param : request.getParameterMap().keySet()) {
            if (!KruizeSupportedTypes.CLUSTER_PARAMS_SUPPORTED.contains(param)) {
                invalidParams.add(param);
            }
        }
        if (invalidParams.isEmpty()) {
            // load recommendations based on params
            try {
                ExperimentDBService experimentDBService = new ExperimentDBService();
                if (clusterName != null) {
                    clusterName = clusterName.trim();
                    if (nsName != null) {
                        nsName = nsName.trim();
                        experimentDBService.loadAllRecommendationsByClusterAndNSName(mKruizeExperimentMap, clusterName, nsName);
                    } else {
                        experimentDBService.loadAllRecommendationsByClusterName(mKruizeExperimentMap, clusterName);
                    }
                } else if (nsName != null) {
                    nsName = nsName.trim();
                    experimentDBService.loadAllRecommendationsByNSName(mKruizeExperimentMap, nsName);
                } else {
                    experimentDBService.loadAllExperimentsAndRecommendations(mKruizeExperimentMap);
                }
            } catch (Exception e) {
                LOGGER.error("Loading saved recommendations failed: {} ", e.getMessage());
            }
            // Add all experiments to list
            List<KruizeObject> kruizeObjectList = new ArrayList<>(mKruizeExperimentMap.values());
            try {
                // get the latest recommendations
                recommendationList = ListRecommendations.buildAPIResponse(kruizeObjectList, false,
                        true, null);
                // convert the recommendation response to summarizeAPI Object
                List<SummarizeAPIObject> summarizeAPIObjectList = Converters.KruizeObjectConverters.
                        convertListRecommendationAPIObjToSummarizeAPIObj(recommendationList, nsName, clusterName);

                String gsonStr = "[]";
                Gson gsonObj = new GsonBuilder()
                        .disableHtmlEscaping()
                        .setPrettyPrinting()
                        .enableComplexMapKeySerialization()
                        .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                        .create();
                gsonStr = gsonObj.toJson(summarizeAPIObjectList);
                response.getWriter().println(gsonStr);
                response.getWriter().close();
            } catch (Exception e) {
                LOGGER.error("Exception: " + e.getMessage());
                e.printStackTrace();
                sendErrorResponse(response, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } finally {
                timerListRec.stop(MetricsConfig.timerListRec);
            }
        } else {
            sendErrorResponse(
                    response,
                    new Exception(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_QUERY_PARAM),
                    HttpServletResponse.SC_BAD_REQUEST,
                    String.format(AnalyzerErrorConstants.APIErrors.ListRecommendationsAPI.INVALID_QUERY_PARAM, invalidParams)
            );
        }
    }
    public RecommendationSummary convertToSummary(Recommendation recommendation) {
        RecommendationSummary summary = new RecommendationSummary();
        summary.setCurrentConfig(recommendation.getCurrentConfig());
        summary.setConfig(recommendation.getConfig());
        summary.setChange(calculateChange(recommendation));
        summary.setNotificationsSummary(calculateNotificationsSummary(recommendation.getNotifications()));
        return summary;
    }

    private HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> calculateChange(Recommendation recommendation) {
        HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> changeMap = new HashMap<>();
        changeMap.put(AnalyzerConstants.ResourceChange.increase, new HashMap<>());
        changeMap.put(AnalyzerConstants.ResourceChange.decrease, new HashMap<>());
        changeMap.put(AnalyzerConstants.ResourceChange.variation, new HashMap<>());

        HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> variationConfig = recommendation.getVariation();

        for (Map.Entry<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> settingEntry : variationConfig.entrySet()) {
            AnalyzerConstants.ResourceSetting resourceSetting = settingEntry.getKey();
            HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> itemMap = settingEntry.getValue();

            for (Map.Entry<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> itemEntry : itemMap.entrySet()) {
                AnalyzerConstants.RecommendationItem recommendationItem = itemEntry.getKey();
                RecommendationConfigItem configItem = itemEntry.getValue();
                double amount = configItem.getAmount();

                if (amount > 0) {
                    changeMap.get(AnalyzerConstants.ResourceChange.increase)
                            .computeIfAbsent(resourceSetting, k -> new HashMap<>())
                            .put(recommendationItem, configItem);
                } else if (amount < 0) {
                    changeMap.get(AnalyzerConstants.ResourceChange.decrease)
                            .computeIfAbsent(resourceSetting, k -> new HashMap<>())
                            .put(recommendationItem, configItem);
                }
            }
        }

        // Calculate the variation
        HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> increaseConfig = changeMap.get(AnalyzerConstants.ResourceChange.increase);
        HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> decreaseConfig = changeMap.get(AnalyzerConstants.ResourceChange.decrease);

        HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> variationMap = new HashMap<>();
        for (AnalyzerConstants.ResourceSetting resourceSetting : variationConfig.keySet()) {
            HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> variationItemMap = new HashMap<>();
            HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> increaseItemMap = increaseConfig.getOrDefault(resourceSetting, new HashMap<>());
            HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> decreaseItemMap = decreaseConfig.getOrDefault(resourceSetting, new HashMap<>());

            for (Map.Entry<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> entry : variationConfig.get(resourceSetting).entrySet()) {
                AnalyzerConstants.RecommendationItem recommendationItem = entry.getKey();
                RecommendationConfigItem variationConfigItem = entry.getValue();

                RecommendationConfigItem increaseConfigItem = increaseItemMap.get(recommendationItem);
                if (increaseConfigItem == null) {
                    increaseConfigItem = new RecommendationConfigItem(0.0, null);
                }

                RecommendationConfigItem decreaseConfigItem = decreaseItemMap.get(recommendationItem);
                if (decreaseConfigItem == null) {
                    decreaseConfigItem = new RecommendationConfigItem(0.0, null);
                }

                double variationValue = increaseConfigItem.getAmount() + decreaseConfigItem.getAmount();
                variationConfigItem.setAmount(variationValue);
                variationItemMap.put(recommendationItem, variationConfigItem);
            }

            variationMap.put(resourceSetting, variationItemMap);
        }
        changeMap.put(AnalyzerConstants.ResourceChange.variation, variationMap);
        return changeMap;
    }

    public NotificationsSummary calculateNotificationsSummary(HashMap<Integer, RecommendationNotification> notifications) {
        NotificationsSummary summary = new NotificationsSummary();
        int infoCount = 0;
        int noticeCount = 0;
        int warningCount = 0;
        int errorCount = 0;
        int criticalCount = 0;

        for (RecommendationNotification notification : notifications.values()) {
            switch (notification.getType()) {
                case "info" -> infoCount++;
                case "notice" -> noticeCount++;
                case "warning" -> warningCount++;
                case "error" -> errorCount++;
                case "critical" -> criticalCount++;
            }
        }
        summary.setInfo(infoCount);
        summary.setNotice(noticeCount);
        summary.setWarning(warningCount);
        summary.setError(errorCount);
        summary.setCritical(criticalCount);
        return summary;
    }
    public RecommendationSummary mergeSummaries(RecommendationSummary summary1, RecommendationSummary summary2) {
        RecommendationSummary mergedSummary = new RecommendationSummary();

        mergedSummary.setCurrentConfig(mergeConfigItems(summary1.getCurrentConfig(), summary2.getCurrentConfig(), mergedSummary.getCurrentConfig()));
        mergedSummary.setConfig(mergeConfigItems(summary1.getConfig(), summary2.getConfig(), mergedSummary.getConfig()));
        mergedSummary.setChange(mergeChange(summary1, summary2, mergedSummary.getChange()));
        mergedSummary.setNotificationsSummary(mergeNotificationsSummary(summary1.getNotificationsSummary(), summary2.getNotificationsSummary()));
        return mergedSummary;
    }

    private HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> mergeConfigItems(HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> config1, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> config2, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> configMap) {
        if (configMap == null) {
            configMap = new HashMap<>();
        }

        mergeConfigMap(configMap, config1);
        mergeConfigMap(configMap, config2);

        return configMap;
    }

    private void mergeConfigMap(HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem,
            RecommendationConfigItem>> targetMap, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem,
            RecommendationConfigItem>> sourceMap) {
        for (Map.Entry<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> entry :
                sourceMap.entrySet()) {
            AnalyzerConstants.ResourceSetting resourceSetting = entry.getKey();
            HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> itemMap = entry.getValue();

            for (Map.Entry<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> itemEntry : itemMap.entrySet()) {
                AnalyzerConstants.RecommendationItem recommendationItem = itemEntry.getKey();
                RecommendationConfigItem configItem = itemEntry.getValue();

                targetMap.computeIfAbsent(resourceSetting, k -> new HashMap<>())
                        .merge(recommendationItem, configItem, (existingItem, newItem) -> {
                            // Sum the amount values for existing and new RecommendationConfigItems
                            existingItem.setAmount(existingItem.getAmount() + newItem.getAmount());
                            return existingItem;
                        });
            }
        }
    }

    private HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> mergeChange(RecommendationSummary summary1, RecommendationSummary summary2, HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> changeMap) {
        if (changeMap == null) {
            changeMap = new HashMap<>();
        }
        HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> changeMap1 = summary1.getChange();
        HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> changeMap2 = summary2.getChange();

        mergeChangeMap(changeMap, changeMap1);
        mergeChangeMap(changeMap, changeMap2);

        return changeMap;
    }

    private void mergeChangeMap(HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> targetMap, HashMap<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> sourceMap) {
        for (Map.Entry<AnalyzerConstants.ResourceChange, HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>>> entry : sourceMap.entrySet()) {
            AnalyzerConstants.ResourceChange resourceChange = entry.getKey();
            HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> settingMap = entry.getValue();

            for (Map.Entry<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> settingEntry : settingMap.entrySet()) {
                AnalyzerConstants.ResourceSetting resourceSetting = settingEntry.getKey();
                HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> itemMap = settingEntry.getValue();

                for (Map.Entry<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> itemEntry : itemMap.entrySet()) {
                    AnalyzerConstants.RecommendationItem recommendationItem = itemEntry.getKey();
                    RecommendationConfigItem configItem = itemEntry.getValue();

                    targetMap.computeIfAbsent(resourceChange, k -> new HashMap<>())
                            .computeIfAbsent(resourceSetting, k -> new HashMap<>())
                            .merge(recommendationItem, configItem, (existingItem, newItem) -> {
                                // Sum the amount values for existing and new RecommendationConfigItems
                                existingItem.setAmount(existingItem.getAmount() + newItem.getAmount());
                                return existingItem;
                            });
                }
            }
        }
    }

    public NotificationsSummary mergeNotificationsSummary(NotificationsSummary notifications1, NotificationsSummary notifications2) {

        int infoCount = notifications1.getInfo() + notifications2.getInfo();
        int noticeCount = notifications1.getNotice() + notifications2.getNotice();
        int warningCount = notifications1.getWarning() + notifications2.getWarning();
        int errorCount = notifications1.getError() + notifications2.getError();
        int criticalCount = notifications1.getCritical() + notifications2.getCritical();

        NotificationsSummary mergedSummary = new NotificationsSummary();
        mergedSummary.setInfo(infoCount);
        mergedSummary.setNotice(noticeCount);
        mergedSummary.setWarning(warningCount);
        mergedSummary.setError(errorCount);
        mergedSummary.setCritical(criticalCount);
        return mergedSummary;
    }

    public void sendErrorResponse(HttpServletResponse response, Exception e, int httpStatusCode, String errorMsg) throws
            IOException {
        if (null != e) {
            LOGGER.error(e.getMessage());
            if (null == errorMsg) errorMsg = e.getMessage();
        }
        response.sendError(httpStatusCode, errorMsg);
    }


}
