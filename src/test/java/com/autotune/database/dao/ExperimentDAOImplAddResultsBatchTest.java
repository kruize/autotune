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
package com.autotune.database.dao;

import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.database.helper.DBConstants;
import com.autotune.database.init.KruizeHibernateUtil;
import com.autotune.database.table.KruizeResultsEntry;
import jakarta.persistence.PersistenceException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for batched {@link ExperimentDAOImpl#addToDBAndFetchFailedResults}.
 * Verifies happy-path single transaction and per-row fallback on duplicate-key failure.
 */
public class ExperimentDAOImplAddResultsBatchTest {

    private static KruizeResultsEntry resultsEntry(String experimentName, Instant end) {
        KruizeResultsEntry entry = new KruizeResultsEntry();
        entry.setExperiment_name(experimentName);
        entry.setInterval_end_time(Timestamp.from(end));
        entry.setInterval_start_time(Timestamp.from(end.minusSeconds(900)));
        entry.setDuration_minutes(15.0);
        entry.setVersion("1.0");
        return entry;
    }

    @Test
    @DisplayName("Happy path persists all rows in a single transaction")
    public void testHappyPathSingleTransaction() {
        SessionFactory sessionFactory = mock(SessionFactory.class);
        Session session = mock(Session.class);
        Transaction tx = mock(Transaction.class);
        when(sessionFactory.openSession()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(tx);

        List<KruizeResultsEntry> entries = List.of(
                resultsEntry("exp-a", Instant.parse("2024-01-01T00:15:00Z")),
                resultsEntry("exp-a", Instant.parse("2024-01-01T00:30:00Z"))
        );

        try (MockedStatic<KruizeHibernateUtil> hibernate = mockStatic(KruizeHibernateUtil.class)) {
            hibernate.when(KruizeHibernateUtil::getSessionFactory).thenReturn(sessionFactory);

            List<KruizeResultsEntry> failed = new ExperimentDAOImpl().addToDBAndFetchFailedResults(entries);

            assertTrue(failed.isEmpty());
            verify(session, times(1)).beginTransaction();
            verify(session, times(2)).persist(any(KruizeResultsEntry.class));
            verify(session, times(1)).flush();
            verify(tx, times(1)).commit();
            verify(tx, never()).rollback();
            verify(session, never()).clear();
        }
    }

    @Test
    @DisplayName("Duplicate on batch flush falls back to per-row commits and reports RESULTS_ALREADY_EXISTS")
    public void testDuplicateFallsBackToPerRow() {
        SessionFactory sessionFactory = mock(SessionFactory.class);
        Session session = mock(Session.class);
        Transaction tx = mock(Transaction.class);
        TransactionStatus txStatus = mock(TransactionStatus.class);
        when(sessionFactory.openSession()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(tx);
        when(tx.getStatus()).thenReturn(txStatus);
        when(txStatus.canRollback()).thenReturn(true);

        KruizeResultsEntry ok = resultsEntry("exp-a", Instant.parse("2024-01-01T00:15:00Z"));
        KruizeResultsEntry dup = resultsEntry("exp-a", Instant.parse("2024-01-01T00:30:00Z"));
        List<KruizeResultsEntry> entries = List.of(ok, dup);

        AtomicInteger flushCount = new AtomicInteger();
        doAnswer(invocation -> {
            int n = flushCount.incrementAndGet();
            // 1 = batch flush (force fallback); 3 = second per-row flush (duplicate)
            if (n == 1 || n == 3) {
                throw new PersistenceException(
                        "could not execute statement",
                        new RuntimeException(DBConstants.DB_MESSAGES.DUPLICATE_KEY + " violates unique constraint"));
            }
            return null;
        }).when(session).flush();

        try (MockedStatic<KruizeHibernateUtil> hibernate = mockStatic(KruizeHibernateUtil.class)) {
            hibernate.when(KruizeHibernateUtil::getSessionFactory).thenReturn(sessionFactory);

            List<KruizeResultsEntry> failed = new ExperimentDAOImpl().addToDBAndFetchFailedResults(entries);

            assertEquals(1, failed.size());
            assertSame(dup, failed.get(0));
            assertEquals(
                    List.of(AnalyzerErrorConstants.APIErrors.updateResultsAPI.RESULTS_ALREADY_EXISTS),
                    failed.get(0).getErrorReasons());
            assertNull(ok.getErrorReasons());

            // 1 batch attempt + 2 per-row transactions
            verify(session, times(3)).beginTransaction();
            verify(session).clear();
            verify(tx, atLeastOnce()).rollback();
            // per-row finally commits for both rows (including the duplicate row's txn finish)
            verify(tx, times(2)).commit();
        }
    }
}
