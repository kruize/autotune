/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.autotune.analyzer.serviceObjects;

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.recommendations.ContainerRecommendations;
import com.autotune.analyzer.recommendations.NamespaceRecommendations;
import com.autotune.analyzer.recommendations.objects.MappedRecommendationForTimestamp;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.NamespaceData;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.utils.Utils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * Recommendation response conversion clones by default. Only a caller that opts out
 * (updateRecommendations) skips the copy, and only when nothing is filtered. Filtering
 * always works on a clone, not the stored data.
 */
public class ConvertersRecommendationCloneTest {

    @Test
    @DisplayName("does not clone when the caller opts out and nothing is filtered (updateRecommendations)")
    public void skipsCloneWhenNotFiltering() {
        KruizeObject kruizeObject = containerExperiment();
        try (var utils = mockStatic(Utils.class)) {
            Converters.KruizeObjectConverters.convertKruizeObjectToListRecommendationSO(
                    kruizeObject, false, false, timestamp(), false);

            utils.verify(() -> Utils.getClone(any(), eq(ContainerData.class)), never());
            utils.verify(() -> Utils.getClone(any(), eq(NamespaceData.class)), never());
        }
    }

    @Test
    @DisplayName("the default conversion still clones container data even when nothing is filtered")
    public void defaultConversionClonesContainer() {
        KruizeObject kruizeObject = containerExperiment();
        try (var utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getClone(any(ContainerData.class), eq(ContainerData.class)))
                    .thenAnswer(invocation -> copyOf(invocation.getArgument(0, ContainerData.class)));

            ListRecommendationsAPIObject converted = Converters.KruizeObjectConverters.convertKruizeObjectToListRecommendationSO(
                    kruizeObject, false, false, null);

            utils.verify(() -> Utils.getClone(any(ContainerData.class), eq(ContainerData.class)), times(1));
            ContainerRecommendations stored = kruizeObject.getKubernetes_objects().get(0).getContainerDataMap().get("app")
                    .getContainerRecommendations();
            assertNotSame(stored, converted.getKubernetesObjects().get(0).getContainerAPIObjects().get(0)
                    .getContainerRecommendations());
        }
    }

    @Test
    @DisplayName("the default conversion still clones namespace data even when nothing is filtered")
    public void defaultConversionClonesNamespace() {
        KruizeObject kruizeObject = namespaceExperiment();
        try (var utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getClone(any(NamespaceData.class), eq(NamespaceData.class)))
                    .thenAnswer(invocation -> copyOf(invocation.getArgument(0, NamespaceData.class)));

            ListRecommendationsAPIObject converted = Converters.KruizeObjectConverters.convertKruizeObjectToListRecommendationSO(
                    kruizeObject, false, false, null);

            utils.verify(() -> Utils.getClone(any(NamespaceData.class), eq(NamespaceData.class)), times(1));
            NamespaceRecommendations stored = kruizeObject.getKubernetes_objects().get(0).getNamespaceDataMap().get("payments")
                    .getNamespaceRecommendations();
            assertNotSame(stored, converted.getKubernetesObjects().get(0).getNamespaceAPIObject().getNamespaceRecommendations());
        }
    }

    @Test
    @DisplayName("clones container data when latest recommendations are requested")
    public void clonesContainerWhenLatest() {
        KruizeObject kruizeObject = containerExperiment();
        try (var utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getClone(any(ContainerData.class), eq(ContainerData.class)))
                    .thenAnswer(invocation -> copyOf(invocation.getArgument(0, ContainerData.class)));

            ListRecommendationsAPIObject converted = Converters.KruizeObjectConverters.convertKruizeObjectToListRecommendationSO(
                    kruizeObject, true, false, timestamp());

            utils.verify(() -> Utils.getClone(any(ContainerData.class), eq(ContainerData.class)), times(1));
            assertEquals(Set.of(timestamp()), converted.getKubernetesObjects().get(0).getContainerAPIObjects().get(0)
                    .getContainerRecommendations().getData().keySet());
            assertEquals(Set.of(olderTimestamp(), timestamp()), kruizeObject.getKubernetes_objects().get(0)
                    .getContainerDataMap().get("app").getContainerRecommendations().getData().keySet());
        }
    }

    @Test
    @DisplayName("clones namespace data when a timestamp filter is requested")
    public void clonesNamespaceWhenCheckingTimestamp() {
        KruizeObject kruizeObject = namespaceExperiment();
        try (var utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getClone(any(NamespaceData.class), eq(NamespaceData.class)))
                    .thenAnswer(invocation -> copyOf(invocation.getArgument(0, NamespaceData.class)));

            ListRecommendationsAPIObject converted = Converters.KruizeObjectConverters.convertKruizeObjectToListRecommendationSO(
                    kruizeObject, false, true, timestamp());

            utils.verify(() -> Utils.getClone(any(NamespaceData.class), eq(NamespaceData.class)), times(1));
            utils.verify(() -> Utils.getClone(any(ContainerData.class), eq(ContainerData.class)), never());
            assertEquals(Set.of(timestamp()), converted.getKubernetesObjects().get(0).getNamespaceAPIObject()
                    .getNamespaceRecommendations().getData().keySet());
            assertEquals(Set.of(olderTimestamp(), timestamp()), kruizeObject.getKubernetes_objects().get(0)
                    .getNamespaceDataMap().get("payments").getNamespaceRecommendations().getData().keySet());
        }
    }

    private static KruizeObject containerExperiment() {
        K8sObject k8sObject = new K8sObject("demo-app", "deployment", "default");
        HashMap<String, ContainerData> containers = new HashMap<>();
        ContainerRecommendations recommendations = new ContainerRecommendations();
        recommendations.setData(twoTimestamps());
        containers.put("app", new ContainerData("app", "image:latest", recommendations, null));
        k8sObject.setContainerDataMap(containers);

        KruizeObject kruizeObject = mock(KruizeObject.class);
        when(kruizeObject.getExperimentName()).thenReturn("demo-exp");
        when(kruizeObject.getClusterName()).thenReturn("cluster");
        when(kruizeObject.isNamespaceExperiment()).thenReturn(false);
        when(kruizeObject.getKubernetes_objects()).thenReturn(List.of(k8sObject));
        return kruizeObject;
    }

    private static KruizeObject namespaceExperiment() {
        K8sObject k8sObject = new K8sObject(null, null, "payments");
        k8sObject.setContainerDataMap(new HashMap<>());
        HashMap<String, NamespaceData> namespaces = new HashMap<>();
        NamespaceRecommendations recommendations = new NamespaceRecommendations();
        recommendations.setData(twoTimestamps());
        namespaces.put("payments", new NamespaceData("payments", recommendations, null));
        k8sObject.setNamespaceDataMap(namespaces);

        KruizeObject kruizeObject = mock(KruizeObject.class);
        when(kruizeObject.getExperimentName()).thenReturn("demo-exp");
        when(kruizeObject.getClusterName()).thenReturn("cluster");
        when(kruizeObject.isNamespaceExperiment()).thenReturn(true);
        when(kruizeObject.getKubernetes_objects()).thenReturn(List.of(k8sObject));
        return kruizeObject;
    }

    private static Timestamp timestamp() {
        return Timestamp.from(Instant.parse("2024-01-01T00:15:00Z"));
    }

    private static Timestamp olderTimestamp() {
        return Timestamp.from(Instant.parse("2024-01-01T00:00:00Z"));
    }

    private static HashMap<Timestamp, MappedRecommendationForTimestamp> twoTimestamps() {
        HashMap<Timestamp, MappedRecommendationForTimestamp> data = new HashMap<>();
        data.put(olderTimestamp(), new MappedRecommendationForTimestamp());
        data.put(timestamp(), new MappedRecommendationForTimestamp());
        return data;
    }

    // Stand-in for Utils.getClone: a separate object with its own recommendation map.
    private static ContainerData copyOf(ContainerData source) {
        ContainerRecommendations recommendations = new ContainerRecommendations();
        recommendations.setData(new HashMap<>(source.getContainerRecommendations().getData()));
        return new ContainerData(source.getContainer_name(), source.getContainer_image_name(), recommendations, null);
    }

    private static NamespaceData copyOf(NamespaceData source) {
        NamespaceRecommendations recommendations = new NamespaceRecommendations();
        recommendations.setData(new HashMap<>(source.getNamespaceRecommendations().getData()));
        return new NamespaceData(source.getNamespace_name(), recommendations, null);
    }
}
