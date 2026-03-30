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
import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;

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
    protected CurrentConfigValues extractCurrentConfig(
            HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> currentConfigMap) {

        RecommendationConfigItem currentCPURequest = null;
        RecommendationConfigItem currentCPULimit = null;
        RecommendationConfigItem currentMemRequest = null;
        RecommendationConfigItem currentMemLimit = null;

        if (currentConfigMap.containsKey(AnalyzerConstants.ResourceSetting.requests) &&
                null != currentConfigMap.get(AnalyzerConstants.ResourceSetting.requests)) {
            HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requestsMap =
                    currentConfigMap.get(AnalyzerConstants.ResourceSetting.requests);
            if (requestsMap.containsKey(AnalyzerConstants.RecommendationItem.CPU) &&
                    null != requestsMap.get(AnalyzerConstants.RecommendationItem.CPU)) {
                currentCPURequest = requestsMap.get(AnalyzerConstants.RecommendationItem.CPU);
            }
            if (requestsMap.containsKey(AnalyzerConstants.RecommendationItem.MEMORY) &&
                    null != requestsMap.get(AnalyzerConstants.RecommendationItem.MEMORY)) {
                currentMemRequest = requestsMap.get(AnalyzerConstants.RecommendationItem.MEMORY);
            }
        }

        if (currentConfigMap.containsKey(AnalyzerConstants.ResourceSetting.limits) &&
                null != currentConfigMap.get(AnalyzerConstants.ResourceSetting.limits)) {
            HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> limitsMap =
                    currentConfigMap.get(AnalyzerConstants.ResourceSetting.limits);
            if (limitsMap.containsKey(AnalyzerConstants.RecommendationItem.CPU) &&
                    null != limitsMap.get(AnalyzerConstants.RecommendationItem.CPU)) {
                currentCPULimit = limitsMap.get(AnalyzerConstants.RecommendationItem.CPU);
            }
            if (limitsMap.containsKey(AnalyzerConstants.RecommendationItem.MEMORY) &&
                    null != limitsMap.get(AnalyzerConstants.RecommendationItem.MEMORY)) {
                currentMemLimit = limitsMap.get(AnalyzerConstants.RecommendationItem.MEMORY);
            }
        }

        return new CurrentConfigValues(currentCPURequest, currentCPULimit, currentMemRequest, currentMemLimit);
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
