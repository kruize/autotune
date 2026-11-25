/*******************************************************************************
 * Copyright (c) 2026 Red Hat, IBM Corporation and others.
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

package com.autotune.analyzer.recommendations.engine;

import com.autotune.analyzer.kruizeObject.RecommendationSettings;
import com.autotune.analyzer.kruizeObject.ResourceScopeSettings;
import com.autotune.analyzer.recommendations.Config;
import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationScope;
import com.autotune.analyzer.recommendations.model.RecommendationModel;
import com.autotune.analyzer.recommendations.objects.MappedRecommendationForModel;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationValueConstants.DEFAULT_CPU_THRESHOLD;
import static com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationValueConstants.DEFAULT_MEMORY_THRESHOLD;

/**
 * Base class for recommendation processors that provides shared functionality
 * for threshold handling, current config extraction, and common validation logic.
 */
public abstract class BaseRecommendationProcessor {

    protected final RecommendationEngineService engineService;
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseRecommendationProcessor.class);

    protected BaseRecommendationProcessor(RecommendationEngineService engineService) {
        this.engineService = engineService;
    }

    /**
     * Extracts and validates threshold values from recommendation settings.
     *
     * @param recommendationSettings The recommendation settings containing threshold configuration
     * @return A ThresholdValues object containing CPU and memory thresholds
     */
    protected ThresholdValues extractThresholds(RecommendationSettings recommendationSettings) {
        double cpuThreshold = DEFAULT_CPU_THRESHOLD;
        double memoryThreshold = DEFAULT_MEMORY_THRESHOLD;

        if (null != recommendationSettings) {
            Double threshold = recommendationSettings.getThreshold();
            if (null == threshold) {
                LOGGER.info("{}", String.format(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.THRESHOLD_NOT_SET,
                        DEFAULT_CPU_THRESHOLD, DEFAULT_MEMORY_THRESHOLD));
            } else if (threshold <= 0.0) {
                LOGGER.error("{}", String.format(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.INVALID_THRESHOLD,
                        DEFAULT_CPU_THRESHOLD, DEFAULT_MEMORY_THRESHOLD));
            } else {
                cpuThreshold = threshold;
                memoryThreshold = threshold;
            }
        } else {
            LOGGER.error("{}", String.format(AnalyzerErrorConstants.APIErrors.UpdateRecommendationsAPI.NULL_RECOMMENDATION_SETTINGS,
                    DEFAULT_CPU_THRESHOLD, DEFAULT_MEMORY_THRESHOLD));
        }

        return new ThresholdValues(cpuThreshold, memoryThreshold);
    }

    /**
     * Validates a configuration item and adds appropriate notifications if validation fails.
     *
     * @param configItem         The configuration item to validate
     * @param recommendationItem The type of recommendation item (CPU or MEMORY)
     * @param notifications      List to add validation failure notifications to
     * @param logger            Logger instance for logging errors
     * @param experimentName    Experiment name for error messages
     * @param intervalEndTime   Interval end time for error messages
     * @return true if validation passes, false otherwise
     */
    protected boolean validateConfigItem(RecommendationConfigItem configItem,
                                        AnalyzerConstants.RecommendationItem recommendationItem,
                                        ArrayList<RecommendationConstants.RecommendationNotification> notifications,
                                        Logger logger,
                                        String experimentName,
                                        Timestamp intervalEndTime) {
        if (null == configItem) {
            return false;
        }

        String itemType = recommendationItem.equals(AnalyzerConstants.RecommendationItem.CPU) ? "CPU" : "MEMORY";

        if (null == configItem.getAmount()) {
            if (recommendationItem.equals(AnalyzerConstants.RecommendationItem.CPU)) {
                notifications.add(RecommendationConstants.RecommendationNotification.ERROR_AMOUNT_MISSING_IN_CPU_SECTION);
                logger.error(RecommendationConstants.RecommendationNotificationMsgConstant.AMOUNT_MISSING_IN_CPU_SECTION
                        .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME,
                                experimentName, intervalEndTime)));
            } else {
                notifications.add(RecommendationConstants.RecommendationNotification.ERROR_AMOUNT_MISSING_IN_MEMORY_SECTION);
                logger.error(RecommendationConstants.RecommendationNotificationMsgConstant.AMOUNT_MISSING_IN_MEMORY_SECTION
                        .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME,
                                experimentName, intervalEndTime)));
            }
            return false;
        }

        if (null == configItem.getFormat()) {
            if (recommendationItem.equals(AnalyzerConstants.RecommendationItem.CPU)) {
                notifications.add(RecommendationConstants.RecommendationNotification.ERROR_FORMAT_MISSING_IN_CPU_SECTION);
                logger.error(RecommendationConstants.RecommendationNotificationMsgConstant.FORMAT_MISSING_IN_CPU_SECTION
                        .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME,
                                experimentName, intervalEndTime)));
            } else {
                notifications.add(RecommendationConstants.RecommendationNotification.ERROR_FORMAT_MISSING_IN_MEMORY_SECTION);
                logger.error(RecommendationConstants.RecommendationNotificationMsgConstant.FORMAT_MISSING_IN_MEMORY_SECTION
                        .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME,
                                experimentName, intervalEndTime)));
            }
            return false;
        }

        if (configItem.getAmount() <= 0.0) {
            if (recommendationItem.equals(AnalyzerConstants.RecommendationItem.CPU)) {
                notifications.add(RecommendationConstants.RecommendationNotification.ERROR_INVALID_AMOUNT_IN_CPU_SECTION);
                logger.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_AMOUNT_IN_CPU_SECTION
                        .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME,
                                experimentName, intervalEndTime)));
            } else {
                notifications.add(RecommendationConstants.RecommendationNotification.ERROR_INVALID_AMOUNT_IN_MEMORY_SECTION);
                logger.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_AMOUNT_IN_MEMORY_SECTION
                        .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME,
                                experimentName, intervalEndTime)));
            }
            return false;
        }

        if (configItem.getFormat().isEmpty() || configItem.getFormat().isBlank()) {
            if (recommendationItem.equals(AnalyzerConstants.RecommendationItem.CPU)) {
                notifications.add(RecommendationConstants.RecommendationNotification.ERROR_INVALID_FORMAT_IN_CPU_SECTION);
                logger.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_FORMAT_IN_CPU_SECTION
                        .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME,
                                experimentName, intervalEndTime)));
            } else {
                notifications.add(RecommendationConstants.RecommendationNotification.ERROR_INVALID_FORMAT_IN_MEMORY_SECTION);
                logger.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_FORMAT_IN_MEMORY_SECTION
                        .concat(String.format(AnalyzerErrorConstants.AutotuneObjectErrors.EXPERIMENT_AND_INTERVAL_END_TIME,
                                experimentName, intervalEndTime)));
            }
            return false;
        }

        return true;
    }

    /**
     * Extracts current configuration items from requests and limits maps.
     *
     * @param currentConfigMap Map containing current configuration
     * @return CurrentConfigValues object containing CPU and memory request/limit values
     */
    protected CurrentConfigValues extractCurrentConfig(Config currentConfig) {

        RecommendationConfigItem currentCPURequest = null;
        RecommendationConfigItem currentCPULimit = null;
        RecommendationConfigItem currentMemRequest = null;
        RecommendationConfigItem currentMemLimit = null;

        Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requests = currentConfig.getRequests();
        Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> limits = currentConfig.getLimits();
        if (null != requests) {
            currentCPURequest = requests.get(AnalyzerConstants.RecommendationItem.CPU);
            currentMemRequest = requests.get(AnalyzerConstants.RecommendationItem.MEMORY);
        }
        if (null != limits) {
            currentCPULimit = limits.get(AnalyzerConstants.RecommendationItem.CPU);
            currentMemLimit = limits.get(AnalyzerConstants.RecommendationItem.MEMORY);
        }

        return new CurrentConfigValues(currentCPURequest, currentCPULimit, currentMemRequest, currentMemLimit);
    }

    /**
     * Classifies a resource into its elementary scope state from its requests variation.
     * Precondition: variation.amount has already been threshold-snapped by RecommendationEngine,
     * so 0.0 means within threshold.
     *
     * @param variationItem the requests variation of the resource, may be null
     * @param isIdle        true if the resource is idle (CPU only)
     */
    protected RecommendationScope classifyResource(RecommendationConfigItem variationItem, boolean isIdle) {
        if (isIdle) return RecommendationScope.IDLE;
        if (null == variationItem || null == variationItem.getAmount()) return RecommendationScope.OPTIMISED;
        double amount = variationItem.getAmount();
        if (amount > 0.0) return RecommendationScope.UNDER_PROVISIONED;
        if (amount < 0.0) return RecommendationScope.OVER_PROVISIONED;
        return RecommendationScope.OPTIMISED;
    }

    /**
     * Resolves the effective scopes: the user-provided resource_scope_settings if present, else the model default.
     * Call once per model, outside the per-recommendation work.
     * <p>
     * The user-provided lists are re-parsed into a fresh object rather than trusting the parsed sets on the stored
     * one, because Gson bypasses setters and an experiment reloaded from the DB has unparsed (null) sets.
     */
    protected ResourceScopeSettings resolveEffectiveScopes(RecommendationSettings settings, RecommendationModel model) {
        if (null != settings && null != settings.getResourceScopeSettings()) {
            ResourceScopeSettings userScopes = settings.getResourceScopeSettings();
            ResourceScopeSettings effective = new ResourceScopeSettings(userScopes.getCpu(), userScopes.getMemory());
            effective.parse();
            return effective;
        }
        return model.getDefaultResourceScopeSettings();
    }

    /**
     * Returns true if the recommendation satisfies the effective scopes.
     * Per-resource AND logic: every constrained resource must match. A null set means the key was absent,
     * i.e. unconstrained, and always passes.
     * <p>
     * The idle state is read from the model's own notifications (NOTICE_CPU_RECORDS_ARE_IDLE), where the engine
     * adds it; it is not present in the term level notifications.
     */
    protected boolean isRecommendationInScope(MappedRecommendationForModel rec, ResourceScopeSettings effectiveScopes) {
        EnumSet<RecommendationScope> cpuScopes = effectiveScopes.getParsedCpuScopes();
        EnumSet<RecommendationScope> memScopes = effectiveScopes.getParsedMemoryScopes();

        boolean cpuAll = null == cpuScopes || cpuScopes.contains(RecommendationScope.ALL);
        boolean memAll = null == memScopes || memScopes.contains(RecommendationScope.ALL);
        if (cpuAll && memAll) return true; // fast path, nothing to filter

        boolean isCpuIdle = null != rec.getNotificationHashMap()
                && rec.getNotificationHashMap().containsKey(RecommendationConstants.NotificationCodes.NOTICE_CPU_RECORDS_ARE_IDLE);

        RecommendationScope cpuState = RecommendationScope.OPTIMISED;
        RecommendationScope memState = RecommendationScope.OPTIMISED;
        if (null != rec.getVariation() && null != rec.getVariation().getRequests()) {
            cpuState = classifyResource(rec.getVariation().getRequests().get(AnalyzerConstants.RecommendationItem.CPU), isCpuIdle);
            memState = classifyResource(rec.getVariation().getRequests().get(AnalyzerConstants.RecommendationItem.MEMORY), false);
        } else if (isCpuIdle) {
            cpuState = RecommendationScope.IDLE;
        }

        return (cpuAll || cpuScopes.contains(cpuState)) && (memAll || memScopes.contains(memState));
    }

    /**
     * Helper class to hold threshold values.
     */
    protected static class ThresholdValues {
        public final double cpuThreshold;
        public final double memoryThreshold;

        public ThresholdValues(double cpuThreshold, double memoryThreshold) {
            this.cpuThreshold = cpuThreshold;
            this.memoryThreshold = memoryThreshold;
        }
    }

    /**
     * Helper class to hold current configuration values.
     */
    protected static class CurrentConfigValues {
        public final RecommendationConfigItem cpuRequest;
        public final RecommendationConfigItem cpuLimit;
        public final RecommendationConfigItem memoryRequest;
        public final RecommendationConfigItem memoryLimit;

        public CurrentConfigValues(RecommendationConfigItem cpuRequest,
                                  RecommendationConfigItem cpuLimit,
                                  RecommendationConfigItem memoryRequest,
                                  RecommendationConfigItem memoryLimit) {
            this.cpuRequest = cpuRequest;
            this.cpuLimit = cpuLimit;
            this.memoryRequest = memoryRequest;
            this.memoryLimit = memoryLimit;
        }
    }
}
