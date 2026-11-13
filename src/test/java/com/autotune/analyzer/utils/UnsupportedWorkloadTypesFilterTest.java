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
package com.autotune.analyzer.utils;

import com.autotune.utils.Utils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the unsupported workload type filter:
 * {@link Utils#getAppropriateK8sObjectTypeString} and {@link AnalyzerConstants#getUnsupportedWorkloadTypesFilter()}.
 */
class UnsupportedWorkloadTypesFilterTest {

    @Test
    @DisplayName("Every K8S_OBJECT_TYPES value maps to a non-null workload type string")
    void everyK8sObjectTypeHasStringMapping() {
        for (AnalyzerConstants.K8S_OBJECT_TYPES type : AnalyzerConstants.K8S_OBJECT_TYPES.values()) {
            assertNotNull(Utils.getAppropriateK8sObjectTypeString(type),
                    "Missing string mapping for K8S_OBJECT_TYPES." + type);
        }
    }

    @Test
    @DisplayName("Newly mapped types return the expected strings")
    void newlyMappedTypesReturnExpectedStrings() {
        assertEquals("job", Utils.getAppropriateK8sObjectTypeString(AnalyzerConstants.K8S_OBJECT_TYPES.JOB));
        assertEquals("ConfigMap", Utils.getAppropriateK8sObjectTypeString(AnalyzerConstants.K8S_OBJECT_TYPES.CONFIGMAP));
        assertEquals("CatalogSource", Utils.getAppropriateK8sObjectTypeString(AnalyzerConstants.K8S_OBJECT_TYPES.CATALOGSOURCE));
        assertEquals("CronJob", Utils.getAppropriateK8sObjectTypeString(AnalyzerConstants.K8S_OBJECT_TYPES.CRONJOB));
        assertEquals("barepod", Utils.getAppropriateK8sObjectTypeString(AnalyzerConstants.K8S_OBJECT_TYPES.BAREPOD));
    }

    @Test
    @DisplayName("Unsupported list covers every unsupported workload type reported in the issue")
    void unsupportedListCoversIssueTypes() {
        assertTrue(AnalyzerConstants.UNSUPPORTED_K8S_OBJECT_TYPES.containsAll(java.util.List.of(
                AnalyzerConstants.K8S_OBJECT_TYPES.CONFIGMAP,
                AnalyzerConstants.K8S_OBJECT_TYPES.CATALOGSOURCE,
                AnalyzerConstants.K8S_OBJECT_TYPES.CRONJOB,
                AnalyzerConstants.K8S_OBJECT_TYPES.BAREPOD)));
    }

    @Test
    @DisplayName("Every type is either supported (accepted by remote monitoring createExperiment) or in the unsupported list, never both")
    void supportedAndUnsupportedAreComplementary() {
        for (AnalyzerConstants.K8S_OBJECT_TYPES type : AnalyzerConstants.K8S_OBJECT_TYPES.values()) {
            // DeploymentConfig is the one known exception: it is filtered out of local monitoring metadata
            // discovery (deprecated from OpenShift 4.14) but is still accepted by remote monitoring createExperiment
            if (type == AnalyzerConstants.K8S_OBJECT_TYPES.DEPLOYMENT_CONFIG) {
                continue;
            }
            // This is the same check ExperimentValidation uses for remote monitoring
            boolean supported = type == Utils.getApproriateK8sObjectType(Utils.getAppropriateK8sObjectTypeString(type));
            boolean unsupported = AnalyzerConstants.UNSUPPORTED_K8S_OBJECT_TYPES.contains(type);
            assertNotEquals(supported, unsupported,
                    type + " must be exactly one of supported (remote monitoring) or unsupported");
        }
    }

    @Test
    @DisplayName("DeploymentConfig is the known exception: filtered from local discovery but still accepted by createExperiment")
    void deploymentConfigIsKnownException() {
        assertTrue(AnalyzerConstants.UNSUPPORTED_K8S_OBJECT_TYPES.contains(AnalyzerConstants.K8S_OBJECT_TYPES.DEPLOYMENT_CONFIG));
        assertEquals(AnalyzerConstants.K8S_OBJECT_TYPES.DEPLOYMENT_CONFIG, Utils.getApproriateK8sObjectType("deploymentConfig"));
    }

    @Test
    @DisplayName("Types supported by remote monitoring are not excluded by the filter")
    void supportedTypesAreNotFiltered() {
        String filter = AnalyzerConstants.getUnsupportedWorkloadTypesFilter();
        for (String supported : new String[]{"deployment", "statefulset", "replicaset",
                "replicationController", "daemonset", "job"}) {
            assertFalse(filter.contains("(?i)" + supported + "\""),
                    supported + " is supported by remote monitoring and must not be filtered out");
        }
    }

    @Test
    @DisplayName("Null object type returns null")
    void nullTypeReturnsNull() {
        assertNull(Utils.getAppropriateK8sObjectTypeString(null));
    }

    @Test
    @DisplayName("Filter excludes DeploymentConfig, ConfigMap, CatalogSource, CronJob and barepod")
    void filterContainsAllUnsupportedTypes() {
        String expected = ", workload_type!~\"(?i)deploymentConfig\""
                + ", workload_type!~\"(?i)ConfigMap\""
                + ", workload_type!~\"(?i)CatalogSource\""
                + ", workload_type!~\"(?i)CronJob\""
                + ", workload_type!~\"(?i)barepod\"";
        assertEquals(expected, AnalyzerConstants.getUnsupportedWorkloadTypesFilter());
    }

    @Test
    @DisplayName("Filter never contains the literal string 'null'")
    void filterDoesNotContainNull() {
        assertFalse(AnalyzerConstants.getUnsupportedWorkloadTypesFilter().contains("null"));
    }

    @Test
    @DisplayName("Filter has one clause per unsupported type")
    void filterHasOneClausePerUnsupportedType() {
        String filter = AnalyzerConstants.getUnsupportedWorkloadTypesFilter();
        int clauses = filter.split("workload_type!~", -1).length - 1;
        assertEquals(AnalyzerConstants.UNSUPPORTED_K8S_OBJECT_TYPES.size(), clauses);
    }

    @Test
    @DisplayName("Every unsupported type has a string mapping, so the filter builder cannot fail")
    void unsupportedTypesAllMapped() {
        for (AnalyzerConstants.K8S_OBJECT_TYPES type : AnalyzerConstants.UNSUPPORTED_K8S_OBJECT_TYPES) {
            assertNotNull(Utils.getAppropriateK8sObjectTypeString(type));
        }
    }
}
