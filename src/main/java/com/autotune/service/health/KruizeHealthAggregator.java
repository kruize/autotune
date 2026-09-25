/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

import com.autotune.common.datasource.DataSourceCollection;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.utils.KruizeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Orchestrates all health checks and produces a {@link HealthReport}.
 *
 * <ol>
 *   <li>Runs the database check synchronously.</li>
 *   <li>Fans out datasource checks in parallel so one slow datasource does not
 *       block the rest. Each check carries an individual timeout.</li>
 *   <li>Computes {@code overallStatus}: {@code DOWN} if DB is down,
 *       {@code DEGRADED} if any datasource is down, {@code UP} otherwise.</li>
 * </ol>
 */
public class KruizeHealthAggregator {

    private static final Logger LOGGER = LoggerFactory.getLogger(KruizeHealthAggregator.class);

    private final DatabaseHealthChecker    dbChecker;
    private final DatasourceHealthChecker  dsChecker;
    private final ExecutorService          executor;
    private final int                      dsTimeoutSeconds;

    /** Production constructor. */
    public KruizeHealthAggregator() {
        this(new DatabaseHealthChecker(),
             new DatasourceHealthChecker(),
             Executors.newFixedThreadPool(KruizeConstants.HealthConstants.HEALTH_CHECK_THREAD_POOL_SIZE),
             KruizeConstants.HealthConstants.DATASOURCE_CHECK_TIMEOUT_SECONDS);
    }

    /** Testable constructor — inject mocks and a synchronous executor. */
    KruizeHealthAggregator(DatabaseHealthChecker dbChecker,
                           DatasourceHealthChecker dsChecker,
                           ExecutorService executor,
                           int dsTimeoutSeconds) {
        this.dbChecker        = dbChecker;
        this.dsChecker        = dsChecker;
        this.executor         = executor;
        this.dsTimeoutSeconds = dsTimeoutSeconds;
    }

    /** Runs all checks and returns a fully-populated report. Never throws. */
    public HealthReport collectHealth() {
        DatabaseHealthResult dbResult = dbChecker.check();

        Collection<DataSourceInfo> allDs =
                DataSourceCollection.getInstance().getDataSourcesCollection().values();
        List<DatasourceHealthResult> dsResults = checkInParallel(allDs);

        String overallStatus = computeOverallStatus(dbResult, dsResults);
        return new HealthReport(overallStatus, dbResult, dsResults, new Date());
    }

    private List<DatasourceHealthResult> checkInParallel(Collection<DataSourceInfo> dataSources) {
        List<CompletableFuture<DatasourceHealthResult>> futures = new ArrayList<>();
        for (DataSourceInfo ds : dataSources) {
            CompletableFuture<DatasourceHealthResult> f = CompletableFuture
                    .supplyAsync(() -> dsChecker.check(ds), executor)
                    .exceptionally(ex -> {
                        LOGGER.warn("Unexpected error checking datasource {}: {}",
                                ds.getName(), ex.getMessage());
                        return new DatasourceHealthResult(
                                ds.getName(), ds.getProvider(), ds.getServiceName(),
                                ds.getNamespace(),
                                ds.getUrl() != null ? ds.getUrl().toString() : "",
                                KruizeConstants.HealthConstants.ComponentStatus.DOWN, 0L,
                                KruizeConstants.HealthConstants.Messages.CHECK_FAILED,
                                new Date());
                    });
            futures.add(f);
        }

        List<DatasourceHealthResult> results = new ArrayList<>();
        for (CompletableFuture<DatasourceHealthResult> f : futures) {
            try {
                results.add(f.get(dsTimeoutSeconds + 1L, TimeUnit.SECONDS));
            } catch (Exception e) {
                LOGGER.warn("Datasource health future timed out: {}", e.getMessage());
            }
        }
        return results;
    }

    private String computeOverallStatus(DatabaseHealthResult db,
                                        List<DatasourceHealthResult> dsResults) {
        if (KruizeConstants.HealthConstants.ComponentStatus.DOWN.equals(db.getStatus())) {
            return KruizeConstants.HealthConstants.OverallStatus.DOWN;
        }
        boolean anyDown = dsResults.stream().anyMatch(
                r -> KruizeConstants.HealthConstants.ComponentStatus.DOWN.equals(r.getStatus()));
        return anyDown
                ? KruizeConstants.HealthConstants.OverallStatus.DEGRADED
                : KruizeConstants.HealthConstants.OverallStatus.UP;
    }
}
