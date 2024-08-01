package com.autotune.common.data.result;

public interface GPUDeviceDetails extends DeviceDetails{
    public String getManufacturer();
    public String getModelName();
    public String getHostName();
    public String getUUID();
    public String getDeviceName();
}