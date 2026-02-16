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

package com.autotune.analyzer.kruizeLayer.recommendations.impl;

import com.autotune.analyzer.kruizeLayer.recommendations.LayerRecommendationContext;
import com.autotune.analyzer.kruizeLayer.recommendations.LayerRecommendationHandler;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.utils.KruizeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Recommendation handler for the Hotspot (OpenJDK) JVM layer.
 * Produces GC policy and MaxRAMPercentage recommendations.
 */
public class HotspotLayerRecommendationHandler implements LayerRecommendationHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(HotspotLayerRecommendationHandler.class);

    private static final HotspotLayerRecommendationHandler INSTANCE = new HotspotLayerRecommendationHandler();

    public static HotspotLayerRecommendationHandler getInstance() {
        return INSTANCE;
    }

    private HotspotLayerRecommendationHandler() {
    }

    @Override
    public String getLayerName() {
        return AnalyzerConstants.AutotuneConfigConstants.LAYER_HOTSPOT;
    }

    @Override
    public Object getRecommendation(String tunableName, LayerRecommendationContext context) {
        if (!AnalyzerConstants.AutotuneConfigConstants.LAYER_HOTSPOT.equalsIgnoreCase(context.getEffectiveLayer())) {
            LOGGER.debug("Hotspot handler: effectiveLayer '{}' does not match layerName 'hotspot'", context.getEffectiveLayer());
            return null;
        }

        if (AnalyzerConstants.MetricNameConstants.MAX_RAM_PERCENTAGE.equals(tunableName)) {
            return AnalyzerConstants.HotspotConstants.MAX_RAM_PERCENTAGE_VALUE;
        }

        if (AnalyzerConstants.MetricNameConstants.GC_POLICY.equals(tunableName)) {
            String jdkVersion = context.getJdkVersion();
            if (jdkVersion == null || jdkVersion.isEmpty()) {
                LOGGER.warn("JVM version is null or empty (layerName=hotspot)");
                return null;
            }
            Double jvmHeapSizeMB = null;
            double maxRamPercentage = AnalyzerConstants.HotspotConstants.MAX_RAM_PERCENTAGE_VALUE;
            return decideGCPolicy(jvmHeapSizeMB, maxRamPercentage, context.getMemLimit(), context.getCpuLimit(), jdkVersion);
        }

        return null;
    }

    @Override
    public void formatForEnv(String tunableName, Object value, Map<String, StringBuilder> envBuilders) {
        formatForJVMEnv(tunableName, value, envBuilders);
    }

    static void formatForJVMEnv(String tunableName, Object value, Map<String, StringBuilder> envBuilders) {
        if (value == null) return;

        StringBuilder jdkOpts = envBuilders.get(KruizeConstants.JSONKeys.JDK_JAVA_OPTIONS);
        StringBuilder javaOpts = envBuilders.get(KruizeConstants.JSONKeys.JAVA_OPTIONS);
        StringBuilder target = (jdkOpts != null) ? jdkOpts : javaOpts;
        if (target == null) return;

        if (AnalyzerConstants.MetricNameConstants.MAX_RAM_PERCENTAGE.equals(tunableName)) {
            target.append("-XX:MaxRAMPercentage=").append(value).append(" ");
        } else if (AnalyzerConstants.MetricNameConstants.GC_POLICY.equals(tunableName)) {
            target.append(value).append(" ");
        }
    }

    private String decideGCPolicy(Double jvmHeapSizeMB, double maxRAMPercent, double memLimit, double cpuCores, String jdkVersionStr) {
        int jdkVersion = parseMajorVersion(jdkVersionStr);

        if (jvmHeapSizeMB == null || jvmHeapSizeMB == 0) {
            double memLimitMB = memLimit / (1024 * 1024);
            jvmHeapSizeMB = Math.ceil((maxRAMPercent / 100) * memLimitMB);
        }

        if (cpuCores <= 1 && jvmHeapSizeMB < 4096) {
            return "-XX:+UseSerialGC";
        } else if (cpuCores > 1 && jvmHeapSizeMB < 4096) {
            return "-XX:+UseParallelGC";
        } else if (jvmHeapSizeMB >= 4096) {
            if (jdkVersion >= 17) {
                return "-XX:+UseZGC";
            } else if (jdkVersion >= 11) {
                return "-XX:+UseShenandoahGC";
            } else {
                return "-XX:+UseG1GC";
            }
        } else {
            return "-XX:+UseG1GC";
        }
    }

    private static int parseMajorVersion(String version) {
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
}
