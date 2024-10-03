package com.autotune.common.data.system.info.device.accelerator.metadata;

public class AcceleratorProfile {
    private final String profileName;
    private final double memoryFraction;
    private final double smFraction;
    private final int instancesAvailable;

    public AcceleratorProfile(String profileName, double memoryFraction, double smFraction, int instancesAvailable) {
        this.profileName = profileName;
        this.memoryFraction = memoryFraction;
        this.smFraction = smFraction;
        this.instancesAvailable = instancesAvailable;
    }

    public String getProfileName() {
        return profileName;
    }

    public double getMemoryFraction() {
        return memoryFraction;
    }

    public double getSmFraction() {
        return smFraction;
    }

    public int getInstancesAvailable() {
        return instancesAvailable;
    }

    @Override
    public String toString() {
        return "AcceleratorProfile{" +
                "profileName='" + profileName + '\'' +
                ", memoryFraction=" + memoryFraction +
                ", smFraction=" + smFraction +
                ", instancesAvailable=" + instancesAvailable +
                '}';
    }
}
