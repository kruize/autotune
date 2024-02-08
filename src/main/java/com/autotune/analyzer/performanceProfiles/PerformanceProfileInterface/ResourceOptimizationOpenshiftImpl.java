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
import com.autotune.analyzer.recommendations.ContainerRecommendations;
import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.recommendations.RecommendationNotification;
import com.autotune.analyzer.recommendations.engine.CostRecommendationEngine;
import com.autotune.analyzer.recommendations.engine.KruizeRecommendationEngine;
import com.autotune.analyzer.recommendations.engine.PerformanceRecommendationEngine;
import com.autotune.analyzer.recommendations.objects.MappedRecommendationForEngine;
import com.autotune.analyzer.recommendations.objects.MappedRecommendationForTimestamp;
import com.autotune.analyzer.recommendations.objects.TermRecommendations;
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
import java.util.*;

import static com.autotune.utils.KruizeConstants.TimeConv.*;


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
        PerformanceRecommendationEngine performanceRecommendationEngine = new PerformanceRecommendationEngine();
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
             The general strategy involves initially attempting the optimal query;
        */
        // Convert the Timestamp to a Calendar instance in UTC time zone
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(interval_end_time.getTime());
        /*
         * interval_start_time Subtract (LONG_TERM_DURATION_DAYS +  THRESHOLD days)
         * Incorporate a buffer period of "threshold days" to account for potential remote cluster downtime.
         * This adjustment aims to align the cumulative hours' duration with LONG_TERM_DURATION_DAYS.
         */
        cal.add(Calendar.DAY_OF_MONTH, -(KruizeConstants.RecommendationEngineConstants.DurationBasedEngine.DurationAmount.LONG_TERM_DURATION_DAYS));
        // Get the new Timestamp after subtracting 15 days
        Timestamp calculated_start_time = new Timestamp(cal.getTimeInMillis());
        Map<String, KruizeObject> mainKruizeExperimentMap = new HashMap<>();
        String experiment_name = kruizeObject.getExperimentName();
        double measurementDuration = kruizeObject.getTrial_settings().getMeasurement_durationMinutes_inDouble();
        mainKruizeExperimentMap.put(experiment_name, kruizeObject);
        new ExperimentDBService().loadResultsFromDBByName(mainKruizeExperimentMap,
                experiment_name,
                calculated_start_time, interval_end_time);
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

                        // Get the ContainerData from the KruizeObject and not from ResultData
                        ContainerData containerDataKruizeObject = k8sObjectKruizeObject.getContainerDataMap().get(cName);

                        HashMap<Integer, RecommendationNotification> notificationHashMap = new HashMap<>();
                        // Get the monitoringEndTime from ResultData's ContainerData. Should have only one element
                        Timestamp monitoringEndTime = containerDataKruizeObject.getResults().keySet().stream().max(Timestamp::compareTo).get();

                        ContainerRecommendations containerRecommendations = containerDataKruizeObject.getContainerRecommendations();
                        // Just to make sure the container recommendations object is not empty
                        if (null == containerRecommendations) {
                            containerRecommendations = new ContainerRecommendations();
                        }

                        HashMap<Integer, RecommendationNotification> recommendationLevelNM = containerRecommendations.getNotificationMap();
                        if (null == recommendationLevelNM) {
                            recommendationLevelNM = new HashMap<>();
                        }

                        // Check for min data before setting notifications
                        boolean recommendationAvailable = false;
                        for (RecommendationConstants.RecommendationTerms recommendationTerm : RecommendationConstants.RecommendationTerms.values()) {
                            if (RecommendationUtils.checkIfMinDataAvailableForTerm(containerDataKruizeObject, recommendationTerm, monitoringEndTime)) {
                                recommendationAvailable = true;
                            }
                        }
                        if (!recommendationAvailable) {
                            RecommendationNotification recommendationNotification = new RecommendationNotification(
                                    RecommendationConstants.RecommendationNotification.INFO_NOT_ENOUGH_DATA
                            );
                            recommendationLevelNM.put(recommendationNotification.getCode(), recommendationNotification);
                            continue;
                        }

                        recommendationAvailable = false;

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
                                if (null == configItem.getAmount()) {
                                    if (recommendationItem.equals(AnalyzerConstants.RecommendationItem.cpu))
                                        notifications.add(RecommendationConstants.RecommendationNotification.ERROR_AMOUNT_MISSING_IN_CPU_SECTION);
                                    else if (recommendationItem.equals((AnalyzerConstants.RecommendationItem.memory)))
                                        notifications.add(RecommendationConstants.RecommendationNotification.ERROR_AMOUNT_MISSING_IN_MEMORY_SECTION);
                                    continue;
                                }
                                if (null == configItem.getFormat()) {
                                    if (recommendationItem.equals(AnalyzerConstants.RecommendationItem.cpu))
                                        notifications.add(RecommendationConstants.RecommendationNotification.ERROR_FORMAT_MISSING_IN_CPU_SECTION);
                                    else if (recommendationItem.equals((AnalyzerConstants.RecommendationItem.memory)))
                                        notifications.add(RecommendationConstants.RecommendationNotification.ERROR_FORMAT_MISSING_IN_MEMORY_SECTION);
                                    continue;
                                }
                                if (configItem.getAmount() <= 0.0) {
                                    if (recommendationItem.equals(AnalyzerConstants.RecommendationItem.cpu))
                                        notifications.add(RecommendationConstants.RecommendationNotification.ERROR_INVALID_AMOUNT_IN_CPU_SECTION);
                                    else if (recommendationItem.equals((AnalyzerConstants.RecommendationItem.memory)))
                                        notifications.add(RecommendationConstants.RecommendationNotification.ERROR_INVALID_AMOUNT_IN_MEMORY_SECTION);
                                    continue;
                                }
                                if (configItem.getFormat().isEmpty() || configItem.getFormat().isBlank()) {
                                    if (recommendationItem.equals(AnalyzerConstants.RecommendationItem.cpu))
                                        notifications.add(RecommendationConstants.RecommendationNotification.ERROR_INVALID_FORMAT_IN_CPU_SECTION);
                                    else if (recommendationItem.equals((AnalyzerConstants.RecommendationItem.memory)))
                                        notifications.add(RecommendationConstants.RecommendationNotification.ERROR_INVALID_FORMAT_IN_MEMORY_SECTION);
                                    continue;
                                }

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
                            timestampRecommendation.addNotification(new RecommendationNotification(recommendationNotification));
                        }

                        // Check if map is not empty and set requests map to current config
                        if (!currentRequestsMap.isEmpty()) {
                            currentConfig.put(AnalyzerConstants.ResourceSetting.requests, currentRequestsMap);
                        }

                        // Check if map is not empty and set limits map to current config
                        if (!currentLimitsMap.isEmpty()) {
                            currentConfig.put(AnalyzerConstants.ResourceSetting.limits, currentLimitsMap);
                        }

                        timestampRecommendation.setCurrentConfig(currentConfig);

                        boolean termLevelRecommendationExist = false;
                        for (RecommendationConstants.RecommendationTerms recommendationTerm : RecommendationConstants.RecommendationTerms.values()) {
                            String term = recommendationTerm.getValue();
                            LOGGER.debug("term = {}", term);
                            double duration = recommendationTerm.getDuration();

                            // TODO: Add check for min data

                            TermRecommendations mappedRecommendationForTerm = new TermRecommendations();

                            ArrayList<RecommendationNotification> termLevelNotifications = new ArrayList<>();

                            // Check if there is min data available for the term
                            if (!RecommendationUtils.checkIfMinDataAvailableForTerm(containerDataKruizeObject, recommendationTerm, monitoringEndTime)) {
                                RecommendationNotification recommendationNotification = new RecommendationNotification(
                                        RecommendationConstants.RecommendationNotification.INFO_NOT_ENOUGH_DATA);
                                mappedRecommendationForTerm.addNotification(recommendationNotification);
                            } else {
                                Timestamp monitoringStartTime = null;
                                for (KruizeRecommendationEngine engine : getEngines()) {
                                    boolean isCostEngine = false;
                                    boolean isPerfEngine = false;

                                    if (engine.getEngineName().equalsIgnoreCase(RecommendationConstants.RecommendationEngine.EngineNames.COST)) {
                                        isCostEngine = true;
                                    }
                                    if (engine.getEngineName().equalsIgnoreCase(RecommendationConstants.RecommendationEngine.EngineNames.PERFORMANCE)) {
                                        isPerfEngine = true;
                                    }

                                    // Get the results
                                    HashMap<Timestamp, IntervalResults> resultsMap = containerDataKruizeObject.getResults();
                                    monitoringStartTime = RecommendationUtils.getMonitoringStartTime(resultsMap,
                                            monitoringEndTime,
                                            duration);
                                    LOGGER.debug("monitoringStartTime = {}", monitoringStartTime);

                                    // Now generate a new recommendation for the new data corresponding to the monitoringEndTime
                                    MappedRecommendationForEngine mappedRecommendationForEngine = engine.generateRecommendation(
                                            monitoringStartTime,
                                            containerDataKruizeObject,
                                            monitoringEndTime,
                                            term,
                                            recommendationSettings,
                                            currentConfig,
                                            Double.valueOf(String.valueOf(duration)));

                                    if (null == mappedRecommendationForEngine) {
                                        continue;
                                    }

                                    // Set term level notification available
                                    termLevelRecommendationExist = true;

                                    // Adding the term level recommendation availability after confirming the recommendation exists
                                    RecommendationNotification rn = RecommendationUtils.getNotificationForTermAvailability(recommendationTerm);
                                    if (null != rn) {
                                        timestampRecommendation.addNotification(rn);
                                    }

                                    RecommendationNotification recommendationNotification = null;
                                    if (isCostEngine) {
                                        // Setting it as at least one recommendation available
                                        recommendationAvailable = true;
                                        recommendationNotification = new RecommendationNotification(
                                                RecommendationConstants.RecommendationNotification.INFO_COST_RECOMMENDATIONS_AVAILABLE
                                        );
                                    }

                                    if (isPerfEngine) {
                                        // Setting it as at least one recommendation available
                                        recommendationAvailable = true;
                                        recommendationNotification = new RecommendationNotification(
                                                RecommendationConstants.RecommendationNotification.INFO_PERFORMANCE_RECOMMENDATIONS_AVAILABLE
                                        );
                                    }

                                    if (null != recommendationNotification) {
                                        termLevelNotifications.add(recommendationNotification);
                                    } else {
                                        recommendationNotification = new RecommendationNotification(
                                                RecommendationConstants.RecommendationNotification.INFO_NOT_ENOUGH_DATA
                                        );
                                        termLevelNotifications.add(recommendationNotification);
                                    }
                                    mappedRecommendationForTerm.setRecommendationForEngineHashMap(engine.getEngineName(), mappedRecommendationForEngine);
                                }

                                for (RecommendationNotification recommendationNotification : termLevelNotifications) {
                                    mappedRecommendationForTerm.addNotification(recommendationNotification);
                                }
                                mappedRecommendationForTerm.setMonitoringStartTime(monitoringStartTime);
                            }
                            setDurationBasedOnTerm(containerDataKruizeObject, mappedRecommendationForTerm, recommendationTerm, monitoringEndTime, measurementDuration);
                            timestampRecommendation.setRecommendationForTermHashMap(term, mappedRecommendationForTerm);
                        }
                        if (!termLevelRecommendationExist) {
                            RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.INFO_NOT_ENOUGH_DATA);
                            timestampRecommendation.addNotification(recommendationNotification);
                        }

                        // put recommendations tagging to timestamp
                        timestampBasedRecommendationMap.put(monitoringEndTime, timestampRecommendation);

                        if (recommendationAvailable) {
                            // set the Recommendations object level notifications
                            RecommendationNotification rn = new RecommendationNotification(RecommendationConstants.RecommendationNotification.INFO_RECOMMENDATIONS_AVAILABLE);
                            recommendationLevelNM.put(rn.getCode(), rn);
                        }

                        containerRecommendations.setNotificationMap(recommendationLevelNM);
                        // set the data object to map
                        containerRecommendations.setData(timestampBasedRecommendationMap);
                        // set the container recommendations in container object
                        containerDataKruizeObject.setContainerRecommendations(containerRecommendations);
                    }
                }
            }
        }
    }

    private void setDurationBasedOnTerm(ContainerData containerDataKruizeObject, TermRecommendations mappedRecommendationForTerm, RecommendationConstants.RecommendationTerms recommendationTerm, Timestamp monitoringEndTime, double measurementDuration) {

        int dataPoints = RecommendationUtils.getTheAvailableDataPoints(containerDataKruizeObject, recommendationTerm, monitoringEndTime);
        double durationSummation = (dataPoints * measurementDuration) / NO_OF_MINUTES_PER_HOUR;
        durationSummation = Double.parseDouble(String.format("%.1f", durationSummation));
        if (durationSummation <= NO_OF_HOURS_PER_DAY) {
            mappedRecommendationForTerm.setDurationInHrs(durationSummation);
        } else if (durationSummation <= NO_OF_HOURS_IN_7_DAYS) {
            if (recommendationTerm.getValue().equalsIgnoreCase(RecommendationConstants.RecommendationTerms.SHORT_TERM.getValue())) {
                mappedRecommendationForTerm.setDurationInHrs(NO_OF_HOURS_PER_DAY);
            } else {
                mappedRecommendationForTerm.setDurationInHrs(durationSummation);
            }
        } else {
            if (recommendationTerm.getValue().equalsIgnoreCase(RecommendationConstants.RecommendationTerms.SHORT_TERM.getValue())) {
                mappedRecommendationForTerm.setDurationInHrs(NO_OF_HOURS_PER_DAY);
            } else if (recommendationTerm.getValue().equalsIgnoreCase(RecommendationConstants.RecommendationTerms.MEDIUM_TERM.getValue())) {
                mappedRecommendationForTerm.setDurationInHrs(NO_OF_HOURS_IN_7_DAYS);
            } else if (recommendationTerm.getValue().equalsIgnoreCase(RecommendationConstants.RecommendationTerms.LONG_TERM.getValue())) {
                mappedRecommendationForTerm.setDurationInHrs(Math.min(NO_OF_HOURS_15_DAYS, durationSummation));
            }
        }
    }
}
