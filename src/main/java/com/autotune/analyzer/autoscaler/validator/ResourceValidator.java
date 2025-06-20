/*******************************************************************************
 * Copyright (c) 2025 Red Hat, IBM Corporation and others.
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

package com.autotune.analyzer.autoscaler.validator;


import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.utils.KruizeConstants;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



/* The ResourceValidator class is responsible for determining if the recommended resources are valid or not
 ** It verifies limit ranges, namespace quotas etc.
 */

public class ResourceValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceValidator.class);

    /**
     * Verifies the specified resource requests for a pod and perform dry run.
     * @param containerName String the name of the container for which resource verification is performed
     * @param imageName String the name of the image used for the container
     * @param namespace String the namespace where workload is deployed
     * @param resourceMap map containing resource names as keys and their recommended values.
     * @return ValidationOutputData object containing the result of the verification,
     */
    public static ValidationOutputData verifyResources(String containerName, String imageName,
                                                       String namespace, Map<String, Quantity> resourceMap) {

        ValidationOutputData validationOutputData = new ValidationOutputData(true, null, null);
        // perform dry run
        dryRunPod(containerName, imageName, namespace, resourceMap, validationOutputData);
        return validationOutputData;
    }

    /**
     * This method performs a dry run of a pod with the given resource values to determine if they are available.
     * @param containerName String the name of the container for which resource verification is performed
     * @param image String the name of the image used for the container
     * @param namespace String the namespace where workload is deployed
     * @param resourceMap map containing resource names as keys and their recommended values.
     */

    public static void dryRunPod(String containerName, String image, String namespace,
                                 Map<String, Quantity> resourceMap, ValidationOutputData validationOutputData) {
        try (KubernetesClient client = new DefaultKubernetesClient()) {
            Quantity cpuValue = null;
            Quantity memoryValue = null;

            if (null != resourceMap) {
                cpuValue = resourceMap.get(KruizeConstants.JSONKeys.CPU);
                memoryValue = resourceMap.get(KruizeConstants.JSONKeys.MEMORY);
            }

            if (cpuValue == null || memoryValue == null) {
                validationOutputData.setSuccess(false);
                validationOutputData.setMessage(AnalyzerErrorConstants.AutoscalerErrors.MISSING_REQUIRED_VALUES);
            } else {
                // Create a pod definition with recommended resoruces
                Pod pod = new PodBuilder()
                        .withNewMetadata()
                        .withName(containerName + "-dryrun")
                        .withNamespace(namespace)
                        .endMetadata()
                        .withNewSpec()
                        .addNewContainer()
                        .withName(containerName)
                        .withImage(image)
                        .withNewResources()
                        .addToRequests(AnalyzerConstants.RecommendationItem.CPU.toString(), cpuValue)
                        .addToRequests(AnalyzerConstants.RecommendationItem.MEMORY.toString(), memoryValue)
                        .addToLimits(AnalyzerConstants.RecommendationItem.CPU.toString(), cpuValue)
                        .addToLimits(AnalyzerConstants.RecommendationItem.MEMORY.toString(), memoryValue)
                        .endResources()
                        .endContainer()
                        .endSpec()
                        .build();

                // Perform a dry-run to check scheduling
                LOGGER.debug("Performing dry run on pod {}", pod.getMetadata().getName());
                Pod dryRunPod = client.pods().inNamespace(namespace).resource(pod).dryRun(true).create();
                // no error occurs during the dry run, set validation result is set to true
                validationOutputData.setSuccess(true);
                validationOutputData.setMessage(AnalyzerConstants.AutoscalerConstants.InfoMsgs.POD_READY);
                LOGGER.debug(AnalyzerConstants.AutoscalerConstants.InfoMsgs.POD_READY);
            }
        } catch (Exception e) {
            String logMsg = e.getMessage();
            // fetching the error message
            Pattern pattern = Pattern.compile(AnalyzerConstants.AutoscalerConstants.REGEX_FOR_DRY_RUN_ERROR);
            Matcher matcher = pattern.matcher(logMsg);
            if (matcher.find()) {
                validationOutputData.setMessage(matcher.group(1));
            } else {
                validationOutputData.setMessage(e.getMessage());
            }
            validationOutputData.setSuccess(false);
        }
    }
}
