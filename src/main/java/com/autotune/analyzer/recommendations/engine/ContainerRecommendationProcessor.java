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
import com.autotune.common.data.metrics.MetricAggregationInfoResults;
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
public final class ContainerRecommendationProcessor extends BaseRecommendationProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerRecommendationProcessor.class);

    public ContainerRecommendationProcessor(RecommendationEngineService engineService) {
        super(engineService);
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

        String experimentName = engineService.getExperimentName();
        Timestamp intervalEndTime = engineService.getInterval_end_time();

        for (AnalyzerConstants.ResourceSetting resourceSetting : AnalyzerConstants.ResourceSetting.values()) {
            for (AnalyzerConstants.RecommendationItem recommendationItem : AnalyzerConstants.RecommendationItem.values()) {
                RecommendationConfigItem configItem = RecommendationUtils.getCurrentValue(containerData.getResults(),
                        monitoringEndTime, resourceSetting, recommendationItem, notifications);

                // Use base class validation method
                if (!validateConfigItem(configItem, recommendationItem, notifications, LOGGER, experimentName, intervalEndTime)) {
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

            // Extract the datapoints from monitoringStartTime to monitoringEndTime to be used for all recommendation models
            Map<Timestamp, IntervalResults> filteredResultsMap = null;
            if (containerData.getResults() != null) {
                filteredResultsMap = containerData.getResults().entrySet().stream().filter(entry -> (entry.getKey().after(monitoringStartTime) && entry.getKey().before(monitoringEndTime))).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            }

            TermRecommendations mappedRecommendationForTerm = new TermRecommendations();
            if (!Terms.checkIfMinDataAvailableForTerm(filteredResultsMap, terms, measurementDuration)) {
                RecommendationNotification recommendationNotification = new RecommendationNotification(
                        RecommendationConstants.RecommendationNotification.INFO_NOT_ENOUGH_DATA);
                mappedRecommendationForTerm.addNotification(recommendationNotification);
            } else {
                // Determine min, max, avg pod count for a given term
                MetricAggregationInfoResults podCountAggrInfo = getPodCountAggrInfo(filteredResultsMap);
                mappedRecommendationForTerm.addMetricsInfo(KruizeConstants.JSONKeys.POD_COUNT, podCountAggrInfo);

                ArrayList<RecommendationNotification> termLevelNotifications = new ArrayList<>();
                for (RecommendationModel model : engineService.getModels()) {
                    MappedRecommendationForModel mappedRecommendationForModel = generateRecommendationBasedOnModel(model, containerData, filteredResultsMap, kruizeObject, currentConfig, termsEntry);

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

    private MappedRecommendationForModel generateRecommendationBasedOnModel(RecommendationModel model, ContainerData containerData,
                                                                            Map<Timestamp, IntervalResults> filteredResultsMap,
                                                                            KruizeObject kruizeObject,
                                                                            HashMap<AnalyzerConstants.ResourceSetting,
                                                                                    HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> currentConfigMap,
                                                                            Map.Entry<String, Terms> termEntry) {

        MappedRecommendationForModel mappedRecommendationForModel = new MappedRecommendationForModel();
        
        // Extract thresholds using base class helper
        ThresholdValues thresholds = extractThresholds(kruizeObject.getRecommendation_settings());
        double cpuThreshold = thresholds.cpuThreshold;
        double memoryThreshold = thresholds.memoryThreshold;

        // Extract current config using base class helper
        CurrentConfigValues currentConfig = extractCurrentConfig(currentConfigMap);
        RecommendationConfigItem currentCPURequest = currentConfig.cpuRequest;
        RecommendationConfigItem currentCPULimit = currentConfig.cpuLimit;
        RecommendationConfigItem currentMemRequest = currentConfig.memoryRequest;
        RecommendationConfigItem currentMemLimit = currentConfig.memoryLimit;

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

        engineService.populateRecommendation(termEntry, mappedRecommendationForModel, notifications, internalMapToPopulate, cpuThreshold, memoryThreshold, recommendationAcceleratorRequestMap, runtimeRecommList);

        return mappedRecommendationForModel;
    }

    private static MetricAggregationInfoResults getPodCountAggrInfo(Map<Timestamp, IntervalResults> filteredResultsMap) {
        MetricAggregationInfoResults metricAggregationInfoResults = new MetricAggregationInfoResults();
        Double avg = 0.0, min = 0.0, max = 0.0;
        LOGGER.debug("filteredResultsMap: size = {}", filteredResultsMap.size());

        // 1. Use 'podCount' metric data points
        List<MetricAggregationInfoResults> podCountMetrics = filteredResultsMap.values().stream()
                .filter(results -> results.getMetricResultsMap() != null)
                .flatMap(results -> results.getMetricResultsMap().entrySet().stream())
                .filter(metricEntry -> metricEntry.getKey() == AnalyzerConstants.MetricName.podCount)
                .map(metricEntry -> metricEntry.getValue().getAggregationInfoResult())
                .filter(aggInfo -> aggInfo != null && aggInfo.getAvg() != null)
                .toList();
        LOGGER.debug("podCountMetrics : size = {}, content = {}", podCountMetrics.size(), podCountMetrics);
        if (!podCountMetrics.isEmpty()) {
            avg = podCountMetrics.stream()
                    .mapToDouble(MetricAggregationInfoResults::getAvg)
                    .average()
                    .orElse(0.0);
            if (avg > 0.0) {
                min = podCountMetrics.stream()
                        .mapToDouble(MetricAggregationInfoResults::getMin)
                        .min()
                        .orElse(0.0);
                max = podCountMetrics.stream()
                        .mapToDouble(MetricAggregationInfoResults::getMax)
                        .max()
                        .orElse(0.0);
                metricAggregationInfoResults.setAvg(Math.ceil(avg));
                metricAggregationInfoResults.setMin(Math.ceil(min));
                metricAggregationInfoResults.setMax(Math.ceil(max));
                LOGGER.info("Pod Count Aggregation Info: avg = {} min={}, max={}", avg, min, max);
                return metricAggregationInfoResults;
            }
        }

        // 2. Calculate from 'cpuUsage' datapoints using formulae avg of 'sum/avg', min of 'sum/avg', max of 'sum/avg'
        List<MetricAggregationInfoResults> cpuUsageMetrics = filteredResultsMap.values().stream()
                .filter(results -> results.getMetricResultsMap() != null)
                .flatMap(results -> results.getMetricResultsMap().entrySet().stream())
                .filter(metricEntry -> metricEntry.getKey() == AnalyzerConstants.MetricName.cpuUsage)
                .map(metricEntry -> metricEntry.getValue().getAggregationInfoResult())
                .filter(aggInfo -> (aggInfo != null && aggInfo.getAvg() != null && aggInfo.getAvg() != 0.0 && aggInfo.getSum() != null))
                .toList();
        LOGGER.debug("cpuUsageMetrics : size = {}, content = {}", cpuUsageMetrics.size(), cpuUsageMetrics);
        if (!cpuUsageMetrics.isEmpty()) {
            List<Double> calcPodCounts = cpuUsageMetrics.stream()
                    .mapToDouble(aggInfo -> aggInfo.getSum() / aggInfo.getAvg())
                    .boxed()
                    .toList();

            avg = calcPodCounts.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            min = calcPodCounts.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            max = calcPodCounts.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            if (avg > 0.0) {
                metricAggregationInfoResults.setAvg(Math.ceil(avg));
                metricAggregationInfoResults.setMin(Math.ceil(min));
                metricAggregationInfoResults.setMax(Math.ceil(max));
                LOGGER.info("Pod Count Aggregation Info calculated using cpuUsage metric : avg = {} min={}, max={}", avg, min, max);
                return metricAggregationInfoResults;
            }
        }

        // 3. Calculate from 'memoryUsage' datapoints using formulae avg of 'sum/avg', min of 'sum/avg', max of 'sum/avg'
        List<MetricAggregationInfoResults> memoryUsageMetrics = filteredResultsMap.values().stream()
                .filter(results -> results.getMetricResultsMap() != null)
                .flatMap(results -> results.getMetricResultsMap().entrySet().stream())
                .filter(metricEntry -> metricEntry.getKey() == AnalyzerConstants.MetricName.memoryUsage)
                .map(metricEntry -> metricEntry.getValue().getAggregationInfoResult())
                .filter(aggInfo -> (aggInfo != null && aggInfo.getAvg() != null && aggInfo.getAvg() != 0.0 && aggInfo.getSum() != null))
                .toList();
        LOGGER.debug("memoryUsageMetrics : size = {}, content = {}", memoryUsageMetrics.size(), memoryUsageMetrics);
        if (!memoryUsageMetrics.isEmpty()) {
            List<Double> calcPodCounts = memoryUsageMetrics.stream()
                    .mapToDouble(aggInfo -> aggInfo.getSum() / aggInfo.getAvg())
                    .boxed()
                    .toList();

            avg = calcPodCounts.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            min = calcPodCounts.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            max = calcPodCounts.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            if (avg > 0.0) {
                metricAggregationInfoResults.setAvg(Math.ceil(avg));
                metricAggregationInfoResults.setMin(Math.ceil(min));
                metricAggregationInfoResults.setMax(Math.ceil(max));
                LOGGER.info("Pod Count Aggregation Info calculated using memoryUsage metric : avg = {} min={}, max={}", avg, min, max);
                return metricAggregationInfoResults;
            }
        }

        LOGGER.error("Failed to calculate Pod Count Aggregation Info. Size of : podCountMetrics = {}, cpuUsageMetrics = {}, memoryUsageMetrics = {}", podCountMetrics.size(), cpuUsageMetrics.size(), memoryUsageMetrics.size());
        return null;
    }
}
