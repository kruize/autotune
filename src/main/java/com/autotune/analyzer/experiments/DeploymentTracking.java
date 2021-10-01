package com.autotune.analyzer.experiments;

import java.util.ArrayList;

public class DeploymentTracking {
    ArrayList<String> trackers;

    public DeploymentTracking(ArrayList<String> trackers) {
        this.trackers = trackers;
    }

    public ArrayList<String> getTrackers() {
        return trackers;
    }

    public void setTrackers(ArrayList<String> trackers) {
        this.trackers = trackers;
    }
}
