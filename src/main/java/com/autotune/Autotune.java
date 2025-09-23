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
import com.autotune.analyzer.metadataProfiles.MetadataProfileCollection;
import com.autotune.analyzer.performanceProfiles.MetricProfileCollection;
import com.autotune.analyzer.autoscaler.AutoscalerService;
import com.autotune.analyzer.autoscaler.settings.AutoscalingSettings;
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
import org.eclipse.jetty.ee8.servlet.ServletContextHandler;
import org.eclipse.jetty.ee8.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.autotune.utils.KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.DATASOURCE_CONNECTION_FAILED;
import static com.autotune.utils.KruizeConstants.MetadataProfileConstants.MetadataProfileErrorMsgs.SET_UP_DEFAULT_METADATA_PROFILE_ERROR;
import static com.autotune.utils.KruizeConstants.MetricProfileConstants.MetricProfileErrorMsgs.SET_UP_DEFAULT_METRIC_PROFILE_ERROR;

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
        threadPool.setMaxThreads(ServerContext.KRUIZE_HTTP_THREAD_POOL_COUNT); // Set the maximum number of threads in the pool
        Server server = new Server(threadPool);
        // Create a connector (e.g., HTTP)
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(ServerContext.KRUIZE_SERVER_PORT);
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
            LOGGER.info("Kruize getting started with is_ros_enabled:{} , local:{}", KruizeDeploymentInfo.is_ros_enabled, KruizeDeploymentInfo.local);
            if (KruizeDeploymentInfo.is_ros_enabled) {
                executeDDLs(AnalyzerConstants.RM);
                // Short sleep to let the DB operation reflect
                Thread.sleep(KruizeDeploymentInfo.service_startup_delay);
            }

            // Read and execute the DDLs here
            if (KruizeDeploymentInfo.local) {
                LOGGER.debug("Now running kruize local DDL's ");
                executeDDLs(AnalyzerConstants.LM);
                // load available datasources from db
                loadDataSourcesFromDB();

                // setting up DataSources
                try {
                    setUpDataSources();
                } catch (Exception e) {
                    LOGGER.error(DATASOURCE_CONNECTION_FAILED, e.getMessage());
                }

                // checking available DataSources
                checkAvailableDataSources();
                // load available metric profiles from db
                loadMetricProfilesFromDB();

                if (KruizeDeploymentInfo.is_ros_enabled) {
                    // setting up metric profile
                    try {
                        setUpMetricProfile();
                    } catch (Exception e) {
                        LOGGER.error(SET_UP_DEFAULT_METRIC_PROFILE_ERROR, e.getMessage());
                    }
                }

                // load available metadata profiles from db
                loadMetadataProfilesFromDB();

                if (KruizeDeploymentInfo.is_ros_enabled) {
                    // setting up metadata profile
                    try {
                        setUpMetadataProfile();
                    } catch (Exception e) {
                        LOGGER.error(SET_UP_DEFAULT_METADATA_PROFILE_ERROR, e.getMessage());
                    }
                }


                // start updater service
                startAutoscalerService();
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

        if (KruizeDeploymentInfo.local) {
            // Check the settings initially while starting
            AutoscalingSettings.getInstance().initialiseAutoscalingSettings();
        }

        try {
            // check if kafka flag is enabled and the corresponding server details are added
            if (KruizeDeploymentInfo.is_kafka_enabled && (KruizeDeploymentInfo.kafka_bootstrap_servers == null || KruizeDeploymentInfo.kafka_bootstrap_servers.isEmpty())) {
                LOGGER.error(KruizeConstants.KAFKA_CONSTANTS.BOOTSTRAP_SERVER_MISSING);
                throw new IllegalStateException(KruizeConstants.KAFKA_CONSTANTS.BOOTSTRAP_SERVER_MISSING);
            }
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

    /**
     * Set up the metric profile at installation time
     */
    private static void setUpMetricProfile() throws IOException {
        MetricProfileCollection metricProfileCollection = MetricProfileCollection.getInstance();
        metricProfileCollection.addMetricProfileFromConfigFile();
    }

    /**
     * loads metadata profiles from database
     */
    private static void loadMetadataProfilesFromDB() {
        MetadataProfileCollection metadataProfileCollection = MetadataProfileCollection.getInstance();
        metadataProfileCollection.loadMetadataProfilesFromDB();
    }

    /**
     * Set up the metadata profile at installation time
     */
    private static void setUpMetadataProfile() throws IOException {
        MetadataProfileCollection metadataProfileCollection = MetadataProfileCollection.getInstance();
        metadataProfileCollection.addMetadataProfileFromConfigFile();
    }

    private static void addAutotuneServlets(ServletContextHandler context) {
        context.addServlet(HealthService.class, ServerContext.HEALTH_SERVICE);
        // Start the Prometheus end point (/metrics) for Autotune
        context.addServlet(
                new ServletHolder(
                    new MetricsServlet(MetricsConfig.meterRegistry().getPrometheusRegistry())
                ), ServerContext.METRICS_SERVICE);
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

    private static void executeDDLs(String ddlPathOrFile) throws Exception {
        SessionFactory factory = KruizeHibernateUtil.getSessionFactory();
        Session session = null;

        // TARGET/MIGRATIONS/<ddlPathOrFile>
        Path basePath = Paths.get(AnalyzerConstants.TARGET, AnalyzerConstants.MIGRATIONS, ddlPathOrFile);

        try {
            // Collect single file or all .sql files under directory
            List<Path> sqlFiles = new ArrayList<>();
            if (Files.notExists(basePath)) {
                throw new IllegalArgumentException("DDL path does not exist: " + basePath);
            }

            if (Files.isRegularFile(basePath)) {
                sqlFiles.add(basePath);
            } else if (Files.isDirectory(basePath)) {
                try (Stream<Path> walk = Files.walk(basePath)) {
                    sqlFiles = walk
                            .filter(p -> Files.isRegularFile(p) && p.toString().toLowerCase().endsWith(".sql"))
                            .collect(Collectors.toList());
                }
                // sort files by extracted numeric version if possible, otherwise by filename
                sqlFiles.sort((p1, p2) -> {
                    Integer v1 = extractVersion(p1.getFileName().toString());
                    Integer v2 = extractVersion(p2.getFileName().toString());
                    if (v1 != null && v2 != null) {
                        return Integer.compare(v1, v2);
                    } else if (v1 != null) {
                        return -1;
                    } else if (v2 != null) {
                        return 1;
                    } else {
                        return p1.getFileName().toString().compareTo(p2.getFileName().toString());
                    }
                });
            } else {
                throw new IllegalArgumentException("Provided path is not a file or directory: " + basePath);
            }

            if (sqlFiles.isEmpty()) {
                LOGGER.info("No SQL files found at path: {}", basePath);
                return;
            }

            session = factory.openSession();

            for (Path sqlFilePath : sqlFiles) {
                LOGGER.debug("Applying SQL file: {}", sqlFilePath.toString());
                String sqlStatement = Files.readString(sqlFilePath, StandardCharsets.UTF_8);
                Transaction transaction = session.beginTransaction();
                String trimmed = sqlStatement.trim();
                // skip blank and comment-only statements
                if (trimmed.isEmpty()
                        || trimmed.startsWith("--")
                        || trimmed.startsWith("#")
                        || trimmed.startsWith("/*")) {
                    continue;
                }

                try {
                    session.createNativeQuery(trimmed).executeUpdate();
                } catch (Exception e) {
                    String msg = Optional.ofNullable(e.getMessage()).orElse("");
                    if (msg.contains(DBConstants.DB_MESSAGES.ADD_CONSTRAINT)) {
                        LOGGER.warn("sql: {} failed due to : {}{}", trimmed, DBConstants.DB_MESSAGES.ADD_CONSTRAINT, DBConstants.DB_MESSAGES.DUPLICATE_DB_OPERATION);
                    } else if (msg.contains(DBConstants.DB_MESSAGES.ADD_COLUMN)) {
                        LOGGER.warn("sql: {} failed due to : {}{}", trimmed, DBConstants.DB_MESSAGES.ADD_COLUMN, DBConstants.DB_MESSAGES.DUPLICATE_DB_OPERATION);
                    } else {
                        LOGGER.error("sql: {} failed due to : {}", trimmed, msg);
                    }

                    // Commit current transaction and begin a new one
                    try {
                        transaction.commit();
                    } catch (Exception ignore) {
                    }
                    transaction = session.beginTransaction();
                }

                // commit file-level transaction
                try {
                    transaction.commit();
                } catch (Exception e) {
                    LOGGER.error("Failed to commit transaction after processing file {}: {}", sqlFilePath, e.getMessage());
                }
            }

            LOGGER.info(DBConstants.DB_MESSAGES.DB_CREATION_SUCCESS);
        } catch (Exception e) {
            LOGGER.error("Exception occurred while trying to read the DDL(s): {}", e.getMessage());
            throw new Exception(e);
        } finally {
            if (session != null) session.close();
        }

        LOGGER.info(DBConstants.DB_MESSAGES.DB_LIVELINESS_PROBE_SUCCESS);
    }

    /**
     * Extract numeric version from filename patterns like V001__desc.sql or V1__desc.sql
     * Returns Integer value or null if not present.
     */
    private static Integer extractVersion(String filename) {
        if (filename == null) return null;
        Pattern p = Pattern.compile("^V0*([0-9]+)__.*", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(filename);
        if (m.matches()) {
            try {
                return Integer.valueOf(m.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    // starts the recommendation updater service
    private static void startAutoscalerService() {
        AutoscalerService.initiateAutoscalerService();
    }
}
