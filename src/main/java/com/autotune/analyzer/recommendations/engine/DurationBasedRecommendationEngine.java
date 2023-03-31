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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
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
        this.name = AnalyzerConstants.RecommendationEngine.EngineNames.DURATION_BASED;
        this.key = AnalyzerConstants.RecommendationEngine.EngineKeys.DURATION_BASED_KEY;
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
        // TODO: Needs to be implemented
        AnalyzerConstants.RecommendationCategory recommendationCategory = AnalyzerConstants.RecommendationCategory.DURATION_BASED;
        HashMap<Timestamp, IntervalResults> resultsMap = containerData.getResults();
        Timestamp minDate = resultsMap.keySet().stream().min(Timestamp::compareTo).get();
        HashMap<String, Recommendation> resultRecommendation = new HashMap<String, Recommendation>();
        for (RecommendationSubCategory recommendationSubCategory : recommendationCategory.getRecommendationSubCategories()) {
            DurationBasedRecommendationSubCategory durationBasedRecommendationSubCategory = (DurationBasedRecommendationSubCategory) recommendationSubCategory;
            String recPeriod = durationBasedRecommendationSubCategory.getSubCategory();
            int days = durationBasedRecommendationSubCategory.getDuration();
            Timestamp monitorStartDate = CommonUtils.addDays(monitoringEndTime, -1 * days);
            if (monitorStartDate.compareTo(minDate) >= 0 || days == 1) {
                Timestamp finalMonitorStartDate = monitorStartDate;
                Map<Timestamp, IntervalResults> filteredResultsMap = containerData.getResults().entrySet().stream()
                        .filter((x -> ((x.getKey().compareTo(finalMonitorStartDate) >= 0)
                                && (x.getKey().compareTo(monitoringEndTime) <= 0))))
                        .collect((Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
                Recommendation recommendation = new Recommendation(monitorStartDate, monitoringEndTime);
                HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> config = new HashMap<>();
                HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requestsMap = new HashMap<>();
                requestsMap.put(AnalyzerConstants.RecommendationItem.cpu, getCPURequestRecommendation(filteredResultsMap));
                requestsMap.put(AnalyzerConstants.RecommendationItem.memory, getMemoryRequestRecommendation(filteredResultsMap));
                config.put(AnalyzerConstants.ResourceSetting.requests, requestsMap);
                HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> limitsMap = new HashMap<>();
                limitsMap.put(AnalyzerConstants.RecommendationItem.cpu, getCPULimitRecommendation(filteredResultsMap));
                limitsMap.put(AnalyzerConstants.RecommendationItem.memory, getMemoryLimitRecommendation(filteredResultsMap));
                config.put(AnalyzerConstants.ResourceSetting.limits, limitsMap);
                Double hours = filteredResultsMap.values().stream().map((x) -> (x.getDurationInMinutes()))
                        .collect(Collectors.toList())
                        .stream()
                        .mapToDouble(f -> f.doubleValue()).sum() / 60;
                recommendation.setDuration_in_hours(hours);
                recommendation.setConfig(config);
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
            recommendationConfigItem = new RecommendationConfigItem(CommonUtils.percentile(0.9, doubleList), format);

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
            recommendationConfigItem = new RecommendationConfigItem(CommonUtils.percentile(0.9, doubleList), format);
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
}
