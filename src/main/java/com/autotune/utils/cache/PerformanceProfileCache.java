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

package com.autotune.utils.cache;

import com.autotune.analyzer.performanceProfiles.PerformanceProfile;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PerformanceProfileCache {
    private static final Map<String, PerformanceProfile> performanceProfileMap = new ConcurrentHashMap<>();

    private PerformanceProfileCache() {

    }

    public static boolean exists(String performanceProfileName) {
        return performanceProfileMap.containsKey(performanceProfileName);
    }

    public static PerformanceProfile get(String performanceProfileName) {
        return performanceProfileMap.get(performanceProfileName);
    }

    public static void put(String performanceProfileName, PerformanceProfile performanceProfile) {
        performanceProfileMap.put(performanceProfileName, performanceProfile);
    }

    public static void remove(String performanceProfileName) {
        performanceProfileMap.remove(performanceProfileName);
    }
}