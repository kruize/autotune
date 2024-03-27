package com.autotune.analyzer.recommendations.term;

import com.autotune.analyzer.recommendations.objects.TermRecommendations;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.utils.KruizeConstants;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.autotune.utils.KruizeConstants.JSONKeys.*;
import static com.autotune.utils.KruizeConstants.RecommendationEngineConstants.DurationBasedEngine.RecommendationDurationRanges.*;

public class Terms {
    int days;
    double threshold_in_days;
    String name;
    String performanceProfile;
    private int plots_datapoints;
    private double plots_datapoints_delta_in_days;

    public Terms(String name, int days, double threshold_in_days,int plots_datapoints, double plots_datapoints_delta_in_days) {
        this.name = name;
        this.days = days;
        this.threshold_in_days = threshold_in_days;
	this.plots_datapoints = plots_datapoints;
        this.plots_datapoints_delta_in_days = plots_datapoints_delta_in_days;
    }

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        this.days = days;
    }

    public double getThreshold_in_days() {
        return threshold_in_days;
    }

    public void setThreshold_in_days(double threshold_in_days) {
        this.threshold_in_days = threshold_in_days;
    }

    public String getPerformanceProfile() {
        return performanceProfile;
    }

    public void setPerformanceProfile(String performanceProfile) {
        this.performanceProfile = performanceProfile;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static int getMaxDays(Map<String, Terms> terms) {
        Optional<Terms> maxTerms = terms.values().stream()
                .max(Comparator.comparingInt(term -> term.days));
        return maxTerms.map(term -> term.days).orElse(0); // Return the max days or 0 if terms is empty
    }

    public static boolean checkIfMinDataAvailableForTerm(ContainerData containerData, Terms term, Timestamp monitoringEndTime,
                                                         double measurementDuration) {
        // Check if data is available
        if (null == containerData || null == containerData.getResults() || containerData.getResults().isEmpty()) {
            return false;
        }

        // Initialize sum of durations
        double sum = 0;
        // Threshold in milliseconds
        long thresholdInMillis = KruizeConstants.TimeConv.MEASUREMENT_DURATION_THRESHOLD_SECONDS * 1000;

        // Iterate backward in time
        for (Map.Entry<Timestamp, IntervalResults> entry : containerData.getResults().entrySet()) {
            IntervalResults intervalResults = entry.getValue();
            Timestamp intervalStartTime = intervalResults.getIntervalStartTime();
            Timestamp intervalEndTime = intervalResults.getIntervalEndTime();

            // Calculate the exact difference between "interval_end_time" and "interval_start_time" in seconds
            long differenceInSeconds = (intervalEndTime.getTime() - intervalStartTime.getTime()) / 1000;

            // Subtract the exact difference from the monitoringEndTime in each iteration
            Timestamp currentTimestamp = Timestamp.valueOf(monitoringEndTime.toLocalDateTime().minusSeconds(differenceInSeconds));

            // Check if the currentTimestamp exists in the resultsMap as it is or within the tolerance range
            if (containerData.getResults().containsKey(currentTimestamp) || isWithinThreshold(currentTimestamp,
                    containerData.getResults().keySet(), thresholdInMillis)) {
                // If present within the threshold range, add the duration of the interval to the sum
                sum += intervalResults.getDurationInMinutes();
            }
        }

        double minimumDurationInMins = term.getThreshold_in_days() * KruizeConstants.TimeConv.NO_OF_HOURS_PER_DAY *
                KruizeConstants.TimeConv.NO_OF_MINUTES_PER_HOUR;
        // Set bounds to check if we get minimum requirement satisfied
        double lowerBound = minimumDurationInMins - MEASUREMENT_DURATION_BUFFER_IN_MINS;
        // We don't consider upper bound to check if sum is in-between as we may over shoot and end-up resulting false
        if (sum >= lowerBound)
            return true;

        return false;
    }

    private static boolean isWithinThreshold(Timestamp currentTimestamp, Set<Timestamp> timestamps, long thresholdInMillis) {
        for (Timestamp timestamp : timestamps) {
            if (Math.abs(currentTimestamp.getTime() - timestamp.getTime()) <= thresholdInMillis) {
                return true;
            }
        }
        return false;
    }

    public static double getDurationSummation(ContainerData containerData) {
        // Loop over the data to check if there is min data available
        double sum = 0.0;
        for (IntervalResults intervalResults : containerData.getResults().values()) {
            sum = sum + intervalResults.getDurationInMinutes();
        }
        return sum;
    }

    public static void setDurationBasedOnTerm(ContainerData containerDataKruizeObject, TermRecommendations
            mappedRecommendationForTerm, String recommendationTerm) {

        double durationSummation = getDurationSummation(containerDataKruizeObject);
        durationSummation = Double.parseDouble(String.format("%.1f", durationSummation));
        // Get the maximum duration allowed for the term
        double maxDurationInHours = getMaxDuration(recommendationTerm);
        double maxDurationInMinutes = maxDurationInHours * KruizeConstants.TimeConv.NO_OF_MINUTES_PER_HOUR;
        // Set durationSummation to the maximum duration if it exceeds the maximum duration
        if (durationSummation > maxDurationInMinutes) {
            durationSummation = maxDurationInMinutes;
        }
        double durationSummationInHours = durationSummation / KruizeConstants.TimeConv.NO_OF_MINUTES_PER_HOUR;
        mappedRecommendationForTerm.setDurationInHrs(durationSummationInHours);
    }

    public static double getMaxDuration(String termValue) {
        return switch (termValue) {
            case SHORT_TERM -> SHORT_TERM_HOURS;
            case MEDIUM_TERM -> MEDIUM_TERM_HOURS;
            case LONG_TERM -> LONG_TERM_HOURS;
            default -> throw new IllegalStateException("Unexpected value: " + termValue);
        };
    }

    public static Timestamp getMonitoringStartTime(Timestamp endTime, int durationInDays) {

        double durationInHrs = KruizeConstants.TimeConv.NO_OF_HOURS_PER_DAY * durationInDays;
        Timestamp intervalEndTime;
        try {
            // Convert Timestamp to LocalDateTime
            LocalDateTime localDateTime = endTime.toLocalDateTime();
            long maxTermDuration = (long) durationInHrs;
            // Subtract hours
            LocalDateTime newLocalDateTime = localDateTime.minusHours(maxTermDuration);
            // Convert back to Timestamp
            intervalEndTime = Timestamp.valueOf(newLocalDateTime);
            return intervalEndTime;
        } catch (NullPointerException npe) {
            return null;
        }
    }
    public int getPlots_datapoints() {
        return plots_datapoints;
    }

    public void setPlots_datapoints(int plots_datapoints) {
        this.plots_datapoints = plots_datapoints;
    }

    public double getPlots_datapoints_delta_in_days() {
        return plots_datapoints_delta_in_days;
    }

    public void setPlots_datapoints_delta_in_days(double plots_datapoints_delta_in_days) {
        this.plots_datapoints_delta_in_days = plots_datapoints_delta_in_days;
    }
}
