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
package com.autotune.database.helper;

import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.metrics.AcceleratorMetricMetadata;
import com.autotune.common.data.metrics.Metric;
import com.autotune.common.data.metrics.MetricAggregationInfoResults;
import com.autotune.common.data.metrics.MetricMetadata;
import com.autotune.common.data.metrics.MetricMetadataResults;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.NamespaceData;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.utils.KruizeConstants;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Walks {@code kruize_results.extended_data} once and builds only the objects
 * {@code attachContainerInterval} / {@code attachNamespaceInterval} read.
 * Interval start and end stay on the JDBC columns; the stored {@code results}
 * map is skipped. Unexpected tokens throw {@link Fallback} so the caller can
 * use the Gson path for that row.
 */
final class ResultsExtendedDataStreamParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResultsExtendedDataStreamParser.class);
    private static final AtomicBoolean FALLBACK_LOGGED = new AtomicBoolean(false);

    /**
     * Same pattern as {@link com.autotune.analyzer.utils.GsonUTCDateAdapter}.
     * The kept subgraph has no date fields today. Interval bounds come from the
     * result row. This formatter is the parser for a date string if one appears
     * on a kept field.
     */
    static final DateTimeFormatter UTC_TIMESTAMP = DateTimeFormatter
            .ofPattern(KruizeConstants.DateFormats.STANDARD_JSON_DATE_FORMAT, Locale.ROOT)
            .withZone(ZoneOffset.UTC);

    private ResultsExtendedDataStreamParser() {
    }

    static final class Fallback extends RuntimeException {
        Fallback(String message) {
            super(message);
        }

        Fallback(String message, Throwable cause) {
            super(message, cause);
        }
    }

    static void logFallbackOnce(Exception exception) {
        if (FALLBACK_LOGGED.compareAndSet(false, true)) {
            LOGGER.debug("extended_data stream parse fell back to Gson: {}", exception.toString());
        }
    }

    /**
     * @return the kubernetes objects, or null when the key is missing or JSON null
     */
    static List<K8sObject> parseKubernetesObjects(String extendedDataJson) {
        try (JsonReader reader = new JsonReader(new StringReader(extendedDataJson))) {
            JsonToken token = reader.peek();
            if (token == JsonToken.NULL) {
                reader.nextNull();
                return null;
            }
            if (token != JsonToken.BEGIN_OBJECT) {
                throw new Fallback("extended_data root is " + token);
            }
            return readRoot(reader);
        } catch (Fallback fallback) {
            throw fallback;
        } catch (Exception exception) {
            throw new Fallback(exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage(), exception);
        }
    }

    /**
     * Parses a kept date string with {@link #UTC_TIMESTAMP}. A value this rejects
     * is a {@link Fallback} so the row uses Gson.
     */
    static long parseUtcEpochMilli(String text) {
        try {
            return Instant.from(UTC_TIMESTAMP.parse(text)).toEpochMilli();
        } catch (RuntimeException exception) {
            throw new Fallback("date " + text, exception);
        }
    }

    private static List<K8sObject> readRoot(JsonReader reader) throws IOException {
        reader.beginObject();
        List<K8sObject> kubernetesObjects = null;
        boolean sawKubernetesObjects = false;
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (KruizeConstants.JSONKeys.KUBERNETES_OBJECTS.equals(name)) {
                kubernetesObjects = readKubernetesObjects(reader);
                sawKubernetesObjects = true;
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        if (!sawKubernetesObjects) {
            return null;
        }
        return kubernetesObjects;
    }

    private static List<K8sObject> readKubernetesObjects(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }
        if (reader.peek() != JsonToken.BEGIN_ARRAY) {
            throw new Fallback("kubernetes_objects is " + reader.peek());
        }
        List<K8sObject> objects = new ArrayList<>();
        reader.beginArray();
        while (reader.hasNext()) {
            objects.add(readK8sObject(reader));
        }
        reader.endArray();
        return objects;
    }

    private static K8sObject readK8sObject(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            throw new Fallback("kubernetes object is " + reader.peek());
        }
        K8sObject k8sObject = new K8sObject();
        boolean sawContainers = false;
        boolean sawNamespaces = false;
        HashMap<String, ContainerData> containers = null;
        HashMap<String, NamespaceData> namespaces = null;
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case KruizeConstants.JSONKeys.TYPE:
                    k8sObject.setType(readNullableString(reader));
                    break;
                case KruizeConstants.JSONKeys.NAME:
                    k8sObject.setName(readNullableString(reader));
                    break;
                case KruizeConstants.JSONKeys.NAMESPACE:
                    k8sObject.setNamespace(readNullableString(reader));
                    break;
                case KruizeConstants.JSONKeys.CONTAINERS:
                    containers = readContainers(reader);
                    sawContainers = true;
                    break;
                case KruizeConstants.JSONKeys.NAMESPACES:
                    namespaces = readNamespaces(reader);
                    sawNamespaces = true;
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        if (sawContainers) {
            k8sObject.setContainerDataMap(containers);
        }
        if (sawNamespaces) {
            k8sObject.setNamespaceDataMap(namespaces);
        }
        return k8sObject;
    }

    private static HashMap<String, ContainerData> readContainers(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            throw new Fallback("containers is " + reader.peek());
        }
        HashMap<String, ContainerData> containers = new HashMap<>();
        reader.beginObject();
        while (reader.hasNext()) {
            String key = reader.nextName();
            containers.put(key, readContainer(reader));
        }
        reader.endObject();
        return containers;
    }

    private static ContainerData readContainer(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            throw new Fallback("container is " + reader.peek());
        }
        ContainerData containerData = new ContainerData();
        boolean sawMetrics = false;
        HashMap<AnalyzerConstants.MetricName, Metric> metrics = null;
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case KruizeConstants.JSONKeys.CONTAINER_NAME:
                    containerData.setContainer_name(readNullableString(reader));
                    break;
                case KruizeConstants.JSONKeys.CONTAINER_IMAGE_NAME:
                    containerData.setContainer_image_name(readNullableString(reader));
                    break;
                case KruizeConstants.JSONKeys.METRICS:
                    metrics = readMetrics(reader);
                    sawMetrics = true;
                    break;
                default:
                    // results, recommendations, devices, layers
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        if (sawMetrics) {
            containerData.setMetrics(metrics);
        }
        return containerData;
    }

    private static HashMap<String, NamespaceData> readNamespaces(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            throw new Fallback("namespaces is " + reader.peek());
        }
        HashMap<String, NamespaceData> namespaces = new HashMap<>();
        reader.beginObject();
        while (reader.hasNext()) {
            String key = reader.nextName();
            namespaces.put(key, readNamespace(reader));
        }
        reader.endObject();
        return namespaces;
    }

    private static NamespaceData readNamespace(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            throw new Fallback("namespace is " + reader.peek());
        }
        NamespaceData namespaceData = new NamespaceData();
        boolean sawMetrics = false;
        HashMap<AnalyzerConstants.MetricName, Metric> metrics = null;
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case KruizeConstants.JSONKeys.NAMESPACE:
                    namespaceData.setNamespace_name(readNullableString(reader));
                    break;
                case KruizeConstants.JSONKeys.METRICS:
                    metrics = readMetrics(reader);
                    sawMetrics = true;
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        if (sawMetrics) {
            namespaceData.setMetrics(metrics);
        }
        return namespaceData;
    }

    private static HashMap<AnalyzerConstants.MetricName, Metric> readMetrics(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            throw new Fallback("metrics is " + reader.peek());
        }
        HashMap<AnalyzerConstants.MetricName, Metric> metrics = new HashMap<>();
        reader.beginObject();
        while (reader.hasNext()) {
            String key = reader.nextName();
            final AnalyzerConstants.MetricName metricName;
            try {
                metricName = AnalyzerConstants.MetricName.valueOf(key);
            } catch (IllegalArgumentException exception) {
                throw new Fallback("metric " + key, exception);
            }
            metrics.put(metricName, readMetric(reader));
        }
        reader.endObject();
        return metrics;
    }

    private static Metric readMetric(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            throw new Fallback("metric is " + reader.peek());
        }
        String metricName = null;
        boolean sawResults = false;
        MetricResults metricResults = null;
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case KruizeConstants.JSONKeys.NAME:
                    metricName = readNullableString(reader);
                    break;
                case KruizeConstants.JSONKeys.RESULTS:
                    metricResults = readMetricResults(reader);
                    sawResults = true;
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        Metric metric = new Metric(metricName, null, null, null, null);
        if (sawResults) {
            metric.setMetricResult(metricResults);
        }
        return metric;
    }

    private static MetricResults readMetricResults(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            throw new Fallback("metric results is " + reader.peek());
        }
        // No-arg constructor leaves an empty aggregation object, matching Gson
        // when aggregation_info is absent.
        MetricResults metricResults = new MetricResults();
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case KruizeConstants.JSONKeys.AGGREGATION_INFO:
                    metricResults.setAggregationInfoResult(readAggregation(reader));
                    break;
                case AnalyzerConstants.AutotuneObjectConstants.METADATA:
                    metricResults.setMetadata(readMetadata(reader));
                    break;
                case AnalyzerConstants.AutotuneObjectConstants.JVM_METADATA:
                    metricResults.setMetricMetadataResults(readJvmMetadata(reader));
                    break;
                case KruizeConstants.JSONKeys.NAME:
                    metricResults.setName(readNullableString(reader));
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        return metricResults;
    }

    private static MetricAggregationInfoResults readAggregation(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            throw new Fallback("aggregation_info is " + reader.peek());
        }
        MetricAggregationInfoResults aggregation = new MetricAggregationInfoResults();
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case KruizeConstants.JSONKeys.AVG:
                    aggregation.setAvg(readNullableDouble(reader));
                    break;
                case KruizeConstants.JSONKeys.MIN:
                    aggregation.setMin(readNullableDouble(reader));
                    break;
                case KruizeConstants.JSONKeys.MAX:
                    aggregation.setMax(readNullableDouble(reader));
                    break;
                case KruizeConstants.JSONKeys.SUM:
                    aggregation.setSum(readNullableDouble(reader));
                    break;
                case KruizeConstants.JSONKeys.MEDIAN:
                    aggregation.setMedian(readNullableDouble(reader));
                    break;
                case KruizeConstants.JSONKeys.MODE:
                    aggregation.setMode(readNullableDouble(reader));
                    break;
                case KruizeConstants.JSONKeys.RANGE:
                    aggregation.setRange(readNullableDouble(reader));
                    break;
                case KruizeConstants.JSONKeys.COUNT:
                    aggregation.setCount(readNullableCount(reader));
                    break;
                case KruizeConstants.JSONKeys.FORMAT:
                    aggregation.setFormat(readNullableString(reader));
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        return aggregation;
    }

    /**
     * Same rules as {@link com.autotune.analyzer.adapters.MetricMetadataAdapter}:
     * a missing type, or type {@code accelerator}, builds accelerator metadata.
     * Any other type falls back to Gson.
     */
    private static MetricMetadata readMetadata(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            throw new Fallback("metadata is " + reader.peek());
        }
        String type = null;
        String modelName = null;
        String profileName = null;
        String node = null;
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case KruizeConstants.JSONKeys.TYPE:
                    type = readNullableString(reader);
                    break;
                case KruizeConstants.JSONKeys.ACCELERATOR_MODEL_NAME:
                    modelName = readNullableString(reader);
                    break;
                case KruizeConstants.JSONKeys.ACCELERATOR_PROFILE_NAME:
                    profileName = readNullableString(reader);
                    break;
                case KruizeConstants.JSONKeys.NODE:
                    node = readNullableString(reader);
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        if (type == null || type.equalsIgnoreCase(AnalyzerConstants.DeviceType.ACCELERATOR.toString())) {
            return new AcceleratorMetricMetadata(modelName, profileName, node);
        }
        throw new Fallback("metadata type " + type);
    }

    private static MetricMetadataResults readJvmMetadata(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            throw new Fallback("jvm_metadata is " + reader.peek());
        }
        MetricMetadataResults metadata = new MetricMetadataResults();
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case KruizeConstants.JSONKeys.VERSION:
                    metadata.setVersion(readNullableString(reader));
                    break;
                case AnalyzerConstants.RUNTIME:
                    metadata.setRuntime(readNullableString(reader));
                    break;
                case AnalyzerConstants.VENDOR:
                    metadata.setVendor(readNullableString(reader));
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        return metadata;
    }

    private static String readNullableString(JsonReader reader) throws IOException {
        JsonToken token = reader.peek();
        if (token == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }
        if (token == JsonToken.BEGIN_OBJECT || token == JsonToken.BEGIN_ARRAY) {
            throw new Fallback("expected string but was " + token);
        }
        return reader.nextString();
    }

    private static Double readNullableDouble(JsonReader reader) throws IOException {
        JsonToken token = reader.peek();
        if (token == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }
        if (token != JsonToken.NUMBER) {
            throw new Fallback("aggregation number is " + token);
        }
        return reader.nextDouble();
    }

    private static Integer readNullableCount(JsonReader reader) throws IOException {
        JsonToken token = reader.peek();
        if (token == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }
        if (token != JsonToken.NUMBER) {
            throw new Fallback("count is " + token);
        }
        try {
            return reader.nextInt();
        } catch (NumberFormatException exception) {
            throw new Fallback("count", exception);
        }
    }
}
