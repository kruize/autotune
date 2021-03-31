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
import com.autotune.experimentmanager.fsm.events.GatherMatrixEvent;

/**
 * This class is responsible for connecting to the trails deployment(pod) and 
 * gather the performance data.
 * 
 * @author Bipin Kumar
 */
public class GatherMatrixHandler implements EMEventHandler<GatherMatrixEvent>{
	
	@Override
	public void handleEvent(GatherMatrixEvent event) throws Exception {
		//TODO
		System.out.println("Connect to the Matrix tool like Prometheous and get matrix.......lets work on this information");
	}

}
