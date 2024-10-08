
package com.autotune.analyzer.adapters;

import com.autotune.analyzer.utils.AnalyzerConstants;
import com.google.gson.*;

import java.lang.reflect.Type;

/**
 * Earlier the RecommendationItem enum has only two entries cpu and memory.
 * At the time if serialization (store in DB or return as JSON via API)
 * java has handled the toString conversion and have converted them to "cpu" and "memory" strings.
 * They are also keys in the recommendation (requests & limits)
 *
 * But in case of NVIDIA the resources have / and . in their string representation of the MIG name.
 * So we cannot add them as enums as is, So we had to create an entry which accepts a string
 * and then the toString returns the string value of it.
 *
 * At the time of deserailization the string entries are converted to enum entries and vice versa in serialization.
 * For example if the entry is NVIDIA_GPU_PARTITION_1_CORE_5GB("nvidia.com/mig-1g.5gb") then tostring of it
 * will be nvidia.com/mig-1g.5gb which will not match the enum entry NVIDIA_GPU_PARTITION_1_CORE_5GB
 *
 * Also to maintain consistency we changed the cpu to CPU so without the adapter
 * the JSON will be generated with CPU as the key.
 */
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