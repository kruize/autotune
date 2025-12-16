/*******************************************************************************
 * Copyright (c) 2020, 2022 Red Hat, IBM Corporation and others.
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

import com.autotune.analyzer.application.ApplicationDeployment;
import com.autotune.analyzer.application.ApplicationServiceStack;
import com.autotune.analyzer.exceptions.*;
import com.autotune.analyzer.experiment.ExperimentInitiator;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.kruizeObject.SelectorInfo;
import com.autotune.analyzer.kruizeObject.SloInfo;
import com.autotune.analyzer.metadataProfiles.MetadataProfileDeployment;
import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.analyzer.performanceProfiles.PerformanceProfilesDeployment;
import com.autotune.analyzer.performanceProfiles.utils.PerformanceProfileUtil;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.k8sObjects.KubernetesContexts;
import com.autotune.common.target.kubernetes.service.KubernetesServices;
import com.autotune.common.target.kubernetes.service.impl.KubernetesServicesImpl;
import com.autotune.utils.EventLogger;
import com.autotune.utils.KubeEventLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.autotune.analyzer.utils.AnalyzerConstants.POD_TEMPLATE_HASH;

/**
 * Maintains information about the Autotune resources deployed in the cluster
 */
public class KruizeOperator {
    private static final Logger LOGGER = LoggerFactory.getLogger(KruizeOperator.class);
    /**
     * Key: Name of autotuneObject
     * Value: AutotuneObject instance matching the name
     */
    public static Map<String, KruizeObject> autotuneObjectMap = new ConcurrentHashMap<>();
    /**
     * Outer map:
     * Key: Name of autotune Object
     * <p>
     * Inner map:
     * Key: Name of deployment
     * Value: ApplicationDeployment instance matching the name
     */
    public static Map<String, Map<String, ApplicationDeployment>> deploymentMap = new HashMap<>();

    /**
     * Get Autotune objects from kubernetes, and watch for any additions, modifications or deletions.
     * Add obtained autotune objects to map and match autotune object with pods.
     *
     * @param kruizeOperator
     * @throws IOException if unable to get Kubernetes config
     */
    public static void getKruizeObjects(final KruizeOperator kruizeOperator) throws IOException {
        /* Watch for events (additions, modifications or deletions) of autotune objects */
        Watcher<String> autotuneObjectWatcher = new Watcher<>() {
            @Override
            public void eventReceived(Action action, String resource) {
                KruizeObject kruizeObject = null;

                switch (action.toString().toUpperCase()) {
                    case "MODIFIED":
                    case "ADDED":        //TO DO consider MODIFIED after discussing PATCH request on already created experiments.
                        kruizeObject = getKruizeObject(resource);
                        processKruizeObject(kruizeObject);
                        break;
                    case "DELETED":
                        deleteExistingAutotuneObject(resource);
                    default:
                        break;
                }
            }


            @Override
            public void onClose(WatcherException e) {
            }
        };

        KubernetesServices kubernetesServices = new KubernetesServicesImpl();
        kubernetesServices.addWatcher(KubernetesContexts.getAutotuneCrdContext(), autotuneObjectWatcher);
    }

    public static void processKruizeObject(KruizeObject kruizeObject) {
        try {
            if (null != kruizeObject) {
                List<KruizeObject> kruizeObjectList = new ArrayList<>();
                kruizeObjectList.add(kruizeObject);
                ExperimentInitiator experimentInitiator = new ExperimentInitiator();
                experimentInitiator.validateAndAddNewExperiments(autotuneObjectMap, kruizeObjectList);
                KruizeObject invalidKruizeObject = kruizeObjectList.stream().filter((ko) -> (!ko.getValidation_data().isSuccess())).findAny().orElse(null);
                if (invalidKruizeObject != null) {
                    new KubeEventLogger(Clock.systemUTC()).log("Failed", invalidKruizeObject.getValidation_data().getMessage(), EventLogger.Type.Warning, null, null, kruizeObject.getObjectReference(), null);
                } else {
                    LOGGER.debug(kruizeObject.getExperimentName() + " " + kruizeObject.getValidation_data().getMessage());
                }
            } else {
                new KubeEventLogger(Clock.systemUTC()).log("Failed", "Not able to process KruizeObject ", EventLogger.Type.Warning, null, null, kruizeObject.getObjectReference(), null);
            }
        } catch (Exception e) {
            new KubeEventLogger(Clock.systemUTC()).log("Failed", e.getMessage(), EventLogger.Type.Warning, null, null, kruizeObject.getObjectReference(), null);
        }
    }

    /**
     * Add autotuneobject to monitoring map and match pods and autotuneconfigs
     *
     * @param kruizeObject
     */
    private static void addAutotuneObject(KruizeObject kruizeObject) {
        autotuneObjectMap.put(kruizeObject.getExperimentName(), kruizeObject);
        matchPodsToAutotuneObject(kruizeObject);
    }

