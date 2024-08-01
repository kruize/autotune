package com.autotune.analyzer.recommendations.utils;

import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.IntervalResults;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.*;

public class RecommendationUtils {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RecommendationUtils.class);
    public static RecommendationConfigItem getCurrentValueForNamespace(Map<Timestamp, IntervalResults> filteredResultsMap,
                                                           Timestamp timestampToExtract,
                                                           AnalyzerConstants.ResourceSetting resourceSetting,
                                                           AnalyzerConstants.RecommendationItem recommendationItem,
                                                           ArrayList<RecommendationConstants.RecommendationNotification> notifications) {
        Double currentValue = null;
        String format = null;
        RecommendationConfigItem recommendationConfigItem = null;
        AnalyzerConstants.MetricName metricName = null;
        for (Timestamp timestamp : filteredResultsMap.keySet()) {
            if (!timestamp.equals(timestampToExtract))
                continue;
            IntervalResults intervalResults = filteredResultsMap.get(timestamp);
            if (resourceSetting == AnalyzerConstants.ResourceSetting.requests) {
                if (recommendationItem == AnalyzerConstants.RecommendationItem.CPU)
                    metricName = AnalyzerConstants.MetricName.namespaceCpuRequest;
                if (recommendationItem == AnalyzerConstants.RecommendationItem.MEMORY)
                    metricName = AnalyzerConstants.MetricName.namespaceMemoryRequest;
            }
            if (resourceSetting == AnalyzerConstants.ResourceSetting.limits) {
                if (recommendationItem == AnalyzerConstants.RecommendationItem.CPU)
                    metricName = AnalyzerConstants.MetricName.namespaceCpuLimit;
                if (recommendationItem == AnalyzerConstants.RecommendationItem.MEMORY)
                    metricName = AnalyzerConstants.MetricName.namespaceMemoryLimit;
            }
            if (null != metricName) {
                if (intervalResults.getMetricResultsMap().containsKey(metricName)) {
                    Optional<MetricResults> metricResults = Optional.ofNullable(intervalResults.getMetricResultsMap().get(metricName));
                    currentValue = metricResults.map(m -> m.getAggregationInfoResult().getSum()).orElse(null);
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

    public static RecommendationConfigItem getCurrentValue(Map<Timestamp, IntervalResults> filteredResultsMap,
                                                           Timestamp timestampToExtract,
                                                           AnalyzerConstants.ResourceSetting resourceSetting,
                                                           AnalyzerConstants.RecommendationItem recommendationItem,
                                                           ArrayList<RecommendationConstants.RecommendationNotification> notifications) {
        Double currentValue = null;
        String format = null;
        RecommendationConfigItem recommendationConfigItem = null;
        AnalyzerConstants.MetricName metricName = null;
        for (Timestamp timestamp : filteredResultsMap.keySet()) {
            if (!timestamp.equals(timestampToExtract))
                continue;
            IntervalResults intervalResults = filteredResultsMap.get(timestamp);
            if (resourceSetting == AnalyzerConstants.ResourceSetting.requests) {
                if (recommendationItem == AnalyzerConstants.RecommendationItem.CPU)
                    metricName = AnalyzerConstants.MetricName.cpuRequest;
                if (recommendationItem == AnalyzerConstants.RecommendationItem.MEMORY)
                    metricName = AnalyzerConstants.MetricName.memoryRequest;
            }
            if (resourceSetting == AnalyzerConstants.ResourceSetting.limits) {
                if (recommendationItem == AnalyzerConstants.RecommendationItem.CPU)
                    metricName = AnalyzerConstants.MetricName.cpuLimit;
                if (recommendationItem == AnalyzerConstants.RecommendationItem.MEMORY)
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
                                            ArrayList<RecommendationConstants.RecommendationNotification> notifications) {
        // Check notifications is null, If it's null -> return.
        if (null == notifications)
            return;
        // Check if the item is CPU
        if (recommendationItem == AnalyzerConstants.RecommendationItem.CPU) {
            // Check if the setting is REQUESTS
            if (resourceSetting == AnalyzerConstants.ResourceSetting.requests) {
                notifications.add(
                        RecommendationConstants.RecommendationNotification.CRITICAL_CPU_REQUEST_NOT_SET
                );
            }
            // Check if the setting is LIMITS
            else if (resourceSetting == AnalyzerConstants.ResourceSetting.limits) {
                notifications.add(
                        RecommendationConstants.RecommendationNotification.WARNING_CPU_LIMIT_NOT_SET
                );
            }
        }
        // Check if the item is Memory
        else if (recommendationItem == AnalyzerConstants.RecommendationItem.MEMORY) {
            // Check if the setting is REQUESTS
            if (resourceSetting == AnalyzerConstants.ResourceSetting.requests) {
                notifications.add(
                        RecommendationConstants.RecommendationNotification.CRITICAL_MEMORY_REQUEST_NOT_SET
                );
            }
            // Check if the setting is LIMITS
            else if (resourceSetting == AnalyzerConstants.ResourceSetting.limits) {
                notifications.add(
                        RecommendationConstants.RecommendationNotification.CRITICAL_MEMORY_LIMIT_NOT_SET
                );
            }
        }
    }


}

