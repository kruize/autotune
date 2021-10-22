package com.autotune.analyzer.experiments;

import java.util.ArrayList;

public class DeploymentTracking {
    private final ArrayList<String> trackers;

    public DeploymentTracking(ArrayList<String> trackers) {
        this.trackers = trackers;
    }

    public ArrayList<String> getTrackers() {
        return trackers;
    }
}