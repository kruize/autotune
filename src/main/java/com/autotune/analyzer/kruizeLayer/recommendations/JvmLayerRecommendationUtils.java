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

package com.autotune.analyzer.kruizeLayer.recommendations;

import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.utils.KruizeConstants;

import java.util.Map;

/**
 * Shared utilities for JVM runtime layer recommendation handlers (Hotspot, Semeru, OpenJ9, etc.).
 * Provides common logic for version parsing and JVM option formatting.
 */
public final class JvmLayerRecommendationUtils {

    private JvmLayerRecommendationUtils() {
    }

    /**
     * Parses JDK version string to major version number.
     * Handles both legacy ("1.8", "1.11") and new ("17", "21") formats.
     *
     * @param version version string (e.g. "1.8", "17.0.1")
     * @return major version number; 8 if version is null or empty
     */
    public static int parseMajorVersion(String version) {
        if (version == null || version.isEmpty()) return 8;
        version = version.trim();
        if (version.startsWith("1.")) {
            return Integer.parseInt(version.substring(2, 3));
        }
        int dotIndex = version.indexOf(".");
        return (dotIndex != -1)
                ? Integer.parseInt(version.substring(0, dotIndex))
                : Integer.parseInt(version);
    }

    /**
     * Formats tunable values for JVM environment variables (JDK_JAVA_OPTIONS / JAVA_OPTIONS).
     * Shared by Hotspot, Semeru, and any future JVM runtime layer handlers.
     *
     * @param tunableName  tunable name (e.g. MaxRAMPercentage, GCPolicy)
     * @param value       recommended value
     * @param envBuilders map of env var name to StringBuilder
     */
    public static void formatForJVMEnv(String tunableName, Object value, Map<String, StringBuilder> envBuilders) {
        if (value == null) return;

        StringBuilder jdkOpts = envBuilders.get(KruizeConstants.JSONKeys.JDK_JAVA_OPTIONS);
        StringBuilder javaOpts = envBuilders.get(KruizeConstants.JSONKeys.JAVA_OPTIONS);
        StringBuilder target = (jdkOpts != null) ? jdkOpts : javaOpts;
        if (target == null) return;

        if (AnalyzerConstants.LayerConstants.TunablesConstants.MAX_RAM_PERC.equals(tunableName)) {
            target.append("-XX:MaxRAMPercentage=").append(value).append(" ");
        } else if (AnalyzerConstants.LayerConstants.TunablesConstants.GC_POLICY.equals(tunableName)) {
            target.append(value).append(" ");
        }
    }
}
