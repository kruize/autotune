/*******************************************************************************
 * Copyright (c) 2024 Red Hat, IBM Corporation and others.
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

package com.autotune.analyzer.recommendations.autoscaler.vpa;

import com.autotune.analyzer.exceptions.ApplyRecommendationsError;
import com.autotune.analyzer.exceptions.UnableToCreateVPAException;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.autoscaler.RecommendationUpdaterImpl;
import com.autotune.analyzer.recommendations.utils.RecommendationUtils;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.common.data.result.ContainerData;
import com.autotune.analyzer.recommendations.objects.MappedRecommendationForTimestamp;
import com.autotune.analyzer.recommendations.objects.TermRecommendations;
import io.fabric8.autoscaling.api.model.v1.*;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionList;
import io.fabric8.kubernetes.api.model.autoscaling.v1.CrossVersionObjectReferenceBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ApiextensionsAPIGroupDSL;
import io.fabric8.verticalpodautoscaler.client.DefaultVerticalPodAutoscalerClient;
import io.fabric8.verticalpodautoscaler.client.NamespacedVerticalPodAutoscalerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VpaUpdaterImpl extends RecommendationUpdaterImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(VpaUpdaterImpl.class);
    private static VpaUpdaterImpl vpaUpdater;

    private KubernetesClient kubernetesClient;
    private ApiextensionsAPIGroupDSL apiextensionsClient;


    private VpaUpdaterImpl() {
        this.kubernetesClient = new DefaultKubernetesClient();
        this.apiextensionsClient = kubernetesClient.apiextensions();
    }

    public static VpaUpdaterImpl getInstance() {
        if (null != vpaUpdater) {
            return vpaUpdater;
        }

        synchronized (VpaUpdaterImpl.class) {
            if (null == vpaUpdater) {
                vpaUpdater = new VpaUpdaterImpl();
            }
        }

        return vpaUpdater;
    }

    /**
     * Checks whether the necessary updater dependencies are installed or available in the system.
     * @return boolean true if the required updaters are installed, false otherwise.
     */
    @Override
    public boolean isUpdaterInstalled() {
        try {
            LOGGER.debug(AnalyzerConstants.RecommendationUpdaterConstants.InfoMsgs.CHECKING_IF_UPDATER_INSTALLED,
                    AnalyzerConstants.RecommendationUpdaterConstants.SupportedUpdaters.VPA);
            // checking if VPA CRD is present or not
            boolean isVpaInstalled = false;
            CustomResourceDefinitionList crdList = apiextensionsClient.v1().customResourceDefinitions().list();
            if (null != crdList && null != crdList.getItems() && !crdList.getItems().isEmpty()) {
                isVpaInstalled = crdList.getItems().stream().anyMatch(crd -> AnalyzerConstants.RecommendationUpdaterConstants.VPA.VPA_PLURAL.equalsIgnoreCase(crd.getSpec().getNames().getKind()));
            }
            if (isVpaInstalled) {
                LOGGER.debug(AnalyzerConstants.RecommendationUpdaterConstants.InfoMsgs.FOUND_UPDATER_INSTALLED, AnalyzerConstants.RecommendationUpdaterConstants.SupportedUpdaters.VPA);
            } else {
                LOGGER.error(AnalyzerErrorConstants.RecommendationUpdaterErrors.UPDATER_NOT_INSTALLED);
            }
            return isVpaInstalled;
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a Vertical Pod Autoscaler (VPA) object with the specified name is present.
     *
     * @param vpaName String containing the name of the VPA object to search for
     * @return true if the VPA object with the specified name is present, false otherwise
     */
    private boolean checkIfVpaIsPresent(String vpaName) {
        try {
            if (null == vpaName || vpaName.isEmpty()) {
                throw new Exception(AnalyzerErrorConstants.RecommendationUpdaterErrors.INVALID_VPA_NAME);
            } else {
                LOGGER.debug(String.format(AnalyzerConstants.RecommendationUpdaterConstants.InfoMsgs.CHECKING_IF_VPA_PRESENT, vpaName));
                NamespacedVerticalPodAutoscalerClient client = new DefaultVerticalPodAutoscalerClient();
                VerticalPodAutoscalerList vpas = client.v1().verticalpodautoscalers().inAnyNamespace().list();

                if (null != vpas && null != vpas.getItems() && !vpas.getItems().isEmpty()) {
                    // TODO:// later we can also check here is the recommender is Kruize to confirm
                    for (VerticalPodAutoscaler vpa : vpas.getItems()) {
                        if (vpaName.equals(vpa.getMetadata().getName())) {
                            LOGGER.debug(String.format(AnalyzerConstants.RecommendationUpdaterConstants.InfoMsgs.VPA_WITH_NAME_FOUND, vpaName));
                            return true;
                        }
                    }
                }
                LOGGER.error(String.format(AnalyzerConstants.RecommendationUpdaterConstants.InfoMsgs.VPA_WITH_NAME_NOT_FOUND, vpaName));
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("Error while checking VPA presence: " + e.getMessage(), e);
            return false;
        }
    }


    /**
     * Returns the VPA Object if present with the name
     *
     * @param vpaName String containing the name of the VPA object to search for
     * @return VerticalPodAutoscaler if the VPA object with the specified name is present, null otherwise
     */
    private VerticalPodAutoscaler getVpaIsPresent(String vpaName) {
        try {
            if (null == vpaName || vpaName.isEmpty()) {
                throw new Exception(AnalyzerErrorConstants.RecommendationUpdaterErrors.INVALID_VPA_NAME);
            } else {
                LOGGER.debug(String.format(AnalyzerConstants.RecommendationUpdaterConstants.InfoMsgs.CHECKING_IF_VPA_PRESENT, vpaName));
                NamespacedVerticalPodAutoscalerClient client = new DefaultVerticalPodAutoscalerClient();
                VerticalPodAutoscalerList vpas = client.v1().verticalpodautoscalers().inAnyNamespace().list();

                if (null != vpas && null != vpas.getItems() && !vpas.getItems().isEmpty()) {
                    for (VerticalPodAutoscaler vpa : vpas.getItems()) {
                        if (vpaName.equals(vpa.getMetadata().getName())) {
                            LOGGER.debug(String.format(AnalyzerConstants.RecommendationUpdaterConstants.InfoMsgs.VPA_WITH_NAME_FOUND, vpaName));
                            return vpa;
                        }
                    }
                }
                LOGGER.error(String.format(AnalyzerConstants.RecommendationUpdaterConstants.InfoMsgs.VPA_WITH_NAME_NOT_FOUND, vpaName));
                return null;
            }
        } catch (Exception e) {
            LOGGER.error("Error while checking VPA presence: " + e.getMessage(), e);
            return null;
        }
    }


    /**
     * Applies the resource recommendations contained within the provided KruizeObject
     * This method will take the KruizeObject, which contains the resource recommendations,
     * and apply them to the desired resources.
     *
     * @param kruizeObject KruizeObject containing the resource recommendations to be applied.
     * @throws ApplyRecommendationsError in case of any error.
     */
    @Override
    public void applyResourceRecommendationsForExperiment(KruizeObject kruizeObject) throws ApplyRecommendationsError {
        try {
            // checking if VPA is installed or not
            if (!isUpdaterInstalled()) {
                LOGGER.error(AnalyzerErrorConstants.RecommendationUpdaterErrors.UPDATER_NOT_INSTALLED);
            } else {
                String expName = kruizeObject.getExperimentName();
                boolean vpaPresent = checkIfVpaIsPresent(expName);

                // create VPA Object is not present
                if (!vpaPresent) {
                    createVpaObject(kruizeObject);
                }

                for (K8sObject k8sObject: kruizeObject.getKubernetes_objects()) {
                    List<RecommendedContainerResources> containerRecommendations = convertRecommendationsToContainerPolicy(k8sObject.getContainerDataMap());
                    if (containerRecommendations.isEmpty()){
                        LOGGER.error(AnalyzerErrorConstants.RecommendationUpdaterErrors.RECOMMENDATION_DATA_NOT_PRESENT);
                    } else {
                        RecommendedPodResources recommendedPodResources = new RecommendedPodResources();
                        recommendedPodResources.setContainerRecommendations(containerRecommendations);
                        VerticalPodAutoscalerStatus vpaObjectStatus = new VerticalPodAutoscalerStatusBuilder()
                                .withRecommendation(recommendedPodResources)
                                .build();

                        // patching existing VPA Object
                        if (vpaObjectStatus != null) {
                            VerticalPodAutoscaler vpaObject = getVpaIsPresent(expName);
                            vpaObject.setStatus(vpaObjectStatus);

                            NamespacedVerticalPodAutoscalerClient client = new DefaultVerticalPodAutoscalerClient();
                            client.v1().verticalpodautoscalers()
                                    .inNamespace(vpaObject
                                            .getMetadata()
                                            .getNamespace())
                                    .withName(vpaObject
                                            .getMetadata()
                                            .getName())
                                    .patchStatus(vpaObject);

                            LOGGER.debug(String.format(AnalyzerConstants.RecommendationUpdaterConstants.InfoMsgs.VPA_PATCHED,
                                    vpaObject.getMetadata().getName()));
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new ApplyRecommendationsError(e.getMessage());
        }
    }

    /**
     * This function converts container recommendations for VPA Container Recommendations Object Format
     */
    private List<RecommendedContainerResources>  convertRecommendationsToContainerPolicy(HashMap<String, ContainerData> containerDataMap) {
        List<RecommendedContainerResources> containerRecommendations = new ArrayList<>();

        for (Map.Entry<String, ContainerData> containerDataEntry : containerDataMap.entrySet()) {
            // fetching container data
            ContainerData containerData = containerDataEntry.getValue();
            String containerName = containerData.getContainer_name();
            HashMap<Timestamp, MappedRecommendationForTimestamp> recommendationData = containerData.getContainerRecommendations().getData();

            // checking if recommendation data is present
            if (null == recommendationData) {
                LOGGER.error(AnalyzerErrorConstants.RecommendationUpdaterErrors.RECOMMENDATION_DATA_NOT_PRESENT);
            } else {
                for (MappedRecommendationForTimestamp value : recommendationData.values()) {
                    /*
                     * The short-term performance recommendations is currently the default for VPA and is hardcoded.
                     * TODO:// Implement functionality to choose the desired term and model
                     **/
                    TermRecommendations termRecommendations = value.getShortTermRecommendations();
                    HashMap<AnalyzerConstants.ResourceSetting,
                            HashMap<AnalyzerConstants.RecommendationItem,
                                    RecommendationConfigItem>> recommendationsConfig = termRecommendations.getPerformanceRecommendations().getConfig();

                    Double cpuRecommendationValue = recommendationsConfig.get(AnalyzerConstants.ResourceSetting.requests).get(AnalyzerConstants.RecommendationItem.CPU).getAmount();
                    Double memoryRecommendationValue = recommendationsConfig.get(AnalyzerConstants.ResourceSetting.requests).get(AnalyzerConstants.RecommendationItem.MEMORY).getAmount();

                    LOGGER.debug(String.format(AnalyzerConstants.RecommendationUpdaterConstants.InfoMsgs.RECOMMENDATION_VALUE,
                            AnalyzerConstants.RecommendationItem.CPU, containerName, cpuRecommendationValue));
                    LOGGER.debug(String.format(AnalyzerConstants.RecommendationUpdaterConstants.InfoMsgs.RECOMMENDATION_VALUE,
                            AnalyzerConstants.RecommendationItem.MEMORY, containerName, memoryRecommendationValue));

                    String cpuRecommendationValueForVpa = RecommendationUtils.resource2str(AnalyzerConstants.RecommendationItem.CPU.toString(),
                            cpuRecommendationValue);
                    String memoryRecommendationValueForVpa = RecommendationUtils.resource2str(AnalyzerConstants.RecommendationItem.MEMORY.toString(),
                            memoryRecommendationValue);

                    // creating container resource vpa object
                    RecommendedContainerResources recommendedContainerResources = new RecommendedContainerResources();
                    recommendedContainerResources.setContainerName(containerName);

                    // setting target values
                    Map<String, Quantity> target = new HashMap<>();
                    target.put(AnalyzerConstants.RecommendationItem.CPU.toString(), new Quantity(cpuRecommendationValueForVpa));
                    target.put(AnalyzerConstants.RecommendationItem.MEMORY.toString(), new Quantity(memoryRecommendationValueForVpa));

                    // setting lower bound values
                    Map<String, Quantity> lowerBound = new HashMap<>();
                    lowerBound.put(AnalyzerConstants.RecommendationItem.CPU.toString(), new Quantity(cpuRecommendationValueForVpa));
                    lowerBound.put(AnalyzerConstants.RecommendationItem.MEMORY.toString(), new Quantity(memoryRecommendationValueForVpa));

                    // setting upper bound values
                    Map<String, Quantity> upperBound = new HashMap<>();
                    upperBound.put(AnalyzerConstants.RecommendationItem.CPU.toString(), new Quantity(cpuRecommendationValueForVpa));
                    upperBound.put(AnalyzerConstants.RecommendationItem.MEMORY.toString(), new Quantity(memoryRecommendationValueForVpa));

                    recommendedContainerResources.setLowerBound(lowerBound);
                    recommendedContainerResources.setTarget(target);
                    recommendedContainerResources.setUpperBound(upperBound);

                    containerRecommendations.add(recommendedContainerResources);
                }
            }
        }
        return containerRecommendations;
    }

    /*
     * Creates a Vertical Pod Autoscaler (VPA) object in the specified namespace
     * for the given deployment and containers.
     */
    public void createVpaObject(KruizeObject kruizeObject) throws UnableToCreateVPAException {
        try {
            // checks if updater is installed or not
            if (isUpdaterInstalled()) {
                LOGGER.debug(String.format(AnalyzerConstants.RecommendationUpdaterConstants.InfoMsgs.CREATEING_VPA, kruizeObject.getExperimentName()));

                // updating recommender to Kruize for VPA Object
                Map<String, Object> additionalVpaObjectProps = getAdditionalVpaObjectProps();

                // updating controlled resources
                List<String> controlledResources = new ArrayList<>();
                controlledResources.add(AnalyzerConstants.RecommendationItem.CPU.toString());
                controlledResources.add(AnalyzerConstants.RecommendationItem.MEMORY.toString());

                // updating container policies
                for (K8sObject k8sObject: kruizeObject.getKubernetes_objects()) {
                    List<String> containers = new ArrayList<>(k8sObject.getContainerDataMap().keySet());
                    List<ContainerResourcePolicy> containerPolicies = new ArrayList<>();
                    for (String containerName : containers) {
                        ContainerResourcePolicy policy = new ContainerResourcePolicyBuilder()
                                .withContainerName(containerName)
                                .withControlledResources(controlledResources)
                                .build();
                        containerPolicies.add(policy);
                    }

                    PodResourcePolicy podPolicy = new PodResourcePolicyBuilder()
                            .withContainerPolicies(containerPolicies)
                            .build();

                    VerticalPodAutoscaler vpa = new VerticalPodAutoscalerBuilder()
                            .withApiVersion(AnalyzerConstants.RecommendationUpdaterConstants.VPA.VPA_API_VERSION)
                            .withKind(AnalyzerConstants.RecommendationUpdaterConstants.VPA.VPA_PLURAL)
                            .withMetadata(new ObjectMeta() {{
                                setName(kruizeObject.getExperimentName());
                            }})
                            .withSpec(new VerticalPodAutoscalerSpecBuilder()
                                    .withTargetRef(new CrossVersionObjectReferenceBuilder()
                                            .withApiVersion(AnalyzerConstants.RecommendationUpdaterConstants.VPA.VPA_TARGET_REF_API_VERSION)
                                            .withKind(AnalyzerConstants.RecommendationUpdaterConstants.VPA.VPA_TARGET_REF_KIND)
                                            .withName(k8sObject.getName())
                                            .build())
                                    .withResourcePolicy(podPolicy)
                                    .withAdditionalProperties(additionalVpaObjectProps)
                                    .build())
                            .build();

                    kubernetesClient.resource(vpa).inNamespace(k8sObject.getNamespace()).createOrReplace();
                    LOGGER.debug(String.format(AnalyzerConstants.RecommendationUpdaterConstants.InfoMsgs.CREATED_VPA, kruizeObject.getExperimentName()));
                }

            } else {
                throw new UnableToCreateVPAException(AnalyzerErrorConstants.RecommendationUpdaterErrors.UPDATER_NOT_INSTALLED);
            }
        } catch (Exception e) {
            throw new UnableToCreateVPAException(e.getMessage());
        }
    }

    /*
     * Prepare Object Map with addiional properties required for VPA Object
     * such as - Recommender Name
     */
    private static Map<String, Object> getAdditionalVpaObjectProps() {
        Map<String, Object> additionalVpaObjectProps = new HashMap<>();
        List<Map<String, Object>> recommenders = new ArrayList<>();
        Map<String, Object> recommender = new HashMap<>();
        recommender.put(AnalyzerConstants.RecommendationUpdaterConstants.VPA.RECOMMENDER_KEY,
                AnalyzerConstants.RecommendationUpdaterConstants.VPA.RECOMMENDER_NAME);
        recommenders.add(recommender);
        additionalVpaObjectProps.put(AnalyzerConstants.RecommendationUpdaterConstants.VPA.RECOMMENDERS, recommenders);
        return additionalVpaObjectProps;
    }
}
