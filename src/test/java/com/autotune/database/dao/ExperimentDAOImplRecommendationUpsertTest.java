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

import com.autotune.database.helper.DBConstants;
import com.autotune.database.init.KruizeHibernateUtil;
import com.autotune.database.table.KruizeRecommendationEntry;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.Timestamp;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Remote-monitoring recommendation writes are one upsert on the existing primary key.
 */
public class ExperimentDAOImplRecommendationUpsertTest {

    @Test
    @DisplayName("upserts extended_data and accepts a null cluster name")
    public void upsertsOnPrimaryKey() {
        SessionFactory sessionFactory = mock(SessionFactory.class);
        Session session = mock(Session.class);
        Transaction tx = mock(Transaction.class);
        @SuppressWarnings("unchecked")
        NativeQuery<Object> query = mock(NativeQuery.class);
        when(sessionFactory.openSession()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(tx);
        when(session.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), nullable(Object.class), any(Class.class))).thenReturn(query);
        when(query.executeUpdate()).thenReturn(1);

        KruizeRecommendationEntry entry = new KruizeRecommendationEntry();
        entry.setVersion("1.0");
        entry.setExperiment_name("demo-exp");
        entry.setCluster_name(null);
        entry.setInterval_end_time(Timestamp.from(Instant.parse("2024-01-01T00:15:00Z")));
        entry.setExtended_data(JsonNodeFactory.instance.objectNode().put("kubernetes_objects", "[]"));

        try (var hibernate = mockStatic(KruizeHibernateUtil.class)) {
            hibernate.when(KruizeHibernateUtil::getSessionFactory).thenReturn(sessionFactory);

            assertTrue(new ExperimentDAOImpl().addRecommendationToDB(entry).isSuccess());
        }

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(session, times(1)).createNativeQuery(sql.capture());
        assertEquals(DBConstants.SQLQUERY.UPSERT_KRUIZE_RECOMMENDATION, sql.getValue());
        assertTrue(sql.getValue().contains("ON CONFLICT (experiment_name, interval_end_time)"));
        assertTrue(sql.getValue().contains("DO UPDATE SET extended_data = EXCLUDED.extended_data"));
        verify(query).setParameter(eq("clusterName"), isNull(), eq(String.class));
        verify(query, times(1)).executeUpdate();
        verify(tx, times(1)).commit();
        verify(session, never()).persist(any());
        verify(session, never()).merge(any());
    }
}
