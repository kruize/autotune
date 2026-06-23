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

package com.autotune.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for cluster name validation and parsing operations.
 * Centralizes DNS-1123 subdomain validation logic used across the application.
 */
public class ClusterNameUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterNameUtils.class);
    
    // DNS-1123 subdomain regex: lowercase alphanumeric, hyphens, dots
    // Cannot start/end with hyphen, max 253 characters
    private static final String DNS_1123_REGEX = "^[a-z0-9]([-a-z0-9]*[a-z0-9])?(\\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)*$";
    private static final int MAX_CLUSTER_NAME_LENGTH = 253;
    
    /**
     * Validates a cluster name according to Kubernetes DNS-1123 subdomain rules.
     * 
     * Rules:
     * - Lowercase alphanumeric characters, hyphens, and dots only
     * - Cannot start or end with a hyphen
     * - Maximum 253 characters
     * - Cannot be empty (after trimming)
     * 
     * @param clusterName The cluster name to validate (can be null)
     * @return Empty string if valid, error message if invalid
     */
    public static String validateClusterName(String clusterName) {
        // Null is acceptable (optional field)
        if (clusterName == null) {
            return "";
        }
        
        // Trim whitespace
        clusterName = clusterName.trim();
        
        // Check if empty after trimming
        if (clusterName.isEmpty()) {
            return "cluster_name cannot be an empty string";
        }
        
        // Check length
        if (clusterName.length() > MAX_CLUSTER_NAME_LENGTH) {
            return String.format("cluster_name is too long (max %d characters, got %d)", 
                               MAX_CLUSTER_NAME_LENGTH, clusterName.length());
        }
        
        // Validate DNS-1123 subdomain format
        if (!clusterName.matches(DNS_1123_REGEX)) {
            return "Invalid cluster_name format. Must be lowercase alphanumeric characters, " +
                   "hyphens, and dots only. Cannot start or end with hyphen.";
        }
        
        return ""; // Valid
    }
    
    /**
     * Checks if a cluster name is valid according to DNS-1123 rules.
     * 
     * @param clusterName The cluster name to check
     * @return true if valid, false otherwise
     */
    public static boolean isValidClusterName(String clusterName) {
        return validateClusterName(clusterName).isEmpty();
    }
    
    /**
     * Parses a comma-separated string of cluster names into a validated list.
     * Invalid cluster names are logged and skipped.
     * 
     * @param clustersString Comma-separated cluster names (can be null or empty)
     * @return List of valid cluster names (empty list if input is null/empty)
     */
    public static List<String> parseClusterList(String clustersString) {
        if (clustersString == null || clustersString.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        return Arrays.stream(clustersString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(clusterName -> {
                    String validationError = validateClusterName(clusterName);
                    if (!validationError.isEmpty()) {
                        LOGGER.warn("Skipping invalid cluster name '{}': {}", clusterName, validationError);
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Validates and filters a list of cluster names.
     * Invalid cluster names are logged and removed from the result.
     * 
     * @param clusterList List of cluster names to validate (can be null)
     * @return List of valid cluster names (empty list if input is null)
     */
    public static List<String> validateAndFilterClusterList(List<String> clusterList) {
        if (clusterList == null || clusterList.isEmpty()) {
            return new ArrayList<>();
        }
        
        return clusterList.stream()
                .filter(clusterName -> clusterName != null && !clusterName.trim().isEmpty())
                .map(String::trim)
                .filter(clusterName -> {
                    String validationError = validateClusterName(clusterName);
                    if (!validationError.isEmpty()) {
                        LOGGER.warn("Skipping invalid cluster name '{}': {}", clusterName, validationError);
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Converts a list of cluster names to a comma-separated string.
     * 
     * @param clusterList List of cluster names (can be null or empty)
     * @return Comma-separated string, or null if list is null/empty
     */
    public static String clusterListToString(List<String> clusterList) {
        if (clusterList == null || clusterList.isEmpty()) {
            return null;
        }
        return String.join(",", clusterList);
    }
}

// Made with Bob
