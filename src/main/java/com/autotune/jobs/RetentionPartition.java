package com.autotune.jobs;

import com.autotune.analyzer.exceptions.K8sTypeNotSupportedException;
import com.autotune.analyzer.exceptions.MonitoringAgentNotFoundException;
import com.autotune.analyzer.exceptions.MonitoringAgentNotSupportedException;
import com.autotune.database.init.KruizeHibernateUtil;
import com.autotune.operator.InitializeDeployment;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RetentionPartition {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetentionPartition.class);

    public static void main(String[] args) {
        LOGGER.info("Checking Liveliness probe DB connection...");
        try {
            InitializeDeployment.setup_deployment_info();
        } catch (Exception | K8sTypeNotSupportedException | MonitoringAgentNotSupportedException |
                 MonitoringAgentNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
        SessionFactory factory = KruizeHibernateUtil.getSessionFactory();
        
        Session session = factory.openSession();

        session.close();
        LOGGER.info("DB Liveliness probe connection successful!");
    }
}
