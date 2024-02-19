package com.autotune.jobs;

import com.autotune.analyzer.exceptions.K8sTypeNotSupportedException;
import com.autotune.analyzer.exceptions.MonitoringAgentNotFoundException;
import com.autotune.analyzer.exceptions.MonitoringAgentNotSupportedException;
import com.autotune.database.dao.ExperimentDAOImpl;
import com.autotune.operator.InitializeDeployment;
import com.autotune.operator.KruizeDeploymentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RetentionPartition {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetentionPartition.class);

    public static void main(String[] args) {
        LOGGER.info("RetentionPartition");
        try {
            InitializeDeployment.setup_deployment_info();
            new ExperimentDAOImpl().deletePartitions(KruizeDeploymentInfo.delete_partition_threshold_in_days);
        } catch (Exception | K8sTypeNotSupportedException | MonitoringAgentNotSupportedException |
                 MonitoringAgentNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
