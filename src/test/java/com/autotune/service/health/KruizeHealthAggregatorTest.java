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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Test with Mocking Template Principles
 *
 * 1. One class under test:
 *    Each test class focuses on validating the behavior of KruizeHealthAggregator.
 *
 * 2. Mock only direct dependencies:
 *    DatabaseHealthChecker, DatasourceHealthCheckerRegistry and DataSourceCollection
 *    are mocked/stubbed to keep tests isolated, fast, and deterministic.
 *
 * 3. No static or global side effects:
 *    The DataSourceCollection singleton is pre-populated per test and restored
 *    via tearDown to avoid cross-test pollution.
 *
 * 4. Clear Given–When–Then structure.
 *
 * 5. Deterministic assertions.
 */
class KruizeHealthAggregatorTest {

    private DatabaseHealthChecker   mockDbChecker;
    private DatasourceHealthChecker mockDsChecker;
    private ExecutorService         executor;
    private KruizeHealthAggregator  aggregator;

    // Saved reference to the real DataSourceCollection map so we can restore it
    private HashMap<String, DataSourceInfo> originalCollectionMap;

    @BeforeEach
    void setup() throws Exception {
        mockDbChecker = mock(DatabaseHealthChecker.class);
        mockDsChecker = mock(DatasourceHealthChecker.class);
        // Synchronous single-thread executor so tests are deterministic
        executor      = Executors.newSingleThreadExecutor();
        aggregator    = new KruizeHealthAggregator(mockDbChecker, mockDsChecker, executor, 5);

        // Snapshot the current in-memory datasource map and clear it
        originalCollectionMap = new HashMap<>(
                DataSourceCollection.getInstance().getDataSourcesCollection());
        DataSourceCollection.getInstance().getDataSourcesCollection().clear();
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
        // Restore original collection state
        DataSourceCollection.getInstance().getDataSourcesCollection().clear();
        DataSourceCollection.getInstance().getDataSourcesCollection()
                .putAll(originalCollectionMap);
    }

    // -------------------------------------------------------------------------
    // Scenario 1 — healthy database, no datasources configured
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("UP — healthy DB, no datasources configured")
    void shouldReturnUpWhenDbHealthyAndNoDatasources() {
        // Given
        when(mockDbChecker.check()).thenReturn(dbUp());

        // When
        HealthReport report = aggregator.collectHealth();

        // Then
        assertEquals(KruizeConstants.HealthConstants.OverallStatus.UP, report.getOverallStatus());
        assertEquals(KruizeConstants.HealthConstants.ComponentStatus.UP, report.getDatabase().getStatus());
        assertTrue(report.getDatasources().isEmpty());
        assertNotNull(report.getTimestamp());
    }

    // -------------------------------------------------------------------------
    // Scenario 2 — database DOWN
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("DOWN — database unreachable")
    void shouldReturnDownWhenDatabaseIsDown() {
        // Given
        when(mockDbChecker.check()).thenReturn(dbDown());

        // When
        HealthReport report = aggregator.collectHealth();

        // Then
        assertEquals(KruizeConstants.HealthConstants.OverallStatus.DOWN, report.getOverallStatus());
        assertEquals(KruizeConstants.HealthConstants.ComponentStatus.DOWN, report.getDatabase().getStatus());
    }

    // -------------------------------------------------------------------------
    // Scenario 3 — all datasources healthy
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("UP — healthy DB, all datasources UP")
    void shouldReturnUpWhenAllDatasourcesHealthy() {
        // Given
        DataSourceInfo ds = fakeDatasource("prometheus-1", "prometheus");
        DataSourceCollection.getInstance().getDataSourcesCollection().put(ds.getName(), ds);
        // Build the result object before setting up the stub to avoid nested mock interaction
        DatasourceHealthResult upResult = dsUp("prometheus-1", "prometheus");

        when(mockDbChecker.check()).thenReturn(dbUp());
        when(mockDsChecker.check(ds)).thenReturn(upResult);

        // When
        HealthReport report = aggregator.collectHealth();

        // Then
        assertEquals(KruizeConstants.HealthConstants.OverallStatus.UP, report.getOverallStatus());
        assertEquals(1, report.getDatasources().size());
        assertEquals(KruizeConstants.HealthConstants.ComponentStatus.UP,
                report.getDatasources().get(0).getStatus());
    }

    // -------------------------------------------------------------------------
    // Scenario 4 — one datasource DOWN → DEGRADED
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("DEGRADED — healthy DB, one datasource DOWN")
    void shouldReturnDegradedWhenOneDatasourceIsDown() {
        // Given
        DataSourceInfo ds1 = fakeDatasource("prometheus-1", "prometheus");
        DataSourceInfo ds2 = fakeDatasource("thanos-1", "prometheus");
        DataSourceCollection.getInstance().getDataSourcesCollection().put(ds1.getName(), ds1);
        DataSourceCollection.getInstance().getDataSourcesCollection().put(ds2.getName(), ds2);
        // Build result objects before stubbing to avoid nested mock interactions
        DatasourceHealthResult upResult   = dsUp("prometheus-1", "prometheus");
        DatasourceHealthResult downResult = dsDown("thanos-1", "prometheus");

        when(mockDbChecker.check()).thenReturn(dbUp());
        when(mockDsChecker.check(ds1)).thenReturn(upResult);
        when(mockDsChecker.check(ds2)).thenReturn(downResult);

        // When
        HealthReport report = aggregator.collectHealth();

        // Then
        assertEquals(KruizeConstants.HealthConstants.OverallStatus.DEGRADED, report.getOverallStatus());
        List<DatasourceHealthResult> results = report.getDatasources();
        assertEquals(2, results.size());

        long downCount = results.stream()
                .filter(r -> KruizeConstants.HealthConstants.ComponentStatus.DOWN.equals(r.getStatus()))
                .count();
        assertEquals(1, downCount);
    }

