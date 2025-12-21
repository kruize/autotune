/*******************************************************************************
 * Copyright (c) 2026 Red Hat, IBM Corporation and others.
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
package com.autotune.analyzer.kruizeLayer.presence;

import com.autotune.analyzer.exceptions.MonitoringAgentNotSupportedException;
import com.autotune.utils.KruizeSupportedTypes;
import com.google.gson.annotations.SerializedName;

/**
 * Used to detect the presence of the layer in an application. Autotune runs the query, looks for
 * the key, and all applications in the query output are matched to the KruizeLayer object.
 */
public class LayerPresenceQuery {
	@SerializedName("datasource")
	private String dataSource;

	@SerializedName("key")
	private String layerPresenceKey;

	@SerializedName("query")
	private String layerPresenceQuery;

	public LayerPresenceQuery() {
	}

	public LayerPresenceQuery(String datasource,
							  String layerPresenceQuery,
							  String layerPresenceKey) throws MonitoringAgentNotSupportedException {

		if (KruizeSupportedTypes.MONITORING_AGENTS_SUPPORTED.contains(datasource)) {
			this.dataSource = datasource;
		} else {
			throw new MonitoringAgentNotSupportedException(
				"Unsupported monitoring agent datasource: '" + datasource + "'. " +
				"Supported datasources: " + KruizeSupportedTypes.MONITORING_AGENTS_SUPPORTED
			);
		}
		this.layerPresenceQuery = layerPresenceQuery;
		this.layerPresenceKey = layerPresenceKey;
	}

	public String getDataSource() {
		return dataSource;
	}

	public void setDataSource(String dataSource) {
		this.dataSource = dataSource;
	}

	public String getLayerPresenceKey() {
		return layerPresenceKey;
	}

	public void setLayerPresenceKey(String layerPresenceKey) {
		this.layerPresenceKey = layerPresenceKey;
	}

	public String getLayerPresenceQuery() {
		return layerPresenceQuery;
	}

	public void setLayerPresenceQuery(String layerPresenceQuery) {
		this.layerPresenceQuery = layerPresenceQuery;
	}

	@Override
	public String toString() {
		return "LayerPresenceQuery{" +
				"dataSource='" + dataSource + '\'' +
				", layerPresenceKey='" + layerPresenceKey + '\'' +
				", layerPresenceQuery='" + layerPresenceQuery + '\'' +
				'}';
	}
}
