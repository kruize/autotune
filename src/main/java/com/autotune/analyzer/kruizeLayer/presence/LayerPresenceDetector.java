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

package com.autotune.analyzer.kruizeLayer.presence;

import com.autotune.analyzer.utils.AnalyzerConstants.LayerConstants.PresenceType;

/**
 * Interface for different layer presence detection strategies
 */
public interface LayerPresenceDetector {

    /**
     * Get the type of presence detection
     * @return PresenceType enum value
     */
    PresenceType getType();

    /**
     * Detect if the layer is present in the given namespace and workload
     * @param namespace The Kubernetes namespace to check
     * @param workloadName The workload name to check (optional, can be null for namespace-level detection)
     * @return true if the layer is detected, false otherwise
     * @throws Exception if detection fails due to connectivity or other issues
     */
    boolean detectPresence(String namespace, String workloadName) throws Exception;


}
