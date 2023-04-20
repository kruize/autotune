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

import com.autotune.utils.KruizeConstants;
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
    }

    public MetricAggregationInfoResults(JSONObject jsonObject) {
        this.avg = (jsonObject.has(KruizeConstants.JSONKeys.MEAN)) ? jsonObject.getDouble(KruizeConstants.JSONKeys.MEAN) : null;
        this.count = (jsonObject.has(KruizeConstants.JSONKeys.COUNT)) ? jsonObject.getInt(KruizeConstants.JSONKeys.COUNT) : null;
        this.max = (jsonObject.has(KruizeConstants.JSONKeys.MAX)) ? jsonObject.getDouble(KruizeConstants.JSONKeys.MAX) : null;
        this.median = (jsonObject.has(KruizeConstants.JSONKeys.MEDIAN)) ? jsonObject.getDouble(KruizeConstants.JSONKeys.MEDIAN) : null;
        this.min = (jsonObject.has(KruizeConstants.JSONKeys.MIN)) ? jsonObject.getDouble(KruizeConstants.JSONKeys.MIN) : null;
        this.mode = (jsonObject.has(KruizeConstants.JSONKeys.MODE)) ? jsonObject.getDouble(KruizeConstants.JSONKeys.MODE) : null;
        this.range = (jsonObject.has(KruizeConstants.JSONKeys.RANGE)) ? jsonObject.getDouble(KruizeConstants.JSONKeys.RANGE) : null;
        this.sum = (jsonObject.has(KruizeConstants.JSONKeys.SUM)) ? jsonObject.getDouble(KruizeConstants.JSONKeys.SUM) : null;
        this.format = (jsonObject.has(KruizeConstants.JSONKeys.FORMAT)) ? jsonObject.getString(KruizeConstants.JSONKeys.FORMAT) : "";
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
