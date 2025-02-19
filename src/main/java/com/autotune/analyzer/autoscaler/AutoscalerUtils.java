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

package com.autotune.analyzer.autoscaler;

import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.ValidationOutputData;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class AutoscalerUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoscalerUtils.class);

    public static boolean checkPodScheduling(String containerName, String image, String namespace, String result,
                                             String cpuValue, String memoryValue) {
        try {
            KubernetesClient client = new DefaultKubernetesClient();
            // Create a pod definition
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
                    .addToRequests(AnalyzerConstants.RecommendationItem.CPU.toString(), new Quantity(cpuValue))
                    .addToRequests(AnalyzerConstants.RecommendationItem.MEMORY.toString(), new Quantity(memoryValue))
                    .addToLimits(AnalyzerConstants.RecommendationItem.CPU.toString(), new Quantity(cpuValue))
                    .addToLimits(AnalyzerConstants.RecommendationItem.MEMORY.toString(), new Quantity(memoryValue))
                    .endResources()
                    .endContainer()
                    .endSpec()
                    .build();

            // Perform a dry-run to check scheduling
            LOGGER.info("CHECKING DRY RUN");
            Pod pod1 = client.pods().inNamespace(namespace).resource(pod).dryRun(true).create();
            result = AnalyzerConstants.AutoscalerConstants.InfoMsgs.POD_READY;
            LOGGER.info("CHECKING DRY RUN PASSSED");
            return true;
        } catch (Exception e) {
            LOGGER.info("CHECKING DRY RUN FAILLLLED");
            result = e.getMessage();
            LOGGER.info(e.getMessage());
            return false;
        }
    }


    public static ValidationOutputData verifyResourceAvailability() {
        ValidationOutputData validationOutputData = null;
        KubernetesClient client = new DefaultKubernetesClient();

        // getting list of nodes
        List<Node> nodes = client.nodes().list().getItems();
        if (nodes == null || nodes.isEmpty()) {
            validationOutputData = new ValidationOutputData(false, "Unable to verify Nodes", null);
            return validationOutputData;
        }

        Quantity totalAvailableMemory = new Quantity("0");
        Quantity totalAvailableCPU = new Quantity("0");

        Quantity totalUsedCpu = new Quantity("0");
        Quantity totalUsedMemory = new Quantity("0");


        LOGGER.info("===== HELLOOO VERIFYING RESOURCE AVAILABILITY ====== ");

        for (Node node : nodes) {
            Map<String, String> nodeLabels = node.getMetadata().getLabels();

            // verifying worker node
//            if (!(nodeLabels != null && nodeLabels.containsKey("node-role.kubernetes.io/worker"))) {
//                continue;
//            }
            ResourceQuotaList quotaList = client.resourceQuotas().inNamespace("namespace").list();
            for (ResourceQuota quota : quotaList.getItems()) {
                quota.getStatus().getUsed();
            }
            Map<String, Quantity> allocatable = node.getStatus().getAllocatable();

            for (Map.Entry<String, Quantity> entry : allocatable.entrySet()) {
                LOGGER.info("Checking allocateble for node: " + entry.getKey());
                if (entry.getKey().equalsIgnoreCase("cpu")) {
                    LOGGER.info("Checking allocateble for node CPU: " + entry.getValue().getAmount());
                    totalAvailableCPU = totalAvailableCPU.add(entry.getValue());
                }
                if (entry.getKey().equalsIgnoreCase("memory")) {
                    LOGGER.info("Checking allocateble memory for node: " + entry.getValue().getAmount());
                    totalAvailableMemory = totalAvailableMemory.add(entry.getValue());
                }
            }

            List<Pod> pods = client.pods().list().getItems();
            String nodeName = node.getMetadata().getName();

            for(Pod pod : pods) {
                if (pod.getSpec() != null && nodeName.equalsIgnoreCase(pod.getSpec().getNodeName())) {
                    if (pod.getSpec().getContainers() != null) {
                        for (Container container : pod.getSpec().getContainers()) {
                            ResourceRequirements resources = container.getResources();
                            if (resources != null && resources.getRequests() != null) {
                                Map<String, Quantity> requests = resources.getRequests();
                                for (Map.Entry<String, Quantity> entry : requests.entrySet()) {
                                    if (entry.getKey().equalsIgnoreCase("cpu")) {
                                        totalUsedCpu = totalUsedCpu.add(entry.getValue());
                                    }
                                    if (entry.getKey().equalsIgnoreCase("memory")) {
                                        totalUsedMemory = totalUsedMemory.add(entry.getValue());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Quantity totalUnallocatedMemory = new Quantity("0");
        Quantity totalUnallocatedCPU = new Quantity("0");

        totalUnallocatedCPU = totalAvailableCPU.subtract(totalUsedCpu);
        totalUnallocatedMemory = totalAvailableMemory.subtract(totalUsedMemory);

        LOGGER.info("Total Unallocated Memory: " + totalUnallocatedMemory);
        LOGGER.info("Total Unallocated CPU: " + totalUnallocatedCPU);
        LOGGER.info("Total totalAvailableCPU Memory: " + totalAvailableCPU);
        LOGGER.info("Total totalAvailableCPU Memory: " + totalAvailableMemory);
        LOGGER.info("Total totalUsedCpu Memory: " + totalUsedCpu);
        LOGGER.info("Total totalUsedMemory Memory: " + totalUsedMemory);


        return validationOutputData;
    }
}
