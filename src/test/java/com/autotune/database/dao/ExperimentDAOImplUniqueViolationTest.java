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
package com.autotune.database.dao;

import org.hibernate.HibernateException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.JDBCConnectionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ExperimentDAOImpl#isUniqueViolation(Throwable)}, which decides whether a failed
 * insert is reported as a conflict (HTTP 409) or as a server error (HTTP 500).
 */
class ExperimentDAOImplUniqueViolationTest {

    @Test
    @DisplayName("ConstraintViolationException is a unique violation")
    void constraintViolationIsUniqueViolation() {
        assertTrue(ExperimentDAOImpl.isUniqueViolation(
                new ConstraintViolationException("duplicate", new SQLException("dup", "23505"), "pkey")));
    }

    @Test
    @DisplayName("A cause with SQLState 23505 is a unique violation")
    void sqlStateInCauseIsUniqueViolation() {
        assertTrue(ExperimentDAOImpl.isUniqueViolation(
                new HibernateException("could not execute", new SQLException("dup", "23505"))));
    }

    @Test
    @DisplayName("Other database failures are not unique violations")
    void otherFailuresAreNotUniqueViolations() {
        assertFalse(ExperimentDAOImpl.isUniqueViolation(
                new JDBCConnectionException("refused", new SQLException("refused", "08001"))));
        assertFalse(ExperimentDAOImpl.isUniqueViolation(new HibernateException("other")));
        assertFalse(ExperimentDAOImpl.isUniqueViolation(null));
    }
}
