package com.autotune.analyzer.plots;

import com.autotune.analyzer.recommendations.model.CostBasedRecommendationModel;
import com.autotune.analyzer.recommendations.model.GenericRecommendationModel;
import com.autotune.analyzer.recommendations.term.Terms;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.common.utils.CommonUtils;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.*;

import static com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationEngine.PercentileConstants.*;
import static com.autotune.analyzer.utils.AnalyzerConstants.ExperimentType.NAMESPACE;
import static com.autotune.analyzer.utils.AnalyzerConstants.MetricName.*;

public class PlotManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlotManager.class);
    private final HashMap<Timestamp, IntervalResults> resultsMap;
    private final Terms recommendationTerm;
    private final Timestamp monitoringStartTime;
    private final Timestamp monitoringEndTime;

    public PlotManager(HashMap<Timestamp, IntervalResults> resultsMap, Terms recommendationTerm, Timestamp monitoringStartTime, Timestamp monitoringEndTime) {
        this.resultsMap = resultsMap;
        this.recommendationTerm = recommendationTerm;
        this.monitoringStartTime = monitoringStartTime;
        this.monitoringEndTime = monitoringEndTime;
    }

    public PlotData.PlotsData generatePlots(AnalyzerConstants.ExperimentType experimentType) {

        AnalyzerConstants.MetricName cpuMetric = experimentType == NAMESPACE ? namespaceCpuUsage : cpuUsage;
        AnalyzerConstants.MetricName memMetric = experimentType == NAMESPACE ? namespaceMemoryUsage : memoryUsage;

        // Convert the HashMap to a TreeMap to maintain sorted order based on IntervalEndTime
        TreeMap<Timestamp, IntervalResults> sortedResultsHashMap = new TreeMap<>(Collections.reverseOrder());
        sortedResultsHashMap.putAll(resultsMap);
        // Retrieve entries within the specified range
        Map<Timestamp, IntervalResults> resultInRange = sortedResultsHashMap.subMap(monitoringEndTime, true, monitoringStartTime, false);

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
                    incrementStartTime,false), cpuMetric);
            PlotData.UsageData memoryUsage = getUsageData(sortedResultsHashMap.subMap(newTimestamp, true,
                    incrementStartTime, false), memMetric);
            plotsDataMap.put(newTimestamp, new PlotData.PlotPoint(cpuUsage, memoryUsage));
            incrementStartTime = newTimestamp;
        }

        return new PlotData.PlotsData(recommendationTerm.getPlots_datapoints(), plotsDataMap);
    }

    PlotData.UsageData getUsageData(Map<Timestamp, IntervalResults> resultInRange, AnalyzerConstants.MetricName metricName) {
        // stream through the results value and extract the CPU values
        try {
            if (metricName == AnalyzerConstants.MetricName.cpuUsage || metricName == AnalyzerConstants.MetricName.namespaceCpuUsage) {
                List<Double> cpuMaxValues = new ArrayList<>();
                List<Double> cpuMinValues = new ArrayList<>();
                boolean namespaceCpu = metricName == AnalyzerConstants.MetricName.namespaceCpuUsage;
                for (IntervalResults intervalResults : resultInRange.values()) {
                    GenericRecommendationModel.IntervalBound bound = namespaceCpu
                            ? GenericRecommendationModel.namespaceCpuUsageBound(intervalResults)
                            : GenericRecommendationModel.cpuUsageBound(intervalResults);
                    cpuMaxValues.add(bound.max);
                    cpuMinValues.add(bound.min);
                }
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("cpuMaxValues : {}", cpuMaxValues);
                    LOGGER.debug("cpuMinValues : {}", cpuMinValues);
                }
                if (!cpuMaxValues.isEmpty()) {
                    return getPercentileData(cpuMaxValues, cpuMinValues, resultInRange, metricName);
                }

            } else {
                // loop through the results value and extract the memory values
                List<Double> memUsageMinList = new ArrayList<>();
                List<Double> memUsageMaxList = new ArrayList<>();
                boolean memDataAvailable = false;
                boolean namespaceMemory = metricName == namespaceMemoryUsage;
                for (IntervalResults intervalResults: resultInRange.values()) {
                    GenericRecommendationModel.IntervalBound bound = namespaceMemory
                            ? GenericRecommendationModel.namespaceMemoryUsageBound(intervalResults)
                            : GenericRecommendationModel.memoryUsageBound(intervalResults);
                    // calculateMemoryUsage always returns min and max, so the JSON object was never empty
                    memDataAvailable = true;
                    memUsageMaxList.add(bound.max);
                    memUsageMinList.add(bound.min);
                }
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("memValues Max : {}, Min : {}", memUsageMaxList, memUsageMinList);
                }
                if (memDataAvailable)
                    return getPercentileData(memUsageMaxList, memUsageMinList, resultInRange, metricName);
            }
        } catch (JSONException e) {
            LOGGER.error("Exception occurred while extracting metric values: {}", e.getMessage());
        }
        return null;
    }

    private PlotData.UsageData getPercentileData(List<Double> metricValuesMax, List<Double> metricValuesMin, Map<Timestamp, IntervalResults> resultInRange, AnalyzerConstants.MetricName metricName) {
        try {
            if (!metricValuesMax.isEmpty()) {
                Collections.sort(metricValuesMax);
                double q1 = CommonUtils.percentileOfSorted(TWENTYFIVE_PERCENTILE, metricValuesMax);
                double q3 = CommonUtils.percentileOfSorted(SEVENTYFIVE_PERCENTILE, metricValuesMax);
                double median = CommonUtils.percentileOfSorted(FIFTY_PERCENTILE, metricValuesMax);
                // Find max and min
                double max = Collections.max(metricValuesMax);
                double min;
                // check for non zero values
                boolean nonZeroCheck = metricValuesMin.stream().noneMatch(value -> value.equals(0.0));
                if (nonZeroCheck) {
                    min = Collections.min(metricValuesMin);
                } else {
                    min = 0.0;
                }

                LOGGER.debug("q1 : {}, q3 : {}, median : {}, max : {}, min : {}", q1, q3, median, max, min);
                String format = CostBasedRecommendationModel.getFormatValue(resultInRange, metricName);
                return new PlotData.UsageData(min, q1, median, q3, max, format);
            } else {
                return null;
            }
        } catch (Exception e) {
            LOGGER.error("Exception occurred while generating percentiles: {}", e.getMessage());
        }
        return null;
    }
}
