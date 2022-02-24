/*******************************************************************************
 * Copyright (c) 2022, 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.utils;

import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.EventBuilder;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;

/**
 * Event logging class that allows creating or replacing events with custom messages and reasons
 * for kubernetes objects.
 */
public class KubeEventLogger implements EventLogger {
    private final KubernetesClient kubeClient;
    private final Clock clock;

    private static final Logger LOGGER = LoggerFactory.getLogger(KubeEventLogger.class);

    public KubeEventLogger(KubernetesClient kubeClient, Clock clock) {
        this.kubeClient = kubeClient;
        this.clock = clock;
    }

    @Override
    public void log(String reason, String message, Type type, String objectName, String namespace, ObjectReference objectReference, String kind) {
        String componentName = AnalyzerConstants.AUTOTUNE;
        String eventName = componentName + "." + ((reason + message + type + objectName).hashCode() & 0x7FFFFFFF);
        Event existing = kubeClient.events().inNamespace(namespace).withName(eventName).get();
        String timestamp = Instant.now(clock).toString();
        try {
            if (existing != null && existing.getType().equals(type.name()) && existing.getReason().equals(reason) && existing.getInvolvedObject().getName().equals(objectName) && existing.getInvolvedObject().getKind().equals(kind)) {
                existing.setCount(existing.getCount() + 1);
                existing.setLastTimestamp(timestamp);
                kubeClient.events().inNamespace(namespace).withName(eventName).replace(existing);
            } else {
                Event newEvent = new EventBuilder()
                        .withNewMetadata()
                        .withName(eventName)
                        .endMetadata()
                        .withCount(1)
                        .withReason(reason)
                        .withMessage(message)
                        .withType(type.name())
                        .withInvolvedObject(objectReference)
                        .withFirstTimestamp(timestamp)
                        .withLastTimestamp(timestamp)
                        .withNewSource()
                        .withComponent(componentName)
                        .endSource()
                        .build();
                kubeClient.events().inNamespace(namespace).withName(eventName).create(newEvent);
            }
        } catch (KubernetesClientException e) {
            LOGGER.warn("Error reporting event: {}", e.getMessage());
        }
    }
}
