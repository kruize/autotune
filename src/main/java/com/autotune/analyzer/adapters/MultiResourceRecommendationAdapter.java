package com.autotune.analyzer.adapters;

import com.autotune.analyzer.recommendations.AcceleratorRecommendationItem;
import com.autotune.analyzer.recommendations.MultiResourceRecommendation;
import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Serializes MultiResourceRecommendation as array (unwraps the wrapper)
 */
public class MultiResourceRecommendationAdapter
        implements JsonSerializer<MultiResourceRecommendation>, JsonDeserializer<MultiResourceRecommendation> {

    @Override
    public JsonElement serialize(MultiResourceRecommendation src, Type typeOfSrc, JsonSerializationContext context) {
        return context.serialize(src.getItems());
    }

    @Override
    public MultiResourceRecommendation deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        Type listType = new com.google.gson.reflect.TypeToken<List<AcceleratorRecommendationItem>>(){}.getType();
        List<AcceleratorRecommendationItem> items = context.deserialize(json, listType);
        return new MultiResourceRecommendation(items);
    }
}

