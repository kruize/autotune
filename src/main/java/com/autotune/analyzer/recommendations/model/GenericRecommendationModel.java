package com.autotune.analyzer.recommendations.model;

import com.autotune.analyzer.kruizeLayer.impl.TunableSpec;
import com.autotune.analyzer.recommendations.LayerRecommendationHandler;
import com.autotune.analyzer.recommendations.LayerRecommendationHandlerRegistry;
import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.recommendations.RecommendationNotification;
import com.autotune.analyzer.recommendations.utils.RecommendationUtils;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.metrics.AcceleratorMetricMetadata;
import com.autotune.common.data.metrics.AcceleratorMetricResult;
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

import static com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationValueConstants.*;

/**
 * This is a generic class that implements RecommendationModel abstract class and provides the code for functions
 * that are common across cost and performance recommendation model.
 */

public class GenericRecommendationModel implements RecommendationModel{

    protected String name;
    protected double modelMemoryPercentile;
    protected double modelCPUPercentile;
    protected double modelAcceleratorPercentile;

    // constructor
    public GenericRecommendationModel( String name, RecommendationTunables recommendationTunables) {
        // null checks
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException(AnalyzerErrorConstants.APIErrors.generateRecommendationsAPI.NULL_OR_EMPTY_MODEL_NAME);
        }
        if (recommendationTunables == null) {
            throw new IllegalArgumentException(AnalyzerErrorConstants.APIErrors.generateRecommendationsAPI.NULL_RECOMMENDATION_TUNABLES);
        }

