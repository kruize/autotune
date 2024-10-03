package com.autotune.common.data.system.info.device.accelerator.metadata;



import com.autotune.analyzer.utils.AnalyzerConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AcceleratorMetaDataService {
    private static Map<String, List<AcceleratorProfile>> acceleratorProfilesMap;
    private static AcceleratorMetaDataService acceleratorMetaDataService = null;

    private AcceleratorMetaDataService(){
        acceleratorProfilesMap = new HashMap<>();
        initializeAcceleratorProfiles();
    }

    private static void initializeAcceleratorProfiles() {
        List<AcceleratorProfile> commonProfiles = new ArrayList<>();
        // IMPORTANT: Add it in the ascending order according to GPU Core and Memory Units as we will break the loop upon getting the right one
        commonProfiles.add(new AcceleratorProfile("1g.10gb", 1.0 / 8, 1.0 / 7, 7));
        commonProfiles.add(new AcceleratorProfile("1g.20gb", 1.0 / 4, 1.0 / 7, 4));
        commonProfiles.add(new AcceleratorProfile("2g.20gb", 2.0 / 8, 2.0 / 7, 3));
        commonProfiles.add(new AcceleratorProfile("3g.40gb", 4.0 / 8, 3.0 / 7, 2));
        commonProfiles.add(new AcceleratorProfile("4g.40gb", 4.0 / 8, 4.0 / 7, 1));
        commonProfiles.add(new AcceleratorProfile("7g.80gb", 1.0, 1.0, 1));

        List<AcceleratorProfile> a100_40_gb_profiles = new ArrayList<>();
        // IMPORTANT: Add it in the ascending order according to GPU Core and Memory Units as we will break the loop upon getting the right one
        a100_40_gb_profiles.add(new AcceleratorProfile("1g.5gb", 1.0 / 8, 1.0 / 7, 7));
        a100_40_gb_profiles.add(new AcceleratorProfile("1g.10gb", 1.0 / 4, 1.0 / 7, 4));
        a100_40_gb_profiles.add(new AcceleratorProfile("2g.10gb", 2.0 / 8, 2.0 / 7, 3));
        a100_40_gb_profiles.add(new AcceleratorProfile("3g.20gb", 4.0 / 8, 3.0 / 7, 2));
        a100_40_gb_profiles.add(new AcceleratorProfile("4g.20gb", 4.0 / 8, 4.0 / 7, 1));
        a100_40_gb_profiles.add(new AcceleratorProfile("7g.40gb", 1.0, 1.0, 1));

        acceleratorProfilesMap.put(AnalyzerConstants.SupportedGPUs.A100_80_GB, new ArrayList<>(commonProfiles));
        acceleratorProfilesMap.put(AnalyzerConstants.SupportedGPUs.H100, new ArrayList<>(commonProfiles));
        acceleratorProfilesMap.put(AnalyzerConstants.SupportedGPUs.A100_40_GB, new ArrayList<>(a100_40_gb_profiles));
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
        if (!modelName.equalsIgnoreCase(AnalyzerConstants.SupportedGPUs.A100_80_GB)
                && !modelName.equalsIgnoreCase(AnalyzerConstants.SupportedGPUs.H100)
                && !modelName.equalsIgnoreCase(AnalyzerConstants.SupportedGPUs.A100_40_GB)) {
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
