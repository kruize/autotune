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


import com.autotune.database.table.*;
import com.autotune.database.table.lm.KruizeBulkJobEntry;
import com.autotune.database.table.lm.KruizeLMExperimentEntry;
import com.autotune.database.table.lm.KruizeLMMetadataProfileEntry;
import com.autotune.database.table.lm.KruizeLMRecommendationEntry;
import com.autotune.operator.KruizeDeploymentInfo;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KruizeHibernateUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(KruizeHibernateUtil.class);
    private static SessionFactory sessionFactory;

    static {
        buildSessionFactory();
    }

    public static void buildSessionFactory() {
        SessionFactory sfTemp = null;
        try {
            Configuration configuration = new Configuration();
            String connectionURL = KruizeDeploymentInfo.settings_db_driver +
                    KruizeDeploymentInfo.database_hostname +
                    ":" + Integer.parseInt(KruizeDeploymentInfo.database_port) +
                    "/" + KruizeDeploymentInfo.database_dbname;
            configuration.setProperty("hibernate.connection.url", connectionURL);
            configuration.setProperty("hibernate.connection.username", KruizeDeploymentInfo.database_username);
            configuration.setProperty("hibernate.connection.password", KruizeDeploymentInfo.database_password);
            configuration.setProperty("hibernate.dialect", KruizeDeploymentInfo.settings_hibernate_dialect);
            configuration.setProperty("hibernate.connection.driver_class", KruizeDeploymentInfo.settings_hibernate_connection_driver_class);
            configuration.setProperty("hibernate.c3p0.min_size", KruizeDeploymentInfo.settings_hibernate_c3p0_min_size);
            configuration.setProperty("hibernate.c3p0.max_size", KruizeDeploymentInfo.settings_hibernate_c3p0_max_size);
            configuration.setProperty("hibernate.c3p0.timeout", KruizeDeploymentInfo.settings_hibernate_c3p0_timeout);
            configuration.setProperty("hibernate.c3p0.max_statements", KruizeDeploymentInfo.settings_hibernate_c3p0_max_statements);
            configuration.setProperty("hibernate.hbm2ddl.auto", KruizeDeploymentInfo.settings_hibernate_hbm2ddl_auto);
            configuration.setProperty("hibernate.show_sql", KruizeDeploymentInfo.settings_hibernate_show_sql);
            configuration.setProperty("hibernate.jdbc.time_zone", KruizeDeploymentInfo.settings_hibernate_time_zone);
            configuration.addAnnotatedClass(KruizeExperimentEntry.class);
            configuration.addAnnotatedClass(KruizeResultsEntry.class);
            configuration.addAnnotatedClass(KruizeRecommendationEntry.class);
            configuration.addAnnotatedClass(KruizePerformanceProfileEntry.class);
            configuration.addAnnotatedClass(KruizePodStatus.class);
            if (KruizeDeploymentInfo.local) {
                configuration.addAnnotatedClass(KruizeLMExperimentEntry.class);
                configuration.addAnnotatedClass(KruizeLMRecommendationEntry.class);
                configuration.addAnnotatedClass(KruizeDataSourceEntry.class);
                configuration.addAnnotatedClass(KruizeDSMetadataEntry.class);
                configuration.addAnnotatedClass(KruizeMetricProfileEntry.class);
                configuration.addAnnotatedClass(KruizeAuthenticationEntry.class);
                configuration.addAnnotatedClass(KruizeLMMetadataProfileEntry.class);
                configuration.addAnnotatedClass(KruizeBulkJobEntry.class);
            }
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

    public static void closeSessionFactory() {
        if (sessionFactory != null) {
            sessionFactory.close();
            sessionFactory = null;
        }
    }
}
