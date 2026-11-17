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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.autotune.database.helper.DBConstants;
import com.autotune.database.init.KruizeHibernateUtil;
import com.autotune.database.table.KruizeResultsEntry;
import com.autotune.utils.KruizeConstants;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.PersistenceException;
import org.hibernate.HibernateException;
import org.hibernate.PersistentObjectException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.jdbc.Work;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            verify(dao, atLeastOnce()).addPartitions(eq(DBConstants.TABLE_NAMES.KRUIZE_RESULTS), anyString(), anyString(), anyInt(), anyString());
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
    @DisplayName("Batch commit outcome unknown: a duplicate counts as stored only if one stored row matches the payload")
    public void testBatchCommitFailureStillReportsDuplicates() throws Exception {
        SessionFactory sessionFactory = mock(SessionFactory.class);
        Transaction batchTx = mock(Transaction.class);
        Transaction storedTx = mock(Transaction.class);
        Transaction otherTx = mock(Transaction.class);
        Transaction missingTx = mock(Transaction.class);
        Transaction newTx = mock(Transaction.class);
        Transaction badTx = mock(Transaction.class);
        Session batchSession = sessionWith(batchTx);
        Session storedSession = sessionWith(storedTx);
        Session otherSession = sessionWith(otherTx);
        Session missingSession = sessionWith(missingTx);
        Session newSession = sessionWith(newTx);
        Session badSession = sessionWith(badTx);

        KruizeResultsEntry stored = resultsEntry("exp-a", Instant.parse("2024-01-01T00:15:00Z"));
        KruizeResultsEntry other = resultsEntry("exp-a", Instant.parse("2024-01-01T00:30:00Z"));
        KruizeResultsEntry missing = resultsEntry("exp-a", Instant.parse("2024-01-01T00:45:00Z"));
        KruizeResultsEntry fresh = resultsEntry("exp-a", Instant.parse("2024-01-01T01:00:00Z"));
        KruizeResultsEntry bad = resultsEntry("exp-a", Instant.parse("2024-01-01T01:15:00Z"));
        KruizeResultsEntry otherPayload = copyOf(other);
        otherPayload.setVersion("2.0");
        Session storedLookup = lookupSessionFor(stored, List.of(copyOf(stored)));
        Session otherLookup = lookupSessionFor(other, List.of(otherPayload));
        Session missingLookup = lookupSessionFor(missing, List.of());
        when(sessionFactory.openSession()).thenReturn(batchSession, storedSession, storedLookup, otherSession, otherLookup,
                missingSession, missingLookup, newSession, badSession);

        // flush succeeded, commit() threw: the batch may or may not have been stored
        doThrow(new PersistenceException("commit acknowledgement lost")).when(batchTx).commit();
        doThrow(duplicateKey()).when(storedSession).flush();
        doThrow(duplicateKey()).when(otherSession).flush();
        doThrow(duplicateKey()).when(missingSession).flush();
        doThrow(dbError("value too long")).when(badSession).flush();

        try (MockedStatic<KruizeHibernateUtil> hibernate = mockStatic(KruizeHibernateUtil.class)) {
            hibernate.when(KruizeHibernateUtil::getSessionFactory).thenReturn(sessionFactory);

            List<KruizeResultsEntry> failed = new ExperimentDAOImpl()
                    .addToDBAndFetchFailedResults(List.of(stored, other, missing, fresh, bad));

            assertEquals(List.of(other, missing, bad), failed);
            assertNull(stored.getErrorReasons());
            assertEquals(List.of(AnalyzerErrorConstants.APIErrors.updateResultsAPI.RESULTS_ALREADY_EXISTS), other.getErrorReasons());
            assertEquals(List.of(AnalyzerErrorConstants.APIErrors.updateResultsAPI.RESULTS_ALREADY_EXISTS), missing.getErrorReasons());
            assertNull(fresh.getErrorReasons());
            assertEquals(List.of("could not execute statement"), bad.getErrorReasons());
            // one lookup per duplicate only; none for the inserted row or the non-duplicate failure
            verify(sessionFactory, times(9)).openSession();
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

    // ---- Round 3 review fixes ----

    private static KruizeResultsEntry copyOf(KruizeResultsEntry e) {
        KruizeResultsEntry c = resultsEntry(e.getExperiment_name(), e.getInterval_end_time().toInstant());
        c.setInterval_start_time(e.getInterval_start_time());
        c.setVersion(e.getVersion());
        c.setCluster_name(e.getCluster_name());
        c.setDuration_minutes(e.getDuration_minutes());
        c.setExtended_data(e.getExtended_data());
        c.setMeta_data(e.getMeta_data());
        return c;
    }

    /** Session whose lookup returns the given rows only if it is bound to key's experiment_name and interval_end_time. */
    @SuppressWarnings("unchecked")
    private static Session lookupSessionFor(KruizeResultsEntry key, List<KruizeResultsEntry> rows) {
        Session session = mock(Session.class);
        Query<KruizeResultsEntry> query = mock(Query.class, RETURNS_SELF);
        Map<String, Object> bound = new HashMap<>();
        when(session.createQuery(anyString(), eq(KruizeResultsEntry.class))).thenReturn(query);
        when(query.setParameter(anyString(), any(Object.class))).thenAnswer(inv -> {
            bound.put(inv.getArgument(0), inv.getArgument(1));
            return query;
        });
        when(query.getResultList()).thenAnswer(inv -> Map.of(
                KruizeConstants.JSONKeys.EXPERIMENT_NAME, key.getExperiment_name(),
                KruizeConstants.JSONKeys.INTERVAL_END_TIME, key.getInterval_end_time()).equals(bound) ? rows : List.of());
        return session;
    }

    /** One row whose per-row flush succeeds but whose commit() throws; the 3rd openSession is the lookup. */
    private static List<KruizeResultsEntry> runWithLostRowCommit(KruizeResultsEntry entry, Session lookupSession,
                                                                 SessionFactory sessionFactory, Transaction rowTx) {
        Transaction batchTx = mock(Transaction.class);
        Session batchSession = sessionWith(batchTx);
        Session rowSession = sessionWith(rowTx);
        doThrow(duplicateKey()).when(batchSession).flush();
        doThrow(new PersistenceException("commit acknowledgement lost")).when(rowTx).commit();
        if (null == lookupSession) {
            when(sessionFactory.openSession()).thenReturn(batchSession, rowSession)
                    .thenThrow(new HibernateException("lookup failed"));
        } else {
            when(sessionFactory.openSession()).thenReturn(batchSession, rowSession, lookupSession);
        }
        try (MockedStatic<KruizeHibernateUtil> hibernate = mockStatic(KruizeHibernateUtil.class)) {
            hibernate.when(KruizeHibernateUtil::getSessionFactory).thenReturn(sessionFactory);
            return new ExperimentDAOImpl().addToDBAndFetchFailedResults(List.of(entry));
        }
    }

    private static KruizeResultsEntry entryWithPayload() throws Exception {
        KruizeResultsEntry entry = resultsEntry("exp-a", Instant.parse("2024-01-01T00:15:00Z"));
        entry.setCluster_name("default");
        entry.setExtended_data(new ObjectMapper().readTree("{\"cpu\":{\"avg\":1.0},\"pods\":[1,2]}"));
        return entry;
    }

    @Test
    @DisplayName("Per-row commit throws but the row is stored with the same payload: counted as stored")
    public void testLostRowCommitWithMatchingRowIsStored() throws Exception {
        SessionFactory sessionFactory = mock(SessionFactory.class);
        Transaction rowTx = mock(Transaction.class);
        KruizeResultsEntry entry = entryWithPayload();
        KruizeResultsEntry stored = copyOf(entry);
        // jsonb reorders keys and may print numbers differently
        stored.setExtended_data(new ObjectMapper().readTree("{\"pods\":[1,2],\"cpu\":{\"avg\":1.00}}"));
        Session lookupSession = lookupSessionFor(entry, List.of(stored));

        List<KruizeResultsEntry> failed = runWithLostRowCommit(entry, lookupSession, sessionFactory, rowTx);

        assertTrue(failed.isEmpty());
        assertNull(entry.getErrorReasons());
        verify(sessionFactory, times(3)).openSession();
        InOrder order = inOrder(rowTx, lookupSession);
        order.verify(rowTx).commit();
        order.verify(lookupSession).createQuery(anyString(), eq(KruizeResultsEntry.class));
        verify(lookupSession, never()).persist(any());
        verify(lookupSession).close();
    }

    @Test
    @DisplayName("Per-row commit throws and the stored row has a different payload: reported failed")
    public void testLostRowCommitWithMismatchingRowFails() throws Exception {
        SessionFactory sessionFactory = mock(SessionFactory.class);
        KruizeResultsEntry entry = entryWithPayload();
        KruizeResultsEntry other = copyOf(entry);
        other.setExtended_data(new ObjectMapper().readTree("{\"cpu\":{\"avg\":2.0},\"pods\":[1,2]}"));

        List<KruizeResultsEntry> failed = runWithLostRowCommit(entry, lookupSessionFor(entry, List.of(other)),
                sessionFactory, mock(Transaction.class));

        assertEquals(List.of(entry), failed);
        assertEquals(List.of("commit acknowledgement lost"), entry.getErrorReasons());
    }

    @Test
    @DisplayName("Per-row commit throws and the row is not in the DB: reported failed")
    public void testLostRowCommitWithMissingRowFails() throws Exception {
        SessionFactory sessionFactory = mock(SessionFactory.class);
        KruizeResultsEntry entry = entryWithPayload();

        List<KruizeResultsEntry> failed = runWithLostRowCommit(entry, lookupSessionFor(entry, List.of()),
                sessionFactory, mock(Transaction.class));

        assertEquals(List.of(entry), failed);
        assertEquals(List.of("commit acknowledgement lost"), entry.getErrorReasons());
    }

    @Test
    @DisplayName("Per-row commit throws and the lookup fails: reported failed with the original error")
    public void testLostRowCommitWithFailingLookupFails() throws Exception {
        SessionFactory sessionFactory = mock(SessionFactory.class);
        KruizeResultsEntry entry = entryWithPayload();

        List<KruizeResultsEntry> failed = runWithLostRowCommit(entry, null, sessionFactory, mock(Transaction.class));

        assertEquals(List.of(entry), failed);
        assertEquals(List.of("commit acknowledgement lost"), entry.getErrorReasons());
        verify(sessionFactory, times(3)).openSession();
    }

    @Test
    @DisplayName("addPartitions sets a local lock_timeout before taking the advisory lock")
    public void testAddPartitionsSetsLockTimeoutBeforeAdvisoryLock() throws Exception {
        SessionFactory sessionFactory = mock(SessionFactory.class);
        Transaction tx = mock(Transaction.class);
        Session session = sessionWith(tx);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        PreparedStatement lock = mock(PreparedStatement.class);
        NativeQuery<?> ddl = mock(NativeQuery.class);
        when(sessionFactory.openSession()).thenReturn(session);
        when(connection.createStatement()).thenReturn(statement);
        when(connection.prepareStatement(anyString())).thenReturn(lock);
        doReturn(ddl).when(session).createNativeQuery(anyString());
        doAnswer(inv -> {
            inv.<Work>getArgument(0).execute(connection);
            return null;
        }).when(session).doWork(any(Work.class));

        try (MockedStatic<KruizeHibernateUtil> hibernate = mockStatic(KruizeHibernateUtil.class)) {
            hibernate.when(KruizeHibernateUtil::getSessionFactory).thenReturn(sessionFactory);

            // 31 Jan by_month: exactly one partition DDL
            new ExperimentDAOImpl().addPartitions(DBConstants.TABLE_NAMES.KRUIZE_RESULTS, "01", "2024", 31,
                    DBConstants.PARTITION_TYPES.BY_MONTH);

            InOrder order = inOrder(session, statement, connection, lock, ddl, tx);
            order.verify(session).beginTransaction();
            order.verify(statement).execute("SET LOCAL lock_timeout = '30s'");
            order.verify(connection).prepareStatement("SELECT pg_advisory_xact_lock(?)");
            order.verify(lock).setLong(1, 0x4B5255495A45L);   // PARTITION_DDL_ADVISORY_LOCK_KEY
            order.verify(lock).execute();
            order.verify(ddl).executeUpdate();
            order.verify(tx).commit();
        }
    }

    // ---- Round 4 review fixes ----

    @Test
    @DisplayName("Per-row commit throws and the stored start time differs by less than 1 ms: reported failed")
    public void testLostRowCommitWithSubMillisecondStartTimeDifferenceFails() throws Exception {
        SessionFactory sessionFactory = mock(SessionFactory.class);
        KruizeResultsEntry entry = entryWithPayload();
        Timestamp start = Timestamp.valueOf("2024-01-01 00:00:00.123456");
        entry.setInterval_start_time(start);
        KruizeResultsEntry stored = copyOf(entry);
        Timestamp storedStart = new Timestamp(start.getTime());
        storedStart.setNanos(123_457_000);   // same millisecond, 1 µs later
        stored.setInterval_start_time(storedStart);

        List<KruizeResultsEntry> failed = runWithLostRowCommit(entry, lookupSessionFor(entry, List.of(stored)),
                sessionFactory, mock(Transaction.class));

        assertEquals(List.of(entry), failed);
        assertEquals(List.of("commit acknowledgement lost"), entry.getErrorReasons());
    }

    @Test
    @DisplayName("addPartitions issues one CREATE TABLE IF NOT EXISTS ... PARTITION OF kruize_results per remaining day")
    public void testAddPartitionsCreatesExpectedKruizeResultsPartitions() throws Exception {
        SessionFactory sessionFactory = mock(SessionFactory.class);
        Transaction tx = mock(Transaction.class);
        Session session = sessionWith(tx);
        Connection connection = mock(Connection.class);
        NativeQuery<?> ddl = mock(NativeQuery.class);
        when(sessionFactory.openSession()).thenReturn(session);
        when(connection.createStatement()).thenReturn(mock(Statement.class));
        when(connection.prepareStatement(anyString())).thenReturn(mock(PreparedStatement.class));
        doReturn(ddl).when(session).createNativeQuery(anyString());
        doAnswer(inv -> {
            inv.<Work>getArgument(0).execute(connection);
            return null;
        }).when(session).doWork(any(Work.class));

        try (MockedStatic<KruizeHibernateUtil> hibernate = mockStatic(KruizeHibernateUtil.class)) {
            hibernate.when(KruizeHibernateUtil::getSessionFactory).thenReturn(sessionFactory);

            new ExperimentDAOImpl().addPartitions(DBConstants.TABLE_NAMES.KRUIZE_RESULTS, "01", "2024", 29,
                    DBConstants.PARTITION_TYPES.BY_MONTH);

            ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
            verify(session, times(3)).createNativeQuery(sql.capture());
            assertEquals(List.of(
                    "CREATE TABLE IF NOT EXISTS kruize_results_20240129 PARTITION OF kruize_results FOR VALUES FROM ('2024-01-29 00:00:00.000') TO ('2024-01-29 23:59:59');",
                    "CREATE TABLE IF NOT EXISTS kruize_results_20240130 PARTITION OF kruize_results FOR VALUES FROM ('2024-01-30 00:00:00.000') TO ('2024-01-30 23:59:59');",
                    "CREATE TABLE IF NOT EXISTS kruize_results_20240131 PARTITION OF kruize_results FOR VALUES FROM ('2024-01-31 00:00:00.000') TO ('2024-01-31 23:59:59');"),
                    sql.getAllValues());
            verify(ddl, times(3)).executeUpdate();
            verify(tx).commit();
        }
    }

    @Test
    @DisplayName("Missing partition for a 20 Jan row creates kruize_results partitions from 5 Jan and from 1 Jan")
    public void testMissingPartitionCreatesKruizeResultsPartitionsForEntryDate() {
        SessionFactory sessionFactory = mock(SessionFactory.class);
        Session batchSession = sessionWith(mock(Transaction.class));
        Session firstTrySession = sessionWith(mock(Transaction.class));
        Session retrySession = sessionWith(mock(Transaction.class));
        when(sessionFactory.openSession()).thenReturn(batchSession, firstTrySession, retrySession);

        KruizeResultsEntry entry = resultsEntry("exp-a", Instant.parse("2024-01-20T12:00:00Z"));
        entry.setInterval_end_time(Timestamp.valueOf("2024-01-20 12:00:00"));   // local date is 20 Jan in any zone
        PersistenceException noPartition = dbError(DBConstants.DB_MESSAGES.NO_PARTITION_RELATION + " \"kruize_results\" found for row");
        doThrow(noPartition).when(batchSession).flush();
        doThrow(noPartition).when(firstTrySession).flush();

        ExperimentDAOImpl dao = spy(new ExperimentDAOImpl());
        doNothing().when(dao).addPartitions(anyString(), anyString(), anyString(), anyInt(), anyString());

        try (MockedStatic<KruizeHibernateUtil> hibernate = mockStatic(KruizeHibernateUtil.class)) {
            hibernate.when(KruizeHibernateUtil::getSessionFactory).thenReturn(sessionFactory);

            assertTrue(dao.addToDBAndFetchFailedResults(List.of(entry)).isEmpty());

            String results = DBConstants.TABLE_NAMES.KRUIZE_RESULTS;
            String byMonth = DBConstants.PARTITION_TYPES.BY_MONTH;
            InOrder order = inOrder(dao, retrySession);
            order.verify(dao).addPartitions(results, "01", "2024", 5, byMonth);    // 15 days back
            order.verify(dao).addPartitions(results, "01", "2024", 1, byMonth);    // day < 25: whole month
            order.verify(retrySession).persist(entry);
        }
    }
}
