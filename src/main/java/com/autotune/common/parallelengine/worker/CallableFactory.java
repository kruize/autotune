/*******************************************************************************
 * Copyright (c) 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.common.parallelengine.worker;

import com.autotune.experimentManager.handler.eminterface.EMHandlerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;

/**
 * Returns WorkerImplementation class as per Class name declared.
 * Worker implementation must be declared here
 */
public final class CallableFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(EMHandlerFactory.class);

    public <T extends AutotuneWorker> AutotuneWorker create(Class<T> classType) {
        AutotuneWorker toReturn = null;
        try {
            Constructor<?> constructor = classType.getConstructor();
            toReturn = (AutotuneWorker) constructor.newInstance();
        } catch (Exception e) {
            LOGGER.error("Factory class not updated for Handler : {} ", classType.getName());
        }
        return toReturn;
    }
}
