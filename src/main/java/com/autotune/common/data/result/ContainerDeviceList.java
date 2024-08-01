package com.autotune.common.data.result;

import com.autotune.analyzer.utils.AnalyzerConstants;

import java.util.ArrayList;
import java.util.HashMap;

public class ContainerDeviceList {
    private HashMap<AnalyzerConstants.DeviceType, ArrayList<DeviceDetails>> deviceMap;
    public void addDevice(AnalyzerConstants.DeviceType deviceType, DeviceDetails deviceInfo) {
        // TODO: Need to be implemented while adding dynamic GPU detection
    }
}