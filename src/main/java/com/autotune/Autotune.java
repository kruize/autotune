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
import com.autotune.analyzer.utils.ServerContext;
import com.autotune.service.HealthService;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Autotune
{
	private static final int PORT = 8080;
	private static final String ROOT_CONTEXT = "/";
	private static final String HEALTH_SERVICE = ROOT_CONTEXT + "health";
	private static final Logger LOGGER = LoggerFactory.getLogger(Autotune.class);

	public static void main(String[] args) {
		ServletContextHandler context = null;

		disableServerLogging();

		Server server = new Server(PORT);
		context = new ServletContextHandler();
		context.setContextPath(ServerContext.ROOT_CONTEXT);
		server.setHandler(context);
		addAutotuneServlets(context);

		Analyzer.start(context);
		try {
			server.start();
		} catch (Exception e) {
			LOGGER.error("Could not start the server!");
			e.printStackTrace();
		}
	}

	private static void addAutotuneServlets(ServletContextHandler context) {
		context.addServlet(HealthService.class, HEALTH_SERVICE);
	}

	private static void disableServerLogging() {
		/* The jetty server creates a lot of server log messages that are unnecessary.
		 * This disables jetty logging. */
		System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
		System.setProperty("org.eclipse.jetty.LEVEL", "OFF");
	}

}
