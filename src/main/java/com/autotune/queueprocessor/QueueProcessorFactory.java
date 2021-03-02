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

import com.autotune.em.utils.EMUtils.EMProcessorType;

public class QueueProcessorFactory {
	
	
	public static QueueProcessor getProcessor(EMProcessorType proc) {
		QueueProcessor processor= null;
		switch (proc) {
			case DAPROCESSOR: 
				processor = new DAQueueProcessor();
				break;
			case MLPROCESSOR:
				processor = new QueueMLProcessor();
				break;
			case EXPMGRPROCESSOR:
				processor = new ExpMgrQueueProcessor();
				break;
			case RECMGRPROCESSOR:
				processor = new RecMgrQueueProcessor();
				break;
			default :
				System.out.println("not a valid processor type provided.");
				break;
		}
		return processor;
	}
}
