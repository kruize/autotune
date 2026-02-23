/*******************************************************************************
 * Copyright (c) 2026 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use it except in compliance with the License.
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

package com.autotune.analyzer.recommendations.engine;

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.kruizeObject.RecommendationSettings;
import com.autotune.analyzer.plots.PlotManager;
import com.autotune.analyzer.recommendations.*;
import com.autotune.analyzer.recommendations.model.RecommendationModel;
import com.autotune.analyzer.recommendations.objects.MappedRecommendationForModel;
import com.autotune.analyzer.recommendations.objects.MappedRecommendationForTimestamp;
import com.autotune.analyzer.recommendations.objects.TermRecommendations;
import com.autotune.analyzer.recommendations.term.Terms;
import com.autotune.analyzer.recommendations.utils.RecommendationUtils;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.MetricsConfig;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import static com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationValueConstants.DEFAULT_CPU_THRESHOLD;
import static com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationValueConstants.DEFAULT_MEMORY_THRESHOLD;

/**
 * Processes container-level recommendations.
 */
public final class ContainerRecommendationProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerRecommendationProcessor.class);

    private final RecommendationEngine engine;

    public ContainerRecommendationProcessor(RecommendationEngine engine) {
        this.engine = engine;
    }

    /**
     * Generates recommendations for a container and populates containerData.
     */
    public void process(ContainerData containerData, KruizeObject kruizeObject) {
        ContainerRecommendations containerRecommendations = containerData.getContainerRecommendations();
        if (null == containerRecommendations) {
            containerRecommendations = new ContainerRecommendations();
        }

        Map<Timestamp, IntervalResults> results = containerData.getResults();
        Timestamp monitoringEndTime = results.keySet().stream().max(Timestamp::compareTo).get();

        HashMap<Integer, RecommendationNotification> recommendationLevelNM = containerRecommendations.getNotificationMap();
        if (null == recommendationLevelNM) {
            recommendationLevelNM = new HashMap<>();
        }

        HashMap<Timestamp, MappedRecommendationForTimestamp> timestampBasedRecommendationMap = containerRecommendations.getData();
        if (null == timestampBasedRecommendationMap) {
            timestampBasedRecommendationMap = new HashMap<>();
        }

        MappedRecommendationForTimestamp timestampRecommendation;
        if (timestampBasedRecommendationMap.containsKey(monitoringEndTime)) {
            timestampRecommendation = timestampBasedRecommendationMap.get(monitoringEndTime);
        } else {
            timestampRecommendation = new MappedRecommendationForTimestamp();
        }

        timestampRecommendation.setMonitoringEndTime(monitoringEndTime);

        HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> currentConfig =
                getCurrentConfigData(containerData, monitoringEndTime, timestampRecommendation);
        timestampRecommendation.setCurrentConfig(currentConfig);

        boolean recommendationAvailable = generateRecommendationsBasedOnTerms(containerData, kruizeObject, monitoringEndTime, currentConfig, timestampRecommendation);

        RecommendationNotification recommendationsLevelNotifications;
        if (recommendationAvailable) {
            timestampBasedRecommendationMap.put(monitoringEndTime, timestampRecommendation);
            recommendationsLevelNotifications = new RecommendationNotification(RecommendationConstants.RecommendationNotification.INFO_RECOMMENDATIONS_AVAILABLE);
        } else {
            recommendationsLevelNotifications = new RecommendationNotification(RecommendationConstants.RecommendationNotification.INFO_NOT_ENOUGH_DATA);
            timestampBasedRecommendationMap = new HashMap<>();
        }

        recommendationLevelNM.put(recommendationsLevelNotifications.getCode(), recommendationsLevelNotifications);
        containerRecommendations.setNotificationMap(recommendationLevelNM);
        containerRecommendations.setData(timestampBasedRecommendationMap);

        containerData.setContainerRecommendations(containerRecommendations);
    }

    private HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> getCurrentConfigData(
            ContainerData containerData, Timestamp monitoringEndTime, MappedRecommendationForTimestamp timestampRecommendation) {

        HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> currentConfig = new HashMap<>();
        ArrayList<RecommendationConstants.RecommendationNotification> notifications = new ArrayList<>();
        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> currentRequestsMap = new HashMap<>();
        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> currentLimitsMap = new HashMap<>();

        String experimentName = engine.getExperimentName();
        Timestamp intervalEndTime = engine.getInterval_end_time();

        for (AnalyzerConstants.ResourceSetting resourceSetting : AnalyzerConstants.ResourceSetting.values()) {
            for (AnalyzerConstants.RecommendationItem recommendationItem : AnalyzerConstants.RecommendationItem.values()) {
                RecommendationConfigItem configItem = RecommendationUtils.getCurrentValue(containerData.getResults(),
                        monitoringEndTime, resourceSetting, recommendationItem, notifications);

                if (null == configItem) continue;
                if (null == configItem.getAmount()) {
                    if (recommendationItem.equals(AnalyzerConstants.RecommendationItem.CPU)) {
                        notifications.add(RecommendationConstants.RecommendationNotification.ERROR_AMOUNT_MISSING_IN_CPU_SECTION);
                        LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.AMOUNT_MISSING_IN_CPU_SECTION
                                .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME, experimentName, intervalEndTime)));
                    } else if (recommendationItem.equals(AnalyzerConstants.RecommendationItem.MEMORY)) {
                        notifications.add(RecommendationConstants.RecommendationNotification.ERROR_AMOUNT_MISSING_IN_MEMORY_SECTION);
                        LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.AMOUNT_MISSING_IN_MEMORY_SECTION
                                .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME, experimentName, intervalEndTime)));
                    }
                    continue;
                }
                if (null == configItem.getFormat()) {
                    if (recommendationItem.equals(AnalyzerConstants.RecommendationItem.CPU)) {
                        notifications.add(RecommendationConstants.RecommendationNotification.ERROR_FORMAT_MISSING_IN_CPU_SECTION);
                        LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.FORMAT_MISSING_IN_CPU_SECTION
                                .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME, experimentName, intervalEndTime)));
                    } else if (recommendationItem.equals(AnalyzerConstants.RecommendationItem.MEMORY)) {
                        notifications.add(RecommendationConstants.RecommendationNotification.ERROR_FORMAT_MISSING_IN_MEMORY_SECTION);
                        LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.FORMAT_MISSING_IN_MEMORY_SECTION
                                .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME, experimentName, intervalEndTime)));
                    }
                    continue;
                }
                if (configItem.getAmount() <= 0.0) {
                    if (recommendationItem.equals(AnalyzerConstants.RecommendationItem.CPU)) {
                        notifications.add(RecommendationConstants.RecommendationNotification.ERROR_INVALID_AMOUNT_IN_CPU_SECTION);
                        LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_AMOUNT_IN_CPU_SECTION
                                .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME, experimentName, intervalEndTime)));
                    } else if (recommendationItem.equals(AnalyzerConstants.RecommendationItem.MEMORY)) {
                        notifications.add(RecommendationConstants.RecommendationNotification.ERROR_INVALID_AMOUNT_IN_MEMORY_SECTION);
                        LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_AMOUNT_IN_MEMORY_SECTION
                                .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME, experimentName, intervalEndTime)));
                    }
                    continue;
                }
                if (configItem.getFormat().isEmpty() || configItem.getFormat().isBlank()) {
                    if (recommendationItem.equals(AnalyzerConstants.RecommendationItem.CPU)) {
                        notifications.add(RecommendationConstants.RecommendationNotification.ERROR_INVALID_FORMAT_IN_CPU_SECTION);
                        LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_FORMAT_IN_CPU_SECTION
                                .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME, experimentName, intervalEndTime)));
                    } else if (recommendationItem.equals(AnalyzerConstants.RecommendationItem.MEMORY)) {
                        notifications.add(RecommendationConstants.RecommendationNotification.ERROR_INVALID_FORMAT_IN_MEMORY_SECTION);
                        LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_FORMAT_IN_MEMORY_SECTION
                                .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME, experimentName, intervalEndTime)));
                    }
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

        for (RecommendationConstants.RecommendationNotification recommendationNotification : notifications) {
            timestampRecommendation.addNotification(new RecommendationNotification(recommendationNotification));
        }
        if (!currentRequestsMap.isEmpty()) {
            currentConfig.put(AnalyzerConstants.ResourceSetting.requests, currentRequestsMap);
        }
        if (!currentLimitsMap.isEmpty()) {
            currentConfig.put(AnalyzerConstants.ResourceSetting.limits, currentLimitsMap);
        }
        return currentConfig;
    }

    private boolean generateRecommendationsBasedOnTerms(ContainerData containerData, KruizeObject kruizeObject,
                                                       Timestamp monitoringEndTime,
                                                       HashMap<AnalyzerConstants.ResourceSetting,
                                                               HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> currentConfig,
                                                       MappedRecommendationForTimestamp timestampRecommendation) {
        boolean recommendationAvailable = false;
        double measurementDuration = kruizeObject.getTrial_settings().getMeasurement_durationMinutes_inDouble();

        for (Map.Entry<String, Terms> termsEntry : kruizeObject.getTerms().entrySet()) {
            String recommendationTerm = termsEntry.getKey();
            Terms terms = termsEntry.getValue();
            LOGGER.debug(String.format(KruizeConstants.APIMessages.RECOMMENDATION_TERM, recommendationTerm));
            int duration = termsEntry.getValue().getDays();
            Timestamp monitoringStartTime = Terms.getMonitoringStartTime(monitoringEndTime, duration);
            LOGGER.debug(String.format(KruizeConstants.APIMessages.MONITORING_START_TIME, monitoringStartTime));

            TermRecommendations mappedRecommendationForTerm = new TermRecommendations();
            if (!Terms.checkIfMinDataAvailableForTerm(containerData, terms, monitoringEndTime, measurementDuration)) {
                RecommendationNotification recommendationNotification = new RecommendationNotification(
                        RecommendationConstants.RecommendationNotification.INFO_NOT_ENOUGH_DATA);
                mappedRecommendationForTerm.addNotification(recommendationNotification);
            } else {
                ArrayList<RecommendationNotification> termLevelNotifications = new ArrayList<>();
                for (RecommendationModel model : engine.getModels()) {
                    MappedRecommendationForModel mappedRecommendationForModel = generateRecommendationBasedOnModel(
                            monitoringStartTime, model, containerData, monitoringEndTime, kruizeObject, currentConfig, termsEntry);

                    if (null == mappedRecommendationForModel) continue;

                    RecommendationNotification rn = RecommendationNotification.getNotificationForTermAvailability(recommendationTerm);
                    if (null != rn) {
                        timestampRecommendation.addNotification(rn);
                    }
                    RecommendationNotification recommendationNotification = null;
                    if (model.getModelName().equalsIgnoreCase(RecommendationConstants.RecommendationEngine.ModelNames.COST)) {
                        recommendationAvailable = true;
                        recommendationNotification = new RecommendationNotification(
                                RecommendationConstants.RecommendationNotification.INFO_COST_RECOMMENDATIONS_AVAILABLE);
                    } else if (model.getModelName().equalsIgnoreCase(RecommendationConstants.RecommendationEngine.ModelNames.PERFORMANCE)) {
                        recommendationAvailable = true;
                        recommendationNotification = new RecommendationNotification(
                                RecommendationConstants.RecommendationNotification.INFO_PERFORMANCE_RECOMMENDATIONS_AVAILABLE);
                    } else if (null != model.getModelName()) {
                        recommendationAvailable = true;
                        recommendationNotification = new RecommendationNotification(
                                RecommendationConstants.RecommendationNotification.INFO_MODEL_RECOMMENDATIONS_AVAILABLE);
                    }
                    if (null != recommendationNotification) {
                        termLevelNotifications.add(recommendationNotification);
                    } else {
                        recommendationNotification = new RecommendationNotification(
                                RecommendationConstants.RecommendationNotification.INFO_NOT_ENOUGH_DATA);
                        termLevelNotifications.add(recommendationNotification);
                    }
                    mappedRecommendationForTerm.setRecommendationForEngineHashMap(model.getModelName(), mappedRecommendationForModel);
                }

                for (RecommendationNotification recommendationNotification : termLevelNotifications) {
                    mappedRecommendationForTerm.addNotification(recommendationNotification);
                }
                mappedRecommendationForTerm.setMonitoringStartTime(monitoringStartTime);

                if (KruizeDeploymentInfo.plots && null != monitoringStartTime) {
                    Timer.Sample timerBoxPlots = null;
                    String status = KruizeConstants.APIMessages.SUCCESS;
                    try {
                        timerBoxPlots = Timer.start(MetricsConfig.meterRegistry());
                        mappedRecommendationForTerm.setPlots(new PlotManager(containerData.getResults(), terms, monitoringStartTime, monitoringEndTime).generatePlots(AnalyzerConstants.ExperimentType.CONTAINER));
                    } catch (Exception e) {
                        status = String.format(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.BOX_PLOTS_FAILURE, e.getMessage());
                        LOGGER.debug(status);
                    } finally {
                        if (timerBoxPlots != null) {
                            MetricsConfig.timerBoxPlots = MetricsConfig.timerBBoxPlots.tag(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.STATUS, status).register(MetricsConfig.meterRegistry());
                            timerBoxPlots.stop(MetricsConfig.timerBoxPlots);
                        }
                    }
                }
            }
            Terms.setDurationBasedOnTerm(containerData, mappedRecommendationForTerm, recommendationTerm);
            timestampRecommendation.setRecommendationForTermHashMap(recommendationTerm, mappedRecommendationForTerm);
        }
        return recommendationAvailable;
    }

    private MappedRecommendationForModel generateRecommendationBasedOnModel(Timestamp monitoringStartTime, RecommendationModel model, ContainerData containerData,
                                                                            Timestamp monitoringEndTime, KruizeObject kruizeObject,
                                                                            HashMap<AnalyzerConstants.ResourceSetting,
                                                                                    HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> currentConfigMap,
                                                                            Map.Entry<String, Terms> termEntry) {

        MappedRecommendationForModel mappedRecommendationForModel = new MappedRecommendationForModel();
        double cpuThreshold = DEFAULT_CPU_THRESHOLD;
        double memoryThreshold = DEFAULT_MEMORY_THRESHOLD;
        RecommendationSettings recommendationSettings = kruizeObject.getRecommendation_settings();
        if (null != recommendationSettings) {
            Double threshold = recommendationSettings.getThreshold();
            if (null == threshold) {
                LOGGER.info(String.format(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.THRESHOLD_NOT_SET, DEFAULT_CPU_THRESHOLD, DEFAULT_MEMORY_THRESHOLD));
            } else if (threshold <= 0.0) {
                LOGGER.error(String.format(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.INVALID_THRESHOLD, DEFAULT_CPU_THRESHOLD, DEFAULT_MEMORY_THRESHOLD));
            } else {
                cpuThreshold = threshold;
                memoryThreshold = threshold;
            }
        } else {
            LOGGER.error(String.format(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.NULL_RECOMMENDATION_SETTINGS, DEFAULT_CPU_THRESHOLD, DEFAULT_MEMORY_THRESHOLD));
        }

        RecommendationConfigItem currentCPURequest = null;
        RecommendationConfigItem currentCPULimit = null;
        RecommendationConfigItem currentMemRequest = null;
        RecommendationConfigItem currentMemLimit = null;

        if (currentConfigMap.containsKey(AnalyzerConstants.ResourceSetting.requests) && null != currentConfigMap.get(AnalyzerConstants.ResourceSetting.requests)) {
            HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requestsMap = currentConfigMap.get(AnalyzerConstants.ResourceSetting.requests);
            if (requestsMap.containsKey(AnalyzerConstants.RecommendationItem.CPU) && null != requestsMap.get(AnalyzerConstants.RecommendationItem.CPU)) {
                currentCPURequest = requestsMap.get(AnalyzerConstants.RecommendationItem.CPU);
            }
            if (requestsMap.containsKey(AnalyzerConstants.RecommendationItem.MEMORY) && null != requestsMap.get(AnalyzerConstants.RecommendationItem.MEMORY)) {
                currentMemRequest = requestsMap.get(AnalyzerConstants.RecommendationItem.MEMORY);
            }
        }
        if (currentConfigMap.containsKey(AnalyzerConstants.ResourceSetting.limits) && null != currentConfigMap.get(AnalyzerConstants.ResourceSetting.limits)) {
            HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> limitsMap = currentConfigMap.get(AnalyzerConstants.ResourceSetting.limits);
            if (limitsMap.containsKey(AnalyzerConstants.RecommendationItem.CPU) && null != limitsMap.get(AnalyzerConstants.RecommendationItem.CPU)) {
                currentCPULimit = limitsMap.get(AnalyzerConstants.RecommendationItem.CPU);
            }
            if (limitsMap.containsKey(AnalyzerConstants.RecommendationItem.MEMORY) && null != limitsMap.get(AnalyzerConstants.RecommendationItem.MEMORY)) {
                currentMemLimit = limitsMap.get(AnalyzerConstants.RecommendationItem.MEMORY);
            }
        }

        if (null != monitoringStartTime) {
            Timestamp finalMonitoringStartTime = monitoringStartTime;
            Map<Timestamp, IntervalResults> filteredResultsMap = containerData.getResults().entrySet().stream()
                    .filter((x -> ((x.getKey().compareTo(finalMonitoringStartTime) >= 0) && (x.getKey().compareTo(monitoringEndTime) <= 0))))
                    .collect((Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

            int numPods = getNumPods(filteredResultsMap);
            mappedRecommendationForModel.setPodsCount(numPods);

            ArrayList<RecommendationNotification> notifications = new ArrayList<>();
            RecommendationConfigItem recommendationCpuRequest = model.getCPURequestRecommendation(filteredResultsMap, notifications);
            RecommendationConfigItem recommendationMemRequest = model.getMemoryRequestRecommendation(filteredResultsMap, notifications);
            Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> recommendationAcceleratorRequestMap = model.getAcceleratorRequestRecommendation(filteredResultsMap, notifications);

            RecommendationConfigItem recommendationCpuLimits = recommendationCpuRequest;
            RecommendationConfigItem recommendationMemLimits = recommendationMemRequest;

            HashMap<String, RecommendationConfigItem> internalMapToPopulate = new HashMap<>();
            internalMapToPopulate.put(RecommendationConstants.RecommendationEngine.InternalConstants.CURRENT_CPU_REQUEST, currentCPURequest);
            internalMapToPopulate.put(RecommendationConstants.RecommendationEngine.InternalConstants.CURRENT_CPU_LIMIT, currentCPULimit);
            internalMapToPopulate.put(RecommendationConstants.RecommendationEngine.InternalConstants.CURRENT_MEMORY_REQUEST, currentMemRequest);
            internalMapToPopulate.put(RecommendationConstants.RecommendationEngine.InternalConstants.CURRENT_MEMORY_LIMIT, currentMemLimit);
            internalMapToPopulate.put(RecommendationConstants.RecommendationEngine.InternalConstants.RECOMMENDED_CPU_REQUEST, recommendationCpuRequest);
            internalMapToPopulate.put(RecommendationConstants.RecommendationEngine.InternalConstants.RECOMMENDED_CPU_LIMIT, recommendationCpuLimits);
            internalMapToPopulate.put(RecommendationConstants.RecommendationEngine.InternalConstants.RECOMMENDED_MEMORY_REQUEST, recommendationMemRequest);
            internalMapToPopulate.put(RecommendationConstants.RecommendationEngine.InternalConstants.RECOMMENDED_MEMORY_LIMIT, recommendationMemLimits);
            List<RecommendationConfigEnv> runtimeRecommList = null;

            try {
                if (RuntimeRecommendationProcessor.isRuntimeLayerPresent(containerData.getLayerMap())) {
                    runtimeRecommList = RuntimeRecommendationProcessor.handleRuntimeRecommendations(kruizeObject, containerData, model, filteredResultsMap, notifications, recommendationCpuRequest, recommendationMemRequest, recommendationCpuLimits, recommendationMemLimits);
                }
            } catch (Exception e) {
                LOGGER.error("Exception occurred while preparing runtime recommendations: {}", e.getMessage());
            }

            engine.populateRecommendation(termEntry, mappedRecommendationForModel, notifications, internalMapToPopulate, numPods, cpuThreshold, memoryThreshold, recommendationAcceleratorRequestMap, runtimeRecommList);
        } else {
            RecommendationNotification notification = new RecommendationNotification(
                    RecommendationConstants.RecommendationNotification.INFO_NOT_ENOUGH_DATA);
            mappedRecommendationForModel.addNotification(notification);
        }
        return mappedRecommendationForModel;
    }

    private static int getNumPods(Map<Timestamp, IntervalResults> filteredResultsMap) {
        Double max_pods_cpu = filteredResultsMap.values().stream()
                .map(e -> {
                    Optional<MetricResults> cpuUsageResults = Optional.ofNullable(e.getMetricResultsMap().get(AnalyzerConstants.MetricName.cpuUsage));
                    double cpuUsageSum = cpuUsageResults.map(m -> m.getAggregationInfoResult().getSum()).orElse(0.0);
                    double cpuUsageAvg = cpuUsageResults.map(m -> m.getAggregationInfoResult().getAvg()).orElse(0.0);
                    double numPods = 0;
                    if (0 != cpuUsageAvg) {
                        numPods = (int) Math.ceil(cpuUsageSum / cpuUsageAvg);
                    }
                    return numPods;
                })
                .max(Double::compareTo).get();
        return (int) Math.ceil(max_pods_cpu);
    }
}
