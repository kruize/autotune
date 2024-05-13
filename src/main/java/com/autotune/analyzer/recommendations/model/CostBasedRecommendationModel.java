package com.autotune.analyzer.recommendations.model;

import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.recommendations.RecommendationNotification;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationEngine.PercentileConstants.COST_CPU_PERCENTILE;
import static com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationEngine.PercentileConstants.COST_MEMORY_PERCENTILE;
import static com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationValueConstants.*;

public class CostBasedRecommendationModel implements RecommendationModel {

    private int percentile;
    private String name;
    private static final Logger LOGGER = LoggerFactory.getLogger(CostBasedRecommendationModel.class);

    public CostBasedRecommendationModel() {
        this.name = RecommendationConstants.RecommendationEngine.ModelNames.COST;
    }

    public CostBasedRecommendationModel(int percentile) {
        this.percentile = percentile;
    }

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
        // Extract 'max' values from cpuUsageList
        List<Double> cpuMaxValues = IntStream.range(0, cpuUsageList.length())
                .mapToObj(cpuUsageList::getJSONObject)
                .map(jsonObject -> jsonObject.getDouble(KruizeConstants.JSONKeys.MAX))
                .toList();

        Double cpuRequest;
        Double cpuRequestMax = Collections.max(cpuMaxValues);
        if (null != cpuRequestMax && CPU_ONE_CORE > cpuRequestMax) {
            cpuRequest = cpuRequestMax;
        } else {
            cpuRequest = CommonUtils.percentile(COST_CPU_PERCENTILE, cpuMaxValues);
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
        CostBasedRecommendationModel costBasedRecommendationModel  = new CostBasedRecommendationModel();
        List<Double> memUsageList = new ArrayList<>();
        for (IntervalResults intervalResults: filteredResultsMap.values()) {
            JSONObject jsonObject = costBasedRecommendationModel.calculateMemoryUsage(intervalResults);
            Double memUsage = jsonObject.getDouble(KruizeConstants.JSONKeys.MAX);
            memUsageList.add(memUsage);
        }

        List<Double> spikeList = filteredResultsMap.values()
                .stream()
                .map(CostBasedRecommendationModel::calculateIntervalSpike)
                .collect(Collectors.toList());

        Double memRecUsage = calculatePercentile(memUsageList, COST_MEMORY_PERCENTILE);
        Double memRecUsageBuf = memRecUsage + (memRecUsage * MEM_USAGE_BUFFER_DECIMAL);

        Double memRecSpike = calculatePercentile(spikeList, COST_MEMORY_PERCENTILE);
        memRecSpike += (memRecSpike * MEM_SPIKE_BUFFER_DECIMAL);
        Double memRecSpikeBuf = memRecUsage + memRecSpike;

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
    public String getModelName() {
        return this.name;
    }

    @Override
    public void validate() {

    }

    public static JSONObject calculateMemoryUsage(IntervalResults intervalResults) {
        // create a JSON object which should be returned here having two values, Math.max and Collections.Min
        JSONObject jsonObject = new JSONObject();
        Optional<MetricResults> cpuUsageResults = Optional.ofNullable(intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.cpuUsage));
        double cpuUsageAvg = cpuUsageResults.map(m -> m.getAggregationInfoResult().getAvg()).orElse(0.0);
        double cpuUsageSum = cpuUsageResults.map(m -> m.getAggregationInfoResult().getSum()).orElse(0.0);
        Optional<MetricResults> memoryUsageResults = Optional.ofNullable(intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.memoryUsage));
        double memUsageAvg = memoryUsageResults.map(m -> m.getAggregationInfoResult().getAvg()).orElse(0.0);
        double memUsageMax = memoryUsageResults.map(m -> m.getAggregationInfoResult().getMax()).orElse(0.0);
        double memUsageMin = memoryUsageResults.map(m -> m.getAggregationInfoResult().getMin()).orElse(0.0);
        double memUsageSum = memoryUsageResults.map(m -> m.getAggregationInfoResult().getSum()).orElse(0.0);
        double memUsage = 0;
        int numPods = 0;

        if (0 != cpuUsageAvg) {
            numPods = (int) Math.ceil(cpuUsageSum / cpuUsageAvg);
        }
        if (0 == numPods && 0 != memUsageAvg) {
            numPods = (int) Math.ceil(memUsageSum / memUsageAvg);
        }
        if (0 < numPods) {
            memUsage = (memUsageSum / numPods);
        }
        memUsageMax = Math.max(memUsage, memUsageMax);
        // traverse over a stream of positive values and find the minimum value
        memUsageMin = Stream.of(memUsage, memUsageMax, memUsageMin)
                .filter(value -> value > 0.0)
                .min(Double::compare)
                .orElse(0.0);

