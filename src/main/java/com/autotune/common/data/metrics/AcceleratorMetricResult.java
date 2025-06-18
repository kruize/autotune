package com.autotune.common.data.metrics;

import com.autotune.common.data.system.info.device.accelerator.NvidiaAcceleratorDeviceData;

public class AcceleratorMetricResult {
    private NvidiaAcceleratorDeviceData acceleratorDeviceData;
    private MetricResults metricResults;

    public AcceleratorMetricResult(NvidiaAcceleratorDeviceData acceleratorDeviceData, MetricResults metricResults) {
        this.acceleratorDeviceData = acceleratorDeviceData;
        this.metricResults = metricResults;
    }

    public NvidiaAcceleratorDeviceData getAcceleratorDeviceData() {
        return acceleratorDeviceData;
    }

    public void setAcceleratorDeviceData(NvidiaAcceleratorDeviceData acceleratorDeviceData) {
        this.acceleratorDeviceData = acceleratorDeviceData;
    }

    public MetricResults getMetricResults() {
        return metricResults;
    }

    public void setMetricResults(MetricResults metricResults) {
        this.metricResults = metricResults;
    }
}
