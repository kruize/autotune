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

package com.autotune.analyzer.utils;

import com.autotune.analyzer.adapters.DeviceDetailsAdapter;
import com.autotune.analyzer.adapters.MetricAggregationInfoResultsIntSerializer;
import com.autotune.analyzer.adapters.MetricMetadataAdapter;
import com.autotune.analyzer.adapters.RecommendationItemAdapter;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.serviceObjects.ContainerAPIObject;
import com.autotune.analyzer.serviceObjects.Converters;
import com.autotune.analyzer.serviceObjects.ListRecommendationsAPIObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.GsonUTCDateAdapter;
import com.autotune.common.data.metrics.MetricAggregationInfoResults;
import com.autotune.common.data.metrics.MetricMetadata;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.system.info.device.DeviceDetails;
import com.autotune.utils.KruizeConstants;
import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

/**
 * Helper class that provides common recommendation operations.
 * This class extracts only the common/reusable logic from ListRecommendations and UpdateRecommendations
 * to avoid code duplication while keeping the original servlets working as-is.
 */
public class RecommendationHelpers {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecommendationHelpers.class);

    /**
     * Converts a list of KruizeObjects to ListRecommendationsAPIObjects.
     * This is the common conversion logic used by both ListRecommendations and UpdateRecommendations.
     *
     * @param kruizeObjectList   List of KruizeObjects to convert
     * @param getLatest         Whether to get only the latest recommendation
     * @param checkForTimestamp Whether to check for a specific timestamp
     * @param monitoringEndTime The specific monitoring end time to filter by
     * @param useV1Converter    Whether to use V1 converter for new schema
     * @return List of ListRecommendationsAPIObject
     */
    public static List<ListRecommendationsAPIObject> convertKruizeObjectsToRecommendations(
            List<KruizeObject> kruizeObjectList,
            boolean getLatest,
            boolean checkForTimestamp,
            Timestamp monitoringEndTime,
            boolean useV1Converter) {

        List<ListRecommendationsAPIObject> recommendationList = new ArrayList<>();

        for (KruizeObject ko : kruizeObjectList) {
            try {
                ListRecommendationsAPIObject listRecommendationsAPIObject;
                if (useV1Converter) {
                    // Use V1 converter for new schema
                    listRecommendationsAPIObject = Converters.KruizeObjectConverters
                            .convertKruizeObjectToListRecommendationSOV1(
                                    ko, getLatest, checkForTimestamp, monitoringEndTime);
                } else {
                    // Use standard converter
                    listRecommendationsAPIObject = Converters.KruizeObjectConverters
                            .convertKruizeObjectToListRecommendationSO(
                                    ko, getLatest, checkForTimestamp, monitoringEndTime);
                }
                recommendationList.add(listRecommendationsAPIObject);
            } catch (Exception e) {
                LOGGER.error("Not able to generate recommendation for expName: {} due to {}",
                        ko.getExperimentName(), e.getMessage());
            }
        }

        return recommendationList;
    }

    /**
     * Converts a single KruizeObject to ListRecommendationsAPIObject.
     * Convenience method for single object conversion.
     *
     * @param kruizeObject      The KruizeObject to convert
     * @param getLatest        Whether to get only the latest recommendation
     * @param checkForTimestamp Whether to check for a specific timestamp
     * @param monitoringEndTime The specific monitoring end time to filter by
     * @param useV1Converter   Whether to use V1 converter for new schema
     * @return ListRecommendationsAPIObject or null if conversion fails
     */
    public static ListRecommendationsAPIObject convertKruizeObjectToRecommendation(
            KruizeObject kruizeObject,
            boolean getLatest,
            boolean checkForTimestamp,
            Timestamp monitoringEndTime,
            boolean useV1Converter) {

        try {
            if (useV1Converter) {
                return Converters.KruizeObjectConverters.convertKruizeObjectToListRecommendationSOV1(
                        kruizeObject, getLatest, checkForTimestamp, monitoringEndTime);
            } else {
                return Converters.KruizeObjectConverters.convertKruizeObjectToListRecommendationSO(
                        kruizeObject, getLatest, checkForTimestamp, monitoringEndTime);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to generate recommendation for experiment: {}, error: {}",
                    kruizeObject.getExperimentName(), e.getMessage());
            return null;
        }
    }

    /**
     * Creates the common Gson object with all necessary type adapters and exclusion strategies.
     * This is the common JSON serialization configuration used by both servlets.
     *
     * @return Configured Gson object
     */
    public static Gson createGsonObject() {
        ExclusionStrategy strategy = new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes field) {
                return field.getDeclaringClass() == ContainerData.class && (field.getName().equals(KruizeConstants.JSONKeys.RESULTS))
                        || (field.getDeclaringClass() == ContainerAPIObject.class && (field.getName().equals(KruizeConstants.JSONKeys.METRICS)));
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        };

        return new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .enableComplexMapKeySerialization()
                .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                .registerTypeAdapter(AnalyzerConstants.RecommendationItem.class, new RecommendationItemAdapter())
                .registerTypeAdapter(DeviceDetails.class, new DeviceDetailsAdapter())
                .registerTypeAdapter(MetricMetadata.class, new MetricMetadataAdapter())
                .registerTypeAdapter(MetricAggregationInfoResults.class, new MetricAggregationInfoResultsIntSerializer())
                .setExclusionStrategies(strategy)
                .create();
    }

    /**
     * Writes the recommendation list as JSON to the HTTP response.
     * This is the common response writing logic used by both servlets.
     *
     * @param response          The HttpServletResponse to write to
     * @param recommendationList The list of recommendations to serialize
     * @param logResponse       Whether to log the response (for debugging)
     * @throws IOException if writing to response fails
     */
    public static void writeRecommendationsResponse(
            HttpServletResponse response,
            List<ListRecommendationsAPIObject> recommendationList,
            boolean logResponse) throws IOException {

        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);

        String gsonStr = "[]";
        if (!recommendationList.isEmpty()) {
            Gson gsonObj = createGsonObject();
            gsonStr = gsonObj.toJson(recommendationList);
        }

        if (logResponse) {
            LOGGER.info("Recommendations response: {}", new Gson().toJson(JsonParser.parseString(gsonStr)));
        }

        response.getWriter().println(gsonStr);
        response.getWriter().close();
    }
}
