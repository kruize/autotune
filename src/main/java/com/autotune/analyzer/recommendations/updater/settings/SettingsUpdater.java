package com.autotune.analyzer.recommendations.updater.settings;

import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.List;

public class SettingsUpdater {
    public static void checkIfInstasliceIsAvailable(AutoscalerSettings autoscalerSettings) {
        try (KubernetesClient client = new DefaultKubernetesClient()) {
            List<DaemonSet> daemonSets = client.apps().daemonSets().inAnyNamespace().list().getItems();
            List<DaemonSet> matchingDaemonSets = daemonSets.stream()
                    .filter(ds -> ds.getMetadata().getName().contains("instaslice"))
                    .toList();
            autoscalerSettings.setAllowGPUResourceUpdates(!matchingDaemonSets.isEmpty());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void initialiseAutoscalerSettings() {
        AutoscalerSettings autoscalerSettings = AutoscalerSettings.getInstance();

        /**
         * Uncomment the below line in MVP to check for instaslice installation
         */
         checkIfInstasliceIsAvailable(autoscalerSettings);

        /**
         * The setters below should be replaced with appropriate functions
         * which check and update the settings
         *
         * Hardcoding it for now
         */
        autoscalerSettings.setKoEditPermissionsAvailable(true);
    }
}