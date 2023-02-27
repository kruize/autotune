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
package com.autotune.dbactivites.livenessProbe;

import com.autotune.dbactivites.init.KruizeHibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

public class TestDBConnection {
    public static void main(String[] args) {
        System.out.println("Checking DB connection...");
        SessionFactory factory = KruizeHibernateUtil.getSessionFactory();
        Session session = factory.openSession();
        String sql = "SELECT routine_name FROM information_schema.routines WHERE routine_schema='public' AND routine_type='FUNCTION'";
        List<String> functionNames = session.createNativeQuery(sql).getResultList();
        for (String functionName : functionNames) {
            System.out.println("Function: " + functionName);
        }
        session.close();
        System.out.println("DB connection successful!");
    }
}
