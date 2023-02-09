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


import com.google.gson.annotations.SerializedName;

public class Results {
    @SerializedName("aggregation_info")
    private AggregationInfoResult aggregation_info;
    private Double value;
    private String units;

    public AggregationInfoResult getAggregation_info() {
        return aggregation_info;
    }

    public void setAggregation_info(AggregationInfoResult aggregation_info) {
        this.aggregation_info = aggregation_info;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    @Override
    public String toString() {
        return "ContainersResultData{" +
                "aggregation_info=" + aggregation_info +
                ", value=" + value +
                ", units='" + units + '\'' +
                '}';
    }
}
