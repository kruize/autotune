/*******************************************************************************
 * Copyright (c) 2023 Red Hat, IBM Corporation and others.
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

package com.autotune.analyzer.serviceObjects;

import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

public class DSMetadataAPIObject {
    private String version;
    @SerializedName(KruizeConstants.JSONKeys.DATASOURCE_NAME)
    private String dataSourceName;
    @SerializedName(KruizeConstants.JSONKeys.METADATA_PROFILE)
    private String metadataProfileName;
    @SerializedName(KruizeConstants.JSONKeys.MEASUREMENT_DURATION)
    private String measurement_durationMinutes;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public String getMetadataProfile() { return metadataProfileName; }

    public void setMetadataProfile(String metadataProfileName) { this.metadataProfileName = metadataProfileName; }

    public String getMeasurementDurationMinutes() { return measurement_durationMinutes; }

    public void setMeasurementDurationMinutes(String measurement_durationMinutes) {this.measurement_durationMinutes = measurement_durationMinutes;}
}
