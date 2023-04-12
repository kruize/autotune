/*******************************************************************************
 * Copyright (c) 2020, 2021 Red Hat, IBM Corporation and others.
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
package com.autotune.database.init.service;

import com.autotune.database.init.KruizeHibernateUtil;
import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.utils.KruizeConstants;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

@WebServlet(asyncSupported = true)
public class CheckDBStatus extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckDBStatus.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        boolean isDBConnectSuccess = false;
        if (KruizeDeploymentInfo.isSaveToDB()) {
            if (null != KruizeHibernateUtil.getSessionFactory()) {
                Session session = null;
                try {
                    session = KruizeHibernateUtil.getSessionFactory().openSession();
                    Query query = session.createQuery(KruizeConstants.SQL_DB_TEST_QUERY, null);
                    query.uniqueResult();
                    isDBConnectSuccess = true;
                } catch (Exception e) {
                    LOGGER.error("DB liveliness probe check status failed to to {} ", e.getMessage());
                } finally {
                    if (null != session) session.close();
                }
            }
        }

        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING);
        if (isDBConnectSuccess) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("UP");
        } else {
            response.setStatus(HttpServletResponse.SC_EXPECTATION_FAILED);
            response.getWriter().println("DOWN");
        }
        response.getWriter().close();
    }
}
