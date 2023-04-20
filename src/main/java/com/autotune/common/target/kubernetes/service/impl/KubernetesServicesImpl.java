/****************************************************************************
 Copyright (c) 2022 Red Hat, IBM Corporation and others.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.autotune.common.target.kubernetes.service.impl;

import com.autotune.common.trials.ContainerConfigData;
import com.autotune.common.target.common.exception.TargetHandlerConnectException;
import com.autotune.common.target.common.exception.TargetHandlerException;
import com.autotune.common.target.kubernetes.service.KubernetesServices;
import com.autotune.common.utils.ExponentialBackOff;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * KubernetesServicesImpl implements functions which are used to
 * communicate with Kubernetes cluster.
 */
public class KubernetesServicesImpl implements KubernetesServices {
    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesServicesImpl.class);
    private KubernetesClient kubernetesClient;

    public KubernetesServicesImpl() {
        initialize();
    }

    /**
     * kubernetesClient client connection established inside cluster itself
     */
    public void initialize() {
        try {
            this.kubernetesClient = new DefaultKubernetesClient();
        } catch (Exception e) {
            new TargetHandlerConnectException(e, "Default connection to kubernetes failed!");
        }
    }

    /**
     * getNamespaceList query and return list of Namespace object
     * later it will be processed further into JSON with only required attributes.
     *
     * @return List<Namespace>
     */
    @Override
    public List<Namespace> getNamespaces() {
        List<Namespace> namespaceList = null;
        try {
            namespaceList = kubernetesClient.namespaces().list().getItems();
        } catch (Exception e) {
            new TargetHandlerException(e, "getNamespaces failed!");
        }
        return namespaceList;
    }

    /**
     * Get kubernetes services.
     *
     * @param namespace
     * @return
     */
    @Override
    public List<Service> getServicelist(String namespace) {
        List<Service> serviceList = null;
        try {
            if (namespace != null) {
                serviceList = kubernetesClient.services().inNamespace(namespace).list().getItems();
            } else {
                serviceList = kubernetesClient.services().inAnyNamespace().list().getItems();
            }
        } catch (Exception e) {
            new TargetHandlerException(e, "getServicelist failed!");
        }
        return serviceList;
    }

    /**
     * Return list of pods in specified namespace.
     *
     * @param namespace
     * @return
     */
    public List<Pod> getPodsBy(String namespace) {
        List<Pod> podList = null;
        try {
            if (namespace != null) {
                podList = kubernetesClient
                        .pods()
                        .inNamespace(namespace)
                        .list()
                        .getItems();
            } else {
                podList = kubernetesClient
                        .pods()
                        .inAnyNamespace()
                        .list()
                        .getItems();
            }
        } catch (Exception e) {
            new TargetHandlerException(e, "getPodsBy failed!");
        }
        return podList;
    }

    /**
     * Return POD using name.
     *
     * @param namespace
     * @param name
     * @return
     */
    @Override
    public Pod getPodsBy(String namespace, String name) {
        Pod podObj = null;
        try {
            if (namespace != null) {
                podObj = kubernetesClient
                        .pods()
                        .inNamespace(namespace)
                        .withName(name).get();
            } else {
                throw new Exception("Namespace is mandatory to getPodsBy using name.");
            }
        } catch (Exception e) {
            new TargetHandlerException(e, "getPodsBy failed!");
        }
        return podObj;
    }

    /**
     * Get list pods using following params.
     *
     * @param namespace
     * @param labelKey
     * @param labelValue
     * @return
     */
    @Override
    public List<Pod> getPodsBy(String namespace, String labelKey, String labelValue) {
        List<Pod> podList = null;
        try {
            if (namespace != null) {
                podList = kubernetesClient
                        .pods()
                        .inNamespace(namespace)
                        .withLabel(labelKey, labelValue)
                        .list()
                        .getItems();
            } else {
                podList = kubernetesClient
                        .pods()
                        .inAnyNamespace()
                        .withLabel(labelKey, labelValue)
                        .list()
                        .getItems();
            }
        } catch (Exception e) {
            new TargetHandlerException(e, "getPodsBy failed!");
        }
        return podList;
    }

    /**
     * Get Replicas set.
     *
     * @param namespace
     * @param labelKey
     * @param labelValue
     * @return
     */
    @Override
    public List<ReplicaSet> getReplicasBy(String namespace, String labelKey, String labelValue) {
        List<ReplicaSet> replicasList = null;
        try {
            replicasList = kubernetesClient
                    .apps()
                    .replicaSets()
                    .inNamespace(namespace)
                    .withLabel(labelKey, labelValue)
                    .list()
                    .getItems();
        } catch (Exception e) {
            new TargetHandlerException(e, "getReplicasBy failed!");
        }
        return replicasList;
    }

    /**
     * Get CRD's environment variables.
     *
     * @param crd
     * @param namespace
     * @param kubernetesType
     * @return
     */
    @Override
    public Map<String, Object> getCRDEnvMap(CustomResourceDefinitionContext crd, String namespace, String kubernetesType) {
        Map<String, Object> envMap = null;
        try {
            envMap = kubernetesClient.customResource(crd).get(namespace, kubernetesType);
        } catch (Exception e) {
            new TargetHandlerException(e, "getCRDEnvMap failed!");
        }
        return envMap;
    }

    /**
     * Get deployment object
     *
     * @param namespace
     * @param deploymentName
     * @return
     */
    @Override
    public Deployment getDeploymentBy(String namespace, String deploymentName) {
        Deployment deployment = null;
        try {
            deployment = kubernetesClient
                    .apps()
                    .deployments()
                    .inNamespace(namespace)
                    .withName(deploymentName)
                    .get();
        } catch (Exception e) {
            new TargetHandlerException(e, "failed to get deployment with name: " + deploymentName + " under namespace: " + namespace);
        }
        return deployment;
    }

    @Override
    public List<Deployment> getDeploymentsBy(String namespace, String deploymentName, String labelKey, String labelValue) {
        List<Deployment> deploymentList = null;
        try {
            deploymentList = kubernetesClient
                    .apps()
                    .deployments()
                    .inNamespace(namespace)
                    .withLabel(labelKey, labelValue)
                    .list()
                    .getItems();
        } catch (Exception e) {
            new TargetHandlerException(e, "getDeploymentBy(namespace,deploymentName) failed!");
        }
        return deploymentList;
    }

    /**
     * Get list of deployments in the namespace
     * @param namespace
     * @return
     */
    @Override
    public List<Deployment> getDeploymentsBy(String namespace) {
        List<Deployment> deploymentList = null;
        if (null != namespace && !namespace.isBlank() && !namespace.isEmpty()) {
            deploymentList = kubernetesClient.apps().deployments().inNamespace(namespace).list().getItems();
        }
        return deploymentList;
    }

    /**
     * Replace with new deployment.
     *
     * @param namespace
     * @param deploymentName
     * @param newDeployment
     * @return
     */
    public boolean replaceDeployment(String namespace, String deploymentName, Deployment newDeployment) {
        boolean deployed = false;
        try {
            kubernetesClient
                    .apps()
                    .deployments()
                    .inNamespace(namespace)
                    .withName(deploymentName)
                    .createOrReplace(newDeployment);
            deployed = true;
        } catch (Exception e) {
            new TargetHandlerException(e, "replaceDeployment with the deployment name: " + deploymentName + " under namespace: " + namespace + "  failed!");
        }
        return deployed;
    }

    /**
     * Restart deployment. Used by EM to restart deployment during warmup/measurements cycles.
     *
     * @param namespace
     * @param deploymentName
     * @return
     */
    @Override
    public boolean restartDeployment(String namespace, String deploymentName) {
        boolean restarted = false;
        try {
            kubernetesClient
                    .apps()
                    .deployments()
                    .inNamespace(namespace)
                    .withName(deploymentName)
                    .rolling()
                    .restart();
            restarted = true;
        } catch (Exception e) {
            new TargetHandlerException(e, "restartDeployment with the deployment name: " + deploymentName + " under namespace: " + namespace + "  failed!");
        }
        return restarted;
    }

    /**
     * start Deploying using following params. Used by EM to apply config from trials
     *
     * @param namespace
     * @param deploymentName
     * @param containerConfigData
     * @return
     */
    @Override
    public boolean startDeploying(String namespace, String deploymentName, ContainerConfigData containerConfigData) {
        boolean deployed = false;
        try {
            Deployment existingDeployment = getDeploymentBy(namespace, deploymentName);
            if (existingDeployment != null) {
                Deployment newDeployment = amendDeployment(namespace, deploymentName, existingDeployment, containerConfigData);
                if (newDeployment != null) {
                    replaceDeployment(namespace, deploymentName, newDeployment);
                    deployed = true;
                }
            } else {
                throw new Exception("Deployment with name: " + deploymentName + " under namespace: " + namespace +" does not exist.");
            }
        } catch (Exception e) {
            new TargetHandlerException(e, "deployDeployment failed!");
        }
        return deployed;
    }

    /**
     * Get modified deployment as per configs.
     *
     * @param namespace
     * @param deploymentName
     * @param existingDeployment
     * @param containerConfigData
     * @return
     */
    public Deployment amendDeployment(String namespace, String deploymentName, Deployment existingDeployment, ContainerConfigData containerConfigData) {
        try {
            AtomicBoolean foundErrorInConfig = new AtomicBoolean(false);
            existingDeployment
                    .getSpec()
                    .getTemplate()
                    .getSpec()
                    .getContainers()   //TODO : Check for which container config should get applied
                    .forEach(
                            (deployedAppContainer) -> {
                                if (deployedAppContainer.getName().equals(containerConfigData.getContainerName())) {
                                    ResourceRequirements resourceRequirements = deployedAppContainer.getResources();
                                    if (null != containerConfigData.getRequestPropertiesMap())
                                        resourceRequirements.setRequests(containerConfigData.getRequestPropertiesMap());
                                    if (null != containerConfigData.getLimitPropertiesMap())
                                        resourceRequirements.setLimits(containerConfigData.getLimitPropertiesMap());
                                    deployedAppContainer.setResources(resourceRequirements);
                                    if (null != containerConfigData.getEnvList())
                                        deployedAppContainer.setEnv(containerConfigData.getEnvList());
                                    if (null != containerConfigData.getStackName())
                                        deployedAppContainer.setImage(containerConfigData.getStackName());
                                } else {
                                    foundErrorInConfig.set(true);
                                    return;
                                }
                            }
                    );
            if (foundErrorInConfig.get()) throw new Exception("Container Name: " + containerConfigData.getContainerName() + " not found in config");
        } catch (Exception e) {
            new TargetHandlerException(e, "getAmendedDeployment failed!");
        }
        return existingDeployment;
    }

    /**
     * Add Watcher.
     *
     * @param crd
     * @param watcher
     */
    @Override
    public void addWatcher(CustomResourceDefinitionContext crd, Watcher watcher) {
        try {
            kubernetesClient.customResource(crd).watch(watcher);
        } catch (Exception e) {
            LOGGER.warn("Watcher not added! Only REST API access is enabled.");
        }
    }

    /**
     * Get events.
     *
     * @param namespace
     * @param eventName
     * @return
     */
    @Override
    public Event getEvent(String namespace, String eventName) {
        Event event = null;
        try {
            event = kubernetesClient.events().inNamespace(namespace).withName(eventName).get();
        } catch (Exception e) {
            new TargetHandlerException(e, "getEvent failed! Event : " + event + " not found!");
        }
        return event;
    }

    /**
     * Replace events.
     *
     * @param namespace
     * @param eventName
     * @param newEvent
     * @return
     */
    @Override
    public boolean replaceEvent(String namespace, String eventName, Event newEvent) {
        boolean replaced = false;
        try {
            kubernetesClient.events().inNamespace(namespace).withName(eventName).replace(newEvent);
            replaced = true;
        } catch (Exception e) {
            new TargetHandlerException(e, "replaceEvent for the eventName " + eventName + " failed!");
        }
        return replaced;
    }

    /**
     * Create event.
     *
     * @param namespace
     * @param eventName
     * @param newEvent
     * @return
     */
    @Override
    public boolean createEvent(String namespace, String eventName, Event newEvent) {
        boolean created = false;
        try {
            kubernetesClient.events().inNamespace(namespace).withName(eventName).create(newEvent);
            created = true;
        } catch (Exception e) {
            new TargetHandlerException(e, "createEvent for the eventName " + eventName + " failed!");
        }
        return created;
    }

    /**
     * Watch endpoints to trigger events.
     *
     * @param crd
     */
    @Override
    public void watchEndpoints(CustomResourceDefinitionContext crd) {
        Watcher<String> autotuneObjectWatcher = new Watcher<>() {

            @Override
            public void eventReceived(Action action, String resource) {
            }

            @Override
            public void onClose(KubernetesClientException cause) {
            }
        };
        try {
            kubernetesClient.customResource(crd).watch(autotuneObjectWatcher);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isDeploymentReady(String namespace, String deploymentName) {
        boolean deploymentReady = false;

        try {
            Deployment existingDeployment = getDeploymentBy(namespace, deploymentName);
            if (existingDeployment != null) {
                DeploymentSpec spec = existingDeployment.getSpec();
                DeploymentStatus status = existingDeployment.getStatus();
                if (status == null || status.getReplicas() == null || status.getAvailableReplicas() == null) {
                    deploymentReady = false;
                } else if (spec == null || spec.getReplicas() == null) {
                    deploymentReady = false;
                } else {
                    deploymentReady = spec.getReplicas().intValue() == status.getReplicas() &&
                            spec.getReplicas().intValue() <= status.getAvailableReplicas();
                }
            } else {
                throw new Exception("Deployment: " + existingDeployment + " does not exist.");
            }
        } catch (Exception e) {
            new TargetHandlerException(e, "getDeploymentStatus failed!");
        }
        return deploymentReady;
    }

    @Override
    public boolean isDeploymentReady(String namespace, String deploymentName, ExponentialBackOff exponentialBackOff) {
        boolean deploymentReady = false;
        try {
            exponentialBackOff.waitBeforeFirstTry();
            while (exponentialBackOff.shouldRetry()) {
                deploymentReady = isDeploymentReady(namespace, deploymentName);
                if (deploymentReady) {
                    LOGGER.debug("Deployment is ready with in {}Millis / {}Millis", exponentialBackOff.getTotalRetryIntervalMillis(), exponentialBackOff.getMaxElapsedTimeMillis());
                    exponentialBackOff.doNotRetry();
                    break;
                } else {
                    LOGGER.debug("Deployment not yet ready with in {}Millis / {}Millis", exponentialBackOff.getTotalRetryIntervalMillis(), exponentialBackOff.getMaxElapsedTimeMillis());
                    exponentialBackOff.validateBackoff();
                }
            }
        } catch (Exception e) {
            new TargetHandlerException(e, "getDeploymentStatus failed!");
        }
        return deploymentReady;
    }

    @Override
    public boolean arePodsRunning(String namespace, String deploymentName) {   //ToDo :  filter pod list using deployment name
        boolean ready = false;
        int podsCount = -1;
        AtomicInteger podsReadyCount = new AtomicInteger();
        try {
            List<Pod> list = kubernetesClient.pods().inNamespace(namespace).list().getItems();
            podsCount = list.stream().collect(Collectors.toList()).size();
            list.forEach((p) -> {
                PodStatus pStatus = p.getStatus();
                if (pStatus.getPhase().equalsIgnoreCase("running"))
                    podsReadyCount.set(podsReadyCount.get() + 1);
            });
            if (podsCount == podsReadyCount.get()) {
                ready = true;
            }
        } catch (Exception e) {
            new TargetHandlerException(e, "awaitPodReadinessOrFail failed!");
        }
        return ready;
    }

    @Override
    public boolean arePodsRunning(String namespace, String deploymentName, ExponentialBackOff exponentialBackOff) {
        boolean running = false;
        try {
            exponentialBackOff.waitBeforeFirstTry();
            while (exponentialBackOff.shouldRetry()) {
                running = arePodsRunning(namespace, deploymentName);
                if (running) {
                    LOGGER.debug("PODS are running with in {}Millis / {}Millis", exponentialBackOff.getTotalRetryIntervalMillis(), exponentialBackOff.getMaxElapsedTimeMillis());
                    exponentialBackOff.doNotRetry();
                    break;
                } else {
                    LOGGER.debug("PODS are not yet running with in {}Millis / {}Millis", exponentialBackOff.getTotalRetryIntervalMillis(), exponentialBackOff.getMaxElapsedTimeMillis());
                    exponentialBackOff.validateBackoff();
                }
            }
        } catch (Exception e) {
            new TargetHandlerException(e, "awaitPodReadinessOrFail failed!");
        }
        return running;
    }

    /**
     * Close connection with Kubernetes.
     *
     * @return
     */
    @Override
    public boolean shutdownClient() {
        boolean closed = false;
        try {
            kubernetesClient.close();
            closed = true;
        } catch (Exception e) {
            new TargetHandlerException(e, "shutdownClient failed!");
        }
        return closed;
    }

}
