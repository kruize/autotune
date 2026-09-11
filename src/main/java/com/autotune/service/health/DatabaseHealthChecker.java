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

import com.autotune.database.init.KruizeHibernateUtil;
import com.autotune.utils.KruizeConstants;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Checks whether the Kruize PostgreSQL database is reachable by opening a
 * Hibernate session and executing a lightweight {@code SELECT 1} probe.
 *
 * <p>The check is intentionally minimal: it verifies connectivity and that the
 * connection pool is functional without touching any application table.
 */
public class DatabaseHealthChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseHealthChecker.class);

    /**
     * Performs the database liveness probe.
     *
     * @return a {@link DatabaseHealthResult} — never {@code null}.
     *         Status is {@code "UP"} on success, {@code "DOWN"} on any failure.
     */
    public DatabaseHealthResult check() {
        Session session = null;
        try {
            SessionFactory factory = KruizeHibernateUtil.getSessionFactory();
            if (factory == null) {
                LOGGER.warn("Hibernate SessionFactory is null — DB not initialised");
                return down();
            }
            session = factory.openSession();
            session.createNativeQuery(KruizeConstants.HealthConstants.DB_LIVENESS_QUERY, Integer.class)
                   .getSingleResult();
            LOGGER.debug("DB health check passed");
            return new DatabaseHealthResult(
                    KruizeConstants.HealthConstants.ComponentStatus.UP);
        } catch (Exception e) {
            LOGGER.warn("DB health check failed: {}", e.getMessage());
            return down();
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (Exception ignored) {
                    // best-effort close; do not mask the real result
                }
            }
        }
    }

    private DatabaseHealthResult down() {
        return new DatabaseHealthResult(
                KruizeConstants.HealthConstants.ComponentStatus.DOWN);
    }
}
