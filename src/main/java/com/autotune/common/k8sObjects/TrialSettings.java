/*******************************************************************************
 * Copyright (c) 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.common.k8sObjects;

import com.google.gson.annotations.SerializedName;

/**
 * ToDO TrialSettings in common.trials and this class should be merged
 */
public class TrialSettings {
    @SerializedName("measurement_duration")
    private String measurement_durationMinutes;
    /**
     * Indicates number of times that same tails should get executed to collect accurate metrics.
     */
    @SerializedName("iterations")
    private String trialIterations;
    /**
     * Total time took by cluster/Node to spin up newly created deployments pods .
     */
    @SerializedName("warmup_duration")
    private String trialWarmupDuration;
    /**
     * Indicates number of times that same tails should get executed to collect accurate metrics after warmup_durations.
     */
    @SerializedName("warmup_cycles")
    private String trialWarmupCycles;
    @SerializedName("measurement_cycles")
    private String trialMeasurementCycles;

    public String getMeasurement_durationMinutes() {
        return measurement_durationMinutes;
    }

    public void setMeasurement_durationMinutes(String measurement_durationMinutes) {
        this.measurement_durationMinutes = measurement_durationMinutes;
    }

    public Double getMeasurement_durationMinutes_inDouble() {
        String measurementDurationInMins = getMeasurement_durationMinutes();
        return Double.parseDouble(measurementDurationInMins.substring(0, measurementDurationInMins.length() - 3));
    }

    @Override
    public String toString() {
        return "TrialSettings{" +
                "measurement_durationMinutes=" + measurement_durationMinutes +
                '}';
    }
}
