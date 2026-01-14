package com.autotune.analyzer.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.autotune.analyzer.serviceObjects.BulkInput;
import com.autotune.analyzer.serviceObjects.BulkJobStatus;
import com.autotune.analyzer.workerimpl.BulkJobManager;
import com.autotune.common.data.dataSourceMetadata.DataSourceCluster;
import com.autotune.common.data.dataSourceMetadata.DataSourceContainer;
import com.autotune.common.data.dataSourceMetadata.DataSourceNamespace;
import com.autotune.common.data.dataSourceMetadata.DataSourceWorkload;
import com.autotune.operator.KruizeDeploymentInfo;

class BulkJobManagerMockedTest {

    private BulkJobManager bulkJobManager;
    private BulkInput bulkInput;
    private BulkJobStatus jobStatus;

    @BeforeEach
    void setup() {
       
        bulkInput = mock(BulkInput.class);
        jobStatus = mock(BulkJobStatus.class);

        when(bulkInput.getDatasource()).thenReturn("prometheus");

        KruizeDeploymentInfo.experiment_name_format =
                "%datasource%-%clustername%-%namespace%-%workloadname%-%workloadtype%-%containername%";

        bulkJobManager = new BulkJobManager(
                "job-123",
                jobStatus,
                bulkInput
        );
    }

    @Test
    void testFrameExperimentNameBasicSuccess() {
        
        DataSourceCluster cluster = mock(DataSourceCluster.class);
        when(cluster.getDataSourceClusterName()).thenReturn("cluster1");

        DataSourceNamespace namespace = mock(DataSourceNamespace.class);
        when(namespace.getNamespace()).thenReturn("default");

        DataSourceWorkload workload = mock(DataSourceWorkload.class);
        when(workload.getWorkloadName()).thenReturn("sysbench");
        when(workload.getWorkloadType()).thenReturn("deployment");

        DataSourceContainer container = mock(DataSourceContainer.class);
        when(container.getContainerName()).thenReturn("sysbench");

        String experimentName = bulkJobManager.frameExperimentName(
                null,
                cluster,
                namespace,
                workload,
                container
        );

        assertEquals(
                "prometheus-cluster1-default-sysbench-deployment-sysbench",
                experimentName
        );
    }

    @Test
    void testFrameExperimentNameWithLabels() {
    
        DataSourceCluster cluster = mock(DataSourceCluster.class);
        when(cluster.getDataSourceClusterName()).thenReturn("cluster1");

        DataSourceNamespace namespace = mock(DataSourceNamespace.class);
        when(namespace.getNamespace()).thenReturn("default");

        DataSourceWorkload workload = mock(DataSourceWorkload.class);
        when(workload.getWorkloadName()).thenReturn("sysbench");
        when(workload.getWorkloadType()).thenReturn("deployment");

        DataSourceContainer container = mock(DataSourceContainer.class);
        when(container.getContainerName()).thenReturn("sysbench");

        KruizeDeploymentInfo.experiment_name_format =
                "%datasource%-%clustername%-%namespace%-%workloadname%-%workloadtype%-%containername%-%label:env%-%label:version%";

        String labelString = "env=prod,version=v1.2";

        String experimentName = bulkJobManager.frameExperimentName(
                labelString,
                cluster,
                namespace,
                workload,
                container
        );

        assertEquals(
                "prometheus-cluster1-default-sysbench-deployment-sysbench-prod-v1.2",
                experimentName
        );
}

}
