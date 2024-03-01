package com.autotune.analyzer.recommendations.utils;

import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.recommendations.RecommendationNotification;
import com.autotune.analyzer.recommendations.subCategory.CostRecommendationSubCategory;
import com.autotune.analyzer.recommendations.subCategory.RecommendationSubCategory;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.utils.KruizeConstants;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

public class RecommendationUtils {
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
                                            ArrayList<RecommendationConstants.RecommendationNotification> notifications) {
        // Check notifications is null, If it's null -> return.
        if (null == notifications)
            return;
        // Check if the item is CPU
        if (recommendationItem == AnalyzerConstants.RecommendationItem.cpu) {
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
        else if (recommendationItem == AnalyzerConstants.RecommendationItem.memory) {
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

    public static boolean checkIfMinDataAvailableForTerm(ContainerData containerData, RecommendationConstants.RecommendationTerms recommendationTerms) {
        // Check if data available
        if (null == containerData || null == containerData.getResults() || containerData.getResults().isEmpty()) {
            return false;
        }

        // Set bounds to check if we get minimum requirement satisfied
        double lowerBound = recommendationTerms.getLowerBound();
        double sum = getDurationSummation(containerData);
        // We don't consider upper bound to check if sum is in-between as we may over shoot and end-up resulting false
        if (sum >= lowerBound)
            return true;

        return false;
    }

    public static double getDurationSummation(ContainerData containerData) {
        // Loop over the data to check if there is min data available
        double sum = 0.0;
        for (IntervalResults intervalResults : containerData.getResults().values()) {
            sum = sum + intervalResults.getDurationInMinutes();
        }
        return sum;
    }

    public static Timestamp getMonitoringStartTime(Timestamp endTime,
                                                   Double durationInHrs) {

        Timestamp intervalEndTime;
        try {
            // Convert Timestamp to LocalDateTime
            LocalDateTime localDateTime = endTime.toLocalDateTime();
            long maxTermDuration = (long) durationInHrs.doubleValue();
            // Subtract hours
            LocalDateTime newLocalDateTime = localDateTime.minusHours(maxTermDuration);
            // Convert back to Timestamp
            intervalEndTime = Timestamp.valueOf(newLocalDateTime);
            return intervalEndTime;
        } catch (NullPointerException npe) {
            return null;
        }
    }

    public static RecommendationNotification getNotificationForTermAvailability(RecommendationConstants.RecommendationTerms recommendationTerm) {
        RecommendationNotification recommendationNotification = null;
        if (recommendationTerm.getValue().equalsIgnoreCase(RecommendationConstants.RecommendationTerms.SHORT_TERM.getValue())) {
            recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.INFO_SHORT_TERM_RECOMMENDATIONS_AVAILABLE);
        } else if (recommendationTerm.getValue().equalsIgnoreCase(RecommendationConstants.RecommendationTerms.MEDIUM_TERM.getValue())) {
            recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.INFO_MEDIUM_TERM_RECOMMENDATIONS_AVAILABLE);
        } else if (recommendationTerm.getValue().equalsIgnoreCase(RecommendationConstants.RecommendationTerms.LONG_TERM.getValue())) {
            recommendationNotification = new RecommendationNotification(RecommendationConstants.RecommendationNotification.INFO_LONG_TERM_RECOMMENDATIONS_AVAILABLE);
        }
        return recommendationNotification;
    }


}

