package com.autotune.analyzer.recommendations;

import com.autotune.analyzer.adapters.MultiResourceRecommendationAdapter;
import com.google.gson.annotations.JsonAdapter;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents multiple resource recommendations (Accelerators array)
 * Custom adapter ensures it serializes as array, not object
 */
@JsonAdapter(MultiResourceRecommendationAdapter.class)
public final class MultiResourceRecommendation implements ResourceRecommendation {
    private List<AcceleratorRecommendationItem> items;

    public MultiResourceRecommendation() {
        this.items = new ArrayList<>();
    }

    public MultiResourceRecommendation(List<AcceleratorRecommendationItem> items) {
        this.items = items;
    }

    public List<AcceleratorRecommendationItem> getItems() {
        return items;
    }

    public void setItems(List<AcceleratorRecommendationItem> items) {
        this.items = items;
    }
}
