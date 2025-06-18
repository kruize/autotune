package com.autotune.common.data.metrics;

import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

/**
 * This class represents metadata specific to accelerator devices (e.g., Nvidia GPUs) used by the workload
 * It implements the {@link MetricMetadata} interface to provide device-specific metadata like
 * model name, profile name, and node name associated with the metric.
 */
public class AcceleratorMetricMetadata implements MetricMetadata {
    /**
     * The type of the device. Always set to "accelerator".
     */
    private final String type;

    /**
     * The model name of the accelerator (e.g., "A100", "H100").
     */
    @SerializedName(KruizeConstants.JSONKeys.ACCELERATOR_MODEL_NAME)
    private String modelName;

    /**
     * The profile name associated with the accelerator's partition profile configuration (if any like 3g.20gb).
     */
    @SerializedName(KruizeConstants.JSONKeys.ACCELERATOR_PROFILE_NAME)
    private String profileName;

    /**
     * The name of the node where this accelerator is deployed.
     */
    @SerializedName(KruizeConstants.JSONKeys.NODE)
    private String node;

    /**
     * Constructs an instance of {@link AcceleratorMetricMetadata} with the provided model name,
     * profile name, and node.
     *
     * @param modelName   The model of the accelerator.
     * @param profileName The profile name representing the partition slice.
     * @param node        The node name where this accelerator is installed [Typically the node where workload is running].
     */
    public AcceleratorMetricMetadata(String modelName, String profileName, String node) {
        this.type = AnalyzerConstants.DeviceType.ACCELERATOR.toString().toLowerCase();
        this.modelName = modelName;
        this.profileName = profileName;
        this.node = node;
    }

    /**
     * Gets the type of device.
     *
     * @return The string "accelerator".
     */
    @Override
    public String getType() {
        return type;
    }

    /**
     * Gets the model name of the accelerator.
     *
     * @return Accelerator model name.
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * Gets the profile name of the accelerator.
     *
     * @return Accelerator profile name.
     */
    public String getProfileName() {
        return profileName;
    }

    /**
     * Gets the node name where the accelerator is located.
     *
     * @return Node name.
     */
    public String getNode() {
        return node;
    }
}
