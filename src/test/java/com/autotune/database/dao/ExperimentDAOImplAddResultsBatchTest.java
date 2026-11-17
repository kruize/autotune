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
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.PersistenceException;
import org.hibernate.HibernateException;
import org.hibernate.PersistentObjectException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for batched {@link ExperimentDAOImpl#addToDBAndFetchFailedResults}.
 * Verifies the happy-path single transaction and the per-row fallback (fresh session per row,
 * commit only on success, rollback on failure).
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

    private static PersistenceException dbError(String message) {
        return new PersistenceException("could not execute statement", new RuntimeException(message));
    }

    private static PersistenceException duplicateKey() {
        return dbError(DBConstants.DB_MESSAGES.DUPLICATE_KEY + " violates unique constraint");
    }

    /** Session whose beginTransaction() returns tx; tx reports a rollback-able status. */
    private static Session sessionWith(Transaction tx) {
        Session session = mock(Session.class);
        TransactionStatus status = mock(TransactionStatus.class);
        when(status.canRollback()).thenReturn(true);
        when(tx.getStatus()).thenReturn(status);
        when(session.beginTransaction()).thenReturn(tx);
        return session;
    }

    @Test
    @DisplayName("Happy path persists all rows in a single transaction")
    public void testHappyPathSingleTransaction() {
        SessionFactory sessionFactory = mock(SessionFactory.class);
        Session session = mock(Session.class);
        Transaction tx = mock(Transaction.class);
        when(sessionFactory.openSession()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(tx);

        KruizeResultsEntry first = resultsEntry("exp-a", Instant.parse("2024-01-01T00:15:00Z"));
        KruizeResultsEntry second = resultsEntry("exp-a", Instant.parse("2024-01-01T00:30:00Z"));
        List<KruizeResultsEntry> entries = List.of(first, second);

        try (MockedStatic<KruizeHibernateUtil> hibernate = mockStatic(KruizeHibernateUtil.class)) {
            hibernate.when(KruizeHibernateUtil::getSessionFactory).thenReturn(sessionFactory);

            List<KruizeResultsEntry> failed = new ExperimentDAOImpl().addToDBAndFetchFailedResults(entries);

            assertTrue(failed.isEmpty());
            verify(sessionFactory, times(1)).openSession();
            verify(session, times(1)).beginTransaction();
            verify(session, times(2)).persist(any(KruizeResultsEntry.class));
            verify(session, times(1)).flush();
            verify(tx, times(1)).commit();
            verify(tx, never()).rollback();
            verify(session, never()).clear();

            // Rows must be persisted inside the transaction: begin -> persist x2 -> flush -> commit -> close
            InOrder order = inOrder(session, tx);
            order.verify(session).beginTransaction();
            order.verify(session).persist(first);
            order.verify(session).persist(second);
            order.verify(session).flush();
            order.verify(tx).commit();
            order.verify(session).close();
        }
    }

    @Test
    @DisplayName("Duplicate on batch flush falls back to per-row commits and reports RESULTS_ALREADY_EXISTS")
    public void testDuplicateFallsBackToPerRow() {
        SessionFactory sessionFactory = mock(SessionFactory.class);
        Transaction batchTx = mock(Transaction.class);
        Transaction okTx = mock(Transaction.class);
        Transaction dupTx = mock(Transaction.class);
        Session batchSession = sessionWith(batchTx);
        Session okSession = sessionWith(okTx);
        Session dupSession = sessionWith(dupTx);
        when(sessionFactory.openSession()).thenReturn(batchSession, okSession, dupSession);

        KruizeResultsEntry ok = resultsEntry("exp-a", Instant.parse("2024-01-01T00:15:00Z"));
        KruizeResultsEntry dup = resultsEntry("exp-a", Instant.parse("2024-01-01T00:30:00Z"));
        List<KruizeResultsEntry> entries = List.of(ok, dup);

        doThrow(duplicateKey()).when(batchSession).flush();
        doThrow(duplicateKey()).when(dupSession).flush();

        try (MockedStatic<KruizeHibernateUtil> hibernate = mockStatic(KruizeHibernateUtil.class)) {
            hibernate.when(KruizeHibernateUtil::getSessionFactory).thenReturn(sessionFactory);

            List<KruizeResultsEntry> failed = new ExperimentDAOImpl().addToDBAndFetchFailedResults(entries);

            assertEquals(1, failed.size());
            assertSame(dup, failed.get(0));
            assertEquals(
                    List.of(AnalyzerErrorConstants.APIErrors.updateResultsAPI.RESULTS_ALREADY_EXISTS),
                    failed.get(0).getErrorReasons());
            assertNull(ok.getErrorReasons());

            // 1 batch session + 1 fresh session per row; the failed batch session is not reused
            verify(sessionFactory, times(3)).openSession();
            verify(batchTx).rollback();
            verify(batchTx, never()).commit();
            verify(batchSession).close();

            // Successful row: begin -> persist(ok) -> flush -> commit, never rolled back
            InOrder okOrder = inOrder(okSession, okTx);
            okOrder.verify(okSession).beginTransaction();
            okOrder.verify(okSession).persist(ok);
            okOrder.verify(okSession).flush();
            okOrder.verify(okTx).commit();
            verify(okTx, never()).rollback();
            verify(okSession).close();

            // Duplicate row: persisted then rolled back, never committed
            InOrder dupOrder = inOrder(dupSession, dupTx);
            dupOrder.verify(dupSession).beginTransaction();
            dupOrder.verify(dupSession).persist(dup);
            dupOrder.verify(dupSession).flush();
            dupOrder.verify(dupTx).rollback();
            verify(dupTx, never()).commit();
            verify(dupSession).close();
        }
    }

    @Test
    @DisplayName("A per-row commit failure is reported for that row only and later rows are still saved")
    public void testPerRowCommitFailureDoesNotAbortLoop() {
        SessionFactory sessionFactory = mock(SessionFactory.class);
        Transaction batchTx = mock(Transaction.class);
        Transaction firstTx = mock(Transaction.class);
        Transaction badTx = mock(Transaction.class);
        Transaction lastTx = mock(Transaction.class);
        Session batchSession = sessionWith(batchTx);
        Session firstSession = sessionWith(firstTx);
        Session badSession = sessionWith(badTx);
        Session lastSession = sessionWith(lastTx);
        when(sessionFactory.openSession()).thenReturn(batchSession, firstSession, badSession, lastSession);

        KruizeResultsEntry first = resultsEntry("exp-a", Instant.parse("2024-01-01T00:15:00Z"));
        KruizeResultsEntry bad = resultsEntry("exp-a", Instant.parse("2024-01-01T00:30:00Z"));
        KruizeResultsEntry last = resultsEntry("exp-a", Instant.parse("2024-01-01T00:45:00Z"));

        doThrow(duplicateKey()).when(batchSession).flush();
        doThrow(new PersistenceException("commit failed")).when(badTx).commit();

        try (MockedStatic<KruizeHibernateUtil> hibernate = mockStatic(KruizeHibernateUtil.class)) {
            hibernate.when(KruizeHibernateUtil::getSessionFactory).thenReturn(sessionFactory);

            List<KruizeResultsEntry> failed = new ExperimentDAOImpl()
                    .addToDBAndFetchFailedResults(List.of(first, bad, last));

            assertEquals(List.of(bad), failed);
            assertEquals(List.of("commit failed"), bad.getErrorReasons());
            assertNull(first.getErrorReasons());
            assertNull(last.getErrorReasons());

            verify(firstTx).commit();
            verify(badTx).rollback();
            verify(lastSession).persist(last);
            verify(lastTx).commit();
            verify(lastTx, never()).rollback();
        }
    }

    @Test
    @DisplayName("Missing partition: row is rolled back, partitions are created, and the row is retried in a new session")
    public void testMissingPartitionCreatesPartitionsAndRetries() throws Exception {
        SessionFactory sessionFactory = mock(SessionFactory.class);
        Transaction batchTx = mock(Transaction.class);
        Transaction firstTryTx = mock(Transaction.class);
        Transaction retryTx = mock(Transaction.class);
        Session batchSession = sessionWith(batchTx);
        Session firstTrySession = sessionWith(firstTryTx);
        Session retrySession = sessionWith(retryTx);
        when(sessionFactory.openSession()).thenReturn(batchSession, firstTrySession, retrySession);

        KruizeResultsEntry entry = resultsEntry("exp-a", Instant.parse("2024-01-20T00:15:00Z"));
        PersistenceException noPartition = dbError(DBConstants.DB_MESSAGES.NO_PARTITION_RELATION + " \"kruize_results\" found for row");
        doThrow(noPartition).when(batchSession).flush();
        doThrow(noPartition).when(firstTrySession).flush();

        ExperimentDAOImpl dao = spy(new ExperimentDAOImpl());
        doNothing().when(dao).addPartitions(anyString(), anyString(), anyString(), anyInt(), anyString());

        try (MockedStatic<KruizeHibernateUtil> hibernate = mockStatic(KruizeHibernateUtil.class)) {
            hibernate.when(KruizeHibernateUtil::getSessionFactory).thenReturn(sessionFactory);

            List<KruizeResultsEntry> failed = dao.addToDBAndFetchFailedResults(List.of(entry));

            assertTrue(failed.isEmpty());
            assertNull(entry.getErrorReasons());
            verify(firstTryTx).rollback();
            verify(firstTryTx, never()).commit();
            verify(dao, atLeastOnce()).addPartitions(anyString(), anyString(), anyString(), anyInt(), anyString());
            verify(retrySession).persist(entry);
            verify(retryTx).commit();
        }
    }

    // ---- Round 2 review fixes ----

    @Test
    @DisplayName("Batch: a close() failure after commit does not turn stored rows into failures")
    public void testBatchCloseFailureAfterCommitIsSuccess() {
        SessionFactory sessionFactory = mock(SessionFactory.class);
        Transaction tx = mock(Transaction.class);
        Session session = sessionWith(tx);
        when(sessionFactory.openSession()).thenReturn(session);
        doThrow(new HibernateException("close failed")).when(session).close();

        KruizeResultsEntry first = resultsEntry("exp-a", Instant.parse("2024-01-01T00:15:00Z"));
        KruizeResultsEntry second = resultsEntry("exp-a", Instant.parse("2024-01-01T00:30:00Z"));

        try (MockedStatic<KruizeHibernateUtil> hibernate = mockStatic(KruizeHibernateUtil.class)) {
            hibernate.when(KruizeHibernateUtil::getSessionFactory).thenReturn(sessionFactory);

            List<KruizeResultsEntry> failed = new ExperimentDAOImpl().addToDBAndFetchFailedResults(List.of(first, second));

            assertTrue(failed.isEmpty());
            assertNull(first.getErrorReasons());
            assertNull(second.getErrorReasons());
            verify(sessionFactory, times(1)).openSession();   // no per-row replay
            InOrder order = inOrder(session, tx);
            order.verify(tx).commit();
            order.verify(session).close();
            verify(tx, never()).rollback();
        }
    }

    @Test
    @DisplayName("Per-row: a close() failure after commit does not report that row as failed")
    public void testPerRowCloseFailureAfterCommitIsSuccess() {
        SessionFactory sessionFactory = mock(SessionFactory.class);
        Transaction batchTx = mock(Transaction.class);
        Transaction okTx = mock(Transaction.class);
        Transaction dupTx = mock(Transaction.class);
        Session batchSession = sessionWith(batchTx);
        Session okSession = sessionWith(okTx);
        Session dupSession = sessionWith(dupTx);
        when(sessionFactory.openSession()).thenReturn(batchSession, okSession, dupSession);

        KruizeResultsEntry ok = resultsEntry("exp-a", Instant.parse("2024-01-01T00:15:00Z"));
        KruizeResultsEntry dup = resultsEntry("exp-a", Instant.parse("2024-01-01T00:30:00Z"));
        doThrow(duplicateKey()).when(batchSession).flush();
        doThrow(new HibernateException("close failed")).when(okSession).close();
        doThrow(duplicateKey()).when(dupSession).flush();

        try (MockedStatic<KruizeHibernateUtil> hibernate = mockStatic(KruizeHibernateUtil.class)) {
            hibernate.when(KruizeHibernateUtil::getSessionFactory).thenReturn(sessionFactory);

            List<KruizeResultsEntry> failed = new ExperimentDAOImpl().addToDBAndFetchFailedResults(List.of(ok, dup));

            assertEquals(List.of(dup), failed);
            assertNull(ok.getErrorReasons());
            assertEquals(List.of(AnalyzerErrorConstants.APIErrors.updateResultsAPI.RESULTS_ALREADY_EXISTS), dup.getErrorReasons());
            InOrder okOrder = inOrder(okSession, okTx);
            okOrder.verify(okTx).commit();
            okOrder.verify(okSession).close();
            verify(okTx, never()).rollback();
        }
    }

    @Test
    @DisplayName("Batch commit outcome unknown: per-row duplicates count as stored, other errors still fail")
    public void testBatchCommitFailureTreatsDuplicatesAsStored() {
        SessionFactory sessionFactory = mock(SessionFactory.class);
        Transaction batchTx = mock(Transaction.class);
        Transaction storedTx = mock(Transaction.class);
        Transaction newTx = mock(Transaction.class);
        Transaction badTx = mock(Transaction.class);
        Session batchSession = sessionWith(batchTx);
        Session storedSession = sessionWith(storedTx);
        Session newSession = sessionWith(newTx);
        Session badSession = sessionWith(badTx);
        when(sessionFactory.openSession()).thenReturn(batchSession, storedSession, newSession, badSession);

        KruizeResultsEntry stored = resultsEntry("exp-a", Instant.parse("2024-01-01T00:15:00Z"));
        KruizeResultsEntry fresh = resultsEntry("exp-a", Instant.parse("2024-01-01T00:30:00Z"));
        KruizeResultsEntry bad = resultsEntry("exp-a", Instant.parse("2024-01-01T00:45:00Z"));
        // flush succeeded, commit() threw: the batch may or may not have been stored
        doThrow(new PersistenceException("commit acknowledgement lost")).when(batchTx).commit();
        doThrow(duplicateKey()).when(storedSession).flush();
        doThrow(dbError("value too long")).when(badSession).flush();

        try (MockedStatic<KruizeHibernateUtil> hibernate = mockStatic(KruizeHibernateUtil.class)) {
            hibernate.when(KruizeHibernateUtil::getSessionFactory).thenReturn(sessionFactory);

            List<KruizeResultsEntry> failed = new ExperimentDAOImpl().addToDBAndFetchFailedResults(List.of(stored, fresh, bad));

            assertEquals(List.of(bad), failed);
            assertNull(stored.getErrorReasons());
            assertNull(fresh.getErrorReasons());
            assertEquals(List.of("could not execute statement"), bad.getErrorReasons());
            InOrder batchOrder = inOrder(batchSession, batchTx);
            batchOrder.verify(batchSession).flush();
            batchOrder.verify(batchTx).commit();
            batchOrder.verify(batchTx).rollback();
            verify(storedTx).rollback();
            verify(storedTx, never()).commit();
            verify(newTx).commit();
            verify(badTx).rollback();
        }
    }

    @Test
    @DisplayName("A row failure with a null message is recorded for that row only")
    public void testNullMessageRowFailureIsRecorded() {
        SessionFactory sessionFactory = mock(SessionFactory.class);
        Transaction batchTx = mock(Transaction.class);
        Transaction badTx = mock(Transaction.class);
        Transaction okTx = mock(Transaction.class);
        Session batchSession = sessionWith(batchTx);
        Session badSession = sessionWith(badTx);
        Session okSession = sessionWith(okTx);
        when(sessionFactory.openSession()).thenReturn(batchSession, badSession, okSession);

        KruizeResultsEntry bad = resultsEntry("exp-a", Instant.parse("2024-01-01T00:15:00Z"));
        KruizeResultsEntry ok = resultsEntry("exp-a", Instant.parse("2024-01-01T00:30:00Z"));
        doThrow(new PersistenceException()).when(batchSession).flush();
        doThrow(new PersistenceException()).when(badSession).flush();

        try (MockedStatic<KruizeHibernateUtil> hibernate = mockStatic(KruizeHibernateUtil.class)) {
            hibernate.when(KruizeHibernateUtil::getSessionFactory).thenReturn(sessionFactory);

            List<KruizeResultsEntry> failed = new ExperimentDAOImpl().addToDBAndFetchFailedResults(List.of(bad, ok));

            assertEquals(List.of(bad), failed);
            assertEquals(List.of(PersistenceException.class.getName()), bad.getErrorReasons());
            assertNull(ok.getErrorReasons());
            verify(badTx).rollback();
            verify(okTx).commit();
        }
    }

    @Test
    @DisplayName("Connection failure on the batch skips the per-row fallback and fails every row")
    public void testConnectionFailureSkipsPerRowFallback() {
        SessionFactory sessionFactory = mock(SessionFactory.class);
        Transaction batchTx = mock(Transaction.class);
        Session batchSession = sessionWith(batchTx);
        when(sessionFactory.openSession()).thenReturn(batchSession);

        KruizeResultsEntry first = resultsEntry("exp-a", Instant.parse("2024-01-01T00:15:00Z"));
        KruizeResultsEntry second = resultsEntry("exp-a", Instant.parse("2024-01-01T00:30:00Z"));
        JDBCConnectionException down = new JDBCConnectionException("Unable to acquire JDBC Connection",
                new SQLException("Connection refused", "08001"));
        doThrow(down).when(batchSession).flush();

        try (MockedStatic<KruizeHibernateUtil> hibernate = mockStatic(KruizeHibernateUtil.class)) {
            hibernate.when(KruizeHibernateUtil::getSessionFactory).thenReturn(sessionFactory);

            List<KruizeResultsEntry> failed = new ExperimentDAOImpl().addToDBAndFetchFailedResults(List.of(first, second));

            assertEquals(List.of(first, second), failed);
            assertEquals(List.of(down.getMessage()), first.getErrorReasons());
            assertEquals(List.of(down.getMessage()), second.getErrorReasons());
            verify(sessionFactory, times(1)).openSession();   // no per-row sessions
            verify(batchTx).rollback();
            verify(batchTx, never()).commit();
            verify(batchSession).close();
        }
    }

    @Test
    @DisplayName("Connection lost during the per-row fallback stops the loop and fails the remaining rows")
    public void testConnectionFailureStopsPerRowLoop() {
        SessionFactory sessionFactory = mock(SessionFactory.class);
        Transaction batchTx = mock(Transaction.class);
        Transaction okTx = mock(Transaction.class);
        Transaction lostTx = mock(Transaction.class);
        Session batchSession = sessionWith(batchTx);
        Session okSession = sessionWith(okTx);
        Session lostSession = sessionWith(lostTx);
        when(sessionFactory.openSession()).thenReturn(batchSession, okSession, lostSession);

        KruizeResultsEntry ok = resultsEntry("exp-a", Instant.parse("2024-01-01T00:15:00Z"));
        KruizeResultsEntry lost = resultsEntry("exp-a", Instant.parse("2024-01-01T00:30:00Z"));
        KruizeResultsEntry skipped = resultsEntry("exp-a", Instant.parse("2024-01-01T00:45:00Z"));
        PersistenceException connectionLost = new PersistenceException("could not execute statement",
                new SQLException("terminating connection due to administrator command", "57P01"));
        doThrow(duplicateKey()).when(batchSession).flush();
        doThrow(connectionLost).when(lostSession).flush();

        try (MockedStatic<KruizeHibernateUtil> hibernate = mockStatic(KruizeHibernateUtil.class)) {
            hibernate.when(KruizeHibernateUtil::getSessionFactory).thenReturn(sessionFactory);

            List<KruizeResultsEntry> failed = new ExperimentDAOImpl().addToDBAndFetchFailedResults(List.of(ok, lost, skipped));

            assertEquals(List.of(lost, skipped), failed);
            assertNull(ok.getErrorReasons());
            assertEquals(List.of("could not execute statement"), lost.getErrorReasons());
            assertEquals(List.of("could not execute statement"), skipped.getErrorReasons());
            verify(sessionFactory, times(3)).openSession();   // the last row is never attempted
            verify(okTx).commit();
            verify(lostTx).rollback();
            verify(lostTx, never()).commit();
        }
    }

    @Test
    @DisplayName("Hibernate 'detached entity passed to persist' for an existing key maps to RESULTS_ALREADY_EXISTS")
    public void testDetachedEntityMapsToResultsAlreadyExists() {
        SessionFactory sessionFactory = mock(SessionFactory.class);
        Transaction batchTx = mock(Transaction.class);
        Transaction okTx = mock(Transaction.class);
        Transaction dupTx = mock(Transaction.class);
        Session batchSession = sessionWith(batchTx);
        Session okSession = sessionWith(okTx);
        Session dupSession = sessionWith(dupTx);
        when(sessionFactory.openSession()).thenReturn(batchSession, okSession, dupSession);

        KruizeResultsEntry ok = resultsEntry("exp-a", Instant.parse("2024-01-01T00:15:00Z"));
        KruizeResultsEntry dup = resultsEntry("exp-a", Instant.parse("2024-01-01T00:30:00Z"));
        // What Session.persist() throws in Hibernate 6.6 when the assigned id already exists in the DB
        String detached = "detached entity passed to persist: " + KruizeResultsEntry.class.getName();
        doThrow(new EntityExistsException(detached, new PersistentObjectException(detached))).when(batchSession).persist(dup);
        doThrow(new EntityExistsException(detached, new PersistentObjectException(detached))).when(dupSession).persist(dup);

        try (MockedStatic<KruizeHibernateUtil> hibernate = mockStatic(KruizeHibernateUtil.class)) {
            hibernate.when(KruizeHibernateUtil::getSessionFactory).thenReturn(sessionFactory);

            List<KruizeResultsEntry> failed = new ExperimentDAOImpl().addToDBAndFetchFailedResults(List.of(ok, dup));

            assertEquals(List.of(dup), failed);
            assertEquals(List.of(AnalyzerErrorConstants.APIErrors.updateResultsAPI.RESULTS_ALREADY_EXISTS), dup.getErrorReasons());
            assertNull(ok.getErrorReasons());
            verify(okTx).commit();
            InOrder dupOrder = inOrder(dupSession, dupTx);
            dupOrder.verify(dupSession).beginTransaction();
            dupOrder.verify(dupSession).persist(dup);
            dupOrder.verify(dupTx).rollback();
            verify(dupSession, never()).flush();
            verify(dupTx, never()).commit();
        }
    }
}
