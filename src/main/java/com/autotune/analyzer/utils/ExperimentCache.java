/*******************************************************************************
 * Copyright (c) 2022 Red Hat, IBM Corporation and others.
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

package com.autotune.analyzer.utils;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.operator.KruizeDeploymentInfo;

/**
 * Cache implementation for experiment names to avoid frequent database lookups.
 * Handles cache miss by loading from appropriate database table based on 
 * KruizeDeploymentInfo.is_ros_enabled configuration.
 */
public class ExperimentCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentCache.class);
    
    private final Set<String> experimentNamesCache = ConcurrentHashMap.newKeySet();
    
    /**
     * Check if an experiment exists in cache or database.
     *
     * Fast path:
     *   - Null check
     *   - Concurrent cache lookup
     * Slow path:
     *   - DB lookup (potentially blocking)
     *   - On positive hit, cache the experiment name
     *
     * This avoids serializing callers on a single monitor while still keeping the cache consistent.
     * 
     * @param experiment_name the experiment name to check
     * @return true if experiment exists, false otherwise
     */
    public boolean isExists(String experiment_name) {
        if (experiment_name == null) {
            return false;
        }
        if (experimentNamesCache.contains(experiment_name)) {
            LOGGER.debug("Experiment {} found in cache", experiment_name);
            return true;
        }

        // Slow-path DB lookup outside any lock to avoid serializing callers
        boolean existsInDb = checkExperimentInDatabase(experiment_name);

        // If found in DB, cache it for future calls
        if (existsInDb) {
            experimentNamesCache.add(experiment_name);
            LOGGER.debug("Experiment {} found in database, added to cache", experiment_name);
            return true;
        }

        return false;
    }
    
    /**
     * Helper method to check if experiment exists in database using existing ExperimentDBService API.
     * 
     * @param experiment_name the experiment name to check
     * @return true if experiment exists in database, false otherwise
     */
    private boolean checkExperimentInDatabase(String experiment_name) {
        Map<String, KruizeObject> experimentMap = new ConcurrentHashMap<>();
        try {
            ExperimentDBService experimentDBService = new ExperimentDBService();
            if (KruizeDeploymentInfo.is_ros_enabled) {
                // load from kruize_experiments table
                experimentDBService.loadExperimentFromDBByName(experimentMap, experiment_name);
            } else {
                // load from kruize_lm_experiments table
                experimentDBService.loadLMExperimentFromDBByName(experimentMap, experiment_name);
            }
            
            return experimentMap.containsKey(experiment_name);
        } catch (Exception e) {
            LOGGER.debug("Database check for experiment {} failed: {}", experiment_name, e.getMessage());
            return false;
        }
    }
    
    /**
     * Add an experiment name to the cache.
     * 
     * @param experiment_name the experiment name to add
     */
    public void add(String experiment_name) {
        if (experiment_name != null) {
            experimentNamesCache.add(experiment_name);
            LOGGER.debug("Added experiment {} to cache", experiment_name);
        }
    }
    
    /**
     * Remove an experiment name from the cache.
     * 
     * @param experiment_name the experiment name to remove
     */
    public void remove(String experiment_name) {
        if (experiment_name != null) {
            boolean removed = experimentNamesCache.remove(experiment_name);
            if (removed) {
                LOGGER.debug("Removed experiment {} from cache", experiment_name);
            }
        }
    }
}