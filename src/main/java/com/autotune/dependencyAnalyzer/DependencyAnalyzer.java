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
package com.autotune.dependencyAnalyzer;

import com.autotune.dependencyAnalyzer.deployment.AutotuneDeployment;
import com.autotune.dependencyAnalyzer.service.*;
import com.autotune.dependencyAnalyzer.util.ServerContext;
import org.eclipse.jetty.servlet.ServletContextHandler;

public class DependencyAnalyzer
{
	public static void start(ServletContextHandler contextHandler) {
		AutotuneDeployment autotuneDeployment = new AutotuneDeployment();

		try {
			addServlets(contextHandler);
			AutotuneDeployment.getAutotuneObjects(autotuneDeployment);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void addServlets(ServletContextHandler context) {
		context.addServlet(ListApplicationService.class, ServerContext.LIST_APPLICATIONS);
		context.addServlet(ListAppLayers.class, ServerContext.LIST_APP_LAYERS);
		context.addServlet(ListAppTunables.class, ServerContext.LIST_APP_TUNABLES);
		context.addServlet(ListAutotuneTunables.class, ServerContext.LIST_AUTOTUNE_TUNABLES);
		context.addServlet(SearchSpace.class, ServerContext.SEARCH_SPACE);
	}
}
