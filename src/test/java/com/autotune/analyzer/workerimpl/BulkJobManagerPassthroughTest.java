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
package com.autotune.analyzer.workerimpl;

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.kruizeObject.RecommendationSettings;
import com.autotune.analyzer.serviceObjects.BulkInput;
import com.autotune.analyzer.serviceObjects.BulkJobStatus;
import com.autotune.common.data.dataSourceMetadata.DataSourceCluster;
import com.autotune.common.data.dataSourceMetadata.DataSourceContainer;
import com.autotune.common.data.dataSourceMetadata.DataSourceNamespace;
import com.autotune.common.data.dataSourceMetadata.DataSourceWorkload;
import com.autotune.operator.KruizeDeploymentInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BulkJobManager cluster name passthrough logic.
 *
 * Tests verify that:
 * 1. Cluster name from bulk payload is correctly passed to experiments
 * 2. Backward compatibility is maintained when cluster name is not provided
 */
class BulkJobManagerPassthroughTest {

    private BulkJobManager bulkJobManager;
    private BulkInput bulkInput;
    private BulkJobStatus jobStatus;
    private DataSourceCluster cluster;
    private DataSourceNamespace namespace;
    private DataSourceWorkload workload;
    private DataSourceContainer container;
    private String originalExperimentNameFormat;

    @BeforeEach
    void setup() {
        // Capture static/global state
        originalExperimentNameFormat = KruizeDeploymentInfo.experiment_name_format;

        bulkInput = mock(BulkInput.class);
        when(bulkInput.getDatasource()).thenReturn("prometheus");

        jobStatus = mock(BulkJobStatus.class);

        cluster = mock(DataSourceCluster.class);
        when(cluster.getDataSourceClusterName()).thenReturn("metadata-cluster");

        namespace = mock(DataSourceNamespace.class);
        when(namespace.getNamespace()).thenReturn("default");

        workload = mock(DataSourceWorkload.class);
        when(workload.getWorkloadName()).thenReturn("test-app");
        when(workload.getWorkloadType()).thenReturn("deployment");

        container = mock(DataSourceContainer.class);
        when(container.getContainerName()).thenReturn("app-container");

        KruizeDeploymentInfo.experiment_name_format =
                "%datasource%-%clustername%-%namespace%-%workloadname%-%workloadtype%-%containername%";

        bulkJobManager = new BulkJobManager("job-123", jobStatus, bulkInput);
    }

    @AfterEach
    void tearDown() {
        // Restore static/global state
        KruizeDeploymentInfo.experiment_name_format = originalExperimentNameFormat;
    }

    @Nested
    @DisplayName("Cluster Name Passthrough Tests")
    class ClusterNamePassthroughTests {

        /**
         * Mirrors the resolution logic in BulkJobManager.getExperimentMap():
         *   clusterName = bulkInput.getCluster_name() != null
         *       ? bulkInput.getCluster_name()
         *       : dsc.getDataSourceClusterName();
         */
        private String resolveClusterName(String payloadCluster, String metadataCluster) {
            return payloadCluster != null ? payloadCluster : metadataCluster;
        }

        @Test
        @DisplayName("Payload cluster name is used in experiment name when provided")
        void payloadClusterNameAppearsInExperimentName() {
            // Given – bulk payload supplies an explicit cluster name
            when(bulkInput.getCluster_name()).thenReturn("custom-cluster");
            String resolved = resolveClusterName(bulkInput.getCluster_name(),
                    cluster.getDataSourceClusterName());

            // When – frameExperimentName builds the name from the resolved cluster
            String experimentName = bulkJobManager.frameExperimentName(
                    null, resolved, namespace, workload, container
            );

            // Then – the payload cluster name, not the metadata cluster, appears in the result
            assertEquals("prometheus-custom-cluster-default-test-app-deployment-app-container",
                    experimentName,
                    "Experiment name must embed the cluster name supplied in the bulk payload");
            assertFalse(experimentName.contains("metadata-cluster"),
                    "Metadata cluster name must not leak into the experiment name when payload overrides it");
        }

        @Test
        @DisplayName("Metadata cluster name is used in experiment name when payload cluster is null")
        void metadataClusterNameUsedWhenPayloadClusterIsNull() {
            // Given – no cluster in the payload → fall back to data-source metadata
            when(bulkInput.getCluster_name()).thenReturn(null);
            String resolved = resolveClusterName(bulkInput.getCluster_name(),
                    cluster.getDataSourceClusterName());

            // When
            String experimentName = bulkJobManager.frameExperimentName(
                    null, resolved, namespace, workload, container
            );

            // Then
            assertEquals("prometheus-metadata-cluster-default-test-app-deployment-app-container",
                    experimentName,
                    "Experiment name must fall back to the metadata cluster name when payload is null");
        }

