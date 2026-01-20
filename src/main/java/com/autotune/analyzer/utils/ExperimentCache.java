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

import java.util.HashSet;
import java.util.Map;
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
    
    private final HashSet<String> experimentNamesCache = new HashSet<>();
    private final ExperimentDBService experimentDBService = new ExperimentDBService();
    
    /**
     * Check if an experiment exists in cache or database.
     * 
     * @param experiment_name the experiment name to check
     * @return true if experiment exists, false otherwise
     */
    public synchronized boolean isExists(String experiment_name) {
        if (experiment_name == null) {
            return false;
        }
        
        // Check cache first
        if (experimentNamesCache.contains(experiment_name)) {
            LOGGER.debug("Experiment {} found in cache", experiment_name);
            return true;
        }
        
        // Cache miss - check database
        Map<String, KruizeObject> experimentMap = new ConcurrentHashMap<>();
        try {
            if (KruizeDeploymentInfo.is_ros_enabled) {
                // load from kruize_experiments table
                experimentDBService.loadExperimentFromDBByName(experimentMap, experiment_name);
            } else {
                // load from kruize_lm_experiments table
                experimentDBService.loadLMExperimentFromDBByName(experimentMap, experiment_name);
            }
            
            // If found in database, add to cache
            if (experimentMap.containsKey(experiment_name)) {
                experimentNamesCache.add(experiment_name);
                LOGGER.debug("Experiment {} found in database, added to cache", experiment_name);
                return true;
            }
        } catch (Exception e) {
            LOGGER.debug("Database check for experiment {} failed: {}", experiment_name, e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Add an experiment name to the cache.
     * 
     * @param experiment_name the experiment name to add
     */
    public synchronized void add(String experiment_name) {
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
    public synchronized void remove(String experiment_name) {
        if (experiment_name != null) {
            boolean removed = experimentNamesCache.remove(experiment_name);
            if (removed) {
                LOGGER.debug("Removed experiment {} from cache", experiment_name);
            }
        }
    }
}