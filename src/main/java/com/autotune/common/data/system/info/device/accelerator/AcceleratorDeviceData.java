package com.autotune.common.data.system.info.device.accelerator;

import com.autotune.analyzer.utils.AnalyzerConstants;

public class AcceleratorDeviceData implements AcceleratorDeviceDetails {
    private final String manufacturer;
    private final String modelName;
    private final String hostName;
    private final String UUID;
    private final String deviceName;
    private final String profile;
    private boolean isMIGSupported;
    private boolean isMIGPartition;

    public AcceleratorDeviceData (String modelName,
                                  String hostName,
                                  String UUID,
                                  String deviceName,
                                  String profile,
                                  boolean isMIGSupported,
                                  boolean isMIGPartition) {
        this.profile = profile;
        this.isMIGPartition = isMIGPartition;
        this.manufacturer = "NVIDIA";
        this.modelName = modelName;
        this.hostName = hostName;
        this.UUID = UUID;
        this.deviceName = deviceName;
        this.isMIGSupported = isMIGSupported;
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

    public boolean isMIGPartition() {
        return isMIGPartition;
    }

    public boolean isMIGSupported() {
        return isMIGSupported;
    }

    @Override
    public AnalyzerConstants.DeviceType getType() {
        return AnalyzerConstants.DeviceType.ACCELERATOR;
    }
}
