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

        List<AcceleratorProfile> h200_141_gb_profiles = new ArrayList<>();
        // IMPORTANT: Add it in the ascending order according to GPU Core and Memory Units as we will break the loop upon getting the right one
        h200_141_gb_profiles.add(new AcceleratorProfile(
                AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.H200_PROFILE_1G_18GB,
                1.0 / 8,
                1.0 / 7,
                7
        ));
        h200_141_gb_profiles.add(new AcceleratorProfile(
                AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.H200_PROFILE_1G_35GB,
                1.0 / 4,
                1.0 / 7,
                4
        ));
        h200_141_gb_profiles.add(new AcceleratorProfile(
                AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.H200_PROFILE_2G_35GB,
                2.0 / 8,
                2.0 / 7,
                3
        ));
        h200_141_gb_profiles.add(new AcceleratorProfile(
                AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.H200_PROFILE_3G_71GB,
                4.0 / 8,
                3.0 / 7,
                2
        ));
        h200_141_gb_profiles.add(new AcceleratorProfile(
                AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.H200_PROFILE_4G_71GB,
                4.0 / 8,
                4.0 / 7,
                1
        ));
        h200_141_gb_profiles.add(new AcceleratorProfile(
                AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.H200_PROFILE_7G_141GB,
                1,
                1,
                1
        ));

        List<AcceleratorProfile> b200_180_gb_profiles = new ArrayList<>();
        b200_180_gb_profiles.add(new AcceleratorProfile(
                AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.B200_PROFILE_1G_23GB,
                1.0/8,
                1.0/7,
                7
        ));
        b200_180_gb_profiles.add(new AcceleratorProfile(
                AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.B200_PROFILE_1G_45GB,
                2.0/8,
                1.0/7,
                4
        ));
        b200_180_gb_profiles.add(new AcceleratorProfile(
                AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.B200_PROFILE_2G_45GB,
                2.0/8,
                2.0/7,
                3
        ));
        b200_180_gb_profiles.add(new AcceleratorProfile(
                AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.B200_PROFILE_3G_90GB,
                4.0/8,
                3.0/7,
                2
        ));
        b200_180_gb_profiles.add(new AcceleratorProfile(
                AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.B200_PROFILE_4G_90GB,
                4.0/8,
                4.0/7,
                1
        ));
        b200_180_gb_profiles.add(new AcceleratorProfile(
                AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.B200_PROFILE_7G_180GB,
                1,
                1,
                1
        ));

        List<AcceleratorProfile> rtx_pro_5000_48gb_profiles = new ArrayList<>();
        rtx_pro_5000_48gb_profiles.add(new AcceleratorProfile(
                AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.RTX_PRO_5000_PROFILE_1G_12GB,
                1.0/4,
                1.0/4,
                3
        ));
        rtx_pro_5000_48gb_profiles.add(new AcceleratorProfile(
                AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.RTX_PRO_5000_PROFILE_2G_24GB,
                2.0/4,
                2.0/4,
                1
        ));
        rtx_pro_5000_48gb_profiles.add(new AcceleratorProfile(
                AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.RTX_PRO_5000_PROFILE_4G_48GB,
                1,
                1,
                1
        ));

        List<AcceleratorProfile> rtx_pro_6000_96gb_profiles  = new ArrayList<>();
        rtx_pro_6000_96gb_profiles.add(new AcceleratorProfile(
                AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.RTX_PRO_6000_PROFILE_1G_24GB,
                1.0/4,
                1.0/4,
                4
        ));
        rtx_pro_6000_96gb_profiles.add(new AcceleratorProfile(
                AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.RTX_PRO_6000_PROFILE_2G_48GB,
                2.0/4,
                2.0/4,
                2
        ));
        rtx_pro_6000_96gb_profiles.add(new AcceleratorProfile(
                AnalyzerConstants.AcceleratorConstants.AcceleratorProfiles.RTX_PRO_6000_PROFILE_4G_96GB,
                1,
                1,
                1
        ));

        acceleratorProfilesMap.put(AnalyzerConstants.AcceleratorConstants.SupportedAccelerators.A100_80_GB, commonProfiles);
        acceleratorProfilesMap.put(AnalyzerConstants.AcceleratorConstants.SupportedAccelerators.H100_80_GB, commonProfiles);
        acceleratorProfilesMap.put(AnalyzerConstants.AcceleratorConstants.SupportedAccelerators.A100_40_GB, a100_40_gb_profiles);
        acceleratorProfilesMap.put(AnalyzerConstants.AcceleratorConstants.SupportedAccelerators.H200_141_GB, h200_141_gb_profiles);
        acceleratorProfilesMap.put(AnalyzerConstants.AcceleratorConstants.SupportedAccelerators.B200_180_GB, b200_180_gb_profiles);
        acceleratorProfilesMap.put(AnalyzerConstants.AcceleratorConstants.SupportedAccelerators.BW_RTX_PRO_5000_48_GB, rtx_pro_5000_48gb_profiles);
        acceleratorProfilesMap.put(AnalyzerConstants.AcceleratorConstants.SupportedAccelerators.BW_RTX_PRO_6000_96_GB, rtx_pro_6000_96gb_profiles);
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
                && !modelName.equalsIgnoreCase(AnalyzerConstants.AcceleratorConstants.SupportedAccelerators.A100_40_GB)
                && !modelName.equalsIgnoreCase(AnalyzerConstants.AcceleratorConstants.SupportedAccelerators.H200_141_GB)
                && !modelName.equalsIgnoreCase(AnalyzerConstants.AcceleratorConstants.SupportedAccelerators.B200_180_GB)
                && !modelName.equalsIgnoreCase(AnalyzerConstants.AcceleratorConstants.SupportedAccelerators.BW_RTX_PRO_5000_48_GB)
                && !modelName.equalsIgnoreCase(AnalyzerConstants.AcceleratorConstants.SupportedAccelerators.BW_RTX_PRO_6000_96_GB)
        ) {
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
