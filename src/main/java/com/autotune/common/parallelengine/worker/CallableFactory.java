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

import com.autotune.experimentManager.workerimpl.IterationManager;

/**
 * Returns WorkerImplementation class as per Class name declared.
 * Worker implementation must be declared here
 */
public final class CallableFactory {

    public <T extends AutotuneWorker> AutotuneWorker create(Class<T> clazz) {
        AutotuneWorker toReturn = null;
        if (IterationManager.class.equals(clazz)) {
            toReturn = new IterationManager();
        }
        return toReturn;
    }
}
