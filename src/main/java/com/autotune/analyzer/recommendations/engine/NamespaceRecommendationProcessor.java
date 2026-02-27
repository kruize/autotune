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
import com.autotune.analyzer.recommendations.NamespaceRecommendations;
import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.recommendations.RecommendationNotification;
import com.autotune.analyzer.recommendations.model.RecommendationModel;
import com.autotune.analyzer.recommendations.objects.MappedRecommendationForModel;
import com.autotune.analyzer.recommendations.objects.MappedRecommendationForTimestamp;
import com.autotune.analyzer.recommendations.objects.TermRecommendations;
import com.autotune.analyzer.recommendations.term.Terms;
import com.autotune.analyzer.recommendations.utils.RecommendationUtils;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.common.data.result.NamespaceData;
import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.MetricsConfig;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationValueConstants.DEFAULT_CPU_THRESHOLD;
import static com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationValueConstants.DEFAULT_MEMORY_THRESHOLD;

/**
 * Processes namespace-level recommendations for Kubernetes workloads.
 */
public final class NamespaceRecommendationProcessor extends BaseRecommendationProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(NamespaceRecommendationProcessor.class);

    public NamespaceRecommendationProcessor(RecommendationEngineService engineService) {
        super(engineService);
    }

    /**
     * Generates recommendations for a namespace and populates namespaceData.
     */
    public void process(NamespaceData namespaceData, KruizeObject kruizeObject) {
        try {
            NamespaceRecommendations namespaceRecommendations = namespaceData.getNamespaceRecommendations();
            if (null == namespaceRecommendations) {
                namespaceRecommendations = new NamespaceRecommendations();
            }

            Map<Timestamp, IntervalResults> results = namespaceData.getResults();
            Timestamp monitoringEndTime = results.keySet().stream().max(Timestamp::compareTo).get();

            HashMap<Integer, RecommendationNotification> recommendationLevelNM = namespaceRecommendations.getNotificationMap();
            if (null == recommendationLevelNM) {
                recommendationLevelNM = new HashMap<>();
            }

            HashMap<Timestamp, MappedRecommendationForTimestamp> timestampBasedRecommendationMap = namespaceRecommendations.getData();
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
                    getCurrentNamespaceConfigData(namespaceData, monitoringEndTime, timestampRecommendation);
            timestampRecommendation.setCurrentConfig(currentConfig);

            boolean recommendationAvailable = generateNamespaceRecommendationsBasedOnTerms(namespaceData, kruizeObject, monitoringEndTime, currentConfig, timestampRecommendation);

            RecommendationNotification recommendationsLevelNotifications;
            if (recommendationAvailable) {
                timestampBasedRecommendationMap.put(monitoringEndTime, timestampRecommendation);
                recommendationsLevelNotifications = new RecommendationNotification(RecommendationConstants.RecommendationNotification.INFO_RECOMMENDATIONS_AVAILABLE);
            } else {
                recommendationsLevelNotifications = new RecommendationNotification(RecommendationConstants.RecommendationNotification.INFO_NOT_ENOUGH_DATA);
                timestampBasedRecommendationMap = new HashMap<>();
            }

            recommendationLevelNM.put(recommendationsLevelNotifications.getCode(), recommendationsLevelNotifications);
            namespaceRecommendations.setNotificationMap(recommendationLevelNM);
            namespaceRecommendations.setData(timestampBasedRecommendationMap);

            namespaceData.setNamespaceRecommendations(namespaceRecommendations);
        } catch (Exception e) {
            LOGGER.error(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.GENERATE_RECOMMENDATION_FAILURE, kruizeObject.getExperimentName(), e.getMessage());
        }
    }

    private HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> getCurrentNamespaceConfigData(
            NamespaceData namespaceData, Timestamp monitoringEndTime, MappedRecommendationForTimestamp timestampRecommendation) {

        HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> currentNamespaceConfig = new HashMap<>();
        ArrayList<RecommendationConstants.RecommendationNotification> notifications = new ArrayList<>();
        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> currentNamespaceRequestsMap = new HashMap<>();
        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> currentNamespaceLimitsMap = new HashMap<>();

        String experimentName = engineService.getExperimentName();
        Timestamp intervalEndTime = engineService.getInterval_end_time();

        for (AnalyzerConstants.ResourceSetting resourceSetting : AnalyzerConstants.ResourceSetting.values()) {
            for (AnalyzerConstants.RecommendationItem recommendationItem : AnalyzerConstants.RecommendationItem.values()) {
                RecommendationConfigItem configItem = RecommendationUtils.getCurrentValueForNamespace(namespaceData.getResults(), monitoringEndTime, resourceSetting, recommendationItem, notifications);

                // Use base class validation method
                if (!validateConfigItem(configItem, recommendationItem, notifications, LOGGER, experimentName, intervalEndTime)) {
                    continue;
                }

                if (resourceSetting == AnalyzerConstants.ResourceSetting.requests) {
                    currentNamespaceRequestsMap.put(recommendationItem, configItem);
                }
                if (resourceSetting == AnalyzerConstants.ResourceSetting.limits) {
                    currentNamespaceLimitsMap.put(recommendationItem, configItem);
                }
            }
        }

        for (RecommendationConstants.RecommendationNotification recommendationNotification : notifications) {
            timestampRecommendation.addNotification(new RecommendationNotification(recommendationNotification));
        }
        if (!currentNamespaceRequestsMap.isEmpty()) {
            currentNamespaceConfig.put(AnalyzerConstants.ResourceSetting.requests, currentNamespaceRequestsMap);
        }
        if (!currentNamespaceLimitsMap.isEmpty()) {
            currentNamespaceConfig.put(AnalyzerConstants.ResourceSetting.limits, currentNamespaceLimitsMap);
        }
        return currentNamespaceConfig;
    }

    private boolean generateNamespaceRecommendationsBasedOnTerms(NamespaceData namespaceData, KruizeObject kruizeObject,
                                                                Timestamp monitoringEndTime,
                                                                HashMap<AnalyzerConstants.ResourceSetting,
                                                                        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> currentConfig,
                                                                MappedRecommendationForTimestamp timestampRecommendation) {
        boolean namespaceRecommendationAvailable = false;
        double measurementDuration = kruizeObject.getTrial_settings().getMeasurement_durationMinutes_inDouble();

        for (Map.Entry<String, Terms> termsEntry : kruizeObject.getTerms().entrySet()) {
            String recommendationTerm = termsEntry.getKey();
            Terms terms = termsEntry.getValue();
            LOGGER.debug("Namespace Recommendation Term = {}", recommendationTerm);
            int duration = termsEntry.getValue().getDays();
            Timestamp monitoringStartTime = Terms.getMonitoringStartTime(monitoringEndTime, duration);

            TermRecommendations mappedRecommendationForTerm = new TermRecommendations();
            if (!Terms.checkIfMinDataAvailableForTermForNamespace(namespaceData, terms, monitoringEndTime, measurementDuration)) {
                RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.INFO_NOT_ENOUGH_DATA);
                mappedRecommendationForTerm.addNotification(recommendationNotification);
            } else {
                ArrayList<RecommendationNotification> termLevelNotifications = new ArrayList<>();
                for (RecommendationModel model : engineService.getModels()) {
                    MappedRecommendationForModel mappedRecommendationForModel = generateNamespaceRecommendationBasedOnModel(
                            monitoringStartTime, model, namespaceData, monitoringEndTime, kruizeObject.getRecommendation_settings(), currentConfig, termsEntry);

                    if (null == mappedRecommendationForModel)
                        continue;

                    RecommendationNotification rn = RecommendationNotification.getNotificationForTermAvailability(recommendationTerm);
                    if (null != rn) {
                        timestampRecommendation.addNotification(rn);
                    }
                    RecommendationNotification recommendationNotification = null;
                    if (model.getModelName().equalsIgnoreCase(RecommendationConstants.RecommendationEngine.ModelNames.COST)) {
                        namespaceRecommendationAvailable = true;
                        recommendationNotification = new RecommendationNotification(
                                RecommendationConstants.RecommendationNotification.INFO_COST_RECOMMENDATIONS_AVAILABLE);
                    } else if (model.getModelName().equalsIgnoreCase(RecommendationConstants.RecommendationEngine.ModelNames.PERFORMANCE)) {
                        namespaceRecommendationAvailable = true;
                        recommendationNotification = new RecommendationNotification(
                                RecommendationConstants.RecommendationNotification.INFO_PERFORMANCE_RECOMMENDATIONS_AVAILABLE);
                    } else if (null != model.getModelName()) {
                        namespaceRecommendationAvailable = true;
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
                        LOGGER.debug("terms: {}", terms);
                        mappedRecommendationForTerm.setPlots(new PlotManager(namespaceData.getResults(), terms, monitoringStartTime, monitoringEndTime).generatePlots(AnalyzerConstants.ExperimentType.NAMESPACE));
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
            Terms.setDurationBasedOnTermNamespace(namespaceData, mappedRecommendationForTerm, recommendationTerm);
            timestampRecommendation.setRecommendationForTermHashMap(recommendationTerm, mappedRecommendationForTerm);
        }
        return namespaceRecommendationAvailable;
    }

    private MappedRecommendationForModel generateNamespaceRecommendationBasedOnModel(Timestamp monitoringStartTime,
                                                                                    RecommendationModel model,
                                                                                    NamespaceData namespaceData,
                                                                                    Timestamp monitoringEndTime,
                                                                                    RecommendationSettings recommendationSettings,
                                                                                    HashMap<AnalyzerConstants.ResourceSetting,
                                                                                            HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> currentNamespaceConfigMap,
                                                                                    Map.Entry<String, Terms> termEntry) {
        MappedRecommendationForModel mappedRecommendationForModel = new MappedRecommendationForModel();
        
        // Extract thresholds using base class helper
        ThresholdValues thresholds = extractThresholds(recommendationSettings);
        double namespaceCpuThreshold = thresholds.cpuThreshold;
        double namespaceMemoryThreshold = thresholds.memoryThreshold;

        // Extract current config using base class helper
        CurrentConfigValues currentConfig = extractCurrentConfig(currentNamespaceConfigMap);
        RecommendationConfigItem currentNamespaceCPURequest = currentConfig.cpuRequest;
        RecommendationConfigItem currentNamespaceCPULimit = currentConfig.cpuLimit;
        RecommendationConfigItem currentNamespaceMemRequest = currentConfig.memoryRequest;
        RecommendationConfigItem currentNamespaceMemLimit = currentConfig.memoryLimit;

        if (null != monitoringStartTime) {
            Timestamp finalMonitoringStartTime = monitoringStartTime;
            Map<Timestamp, IntervalResults> filteredResultsMap = namespaceData.getResults().entrySet().stream()
                    .filter((x -> ((x.getKey().compareTo(finalMonitoringStartTime) >= 0) && (x.getKey().compareTo(monitoringEndTime) <= 0))))
                    .collect((Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

            int numPodsInNamespace = getNumPodsForNamespace(filteredResultsMap);
            mappedRecommendationForModel.setPodsCount(numPodsInNamespace);

            ArrayList<RecommendationNotification> notifications = new ArrayList<>();
            RecommendationConfigItem namespaceRecommendationCpuRequest = model.getCPURequestRecommendationForNamespace(filteredResultsMap, notifications);
            RecommendationConfigItem namespaceRecommendationMemRequest = model.getMemoryRequestRecommendationForNamespace(filteredResultsMap, notifications);
            RecommendationConfigItem namespaceRecommendationCpuLimits = namespaceRecommendationCpuRequest;
            RecommendationConfigItem namespaceRecommendationMemLimits = namespaceRecommendationMemRequest;

            HashMap<String, RecommendationConfigItem> internalMapToPopulate = new HashMap<>();
            internalMapToPopulate.put(RecommendationConstants.RecommendationEngine.InternalConstants.CURRENT_CPU_REQUEST, currentNamespaceCPURequest);
            internalMapToPopulate.put(RecommendationConstants.RecommendationEngine.InternalConstants.CURRENT_CPU_LIMIT, currentNamespaceCPULimit);
            internalMapToPopulate.put(RecommendationConstants.RecommendationEngine.InternalConstants.CURRENT_MEMORY_REQUEST, currentNamespaceMemRequest);
            internalMapToPopulate.put(RecommendationConstants.RecommendationEngine.InternalConstants.CURRENT_MEMORY_LIMIT, currentNamespaceMemLimit);
            internalMapToPopulate.put(RecommendationConstants.RecommendationEngine.InternalConstants.RECOMMENDED_CPU_REQUEST, namespaceRecommendationCpuRequest);
            internalMapToPopulate.put(RecommendationConstants.RecommendationEngine.InternalConstants.RECOMMENDED_CPU_LIMIT, namespaceRecommendationCpuLimits);
            internalMapToPopulate.put(RecommendationConstants.RecommendationEngine.InternalConstants.RECOMMENDED_MEMORY_REQUEST, namespaceRecommendationMemRequest);
            internalMapToPopulate.put(RecommendationConstants.RecommendationEngine.InternalConstants.RECOMMENDED_MEMORY_LIMIT, namespaceRecommendationMemLimits);

            engineService.populateRecommendation(termEntry, mappedRecommendationForModel, notifications, internalMapToPopulate, numPodsInNamespace, namespaceCpuThreshold, namespaceMemoryThreshold, null, null);
        } else {
            RecommendationNotification notification = new RecommendationNotification(
                    RecommendationConstants.RecommendationNotification.INFO_NOT_ENOUGH_DATA);
            mappedRecommendationForModel.addNotification(notification);
        }
        return mappedRecommendationForModel;
    }

    private static int getNumPodsForNamespace(Map<Timestamp, IntervalResults> filteredResultsMap) {
        LOGGER.debug("Size of Filter Map: {}", filteredResultsMap.size());
        Double max_pods_cpu = filteredResultsMap.values().stream()
                .map(e -> {
                    Optional<MetricResults> numPodsResults = Optional.ofNullable(e.getMetricResultsMap().get(AnalyzerConstants.MetricName.namespaceTotalPods));
                    double numPods = numPodsResults.map(m -> m.getAggregationInfoResult().getAvg()).orElse(0.0);
                    return numPods;
                })
                .max(Double::compareTo).get();
        return (int) Math.ceil(max_pods_cpu);
    }
}
