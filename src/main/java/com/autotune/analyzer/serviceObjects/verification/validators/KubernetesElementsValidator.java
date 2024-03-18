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
import com.autotune.analyzer.serviceObjects.Converters;
import com.autotune.analyzer.serviceObjects.UpdateResultsAPIObject;
import com.autotune.analyzer.serviceObjects.verification.annotators.KubernetesElementsCheck;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.ExperimentResultData;
import jakarta.validation.*;
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
        String errorMessage = "";
        try {
            KruizeObject kruizeObject = updateResultsAPIObject.getKruizeObject();
            ExperimentResultData resultData = Converters.KruizeObjectConverters.convertUpdateResultsAPIObjToExperimentResultData(updateResultsAPIObject);
            String expName = kruizeObject.getExperimentName();
            String errorMsg = "";
            boolean kubeObjsMisMatch = false;

            // Check if Kubernetes Object Type is matched
            String kubeObjTypeInKruizeObject = kruizeObject.getKubernetes_objects().get(0).getType();
            String kubeObjTypeInResultData = resultData.getKubernetes_objects().get(0).getType();

            if (!kubeObjTypeInKruizeObject.equals(kubeObjTypeInResultData)) {
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
            String kubeObjNameInResultsData = resultData.getKubernetes_objects().get(0).getName();

            if (!kubeObjNameInKruizeObject.equals(kubeObjNameInResultsData)) {
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
            String kubeObjNameSpaceInResultsData = resultData.getKubernetes_objects().get(0).getNamespace();

            if (!kubeObjNameSpaceInKruizeObject.equals(kubeObjNameSpaceInResultsData)) {
                kubeObjsMisMatch = true;
                errorMsg = errorMsg.concat(
                        String.format(
                                "Kubernetes Object Namespaces MisMatched. Expected Namespace: %s, Found: %s in Results for experiment: %s \n",
                                kubeObjNameSpaceInKruizeObject,
                                kubeObjNameSpaceInResultsData,
                                expName
                        ));
            }

            // Validate blank or null container names and image names
            List<ContainerData> resultContainers = resultData.getKubernetes_objects().get(0).getContainerDataMap().values().stream().toList();
            List<String> validationErrors = resultContainers.stream()
                    .flatMap(containerData -> validateContainerData(containerData).stream())
                    .toList();

            if (!validationErrors.isEmpty()) {
                kubeObjsMisMatch = true;
                errorMsg = errorMsg.concat(validationErrors.toString());
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
     * Validation method for a single ContainerData object
     * @param containerData contains the container data in which the container name is present which needs to be validated
     * @return list of errors if any, while validating the containerData Object
     */
    private static List<String> validateContainerData(ContainerData containerData) {
        List<String> errors = new ArrayList<>();
        if (containerData.getContainer_name() == null || containerData.getContainer_name().trim().isEmpty()) {
            errors.add(AnalyzerErrorConstants.AutotuneObjectErrors.NULL_OR_BLANK_CONTAINER_NAME);
        }
        return errors;
    }
}
