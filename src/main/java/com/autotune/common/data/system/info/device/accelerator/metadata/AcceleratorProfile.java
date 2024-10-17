package com.autotune.common.data.system.info.device.accelerator.metadata;

/**
 * Class which is used to store the details of an accelerator profile
 */
public class AcceleratorProfile {
    private final String profileName;
    private final double memoryFraction;
    private final double smFraction;
    private final int instancesAvailable;

    /**
     * Constructor to create the Accelerator Profile
     * @param profileName                   - Name of the profile
     * @param memoryFraction                - Fraction of memory out of the whole accelerator memory
     * @param smFraction                    - Fraction of Cores or Streaming Processors out if the whole accelerator cores
     * @param instancesAvailable            - Number of instances of a profile available on an Accelerator
     */
    public AcceleratorProfile(String profileName, double memoryFraction, double smFraction, int instancesAvailable) {
        this.profileName = profileName;
        this.memoryFraction = memoryFraction;
        this.smFraction = smFraction;
        this.instancesAvailable = instancesAvailable;
    }

    public String getProfileName() {
        return this.profileName;
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
