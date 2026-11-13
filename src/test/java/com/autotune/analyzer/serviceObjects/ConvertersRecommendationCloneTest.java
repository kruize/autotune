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
package com.autotune.analyzer.serviceObjects;

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.recommendations.ContainerRecommendations;
import com.autotune.analyzer.recommendations.NamespaceRecommendations;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * Recommendation response conversion clones only when it will filter the maps.
 */
public class ConvertersRecommendationCloneTest {

    @Test
    @DisplayName("does not clone when neither timestamp nor latest is requested")
    public void skipsCloneWhenNotFiltering() {
        KruizeObject kruizeObject = containerExperiment();
        try (var utils = mockStatic(Utils.class)) {
            Converters.KruizeObjectConverters.convertKruizeObjectToListRecommendationSO(
                    kruizeObject, false, false, timestamp());

            utils.verify(() -> Utils.getClone(any(), eq(ContainerData.class)), never());
            utils.verify(() -> Utils.getClone(any(), eq(NamespaceData.class)), never());
        }
    }

    @Test
    @DisplayName("clones container data when latest recommendations are requested")
    public void clonesContainerWhenLatest() {
        KruizeObject kruizeObject = containerExperiment();
        try (var utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getClone(any(ContainerData.class), eq(ContainerData.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Converters.KruizeObjectConverters.convertKruizeObjectToListRecommendationSO(
                    kruizeObject, true, false, timestamp());

            utils.verify(() -> Utils.getClone(any(ContainerData.class), eq(ContainerData.class)), times(1));
        }
    }

    @Test
    @DisplayName("clones namespace data when a timestamp filter is requested")
    public void clonesNamespaceWhenCheckingTimestamp() {
        KruizeObject kruizeObject = namespaceExperiment();
        try (var utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getClone(any(NamespaceData.class), eq(NamespaceData.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Converters.KruizeObjectConverters.convertKruizeObjectToListRecommendationSO(
                    kruizeObject, false, true, timestamp());

            utils.verify(() -> Utils.getClone(any(NamespaceData.class), eq(NamespaceData.class)), times(1));
            utils.verify(() -> Utils.getClone(any(ContainerData.class), eq(ContainerData.class)), never());
        }
    }

    private static KruizeObject containerExperiment() {
        K8sObject k8sObject = new K8sObject("demo-app", "deployment", "default");
        HashMap<String, ContainerData> containers = new HashMap<>();
        containers.put("app", new ContainerData("app", "image:latest", new ContainerRecommendations(), null));
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
        namespaces.put("payments", new NamespaceData("payments", new NamespaceRecommendations(), null));
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
}
