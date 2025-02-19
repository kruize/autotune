package com.autotune.analyzer.autoscaler.instaslice;

import com.autotune.analyzer.utils.AnalyzerConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class InstasliceHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstasliceHelper.class);

    private static InstasliceHelper instance;
    private static final String instasliceNamespace = "instaslice-system";
    private static final String group = "inference.redhat.com";
    private static final String version = "v1alpha1";
    private static final String namespace = "instaslice-system";
    private static final String plural = "instaslices";
    private static final String scope = "Namespaced";


    private InstasliceHelper() {
    }

    private boolean checkIfInstasliceExist() {
        try (KubernetesClient client = new DefaultKubernetesClient()){
            return client.namespaces().withName(namespace).get() != null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private List<GenericKubernetesResource> getInstasliceObjects() {
        try (KubernetesClient client = new DefaultKubernetesClient()) {
            CustomResourceDefinitionContext crdContext = new CustomResourceDefinitionContext.Builder()
                    .withGroup(group)
                    .withVersion(version)
                    .withScope(scope)
                    .withPlural(plural)
                    .build();

            return client.genericKubernetesResources(crdContext).inNamespace(namespace).list().getItems();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static InstasliceHelper getInstance() {
        if (null == instance) {
            synchronized (InstasliceHelper.class) {
                if (null == instance) {
                    instance = new InstasliceHelper();
                }
            }
        }
        return instance;
    }

    public String getUUID(String workloadNamespace, String workloadName) {
        if (!checkIfInstasliceExist())
            return null;

        AtomicReference<String> uuid = new AtomicReference<>(null);

        List<GenericKubernetesResource> instasliceObjects = getInstasliceObjects();
        ObjectMapper mapper = new ObjectMapper();

        if (null != instasliceObjects && !instasliceObjects.isEmpty()) {
            try {
                instasliceObjects.forEach(item -> {
                    String name = item.getMetadata().getName();
                    LOGGER.debug(AnalyzerConstants.AutoscalerConstants.InfoMsgs.FOUND_INSTASLICE, name);

                    Map<String, Object> allocations = (Map<String, Object>) ((Map<String, Object>) item.getAdditionalProperties().get("spec")).get("allocations");
                    for (Map.Entry<String, Object> entry : allocations.entrySet()) {
                        String key = entry.getKey();
                        Map<String, Object> allocationMap = (Map<String, Object>) entry.getValue();
                        if (allocationMap.get("namespace").toString().equalsIgnoreCase(workloadNamespace)
                                && allocationMap.get("podName").toString().startsWith(workloadName)) {
                            uuid.set(allocationMap.get("gpuUUID").toString());
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            LOGGER.info(AnalyzerConstants.AutoscalerConstants.InfoMsgs.NO_INSTASLICE_OBJECTS, namespace);
        }
        return uuid.get();
    }

    public String getMIGProfile(String workloadNamespace, String workloadName) {
        if (!checkIfInstasliceExist())
            return null;

        AtomicReference<String> profile = new AtomicReference<>(null);

        List<GenericKubernetesResource> instasliceObjects = getInstasliceObjects();
        ObjectMapper mapper = new ObjectMapper();

        if (null != instasliceObjects && !instasliceObjects.isEmpty()) {
            try {
                instasliceObjects.forEach(item -> {
                    String name = item.getMetadata().getName();
                    LOGGER.debug(AnalyzerConstants.AutoscalerConstants.InfoMsgs.FOUND_INSTASLICE, name);

                    Map<String, Object> allocations = (Map<String, Object>) ((Map<String, Object>) item.getAdditionalProperties().get("spec")).get("allocations");
                    for (Map.Entry<String, Object> entry : allocations.entrySet()) {
                        String key = entry.getKey();
                        Map<String, Object> allocationMap = (Map<String, Object>) entry.getValue();
                        if (allocationMap.get("namespace").toString().equalsIgnoreCase(workloadNamespace)
                                && allocationMap.get("podName").toString().startsWith(workloadName)) {
                            profile.set(allocationMap.get("profile").toString());
                        }
                    }

                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            LOGGER.info(AnalyzerConstants.AutoscalerConstants.InfoMsgs.NO_INSTASLICE_OBJECTS, namespace);
        }
        return profile.get();
    }
}