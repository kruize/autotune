package com.autotune.analyzer.plots;

import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.common.utils.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import static com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationEngine.PercentileConstants.*;

public class PlotManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlotManager.class);
    private HashMap<Timestamp, IntervalResults> containerResultsMap;
    private RecommendationConstants.RecommendationTerms recommendationTerm;
    private Timestamp monitoringStartTime;
    private Timestamp monitoringEndTime;

    public PlotManager(HashMap<Timestamp, IntervalResults> containerResultsMap, RecommendationConstants.RecommendationTerms recommendationTerm, Timestamp monitoringStartTime, Timestamp monitoringEndTime) {
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
        for (int i = delimiterNumber; i < resultInRange.size(); i = i + delimiterNumber) {
            PlotData.UsageData cpuUsage = getUsageData(sortedResultsHashMap.subMap(timestampList.get(i), true, incrementStartTime, true), AnalyzerConstants.MetricName.cpuUsage, "cores");
            PlotData.UsageData memoryUsage = getUsageData(sortedResultsHashMap.subMap(timestampList.get(i), true, incrementStartTime, true), AnalyzerConstants.MetricName.memoryUsage, "MiB");
            plotsDataMap.put(timestampList.get(i), new PlotData.PlotPoint(cpuUsage, memoryUsage));
            incrementStartTime = timestampList.get(i);
        }
        PlotData.UsageData cpuUsage = getUsageData(sortedResultsHashMap.subMap(monitoringEndTime, true, incrementStartTime, true), AnalyzerConstants.MetricName.cpuUsage, "cores");
        PlotData.UsageData memoryUsage = getUsageData(sortedResultsHashMap.subMap(monitoringEndTime, true, incrementStartTime, true), AnalyzerConstants.MetricName.memoryUsage, "MiB");
        plotsDataMap.put(timestampList.get(resultInRange.size() - 1), new PlotData.PlotPoint(cpuUsage, memoryUsage));

        return new PlotData.PlotsData(recommendationTerm.getPlots_datapoints(), plotsDataMap);
    }

    PlotData.UsageData getUsageData(Map<Timestamp, IntervalResults> resultInRange, AnalyzerConstants.MetricName metricName, String format) {
        // Extract CPU values
        List<Double> cpuValues = resultInRange.values().stream()
                .filter(intervalResults -> intervalResults.getMetricResultsMap().containsKey(metricName))
                .mapToDouble(intervalResults -> {
                    MetricResults metricResults = intervalResults.getMetricResultsMap().get(metricName);
                    return (metricResults != null && metricResults.getAggregationInfoResult() != null) ? metricResults.getAggregationInfoResult().getSum() : 0.0;
                })
                .boxed() // Convert double to Double
                .collect(Collectors.toList());
        double q1 = CommonUtils.percentile(TWENTYFIVE_PERCENTILE, cpuValues);
        double q3 = CommonUtils.percentile(SEVENTYFIVE_PERCENTILE, cpuValues);
        double median = CommonUtils.percentile(FIFTY_PERCENTILE, cpuValues);
        // Find max and min
        double max = Collections.max(cpuValues);
        double min = Collections.min(cpuValues);

        return new PlotData.UsageData(min, q1, median, q3, max, format);
    }
}
