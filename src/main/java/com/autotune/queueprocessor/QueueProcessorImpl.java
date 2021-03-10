/*******************************************************************************
 * Copyright (c) 2020, 2021 Red Hat, IBM Corporation and others.
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

package com.autotune.queueprocessor;

import com.autotune.queue.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class QueueProcessorImpl implements QueueProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(QueueProcessorImpl.class);

	@Override
	public void send(AutotuneDTO autotuneDTO, String queueName) {
		AutotuneQueue queue = AutotuneQueueFactory.getQueue(queueName);

		EventProducer eventProducer = new EventProducer(queue, autotuneDTO);
    	FutureTask<Boolean> addToQueueTask = new FutureTask<Boolean>(eventProducer);
    	Thread t = new Thread(addToQueueTask);
    	t.start();

		while(true) {
			if(addToQueueTask.isDone()) {
				LOGGER.info("Queue: sent to {}", queueName);
				break;
			}
		}
	}

	@Override
	public AutotuneDTO receive(String queueName) {
		EventConsumer consumerEvnt = new EventConsumer(AutotuneQueueFactory.getQueue(queueName));
		FutureTask<AutotuneDTO> recieveFromQueueTask = new FutureTask<>(consumerEvnt);
		Thread t = new Thread(recieveFromQueueTask);
		t.start();
		AutotuneDTO autotuneDTO = null;

		try {
			while(true) {
				if (recieveFromQueueTask.isDone()) {
					autotuneDTO = recieveFromQueueTask.get();
					return autotuneDTO;
				}
				Thread.sleep(100);
			}

		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			return autotuneDTO;
		}
	}
}
