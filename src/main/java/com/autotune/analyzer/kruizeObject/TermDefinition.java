/*******************************************************************************
 * Copyright (c) 2025 Red Hat, IBM Corporation and others.
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

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TermDefinition {

    @SerializedName("duration_in_days")
    private Double durationInDays;

    @SerializedName("start_timestamp")
    private String startTimestamp;

    @SerializedName("end_timestamp")
    private String endTimestamp;

    @SerializedName("duration_threshold")
    private String durationThreshold;

    @SerializedName("plots_datapoint")
    private Integer plotsDatapoint;

    @SerializedName("plots_datapoint_delta_in_days")
    private Double plotsDatapointDeltaInDays;

    @SerializedName("days_of_week")
    private List<String> daysOfWeek;

    @SerializedName("daily_start_time")
    private String dailyStartTime;

    @SerializedName("daily_end_time")
    private String dailyEndTime;


    public TermDefinition() {}

    public Double getDurationInDays() {
        return durationInDays;
    }

    public void setDurationInDays(Double durationInDays) {
        this.durationInDays = durationInDays;
    }

    public String getDurationThreshold() {
        return durationThreshold;
    }

    public void setDurationThreshold(String durationThreshold) {
        this.durationThreshold = durationThreshold;
    }

    public Integer getPlotsDatapoint() {
        return plotsDatapoint;
    }

    public void setPlotsDatapoint(Integer plotsDatapoint) {
        this.plotsDatapoint = plotsDatapoint;
    }

    public Double getPlotsDatapointDeltaInDays() {
        return plotsDatapointDeltaInDays;
    }

    public void setPlotsDatapointDeltaInDays(Double plotsDatapointDeltaInDays) {
        this.plotsDatapointDeltaInDays = plotsDatapointDeltaInDays;
    }

    public String getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(String startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public String getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(String endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public List<String> getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(List<String> daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    public String getDailyStartTime() {
        return dailyStartTime;
    }

    public void setDailyStartTime(String dailyStartTime) {
        this.dailyStartTime = dailyStartTime;
    }

    public String getDailyEndTime() {
        return dailyEndTime;
    }

    public void setDailyEndTime(String dailyEndTime) {
        this.dailyEndTime = dailyEndTime;
    }

    @Override
    public String toString() {
        return "TermDefinition{" +
                ", durationInDays=" + durationInDays +
                ", startTimestamp='" + startTimestamp + '\'' +
                ", endTimestamp='" + endTimestamp + '\'' +
                ", durationThreshold='" + durationThreshold +
                ", plotsDatapoint=" + plotsDatapoint +
                ", plotsDatapointDeltaInDays=" + plotsDatapointDeltaInDays +
                ", daysOfWeek=" + daysOfWeek +
                ", dailyStartTime='" + dailyStartTime + '\'' +
                ", dailyEndTime='" + dailyEndTime + '\'' +
                '}';
    }
}