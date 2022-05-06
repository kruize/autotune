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

/**
 * This object holds information about trail run configurations.
 * Example
 *        "trial_settings": {
 *             "measurement_cycles": "3",
 *             "warmup_duration": "1min",
 *             "warmup_cycles": "3",
 *             "measurement_duration": "1min",
 *             "iterations": "3"
 *         }
 */
public class TrialSettings {
    /**
     * Indicates number of times that same tails should get executed to collect accurate metrics.
     */
    @SerializedName("iterations")
    private final String trialIterations;
    /**
     * Total time took by cluster/Node to spin up newly created deployments pods .
     */
    @SerializedName("warmup_duration")
    private final String trialWarmupDuration;
    /**
     * Indicates number of times that same tails should get executed to collect accurate metrics after warmup_durations.
     */
    @SerializedName("warmup_cycles")
    private final String trialWarmupCycles;
    @SerializedName("measurement_duration")
    private final String trialMeasurementDuration;
    @SerializedName("measurement_cycles")
    private final String trialMeasurementCycles;

    public TrialSettings(String trialIterations,
                         String trialWarmupDuration,
                         String trialWarmupCycles,
                         String trialMeasurementDuration,
                         String trialMeasurementCycles) {
        this.trialIterations = trialIterations;
        this.trialWarmupDuration = trialWarmupDuration;
        this.trialWarmupCycles = trialWarmupCycles;
        this.trialMeasurementDuration = trialMeasurementDuration;
        this.trialMeasurementCycles = trialMeasurementCycles;
    }

    public String getTrialMeasurementDuration() {
        return trialMeasurementDuration;
    }

    public String getTrialIterations() {
        return trialIterations;
    }

    public String getTrialWarmupDuration() {
        return trialWarmupDuration;
    }

    public String getTrialWarmupCycles() {
        return trialWarmupCycles;
    }

    public String getTrialMeasurementCycles() {
        return trialMeasurementCycles;
    }

    @Override
    public String toString() {
        return "TrialSettings{" +
                "trialIterations='" + trialIterations + '\'' +
                ", trialWarmupDuration='" + trialWarmupDuration + '\'' +
                ", trialWarmupCycles='" + trialWarmupCycles + '\'' +
                ", trialMeasurementDuration='" + trialMeasurementDuration + '\'' +
                ", trialMeasurementCycles='" + trialMeasurementCycles + '\'' +
                '}';
    }
}
