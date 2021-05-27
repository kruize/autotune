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
import com.autotune.experimentmanager.fsm.events.BenchMarkResultEvent;

/**
 * BenchMarkResultHandler handle the BenchMarkResultEvent and further do analysis of the performance
 * data received from the trail deployments.
 * @author Bipin Kumar
 *
 * @Date Mar 31, 2021
 */
public class BenchMarkResultHandler implements EMEventHandler<BenchMarkResultEvent>{
	
	@Override
	public void handleEvent(BenchMarkResultEvent event) throws Exception {
		System.out.println("Bench mark the result receieved from the prometheous from the after apply new configuration.......lets work on this information");
		
		//TODO implement the logic here.
	}

}
