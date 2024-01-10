package com.autotune.analyzer.recommendations.confidence;

import com.autotune.analyzer.recommendations.utils.RecommendationUtils;
import com.autotune.common.data.result.ContainerData;
import com.autotune.utils.KruizeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultConfidenceCalculator implements ConfidenceLevelCalculator {

    @Override
    public double calculateConfidenceBasedOnRecommendationPeriod(ContainerData containerData, String recPeriod) throws Exception {
        double term_max_data_mins;
        if (recPeriod.equalsIgnoreCase(KruizeConstants.JSONKeys.SHORT_TERM)) {
            term_max_data_mins = KruizeConstants.RecommendationEngineConstants.DurationBasedEngine.RecommendationDurationRanges.SHORT_TERM_MAX_DATA_MINS;
        } else if (recPeriod.equalsIgnoreCase(KruizeConstants.JSONKeys.MEDIUM_TERM)) {
            term_max_data_mins = KruizeConstants.RecommendationEngineConstants.DurationBasedEngine.RecommendationDurationRanges.MEDIUM_TERM_MAX_DATA_MINS;
        } else if (recPeriod.equalsIgnoreCase(KruizeConstants.JSONKeys.LONG_TERM)) {
            term_max_data_mins = KruizeConstants.RecommendationEngineConstants.DurationBasedEngine.RecommendationDurationRanges.LONG_TERM_MAX_DATA_MINS;
        } else {
            throw new Exception("Invalid Recommendation Term");
        }
        // set the confidenceLevel to 1.0 if it exceeds 1 since the confidence can only vary between 0 and 1
        double availableData = RecommendationUtils.getDurationSummation(containerData);
        double confidenceLevel = (availableData / term_max_data_mins);
        confidenceLevel = Math.min(Math.max(confidenceLevel, 0.1), 1.0);
        return confidenceLevel;
    }
}
