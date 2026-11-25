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
package com.autotune.database.helper;

import com.autotune.analyzer.recommendations.ContainerRecommendations;
import com.autotune.analyzer.recommendations.NamespaceRecommendations;
import com.autotune.analyzer.recommendations.objects.MappedRecommendationForTimestamp;
import com.autotune.analyzer.serviceObjects.ContainerAPIObject;
import com.autotune.analyzer.serviceObjects.KubernetesAPIObject;
import com.autotune.analyzer.serviceObjects.ListRecommendationsAPIObject;
import com.autotune.analyzer.serviceObjects.NamespaceAPIObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Timestamp selection for the DB copy of a prepared recommendation list.
 * Must match getListRecommendationAPIObjectForDB: namespace needs an exact match,
 * containers fall back to their latest timestamp.
 */
public class RecommendationListForDbTest {

    private static final Timestamp OLDER = Timestamp.from(Instant.parse("2024-01-01T00:00:00Z"));
    private static final Timestamp CURRENT = Timestamp.from(Instant.parse("2024-01-01T00:15:00Z"));

    @Test
    @DisplayName("namespace recommendation without the requested timestamp is not stored")
    public void namespaceWithoutRequestedTimestampIsSkipped() {
        ListRecommendationsAPIObject source = namespaceList(OLDER);

        assertNull(DBHelpers.Converters.KruizeObjectConverters.recommendationListForDb(source, CURRENT));
    }

    @Test
    @DisplayName("namespace recommendation keeps only the requested timestamp")
    public void namespaceKeepsOnlyRequestedTimestamp() {
        ListRecommendationsAPIObject source = namespaceList(OLDER, CURRENT);

        ListRecommendationsAPIObject filtered = DBHelpers.Converters.KruizeObjectConverters.recommendationListForDb(source, CURRENT);

        assertNotNull(filtered);
        NamespaceRecommendations stored = filtered.getKubernetesObjects().get(0).getNamespaceAPIObject().getNamespaceRecommendations();
        assertEquals(Set.of(CURRENT), stored.getData().keySet());
        // the prepared list is still used for the response, so it must keep both timestamps
        assertEquals(Set.of(OLDER, CURRENT),
                source.getKubernetesObjects().get(0).getNamespaceAPIObject().getNamespaceRecommendations().getData().keySet());
    }

    @Test
    @DisplayName("container recommendation falls back to its latest timestamp")
    public void containerFallsBackToLatest() {
        ListRecommendationsAPIObject source = containerList(OLDER);

        ListRecommendationsAPIObject filtered = DBHelpers.Converters.KruizeObjectConverters.recommendationListForDb(source, CURRENT);

        assertNotNull(filtered);
        ContainerRecommendations stored = filtered.getKubernetesObjects().get(0).getContainerAPIObjects().get(0).getContainerRecommendations();
        assertEquals(Set.of(OLDER), stored.getData().keySet());
    }

    private static ListRecommendationsAPIObject namespaceList(Timestamp... timestamps) {
        NamespaceRecommendations recommendations = new NamespaceRecommendations();
        recommendations.setData(data(timestamps));
        KubernetesAPIObject k8sObject = new KubernetesAPIObject(null, null, "payments");
        k8sObject.setNamespaceAPIObject(new NamespaceAPIObject("payments", recommendations, null));
        k8sObject.setContainerAPIObjects(new ArrayList<>());
        return list(AnalyzerConstants.ExperimentType.NAMESPACE, k8sObject);
    }

    private static ListRecommendationsAPIObject containerList(Timestamp... timestamps) {
        ContainerRecommendations recommendations = new ContainerRecommendations();
        recommendations.setData(data(timestamps));
        KubernetesAPIObject k8sObject = new KubernetesAPIObject("demo-app", "deployment", "default");
        k8sObject.setContainerAPIObjects(List.of(new ContainerAPIObject("app", "image:latest", recommendations, null)));
        return list(AnalyzerConstants.ExperimentType.CONTAINER, k8sObject);
    }

    private static ListRecommendationsAPIObject list(AnalyzerConstants.ExperimentType type, KubernetesAPIObject k8sObject) {
        ListRecommendationsAPIObject list = new ListRecommendationsAPIObject();
        list.setExperimentName("demo-exp");
        list.setClusterName("cluster");
        list.setExperimentType(type);
        list.setKubernetesObjects(List.of(k8sObject));
        return list;
    }

    private static HashMap<Timestamp, MappedRecommendationForTimestamp> data(Timestamp... timestamps) {
        HashMap<Timestamp, MappedRecommendationForTimestamp> data = new HashMap<>();
        for (Timestamp timestamp : timestamps) {
            data.put(timestamp, new MappedRecommendationForTimestamp());
        }
        return data;
    }
}
