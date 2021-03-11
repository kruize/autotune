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
/**
 * AutotuneQueue is an interface having three main contracts
 * send method: it send data to the AutotuneQueue 
 * get method: recieved data from AutotuneQueue
 * getName method: return the name of the component currently operating on AutotuneDTO object
 * @author bipkumar
 */
public interface AutotuneQueue {
	
	public boolean send(AutotuneDTO data) throws InterruptedException;
	
	public AutotuneDTO get() throws InterruptedException;
	
	public String getName();
}
