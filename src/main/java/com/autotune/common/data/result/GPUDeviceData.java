package com.autotune.common.data.result;

import com.autotune.analyzer.utils.AnalyzerConstants;

public class GPUDeviceData implements GPUDeviceDetails{
    private final String manufacturer;
    private final String modelName;
    private final String hostName;
    private final String UUID;
    private final String deviceName;
    private boolean isMIG;

    public GPUDeviceData(String modelName, String hostName, String UUID, String deviceName, boolean isMIG) {
        this.manufacturer = "NVIDIA";
        this.modelName = modelName;
        this.hostName = hostName;
        this.UUID = UUID;
        this.deviceName = deviceName;
        this.isMIG = isMIG;
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

    public boolean isMIG() {
        return isMIG;
    }

    public void setMIG(boolean isMIG) {
        this.isMIG = isMIG;
    }

    @Override
    public AnalyzerConstants.DeviceType getType() {
        return AnalyzerConstants.DeviceType.GPU;
    }
}
