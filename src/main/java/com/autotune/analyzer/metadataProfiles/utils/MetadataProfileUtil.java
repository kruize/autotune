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

    /**
     * Validates the metadata profile fields and the data before updating the existing profile
     *
     * @param metadataProfileMapProfilesMap Map storing MetadataProfile name and the corresponding object
     * @param metadataProfile MetadataProfile object to be validated
     * @return A ValidationOutputData object indicating the success/failure of validations.
     */
    public static ValidationOutputData validateMetadataProfile(Map<String, MetadataProfile> metadataProfileMapProfilesMap, MetadataProfile metadataProfile) {
        ValidationOutputData validationOutputData;
        try {
            validationOutputData = new MetadataProfileValidation(metadataProfileMapProfilesMap).validateProfileDataBeforeUpdating(metadataProfile);
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
     *
     * @param inputQuery MetricQuery from which the 'sum by' clause is extracted and validated against expected identifiers
     * @return The identifier of input query
     */
    public static AnalyzerConstants.MetadataProfileQueryIdentifier matchSumByClause(String inputQuery) {
        Matcher matcher = Pattern.compile(AnalyzerConstants.METADATA_PROFILE_QUERY_MATCHER).matcher(inputQuery);
        if (matcher.find()) {
            String identifiersPart = matcher.group(1);
            String[] inputIdentifiers = identifiersPart.split(AnalyzerConstants.COMMA_SPACE_REGEX);
            Set<String> inputIdentifierSet = new HashSet<>(Arrays.asList(inputIdentifiers));

            for (AnalyzerConstants.MetadataProfileQueryIdentifier pattern : AnalyzerConstants.MetadataProfileQueryIdentifier.values()) {
                if (inputIdentifierSet.equals(pattern.getExpectedIdentifiers())) {
                    return pattern;
                }
            }
        }
        return null;
    }

    /**
     * Extracts an identifier from a given metric name based on predefined constants.
     * This method checks if the metric name contains specific constant strings (NAMESPACE, WORKLOAD, CONTAINER)
     * and returns the first matching constant found.
     *
     * @param metricName MetricName from which to extract the identifier.
     * @return The identified constant string (NAMESPACE, WORKLOAD, or CONTAINER)
     */
    public static String getMetricIdentifier(String metricName) {
        String identifier = null;
        if (metricName.contains(AnalyzerConstants.NAMESPACE)) {
            identifier = AnalyzerConstants.NAMESPACE;
        } else if (metricName.contains(AnalyzerConstants.WORKLOAD)) {
            identifier = AnalyzerConstants.WORKLOAD;
        } else if (metricName.contains(AnalyzerConstants.CONTAINER)) {
            identifier = AnalyzerConstants.CONTAINER;
        }
        return identifier;
    }

    /**
     * Formats a metric filter string based on the given metric identifier.
     * Follows different formatting rules for namespace identifiers versus
     * workload or container identifiers.
     *
     * @param metricIdentifier The identifier used to determine the filter string's structure
     * @return A formatted string with filtering metrics.
     */
    public static String formatMetricFilterString(String metricIdentifier) {
        if (null == metricIdentifier || metricIdentifier.isEmpty()) {
            return null;
        }
        String workloadAndContainerIdentifierToFilter = AnalyzerConstants.KRUIZE_PROFILE_FILTER;
        String namespaceIdentifierToFilter = AnalyzerConstants.NAMESPACE_PROFILE_FILTER;
        String filterString;
        if (metricIdentifier.equals(AnalyzerConstants.NAMESPACE)) {
            filterString = String.format(AnalyzerConstants.NAMESPACE_FILTER_IDENTIFIER, metricIdentifier, namespaceIdentifierToFilter);
        } else {
            filterString = String.format(AnalyzerConstants.WORKLOAD_FILTER_IDENTIFIER, metricIdentifier, workloadAndContainerIdentifierToFilter);
        }
        return filterString;
    }

    /**
     * Appends additional filter clauses directly inside the first encountered curly braces of a query string.
     * This method assumes the query contains a pair of curly braces '{...}' where filters should be inserted.
     *
     * <p>Example:</p>
     * <pre></pre>
     * String namespaceQuery = "sum by (namespace) (avg_over_time(kube_namespace_status_phase{namespace!=""}[15m]))";
     *
     * Example filter string generated by formatMetricFilterString for namespace:
     * , namespace~=\"openshift-tuning|monitoring\"
     *
     * modifiedQuery will be:
     * "sum by (namespace) (avg_over_time(kube_namespace_status_phase{namespace!=\"\", namespace~=\"openshift-tuning|monitoring\"}[15m]))"
     * </pre>
     *
     * @param query The original query string to which filters will be appended.
     * @param filtersToAppend The filter string to be inserted into the query.
     * @return The modified query string with filters appended.
     */
    public static String appendFiltersToQuery(String query, String filtersToAppend) {
        if (null == query || query.isEmpty() || null == filtersToAppend || filtersToAppend.isEmpty()) {
            return query;
        }

        int firstOpeningBraceIndex = query.indexOf("{");
        if (firstOpeningBraceIndex == -1) {
            return query;
        }

        int firstClosingBraceIndex = query.indexOf("}", firstOpeningBraceIndex);
        if (firstClosingBraceIndex == -1) {
            return query;
        }

        int contentStartIndex = firstOpeningBraceIndex + 1;
        String queryPrefix = query.substring(0, contentStartIndex);
        String contentInsideBraces = query.substring(contentStartIndex, firstClosingBraceIndex).trim();
        String querySuffix = query.substring(firstClosingBraceIndex);

        // Strip leading ", " from the filter when the selector is empty to avoid {, filter=...}
        String effectiveFilter = contentInsideBraces.isEmpty()
                ? filtersToAppend.replaceFirst("^,\\s*", "")
                : filtersToAppend;

        return queryPrefix + contentInsideBraces + effectiveFilter + querySuffix;
    }

    /**
     * Appends filter clauses to the selector whose preceding metric name contains the given
     * identifier. This is purely structure-based: it scans each {@code metric{...}} occurrence
     * in the query and picks the first one whose metric name (the text before {@code {}) contains
     * the identifier string. This correctly handles simple queries as well as any joined
     * multi-metric query without relying on a hardcoded metric name.
     *
     * <p>Selector boundaries are found by {@link #findSelectorCloseBrace(String, int)}, which
     * skips over quoted label values so that a {@code }} inside a regex value (e.g.
     * {@code workload_type!~"(?i)foo{bar}"}) is not mistaken for the end of the selector.</p>
     *
     * <p>If no selector's metric name contains the identifier, falls back to the first selector.</p>
     *
     * @param query            The query string to modify.
     * @param metricIdentifier The identifier to match against metric names (e.g. "namespace",
     *                         "workload", "container").
     * @param filtersToAppend  The filter string to insert (may start with ", ").
     * @return The modified query with the filter inserted into the correct selector.
     */
    public static String appendFiltersToMatchingSelector(String query, String metricIdentifier, String filtersToAppend) {
        if (null == query || query.isEmpty() || null == filtersToAppend || filtersToAppend.isEmpty()) {
            return query;
        }

        int searchFrom = 0;
        int matchedOpen = -1;
        int matchedClose = -1;

        // Walk every {…} selector in the query and pick the first whose metric name
        // (the text between the previous } or start-of-string and the {) contains the identifier.
        while (true) {
            int openBrace = query.indexOf("{", searchFrom);
            if (openBrace == -1) break;

            // Find the real closing brace by respecting quoted label values so that
            // a } inside a quoted regex (e.g. workload_type!~"(?i)Foo{Bar}") is not
            // mistaken for the end of the selector.
            int closeBrace = findSelectorCloseBrace(query, openBrace + 1);
            if (closeBrace == -1) break;

            // Extract the metric name: everything after the last } (or start) up to this {
            int prevClose = query.lastIndexOf("}", openBrace - 1);
            String metricNameSegment = query.substring(prevClose + 1, openBrace);

            if (metricNameSegment.contains(metricIdentifier)) {
                matchedOpen = openBrace;
                matchedClose = closeBrace;
                break;
            }
            searchFrom = closeBrace + 1;
        }

        // Fall back to the first selector if no metric name matched
        if (matchedOpen == -1) {
            return appendFiltersToQuery(query, filtersToAppend);
        }

        int contentStartIndex = matchedOpen + 1;
        String queryPrefix = query.substring(0, contentStartIndex);
        String contentInsideBraces = query.substring(contentStartIndex, matchedClose).trim();
        String querySuffix = query.substring(matchedClose);

        // Strip leading ", " from the filter when the selector is empty
        String effectiveFilter = contentInsideBraces.isEmpty()
                ? filtersToAppend.replaceFirst("^,\\s*", "")
                : filtersToAppend;

        return queryPrefix + contentInsideBraces + effectiveFilter + querySuffix;
    }

    /**
     * Finds the index of the closing {@code }} of a PromQL label selector, starting the scan at
     * {@code startIndex} (one past the opening {@code {}). Quoted label values are skipped so that
     * a {@code }} inside a string (e.g. a regex like {@code "(?i)foo{bar}"}) is never mistaken for
     * the selector boundary.
     *
     * @param query      The full query string.
     * @param startIndex The index to begin scanning from (one past the opening {@code {}).
     * @return The index of the closing {@code }}, or {@code -1} if not found.
     */
    public static int findSelectorCloseBrace(String query, int startIndex) {
        int i = startIndex;
        while (i < query.length()) {
            char c = query.charAt(i);
            if (c == '}') {
                return i;
            } else if (c == '"') {
                // Skip over the quoted string, handling \" escapes
                i++;
                while (i < query.length()) {
                    char q = query.charAt(i);
                    if (q == '\\') {
                        i += 2; // skip escaped character
                    } else if (q == '"') {
                        i++; // step past closing quote
                        break;
                    } else {
                        i++;
                    }
                }
            } else {
                i++;
            }
        }
        return -1;
    }

    /**
     * Validates if any metric object within the result JSON array of prometheus query output contains all expected identifiers.
     *
     * @param resultArrayJson The JsonArray of prometheus query results, where each element may contain a metric object.
     * Cannot be null or empty.
     * @param queryIdentifier An enum or object holding the set of expected identifier strings. Cannot be null.
     * @return {@code true} if at least one metric object contains all expected identifiers; {@code false} otherwise.
     */
    public static boolean validateResultIdentifiers(JsonArray resultArrayJson, AnalyzerConstants.MetadataProfileQueryIdentifier queryIdentifier) {
        if (queryIdentifier == null || resultArrayJson == null || resultArrayJson.isEmpty()) {
            return false;
        }

        Set<String> expectedIdentifiers = queryIdentifier.getExpectedIdentifiers();

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
