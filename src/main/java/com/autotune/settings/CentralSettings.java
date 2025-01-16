package com.autotune.settings;

public class CentralSettings {
    private static volatile CentralSettings centralSettings;
    private boolean allowGPUResourceUpdates;
    private boolean koEditPermissionsAvailable;

    private CentralSettings() {
        this.allowGPUResourceUpdates = false;
        this.koEditPermissionsAvailable = false;
    }

    public static CentralSettings getInstance() {
        if (centralSettings == null) {
            synchronized (CentralSettings.class) {
                if (centralSettings == null) {
                    centralSettings = new CentralSettings();
                }
            }
        }
        return centralSettings;
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