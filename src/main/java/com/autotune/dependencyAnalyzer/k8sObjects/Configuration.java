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
package com.autotune.dependencyAnalyzer.k8sObjects;

import com.autotune.dependencyAnalyzer.util.DAConstants;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;

/**
 * Holds Kubernetes configuration for autotune kinds in the cluster
 */
public class Configuration
{
    public static final CustomResourceDefinitionContext autotuneCrdContext = new CustomResourceDefinitionContext
            .Builder()
            .withGroup(DAConstants.group)
            .withScope(DAConstants.scope)
            .withPlural(DAConstants.autotunePlurals)
            .withVersion(DAConstants.apiVersion)
            .withName(DAConstants.autotuneResourceName)
            .build();

    public static final CustomResourceDefinitionContext autotuneConfigContext = new CustomResourceDefinitionContext
            .Builder()
            .withGroup(DAConstants.group)
            .withScope(DAConstants.scope)
            .withPlural(DAConstants.autotuneConfigPlurals)
            .withVersion(DAConstants.apiVersion)
            .withName(DAConstants.autotuneConfigResourceName)
            .build();

    public static final CustomResourceDefinitionContext autotuneVariableContext = new CustomResourceDefinitionContext
            .Builder()
            .withGroup(DAConstants.group)
            .withScope(DAConstants.scope)
            .withPlural(DAConstants.autotuneVariablePlurals)
            .withVersion(DAConstants.apiVersion)
            .withName(DAConstants.autotuneVariableResourceName)
            .build();
}
