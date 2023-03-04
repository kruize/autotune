package com.autotune.analyzer.data;

/**
 * Interface to add the recommendation sub-category
 *
 * Example:
 * Duration Based recommendations has sub categories like `short term`, `medium term`, `long term`
 */
public interface RecommendationSubCategory {
    public String getSubCategory();
}
