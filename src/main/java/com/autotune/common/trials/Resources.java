/*******************************************************************************
 * Copyright (c) 2021, 2021 Red Hat, IBM Corporation and others.
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
package com.autotune.common.trials;

import static com.autotune.analyzer.utils.AnalyzerConstants.AutotuneConfigConstants.BOUND_CHARS;
import static com.autotune.analyzer.utils.AnalyzerConstants.AutotuneConfigConstants.BOUND_DIGITS;

/**
 *
 */
public class Resources {
    private Double cpuValue;
    private String cpuUnits;
    private Double memoryValue;
    private String memoryUnits;

    public Resources(String cpu, String memory) {
        // Parse the value for the bounds from the strings passed in
        setCpu(cpu);
        setMemory(memory);
    }

    public String getCpu() {
        return String.format("%.2f", cpuValue) + cpuUnits;
    }

    public String getMemory() {
        return String.format("%.2f", memoryValue) + memoryUnits;
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

    public void setCpu(String cpu) {
        if (cpu != null && !cpu.isEmpty()) {
            Double cpuValue = Double.parseDouble(BOUND_CHARS.matcher(cpu).replaceAll(""));
            String cpuUnits = BOUND_DIGITS.matcher(cpu).replaceAll("");
            this.cpuValue = cpuValue;
            this.cpuUnits = cpuUnits;
        }
    }

    public void setMemory(String memory) {
        if (memory != null && !memory.isEmpty()) {
            Double memoryValue = Double.parseDouble(BOUND_CHARS.matcher(memory).replaceAll(""));
            /* Parse the bound units from the strings passed in and make sure they are the same */
            String memoryUnits = BOUND_DIGITS.matcher(memory).replaceAll("");
            this.memoryValue = memoryValue;
            this.memoryUnits = memoryUnits;
        }
    }
}
