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
package com.autotune.analyzer.kruizeObject;

import com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationScope;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Maps the "resource_scope_settings" object under "recommendation_settings".
 * Each key lists the provisioning states of that resource for which recommendations are emitted.
 * An absent key means the resource is unconstrained.
 */
public class ResourceScopeSettings {
    @SerializedName(KruizeConstants.JSONKeys.CPU)
    private List<String> cpu;

    @SerializedName(KruizeConstants.JSONKeys.MEMORY)
    private List<String> memory;

    // Gson does not populate or serialize transient fields; call parse() after deserialization.
    private transient EnumSet<RecommendationScope> parsedCpuScopes = null;
    private transient EnumSet<RecommendationScope> parsedMemoryScopes = null;

    public ResourceScopeSettings() {
    }

    public ResourceScopeSettings(List<String> cpu, List<String> memory) {
        this.cpu = cpu;
        this.memory = memory;
    }

    public List<String> getCpu() {
        return cpu;
    }

    public List<String> getMemory() {
        return memory;
    }

    /**
     * Eagerly parses both scope lists into EnumSets.
     * Memory scopes are checked against the scopes supported for memory, so the CPU-only 'idle' is never stored.
     * Gson bypasses setters, so this must be called explicitly after deserialization.
     */
    public void parse() {
        this.parsedCpuScopes = RecommendationScope.parseScopes(cpu, RecommendationScope.CPU_SCOPES);
        this.parsedMemoryScopes = RecommendationScope.parseScopes(memory, RecommendationScope.MEMORY_SCOPES);
    }

    /**
     * @return null if the "cpu" key was absent or empty, meaning CPU is unconstrained.
     */
    public EnumSet<RecommendationScope> getParsedCpuScopes() {
        return parsedCpuScopes;
    }

    /**
     * @return null if the "memory" key was absent or empty, meaning memory is unconstrained.
     */
    public EnumSet<RecommendationScope> getParsedMemoryScopes() {
        return parsedMemoryScopes;
    }

    @Override
    public String toString() {
        return "ResourceScopeSettings{" +
                "cpu=" + sanitize(cpu) +
                ", memory=" + sanitize(memory) +
                '}';
    }

    /**
     * Scope values come straight from the request, so escape control characters (e.g. CR/LF)
     * before they are rendered in logs, to prevent log forging.
     */
    private static List<String> sanitize(List<String> scopes) {
        if (scopes == null) {
            return null;
        }
        List<String> sanitized = new ArrayList<>(scopes.size());
        for (String scope : scopes) {
            if (scope == null) {
                sanitized.add(null);
                continue;
            }
            StringBuilder sb = new StringBuilder(scope.length());
            for (char c : scope.toCharArray()) {
                if (Character.isISOControl(c)) {
                    sb.append(String.format("\\u%04x", (int) c));
                } else {
                    sb.append(c);
                }
            }
            sanitized.add(sb.toString());
        }
        return sanitized;
    }
}
