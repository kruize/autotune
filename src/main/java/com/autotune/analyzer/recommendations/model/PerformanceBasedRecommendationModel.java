package com.autotune.analyzer.recommendations.model;

import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.recommendations.RecommendationNotification;
import com.autotune.analyzer.services.UpdateRecommendations;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.metrics.MetricAggregationInfoResults;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.common.utils.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import static com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationEngine.PercentileConstants.PERFORMANCE_CPU_PERCENTILE;
import static com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationEngine.PercentileConstants.PERFORMANCE_MEMORY_PERCENTILE;
import static com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationValueConstants.*;

public class PerformanceBasedRecommendationModel implements RecommendationModel {

    private int percentile;
    private String name;

    public PerformanceBasedRecommendationModel(int percentile) {
        this.percentile = percentile;
    }

    public PerformanceBasedRecommendationModel() {
        this.name = RecommendationConstants.RecommendationEngine.ModelNames.PERFORMANCE;
    }
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateRecommendations.class);

    @Override
    public RecommendationConfigItem getCPURequestRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap,
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
            cpuRequest = CommonUtils.percentile(PERFORMANCE_CPU_PERCENTILE, cpuUsageList);
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

    @Override
    public RecommendationConfigItem getMemoryRequestRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap,
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
        Double memRecUsage = CommonUtils.percentile(PERFORMANCE_MEMORY_PERCENTILE, memUsageList);
        Double memRecUsageBuf = memRecUsage + (memRecUsage * MEM_USAGE_BUFFER_DECIMAL);

        // Add a small buffer to the current usage spike max and add it to the current usage max
        Double memRecSpike = CommonUtils.percentile(PERFORMANCE_MEMORY_PERCENTILE, spikeList);
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

    @Override
    public String getModelName() {
        return this.name;
    }

    @Override
    public void validate() {

    }
}
