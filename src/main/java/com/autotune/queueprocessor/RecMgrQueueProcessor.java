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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import com.autotune.em.utils.EMUtils.QueueName;
import com.autotune.queue.AutotuneDTO;
import com.autotune.queue.AutotuneQueueFactory;
import com.autotune.queue.EventConsumer;
import com.autotune.queue.EventProducer;

public class RecMgrQueueProcessor implements QueueProcessor {

	@Override
	public AutotuneDTO receive(String queueName) {
		
		EventConsumer consumerEvnt = new EventConsumer(AutotuneQueueFactory.getQueue(QueueName.RECMGRQUEUE.name()));
		FutureTask<AutotuneDTO> recieveFromQueueTask = new FutureTask<AutotuneDTO>(consumerEvnt);
		Thread t = new Thread(recieveFromQueueTask);
    	t.start();
		AutotuneDTO recMgrInputDTO = null;
		
		try {
			while(true) {
				if (recieveFromQueueTask.isDone()) {
					recMgrInputDTO = recieveFromQueueTask.get();
					recMgrInputDTO.setName("RecMgrProcessor");
	    			System.out.println("resultDTO --->" + recMgrInputDTO);
	    			break;
				}
				Thread.sleep(100);
    		}
			
			// call the ML module for recommendation
			// business logic
			
			
			// create a new DTO and send to the ExperimentManagerQueue
			EventProducer expMgrEventProducer = new EventProducer(AutotuneQueueFactory.getQueue(QueueName.EXPMGRQUEUE.name()), recMgrInputDTO);
			FutureTask<Boolean> expMgrTask = new FutureTask<Boolean>(expMgrEventProducer);
			Thread t1= new Thread(expMgrTask);
			t1.start();
			boolean isAdded = false;
			
			while(true) {
				if(expMgrTask.isDone()) {
					isAdded =(boolean) expMgrTask.get();
					break;
				}
			}
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		
		ExpMgrQueueProcessor expMgrProcessor = new ExpMgrQueueProcessor();
		expMgrProcessor.process();
		return recMgrInputDTO;
	}

	@Override
	public void send(AutotuneDTO autotuneDTO, String queueName) {
	}
}
