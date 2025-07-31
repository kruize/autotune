package com.autotune.analyzer.adapters;

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class KruizeObjectAdapter implements JsonSerializer<KruizeObject> {
    @Override
    public JsonElement serialize(KruizeObject obj, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = context.serialize(obj).getAsJsonObject();

        // Replace the experiment_type field
        String typeStr = obj.getExperimentTypeString();
        jsonObject.remove("experiment_type");
        jsonObject.addProperty("experiment_type", typeStr);

        return jsonObject;
    }
}
