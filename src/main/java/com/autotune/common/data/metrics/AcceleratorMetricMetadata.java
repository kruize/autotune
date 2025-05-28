package com.autotune.common.data.metrics;

import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

public class AcceleratorMetricMetadata implements MetricMetadata {
    private final String type;

    @SerializedName(KruizeConstants.JSONKeys.ACCELERATOR_MODEL_NAME)
    private String modelName;
    @SerializedName(KruizeConstants.JSONKeys.ACCELERATOR_PROFILE_NAME)
    private String profileName;
    @SerializedName(KruizeConstants.JSONKeys.NODE)
    private String node;

    public AcceleratorMetricMetadata(String modelName, String profileName, String node) {
        type = AnalyzerConstants.DeviceType.ACCELERATOR.toString().toLowerCase();
        this.modelName = modelName;
        this.profileName = profileName;
        this.node = node;
    }
    @Override
    public String getType() {
        return type;
    }

    public String getModelName() {
        return modelName;
    }

    public String getProfileName() {
        return profileName;
    }

    public String getNode() {
        return node;
    }
}
