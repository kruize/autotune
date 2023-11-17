/*******************************************************************************
 * Copyright (c) 2023 Red Hat, IBM Corporation and others.
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
import com.autotune.analyzer.recommendations.Recommendation;
import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.recommendations.RecommendationNotification;
import com.autotune.analyzer.recommendations.subCategory.DurationBasedRecommendationSubCategory;
import com.autotune.analyzer.recommendations.subCategory.RecommendationSubCategory;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.metrics.MetricAggregationInfoResults;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.common.utils.CommonUtils;
import com.autotune.utils.KruizeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationValueConstants.*;
import static com.autotune.analyzer.utils.AnalyzerConstants.PercentileConstants.HUNDREDTH_PERCENTILE;
import static com.autotune.analyzer.utils.AnalyzerConstants.PercentileConstants.NINETY_EIGHTH_PERCENTILE;

public class DurationBasedRecommendationEngine implements KruizeRecommendationEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(DurationBasedRecommendationEngine.class);
    private String name;
    private String key;
    private RecommendationConstants.RecommendationCategory category;

    public DurationBasedRecommendationEngine() {
        this.name = RecommendationConstants.RecommendationEngine.EngineNames.DURATION_BASED;
        this.key = RecommendationConstants.RecommendationEngine.EngineKeys.DURATION_BASED_KEY;
        this.category = RecommendationConstants.RecommendationCategory.DURATION_BASED;
    }

    public DurationBasedRecommendationEngine(String name) {
        this.name = name;
    }

    private static Timestamp getMonitoringStartTime(HashMap<Timestamp, IntervalResults> resultsHashMap,
                                                    DurationBasedRecommendationSubCategory durationBasedRecommendationSubCategory,
                                                    Timestamp endTime) {

        // Convert the HashMap to a TreeMap to maintain sorted order based on IntervalEndTime
        TreeMap<Timestamp, IntervalResults> sortedResultsHashMap = new TreeMap<>(Collections.reverseOrder());
        sortedResultsHashMap.putAll(resultsHashMap);

        double sum = 0.0;
        Timestamp intervalEndTime = null;
        for (Timestamp timestamp : sortedResultsHashMap.keySet()) {
            if (!timestamp.after(endTime)) {
                sum = sum + sortedResultsHashMap.get(timestamp).getDurationInMinutes();
                if (sum >= durationBasedRecommendationSubCategory.getGetDurationLowerBound()) {
                    // Storing the timestamp value in startTimestamp variable to return
                    intervalEndTime = timestamp;
                    break;
                }
            }
        }
        try {
            return sortedResultsHashMap.get(intervalEndTime).getIntervalStartTime();
        } catch (NullPointerException npe) {
            return null;
        }
    }

    /**
     * Calculate the number of pods being used as per the latest results
     * <p>
     * pods are calculated independently based on both the CPU and Memory usage results.
     * The max of both is then returned
     *
     * @param filteredResultsMap
     * @return
     */
    private static int getNumPods(Map<Timestamp, IntervalResults> filteredResultsMap) {
        Double max_pods_cpu = filteredResultsMap.values()
                .stream()
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

    private static RecommendationConfigItem getCPURequestRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap,
                                                                        Timestamp monitoringEndTimestamp,
                                                                        ArrayList<RecommendationNotification> notifications) {
        boolean setNotification = true;
        if (null == notifications) {
            LOGGER.error("Notifications Object passed is empty. The notifications are not sent as part of recommendation.");
            setNotification = false;
        }
        RecommendationConfigItem recommendationConfigItem = null;
        String format = "";
        List<Double> cpuUsageList = filteredResultsMap.values()
                .stream()
                .map(e -> {
                    Optional<MetricResults> cpuUsageResults = Optional.ofNullable(e.getMetricResultsMap().get(AnalyzerConstants.MetricName.cpuUsage));
                    Optional<MetricResults> cpuThrottleResults = Optional.ofNullable(e.getMetricResultsMap().get(AnalyzerConstants.MetricName.cpuThrottle));
                    double cpuUsageAvg = cpuUsageResults.map(m -> m.getAggregationInfoResult().getAvg()).orElse(0.0);
                    double cpuUsageMax = cpuUsageResults.map(m -> m.getAggregationInfoResult().getMax()).orElse(0.0);
                    double cpuUsageSum = cpuUsageResults.map(m -> m.getAggregationInfoResult().getSum()).orElse(0.0);
                    double cpuThrottleAvg = cpuThrottleResults.map(m -> m.getAggregationInfoResult().getAvg()).orElse(0.0);
                    double cpuThrottleMax = cpuThrottleResults.map(m -> m.getAggregationInfoResult().getMax()).orElse(0.0);
                    double cpuThrottleSum = cpuThrottleResults.map(m -> m.getAggregationInfoResult().getSum()).orElse(0.0);
                    double cpuRequestInterval = 0.0;
                    double cpuUsagePod = 0;
                    int numPods = 0;

                    // Use the Max value when available, if not use the Avg
                    double cpuUsage = (cpuUsageMax > 0) ? cpuUsageMax : cpuUsageAvg;
                    double cpuThrottle = (cpuThrottleMax > 0) ? cpuThrottleMax : cpuThrottleAvg;
                    double cpuUsageTotal = cpuUsage + cpuThrottle;

                    // Usage is less than 1 core, set it to the observed value.
                    if (CPU_ONE_CORE > cpuUsageTotal) {
                        cpuRequestInterval = cpuUsageTotal;
                    } else {
                        // Sum/Avg should give us the number of pods
                        if (0 != cpuUsageAvg) {
                            numPods = (int) Math.ceil(cpuUsageSum / cpuUsageAvg);
                            if (0 < numPods) {
                                cpuUsagePod = (cpuUsageSum + cpuThrottleSum) / numPods;
                            }
                        }
                        cpuRequestInterval = Math.max(cpuUsagePod, cpuUsageTotal);
                    }
                    return cpuRequestInterval;
                })
                .collect(Collectors.toList());

        Double cpuRequest = 0.0;
        Double cpuRequestMax = Collections.max(cpuUsageList);
        if (null != cpuRequestMax && CPU_ONE_CORE > cpuRequestMax) {
            cpuRequest = cpuRequestMax;
        } else {
            cpuRequest = CommonUtils.percentile(NINETY_EIGHTH_PERCENTILE, cpuUsageList);
        }

        // TODO: This code below should be optimised with idle detection (0 cpu usage in recorded data) in recommendation ALGO
        // Make sure that the recommendation cannot be null
        // Check if the cpu request is null
        if (null == cpuRequest) {
            cpuRequest = CPU_ZERO;
        }

        // Set notifications only if notification object is available
        if (setNotification) {
            // Check for Zero CPU
            if (CPU_ZERO.equals(cpuRequest)) {
                // Add notification for CPU_RECORDS_ARE_ZERO
                notifications.add(new RecommendationNotification(
                        RecommendationConstants.RecommendationNotification.NOTICE_CPU_RECORDS_ARE_ZERO
                ));
                // Returning null will make sure that the map is not populated with values
                return null;
            }
            // Check for IDLE CPU
            else if (CPU_ONE_MILLICORE >= cpuRequest) {
                // Add notification for CPU_RECORDS_ARE_IDLE
                notifications.add(new RecommendationNotification(
                        RecommendationConstants.RecommendationNotification.NOTICE_CPU_RECORDS_ARE_IDLE
                ));
                // Returning null will make sure that the map is not populated with values
                return null;
            }
        }

        for (IntervalResults intervalResults : filteredResultsMap.values()) {
            MetricResults cpuUsageResults = intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.cpuUsage);
            if (cpuUsageResults != null) {
                MetricAggregationInfoResults aggregationInfoResult = cpuUsageResults.getAggregationInfoResult();
                if (aggregationInfoResult != null) {
                    format = aggregationInfoResult.getFormat();
                    if (format != null && !format.isEmpty()) {
                        break;
                    }
                }
            }
        }

        recommendationConfigItem = new RecommendationConfigItem(cpuRequest, format);
        return recommendationConfigItem;
    }

    private static RecommendationConfigItem getCPULimitRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap) {
        // This method is not used for now
        return null;
    }

    private static RecommendationConfigItem getMemoryRequestRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap,
                                                                           Timestamp monitoringEndTimestamp,
                                                                           ArrayList<RecommendationNotification> notifications) {
        boolean setNotification = true;
        if (null == notifications) {
            LOGGER.error("Notifications Object passed is empty. The notifications are not sent as part of recommendation.");
            setNotification = false;
        }
        RecommendationConfigItem recommendationConfigItem = null;
        String format = "";
        List<Double> memUsageList = filteredResultsMap.values()
                .stream()
                .map(e -> {
                    Optional<MetricResults> cpuUsageResults = Optional.ofNullable(e.getMetricResultsMap().get(AnalyzerConstants.MetricName.cpuUsage));
                    double cpuUsageAvg = cpuUsageResults.map(m -> m.getAggregationInfoResult().getAvg()).orElse(0.0);
                    double cpuUsageSum = cpuUsageResults.map(m -> m.getAggregationInfoResult().getSum()).orElse(0.0);
                    Optional<MetricResults> memoryUsageResults = Optional.ofNullable(e.getMetricResultsMap().get(AnalyzerConstants.MetricName.memoryUsage));
                    double memUsageAvg = memoryUsageResults.map(m -> m.getAggregationInfoResult().getAvg()).orElse(0.0);
                    double memUsageMax = memoryUsageResults.map(m -> m.getAggregationInfoResult().getMax()).orElse(0.0);
                    double memUsageSum = memoryUsageResults.map(m -> m.getAggregationInfoResult().getSum()).orElse(0.0);
                    double memUsage = 0;
                    int numPods = 0;

                    if (0 != cpuUsageAvg) {
                        numPods = (int) Math.ceil(cpuUsageSum / cpuUsageAvg);
                    }
                    // If numPods is still zero, could be because there is no CPU info
                    // We can use mem data to calculate pods, this is not as reliable as cpu
                    // but better than nothing!
                    if (0 == numPods) {
                        if (0 != memUsageAvg) {
                            numPods = (int) Math.ceil(memUsageSum / memUsageAvg);
                        }
                    }
                    if (0 < numPods) {
                        memUsage = (memUsageSum / numPods);
                    }
                    memUsage = Math.max(memUsage, memUsageMax);

                    return memUsage;
                })
                .collect(Collectors.toList());

        // spikeList is the max spike observed in each measurementDuration
        List<Double> spikeList = filteredResultsMap.values()
                .stream()
                .map(e -> {
                    Optional<MetricResults> memoryUsageResults = Optional.ofNullable(e.getMetricResultsMap().get(AnalyzerConstants.MetricName.memoryUsage));
                    Optional<MetricResults> memoryRSSResults = Optional.ofNullable(e.getMetricResultsMap().get(AnalyzerConstants.MetricName.memoryRSS));
                    double memUsageMax = memoryUsageResults.map(m -> m.getAggregationInfoResult().getMax()).orElse(0.0);
                    double memUsageMin = memoryUsageResults.map(m -> m.getAggregationInfoResult().getMin()).orElse(0.0);
                    double memRSSMax = memoryRSSResults.map(m -> m.getAggregationInfoResult().getMax()).orElse(0.0);
                    double memRSSMin = memoryRSSResults.map(m -> m.getAggregationInfoResult().getMin()).orElse(0.0);
                    // Calculate the spike in each interval
                    double intervalSpike = Math.max(Math.ceil(memUsageMax - memUsageMin), Math.ceil(memRSSMax - memRSSMin));

                    return intervalSpike;
                })
                .collect(Collectors.toList());

        // Add a buffer to the current usage max
        Double memRecUsage = CommonUtils.percentile(HUNDREDTH_PERCENTILE, memUsageList);
        Double memRecUsageBuf = memRecUsage + (memRecUsage * MEM_USAGE_BUFFER_DECIMAL);

        // Add a small buffer to the current usage spike max and add it to the current usage max
        Double memRecSpike = CommonUtils.percentile(HUNDREDTH_PERCENTILE, spikeList);
        memRecSpike += (memRecSpike * MEM_SPIKE_BUFFER_DECIMAL);
        Double memRecSpikeBuf = memRecUsage + memRecSpike;

        // We'll use the minimum of the above two values
        Double memRec = Math.min(memRecUsageBuf, memRecSpikeBuf);

        // Set notifications only if notification object is available
        if (setNotification) {
            // Check if the memory recommendation is 0
            if (null == memRec || 0.0 == memRec) {
                // Add appropriate Notification - MEMORY_RECORDS_ARE_ZERO
                notifications.add(new RecommendationNotification(
                        RecommendationConstants.RecommendationNotification.NOTICE_MEMORY_RECORDS_ARE_ZERO
                ));
                // Returning null will make sure that the map is not populated with values
                return null;
            }
        }

        for (IntervalResults intervalResults : filteredResultsMap.values()) {
            MetricResults memoryUsageResults = intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.memoryUsage);
            if (memoryUsageResults != null) {
                MetricAggregationInfoResults aggregationInfoResult = memoryUsageResults.getAggregationInfoResult();
                if (aggregationInfoResult != null) {
                    format = aggregationInfoResult.getFormat();
                    if (format != null && !format.isEmpty()) {
                        break;
                    }
                }
            }
        }

        recommendationConfigItem = new RecommendationConfigItem(memRec, format);
        return recommendationConfigItem;
    }

    private static RecommendationConfigItem getMemoryLimitRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap) {
        // This method is not used for now
        return null;
    }

    private static RecommendationConfigItem getCurrentValue(Map<Timestamp, IntervalResults> filteredResultsMap,
                                                            Timestamp timestampToExtract,
                                                            AnalyzerConstants.ResourceSetting resourceSetting,
                                                            AnalyzerConstants.RecommendationItem recommendationItem,
                                                            ArrayList<RecommendationNotification> notifications) {
        Double currentValue = null;
        String format = null;
        RecommendationConfigItem recommendationConfigItem = null;
        AnalyzerConstants.MetricName metricName = null;
        for (Timestamp timestamp : filteredResultsMap.keySet()) {
            if (!timestamp.equals(timestampToExtract))
                continue;
            IntervalResults intervalResults = filteredResultsMap.get(timestamp);
            if (resourceSetting == AnalyzerConstants.ResourceSetting.requests) {
                if (recommendationItem == AnalyzerConstants.RecommendationItem.cpu)
                    metricName = AnalyzerConstants.MetricName.cpuRequest;
                if (recommendationItem == AnalyzerConstants.RecommendationItem.memory)
                    metricName = AnalyzerConstants.MetricName.memoryRequest;
            }
            if (resourceSetting == AnalyzerConstants.ResourceSetting.limits) {
                if (recommendationItem == AnalyzerConstants.RecommendationItem.cpu)
                    metricName = AnalyzerConstants.MetricName.cpuLimit;
                if (recommendationItem == AnalyzerConstants.RecommendationItem.memory)
                    metricName = AnalyzerConstants.MetricName.memoryLimit;
            }
            if (null != metricName) {
                if (intervalResults.getMetricResultsMap().containsKey(metricName)) {
                    Optional<MetricResults> metricResults = Optional.ofNullable(intervalResults.getMetricResultsMap().get(metricName));
                    currentValue = metricResults.map(m -> m.getAggregationInfoResult().getAvg()).orElse(null);
                    format = metricResults.map(m -> m.getAggregationInfoResult().getFormat()).orElse(null);
                }
                if (null == currentValue) {
                    setNotificationsFor(resourceSetting, recommendationItem, notifications);
                }
                return new RecommendationConfigItem(currentValue, format);
            }
        }
        setNotificationsFor(resourceSetting, recommendationItem, notifications);
        return null;
    }

    private static void setNotificationsFor(AnalyzerConstants.ResourceSetting resourceSetting,
                                            AnalyzerConstants.RecommendationItem recommendationItem,
                                            ArrayList<RecommendationNotification> notifications) {
        // Check notifications is null, If it's null -> return.
        if (null == notifications)
            return;
        // Check if the item is CPU
        if (recommendationItem == AnalyzerConstants.RecommendationItem.cpu) {
            // Check if the setting is REQUESTS
            if (resourceSetting == AnalyzerConstants.ResourceSetting.requests) {
                notifications.add(new RecommendationNotification(
                        RecommendationConstants.RecommendationNotification.CRITICAL_CPU_REQUEST_NOT_SET
                ));
            }
            // Check if the setting is LIMITS
            else if (resourceSetting == AnalyzerConstants.ResourceSetting.limits) {
                notifications.add(new RecommendationNotification(
                        RecommendationConstants.RecommendationNotification.WARNING_CPU_LIMIT_NOT_SET
                ));
            }

        }
        // Check if the item is Memory
        else if (recommendationItem == AnalyzerConstants.RecommendationItem.memory) {
            // Check if the setting is REQUESTS
            if (resourceSetting == AnalyzerConstants.ResourceSetting.requests) {
                notifications.add(new RecommendationNotification(
                        RecommendationConstants.RecommendationNotification.CRITICAL_MEMORY_REQUEST_NOT_SET
                ));
            }
            // Check if the setting is LIMITS
            else if (resourceSetting == AnalyzerConstants.ResourceSetting.limits) {
                notifications.add(new RecommendationNotification(
                        RecommendationConstants.RecommendationNotification.CRITICAL_MEMORY_LIMIT_NOT_SET
                ));
            }
        }
    }

    @Override
    public String getEngineName() {
        return this.name;
    }

    @Override
    public String getEngineKey() {
        return this.key;
    }

    @Override
    public RecommendationConstants.RecommendationCategory getEngineCategory() {
        return this.category;
    }

    /**
     * This method handles validating the data and populating to the recommendation object
     * <p>
     * DO NOT EDIT THIS METHOD UNLESS THERE ARE ANY CHANGES TO BE ADDED IN VALIDATION OR POPULATION MECHANISM
     * EDITING THIS METHOD MIGHT LEAD TO UNEXPECTED OUTCOMES IN RECOMMENDATIONS, PLEASE PROCEED WITH CAUTION
     *
     * @param recommendationTerm
     * @param recommendation
     * @param notifications
     * @param internalMapToPopulate
     */
    private boolean populateRecommendation(String recommendationTerm,
                                           Recommendation recommendation,
                                           ArrayList<RecommendationNotification> notifications,
                                           HashMap<String, RecommendationConfigItem> internalMapToPopulate,
                                           int numPods, double hours, double cpuThreshold, double memoryThreshold) {
        // Check for cpu & memory Thresholds (Duplicate check if the caller is generate recommendations)
        if (cpuThreshold <= 0.0) {
            LOGGER.error("Given CPU Threshold is invalid, setting Default CPU Threshold : " + DEFAULT_CPU_THRESHOLD);
            cpuThreshold = DEFAULT_CPU_THRESHOLD;
        }
        if (memoryThreshold <= 0.0) {
            LOGGER.error("Given Memory Threshold is invalid, setting Default Memory Threshold : " + DEFAULT_MEMORY_THRESHOLD);
            memoryThreshold = DEFAULT_MEMORY_THRESHOLD;
        }
        // Check for null
        if (null == recommendationTerm) {
            LOGGER.error("Recommendation term cannot be null");
            return false;
        }
        // Remove whitespaces
        recommendationTerm = recommendationTerm.trim();

        // Check if term is not empty and also must be one of short, medium or long term
        if (recommendationTerm.isEmpty() ||
                (
                        !recommendationTerm.equalsIgnoreCase(KruizeConstants.JSONKeys.SHORT_TERM) &&
                                !recommendationTerm.equalsIgnoreCase(KruizeConstants.JSONKeys.MEDIUM_TERM) &&
                                !recommendationTerm.equalsIgnoreCase(KruizeConstants.JSONKeys.LONG_TERM)
                )
        ) {
            LOGGER.error("Invalid Recommendation Term");
            return false;
        }

        // Check if recommendation is null
        if (null == recommendation) {
            LOGGER.error("Recommendation cannot be null");
            return false;
        }

        // Check if notification is null (Do not check for empty as notifications might not have been populated)
        if (null == notifications) {
            LOGGER.error("Notifications cannot be null");
            return false;
        }

        // Check if the map is populated with atleast one data point
        if (null == internalMapToPopulate || internalMapToPopulate.isEmpty()) {
            LOGGER.error("Internal map sent to populate method cannot be null or empty");
            return false;
        }

        boolean isSuccess = true;

        // CPU flags
        //      Current Request and Limits flags
        boolean isCurrentCPURequestAvailable = false;
        boolean isCurrentCPULimitAvailable = false;

        //      Recommended Request and Limits flags
        boolean isRecommendedCPURequestAvailable = false;
        boolean isRecommendedCPULimitAvailable = false;

        //      Variation Requests and Limits flags
        boolean isVariationCPURequestAvailable = false;
        boolean isVariationCPULimitAvailable = false;

        // Memory flags
        //      Current Request and Limits flags
        boolean isCurrentMemoryRequestAvailable = false;
        boolean isCurrentMemoryLimitAvailable = false;

        //      Recommended Request and Limits flags
        boolean isRecommendedMemoryRequestAvailable = false;
        boolean isRecommendedMemoryLimitAvailable = false;

        //      Variation Requests and Limits flags
        boolean isVariationMemoryRequestAvailable = false;
        boolean isVariationMemoryLimitAvailable = false;


        if (numPods == 0) {
            RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_NUM_PODS_CANNOT_BE_ZERO);
            notifications.add(recommendationNotification);
            LOGGER.debug("Number of pods cannot be zero");  //
            isSuccess = false;
        } else if (numPods < 0) {
            RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_NUM_PODS_CANNOT_BE_NEGATIVE);
            notifications.add(recommendationNotification);
            LOGGER.debug("Number of pods cannot be negative");
            isSuccess = false;
        } else {
            recommendation.setPodsCount(numPods);
        }


        // Set Hours
        if (hours == 0.0) {
            RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_HOURS_CANNOT_BE_ZERO);
            notifications.add(recommendationNotification);
            LOGGER.error("Duration hours cannot be zero");
            isSuccess = false;
        } else if (hours < 0) {
            RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_HOURS_CANNOT_BE_NEGATIVE);
            notifications.add(recommendationNotification);
            LOGGER.error("Duration hours cannot be negative");
            isSuccess = false;
        } else {
            recommendation.setDuration_in_hours(hours);
        }

        RecommendationConfigItem recommendationCpuRequest = null;
        RecommendationConfigItem recommendationMemRequest = null;
        RecommendationConfigItem recommendationCpuLimits = null;
        RecommendationConfigItem recommendationMemLimits = null;

        RecommendationConfigItem currentCpuRequest = null;
        RecommendationConfigItem currentMemRequest = null;
        RecommendationConfigItem currentCpuLimit = null;
        RecommendationConfigItem currentMemLimit = null;

        RecommendationConfigItem variationCpuRequest = null;
        RecommendationConfigItem variationMemRequest = null;
        RecommendationConfigItem variationCpuLimit = null;
        RecommendationConfigItem variationMemLimit = null;

        if (internalMapToPopulate.containsKey(RecommendationConstants.RecommendationEngine.InternalConstants.RECOMMENDED_CPU_REQUEST))
            recommendationCpuRequest = internalMapToPopulate.get(RecommendationConstants.RecommendationEngine.InternalConstants.RECOMMENDED_CPU_REQUEST);

        if (internalMapToPopulate.containsKey(RecommendationConstants.RecommendationEngine.InternalConstants.RECOMMENDED_MEMORY_REQUEST))
            recommendationMemRequest = internalMapToPopulate.get(RecommendationConstants.RecommendationEngine.InternalConstants.RECOMMENDED_MEMORY_REQUEST);

        if (internalMapToPopulate.containsKey(RecommendationConstants.RecommendationEngine.InternalConstants.RECOMMENDED_CPU_LIMIT))
            recommendationCpuLimits = internalMapToPopulate.get(RecommendationConstants.RecommendationEngine.InternalConstants.RECOMMENDED_CPU_LIMIT);

        if (internalMapToPopulate.containsKey(RecommendationConstants.RecommendationEngine.InternalConstants.RECOMMENDED_MEMORY_LIMIT))
            recommendationMemLimits = internalMapToPopulate.get(RecommendationConstants.RecommendationEngine.InternalConstants.RECOMMENDED_MEMORY_LIMIT);

        if (internalMapToPopulate.containsKey(RecommendationConstants.RecommendationEngine.InternalConstants.CURRENT_CPU_REQUEST))
            currentCpuRequest = internalMapToPopulate.get(RecommendationConstants.RecommendationEngine.InternalConstants.CURRENT_CPU_REQUEST);

        if (internalMapToPopulate.containsKey(RecommendationConstants.RecommendationEngine.InternalConstants.CURRENT_MEMORY_REQUEST))
            currentMemRequest = internalMapToPopulate.get(RecommendationConstants.RecommendationEngine.InternalConstants.CURRENT_MEMORY_REQUEST);

        if (internalMapToPopulate.containsKey(RecommendationConstants.RecommendationEngine.InternalConstants.CURRENT_CPU_LIMIT))
            currentCpuLimit = internalMapToPopulate.get(RecommendationConstants.RecommendationEngine.InternalConstants.CURRENT_CPU_LIMIT);

        if (internalMapToPopulate.containsKey(RecommendationConstants.RecommendationEngine.InternalConstants.CURRENT_MEMORY_LIMIT))
            currentMemLimit = internalMapToPopulate.get(RecommendationConstants.RecommendationEngine.InternalConstants.CURRENT_MEMORY_LIMIT);


        HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> config = new HashMap<>();
        // Create Request Map
        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requestsMap = new HashMap<>();
        // Recommendation Item checks
        boolean isCpuRequestValid = true;
        boolean isMemoryRequestValid = true;

        if (null == recommendationCpuRequest || null == recommendationCpuRequest.getAmount() || recommendationCpuRequest.getAmount() <= 0) {
            isCpuRequestValid = false;
        }
        if (null == recommendationMemRequest || null == recommendationMemRequest.getAmount() || recommendationMemRequest.getAmount() <= 0) {
            isMemoryRequestValid = false;
        }

        // Initiate generated value holders with min values constants to compare later
        Double generatedCpuRequest = null;
        String generatedCpuRequestFormat = null;
        Double generatedMemRequest = null;
        String generatedMemRequestFormat = null;

        // Check for null
        if (null != recommendationCpuRequest && isCpuRequestValid) {
            generatedCpuRequest = recommendationCpuRequest.getAmount();
            generatedCpuRequestFormat = recommendationCpuRequest.getFormat();
            if (null != generatedCpuRequestFormat && !generatedCpuRequestFormat.isEmpty()) {
                isRecommendedCPURequestAvailable = true;
                requestsMap.put(AnalyzerConstants.RecommendationItem.cpu, recommendationCpuRequest);
            } else {
                RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_FORMAT_MISSING_IN_CPU_SECTION);
                notifications.add(recommendationNotification);
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.FORMAT_MISSING_IN_CPU_SECTION);
            }
        }

        // Check for null
        if (null != recommendationMemRequest && isMemoryRequestValid) {
            generatedMemRequest = recommendationMemRequest.getAmount();
            generatedMemRequestFormat = recommendationMemRequest.getFormat();
            if (null != generatedMemRequestFormat && !generatedMemRequestFormat.isEmpty()) {
                isRecommendedMemoryRequestAvailable = true;
                requestsMap.put(AnalyzerConstants.RecommendationItem.memory, recommendationMemRequest);
            } else {
                RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_FORMAT_MISSING_IN_MEMORY_SECTION);
                notifications.add(recommendationNotification);
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.FORMAT_MISSING_IN_MEMORY_SECTION);
            }
        }

        // Create Limits Map
        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> limitsMap = new HashMap<>();
        // Recommendation Item checks (adding additional check for limits even though they are same as limits to maintain code to be flexible to add limits in future)
        boolean isCpuLimitValid = true;
        boolean isMemoryLimitValid = true;


        if (null == recommendationCpuLimits || null == recommendationCpuLimits.getAmount() || recommendationCpuLimits.getAmount() <= 0) {
            isCpuLimitValid = false;
        }
        if (null == recommendationMemLimits || null == recommendationMemLimits.getAmount() || recommendationMemLimits.getAmount() <= 0) {
            isMemoryLimitValid = false;
        }

        // Initiate generated value holders with min values constants to compare later
        Double generatedCpuLimit = null;
        String generatedCpuLimitFormat = null;
        Double generatedMemLimit = null;
        String generatedMemLimitFormat = null;

        // Check for null
        if (null != recommendationCpuLimits && isCpuLimitValid) {
            generatedCpuLimit = recommendationCpuLimits.getAmount();
            generatedCpuLimitFormat = recommendationCpuLimits.getFormat();
            if (null != generatedCpuLimitFormat && !generatedCpuLimitFormat.isEmpty()) {
                isRecommendedCPULimitAvailable = true;
                limitsMap.put(AnalyzerConstants.RecommendationItem.cpu, recommendationCpuLimits);
            } else {
                RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_FORMAT_MISSING_IN_CPU_SECTION);
                notifications.add(recommendationNotification);
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.FORMAT_MISSING_IN_CPU_SECTION);
            }
        }

        // Check for null
        if (null != recommendationMemLimits && isMemoryLimitValid) {
            generatedMemLimit = recommendationMemLimits.getAmount();
            generatedMemLimitFormat = recommendationMemLimits.getFormat();
            if (null != generatedMemLimitFormat && !generatedMemLimitFormat.isEmpty()) {
                isRecommendedMemoryLimitAvailable = true;
                limitsMap.put(AnalyzerConstants.RecommendationItem.memory, recommendationMemLimits);
            } else {
                RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_FORMAT_MISSING_IN_MEMORY_SECTION);
                notifications.add(recommendationNotification);
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.FORMAT_MISSING_IN_MEMORY_SECTION);
            }
        }

        // Create Current Map
        HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> currentConfig = new HashMap<>();

        // Create Current Requests Map
        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> currentRequestsMap = new HashMap<>();

        // Check if Current CPU Requests Exists
        if (null != currentCpuRequest && null != currentCpuRequest.getAmount()) {
            if (currentCpuRequest.getAmount() <= 0.0) {
                RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_INVALID_AMOUNT_IN_CPU_SECTION);
                notifications.add(recommendationNotification);
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_AMOUNT_IN_CPU_SECTION);
            } else if (null == currentCpuRequest.getFormat() || currentCpuRequest.getFormat().isEmpty()) {
                RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_INVALID_FORMAT_IN_CPU_SECTION);
                notifications.add(recommendationNotification);
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_FORMAT_IN_CPU_SECTION);
            } else {
                isCurrentCPURequestAvailable = true;
                currentRequestsMap.put(AnalyzerConstants.RecommendationItem.cpu, currentCpuRequest);
            }
        }

        // Check if Current Memory Requests Exists
        if (null != currentMemRequest && null != currentMemRequest.getAmount()) {
            if (currentMemRequest.getAmount() <= 0) {
                RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_INVALID_AMOUNT_IN_MEMORY_SECTION);
                notifications.add(recommendationNotification);
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_AMOUNT_IN_MEMORY_SECTION);
            } else if (null == currentMemRequest.getFormat() || currentMemRequest.getFormat().isEmpty()) {
                RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_INVALID_FORMAT_IN_MEMORY_SECTION);
                notifications.add(recommendationNotification);
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_FORMAT_IN_MEMORY_SECTION);
            } else {
                isCurrentMemoryRequestAvailable = true;
                currentRequestsMap.put(AnalyzerConstants.RecommendationItem.memory, currentMemRequest);
            }
        }

        // Create Current Limits Map
        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> currentLimitsMap = new HashMap<>();

        // Check if Current CPU Limits Exists
        if (null != currentCpuLimit && null != currentCpuLimit.getAmount()) {
            if (currentCpuLimit.getAmount() <= 0.0) {
                RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_INVALID_AMOUNT_IN_CPU_SECTION);
                notifications.add(recommendationNotification);
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_AMOUNT_IN_CPU_SECTION);
            } else if (null == currentCpuLimit.getFormat() || currentCpuLimit.getFormat().isEmpty()) {
                RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_INVALID_FORMAT_IN_CPU_SECTION);
                notifications.add(recommendationNotification);
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_FORMAT_IN_CPU_SECTION);
            } else {
                isCurrentCPULimitAvailable = true;
                currentLimitsMap.put(AnalyzerConstants.RecommendationItem.cpu, currentCpuLimit);
            }
        }

        // Check if Current Memory Limits Exists
        if (null != currentMemLimit && null != currentMemLimit.getAmount()) {
            if (currentMemLimit.getAmount() <= 0.0) {
                RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_INVALID_AMOUNT_IN_MEMORY_SECTION);
                notifications.add(recommendationNotification);
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_AMOUNT_IN_MEMORY_SECTION);
            } else if (null == currentMemLimit.getFormat() || currentMemLimit.getFormat().isEmpty()) {
                RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.ERROR_INVALID_FORMAT_IN_MEMORY_SECTION);
                notifications.add(recommendationNotification);
                LOGGER.error(RecommendationConstants.RecommendationNotificationMsgConstant.INVALID_FORMAT_IN_MEMORY_SECTION);
            } else {
                isCurrentMemoryLimitAvailable = true;
                currentLimitsMap.put(AnalyzerConstants.RecommendationItem.memory, currentMemLimit);
            }
        }

        // Create variation map
        HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> variation = new HashMap<>();
        // Create a new map for storing variation in requests
        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requestsVariationMap = new HashMap<>();

        double currentCpuRequestValue = 0.0;
        if (null != currentCpuRequest && null != currentCpuRequest.getAmount() && currentCpuRequest.getAmount() > 0.0) {
            currentCpuRequestValue = currentCpuRequest.getAmount();
        }
        if (null != generatedCpuRequest && null != generatedCpuRequestFormat) {
            double diff = generatedCpuRequest - currentCpuRequestValue;
            // TODO: If difference is positive it can be considered as under-provisioning, Need to handle it better
            isVariationCPURequestAvailable = true;
            variationCpuRequest = new RecommendationConfigItem(diff, generatedCpuRequestFormat);
            requestsVariationMap.put(AnalyzerConstants.RecommendationItem.cpu, variationCpuRequest);
        }

        double currentMemRequestValue = 0.0;
        if (null != currentMemRequest && null != currentMemRequest.getAmount() && currentMemRequest.getAmount() > 0.0) {
            currentMemRequestValue = currentMemRequest.getAmount();
        }
        if (null != generatedMemRequest && null != generatedMemRequestFormat) {
            double diff = generatedMemRequest - currentMemRequestValue;
            // TODO: If difference is positive it can be considered as under-provisioning, Need to handle it better
            isVariationMemoryRequestAvailable = true;
            variationMemRequest = new RecommendationConfigItem(diff, generatedMemRequestFormat);
            requestsVariationMap.put(AnalyzerConstants.RecommendationItem.memory, variationMemRequest);
        }

        // Create a new map for storing variation in limits
        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> limitsVariationMap = new HashMap<>();

        // No notification if CPU limit not set
        // Check if currentCpuLimit is not null and

        double currentCpuLimitValue = 0.0;
        if (null != currentCpuLimit && null != currentCpuLimit.getAmount() && currentCpuLimit.getAmount() > 0.0) {
            currentCpuLimitValue = currentCpuLimit.getAmount();
        }
        if (null != generatedCpuLimit && null != generatedCpuLimitFormat) {
            double diff = generatedCpuLimit - currentCpuLimitValue;
            isVariationCPULimitAvailable = true;
            variationCpuLimit = new RecommendationConfigItem(diff, generatedCpuLimitFormat);
            limitsVariationMap.put(AnalyzerConstants.RecommendationItem.cpu, variationCpuLimit);
        }

        double currentMemLimitValue = 0.0;
        if (null != currentMemLimit && null != currentMemLimit.getAmount() && currentMemLimit.getAmount() > 0.0) {
            currentMemLimitValue = currentMemLimit.getAmount();
        }
        if (null != generatedMemLimit && null != generatedMemLimitFormat) {
            double diff = generatedMemLimit - currentMemLimitValue;
            isVariationMemoryLimitAvailable = true;
            variationMemLimit = new RecommendationConfigItem(diff, generatedMemLimitFormat);
            limitsVariationMap.put(AnalyzerConstants.RecommendationItem.memory, variationMemLimit);
        }

        // Check for thresholds
        if (isRecommendedCPURequestAvailable) {
            if (isCurrentCPURequestAvailable && currentCpuRequestValue > 0.0) {
                double diffCpuRequestPercentage = CommonUtils.getPercentage(generatedCpuRequest.doubleValue(), currentCpuRequestValue);
                // Check if variation percentage is negative
                if (diffCpuRequestPercentage < 0.0) {
                    // Convert to positive to check with threshold
                    diffCpuRequestPercentage = diffCpuRequestPercentage * (-1);
                }
                if (diffCpuRequestPercentage <= cpuThreshold) {
                    // Remove from Config
                    requestsMap.remove(AnalyzerConstants.RecommendationItem.cpu);
                    // Remove from Variation
                    requestsVariationMap.remove(AnalyzerConstants.RecommendationItem.cpu);
                    RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.NOTICE_CPU_REQUESTS_OPTIMISED);
                    notifications.add(recommendationNotification);
                }
            }
        }

        if (isRecommendedCPULimitAvailable) {
            if (isCurrentCPULimitAvailable && currentCpuLimitValue > 0.0) {
                double diffCPULimitPercentage = CommonUtils.getPercentage(generatedCpuLimit.doubleValue(), currentCpuLimitValue);
                // Check if variation percentage is negative
                if (diffCPULimitPercentage < 0.0) {
                    // Convert to positive to check with threshold
                    diffCPULimitPercentage = diffCPULimitPercentage * (-1);
                }
                if (diffCPULimitPercentage <= cpuThreshold) {
                    // Remove from Config
                    limitsMap.remove(AnalyzerConstants.RecommendationItem.cpu);
                    // Remove from Variation
                    limitsVariationMap.remove(AnalyzerConstants.RecommendationItem.cpu);
                    RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.NOTICE_CPU_LIMITS_OPTIMISED);
                    notifications.add(recommendationNotification);
                }
            }
        }

        if (isRecommendedMemoryRequestAvailable) {
            if (isCurrentMemoryRequestAvailable && currentMemRequestValue > 0.0) {
                double diffMemRequestPercentage = CommonUtils.getPercentage(generatedMemRequest.doubleValue(), currentMemRequestValue);
                // Check if variation percentage is negative
                if (diffMemRequestPercentage < 0.0) {
                    // Convert to positive to check with threshold
                    diffMemRequestPercentage = diffMemRequestPercentage * (-1);
                }
                if (diffMemRequestPercentage <= memoryThreshold) {
                    // Remove from Config
                    requestsMap.remove(AnalyzerConstants.RecommendationItem.memory);
                    // Remove from Variation
                    requestsVariationMap.remove(AnalyzerConstants.RecommendationItem.memory);
                    RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.NOTICE_MEMORY_REQUESTS_OPTIMISED);
                    notifications.add(recommendationNotification);
                }
            }
        }

        if (isRecommendedMemoryLimitAvailable) {
            if (isCurrentMemoryLimitAvailable && currentMemLimitValue > 0.0) {
                double diffMemLimitPercentage = CommonUtils.getPercentage(generatedMemLimit.doubleValue(), currentMemLimitValue);
                // Check if variation percentage is negative
                if (diffMemLimitPercentage < 0.0) {
                    // Convert to positive to check with threshold
                    diffMemLimitPercentage = diffMemLimitPercentage * (-1);
                }
                if (diffMemLimitPercentage <= cpuThreshold) {
                    // Remove from Config
                    limitsMap.remove(AnalyzerConstants.RecommendationItem.memory);
                    // Remove from Variation
                    limitsVariationMap.remove(AnalyzerConstants.RecommendationItem.memory);
                    RecommendationNotification recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.NOTICE_MEMORY_LIMITS_OPTIMISED);
                    notifications.add(recommendationNotification);
                }
            }
        }

        // Set Request Map
        if (!requestsMap.isEmpty()) {
            config.put(AnalyzerConstants.ResourceSetting.requests, requestsMap);
        }

        // Set Limits Map
        if (!limitsMap.isEmpty()) {
            config.put(AnalyzerConstants.ResourceSetting.limits, limitsMap);
        }

        // Set Config
        if (!config.isEmpty()) {
            recommendation.setConfig(config);
        }

        // Check if map is not empty and set requests map to current config
        if (!currentRequestsMap.isEmpty()) {
            currentConfig.put(AnalyzerConstants.ResourceSetting.requests, currentRequestsMap);
        }

        // Check if map is not empty and set limits map to current config
        if (!currentLimitsMap.isEmpty()) {
            currentConfig.put(AnalyzerConstants.ResourceSetting.limits, currentLimitsMap);
        }

        // Set Current Config
        if (!currentConfig.isEmpty()) {
            recommendation.setCurrentConfig(currentConfig);
        }

        // Set Request variation map
        if (!requestsVariationMap.isEmpty()) {
            variation.put(AnalyzerConstants.ResourceSetting.requests, requestsVariationMap);
        }

        // Set Limits variation map
        if (!limitsVariationMap.isEmpty()) {
            variation.put(AnalyzerConstants.ResourceSetting.limits, limitsVariationMap);
        }

        // Set Variation Map
        if (!variation.isEmpty()) {
            recommendation.setVariation(variation);
        }

        return isSuccess;
    }

    @Override
    public HashMap<String, Recommendation> generateRecommendation(ContainerData containerData, Timestamp monitoringEndTime, RecommendationSettings recommendationSettings) {
        // Set CPU threshold to default
        double cpuThreshold = DEFAULT_CPU_THRESHOLD;
        // Set Memory threshold to default
        double memoryThreshold = DEFAULT_MEMORY_THRESHOLD;
        if (null != recommendationSettings) {
            Double threshold = recommendationSettings.getThreshold();
            if (null == threshold) {
                LOGGER.info("Threshold is not set, setting Default CPU Threshold : " + DEFAULT_CPU_THRESHOLD + " and Memory Threshold : " + DEFAULT_MEMORY_THRESHOLD);
            } else if (threshold.doubleValue() <= 0.0) {
                LOGGER.error("Given Threshold is invalid, setting Default CPU Threshold : " + DEFAULT_CPU_THRESHOLD + " and Memory Threshold : " + DEFAULT_MEMORY_THRESHOLD);
            } else {
                cpuThreshold = threshold.doubleValue();
                memoryThreshold = threshold.doubleValue();
            }
        } else {
            LOGGER.error("Recommendation Settings are null, setting Default CPU Threshold : " + DEFAULT_CPU_THRESHOLD + " and Memory Threshold : " + DEFAULT_MEMORY_THRESHOLD);
        }

        // Get the results
        HashMap<Timestamp, IntervalResults> resultsMap = containerData.getResults();
        // Create a new map for returning the result
        HashMap<String, Recommendation> resultRecommendation = new HashMap<String, Recommendation>();
        for (RecommendationSubCategory recommendationSubCategory : this.category.getRecommendationSubCategories()) {
            DurationBasedRecommendationSubCategory durationBasedRecommendationSubCategory = (DurationBasedRecommendationSubCategory) recommendationSubCategory;
            String recPeriod = durationBasedRecommendationSubCategory.getSubCategory();
            int days = durationBasedRecommendationSubCategory.getDuration();
            Timestamp monitoringStartTime = getMonitoringStartTime(resultsMap,
                    durationBasedRecommendationSubCategory,
                    monitoringEndTime);
            if (null != monitoringStartTime) {

                Timestamp finalMonitoringStartTime = monitoringStartTime;
                // Set the timestamp to extract
                Timestamp timestampToExtract = monitoringEndTime;

                Map<Timestamp, IntervalResults> filteredResultsMap = containerData.getResults().entrySet().stream()
                        .filter((x -> ((x.getKey().compareTo(finalMonitoringStartTime) >= 0)
                                && (x.getKey().compareTo(monitoringEndTime) <= 0))))
                        .collect((Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

                Recommendation recommendation = new Recommendation(monitoringStartTime, monitoringEndTime);

                // Set number of pods
                int numPods = getNumPods(filteredResultsMap);


                // Set Duration in hours
                double hours = days * KruizeConstants.TimeConv.NO_OF_HOURS_PER_DAY;


                // Pass Notification object to all callers to update the notifications required
                ArrayList<RecommendationNotification> notifications = new ArrayList<RecommendationNotification>();

                // Get the Recommendation Items
                RecommendationConfigItem recommendationCpuRequest = getCPURequestRecommendation(
                        filteredResultsMap,
                        monitoringEndTime,
                        notifications);
                RecommendationConfigItem recommendationMemRequest = getMemoryRequestRecommendation(
                        filteredResultsMap,
                        monitoringEndTime,
                        notifications);

                // Get the Recommendation Items
                // Calling requests on limits as we are maintaining limits and requests as same
                // Maintaining different flow for both of them even though if they are same as in future we might have
                // a different implementation for both and this avoids confusion
                RecommendationConfigItem recommendationCpuLimits = recommendationCpuRequest;
                RecommendationConfigItem recommendationMemLimits = recommendationMemRequest;

                // Current CPU Request
                RecommendationConfigItem currentCpuRequest = getCurrentValue(filteredResultsMap,
                        timestampToExtract,
                        AnalyzerConstants.ResourceSetting.requests,
                        AnalyzerConstants.RecommendationItem.cpu,
                        notifications);

                // Current Memory Request
                RecommendationConfigItem currentMemRequest = getCurrentValue(filteredResultsMap,
                        timestampToExtract,
                        AnalyzerConstants.ResourceSetting.requests,
                        AnalyzerConstants.RecommendationItem.memory,
                        notifications);

                // Current CPU Limit
                RecommendationConfigItem currentCpuLimit = getCurrentValue(filteredResultsMap,
                        timestampToExtract,
                        AnalyzerConstants.ResourceSetting.limits,
                        AnalyzerConstants.RecommendationItem.cpu,
                        notifications);

                // Current Memory Limit
                RecommendationConfigItem currentMemLimit = getCurrentValue(filteredResultsMap,
                        timestampToExtract,
                        AnalyzerConstants.ResourceSetting.limits,
                        AnalyzerConstants.RecommendationItem.memory,
                        notifications);

                // Create an internal map to send data to populate
                HashMap<String, RecommendationConfigItem> internalMapToPopulate = new HashMap<String, RecommendationConfigItem>();
                // Add current values
                internalMapToPopulate.put(RecommendationConstants.RecommendationEngine.InternalConstants.CURRENT_CPU_REQUEST, currentCpuRequest);
                internalMapToPopulate.put(RecommendationConstants.RecommendationEngine.InternalConstants.CURRENT_CPU_LIMIT, currentCpuLimit);
                internalMapToPopulate.put(RecommendationConstants.RecommendationEngine.InternalConstants.CURRENT_MEMORY_REQUEST, currentMemRequest);
                internalMapToPopulate.put(RecommendationConstants.RecommendationEngine.InternalConstants.CURRENT_MEMORY_LIMIT, currentMemLimit);
                // Add recommended values
                internalMapToPopulate.put(RecommendationConstants.RecommendationEngine.InternalConstants.RECOMMENDED_CPU_REQUEST, recommendationCpuRequest);
                internalMapToPopulate.put(RecommendationConstants.RecommendationEngine.InternalConstants.RECOMMENDED_CPU_LIMIT, recommendationCpuLimits);
                internalMapToPopulate.put(RecommendationConstants.RecommendationEngine.InternalConstants.RECOMMENDED_MEMORY_REQUEST, recommendationMemRequest);
                internalMapToPopulate.put(RecommendationConstants.RecommendationEngine.InternalConstants.RECOMMENDED_MEMORY_LIMIT, recommendationMemLimits);

                // Call the populate method to validate and populate the recommendation object
                boolean isSuccess = populateRecommendation(
                        recPeriod,
                        recommendation,
                        notifications,
                        internalMapToPopulate,
                        numPods,
                        hours,
                        cpuThreshold,
                        memoryThreshold
                );

                // Use success in future if you want to avoid sending the recommendations
                if (!isSuccess) {
                    // Handle the issue accordingly

                    // Currently, it's unused as we are adding notification for every error we encounter
                    // Also stopping the recommendation process is not advisable as the API USER need to know it via
                    // Notifications and check the logs for what went wrong
                }

                // Iterate over notifications and set to recommendations
                for (RecommendationNotification recommendationNotification : notifications) {
                    recommendation.addNotification(recommendationNotification);
                }

                // Set Recommendations
                resultRecommendation.put(recPeriod, recommendation);
            } else {
                RecommendationNotification notification = new RecommendationNotification(
                        RecommendationConstants.RecommendationNotification.INFO_NOT_ENOUGH_DATA);
                resultRecommendation.put(recPeriod, new Recommendation(notification));
            }
        }
        return resultRecommendation;
    }

    @Override
    public void validateRecommendations() {

    }

    @Override
    public boolean checkIfMinDataAvailable(ContainerData containerData) {
        // Check if data available
        if (null == containerData || null == containerData.getResults() || containerData.getResults().isEmpty()) {
            return false;
        }
        // Initiate to the first sub category available
        DurationBasedRecommendationSubCategory categoryToConsider = (DurationBasedRecommendationSubCategory) this.category.getRecommendationSubCategories()[0];
        // Loop over categories to set the least category
        for (RecommendationSubCategory recommendationSubCategory : this.category.getRecommendationSubCategories()) {
            DurationBasedRecommendationSubCategory durationBasedRecommendationSubCategory = (DurationBasedRecommendationSubCategory) recommendationSubCategory;
            if (durationBasedRecommendationSubCategory.getDuration() < categoryToConsider.getDuration()) {
                categoryToConsider = durationBasedRecommendationSubCategory;
            }
        }
        // Set bounds to check if we get minimum requirement satisfied
        double lowerBound = categoryToConsider.getGetDurationLowerBound();
        double sum = 0.0;
        // Loop over the data to check if there is min data available
        for (IntervalResults intervalResults : containerData.getResults().values()) {
            sum = sum + intervalResults.getDurationInMinutes();
            // We don't consider upper bound to check if sum is in-between as we may over shoot and end-up resulting false
            if (sum >= lowerBound)
                return true;
        }
        return false;
    }

    private static Timestamp calculateThresholdTimeBasedOnTerm(DurationBasedRecommendationSubCategory durationBasedRecommendationSubCategory, Timestamp endTime) {
        // Check for null
        if (null == durationBasedRecommendationSubCategory || null == endTime)
            return null;
        // Initialise threshold time
        Timestamp thresholdTime = null;

        // Extract the duration as count
        int count = durationBasedRecommendationSubCategory.getDuration();
        // Extract units
        TimeUnit units = durationBasedRecommendationSubCategory.getRecommendationDurationUnits();

        // Assuming units is hours by default
        int totalDurationInHrs = count;

        // Checking if it's days
        if (units == TimeUnit.DAYS) {
            totalDurationInHrs = count * KruizeConstants.TimeConv.NO_OF_HOURS_PER_DAY;
        }
        // TODO: Add checks for other timeunits like minutes, weeks & months if needed later

        // Add Threshold based on term
        if (durationBasedRecommendationSubCategory.getSubCategory().equalsIgnoreCase(KruizeConstants.JSONKeys.SHORT_TERM))
            totalDurationInHrs = totalDurationInHrs + THRESHOLD_HRS_SHORT_TERM;
        else if (durationBasedRecommendationSubCategory.getSubCategory().equalsIgnoreCase(KruizeConstants.JSONKeys.MEDIUM_TERM))
            totalDurationInHrs = totalDurationInHrs + THRESHOLD_HRS_MEDIUM_TERM;
        else if (durationBasedRecommendationSubCategory.getSubCategory().equalsIgnoreCase(KruizeConstants.JSONKeys.LONG_TERM))
            totalDurationInHrs = totalDurationInHrs + THRESHOLD_HRS_LONG_TERM;

        // Remove the number of hours from end time
        long endTimeMillis = endTime.getTime();
        long startTimeMillis = endTimeMillis - TimeUnit.HOURS.toMillis(totalDurationInHrs);

        thresholdTime = new Timestamp(startTimeMillis);

        return thresholdTime;
    }
}
