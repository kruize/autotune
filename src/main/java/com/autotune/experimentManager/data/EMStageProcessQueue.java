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


package com.autotune.experimentManager.data;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
* EMStageProcessQueue instance is a holder for the instances of two ConcurrentLinkedQueue's
*
* 1. Regular Queue - To hold EMStageTransition objects
* 2. Scheduled Queue - To hold EMStageScheduledTransition objects
*/
public class EMStageProcessQueue {
    private static volatile EMStageProcessQueue emStageProcessQueue = null;
    private volatile ConcurrentLinkedQueue<EMStageTransition> emStageQueue;
    private volatile ConcurrentLinkedQueue<EMStageScheduledTransition> emStageScheduledQueue;

    /**
     * Making it a singleton class by having a private constructor and initialising its own instance
     * when we make a first call to the class methods
     */
    private EMStageProcessQueue() {
        emStageQueue = new ConcurrentLinkedQueue<EMStageTransition>();
        emStageScheduledQueue = new ConcurrentLinkedQueue<EMStageScheduledTransition>();
    }

    public static EMStageProcessQueue getStageProcessQueueInstance() {
        /**
         * Check if the instance of the current class exist
         */
        if (null == emStageProcessQueue) {
            /**
             * Making sure that only one thread could create it and rest of them access the existing instance
             */
            synchronized (EMStageProcessQueue.class) {
                /**
                * Why we are having a multiple if checks for same attribute?
                *
                * This is required to handle a scenario where initial call is made by two threads at same time
                * so one goes in this block and creates a instance and if there is no check for null the other thread also
                * creates an instance and the first instance created by the previous thread is no longer valid
                *
                * Why can't we have this check only here and why to have it above synchronized block?
                *
                * Well it's just a step taking performance into consideration, If we don't have a if check for null above
                * synchronized block everytime to access the object the threads would be in a queued stated accessing
                * one by one which brings down the EM performance. So we allow threads to access the object parallel as
                * it's volatile
                * */
                if (null == emStageProcessQueue) {
                    emStageProcessQueue = new EMStageProcessQueue();
                }
            }
        }
        return emStageProcessQueue;
    }

    /**
     * Returns the regular queue
     * @return emStageQueue
     */
    public ConcurrentLinkedQueue<EMStageTransition> getQueue() {
        return emStageQueue;
    }

    /**
     * Returns the scheduled queue
     * @return emStageScheduledQueue
     */
    public ConcurrentLinkedQueue<EMStageScheduledTransition> getScheduledQueue() {
        return emStageScheduledQueue;
    }
}
