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
package com.autotune.analyzer.services;

import com.autotune.common.target.kubernetes.service.KubernetesServices;
import com.autotune.common.target.kubernetes.service.impl.KubernetesServicesImpl;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * EnableMonitoring servlet enables JMX exporter on Java workloads using ConfigMap-based configuration.
 * The JMX exporter agent is attached to the Java application via JAVA_OPTS, and metrics are exposed
 * on a dedicated port. A ConfigMap stores the JMX exporter configuration, which is mounted as a volume
 * in the pod. Prometheus can then scrape these metrics from the exposed port.
 */
public class EnableMonitoring extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnableMonitoring.class);
    
    // JMX Exporter constants
    private static final String JMX_EXPORTER_AGENT_PORT = "8778";
    private static final String JMX_PROMETHEUS_PORT_NAME = "jmx-metrics";
    private static final String JAVA_OPTS_ENV = "JAVA_OPTS";
    private static final String JMX_CONFIG_VOLUME_NAME = "jmx-exporter-config";
    private static final String JMX_CONFIG_MOUNT_PATH = "/opt/jmx-exporter";
    private static final String JMX_CONFIG_FILE = "jmx-exporter-config.yaml";
    private static final String JMX_JAR_FILE = "jmx_prometheus_javaagent.jar";
    private static final String JMX_CONFIGMAP_NAME_PREFIX = "jmx-exporter-config-";
    private static final String JMX_EXPORTER_VERSION = "0.17.2";
    private static final String JMX_EXPORTER_DOWNLOAD_URL = "https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/"
        + JMX_EXPORTER_VERSION + "/jmx_prometheus_javaagent-" + JMX_EXPORTER_VERSION + ".jar";
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        KubernetesServices kubernetesServices = null;
        JSONObject responseJson = new JSONObject();
        JSONArray successList = new JSONArray();
        JSONArray failureList = new JSONArray();
        
        try {
            // Set content type and encoding
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            
            // Read request body
            String inputData = request.getReader().lines().collect(Collectors.joining());
            JSONObject inputJson = new JSONObject(inputData);
            
            // Validate input
            if (!inputJson.has("containers")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                responseJson.put("error", "Missing 'containers' field in request body");
                response.getWriter().println(responseJson.toString(4));
                return;
            }
            
            JSONArray containers = inputJson.getJSONArray("containers");
            if (containers.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                responseJson.put("error", "Empty 'containers' array");
                response.getWriter().println(responseJson.toString(4));
                return;
            }
            
            // Initialize Kubernetes service
            kubernetesServices = new KubernetesServicesImpl();
            
            // Process each container
            for (int i = 0; i < containers.length(); i++) {
                JSONObject containerInfo = containers.getJSONObject(i);
                JSONObject result = processContainer(kubernetesServices, containerInfo);
                
                if (result.getBoolean("success")) {
                    successList.put(result);
                } else {
                    failureList.put(result);
                }
            }
            
            // Build response
            responseJson.put("message", "JMX monitoring enablement completed");
            responseJson.put("successful", successList);
            responseJson.put("failed", failureList);
            responseJson.put("total_processed", containers.length());
            responseJson.put("success_count", successList.length());
            responseJson.put("failure_count", failureList.length());
            
            // Set response status
            if (failureList.length() == 0) {
                response.setStatus(HttpServletResponse.SC_OK);
            } else if (successList.length() == 0) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            } else {
                // Partial success - use 207 Multi-Status (not in HttpServletResponse constants)
                response.setStatus(207);
            }
            
            response.getWriter().println(responseJson.toString(4));
            
        } catch (Exception e) {
            LOGGER.error("Error processing enableMonitoring request", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            responseJson.put("error", "Internal server error: " + e.getMessage());
            response.getWriter().println(responseJson.toString(4));
        } finally {
            if (kubernetesServices != null) {
                kubernetesServices.shutdownClient();
            }
        }
    }
    
    /**
     * Process a single container to enable JMX monitoring
     */
    private JSONObject processContainer(KubernetesServices kubernetesServices, JSONObject containerInfo) {
        JSONObject result = new JSONObject();
        
        try {
            // Extract container information
            String clusterName = containerInfo.optString("cluster_name", "");
            String namespace = containerInfo.getString("namespace");
            String workloadName = containerInfo.getString("workload_name");
            String workloadType = containerInfo.getString("workload_type");
            String containerName = containerInfo.getString("container_name");
            
            result.put("namespace", namespace);
            result.put("workload_name", workloadName);
            result.put("workload_type", workloadType);
            result.put("container_name", containerName);
            
            // Validate workload type
            if (!workloadType.equalsIgnoreCase("deployment")) {
                result.put("success", false);
                result.put("error", "Only 'deployment' workload type is currently supported");
                return result;
            }
            
            // Get the deployment
            Deployment deployment = kubernetesServices.getDeploymentBy(namespace, workloadName);
            if (deployment == null) {
                result.put("success", false);
                result.put("error", "Deployment not found: " + workloadName + " in namespace: " + namespace);
                return result;
            }
            
            // Enable JMX monitoring on the deployment
            boolean enabled = enableJMXMonitoring(kubernetesServices, deployment, namespace, workloadName, containerName);
            
            if (enabled) {
                result.put("success", true);
                result.put("message", "JMX monitoring enabled successfully");
            } else {
                result.put("success", false);
                result.put("error", "Failed to enable JMX monitoring");
            }
            
        } catch (Exception e) {
            LOGGER.error("Error processing container: " + containerInfo.toString(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Enable JMX monitoring on a deployment using ConfigMap-based JMX exporter configuration
     */
    private boolean enableJMXMonitoring(KubernetesServices kubernetesServices, Deployment deployment,
                                       String namespace, String workloadName, String containerName) {
        KubernetesClient client = null;
        try {
            // Create Kubernetes client
            client = new DefaultKubernetesClient();
            
            // Step 1: Create ConfigMap with JMX exporter configuration
            String configMapName = JMX_CONFIGMAP_NAME_PREFIX + workloadName;
            if (!createJMXConfigMap(client, namespace, configMapName)) {
                LOGGER.error("Failed to create ConfigMap for JMX exporter");
                return false;
            }
            
            // Step 2: Add init container to copy JAR from ConfigMap to emptyDir
            PodSpec podSpec = deployment.getSpec().getTemplate().getSpec();
            
            // Add init container if not already present
            List<Container> initContainers = podSpec.getInitContainers();
            if (initContainers == null) {
                initContainers = new ArrayList<>();
            }
            
            boolean initContainerExists = initContainers.stream()
                .anyMatch(c -> c.getName().equals("jmx-exporter-init"));
            
            if (!initContainerExists) {
                Container initContainer = new Container();
                initContainer.setName("jmx-exporter-init");
                initContainer.setImage("busybox:latest");
                initContainer.setCommand(List.of("sh", "-c",
                    "cp /config/" + JMX_JAR_FILE + " /jmx-exporter/ && " +
                    "cp /config/" + JMX_CONFIG_FILE + " /jmx-exporter/ && " +
                    "chmod 644 /jmx-exporter/*"));
                
                List<VolumeMount> initVolumeMounts = new ArrayList<>();
                VolumeMount configMount = new VolumeMount();
                configMount.setName(JMX_CONFIG_VOLUME_NAME);
                configMount.setMountPath("/config");
                configMount.setReadOnly(true);
                initVolumeMounts.add(configMount);
                
                VolumeMount jmxMount = new VolumeMount();
                jmxMount.setName("jmx-exporter-jar");
                jmxMount.setMountPath("/jmx-exporter");
                initVolumeMounts.add(jmxMount);
                
                initContainer.setVolumeMounts(initVolumeMounts);
                initContainers.add(initContainer);
                podSpec.setInitContainers(initContainers);
            }
            
            // Step 3: Update target container
            List<Container> containers = podSpec.getContainers();
            
            // Find the target container
            Container targetContainer = null;
            for (Container container : containers) {
                if (container.getName().equals(containerName)) {
                    targetContainer = container;
                    break;
                }
            }
            
            if (targetContainer == null) {
                LOGGER.error("Container {} not found in deployment {}", containerName, workloadName);
                return false;
            }
            
            // Add volume mount for JMX exporter JAR (from emptyDir)
            List<VolumeMount> volumeMounts = targetContainer.getVolumeMounts();
            if (volumeMounts == null) {
                volumeMounts = new ArrayList<>();
            }
            
            // Remove any existing mount at /opt/jmx-exporter (could be old ConfigMap mount)
            volumeMounts.removeIf(vm -> vm.getMountPath().equals(JMX_CONFIG_MOUNT_PATH));
            
            // Check if correct emptyDir mount exists
            boolean jmxVolumeMountExists = volumeMounts.stream()
                .anyMatch(vm -> vm.getName().equals("jmx-exporter-jar") &&
                               vm.getMountPath().equals(JMX_CONFIG_MOUNT_PATH));
            
            if (!jmxVolumeMountExists) {
                VolumeMount jmxMount = new VolumeMount();
                jmxMount.setName("jmx-exporter-jar");
                jmxMount.setMountPath(JMX_CONFIG_MOUNT_PATH);
                volumeMounts.add(jmxMount);
                LOGGER.info("Added emptyDir volume mount 'jmx-exporter-jar' at {}", JMX_CONFIG_MOUNT_PATH);
            } else {
                LOGGER.info("Correct emptyDir volume mount already exists at {}", JMX_CONFIG_MOUNT_PATH);
            }
            
            targetContainer.setVolumeMounts(volumeMounts);
            
            // Add JAVA_OPTS environment variable to attach JMX exporter agent
            List<EnvVar> envVars = targetContainer.getEnv();
            if (envVars == null) {
                envVars = new ArrayList<>();
            }
            
            String jmxAgentOpts = "-javaagent:/opt/jmx-exporter/jmx_prometheus_javaagent.jar=" +
                                 JMX_EXPORTER_AGENT_PORT + ":" + JMX_CONFIG_MOUNT_PATH + "/" + JMX_CONFIG_FILE;
            
            boolean javaOptsExists = false;
            for (EnvVar envVar : envVars) {
                if (envVar.getName().equals(JAVA_OPTS_ENV)) {
                    javaOptsExists = true;
                    String currentValue = envVar.getValue();
                    if (currentValue != null && !currentValue.contains("jmx_prometheus_javaagent")) {
                        envVar.setValue(currentValue + " " + jmxAgentOpts);
                        LOGGER.info("Updated existing JAVA_OPTS with JMX agent");
                    } else {
                        LOGGER.info("JAVA_OPTS already contains JMX agent configuration");
                    }
                    break;
                }
            }
            
            if (!javaOptsExists) {
                EnvVar javaOptsEnv = new EnvVar();
                javaOptsEnv.setName(JAVA_OPTS_ENV);
                javaOptsEnv.setValue(jmxAgentOpts);
                envVars.add(javaOptsEnv);
                targetContainer.setEnv(envVars);
                LOGGER.info("Added JAVA_OPTS environment variable with JMX agent");
            }
            
            // Add port for JMX metrics
            List<ContainerPort> ports = targetContainer.getPorts();
            if (ports == null) {
                ports = new ArrayList<>();
            }
            
            boolean portExists = ports.stream()
                .anyMatch(p -> (p.getName() != null && p.getName().equals(JMX_PROMETHEUS_PORT_NAME)) ||
                              (p.getContainerPort() != null && p.getContainerPort().equals(Integer.parseInt(JMX_EXPORTER_AGENT_PORT))));
            
            if (!portExists) {
                ContainerPort metricsPort = new ContainerPort();
                metricsPort.setName(JMX_PROMETHEUS_PORT_NAME);
                metricsPort.setContainerPort(Integer.parseInt(JMX_EXPORTER_AGENT_PORT));
                metricsPort.setProtocol("TCP");
                ports.add(metricsPort);
                targetContainer.setPorts(ports);
            } else {
                LOGGER.info("JMX metrics port {} already exists", JMX_EXPORTER_AGENT_PORT);
            }
            
            // Add volumes: ConfigMap and emptyDir
            List<Volume> volumes = podSpec.getVolumes();
            if (volumes == null) {
                volumes = new ArrayList<>();
            }
            
            // Add ConfigMap volume
            boolean configVolumeExists = volumes.stream()
                .anyMatch(v -> v.getName().equals(JMX_CONFIG_VOLUME_NAME));
            
            if (!configVolumeExists) {
                Volume configVolume = new Volume();
                configVolume.setName(JMX_CONFIG_VOLUME_NAME);
                ConfigMapVolumeSource configMapSource = new ConfigMapVolumeSource();
                configMapSource.setName(configMapName);
                configVolume.setConfigMap(configMapSource);
                volumes.add(configVolume);
            } else {
                LOGGER.info("ConfigMap volume '{}' already exists", JMX_CONFIG_VOLUME_NAME);
            }
            
            // Add emptyDir volume for JAR
            boolean jmxVolumeExists = volumes.stream()
                .anyMatch(v -> v.getName().equals("jmx-exporter-jar"));
            
            if (!jmxVolumeExists) {
                Volume jmxVolume = new Volume();
                jmxVolume.setName("jmx-exporter-jar");
                EmptyDirVolumeSource emptyDir = new EmptyDirVolumeSource();
                jmxVolume.setEmptyDir(emptyDir);
                volumes.add(jmxVolume);
            } else {
                LOGGER.info("EmptyDir volume 'jmx-exporter-jar' already exists");
            }
            
            podSpec.setVolumes(volumes);
            
            // Update the deployment
            LOGGER.info("Attempting to replace deployment {} in namespace {}", workloadName, namespace);
            boolean updated = kubernetesServices.replaceDeployment(namespace, workloadName, deployment);
            
            if (updated) {
                LOGGER.info("Successfully enabled JMX monitoring for deployment {} in namespace {}",
                           workloadName, namespace);
                
                // Force pod restart to apply changes
                LOGGER.info("Restarting deployment {} to apply JMX monitoring changes", workloadName);
                boolean restarted = kubernetesServices.restartDeployment(namespace, workloadName);
                if (restarted) {
                    LOGGER.info("Successfully restarted deployment {} in namespace {}", workloadName, namespace);
                } else {
                    LOGGER.warn("Failed to restart deployment {} in namespace {} - pods may not have JMX agent loaded",
                               workloadName, namespace);
                }
                
                // Update Service to add JMX metrics port
                boolean serviceUpdated = updateServiceWithJMXPort(client, namespace, workloadName);
                if (serviceUpdated) {
                    LOGGER.info("Successfully updated Service with JMX metrics port for deployment {} in namespace {}",
                               workloadName, namespace);
                } else {
                    LOGGER.warn("Failed to update Service with JMX metrics port for deployment {} in namespace {}",
                               workloadName, namespace);
                }
                
                // Create ServiceMonitor for Prometheus to scrape JMX metrics
                boolean serviceMonitorCreated = createServiceMonitor(client, namespace, workloadName);
                if (serviceMonitorCreated) {
                    LOGGER.info("Successfully created ServiceMonitor for deployment {} in namespace {}",
                               workloadName, namespace);
                } else {
                    LOGGER.warn("Failed to create ServiceMonitor for deployment {} in namespace {} - metrics may not be scraped by Prometheus",
                               workloadName, namespace);
                }
            } else {
                LOGGER.error("Failed to update deployment {} in namespace {} - replaceDeployment returned false",
                           workloadName, namespace);
            }
            
            return updated;
            
        } catch (Exception e) {
            LOGGER.error("Error enabling JMX monitoring for deployment {} in namespace {}: {}",
                        workloadName, namespace, e.getMessage(), e);
            return false;
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }
    
    /**
     * Create ConfigMap with JMX exporter configuration and JAR file using Kubernetes client
     */
    private boolean createJMXConfigMap(KubernetesClient client, String namespace, String configMapName) {
        try {
            // Default JMX exporter configuration
            String jmxConfig = "lowercaseOutputName: true\n" +
                "lowercaseOutputLabelNames: true\n" +
                "rules:\n" +
                "- pattern: \".*\"\n";
            
            // Download JMX exporter JAR
            LOGGER.info("Downloading JMX exporter JAR from {}", JMX_EXPORTER_DOWNLOAD_URL);
            byte[] jarBytes = downloadJMXExporterJar();
            
            if (jarBytes == null || jarBytes.length == 0) {
                LOGGER.error("Failed to download JMX exporter JAR");
                return false;
            }
            
            LOGGER.info("Successfully downloaded JMX exporter JAR ({} bytes)", jarBytes.length);
            
            // Create ConfigMap with both config and JAR as binary data
            Map<String, String> data = new HashMap<>();
            data.put(JMX_CONFIG_FILE, jmxConfig);
            
            Map<String, String> binaryData = new HashMap<>();
            binaryData.put(JMX_JAR_FILE, java.util.Base64.getEncoder().encodeToString(jarBytes));
            
            ConfigMap configMap = new ConfigMapBuilder()
                .withNewMetadata()
                    .withName(configMapName)
                    .withNamespace(namespace)
                .endMetadata()
                .withData(data)
                .withBinaryData(binaryData)
                .build();
            
            client.configMaps().inNamespace(namespace).resource(configMap).createOrReplace();
            LOGGER.info("Created ConfigMap {} in namespace {} with JMX exporter JAR", configMapName, namespace);
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Error creating ConfigMap: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Download JMX exporter JAR file from Maven Central
     */
    private byte[] downloadJMXExporterJar() {
        try {
            java.net.URL url = new java.net.URL(JMX_EXPORTER_DOWNLOAD_URL);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000); // 30 seconds
            connection.setReadTimeout(60000); // 60 seconds

            int responseCode = connection.getResponseCode();
            if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                java.io.InputStream inputStream = connection.getInputStream();
                java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                inputStream.close();
                outputStream.close();

                return outputStream.toByteArray();
            } else {
                LOGGER.error("Failed to download JMX exporter JAR. HTTP response code: {}", responseCode);
                return null;
            }
        } catch (Exception e) {
            LOGGER.error("Error downloading JMX exporter JAR: " + e.getMessage(), e);
            return null;
        }
    }
    
    
    /**
     * Update Service to add JMX metrics port if it doesn't exist
     * 
     * @param client Kubernetes client
     * @param namespace Namespace where the service exists
     * @param workloadName Name of the workload (used to find the service)
     * @return true if Service was updated successfully, false otherwise
     */
    private boolean updateServiceWithJMXPort(KubernetesClient client, String namespace, String workloadName) {
        try {
            // Try to find service with matching labels
            List<Service> services = client.services()
                .inNamespace(namespace)
                .withLabel("app", workloadName)
                .list()
                .getItems();
            
            if (services.isEmpty()) {
                LOGGER.warn("No Service found with label app={} in namespace {}", workloadName, namespace);
                return false;
            }
            
            // Use the first matching service
            Service service = services.get(0);
            String serviceName = service.getMetadata().getName();
            LOGGER.info("Found Service {} for workload {} in namespace {}", serviceName, workloadName, namespace);
            
            // Check if JMX metrics port already exists
            List<ServicePort> ports = service.getSpec().getPorts();
            boolean jmxPortExists = ports.stream()
                .anyMatch(p -> p.getName() != null && p.getName().equals(JMX_PROMETHEUS_PORT_NAME));
            
            if (jmxPortExists) {
                LOGGER.info("Service {} already has JMX metrics port", serviceName);
                return true;
            }
            
            // Ensure all existing ports have names
            for (int i = 0; i < ports.size(); i++) {
                ServicePort port = ports.get(i);
                if (port.getName() == null || port.getName().isEmpty()) {
                    // Generate a name based on port number
                    String portName = "port-" + port.getPort();
                    port.setName(portName);
                    LOGGER.info("Added name '{}' to unnamed port {} in Service {}", portName, port.getPort(), serviceName);
                }
            }
            
            // Add JMX metrics port
            ServicePort jmxPort = new ServicePort();
            jmxPort.setName(JMX_PROMETHEUS_PORT_NAME);
            jmxPort.setPort(Integer.parseInt(JMX_EXPORTER_AGENT_PORT));
            jmxPort.setTargetPort(new IntOrString(Integer.parseInt(JMX_EXPORTER_AGENT_PORT)));
            jmxPort.setProtocol("TCP");
            ports.add(jmxPort);
            
            // Update the service
            service.getSpec().setPorts(ports);
            client.services()
                .inNamespace(namespace)
                .withName(serviceName)
                .replace(service);
            
            LOGGER.info("Successfully added JMX metrics port {} to Service {}", JMX_EXPORTER_AGENT_PORT, serviceName);
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Error updating Service with JMX port: {}", e.getMessage(), e);
            return false;
        }
    }
    /**
     * Create ServiceMonitor for Prometheus to scrape JMX metrics
     * 
     * @param client Kubernetes client
     * @param namespace Namespace where the deployment exists
     * @param workloadName Name of the deployment
     * @return true if ServiceMonitor was created successfully, false otherwise
     */
    private boolean createServiceMonitor(KubernetesClient client, String namespace, String workloadName) {
        try {
            String serviceMonitorName = "jmx-metrics-" + workloadName;
            
            // Define ServiceMonitor CRD context
            CustomResourceDefinitionContext serviceMonitorCRD = new CustomResourceDefinitionContext.Builder()
                .withGroup("monitoring.coreos.com")
                .withVersion("v1")
                .withKind("ServiceMonitor")
                .withPlural("servicemonitors")
                .withScope("Namespaced")
                .build();
            
            // Create ServiceMonitor spec
            Map<String, Object> serviceMonitor = new HashMap<>();
            serviceMonitor.put("apiVersion", "monitoring.coreos.com/v1");
            serviceMonitor.put("kind", "ServiceMonitor");
            
            // Metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("name", serviceMonitorName);
            metadata.put("namespace", namespace);
            Map<String, String> labels = new HashMap<>();
            labels.put("app", workloadName);
            labels.put("monitoring", "jmx-exporter");
            // Add OpenShift Prometheus collection profile label (required for Prometheus to discover this ServiceMonitor)
            labels.put("monitoring.openshift.io/collection-profile", "full");
            metadata.put("labels", labels);
            serviceMonitor.put("metadata", metadata);
            
            // Spec
            Map<String, Object> spec = new HashMap<>();
            
            // Selector
            Map<String, Object> selector = new HashMap<>();
            Map<String, String> matchLabels = new HashMap<>();
            matchLabels.put("app", workloadName);
            selector.put("matchLabels", matchLabels);
            spec.put("selector", selector);
            
            // Endpoints
            List<Map<String, Object>> endpoints = new ArrayList<>();
            Map<String, Object> endpoint = new HashMap<>();
            endpoint.put("port", JMX_PROMETHEUS_PORT_NAME);
            endpoint.put("interval", "30s");
            endpoint.put("path", "/metrics");
            endpoints.add(endpoint);
            spec.put("endpoints", endpoints);
            
            serviceMonitor.put("spec", spec);
            
            // Convert Map to GenericKubernetesResource
            String serviceMonitorJson = Serialization.asJson(serviceMonitor);
            GenericKubernetesResource serviceMonitorResource = Serialization.unmarshal(serviceMonitorJson, GenericKubernetesResource.class);
            
            // Check if ServiceMonitor already exists
            try {
                GenericKubernetesResource existing = client.genericKubernetesResources(serviceMonitorCRD)
                    .inNamespace(namespace)
                    .withName(serviceMonitorName)
                    .get();
                
                if (existing != null) {
                    LOGGER.info("ServiceMonitor {} already exists in namespace {}, updating it",
                               serviceMonitorName, namespace);
                    client.genericKubernetesResources(serviceMonitorCRD)
                        .inNamespace(namespace)
                        .withName(serviceMonitorName)
                        .patch(serviceMonitorResource);
                } else {
                    // Create new ServiceMonitor
                    client.genericKubernetesResources(serviceMonitorCRD)
                        .inNamespace(namespace)
                        .create(serviceMonitorResource);
                    LOGGER.info("Created ServiceMonitor {} in namespace {}", serviceMonitorName, namespace);
                }
                
                return true;
            } catch (Exception e) {
                LOGGER.error("Error creating/updating ServiceMonitor: {}", e.getMessage(), e);
                return false;
            }
            
        } catch (Exception e) {
            LOGGER.error("Error in createServiceMonitor: {}", e.getMessage(), e);
            return false;
        }
    }
}

