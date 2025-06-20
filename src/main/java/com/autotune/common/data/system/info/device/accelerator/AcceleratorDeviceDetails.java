package com.autotune.common.data.system.info.device.accelerator;

import com.autotune.common.data.system.info.device.DeviceDetails;

public interface AcceleratorDeviceDetails extends DeviceDetails {
    public String getManufacturer();
    public String getModelName();
    public String getHostName();
    public String getUUID();
    public String getDeviceName();
    public boolean isPartitionSupported();
    public boolean isPartition();
}
