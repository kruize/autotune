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

/**
 * Raw results are segregated and organized using IntervalResults
 */
public class IntervalResults {
    @SerializedName("metrics")
    HashMap<AnalyzerConstants.MetricName, MetricResults> metricResultsMap;
    private Timestamp intervalStart;
    private Timestamp intervalEnd;
    private Double durationInMinutes;

    public IntervalResults(Timestamp intervalStart, Timestamp intervalEnd) {
        this.intervalStart = intervalStart;
        this.intervalEnd = intervalEnd;
        this.durationInMinutes = Double.valueOf((intervalEnd.getTime() - intervalStart.getTime()) / (60 * 1000));
    }

    public Timestamp getIntervalStart() {
        return intervalStart;
    }

    public void setIntervalStart(Timestamp intervalStart) {
        this.intervalStart = intervalStart;
    }


    public Timestamp getIntervalEnd() {
        return intervalEnd;
    }

    public void setIntervalEnd(Timestamp intervalEnd) {
        this.intervalEnd = intervalEnd;
    }

    public Double getDurationInMinutes() {
        return durationInMinutes;
    }

    public void setDurationInMinutes(Double durationInMinutes) {
        this.durationInMinutes = durationInMinutes;
    }

    public HashMap<AnalyzerConstants.MetricName, MetricResults> getMetricResultsMap() {
        return metricResultsMap;
    }

    public void setMetricResultsMap(HashMap<AnalyzerConstants.MetricName, MetricResults> metricResultsMap) {
        this.metricResultsMap = metricResultsMap;
    }

    @Override
    public String toString() {
        return "IntervalResults{" +
                "metricResultsMap=" + metricResultsMap +
                ", intervalStart=" + intervalStart +
                ", intervalEnd=" + intervalEnd +
                ", durationInMinutes=" + durationInMinutes +
                '}';
    }
}
