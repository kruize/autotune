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

import com.autotune.common.datasource.DataSourceInfo;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Custom Gson serializer for DataSourceInfo to conditionally exclude empty clusters field
 */
public class DataSourceInfoAdapter implements JsonSerializer<DataSourceInfo> {

    @Override
    public JsonElement serialize(DataSourceInfo src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        
        jsonObject.addProperty("name", src.getName());
        jsonObject.addProperty("provider", src.getProvider());
        jsonObject.addProperty("serviceName", src.getServiceName());
        jsonObject.addProperty("namespace", src.getNamespace());
        jsonObject.addProperty("url", src.getUrl() != null ? src.getUrl().toString() : null);
        
        // Serialize authenticationConfig
        if (src.getAuthenticationConfig() != null) {
            jsonObject.add("authenticationConfig", context.serialize(src.getAuthenticationConfig()));
        }
        
        // Only add clusters field if the list is not empty
        List<String> clusters = src.getClusters();
        if (clusters != null && !clusters.isEmpty()) {
            jsonObject.add("clusters", context.serialize(clusters));
        }
        
        return jsonObject;
    }
}
