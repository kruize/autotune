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
import static com.autotune.utils.KruizeConstants.KRUIZE_CONFIG_DEFAULT_VALUE.DELETE_PARTITION_THRESHOLD_IN_DAYS;

/**
 * Contains information about the current deployment by parsing the autotune config map
 */
public class KruizeDeploymentInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(KruizeDeploymentInfo.class);
    public static String database_username;
    public static String database_password;
    public static String database_hostname;
    public static String database_dbname;
    public static String database_port;
    public static String settings_db_driver;
    public static String settings_hibernate_dialect;
    public static String settings_hibernate_connection_driver_class;
    public static String settings_hibernate_c3p0_min_size;
    public static String settings_hibernate_c3p0_max_size;
    public static String settings_hibernate_c3p0_timeout;
    public static String settings_hibernate_c3p0_max_statements;
    public static String settings_hibernate_hbm2ddl_auto;
    public static String settings_hibernate_show_sql;
    public static String settings_hibernate_time_zone;
    public static String autotune_mode;
    public static String monitoring_agent;
    public static String monitoring_service;
    public static String monitoring_agent_endpoint;
    public static String cluster_type;
    public static String k8s_type;       // ABC
    public static String auth_type;
    public static Boolean plots = false;
    public static String auth_token;
    public static String database_admin_username;
    public static String database_admin_password;
    public static String database_ssl_mode;

    public static String cloudwatch_logs_access_key_id;
    public static String cloudwatch_logs_secret_access_key;
    public static String cloudwatch_logs_log_group;
    public static String cloudwatch_logs_region;
    public static String cloudwatch_logs_log_level;
    public static String cloudwatch_logs_log_stream;

    public static Boolean settings_save_to_db;
    public static String em_only_mode;
    public static Integer bulk_update_results_limit = 100;
    public static Boolean local = false;
    public static Boolean log_http_req_resp = false;

    public static int generate_recommendations_date_range_limit_in_days = 15;
    public static Integer delete_partition_threshold_in_days = DELETE_PARTITION_THRESHOLD_IN_DAYS;
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


    public static void setMonitoring_agent_endpoint(String monitoring_agent_endpoint) {
        if (monitoring_agent_endpoint.endsWith("/")) {
            KruizeDeploymentInfo.monitoring_agent_endpoint =
                    monitoring_agent_endpoint.substring(0, monitoring_agent_endpoint.length() - 1);
        } else {
            KruizeDeploymentInfo.monitoring_agent_endpoint = monitoring_agent_endpoint;
        }
    }


    public static void setCluster_type(String cluster_type) throws ClusterTypeNotSupportedException {
        if (cluster_type != null)
            cluster_type = cluster_type.toLowerCase();

        if (KruizeSupportedTypes.CLUSTER_TYPES_SUPPORTED.contains(cluster_type)) {
            KruizeDeploymentInfo.cluster_type = cluster_type;
        } else {
            LOGGER.error("Cluster type {} is not supported", cluster_type);
            throw new ClusterTypeNotSupportedException();
        }
    }


    public static void setKubernetesType(String kubernetesType) throws K8sTypeNotSupportedException {
        if (kubernetesType != null)
            kubernetesType = kubernetesType.toLowerCase();

        if (KruizeSupportedTypes.K8S_TYPES_SUPPORTED.contains(kubernetesType)) {
            KruizeDeploymentInfo.k8s_type = kubernetesType;
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


    public static void setAuth_type(String auth_type) {
        if (auth_type != null)
            auth_type = auth_type.toLowerCase();

        if (KruizeSupportedTypes.AUTH_TYPES_SUPPORTED.contains(auth_type)) {
            KruizeDeploymentInfo.auth_type = auth_type;
        }
    }


    public static void setMonitoring_agent(String monitoring_agent) throws MonitoringAgentNotSupportedException {
        if (monitoring_agent != null)
            monitoring_agent = monitoring_agent.toLowerCase();

        if (KruizeSupportedTypes.MONITORING_AGENTS_SUPPORTED.contains(monitoring_agent)) {
            KruizeDeploymentInfo.monitoring_agent = monitoring_agent;
        } else {
            LOGGER.error("Monitoring agent {}  is not supported", monitoring_agent);
            throw new MonitoringAgentNotSupportedException();
        }
    }


    public static void setMonitoringAgentService(String monitoringAgentService) {
        if (monitoringAgentService != null)
            KruizeDeploymentInfo.monitoring_service = monitoringAgentService.toLowerCase();
    }

    public static void logDeploymentInfo() {
        LOGGER.info("Cluster Type: {}", KruizeDeploymentInfo.cluster_type);
        LOGGER.info("Kubernetes Type: {}", KruizeDeploymentInfo.k8s_type);
        LOGGER.info("Auth Type: {}", KruizeDeploymentInfo.auth_type);
        LOGGER.info("Monitoring Agent: {}", KruizeDeploymentInfo.monitoring_agent);
        LOGGER.info("Monitoring Agent URL: {}", KruizeDeploymentInfo.monitoring_agent_endpoint);
        LOGGER.info("Monitoring agent service: {}", KruizeDeploymentInfo.monitoring_service);
        LOGGER.info("Kruize Local Flag: {}\n\n", KruizeDeploymentInfo.local);
        LOGGER.info("Log Request and Response: {}\n\n", KruizeDeploymentInfo.log_http_req_resp);
    }
}


