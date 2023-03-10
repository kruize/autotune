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
package com.autotune.common.data.metrics;

import com.autotune.utils.AutotuneConstants;
import org.json.JSONObject;

public class MetricAggregationInfoResults {
    private Double avg;
    private Integer count;
    private Double max;
    private Double median;
    private Double min;
    private Double mode;
    private Double range;
    private Double sum;
    private String format;

    public MetricAggregationInfoResults() {
        avg = Double.MIN_VALUE;
        count = Integer.MIN_VALUE;
        max = Double.MIN_VALUE;
        median = Double.MIN_VALUE;
        min = Double.MIN_VALUE;
        mode = Double.MIN_VALUE;
        range = Double.MIN_VALUE;
        sum = Double.MIN_VALUE;
        format = "";
    }

    public MetricAggregationInfoResults(JSONObject jsonObject) {
        this.avg = (jsonObject.has(AutotuneConstants.JSONKeys.MEAN)) ? jsonObject.getDouble(AutotuneConstants.JSONKeys.MEAN) : Double.MIN_VALUE;
        this.count = (jsonObject.has(AutotuneConstants.JSONKeys.COUNT)) ? jsonObject.getInt(AutotuneConstants.JSONKeys.COUNT) : Integer.MIN_VALUE;
        this.max = (jsonObject.has(AutotuneConstants.JSONKeys.MAX)) ? jsonObject.getDouble(AutotuneConstants.JSONKeys.MAX) : Double.MIN_VALUE;
        this.median = (jsonObject.has(AutotuneConstants.JSONKeys.MEDIAN)) ? jsonObject.getDouble(AutotuneConstants.JSONKeys.MEDIAN) : Double.MIN_VALUE;
        this.min = (jsonObject.has(AutotuneConstants.JSONKeys.MIN)) ? jsonObject.getDouble(AutotuneConstants.JSONKeys.MIN) : Double.MIN_VALUE;
        this.mode = (jsonObject.has(AutotuneConstants.JSONKeys.MODE)) ? jsonObject.getDouble(AutotuneConstants.JSONKeys.MODE) : Double.MIN_VALUE;
        this.range = (jsonObject.has(AutotuneConstants.JSONKeys.RANGE)) ? jsonObject.getDouble(AutotuneConstants.JSONKeys.RANGE) : Double.MIN_VALUE;
        this.sum = (jsonObject.has(AutotuneConstants.JSONKeys.SUM)) ? jsonObject.getDouble(AutotuneConstants.JSONKeys.SUM) : Double.MIN_VALUE;
        this.format = (jsonObject.has(AutotuneConstants.JSONKeys.FORMAT)) ? jsonObject.getString(AutotuneConstants.JSONKeys.FORMAT) : "";
    }

    public Double getSum() {
        return sum;
    }

    public void setSum(Double sum) {
        this.sum = sum;
    }

    public Double getAvg() {
        return avg;
    }

    public void setAvg(Double avg) {
        this.avg = avg;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Double getMin() {
        return min;
    }

    public void setMin(Double min) {
        this.min = min;
    }

    public Double getMax() {
        return max;
    }

    public void setMax(Double max) {
        this.max = max;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Double getMedian() {
        return median;
    }

    public void setMedian(Double median) {
        this.median = median;
    }

    public Double getMode() {
        return mode;
    }

    public void setMode(Double mode) {
        this.mode = mode;
    }

    public Double getRange() {
        return range;
    }

    public void setRange(Double range) {
        this.range = range;
    }
    @Override
    public String toString() {
        return "MetricAggregationInfoResult{" +
                "avg=" + avg +
                ", count=" + count +
                ", max=" + max +
                ", median=" + median +
                ", min=" + min +
                ", mode=" + mode +
                ", range=" + range +
                ", sum=" + sum +
                ", format='" + format + '\'' +
                '}';
    }
}
