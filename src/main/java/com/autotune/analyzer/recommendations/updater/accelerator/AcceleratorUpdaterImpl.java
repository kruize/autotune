package com.autotune.analyzer.recommendations.updater.accelerator;

import com.autotune.analyzer.exceptions.ApplyRecommendationsError;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.objects.MappedRecommendationForTimestamp;
import com.autotune.analyzer.recommendations.objects.TermRecommendations;
import com.autotune.analyzer.recommendations.updater.RecommendationUpdaterImpl;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.utils.CommonUtils;
import com.autotune.settings.CentralSettings;
import com.autotune.settings.SettingsUpdater;
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

public class AcceleratorUpdaterImpl extends RecommendationUpdaterImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcceleratorUpdaterImpl.class);
    private static AcceleratorUpdaterImpl acceleratorUpdater;
    private KubernetesClient kubernetesClient;

    private AcceleratorUpdaterImpl() {
        this.kubernetesClient = new DefaultKubernetesClient();
    }

    public static AcceleratorUpdaterImpl getInstance() {
        if (null != acceleratorUpdater) {
            return acceleratorUpdater;
        }
        synchronized (AcceleratorUpdaterImpl.class) {
            if (null == acceleratorUpdater) {
                acceleratorUpdater = new AcceleratorUpdaterImpl();
            }
        }
        return acceleratorUpdater;
    }

    @Override
    public boolean isUpdaterInstalled() {
        CentralSettings centralSettings = CentralSettings.getInstance();
        SettingsUpdater.checkIfInstasliceIsAvailable(centralSettings);
        return centralSettings.isAllowGPUResourceUpdates();
    }

    @Override
    public void applyResourceRecommendationsForExperiment(KruizeObject kruizeObject) throws ApplyRecommendationsError {
        if (!isUpdaterInstalled()) {
            LOGGER.error(AnalyzerErrorConstants.RecommendationUpdaterErrors.UPDATER_NOT_INSTALLED);
            return;
        }

        try {
            String namespace = kruizeObject.getKubernetes_objects().get(0).getNamespace();
            String workloadName = kruizeObject.getKubernetes_objects().get(0).getName();
            if (null == namespace){
                // Add Logger
                return;
            }

            Map<String, ContainerData> containers = kruizeObject.getKubernetes_objects().get(0).getContainerDataMap();
            if (null == containers) {
                // Add Logger
                return;
            }

            for (Map.Entry<String, ContainerData> entry : containers.entrySet()) {
                String containerName = entry.getKey();
                ContainerData containerData = entry.getValue();

                Map<Timestamp, MappedRecommendationForTimestamp> timestampMap = containerData.getContainerRecommendations().getData();
                if (null == timestampMap || timestampMap.isEmpty()) {
                    System.out.println("Timestamp map is empty");
                    continue;
                }


                Map.Entry<Timestamp, MappedRecommendationForTimestamp> latestEntry = null;
                for (Map.Entry<Timestamp, MappedRecommendationForTimestamp> timestampEntry : timestampMap.entrySet()) {
                    if (latestEntry == null || timestampEntry.getKey().after(latestEntry.getKey()))
                        latestEntry = timestampEntry;
                }

                MappedRecommendationForTimestamp latestRecommendation = latestEntry.getValue();
                if (null == latestRecommendation) {
                    System.out.println("latest recommendation is null");
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

                System.out.println("Calling updater");
                updateOrRevertResources(containerName, namespace, workloadName, "job",
                        updatedRec);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateOrRevertResources(String containerName,
                                               String namespace,
                                               String workloadName,
                                               String koType,
                                               HashMap<AnalyzerConstants.ResourceSetting,
                                                       HashMap<AnalyzerConstants.RecommendationItem,
                                                               RecommendationConfigItem>> recommendations )  {

        System.out.println("Updating Resources");
        Map<String, Map<String, Quantity>> originalResourcesRequests = new HashMap<>();
        Map<String, Map<String, Quantity>> originalResourcesLimits = new HashMap<>();

        try (KubernetesClient kubernetesClient = new DefaultKubernetesClient()) {
            if (koType.equalsIgnoreCase("deployment")) {
                kubernetesClient.apps().deployments().inNamespace(namespace).list().getItems().forEach(deployment -> {
                    deployment.getSpec().getTemplate().getSpec().getContainers().stream().filter(
                            container -> container.getName().equals(containerName)
                    ).forEach(container -> {
                        originalResourcesRequests.put(containerName, container.getResources().getRequests());
                        originalResourcesLimits.put(containerName, container.getResources().getLimits());

                        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requestRecommendation =
                                recommendations.get(AnalyzerConstants.ResourceSetting.requests);

                        HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> limitsRecommendation =
                                recommendations.get(AnalyzerConstants.ResourceSetting.limits);

                        Map<String, Quantity> requestMap = new HashMap<>();
                        Map<String, Quantity> limitsMap = new HashMap<>();

                        for (Map.Entry<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> entry : requestRecommendation.entrySet()) {
                            System.out.println("Recommendation: " + entry.getKey().toString());
                            System.out.println("Quantity: " + entry.getValue().getAmount().intValue() + " | Format: entry.getValue().getFormat()");
                            requestMap.put(entry.getKey().toString(), new Quantity(String.valueOf(entry.getValue().getAmount().intValue()), entry.getValue().getFormat()));
                        }

                        for (Map.Entry<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> entry : limitsRecommendation.entrySet()) {
                            System.out.println("Recommendation: " + entry.getKey().toString());
                            System.out.println("Quantity: " + entry.getValue().getAmount().intValue() + " | Format: entry.getValue().getFormat()");
                            if (entry.getKey().toString().contains("nvidia")) {
                                limitsMap.put(entry.getKey().toString(), new Quantity(String.valueOf(entry.getValue().getAmount().intValue())));
                            } else {
                                limitsMap.put(entry.getKey().toString(), new Quantity(String.valueOf(entry.getValue().getAmount().intValue()), entry.getValue().getFormat()));
                            }
                        }


                        container.getResources().setRequests(requestMap);
                        container.getResources().setLimits(limitsMap);

                        kubernetesClient.apps().deployments()
                                .inNamespace(namespace)
                                .withName(deployment.getMetadata().getName())
                                .patch(deployment);
                    });
                });
            } else if (koType.equalsIgnoreCase("statefulset")) {
                kubernetesClient.apps().statefulSets().inNamespace(namespace).list().getItems().forEach(statefulSet -> {
                    statefulSet.getSpec().getTemplate().getSpec().getContainers().stream().filter(
                            container -> container.getName().equals(containerName)
                    ).forEach(container -> {
                        originalResourcesRequests.put(containerName, container.getResources().getRequests());
                        originalResourcesLimits.put(containerName, container.getResources().getLimits());

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

                        kubernetesClient.apps().statefulSets()
                                .inNamespace(namespace)
                                .withName(statefulSet.getMetadata().getName())
                                .patch(statefulSet);
                    });
                });
            } else if (koType.equalsIgnoreCase("job")) {
                System.out.println("inside job edit");
                Job existingJob = kubernetesClient.batch().jobs().inNamespace(namespace).withName(workloadName).get();
                if (existingJob == null) {
                    System.out.println("Job not found!");
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
                            System.out.println("Recommendation: " + entry.getKey().toString());
                            System.out.println("Quantity: " + entry.getValue().getAmount().intValue() + " | Format:" + entry.getValue().getFormat());
                            requestMap.put(entry.getKey().toString(), new Quantity(String.valueOf(entry.getValue().getAmount().intValue()), entry.getValue().getFormat()));
                        }

                        for (Map.Entry<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> entry : limitsRecommendation.entrySet()) {
                            System.out.println("Recommendation: " + entry.getKey().toString());
                            System.out.println("Quantity: " + entry.getValue().getAmount().intValue() + " | Format:" + entry.getValue().getFormat());
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
                    labels.remove("controller-uid");
                    labels.remove("batch.kubernetes.io/controller-uid");
                }

                existingJob.getSpec().setSelector(null);

                kubernetesClient.batch().jobs().inNamespace(namespace).withName(workloadName).delete();
                kubernetesClient.batch().jobs().inNamespace(namespace).createOrReplace(existingJob);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
