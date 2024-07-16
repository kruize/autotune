package com.autotune.analyzer.adapters;

import com.autotune.analyzer.utils.AnalyzerConstants;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class RecommendationItemAdapter implements JsonSerializer<AnalyzerConstants.RecommendationItem> {
    @Override
    public JsonElement serialize(AnalyzerConstants.RecommendationItem recommendationItem, Type type, JsonSerializationContext jsonSerializationContext) {
        return jsonSerializationContext.serialize(recommendationItem.toString());
    }
}
