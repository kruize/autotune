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
package com.autotune.analyzer.performanceProfiles.PerformanceProfileInterface;

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.kruizeObject.RecommendationSettings;
import com.autotune.analyzer.recommendations.*;
import com.autotune.analyzer.recommendations.engine.CostRecommendationEngine;
import com.autotune.analyzer.recommendations.engine.KruizeRecommendationEngine;
import com.autotune.analyzer.recommendations.engine.PerformanceRecommendationEngine;
import com.autotune.analyzer.recommendations.objects.MappedRecommendationForTimestamp;
import com.autotune.analyzer.recommendations.utils.RecommendationUtils;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.utils.KruizeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Util class to validate the performance profile metrics with the experiment results metrics.
 */
public class ResourceOptimizationOpenshiftImpl extends PerfProfileImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceOptimizationOpenshiftImpl.class);
    List<KruizeRecommendationEngine> kruizeRecommendationEngineList;

    public ResourceOptimizationOpenshiftImpl() {
        this.init();
    }

    private void init() {
        // Add new engines
        kruizeRecommendationEngineList = new ArrayList<KruizeRecommendationEngine>();
        // Create Duration based engine
        CostRecommendationEngine costRecommendationEngine = new CostRecommendationEngine();
        // TODO: Create profile based engine
        AnalyzerConstants.RegisterRecommendationEngineStatus _unused_status = registerEngine(costRecommendationEngine);
        PerformanceRecommendationEngine performanceRecommendationEngine =  new PerformanceRecommendationEngine();
        _unused_status = registerEngine(performanceRecommendationEngine);
        // TODO: Add profile based once recommendation algos are available
    }

    @Override
    public AnalyzerConstants.RegisterRecommendationEngineStatus registerEngine(KruizeRecommendationEngine kruizeRecommendationEngine) {
        if (null == kruizeRecommendationEngine) {
            return AnalyzerConstants.RegisterRecommendationEngineStatus.INVALID;
        }
        for (KruizeRecommendationEngine engine : getEngines()) {
            if (engine.getEngineName().equalsIgnoreCase(kruizeRecommendationEngine.getEngineName()))
                return AnalyzerConstants.RegisterRecommendationEngineStatus.ALREADY_EXISTS;
        }
        // Add engines
        getEngines().add(kruizeRecommendationEngine);
        return AnalyzerConstants.RegisterRecommendationEngineStatus.SUCCESS;
    }

    @Override
    public List<KruizeRecommendationEngine> getEngines() {
        return this.kruizeRecommendationEngineList;
    }

    @Override
    public void generateRecommendation(KruizeObject kruizeObject, List<ExperimentResultData> experimentResultDataList, Timestamp interval_start_time, Timestamp interval_end_time) throws Exception {
        /*
            To restrict the number of rows in the result set, the Load results operation involves locating the appropriate method and configuring the desired limitation.
            It's important to note that in order for the Limit rows feature to function correctly,
            the CreateExperiment API must adhere strictly to the trail settings' measurement duration and should not allow arbitrary values
        */
        String experiment_name = kruizeObject.getExperimentName();
        int limitRows = (int) ((
                KruizeConstants.RecommendationEngineConstants.DurationBasedEngine.DurationAmount.LONG_TERM_DURATION_DAYS *
                        KruizeConstants.DateFormats.MINUTES_FOR_DAY)
                / kruizeObject.getTrial_settings().getMeasurement_durationMinutes_inDouble());

        if (null != interval_start_time) {
            long diffMilliseconds = interval_end_time.getTime() - interval_start_time.getTime();
            long minutes = diffMilliseconds / (60 * 1000);
            int addToLimitRows = (int) (minutes / kruizeObject.getTrial_settings().getMeasurement_durationMinutes_inDouble());
            LOGGER.debug("add to limit rows set to {}", addToLimitRows);
            limitRows = limitRows + addToLimitRows;
        }
        LOGGER.debug("Limit rows set to {}", limitRows);

        Map<String, KruizeObject> mainKruizeExperimentMap = new HashMap<>();
        mainKruizeExperimentMap.put(experiment_name, kruizeObject);
        new ExperimentDBService().loadResultsFromDBByName(mainKruizeExperimentMap,
                experiment_name,
                interval_end_time,
                limitRows);
        //TODO: Will be updated once algo is completed
        for (ExperimentResultData experimentResultData : experimentResultDataList) {
            if (null != kruizeObject && null != experimentResultData) {
                RecommendationSettings recommendationSettings = kruizeObject.getRecommendation_settings();

                for (K8sObject k8sObjectResultData : experimentResultData.getKubernetes_objects()) {
                    // We only support one K8sObject currently
                    K8sObject k8sObjectKruizeObject = kruizeObject.getKubernetes_objects().get(0);
                    for (String cName : k8sObjectResultData.getContainerDataMap().keySet()) {
                        ContainerData containerDataResultData = k8sObjectResultData.getContainerDataMap().get(cName);
                        if (null == containerDataResultData.getResults())
                            continue;
                        if (containerDataResultData.getResults().isEmpty())
                            continue;
                        // Get the monitoringEndTime from ResultData's ContainerData. Should have only one element
                        Timestamp monitoringEndTime = containerDataResultData.getResults().keySet().stream().max(Timestamp::compareTo).get();
                        // Get the ContainerData from the KruizeObject and not from ResultData
                        ContainerData containerDataKruizeObject = k8sObjectKruizeObject.getContainerDataMap().get(cName);

                        ContainerRecommendations containerRecommendations = containerDataKruizeObject.getContainerRecommendations();
                        // Get the engine recommendation map for a time stamp if it exists else create one
                        HashMap<Timestamp, MappedRecommendationForTimestamp> timestampBasedRecommendationMap
                                = containerRecommendations.getData();
                        if (null == timestampBasedRecommendationMap) {
                            timestampBasedRecommendationMap = new HashMap<Timestamp, MappedRecommendationForTimestamp>();
                        }
                        // check if engines map exists else create one
                        MappedRecommendationForTimestamp timestampRecommendation = null;
                        if (timestampBasedRecommendationMap.containsKey(monitoringEndTime)) {
                            timestampRecommendation = timestampBasedRecommendationMap.get(monitoringEndTime);
                        } else {
                            timestampRecommendation = new MappedRecommendationForTimestamp();
                        }
                        HashMap<Timestamp, IntervalResults> intervalResultsHashMap = containerDataResultData.getResults();
                        HashMap<Integer, RecommendationConstants.RecommendationNotification> notificationHashMap = new HashMap<>();
                        timestampRecommendation.setMonitoringEndTime(monitoringEndTime);
                        HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> currentConfig = new HashMap<>();

                        ArrayList<RecommendationConstants.RecommendationNotification> notifications = new ArrayList<>();

                        // Create Current Requests Map
                        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> currentRequestsMap = new HashMap<>();

                        // Create Current Limits Map
                        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> currentLimitsMap = new HashMap<>();

                        for (AnalyzerConstants.ResourceSetting resourceSetting : AnalyzerConstants.ResourceSetting.values()) {
                            for (AnalyzerConstants.RecommendationItem recommendationItem : AnalyzerConstants.RecommendationItem.values()) {
                                RecommendationConfigItem configItem = RecommendationUtils.getCurrentValue(intervalResultsHashMap,
                                                                    monitoringEndTime,
                                                                    resourceSetting,
                                                                    recommendationItem,
                                                                    notifications);

                                if (null == configItem)
                                    continue;
                                // Need to set appropriate notifications
                                if (null == configItem.getAmount())
                                    continue;
                                // Need to set appropriate notifications
                                if (null == configItem.getFormat())
                                    continue;
                                // Need to set appropriate notifications
                                if (configItem.getAmount() <= 0.0)
                                    continue;
                                // Need to set appropriate notifications
                                if (configItem.getFormat().isEmpty() || configItem.getFormat().isBlank())
                                    continue;

                                if (resourceSetting == AnalyzerConstants.ResourceSetting.requests) {
                                    currentRequestsMap.put(recommendationItem, configItem);
                                }
                                if (resourceSetting == AnalyzerConstants.ResourceSetting.limits) {
                                    currentLimitsMap.put(recommendationItem, configItem);
                                }
                            }
                        }

                        // Iterate over notifications and set to recommendations
                        for (RecommendationConstants.RecommendationNotification recommendationNotification : notifications) {
                            if (!notificationHashMap.containsKey(recommendationNotification.getCode()))
                                notificationHashMap.put(recommendationNotification.getCode(), recommendationNotification);
                        }
                        timestampRecommendation.setHigherLevelNotificationMap(notificationHashMap);

                        // Check if map is not empty and set requests map to current config
                        if (!currentRequestsMap.isEmpty()) {
                            currentConfig.put(AnalyzerConstants.ResourceSetting.requests, currentRequestsMap);
                        }

                        // Check if map is not empty and set limits map to current config
                        if (!currentLimitsMap.isEmpty()) {
                            currentConfig.put(AnalyzerConstants.ResourceSetting.limits, currentLimitsMap);
                        }

                        for (RecommendationConstants.RecommendationTerms recommendationTerm : RecommendationConstants.RecommendationTerms.values()) {
                            String term = recommendationTerm.getValue();
                            int duration = recommendationTerm.getDuration();
                            for (KruizeRecommendationEngine engine : getEngines()) {
                                boolean isCostEngine = false;
                                boolean isPerfEngine = false;

                                if (engine.getEngineName().equalsIgnoreCase(RecommendationConstants.RecommendationEngine.EngineNames.COST))
                                    isCostEngine = true;
                                if (engine.getEngineName().equalsIgnoreCase(RecommendationConstants.RecommendationEngine.EngineNames.PERFORMANCE))
                                    isPerfEngine = true;

                                // Check if minimum data available to generate recommendation
                                if (!engine.checkIfMinDataAvailable(containerDataKruizeObject))
                                    continue;

                                // Now generate a new recommendation for the new data corresponding to the monitoringEndTime
                                HashMap<String, Recommendation> recommendationHashMap = engine.generateRecommendation(containerDataKruizeObject, monitoringEndTime, recommendationSettings);
                                if (null == recommendationHashMap || recommendationHashMap.isEmpty())
                                    continue;

                                // Just to make sure the container recommendations object is not empty
                                if (null == containerRecommendations) {
                                    containerRecommendations = new ContainerRecommendations();
                                }
                                // check if notification exists
                                boolean notificationExist = false;
                                if (isCostEngine && containerRecommendations.getNotificationMap().containsKey(RecommendationConstants.NotificationCodes.INFO_COST_RECOMMENDATIONS_AVAILABLE)) {
                                    notificationExist = true;
                                } else if (isPerfEngine && containerRecommendations.getNotificationMap().containsKey(RecommendationConstants.NotificationCodes.INFO_PERFORMANCE_RECOMMENDATIONS_AVAILABLE))
                                    notificationExist = true;

                                // If there is no notification add one
                                if (!notificationExist) {
                                    if (isCostEngine) {
                                        RecommendationNotification recommendationNotification = new RecommendationNotification(
                                                RecommendationConstants.RecommendationNotification.INFO_COST_RECOMMENDATIONS_AVAILABLE
                                        );
                                        containerRecommendations.getNotificationMap().put(recommendationNotification.getCode(), recommendationNotification);
                                    }
                                    if (isPerfEngine) {
                                        RecommendationNotification recommendationNotification = new RecommendationNotification(
                                                RecommendationConstants.RecommendationNotification.INFO_PERFORMANCE_RECOMMENDATIONS_AVAILABLE
                                        );
                                        containerRecommendations.getNotificationMap().put(recommendationNotification.getCode(), recommendationNotification);
                                    }
                                }
                            }
                        }

                        // put recommendations tagging to timestamp
                        timestampBasedRecommendationMap.put(monitoringEndTime, timestampRecommendation);
                        // set the data object to map
                        containerRecommendations.setData(timestampBasedRecommendationMap);
                        // set the container recommendations in container object
                        containerDataKruizeObject.setContainerRecommendations(containerRecommendations);
                    }
                }
            }
        }
    }
}
