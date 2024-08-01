package com.autotune.common.data.result;

import com.autotune.common.data.metrics.MetricResults;

public class GpuMetricResult {
    private GPUDeviceData gpuDeviceData;
    private MetricResults metricResults;

    public GpuMetricResult(GPUDeviceData gpuDeviceData, MetricResults metricResults) {
        this.gpuDeviceData = gpuDeviceData;
        this.metricResults = metricResults;
    }

    public GPUDeviceData getGpuDeviceData() {
        return gpuDeviceData;
    }

    public void setGpuDeviceData(GPUDeviceData gpuDeviceData) {
        this.gpuDeviceData = gpuDeviceData;
    }

    public MetricResults getMetricResults() {
        return metricResults;
    }

    public void setMetricResults(MetricResults metricResults) {
        this.metricResults = metricResults;
    }
}