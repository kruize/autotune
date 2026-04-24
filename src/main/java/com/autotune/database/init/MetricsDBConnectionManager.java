/*******************************************************************************
 * Copyright (c) 2026 Red Hat, IBM Corporation and others.
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

package com.autotune.database.init;

import com.autotune.common.data.metrics.MetricDataPoint;
import com.autotune.utils.KruizeConstants;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collections;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.sql.Timestamp;
import java.util.ArrayList;

/**
 * Manages Hibernate sessions for metrics databases (db1, db2) configured in KruizeConfig.
 * Used for Metric DB POC to connect to custom metrics databases and execute SQL queries.
 */
public class MetricsDBConnectionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsDBConnectionManager.class);
    private static final String METRICS_DATABASES_KEY = "metricsDatabases";

    private static MetricsDBConnectionManager instance;
    private final Map<String, SessionFactory> sessionFactoryMap = new HashMap<>();

    private MetricsDBConnectionManager() {
    }

    public static synchronized MetricsDBConnectionManager getInstance() {
        if (instance == null) {
            instance = new MetricsDBConnectionManager();
        }
        return instance;
    }

    /**
     * Initializes metrics DB connections from the dbconfigjson file.
     * Must be called after InitializeDeployment.setup_deployment_info().
     */
    public void initialize() {
        String configFile = System.getenv(KruizeConstants.DBConstants.CONFIG_FILE);
        if (configFile == null || configFile.isEmpty()) {
            LOGGER.info("DB_CONFIG_FILE not set, skipping metrics databases initialization");
            return;
        }

        try (InputStream is = new FileInputStream(configFile)) {
            String jsonTxt = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            JSONObject configObject = new JSONObject(jsonTxt);

            if (!configObject.has(METRICS_DATABASES_KEY)) {
                LOGGER.info("No metricsDatabases configuration found in dbconfig");
                return;
            }

            JSONObject metricsDatabases = configObject.getJSONObject(METRICS_DATABASES_KEY);
            for (String dbName : metricsDatabases.keySet()) {
                try {
                    JSONObject dbConfig = metricsDatabases.getJSONObject(dbName);
                    SessionFactory sf = createSessionFactory(dbName, dbConfig);
                    if (sf != null) {
                        sessionFactoryMap.put(dbName, sf);
                        LOGGER.info("Metrics DB '{}' connection initialized successfully", dbName);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to initialize metrics DB '{}': {}", dbName, e.getMessage());
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Could not load metrics databases config from {}: {}", configFile, e.getMessage());
        }
    }

    private SessionFactory createSessionFactory(String dbName, JSONObject dbConfig) {
        try {
            // All values must come from config - no hardcoded defaults
            String hostname = getRequiredConfig(dbConfig, dbName, "hostname");
            Integer port = getRequiredConfigInt(dbConfig, dbName, "port");
            String dbNameConfig = getRequiredConfig(dbConfig, dbName, "name");
            String username = getRequiredConfig(dbConfig, dbName, "username");
            String password = getRequiredConfig(dbConfig, dbName, "password");
            String sslMode = getRequiredConfig(dbConfig, dbName, "sslMode");
            String dbdriver = getRequiredConfig(dbConfig, dbName, "dbdriver");
            String dialect = getRequiredConfig(dbConfig, dbName, "dialect");
            String driver = getRequiredConfig(dbConfig, dbName, "driver");
            String hbm2ddlauto = getRequiredConfig(dbConfig, dbName, "hbm2ddlauto");
            String showsql = getRequiredConfig(dbConfig, dbName, "showsql");

            if (hostname == null || port == null || dbNameConfig == null || username == null
                    || password == null || sslMode == null || dbdriver == null || dialect == null
                    || driver == null || hbm2ddlauto == null || showsql == null) {
                return null;
            }

            String connectionURL = dbdriver + hostname + ":" + port + "/" + dbNameConfig
                    + "?sslmode=" + sslMode;

            Configuration configuration = new Configuration();
            configuration.setProperty("hibernate.connection.url", connectionURL);
            configuration.setProperty("hibernate.connection.username", username);
            configuration.setProperty("hibernate.connection.password", password);
            configuration.setProperty("hibernate.dialect", dialect);
            configuration.setProperty("hibernate.connection.driver_class", driver);
            configuration.setProperty("hibernate.hbm2ddl.auto", hbm2ddlauto);
            configuration.setProperty("hibernate.show_sql", showsql);

            return configuration.buildSessionFactory();
        } catch (Exception e) {
            LOGGER.error("Failed to create SessionFactory for metrics DB '{}': {}", dbName, e.getMessage());
            return null;
        }
    }

    private String getRequiredConfig(JSONObject dbConfig, String dbName, String key) {
        if (!dbConfig.has(key)) {
            LOGGER.error("Metrics DB '{}' missing required config: '{}'", dbName, key);
            return null;
        }
        String value = dbConfig.optString(key);
        if (value == null || value.isEmpty()) {
            LOGGER.error("Metrics DB '{}' has empty value for config: '{}'", dbName, key);
            return null;
        }
        return value;
    }

    private Integer getRequiredConfigInt(JSONObject dbConfig, String dbName, String key) {
        if (!dbConfig.has(key)) {
            LOGGER.error("Metrics DB '{}' missing required config: '{}'", dbName, key);
            return null;
        }
        try {
            return dbConfig.getInt(key);
        } catch (Exception e) {
            LOGGER.error("Metrics DB '{}' invalid integer for config '{}': {}", dbName, key, e.getMessage());
            return null;
        }
    }

    /**
     * Gets a session for the specified metrics database.
     *
     * @param metricsDbRef the datasource reference (e.g., "db1", "db2")
     * @return Session or null if not configured
     */
    public Session getSession(String metricsDbRef) {
        SessionFactory sf = sessionFactoryMap.get(metricsDbRef);
        if (sf == null) {
            return null;
        }
        return sf.openSession();
    }

    /**
     * Executes a SQL query on the specified metrics database and returns the result as string for logging.
     * Logs the query and output to demonstrate successful connection and read for the Metric DB POC.
     *
     * @param metricsDbRef the datasource reference (e.g., "db1", "db2")
     * @param sql          the SQL query to execute
     * @return query result as string, or error message
     */
    public String executeQueryAndLog(String metricsDbRef, String sql) {
        LOGGER.info("Metric DB POC: Executing query on '{}' - Query: {}", metricsDbRef, sql);
        Session session = null;
        try {
            session = getSession(metricsDbRef);
            if (session == null) {
                String err = "Metrics DB '" + metricsDbRef + "' not configured";
                LOGGER.error("Metric DB POC: Connection failed - {}", err);
                return err;
            }
            Object result = session.createNativeQuery(sql).getSingleResult();
            String resultStr = result != null ? result.toString() : "null";
            LOGGER.info("Metric DB POC: Successfully connected and read from '{}'. Query: '{}' | Output: {}",
                    metricsDbRef, sql, resultStr);
            return resultStr;
        } catch (Exception e) {
            String errMsg = "Error executing query on " + metricsDbRef + ": " + e.getMessage();
            LOGGER.error("Metric DB POC: Validation failed - {}", errMsg);
            return errMsg;
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (Exception e) {
                    LOGGER.debug("Error closing session: {}", e.getMessage());
                }
            }
        }
    }

    public List<Object[]> getMetricsData(String metricsDbRef, String sql, Map<String, String> params) {
        return getMetricsData(metricsDbRef, sql, params, null);
    }

    public List<Object[]> getMetricsData(String metricsDbRef, String sql, Map<String, String> params, List<String> resultColumns) {
        Session session = null;
        try {
            session = getSession(metricsDbRef);
            if (session == null) {
                String err = "Metrics DB '" + metricsDbRef + "' not configured";
                LOGGER.error("Metric DB POC: Connection failed - {}", err);
                return null;
            }
            
            NativeQuery query = session.createNativeQuery(sql);
            
            // Dynamically add scalars based on result_columns if provided
            if (resultColumns != null && !resultColumns.isEmpty()) {
                for (String columnName : resultColumns) {
                    if (columnName.contains("time")) {
                        query.addScalar(columnName, java.sql.Timestamp.class);
                    } else {
                        query.addScalar(columnName, String.class);
                    }
                }
            } 
            // No Fallback is maingto default columns for backward compatibility as the result columns sequence should match the sequence in which query params are defined in query
                
            
            LOGGER.error("final query = {}", query.toString());
            if (params != null) {
                for (String key : params.keySet()) {
                    query.setParameter(key, params.get(key));
                }
            }

            List<Object[]> resultList = query.getResultList();
            return resultList;

        } catch (Exception e) {
            LOGGER.error("Metric DB POC: Validation failed - {}", e);
            return null;
        } finally {
            if (session != null) {
                try {
                    session.close();
                }  catch (Exception e) {
                    LOGGER.error("Error closing session: {}", e.getMessage());
                }
            }
        }
    }


    /**
     * Checks if a metrics database is configured.
     */
    public boolean isConfigured(String metricsDbRef) {
        return sessionFactoryMap.containsKey(metricsDbRef);
    }

    /**
     * Returns the set of configured metrics database names.
     */
    public java.util.Set<String> getConfiguredDatabases() {
        return sessionFactoryMap.keySet();
    }

    /**
     * Closes all metrics DB connections.
     */
    public void shutdown() {
        for (Map.Entry<String, SessionFactory> entry : sessionFactoryMap.entrySet()) {
            try {
                entry.getValue().close();
                LOGGER.info("Closed metrics DB connection: {}", entry.getKey());
            } catch (Exception e) {
                LOGGER.warn("Error closing metrics DB '{}': {}", entry.getKey(), e.getMessage());
            }
        }
        sessionFactoryMap.clear();
    }

}
