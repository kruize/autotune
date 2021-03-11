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
package com.autotune.queue;

import java.util.concurrent.Callable;
/**
 * EventProducer is a collable which will run in a separate 
 * thread and push the data into the AutotuneQueue.
 * @author bipkumar
 *
 */
public class EventProducer implements Callable {
	
	private final AutotuneQueue queue;
	private final AutotuneDTO inputDTO;
	
	public EventProducer(AutotuneQueue queue, AutotuneDTO inputData) {
		this.queue = queue;
		this.inputDTO = inputData;
	}
	
	@Override
	public Boolean call() {
		try {
			return queue.send(inputDTO);

		} catch (InterruptedException e) {
			System.out.println("Adding Thread was interrupted");
		}

		return false;
	}
}