        this.name = name;
        this.modelMemoryPercentile = recommendationTunables.getMemoryPercentile();
        this.modelCPUPercentile = recommendationTunables.getCpuPercentile();
        this.modelAcceleratorPercentile = recommendationTunables.getAcceleratorPercentile();

    }

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericRecommendationModel.class);

    @Override
    public RecommendationConfigItem getCPURequestRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap, ArrayList<RecommendationNotification> notifications) {
        boolean setNotification = true;
        if (null == notifications) {
            LOGGER.error(KruizeConstants.ErrorMsgs.RecommendationErrorMsgs.EMPTY_NOTIFICATIONS_OBJECT);
            setNotification = false;
        }

        RecommendationConfigItem recommendationConfigItem = null;
        String format = "";
        List<Double> cpuMaxValues = new ArrayList<>();
        for (IntervalResults intervalResults : filteredResultsMap.values()) {
            cpuMaxValues.add(cpuUsageBound(intervalResults).max);
        }

        Double cpuRequest = 0.0;
        Double cpuRequestMax = Collections.max(cpuMaxValues);
        if (null != cpuRequestMax && CPU_ONE_CORE > cpuRequestMax) {
            cpuRequest = cpuRequestMax;
        } else {
            cpuRequest = CommonUtils.percentile(modelCPUPercentile, cpuMaxValues);
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

    public static List<Double> getCPUMaxValues(JSONArray cpuUsageList){
        List<Double> cpuMaxValues = new ArrayList<>();

        for (int i = 0; i < cpuUsageList.length(); i++) {
            JSONObject jsonObject = cpuUsageList.getJSONObject(i);
            double maxValue = jsonObject.getDouble(KruizeConstants.JSONKeys.MAX);
            cpuMaxValues.add(maxValue);
        }
        return cpuMaxValues;
    }

    /**
     * Per-interval CPU min and max. Public so plot generation can skip the JSON wrapper.
     */
    public static final class IntervalBound {
        public final double min;
        public final double max;

        public IntervalBound(double min, double max) {
            this.min = min;
            this.max = max;
        }
    }

    public static IntervalBound cpuUsageBound(IntervalResults intervalResults) {
        Map<AnalyzerConstants.MetricName, MetricResults> metrics = intervalResults.getMetricResultsMap();
        MetricResults cpuUsageResults = metrics.get(AnalyzerConstants.MetricName.cpuUsage);
        MetricResults cpuThrottleResults = metrics.get(AnalyzerConstants.MetricName.cpuThrottle);
        MetricResults memoryUsageResults = metrics.get(AnalyzerConstants.MetricName.memoryUsage);
        double cpuUsageAvg = avgOrZero(cpuUsageResults);
        double cpuUsageMax = maxOrZero(cpuUsageResults);
        double cpuUsageSum = sumOrZero(cpuUsageResults);
        double cpuUsageMin = minOrZero(cpuUsageResults);
        double cpuThrottleAvg = avgOrZero(cpuThrottleResults);
        double cpuThrottleMax = maxOrZero(cpuThrottleResults);
        double cpuThrottleSum = sumOrZero(cpuThrottleResults);
        double cpuThrottleMin = minOrZero(cpuThrottleResults);
        double memUsageAvg = avgOrZero(memoryUsageResults);
        double memUsageSum = sumOrZero(memoryUsageResults);

        double cpuRequestIntervalMax;
        double cpuUsagePod = 0;
        double numPods = 0;

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
                numPods = cpuUsageSum / cpuUsageAvg;
            }
            if (0 == numPods && 0 != memUsageAvg) {
                numPods = memUsageSum / memUsageAvg;
            }
            if (0 < numPods) {
                cpuUsagePod = (cpuUsageSum + cpuThrottleSum) / numPods;
            }
            cpuRequestIntervalMax = Math.max(cpuUsagePod, cpuUsageTotal);
        }
        double cpuMinTotal = cpuUsageMin + cpuThrottleMin;
        double cpuRequestIntervalMin = lowestPositive(cpuUsagePod, cpuUsageTotal, cpuMinTotal);
        return new IntervalBound(cpuRequestIntervalMin, cpuRequestIntervalMax);
    }

    // helper function common to both cost and performance model hence just taken from there.
    public static JSONArray getCPUUsageList(Map<Timestamp, IntervalResults> filteredResultsMap) {
        JSONArray cpuRequestIntervalArray = new JSONArray();
        for (IntervalResults intervalResults : filteredResultsMap.values()) {
            IntervalBound bound = cpuUsageBound(intervalResults);
            JSONObject cpuRequestInterval = new JSONObject();
            cpuRequestInterval.put(KruizeConstants.JSONKeys.MIN, bound.min);
            cpuRequestInterval.put(KruizeConstants.JSONKeys.MAX, bound.max);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("cpuRequestInterval : {}", cpuRequestInterval);
            }
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
            LOGGER.error(KruizeConstants.ErrorMsgs.RecommendationErrorMsgs.EMPTY_NOTIFICATIONS_OBJECT);
            setNotification = false;
        }
        RecommendationConfigItem recommendationConfigItem = null;
        String format = "";

        List<Double> memUsageList = new ArrayList<>();
        List<Double> spikeList = new ArrayList<>();
        for (IntervalResults intervalResults: filteredResultsMap.values()) {
            memUsageList.add(memoryUsageBound(intervalResults).max);
            spikeList.add(calculateIntervalSpike(intervalResults));
        }

        // Add a buffer to the current usage max
        Double memRecUsage = CommonUtils.percentile(modelMemoryPercentile, memUsageList);
        Double memRecUsageBuf = memRecUsage + (memRecUsage * MEM_USAGE_BUFFER_DECIMAL);

        // Add a small buffer to the current usage spike max and add it to the current usage max
        Double memRecSpike = CommonUtils.percentile(modelMemoryPercentile, spikeList);
        memRecSpike += (memRecSpike * MEM_SPIKE_BUFFER_DECIMAL);
        Double memRecSpikeBuf = memRecUsage + memRecSpike;

        // We'll use the minimum of the above two values
        Double memRec = Math.min(memRecUsageBuf, memRecSpikeBuf);

        if (setNotification && 0.0 == memRec) {
            notifications.add(new RecommendationNotification(
                    RecommendationConstants.RecommendationNotification.NOTICE_MEMORY_RECORDS_ARE_ZERO
            ));
            return null;
        }

        format = getFormatValue(filteredResultsMap, AnalyzerConstants.MetricName.memoryUsage);

        recommendationConfigItem = new RecommendationConfigItem(memRec, format);
        return recommendationConfigItem;
    }

    @Override
    public RecommendationConfigItem getCPULimitRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap, ArrayList<RecommendationNotification> notifications) {
        // Default: same as request. Override when model has distinct limit-specific logic.
        return getCPURequestRecommendation(filteredResultsMap, notifications);
    }

    @Override
    public RecommendationConfigItem getMemoryLimitRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap, ArrayList<RecommendationNotification> notifications) {
        // Default: same as request. Override when model has distinct limit-specific logic.
        return getMemoryRequestRecommendation(filteredResultsMap, notifications);
    }

    // helper functions for getMemoryRequestRecommendation

    public static IntervalBound memoryUsageBound(IntervalResults intervalResults) {
        Map<AnalyzerConstants.MetricName, MetricResults> metrics = intervalResults.getMetricResultsMap();
        MetricResults cpuUsageResults = metrics.get(AnalyzerConstants.MetricName.cpuUsage);
        double cpuUsageAvg = avgOrZero(cpuUsageResults);
        double cpuUsageSum = sumOrZero(cpuUsageResults);
        MetricResults memoryUsageResults = metrics.get(AnalyzerConstants.MetricName.memoryUsage);
        double memUsageAvg = avgOrZero(memoryUsageResults);
        double memUsageMax = maxOrZero(memoryUsageResults);
        double memUsageMin = minOrZero(memoryUsageResults);
        double memUsageSum = sumOrZero(memoryUsageResults);
        double memUsage = 0;
        double numPods = 0;

        if (0 != cpuUsageAvg) {
            numPods = cpuUsageSum / cpuUsageAvg;
        }
        if (0 == numPods && 0 != memUsageAvg) {
            numPods = memUsageSum / memUsageAvg;
        }
        if (0 < numPods) {
            memUsage = (memUsageSum / numPods);
        }
        memUsageMax = Math.max(memUsage, memUsageMax);
        memUsageMin = lowestPositive(memUsage, memUsageMax, memUsageMin);
        return new IntervalBound(memUsageMin, memUsageMax);
    }

    public static JSONObject calculateMemoryUsage(IntervalResults intervalResults) {
        // create a JSON object which should be returned here having two values, Math.max and Collections.Min
        IntervalBound bound = memoryUsageBound(intervalResults);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(KruizeConstants.JSONKeys.MIN, bound.min);
        jsonObject.put(KruizeConstants.JSONKeys.MAX, bound.max);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("memRequestInterval : {}", jsonObject);
        }
        return jsonObject;
    }

    private static double calculateIntervalSpike(IntervalResults intervalResults) {
        Map<AnalyzerConstants.MetricName, MetricResults> metrics = intervalResults.getMetricResultsMap();
        MetricResults memoryUsageResults = metrics.get(AnalyzerConstants.MetricName.memoryUsage);
        MetricResults memoryRSSResults = metrics.get(AnalyzerConstants.MetricName.memoryRSS);
        double memUsageMax = maxOrZero(memoryUsageResults);
        double memUsageMin = minOrZero(memoryUsageResults);
        double memRSSMax = maxOrZero(memoryRSSResults);
        double memRSSMin = minOrZero(memoryRSSResults);

        return Math.max(Math.ceil(memUsageMax - memUsageMin), Math.ceil(memRSSMax - memRSSMin));
    }


    @Override
    public RecommendationConfigItem getCPURequestRecommendationForNamespace(Map<Timestamp, IntervalResults> filteredResultsMap, ArrayList<RecommendationNotification> notifications) {
        boolean setNotification = true;
        if (null == notifications) {
            LOGGER.error(KruizeConstants.ErrorMsgs.RecommendationErrorMsgs.EMPTY_NOTIFICATIONS_OBJECT);
            setNotification = false;
        }

        RecommendationConfigItem recommendationConfigItem = null;
        String format = "";

        List<Double> namespaceCpuMaxValues = new ArrayList<>();
        for (IntervalResults intervalResults : filteredResultsMap.values()) {
            namespaceCpuMaxValues.add(namespaceCpuUsageBound(intervalResults).max);
        }

        Double namespaceCpuRequest = 0.0;
        Double namespaceCpuRequestMax = Collections.max(namespaceCpuMaxValues);
        if (null != namespaceCpuRequestMax && CPU_ONE_CORE > namespaceCpuRequestMax) {
            namespaceCpuRequest = namespaceCpuRequestMax;
        } else {
            namespaceCpuRequest = CommonUtils.percentile(modelCPUPercentile, namespaceCpuMaxValues);
        }

        if (null == namespaceCpuRequest) {
            namespaceCpuRequest = CPU_ZERO;
        }

        // Set notifications only if notification object is available
        if (setNotification) {
            // Check for Zero CPU
            if (CPU_ZERO.equals(namespaceCpuRequest)) {
                // Add notification for CPU_RECORDS_ARE_ZERO
                notifications.add(new RecommendationNotification(
                        RecommendationConstants.RecommendationNotification.NOTICE_CPU_RECORDS_ARE_ZERO
                ));
                // Returning null will make sure that the map is not populated with values
                return null;
            }
            // Check for IDLE CPU
            else if (CPU_ONE_MILLICORE >= namespaceCpuRequest) {
                // Add notification for CPU_RECORDS_ARE_IDLE
                notifications.add(new RecommendationNotification(
                        RecommendationConstants.RecommendationNotification.NOTICE_CPU_RECORDS_ARE_IDLE
                ));
                // Returning null will make sure that the map is not populated with values
                return null;
            }
        }

        format = getFormatValue(filteredResultsMap, AnalyzerConstants.MetricName.namespaceCpuUsage);

        recommendationConfigItem = new RecommendationConfigItem(namespaceCpuRequest, format);
        return recommendationConfigItem;

    }

    // helper functions for getCPURequestRecommendationForNamespace

    public static IntervalBound namespaceCpuUsageBound(IntervalResults intervalResults) {
        Map<AnalyzerConstants.MetricName, MetricResults> metrics = intervalResults.getMetricResultsMap();
        MetricResults namespaceCpuUsageResults = metrics.get(AnalyzerConstants.MetricName.namespaceCpuUsage);
        MetricResults namespaceCpuThrottleResults = metrics.get(AnalyzerConstants.MetricName.namespaceCpuThrottle);

        double namespaceCpuUsageAvg = avgOrZero(namespaceCpuUsageResults);
        double namespaceCpuUsageMax = maxOrZero(namespaceCpuUsageResults);
        double namespaceCpuUsageMin = minOrZero(namespaceCpuUsageResults);
        double namespaceCpuThrottleAvg = avgOrZero(namespaceCpuThrottleResults);
        double namespaceCpuThrottleMax = maxOrZero(namespaceCpuThrottleResults);
        double namespaceCpuThrottleMin = minOrZero(namespaceCpuThrottleResults);

        // Use the Max value when available, if not use the Avg
        double namespaceCpuUsage = (namespaceCpuUsageMax > 0) ? namespaceCpuUsageMax : namespaceCpuUsageAvg;
        double namespaceCpuThrottle = (namespaceCpuThrottleMax > 0) ? namespaceCpuThrottleMax : namespaceCpuThrottleAvg;
        double namespaceCpuUsageTotal = namespaceCpuUsage + namespaceCpuThrottle;
        double namespaceCpuMinTotal = namespaceCpuUsageMin + namespaceCpuThrottleMin;
        double namespaceCpuRequestIntervalMin = lowestPositive(namespaceCpuUsageTotal, namespaceCpuMinTotal);
        return new IntervalBound(namespaceCpuRequestIntervalMin, namespaceCpuUsageTotal);
    }

    public static JSONArray getNamespaceCPUUsageList(Map<Timestamp, IntervalResults> filteredResultsMap) {
        JSONArray namespaceCpuRequestIntervalArray = new JSONArray();
        for (IntervalResults intervalResults : filteredResultsMap.values()) {
            IntervalBound bound = namespaceCpuUsageBound(intervalResults);
            JSONObject namespaceCpuRequestInterval = new JSONObject();
            namespaceCpuRequestInterval.put(KruizeConstants.JSONKeys.MIN, bound.min);
            namespaceCpuRequestInterval.put(KruizeConstants.JSONKeys.MAX, bound.max);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("cpuRequestInterval : {}", namespaceCpuRequestInterval);
            }
            namespaceCpuRequestIntervalArray.put(namespaceCpuRequestInterval);
        }
        return namespaceCpuRequestIntervalArray;
    }


    @Override
    public RecommendationConfigItem getMemoryRequestRecommendationForNamespace(Map<Timestamp, IntervalResults> filteredResultsMap, ArrayList<RecommendationNotification> notifications) {
        boolean setNotification = true;
        if (null == notifications) {
            LOGGER.error(KruizeConstants.ErrorMsgs.RecommendationErrorMsgs.EMPTY_NOTIFICATIONS_OBJECT);
            setNotification = false;
        }

        RecommendationConfigItem recommendationConfigItem = null;
        String format = "";

        List<Double> namespaceMemUsageList = new ArrayList<>();
        List<Double> spikeList = new ArrayList<>();
        for (IntervalResults intervalResults: filteredResultsMap.values()) {
            namespaceMemUsageList.add(namespaceMemoryUsageBound(intervalResults).max);
            spikeList.add(calculateIntervalSpikeForNamespace(intervalResults));
        }


        // Add a buffer to the current usage max
        Double namespaceMemRecUsage = CommonUtils.percentile(modelMemoryPercentile, namespaceMemUsageList);
        Double namespaceMemRecUsageBuf = namespaceMemRecUsage + (namespaceMemRecUsage * MEM_USAGE_BUFFER_DECIMAL);

        // Add a small buffer to the current usage spike max and add it to the current usage max
        Double namespaceMemRecSpike = CommonUtils.percentile(modelMemoryPercentile, spikeList);
        namespaceMemRecSpike += (namespaceMemRecSpike * MEM_SPIKE_BUFFER_DECIMAL);
        Double namespaceMemRecSpikeBuf = namespaceMemRecUsage + namespaceMemRecSpike;

        // We'll use the minimum of the above two values
        Double namespaceMemRec = Math.min(namespaceMemRecUsageBuf, namespaceMemRecSpikeBuf);

        // Set notifications only if notification object is available
        if (setNotification && 0.0 == namespaceMemRec) {
            notifications.add(new RecommendationNotification(
                    RecommendationConstants.RecommendationNotification.NOTICE_MEMORY_RECORDS_ARE_ZERO
            ));
            return null;
        }

        format = getFormatValue(filteredResultsMap, AnalyzerConstants.MetricName.namespaceMemoryUsage);

        recommendationConfigItem = new RecommendationConfigItem(namespaceMemRec, format);
        return recommendationConfigItem;
    }

    // helper function 1 nsp

    private static double calculateIntervalSpikeForNamespace(IntervalResults intervalResults) {
        Map<AnalyzerConstants.MetricName, MetricResults> metrics = intervalResults.getMetricResultsMap();
        MetricResults namespaceMemoryUsageResults = metrics.get(AnalyzerConstants.MetricName.namespaceMemoryUsage);
        MetricResults namespaceMemoryRSSResults = metrics.get(AnalyzerConstants.MetricName.namespaceMemoryRSS);
        double namespaceMemUsageMax = maxOrZero(namespaceMemoryUsageResults);
        double namespaceMemUsageMin = minOrZero(namespaceMemoryUsageResults);
        double namespaceMemRSSMax = maxOrZero(namespaceMemoryRSSResults);
        double namespaceMemRSSMin = minOrZero(namespaceMemoryRSSResults);

        return Math.max(Math.ceil(namespaceMemUsageMax - namespaceMemUsageMin), Math.ceil(namespaceMemRSSMax - namespaceMemRSSMin));
    }

    public static IntervalBound namespaceMemoryUsageBound(IntervalResults intervalResults) {
        MetricResults namespaceMemoryUsageResults = intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.namespaceMemoryUsage);
        double namespaceMemUsageMax = maxOrZero(namespaceMemoryUsageResults);
        double namespaceMemUsageMin = minOrZero(namespaceMemoryUsageResults);
        namespaceMemUsageMin = lowestPositive(namespaceMemUsageMax, namespaceMemUsageMin);
        return new IntervalBound(namespaceMemUsageMin, namespaceMemUsageMax);
    }

    // helper function 2 nsp
    public static JSONObject calculateNamespaceMemoryUsage(IntervalResults intervalResults) {
        IntervalBound bound = namespaceMemoryUsageBound(intervalResults);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(KruizeConstants.JSONKeys.MIN, bound.min);
        jsonObject.put(KruizeConstants.JSONKeys.MAX, bound.max);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("memRequestInterval : {}", jsonObject);
        }
        return jsonObject;
    }

    private static double avgOrZero(MetricResults metricResults) {
        if (metricResults == null) {
            return 0.0;
        }
        Double value = metricResults.getAggregationInfoResult().getAvg();
        return value == null ? 0.0 : value;
    }

    private static double minOrZero(MetricResults metricResults) {
        if (metricResults == null) {
            return 0.0;
        }
        Double value = metricResults.getAggregationInfoResult().getMin();
        return value == null ? 0.0 : value;
    }

    private static double maxOrZero(MetricResults metricResults) {
        if (metricResults == null) {
            return 0.0;
        }
        Double value = metricResults.getAggregationInfoResult().getMax();
        return value == null ? 0.0 : value;
    }

    private static double sumOrZero(MetricResults metricResults) {
        if (metricResults == null) {
            return 0.0;
        }
        Double value = metricResults.getAggregationInfoResult().getSum();
        return value == null ? 0.0 : value;
    }

    /** Same value as {@code Stream.of(values).filter(v -> v > 0).min(Double::compare).orElse(0.0)}. */
    private static double lowestPositive(double first, double second, double third) {
        double min = 0.0;
        boolean found = false;
        if (first > 0.0) {
            min = first;
            found = true;
        }
        if (second > 0.0 && (!found || second < min)) {
            min = second;
            found = true;
        }
        if (third > 0.0 && (!found || third < min)) {
            min = third;
        }
        return min;
    }

    private static double lowestPositive(double first, double second) {
        double min = 0.0;
        boolean found = false;
        if (first > 0.0) {
            min = first;
            found = true;
        }
        if (second > 0.0 && (!found || second < min)) {
            min = second;
        }
        return min;
    }



    @Override
    public Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> getAcceleratorRequestRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap, ArrayList<RecommendationNotification> notifications) {

        boolean setNotification = true;
        if (null == notifications) {
            LOGGER.error(KruizeConstants.ErrorMsgs.RecommendationErrorMsgs.EMPTY_NOTIFICATIONS_OBJECT);
            setNotification = false;
        }

        List<Double> acceleratorCoreMaxValues = new ArrayList<>();
        List<Double> acceleratorMemoryMaxValues = new ArrayList<>();

        boolean isGpuWorkload = false;
        String acceleratorModel = null;

        for (Map.Entry<Timestamp, IntervalResults> entry : filteredResultsMap.entrySet()) {
            IntervalResults intervalResults = entry.getValue();

            // Check for accelerator metric map
            if (null == intervalResults.getAcceleratorMetricResultHashMap() || intervalResults.getAcceleratorMetricResultHashMap().isEmpty()) {
                if (!intervalResults.getMetricResultsMap().isEmpty()) {
                    // Iterate for accelerator metrics in the interval result
                    for (Map.Entry<AnalyzerConstants.MetricName, MetricResults> metricResultsEntry: intervalResults.getMetricResultsMap().entrySet()) {
                        MetricResults metricResults = metricResultsEntry.getValue();

                        if (null == metricResults)
                            continue;

                        if (null == metricResults.getAggregationInfoResult())
                            continue;

                        AnalyzerConstants.MetricName metricName = metricResultsEntry.getKey();

                        // Check for accelerator related metrics
                        if (metricName == AnalyzerConstants.MetricName.acceleratorCoreUsage ||
                            metricName == AnalyzerConstants.MetricName.acceleratorMemoryUsage ||
                            metricName == AnalyzerConstants.MetricName.acceleratorFrameBufferUsage) {

                            // Check to set the accelerator model
                            if (null == acceleratorModel) {
                                AcceleratorMetricMetadata acceleratorMetricMetadata = (AcceleratorMetricMetadata) metricResults.getMetadata();
                                if (null != acceleratorMetricMetadata.getModelName() &&
                                        !acceleratorMetricMetadata.getModelName().isEmpty() &&
                                        !acceleratorMetricMetadata.getModelName().isBlank() &&
                                        RecommendationUtils.checkIfModelIsKruizeSupportedMIG(acceleratorMetricMetadata.getModelName().strip())
                                ) {
                                    isGpuWorkload = true;
                                    String obtainedAcceleratorName = RecommendationUtils.getSupportedModelBasedOnModelName(acceleratorMetricMetadata.getModelName().strip());

                                    if (null != obtainedAcceleratorName)
                                        acceleratorModel = obtainedAcceleratorName;
                                }
                            }
                            MetricAggregationInfoResults aggregationInfo = metricResults.getAggregationInfoResult();

                            // Skip if max is null or zero or negative
                            if (null == aggregationInfo.getMax() || aggregationInfo.getMax() <= 0.0)
                                continue;

                            boolean isCoreUsage = metricName == AnalyzerConstants.MetricName.acceleratorCoreUsage;
                            boolean isMemoryUsage = metricName == AnalyzerConstants.MetricName.acceleratorMemoryUsage;

                            if (isCoreUsage) {
                                acceleratorCoreMaxValues.add(aggregationInfo.getMax());
                            } else if (isMemoryUsage) {
                                acceleratorMemoryMaxValues.add(aggregationInfo.getMax());
                            } else {
                                // Convert absolutes to percentages to get the recommendation
                                Double value = aggregationInfo.getMax();
                                if (value > 100) {
                                    double cardFrameBuffer = RecommendationUtils.getFrameBufferBasedOnModel(acceleratorModel);
                                    if (cardFrameBuffer != -1) {
                                        if (cardFrameBuffer > 0)
                                            acceleratorMemoryMaxValues.add((value / cardFrameBuffer) * 100);
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                isGpuWorkload = true;
                for (Map.Entry<AnalyzerConstants.MetricName, AcceleratorMetricResult> gpuEntry : intervalResults.getAcceleratorMetricResultHashMap().entrySet()) {
                    AcceleratorMetricResult gpuMetricResult = gpuEntry.getValue();

                    // Set Accelerator name
                    if (null == acceleratorModel
                            && null != gpuMetricResult.getAcceleratorDeviceData().getModelName()
                            && !gpuMetricResult.getAcceleratorDeviceData().getModelName().isEmpty()
                            && RecommendationUtils.checkIfModelIsKruizeSupportedMIG(gpuMetricResult.getAcceleratorDeviceData().getModelName())
                    ) {
                        String obtainedAcceleratorName = RecommendationUtils.getSupportedModelBasedOnModelName(gpuMetricResult.getAcceleratorDeviceData().getModelName());

                        if (null != obtainedAcceleratorName)
                            acceleratorModel = obtainedAcceleratorName;
                    }

                    MetricResults metricResults = gpuMetricResult.getMetricResults();

                    // Skip if metric results is null
                    if (null == metricResults || null == metricResults.getAggregationInfoResult())
                        continue;

                    MetricAggregationInfoResults aggregationInfo = metricResults.getAggregationInfoResult();

                    // Skip if max is null or zero or negative
                    if (null == aggregationInfo.getMax() || aggregationInfo.getMax() <= 0.0)
                        continue;

                    boolean isCoreUsage = gpuEntry.getKey() == AnalyzerConstants.MetricName.acceleratorCoreUsage;
                    boolean isMemoryUsage = (gpuEntry.getKey() == AnalyzerConstants.MetricName.acceleratorMemoryUsage)
                            || (gpuEntry.getKey() == AnalyzerConstants.MetricName.acceleratorFrameBufferUsage);

                    // Skip if it's none of the Accelerator metrics
                    if (!isCoreUsage && !isMemoryUsage)
                        continue;

                    if (isCoreUsage) {
                        acceleratorCoreMaxValues.add(aggregationInfo.getMax());
                    } else {
                        acceleratorMemoryMaxValues.add(aggregationInfo.getMax());
                    }
                }
            }
        }

        if (!isGpuWorkload) {
            if (!acceleratorCoreMaxValues.isEmpty() || !acceleratorMemoryMaxValues.isEmpty()) {
                if (setNotification) {
                    notifications.add(new RecommendationNotification(
                            RecommendationConstants.RecommendationNotification.NOTICE_ACCELERATOR_NOT_SUPPORTED));
                }
            }
            return null;
        }

        // Return null if entries are empty
        if (acceleratorCoreMaxValues.isEmpty() && acceleratorMemoryMaxValues.isEmpty())
            return null;

        double coreAverage = 0.0;
        if (!acceleratorCoreMaxValues.isEmpty())
            coreAverage = CommonUtils.percentile(modelAcceleratorPercentile, acceleratorCoreMaxValues);

        double memoryAverage = 0.0;
        if (!acceleratorMemoryMaxValues.isEmpty())
            memoryAverage = CommonUtils.percentile(modelAcceleratorPercentile, acceleratorMemoryMaxValues);

        double coreFraction = coreAverage / 100;
        // TODO: Need to investigate why data is faulty

        /**
         * The data we deal with is percentages and we are currently considering only one GPU per container
         * so the usage (Avg or Max) should be 100% and when we calculate the fraction we divide by 100
         * so the max we need to get is 1.
         *
         * Also the AcceleratorMetaDataService consider the core and memory fractions needed to come up
         * with the recommended accelerator MIG profile so if fractions exceed 1 none of the MIG configs
         * will match it (not even the whole GPU which considers core and memory fraction as 1) and we will
         * get NULL and hence there will be no recommendation.
         *
         * So if the fractions are greater than 100 there is a higher chance that there is an anomaly in data
         * so we mark it as 1 to give out full GPU as a recommendation.
         */
        if (coreFraction > 1) {
            LOGGER.info(AnalyzerErrorConstants.APIErrors.generateRecommendationsAPI.DATA_IRREGULARITY_DETECTED);
            coreFraction = 1;
        }
        double memoryFraction = memoryAverage / 100;
        // TODO: Need to investigate why data is faulty
        if (memoryFraction > 1) {
            LOGGER.info(AnalyzerErrorConstants.APIErrors.generateRecommendationsAPI.DATA_IRREGULARITY_DETECTED);
            memoryFraction = 1;
        }

        Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> returnMap = RecommendationUtils.getMapWithOptimalProfile(acceleratorModel, coreFraction, memoryFraction);

        if (null != returnMap && !returnMap.isEmpty()) {
            // Add notification based on isGpuWorkload and accelerator model name
            if (setNotification) {
                if (RecommendationUtils.checkIfModelIsKruizeSupportedMIG(acceleratorModel))
                    notifications.add(new RecommendationNotification(
                            RecommendationConstants.RecommendationNotification.INFO_ACCELERATOR_RECOMMENDATIONS_AVAILABLE));
            }
        }

        return returnMap;
    }

    /**
     * @param metricName
     * @param layerName
     * @param filteredResultsMap
     * @param tunableSpecObjectMap
     * @param notifications
     * @return
     */
    @Override
    public Object getRuntimeRecommendations(String metricName, String layerName, Map<Timestamp, IntervalResults> filteredResultsMap, Map<TunableSpec, Object> tunableSpecObjectMap,
                                            ArrayList<RecommendationNotification> notifications) {
        LayerRecommendationHandler handler = LayerRecommendationHandlerRegistry.getInstance().getHandler(layerName);
        return handler != null ? handler.generateRecommendations(metricName, tunableSpecObjectMap, filteredResultsMap) : null;
    }




    @Override
    public String getModelName() {
        return this.name;
    }

    @Override
    public void validate() {

    }
}
