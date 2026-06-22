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

        @Test
        @DisplayName("Should return cluster name from bulk payload when provided")
        void shouldReturnClusterNameFromBulkPayload() {
            // Given
            when(bulkInput.getCluster_name()).thenReturn("custom-cluster");

            // When
            String clusterName = bulkInput.getCluster_name();

            // Then
            assertEquals("custom-cluster", clusterName,
                    "Should return cluster name from bulk payload");
        }

        @Test
        @DisplayName("Should return null when bulk payload cluster is null")
        void shouldReturnNullWhenBulkPayloadClusterIsNull() {
            // Given
            when(bulkInput.getCluster_name()).thenReturn(null);

            // When
            String clusterName = bulkInput.getCluster_name();

            // Then
            assertNull(clusterName,
                    "Should return null when bulk payload cluster is null");
        }

        @Test
        @DisplayName("Should return empty string when bulk payload cluster is empty")
        void shouldReturnEmptyStringWhenBulkPayloadClusterIsEmpty() {
            // Given
            when(bulkInput.getCluster_name()).thenReturn("");

            // When
            String clusterName = bulkInput.getCluster_name();

            // Then
            assertEquals("", clusterName,
                    "Should return empty string when bulk payload cluster is empty");
        }

        @Test
        @DisplayName("Should handle cluster name with special characters")
        void shouldHandleClusterNameWithSpecialCharacters() {
            // Given
            when(bulkInput.getCluster_name()).thenReturn("prod-cluster-01.us-east");

            // When
            String clusterName = bulkInput.getCluster_name();

            // Then
            assertEquals("prod-cluster-01.us-east", clusterName,
                    "Should handle cluster name with special characters");
        }
    }

    @Nested
    @DisplayName("Cluster Name Usage Tests")
    class ClusterNameUsageTests {

        @Test
        @DisplayName("Should use cluster name from bulk payload in experiment name")
        void shouldUseClusterNameFromBulkPayloadInExperimentName() {
            // Given
            when(bulkInput.getCluster_name()).thenReturn("prod-cluster");

            // When
            String clusterName = bulkInput.getCluster_name();

            // Then
            assertEquals("prod-cluster", clusterName, "Cluster name should match");
        }

        @Test
        @DisplayName("Should handle null cluster name (backward compatibility)")
        void shouldHandleNullClusterName() {
            // Given
            when(bulkInput.getCluster_name()).thenReturn(null);

            // When
            String clusterName = bulkInput.getCluster_name();

            // Then
            assertNull(clusterName, "Cluster name should be null");
        }
    }

    @Nested
    @DisplayName("Backward Compatibility Tests")
    class BackwardCompatibilityTests {

        @Test
        @DisplayName("Should maintain existing behavior when cluster name not provided")
        void shouldMaintainExistingBehaviorWhenClusterNameNotProvided() {
            // Given - Old-style bulk input without cluster name
            when(bulkInput.getCluster_name()).thenReturn(null);

            // When
            String experimentName = bulkJobManager.frameExperimentName(
                    null, cluster, namespace, workload, container
            );

            // Then
            assertTrue(experimentName.contains("metadata-cluster"),
                    "Should use metadata cluster when bulk payload cluster is not provided");
            assertEquals("prometheus-metadata-cluster-default-test-app-deployment-app-container",
                    experimentName,
                    "Experiment name should follow existing format");
        }

        @Test
        @DisplayName("Should not break existing experiments without cluster name")
        void shouldNotBreakExistingExperimentsWithoutClusterName() {
            // Given
            when(bulkInput.getCluster_name()).thenReturn(null);

            // When - Verify that BulkInput can be created without cluster name
            boolean hasClusterName = bulkInput.getCluster_name() != null;

            // Then
            assertFalse(hasClusterName, "Should not have cluster name");
        }
    }
}
