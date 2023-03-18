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
import com.autotune.analyzer.application.Tunable;
import com.autotune.analyzer.exceptions.*;
import com.autotune.analyzer.experiment.ExperimentInitiator;
import com.autotune.analyzer.variables.Variables;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.data.datasource.DataSource;
import com.autotune.common.data.datasource.DataSourceFactory;
import com.autotune.common.k8sObjects.*;
import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.analyzer.performanceProfiles.PerformanceProfileInterface.PerfProfileImpl;
import com.autotune.analyzer.performanceProfiles.PerformanceProfilesDeployment;
import com.autotune.common.target.kubernetes.service.KubernetesServices;
import com.autotune.common.target.kubernetes.service.impl.KubernetesServicesImpl;
import com.autotune.utils.AnalyzerConstants;
import com.autotune.utils.AnalyzerConstants.AutotuneConfigConstants;
import com.autotune.utils.AnalyzerErrorConstants;
import com.autotune.utils.EventLogger;
import com.autotune.utils.KubeEventLogger;
import com.google.gson.Gson;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.autotune.utils.AnalyzerConstants.POD_TEMPLATE_HASH;

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
    public static Map<String, KruizeLayer> autotuneConfigMap = new HashMap<>();
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
            public void onClose(KubernetesClientException e) {
            }
        };

        Watcher<String> autotuneConfigWatcher = new Watcher<>() {
            @Override
            public void eventReceived(Action action, String resource) {
                KruizeLayer kruizeLayer = null;

                switch (action.toString().toUpperCase()) {
                    case "ADDED":
                        kruizeLayer = getAutotuneConfig(resource, KubernetesContexts.getAutotuneVariableContext());
                        if (kruizeLayer != null) {
                            autotuneConfigMap.put(kruizeLayer.getName(), kruizeLayer);
                            LOGGER.info("Added autotuneconfig " + kruizeLayer.getName());
                            addLayerInfo(kruizeLayer, null);
                        }
                        break;
                    case "MODIFIED":
                        kruizeLayer = getAutotuneConfig(resource, KubernetesContexts.getAutotuneVariableContext());
                        if (kruizeLayer != null) {
                            deleteExistingConfig(resource);
                            autotuneConfigMap.put(kruizeLayer.getName(), kruizeLayer);
                            LOGGER.info("Added modified autotuneconfig " + kruizeLayer.getName());
                            addLayerInfo(kruizeLayer, null);
                        }
                        break;
                    case "DELETED":
                        deleteExistingConfig(resource);
                    default:
                        break;
                }
            }

            @Override
            public void onClose(KubernetesClientException e) {
            }
        };

        KubernetesServices kubernetesServices = new KubernetesServicesImpl();
        kubernetesServices.addWatcher(KubernetesContexts.getAutotuneCrdContext(), autotuneObjectWatcher);
        kubernetesServices.addWatcher(KubernetesContexts.getAutotuneConfigContext(), autotuneConfigWatcher);
    }

    public static void processKruizeObject(KruizeObject kruizeObject) {
        try {
            if (null != kruizeObject) {
                List<KruizeObject> kruizeObjectList = new ArrayList<>();
                kruizeObjectList.add(kruizeObject);
                ExperimentInitiator experimentInitiator = new ExperimentInitiator();
                experimentInitiator.validateAndAddNewExperiments(autotuneObjectMap, kruizeObjectList);
                KruizeObject invalidKruizeObject = kruizeObjectList.stream().filter((ko) -> (!ko.getValidationData().isSuccess())).findAny().orElse(null);
                if (invalidKruizeObject != null) {
                    new KubeEventLogger(Clock.systemUTC()).log("Failed", invalidKruizeObject.getValidationData().getMessage(), EventLogger.Type.Warning, null, null, kruizeObject.getObjectReference(), null);
                } else {
                    LOGGER.debug(kruizeObject.getExperimentName() + " " + kruizeObject.getValidationData().getMessage());
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
        System.out.println("Autotune Object: " + kruizeObject.getExperimentName() + ": Finding Layers");

        matchPodsToAutotuneObject(kruizeObject);

        for (String autotuneConfig : autotuneConfigMap.keySet()) {
            addLayerInfo(autotuneConfigMap.get(autotuneConfig), kruizeObject);
        }
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
     * Delete existing autotuneconfig in applications monitored by autotune
     *
     * @param resource JSON string of the autotuneconfig object
     */
    private static void deleteExistingConfig(String resource) {
        JSONObject autotuneConfigJson = new JSONObject(resource);
        String configName = autotuneConfigJson.optString(AutotuneConfigConstants.LAYER_NAME);

        LOGGER.info("KruizeLayer " + configName + " removed from autotune monitoring");
        // Remove from collection of autotuneconfigs in map
        autotuneConfigMap.remove(configName);

        // Remove autotuneconfig for all applications monitored
        for (String autotuneObjectKey : deploymentMap.keySet()) {
            Map<String, ApplicationDeployment> depMap = deploymentMap.get(autotuneObjectKey);
            for (String deploymentName : depMap.keySet()) {
                for (String applicationServiceStackName : depMap.get(deploymentName).getApplicationServiceStackMap().keySet()) {
                    ApplicationServiceStack applicationServiceStack = depMap.get(deploymentName).getApplicationServiceStackMap().get(applicationServiceStackName);
                    applicationServiceStack.getApplicationServiceStackLayers().remove(configName);
                }
            }
        }
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
            String mode;
            String targetCluster;
            String clusterName;
            String namespace;
            SelectorInfo selectorInfo;
            JSONObject sloJson = null;
            String hpoAlgoImpl = null;

            JSONObject specJson = autotuneObjectJson.optJSONObject(AnalyzerConstants.AutotuneObjectConstants.SPEC);

            mode = specJson.optString(AnalyzerConstants.AutotuneObjectConstants.MODE,
                    AnalyzerConstants.AutotuneObjectConstants.DEFAULT_MODE);
            targetCluster = specJson.optString(AnalyzerConstants.AutotuneObjectConstants.TARGET_CLUSTER,
                    AnalyzerConstants.AutotuneObjectConstants.DEFAULT_TARGET_CLUSTER);
            clusterName = specJson.optString(AnalyzerConstants.AutotuneObjectConstants.CLUSTER_NAME);
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
                    }
                    else {
                        // check if the Performance profile with the given name exist
                        if (null == PerformanceProfilesDeployment.performanceProfilesMap.get(perfProfileName)) {
                            throw new SloClassNotSupportedException(AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_PERF_PROFILE + perfProfileName);
                        }
                    }
                } else {
                    if (sloJson.isEmpty()) {
                        throw new SloClassNotSupportedException(AnalyzerErrorConstants.AutotuneObjectErrors.MISSING_SLO_DATA);
                    }
                    else {
                        // Only SLO is available so create a default Performance profile with SLO data
                        SloInfo sloInfo = new Gson().fromJson(String.valueOf(sloJson), SloInfo.class);
                        perfProfileName = setDefaultPerformanceProfile(sloInfo, mode, targetCluster);
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
                    objectReference
            );

        } catch (InvalidValueException | NullPointerException | JSONException | SloClassNotSupportedException e) {
            LOGGER.error(e.getMessage());
            new KubeEventLogger(Clock.systemUTC()).log("Failed", e.getMessage(), EventLogger.Type.Warning, null, null,null, null);
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

            if ( null != performanceProfile) {
                ValidationOutputData validationOutputData = new PerfProfileImpl().validateAndAddProfile(PerformanceProfilesDeployment.performanceProfilesMap, performanceProfile);
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
            LOGGER.debug("Exception while adding PP with message: {} ",e.getMessage());
            new KubeEventLogger(Clock.systemUTC()).log("Failed", e.getMessage(), EventLogger.Type.Warning, null, null, null, null);
            return null;
        }
        return performanceProfile.getName();
    }

    /**
     * Parse KruizeLayer JSON and create matching KruizeLayer object
     *
     * @param autotuneConfigResource  The JSON file for the autotuneconfig resource in the cluster.
     * @param autotuneVariableContext
     */
    @SuppressWarnings("unchecked")
    private static KruizeLayer getAutotuneConfig(String autotuneConfigResource, CustomResourceDefinitionContext autotuneVariableContext) {
        KubernetesServices kubernetesServices = null;
        try {
            kubernetesServices = new KubernetesServicesImpl();
            JSONObject autotuneConfigJson = new JSONObject(autotuneConfigResource);
            JSONObject metadataJson = autotuneConfigJson.getJSONObject(AnalyzerConstants.AutotuneObjectConstants.METADATA);
            JSONObject presenceJson = autotuneConfigJson.optJSONObject(AutotuneConfigConstants.LAYER_PRESENCE);

            String presence = null;
            JSONArray layerPresenceQueryJson = null;
            JSONArray layerPresenceLabelJson = null;
            if (presenceJson != null) {
                presence = presenceJson.optString(AnalyzerConstants.AutotuneConfigConstants.PRESENCE);
                layerPresenceQueryJson = presenceJson.optJSONArray(AnalyzerConstants.AutotuneConfigConstants.QUERIES);
                layerPresenceLabelJson = presenceJson.optJSONArray(AnalyzerConstants.AutotuneConfigConstants.LABEL);
            }

            String name = autotuneConfigJson.getJSONObject(AutotuneConfigConstants.METADATA).optString(AutotuneConfigConstants.NAME);
            String namespace = autotuneConfigJson.getJSONObject(AutotuneConfigConstants.METADATA).optString(AutotuneConfigConstants.NAMESPACE);

            // Get the autotunequeryvariables for the current kubernetes environment
            ArrayList<Map<String, String>> queryVarList = null;
            try {
                Map<String, Object> envVariblesMap = kubernetesServices.getCRDEnvMap(autotuneVariableContext, namespace, KruizeDeploymentInfo.getKubernetesType());
                queryVarList = (ArrayList<Map<String, String>>) envVariblesMap.get(AnalyzerConstants.AutotuneConfigConstants.QUERY_VARIABLES);
            } catch (Exception e) {
                LOGGER.error("Autotunequeryvariable and autotuneconfig {} not in the same namespace", name);
                return null;
            }

            String layerPresenceQueryStr = null;
            String layerPresenceKey = null;

            ArrayList<LayerPresenceQuery> layerPresenceQueries = new ArrayList<>();
            if (layerPresenceQueryJson != null) {
                for (Object query : layerPresenceQueryJson) {
                    JSONObject queryJson = (JSONObject) query;
                    String datasource = queryJson.getString(AnalyzerConstants.AutotuneConfigConstants.DATASOURCE);
                    if (datasource.equalsIgnoreCase(KruizeDeploymentInfo.getMonitoringAgent())) {
                        layerPresenceQueryStr = queryJson.getString(AnalyzerConstants.AutotuneConfigConstants.QUERY);
                        layerPresenceKey = queryJson.getString(AnalyzerConstants.AutotuneConfigConstants.KEY);
                        // Replace the queryvariables in the query
                        try {
                            layerPresenceQueryStr = Variables.updateQueryWithVariables(null, null,
                                    layerPresenceQueryStr, queryVarList);
                            LayerPresenceQuery layerPresenceQuery = new LayerPresenceQuery(datasource, layerPresenceQueryStr, layerPresenceKey);
                            layerPresenceQueries.add(layerPresenceQuery);
                        } catch (IOException | MonitoringAgentNotSupportedException e) {
                            LOGGER.error("autotuneconfig {}: Unsupported Datasource: {}", name, datasource);
                            return null;
                        }
                    }
                }
            }

            String layerPresenceLabel = null;
            String layerPresenceLabelValue = null;
            if (layerPresenceLabelJson != null) {
                for (Object label : layerPresenceLabelJson) {
                    JSONObject labelJson = (JSONObject) label;
                    layerPresenceLabel = labelJson.optString(AutotuneConfigConstants.NAME);
                    layerPresenceLabelValue = labelJson.optString(AutotuneConfigConstants.VALUE);
                }
            }

            String layerName = autotuneConfigJson.optString(AnalyzerConstants.AutotuneConfigConstants.LAYER_NAME);
            String details = autotuneConfigJson.optString(AnalyzerConstants.AutotuneConfigConstants.DETAILS);
            int level = autotuneConfigJson.optInt(AnalyzerConstants.AutotuneConfigConstants.LAYER_LEVEL);
            JSONArray tunablesJsonArray = autotuneConfigJson.optJSONArray(AnalyzerConstants.AutotuneConfigConstants.TUNABLES);
            ArrayList<Tunable> tunableArrayList = new ArrayList<>();

            for (Object tunablesObject : tunablesJsonArray) {
                JSONObject tunableJson = (JSONObject) tunablesObject;
                JSONArray tunableQueriesArray = tunableJson.optJSONArray(AnalyzerConstants.AutotuneConfigConstants.QUERIES);

                // Store the datasource and query from the JSON in a map
                Map<String, String> queriesMap = new HashMap<>();
                if (tunableQueriesArray != null) {
                    for (Object tunableQuery : tunableQueriesArray) {
                        JSONObject tunableQueryObj = (JSONObject) tunableQuery;
                        String datasource = tunableQueryObj.optString(AnalyzerConstants.AutotuneConfigConstants.DATASOURCE);
                        String datasourceQuery = tunableQueryObj.optString(AnalyzerConstants.AutotuneConfigConstants.QUERY);
                        queriesMap.put(datasource, datasourceQuery);
                    }
                }

                String tunableName = tunableJson.optString(AnalyzerConstants.AutotuneConfigConstants.NAME);
                String tunableValueType = tunableJson.optString(AnalyzerConstants.AutotuneConfigConstants.VALUE_TYPE);

                ArrayList<String> sloClassList = new ArrayList<>();
                JSONArray sloClassJson = tunableJson.getJSONArray(AnalyzerConstants.AutotuneConfigConstants.SLO_CLASS);

                for (Object sloClassObject : sloClassJson) {
                    String sloClass = (String) sloClassObject;
                    sloClassList.add(sloClass);
                }
                String upperBound = "";
                String lowerBound = "";
                double step = 1;
                List<String> choices = new ArrayList<>();
                Tunable tunable;
                try {
                    /**
                     * check the tunable type, if it's categorical then we need to add the choices
                     * and then invoke the corresponding constructor
                     */
                    if (tunableValueType.equalsIgnoreCase("categorical")) {
                        JSONArray categoricalChoicesJson = tunableJson.getJSONArray(AnalyzerConstants.AutotuneConfigConstants.TUNABLE_CHOICES);
                        for (Object categoricalChoiceObject : categoricalChoicesJson) {
                            String categoricalChoice = (String) categoricalChoiceObject;
                            choices.add(categoricalChoice);
                        }
                        tunable = new Tunable(tunableName, tunableValueType, queriesMap, sloClassList, layerName, choices);
                    } else {
                        upperBound = tunableJson.optString(AnalyzerConstants.AutotuneConfigConstants.UPPER_BOUND);
                        lowerBound = tunableJson.optString(AnalyzerConstants.AutotuneConfigConstants.LOWER_BOUND);
                        // Read in step from the tunable, set it to '1' if not specified.
                        step = tunableJson.optDouble(AutotuneConfigConstants.STEP, 1);
                        tunable = new Tunable(tunableName, tunableValueType, queriesMap, sloClassList, layerName, step, upperBound, lowerBound);
                    }
                    tunableArrayList.add(tunable);
                } catch (InvalidBoundsException e) {
                    e.printStackTrace();
                }
            }

            String resourceVersion = metadataJson.optString(AnalyzerConstants.RESOURCE_VERSION);
            String uid = metadataJson.optString(AnalyzerConstants.UID);
            String apiVersion = autotuneConfigJson.optString(AnalyzerConstants.API_VERSION);
            String kind = autotuneConfigJson.optString(AnalyzerConstants.KIND);

            ObjectReference objectReference = new ObjectReference(apiVersion,
                    null,
                    kind,
                    name,
                    namespace,
                    resourceVersion,
                    uid);

            return new KruizeLayer(name,
                    layerName,
                    level,
                    details,
                    presence,
                    layerPresenceQueries,
                    layerPresenceLabel,
                    layerPresenceLabelValue,
                    tunableArrayList,
                    objectReference);
        } catch (JSONException | InvalidValueException | NullPointerException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * This method adds the default (container) layer to all the monitored applications in the cluster
     * If the autotuneObject is not null, then it adds the default layer only to stacks associated with that object.
     *
     * @param layer
     * @param kruizeObject
     */
    private static void addDefaultLayer(KruizeLayer layer, KruizeObject kruizeObject) {
        String presence = layer.getPresence();
        // Add to all monitored applications in the cluster
        if (presence.equals(AnalyzerConstants.PRESENCE_ALWAYS)) {
            if (kruizeObject == null) {
                for (String autotuneObjectKey : deploymentMap.keySet()) {
                    Map<String, ApplicationDeployment> depMap = deploymentMap.get(autotuneObjectKey);
                    for (String deploymentName : depMap.keySet()) {
                        for (String containerImageName : depMap.get(deploymentName).getApplicationServiceStackMap().keySet()) {
                            ApplicationServiceStack applicationServiceStack = depMap.get(deploymentName).getApplicationServiceStackMap().get(containerImageName);
                            addLayerInfoToApplication(applicationServiceStack, layer);
                        }
                    }
                }
            } else {
                Map<String, ApplicationDeployment> depMap = deploymentMap.get(kruizeObject.getExperimentName());
                for (String deploymentName : depMap.keySet()) {
                    for (String containerImageName : depMap.get(deploymentName).getApplicationServiceStackMap().keySet()) {
                        ApplicationServiceStack applicationServiceStack = depMap.get(deploymentName).getApplicationServiceStackMap().get(containerImageName);
                        addLayerInfoToApplication(applicationServiceStack, layer);
                    }
                }
            }
        }
    }

    /**
     * Check if a layer has a datasource query that validates its presence
     *
     * @param layer
     * @param kruizeObject
     */
    private static void addQueryLayer(KruizeLayer layer, KruizeObject kruizeObject) {
        KubernetesServices kubernetesServices = null;
        try {
            // TODO: This query needs to be optimized to only check for pods in the right namespace
            kubernetesServices = new KubernetesServicesImpl();
            List<Pod> podList = null;
            if (kruizeObject != null) {
                podList = kubernetesServices.getPodsBy(kruizeObject.getNamespace());
            } else {
                podList = kubernetesServices.getPodsBy(null);
            }
            if (podList == null) {
                LOGGER.warn(AnalyzerErrorConstants.AutotuneConfigErrors.COULD_NOT_GET_LIST_OF_APPLICATIONS + layer.getName());
                return;
            }
            DataSource autotuneDataSource = null;
            try {
                autotuneDataSource = DataSourceFactory.getDataSource(KruizeDeploymentInfo.getMonitoringAgent());
            } catch (MonitoringAgentNotFoundException e) {
                e.printStackTrace();
            }
            ArrayList<String> appsForAllQueries = new ArrayList<>();
            ArrayList<LayerPresenceQuery> layerPresenceQueries = layer.getLayerPresenceQueries();
            // Check if a layer has a datasource query that validates its presence
            if (layerPresenceQueries != null && !layerPresenceQueries.isEmpty()) {
                for (LayerPresenceQuery layerPresenceQuery : layerPresenceQueries) {
                    try {
                        // TODO: Check the datasource in the query is the same as the Autotune one
                        ArrayList<String> apps = (ArrayList<String>) autotuneDataSource.getAppsForLayer(layerPresenceQuery.getLayerPresenceQuery(),
                                layerPresenceQuery.getLayerPresenceKey());
                        appsForAllQueries.addAll(apps);
                    } catch (MalformedURLException | NullPointerException e) {
                        LOGGER.warn(AnalyzerErrorConstants.AutotuneConfigErrors.COULD_NOT_GET_LIST_OF_APPLICATIONS + layer.getName());
                    }
                }
                // We now have a list of apps that have the label and the key specified by the user.
                // We now have to find the kubernetes objects corresponding to these apps
                if (!appsForAllQueries.isEmpty()) {
                    for (String application : appsForAllQueries) {
                        List<Container> containers = null;
                        for (Pod pod : podList) {
                            if (pod.getMetadata().getName().contains(application)) {
                                // We found a POD that matches the app name, now get its containers
                                containers = pod.getSpec().getContainers();
                                break;
                            }
                        }
                        // No containers were found that matched the applications, this is weird, log a warning
                        if (containers == null) {
                            LOGGER.warn("Could not find any PODs related to Application name: " + application);
                            continue;
                        }
                        for (Container container : containers) {
                            String containerImageName = container.getImage();
                            // Check if the container image is already present in the applicationServiceStackMap, if not, add it
                            if (kruizeObject != null) {
                                Map<String, ApplicationDeployment> depMap = deploymentMap.get(kruizeObject.getExperimentName());
                                for (String deploymentName : depMap.keySet()) {
                                    if (depMap.get(deploymentName).getApplicationServiceStackMap().containsKey(containerImageName)) {
                                        addLayerInfoToApplication(depMap.get(deploymentName).getApplicationServiceStackMap().get(containerImageName), layer);
                                    }
                                }
                            } else {
                                for (String autotuneObjectKey : deploymentMap.keySet()) {
                                    Map<String, ApplicationDeployment> depMap = deploymentMap.get(autotuneObjectKey);
                                    for (String deploymentName : depMap.keySet()) {
                                        if (depMap.get(deploymentName).getApplicationServiceStackMap().containsKey(containerImageName)) {
                                            addLayerInfoToApplication(depMap.get(deploymentName).getApplicationServiceStackMap().get(containerImageName), layer);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LOGGER.warn(AnalyzerErrorConstants.AutotuneConfigErrors.COULD_NOT_GET_LIST_OF_APPLICATIONS + layer.getName());
                }
            } else {
                LOGGER.warn(AnalyzerErrorConstants.AutotuneConfigErrors.COULD_NOT_GET_LIST_OF_APPLICATIONS + layer.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (kubernetesServices != null) {
                kubernetesServices.shutdownClient();
            }
        }
    }

    /**
     * Attach a newly added to the relevant stacks
     * If the autotuneObject is null, try to find all the relevant stacks
     * under observation currently and add the new layer.
     *
     * @param layer
     * @param kruizeObject
     */
    public static void addLayerInfo(KruizeLayer layer, KruizeObject kruizeObject) {
        // Add the default layer for all monitored pods
        addDefaultLayer(layer, kruizeObject);

        // Match layer presence queries if any
        addQueryLayer(layer, kruizeObject);
        KubernetesServices kubernetesServices = null;
        try {
            kubernetesServices = new KubernetesServicesImpl();
            String layerPresenceLabel = layer.getLayerPresenceLabel();
            String layerPresenceLabelValue = layer.getLayerPresenceLabelValue();
            if (layerPresenceLabel != null) {
                List<Pod> podList = null;
                if (kruizeObject != null) {
                    podList = kubernetesServices.getPodsBy(kruizeObject.getNamespace(), layerPresenceLabel, layerPresenceLabelValue);
                } else {
                    podList = kubernetesServices.getPodsBy(null, layerPresenceLabel, layerPresenceLabelValue);
                }

                if (podList.isEmpty()) {
                    LOGGER.warn(AnalyzerErrorConstants.AutotuneConfigErrors.COULD_NOT_GET_LIST_OF_APPLICATIONS + layer.getName());
                    return;
                }
                for (Pod pod : podList) {
                    for (Container container : pod.getSpec().getContainers()) {
                        String containerImageName = container.getImage();
                        if (kruizeObject != null) {
                            Map<String, ApplicationDeployment> depMap = deploymentMap.get(kruizeObject.getExperimentName());
                            for (String deploymentName : depMap.keySet()) {
                                if (depMap.get(deploymentName).getApplicationServiceStackMap().containsKey(containerImageName)) {
                                    addLayerInfoToApplication(depMap.get(deploymentName).getApplicationServiceStackMap().get(containerImageName), layer);
                                }
                            }
                        } else {
                            for (String autotuneObjectKey : deploymentMap.keySet()) {
                                Map<String, ApplicationDeployment> depMap = deploymentMap.get(autotuneObjectKey);
                                for (String deploymentName : depMap.keySet()) {
                                    if (depMap.get(deploymentName).getApplicationServiceStackMap().containsKey(containerImageName)) {
                                        addLayerInfoToApplication(depMap.get(deploymentName).getApplicationServiceStackMap().get(containerImageName), layer);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (kubernetesServices != null) {
                kubernetesServices.shutdownClient();
            }
        }
    }

    /**
     * Add layer, queries and tunables info to the autotuneObject
     *
     * @param applicationServiceStack ApplicationServiceStack instance that contains the layer
     * @param kruizeLayer          KruizeLayer object for the layer
     */
    private static void addLayerInfoToApplication(ApplicationServiceStack applicationServiceStack, KruizeLayer kruizeLayer) {
        // Check if layer already exists
        if (!applicationServiceStack.getApplicationServiceStackLayers().isEmpty() &&
                applicationServiceStack.getApplicationServiceStackLayers().containsKey(kruizeLayer.getName())) {
            return;
        }

        ArrayList<Tunable> tunables = new ArrayList<>();
        for (Tunable tunable : kruizeLayer.getTunables()) {
            try {
                Map<String, String> queries = new HashMap<>(tunable.getQueries());

                Tunable tunableCopy;
                if (tunable.getValueType().equalsIgnoreCase("categorical")) {
                    tunableCopy = new Tunable(tunable.getName(),
                            tunable.getValueType(),
                            queries,
                            tunable.getSloClassList(),
                            tunable.getLayerName(),
                            tunable.getChoices()
                    );
                } else {
                    tunableCopy = new Tunable(tunable.getName(),
                            tunable.getValueType(),
                            queries,
                            tunable.getSloClassList(),
                            tunable.getLayerName(),
                            tunable.getStep(),
                            tunable.getUpperBound(),
                            tunable.getLowerBound()
                    );
                }
                tunables.add(tunableCopy);
            } catch (InvalidBoundsException ignored) {
            }
        }

        // Create autotuneconfigcopy with updated tunables arraylist
        KruizeLayer kruizeLayerCopy = null;
        try {
            kruizeLayerCopy = new KruizeLayer(
                    kruizeLayer.getName(),
                    kruizeLayer.getLayerName(),
                    kruizeLayer.getLevel(),
                    kruizeLayer.getDetails(),
                    kruizeLayer.getPresence(),
                    kruizeLayer.getLayerPresenceQueries(),
                    kruizeLayer.getLayerPresenceLabel(),
                    kruizeLayer.getLayerPresenceLabelValue(),
                    tunables,
                    kruizeLayer.getObjectReference());
        } catch (InvalidValueException ignored) {
        }

        LOGGER.info("Added layer " + kruizeLayer.getName() + " to stack " + applicationServiceStack.getStackName());
        applicationServiceStack.getApplicationServiceStackLayers().put(kruizeLayer.getName(), kruizeLayerCopy);
    }
}
