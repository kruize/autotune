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
import com.autotune.analyzer.utils.AnalyzerConstants.LayerConstants;
import com.autotune.analyzer.utils.AnalyzerConstants.LayerConstants.LogMessages;
import com.autotune.analyzer.utils.AnalyzerConstants.LayerConstants.PresenceType;
import com.autotune.common.datasource.DataSourceCollection;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.common.datasource.DataSourceOperatorImpl;
import com.google.gson.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation for label-based layer presence detection via Prometheus/kube-state-metrics
 */
public class LabelBasedPresence implements LayerPresenceDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(LabelBasedPresence.class);

    private List<LayerPresenceLabel> label;

    public LabelBasedPresence(List<LayerPresenceLabel> label) {
        this.label = label != null ? label : new ArrayList<>();
    }

    @Override
    public PresenceType getType() {
        return PresenceType.LABEL;
    }

    /**
     * Detect layer presence using Prometheus query on kube_pod_labels metric
     * @param namespace The Kubernetes namespace
     * @param containerName The container name (optional, added as filter)
     * @param datasourceName The datasource name to use for querying
     * @return true if layer is detected
     * @throws Exception if detection fails
     */
    @Override
    public boolean detectPresence(String namespace, String containerName, String datasourceName) throws Exception {
        if (label == null || label.isEmpty()) {
            LOGGER.warn(LogMessages.NO_LABELS_DEFINED);
            return false;
        }

        // Iterate through all configured labels
        for (LayerPresenceLabel layerLabel : label) {
            // Skip null label objects
            if (layerLabel == null) {
                LOGGER.warn(LogMessages.NULL_LABEL_ENCOUNTERED);
                continue;
            }

            String labelName = layerLabel.getName();
            String labelValue = layerLabel.getValue();

            // Validate label name and value
            if (labelName == null || labelName.isBlank()) {
                LOGGER.warn(LogMessages.INVALID_LABEL_NAME);
                continue;
            }
            if (labelValue == null || labelValue.isBlank()) {
                LOGGER.warn(LogMessages.INVALID_LABEL_VALUE);
                continue;
            }

            try {
                // Get the specific datasource by name from the experiment
                DataSourceInfo dataSourceInfo = DataSourceCollection.getInstance()
                        .getDataSourcesCollection()
                        .get(datasourceName);

                if (dataSourceInfo == null) {
                    LOGGER.warn(LogMessages.DATASOURCE_NOT_FOUND, datasourceName);
                    continue;
                }

                // Get the appropriate operator for the datasource provider
                DataSourceOperatorImpl operator = DataSourceOperatorImpl.getInstance()
                        .getOperator(dataSourceInfo.getProvider());

                if (operator == null) {
                    LOGGER.warn(LogMessages.NO_OPERATOR_AVAILABLE, dataSourceInfo.getProvider());
                    continue;
                }

                // Build Prometheus query using kube_pod_labels metric
                // Transform label name: com.redhat.component-name → label_com_redhat_component_name
                String promLabelName = transformLabelName(labelName);
                String query = buildKubePodLabelsQuery(promLabelName, labelValue, namespace, containerName);

                LOGGER.debug(LogMessages.EXECUTING_LABEL_QUERY, query);

                // Execute the query and get results
                JsonArray resultArray = operator.getResultArrayForQuery(
                        dataSourceInfo,
                        query
                );

                // Check if we got any results - if yes, layer is present
                if (resultArray != null && !resultArray.isEmpty()) {
                    LOGGER.debug(LogMessages.LAYER_DETECTED_VIA_LABEL, namespace, labelName, labelValue);
                    return true;
                }
            } catch (Exception e) {
                LOGGER.error(LogMessages.ERROR_CHECKING_LABEL, labelName, labelValue, e);
                // Continue to next label instead of failing completely
            }
        }

        // No labels returned positive results
        return false;
    }

    /**
     * Transform Kubernetes label name to Prometheus label format
     * kube-state-metrics converts K8s labels by:
     * 1. Adding "label_" prefix
     * 2. Replacing dots (.) and hyphens (-) with underscores (_)
     *
     * Example: com.redhat.component-name → label_com_redhat_component_name
     *
     * @param k8sLabelName Original Kubernetes label name
     * @return Prometheus-compatible label name
     */
    private String transformLabelName(String k8sLabelName) {
        if (k8sLabelName == null || k8sLabelName.isEmpty()) {
            return k8sLabelName;
        }

        // Replace dots and hyphens with underscores
        String normalized = k8sLabelName.replace('.', '_').replace('-', '_');

        // Add label_ prefix
        return "label_" + normalized;
    }

    /**
     * Build a PromQL query to check for pod labels via kube_pod_labels metric
     *
     * @param promLabelName Prometheus label name (already transformed)
     * @param labelValue Label value to match
     * @param namespace Kubernetes namespace
     * @param containerName Container name (optional)
     * @return PromQL query string
     */
    private String buildKubePodLabelsQuery(String promLabelName, String labelValue,
                                           String namespace, String containerName) {
        StringBuilder query = new StringBuilder("kube_pod_labels{");

        // Add namespace filter
        if (namespace != null && !namespace.isBlank()) {
            query.append("namespace=\"").append(escapePromQLValue(namespace)).append("\"");
        }

        // Add label filter
        if (promLabelName != null && !promLabelName.isBlank()) {
            if (query.charAt(query.length() - 1) != '{') {
                query.append(",");
            }
            query.append(promLabelName).append("=\"").append(escapePromQLValue(labelValue)).append("\"");
        }

        // Add container filter (optional)
        if (containerName != null && !containerName.isBlank()) {
            if (query.charAt(query.length() - 1) != '{') {
                query.append(",");
            }
            query.append(LayerConstants.LABEL_CONTAINER).append("=\"")
                 .append(escapePromQLValue(containerName)).append("\"");
        }

        query.append("}");
        return query.toString();
    }

    /**
     * Escape special characters in PromQL label values to prevent syntax errors and injection attacks
     *
     * @param value The value to escape
     * @return Escaped value safe for use in PromQL query
     */
    private String escapePromQLValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\t", "\\t")
                   .replace("\r", "\\r");
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
