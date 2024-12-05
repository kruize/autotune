/*******************************************************************************
 * Copyright (c) 2022, 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.analyzer.kruizeObject;

/**
 * THis is a placeholder class for bulkAPI createExperiment template to store defaults
 */
public class CreateExperimentConfigBean {

    // Private fields
    private String mode;
    private String target;
    private String version;
    private String datasourceName;
    private String performanceProfile;
    private String metadataProfile;
    private double threshold;
    private String measurementDurationStr;
    private int measurementDuration;

    // Getters and Setters
    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDatasourceName() {
        return datasourceName;
    }

    public void setDatasourceName(String datasourceName) {
        this.datasourceName = datasourceName;
    }

    public String getPerformanceProfile() {
        return performanceProfile;
    }

    public void setPerformanceProfile(String performanceProfile) {
        this.performanceProfile = performanceProfile;
    }

    public String getMetadataProfile() {
        return metadataProfile;
    }

    public void setMetadataProfile(String metadataProfile) {
        this.metadataProfile = metadataProfile;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public String getMeasurementDurationStr() {
        return measurementDurationStr;
    }

    public void setMeasurementDurationStr(String measurementDurationStr) {
        this.measurementDurationStr = measurementDurationStr;
    }

    public int getMeasurementDuration() {
        return measurementDuration;
    }

    public void setMeasurementDuration(int measurementDuration) {
        this.measurementDuration = measurementDuration;
    }

    @Override
    public String toString() {
        return "CreateExperimentConfigBean{" +
                "mode='" + mode + '\'' +
                ", target='" + target + '\'' +
                ", version='" + version + '\'' +
                ", datasourceName='" + datasourceName + '\'' +
                ", performanceProfile='" + performanceProfile + '\'' +
                ", metadataProfile='" + metadataProfile + '\'' +
                ", threshold=" + threshold +
                ", measurementDurationStr='" + measurementDurationStr + '\'' +
                ", measurementDuration=" + measurementDuration +
                '}';
    }
}
