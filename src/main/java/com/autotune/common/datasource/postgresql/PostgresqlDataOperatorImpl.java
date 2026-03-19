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

package com.autotune.common.datasource.postgresql;

import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.common.datasource.DataSourceOperatorImpl;
import com.autotune.common.utils.CommonUtils;
import com.autotune.database.init.MetricsDBConnectionManager;
import com.autotune.utils.KruizeConstants;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import org.hibernate.Session;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * DataSource operator for PostgreSQL metrics databases.
 * Executes SQL queries against configured metrics DBs (db1, db2).
 */
public class PostgresqlDataOperatorImpl extends DataSourceOperatorImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresqlDataOperatorImpl.class);
    private static PostgresqlDataOperatorImpl instance;

    private PostgresqlDataOperatorImpl() {
        super();
    }

    public static PostgresqlDataOperatorImpl getInstance() {
        if (instance == null) {
            instance = new PostgresqlDataOperatorImpl();
        }
        return instance;
    }

    @Override
    public String getDefaultServicePortForProvider() {
        return "5432";
    }

    @Override
    public CommonUtils.DatasourceReachabilityStatus isServiceable(DataSourceInfo dataSource) {
        String metricsDbRef = dataSource.getMetricsDbRef();
        if (metricsDbRef == null || metricsDbRef.isEmpty()) {
            return CommonUtils.DatasourceReachabilityStatus.NOT_REACHABLE;
        }
        MetricsDBConnectionManager manager = MetricsDBConnectionManager.getInstance();
        if (!manager.isConfigured(metricsDbRef)) {
            return CommonUtils.DatasourceReachabilityStatus.NOT_REACHABLE;
        }
        try {
            String result = manager.executeQueryAndLog(metricsDbRef, "SELECT version()");
            return result != null && !result.contains("Error") && !result.contains("not configured")
                    ? CommonUtils.DatasourceReachabilityStatus.REACHABLE
                    : CommonUtils.DatasourceReachabilityStatus.NOT_REACHABLE;
        } catch (Exception e) {
            LOGGER.debug("Metrics DB {} reachability check failed: {}", metricsDbRef, e.getMessage());
            return CommonUtils.DatasourceReachabilityStatus.NOT_REACHABLE;
        }
    }

    @Override
    public Object getValueForQuery(DataSourceInfo dataSource, String query) throws IOException {
        String metricsDbRef = dataSource.getMetricsDbRef();
        if (metricsDbRef == null) {
            return null;
        }
        Session session = null;
        try {
            session = MetricsDBConnectionManager.getInstance().getSession(metricsDbRef);
            if (session == null) {
                return null;
            }
            return session.createNativeQuery(query).getSingleResult();
        } catch (Exception e) {
            LOGGER.error("SQL query failed on {}: {}", metricsDbRef, e.getMessage());
            throw new IOException("Metrics DB query failed: " + e.getMessage());
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    @Override
    public JSONObject getJsonObjectForQuery(DataSourceInfo dataSource, String query) throws IOException {
        Object result = getValueForQuery(dataSource, query);
        JSONObject json = new JSONObject();
        json.put(KruizeConstants.JSONKeys.DATA, result != null ? result.toString() : "null");
        return json;
    }

    @Override
    public JsonArray getResultArrayForQuery(DataSourceInfo dataSource, String query) throws IOException {
        String metricsDbRef = dataSource.getMetricsDbRef();
        if (metricsDbRef == null) {
            return new JsonArray();
        }
        Session session = null;
        try {
            session = MetricsDBConnectionManager.getInstance().getSession(metricsDbRef);
            if (session == null) {
                return new JsonArray();
            }
            List<?> results = session.createNativeQuery(query).list();
            JsonArray arr = new JsonArray();
            for (Object r : results) {
                arr.add(new JsonPrimitive(r != null ? r.toString() : "null"));
            }
            return arr;
        } catch (Exception e) {
            LOGGER.error("SQL query failed on {}: {}", metricsDbRef, e.getMessage());
            throw new IOException("Metrics DB query failed: " + e.getMessage());
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    @Override
    public boolean validateResultArray(JsonArray resultArray) {
        return resultArray != null && !resultArray.isJsonNull() && resultArray.size() > 0;
    }

    @Override
    public String getQueryEndpoint() {
        return "";
    }
}
