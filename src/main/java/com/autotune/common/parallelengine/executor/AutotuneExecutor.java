package com.autotune.common.parallelengine.executor;


import com.autotune.common.parallelengine.queue.AutotuneQueue;
import com.autotune.common.parallelengine.worker.AutotuneWorker;
import com.autotune.common.parallelengine.worker.CallableFactory;

import java.util.concurrent.*;

public class AutotuneExecutor extends ThreadPoolExecutor {
    private final AutotuneQueue autotuneQueue;

    public AutotuneExecutor(int corePoolSize, int maximumPoolSize,
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
                    AutotuneWorker theWorker = new CallableFactory().create(worker);
                    theWorker.execute(take);
                } catch (Exception e) {
                    System.out.println(e);
                    e.printStackTrace();
                }

            }
        });
    }
}
