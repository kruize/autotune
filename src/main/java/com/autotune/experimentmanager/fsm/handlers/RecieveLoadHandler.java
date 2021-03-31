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

package com.autotune.experimentmanager.fsm.handlers;

import com.autotune.experimentmanager.fsm.api.EMEventHandler;
import com.autotune.experimentmanager.fsm.events.ReceiveLoadEvent;

/**
 * This class is responsible to generate load on the trail deployment and 
 * collect and analyze the data based on some threshold limit.
 * 
 * @author Bipin Kumar
 *
 * @Date Mar 31, 2021
 */
public class RecieveLoadHandler implements EMEventHandler<ReceiveLoadEvent>{
	
	@Override
	public void handleEvent(ReceiveLoadEvent event) throws Exception {
		System.out.println("Receive load from the monitoring MS.......lets work on this information");
	}

}
