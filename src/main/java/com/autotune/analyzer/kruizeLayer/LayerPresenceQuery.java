/*******************************************************************************
 * Copyright (c) 2022, 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.analyzer.kruizeLayer;

import com.autotune.analyzer.exceptions.MonitoringAgentNotSupportedException;
import com.autotune.utils.KruizeSupportedTypes;

/**
 * Used to detect the presence of the layer in an application. Autotune runs the query, looks for
 * the key, and all applications in the query output are matched to the KruizeLayer object.
 */
public class LayerPresenceQuery {
	private final String dataSource;
	private final String layerPresenceKey;
	private final String layerPresenceQuery;

	public LayerPresenceQuery(String datasource,
							  String layerPresenceQuery,
							  String layerPresenceKey) throws MonitoringAgentNotSupportedException {

		if (KruizeSupportedTypes.DATASOURCE_PROVIDERS_SUPPORTED.contains(datasource)) {
			this.dataSource = datasource;
		} else {
			throw new MonitoringAgentNotSupportedException();
		}
		this.layerPresenceQuery = layerPresenceQuery;
		this.layerPresenceKey = layerPresenceKey;
	}

	public String getLayerPresenceKey() { return layerPresenceKey; }

	public String getLayerPresenceQuery() {	return layerPresenceQuery; }
}
