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
package com.autotune.analyzer.k8sObjects;

import com.autotune.utils.AnalyzerConstants;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;

/**
 * Holds Kubernetes contexts for autotune kinds in the cluster
 */
public class KubernetesContexts
{
	private KubernetesContexts() { }

	private static final CustomResourceDefinitionContext autotuneCrdContext = new CustomResourceDefinitionContext
			.Builder()
			.withGroup(AnalyzerConstants.GROUP)
			.withScope(AnalyzerConstants.SCOPE)
			.withPlural(AnalyzerConstants.AUTOTUNE_PLURALS)
			.withVersion(AnalyzerConstants.API_VERSION_V1)
			.withName(AnalyzerConstants.AUTOTUNE_RESOURCE_NAME)
			.build();

	private static final CustomResourceDefinitionContext autotuneConfigContext = new CustomResourceDefinitionContext
			.Builder()
			.withGroup(AnalyzerConstants.GROUP)
			.withScope(AnalyzerConstants.SCOPE)
			.withPlural(AnalyzerConstants.AUTOTUNE_CONFIG_PLURALS)
			.withVersion(AnalyzerConstants.API_VERSION_V1)
			.withName(AnalyzerConstants.AUTOTUNE_CONFIG_RESOURCE_NAME)
			.build();

	private static final CustomResourceDefinitionContext autotuneVariableContext = new CustomResourceDefinitionContext
			.Builder()
			.withGroup(AnalyzerConstants.GROUP)
			.withScope(AnalyzerConstants.SCOPE)
			.withPlural(AnalyzerConstants.AUTOTUNE_VARIABLE_PLURALS)
			.withVersion(AnalyzerConstants.API_VERSION_V1)
			.withName(AnalyzerConstants.AUTOTUNE_VARIABLE_RESOURCE_NAME)
			.build();

	public static CustomResourceDefinitionContext getAutotuneCrdContext() {
		return autotuneCrdContext;
	}

	public static CustomResourceDefinitionContext getAutotuneConfigContext() {
		return autotuneConfigContext;
	}

	public static CustomResourceDefinitionContext getAutotuneVariableContext() {
		return autotuneVariableContext;
	}
}
