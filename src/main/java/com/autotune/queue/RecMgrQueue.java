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

import java.io.Serializable;
import java.util.concurrent.LinkedBlockingQueue;

import com.autotune.utils.AutotuneUtil;
import com.autotune.utils.AutotuneUtil.QueueName;
/**
 * RecMgrQueue is singleton concrete implementation of AutotuneQueue for Recommendation Manager.
 * @author bipkumar
 *
 */
public class RecMgrQueue extends AutotuneQueueImpl implements Serializable {
	
	private static final long serialVersionUID = -6045964856984857449L;
	private static RecMgrQueue instance;

	private RecMgrQueue()
	{
		name = QueueName.RECMGRQUEUE.name();
		queue = new LinkedBlockingQueue<AutotuneDTO>(AutotuneUtil.INITIAL_QUEUE_CAPACITY);
	}

	public static RecMgrQueue getInstance()
	{
		if (instance == null)
		{
			synchronized(RecMgrQueue.class)
			{
				if (instance == null)
				{
					instance = new RecMgrQueue();
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
