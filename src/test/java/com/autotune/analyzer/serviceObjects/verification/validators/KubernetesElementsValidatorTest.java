/*******************************************************************************
 * Copyright (c) 2023 Red Hat, IBM Corporation and others.
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
package com.autotune.analyzer.serviceObjects.verification.validators;

import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.serviceObjects.ContainerAPIObject;
import com.autotune.analyzer.serviceObjects.KubernetesAPIObject;
import com.autotune.analyzer.serviceObjects.NamespaceAPIObject;
import com.autotune.analyzer.serviceObjects.UpdateResultsAPIObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.k8sObjects.K8sObject;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link KubernetesElementsValidator}.
 * Calls {@code isValid} directly so performance-profile validation and the database are not involved.
 */
@ExtendWith(MockitoExtension.class)
class KubernetesElementsValidatorTest {

    private static final String EXP = "demo-exp";
    private static final String TYPE_MISMATCH =
            "Kubernetes Object Types MisMatched. Expected Type: %s, Found: %s in Results for experiment: %s \n";
    private static final String NAME_MISMATCH =
            "Kubernetes Object Names MisMatched. Expected Name: %s, Found: %s in Results for experiment: %s \n";
    private static final String NAMESPACE_MISMATCH =
            "Kubernetes Object Namespaces MisMatched. Expected Namespace: %s, Found: %s in Results for experiment: %s \n";

    @Mock
    private ConstraintValidatorContext context;
    @Mock
    private ConstraintViolationBuilder violationBuilder;
    @Mock
    private NodeBuilderCustomizableContext nodeBuilder;

    private KubernetesElementsValidator validator;

    @BeforeEach
    void setUp() {
        validator = new KubernetesElementsValidator();
        lenient().when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        lenient().when(violationBuilder.addPropertyNode(anyString())).thenReturn(nodeBuilder);
        lenient().when(nodeBuilder.addConstraintViolation()).thenReturn(context);
    }

