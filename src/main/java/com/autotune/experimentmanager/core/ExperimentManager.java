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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.autotune.experimentmanager.fsm.object.ExperimentTrialObject;
import com.autotune.queue.AutotuneDTO;
import com.autotune.queue.queueprocessor.QueueProcessorImpl;
import com.autotune.utils.AutotuneUtil;

/**
 * ExperimentManager is a core class to receive message from AutotuneQueue and
 * start multiple experiment in parallel.
 * 
 * @author Bipin Kumar
 *
 */
public class ExperimentManager {
	private final Logger LOGGER = LoggerFactory.getLogger(ExperimentManager.class);
	private static ConcurrentHashMap<Integer, ExperimentTrialObject> trialsMap = null;
	private AutotuneExecutor executorService = null;

	public ExperimentManager() {
		executorService = new AutotuneExecutor();
		trialsMap = new ConcurrentHashMap<Integer, ExperimentTrialObject>();
	}

	public void satrt() {
		QueueProcessorImpl queueProcessorImpl = new QueueProcessorImpl();
		AutotuneDTO autotuneDTO = queueProcessorImpl.receive(AutotuneUtil.QueueName.EXPMGRQUEUE.name());
		ExperimentTrialObject expTrialInput;
		LOGGER.info("Received autotuneDTO: {}", autotuneDTO.toString());
		System.out.println("Message recieved from Queue dto=" + autotuneDTO.toString());
		try {
			RunExperiment runExperiment = new RunExperiment(autotuneDTO.getUrl());
			Future<ExperimentTrialObject> results = executorService.submitTask(runExperiment);
			if (results.isDone()) {
				expTrialInput = results.get();
				trialsMap.put(autotuneDTO.getId(), expTrialInput);
			}
		} catch (InterruptedException | ExecutionException ie) {
			ie.printStackTrace();

		} finally {
			executorService.shutdown();
		}
	}

	public static ExperimentTrialObject getExperimentMap(int experimentId) {
		if (trialsMap != null) {
			return trialsMap.get(experimentId);
		}
		return null;
	}

}
