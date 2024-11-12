/*******************************************************************************
 * Copyright (c) 2020, 2021 Red Hat, IBM Corporation and others.
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
package com.autotune;

import com.autotune.analyzer.Analyzer;
import com.autotune.analyzer.exceptions.K8sTypeNotSupportedException;
import com.autotune.analyzer.exceptions.KruizeErrorHandler;
import com.autotune.analyzer.exceptions.MonitoringAgentNotFoundException;
import com.autotune.analyzer.exceptions.MonitoringAgentNotSupportedException;
import com.autotune.analyzer.performanceProfiles.MetricProfileCollection;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.datasource.DataSourceCollection;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.common.exceptions.datasource.DataSourceAlreadyExist;
import com.autotune.common.exceptions.datasource.DataSourceNotServiceable;
import com.autotune.common.exceptions.datasource.UnsupportedDataSourceProvider;
import com.autotune.database.helper.DBConstants;
import com.autotune.database.init.KruizeHibernateUtil;
import com.autotune.experimentManager.core.ExperimentManager;
import com.autotune.operator.InitializeDeployment;
import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.service.HealthService;
import com.autotune.service.InitiateListener;
import com.autotune.utils.CloudWatchAppender;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.MetricsConfig;
import com.autotune.utils.ServerContext;
import com.autotune.utils.filter.KruizeCORSFilter;
import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.DefaultExports;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Scanner;

import static com.autotune.utils.ServerContext.*;

public class Autotune {
    private static final Logger LOGGER = LoggerFactory.getLogger(Autotune.class);

    public static void main(String[] args) {

        try {
            // Turning off the logging level for the specific package to reduce console logging
            Configurator.setLevel(KruizeConstants.SQL_EXCEPTION_HELPER_PKG, Level.OFF);
        } catch (Exception e) {
            LOGGER.error("Exception occurred while turning the Logger off for the SQLException class: {}", e.getMessage());
        }

        ServletContextHandler context = null;

        disableServerLogging();
        // Create a thread pool with the desired number of threads
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMaxThreads(KRUIZE_HTTP_THREAD_POOL_COUNT); // Set the maximum number of threads in the pool
        Server server = new Server(threadPool);
        // Create a connector (e.g., HTTP)
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(KRUIZE_SERVER_PORT);
        // Set the connector to the server
        server.addConnector(connector);

        context = new ServletContextHandler();
        context.setContextPath(ServerContext.ROOT_CONTEXT);
        context.setErrorHandler(new KruizeErrorHandler());
        context.addFilter(
                KruizeCORSFilter.getFilter(),
                KruizeConstants.CORSConstants.PATH_WILDCARD,
                EnumSet.of(DispatcherType.REQUEST)
        );
        /**
         *  Adding Listener to initiate variables during server start.
         */
        InitiateListener contextListener = new InitiateListener();
        context.addEventListener(contextListener);
        server.setHandler(context);
        server.addBean(new KruizeErrorHandler());


        try {
            InitializeDeployment.setup_deployment_info();
            // Configure AWS CloudWatch
            CloudWatchAppender.configureLoggerForCloudWatchLog();
            // Read and execute the DDLs here
            executeDDLs(AnalyzerConstants.ROS_DDL_SQL);
            if (KruizeDeploymentInfo.local == true) {
                LOGGER.info("Now running kruize local DDL's ");
                executeDDLs(AnalyzerConstants.KRUIZE_LOCAL_DDL_SQL);
                // load available datasources from db
                loadDataSourcesFromDB();
                // setting up DataSources
                setUpDataSources();
                // checking available DataSources
                checkAvailableDataSources();
                // load available metric profiles from db
                loadMetricProfilesFromDB();

            }
            // close the existing session factory before recreating
            KruizeHibernateUtil.closeSessionFactory();
            //Regenerate a Hibernate session following the creation of new tables
            KruizeHibernateUtil.buildSessionFactory();
        } catch (Exception | K8sTypeNotSupportedException | MonitoringAgentNotSupportedException |
                 MonitoringAgentNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }

        if (KruizeDeploymentInfo.settings_save_to_db) {
            Session session = null;
            try {
                session = KruizeHibernateUtil.getSessionFactory().openSession();
                session.close();
            } catch (Exception e) {
                LOGGER.error("DB connection failed! : " + e.getMessage());
                System.exit(1);
            } finally {
                if (null != session) session.close();
            }
        }
        addAutotuneServlets(context);
        String autotuneMode = KruizeDeploymentInfo.autotune_mode;

        if (null != autotuneMode) {
            if (autotuneMode.equalsIgnoreCase(KruizeConstants.StartUpMode.EM_ONLY_MODE)) {
                startAutotuneEMOnly(context);
            } else {
                startAutotuneNormalMode(context);
            }
        } else {
            startAutotuneNormalMode(context);
        }

        try {
            String startAutotune = System.getenv("START_AUTOTUNE");
            if (startAutotune == null || startAutotune.equalsIgnoreCase("true")) {
                server.start();
                server.join();
            }
        } catch (Exception e) {
            LOGGER.error("Could not start the server!");
            e.printStackTrace();
        }

    }

    /**
     * Set up the data sources available at installation time from config file
     */
    private static void setUpDataSources() throws UnsupportedDataSourceProvider, DataSourceNotServiceable, DataSourceAlreadyExist, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        DataSourceCollection dataSourceCollection = DataSourceCollection.getInstance();
        dataSourceCollection.addDataSourcesFromConfigFile(KruizeConstants.CONFIG_FILE);
    }

    /**
     * loads datasources from database
     */
    private static void loadDataSourcesFromDB() {
        DataSourceCollection dataSourceCollection = DataSourceCollection.getInstance();
        dataSourceCollection.loadDataSourcesFromDB();
    }

    /**
     * checks the data sources available
     */
    private static void checkAvailableDataSources() {
        DataSourceCollection dataSourceCollection = DataSourceCollection.getInstance();
        LOGGER.info(KruizeConstants.DataSourceConstants.DataSourceInfoMsgs.CHECKING_AVAILABLE_DATASOURCE);
        HashMap<String, DataSourceInfo> dataSources = dataSourceCollection.getDataSourcesCollection();
        for (String name : dataSources.keySet()) {
            DataSourceInfo dataSource = dataSources.get(name);
            String dataSourceName = dataSource.getName();
            String url = dataSource.getUrl().toString();
            LOGGER.info(KruizeConstants.DataSourceConstants.DataSourceSuccessMsgs.DATASOURCE_FOUND + dataSourceName + ", " + url);
        }
    }

    /**
     * loads metric profiles from database
     */
    private static void loadMetricProfilesFromDB() {
        MetricProfileCollection metricProfileCollection = MetricProfileCollection.getInstance();
        metricProfileCollection.loadMetricProfilesFromDB();
    }

    private static void addAutotuneServlets(ServletContextHandler context) {
        context.addServlet(HealthService.class, HEALTH_SERVICE);
        // Start the Prometheus end point (/metrics) for Autotune
        context.addServlet(new ServletHolder(new MetricsServlet(MetricsConfig.meterRegistry().getPrometheusRegistry())), METRICS_SERVICE);
        DefaultExports.initialize();
    }

    private static void disableServerLogging() {
        /* The jetty server creates a lot of server log messages that are unnecessary.
         * This disables jetty logging. */
        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
        System.setProperty("org.eclipse.jetty.LEVEL", "OFF");
    }

    private static void startAutotuneEMOnly(ServletContextHandler contextHandler) {
        ExperimentManager.launch(contextHandler);
    }

    private static void startAutotuneNormalMode(ServletContextHandler contextHandler) {
        Analyzer.start(contextHandler);
        ExperimentManager.launch(contextHandler);
    }

    private static void executeDDLs(String ddlFileName) throws Exception {
        SessionFactory factory = KruizeHibernateUtil.getSessionFactory();
        Session session = null;
        try {
            session = factory.openSession();
            Path sqlFilePath = Paths.get(AnalyzerConstants.TARGET, AnalyzerConstants.MIGRATIONS, ddlFileName);
            File sqlFile = sqlFilePath.toFile();
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
                        if (e.getMessage().contains(DBConstants.DB_MESSAGES.ADD_CONSTRAINT)) {
                            LOGGER.warn("sql: {} failed due to : {}", sqlStatement, DBConstants.DB_MESSAGES.ADD_CONSTRAINT + DBConstants.DB_MESSAGES.DUPLICATE_DB_OPERATION);
                        } else if (e.getMessage().contains(DBConstants.DB_MESSAGES.ADD_COLUMN)) {
                            LOGGER.warn("sql: {} failed due to : {}", sqlStatement, DBConstants.DB_MESSAGES.ADD_COLUMN + DBConstants.DB_MESSAGES.DUPLICATE_DB_OPERATION);
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
            LOGGER.info(DBConstants.DB_MESSAGES.DB_CREATION_SUCCESS);
        } catch (Exception e) {
            LOGGER.error("Exception occurred while trying to read the DDL file: {}", e.getMessage());
            throw new Exception(e);
        } finally {
            if (null != session) session.close(); // Close the Hibernate session
        }

        LOGGER.info(DBConstants.DB_MESSAGES.DB_LIVELINESS_PROBE_SUCCESS);
    }

}
