/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.autotune.analyzer.recommendations;

import java.util.Objects;

public class AcceleratorRecommendationItem {
    private String model;
    private String partition;
    private Integer count;
    private RecommendationConfigItem compute;
    private RecommendationConfigItem memory;

    public AcceleratorRecommendationItem(String model, String partition, Integer count, RecommendationConfigItem compute, RecommendationConfigItem memory) {
        this.model = Objects.requireNonNull(model, "model must not be null");
        this.partition = partition;
        this.count = count;
        this.compute = Objects.requireNonNull(compute, "compute must not be null");
        this.memory = Objects.requireNonNull(memory, "memory must not be null");
    }

    public String getModel() {
        return model;
    }

    public String getPartition() {
        return partition;
    }

    public Integer getCount() {
        return count;
    }

    public RecommendationConfigItem getCompute() {
        return compute;
    }

    public RecommendationConfigItem getMemory() {
        return memory;
    }
}
