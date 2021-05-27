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

package com.autotune.experimentmanager.fsm.events;

import com.autotune.experimentmanager.fsm.api.EMAbstractEvent;
import com.autotune.experimentmanager.fsm.object.ExperimentTrialObject;

/**
 * UserResponseEvent trigger when a trials completed and you would like to send final configuration matrix to the 
 * user.
 * @author Bipin Kumar
 *
 * @Date Mar 31, 2021
 */
public class UserResponseEvent extends EMAbstractEvent {
	
	private ExperimentTrialObject data;
	
	public UserResponseEvent(ExperimentTrialObject data) {
		super("UserResponseEvent");
		this.data = data;
	}
	
	public UserResponseEvent(String name) {
		super(name);
	}

	@Override
	public ExperimentTrialObject getData() {
		return data;
	}

}
