package com.autotune.analyzer.autoscaler.accelerator;

import com.autotune.analyzer.autoscaler.AutoscalerImpl;
import com.autotune.analyzer.autoscaler.accelerator.utils.AcceleratorAutoscalerUtils;
import com.autotune.analyzer.autoscaler.settings.AutoscalingSettings;
import com.autotune.analyzer.exceptions.ApplyRecommendationsError;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.objects.MappedRecommendationForTimestamp;
import com.autotune.analyzer.recommendations.objects.TermRecommendations;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.utils.CommonUtils;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AcceleratorAutoscalerImpl extends AutoscalerImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcceleratorAutoscalerImpl.class);
    private static AcceleratorAutoscalerImpl acceleratorUpdater;
    private KubernetesClient kubernetesClient;

    private AcceleratorAutoscalerImpl() {
        this.kubernetesClient = new DefaultKubernetesClient();
    }

    public static AcceleratorAutoscalerImpl getInstance() {
        if (null != acceleratorUpdater) {
            return acceleratorUpdater;
        }
        synchronized (AcceleratorAutoscalerImpl.class) {
            if (null == acceleratorUpdater) {
                acceleratorUpdater = new AcceleratorAutoscalerImpl();
            }
        }
        return acceleratorUpdater;
    }

    @Override
    public boolean isUpdaterInstalled() {
        AutoscalingSettings.getInstance().checkIfInstasliceIsAvailable();
        return AutoscalingSettings.getInstance().isAllowGPUResourceUpdates();
    }

    @Override
    public void applyResourceRecommendationsForExperiment(KruizeObject kruizeObject) throws ApplyRecommendationsError {
        if (!isUpdaterInstalled()) {
            LOGGER.error(AnalyzerErrorConstants.AutoscalerErrors.UPDATER_NOT_INSTALLED);
            return;
        }

        try {
            String namespace = kruizeObject.getKubernetes_objects().get(0).getNamespace();
            String workloadName = kruizeObject.getKubernetes_objects().get(0).getName();
            if (null == namespace){
                LOGGER.error(AnalyzerErrorConstants.AutoscalerErrors.AcceleratorAutoscaler.NAMESPACE_NULL);
                return;
            }

            Map<String, ContainerData> containers = kruizeObject.getKubernetes_objects().get(0).getContainerDataMap();
            if (null == containers) {
                LOGGER.error(AnalyzerErrorConstants.AutoscalerErrors.AcceleratorAutoscaler.CONTAINER_NULL);
                return;
            }

            for (Map.Entry<String, ContainerData> entry : containers.entrySet()) {
                String containerName = entry.getKey();
                ContainerData containerData = entry.getValue();

                Map<Timestamp, MappedRecommendationForTimestamp> timestampMap = containerData.getContainerRecommendations().getData();
                if (null == timestampMap || timestampMap.isEmpty()) {
                    continue;
                }


                Map.Entry<Timestamp, MappedRecommendationForTimestamp> latestEntry = null;
                for (Map.Entry<Timestamp, MappedRecommendationForTimestamp> timestampEntry : timestampMap.entrySet()) {
                    if (latestEntry == null || timestampEntry.getKey().after(latestEntry.getKey()))
                        latestEntry = timestampEntry;
                }

                MappedRecommendationForTimestamp latestRecommendation = latestEntry.getValue();
                if (null == latestRecommendation) {
                    continue;
                }


                TermRecommendations shortTermRec = latestRecommendation.getShortTermRecommendations();
                HashMap<AnalyzerConstants.ResourceSetting,
                        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> existingMap =
                        shortTermRec.getCostRecommendations().getConfig();

                HashMap<AnalyzerConstants.ResourceSetting,
                        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> updatedRec = new HashMap<>();

                // Process requests
                HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> updatedRequests = new HashMap<>();
                Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> existingRequests = existingMap.get(AnalyzerConstants.ResourceSetting.requests);

                for (Map.Entry<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requestMapEntry : existingRequests.entrySet()) {
                    AnalyzerConstants.RecommendationItem recommendationItem = requestMapEntry.getKey();
                    RecommendationConfigItem recommendationConfigItem = requestMapEntry.getValue();

                    if (recommendationItem == AnalyzerConstants.RecommendationItem.CPU) {
                        updatedRequests.put(recommendationItem, CommonUtils.formatCpuUnits(recommendationConfigItem));
                    } else if (recommendationItem == AnalyzerConstants.RecommendationItem.MEMORY) {
                        updatedRequests.put(recommendationItem, CommonUtils.formatMemoryUnits(recommendationConfigItem));
                    } else {
                        updatedRequests.put(recommendationItem, CommonUtils.formatAcceleratorUnits(recommendationConfigItem));
                    }
                }

                HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> updatedLimits = new HashMap<>();
                Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> existingLimits = existingMap.get(AnalyzerConstants.ResourceSetting.limits);

                for (Map.Entry<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> limitsMapEntry : existingLimits.entrySet()) {
                    AnalyzerConstants.RecommendationItem recommendationItem = limitsMapEntry.getKey();
                    RecommendationConfigItem recommendationConfigItem = limitsMapEntry.getValue();

                    if (recommendationItem == AnalyzerConstants.RecommendationItem.CPU) {
                        updatedLimits.put(recommendationItem, CommonUtils.formatCpuUnits(recommendationConfigItem));
                    } else if (recommendationItem == AnalyzerConstants.RecommendationItem.MEMORY) {
                        updatedLimits.put(recommendationItem, CommonUtils.formatMemoryUnits(recommendationConfigItem));
                    } else {
                        updatedLimits.put(recommendationItem, CommonUtils.formatAcceleratorUnits(recommendationConfigItem));
                    }
                }

                if (!updatedRequests.isEmpty())
                    updatedRec.put(AnalyzerConstants.ResourceSetting.requests, updatedRequests);

                if (!updatedLimits.isEmpty())
                    updatedRec.put(AnalyzerConstants.ResourceSetting.limits, updatedLimits);

                updateOrRevertResources(containerName, namespace, workloadName, AnalyzerConstants.K8sObjectConstants.Types.JOB,
                        updatedRec);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates or reverts (Yet to be implemented) resource settings for a specified container in a Kubernetes workload.
     * <p>
     * This method adjusts resource requests and limits for a container within a Deployment, StatefulSet, or Job
     * based on the provided recommendations. It supports the following Kubernetes object types:
     * <ul>
     *     <li>Deployment</li>
     *     <li>StatefulSet</li>
     *     <li>Job</li>
     * </ul>
     * <p>
     * For Deployments and StatefulSets:
     * <ul>
     *     <li>Updates resource values of the container using the provided recommendations.</li>
     *     <li>Patches the updated Deployment or StatefulSet back to the cluster.</li>
     * </ul>
     * <p>
     * For Jobs:
     * <ul>
     *     <li>Finds the Job by name in the specified namespace.</li>
     *     <li>Updates the container's resource requests and limits according to the recommendations.</li>
     *     <li>Removes specific controller labels to avoid conflicts during recreation.</li>
     *     <li>Deletes the existing Job and recreates it with the modified configuration.</li>
     * </ul>
     *
     * @param containerName  The name of the container whose resources are to be updated.
     * @param namespace      The namespace where the workload resides.
     * @param workloadName   The name of the workload (applicable only for Jobs).
     * @param koType         The type of Kubernetes object (Deployment, StatefulSet, or Job).
     *                       Only these types are supported.
     * @param recommendations A map containing recommended resource settings (requests and limits)
     *                        for the container.
     */
    public static void updateOrRevertResources(String containerName,
                                               String namespace,
                                               String workloadName,
                                               String koType,
                                               HashMap<AnalyzerConstants.ResourceSetting,
                                                       HashMap<AnalyzerConstants.RecommendationItem,
                                                               RecommendationConfigItem>> recommendations )  {

        Map<String, Map<String, Quantity>> originalResourcesRequests = new HashMap<>();
        Map<String, Map<String, Quantity>> originalResourcesLimits = new HashMap<>();

        try (KubernetesClient kubernetesClient = new DefaultKubernetesClient()) {
            if (koType.equalsIgnoreCase(AnalyzerConstants.K8sObjectConstants.Types.DEPLOYMENT)) {
                kubernetesClient.apps().deployments().inNamespace(namespace).list().getItems().forEach(deployment -> {
                    deployment.getSpec().getTemplate().getSpec().getContainers().stream().filter(
                            container -> container.getName().equals(containerName)
                    ).forEach(container -> {
                        AcceleratorAutoscalerUtils.updateResourceValues(containerName,
                                container,
                                recommendations,
                                originalResourcesRequests,
                                originalResourcesLimits);

                        kubernetesClient.apps().deployments()
                                .inNamespace(namespace)
                                .withName(deployment.getMetadata().getName())
                                .patch(deployment);
                        // TODO: Add logic / watcher for reverting if the recommendations fail
                    });
                });
            } else if (koType.equalsIgnoreCase(AnalyzerConstants.K8sObjectConstants.Types.STATEFULSET)) {
                kubernetesClient.apps().statefulSets().inNamespace(namespace).list().getItems().forEach(statefulSet -> {
                    statefulSet.getSpec().getTemplate().getSpec().getContainers().stream().filter(
                            container -> container.getName().equals(containerName)
                    ).forEach(container -> {
                        AcceleratorAutoscalerUtils.updateResourceValues(containerName,
                                container,
                                recommendations,
                                originalResourcesRequests,
                                originalResourcesLimits);

                        kubernetesClient.apps().statefulSets()
                                .inNamespace(namespace)
                                .withName(statefulSet.getMetadata().getName())
                                .patch(statefulSet);
                        // TODO: Add logic / watcher for reverting if the recommendations fail
                    });
                });
            } else if (koType.equalsIgnoreCase(AnalyzerConstants.K8sObjectConstants.Types.JOB)) {
                Job existingJob = kubernetesClient.batch().jobs().inNamespace(namespace).withName(workloadName).get();
                if (existingJob == null) {
                    return;
                }
                PodTemplateSpec podTemplate = existingJob.getSpec().getTemplate();
                List<Container> containers = podTemplate.getSpec().getContainers();

                for (Container container : containers) {
                    if (container.getName().equals(containerName)) {
                        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requestRecommendation =
                                recommendations.get(AnalyzerConstants.ResourceSetting.requests);

                        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> limitsRecommendation =
                                recommendations.get(AnalyzerConstants.ResourceSetting.limits);

                        Map<String, Quantity> requestMap = new HashMap<>();
                        Map<String, Quantity> limitsMap = new HashMap<>();

                        for (Map.Entry<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> entry : requestRecommendation.entrySet()) {
                            requestMap.put(entry.getKey().toString(), new Quantity(String.valueOf(entry.getValue().getAmount().intValue()), entry.getValue().getFormat()));
                        }

                        for (Map.Entry<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> entry : limitsRecommendation.entrySet()) {
                            if (entry.getKey().toString().contains("nvidia")) {
                                limitsMap.put(entry.getKey().toString(), new Quantity(String.valueOf(entry.getValue().getAmount().intValue())));
                            } else {
                                limitsMap.put(entry.getKey().toString(), new Quantity(String.valueOf(entry.getValue().getAmount().intValue()), entry.getValue().getFormat()));
                            }
                        }

                        container.getResources().setRequests(requestMap);
                        container.getResources().setLimits(limitsMap);
                    }
                }

                Map<String, String> labels = podTemplate.getMetadata().getLabels();
                if (labels != null) {
                    labels.remove(AnalyzerConstants.AcceleratorConstants.AcceleratorAutoscalerLabels.CONTROLLER_UID);
                    labels.remove(AnalyzerConstants.AcceleratorConstants.AcceleratorAutoscalerLabels.BATCH_CONTROLLER_UID);
                }

                existingJob.getSpec().setSelector(null);

                kubernetesClient.batch().jobs().inNamespace(namespace).withName(workloadName).delete();

                // TODO: Add watcher to trigger event after complete deletion and proceed to job creation

                kubernetesClient.batch().jobs().inNamespace(namespace).createOrReplace(existingJob);

                // TODO: Add logic / watcher for reverting if the recommendations fail
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
