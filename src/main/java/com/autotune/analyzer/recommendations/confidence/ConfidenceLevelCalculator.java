package com.autotune.analyzer.recommendations.confidence;

import com.autotune.common.data.result.ContainerData;

public interface ConfidenceLevelCalculator {
    double calculateConfidenceBasedOnRecommendationPeriod(ContainerData containerData, String recommendationPeriod) throws Exception;
}

