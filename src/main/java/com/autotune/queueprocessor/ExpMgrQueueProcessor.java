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
import com.autotune.queue.AutotuneExecutor;
import com.autotune.queue.AutotuneQueueFactory;
import com.autotune.queue.EventConsumer;

public class ExpMgrQueueProcessor implements QueueProcessor {
	private AutotuneExecutor executorService;
	private AutotuneDTO returnRecmMgrDTO;
	
	public ExpMgrQueueProcessor() {
		executorService = new AutotuneExecutor();
	}
	
	@Override
	public void process() {
		
		EventConsumer recMgrEventConsumer = new EventConsumer(AutotuneQueueFactory.getQueue(QueueName.EXPMGRQUEUE.name()));
		FutureTask<AutotuneDTO> expMgrInputTask = new FutureTask<AutotuneDTO>(recMgrEventConsumer);
		Thread t= new Thread(expMgrInputTask);
		t.start();
		
		try {
			
			while( true ) {
				if(expMgrInputTask.isDone()) {
					returnRecmMgrDTO = expMgrInputTask.get();
					break;
				}
				
			}
		
		} catch (InterruptedException ie) {
			ie.printStackTrace();
			
		} catch (ExecutionException ee) {
			ee.printStackTrace();
		}
		finally {
			
			executorService.shutdown();
		}
	}
	
	
}
