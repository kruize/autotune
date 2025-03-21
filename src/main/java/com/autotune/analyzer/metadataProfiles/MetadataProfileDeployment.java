package com.autotune.analyzer.metadataProfiles;

import com.autotune.analyzer.metadataProfiles.utils.MetadataProfileUtil;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.k8sObjects.KubernetesContexts;
import com.autotune.common.target.kubernetes.service.KubernetesServices;
import com.autotune.common.target.kubernetes.service.impl.KubernetesServicesImpl;
import com.autotune.utils.EventLogger;
import com.autotune.utils.KubeEventLogger;
import com.google.gson.Gson;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

public class MetadataProfileDeployment {
    public static Map<String, MetadataProfile> metadataProfilesMap = new HashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataProfileDeployment.class);

    /**
     * Get Metadata Profile objects from kubernetes, and watch for any additions, modifications or deletions.
     *
     */
    public static void getMetadataProfiles() {
        /* Watch for events (additions, modifications or deletions) of metadata profile objects */
        Watcher<String> metadataProfileObjectWatcher = new Watcher<>() {
            @Override
            public void eventReceived(Action action, String resource) {
                MetadataProfile metadataProfile;

                switch (action.toString().toUpperCase()) {
                    case "ADDED":
                        metadataProfile = getMetadataProfile(resource);
                        if (validateMetadataProfile(metadataProfile))
                            MetadataProfileUtil.addMetadataProfile(metadataProfilesMap, metadataProfile);
                        break;
                    case "MODIFIED":
                        metadataProfile = getMetadataProfile(resource);
                        if (null != metadataProfile) {
                            String metadataProfileName = metadataProfile.getMetadata().get("name").asText();
                            // Check if any of the values have changed from the existing object in the map
                            if (null != metadataProfileName && !metadataProfileName.isEmpty() &&
                                    !metadataProfilesMap.get(metadataProfileName).equals(metadataProfile)) {
                                if (validateMetadataProfile(metadataProfile)) {
                                    deleteExistingMetadataProfile(resource);
                                    MetadataProfileUtil.addMetadataProfile(metadataProfilesMap, metadataProfile);
                                }
                            }
                        }
                        break;
                    case "DELETED":
                        deleteExistingMetadataProfile(resource);
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onClose(WatcherException e) { }
        };

        KubernetesServices kubernetesServices = new KubernetesServicesImpl();
        kubernetesServices.addWatcher(KubernetesContexts.getMetadataProfileCrdContext(), metadataProfileObjectWatcher);
    }

    private static boolean validateMetadataProfile(MetadataProfile metadataProfile) {
        boolean validationStatus = false;
        KubeEventLogger kubeEventLogger = new KubeEventLogger(Clock.systemUTC());
        try {
            if (null != metadataProfile) {
                ValidationOutputData validationOutputData = new MetadataProfileValidation(metadataProfilesMap).validate(metadataProfile);
                if (validationOutputData.isSuccess())
                    validationStatus = true;
                else
                    kubeEventLogger.log("Failed", validationOutputData.getMessage(), EventLogger.Type.Warning, null, null, null, null);
            } else {
                kubeEventLogger.log("Failed", AnalyzerErrorConstants.AutotuneObjectErrors.METADATA_PROFILE_VALIDATION_FAILED, EventLogger.Type.Warning, null, null, null, null);
            }
        } catch (Exception e) {
            kubeEventLogger.log("Failed", e.getMessage(), EventLogger.Type.Warning, null, null, null, null);
        }
        return validationStatus;
    }

    public static MetadataProfile getMetadataProfile(String metadataProfileObjectJsonStr) {
        try {
            JSONObject metadataProfileObjectJson = new JSONObject(metadataProfileObjectJsonStr);
            JSONObject metadataJson = metadataProfileObjectJson
                    .getJSONObject(AnalyzerConstants.AutotuneObjectConstants.METADATA);
            metadataProfileObjectJson.remove("apiversion");
            metadataProfileObjectJson.remove("kind");
            metadataProfileObjectJson.remove("metadata");

            String metadataProfileName = metadataJson.optString(AnalyzerConstants.MetadataProfileConstants.METADATA_PROFILE_NAME);

            if (metadataProfileName == null || metadataProfileName.isEmpty()) {
                throw new JSONException(AnalyzerErrorConstants.AutotuneObjectErrors.INVALID_METADATA_PROFILE_NAME);
            }

            metadataProfileObjectJson.put("name", metadataProfileName);
            MetadataProfile metadataProfile = new Gson().fromJson(metadataProfileObjectJson.toString(), MetadataProfile.class);

            return metadataProfile;

        } catch (NullPointerException | JSONException e) {
            LOGGER.error(AnalyzerErrorConstants.AutotuneObjectErrors.PARSE_ERROR_MESSAGE,e.getMessage());
            return null;
        }
    }

    /**
     * Delete the metadata Profile object corresponding to the passed parameter
     *
     * @param metadataProfileObject - removes the Metadata Profile object from the map
     */
    private static void deleteExistingMetadataProfile(String metadataProfileObject) {
        JSONObject metadataProfileObjectJson = new JSONObject(metadataProfileObject);
        String name = metadataProfileObjectJson.getJSONObject(AnalyzerConstants.AutotuneObjectConstants.METADATA)
                .optString(AnalyzerConstants.AutotuneObjectConstants.NAME);

        metadataProfilesMap.remove(name);
        LOGGER.debug(AnalyzerErrorConstants.AutotuneObjectErrors.DELETED_METADATA_PROFILE, name);
    }
}
