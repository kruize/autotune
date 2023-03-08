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

import com.autotune.utils.AutotuneConstants;
import org.json.JSONObject;

public class AggregationInfoResult {
    private Double sum;
    private Double avg;
    private Double min;
    private Double max;
    private String format;

    public AggregationInfoResult() {
        sum = Double.MIN_VALUE;
        avg = Double.MIN_VALUE;
        min = Double.MIN_VALUE;
        max = Double.MIN_VALUE;
        format = "";
    }

    public AggregationInfoResult(JSONObject jsonObject) {
        this.sum = (jsonObject.has(AutotuneConstants.JSONKeys.SUM)) ? jsonObject.getDouble(AutotuneConstants.JSONKeys.SUM) : Double.MIN_VALUE;
        this.avg = (jsonObject.has(AutotuneConstants.JSONKeys.MEAN)) ? jsonObject.getDouble(AutotuneConstants.JSONKeys.MEAN) : Double.MIN_VALUE;
        this.max = (jsonObject.has(AutotuneConstants.JSONKeys.MAX)) ? jsonObject.getDouble(AutotuneConstants.JSONKeys.MAX) : Double.MIN_VALUE;
        this.min = (jsonObject.has(AutotuneConstants.JSONKeys.MIN)) ? jsonObject.getDouble(AutotuneConstants.JSONKeys.MIN) : Double.MIN_VALUE;
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

    @Override
    public String toString() {
        return "AggregationInfoResult{" +
                "sum=" + sum +
                ", avg=" + avg +
                ", min=" + min +
                ", max=" + max +
                ", format='" + format + '\'' +
                '}';
    }
}
