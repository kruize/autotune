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
 * This adapter actually specifies the GSON to identify the type of implementation of MetricMetadata
 * to serialize or deserialize
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