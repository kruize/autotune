/*******************************************************************************
 * Copyright (c) 2023 Red Hat, IBM Corporation and others.
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
package com.autotune.database.init;


import com.autotune.database.table.KruizeExperimentEntry;
import com.autotune.database.table.KruizeRecommendationEntry;
import com.autotune.database.table.KruizeResultsEntry;
import com.autotune.operator.KruizeDeploymentInfo;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KruizeHibernateUtil {
    private static final SessionFactory sessionFactory;
    private static final Logger LOGGER = LoggerFactory.getLogger(KruizeHibernateUtil.class);

    static {
        SessionFactory sfTemp = null;
        try {
            Configuration configuration = new Configuration();
            String connectionURL = KruizeDeploymentInfo.SETTINGS_DB_DRIVER +
                    KruizeDeploymentInfo.DATABASE_HOSTNAME +
                    ":" + Integer.parseInt(KruizeDeploymentInfo.DATABASE_PORT) +
                    "/" + KruizeDeploymentInfo.DATABASE_DBNAME;
            configuration.setProperty("hibernate.connection.url", connectionURL);
            configuration.setProperty("hibernate.connection.username", KruizeDeploymentInfo.DATABASE_USERNAME);
            configuration.setProperty("hibernate.connection.password", KruizeDeploymentInfo.DATABASE_PASSWORD);
            configuration.setProperty("hibernate.dialect", KruizeDeploymentInfo.SETTINGS_HIBERNATE_DIALECT);
            configuration.setProperty("hibernate.connection.driver_class", KruizeDeploymentInfo.SETTINGS_HIBERNATE_CONNECTION_DRIVER_CLASS);
            configuration.setProperty("hibernate.c3p0.min_size", KruizeDeploymentInfo.SETTINGS_HIBERNATE_C3P0_MIN_SIZE);
            configuration.setProperty("hibernate.c3p0.max_size", KruizeDeploymentInfo.SETTINGS_HIBERNATE_C3P0_MAX_SIZE);
            configuration.setProperty("hibernate.c3p0.timeout", KruizeDeploymentInfo.SETTINGS_HIBERNATE_C3P0_TIMEOUT);
            configuration.setProperty("hibernate.c3p0.max_statements", KruizeDeploymentInfo.SETTINGS_HIBERNATE_C3P0_MAX_STATEMENTS);
            configuration.setProperty("hibernate.hbm2ddl.auto", KruizeDeploymentInfo.SETTINGS_HIBERNATE_HBM2DDL_AUTO);
            configuration.setProperty("hibernate.show_sql", KruizeDeploymentInfo.SETTINGS_HIBERNATE_SHOW_SQL);
            configuration.addAnnotatedClass(KruizeExperimentEntry.class);
            configuration.addAnnotatedClass(KruizeResultsEntry.class);
            configuration.addAnnotatedClass(KruizeRecommendationEntry.class);
            LOGGER.info("DB is trying to connect to {}", connectionURL);
            sfTemp = configuration.buildSessionFactory();
            LOGGER.info("DB build session is successful !");
        } catch (Exception e) {
            LOGGER.error("DB init failed: {}", e.getMessage());
            e.printStackTrace();
        } finally {
            sessionFactory = sfTemp;
        }
    }

    public static Session getSession() {
        return sessionFactory.getCurrentSession();
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }
}
