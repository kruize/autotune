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

import com.autotune.dependencyAnalyzer.DependencyAnalyzer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

public class Autotune
{
	public static void main(String[] args) {
		ServletContextHandler context = null;

		disableServerLogging();

		Server server = new Server(8080);
		context = new ServletContextHandler();
		context.setContextPath("/");
		server.setHandler(context);

		DependencyAnalyzer.start(context);
		try {
			server.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void disableServerLogging() {
		/* The jetty server creates a lot of server log messages that are unnecessary.
		 * This disables jetty logging. */
		System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
		System.setProperty("org.eclipse.jetty.LEVEL", "OFF");
	}

}
