package com.autotune.analyzer.recommendations.model;

public class RecommendationTunables {

    public double memoryPercentile;
    public double cpuPercentile;
    public double acceleratorPercentile;

    public RecommendationTunables(double CPU_PERCENTILE, double MEMORY_PERCENTILE, double ACCELERATOR_PERCENTILE) {
        this.cpuPercentile = CPU_PERCENTILE;
        this.memoryPercentile = MEMORY_PERCENTILE;
        this.acceleratorPercentile = ACCELERATOR_PERCENTILE;
    }

    public double getMemoryPercentile() {
        return memoryPercentile;
    }

    public void setMemoryPercentile(double memoryPercentile) {
        this.memoryPercentile = memoryPercentile;
    }

    public double getCpuPercentile() {
        return cpuPercentile;
    }

    public void setCpuPercentile(double cpuPercentile) {
        this.cpuPercentile = cpuPercentile;
    }

    public double getAcceleratorPercentile() {
        return acceleratorPercentile;
    }

    public void setAcceleratorPercentile(double acceleratorPercentile) {
        this.acceleratorPercentile = acceleratorPercentile;
    }
}
