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

import com.autotune.analyzer.recommendations.Recommendation;
import com.autotune.analyzer.recommendations.RecommendationConfigItem;
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
import java.util.stream.Collectors;

import static com.autotune.analyzer.utils.AnalyzerConstants.RecommendationConstants.*;

public class DurationBasedRecommendationEngine implements KruizeRecommendationEngine{
    private static final Logger LOGGER = LoggerFactory.getLogger(DurationBasedRecommendationEngine.class);
    private String name;
    private String key;
    private AnalyzerConstants.RecommendationCategory category;

    public DurationBasedRecommendationEngine() {
        this.name           = AnalyzerConstants.RecommendationEngine.EngineNames.DURATION_BASED;
        this.key            = AnalyzerConstants.RecommendationEngine.EngineKeys.DURATION_BASED_KEY;
        this.category       = AnalyzerConstants.RecommendationCategory.DURATION_BASED;
    }

    public DurationBasedRecommendationEngine(String name) {
        this.name = name;
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
    public AnalyzerConstants.RecommendationCategory getEngineCategory() {
        return this.category;
    }

    @Override
    public HashMap<String, Recommendation> generateRecommendation(ContainerData containerData, Timestamp monitoringEndTime) {
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
                Map<Timestamp, IntervalResults> filteredResultsMap = containerData.getResults().entrySet().stream()
                        .filter((x -> ((x.getKey().compareTo(finalMonitoringStartTime) >= 0)
                                && (x.getKey().compareTo(monitoringEndTime) <= 0))))
                        .collect((Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
                Recommendation recommendation = new Recommendation(monitoringStartTime, monitoringEndTime);
                HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> config = new HashMap<>();
                // Create Request Map
                HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requestsMap = new HashMap<>();
                // Get the Recommendation Items
                RecommendationConfigItem cpuRequestItem = getCPURequestRecommendation(filteredResultsMap);
                RecommendationConfigItem memRequestItem = getMemoryRequestRecommendation(filteredResultsMap);
                // Initiate generated value holders with min values constants to compare later
                Double generatedCpuRequest = null;
                String generatedCpuRequestFormat = null;
                Double generatedMemRequest = null;
                String generatedMemRequestFormat = null;

                // Check for null
                if (null != cpuRequestItem) {
                    generatedCpuRequest = cpuRequestItem.getAmount();
                    generatedCpuRequestFormat = cpuRequestItem.getFormat();
                    requestsMap.put(AnalyzerConstants.RecommendationItem.cpu, cpuRequestItem);
                }
                // Check for null
                if (null != memRequestItem) {
                    generatedMemRequest = memRequestItem.getAmount();
                    generatedMemRequestFormat = memRequestItem.getFormat();
                    requestsMap.put(AnalyzerConstants.RecommendationItem.memory, memRequestItem);
                }

                // Set Request Map
                config.put(AnalyzerConstants.ResourceSetting.requests, requestsMap);

                // Create Limits Map
                HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> limitsMap = new HashMap<>();
                // Get the Recommendation Items
                // Calling requests on limits as we are maintaining limits and requests as same
                // Maintaining different flow for both of them even though if they are same as in future we might have
                // a different implementation for both and this avoids confusion
                RecommendationConfigItem cpuLimitsItem =  cpuRequestItem;
                RecommendationConfigItem memLimitsItem = memRequestItem;
                // Initiate generated value holders with min values constants to compare later
                Double generatedCpuLimit = null;
                String generatedCpuLimitFormat = null;
                Double generatedMemLimit = null;
                String generatedMemLimitFormat = null;

                // Check for null
                if (null != cpuLimitsItem) {
                    generatedCpuLimit = cpuLimitsItem.getAmount();
                    generatedCpuLimitFormat = cpuLimitsItem.getFormat();
                    limitsMap.put(AnalyzerConstants.RecommendationItem.cpu, cpuLimitsItem);
                }

                // Check for null
                if (null != memLimitsItem) {
                    generatedMemLimit = memLimitsItem.getAmount();
                    generatedMemLimitFormat = memLimitsItem.getFormat();
                    limitsMap.put(AnalyzerConstants.RecommendationItem.memory, memLimitsItem);
                }

                // Set Limits Map
                config.put(AnalyzerConstants.ResourceSetting.limits, limitsMap);
                // Set Config
                recommendation.setConfig(config);

                // Set number of pods
                int numPods = getNumPods(filteredResultsMap);
                recommendation.setPodsCount(numPods);

                // Set Duration in hours
                double hours = days * KruizeConstants.TimeConv.NO_OF_HOURS_PER_DAY;
                recommendation.setDuration_in_hours(hours);

                Timestamp timestampToExtract = monitoringEndTime;
                // Create variation map
                HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> variation = new HashMap<>();
                // Create a new map for storing variation in requests
                HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requestsVariationMap = new HashMap<>();
                Double currentCpuRequest = getCurrentValue( filteredResultsMap,
                                                            timestampToExtract,
                                                            AnalyzerConstants.ResourceSetting.requests,
                                                            AnalyzerConstants.RecommendationItem.cpu);
                Double currentMemRequest = getCurrentValue( filteredResultsMap,
                                                            timestampToExtract,
                                                            AnalyzerConstants.ResourceSetting.requests,
                                                            AnalyzerConstants.RecommendationItem.memory);

                if (null == currentCpuRequest) {
                    RecommendationNotification notification = new RecommendationNotification(
                            AnalyzerConstants.RecommendationNotificationTypes.CRITICAL.getName(),
                            AnalyzerConstants.RecommendationNotificationMsgConstant.CPU_REQUEST_NOT_SET);
                    recommendation.addNotification(notification);
                } else if (null != generatedCpuRequest
                            && null != generatedCpuRequestFormat) {
                    double diff = generatedCpuRequest - currentCpuRequest;
                    // TODO: If difference is positive it can be considered as under-provisioning, Need to handle it better
                    RecommendationConfigItem recommendationConfigItem = new RecommendationConfigItem(diff, generatedCpuRequestFormat);
                    requestsVariationMap.put(AnalyzerConstants.RecommendationItem.cpu, recommendationConfigItem);
                }

                if (null == currentMemRequest) {
                    RecommendationNotification notification = new RecommendationNotification(
                            AnalyzerConstants.RecommendationNotificationTypes.CRITICAL.getName(),
                            AnalyzerConstants.RecommendationNotificationMsgConstant.MEMORY_REQUEST_NOT_SET);
                    recommendation.addNotification(notification);
                } else if (null != generatedMemRequest
                            && null != generatedMemRequestFormat) {
                    double diff = generatedMemRequest - currentMemRequest;
                    // TODO: If difference is positive it can be considered as under-provisioning, Need to handle it better
                    RecommendationConfigItem recommendationConfigItem = new RecommendationConfigItem(diff, generatedMemRequestFormat);
                    requestsVariationMap.put(AnalyzerConstants.RecommendationItem.memory, recommendationConfigItem);
                }

                // Set Request variation map
                variation.put(AnalyzerConstants.ResourceSetting.requests, requestsVariationMap);

                // Create a new map for storing variation in limits
                HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> limitsVariationMap = new HashMap<>();

                // Calling requests on limits as we are maintaining limits and requests as same
                // Maintaining different flow for both of them even though if they are same as in future we might have
                // a different implementation for both and this avoids confusion
                Double currentCpuLimit = currentCpuRequest;
                Double currentMemLimit = currentMemRequest;

                if (null != currentCpuLimit
                        && null != generatedCpuLimit
                        && null != generatedCpuLimitFormat) {
                    double diff = generatedCpuLimit - currentCpuLimit;
                    RecommendationConfigItem recommendationConfigItem = new RecommendationConfigItem(diff, generatedCpuLimitFormat);
                    limitsVariationMap.put(AnalyzerConstants.RecommendationItem.cpu, recommendationConfigItem);
                }

                if (null == currentMemLimit) {
                    RecommendationNotification notification = new RecommendationNotification(
                            AnalyzerConstants.RecommendationNotificationTypes.CRITICAL.getName(),
                            AnalyzerConstants.RecommendationNotificationMsgConstant.MEMORY_LIMIT_NOT_SET);
                    recommendation.addNotification(notification);
                } else if (null != generatedMemLimit
                        && null != generatedMemLimitFormat) {
                    double diff = generatedMemLimit - currentMemLimit;
                    RecommendationConfigItem recommendationConfigItem = new RecommendationConfigItem(diff, generatedMemLimitFormat);
                    limitsVariationMap.put(AnalyzerConstants.RecommendationItem.memory, recommendationConfigItem);
                }

                // Set Limits variation map
                variation.put(AnalyzerConstants.ResourceSetting.limits, limitsMap);

                recommendation.setVariation(variation);

                // Set Recommendations
                resultRecommendation.put(recPeriod, recommendation);
            } else {
                RecommendationNotification notification = new RecommendationNotification(
                        AnalyzerConstants.RecommendationNotificationTypes.INFO.getName(),
                        AnalyzerConstants.RecommendationNotificationMsgConstant.NOT_ENOUGH_DATA);
                resultRecommendation.put(recPeriod, new Recommendation(notification));
            }
        }
        return resultRecommendation;
    }

    @Override
    public boolean checkIfMinDataAvailable(ContainerData containerData) {
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
        // Loop over the data to check if there is
        for (IntervalResults intervalResults: containerData.getResults().values()) {
            sum = sum + intervalResults.getDurationInMinutes();
            // We don't consider upper bound to check if sum is in-between as we may over shoot and end-up resulting false
            if (sum >= lowerBound)
                return true;
        }
        return false;
    }

    private static Timestamp getMonitoringStartTime(HashMap<Timestamp, IntervalResults> resultsHashMap,
                                                    DurationBasedRecommendationSubCategory durationBasedRecommendationSubCategory,
                                                    Timestamp endTime) {
        double sum = 0.0;
        // As we cannot sort HashSet
        List<Timestamp> timestampList = new ArrayList<Timestamp>(resultsHashMap.keySet());
        // Sort the time stamps in descending order
        timestampList.sort((t1, t2) -> t2.compareTo(t1));
        for (Timestamp timestamp: timestampList) {
            if (sum >= durationBasedRecommendationSubCategory.getGetDurationLowerBound()) {
                return timestamp;
            }
            if (timestamp.equals(endTime) || timestamp.before(endTime)) {
                if (resultsHashMap.containsKey(timestamp)) {
                    sum = sum + resultsHashMap.get(timestamp).getDurationInMinutes();
                }
            }
        }
        return null;
    }

    /**
     * Calculate the number of pods being used as per the latest results
     *
     * pods are calculated independently based on both the CPU and Memory usage results.
     * The max of both is then returned
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

    private static RecommendationConfigItem getCPURequestRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap) {
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
                    double cpuRequest = 0.0;
                    double cpuUsagePod = 0;
                    int numPods = 0;

                    // Use the Max value when available, if not use the Avg
                    double cpuUsage = (cpuUsageMax>0)?cpuUsageMax:cpuUsageAvg;
                    double cpuThrottle = (cpuThrottleMax>0)?cpuThrottleMax:cpuThrottleAvg;
                    double cpuUsageTotal = cpuUsage + cpuThrottle;

                    // Usage is less than 1 core, set it to the observed value.
                    if (ONE_CPU_CORE > cpuUsageTotal) {
                        cpuRequest = cpuUsageTotal;
                    } else {
                        // Sum/Avg should give us the number of pods
                        if (0 != cpuUsageAvg) {
                            numPods = (int) Math.ceil(cpuUsageSum / cpuUsageAvg);
                            if (numPods > 0) {
                                cpuUsagePod = (cpuUsageSum + cpuThrottleSum) / numPods;
                            }
                        }
                        cpuRequest = Math.max(cpuUsagePod, cpuUsageTotal);
                    }
                    return cpuRequest;
                })
                .collect(Collectors.toList());

        for (IntervalResults intervalResults: filteredResultsMap.values()) {
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

        recommendationConfigItem = new RecommendationConfigItem(CommonUtils.percentile(CPU_USAGE_PERCENTILE, cpuUsageList), format);
        return recommendationConfigItem;
    }

    private static RecommendationConfigItem getCPULimitRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap) {
        // This method is not used for now
        return null;
    }

    private static RecommendationConfigItem getMemoryRequestRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap) {
        RecommendationConfigItem recommendationConfigItem = null;
        String format = "";
        List<Double> doubleList = filteredResultsMap.values()
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
                    if (numPods > 0) {
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
        Double memRecUsage = CommonUtils.percentile(MEM_USAGE_PERCENTILE, doubleList);
        Double memRecUsageBuf = memRecUsage + (memRecUsage * MEM_USAGE_BUFFER_DECIMAL);

        // Add a small buffer to the current usage spike max and add it to the current usage max
        Double memRecSpike = CommonUtils.percentile(MEM_USAGE_PERCENTILE, spikeList);
        memRecSpike += (memRecSpike * MEM_SPIKE_BUFFER_DECIMAL);
        Double memRecSpikeBuf = memRecUsage + memRecSpike;

        // We'll use the minimum of the above two values
        Double memRec = Math.min(memRecUsageBuf, memRecSpikeBuf);

        for (IntervalResults intervalResults: filteredResultsMap.values()) {
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

    private static Double getCurrentValue(Map<Timestamp, IntervalResults> filteredResultsMap,
                                          Timestamp timestampToExtract,
                                          AnalyzerConstants.ResourceSetting resourceSetting,
                                          AnalyzerConstants.RecommendationItem recommendationItem) {
        Double currentValue = null;
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
                    Optional<MetricResults>  metricResults = Optional.ofNullable(intervalResults.getMetricResultsMap().get(metricName));
                    currentValue = metricResults.map(m -> m.getAggregationInfoResult().getAvg()).orElse(null);
                }
                return currentValue;
            }
        }
        return null;
    }
}
