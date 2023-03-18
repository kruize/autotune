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
package com.autotune.common.queue;

import java.io.Serializable;
import java.util.concurrent.LinkedBlockingQueue;

import com.autotune.utils.KruizeUtils;
import com.autotune.utils.KruizeUtils.QueueName;

/**
 * ExpMgrQueue is singleton concrete implementation of KruizeQueue for Experiment Manager
 *  * @author bipkumar
 *
 */
public class ExperimentManagerQueue extends AutotuneQueueImpl implements Serializable {

	private static final long serialVersionUID = -6045964856984857449L;
	private static ExperimentManagerQueue instance;

	private ExperimentManagerQueue()
	{
		name = QueueName.EXPMGRQUEUE.name();
		queue = new LinkedBlockingQueue<AutotuneDTO>(KruizeUtils.INITIAL_QUEUE_CAPACITY);
	}

	public static ExperimentManagerQueue getInstance()
	{
		if (instance == null)
		{
			synchronized(ExperimentManagerQueue.class)
			{
				if (instance == null)
				{
					instance = new ExperimentManagerQueue();
				}
			}
		}

		return instance;
	}

	// implement this method to avoid the creating the copy of object from a external stream.
	private Object readResolve() {
		return getInstance();
	}
}
