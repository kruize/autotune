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
package com.autotune.dbactivites.init;

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
            Configuration configuration = new Configuration().configure();
            String connectionURL = "jdbc:postgresql://" +
                    System.getenv("DATABASE_URL") +
                    ":" + System.getenv("DB_PORT") +
                    "/" + System.getenv("DB_NAME");
            configuration.setProperty("hibernate.connection.url", connectionURL);
            configuration.setProperty("hibernate.connection.username", System.getenv("DATABASE_USER"));
            configuration.setProperty("hibernate.connection.password", System.getenv("DATABASE_PASSWORD"));
            sfTemp = configuration.buildSessionFactory();
        } catch (Exception e) {
            LOGGER.error("DB init failed due to : {}", e.getMessage());
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
