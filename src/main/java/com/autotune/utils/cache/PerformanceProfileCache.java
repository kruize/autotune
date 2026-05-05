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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class PerformanceProfileCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceProfileCache.class);
    private static final Map<String, PerformanceProfile> performanceProfileMap = new ConcurrentHashMap<>();
    private static final Map<String, Object> refreshLocks = new ConcurrentHashMap<>();

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

    /**
     * Atomically refresh a performance profile from the database.
     * Only one thread will reload a given profile at a time; other threads will block until the refresh completes.
     *
     * @param profileName the name of the profile to refresh
     * @param loader a Supplier that loads the profile and returns it (or null if not found)
     * @return the refreshed PerformanceProfile, or null if not found
     */
    public static PerformanceProfile refresh(String profileName, Supplier<PerformanceProfile> loader) {
        // Get or create a lock object for this specific profile name
        Object lock = refreshLocks.computeIfAbsent(profileName, k -> new Object());
        
        synchronized (lock) {
            try {
                LOGGER.debug("Refreshing performance profile from DB: {}", profileName);
                
                // Remove the stale entry from cache
                performanceProfileMap.remove(profileName);
                
                // Load the fresh profile from DB
                PerformanceProfile refreshedProfile = loader.get();
                
                // Update cache with the fresh profile (if found)
                if (refreshedProfile != null) {
                    performanceProfileMap.put(profileName, refreshedProfile);
                    LOGGER.debug("Successfully refreshed performance profile: {}", profileName);
                } else {
                    LOGGER.debug("Performance profile not found in DB: {}", profileName);
                }
                
                return refreshedProfile;
            } finally {
                // Clean up the lock object to prevent memory leak
                refreshLocks.remove(profileName);
            }
        }
    }
}