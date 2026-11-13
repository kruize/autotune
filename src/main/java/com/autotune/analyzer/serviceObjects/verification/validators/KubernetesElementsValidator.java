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
import com.autotune.analyzer.serviceObjects.verification.annotators.KubernetesElementsCheck;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class KubernetesElementsValidator implements ConstraintValidator<KubernetesElementsCheck, UpdateResultsAPIObject> {
    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesElementsValidator.class);

    @Override
    public void initialize(KubernetesElementsCheck constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(UpdateResultsAPIObject updateResultsAPIObject, ConstraintValidatorContext context) {
        LOGGER.debug("KubernetesElementsValidator expName - {} - {} - {}", updateResultsAPIObject.getExperimentName(), updateResultsAPIObject.getStartTimestamp(), updateResultsAPIObject.getEndTimestamp());
        boolean success = false;
        try {
            KruizeObject kruizeObject = updateResultsAPIObject.getKruizeObject();
            // Assuming that there is only one Kubernetes object
            KubernetesAPIObject resultKubeObj = updateResultsAPIObject.getKubernetesObjects().get(0);
            List<ContainerAPIObject> containerAPIObjects = resultKubeObj.getContainerAPIObjects();
            NamespaceAPIObject namespaceAPIObject = resultKubeObj.getNamespaceAPIObject();
            // The domain convert omitted this object when it had neither containers nor a namespace,
            // and the following get(0) failed before any field comparison.
            if ((containerAPIObjects == null || containerAPIObjects.isEmpty()) && namespaceAPIObject == null) {
                throw new IndexOutOfBoundsException("Index 0 out of bounds for length 0");
            }

            String expName = kruizeObject.getExperimentName();
            String errorMsg = "";
            boolean kubeObjsMisMatch = false;

            // Check if Kubernetes Object Namespaces is matched
            if (kruizeObject.getExperimentType().equals(AnalyzerConstants.ExperimentType.NAMESPACE)) {
                // Validate the absence of type, name and namespace objects
                String kubeObjTypeInResultData = resultKubeObj.getType();
                String kubeObjNameInResultsData = resultKubeObj.getName();
                String kubeObjNameSpaceInResultsData = resultKubeObj.getNamespace();

                if (kubeObjTypeInResultData != null && kubeObjNameInResultsData != null && kubeObjNameSpaceInResultsData != null) {
                    kubeObjsMisMatch = true;
                    errorMsg = errorMsg.concat(
                            String.format(
                                    AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_NAMESPACE_SPECIFIC_UPDATE_RESULTS_FIELDS,
                                    expName
                            ));
                }
            }

            // Check if Kubernetes Object Type is matched
            String kubeObjTypeInKruizeObject = kruizeObject.getKubernetes_objects().get(0).getType();
            String kubeObjTypeInResultData = resultKubeObj.getType();

            if (kubeObjTypeInKruizeObject != null && !kubeObjTypeInKruizeObject.equals(kubeObjTypeInResultData)) {
                kubeObjsMisMatch = true;
                errorMsg = errorMsg.concat(
                        String.format(
                                "Kubernetes Object Types MisMatched. Expected Type: %s, Found: %s in Results for experiment: %s \n",
                                kubeObjTypeInKruizeObject,
                                kubeObjTypeInResultData,
                                expName
                        ));
            }

            // Check if Kubernetes Object Name is matched
            String kubeObjNameInKruizeObject = kruizeObject.getKubernetes_objects().get(0).getName();
            String kubeObjNameInResultsData = resultKubeObj.getName();

            if (kubeObjNameInKruizeObject != null && !kubeObjNameInKruizeObject.equals(kubeObjNameInResultsData)) {
                kubeObjsMisMatch = true;
                errorMsg = errorMsg.concat(
                        String.format(
                                "Kubernetes Object Names MisMatched. Expected Name: %s, Found: %s in Results for experiment: %s \n",
                                kubeObjNameInKruizeObject,
                                kubeObjNameInResultsData,
                                expName
                        ));
            }


            // Check if Kubernetes Object NameSpace is matched
            String kubeObjNameSpaceInKruizeObject = kruizeObject.getKubernetes_objects().get(0).getNamespace();
            String kubeObjNameSpaceInResultsData = resultKubeObj.getNamespace();

            if (kruizeObject.getExperimentType().equals(AnalyzerConstants.ExperimentType.CONTAINER)) {
                if (kubeObjNameSpaceInKruizeObject != null && !kubeObjNameSpaceInKruizeObject.equals(kubeObjNameSpaceInResultsData)) {
                    kubeObjsMisMatch = true;
                    errorMsg = errorMsg.concat(
                            String.format(
                                    "Kubernetes Object Namespaces MisMatched. Expected Namespace: %s, Found: %s in Results for experiment: %s \n",
                                    kubeObjNameSpaceInKruizeObject,
                                    kubeObjNameSpaceInResultsData,
                                    expName
                            ));
                }
            }
            // Validate blank or null container names
            // Assuming that there is only one Kubernetes object
            List<String> validationErrors = new ArrayList<>();
            if (containerAPIObjects != null && !containerAPIObjects.isEmpty()) {
                for (ContainerAPIObject containerAPIObject : containerAPIObjects) {
                    validationErrors.addAll(validateContainerName(containerAPIObject.getContainer_name()));
                }
            }

            if (!validationErrors.isEmpty()) {
                kubeObjsMisMatch = true;
                errorMsg = errorMsg.concat(validationErrors.toString());
            }

            // Validate for blank or null Namespace Name
            // Assuming that there is only one Kubernetes object
            List<String> validationErrorsNamespace = new ArrayList<>();
            if ((containerAPIObjects == null || containerAPIObjects.isEmpty()) && namespaceAPIObject != null) {
                validationErrorsNamespace.addAll(validateNamespaceName(namespaceAPIObject.getNamespace()));
            }

            if (!validationErrorsNamespace.isEmpty()) {
                kubeObjsMisMatch = true;
                errorMsg = errorMsg.concat(validationErrorsNamespace.toString());
            }


            if (kubeObjsMisMatch) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(errorMsg)
                        .addPropertyNode("kubernetes_objects ")
                        .addConstraintViolation();
            } else {
                success = true;
            }

        } catch (Exception e) {
            LOGGER.error(e.toString());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String stackTrace = sw.toString();
            LOGGER.debug(stackTrace);
            if (null != e.getMessage()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(e.getMessage())
                        .addPropertyNode("Kubernetes Elements")
                        .addConstraintViolation();
            } else {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Null value found")
                        .addPropertyNode("Kubernetes Elements")
                        .addConstraintViolation();
            }
        }
        LOGGER.debug("KubernetesElementsValidator success : {}", success);
        return success;
    }

    /**
     * Validation method for a single container name from the updateResults API object
     * @param containerName container name to validate
     * @return list of errors if any, while validating the container name
     */
    private static List<String> validateContainerName(String containerName) {
        List<String> errors = new ArrayList<>();
        if (containerName == null || containerName.trim().isEmpty()) {
            errors.add(AnalyzerErrorConstants.AutotuneObjectErrors.NULL_OR_BLANK_CONTAINER_NAME);
        }
        return errors;
    }

    /**
     * Validation method for a single namespace name from the updateResults API object
     * @param namespaceName namespace name to validate
     * @return list of errors if any, while validating the namespace name
     */
    private static List<String> validateNamespaceName(String namespaceName) {
        List<String> errors = new ArrayList<>();
        if (namespaceName == null || namespaceName.trim().isEmpty()) {
            errors.add(AnalyzerErrorConstants.AutotuneObjectErrors.NULL_OR_BLANK_NAMESPACE_NAME);
        }
        return errors;
    }
}
