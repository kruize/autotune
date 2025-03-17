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
import com.autotune.utils.KruizeConstants;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Stream;

import static com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationEngine.PercentileConstants.PERFORMANCE_CPU_PERCENTILE;
import static com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationValueConstants.*;

public class GenericRecommendationModel implements RecommendationModel{

    protected int percentile;
    protected String name;

    // constructor definition
    public GenericRecommendationModel(int percentile, String name) {
        this.percentile = percentile;
        this.name = name;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateRecommendations.class);


    @Override
    public RecommendationConfigItem getCPURequestRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap, ArrayList<RecommendationNotification> notifications) {
        boolean setNotification = true;
        if (null == notifications) {
            LOGGER.error("Notifications Object passed is empty. The notifications are not sent as part of recommendation.");
            setNotification = false;
        }

        RecommendationConfigItem recommendationConfigItem = null;
        String format = "";
        JSONArray cpuUsageList = getCPUUsageList(filteredResultsMap);
        LOGGER.debug("cpuUsageList : {}", cpuUsageList);


        // Extract "max" values from cpuUsageList
        List<Double> cpuMaxValues = new ArrayList<>();
        for (int i = 0; i < cpuUsageList.length(); i++) {
            JSONObject jsonObject = cpuUsageList.getJSONObject(i);
            double maxValue = jsonObject.getDouble(KruizeConstants.JSONKeys.MAX);
            cpuMaxValues.add(maxValue);
        }


        int INPUT_MODEL_CPU_PERCENTILE = percentile;

        Double cpuRequest = 0.0;
        Double cpuRequestMax = Collections.max(cpuMaxValues);
        if (null != cpuRequestMax && CPU_ONE_CORE > cpuRequestMax) {
            cpuRequest = cpuRequestMax;
        } else {
            cpuRequest = CommonUtils.percentile(INPUT_MODEL_CPU_PERCENTILE, cpuMaxValues);
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

        format = getFormatValue(filteredResultsMap, AnalyzerConstants.MetricName.cpuUsage);

        recommendationConfigItem = new RecommendationConfigItem(cpuRequest, format);
        return recommendationConfigItem;
    }

    // helper function common to both cost and performance model hence just taken from there.
    public static JSONArray getCPUUsageList(Map<Timestamp, IntervalResults> filteredResultsMap) {
        JSONArray cpuRequestIntervalArray = new JSONArray();
        for (IntervalResults intervalResults : filteredResultsMap.values()) {
            JSONObject cpuRequestInterval = new JSONObject();
            Optional<MetricResults> cpuUsageResults = Optional.ofNullable(intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.cpuUsage));
            Optional<MetricResults> cpuThrottleResults = Optional.ofNullable(intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.cpuThrottle));
            double cpuUsageAvg = cpuUsageResults.map(m -> m.getAggregationInfoResult().getAvg()).orElse(0.0);
            double cpuUsageMax = cpuUsageResults.map(m -> m.getAggregationInfoResult().getMax()).orElse(0.0);
            double cpuUsageSum = cpuUsageResults.map(m -> m.getAggregationInfoResult().getSum()).orElse(0.0);
            double cpuUsageMin = cpuUsageResults.map(m -> m.getAggregationInfoResult().getMin()).orElse(0.0);
            double cpuThrottleAvg = cpuThrottleResults.map(m -> m.getAggregationInfoResult().getAvg()).orElse(0.0);
            double cpuThrottleMax = cpuThrottleResults.map(m -> m.getAggregationInfoResult().getMax()).orElse(0.0);
            double cpuThrottleSum = cpuThrottleResults.map(m -> m.getAggregationInfoResult().getSum()).orElse(0.0);
            double cpuThrottleMin = cpuThrottleResults.map(m -> m.getAggregationInfoResult().getMin()).orElse(0.0);

            double cpuRequestIntervalMax;
            double cpuRequestIntervalMin;
            double cpuUsagePod = 0;
            int numPods;

            // Use the Max value when available, if not use the Avg
            double cpuUsage = (cpuUsageMax > 0) ? cpuUsageMax : cpuUsageAvg;
            double cpuThrottle = (cpuThrottleMax > 0) ? cpuThrottleMax : cpuThrottleAvg;
            double cpuUsageTotal = cpuUsage + cpuThrottle;

            // Usage is less than 1 core, set it to the observed value.
            if (CPU_ONE_CORE > cpuUsageTotal) {
                cpuRequestIntervalMax = cpuUsageTotal;
            } else {
                // Sum/Avg should give us the number of pods
                if (0 != cpuUsageAvg) {
                    numPods = (int) Math.ceil(cpuUsageSum / cpuUsageAvg);
                    if (0 < numPods) {
                        cpuUsagePod = (cpuUsageSum + cpuThrottleSum) / numPods;
                    }
                }
                cpuRequestIntervalMax = Math.max(cpuUsagePod, cpuUsageTotal);
            }
            double cpuMinTotal = cpuUsageMin + cpuThrottleMin;
            // traverse over a stream of positive values and find the minimum value
            cpuRequestIntervalMin = Stream.of(cpuUsagePod, cpuUsageTotal, cpuMinTotal)
                    .filter(value -> value > 0.0)
                    .min(Double::compare)
                    .orElse(0.0);

            cpuRequestInterval.put(KruizeConstants.JSONKeys.MIN, cpuRequestIntervalMin);
            cpuRequestInterval.put(KruizeConstants.JSONKeys.MAX, cpuRequestIntervalMax);
            LOGGER.debug("cpuRequestInterval : {}", cpuRequestInterval);
            cpuRequestIntervalArray.put(cpuRequestInterval);
        }
        return cpuRequestIntervalArray;
    }

    // helper function to get format value
    public static String getFormatValue(Map<Timestamp, IntervalResults> filteredResultsMap, AnalyzerConstants.MetricName metricName) {
        String format = "";
        for (IntervalResults intervalResults : filteredResultsMap.values()) {
            MetricResults memoryUsageResults = intervalResults.getMetricResultsMap().get(metricName);
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
        return format;
    }

    @Override
    public RecommendationConfigItem getMemoryRequestRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap, ArrayList<RecommendationNotification> notifications) {
        boolean setNotification = true;
        if (null == notifications) {
            LOGGER.error("Notifications Object passed is empty. The notifications are not sent as part of recommendation.");
            setNotification = false;
        }
        RecommendationConfigItem recommendationConfigItem = null;
        String format = "";



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

        format = getFormatValue(filteredResultsMap, AnalyzerConstants.MetricName.memoryUsage);

        recommendationConfigItem = new RecommendationConfigItem(memRec, format);
        return recommendationConfigItem;
    }

    @Override
    public RecommendationConfigItem getCPURequestRecommendationForNamespace(Map<Timestamp, IntervalResults> filteredResultsMap, ArrayList<RecommendationNotification> notifications) {
        return null;
    }

    @Override
    public RecommendationConfigItem getMemoryRequestRecommendationForNamespace(Map<Timestamp, IntervalResults> filteredResultsMap, ArrayList<RecommendationNotification> notifications) {
        return null;
    }

    @Override
    public Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> getAcceleratorRequestRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap, ArrayList<RecommendationNotification> notifications) {
        return Map.of();
    }

    @Override
    public String getModelName() {
        return "";
    }

    @Override
    public void validate() {

    }
}
