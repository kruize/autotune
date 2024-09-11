package com.autotune.common.data.result;

public interface DeviceComponentDetector {
    public boolean isGPUDeviceDetected();
    public boolean isCPUDeviceDetected();
    public boolean isMemoryDeviceDetected();
    public boolean isNetworkDeviceDetected();
}
