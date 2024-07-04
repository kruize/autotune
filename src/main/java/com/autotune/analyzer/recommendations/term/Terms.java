package com.autotune.analyzer.recommendations.term;

import com.autotune.analyzer.recommendations.objects.TermRecommendations;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.utils.KruizeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

import static com.autotune.utils.KruizeConstants.JSONKeys.*;
import static com.autotune.utils.KruizeConstants.RecommendationEngineConstants.DurationBasedEngine.RecommendationDurationRanges.*;

public class Terms {
    int days;
    double threshold_in_days;
    String name;
    String performanceProfile;
    private int plots_datapoints;
    private double plots_datapoints_delta_in_days;
    private static final Logger LOGGER = LoggerFactory.getLogger(Terms.class);


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
        long thresholdInMillis = KruizeConstants.TimeConv.MEASUREMENT_DURATION_THRESHOLD_SECONDS * KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC;
        LocalDateTime monitoringStartDateTime = monitoringEndTime.toLocalDateTime().minusDays(term.days);

        double durationInSeconds = measurementDuration * KruizeConstants.TimeConv.NO_OF_SECONDS_PER_MINUTE;
            try {
                for (LocalDateTime current = monitoringEndTime.toLocalDateTime(); current.isAfter(monitoringStartDateTime); current = current.minusSeconds((long) durationInSeconds)) {
                    Timestamp currentTimestamp = Timestamp.valueOf(current);

                    // Check if the current timestamp exists in the resultsMap or within the tolerance range
                    Double diffInSec = getTimestampWithinTolerance(currentTimestamp, containerData.getResults().keySet(), thresholdInMillis);
                    if (containerData.getResults().containsKey(currentTimestamp) || diffInSec != null) {
                        // If there's a change in the timestamp within the threshold value, add the difference in the duration
                        if (diffInSec != null) {
                            durationInSeconds += diffInSec;
                        }
                        sum += measurementDuration;
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Exception occurred while checking min data : {}", e.getMessage());
                return false;
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

    private static Double getTimestampWithinTolerance(Timestamp currentTimestamp, Set<Timestamp> timestamps, long toleranceInMillis) {
        for (Timestamp timestamp : timestamps) {
            long timeDiff = currentTimestamp.getTime() - timestamp.getTime();
            if (Math.abs(timeDiff) <= toleranceInMillis) {
                return (double) ((timeDiff)/KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC);
            }
        }
        return null;
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
        // Get the maximum duration allowed for the term
        double maxDurationInHours = getMaxDuration(recommendationTerm);
        double maxDurationInMinutes = maxDurationInHours * KruizeConstants.TimeConv.NO_OF_MINUTES_PER_HOUR;
        // Set durationSummation to the maximum duration if it exceeds the maximum duration
        if (durationSummation > maxDurationInMinutes) {
            durationSummation = maxDurationInMinutes;
        }
        double durationSummationInHours = durationSummation / KruizeConstants.TimeConv.NO_OF_MINUTES_PER_HOUR;
        durationSummationInHours = Double.parseDouble(String.format("%.2f", durationSummationInHours));
        mappedRecommendationForTerm.setDurationInHrs(durationSummationInHours);
    }

    public static double getMaxDuration(String termValue) {
        return switch (termValue) {
            case SHORT_TERM -> SHORT_TERM_HOURS;
            case MEDIUM_TERM -> MEDIUM_TERM_HOURS;
            case LONG_TERM -> LONG_TERM_HOURS;
            case FIXED_TERM -> FIXED_TERM_HOURS;
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

    @Override
    public String toString() {
        return "Terms{" +
                "days=" + days +
                ", threshold_in_days=" + threshold_in_days +
                ", name='" + name + '\'' +
                '}';
    }
}
