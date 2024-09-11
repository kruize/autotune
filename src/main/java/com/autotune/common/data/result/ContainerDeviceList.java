package com.autotune.common.data.result;

import com.autotune.analyzer.utils.AnalyzerConstants;

import java.util.ArrayList;
import java.util.HashMap;

public class ContainerDeviceList implements DeviceHandler, DeviceComponentDetector {

    private final HashMap<AnalyzerConstants.DeviceType, ArrayList<DeviceDetails>> deviceMap;
    private boolean isGpuDeviceDetected;
    private boolean isCPUDeviceDetected;
    private boolean isMemoryDeviceDetected;
    private boolean isNetworkDeviceDetected;

    public ContainerDeviceList(){
        this.deviceMap = new HashMap<AnalyzerConstants.DeviceType, ArrayList<DeviceDetails>>();
        this.isGpuDeviceDetected = false;
        // Currently setting up CPU, Memory and Network as true by default
        this.isCPUDeviceDetected = true;
        this.isMemoryDeviceDetected = true;
        this.isNetworkDeviceDetected = true;
    }

    @Override
    public void addDevice(AnalyzerConstants.DeviceType deviceType, DeviceDetails deviceInfo) {
        if (null == deviceType || null == deviceInfo) {
            // TODO: Handle appropriate returns in future
            return;
        }

        if (deviceType == AnalyzerConstants.DeviceType.GPU)
            this.isGpuDeviceDetected = true;

        if (null == deviceMap.get(deviceType)) {
            ArrayList<DeviceDetails> deviceDetailsList = new ArrayList<DeviceDetails>();
            deviceDetailsList.add(deviceInfo);
            this.deviceMap.put(deviceType, deviceDetailsList);
        } else {
            this.deviceMap.get(deviceType).add(deviceInfo);
        }
    }

    @Override
    public void removeDevice(AnalyzerConstants.DeviceType deviceType, DeviceDetails deviceInfo) {
        if (null == deviceType || null == deviceInfo) {
            // TODO: Handle appropriate returns in future
            return;
        }
        // TODO: Need to be implemented if we need a dynamic experiment device updates
        if (deviceType == AnalyzerConstants.DeviceType.GPU) {
            if (null == deviceMap.get(deviceType) || this.deviceMap.get(deviceType).isEmpty()) {
                this.isGpuDeviceDetected = false;
            }
        }
    }

    @Override
    public void updateDevice(AnalyzerConstants.DeviceType deviceType, DeviceDetails deviceInfo) {
        // TODO: Need to be implemented if we need a dynamic experiment device updates
    }


    @Override
    public boolean isGPUDeviceDetected() {
        return this.isGpuDeviceDetected;
    }

    @Override
    public boolean isCPUDeviceDetected() {
        return this.isCPUDeviceDetected;
    }

    @Override
    public boolean isMemoryDeviceDetected() {
        return this.isMemoryDeviceDetected;
    }

    @Override
    public boolean isNetworkDeviceDetected() {
        return this.isNetworkDeviceDetected;
    }
}