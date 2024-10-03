package com.autotune.common.data.system.info.device;

import com.autotune.analyzer.utils.AnalyzerConstants;

import java.util.ArrayList;

public interface DeviceHandler {
    public void addDevice(AnalyzerConstants.DeviceType deviceType, DeviceDetails deviceInfo);
    public void removeDevice(AnalyzerConstants.DeviceType deviceType, DeviceDetails deviceInfo);
    public void updateDevice(AnalyzerConstants.DeviceType deviceType, DeviceDetails deviceInfo);
    public DeviceDetails getDeviceByParameter(AnalyzerConstants.DeviceType deviceType,
                                              String matchIdentifier,
                                              AnalyzerConstants.DeviceParameters deviceParameters);
    public ArrayList<DeviceDetails> getDevices(AnalyzerConstants.DeviceType deviceType);
}
