package com.autotune.common.data.system.info.device.accelerator;

import com.autotune.analyzer.utils.AnalyzerConstants;

public class NvidiaAcceleratorDeviceData implements AcceleratorDeviceDetails {
    private final String manufacturer;
    private final String modelName;
    private final String hostName;
    private final String UUID;
    private final String deviceName;
    private final String profile;
    private final boolean isPartitionSupported;
    private final boolean isPartition;

    public NvidiaAcceleratorDeviceData(String modelName,
                                       String hostName,
                                       String UUID,
                                       String deviceName,
                                       String profile,
                                       boolean isPartitionSupported,
                                       boolean isPartition) {
        this.profile = profile;
        this.isPartition = isPartition;
        this.manufacturer = "NVIDIA";
        this.modelName = modelName;
        this.hostName = hostName;
        this.UUID = UUID;
        this.deviceName = deviceName;
        this.isPartitionSupported = isPartitionSupported;
    }

    @Override
    public String getManufacturer() {
        return this.manufacturer;
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public String getHostName() {
        return hostName;
    }

    @Override
    public String getUUID() {
        return UUID;
    }

    @Override
    public String getDeviceName() {
        return deviceName;
    }

    public String getProfile() {
        return profile;
    }
    @Override
    public boolean isPartition() {
        return isPartition;
    }

    @Override
    public boolean isPartitionSupported() {
        return isPartitionSupported;
    }

    @Override
    public AnalyzerConstants.DeviceType getType() {
        return AnalyzerConstants.DeviceType.ACCELERATOR;
    }
}