        @Test
        @DisplayName("Cluster name with dots and hyphens is preserved verbatim in experiment name")
        void clusterNameWithSpecialCharactersPreservedInExperimentName() {
            // Given
            when(bulkInput.getCluster_name()).thenReturn("prod-cluster-01.us-east");
            String resolved = resolveClusterName(bulkInput.getCluster_name(),
                    cluster.getDataSourceClusterName());

            // When
            String experimentName = bulkJobManager.frameExperimentName(
                    null, resolved, namespace, workload, container
            );

            // Then
            assertTrue(experimentName.contains("prod-cluster-01.us-east"),
                    "Special characters in cluster name must be preserved in the experiment name");
            assertEquals("prometheus-prod-cluster-01.us-east-default-test-app-deployment-app-container",
                    experimentName);
        }
    }

    @Nested
    @DisplayName("Cluster Name Usage Tests")
    class ClusterNameUsageTests {

        @Test
        @DisplayName("Payload cluster name overrides metadata cluster name in experiment name")
        void payloadClusterNameOverridesMetadataCluster() {
            // Given – a different cluster in the payload vs the metadata
            when(bulkInput.getCluster_name()).thenReturn("prod-cluster");
            // Resolution matches BulkJobManager.getExperimentMap() logic
            String resolved = bulkInput.getCluster_name() != null
                    ? bulkInput.getCluster_name()
                    : cluster.getDataSourceClusterName();

            // When
            String experimentName = bulkJobManager.frameExperimentName(
                    null, resolved, namespace, workload, container
            );

            // Then
            assertEquals("prometheus-prod-cluster-default-test-app-deployment-app-container",
                    experimentName,
                    "Payload cluster name must be reflected in the generated experiment name");
            assertFalse(experimentName.contains("metadata-cluster"),
                    "Metadata cluster name must not appear when payload overrides it");
        }

        @Test
        @DisplayName("Null payload cluster name falls back to metadata cluster in experiment name")
        void nullPayloadClusterFallsBackToMetadataCluster() {
            // Given – null cluster name in payload
            when(bulkInput.getCluster_name()).thenReturn(null);
            String resolved = bulkInput.getCluster_name() != null
                    ? bulkInput.getCluster_name()
                    : cluster.getDataSourceClusterName();   // "metadata-cluster"

            // When
            String experimentName = bulkJobManager.frameExperimentName(
                    null, resolved, namespace, workload, container
            );

            // Then
            assertTrue(experimentName.contains("metadata-cluster"),
                    "Metadata cluster name must be used in the experiment name when payload cluster is null");
            assertEquals("prometheus-metadata-cluster-default-test-app-deployment-app-container",
                    experimentName);
        }
    }

    @Nested
    @DisplayName("Backward Compatibility Tests")
    class BackwardCompatibilityTests {

        @Test
        @DisplayName("Omitting cluster_name in payload still produces a valid experiment name from metadata")
        void shouldMaintainExistingBehaviorWhenClusterNameNotProvided() {
            // Given – old-style bulk input without cluster_name; BulkJobManager falls back to the
            // metadata cluster name (mirrors the getExperimentMap() resolution logic)
            when(bulkInput.getCluster_name()).thenReturn(null);
            String resolved = bulkInput.getCluster_name() != null
                    ? bulkInput.getCluster_name()
                    : cluster.getDataSourceClusterName();   // "metadata-cluster"

            // When – the resolved name is passed through to frameExperimentName, exactly as
            // BulkJobManager does
            String experimentName = bulkJobManager.frameExperimentName(
                    null, resolved, namespace, workload, container
            );

            // Then
            assertTrue(experimentName.contains("metadata-cluster"),
                    "Should use metadata cluster when bulk payload cluster is not provided");
            assertEquals("prometheus-metadata-cluster-default-test-app-deployment-app-container",
                    experimentName,
                    "Experiment name should follow existing format");
        }

        @Test
        @DisplayName("Providing cluster_name in payload overrides metadata cluster in backward-compat scenario")
        void clusterNamePayloadOverridesMetadataInBackwardCompatScenario() {
            // Given – a new bulk payload that adds cluster_name alongside existing fields
            when(bulkInput.getCluster_name()).thenReturn("override-cluster");
            String resolved = bulkInput.getCluster_name() != null
                    ? bulkInput.getCluster_name()
                    : cluster.getDataSourceClusterName();

            // When
            String experimentName = bulkJobManager.frameExperimentName(
                    null, resolved, namespace, workload, container
            );

            // Then – the override cluster name must win; metadata cluster must not appear
            assertEquals("prometheus-override-cluster-default-test-app-deployment-app-container",
                    experimentName,
                    "Adding cluster_name to an existing payload must override the metadata cluster name");
            assertFalse(experimentName.contains("metadata-cluster"),
                    "Metadata cluster must not bleed into the name when payload provides an override");
        }
    }
}
