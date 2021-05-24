package com.autotune.experimentManager.data;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EMStageProcessQueue {
    private static volatile EMStageProcessQueue emStageProcessQueue = null;
    private volatile ConcurrentLinkedQueue<EMStageTransition> emStageQueue;
    private volatile ConcurrentLinkedQueue<EMStageScheduledTransition> emStageScheduledQueue;

    private EMStageProcessQueue() {
        emStageQueue = new ConcurrentLinkedQueue<EMStageTransition>();
        emStageScheduledQueue = new ConcurrentLinkedQueue<EMStageScheduledTransition>();
    }

    public static EMStageProcessQueue getStageProcessQueueInstance() {
        if (null == emStageProcessQueue) {
            synchronized (EMStageProcessQueue.class) {
                if (null == emStageProcessQueue) {
                    emStageProcessQueue = new EMStageProcessQueue();
                }
            }
        }
        return emStageProcessQueue;
    }

    public ConcurrentLinkedQueue<EMStageTransition> getQueue() {
        return emStageQueue;
    }

    public ConcurrentLinkedQueue<EMStageScheduledTransition> getScheduledQueue() {
        return emStageScheduledQueue;
    }
}
