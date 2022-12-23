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

public class TrialSettings {
    @SerializedName("measurement_duration")
    private String measurement_durationMinutes;
    private String format;

    public String getMeasurement_durationMinutes() {
        return measurement_durationMinutes;
    }

    public void setMeasurement_durationMinutes(String measurement_durationMinutes) {
        this.measurement_durationMinutes = measurement_durationMinutes;
    }

    @Override
    public String toString() {
        return "TrialSettings{" +
                "measurement_durationMinutes=" + measurement_durationMinutes +
                '}';
    }
}
