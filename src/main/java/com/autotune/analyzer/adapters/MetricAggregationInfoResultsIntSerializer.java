/*******************************************************************************
 * Copyright (c) 2026 Red Hat, IBM Corporation and others.
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
package com.autotune.analyzer.adapters;

import com.autotune.common.data.metrics.MetricAggregationInfoResults;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class MetricAggregationInfoResultsIntSerializer implements JsonSerializer<MetricAggregationInfoResults> {

    @Override
    public JsonElement serialize(MetricAggregationInfoResults metricAggregationInfoResults, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonObject = new JsonObject();
        if(metricAggregationInfoResults.getAvg() != null)
            jsonObject.addProperty("avg", metricAggregationInfoResults.getAvg().intValue());
        if(metricAggregationInfoResults.getSum() != null)
            jsonObject.addProperty("sum", metricAggregationInfoResults.getSum().intValue());
        if(metricAggregationInfoResults.getMin() != null)
            jsonObject.addProperty("min", metricAggregationInfoResults.getMin().intValue());
        if(metricAggregationInfoResults.getMax() != null)
            jsonObject.addProperty("max", metricAggregationInfoResults.getMax().intValue());
        if(metricAggregationInfoResults.getCount() != null)
            jsonObject.addProperty("count", metricAggregationInfoResults.getCount());
        if(metricAggregationInfoResults.getMedian() != null)
            jsonObject.addProperty("median", metricAggregationInfoResults.getMedian().intValue());
        if(metricAggregationInfoResults.getMode() != null)
            jsonObject.addProperty("mode", metricAggregationInfoResults.getMode().intValue());
        if(metricAggregationInfoResults.getRange() != null)
            jsonObject.addProperty("range", metricAggregationInfoResults.getRange().intValue());
        if(metricAggregationInfoResults.getFormat() != null && !metricAggregationInfoResults.getFormat().isEmpty())
            jsonObject.addProperty("format", metricAggregationInfoResults.getFormat());
        return jsonObject;
    }
}
