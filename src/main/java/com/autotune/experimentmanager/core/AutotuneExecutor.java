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
package com.autotune.experimentmanager.core;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.autotune.experimentmanager.fsm.object.ExperimentTrialObject;

/**
 * AutotuneExector is an abstraction of ExecutorService,
 * if you will not specify the default size it will create a
 * fixed size thread pool.
 * @author Bipin Kumar
 */

public class AutotuneExecutor {
	
	/**
	 * Default pool size = 5, it can be configured with new size while 
	 * Initializing the AutotuneExecutor.
	 */
	private final int DEFAULT_POOL_SIZE = 5;
	
	private ExecutorService executorService=null;
	
	public AutotuneExecutor(int poolSize) {
		executorService = Executors.newFixedThreadPool(poolSize);
	}
	
	public AutotuneExecutor() {
		executorService = Executors.newFixedThreadPool(DEFAULT_POOL_SIZE);
		
	}
	
	public Future<ExperimentTrialObject> submitTask(Callable<ExperimentTrialObject> task) {
		if (null != this.executorService) {
			return executorService.submit(task);
		}
		return null;
	}
	
	public void shutdown() {
		executorService.shutdown();
	}
	
}
