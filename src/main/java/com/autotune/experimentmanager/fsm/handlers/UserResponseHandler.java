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
import com.autotune.experimentmanager.fsm.events.UserResponseEvent;
import com.autotune.experimentmanager.fsm.object.ExperimentTrialObject;

/**
 * This class is responsible to generate the user response result output.
 * @author Bipin Kumar
 *
 * @Date Mar 29, 2021
 */
public class UserResponseHandler implements EMEventHandler<UserResponseEvent>{
	
	@Override
	public void handleEvent(UserResponseEvent event) throws Exception {
		System.out.println("Start createating experiment result...");
		ExperimentTrialObject data = event.getData();
		
		//TODO implement logic
	}
	
	

}
