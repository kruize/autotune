package com.autotune.analyzer.recommendations.autoscaler.settings;

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