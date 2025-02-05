package com.autotune.analyzer.recommendations.updater.settings;

public class AutoscalerSettings {
    private static volatile AutoscalerSettings autoscalerSettings;
    private boolean allowGPUResourceUpdates;
    private boolean koEditPermissionsAvailable;

    private AutoscalerSettings() {
        this.allowGPUResourceUpdates = false;
        this.koEditPermissionsAvailable = false;
    }

    public static AutoscalerSettings getInstance() {
        if (autoscalerSettings == null) {
            synchronized (AutoscalerSettings.class) {
                if (autoscalerSettings == null) {
                    autoscalerSettings = new AutoscalerSettings();
                }
            }
        }
        return autoscalerSettings;
    }

    public boolean isAllowGPUResourceUpdates() {
        return allowGPUResourceUpdates;
    }

    public void setAllowGPUResourceUpdates(boolean allowGPUResourceUpdates) {
        this.allowGPUResourceUpdates = allowGPUResourceUpdates;
    }

    public boolean isKoEditPermissionsAvailable() {
        return koEditPermissionsAvailable;
    }

    public void setKoEditPermissionsAvailable(boolean koEditPermissionsAvailable) {
        this.koEditPermissionsAvailable = koEditPermissionsAvailable;
    }
}