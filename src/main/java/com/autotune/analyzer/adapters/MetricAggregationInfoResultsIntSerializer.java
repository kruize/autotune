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
