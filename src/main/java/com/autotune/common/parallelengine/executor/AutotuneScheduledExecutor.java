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
package com.autotune.common.parallelengine.executor;


import com.autotune.common.parallelengine.queue.AutotuneQueue;
import com.autotune.common.parallelengine.worker.AutotuneWorker;
import com.autotune.common.parallelengine.worker.CallableFactory;

import java.util.concurrent.*;

/**
 * This class is used if consumer requires a scheduled executor with specified delay before executing each experiment.
 */
public class AutotuneScheduledExecutor extends ThreadPoolExecutor {
    private final AutotuneQueue autotuneQueue;

    public AutotuneScheduledExecutor(int corePoolSize, int maximumPoolSize,
                                     long keepAliveTime, TimeUnit unit,
                                     BlockingQueue<Runnable> workQueue, AbortPolicy abortPolicy, AutotuneQueue aq, Class worker, int sleep) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        autotuneQueue = aq;

        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    startProcessingEvent(getAutotuneQueue().take(), worker);
                } catch (Exception e) {
                    System.out.println(e);
                    e.printStackTrace();
                }
            }
        }, 0, sleep, TimeUnit.SECONDS);
    }

    public AutotuneQueue getAutotuneQueue() {
        return autotuneQueue;
    }


    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);

    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
    }

    private void startProcessingEvent(Object take, Class worker) {
        submit(new Runnable() {
            @Override
            public void run() {
                try {
                    //AutotuneWorker theWorker = new CallableFactory().create(worker);
                    //theWorker.execute(take);
                } catch (Exception e) {
                    System.out.println(e);
                    e.printStackTrace();
                }

            }
        });
    }
}
