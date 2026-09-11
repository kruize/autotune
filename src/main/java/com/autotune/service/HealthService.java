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
package com.autotune.service;

import com.autotune.analyzer.utils.GsonUTCDateAdapter;
import com.autotune.service.health.HealthReport;
import com.autotune.service.health.KruizeHealthAggregator;
import com.autotune.utils.KruizeConstants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

/**
 * Servlet for the {@code GET /health} endpoint.
 *
 * <p>Returns a JSON body describing the overall health of Kruize, the
 * PostgreSQL database connection, and every configured datasource:
 *
 * <pre>
 * {
 *   "overallStatus": "UP | DEGRADED | DOWN",
 *   "database":   { "status": "UP" },
 *   "datasources": [ { "name": "prometheus-1", "status": "UP", ... } ],
 *   "timestamp":  "2025-01-01T12:00:00.000Z"
 * }
 * </pre>
 *
 * <p>HTTP status codes:
 * <ul>
 *   <li>{@code 200 OK} — {@code UP} or {@code DEGRADED} (service is running)</li>
 *   <li>{@code 503 Service Unavailable} — {@code DOWN} (database unavailable)</li>
 * </ul>
 *
 * <p>All business logic lives in {@link KruizeHealthAggregator}; this class is
 * intentionally kept thin.
 */
public class HealthService extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthService.class);

    // Legacy status constants kept for any existing external callers that might
    // reference them via reflection or static imports.
    public static final int STATUS_UP   = 1;
    public static final int STATUS_DOWN = 0;
    private static int CURRENT_STATUS   = STATUS_UP;

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
            .setPrettyPrinting()
            .create();

    private final KruizeHealthAggregator aggregator;

    /** Production no-arg constructor used by the Jetty servlet container. */
    public HealthService() {
        this(new KruizeHealthAggregator());
    }

    /** Testable constructor — allows injecting a mock aggregator. */
    HealthService(KruizeHealthAggregator aggregator) {
        this.aggregator = aggregator;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType(JSON_CONTENT_TYPE);
        resp.setCharacterEncoding(CHARACTER_ENCODING);

        HealthReport report;
        try {
            report = aggregator.collectHealth();
        } catch (Exception e) {
            // Should never reach here — aggregator is designed to never throw —
            // but guard defensively so the endpoint always returns valid JSON.
            LOGGER.error("Unexpected error in health aggregator: {}", e.getMessage());
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("{\"overallStatus\":\"DOWN\",\"message\":\"Internal health check error\"}");
            return;
        }

        boolean isDown = KruizeConstants.HealthConstants.OverallStatus.DOWN
                .equals(report.getOverallStatus());
        resp.setStatus(isDown
                ? HttpServletResponse.SC_SERVICE_UNAVAILABLE   // 503
                : HttpServletResponse.SC_OK);                  // 200

        resp.getWriter().println(GSON.toJson(report));
    }
}
