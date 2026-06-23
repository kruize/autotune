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
 * Provides simple length-based validation for cluster names.
 */
public class ClusterNameUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterNameUtils.class);
    
    // Maximum length for cluster names
    private static final int MAX_CLUSTER_NAME_LENGTH = 253;
    
    /**
     * Validates a cluster name with simple length-based validation.
     * This method is lenient and treats null as valid since cluster names are optional fields.
     * For strict validation that rejects null, use {@link #isValidClusterNameStrict(String)}.
     *
     * Rules:
     * - Maximum 253 characters
     * - Cannot be empty (after trimming)
     * - Null is considered valid (optional field)
     *
     * @param clusterName The cluster name to validate (can be null for optional fields)
     * @return Empty string if valid or null, error message if invalid
     */
    public static String validateClusterName(String clusterName) {
        // Null is acceptable for optional fields
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
        
        return ""; // Valid
    }
    
    /**
     * Checks if a cluster name is valid with simple length-based validation.
     * This method is lenient and treats null as valid since cluster names are optional fields.
     * For strict validation that rejects null, use {@link #isValidClusterNameStrict(String)}.
     *
     * @param clusterName The cluster name to check (null is considered valid)
     * @return true if valid or null, false otherwise
     */
    public static boolean isValidClusterName(String clusterName) {
        return validateClusterName(clusterName).isEmpty();
    }
    
    /**
     * Strictly checks if a cluster name is valid with simple length-based validation.
     * Unlike {@link #isValidClusterName(String)}, this method treats null as invalid.
     * Use this when you need to ensure a cluster name is actually provided and valid.
     *
     * @param clusterName The cluster name to check (null is considered invalid)
     * @return true if valid and non-null, false if invalid or null
     */
    public static boolean isValidClusterNameStrict(String clusterName) {
        if (clusterName == null) {
            return false;
        }
        return validateClusterName(clusterName).isEmpty();
    }
    
    /**
     * Gets a validation error message for a cluster name, or empty string if valid.
     * This is a convenience method that provides a clearer API than validateClusterName
     * for cases where you want to get the error message.
     *
     * @param clusterName The cluster name to validate (null returns error message)
     * @return Error message if invalid or null, empty string if valid
     */
    public static String getValidationError(String clusterName) {
        if (clusterName == null) {
            return "cluster_name cannot be null";
        }
        return validateClusterName(clusterName);
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
