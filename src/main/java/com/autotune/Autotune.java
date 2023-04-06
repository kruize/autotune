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
package com.autotune;

import com.autotune.analyzer.Analyzer;
import com.autotune.analyzer.exceptions.KruizeErrorHandler;
import com.autotune.database.init.KruizeHibernateUtil;
import com.autotune.experimentManager.core.ExperimentManager;
import com.autotune.operator.KruizeDeploymentInfo;
import com.autotune.service.HealthService;
import com.autotune.service.InitiateListener;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.ServerContext;
import com.autotune.utils.filter.KruizeCORSFilter;
import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.DefaultExports;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import java.util.EnumSet;

import static com.autotune.utils.ServerContext.*;

public class Autotune {
    private static final Logger LOGGER = LoggerFactory.getLogger(Autotune.class);

    public static void main(String[] args) {
        ServletContextHandler context = null;

        disableServerLogging();

        Server server = new Server(KRUIZE_SERVER_PORT);
        context = new ServletContextHandler();
        context.setContextPath(ServerContext.ROOT_CONTEXT);
        context.setErrorHandler(new KruizeErrorHandler());
        context.addFilter(
                KruizeCORSFilter.getFilter(),
                KruizeConstants.CORSConstants.PATH_WILDCARD,
                EnumSet.of(DispatcherType.REQUEST)
        );
        /**
         *  Adding Listener to initiate variables during server start.
         */
        InitiateListener contextListener = new InitiateListener();
        context.addEventListener(contextListener);
        server.setHandler(context);
        server.addBean(new KruizeErrorHandler());
        addAutotuneServlets(context);
        String autotuneMode = KruizeDeploymentInfo.AUTOTUNE_MODE;

        if (null != autotuneMode) {
            if (autotuneMode.equalsIgnoreCase(KruizeConstants.StartUpMode.EM_ONLY_MODE)) {
                startAutotuneEMOnly(context);
            } else {
                startAutotuneNormalMode(context);
            }
        } else {
            startAutotuneNormalMode(context);
        }

        try {
            server.start();
            try {
                LOGGER.info("Checking DB connection...");
                SessionFactory factory = KruizeHibernateUtil.getSessionFactory();
                Session session = factory.openSession();
                session.close();
                LOGGER.info("DB connection successful!");
            } catch (Exception e) {
                LOGGER.error("DB connection failed! : {}", e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            LOGGER.error("Could not start the server!");
            e.printStackTrace();
        }
    }

    private static void addAutotuneServlets(ServletContextHandler context) {
        context.addServlet(HealthService.class, HEALTH_SERVICE);
        // Start the Prometheus end point (/metrics) for Autotune
        context.addServlet(new ServletHolder(new MetricsServlet()), METRICS_SERVICE);
        DefaultExports.initialize();
    }

    private static void disableServerLogging() {
        /* The jetty server creates a lot of server log messages that are unnecessary.
         * This disables jetty logging. */
        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
        System.setProperty("org.eclipse.jetty.LEVEL", "OFF");
    }

    private static void startAutotuneEMOnly(ServletContextHandler contextHandler) {
        ExperimentManager.launch(contextHandler);
    }

    private static void startAutotuneNormalMode(ServletContextHandler contextHandler) {
        Analyzer.start(contextHandler);
        ExperimentManager.launch(contextHandler);
    }
}
