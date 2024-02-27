package com.autotune.analyzer.recommendations.term;

import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.recommendations.objects.TermRecommendations;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.IntervalResults;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

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

    public static boolean checkIfMinDataAvailableForTerm(ContainerData containerData, RecommendationConstants.RecommendationTerms recommendationTerms) {
        // Check if data available
        if (null == containerData || null == containerData.getResults() || containerData.getResults().isEmpty()) {
            return false;
        }

        // Set bounds to check if we get minimum requirement satisfied
        double lowerBound = recommendationTerms.getLowerBound();
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
            mappedRecommendationForTerm, RecommendationConstants.RecommendationTerms recommendationTerm) {

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

    public static double getMaxDuration(RecommendationConstants.RecommendationTerms termValue) {
        return switch (termValue) {
            case SHORT_TERM -> SHORT_TERM_HOURS;
            case MEDIUM_TERM -> MEDIUM_TERM_HOURS;
            case LONG_TERM -> LONG_TERM_HOURS;
        };
    }
}
