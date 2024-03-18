package com.autotune.analyzer.recommendations.term;

import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.recommendations.objects.TermRecommendations;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.utils.KruizeConstants;

import java.sql.Timestamp;
import java.util.*;

import static com.autotune.utils.KruizeConstants.JSONKeys.*;
import static com.autotune.utils.KruizeConstants.RecommendationEngineConstants.DurationBasedEngine.RecommendationDurationRanges.*;

public class Terms {
    int days;
    double threshold_in_days;

    String performanceProfile;

    public Terms(int days, double threshold_in_days) {
        this.days = days;
        this.threshold_in_days = threshold_in_days;
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

    public static int getMaxDays(Map<String, Terms> terms) {
        Optional<Terms> maxTerms = terms.values().stream()
                .max(Comparator.comparingInt(term -> term.days));
        return maxTerms.map(term -> term.days).orElse(0); // Return the max days or 0 if terms is empty
    }

    public static boolean checkIfMinDataAvailableForTerm(ContainerData containerData, double duration) {
        // Check if data available
        if (null == containerData || null == containerData.getResults() || containerData.getResults().isEmpty()) {
            return false;
        }

        // Set bounds to check if we get minimum requirement satisfied
        double lowerBound = duration - BUFFER_VALUE_IN_MINS;
        double sum = getDurationSummation(containerData);
        // We don't consider upper bound to check if sum is in-between as we may over shoot and end-up resulting false
        if (sum >= lowerBound)
            return true;

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
        double maxDuration = getMaxDuration(recommendationTerm);
        // Set durationSummation to the maximum duration if it exceeds the maximum duration
        if (durationSummation > maxDuration) {
            durationSummation = maxDuration;
        }
        mappedRecommendationForTerm.setDurationInHrs(durationSummation);
    }

    public static double getMaxDuration(String termValue) {
        return switch (termValue) {
            case SHORT_TERM -> SHORT_TERM_HOURS;
            case MEDIUM_TERM -> MEDIUM_TERM_HOURS;
            case LONG_TERM -> LONG_TERM_HOURS;
            default -> throw new IllegalStateException("Unexpected value: " + termValue);
        };
    }

    public static Timestamp getMonitoringStartTime(HashMap<Timestamp, IntervalResults> resultsHashMap,
                                                   Timestamp endTime,
                                                   int durationInDays) {

        // Convert the HashMap to a TreeMap to maintain sorted order based on IntervalEndTime
        TreeMap<Timestamp, IntervalResults> sortedResultsHashMap = new TreeMap<>(Collections.reverseOrder());
        sortedResultsHashMap.putAll(resultsHashMap);

        double sum = 0.0;
        double durationInHrs = KruizeConstants.TimeConv.NO_OF_HOURS_PER_DAY * durationInDays;
        Timestamp intervalEndTime = null;
        for (Timestamp timestamp : sortedResultsHashMap.keySet()) {
            if (!timestamp.after(endTime)) {
                if (sortedResultsHashMap.containsKey(timestamp)) {
                    sum = sum + sortedResultsHashMap.get(timestamp).getDurationInMinutes();
                    if (sum >= ((durationInHrs * KruizeConstants.TimeConv.NO_OF_MINUTES_PER_HOUR)
                            - (KruizeConstants.TimeConv.MEASUREMENT_DURATION_THRESHOLD_SECONDS / KruizeConstants.TimeConv.NO_OF_SECONDS_PER_MINUTE))) {
                        // Storing the timestamp value in startTimestamp variable to return
                        intervalEndTime = timestamp;
                        break;
                    }
                }
            }
        }
        try {
            return sortedResultsHashMap.get(intervalEndTime).getIntervalStartTime();
        } catch (NullPointerException npe) {
            return null;
        }
    }
}
