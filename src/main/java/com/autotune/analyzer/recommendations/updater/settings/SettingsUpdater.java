package com.autotune.analyzer.recommendations.updater.settings;

import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.List;

public class SettingsUpdater {
    public static void checkIfInstasliceIsAvailable(AutoscalingSettings autoscalingSettings) {
        try (KubernetesClient client = new DefaultKubernetesClient()) {
            List<DaemonSet> daemonSets = client.apps().daemonSets().inAnyNamespace().list().getItems();
            List<DaemonSet> matchingDaemonSets = daemonSets.stream()
                    .filter(ds -> ds.getMetadata().getName().contains("instaslice"))
                    .toList();
            autoscalingSettings.setAllowGPUResourceUpdates(!matchingDaemonSets.isEmpty());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void initialiseAutoscalingSettings() {
        AutoscalingSettings autoscalingSettings = AutoscalingSettings.getInstance();

        /**
         * Uncomment the below line in MVP to check for instaslice installation
         */
         checkIfInstasliceIsAvailable(autoscalingSettings);

        /**
         * The setters below should be replaced with appropriate functions
         * which check and update the settings
         *
         * Hardcoding it for now
         */
        autoscalingSettings.setKoEditPermissionsAvailable(true);
    }
}