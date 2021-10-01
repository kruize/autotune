package com.autotune.analyzer.experiments;

import static com.autotune.analyzer.utils.AnalyzerConstants.AutotuneConfigConstants.BOUND_CHARS;
import static com.autotune.analyzer.utils.AnalyzerConstants.AutotuneConfigConstants.BOUND_DIGITS;

public class Resources {
    Double cpuValue;
    String cpuUnits;
    Double memoryValue;
    String memoryUnits;

    public Resources(String cpu, String memory) {
        /* Parse the value for the bounds from the strings passed in */
        Double cpuValue = Double.parseDouble(BOUND_CHARS.matcher(cpu).replaceAll(""));
        Double memoryValue = Double.parseDouble(BOUND_CHARS.matcher(memory).replaceAll(""));

        /* Parse the bound units from the strings passed in and make sure they are the same */
        String cpuUnits = BOUND_DIGITS.matcher(cpu).replaceAll("");
        String memoryUnits = BOUND_DIGITS.matcher(memory).replaceAll("");

        this.cpuValue = cpuValue;
        this.memoryValue = memoryValue;
        this.cpuUnits = cpuUnits;
        this.memoryUnits = memoryUnits;
    }

    public String getCpu() {
        return cpuValue + cpuUnits;
    }

    public String getMemory() {
        return memoryValue + memoryUnits;
    }

    public Double getCpuValue() {
        return cpuValue;
    }

    public Double getMemoryValue() {
        return memoryValue;
    }

    public String getCpuUnits() { return cpuUnits; }

    public String getMemoryUnits() {
        return memoryUnits;
    }
}