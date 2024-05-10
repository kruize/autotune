package com.autotune.analyzer.plots;

import com.autotune.analyzer.recommendations.model.CostBasedRecommendationModel;
import com.autotune.analyzer.recommendations.term.Terms;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.common.utils.CommonUtils;
import com.autotune.utils.KruizeConstants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.*;

import static com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationEngine.PercentileConstants.*;

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
        Map<Timestamp, IntervalResults> resultInRange = sortedResultsHashMap.subMap(monitoringEndTime, true, monitoringStartTime, false);

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
        // stream through the results value and extract the CPU values
        try {
            if (metricName.equals(AnalyzerConstants.MetricName.cpuUsage)) {
                JSONArray cpuValues = CostBasedRecommendationModel.getCPUUsageList(resultInRange);
                LOGGER.debug("cpuValues : {}", cpuValues);
                if (!cpuValues.isEmpty()) {
                    // Extract "max" values from cpuUsageList
                    List<Double> cpuMaxValues = new ArrayList<>();
                    List<Double> cpuMinValues = new ArrayList<>();
                    for (int i = 0; i < cpuValues.length(); i++) {
                        JSONObject jsonObject = cpuValues.getJSONObject(i);
                        double maxValue = jsonObject.getDouble(KruizeConstants.JSONKeys.MAX);
                        double minValue = jsonObject.getDouble(KruizeConstants.JSONKeys.MIN);
                        cpuMaxValues.add(maxValue);
                        cpuMinValues.add(minValue);
                    }
                    LOGGER.debug("cpuMaxValues : {}", cpuMaxValues);
                    LOGGER.debug("cpuMinValues : {}", cpuMinValues);
                    return getPercentileData(cpuMaxValues, cpuMinValues, resultInRange, metricName);
                }

            } else {
                // loop through the results value and extract the memory values
                CostBasedRecommendationModel costBasedRecommendationModel  = new CostBasedRecommendationModel();
                List<Double> memUsageMinList = new ArrayList<>();
                List<Double> memUsageMaxList = new ArrayList<>();
                boolean memDataAvailable = false;
                for (IntervalResults intervalResults: resultInRange.values()) {
                    JSONObject jsonObject = costBasedRecommendationModel.calculateMemoryUsage(intervalResults);
                    if (!jsonObject.isEmpty()) {
                        memDataAvailable = true;
                        Double memUsageMax = jsonObject.getDouble(KruizeConstants.JSONKeys.MAX);
                        Double memUsageMin = jsonObject.getDouble(KruizeConstants.JSONKeys.MIN);
                        memUsageMaxList.add(memUsageMax);
                        memUsageMinList.add(memUsageMin);
                    }
                }
                LOGGER.debug("memValues Max : {}, Min : {}", memUsageMaxList, memUsageMinList);
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
                double q1 = CommonUtils.percentile(TWENTYFIVE_PERCENTILE, metricValuesMax);
                double q3 = CommonUtils.percentile(SEVENTYFIVE_PERCENTILE, metricValuesMax);
                double median = CommonUtils.percentile(FIFTY_PERCENTILE, metricValuesMax);
                // Find max and min
                double max = Collections.max(metricValuesMax);
                double min = Collections.min(metricValuesMin);
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
