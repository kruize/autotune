/*******************************************************************************
 * Copyright (c) 2021, 2022 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.autotune.common.experiments;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Example
 *             "deployment_tracking": {
 *                 "trackers": [
 *                     "training"
 *                 ]
 *             }
 */
public class DeploymentTracking {
    @SerializedName("trackers")
    private final ArrayList<String> trackers;

    public DeploymentTracking(ArrayList<String> trackers) {
        this.trackers = trackers;
    }

    public ArrayList<String> getTrackers() {
        return trackers;
    }

    @Override
    public String toString() {
        return "DeploymentTracking{" +
                "trackers=" + trackers.toString() +
                '}';
    }
}
