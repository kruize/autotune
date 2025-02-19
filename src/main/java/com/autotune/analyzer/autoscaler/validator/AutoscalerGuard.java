/*******************************************************************************
 * Copyright (c) 2025 Red Hat, IBM Corporation and others.
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

package com.autotune.analyzer.autoscaler.validator;


import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.utils.KruizeConstants;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



/* The AutoscalerGuard class is responsible for determining if an experiment should be
** excluded from autoscaling. It checks for critical load conditions, such asin restricted namespaces
** (e.g., openshift-*) or other criteria indicating that scaling might negatively impact system stability.
*/

public class AutoscalerGuard {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoscalerGuard.class);

    // contains list of namespaces to exclude from auto-scaling
    private static List<String> excludeNamespaces = new ArrayList<>();

    /**
     * Verifies all the checks
     */

    public static ValidationOutputData checkAutoscalerGuard(KruizeObject kruizeObject) {
        ValidationOutputData validationOutputData = new ValidationOutputData(true, null, null);
        String namespace = "";
        // verify if namespace is excluded
        for (K8sObject k8sObject: kruizeObject.getKubernetes_objects()) {
            namespace = k8sObject.getNamespace();
        }
        if (isExcludedNamespace(namespace)) {
            validationOutputData = new ValidationOutputData(false, String.format(AnalyzerErrorConstants.RecommendationUpdaterErrors.NAMESPACE_EXCLUDED, namespace), null);
        }

        return validationOutputData;
    }


    /**
     * Checks if a given namespace is excluded from auto-scaling
     *
     * @param namespace the namespace to check.
     * @return boolean true if the namespace is excluded, false otherwise
     */
    private static boolean isExcludedNamespace(String namespace) {

        if (excludeNamespaces == null || excludeNamespaces.isEmpty()) {
            getExcludedNamespaces(KruizeConstants.CONFIG_FILE);
        }

        for (String patternStr : excludeNamespaces) {
            // Check if it's a regex pattern or a simple string match
            if (patternStr.contains("*") || patternStr.contains("?") || patternStr.contains("[") || patternStr.contains("]")) {
                try {
                    Pattern pattern = Pattern.compile(patternStr);
                    Matcher matcher = pattern.matcher(namespace);
                    if (matcher.matches()) {
                        return true;
                    }
                } catch (Exception e) {
                    LOGGER.error(e.getMessage());
                }
            } else if (namespace.equalsIgnoreCase(patternStr)) {
                return true;
            }
        }

        return false;
    }



    /**
     * Reads the kruize configuration file and returns a list of excluded namespaces.
     * @param configFile The path to the kruize configuration file.
     * @return A list of strings representing the excluded namespaces.
     */
    private static void getExcludedNamespaces(String configFile) {
        JSONObject configObject = null;
        try {
            configObject = new JSONObject(KruizeDeploymentInfo.autoscaler_options);
            if (configObject.has(AnalyzerConstants.AutoscalerConstants.AUTOSCALER_OPTIONS)) {
                JSONObject autoscalerOptions = configObject.getJSONObject(AnalyzerConstants.AutoscalerConstants.AUTOSCALER_OPTIONS);
                if (autoscalerOptions.has(AnalyzerConstants.AutoscalerConstants.EXCLUDE_NAMESPACES)) {
                    JSONArray namespacesArray = autoscalerOptions.getJSONArray(AnalyzerConstants.AutoscalerConstants.EXCLUDE_NAMESPACES);
                    for (int i = 0; i < namespacesArray.length(); i++) {
                        excludeNamespaces.add(namespacesArray.getString(i));
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

}
