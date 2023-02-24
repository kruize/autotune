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

import com.autotune.analyzer.serviceObjects.CreateExperimentSO;
import com.autotune.common.k8sObjects.DeploymentObject;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.common.k8sObjects.KruizeObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;

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

	public static class Converters {
		private Converters() {

		}

		public static class KruizeObjectConverters {
			private KruizeObjectConverters() {

			}

			public static CreateExperimentSO convertKruizeObjectToCreateExperimentSO(KruizeObject kruizeObject) {
				// Need to be implemented if needed
				return null;
			}

			public static KruizeObject convertCreateExperimentSOToKruizeObject(CreateExperimentSO createExperimentSO) {
				// To be implemented
				KruizeObject kruizeObject =  new KruizeObject();
				HashMap<String, DeploymentObject> deploymentObjectHashMap = new HashMap<String, DeploymentObject>();
				for (K8sObject k8sObject: createExperimentSO.getKubernetesObjects()) {
					if (null != k8sObject.getName() && !k8sObject.getName().isBlank()) {
						DeploymentObject deploymentObject = new DeploymentObject(k8sObject.getName());
						AnalyzerConstants.K8S_OBJECT_TYPES objectType = getApproriateK8sObjectType(k8sObject.getType());
						if (null != objectType)
							deploymentObject.setType(objectType);
					}


				}
				return null;
			}
		}
	}
}
