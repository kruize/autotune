package com.autotune.jobs;

import com.autotune.analyzer.exceptions.K8sTypeNotSupportedException;
import com.autotune.analyzer.exceptions.MonitoringAgentNotFoundException;
import com.autotune.analyzer.exceptions.MonitoringAgentNotSupportedException;
import com.autotune.database.init.KruizeHibernateUtil;
import com.autotune.operator.InitializeDeployment;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Scanner;


public class CreateKruizeDBElements {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateKruizeDBElements.class);

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
        Session session = null;
        try {
            session = factory.openSession();
            File sqlFile = new File("target/bin/migrations/kruize_experiments_ddl.sql");
            Scanner scanner = new Scanner(sqlFile);
            Transaction transaction = session.beginTransaction();

            while (scanner.hasNextLine()) {
                String sqlStatement = scanner.nextLine();
                if (sqlStatement.startsWith("#") || sqlStatement.startsWith("-")) {
                    continue;
                } else {
                    try {
                        session.createNativeQuery(sqlStatement).executeUpdate();
                    } catch (Exception e) {
                        if (e.getMessage().contains("add constraint")) {
                            LOGGER.warn("sql: {} failed due to : {}", sqlStatement, e.getMessage());
                        } else {
                            LOGGER.error("sql: {} failed due to : {}", sqlStatement, e.getMessage());
                        }
                        transaction.commit();
                        transaction = session.beginTransaction();
                    }
                }
            }

            transaction.commit();

            scanner.close();
            LOGGER.info("DB creation successful !");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (null != session) session.close(); // Close the Hibernate session when you're done
        }

        LOGGER.info("DB Liveliness probe connection successful!");
    }
}
