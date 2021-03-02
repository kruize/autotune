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
import com.autotune.queue.AutotuneQueue;
import com.autotune.queue.AutotuneQueueFactory;
import com.autotune.queue.EventProducer;

public class DAQueueProcessor implements QueueProcessor {
	
	private AutotuneQueue recMgrQueue;
	
	@Override
	public void process() {
		
		AutotuneQueueFactory queueFactory = new AutotuneQueueFactory();
		recMgrQueue = queueFactory.getQueue(QueueName.RECMGRQUEUE.name());
		
	    AutotuneDTO newDTO = new AutotuneDTO();
	    newDTO.setId(1000);
	    newDTO.setUrl("https://localhost:8080/recconfig/");
	    newDTO.setName("DA-Processor");
	   
	    EventProducer eventProducer = new EventProducer(recMgrQueue, newDTO);
    	FutureTask<Boolean> addToQueueTask = new FutureTask<Boolean>(eventProducer);
    	Thread t = new Thread(addToQueueTask);
    	t.start();
    	boolean isAdded=false; 
    	
    	try {
    		while(true) {
    			if(addToQueueTask.isDone()) {
	    			isAdded = (Boolean)addToQueueTask.get();
	    			break;
    			}
    		}
    		
			
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
    	
    	// Initializing Recommendation Manager 
		RecMgrQueueProcessor recMgrProcess = new RecMgrQueueProcessor();
		recMgrProcess.process();
	}

}