        jsonObject.put(KruizeConstants.JSONKeys.MIN, memUsageMin);
        jsonObject.put(KruizeConstants.JSONKeys.MAX, memUsageMax);

        LOGGER.debug("memRequestInterval : {}", jsonObject);
        return jsonObject;
    }

    private static double calculateIntervalSpike(IntervalResults intervalResults) {
        Optional<MetricResults> memoryUsageResults = Optional.ofNullable(intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.memoryUsage));
        Optional<MetricResults> memoryRSSResults = Optional.ofNullable(intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.memoryRSS));
        double memUsageMax = memoryUsageResults.map(m -> m.getAggregationInfoResult().getMax()).orElse(0.0);
        double memUsageMin = memoryUsageResults.map(m -> m.getAggregationInfoResult().getMin()).orElse(0.0);
        double memRSSMax = memoryRSSResults.map(m -> m.getAggregationInfoResult().getMax()).orElse(0.0);
        double memRSSMin = memoryRSSResults.map(m -> m.getAggregationInfoResult().getMin()).orElse(0.0);

        return Math.max(Math.ceil(memUsageMax - memUsageMin), Math.ceil(memRSSMax - memRSSMin));
    }

    private static Double calculatePercentile(List<Double> list, double percentile) {
        return CommonUtils.percentile(percentile, list);
    }
    private static RecommendationConfigItem getCPURequestRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap,
                                                                        ArrayList<RecommendationNotification> notifications,
                                                                        double percentile, Double cpuZero, double cpuOneCore, double cpuOneMillicore,
                                                                        RecommendationConstants.RecommendationNotification zeroNotification,
                                                                        RecommendationConstants.RecommendationNotification idleNotification) {
        boolean setNotification = true;
        if (null == notifications) {
            LOGGER.error("Notifications Object passed is empty. The notifications are not sent as part of recommendation.");
            setNotification = false;
        }
        RecommendationConfigItem recommendationConfigItem = null;
        String format = "";
        List<Double> cpuUsageList = filteredResultsMap.values()
                .stream()
                .map(CostBasedRecommendationModel::calculateCPURequestInterval)
                .collect(Collectors.toList());

        Double cpuRequest;
        Double cpuRequestMax = Collections.max(cpuUsageList);
        if (null != cpuRequestMax && cpuOneCore > cpuRequestMax) {
            cpuRequest = cpuRequestMax;
        } else {
            cpuRequest = CommonUtils.percentile(percentile, cpuUsageList);
        }

        if (null == cpuRequest) {
            cpuRequest = cpuZero;
        }

        if (setNotification) {
            if (cpuZero.equals(cpuRequest)) {
                notifications.add(new RecommendationNotification(zeroNotification));
                return null;
            } else if (cpuOneMillicore >= cpuRequest) {
                notifications.add(new RecommendationNotification(idleNotification));
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

    private static double calculateCPURequestInterval(IntervalResults intervalResults) {
        Optional<MetricResults> cpuUsageResults = Optional.ofNullable(intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.cpuUsage));
        Optional<MetricResults> cpuThrottleResults = Optional.ofNullable(intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.cpuThrottle));
        double cpuUsageAvg = cpuUsageResults.map(m -> m.getAggregationInfoResult().getAvg()).orElse(0.0);
        double cpuUsageMax = cpuUsageResults.map(m -> m.getAggregationInfoResult().getMax()).orElse(0.0);
        double cpuUsageSum = cpuUsageResults.map(m -> m.getAggregationInfoResult().getSum()).orElse(0.0);
        double cpuThrottleAvg = cpuThrottleResults.map(m -> m.getAggregationInfoResult().getAvg()).orElse(0.0);
        double cpuThrottleMax = cpuThrottleResults.map(m -> m.getAggregationInfoResult().getMax()).orElse(0.0);
        double cpuThrottleSum = cpuThrottleResults.map(m -> m.getAggregationInfoResult().getSum()).orElse(0.0);
        double cpuRequestInterval = 0.0;
        double cpuUsagePod = 0;
        int numPods = 0;

        double cpuUsage = (cpuUsageMax > 0) ? cpuUsageMax : cpuUsageAvg;
        double cpuThrottle = (cpuThrottleMax > 0) ? cpuThrottleMax : cpuThrottleAvg;
        double cpuUsageTotal = cpuUsage + cpuThrottle;

        if (CPU_ONE_CORE > cpuUsageTotal) {
            cpuRequestInterval = cpuUsageTotal;
        } else {
            if (0 != cpuUsageAvg) {
                numPods = (int) Math.ceil(cpuUsageSum / cpuUsageAvg);
                if (0 < numPods) {
                    cpuUsagePod = (cpuUsageSum + cpuThrottleSum) / numPods;
                }
            }
            cpuRequestInterval = Math.max(cpuUsagePod, cpuUsageTotal);
        }
        return cpuRequestInterval;
    }


}
