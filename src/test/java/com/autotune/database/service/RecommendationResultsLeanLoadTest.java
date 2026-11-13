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
package com.autotune.database.service;

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.serviceObjects.UpdateResultsAPIObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.metrics.Metric;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.ExperimentResultData;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.common.data.result.NamespaceData;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.database.helper.DBHelpers;
import com.autotune.database.table.KruizeResultsEntry;
import com.google.gson.Gson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationResultsLeanLoadTest {
    private static final Gson GSON = new Gson();
    private static final String EXPERIMENT = "demo";
    private static final String CLUSTER = "cluster-a";
    private static final String CONTAINER = "tfb-server-0";
    private static final Timestamp START = Timestamp.from(Instant.parse("2026-01-15T00:00:00Z"));
    private static final Timestamp END = Timestamp.from(Instant.parse("2026-01-15T00:15:00Z"));

    @Test
    @DisplayName("container metrics round-trip through results jsonb and match the old converter")
    void containerRoundTripMatchesOldConverter() {
        HashMap<AnalyzerConstants.MetricName, Metric> metrics = new HashMap<>();
        metrics.put(AnalyzerConstants.MetricName.cpuUsage, metric("""
                {"name":"cpuUsage","results":{"aggregation_info":{"min":0.54,"max":0.94,"sum":0.52,"avg":0.12,"format":"cores"}}}
                """));
        metrics.put(AnalyzerConstants.MetricName.cpuRequest, metric("""
                {"name":"cpuRequest","results":{"aggregation_info":{"sum":3.4,"avg":2.1,"format":"cores"}}}
                """));
        ContainerData containerData = new ContainerData(CONTAINER, "kruize/tfb-db:1.15", null, metrics);
        HashMap<String, ContainerData> containers = new HashMap<>();
        containers.put(CONTAINER, containerData);
        K8sObject k8sObject = new K8sObject("tfb-qrh-deployment", "deployment", "default");
        k8sObject.setContainerDataMap(containers);

        KruizeResultsEntry stored = storedEntry(List.of(k8sObject));
        ExperimentResultData lean = DBHelpers.Converters.KruizeObjectConverters.experimentResultFromResultEntry(stored, CLUSTER);
        ExperimentResultData old = oldConverter(stored);

        assertNotNull(lean);
        assertMetric(lean, AnalyzerConstants.MetricName.cpuUsage, 0.12, "cores");
        assertMetric(lean, AnalyzerConstants.MetricName.cpuRequest, 2.1, "cores");
        assertMetric(old, AnalyzerConstants.MetricName.cpuUsage, 0.12, "cores");
        assertEquals(metricAt(old, AnalyzerConstants.MetricName.cpuUsage).getAggregationInfoResult().getAvg(),
                metricAt(lean, AnalyzerConstants.MetricName.cpuUsage).getAggregationInfoResult().getAvg());
        assertEquals(metricAt(old, AnalyzerConstants.MetricName.cpuUsage).getAggregationInfoResult().getFormat(),
                metricAt(lean, AnalyzerConstants.MetricName.cpuUsage).getAggregationInfoResult().getFormat());
        assertEquals(metricAt(old, AnalyzerConstants.MetricName.cpuRequest).getAggregationInfoResult().getFormat(),
                metricAt(lean, AnalyzerConstants.MetricName.cpuRequest).getAggregationInfoResult().getFormat());

        ContainerData experimentContainer = new ContainerData(CONTAINER, "kruize/tfb-db:1.15", null, new HashMap<>());
        KruizeObject kruizeObject = experimentWith(containerObject(experimentContainer));
        boolean attached = new ExperimentDBService().attachRecommendationResultEntries(
                Map.of(EXPERIMENT, kruizeObject), CLUSTER, List.of(stored));

        assertTrue(attached);
        IntervalResults intervalResults = experimentContainer.getResults().get(END);
        assertNotNull(intervalResults);
        assertEquals(0.12, intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.cpuUsage)
                .getAggregationInfoResult().getAvg());
        assertEquals("cores", intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.cpuUsage)
                .getAggregationInfoResult().getFormat());
    }

    @Test
    @DisplayName("namespace metric round-trip matches the old converter")
    void namespaceRoundTripMatchesOldConverter() {
        HashMap<AnalyzerConstants.MetricName, Metric> metrics = new HashMap<>();
        metrics.put(AnalyzerConstants.MetricName.namespaceCpuUsage, metric("""
                {"name":"namespaceCpuUsage","results":{"aggregation_info":{"sum":3.0,"avg":1.5,"format":"cores"}}}
                """));
        NamespaceData namespaceData = new NamespaceData("payments", null, metrics);
        HashMap<String, NamespaceData> namespaces = new HashMap<>();
        namespaces.put("payments", namespaceData);
        K8sObject k8sObject = new K8sObject();
        k8sObject.setNamespace("payments");
        k8sObject.setNamespaceDataMap(namespaces);

        KruizeResultsEntry stored = storedEntry(List.of(k8sObject));
        ExperimentResultData lean = DBHelpers.Converters.KruizeObjectConverters.experimentResultFromResultEntry(stored, CLUSTER);
        ExperimentResultData old = oldConverter(stored);

        assertNotNull(lean);
        MetricResults leanMetric = namespaceMetric(lean);
        MetricResults oldMetric = namespaceMetric(old);
        assertEquals(1.5, leanMetric.getAggregationInfoResult().getAvg());
        assertEquals("cores", leanMetric.getAggregationInfoResult().getFormat());
        assertEquals(oldMetric.getAggregationInfoResult().getAvg(), leanMetric.getAggregationInfoResult().getAvg());
        assertEquals(oldMetric.getAggregationInfoResult().getFormat(), leanMetric.getAggregationInfoResult().getFormat());

        NamespaceData experimentNamespace = new NamespaceData("payments", null, new HashMap<>());
        K8sObject experimentObject = new K8sObject();
        experimentObject.setNamespace("payments");
        HashMap<String, NamespaceData> experimentNamespaces = new HashMap<>();
        experimentNamespaces.put("payments", experimentNamespace);
        experimentObject.setNamespaceDataMap(experimentNamespaces);
        boolean attached = new ExperimentDBService().attachRecommendationResultEntries(
                Map.of(EXPERIMENT, experimentWith(experimentObject)), CLUSTER, List.of(stored));

        assertTrue(attached);
        IntervalResults intervalResults = experimentNamespace.getResults().get(END);
        assertNotNull(intervalResults);
        assertEquals(3.0, intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.namespaceCpuUsage)
                .getAggregationInfoResult().getSum());
    }

    @Test
    @DisplayName("empty result list does not attach")
    void emptyEntryListReturnsFalse() {
        ContainerData containerData = new ContainerData(CONTAINER, "kruize/tfb-db:1.15", null, new HashMap<>());
        KruizeObject kruizeObject = experimentWith(containerObject(containerData));

        boolean attached = new ExperimentDBService().attachRecommendationResultEntries(
                Map.of(EXPERIMENT, kruizeObject), CLUSTER, List.of());

        assertFalse(attached);
        assertNull(containerData.getResults());
    }

    @Test
    @DisplayName("recommendation JSON is compact and dates stay UTC")
    void compactRecommendationJsonDropsPrettyPrint() {
        Map<String, Date> payload = new HashMap<>();
        payload.put("monitoring_end_time", new Date(0L));

        String json = DBHelpers.Converters.KruizeObjectConverters.compactRecommendationJson(payload);

        assertFalse(json.contains("\n"));
        assertTrue(json.contains("1970-01-01T00:00:00.000Z"));
    }

    private static ExperimentResultData oldConverter(KruizeResultsEntry stored) {
        List<UpdateResultsAPIObject> apiObjects = DBHelpers.Converters.KruizeObjectConverters
                .convertResultEntryToUpdateResultsAPIObject(List.of(stored));
        UpdateResultsAPIObject apiObject = apiObjects.get(0);
        KruizeObject kruizeObject = new KruizeObject();
        kruizeObject.setClusterName(CLUSTER);
        apiObject.setKruizeObject(kruizeObject);
        return com.autotune.analyzer.serviceObjects.Converters.KruizeObjectConverters
                .convertUpdateResultsAPIObjToExperimentResultData(apiObject);
    }

    private static void assertMetric(ExperimentResultData data, AnalyzerConstants.MetricName name, double avg, String format) {
        MetricResults metricResults = metricAt(data, name);
        assertNotNull(metricResults);
        assertEquals(avg, metricResults.getAggregationInfoResult().getAvg());
        assertEquals(format, metricResults.getAggregationInfoResult().getFormat());
    }

    private static MetricResults metricAt(ExperimentResultData data, AnalyzerConstants.MetricName name) {
        ContainerData containerData = data.getKubernetes_objects().get(0).getContainerDataMap().get(CONTAINER);
        assertNotNull(containerData.getResults());
        IntervalResults intervalResults = containerData.getResults().get(END);
        assertNotNull(intervalResults);
        return intervalResults.getMetricResultsMap().get(name);
    }

    private static MetricResults namespaceMetric(ExperimentResultData data) {
        NamespaceData namespaceData = data.getKubernetes_objects().get(0).getNamespaceDataMap().get("payments");
        IntervalResults intervalResults = namespaceData.getResults().get(END);
        assertNotNull(intervalResults);
        return intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.namespaceCpuUsage);
    }

    private static KruizeResultsEntry storedEntry(List<K8sObject> kubernetesObjects) {
        ExperimentResultData experimentResultData = new ExperimentResultData();
        experimentResultData.setVersion("v2.0");
        experimentResultData.setExperiment_name(EXPERIMENT);
        experimentResultData.setCluster_name(CLUSTER);
        experimentResultData.setIntervalStartTime(START);
        experimentResultData.setIntervalEndTime(END);
        experimentResultData.setKubernetes_objects(kubernetesObjects);
        KruizeResultsEntry stored = DBHelpers.Converters.KruizeObjectConverters
                .convertExperimentResultToExperimentResultsTable(experimentResultData);
        assertNotNull(stored);
        assertNotNull(stored.getExtended_data());
        return stored;
    }

    private static Metric metric(String json) {
        return GSON.fromJson(json, Metric.class);
    }

    private static KruizeObject experimentWith(K8sObject k8sObject) {
        KruizeObject kruizeObject = new KruizeObject();
        kruizeObject.setExperimentName(EXPERIMENT);
        kruizeObject.setKubernetes_objects(List.of(k8sObject));
        return kruizeObject;
    }

    private static K8sObject containerObject(ContainerData containerData) {
        K8sObject k8sObject = new K8sObject("tfb-qrh-deployment", "deployment", "default");
        HashMap<String, ContainerData> containers = new HashMap<>();
        containers.put(CONTAINER, containerData);
        k8sObject.setContainerDataMap(containers);
        return k8sObject;
    }
}
