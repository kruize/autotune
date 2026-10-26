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

import com.autotune.analyzer.recommendations.MultiResourceRecommendation;
import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.ResourceRecommendation;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * Requests and limits store both CPU/memory objects and accelerator arrays behind
 * {@link ResourceRecommendation}. Gson cannot construct that interface on its own
 * when a recommendation is cloned, so pick the concrete type from the JSON shape.
 */
public class ResourceRecommendationAdapter implements JsonDeserializer<ResourceRecommendation> {

    @Override
    public ResourceRecommendation deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        if (json == null || json.isJsonNull()) {
            return null;
        }
        if (json.isJsonArray()) {
            return context.deserialize(json, MultiResourceRecommendation.class);
        }
        if (json.isJsonObject()) {
            return context.deserialize(json, RecommendationConfigItem.class);
        }
        throw new JsonParseException("Expected an object or array for ResourceRecommendation but found: " + json);
    }
}
