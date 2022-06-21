package com.autotune.UserInterfaceSupport.util;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.concurrent.atomic.AtomicBoolean;


/**
 * A class to wrap all the utilies needed by the UISM module
 */
public class UISMUtils {
    /**
     * Checks if the namespace exists
     * @param checkNamespace
     * @return
     */
    public static boolean checkIfNamespaceExists(String checkNamespace) {
        AtomicBoolean exists = new AtomicBoolean(false);
        try {
            KubernetesClient client = new DefaultKubernetesClient();
            client.namespaces().list().getItems().forEach(namespace -> {
                if (namespace.getMetadata().getName().equals(checkNamespace)){
                    exists.set(true);
                }
            });
            return exists.get();
        } catch (Exception e) {
            return false;
        }
    }
}
