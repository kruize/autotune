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
import com.autotune.analyzer.kruizeLayer.layers.ContainerLayer;
import com.autotune.analyzer.kruizeLayer.layers.GenericLayer;
import com.autotune.analyzer.kruizeLayer.layers.HotspotLayer;
import com.autotune.analyzer.kruizeLayer.layers.QuarkusLayer;
import com.autotune.utils.KruizeSupportedTypes;
import com.autotune.utils.KubeEventLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.Hashtable;

import static com.autotune.analyzer.utils.AnalyzerConstants.AutotuneConfigConstants.*;

/**
 * Contains information about the current deployment by parsing the autotune config map
 */
public class KruizeDeploymentInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(KruizeDeploymentInfo.class);
    public static String DATABASE_ADMIN_USERNAME;
    public static String DATABASE_ADMIN_PASSWORD;
    public static String DATABASE_USERNAME;
    public static String DATABASE_PASSWORD;
    public static String DATABASE_HOSTNAME;
    public static String DATABASE_DBNAME;
    public static String DATABASE_PORT;
    public static String DATABASE_SSL_MODE;
    public static String SETTINGS_SAVE_TO_DB;
    public static String SETTINGS_DB_DRIVER;
    public static String SETTINGS_HIBERNATE_DIALECT;
    public static String SETTINGS_HIBERNATE_CONNECTION_DRIVER_CLASS;
    public static String SETTINGS_HIBERNATE_C3P0_MIN_SIZE;
    public static String SETTINGS_HIBERNATE_C3P0_MAX_SIZE;
    public static String SETTINGS_HIBERNATE_C3P0_TIMEOUT;
    public static String SETTINGS_HIBERNATE_C3P0_MAX_STATEMENTS;
    public static String SETTINGS_HIBERNATE_HBM2DDL_AUTO;
    public static String SETTINGS_HIBERNATE_SHOW_SQL;
    public static String SETTINGS_HIBERNATE_TIME_ZONE;
    public static String AUTOTUNE_MODE;
    public static String MONITORING_AGENT;
    public static String MONITORING_SERVICE;
    public static String MONITORING_AGENT_ENDPOINT;
    public static String CLUSTER_TYPE;
    public static String K8S_TYPE;
    public static String AUTH_TYPE;
    public static String AUTH_TOKEN;

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


    public static void setMonitoringAgentEndpoint(String monitoringAgentEndpoint) {
        if (monitoringAgentEndpoint.endsWith("/")) {
            KruizeDeploymentInfo.MONITORING_AGENT_ENDPOINT =
                    monitoringAgentEndpoint.substring(0, monitoringAgentEndpoint.length() - 1);
        } else {
            KruizeDeploymentInfo.MONITORING_AGENT_ENDPOINT = monitoringAgentEndpoint;
        }
    }


    public static void setClusterType(String clusterType) throws ClusterTypeNotSupportedException {
        if (clusterType != null)
            clusterType = clusterType.toLowerCase();

        if (KruizeSupportedTypes.CLUSTER_TYPES_SUPPORTED.contains(clusterType)) {
            KruizeDeploymentInfo.CLUSTER_TYPE = clusterType;
        } else {
            LOGGER.error("Cluster type {} is not supported", clusterType);
            throw new ClusterTypeNotSupportedException();
        }
    }


    public static void setKubernetesType(String kubernetesType) throws K8sTypeNotSupportedException {
        if (kubernetesType != null)
            kubernetesType = kubernetesType.toLowerCase();

        if (KruizeSupportedTypes.K8S_TYPES_SUPPORTED.contains(kubernetesType)) {
            KruizeDeploymentInfo.K8S_TYPE = kubernetesType;
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


    public static void setAuthType(String authType) {
        if (authType != null)
            authType = authType.toLowerCase();

        if (KruizeSupportedTypes.AUTH_TYPES_SUPPORTED.contains(authType)) {
            KruizeDeploymentInfo.AUTH_TYPE = authType;
        }
    }


    public static void setMonitoringAgent(String monitoringAgent) throws MonitoringAgentNotSupportedException {
        if (monitoringAgent != null)
            monitoringAgent = monitoringAgent.toLowerCase();

        if (KruizeSupportedTypes.MONITORING_AGENTS_SUPPORTED.contains(monitoringAgent)) {
            KruizeDeploymentInfo.MONITORING_AGENT = monitoringAgent;
        } else {
            LOGGER.error("Monitoring agent {}  is not supported", monitoringAgent);
            throw new MonitoringAgentNotSupportedException();
        }
    }


    public static void setMonitoringAgentService(String monitoringAgentService) {
        if (monitoringAgentService != null)
            KruizeDeploymentInfo.MONITORING_SERVICE = monitoringAgentService.toLowerCase();
    }

    public static void logDeploymentInfo() {
        LOGGER.info("Cluster Type: {}", KruizeDeploymentInfo.CLUSTER_TYPE);
        LOGGER.info("Kubernetes Type: {}", KruizeDeploymentInfo.K8S_TYPE);
        LOGGER.info("Auth Type: {}", KruizeDeploymentInfo.AUTH_TYPE);
        LOGGER.info("Monitoring Agent: {}", KruizeDeploymentInfo.MONITORING_AGENT);
        LOGGER.info("Monitoring Agent URL: {}", KruizeDeploymentInfo.MONITORING_AGENT_ENDPOINT);
        LOGGER.info("Monitoring agent service: {}\n\n", KruizeDeploymentInfo.MONITORING_SERVICE);
    }


}
