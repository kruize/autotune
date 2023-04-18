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
import com.autotune.analyzer.recommendations.algos.DurationBasedRecommendationSubCategory;
import com.autotune.analyzer.recommendations.algos.RecommendationSubCategory;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.common.utils.CommonUtils;
import com.autotune.utils.KruizeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public HashMap<String, Recommendation> getRecommendations(ContainerData containerData, Timestamp monitoringEndTime) {
        // Get the results
        HashMap<Timestamp, IntervalResults> resultsMap = containerData.getResults();
        // Create a new map for returning the result
        HashMap<String, Recommendation> resultRecommendation = new HashMap<String, Recommendation>();
        for (RecommendationSubCategory recommendationSubCategory : this.category.getRecommendationSubCategories()) {
            DurationBasedRecommendationSubCategory durationBasedRecommendationSubCategory = (DurationBasedRecommendationSubCategory) recommendationSubCategory;
            String recPeriod = durationBasedRecommendationSubCategory.getSubCategory();
            int days = durationBasedRecommendationSubCategory.getDuration();
            Timestamp monitorStartTime = getMonitoringStartTime(resultsMap,
                                                                durationBasedRecommendationSubCategory,
                                                                monitoringEndTime);
            if (null != monitorStartTime) {
                Timestamp finalMonitorStartDate = monitorStartTime;
                Map<Timestamp, IntervalResults> filteredResultsMap = containerData.getResults().entrySet().stream()
                        .filter((x -> ((x.getKey().compareTo(finalMonitorStartDate) >= 0)
                                && (x.getKey().compareTo(monitoringEndTime) <= 0))))
                        .collect((Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
                Recommendation recommendation = new Recommendation(monitorStartTime, monitoringEndTime);
                HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> config = new HashMap<>();
                // Create Request Map
                HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requestsMap = new HashMap<>();
                // Get the Recommendation Items
                RecommendationConfigItem cpuResquestItem = getCPURequestRecommendation(filteredResultsMap);
                RecommendationConfigItem memRequestItem = getMemoryRequestRecommendation(filteredResultsMap);
                // Initiate generated value holders with min values constants to compare later
                Double generatedCpuRequest = null;
                String generatedCpuRequestFormat = null;
                Double generatedMemRequest = null;
                String generatedMemRequestFormat = null;

                // Check for null
                if (null != cpuResquestItem) {
                    generatedCpuRequest = cpuResquestItem.getAmount();
                    generatedCpuRequestFormat = cpuResquestItem.getFormat();
                    requestsMap.put(AnalyzerConstants.RecommendationItem.cpu, cpuResquestItem);
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
                RecommendationConfigItem cpuLimitsItem =  cpuResquestItem;
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
                double hours = days * KruizeConstants.TimeConv.NO_OF_HOURS_PER_DAY;
                // Set Duration in hours
                recommendation.setDuration_in_hours(hours);
                // Set Config
                recommendation.setConfig(config);

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

    private static RecommendationConfigItem getCPURequestRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap) {
        RecommendationConfigItem recommendationConfigItem = null;
        String format = "";
        try {
            List<Double> doubleList = filteredResultsMap.values()
                    .stream()
                    .map(e -> e.getMetricResultsMap().get(AnalyzerConstants.MetricName.cpuUsage).getAggregationInfoResult().getSum()
                            + e.getMetricResultsMap().get(AnalyzerConstants.MetricName.cpuThrottle).getAggregationInfoResult().getSum())
                    .collect(Collectors.toList());

            for (IntervalResults intervalResults: filteredResultsMap.values()) {
                format = intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.cpuUsage).getAggregationInfoResult().getFormat();
                if (null != format && !format.isEmpty())
                    break;
            }
            recommendationConfigItem = new RecommendationConfigItem(CommonUtils.percentile(98, doubleList), format);

        } catch (Exception e) {
            LOGGER.error("Not able to get getCPUCapacityRecommendation: " + e.getMessage());
            recommendationConfigItem = new RecommendationConfigItem(e.getMessage());
        }
        return recommendationConfigItem;
    }

    private static RecommendationConfigItem getCPULimitRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap) {
        RecommendationConfigItem recommendationConfigItem = null;
        String format = "";
        try {
            Double max_cpu = filteredResultsMap.values()
                    .stream()
                    .map(e -> e.getMetricResultsMap().get(AnalyzerConstants.MetricName.cpuUsage).getAggregationInfoResult().getMax()
                            + e.getMetricResultsMap().get(AnalyzerConstants.MetricName.cpuThrottle).getAggregationInfoResult().getMax())
                    .max(Double::compareTo).get();
            Double max_pods = filteredResultsMap.values()
                    .stream()
                    .map(e -> e.getMetricResultsMap().get(AnalyzerConstants.MetricName.cpuUsage).getAggregationInfoResult().getSum()
                            / e.getMetricResultsMap().get(AnalyzerConstants.MetricName.cpuUsage).getAggregationInfoResult().getAvg())
                    .max(Double::compareTo).get();
            for (IntervalResults intervalResults: filteredResultsMap.values()) {
                format = intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.cpuUsage).getAggregationInfoResult().getFormat();
                if (null != format && !format.isEmpty())
                    break;
            }
            recommendationConfigItem = new RecommendationConfigItem(max_cpu * max_pods, format);
            LOGGER.debug("Max_cpu : {} , max_pods : {}", max_cpu, max_pods);
        } catch (Exception e) {
            LOGGER.error("Not able to get getCPUMaxRecommendation: " + e.getMessage());
            recommendationConfigItem = new RecommendationConfigItem(e.getMessage());
        }
        return recommendationConfigItem;
    }

    private static RecommendationConfigItem getMemoryRequestRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap) {
        RecommendationConfigItem recommendationConfigItem = null;
        String format = "";
        try {
            List<Double> doubleList = filteredResultsMap.values()
                    .stream()
                    .map(e -> e.getMetricResultsMap().get(AnalyzerConstants.MetricName.memoryRSS).getAggregationInfoResult().getSum())
                    .collect(Collectors.toList());
            for (IntervalResults intervalResults: filteredResultsMap.values()) {
                format = intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.memoryRSS).getAggregationInfoResult().getFormat();
                if (null != format && !format.isEmpty())
                    break;
            }
            recommendationConfigItem = new RecommendationConfigItem(CommonUtils.percentile(100, doubleList), format);
        } catch (Exception e) {
            LOGGER.error("Not able to get getMemoryCapacityRecommendation: " + e.getMessage());
            recommendationConfigItem = new RecommendationConfigItem(e.getMessage());
        }
        return recommendationConfigItem;
    }

    private static RecommendationConfigItem getMemoryLimitRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap) {
        RecommendationConfigItem recommendationConfigItem = null;
        String format = "";
        try {
            Double max_mem = filteredResultsMap.values()
                    .stream()
                    .map(e -> e.getMetricResultsMap().get(AnalyzerConstants.MetricName.memoryUsage).getAggregationInfoResult().getMax())
                    .max(Double::compareTo).get();
            Double max_pods = filteredResultsMap.values()
                    .stream()
                    .map(e -> e.getMetricResultsMap().get(AnalyzerConstants.MetricName.memoryUsage).getAggregationInfoResult().getSum()
                            / e.getMetricResultsMap().get(AnalyzerConstants.MetricName.memoryUsage).getAggregationInfoResult().getAvg())
                    .max(Double::compareTo).get();
            for (IntervalResults intervalResults: filteredResultsMap.values()) {
                format = intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.memoryUsage).getAggregationInfoResult().getFormat();
                if (null != format && !format.isEmpty())
                    break;
            }
            recommendationConfigItem = new RecommendationConfigItem(max_mem * max_pods, format);
            LOGGER.debug("Max_cpu : {} , max_pods : {}", max_mem, max_pods);
        } catch (Exception e) {
            LOGGER.error("Not able to get getCPUMaxRecommendation: " + e.getMessage());
            recommendationConfigItem = new RecommendationConfigItem(e.getMessage());
        }
        return recommendationConfigItem;
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
                if (intervalResults.getMetricResultsMap().containsKey(metricName))
                    currentValue = intervalResults.getMetricResultsMap().get(metricName).getAggregationInfoResult().getAvg();
                return currentValue;
            }
        }
        return currentValue;
    }
}