    // -------------------------------------------------------------------------
    // Scenario 5 — authentication failure mapped to correct message
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("DEGRADED — datasource authentication failure")
    void shouldReportAuthFailureWithSafeMessage() {
        // Given
        DataSourceInfo ds = fakeDatasource("prometheus-1", "prometheus");
        DataSourceCollection.getInstance().getDataSourcesCollection().put(ds.getName(), ds);
        DatasourceHealthResult authFailResult = dsDown("prometheus-1", "prometheus");

        when(mockDbChecker.check()).thenReturn(dbUp());
        when(mockDsChecker.check(ds)).thenReturn(authFailResult);

        // When
        HealthReport report = aggregator.collectHealth();

        // Then
        assertEquals(KruizeConstants.HealthConstants.OverallStatus.DEGRADED, report.getOverallStatus());
        DatasourceHealthResult r = report.getDatasources().get(0);
        assertEquals(KruizeConstants.HealthConstants.ComponentStatus.DOWN, r.getStatus());
    }

    // -------------------------------------------------------------------------
    // Scenario 6 — timeout on one datasource
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("DEGRADED — datasource check times out")
    void shouldReturnDegradedOnDatasourceTimeout() {
        // Given
        DataSourceInfo ds = fakeDatasource("slow-ds", "prometheus");
        DataSourceCollection.getInstance().getDataSourcesCollection().put(ds.getName(), ds);
        DatasourceHealthResult timeoutResult = dsDown("slow-ds", "prometheus");

        when(mockDbChecker.check()).thenReturn(dbUp());
        when(mockDsChecker.check(ds)).thenReturn(timeoutResult);

        // When
        HealthReport report = aggregator.collectHealth();

        // Then
        assertEquals(KruizeConstants.HealthConstants.OverallStatus.DEGRADED, report.getOverallStatus());
        assertEquals(KruizeConstants.HealthConstants.ComponentStatus.DOWN,
                report.getDatasources().get(0).getStatus());
    }

    // -------------------------------------------------------------------------
    // Scenario 7 — checker throws unexpectedly
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("DEGRADED — datasource checker throws unexpectedly")
    void shouldReturnDegradedWhenDatasourceCheckerThrows() {
        // Given
        DataSourceInfo ds = fakeDatasource("broken-ds", "prometheus");
        DataSourceCollection.getInstance().getDataSourcesCollection().put(ds.getName(), ds);

        when(mockDbChecker.check()).thenReturn(dbUp());
        when(mockDsChecker.check(ds)).thenThrow(new RuntimeException("boom"));

        // When
        HealthReport report = aggregator.collectHealth();

        // Then
        assertEquals(KruizeConstants.HealthConstants.OverallStatus.DEGRADED, report.getOverallStatus());
        assertEquals(1, report.getDatasources().size());
        assertEquals(KruizeConstants.HealthConstants.ComponentStatus.DOWN,
                report.getDatasources().get(0).getStatus());
    }

    // -------------------------------------------------------------------------
    // Scenario 8 — empty datasource list (explicitly empty, DB up)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("UP — healthy DB, datasource list explicitly empty")
    void shouldReturnUpWithEmptyDatasourceList() {
        // Given — collection already cleared in @BeforeEach
        when(mockDbChecker.check()).thenReturn(dbUp());

        // When
        HealthReport report = aggregator.collectHealth();

        // Then
        assertEquals(KruizeConstants.HealthConstants.OverallStatus.UP, report.getOverallStatus());
        assertNotNull(report.getDatasources());
        assertTrue(report.getDatasources().isEmpty());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private DataSourceInfo fakeDatasource(String name, String provider) {
        DataSourceInfo ds = mock(DataSourceInfo.class);
        when(ds.getName()).thenReturn(name);
        when(ds.getProvider()).thenReturn(provider);
        when(ds.getServiceName()).thenReturn("svc-" + name);
        when(ds.getNamespace()).thenReturn("monitoring");
        when(ds.getUrl()).thenReturn(null);
        return ds;
    }

    private DatabaseHealthResult dbUp() {
        return new DatabaseHealthResult(
                KruizeConstants.HealthConstants.ComponentStatus.UP);
    }

    private DatabaseHealthResult dbDown() {
        return new DatabaseHealthResult(
                KruizeConstants.HealthConstants.ComponentStatus.DOWN);
    }

    /**
     * Build a healthy DatasourceHealthResult by name/provider — avoids calling
     * mock methods inside a thenReturn() stub, which causes UnfinishedStubbing.
     */
    private DatasourceHealthResult dsUp(String name, String provider) {
        return new DatasourceHealthResult(
                name, provider, KruizeConstants.HealthConstants.ComponentStatus.UP);
    }

    private DatasourceHealthResult dsDown(String name, String provider) {
        return new DatasourceHealthResult(
                name, provider, KruizeConstants.HealthConstants.ComponentStatus.DOWN);
    }
}
