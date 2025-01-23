package com.autotune.analyzer.kruizeObject;

import com.google.gson.annotations.SerializedName;

public class TermDetails {
    @SerializedName("duration_in_days")
    private Integer durationInDays;
    @SerializedName("duration_threshold")
    private Integer durationThreshold;
    @SerializedName("plots_datapoint")
    private Integer plotsDatapoint;
    @SerializedName("plots_datapoint_delta_in_days")
    private Double plotsDatapointDeltaInDays;

    public TermDetails() {
    }

    public Integer getDurationInDays() {
        return durationInDays;
    }

    public void setDurationInDays(Integer durationInDays) {
        this.durationInDays = durationInDays;
    }

    public Integer getDurationThreshold() {
        return durationThreshold;
    }

    public void setDurationThreshold(Integer durationThreshold) {
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

    @Override
    public String toString() {
        return "TermDetails{" +
                "durationInDays=" + durationInDays +
                ", durationThreshold=" + durationThreshold +
                ", plotsDatapoint=" + plotsDatapoint +
                ", plotsDatapointDeltaInDays=" + plotsDatapointDeltaInDays +
                '}';
    }
}
