/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
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
        if (src == null
                || src.getAcceleratorRecommendationItems() == null
                || src.getAcceleratorRecommendationItems().isEmpty()) {
            return JsonNull.INSTANCE;
        }
        return context.serialize(src.getAcceleratorRecommendationItems());
    }

    @Override
    public MultiResourceRecommendation deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

        // if accelerators or any multi object array field missing or null
        if (json == null || json.isJsonNull()) {
            return null;
        }

        if (!json.isJsonArray()) {
            throw new JsonParseException(
                    "Expected a JSON array for MultiResourceRecommendation but found: "
                            + json);
        }

        Type listType = new com.google.gson.reflect.TypeToken<List<AcceleratorRecommendationItem>>(){}.getType();
        List<AcceleratorRecommendationItem> items = context.deserialize(json, listType);

        // Returning null instead of an empty list to avoid emitting an empty array in the JSON during serialization
        if (items == null || items.isEmpty()) {
            return null;
        }

        return new MultiResourceRecommendation(items);
    }
}

