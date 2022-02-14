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

import io.fabric8.kubernetes.api.model.ObjectReference;

/**
 * Interface for handling events
 */
public interface EventLogger
{
    enum Type {
        Normal,
        Warning
    }

    /**
     * Log an event.
     * @param reason
     * @param message Descriptive message
     * @param type any of {@link Type}
     * @param objectName Name of object involved in event
     * @param namespace
     * @param objectReference
     * @param kind Kind of the object
     */
    void log(String reason, String message, Type type, String objectName, String namespace, ObjectReference objectReference, String kind);
}
