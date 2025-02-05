package com.autotune.analyzer.recommendations.autoscaler.settings;

import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.List;

public class AutoscalingSettings {
    private static volatile AutoscalingSettings autoscalingSettings;
    private boolean allowGPUResourceUpdates;
    private boolean koEditPermissionsAvailable;

    private AutoscalingSettings() {
        this.allowGPUResourceUpdates = false;
        this.koEditPermissionsAvailable = false;
    }

    public static AutoscalingSettings getInstance() {
        if (autoscalingSettings == null) {
            synchronized (AutoscalingSettings.class) {
                if (autoscalingSettings == null) {
                    autoscalingSettings = new AutoscalingSettings();
                }
            }
        }
        return autoscalingSettings;
    }

    public boolean isAllowGPUResourceUpdates() {
        return allowGPUResourceUpdates;
    }

    private void setAllowGPUResourceUpdates(boolean allowGPUResourceUpdates) {
        this.allowGPUResourceUpdates = allowGPUResourceUpdates;
    }

    public boolean isKoEditPermissionsAvailable() {
        return koEditPermissionsAvailable;
    }

    private void setKoEditPermissionsAvailable(boolean koEditPermissionsAvailable) {
        this.koEditPermissionsAvailable = koEditPermissionsAvailable;
    }

    public void checkIfInstasliceIsAvailable() {
        try (KubernetesClient client = new DefaultKubernetesClient()) {
            List<DaemonSet> daemonSets = client.apps().daemonSets().inAnyNamespace().list().getItems();
            List<DaemonSet> matchingDaemonSets = daemonSets.stream()
                    .filter(ds -> ds.getMetadata().getName().contains("instaslice"))
                    .toList();
            setAllowGPUResourceUpdates(!matchingDaemonSets.isEmpty());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initialiseAutoscalingSettings() {
        /**
         * Uncomment the below line in MVP to check for instaslice installation
         */
        checkIfInstasliceIsAvailable();

        /**
         * The setters below should be replaced with appropriate functions
         * which check and update the settings
         *
         * Hardcoding it for now
         */
        setKoEditPermissionsAvailable(true);
    }
}