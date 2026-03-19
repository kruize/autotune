package com.autotune.common.data.metrics;

import com.autotune.utils.KruizeConstants;
import software.amazon.awssdk.utils.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;

public class MetricDataPoint {
    private String metricName;
    private Timestamp startTime;
    private  Timestamp endTime;
    private  MetricAggregationInfoResults aggregationInfo = new MetricAggregationInfoResults();

    public MetricDataPoint(String metricName) {
        this.metricName = metricName;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }

    public MetricAggregationInfoResults getAggregationInfo() {
        return aggregationInfo;
    }

    public void setMetricAggregationInfo(String function, Double value) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        Method method = MetricAggregationInfoResults.class.getDeclaredMethod(KruizeConstants.APIMessages.SET + StringUtils.capitalize(function), Double.class);
        method.setAccessible(true);
        method.invoke(aggregationInfo, value);
    }

    public void setMetricAggregationInfo(String function, Integer value) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        Method method = MetricAggregationInfoResults.class.getDeclaredMethod(KruizeConstants.APIMessages.SET + StringUtils.capitalize(function), Integer.class);
        method.setAccessible(true);
        method.invoke(aggregationInfo, value);
    }
}
