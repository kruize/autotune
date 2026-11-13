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
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.common.data.result.NamespaceData;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.database.helper.DBHelpers;
import com.autotune.database.table.KruizeResultsEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String EXPERIMENT = "demo";
    private static final Timestamp START = Timestamp.from(Instant.parse("2026-01-15T00:00:00Z"));
    private static final Timestamp END = Timestamp.from(Instant.parse("2026-01-15T00:15:00Z"));

    @Test
    @DisplayName("container metric lands on the row end timestamp")
    void attachesContainerMetricFromStoredJson() throws Exception {
        ContainerData containerData = new ContainerData("app", "image:latest", null, new HashMap<>());
        KruizeObject kruizeObject = experimentWith(containerObject(containerData));
        KruizeResultsEntry entry = entry("""
                {
                  "kubernetes_objects": [
                    {
                      "type": "deployment",
                      "name": "demo-app",
                      "namespace": "default",
                      "containers": {
                        "app": {
                          "container_name": "app",
                          "container_image_name": "image:latest",
                          "metrics": {
                            "cpuUsage": {
                              "name": "cpuUsage",
                              "results": {
                                "aggregation_info": { "sum": 12.5 }
                              }
                            }
                          }
                        }
                      }
                    }
                  ]
                }
                """);

        boolean attached = new ExperimentDBService().attachRecommendationResultEntries(
                Map.of(EXPERIMENT, kruizeObject), null, List.of(entry));

        assertTrue(attached);
        IntervalResults intervalResults = containerData.getResults().get(END);
        assertNotNull(intervalResults);
        MetricResults metricResults = intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.cpuUsage);
        assertNotNull(metricResults);
        assertEquals(12.5, metricResults.getAggregationInfoResult().getSum());
    }

    @Test
    @DisplayName("namespace metric lands on NamespaceData results")
    void attachesNamespaceMetricFromStoredJson() throws Exception {
        NamespaceData namespaceData = new NamespaceData("payments", null, new HashMap<>());
        K8sObject k8sObject = new K8sObject();
        k8sObject.setNamespace("payments");
        HashMap<String, NamespaceData> namespaces = new HashMap<>();
        namespaces.put("payments", namespaceData);
        k8sObject.setNamespaceDataMap(namespaces);
        KruizeObject kruizeObject = experimentWith(k8sObject);
        KruizeResultsEntry entry = entry("""
                {
                  "kubernetes_objects": [
                    {
                      "namespace": "payments",
                      "namespaces": {
                        "payments": {
                          "namespace": "payments",
                          "metrics": {
                            "namespaceCpuUsage": {
                              "name": "namespaceCpuUsage",
                              "results": {
                                "aggregation_info": { "sum": 3.0 }
                              }
                            }
                          }
                        }
                      }
                    }
                  ]
                }
                """);

        boolean attached = new ExperimentDBService().attachRecommendationResultEntries(
                Map.of(EXPERIMENT, kruizeObject), null, List.of(entry));

        assertTrue(attached);
        IntervalResults intervalResults = namespaceData.getResults().get(END);
        assertNotNull(intervalResults);
        assertTrue(intervalResults.getMetricResultsMap().containsKey(AnalyzerConstants.MetricName.namespaceCpuUsage));
        assertEquals(3.0, intervalResults.getMetricResultsMap()
                .get(AnalyzerConstants.MetricName.namespaceCpuUsage)
                .getAggregationInfoResult()
                .getSum());
    }

    @Test
    @DisplayName("empty result list does not attach")
    void emptyEntryListReturnsFalse() {
        ContainerData containerData = new ContainerData("app", "image:latest", null, new HashMap<>());
        KruizeObject kruizeObject = experimentWith(containerObject(containerData));

        boolean attached = new ExperimentDBService().attachRecommendationResultEntries(
                Map.of(EXPERIMENT, kruizeObject), null, List.of());

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

    private static KruizeObject experimentWith(K8sObject k8sObject) {
        KruizeObject kruizeObject = new KruizeObject();
        kruizeObject.setExperimentName(EXPERIMENT);
        kruizeObject.setKubernetes_objects(List.of(k8sObject));
        return kruizeObject;
    }

    private static K8sObject containerObject(ContainerData containerData) {
        K8sObject k8sObject = new K8sObject("demo-app", "deployment", "default");
        HashMap<String, ContainerData> containers = new HashMap<>();
        containers.put("app", containerData);
        k8sObject.setContainerDataMap(containers);
        return k8sObject;
    }

    private static KruizeResultsEntry entry(String extendedData) throws Exception {
        KruizeResultsEntry entry = new KruizeResultsEntry();
        entry.setExperiment_name(EXPERIMENT);
        entry.setInterval_start_time(START);
        entry.setInterval_end_time(END);
        entry.setExtended_data(MAPPER.readTree(extendedData));
        return entry;
    }
}
