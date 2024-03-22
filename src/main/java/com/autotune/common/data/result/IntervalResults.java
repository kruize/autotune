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

import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.metrics.MetricResults;
import com.google.gson.annotations.SerializedName;

import java.sql.Timestamp;
import java.util.HashMap;

import static com.autotune.utils.KruizeConstants.JSONKeys.*;
import static com.autotune.utils.KruizeConstants.TimeConv.NO_OF_MSECS_IN_SEC;
import static com.autotune.utils.KruizeConstants.TimeConv.NO_OF_SECONDS_PER_MINUTE;

/**
 * Raw results are segregated and organized using IntervalResults
 */
public class IntervalResults {
    @SerializedName(METRICS)
    HashMap<AnalyzerConstants.MetricName, MetricResults> metricResultsMap;
    @SerializedName(INTERVAL_START_TIME)
    private Timestamp intervalStartTime;
    @SerializedName(INTERVAL_END_TIME)
    private Timestamp intervalEndTime;
    @SerializedName(DURATION_IN_MINUTES)
    private Double durationInMinutes;
    private Double duration_in_seconds;

    public IntervalResults(Timestamp intervalStartTime, Timestamp intervalEndTime) {
        this.intervalStartTime = intervalStartTime;
        this.intervalEndTime = intervalEndTime;
        this.duration_in_seconds = Double.valueOf((intervalEndTime.getTime() - intervalStartTime.getTime()) / NO_OF_MSECS_IN_SEC);
        this.durationInMinutes = Double.valueOf(intervalEndTime.getTime() - intervalStartTime.getTime()) / (NO_OF_MSECS_IN_SEC * NO_OF_SECONDS_PER_MINUTE);
    }
    public IntervalResults() {
    }

    public Double getDurationInMinutes() {
        return durationInMinutes;
    }

    public Double getDuration_in_seconds() {
        return duration_in_seconds;
    }

    public HashMap<AnalyzerConstants.MetricName, MetricResults> getMetricResultsMap() {
        return metricResultsMap;
    }

    public void setMetricResultsMap(HashMap<AnalyzerConstants.MetricName, MetricResults> metricResultsMap) {
        this.metricResultsMap = metricResultsMap;
    }

    public Timestamp getIntervalStartTime() {
        return intervalStartTime;
    }

    public Timestamp getIntervalEndTime() {
        return intervalEndTime;
    }

    @Override
    public String toString() {
        return "IntervalResults{" +
                "metricResultsMap=" + metricResultsMap +
                ", intervalStartTime=" + intervalStartTime +
                ", intervalEndTime=" + intervalEndTime +
                ", durationInMinutes=" + durationInMinutes +
                ", durationInSeconds=" + duration_in_seconds +
                '}';
    }
}
