package com.autotune.analyzer.workerimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.autotune.analyzer.serviceObjects.BulkInput;
import com.autotune.analyzer.serviceObjects.BulkJobStatus;
import com.autotune.common.data.dataSourceMetadata.DataSourceCluster;
import com.autotune.common.data.dataSourceMetadata.DataSourceContainer;
import com.autotune.common.data.dataSourceMetadata.DataSourceNamespace;
import com.autotune.common.data.dataSourceMetadata.DataSourceWorkload;
import com.autotune.operator.KruizeDeploymentInfo;


/**
 * Unit Test with Mocking Template Principles
 *
 * 1. One class under test:
 *    Each test class should focus on validating the behavior of a single class.
 *
 * 2. Mock only direct dependencies:
 *    External collaborators are mocked using Mockito to keep tests isolated,
 *    fast, and deterministic.
 *
 * 3. No static or global side effects:
 *    Tests must not depend on or mutate shared global state to avoid flakiness
 *    when executed in parallel.
 *
 * 4. Clear Given–When–Then structure:
 *    - Given: test setup and mocked behavior
 *    - When: execution of the method under test
 *    - Then: assertions on the expected outcome
 *
 * 5. Deterministic assertions:
 *    Assertions must be predictable and repeatable across environments
 *    and test runs.
 */

class BulkJobManagerMockedTest {

    private BulkJobManager bulkJobManager;
    private BulkInput bulkInput;
    private BulkJobStatus jobStatus;

    private DataSourceCluster cluster;
    private DataSourceNamespace namespace;
    private DataSourceWorkload workload;
    private DataSourceContainer container;

    @BeforeEach
    void setup() {
        bulkInput = mock(BulkInput.class);
        jobStatus = mock(BulkJobStatus.class);

        when(bulkInput.getDatasource()).thenReturn("prometheus");

        cluster = mock(DataSourceCluster.class);
        when(cluster.getDataSourceClusterName()).thenReturn("cluster1");

        namespace = mock(DataSourceNamespace.class);
        when(namespace.getNamespace()).thenReturn("default");

        workload = mock(DataSourceWorkload.class);
        when(workload.getWorkloadName()).thenReturn("sysbench");
        when(workload.getWorkloadType()).thenReturn("deployment");

        container = mock(DataSourceContainer.class);
        when(container.getContainerName()).thenReturn("sysbench");

        KruizeDeploymentInfo.experiment_name_format =
                "%datasource%-%clustername%-%namespace%-%workloadname%-%workloadtype%-%containername%";

        bulkJobManager = new BulkJobManager("job-123", jobStatus, bulkInput);
    }

    @Test
    @DisplayName("Frame experiment name without labels")
    void shouldFrameExperimentNameWithoutLabels() {
        // When
        String experimentName = bulkJobManager.frameExperimentName(
                null, cluster, namespace, workload, container
        );

        // Then
        assertEquals(
                "prometheus-cluster1-default-sysbench-deployment-sysbench",
                experimentName
        );
    }

    @Test
    @DisplayName("Frame experiment name with labels")
    void shouldFrameExperimentNameWithLabels() {
        // Given
        KruizeDeploymentInfo.experiment_name_format =
                "%datasource%-%clustername%-%namespace%-%workloadname%-%workloadtype%-%containername%-%label:env%-%label:version%";

        String labelString = "env=prod,version=v1.2";

        // When
        String experimentName = bulkJobManager.frameExperimentName(
                labelString, cluster, namespace, workload, container
        );

        // Then
        assertEquals(
                "prometheus-cluster1-default-sysbench-deployment-sysbench-prod-v1.2",
                experimentName
        );
    }
}
