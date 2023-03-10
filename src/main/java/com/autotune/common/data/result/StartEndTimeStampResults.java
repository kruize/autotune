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

import com.autotune.common.data.metrics.MetricAggregationInfoResults;
import com.autotune.utils.AnalyzerConstants;

import java.sql.Timestamp;
import java.util.HashMap;

/**
 * Raw results are segregated and organized using  StartEndTimeStampResults
 */
public class StartEndTimeStampResults {
    HashMap<AnalyzerConstants.AggregatorType, MetricAggregationInfoResults> metrics;
    private Timestamp startTimeStamp;
    private Timestamp endTimeStamp;
    private Double durationInMinutes;

    public StartEndTimeStampResults(Timestamp startTimeStamp, Timestamp endTimeStamp) {
        this.startTimeStamp = startTimeStamp;
        this.endTimeStamp = endTimeStamp;
        this.durationInMinutes = Double.valueOf((endTimeStamp.getTime() - startTimeStamp.getTime()) / (60 * 1000));
    }

    public Timestamp getStartTimeStamp() {
        return startTimeStamp;
    }

    public void setStartTimeStamp(Timestamp startTimeStamp) {
        this.startTimeStamp = startTimeStamp;
    }


    public Timestamp getEndTimeStamp() {
        return endTimeStamp;
    }

    public void setEndTimeStamp(Timestamp endTimeStamp) {
        this.endTimeStamp = endTimeStamp;
    }

    public Double getDurationInMinutes() {
        return durationInMinutes;
    }

    public void setDurationInMinutes(Double durationInMinutes) {
        this.durationInMinutes = durationInMinutes;
    }

    public HashMap<AnalyzerConstants.AggregatorType, MetricAggregationInfoResults> getMetrics() {
        return metrics;
    }

    public void setMetrics(HashMap<AnalyzerConstants.AggregatorType, MetricAggregationInfoResults> metrics) {
        this.metrics = metrics;
    }

    @Override
    public String toString() {
        return "StartEndTimeStampResults{" +
                "startTimeStamp=" + startTimeStamp +
                ", endTimeStamp=" + endTimeStamp +
                ", durationInMinutes=" + durationInMinutes +
                ", metrics=" + metrics +
                '}';
    }
    
}
