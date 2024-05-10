package com.autotune.analyzer.plots;

import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.sql.Timestamp;
import java.util.Map;

public class PlotData {
    public static class UsageData {
        public Double min;
        public double q1;
        public double median;
        public double q3;
        public double max;
        public String format;

        public UsageData(Double min, double q1, double median, double q3, double max, String format) {
            this.min = (min != null && min.equals(0.0)) ? null : min; // Conditionally set min to null if it's 0.0
            this.q1 = q1;
            this.median = median;
            this.q3 = q3;
            this.max = max;
            this.format = format;
        }
    }

    public static class PlotPoint {
        public UsageData cpuUsage;
        public UsageData memoryUsage;

        public PlotPoint(UsageData cpuUsage, UsageData memoryUsage) {
            this.cpuUsage = cpuUsage;
            this.memoryUsage = memoryUsage;
        }
    }

    public static class PlotsData {
        @SerializedName(KruizeConstants.JSONKeys.PLOTS_DATAPOINTS)
        public int datapoints;
        @SerializedName(KruizeConstants.JSONKeys.PLOTS_DATA)
        public Map<Timestamp, PlotPoint> plotsData;

        public PlotsData(int datapoints, Map<Timestamp, PlotPoint> plotsData) {
            this.datapoints = datapoints;
            this.plotsData = plotsData;
        }
    }

}
