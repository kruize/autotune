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
package com.autotune.analyzer.kruizeObject;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Represents the nested structure for recommendation_types configuration.
 * Allows granular control over which specific resource types and runtime layers to enable.
 * 
 * Example JSON:
 * {
 *   "resources": ["cpu", "memory"],
 *   "runtimes": ["hotspot", "quarkus"]
 * }
 */
public class RecommendationTypesConfig {
    
    @SerializedName("resources")
    private List<String> resources;
    
    @SerializedName("runtimes")
    private List<String> runtimes;
    
    @SerializedName("accelerators")
    private List<String> accelerators;

    public RecommendationTypesConfig() {
    }

    public List<String> getResources() {
        return resources;
    }

    public void setResources(List<String> resources) {
        this.resources = resources;
    }

    public List<String> getRuntimes() {
        return runtimes;
    }

    public void setRuntimes(List<String> runtimes) {
        this.runtimes = runtimes;
    }

    public List<String> getAccelerators() {
        return accelerators;
    }

    public void setAccelerators(List<String> accelerators) {
        this.accelerators = accelerators;
    }

    /**
     * Checks if a specific resource type is enabled.
     * @param resourceType The resource type (e.g., "cpu", "memory")
     * @return true if enabled or if resources list is null/empty (default: all enabled)
     */
    public boolean isResourceEnabled(String resourceType) {
        if (resourceType == null || resourceType.isEmpty()) {
            return false;
        }
        if (resources == null || resources.isEmpty()) {
            return true; // Default: all resources enabled
        }
        return resources.stream()
                .anyMatch(r -> r != null && r.equalsIgnoreCase(resourceType));
    }

    /**
     * Checks if a specific runtime layer is enabled.
     * @param runtimeLayer The runtime layer (e.g., "hotspot", "quarkus", "semeru")
     * @return true if enabled or if runtimes list is null/empty (default: all enabled)
     */
    public boolean isRuntimeEnabled(String runtimeLayer) {
        if (runtimeLayer == null || runtimeLayer.isEmpty()) {
            return false;
        }
        if (runtimes == null || runtimes.isEmpty()) {
            return true; // Default: all runtimes enabled
        }
        return runtimes.stream()
                .anyMatch(r -> r != null && r.equalsIgnoreCase(runtimeLayer));
    }

    /**
     * Checks if a specific accelerator type is enabled.
     * @param acceleratorType The accelerator type (e.g., "gpu")
     * @return true if enabled or if accelerators list is null/empty (default: all enabled)
     */
    public boolean isAcceleratorEnabled(String acceleratorType) {
        if (acceleratorType == null || acceleratorType.isEmpty()) {
            return false;
        }
        if (accelerators == null || accelerators.isEmpty()) {
            return true; // Default: all accelerators enabled
        }
        return accelerators.stream()
                .anyMatch(a -> a != null && a.equalsIgnoreCase(acceleratorType));
    }

    /**
     * Checks if any resource recommendations are enabled.
     * @return true if resources list is null, empty, or contains at least one resource type
     */
    public boolean hasResourcesEnabled() {
        return resources == null || resources.isEmpty() || !resources.isEmpty();
    }

    /**
     * Checks if any runtime recommendations are enabled.
     * @return true if runtimes list is null, empty, or contains at least one runtime layer
     */
    public boolean hasRuntimesEnabled() {
        return runtimes == null || runtimes.isEmpty() || !runtimes.isEmpty();
    }

    /**
     * Checks if any accelerator recommendations are enabled.
     * @return true if accelerators list is null, empty, or contains at least one accelerator type
     */
    public boolean hasAcceleratorsEnabled() {
        return accelerators == null || accelerators.isEmpty() || !accelerators.isEmpty();
    }

    @Override
    public String toString() {
        return "RecommendationTypesConfig{" +
                "resources=" + resources +
                ", runtimes=" + runtimes +
                ", accelerators=" + accelerators +
                '}';
    }
}

// Made with Bob
