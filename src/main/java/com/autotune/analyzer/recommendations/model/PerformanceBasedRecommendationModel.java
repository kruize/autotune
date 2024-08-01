package com.autotune.analyzer.recommendations.model;

import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.recommendations.RecommendationNotification;
import com.autotune.analyzer.services.UpdateRecommendations;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.gpuMetaData.GpuMetaDataService;
import com.autotune.common.data.gpuMetaData.GpuProfile;
import com.autotune.common.data.metrics.MetricAggregationInfoResults;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.GpuMetricResult;
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
        JSONArray cpuUsageList = CostBasedRecommendationModel.getCPUUsageList(filteredResultsMap);
        LOGGER.debug("cpuUsageList : {}", cpuUsageList);
        // Extract "max" values from cpuUsageList
        List<Double> cpuMaxValues = new ArrayList<>();
        for (int i = 0; i < cpuUsageList.length(); i++) {
            JSONObject jsonObject = cpuUsageList.getJSONObject(i);
            double maxValue = jsonObject.getDouble(KruizeConstants.JSONKeys.MAX);
            cpuMaxValues.add(maxValue);
        }

        Double cpuRequest = 0.0;
        Double cpuRequestMax = Collections.max(cpuMaxValues);
        if (null != cpuRequestMax && CPU_ONE_CORE > cpuRequestMax) {
            cpuRequest = cpuRequestMax;
        } else {
            cpuRequest = CommonUtils.percentile(PERFORMANCE_CPU_PERCENTILE, cpuMaxValues);
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
    public RecommendationConfigItem getCPURequestRecommendationForNamespace(Map<Timestamp, IntervalResults> filteredResultsMap, ArrayList<RecommendationNotification> notifications) {
        boolean setNotification = true;
        if (null == notifications) {
            LOGGER.error("Notifications Object passed is empty. The notifications are not sent as part of recommendation.");
            setNotification = false;
        }
        RecommendationConfigItem recommendationConfigItem = null;
        String format = "";
        JSONArray cpuUsageList = CostBasedRecommendationModel.getCPUUsageListForNamespace(filteredResultsMap);
        LOGGER.debug("cpuUsageList : {}", cpuUsageList);
        // Extract "max" values from cpuUsageList
        List<Double> cpuMaxValues = new ArrayList<>();
        for (int i = 0; i < cpuUsageList.length(); i++) {
            JSONObject jsonObject = cpuUsageList.getJSONObject(i);
            double maxValue = jsonObject.getDouble(KruizeConstants.JSONKeys.MAX);
            cpuMaxValues.add(maxValue);
        }

        Double cpuRequest = 0.0;
        Double cpuRequestMax = Collections.max(cpuMaxValues);
        if (null != cpuRequestMax && CPU_ONE_CORE > cpuRequestMax) {
            cpuRequest = cpuRequestMax;
        } else {
            cpuRequest = CommonUtils.percentile(PERFORMANCE_CPU_PERCENTILE, cpuMaxValues);
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
            MetricResults cpuUsageResults = intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.namespaceCpuUsage);
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


    public RecommendationConfigItem getMemoryRequestRecommendationForNamespace(Map<Timestamp, IntervalResults> filteredResultsMap,
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
                    Optional<MetricResults> cpuUsageResults = Optional.ofNullable(e.getMetricResultsMap().get(AnalyzerConstants.MetricName.namespaceCpuUsage));
                    double cpuUsageAvg = cpuUsageResults.map(m -> m.getAggregationInfoResult().getAvg()).orElse(0.0);
                    double cpuUsageMax = cpuUsageResults.map(m -> m.getAggregationInfoResult().getSum()).orElse(0.0);
                    Optional<MetricResults> memoryUsageResults = Optional.ofNullable(e.getMetricResultsMap().get(AnalyzerConstants.MetricName.namespaceMemoryUsage));
                    double memUsageAvg = memoryUsageResults.map(m -> m.getAggregationInfoResult().getAvg()).orElse(0.0);
                    double memUsageMax = memoryUsageResults.map(m -> m.getAggregationInfoResult().getMax()).orElse(0.0);
                    double memUsage = 0;
                    int numPods = 0;

                    if (0 != cpuUsageAvg) {
                        numPods = (int) Math.ceil(cpuUsageMax / cpuUsageAvg);
                    }
                    // If numPods is still zero, could be because there is no CPU info
                    // We can use mem data to calculate pods, this is not as reliable as cpu
                    // but better than nothing!
                    if (0 == numPods) {
                        if (0 != memUsageAvg) {
                            numPods = (int) Math.ceil(memUsageMax / memUsageAvg);
                        }
                    }
                    if (0 < numPods) {
                        memUsage = (memUsageMax / numPods);
                    }
                    memUsage = Math.max(memUsage, memUsageMax);

                    return memUsage;
                })
                .collect(Collectors.toList());

        // spikeList is the max spike observed in each measurementDuration
        List<Double> spikeList = filteredResultsMap.values()
                .stream()
                .map(e -> {
                    Optional<MetricResults> memoryUsageResults = Optional.ofNullable(e.getMetricResultsMap().get(AnalyzerConstants.MetricName.namespaceMemoryUsage));
                    Optional<MetricResults> memoryRSSResults = Optional.ofNullable(e.getMetricResultsMap().get(AnalyzerConstants.MetricName.namespaceMemoryRSS));
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
            MetricResults memoryUsageResults = intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.namespaceMemoryUsage);
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
    public Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> getGpuRequestRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap, ArrayList<RecommendationNotification> notifications) {
        double totalCoreMax = 0.0;
        double totalMemoryMax = 0.0;

        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> returnMap = new HashMap<>();

        boolean isGpuWorkload = false;


        for (Map.Entry<Timestamp, IntervalResults> entry : filteredResultsMap.entrySet()) {
            IntervalResults intervalResults = entry.getValue();

            if (intervalResults.getGpuMetricResultHashMap() != null) {
                isGpuWorkload = true;
                for (Map.Entry<AnalyzerConstants.MetricName, GpuMetricResult> gpuEntry : intervalResults.getGpuMetricResultHashMap().entrySet()) {

                    GpuMetricResult gpuMetricResult = gpuEntry.getValue();

                    MetricResults metricResults = gpuMetricResult.getMetricResults();

                    if (metricResults != null && metricResults.getAggregationInfoResult() != null) {
                        MetricAggregationInfoResults aggregationInfo = metricResults.getAggregationInfoResult();

                        if (aggregationInfo.getMax() != null) {
                            if (gpuEntry.getKey() == AnalyzerConstants.MetricName.gpuCoreUsage) {
                                if (aggregationInfo.getMax() > totalCoreMax) {
                                    totalCoreMax = aggregationInfo.getMax();
                                }
                            }
                            if (gpuEntry.getKey() == AnalyzerConstants.MetricName.gpuMemoryUsage) {
                                if (aggregationInfo.getMax() > totalMemoryMax) {
                                    totalMemoryMax = aggregationInfo.getMax();
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!isGpuWorkload) {
            return null;
        }

        double coreFraction = totalCoreMax / 100;
        double memoryFraction = totalMemoryMax / 100;
        GpuMetaDataService gpuMetaDataService = GpuMetaDataService.getInstance();
        GpuProfile gpuProfile = gpuMetaDataService.getGpuProfile(AnalyzerConstants.SupportedGPUs.A100_40_GB, coreFraction, memoryFraction);
        RecommendationConfigItem recommendationConfigItem = new RecommendationConfigItem(1.0, "cores");
        if (gpuProfile.getProfileName().equalsIgnoreCase("1g.5gb")) {
            returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_1_CORE_5GB, recommendationConfigItem);
        } else if (gpuProfile.getProfileName().equalsIgnoreCase("1g.10gb")) {
            returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_1_CORE_10GB, recommendationConfigItem);
        } else if (gpuProfile.getProfileName().equalsIgnoreCase("1g.20gb")) {
            returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_1_CORE_20GB, recommendationConfigItem);
        } else if (gpuProfile.getProfileName().equalsIgnoreCase("2g.10gb")) {
            returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_2_CORES_10GB, recommendationConfigItem);
        } else if (gpuProfile.getProfileName().equalsIgnoreCase("2g.20gb")) {
            returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_2_CORES_20GB, recommendationConfigItem);
        } else if (gpuProfile.getProfileName().equalsIgnoreCase("3g.20gb")) {
            returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_3_CORES_20GB, recommendationConfigItem);
        } else if (gpuProfile.getProfileName().equalsIgnoreCase("3g.40gb")) {
            returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_3_CORES_40GB, recommendationConfigItem);
        } else if (gpuProfile.getProfileName().equalsIgnoreCase("4g.20gb")) {
            returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_4_CORES_20GB, recommendationConfigItem);
        } else if (gpuProfile.getProfileName().equalsIgnoreCase("4g.40gb")) {
            returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_4_CORES_40GB, recommendationConfigItem);
        } else if (gpuProfile.getProfileName().equalsIgnoreCase("7g.40gb")) {
            returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_7_CORES_40GB, recommendationConfigItem);
        } else if (gpuProfile.getProfileName().equalsIgnoreCase("7g.80gb")) {
            returnMap.put(AnalyzerConstants.RecommendationItem.NVIDIA_GPU_PARTITION_7_CORES_80GB, recommendationConfigItem);
        }
        System.out.println(gpuProfile.getProfileName());
        return returnMap;
    }

    @Override
    public String getModelName() {
        return this.name;
    }

    @Override
    public void validate() {

    }
}
