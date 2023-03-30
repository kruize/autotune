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
package com.autotune.database.livenessProbe;

import com.autotune.database.init.KruizeHibernateUtil;
import com.autotune.operator.KruizeDeploymentInfo;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestDBConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestDBConnection.class);

    public static void main(String[] args) {
        if (KruizeDeploymentInfo.isSaveToDB()) {
            LOGGER.info("Checking Liveliness probe DB connection...");
            SessionFactory factory = KruizeHibernateUtil.getSessionFactory();
            Session session = factory.openSession();
            session.close();
            LOGGER.info("DB Liveliness probe connection successful!");
        } else {
            LOGGER.info("Persistent storage set to local so Testing DB connection not required.");
        }
    }
}
