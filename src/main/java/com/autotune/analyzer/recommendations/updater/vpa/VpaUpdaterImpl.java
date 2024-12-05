package com.autotune.analyzer.recommendations.updater.vpa;

import com.autotune.analyzer.exceptions.FetchMetricsError;
import com.autotune.analyzer.exceptions.VpaObjectCreateError;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.recommendations.ContainerRecommendations;
import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.engine.RecommendationEngine;
import com.autotune.analyzer.recommendations.objects.MappedRecommendationForTimestamp;
import com.autotune.analyzer.recommendations.objects.TermRecommendations;
import com.autotune.analyzer.recommendations.updater.RecommendationUpdaterImpl;
import com.autotune.analyzer.serviceObjects.ContainerAPIObject;
import com.autotune.analyzer.serviceObjects.KubernetesAPIObject;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.utils.KruizeConstants;
import com.autotune.utils.Utils;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionList;
import io.fabric8.kubernetes.api.model.autoscaling.v1.CrossVersionObjectReferenceBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ApiextensionsAPIGroupDSL;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.verticalpodautoscaler.client.DefaultVerticalPodAutoscalerClient;
import io.fabric8.verticalpodautoscaler.client.NamespacedVerticalPodAutoscalerClient;
import io.fabric8.verticalpodautoscaler.client.dsl.V1APIGroupDSL;
import io.fabric8.verticalpodautoscaler.api.model.v1.*;
import io.fabric8.verticalpodautoscaler.client.VerticalPodAutoscalerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VpaUpdaterImpl extends RecommendationUpdaterImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(VpaUpdaterImpl.class);
    private static VpaUpdaterImpl vpaUpdater = new VpaUpdaterImpl();

    private KubernetesClient kubernetesClient;
    private ApiextensionsAPIGroupDSL apiextensionsClient;
    boolean isVpaInstalled = false;
    private String recommendationTerm;
    private String recommendationEngine;
    private int sleepWindow;


    private VpaUpdaterImpl() {
        this.kubernetesClient = new DefaultKubernetesClient();
        this.apiextensionsClient = kubernetesClient.apiextensions();
        this.isVpaInstalled = isUpdaterInstalled();
    }

    public static VpaUpdaterImpl getInstance() {
        return vpaUpdater;
    }

    @Override
    public boolean isUpdaterInstalled() {
        CustomResourceDefinitionList crdList = apiextensionsClient.v1().customResourceDefinitions().list();
        this.isVpaInstalled = crdList.getItems().stream().anyMatch(crd -> AnalyzerConstants.RecommendationUpdaterConstants.VERTICAL_POD_AUTOSCALER.equalsIgnoreCase(crd.getSpec().getNames().getKind()));
        return isVpaInstalled;
    }

    public void initiateUpdater() {
        LOGGER.info("Trying to start the VPA updater...");
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

        if (!isVpaInstalled) {
            LOGGER.error(AnalyzerErrorConstants.RecommendationUpdaterErrorConstant.VPA_NOT_INSTALLED);
        } else {
            setVpaUpdaterConfigs();
            LOGGER.info(AnalyzerConstants.RecommendationUpdaterConstants.STARTING_VPA_INFO_MSG);
            executorService.scheduleAtFixedRate(() -> {
                try {
                    getRecommendationsAndUpdateVpas();
                } catch (Exception e) {
                    LOGGER.error(String.format(AnalyzerErrorConstants.RecommendationUpdaterErrorConstant.ERROR_PROCESSING_THREADS, e.getMessage()));
                }
            }, 0, sleepWindow, TimeUnit.SECONDS);
        }
    }

    private void getRecommendationsAndUpdateVpas() {
        try {
            LOGGER.info("Checking Available VPA Objects...");
            NamespacedVerticalPodAutoscalerClient client = new DefaultVerticalPodAutoscalerClient();
            VerticalPodAutoscalerList vpas = client.v1().verticalpodautoscalers().inAnyNamespace().list();
            LOGGER.info("Found " + vpas.getItems().size() + " vpa objects.");

//            List<VerticalPodAutoscaler> vpasList = selectVpasForKruizeRecommender(vpas.getItems());
            for (VerticalPodAutoscaler vpa: vpas.getItems()) {
                String name = vpa.getMetadata().getName();
                LOGGER.info("Generating Recommendations For VPA - " +  name );
                VerticalPodAutoscalerStatus vpaStatus = generateRecommendations(name);
                if (vpaStatus != null) {
                    vpa.setStatus(vpaStatus);
                    client.v1().verticalpodautoscalers().inNamespace(vpa.getMetadata().getNamespace()).withName(vpa.getMetadata().getName()).patchStatus(vpa);
                    LOGGER.info("VPA Object is patched with recommendations successfully.");
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    private VerticalPodAutoscalerStatus generateRecommendations(String name) {
        try {
            RecommendationEngine recommendationEngine = new RecommendationEngine(name, null, null);
            int calCount = 0;
            String validationMessage = recommendationEngine.validate_local();
            if (validationMessage.isEmpty()) {
                KruizeObject kruizeObject = recommendationEngine.prepareRecommendations(calCount);
                if (kruizeObject.getValidation_data().isSuccess()) {
                    LOGGER.info("Recommendations generated.");
                    for (K8sObject k8sObject: kruizeObject.getKubernetes_objects()) {
//                        LOGGER.info(k8sObject.getContainerDataMap().toString());
                        List<RecommendedContainerResources> recoms = convertRecommendationsToContainerPolicy(k8sObject.getContainerDataMap());
                        if (recoms.isEmpty()){
                            LOGGER.error("No Recommendations could be generated.");
                        } else {
                            RecommendedPodResources recommendedPodResources = new RecommendedPodResources();
                            recommendedPodResources.setContainerRecommendations(recoms);
                            VerticalPodAutoscalerStatus vpaStatus = new VerticalPodAutoscalerStatusBuilder()
                                    .withRecommendation(recommendedPodResources)
                                    .build();
                            return vpaStatus;
                        }
                    }
                } else {
                    LOGGER.error("Failed to generate recommendations.");
                }
            } else {
                LOGGER.error(validationMessage);
            }
        }  catch (Exception | FetchMetricsError e) {
            LOGGER.error(e.getMessage());
        }
        return null;
    }

    private List<RecommendedContainerResources>  convertRecommendationsToContainerPolicy(HashMap<String, ContainerData> containerDataMap) {
        List<RecommendedContainerResources> containerRecommendations = new ArrayList<>();
        for (Map.Entry<String, ContainerData> entry : containerDataMap.entrySet()) {
            String name = entry.getKey();
            ContainerData data = entry.getValue();
            String containerName = data.getContainer_name();
            HashMap<Timestamp, MappedRecommendationForTimestamp> recommendationData = data.getContainerRecommendations().getData();
            if (recommendationData != null) {
                for (MappedRecommendationForTimestamp value : recommendationData.values()) {
                    TermRecommendations termRecommendations = value.getShortTermRecommendations();
                    HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> config = termRecommendations.getCostRecommendations().getConfig();
                    Double shortTermCpu = config.get(AnalyzerConstants.ResourceSetting.requests).get(AnalyzerConstants.RecommendationItem.CPU).getAmount();
                    Double shortTermMemory = config.get(AnalyzerConstants.ResourceSetting.requests).get(AnalyzerConstants.RecommendationItem.MEMORY).getAmount();
                    LOGGER.info("Recommended cpu value for container " + containerName + " - " + shortTermCpu);
                    LOGGER.info("Recommended memory value for container " + containerName + " - " + shortTermMemory);
                    String finalCpu = resource2str("cpu", shortTermCpu);
                    String finalMemory = resource2str("memory", shortTermMemory);

                    RecommendedContainerResources recommendedContainerResources = new RecommendedContainerResources();
////                    Map<String, Quantity>
                    recommendedContainerResources.setContainerName(containerName);
//                    recommendedContainerResources.setLowerBound();

                    Map<String, Quantity> target = new HashMap<>();
                    target.put("cpu", new Quantity(finalCpu)); // Target 1 CPU
                    target.put("memory", new Quantity(finalMemory)); // Target 1 Gi memory

                    Map<String, Quantity> lowerBound = new HashMap<>();
                    lowerBound.put("cpu", new Quantity(finalCpu)); // Lower bound 0.5 CPU
                    lowerBound.put("memory", new Quantity(finalMemory)); // Lower bound 512 Mi memory

                    Map<String, Quantity> upperBound = new HashMap<>();
                    upperBound.put("cpu", new Quantity(finalCpu)); // Upper bound 2 CPUs
                    upperBound.put("memory", new Quantity(finalMemory)); // Upper bound 2 Gi memory

                    recommendedContainerResources.setLowerBound(lowerBound);
                    recommendedContainerResources.setTarget(target);
                    recommendedContainerResources.setUpperBound(upperBound);

                    containerRecommendations.add(recommendedContainerResources);
                }
            } else {
                LOGGER.error("No Recommendation Data.");
            }
        }
        return containerRecommendations;
    }

    public static String resource2str(String resource, double value) {
        if (resource.equalsIgnoreCase("cpu")) {
            if (value < 1) {
                return (int) (value * 1000) + "m";
            } else {
                return String.valueOf(value);
            }
        } else {
            if (value < 1024) {
                return (int) value + "B";
            } else if (value < 1024 * 1024) {
                return (int) (value / 1024) + "k";
            } else if (value < 1024 * 1024 * 1024) {
                return (int) (value / 1024 / 1024) + "Mi";
            } else {
                return (int) (value / 1024 / 1024 / 1024) + "Gi";
            }
        }
    }

//    private List<VerticalPodAutoscaler> selectVpasForKruizeRecommender(List<VerticalPodAutoscaler> vpasList) {
//        List<VerticalPodAutoscaler> selectedVpas = new ArrayList<>();
//        for (VerticalPodAutoscaler vpa : vpasList) {
////            LOGGER.info(vpa.getAdditionalProperties().get(AnalyzerConstants.RecommendationUpdaterConstants.RECOMMENDERS).toString());
////            if (vpa.getAdditionalProperties().containsKey(AnalyzerConstants.RecommendationUpdaterConstants.RECOMMENDERS)) {
//                List<Map<String, Object>> recommenders = (List<Map<String, Object>>) vpa.getAdditionalProperties().get(AnalyzerConstants.RecommendationUpdaterConstants.RECOMMENDERS);
//                for (Map<String, Object> recommender : recommenders) {
////                    if (recommender.get("name").toString().equalsIgnoreCase("kruize")) {
//                        selectedVpas.add(vpa);
////                    }
////                }
////            }
//        }
//        return selectedVpas;
//    }

    public void createVpaObject(String vpaName, String deploymentName, String namespace, List<ContainerAPIObject> containersList) throws VpaObjectCreateError {
        try {
            if (isVpaInstalled) {
                LOGGER.info(AnalyzerConstants.RecommendationUpdaterConstants.CREATING_VPA_OBJECT_INFO_MSG + " " + vpaName);
                Map<String, Object> additionalVpaObjectProps = new HashMap<>();
                List<Map<String, Object>> recommenders = new ArrayList<>();
                Map<String, Object> recommender = new HashMap<>();
                recommender.put("name", "kruize");
                recommenders.add(recommender);

                additionalVpaObjectProps.put(AnalyzerConstants.RecommendationUpdaterConstants.RECOMMENDERS, recommenders);

                List<String> controlledResources = new ArrayList<>();
                controlledResources.add("cpu");
                controlledResources.add("memory");

                List<ContainerResourcePolicy> containerPolicies = new ArrayList<>();
                for (ContainerAPIObject container : containersList) {
                    ContainerResourcePolicy policy = new ContainerResourcePolicyBuilder()
                            .withContainerName(container.getContainer_name())
                            .withControlledResources(controlledResources)
                            .build();
                    containerPolicies.add(policy);
                }

                PodResourcePolicy podPolicy = new PodResourcePolicyBuilder()
                        .withContainerPolicies(containerPolicies)
                        .build();

                VerticalPodAutoscaler vpa = new VerticalPodAutoscalerBuilder()
                        .withApiVersion(AnalyzerConstants.RecommendationUpdaterConstants.VPA_API_VERSION)
                        .withKind(AnalyzerConstants.RecommendationUpdaterConstants.VERTICAL_POD_AUTOSCALER)
                        .withMetadata(new ObjectMeta() {{
                            setName(vpaName);
                        }})
                        .withSpec(new VerticalPodAutoscalerSpecBuilder()
                                .withTargetRef(new CrossVersionObjectReferenceBuilder()
                                        .withApiVersion(AnalyzerConstants.RecommendationUpdaterConstants.VPA_TARGET_REF_API_VERSION)
                                        .withKind(AnalyzerConstants.RecommendationUpdaterConstants.VPA_TARGET_REF_KIND)
                                        .withName(deploymentName)
                                        .build())
                                .withResourcePolicy(podPolicy)
                                .withAdditionalProperties(additionalVpaObjectProps)
                                .build())
                        .build();

//                NamespacedVerticalPodAutoscalerClient vpaClient = new DefaultVerticalPodAutoscalerClient();
//                vpaClient.v1().verticalpodautoscalers().inNamespace(namespace).resource(vpa).create();

                kubernetesClient.resource(vpa).inNamespace(namespace).createOrReplace();

                LOGGER.info(AnalyzerConstants.RecommendationUpdaterConstants.CREATED_VPA_OBJECT_INFO_MSG + " " + vpaName);
            } else {
                throw new VpaObjectCreateError(AnalyzerErrorConstants.RecommendationUpdaterErrorConstant.VPA_NOT_INSTALLED);
            }
        } catch (Exception e) {
            throw new VpaObjectCreateError(e.getMessage());
        }
    }

    private void setVpaUpdaterConfigs() {
        this.recommendationTerm = AnalyzerConstants.RecommendationUpdaterConstants.DEFAULT_RECOMMENDATION_TERM;
        this.recommendationEngine = AnalyzerConstants.RecommendationUpdaterConstants.DEFAULT_RECOMMENDATION_ENGINE;
        this.sleepWindow = AnalyzerConstants.RecommendationUpdaterConstants.SLEEP_WINDOW;
    }
}
