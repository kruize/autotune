package com.autotune.analyzer.recommendations.model;

import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.recommendations.RecommendationNotification;
import com.autotune.analyzer.recommendations.utils.RecommendationUtils;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.metrics.AcceleratorMetricResult;
import com.autotune.common.data.metrics.MetricAggregationInfoResults;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.common.data.system.info.device.accelerator.metadata.AcceleratorMetaDataService;
import com.autotune.common.data.system.info.device.accelerator.metadata.AcceleratorProfile;
import com.autotune.common.utils.CommonUtils;
import com.autotune.utils.KruizeConstants;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cloudwatchlogs.endpoints.internal.Value;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationEngine.PercentileConstants.COST_CPU_PERCENTILE;
import static com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationEngine.PercentileConstants.COST_MEMORY_PERCENTILE;
import static com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationEngine.PercentileConstants.COST_ACCELERATOR_PERCENTILE;

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
        List<Double> cpuMaxValues = new ArrayList<>();
        for (int i = 0; i < cpuUsageList.length(); i++) {
            JSONObject jsonObject = cpuUsageList.getJSONObject(i);
            double maxValue = jsonObject.getDouble(KruizeConstants.JSONKeys.MAX);
            cpuMaxValues.add(maxValue);
        }

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
            Optional<MetricResults> memoryUsageResults = Optional.ofNullable(intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.memoryUsage));
            double memUsageAvg = memoryUsageResults.map(m -> m.getAggregationInfoResult().getAvg()).orElse(0.0);
            double memUsageSum = memoryUsageResults.map(m -> m.getAggregationInfoResult().getSum()).orElse(0.0);

            double cpuRequestIntervalMax;
            double cpuRequestIntervalMin;
            double cpuUsagePod = 0;
            int numPods = 0;

            // Use the Max value when available, if not use the Avg
            double cpuUsage = (cpuUsageMax > 0) ? cpuUsageMax : cpuUsageAvg;
            double cpuThrottle = (cpuThrottleMax > 0) ? cpuThrottleMax : cpuThrottleAvg;
            double cpuUsageTotal = cpuUsage + cpuThrottle;

            // Usage is less than 1 core, set it to the observed value.
            if (CPU_ONE_CORE > cpuUsageTotal) {
                cpuRequestIntervalMax = cpuUsageTotal;
            } else {
                // Sum/Avg should give us the number of pods
                if (0 != memUsageAvg) {
                    numPods = (int) Math.ceil(memUsageSum / memUsageAvg);
                }
                if (0 == numPods && 0 != cpuUsageAvg) {
                    numPods = (int) Math.ceil(cpuUsageSum / cpuUsageAvg);
                }
                if (0 < numPods) {
                    cpuUsagePod = (cpuUsageSum + cpuThrottleSum) / numPods;
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

        if (0 != memUsageAvg) {
            numPods = (int) Math.ceil(memUsageSum / memUsageAvg);
        }
        if (0 == numPods && 0 != cpuUsageAvg) {
            numPods = (int) Math.ceil(cpuUsageSum / cpuUsageAvg);
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
        Optional<MetricResults> memoryUsageResults = Optional.ofNullable(intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.memoryUsage));
        double memUsageAvg = memoryUsageResults.map(m -> m.getAggregationInfoResult().getAvg()).orElse(0.0);
        double memUsageSum = memoryUsageResults.map(m -> m.getAggregationInfoResult().getSum()).orElse(0.0);
        double cpuRequestInterval = 0.0;
        double cpuUsagePod = 0;
        int numPods = 0;

        double cpuUsage = (cpuUsageMax > 0) ? cpuUsageMax : cpuUsageAvg;
        double cpuThrottle = (cpuThrottleMax > 0) ? cpuThrottleMax : cpuThrottleAvg;
        double cpuUsageTotal = cpuUsage + cpuThrottle;

        if (CPU_ONE_CORE > cpuUsageTotal) {
            cpuRequestInterval = cpuUsageTotal;
        } else {
            if (0 != memUsageAvg) {
                numPods = (int) Math.ceil(memUsageSum / memUsageAvg);
            }
            if (numPods ==0 & 0 != cpuUsageAvg) {
                numPods = (int) Math.ceil(cpuUsageSum / cpuUsageAvg);
            }
            if (0 < numPods) {
                cpuUsagePod = (cpuUsageSum + cpuThrottleSum) / numPods;
            }
            cpuRequestInterval = Math.max(cpuUsagePod, cpuUsageTotal);
        }
        return cpuRequestInterval;
    }


    @Override
    public RecommendationConfigItem getCPURequestRecommendationForNamespace(Map<Timestamp, IntervalResults> filteredResultsMap, ArrayList<RecommendationNotification> notifications) {
        boolean setNotification = true;
        if (null == notifications) {
            LOGGER.error("Notifications Object passed is empty. The notifications are not sent as part of recommendation.");
            setNotification = false;
        }
        RecommendationConfigItem recommendationConfigItem = null;
        String format;
        JSONArray namespaceCpuUsageList = getNamespaceCPUUsageList(filteredResultsMap);

        // Extract 'max' values from cpuUsageList
        List<Double> namespaceCpuMaxValues = new ArrayList<>();
        for (int i = 0; i < namespaceCpuUsageList.length(); i++) {
            JSONObject jsonObject = namespaceCpuUsageList.getJSONObject(i);
            double maxValue = jsonObject.getDouble(KruizeConstants.JSONKeys.MAX);
            namespaceCpuMaxValues.add(maxValue);
        }

        Double namespaceCpuRequest;
        Double namespaceCpuRequestMax = Collections.max(namespaceCpuMaxValues);
        if (null != namespaceCpuRequestMax && CPU_ONE_CORE > namespaceCpuRequestMax) {
            namespaceCpuRequest = namespaceCpuRequestMax;
        } else {
            namespaceCpuRequest = CommonUtils.percentile(COST_CPU_PERCENTILE, namespaceCpuMaxValues);
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

    public static JSONArray getNamespaceCPUUsageList(Map<Timestamp, IntervalResults> filteredResultsMap) {
        JSONArray namespaceCpuRequestIntervalArray = new JSONArray();
        for (IntervalResults intervalResults : filteredResultsMap.values()) {
            JSONObject namespaceCpuRequestInterval = new JSONObject();
            Optional<MetricResults> namespaceCpuUsageResults = Optional.ofNullable(intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.namespaceCpuUsage));
            Optional<MetricResults> namespaceCpuThrottleResults = Optional.ofNullable(intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.namespaceCpuThrottle));

            double namespaceCpuUsageAvg = namespaceCpuUsageResults.map(m -> m.getAggregationInfoResult().getAvg()).orElse(0.0);
            double namespaceCpuUsageMax = namespaceCpuUsageResults.map(m -> m.getAggregationInfoResult().getMax()).orElse(0.0);
            double namespaceCpuUsageMin = namespaceCpuUsageResults.map(m -> m.getAggregationInfoResult().getMin()).orElse(0.0);
            double namespaceCpuThrottleAvg = namespaceCpuThrottleResults.map(m -> m.getAggregationInfoResult().getAvg()).orElse(0.0);
            double namespaceCpuThrottleMax = namespaceCpuThrottleResults.map(m -> m.getAggregationInfoResult().getMax()).orElse(0.0);
            double namespaceCpuThrottleMin = namespaceCpuThrottleResults.map(m -> m.getAggregationInfoResult().getMin()).orElse(0.0);

            double namespaceCpuRequestIntervalMax;
            double namespaceCpuRequestIntervalMin;

            // Use the Max value when available, if not use the Avg
            double namespaceCpuUsage = (namespaceCpuUsageMax > 0) ? namespaceCpuUsageMax : namespaceCpuUsageAvg;
            double namespaceCpuThrottle = (namespaceCpuThrottleMax > 0) ? namespaceCpuThrottleMax : namespaceCpuThrottleAvg;
            double namespaceCpuUsageTotal = namespaceCpuUsage + namespaceCpuThrottle;

            namespaceCpuRequestIntervalMax = namespaceCpuUsageTotal;

            double namespaceCpuMinTotal = namespaceCpuUsageMin + namespaceCpuThrottleMin;

            // traverse over a stream of positive values and find the minimum value
            namespaceCpuRequestIntervalMin = Stream.of(namespaceCpuUsageTotal, namespaceCpuMinTotal)
                    .filter(value -> value > 0.0)
                    .min(Double::compare)
                    .orElse(0.0);

            namespaceCpuRequestInterval.put(KruizeConstants.JSONKeys.MIN, namespaceCpuRequestIntervalMin);
            namespaceCpuRequestInterval.put(KruizeConstants.JSONKeys.MAX, namespaceCpuRequestIntervalMax);
            LOGGER.debug("cpuRequestInterval : {}", namespaceCpuRequestInterval);
            namespaceCpuRequestIntervalArray.put(namespaceCpuRequestInterval);
        }
        return namespaceCpuRequestIntervalArray;
    }

    @Override
    public RecommendationConfigItem getMemoryRequestRecommendationForNamespace(Map<Timestamp, IntervalResults> filteredResultsMap, ArrayList<RecommendationNotification> notifications) {
        boolean setNotification = true;
        if (null == notifications) {
            LOGGER.error("Notifications Object passed is empty. The notifications are not sent as part of recommendation.");
            setNotification = false;
        }
        RecommendationConfigItem recommendationConfigItem = null;
        String format;

        List<Double> namespaceMemUsageList = new ArrayList<>();
        for (IntervalResults intervalResults: filteredResultsMap.values()) {
            JSONObject jsonObject = calculateNamespaceMemoryUsage(intervalResults);
            Double namespaceMemUsage = jsonObject.getDouble(KruizeConstants.JSONKeys.MAX);
            namespaceMemUsageList.add(namespaceMemUsage);
        }

        List<Double> spikeList = filteredResultsMap.values()
                .stream()
                .map(CostBasedRecommendationModel::calculateIntervalSpikeForNamespace)
                .collect(Collectors.toList());

        Double namespaceMemRecUsage = calculatePercentile(namespaceMemUsageList, COST_MEMORY_PERCENTILE);
        Double namespaceMemRecUsageBuf = namespaceMemRecUsage + (namespaceMemRecUsage * MEM_USAGE_BUFFER_DECIMAL);

        Double namespaceMemRecSpike = calculatePercentile(spikeList, COST_MEMORY_PERCENTILE);
        namespaceMemRecSpike += (namespaceMemRecSpike * MEM_SPIKE_BUFFER_DECIMAL);
        Double namespaceMemRecSpikeBuf = namespaceMemRecUsage + namespaceMemRecSpike;

        Double namespaceMemRec = Math.min(namespaceMemRecUsageBuf, namespaceMemRecSpikeBuf);

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

    @Override
    public Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> getAcceleratorRequestRecommendation (
            Map<Timestamp, IntervalResults> filteredResultsMap,
            ArrayList<RecommendationNotification> notifications
    ) {
        List<Double> acceleratorCoreMaxValues = new ArrayList<>();
        List<Double> acceleratorMemoryMaxValues = new ArrayList<>();

        boolean isGpuWorkload = false;
        String acceleratorModel = null;

        for (Map.Entry<Timestamp, IntervalResults> entry : filteredResultsMap.entrySet()) {
            IntervalResults intervalResults = entry.getValue();

            // Skip if accelerator map is null
            if (null == intervalResults.getAcceleratorMetricResultHashMap())
                continue;

            // Skip if map is empty
            if (intervalResults.getAcceleratorMetricResultHashMap().isEmpty())
                continue;

            isGpuWorkload = true;

            for (Map.Entry<AnalyzerConstants.MetricName, AcceleratorMetricResult> gpuEntry : intervalResults.getAcceleratorMetricResultHashMap().entrySet()) {
                AcceleratorMetricResult gpuMetricResult = gpuEntry.getValue();

                // Set Accelerator name
                // TODO: Need to handle separate processing in case of container supporting multiple accelerators
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

                boolean isCoreUsage = gpuEntry.getKey() == AnalyzerConstants.MetricName.gpuCoreUsage;
                boolean isMemoryUsage = (gpuEntry.getKey() == AnalyzerConstants.MetricName.gpuMemoryUsage)
                        || (gpuEntry.getKey() == AnalyzerConstants.MetricName.acceleratorMigMemoryUsage);

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

        if (!isGpuWorkload) {
            return null;
        }

        // Return null if entries are empty
        if (acceleratorCoreMaxValues.isEmpty() && acceleratorMemoryMaxValues.isEmpty())
            return null;

        double coreAverage = 0.0;
        if (!acceleratorCoreMaxValues.isEmpty())
            coreAverage = CommonUtils.percentile(COST_ACCELERATOR_PERCENTILE, acceleratorCoreMaxValues);

        double memoryAverage = 0.0;
        if (!acceleratorMemoryMaxValues.isEmpty())
            memoryAverage = CommonUtils.percentile(COST_ACCELERATOR_PERCENTILE, acceleratorMemoryMaxValues);

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
            LOGGER.info("Data irregularity detected, " +
                    "Notification needs to be added explaining we changed the core usage to 100% as it's more than 100%");
            coreFraction = 1;
        }
        double memoryFraction = memoryAverage / 100;
        // TODO: Need to investigate why data is faulty
        if (memoryFraction > 1) {
            LOGGER.info("Data irregularity detected, " +
                    "Notification needs to be added explaining we changed the memory usage to 100% as it's more than 100%");
            memoryFraction = 1;
        }

        return RecommendationUtils.getMapWithOptimalProfile(acceleratorModel, coreFraction, memoryFraction);
    }

    public static JSONObject calculateNamespaceMemoryUsage(IntervalResults intervalResults) {
        // create a JSON object which should be returned here having two values, Math.max and Collections.Min
        JSONObject jsonObject = new JSONObject();

        Optional<MetricResults> namespaceMemoryUsageResults = Optional.ofNullable(intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.namespaceMemoryUsage));

        double namespaceMemUsageMax = namespaceMemoryUsageResults.map(m -> m.getAggregationInfoResult().getMax()).orElse(0.0);
        double namespaceMemUsageMin = namespaceMemoryUsageResults.map(m -> m.getAggregationInfoResult().getMin()).orElse(0.0);

        // traverse over a stream of positive values and find the minimum value
        namespaceMemUsageMin = Stream.of(namespaceMemUsageMax, namespaceMemUsageMin)
                .filter(value -> value > 0.0)
                .min(Double::compare)
                .orElse(0.0);

        jsonObject.put(KruizeConstants.JSONKeys.MIN, namespaceMemUsageMin);
        jsonObject.put(KruizeConstants.JSONKeys.MAX, namespaceMemUsageMax);

        LOGGER.debug("memRequestInterval : {}", jsonObject);
        return jsonObject;
    }

    private static double calculateIntervalSpikeForNamespace(IntervalResults intervalResults) {
        Optional<MetricResults> namespaceMemoryUsageResults = Optional.ofNullable(intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.namespaceMemoryUsage));
        Optional<MetricResults> namespaceMemoryRSSResults = Optional.ofNullable(intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.namespaceMemoryRSS));
        double namespaceMemUsageMax = namespaceMemoryUsageResults.map(m -> m.getAggregationInfoResult().getMax()).orElse(0.0);
        double namespaceMemUsageMin = namespaceMemoryUsageResults.map(m -> m.getAggregationInfoResult().getMin()).orElse(0.0);
        double namespaceMemRSSMax = namespaceMemoryRSSResults.map(m -> m.getAggregationInfoResult().getMax()).orElse(0.0);
        double namespaceMemRSSMin = namespaceMemoryRSSResults.map(m -> m.getAggregationInfoResult().getMin()).orElse(0.0);

        return Math.max(Math.ceil(namespaceMemUsageMax - namespaceMemUsageMin), Math.ceil(namespaceMemRSSMax - namespaceMemRSSMin));
    }

}
