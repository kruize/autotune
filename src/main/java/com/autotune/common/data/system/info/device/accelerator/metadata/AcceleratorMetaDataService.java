package com.autotune.common.data.system.info.device.accelerator.metadata;



import com.autotune.analyzer.utils.AnalyzerConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A service which is created to provide the respective Accelerator Profile
 * based on SM and Memory requirements
 *
 * This service initially loads the profiles of supported Accelerators
 * Currently it supports:
 *          NVIDIA A100 40GB
 *          NVIDIA A100 80GB
 *          NVIDIA H100 80GB
 */
public class AcceleratorMetaDataService {
    private static Map<String, List<AcceleratorProfile>> acceleratorProfilesMap;
    private static AcceleratorMetaDataService acceleratorMetaDataService = null;

    /**
     *
     */
    private AcceleratorMetaDataService(){
        acceleratorProfilesMap = new HashMap<>();
        initializeAcceleratorProfiles();
    }

    private static void initializeAcceleratorProfiles() {
        List<AcceleratorProfile> commonProfiles = new ArrayList<>();
        // IMPORTANT: Add it in the ascending order according to GPU Core and Memory Units as we will break the loop upon getting the right one
        commonProfiles.add(new AcceleratorProfile(AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_1G_10GB,
                1.0 / 8, 1.0 / 7, 7));
        commonProfiles.add(new AcceleratorProfile(AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_1G_20GB,
                1.0 / 4, 1.0 / 7, 4));
        commonProfiles.add(new AcceleratorProfile(AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_2G_20GB,
                2.0 / 8, 2.0 / 7, 3));
        commonProfiles.add(new AcceleratorProfile(AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_3G_40GB,
                4.0 / 8, 3.0 / 7, 2));
        commonProfiles.add(new AcceleratorProfile(AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_4G_40GB,
                4.0 / 8, 4.0 / 7, 1));
        commonProfiles.add(new AcceleratorProfile(AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_7G_80GB,
                1.0, 1.0, 1));

        List<AcceleratorProfile> a100_40_gb_profiles = new ArrayList<>();
        // IMPORTANT: Add it in the ascending order according to GPU Core and Memory Units as we will break the loop upon getting the right one
        a100_40_gb_profiles.add(new AcceleratorProfile(AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_1G_5GB,
                1.0 / 8, 1.0 / 7, 7));
        a100_40_gb_profiles.add(new AcceleratorProfile(AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_1G_10GB,
                1.0 / 4, 1.0 / 7, 4));
        a100_40_gb_profiles.add(new AcceleratorProfile(AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_2G_10GB,
                2.0 / 8, 2.0 / 7, 3));
        a100_40_gb_profiles.add(new AcceleratorProfile(AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_3G_20GB,
                4.0 / 8, 3.0 / 7, 2));
        a100_40_gb_profiles.add(new AcceleratorProfile(AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_4G_20GB,
                4.0 / 8, 4.0 / 7, 1));
        a100_40_gb_profiles.add(new AcceleratorProfile(AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.PROFILE_7G_40GB,
                1.0, 1.0, 1));

        acceleratorProfilesMap.put(AnalyzerConstants.AcceleratorConstants.SupportedAccelerators.A100_80_GB, new ArrayList<>(commonProfiles));
        acceleratorProfilesMap.put(AnalyzerConstants.AcceleratorConstants.SupportedAccelerators.H100_80_GB, new ArrayList<>(commonProfiles));
        acceleratorProfilesMap.put(AnalyzerConstants.AcceleratorConstants.SupportedAccelerators.A100_40_GB, new ArrayList<>(a100_40_gb_profiles));
    }

    public static AcceleratorMetaDataService getInstance() {
        if(null == acceleratorMetaDataService) {
            synchronized (AcceleratorMetaDataService.class) {
                if (null == acceleratorMetaDataService) {
                    acceleratorMetaDataService = new AcceleratorMetaDataService();
                }
            }
        }
        return acceleratorMetaDataService;
    }

    public AcceleratorProfile getAcceleratorProfile(String modelName, Double requiredSmFraction, Double requiredMemoryFraction) {
        if (null == modelName || null == requiredSmFraction || null == requiredMemoryFraction) {
            return null;
        }
        modelName = modelName.strip();
        if (!modelName.equalsIgnoreCase(AnalyzerConstants.AcceleratorConstants.SupportedAccelerators.A100_80_GB)
                && !modelName.equalsIgnoreCase(AnalyzerConstants.AcceleratorConstants.SupportedAccelerators.H100_80_GB)
                && !modelName.equalsIgnoreCase(AnalyzerConstants.AcceleratorConstants.SupportedAccelerators.A100_40_GB)) {
            return null;
        }
        if (requiredMemoryFraction < 0.0 || requiredSmFraction < 0.0) {
            return null;
        }
        List<AcceleratorProfile> gpuProfiles = acceleratorProfilesMap.get(modelName);
        for (AcceleratorProfile profile : gpuProfiles) {
            if (profile.getMemoryFraction() >= requiredMemoryFraction && profile.getSmFraction() >= requiredSmFraction) {
                // Returning the profile as the list is in ascending order
                return profile;
            }
        }
        return null;
    }
}
