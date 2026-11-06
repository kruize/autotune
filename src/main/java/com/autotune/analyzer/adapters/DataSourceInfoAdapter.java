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
import com.autotune.utils.KruizeConstants;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Custom Gson serializer for DataSourceInfo to conditionally exclude empty clusters field.
 * Uses constants from {@link KruizeConstants} for JSON key names so that serialization
 * and deserialization (e.g., in DataSourceCollection) always use the same strings.
 */
public class DataSourceInfoAdapter implements JsonSerializer<DataSourceInfo> {

    @Override
    public JsonElement serialize(DataSourceInfo src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty(KruizeConstants.DataSourceConstants.DATASOURCE_NAME, src.getName());
        jsonObject.addProperty(KruizeConstants.DataSourceConstants.DATASOURCE_PROVIDER, src.getProvider());
        jsonObject.addProperty(KruizeConstants.DataSourceConstants.DATASOURCE_SERVICE_NAME, src.getServiceName());
        jsonObject.addProperty(KruizeConstants.DataSourceConstants.DATASOURCE_SERVICE_NAMESPACE, src.getNamespace());
        jsonObject.addProperty(KruizeConstants.DataSourceConstants.DATASOURCE_URL,
                src.getUrl() != null ? src.getUrl().toString() : null);

        // Use the same "authentication" key as AuthenticationConstants and the design contract
        if (src.getAuthenticationConfig() != null) {
            jsonObject.add(KruizeConstants.AuthenticationConstants.AUTHENTICATION,
                    context.serialize(src.getAuthenticationConfig()));
        }

        // Only add clusters field if the list is not empty
        List<String> clusters = src.getClusters();
        if (clusters != null && !clusters.isEmpty()) {
            jsonObject.add(KruizeConstants.DataSourceConstants.DataSourceMetadataInfoJSONKeys.CLUSTERS,
                    context.serialize(clusters));
        }

        return jsonObject;
    }
}
