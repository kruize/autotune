/*******************************************************************************
 * Copyright (c) 2021, 2021 Red Hat, IBM Corporation and others.
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

/**
 *
 */
public class TrialSettings {
    private final String trialIterations;
    private final String trialWarmupDuration;
    private final String trialWarmupCycles;
    private final String trialMeasurementDuration;
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

    public String getTrialIterations() { return trialIterations; }

    public String getTrialWarmupDuration() { return trialWarmupDuration; }

    public String getTrialWarmupCycles() { return trialWarmupCycles; }

    public String getTrialMeasurementCycles() { return trialMeasurementCycles; }
}
