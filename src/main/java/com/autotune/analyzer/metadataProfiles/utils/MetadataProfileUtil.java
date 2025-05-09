/*******************************************************************************
 * Copyright (c) 2024 Red Hat, IBM Corporation and others.
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
package com.autotune.analyzer.metadataProfiles.utils;

import com.autotune.analyzer.metadataProfiles.MetadataProfile;
import com.autotune.analyzer.metadataProfiles.MetadataProfileValidation;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.utils.KruizeConstants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  Utils class for MetadataProfile
 */
public class MetadataProfileUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(com.autotune.analyzer.metadataProfiles.utils.MetadataProfileUtil.class);
    private MetadataProfileUtil() {

    }

    /**
     * validates the metadata profile fields and the data and then adds it to the map
     * @param metadataProfile
     * @return
     */
    public static ValidationOutputData validateAndAddMetadataProfile(Map<String, MetadataProfile> metadataProfileMapProfilesMap, MetadataProfile metadataProfile) {
        ValidationOutputData validationOutputData;
        try {
            validationOutputData = new MetadataProfileValidation(metadataProfileMapProfilesMap).validate(metadataProfile);
            if (validationOutputData.isSuccess()) {
                addMetadataProfile(metadataProfileMapProfilesMap, metadataProfile);
            } else {
                validationOutputData.setMessage(KruizeConstants.MetadataProfileConstants.METADATA_PROFILE_VALIDATION_FAILURE + validationOutputData.getMessage());
            }
        } catch (Exception e) {
            LOGGER.error(KruizeConstants.MetadataProfileConstants.METADATA_PROFILE_VALIDATION_AND_ADD_FAILURE, e.getMessage());
            validationOutputData = new ValidationOutputData(false,
                    KruizeConstants.MetadataProfileConstants.METADATA_PROFILE_VALIDATION_FAILURE + e.getMessage(),
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        return validationOutputData;
    }


    public static void addMetadataProfile(Map<String, MetadataProfile> metadataProfileMap, MetadataProfile metadataProfile) {
        metadataProfileMap.put(metadataProfile.getMetadata().get("name").asText(), metadataProfile);
        LOGGER.debug(KruizeConstants.MetadataProfileConstants.ADD_METADATA_PROFILE, metadataProfile.getMetadata().get("name"));
    }

    public static ValidationOutputData validateMetadataProfile(Map<String, MetadataProfile> metadataProfileMapProfilesMap, MetadataProfile metadataProfile) {
        ValidationOutputData validationOutputData;
        try {
            validationOutputData = new MetadataProfileValidation(metadataProfileMapProfilesMap).validateProfileData(metadataProfile);
            if (!validationOutputData.isSuccess()) {
                validationOutputData.setMessage(KruizeConstants.MetadataProfileConstants.METADATA_PROFILE_VALIDATION_FAILURE + validationOutputData.getMessage());
            }
        } catch (Exception e) {
            LOGGER.error(KruizeConstants.MetadataProfileConstants.METADATA_PROFILE_VALIDATION_ERROR, e.getMessage());
            validationOutputData = new ValidationOutputData(false,
                    KruizeConstants.MetadataProfileConstants.METADATA_PROFILE_VALIDATION_FAILURE + e.getMessage(),
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        return validationOutputData;
    }

    /**
     * This function extracts the identifiers within the parentheses of the 'sum by' clause and checks if the set of these
     * identifiers matches the expected set for any of the MetadataProfileQueryPattern enum constants.
     * The order of identifiers in the input 'sum by' clause does not affect the matching process.
     */
    public static AnalyzerConstants.MetadataProfileQueryPattern matchSumByClause(String input) {
        Matcher matcher = Pattern.compile(AnalyzerConstants.METADATA_PROFILE_QUERY_MATCHER).matcher(input);
        if (matcher.find()) {
            String identifiersPart = matcher.group(1);
            String[] inputIdentifiers = identifiersPart.split(AnalyzerConstants.commaSpaceRegex);
            Set<String> inputIdentifierSet = new HashSet<>(Arrays.asList(inputIdentifiers));

            for (AnalyzerConstants.MetadataProfileQueryPattern pattern : AnalyzerConstants.MetadataProfileQueryPattern.values()) {
                if (inputIdentifierSet.equals(pattern.getExpectedIdentifiers())) {
                    return pattern;
                }
            }
        }
        return null;
    }

    public static String getIdentifier(String metricName){
        String identifier = null;
        if(metricName.contains(AnalyzerConstants.NAMESPACE)){
            identifier= AnalyzerConstants.NAMESPACE;
        } else if(metricName.contains(AnalyzerConstants.WORKLOAD)) {
            identifier=AnalyzerConstants.WORKLOAD;
        } else if(metricName.contains(AnalyzerConstants.CONTAINER)) {
            identifier=AnalyzerConstants.CONTAINER;
        }
        return identifier;
    }

    public static String getFilterString(String identifier) {
        String identifierToFilter = AnalyzerConstants.KRUIZE_PROFILE_FILTER;
        String namespaceIdentifierToFilter = AnalyzerConstants.NAMESPACE_PROFILE_FILTER;
        String filterString;
        if(identifier.equals(AnalyzerConstants.NAMESPACE)) {
            filterString = String.format(AnalyzerConstants.NAMESPACE_FILTER_IDENTIFIER, identifier, namespaceIdentifierToFilter);
        } else {
            filterString = String.format(AnalyzerConstants.WORKLOAD_FILTER_IDENTIFIER, identifier, identifierToFilter);
        }
        return filterString;
    }

    public static String appendFiltersToQuery(String query, String filters) {
        int firstOpeningBraceIndex = query.indexOf("{");
        if (firstOpeningBraceIndex == -1) {
            return query;
        }

        int firstClosingBraceIndex = query.indexOf("}", firstOpeningBraceIndex);
        if (firstClosingBraceIndex == -1) {
            return query;
        }

        int contentStartIndex = firstOpeningBraceIndex + 1;
        String beforeBrace = query.substring(0, contentStartIndex);
        String insideBrace = query.substring(contentStartIndex, firstClosingBraceIndex).trim();
        String afterBrace = query.substring(firstClosingBraceIndex);

        return beforeBrace + insideBrace + filters + afterBrace;
    }

    public static boolean checkResultIdentifiers(JsonArray resultArrayJson, AnalyzerConstants.MetadataProfileQueryPattern pattern) {
        if (pattern == null || resultArrayJson == null || resultArrayJson.isEmpty()) {
            return false;
        }

        Set<String> expectedIdentifiers = pattern.getExpectedIdentifiers();

        for (JsonElement element : resultArrayJson) {
            if (element.isJsonObject()) {
                JsonObject result = element.getAsJsonObject();
                if (result.has(KruizeConstants.JSONKeys.METRIC) && result.get(KruizeConstants.JSONKeys.METRIC).isJsonObject()) {
                    JsonObject metric = result.get(KruizeConstants.JSONKeys.METRIC).getAsJsonObject();
                    Set<String> actualIdentifiers = metric.keySet();
                    if (actualIdentifiers.containsAll(expectedIdentifiers)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
