package com.autotune.common.data.system.info.device;

import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.system.info.device.accelerator.AcceleratorDeviceData;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class stores the device entries linked to the container
 */
public class ContainerDeviceList implements DeviceHandler, DeviceComponentDetector {
    private final HashMap<AnalyzerConstants.DeviceType, ArrayList<DeviceDetails>> deviceMap;
    private boolean isAcceleratorDeviceDetected;
    private boolean isAcceleratorPartitionDetected;
    private boolean isCPUDeviceDetected;
    private boolean isMemoryDeviceDetected;
    private boolean isNetworkDeviceDetected;

    public ContainerDeviceList(){
        this.deviceMap = new HashMap<AnalyzerConstants.DeviceType, ArrayList<DeviceDetails>>();
        this.isAcceleratorDeviceDetected = false;
        this.isAcceleratorPartitionDetected = false;
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

        if (deviceType == AnalyzerConstants.DeviceType.ACCELERATOR)
            this.isAcceleratorDeviceDetected = true;

        if (deviceType == AnalyzerConstants.DeviceType.ACCELERATOR_PARTITION) {
            this.isAcceleratorPartitionDetected = true;
            this.isAcceleratorDeviceDetected = true;
        }

        // TODO: Handle multiple same entries
        // Currently only first MIG is getting added so no check for existing duplicates is done
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
        if (deviceType == AnalyzerConstants.DeviceType.ACCELERATOR) {
            if (null == deviceMap.get(deviceType) || this.deviceMap.get(deviceType).isEmpty()) {
                this.isAcceleratorDeviceDetected = false;
            }
        }
    }

    @Override
    public void updateDevice(AnalyzerConstants.DeviceType deviceType, DeviceDetails deviceInfo) {
        // TODO: Need to be implemented if we need a dynamic experiment device updates
    }

    /**
     * Returns the Device which matches the identifier based on the device parameter passed
     * @param deviceType                    - Type of the device Eg: CPU, Memory, Network or Accelerator
     * @param matchIdentifier               - String which needs to the matched
     * @param deviceParameters              - Parameter to search in device details list
     * @return the appropriate DeviceDetails object
     *
     * USE CASE: To search the device based on a particular parameter, Let's say you have multiple accelerators
     * to the container, you can pass the Model name as parameter and name of model to get the particular
     * DeviceDetail object.
     */
    @Override
    public DeviceDetails getDeviceByParameter(AnalyzerConstants.DeviceType deviceType, String matchIdentifier, AnalyzerConstants.DeviceParameters deviceParameters) {
        if (null == deviceType)
            return null;
        if (null == matchIdentifier)
            return null;
        if (null == deviceParameters)
            return null;
        if (matchIdentifier.isEmpty())
            return null;
        if (!deviceMap.containsKey(deviceType))
            return null;
        if (null == deviceMap.get(deviceType))
            return null;
        if (deviceMap.get(deviceType).isEmpty())
            return null;

        // Todo: Need to add extractors for each device type currently implementing for GPU
        if (deviceType == AnalyzerConstants.DeviceType.ACCELERATOR) {
            for (DeviceDetails deviceDetails: deviceMap.get(deviceType)) {
                AcceleratorDeviceData deviceData = (AcceleratorDeviceData) deviceDetails;
                if (deviceParameters == AnalyzerConstants.DeviceParameters.MODEL_NAME) {
                    if (deviceData.getModelName().equalsIgnoreCase(matchIdentifier)) {
                        return deviceData;
                    }
                }
            }
        }

        return null;
    }

    @Override
    public ArrayList<DeviceDetails> getDevices(AnalyzerConstants.DeviceType deviceType) {
        if (null == deviceType)
            return null;
        if (!deviceMap.containsKey(deviceType))
            return null;
        if (null == deviceMap.get(deviceType))
            return null;
        if (deviceMap.get(deviceType).isEmpty())
            return null;

        return deviceMap.get(deviceType);
    }

    @Override
    public boolean isAcceleratorDeviceDetected() {
        return this.isAcceleratorDeviceDetected;
    }

    @Override
    public boolean isAcceleratorPartitionDetected() {
        return this.isAcceleratorPartitionDetected;
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
