/*******************************************************************************
 * Copyright (c) 2021, 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.common.parallelengine.queue;

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * LinkedBlockingQueue is used to declare queues for various task like Experiment Queue, Analyser Queue etc.
 * @param <E>
 */
public class AutotuneQueue<E> extends LinkedBlockingQueue {
    public AutotuneQueue() {
        super();
    }

    public AutotuneQueue(int capacity) {
        super(capacity);
    }

    public AutotuneQueue(Collection c) {
        super(c);
    }

    @Override
    public void put(Object o) throws InterruptedException {
        super.put(o);
    }

    @Override
    public Object take() throws InterruptedException {
        return super.take();
    }
}
