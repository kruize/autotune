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
package com.autotune.dependencyAnalyzer.util;

import java.io.File;

/**
 * Holds the server context of the dependency analyzer.
 */
public class ServerContext
{
	public static final String ROOT_CONTEXT = File.separator;
	public static final String LIST_APPLICATIONS = ROOT_CONTEXT + "listApplications";
	public static final String LIST_APP_LAYERS = ROOT_CONTEXT + "listAppLayers";
	public static final String LIST_APP_TUNABLES = ROOT_CONTEXT + "listAppTunables";
	public static final String LIST_AUTOTUNE_TUNABLES = ROOT_CONTEXT + "listAutotuneTunables";
	public static final String SEARCH_SPACE = ROOT_CONTEXT + "searchSpace";
	public static final String HEALTH_SERVICE = ROOT_CONTEXT + "/health";
}
