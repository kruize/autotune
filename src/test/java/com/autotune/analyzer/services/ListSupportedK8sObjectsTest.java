/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.autotune.analyzer.services;

import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.utils.Utils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * /listK8sObjects must report exactly the types remote monitoring createExperiment accepts.
 */
class ListSupportedK8sObjectsTest {

    @Test
    @DisplayName("Lists every type accepted by createExperiment, including DeploymentConfig")
    void listsTypesAcceptedByCreateExperiment() {
        List<String> supported = ListSupportedK8sObjects.getSupportedK8sObjects();
        assertEquals(List.of("deployment", "deploymentConfig", "statefulset", "replicaset",
                "replicationController", "daemonset", "job"), supported);
    }

    @Test
    @DisplayName("Does not list ConfigMap or CatalogSource")
    void doesNotListUnsupportedTypes() {
        List<String> supported = ListSupportedK8sObjects.getSupportedK8sObjects();
        assertFalse(supported.contains("ConfigMap"));
        assertFalse(supported.contains("CatalogSource"));
    }

    @Test
    @DisplayName("Every listed type is accepted by the createExperiment type check")
    void everyListedTypeIsAcceptedByCreateExperiment() {
        for (String type : ListSupportedK8sObjects.getSupportedK8sObjects()) {
            assertNotNull(Utils.getApproriateK8sObjectType(type), type + " would be rejected by createExperiment");
        }
    }

    @Test
    @DisplayName("DeploymentConfig is excluded from local discovery but still listed for remote clients")
    void deploymentConfigStaysListed() {
        assertTrue(AnalyzerConstants.UNSUPPORTED_K8S_OBJECT_TYPES.contains(AnalyzerConstants.K8S_OBJECT_TYPES.DEPLOYMENT_CONFIG));
        assertTrue(ListSupportedK8sObjects.getSupportedK8sObjects().contains("deploymentConfig"));
    }
}
