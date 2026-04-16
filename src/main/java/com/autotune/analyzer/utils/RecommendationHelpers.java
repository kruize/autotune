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
import com.autotune.analyzer.recommendations.Config;
import com.autotune.analyzer.recommendations.Variation;
import com.autotune.analyzer.recommendations.objects.MappedRecommendationForModel;
import com.autotune.analyzer.recommendations.objects.TermRecommendations;
import com.autotune.analyzer.serviceObjects.ContainerAPIObject;
import com.autotune.analyzer.serviceObjects.Converters;
import com.autotune.analyzer.serviceObjects.ListRecommendationsAPIObject;
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
     * Converts a collection of {@link KruizeObject} instances into recommendation response
     * objects.
     *
     * <p>This helper centralizes the shared conversion flow used by the recommendation listing and
     * generation endpoints. Depending on {@code useV1Converter}, it routes conversion to either
     * the legacy schema converter or the V1 schema converter.
     *
     * @param kruizeObjectList the experiment objects to convert
     * @param getLatest whether only the latest recommendation should be retained when no explicit
     *                  timestamp filtering is requested
     * @param checkForTimestamp whether recommendations should be filtered using
     *                          {@code monitoringEndTime}
     * @param monitoringEndTime the timestamp to retain when {@code checkForTimestamp} is true
     * @param useV1Converter whether the V1 recommendation converter should be used
     * @return a list of converted {@link ListRecommendationsAPIObject} instances
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
     * Converts a single {@link KruizeObject} into a recommendation response object.
     *
     * <p>This is a convenience wrapper over the shared conversion flow for callers that only need
     * to serialize one experiment. The legacy or V1 converter is selected using
     * {@code useV1Converter}.
     *
     * @param kruizeObject the experiment object to convert
     * @param getLatest whether only the latest recommendation should be retained when no explicit
     *                  timestamp filtering is requested
     * @param checkForTimestamp whether recommendations should be filtered using
     *                          {@code monitoringEndTime}
     * @param monitoringEndTime the timestamp to retain when {@code checkForTimestamp} is true
     * @param useV1Converter whether the V1 recommendation converter should be used
     * @return the converted {@link ListRecommendationsAPIObject}, or {@code null} if conversion
     *         fails
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
     * Creates the shared {@link Gson} serializer used by the recommendation endpoints.
     *
     * <p>The serializer registers all custom adapters required by the recommendation response
     * model and applies schema-specific exclusion rules:
     * <ul>
     *     <li>legacy responses hide V1-only fields such as {@code replicas} and
     *     {@code metricsInfo}</li>
     *     <li>V1 responses hide legacy duplication such as top-level {@code requests},
     *     {@code limits}, and {@code podsCount}</li>
     * </ul>
     *
     * @param useV1Converter whether the serializer should be configured for the V1 recommendation
     *                       schema
     * @return a configured {@link Gson} instance
     */
    public static Gson createGsonObject(boolean useV1Converter) {
        ExclusionStrategy strategy = new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes field) {
                boolean skipDefaultFields =
                        field.getDeclaringClass() == ContainerData.class && field.getName().equals(KruizeConstants.JSONKeys.RESULTS)
                                || field.getDeclaringClass() == ContainerAPIObject.class && field.getName().equals(KruizeConstants.JSONKeys.METRICS);

                if (skipDefaultFields) {
                    return true;
                }

                if (useV1Converter) {
                    return ((field.getDeclaringClass() == Config.class || field.getDeclaringClass() == Variation.class)
                            && (field.getName().equals(KruizeConstants.JSONKeys.REQUESTS)
                            || field.getName().equals(KruizeConstants.JSONKeys.LIMITS)))
                            || (field.getDeclaringClass() == MappedRecommendationForModel.class
                            && field.getName().equals("podsCount"));
                }

                return (field.getDeclaringClass() == Config.class
                        && field.getName().equals(KruizeConstants.JSONKeys.REPLICAS))
                        || (field.getDeclaringClass() == TermRecommendations.class
                        && field.getName().equals("metricsInfo"));
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
     * Serializes recommendation response objects and writes them to the HTTP response.
     *
     * <p>This helper applies the correct JSON serialization rules for either the legacy or V1
     * response schema and optionally logs the generated payload.
     *
     * @param response the HTTP response to write to
     * @param recommendationList the recommendation response objects to serialize
     * @param logResponse whether the serialized payload should be logged
     * @param useV1Converter whether V1 serialization rules should be applied
     * @throws IOException if the response cannot be written
     */
    public static void writeRecommendationsResponse(
            HttpServletResponse response,
            List<ListRecommendationsAPIObject> recommendationList,
            boolean logResponse,
            boolean useV1Converter) throws IOException {

        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);

        String gsonStr = "[]";
        if (!recommendationList.isEmpty()) {
            Gson gsonObj = createGsonObject(useV1Converter);
            gsonStr = gsonObj.toJson(recommendationList);
        }

        if (logResponse) {
            LOGGER.info("Recommendations response: {}", new Gson().toJson(JsonParser.parseString(gsonStr)));
        }

        response.getWriter().println(gsonStr);
        response.getWriter().close();
    }
}