    @Test
    @DisplayName("matching container experiment passes without reading metrics")
    void matchingContainerExperimentPasses() {
        UpdateResultsAPIObject api = containerUpdate("deployment", "demo-app", "default", "app");

        assertTrue(validator.isValid(api, context));
        verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    @DisplayName("type mismatch fails with the type message")
    void typeMismatch() {
        UpdateResultsAPIObject api = containerUpdate("daemonset", "demo-app", "default", "app");

        assertFalse(validator.isValid(api, context));
        assertViolation(
                String.format(TYPE_MISMATCH, "deployment", "daemonset", EXP),
                "kubernetes_objects ");
    }

    @Test
    @DisplayName("name mismatch fails with the name message")
    void nameMismatch() {
        UpdateResultsAPIObject api = containerUpdate("deployment", "other-app", "default", "app");

        assertFalse(validator.isValid(api, context));
        assertViolation(
                String.format(NAME_MISMATCH, "demo-app", "other-app", EXP),
                "kubernetes_objects ");
    }

    @Test
    @DisplayName("namespace mismatch fails for a container experiment")
    void namespaceMismatchForContainerExperiment() {
        UpdateResultsAPIObject api = containerUpdate("deployment", "demo-app", "kube-system", "app");

        assertFalse(validator.isValid(api, context));
        assertViolation(
                String.format(NAMESPACE_MISMATCH, "default", "kube-system", EXP),
                "kubernetes_objects ");
    }

    @Test
    @DisplayName("namespace mismatch is not reported for a namespace experiment when a result field is null")
    void namespaceMismatchIgnoredForNamespaceExperiment() {
        UpdateResultsAPIObject api = namespaceUpdate(
                null, null, "default",
                null, null, "kube-system",
                "payments");

        assertTrue(validator.isValid(api, context));
        verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    @DisplayName("namespace experiment fails when type, name, and namespace are all set")
    void namespaceExperimentRejectsWorkloadFields() {
        UpdateResultsAPIObject api = namespaceUpdate(
                null, null, null,
                "deployment", "demo-app", "default",
                "payments");

        assertFalse(validator.isValid(api, context));
        assertViolation(
                String.format(AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_NAMESPACE_SPECIFIC_UPDATE_RESULTS_FIELDS, EXP),
                "kubernetes_objects ");
    }

    @Test
    @DisplayName("namespace experiment appends type and name mismatches when stored values differ")
    void namespaceExperimentAppendsTypeAndNameMismatches() {
        UpdateResultsAPIObject api = namespaceUpdate(
                "deployment", "demo-app", "default",
                "daemonset", "other-app", "kube-system",
                "payments");

        assertFalse(validator.isValid(api, context));
        String expected = String.format(AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_NAMESPACE_SPECIFIC_UPDATE_RESULTS_FIELDS, EXP)
                + String.format(TYPE_MISMATCH, "deployment", "daemonset", EXP)
                + String.format(NAME_MISMATCH, "demo-app", "other-app", EXP);
        assertViolation(expected, "kubernetes_objects ");
    }

    @Test
    @DisplayName("null, empty, or whitespace container names fail")
    void blankContainerNames() {
        for (String containerName : new String[]{null, "", "   "}) {
            clearInvocations(context, violationBuilder, nodeBuilder);
            UpdateResultsAPIObject api = containerUpdate("deployment", "demo-app", "default", containerName);

            assertFalse(validator.isValid(api, context), "container name [" + containerName + "]");
            assertViolation(
                    "[" + AnalyzerErrorConstants.AutotuneObjectErrors.NULL_OR_BLANK_CONTAINER_NAME + "]",
                    "kubernetes_objects ");
        }
    }

    @Test
    @DisplayName("name mismatch and a blank container name are reported together")
    void nameMismatchAndBlankContainer() {
        UpdateResultsAPIObject api = containerUpdate("deployment", "other-app", "default", "  ");

        assertFalse(validator.isValid(api, context));
        String expected = String.format(NAME_MISMATCH, "demo-app", "other-app", EXP)
                + "[" + AnalyzerErrorConstants.AutotuneObjectErrors.NULL_OR_BLANK_CONTAINER_NAME + "]";
        assertViolation(expected, "kubernetes_objects ");
    }

    @Test
    @DisplayName("null, empty, or whitespace namespace names fail when there are no containers")
    void blankNamespaceNames() {
        for (String namespaceName : new String[]{null, "", "   "}) {
            clearInvocations(context, violationBuilder, nodeBuilder);
            UpdateResultsAPIObject api = namespaceUpdate(
                    null, null, null,
                    null, null, null,
                    namespaceName);

            assertFalse(validator.isValid(api, context), "namespace name [" + namespaceName + "]");
            assertViolation(
                    "[" + AnalyzerErrorConstants.AutotuneObjectErrors.NULL_OR_BLANK_NAMESPACE_NAME + "]",
                    "kubernetes_objects ");
        }
    }

    @Test
    @DisplayName("matching namespace experiment passes")
    void matchingNamespaceExperimentPasses() {
        UpdateResultsAPIObject api = namespaceUpdate(
                null, null, "payments",
                null, null, null,
                "payments");

        assertTrue(validator.isValid(api, context));
        verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    @DisplayName("missing containers and namespace fails with the empty-list bounds message")
    void missingContainersAndNamespace() {
        KruizeObject kruizeObject = kruizeObject(AnalyzerConstants.ExperimentType.CONTAINER, "demo-app", "deployment", "default");
        KubernetesAPIObject kube = new KubernetesAPIObject("demo-app", "deployment", "default");
        UpdateResultsAPIObject api = updateResults(kruizeObject, kube);

        assertFalse(validator.isValid(api, context));
        assertViolation("Index 0 out of bounds for length 0", "Kubernetes Elements");
    }

    private void assertViolation(String expectedMessage, String expectedNode) {
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> node = ArgumentCaptor.forClass(String.class);
        verify(context).buildConstraintViolationWithTemplate(message.capture());
        verify(violationBuilder).addPropertyNode(node.capture());
        assertEquals(expectedMessage, message.getValue());
        assertEquals(expectedNode, node.getValue());
    }

    private static UpdateResultsAPIObject containerUpdate(String type, String name, String namespace, String containerName) {
        KruizeObject kruizeObject = kruizeObject(AnalyzerConstants.ExperimentType.CONTAINER, "demo-app", "deployment", "default");
        KubernetesAPIObject kube = new KubernetesAPIObject(name, type, namespace);
        kube.setContainerAPIObjects(List.of(new ContainerAPIObject(containerName, null, null, null)));
        return updateResults(kruizeObject, kube);
    }

    private static UpdateResultsAPIObject namespaceUpdate(String kruizeType,
                                                          String kruizeName,
                                                          String kruizeNamespace,
                                                          String resultType,
                                                          String resultName,
                                                          String resultNamespace,
                                                          String namespaceName) {
        KruizeObject kruizeObject = kruizeObject(
                AnalyzerConstants.ExperimentType.NAMESPACE, kruizeName, kruizeType, kruizeNamespace);
        KubernetesAPIObject kube = new KubernetesAPIObject(resultName, resultType, resultNamespace);
        kube.setNamespaceAPIObject(new NamespaceAPIObject(namespaceName, null, null));
        return updateResults(kruizeObject, kube);
    }

    private static UpdateResultsAPIObject updateResults(KruizeObject kruizeObject, KubernetesAPIObject kube) {
        UpdateResultsAPIObject api = new UpdateResultsAPIObject();
        api.setExperimentName(EXP);
        api.setKruizeObject(kruizeObject);
        api.setKubernetesObjects(List.of(kube));
        return api;
    }

    private static KruizeObject kruizeObject(AnalyzerConstants.ExperimentType experimentType,
                                             String name,
                                             String type,
                                             String namespace) {
        KruizeObject kruizeObject = mock(KruizeObject.class);
        lenient().when(kruizeObject.getExperimentName()).thenReturn(EXP);
        lenient().when(kruizeObject.getExperimentType()).thenReturn(experimentType);
        lenient().when(kruizeObject.getKubernetes_objects()).thenReturn(List.of(new K8sObject(name, type, namespace)));
        return kruizeObject;
    }
}
