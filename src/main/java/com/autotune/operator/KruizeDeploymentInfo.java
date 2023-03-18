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
package com.autotune.operator;

import com.autotune.analyzer.exceptions.ClusterTypeNotSupportedException;
import com.autotune.analyzer.exceptions.K8sTypeNotSupportedException;
import com.autotune.analyzer.exceptions.MonitoringAgentNotSupportedException;
import com.autotune.analyzer.layer.ContainerLayer;
import com.autotune.analyzer.layer.GenericLayer;
import com.autotune.analyzer.layer.HotspotLayer;
import com.autotune.analyzer.layer.QuarkusLayer;
import com.autotune.utils.KruizeSupportedTypes;
import com.autotune.utils.KubeEventLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.Hashtable;

import static com.autotune.utils.AnalyzerConstants.AutotuneConfigConstants.*;

/**
 * Contains information about the current deployment by parsing the autotune config map
 */
public class KruizeDeploymentInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(KruizeDeploymentInfo.class);
    private static String clusterType;
    private static String kubernetesType;
    private static String authType;
    private static String authToken;
    private static String monitoringAgent;
    private static String monitoringAgentService;
    private static String monitoringAgentEndpoint;
    private static String loggingLevel;
    private static String rootLoggingLevel;
    private static Hashtable<String, Class> tunableLayerPair;
    //private static KubernetesClient kubernetesClient;
    private static KubeEventLogger kubeEventLogger;
    private KruizeDeploymentInfo() {
    }

    public static void setLayerTable() {
        tunableLayerPair = new Hashtable<String, Class>();
        tunableLayerPair.put(LAYER_GENERIC, GenericLayer.class);
        tunableLayerPair.put(LAYER_CONTAINER, ContainerLayer.class);
        tunableLayerPair.put(LAYER_HOTSPOT, HotspotLayer.class);
        tunableLayerPair.put(LAYER_QUARKUS, QuarkusLayer.class);
    }

    // TODO: Need a better way to get the layer class
    public static Class getLayer(String layerName) {
        return tunableLayerPair.get(layerName);
    }

    public static String getMonitoringAgentEndpoint() {
        return monitoringAgentEndpoint;
    }

    public static void setMonitoringAgentEndpoint(String monitoringAgentEndpoint) {
        if (monitoringAgentEndpoint.endsWith("/")) {
            KruizeDeploymentInfo.monitoringAgentEndpoint =
                    monitoringAgentEndpoint.substring(0, monitoringAgentEndpoint.length() - 1);
        } else {
            KruizeDeploymentInfo.monitoringAgentEndpoint = monitoringAgentEndpoint;
        }
    }

    public static String getClusterType() {
        return clusterType;
    }

    public static void setClusterType(String clusterType) throws ClusterTypeNotSupportedException {
        if (clusterType != null)
            clusterType = clusterType.toLowerCase();

        if (KruizeSupportedTypes.CLUSTER_TYPES_SUPPORTED.contains(clusterType)) {
            KruizeDeploymentInfo.clusterType = clusterType;
        } else {
            LOGGER.error("Cluster type {} is not supported", clusterType);
            throw new ClusterTypeNotSupportedException();
        }
    }

    public static String getKubernetesType() {
        return kubernetesType;
    }

    public static void setKubernetesType(String kubernetesType) throws K8sTypeNotSupportedException {
        if (kubernetesType != null)
            kubernetesType = kubernetesType.toLowerCase();

        if (KruizeSupportedTypes.K8S_TYPES_SUPPORTED.contains(kubernetesType)) {
            KruizeDeploymentInfo.kubernetesType = kubernetesType;
        } else {
            LOGGER.error("k8s type {} is not suppported", kubernetesType);
            throw new K8sTypeNotSupportedException();
        }
    }

    public static void initiateEventLogging() {
        kubeEventLogger = new KubeEventLogger(Clock.systemUTC());
    }

    public static KubeEventLogger getKubeEventLogger() {
        return kubeEventLogger;
    }

    public static String getAuthType() {
        return authType;
    }

    public static void setAuthType(String authType) {
        if (authType != null)
            authType = authType.toLowerCase();

        if (KruizeSupportedTypes.AUTH_TYPES_SUPPORTED.contains(authType)) {
            KruizeDeploymentInfo.authType = authType;
        }
    }

    public static String getAuthToken() {
        return authToken;
    }

    public static void setAuthToken(String authToken) {
        KruizeDeploymentInfo.authToken = (authToken == null) ? "" : authToken;
    }

    public static String getMonitoringAgent() {
        return monitoringAgent;
    }

    public static void setMonitoringAgent(String monitoringAgent) throws MonitoringAgentNotSupportedException {
        if (monitoringAgent != null)
            monitoringAgent = monitoringAgent.toLowerCase();

        if (KruizeSupportedTypes.MONITORING_AGENTS_SUPPORTED.contains(monitoringAgent)) {
            KruizeDeploymentInfo.monitoringAgent = monitoringAgent;
        } else {
            LOGGER.error("Monitoring agent {}  is not supported", monitoringAgent);
            throw new MonitoringAgentNotSupportedException();
        }
    }

    public static String getMonitoringAgentService() {
        return monitoringAgentService;
    }

    public static void setMonitoringAgentService(String monitoringAgentService) {
        if (monitoringAgentService != null)
            KruizeDeploymentInfo.monitoringAgentService = monitoringAgentService.toLowerCase();
    }

    public static String getLoggingLevel() {
        return loggingLevel;
    }

    public static void setLoggingLevel(String loggingLevel) {
        if (loggingLevel != null)
            loggingLevel = loggingLevel.toLowerCase();

        if (KruizeSupportedTypes.LOGGING_TYPES_SUPPORTED.contains(loggingLevel))
            KruizeDeploymentInfo.loggingLevel = loggingLevel;
        else
            KruizeDeploymentInfo.loggingLevel = "info";
    }

    public static String getRootLoggingLevel() {
        return rootLoggingLevel;
    }

    public static void setRootLoggingLevel(String rootLoggingLevel) {
        if (rootLoggingLevel != null)
            rootLoggingLevel = rootLoggingLevel.toLowerCase();

        if (KruizeSupportedTypes.LOGGING_TYPES_SUPPORTED.contains(rootLoggingLevel))
            KruizeDeploymentInfo.rootLoggingLevel = rootLoggingLevel;
        else
            KruizeDeploymentInfo.rootLoggingLevel = "error";
    }

    public static void logDeploymentInfo() {
        LOGGER.info("Cluster Type: {}", getClusterType());
        LOGGER.info("Kubernetes Type: {}", getKubernetesType());
        LOGGER.info("Auth Type: {}", getAuthType());
        LOGGER.info("Monitoring Agent: {}", getMonitoringAgent());
        LOGGER.info("Monitoring Agent URL: {}", getMonitoringAgentEndpoint());
        LOGGER.info("Monitoring agent service: {}\n\n", getMonitoringAgentService());
    }
}