    /**
     * Delete autotuneobject that's currently monitored
     *
     * @param autotuneObject
     */
    private static void deleteExistingAutotuneObject(String autotuneObject) {
        JSONObject autotuneObjectJson = new JSONObject(autotuneObject);
        String name = autotuneObjectJson.getJSONObject(AnalyzerConstants.AutotuneObjectConstants.METADATA)
                .optString(AnalyzerConstants.AutotuneObjectConstants.NAME);

        autotuneObjectMap.remove(name);
        deploymentMap.remove(name);
        // TODO: Stop all the experiments
        LOGGER.info("Deleted autotune object {}", name);
    }


    /**
     * Get map of pods matching the autotune object using the labels.
     *
     * @param kruizeObject
     */
    public static void matchPodsToAutotuneObject(KruizeObject kruizeObject) {
        KubernetesServices kubernetesServices = null;
        try {
            String userLabelKey = kruizeObject.getSelectorInfo().getMatchLabel();
            String userLabelValue = kruizeObject.getSelectorInfo().getMatchLabelValue();
            kubernetesServices = new KubernetesServicesImpl();
            String namespace = kruizeObject.getNamespace();
            String experimentName = kruizeObject.getExperimentName();
            List<Pod> podList = new KubernetesServicesImpl().getPodsBy(namespace, userLabelKey, userLabelValue);
            if (podList.isEmpty()) {
                LOGGER.error("autotune object " + kruizeObject.getExperimentName() + " not added as no related deployments found!");
                // TODO: No matching pods with the userLabelKey found, need to warn the user.
                return;
            }

            // We now have a list of pods. Get the stack (ie docker image) for each pod.
            // Add the unique set of stacks and create an ApplicationServiceStack object for each.
            for (Pod pod : podList) {
                ObjectMeta podMetadata = pod.getMetadata();
                String podTemplateHash = podMetadata.getLabels().get(POD_TEMPLATE_HASH);
                String status = pod.getStatus().getPhase();
                // We want to find the deployment name for this pod.
                // To find that we first find the replicaset corresponding to the pod template hash
                // Replicaset name is of the form 'deploymentName-podTemplateHash'
                // So to get the deployment name we remove the '-podTemplateHash' from the Replicaset name
                List<ReplicaSet> replicaSetList = kubernetesServices.getReplicasBy(namespace, POD_TEMPLATE_HASH, podTemplateHash);
                if (replicaSetList.isEmpty()) {
                    LOGGER.error("autotune object " + kruizeObject.getExperimentName() + " not added as no related deployments found!");
                    // TODO: No matching pods with the userLabelKey found, need to warn the user.
                    return;
                }
                String deploymentName = null;
                for (ReplicaSet replicaSet : replicaSetList) {
                    String replicasetName = replicaSet.getMetadata().getName();
                    StringBuilder podHashSb = new StringBuilder("-").append(podTemplateHash);
                    deploymentName = replicasetName.replace(podHashSb.toString(), "");
                    Deployment deployment = kubernetesServices.getDeploymentBy(namespace, deploymentName);
                    LOGGER.debug("Pod: " + podMetadata.getName()
                            + " podTemplateHash: " + podTemplateHash
                            + " replicasetName: " + replicasetName
                            + " deploymentName: " + deploymentName);

                    if (deployment != null) {
                        // Add the deployment if it is already not there
                        ApplicationDeployment applicationDeployment = null;
                        if (!deploymentMap.containsKey(experimentName)) {
                            applicationDeployment = new ApplicationDeployment(deploymentName,
                                    experimentName, namespace,
                                    deployment.getStatus().toString());
                            Map<String, ApplicationDeployment> depMap = new HashMap<>();
                            depMap.put(deploymentName, applicationDeployment);
                            deploymentMap.put(experimentName, depMap);
                        } else {
                            applicationDeployment = deploymentMap.get(experimentName).get(deploymentName);
                        }
                        // Check docker image id for each container in the pod
                        for (Container container : pod.getSpec().getContainers()) {
                            String containerImageName = container.getImage();
                            String containerName = container.getName();
                            ApplicationServiceStack applicationServiceStack = new ApplicationServiceStack(containerImageName,
                                    containerName);
                            assert (applicationDeployment == null);
                            // Add the container image if it has not already been added to the deployment
                            if (!applicationDeployment.getApplicationServiceStackMap().containsKey(containerImageName)) {
                                applicationDeployment.getApplicationServiceStackMap().put(containerImageName, applicationServiceStack);
                            }
                        }
                        break;
                    }
                }
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add Kruize object to map of monitored objects.
     *
     * @param autotuneObjectJsonStr JSON string of the autotune object
     */
    private static KruizeObject getKruizeObject(String autotuneObjectJsonStr) {
        try {
            //TODO: Make a common method to add data for both CRD and non-CRD
            JSONObject autotuneObjectJson = new JSONObject(autotuneObjectJsonStr);
            JSONObject metadataJson = autotuneObjectJson.getJSONObject(AnalyzerConstants.AutotuneObjectConstants.METADATA);
            String name;
            String perfProfileName = null;
            String metadataProfileName = null;
            String mode;
            String targetCluster;
            String clusterName;
            String namespace;
            String datasource;
            SelectorInfo selectorInfo;
            JSONObject sloJson = null;
            String hpoAlgoImpl = null;

            JSONObject specJson = autotuneObjectJson.optJSONObject(AnalyzerConstants.AutotuneObjectConstants.SPEC);

            mode = specJson.optString(AnalyzerConstants.AutotuneObjectConstants.MODE,
                    AnalyzerConstants.AutotuneObjectConstants.DEFAULT_MODE);
            targetCluster = specJson.optString(AnalyzerConstants.AutotuneObjectConstants.TARGET_CLUSTER,
                    AnalyzerConstants.AutotuneObjectConstants.DEFAULT_TARGET_CLUSTER);
            clusterName = specJson.optString(AnalyzerConstants.AutotuneObjectConstants.CLUSTER_NAME);
            datasource = specJson.optString(AnalyzerConstants.AutotuneObjectConstants.DATASOURCE);
            name = metadataJson.optString(AnalyzerConstants.AutotuneObjectConstants.NAME);
            namespace = metadataJson.optString(AnalyzerConstants.AutotuneObjectConstants.NAMESPACE);
            hpoAlgoImpl = specJson.optString(AnalyzerConstants.AutotuneObjectConstants.HPO_ALGO_IMPL,
                    AnalyzerConstants.AutotuneObjectConstants.DEFAULT_HPO_ALGO_IMPL);

            JSONObject selectorJson = null;
            if (specJson != null) {
                sloJson = specJson.optJSONObject(AnalyzerConstants.AutotuneObjectConstants.SLO);
                perfProfileName = specJson.optString(AnalyzerConstants.PerformanceProfileConstants.PERF_PROFILE);

                if (!perfProfileName.isEmpty()) {
                    if (!sloJson.isEmpty()) {
                        throw new SloClassNotSupportedException(AnalyzerErrorConstants.AutotuneObjectErrors.SLO_REDUNDANCY_ERROR);
                    } else {
                        // check if the Performance profile with the given name exist
                        if (null == PerformanceProfilesDeployment.performanceProfilesMap.get(perfProfileName)) {
                            throw new SloClassNotSupportedException(AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_PERF_PROFILE + perfProfileName);
                        }
                    }
                } else {
                    if (sloJson.isEmpty()) {
                        throw new SloClassNotSupportedException(AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_SLO_DATA);
                    } else {
                        // Only SLO is available so create a default Performance profile with SLO data
                        SloInfo sloInfo = new Gson().fromJson(String.valueOf(sloJson), SloInfo.class);
                        perfProfileName = setDefaultPerformanceProfile(sloInfo, mode, targetCluster);
                    }
                }

                // 'metadata_profile' field is applicable only for local_monitoring experiments
                if (KruizeDeploymentInfo.local && targetCluster.equalsIgnoreCase(AnalyzerConstants.LOCAL)) {
                    metadataProfileName = specJson.optString(AnalyzerConstants.MetadataProfileConstants.METADATA_PROFILE);
                    if (!metadataProfileName.isEmpty()) {
                        // check if the metadata profile with the given name exist
                        if (null == MetadataProfileDeployment.metadataProfilesMap.get(metadataProfileName)) {
                            throw new NullPointerException(AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_METADATA_PROFILE + metadataProfileName);
                        }
                    } else {
                        throw new InvalidValueException(AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_METADATA_PROFILE_FIELD);
                    }
                }

                selectorJson = specJson.getJSONObject(AnalyzerConstants.AutotuneObjectConstants.SELECTOR);
            }
            assert selectorJson != null;
            String matchLabel = selectorJson.optString(AnalyzerConstants.AutotuneObjectConstants.MATCH_LABEL);
            String matchLabelValue = selectorJson.optString(AnalyzerConstants.AutotuneObjectConstants.MATCH_LABEL_VALUE);
            String matchRoute = selectorJson.optString(AnalyzerConstants.AutotuneObjectConstants.MATCH_ROUTE);
            String matchURI = selectorJson.optString(AnalyzerConstants.AutotuneObjectConstants.MATCH_URI);
            String matchService = selectorJson.optString(AnalyzerConstants.AutotuneObjectConstants.MATCH_SERVICE);

            selectorInfo = new SelectorInfo(matchLabel,
                    matchLabelValue,
                    matchRoute,
                    matchURI,
                    matchService);

            String resourceVersion = metadataJson.optString(AnalyzerConstants.RESOURCE_VERSION);
            String uid = metadataJson.optString(AnalyzerConstants.UID);
            String apiVersion = autotuneObjectJson.optString(AnalyzerConstants.API_VERSION);
            String kind = autotuneObjectJson.optString(AnalyzerConstants.KIND);

            ObjectReference objectReference = new ObjectReference(apiVersion,
                    null,
                    kind,
                    name,
                    namespace,
                    resourceVersion,
                    uid);

            return new KruizeObject(name,
                    clusterName,
                    namespace,
                    mode,
                    targetCluster,
                    hpoAlgoImpl,
                    selectorInfo,
                    perfProfileName,
                    metadataProfileName,
                    datasource,
                    objectReference
            );

        } catch (InvalidValueException | NullPointerException | JSONException | SloClassNotSupportedException e) {
            LOGGER.error(e.getMessage());
            new KubeEventLogger(Clock.systemUTC()).log("Failed", e.getMessage(), EventLogger.Type.Warning, null, null, null, null);
            return null;
        }
    }

    public static String setDefaultPerformanceProfile(SloInfo sloInfo, String mode, String targetCluster) {
        PerformanceProfile performanceProfile = null;
        try {
            String name = AnalyzerConstants.PerformanceProfileConstants.DEFAULT_PROFILE;
            double profile_version = AnalyzerConstants.DEFAULT_PROFILE_VERSION;
            String k8s_type = AnalyzerConstants.DEFAULT_K8S_TYPE;
            performanceProfile = new PerformanceProfile(name, profile_version, k8s_type, sloInfo);

            if (null != performanceProfile) {
                ValidationOutputData validationOutputData = PerformanceProfileUtil.validateAndAddProfile(PerformanceProfilesDeployment.performanceProfilesMap, performanceProfile, AnalyzerConstants.OperationType.CREATE);
                if (validationOutputData.isSuccess()) {
                    LOGGER.info("Added Performance Profile : {} into the map with version: {}",
                            performanceProfile.getName(), performanceProfile.getProfile_version());
                } else {
                    new KubeEventLogger(Clock.systemUTC()).log("Failed", validationOutputData.getMessage(), EventLogger.Type.Warning, null, null, null, null);
                }
            } else {
                new KubeEventLogger(Clock.systemUTC()).log("Failed", "Unable to create performance profile ", EventLogger.Type.Warning, null, null, null, null);
            }
        } catch (Exception e) {
            LOGGER.error("Exception while adding PP with message: {} ", e.getMessage());
            new KubeEventLogger(Clock.systemUTC()).log("Failed", e.getMessage(), EventLogger.Type.Warning, null, null, null, null);
            return null;
        }
        return performanceProfile.getName();
    }

    public static String setDefaultMetricProfile(SloInfo sloInfo, String mode, String targetCluster) {
        PerformanceProfile metricProfile = null;
        try {
            String apiVersion = AnalyzerConstants.PerformanceProfileConstants.DEFAULT_API_VERSION;
            String kind = AnalyzerConstants.PerformanceProfileConstants.DEFAULT_KIND;
            String name = AnalyzerConstants.PerformanceProfileConstants.DEFAULT_PROFILE;
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode metadataNode = objectMapper.createObjectNode();
            metadataNode.put("name",name);

            double profile_version = AnalyzerConstants.DEFAULT_PROFILE_VERSION;
            String k8s_type = AnalyzerConstants.DEFAULT_K8S_TYPE;
            metricProfile = new PerformanceProfile(apiVersion, kind, metadataNode, profile_version, k8s_type, sloInfo);

            if (null != metricProfile) {
                ValidationOutputData validationOutputData = PerformanceProfileUtil.validateAndAddMetricProfile(PerformanceProfilesDeployment.performanceProfilesMap, metricProfile);
                if (validationOutputData.isSuccess()) {
                    LOGGER.info("Added metric Profile : {} into the map with version: {}",
                            metricProfile.getName(), metricProfile.getProfile_version());
                } else {
                    new KubeEventLogger(Clock.systemUTC()).log("Failed", validationOutputData.getMessage(), EventLogger.Type.Warning, null, null, null, null);
                }
            } else {
                new KubeEventLogger(Clock.systemUTC()).log("Failed", "Unable to create metric profile ", EventLogger.Type.Warning, null, null, null, null);
            }
        } catch (Exception e) {
            LOGGER.error("Exception while adding Metric profile with message: {} ", e.getMessage());
            new KubeEventLogger(Clock.systemUTC()).log("Failed", e.getMessage(), EventLogger.Type.Warning, null, null, null, null);
            return null;
        }
        return metricProfile.getName();
    }
}
