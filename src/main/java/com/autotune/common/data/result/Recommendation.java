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
package com.autotune.common.data.result;

import com.autotune.utils.AnalyzerConstants;

import java.sql.Timestamp;
import java.util.HashMap;

/**
 * Storage object for recommendation
 */

public class Recommendation {
    private Timestamp monitoringStartTime;
    private Timestamp monitoringEndTime;
    private Double duration_in_hours;
    private int podsCount;
    private double confidence_level;
    private String errorMessage;

    private HashMap<AnalyzerConstants.CapacityMax, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> config;

    public Recommendation(Timestamp monitoringStartTime, Timestamp monitoringEndTime) {
        this.monitoringStartTime = monitoringStartTime;
        this.monitoringEndTime = monitoringEndTime;
    }

    public Recommendation(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Timestamp getMonitoringStartTime() {
        return monitoringStartTime;
    }

    public void setMonitoringStartTime(Timestamp monitoringStartTime) {
        this.monitoringStartTime = monitoringStartTime;
    }

    public Timestamp getMonitoringEndTime() {
        return monitoringEndTime;
    }

    public void setMonitoringEndTime(Timestamp monitoringEndTime) {
        this.monitoringEndTime = monitoringEndTime;
    }

    public Double getDuration_in_hours() {
        return duration_in_hours;
    }

    public void setDuration_in_hours(Double duration_in_hours) {
        this.duration_in_hours = duration_in_hours;
    }

    public int getPodsCount() {
        return podsCount;
    }

    public void setPodsCount(int podsCount) {
        this.podsCount = podsCount;
    }

    public double getConfidence_level() {
        return confidence_level;
    }

    public void setConfidence_level(double confidence_level) {
        this.confidence_level = confidence_level;
    }

    public HashMap<AnalyzerConstants.CapacityMax, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> getConfig() {
        return config;
    }

    public void setConfig(HashMap<AnalyzerConstants.CapacityMax, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> config) {
        this.config = config;
    }

    @Override
    public String toString() {
        return "Recommendation{" +
                "monitoringStartTime=" + monitoringStartTime +
                ", monitoringEndTime=" + monitoringEndTime +
                ", duration_in_hours=" + duration_in_hours +
                ", podsCount=" + podsCount +
                ", confidence_level=" + confidence_level +
                ", config=" + config +
                '}';
    }
}
