package com.autotune.analyzer.metricProfiles;

import com.autotune.analyzer.metricProfiles.utils.MetricProfileUtil;
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
 * Maintains information about the Metric Profile objects deployed in the cluster
 */
public class MetricProfilesDeployment {
    public static Map<String, MetricProfile> metricProfilesMap = new HashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricProfile.class);

    /**
     * Get Metric Profile objects from kubernetes, and watch for any additions, modifications or deletions.
     *
     */
    public static void getMetricProfiles() {
        /* Watch for events (additions, modifications or deletions) of metric profile objects */
        Watcher<String> metricProfileObjectWatcher = new Watcher<>() {
            @Override
            public void eventReceived(Action action, String resource) {
                MetricProfile metricProfile = null;

                switch (action.toString().toUpperCase()) {
                    case "ADDED":
                        metricProfile = getMetricProfile(resource);
                        if ( validateMetricProfile(metricProfile))
                            MetricProfileUtil.addMetricProfile(metricProfilesMap, metricProfile);
                        break;
                    case "MODIFIED":
                        metricProfile = getMetricProfile(resource);
                        if (metricProfile != null) {
                            // Check if any of the values have changed from the existing object in the map
                            if (!metricProfilesMap.get(metricProfile.getName())
                                    .equals(metricProfile)) {
                                if (validateMetricProfile(metricProfile)) {
                                    deleteExistingMetricProfile(resource);
                                    MetricProfileUtil.addMetricProfile(metricProfilesMap, metricProfile);
                                }
                            }
                        }
                        break;
                    case "DELETED":
                        deleteExistingMetricProfile(resource);
                    default:
                        break;
                }
            }

            @Override
            public void onClose(KubernetesClientException e) { }
        };

        KubernetesServices kubernetesServices = new KubernetesServicesImpl();
        kubernetesServices.addWatcher(KubernetesContexts.getPerformanceProfileCrdContext(), metricProfileObjectWatcher);
    }

    private static boolean validateMetricProfile(MetricProfile metricProfile) {
        boolean validationStatus = false;
        try {
            if (null != metricProfile) {
                ValidationOutputData validationOutputData = new MetricProfileValidation(metricProfilesMap).validate(metricProfile);
                if (validationOutputData.isSuccess())
                    validationStatus = true;
                else
                    new KubeEventLogger(Clock.systemUTC()).log("Failed", validationOutputData.getMessage(), EventLogger.Type.Warning, null, null, null, null);
            } else
                    new KubeEventLogger(Clock.systemUTC()).log("Failed", "Validation of metric profile failed! ", EventLogger.Type.Warning, null, null, null, null);
        } catch (Exception e) {
            new KubeEventLogger(Clock.systemUTC()).log("Failed", e.getMessage(), EventLogger.Type.Warning, null, null, null, null);
        }
        return validationStatus;
    }

    public static MetricProfile getMetricProfile(String metricProfileObjectJsonStr) {
        try {
            JSONObject metricProfileProfileObjectJson = new JSONObject(metricProfileObjectJsonStr);
            JSONObject metadataJson = metricProfileProfileObjectJson
                    .getJSONObject(AnalyzerConstants.AutotuneObjectConstants.METADATA);
            metricProfileProfileObjectJson.remove("apiversion");
            metricProfileProfileObjectJson.remove("kind");
            metricProfileProfileObjectJson.remove("metadata");

            metricProfileProfileObjectJson.put("name",metadataJson.optString(AnalyzerConstants.PerformanceProfileConstants.PERF_PROFILE_NAME));
            MetricProfile metricProfile = new Gson().fromJson(metricProfileProfileObjectJson.toString(), MetricProfile.class);

            return metricProfile;

        } catch (NullPointerException | JSONException e) {
            LOGGER.error("Exception occurred while parsing the data: {}",e.getMessage());
            return null;
        }
    }

    /**
     * Delete the metric Profile object corresponding to the passed parameter
     *
     * @param metricProfileObject - removes the Metric Profile object from the map
     */
    private static void deleteExistingMetricProfile(String metricProfileObject) {
        JSONObject metricProfileObjectJson = new JSONObject(metricProfileObject);
        String name = metricProfileObjectJson.getJSONObject(AnalyzerConstants.AutotuneObjectConstants.METADATA)
                .optString(AnalyzerConstants.AutotuneObjectConstants.NAME);

        metricProfilesMap.remove(name);
        LOGGER.info("Deleted metric profile object {}", name);
    }
}
