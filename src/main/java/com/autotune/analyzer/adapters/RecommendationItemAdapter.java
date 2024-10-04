
package com.autotune.analyzer.adapters;

import com.autotune.analyzer.utils.AnalyzerConstants;
import com.google.gson.*;

import java.lang.reflect.Type;

public class RecommendationItemAdapter implements JsonSerializer<AnalyzerConstants.RecommendationItem>, JsonDeserializer<AnalyzerConstants.RecommendationItem> {
    @Override
    public JsonElement serialize(AnalyzerConstants.RecommendationItem recommendationItem, Type type, JsonSerializationContext jsonSerializationContext) {
        return jsonSerializationContext.serialize(recommendationItem.toString());
    }


    @Override
    public AnalyzerConstants.RecommendationItem deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        String value = jsonElement.getAsString();
        for (AnalyzerConstants.RecommendationItem item : AnalyzerConstants.RecommendationItem.values()) {
            if (item.toString().equals(value)) {
                return item;
            }
        }
        throw new JsonParseException("Unknown element " + value);
    }
}