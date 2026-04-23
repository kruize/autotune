/*******************************************************************************
 * Copyright (c) 2026 Red Hat, IBM Corporation and others.
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

package com.autotune.analyzer.kruizeLayer.presence;

import com.autotune.analyzer.kruizeLayer.LayerPresenceLabel;
import com.autotune.analyzer.utils.AnalyzerConstants.LayerConstants.LogMessages;
import com.autotune.analyzer.utils.AnalyzerConstants.LayerConstants.PresenceType;
import com.autotune.common.target.kubernetes.service.KubernetesServices;
import com.autotune.common.target.kubernetes.service.impl.KubernetesServicesImpl;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation for label-based layer presence detection via Fabric8 Kubernetes client
 */
public class LabelBasedPresence implements LayerPresenceDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(LabelBasedPresence.class);

    private List<LayerPresenceLabel> label;
    private KubernetesServices kubernetesServices;

    public LabelBasedPresence(List<LayerPresenceLabel> label) {
        this.label = label != null ? label : new ArrayList<>();
        this.kubernetesServices = new KubernetesServicesImpl();
    }


    @Override
    public PresenceType getType() {
        return PresenceType.LABEL;
    }

    /**
     * Detect layer presence by querying Kubernetes API for pods with specific labels
     * @param namespace The Kubernetes namespace
     * @param containerName The container name (optional, filters to pods containing this container)
     * @param datasourceName Unused for label-based detection (kept for interface compatibility)
     * @return true if layer is detected
     * @throws Exception if detection fails
     */
    @Override
    public boolean detectPresence(String namespace, String containerName, String datasourceName) throws Exception {
        // datasourceName is intentionally ignored - we query Kubernetes API directly

        if (label == null || label.isEmpty()) {
            LOGGER.warn(LogMessages.NO_LABELS_DEFINED);
            return false;
        }

        // check each label
        for (LayerPresenceLabel layerLabel : label) {
            // Skip null label objects
            if (layerLabel == null || layerLabel.getName() == null || layerLabel.getValue() == null) {
                continue;
            }

            try {
                // 1. Query kubernetes for pods with label
                List<Pod> matchingPods = kubernetesServices.getPodsBy(namespace, layerLabel.getName(), layerLabel.getValue());

                // 2. Filter for container name
                if (containerName != null && !containerName.isBlank()) {
                    matchingPods = filterPodsWithContainer(matchingPods, containerName);
                }

                // 3. If found pods, layer is present
                if (matchingPods != null && !matchingPods.isEmpty()) {
                    LOGGER.debug(LogMessages.LAYER_DETECTED_VIA_LABEL, namespace, layerLabel.getName(), layerLabel.getValue());
                    return true;
                }
            } catch (Exception e) {
                LOGGER.error(LogMessages.ERROR_CHECKING_LABEL, layerLabel.getName(), layerLabel.getValue() , e);
                // Continue to next label instead of failing completely
            }
        }

        // Checked all labels, none matched
        return false;
    }

    // helper function to filter by container
    private List<Pod> filterPodsWithContainer(List<Pod> pods, String containerName) {
        if (pods == null || pods.isEmpty()) {
            return pods;
        }
        return pods.stream().filter(pod -> hasContainer(pod, containerName)).collect(Collectors.toList());
    }

    private boolean hasContainer(Pod pod, String containerName) {
        PodSpec spec = pod.getSpec();
        if (spec == null || spec.getContainers() == null) {
            return false;
        }
        return spec.getContainers().stream().anyMatch(container -> containerName.equals(container.getName()));
    }

    public List<LayerPresenceLabel> getLabel() {
        return label;
    }

    public void setLabel(List<LayerPresenceLabel> label) {
        this.label = label != null ? label : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "LabelBasedPresence{" +
                "label=" + label +
                '}';
    }
}
