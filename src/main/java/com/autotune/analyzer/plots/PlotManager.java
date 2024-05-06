package com.autotune.analyzer.plots;

import com.autotune.analyzer.recommendations.term.Terms;
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

import static com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationEngine.PercentileConstants.*;
import static com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationValueConstants.CPU_ONE_CORE;

public class PlotManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlotManager.class);
    private HashMap<Timestamp, IntervalResults> containerResultsMap;
    private Terms recommendationTerm;
    private Timestamp monitoringStartTime;
    private Timestamp monitoringEndTime;

    public PlotManager(HashMap<Timestamp, IntervalResults> containerResultsMap, Terms recommendationTerm, Timestamp monitoringStartTime, Timestamp monitoringEndTime) {
        this.containerResultsMap = containerResultsMap;
        this.recommendationTerm = recommendationTerm;
        this.monitoringStartTime = monitoringStartTime;
        this.monitoringEndTime = monitoringEndTime;
    }

    public PlotData.PlotsData generatePlots() {

        // Convert the HashMap to a TreeMap to maintain sorted order based on IntervalEndTime
        TreeMap<Timestamp, IntervalResults> sortedResultsHashMap = new TreeMap<>(Collections.reverseOrder());
        sortedResultsHashMap.putAll(containerResultsMap);

        // Retrieve entries within the specified range
        Map<Timestamp, IntervalResults> resultInRange = sortedResultsHashMap.subMap(monitoringEndTime, true, monitoringStartTime, true);

        int delimiterNumber = (int) (resultInRange.size() / recommendationTerm.getPlots_datapoints());

        // Convert set to list
        List<Timestamp> timestampList = new LinkedList<>(resultInRange.keySet());
        // Sort the LinkedList
        Collections.sort(timestampList);

        Map<Timestamp, PlotData.PlotPoint> plotsDataMap = new HashMap<>();
        Timestamp incrementStartTime = monitoringStartTime;

        // Convert the Timestamp to a Calendar
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(incrementStartTime.getTime());

        for (int i = 0; i < recommendationTerm.getPlots_datapoints(); i++) {
            // Add days to the Calendar
            double daysToAdd = recommendationTerm.getPlots_datapoints_delta_in_days();
            long millisecondsToAdd = (long) (daysToAdd * 24 * 60 * 60 * 1000); // Convert days to milliseconds
            calendar.add(Calendar.MILLISECOND, (int) millisecondsToAdd);
            // Convert the modified Calendar back to a Timestamp
            Timestamp newTimestamp = new Timestamp(calendar.getTimeInMillis());
            PlotData.UsageData cpuUsage = getUsageData(sortedResultsHashMap.subMap(newTimestamp, true,
                    incrementStartTime,false), AnalyzerConstants.MetricName.cpuUsage);
            PlotData.UsageData memoryUsage = getUsageData(sortedResultsHashMap.subMap(newTimestamp, true,
                    incrementStartTime, false), AnalyzerConstants.MetricName.memoryUsage);
            plotsDataMap.put(newTimestamp, new PlotData.PlotPoint(cpuUsage, memoryUsage));
            incrementStartTime = newTimestamp;
        }

        return new PlotData.PlotsData(recommendationTerm.getPlots_datapoints(), plotsDataMap);
    }

    PlotData.UsageData getUsageData(Map<Timestamp, IntervalResults> resultInRange, AnalyzerConstants.MetricName metricName) {
        // stream through the results value and extract the format and the metric values
        PlotData.UsageData usageData = resultInRange.values().stream()
                .filter(intervalResults -> intervalResults.getMetricResultsMap().containsKey(metricName))
                .map(intervalResults -> {
                    MetricResults metricResults = intervalResults.getMetricResultsMap().get(metricName);
                    double metricUsageAvg = Optional.ofNullable(metricResults)
                            .map(MetricResults::getAggregationInfoResult)
                            .map(MetricAggregationInfoResults::getAvg)
                            .orElse(0.0);
                    double metricUsageMax = Optional.ofNullable(metricResults)
                            .map(MetricResults::getAggregationInfoResult)
                            .map(MetricAggregationInfoResults::getMax)
                            .orElse(0.0);
                    double metricUsageSum = Optional.ofNullable(metricResults)
                            .map(MetricResults::getAggregationInfoResult)
                            .map(MetricAggregationInfoResults::getSum)
                            .orElse(0.0);

                    String format = Optional.ofNullable(metricResults)
                            .map(MetricResults::getAggregationInfoResult)
                            .map(MetricAggregationInfoResults::getFormat)
                            .orElse(null);

                    return new AbstractMap.SimpleEntry<>(format, getMetricRequestInterval(metricUsageMax, metricUsageAvg, metricUsageSum));
                })
                .filter(entry -> entry.getValue() != null) // Filter out entries where metric is null
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        list -> {
                            if (list.isEmpty()) {
                                return null;
                            } else {
                                List<Double> metricValues = list.stream().map(Map.Entry::getValue).collect(Collectors.toList());
                                String format = list.get(0).getKey(); // Since format is common, take it from any entry
                                double q1 = CommonUtils.percentile(TWENTYFIVE_PERCENTILE, metricValues);
                                double q3 = CommonUtils.percentile(SEVENTYFIVE_PERCENTILE, metricValues);
                                double median = CommonUtils.percentile(FIFTY_PERCENTILE, metricValues);
                                // Find max and min
                                double max = Collections.max(metricValues);
                                double min = Collections.min(metricValues);
                                LOGGER.debug("q1 : {}, q3 : {}, median : {}, max : {}, min : {}", q1, q3, median, max, min);
                                return new PlotData.UsageData(min, q1, median, q3, max, format);
                            }
                        }));

        LOGGER.debug("usageData : {}", usageData);
        return usageData;
    }

    private static double getMetricRequestInterval(double metricUsageMax, double metricUsageAvg, double metricUsageSum) {
        double metricUsage = (metricUsageMax > 0) ? metricUsageMax : metricUsageAvg;

        double metricRequestInterval;
        double cpuUsagePod = 0;
        int numPods;

        if (CPU_ONE_CORE > metricUsage) {
            metricRequestInterval = metricUsage;
        } else {
            if (metricUsageAvg != 0) {
                numPods = (int) Math.ceil(metricUsageSum / metricUsageAvg);
                cpuUsagePod = (numPods > 0) ? metricUsageSum / numPods : 0.0;
            }
            metricRequestInterval = Math.max(cpuUsagePod, metricUsage);
        }
        return metricRequestInterval;
    }
}
