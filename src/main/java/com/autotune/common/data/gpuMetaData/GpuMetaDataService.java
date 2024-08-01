package com.autotune.common.data.gpuMetaData;

import com.autotune.analyzer.utils.AnalyzerConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GpuMetaDataService {
    private static Map<String, List<GpuProfile>> gpuProfilesMap;
    private static GpuMetaDataService gpuMetaDataService = null;

    private GpuMetaDataService(){
        gpuProfilesMap = new HashMap<>();
        initializeGpuProfiles();
    }

    private static void initializeGpuProfiles() {
        List<GpuProfile> commonProfiles = new ArrayList<>();
        // IMPORTANT: Add it in the ascending order according to GPU Core and Memory Units as we will break the loop upon getting the right one
        commonProfiles.add(new GpuProfile("1g.10gb", 1.0 / 8, 1.0 / 7, 7));
        commonProfiles.add(new GpuProfile("1g.20gb", 1.0 / 4, 1.0 / 7, 4));
        commonProfiles.add(new GpuProfile("2g.20gb", 2.0 / 8, 2.0 / 7, 3));
        commonProfiles.add(new GpuProfile("3g.40gb", 4.0 / 8, 3.0 / 7, 2));
        commonProfiles.add(new GpuProfile("4g.40gb", 4.0 / 8, 4.0 / 7, 1));
        commonProfiles.add(new GpuProfile("7g.80gb", 1.0, 1.0, 1));

        List<GpuProfile> a100_40_gb_profiles = new ArrayList<>();
        // IMPORTANT: Add it in the ascending order according to GPU Core and Memory Units as we will break the loop upon getting the right one
        a100_40_gb_profiles.add(new GpuProfile("1g.5gb", 1.0 / 8, 1.0 / 7, 7));
        a100_40_gb_profiles.add(new GpuProfile("1g.10gb", 1.0 / 4, 1.0 / 7, 4));
        a100_40_gb_profiles.add(new GpuProfile("2g.10gb", 2.0 / 8, 2.0 / 7, 3));
        a100_40_gb_profiles.add(new GpuProfile("3g.20gb", 4.0 / 8, 3.0 / 7, 2));
        a100_40_gb_profiles.add(new GpuProfile("4g.20gb", 4.0 / 8, 4.0 / 7, 1));
        a100_40_gb_profiles.add(new GpuProfile("7g.40gb", 1.0, 1.0, 1));

        gpuProfilesMap.put(AnalyzerConstants.SupportedGPUs.A100_80_GB, new ArrayList<>(commonProfiles));
        gpuProfilesMap.put(AnalyzerConstants.SupportedGPUs.H100, new ArrayList<>(commonProfiles));
        gpuProfilesMap.put(AnalyzerConstants.SupportedGPUs.A100_40_GB, new ArrayList<>(a100_40_gb_profiles));
    }

    public static GpuMetaDataService getInstance() {
        if(null == gpuMetaDataService) {
            synchronized (GpuMetaDataService.class) {
                if (null == gpuMetaDataService) {
                    gpuMetaDataService = new GpuMetaDataService();
                }
            }
        }
        return gpuMetaDataService;
    }

    public GpuProfile getGpuProfile(String modelName, Double requiredSmFraction, Double requiredMemoryFraction) {
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
        List<GpuProfile> gpuProfiles = gpuProfilesMap.get(modelName);
        for (GpuProfile profile : gpuProfiles) {
            if (profile.getMemoryFraction() >= requiredMemoryFraction && profile.getSmFraction() >= requiredSmFraction) {
                // Returning the profile as the list is in ascending order
                return profile;
            }
        }
        return null;
    }
}