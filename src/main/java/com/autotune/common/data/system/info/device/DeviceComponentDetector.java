package com.autotune.common.data.system.info.device;

public interface DeviceComponentDetector {
    public boolean isAcceleratorDeviceDetected();
    public boolean isCPUDeviceDetected();
    public boolean isMemoryDeviceDetected();
    public boolean isNetworkDeviceDetected();
}
