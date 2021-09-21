/*******************************************************************************
 * Copyright (c) 2021, 2021 Red Hat, IBM Corporation and others.
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

package com.autotune.experimentManager.core;

import java.util.concurrent.Callable;

/**
 * EMScheduledStageProcessor polls the EM Scheduled transitions
 * queues and processes the stage transition based on the current
 * and target stage. It's implemented as a infinite loop which
 * polls when the queue is not empty and sleeps when queue is
 * empty.
 */
public class EMScheduledStageProcessor implements Callable {
    public EMExecutorService emExecutorService;

    public EMScheduledStageProcessor() {
        emExecutorService = EMExecutorService.getInstance();
    }

    @Override
    public Object call() throws Exception {
        // TODO: Need to be implemented
        return null;
    }

    public void notifyProcessor() {
        synchronized (this) {
            notify();
        }
    }
}
