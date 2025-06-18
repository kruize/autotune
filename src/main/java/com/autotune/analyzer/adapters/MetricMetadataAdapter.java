/*******************************************************************************
 * Copyright (c) 2025 Red Hat, IBM Corporation and others.
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

import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.metrics.AcceleratorMetricMetadata;
import com.autotune.common.data.metrics.MetricMetadata;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.autotune.utils.KruizeConstants;


import java.io.IOException;

/**
 * A custom Gson {@link TypeAdapter} implementation for serializing and deserializing
 * {@link MetricMetadata} objects. This adapter currently supports handling metadata
 * for accelerator devices (e.g., GPUs) via the {@link AcceleratorMetricMetadata} class.
 *
 * <p><b>Current Implementation:</b></p>
 * <ul>
 *     <li>During serialization:
 *         <ul>
 *             <li>Writes the `type` field to identify the metadata type.</li>
 *             <li>For {@link AcceleratorMetricMetadata}, writes fields such as model name,
 *                 profile name (MIG profile), and node name.</li>
 *         </ul>
 *     </li>
 *     <li>During deserialization:
 *         <ul>
 *             <li>Reads the `type` field and maps it to the appropriate implementation.</li>
 *             <li>If the `type` is <code>null</code> or equals "accelerator" (case-insensitive),
 *                 an {@link AcceleratorMetricMetadata} instance is created.</li>
 *             <li>Unknown fields are safely skipped.</li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * <p><b>Future Considerations:</b></p>
 * <ul>
 *     <li>Currently, the adapter assumes only accelerator metadata exists.</li>
 *     <li>In the future, support for additional metric metadata types should enforce a non-null `type`
 *         field for proper deserialization.</li>
 *     <li>This class should be extended to handle multiple types of {@link MetricMetadata}
 *         by mapping the `type` field to the appropriate class.</li>
 * </ul>
 *
 */

public class MetricMetadataAdapter extends TypeAdapter<MetricMetadata> {

    @Override
    public void write(JsonWriter out, MetricMetadata value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        out.beginObject();
        if (null != value.getType())
            out.name(KruizeConstants.JSONKeys.TYPE).value(value.getType());

        if (value instanceof AcceleratorMetricMetadata accel) {
            if (null != accel.getModelName())
                out.name(KruizeConstants.JSONKeys.ACCELERATOR_MODEL_NAME).value(accel.getModelName());
            if (null != accel.getProfileName())
                out.name(KruizeConstants.JSONKeys.ACCELERATOR_PROFILE_NAME).value(accel.getProfileName());
            if (null != accel.getNode())
                out.name(KruizeConstants.JSONKeys.NODE).value(accel.getNode());
        }
        out.endObject();
    }

    @Override
    public MetricMetadata read(JsonReader in) throws IOException {
        String type = null;
        String modelName = null;
        String profileName = null;
        String node = null;

        in.beginObject();
        while (in.hasNext()) {
            String name = in.nextName();
            switch (name) {
                case KruizeConstants.JSONKeys.TYPE:
                    type = in.nextString();
                    break;
                case KruizeConstants.JSONKeys.ACCELERATOR_MODEL_NAME:
                    modelName = in.nextString();
                    break;
                case KruizeConstants.JSONKeys.ACCELERATOR_PROFILE_NAME:
                    profileName = in.nextString();
                    break;
                case KruizeConstants.JSONKeys.NODE:
                    node = in.nextString();
                    break;
                default:
                    in.skipValue();
            }
        }
        in.endObject();

        // For now, we assume that no type field present it's accelerator metadata
        // In future if other metrics send different metadata then type cannot be null and type check should be mandatory
        if (type == null || type.equalsIgnoreCase(AnalyzerConstants.DeviceType.ACCELERATOR.toString())) {
            return new AcceleratorMetricMetadata(modelName, profileName, node);
        }
        return null;
    }
}
