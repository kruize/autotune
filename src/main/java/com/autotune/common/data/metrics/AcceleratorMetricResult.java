package com.autotune.common.data.metrics;

import com.autotune.common.data.system.info.device.accelerator.AcceleratorDeviceData;

public class AcceleratorMetricResult {
    private AcceleratorDeviceData acceleratorDeviceData;
    private MetricResults metricResults;

    public AcceleratorMetricResult(AcceleratorDeviceData acceleratorDeviceData, MetricResults metricResults) {
        this.acceleratorDeviceData = acceleratorDeviceData;
        this.metricResults = metricResults;
    }

    public AcceleratorDeviceData getAcceleratorDeviceData() {
        return acceleratorDeviceData;
    }

    public void setAcceleratorDeviceData(AcceleratorDeviceData acceleratorDeviceData) {
        this.acceleratorDeviceData = acceleratorDeviceData;
    }

    public MetricResults getMetricResults() {
        return metricResults;
    }

    public void setMetricResults(MetricResults metricResults) {
        this.metricResults = metricResults;
    }
}
