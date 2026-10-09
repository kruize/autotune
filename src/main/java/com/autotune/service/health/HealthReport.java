/*******************************************************************************
 * Copyright (c) 2025 Red Hat, IBM Corporation and others.
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
package com.autotune.service.health;

import java.util.Date;
import java.util.List;

/**
 * Top-level health response object serialised directly to JSON by Gson.
 *
 * <pre>
 * {
 *   "overallStatus": "UP | DEGRADED | DOWN",
 *   "database":      { ... },
 *   "datasources":   [ ... ],
 *   "timestamp":     "..."
 * }
 * </pre>
 */
public class HealthReport {

    private final String overallStatus;
    private final DatabaseHealthResult database;
    private final List<DatasourceHealthResult> datasources;
    private final Date timestamp;

    public HealthReport(String overallStatus,
                        DatabaseHealthResult database,
                        List<DatasourceHealthResult> datasources,
                        Date timestamp) {
        this.overallStatus = overallStatus;
        this.database      = database;
        this.datasources   = datasources;
        this.timestamp     = timestamp;
    }

    public String getOverallStatus()                     { return overallStatus; }
    public DatabaseHealthResult getDatabase()            { return database; }
    public List<DatasourceHealthResult> getDatasources() { return datasources; }
    public Date getTimestamp()                           { return timestamp; }
}
