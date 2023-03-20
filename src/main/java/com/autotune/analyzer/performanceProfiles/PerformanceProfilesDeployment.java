package com.autotune.analyzer.performanceProfiles;

import com.autotune.analyzer.performanceProfiles.PerformanceProfileInterface.PerfProfileImpl;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.k8sObjects.*;
import com.autotune.common.target.kubernetes.service.KubernetesServices;
import com.autotune.common.target.kubernetes.service.impl.KubernetesServicesImpl;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.utils.EventLogger;
import com.autotune.utils.KubeEventLogger;
import com.google.gson.Gson;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

/**
 * Maintains information about the Performance Profile objects deployed in the cluster
 */
public class PerformanceProfilesDeployment {
    public static Map<String, PerformanceProfile> performanceProfilesMap = new HashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceProfile.class);

    /**
     * Get Performance Profile objects from kubernetes, and watch for any additions, modifications or deletions.
     *
     */
    public static void getPerformanceProfiles() {
        /* Watch for events (additions, modifications or deletions) of performance profile objects */
        Watcher<String> performanceProfileObjectWatcher = new Watcher<>() {
            @Override
            public void eventReceived(Action action, String resource) {
                PerformanceProfile performanceProfile = null;

                switch (action.toString().toUpperCase()) {
                    case "ADDED":
                        performanceProfile = getPerformanceProfile(resource);
                        if ( validatePerformanceProfile(performanceProfile))
                            new PerfProfileImpl().addPerformanceProfile(performanceProfilesMap, performanceProfile);
                        break;
                    case "MODIFIED":
                        performanceProfile = getPerformanceProfile(resource);
                        if (performanceProfile != null) {
                            // Check if any of the values have changed from the existing object in the map
                            if (!performanceProfilesMap.get(performanceProfile.getName())
                                    .equals(performanceProfile)) {
                                if (validatePerformanceProfile(performanceProfile)) {
                                    deleteExistingPerformanceProfile(resource);
                                    new PerfProfileImpl().addPerformanceProfile(performanceProfilesMap, performanceProfile);
                                }
                            }
                        }
                        break;
                    case "DELETED":
                        deleteExistingPerformanceProfile(resource);
                    default:
                        break;
                }
            }

            @Override
            public void onClose(KubernetesClientException e) { }
        };

        KubernetesServices kubernetesServices = new KubernetesServicesImpl();
        kubernetesServices.addWatcher(KubernetesContexts.getPerformanceProfileCrdContext(), performanceProfileObjectWatcher);
    }

    private static boolean validatePerformanceProfile(PerformanceProfile performanceProfile) {
        boolean validationStatus = false;
        try {
            if (null != performanceProfile) {
                ValidationOutputData validationOutputData = new PerformanceProfileValidation(performanceProfilesMap).validate(performanceProfile);
                if (validationOutputData.isSuccess())
                    validationStatus = true;
                else
                    new KubeEventLogger(Clock.systemUTC()).log("Failed", validationOutputData.getMessage(), EventLogger.Type.Warning, null, null, null, null);
            } else
                    new KubeEventLogger(Clock.systemUTC()).log("Failed", "Validation of performance profile failed! ", EventLogger.Type.Warning, null, null, null, null);
        } catch (Exception e) {
            new KubeEventLogger(Clock.systemUTC()).log("Failed", e.getMessage(), EventLogger.Type.Warning, null, null, null, null);
        }
        return validationStatus;
    }

    public static PerformanceProfile getPerformanceProfile(String performanceProfileObjectJsonStr) {
        try {
            JSONObject performanceProfileObjectJson = new JSONObject(performanceProfileObjectJsonStr);
            JSONObject metadataJson = performanceProfileObjectJson
                    .getJSONObject(AnalyzerConstants.AutotuneObjectConstants.METADATA);
            performanceProfileObjectJson.remove("apiversion");
            performanceProfileObjectJson.remove("kind");
            performanceProfileObjectJson.remove("metadata");

            performanceProfileObjectJson.put("name",metadataJson.optString(AnalyzerConstants.PerformanceProfileConstants.PERF_PROFILE_NAME));
            PerformanceProfile performanceProfile = new Gson().fromJson(performanceProfileObjectJson.toString(), PerformanceProfile.class);

            return performanceProfile;

        } catch (NullPointerException | JSONException e) {
            LOGGER.error("Exception occurred while parsing the data: {}",e.getMessage());
            return null;
        }
    }

    /**
     * Delete the performance Profile object corresponding to the passed parameter
     *
     * @param performanceProfileObject - removes the Performance Profile object from the map
     */
    private static void deleteExistingPerformanceProfile(String performanceProfileObject) {
        JSONObject performanceProfileObjectJson = new JSONObject(performanceProfileObject);
        String name = performanceProfileObjectJson.getJSONObject(AnalyzerConstants.AutotuneObjectConstants.METADATA)
                .optString(AnalyzerConstants.AutotuneObjectConstants.NAME);

        performanceProfilesMap.remove(name);
        LOGGER.info("Deleted performance profile object {}", name);
    }
}
