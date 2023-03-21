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
package com.autotune.utils;

import com.autotune.analyzer.recommendations.Recommendation;
import com.autotune.analyzer.serviceObjects.ContainerMetricsHelper;
import com.autotune.analyzer.serviceObjects.CreateExperimentSO;
import com.autotune.analyzer.serviceObjects.ListRecommendationsSO;
import com.autotune.analyzer.serviceObjects.UpdateResultsSO;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.*;
import com.autotune.common.k8sObjects.ContainerObject;
import com.autotune.common.k8sObjects.DeploymentObject;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.analyzer.kruizeObject.KruizeObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Contains methods that are of general utility in the codebase
 */
public class Utils
{
	private Utils() { }

	public static String generateID(Object object) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(object.toString().getBytes(StandardCharsets.UTF_8));

			StringBuffer hexString = new StringBuffer();

			for (int i = 0; i < hash.length; i++) {
				String hex = Integer.toHexString(0xff & hash[i]);
				if (hex.length() == 1) hexString.append('0');
				hexString.append(hex);
			}

			return hexString.toString();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public static AnalyzerConstants.K8S_OBJECT_TYPES getApproriateK8sObjectType(String objectType) {
		if (null == objectType)
			return null;

		if (objectType.isEmpty() || objectType.isBlank())
			return null;

		objectType = objectType.trim();

		if (objectType.equalsIgnoreCase(AnalyzerConstants.K8sObjectConstants.Types.DEPLOYMENT))
			return AnalyzerConstants.K8S_OBJECT_TYPES.DEPLOYMENT;

		if (objectType.equalsIgnoreCase(AnalyzerConstants.K8sObjectConstants.Types.DEPLOYMENT_CONFIG))
			return AnalyzerConstants.K8S_OBJECT_TYPES.DEPLOYMENT_CONFIG;

		if (objectType.equalsIgnoreCase(AnalyzerConstants.K8sObjectConstants.Types.STATEFULSET))
			return AnalyzerConstants.K8S_OBJECT_TYPES.STATEFULSET;

		if (objectType.equalsIgnoreCase(AnalyzerConstants.K8sObjectConstants.Types.REPLICASET))
			return AnalyzerConstants.K8S_OBJECT_TYPES.REPLICASET;

		if (objectType.equalsIgnoreCase(AnalyzerConstants.K8sObjectConstants.Types.REPLICATION_CONTROLLER))
			return AnalyzerConstants.K8S_OBJECT_TYPES.REPLICATION_CONTROLLER;

		if (objectType.equalsIgnoreCase(AnalyzerConstants.K8sObjectConstants.Types.DAEMONSET))
			return AnalyzerConstants.K8S_OBJECT_TYPES.DAEMONSET;

		return null;
	}

	public static String getAppropriateK8sObjectTypeString(AnalyzerConstants.K8S_OBJECT_TYPES objectType) {
		if (null == objectType)
			return null;

		if (objectType == AnalyzerConstants.K8S_OBJECT_TYPES.DEPLOYMENT)
			return AnalyzerConstants.K8sObjectConstants.Types.DEPLOYMENT;

		if (objectType == AnalyzerConstants.K8S_OBJECT_TYPES.DEPLOYMENT_CONFIG)
			return AnalyzerConstants.K8sObjectConstants.Types.DEPLOYMENT_CONFIG;

		if (objectType == AnalyzerConstants.K8S_OBJECT_TYPES.STATEFULSET)
			return AnalyzerConstants.K8sObjectConstants.Types.STATEFULSET;

		if (objectType == AnalyzerConstants.K8S_OBJECT_TYPES.REPLICASET)
			return AnalyzerConstants.K8sObjectConstants.Types.REPLICASET;

		if (objectType == AnalyzerConstants.K8S_OBJECT_TYPES.REPLICATION_CONTROLLER)
			return AnalyzerConstants.K8sObjectConstants.Types.REPLICATION_CONTROLLER;

		if (objectType == AnalyzerConstants.K8S_OBJECT_TYPES.DAEMONSET)
			return AnalyzerConstants.K8sObjectConstants.Types.DAEMONSET;

		return null;
	}

	public static AnalyzerConstants.MetricName getAppropriateMetricName(String metricName) {
		if (null == metricName)
			return null;

		if (metricName.equalsIgnoreCase(AnalyzerConstants.MetricNameConstants.CPU_REQUEST))
			return AnalyzerConstants.MetricName.cpuRequest;

		if (metricName.equalsIgnoreCase(AnalyzerConstants.MetricNameConstants.CPU_LIMIT))
			return AnalyzerConstants.MetricName.cpuLimit;

		if (metricName.equalsIgnoreCase(AnalyzerConstants.MetricNameConstants.CPU_USAGE))
			return AnalyzerConstants.MetricName.cpuUsage;

		if (metricName.equalsIgnoreCase(AnalyzerConstants.MetricNameConstants.CPU_THROTTLE))
			return AnalyzerConstants.MetricName.cpuThrottle;

		if (metricName.equalsIgnoreCase(AnalyzerConstants.MetricNameConstants.MEMORY_REQUEST))
			return AnalyzerConstants.MetricName.memoryRequest;

		if (metricName.equalsIgnoreCase(AnalyzerConstants.MetricNameConstants.MEMORY_LIMIT))
			return AnalyzerConstants.MetricName.memoryLimit;

		if (metricName.equalsIgnoreCase(AnalyzerConstants.MetricNameConstants.MEMORY_USAGE))
			return AnalyzerConstants.MetricName.memoryUsage;

		if (metricName.equalsIgnoreCase(AnalyzerConstants.MetricNameConstants.MEMORY_RSS))
			return AnalyzerConstants.MetricName.memoryRSS;

		return null;
	}

	/**
	 * Get a deep copy of an object
	 *
	 * CAUTION: Using this mechanism will have high impact on the performance. As
	 * this causes performance degradation USE ONLY IF NECESSARY
	 *
	 * @param object
	 * @return
	 */
	public static Object getClone(Object object) {
		if (null == object)
			return null;

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream = null;
		try {
			objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
			objectOutputStream.writeObject(object);
			objectOutputStream.flush();
			objectOutputStream.close();
			byteArrayOutputStream.close();
			ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
			Object cloneObject = new ObjectInputStream(byteArrayInputStream).readObject();
			return cloneObject;
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
}
