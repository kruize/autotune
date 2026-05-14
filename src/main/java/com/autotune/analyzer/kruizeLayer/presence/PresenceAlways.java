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
 * Implementation for layers that are always present
 */
public class PresenceAlways implements LayerPresenceDetector {

    public PresenceAlways() {}

    @Override
    public PresenceType getType() {
        return PresenceType.ALWAYS;
    }

    @Override
    public boolean detectPresence(String namespace, String containerName, String datasourceName) throws Exception {
        // Layers with ALWAYS presence type are always detected
        return true;
    }

    @Override
    public String toString() {
        return "PresenceAlways{}";
    }
}
