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
package com.autotune.experimentmanager.core;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import com.autotune.experimentmanager.fsm.api.EMAbstractEvent;
import com.autotune.experimentmanager.fsm.api.EMFiniteStateMachine;
import com.autotune.experimentmanager.fsm.api.EMState;
import com.autotune.experimentmanager.fsm.api.EMTransition;
import com.autotune.experimentmanager.fsm.core.EMFiniteStateMachineBuilder;
import com.autotune.experimentmanager.fsm.core.EMTransitionBuilder;
import com.autotune.experimentmanager.fsm.events.DeployAppConfigurationEvent;
import com.autotune.experimentmanager.fsm.events.RecommendedConfigReceiveEvent;
import com.autotune.experimentmanager.fsm.events.UserResponseEvent;
import com.autotune.experimentmanager.fsm.handlers.ConfigurationReceiverHandler;
import com.autotune.experimentmanager.fsm.handlers.DeployAppConfigurationHandler;
import com.autotune.experimentmanager.fsm.handlers.UserResponseHandler;
import com.autotune.experimentmanager.fsm.object.ExperimentTrialObject;
import com.autotune.experimentmanager.utils.EMUtils.EMFSMStates;
import com.autotune.experimentmanager.utils.EMUtils.EMFSMTransition;

/**
 * RunExperiment class is a callable type to run multiple experiment in
 * parallel.
 * 
 * @author Bipin Kumar
 * 
 *
 */
public class RunExperiment implements Callable<ExperimentTrialObject> {
	private ExperimentTrialObject expTrialInput;
	private EMFiniteStateMachine fsmSimulator;

	public RunExperiment(String url) {
		expTrialInput = new ExperimentTrialObject();
		expTrialInput.setURL(url);
		fsmSimulator = initializeFSM();
	}

	@Override
	public ExperimentTrialObject call() throws Exception {
		expTrialInput.setEmFSM(fsmSimulator);
		String stateName = fsmSimulator.getCurrentState().getName();
		System.out.println("Starting state=" + stateName);
		EMAbstractEvent recomendedConfigEvent = new RecommendedConfigReceiveEvent(expTrialInput);
		fsmSimulator.fire(recomendedConfigEvent);
		System.out.println("Completed the state=" + stateName);

		stateName = fsmSimulator.getCurrentState().getName();
		System.out.println("Starting State= " + stateName);
		EMAbstractEvent deploymentEvent = new DeployAppConfigurationEvent(expTrialInput);
		fsmSimulator.fire(deploymentEvent);
		System.out.println("Completed the state=" + stateName);

		stateName = fsmSimulator.getCurrentState().getName();
		System.out.println("Starting State= " + stateName);
		EMAbstractEvent userResponseEvent = new UserResponseEvent(expTrialInput);
		fsmSimulator.fire(userResponseEvent);
		System.out.println("Completed the state=" + stateName);

		return expTrialInput;
	}

	private EMFiniteStateMachine initializeFSM() {
		// EMFSM states
		EMState recommededConfigState = new EMState(EMFSMStates.RECOMMENDED_CONFIG_STATE.name());
		EMState deployRecommendedConfigState = new EMState(EMFSMStates.DEPLOYED_RECOMMENDED_CONFIG_STATE.name());
//		EMState loadAppearingState = new EMState(EMFSMStates.LOAD_APPEARING_STATE.name());
//		EMState collectMatrixState = new EMState(EMFSMStates.COLLECT_MATRIX_STATE.name());
//		EMState benchMarkResultState = new EMState(EMFSMStates.BENCH_MARK_RESULT_STATE.name());
		EMState userResponseState = new EMState(EMFSMStates.USER_RESPONSE_STATE.name());

		Set<EMState> states = new HashSet<EMState>();
		states.add(recommededConfigState);
		states.add(deployRecommendedConfigState);
//		states.add(loadAppearingState);
//		states.add(collectMatrixState);
//		states.add(benchMarkResultState);
		states.add(userResponseState);

		// EMFSM transition
		EMTransition receivedRecAppConfigTransmission = new EMTransitionBuilder()
				.name(EMFSMTransition.RECOMMENDED_CONFIG_TRANS.name()).sourceState(recommededConfigState)
				.eventType(RecommendedConfigReceiveEvent.class).eventHandler(new ConfigurationReceiverHandler())
				.targetState(deployRecommendedConfigState).build();

		EMTransition deployedAppWithConfigTransmission = new EMTransitionBuilder()
				.name(EMFSMTransition.DEPLOYED_RECOMMENDED_CONFIG_TRANS.name())
				.sourceState(deployRecommendedConfigState).eventType(DeployAppConfigurationEvent.class)
				.eventHandler(new DeployAppConfigurationHandler()).targetState(userResponseState).build();

//		EMTransition loadAppearingTransmission = new EMTransitionBuilder().name(EMFSMTransition.LOAD_APPEARING_TRANS.name())
//				.sourceState(loadAppearingState).eventType(ReceiveLoadEvent.class).eventHandler(new RecieveLoadHandler())
//				.targetState(collectMatrixState).build();
//
//		EMTransition gatheredMatrixTramission = new EMTransitionBuilder().name(EMFSMTransition.LOAD_APPEARING_TRANS.name())
//				.sourceState(collectMatrixState).eventType(GatherMatrixEvent.class).eventHandler(new GatherMatrixHandler())
//				.targetState(benchMarkResultState).build();
//
//		EMTransition benchMarkingResultTransmission = new EMTransitionBuilder().name(EMFSMTransition.BENCH_MARK_RESULT_TRANS.name())
//				.sourceState(benchMarkResultState).eventType(BenchMarkResultEvent.class)
//				.eventHandler(new BenchMarkResultHandler()).targetState(userResponseState).build();
//
		EMTransition userResultTransmission = new EMTransitionBuilder().name(EMFSMTransition.USER_RESPONSE_TRANS.name())
				.sourceState(userResponseState).eventType(UserResponseEvent.class)
				.eventHandler(new UserResponseHandler()).targetState(userResponseState).build();

		// initialize EMFSM
		EMFiniteStateMachine fsmSimulator = new EMFiniteStateMachineBuilder(states, recommededConfigState)
				.registerTransition(receivedRecAppConfigTransmission)
				.registerTransition(deployedAppWithConfigTransmission).registerTransition(userResultTransmission)
				.build();

		return fsmSimulator;

	}

}
