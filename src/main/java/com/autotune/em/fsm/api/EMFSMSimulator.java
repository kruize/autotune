package com.autotune.em.fsm.api;

import java.util.HashSet;
import java.util.Set;

import com.autotune.em.fsm.core.EMFiniteStateMachineBuilder;
import com.autotune.em.fsm.core.EMTransitionBuilder;
import com.autotune.em.fsm.events.DeployAppConfigurationEvent;
import com.autotune.em.fsm.events.RecommendedConfigReceiveEvent;
import com.autotune.em.fsm.handlers.ConfigurationReceiverHandler;
import com.autotune.em.fsm.handlers.DeployAppConfigurationHandler;
import com.autotune.em.utils.EMUtils.EMFSMStates;
import com.autotune.em.utils.EMUtils.EMFSMTransition;

public class EMFSMSimulator {
	private final EMFiniteStateMachine emFSMSimulator;
	
	public EMFSMSimulator() {
		// EMFSM states
		EMState recommededConfig = new EMState(EMFSMStates.RECOMMENDED_CONFIG_STATE.name());
		EMState deployConfig = new EMState(EMFSMStates.DEPLOYED_RECOMMENDED_CONFIG_STATE.name());
//		EMState loadAppearing = new EMState("load-appearing");
//		EMState gathereMatrix = new EMState("gather-matrix");
//		EMState benchMarkResult = new EMState("bench-mark-result");
//		EMState userResponse = new EMState("send-user-response");

		Set<EMState> states = new HashSet<EMState>();
		states.add(recommededConfig);
		states.add(deployConfig);
//		states.add(loadAppearing);
//		states.add(gathereMatrix);
//		states.add(benchMarkResult);
//		states.add(userResponse);

		// EMFSM transition
		//update
		EMTransition receivedRecAppConfigTransmission = new EMTransitionBuilder()
				.name(EMFSMTransition.RECOMMENDED_CONFIG_TRANS.name())
				.sourceState(recommededConfig)
				.eventType(RecommendedConfigReceiveEvent.class)
				.eventHandler(new ConfigurationReceiverHandler())
				.targetState(deployConfig)
				.build();

		EMTransition deployedAppWithConfigTransmission = new EMTransitionBuilder()
				.name(EMFSMTransition.DEPLOYED_RECOMMENDED_CONFIG_TRANS.name())
				.sourceState(deployConfig)
				.eventType(DeployAppConfigurationEvent.class)
				.eventHandler(new DeployAppConfigurationHandler())
				.targetState(deployConfig)
				.build();

//		EMTransition loadAppearingTransmission = new EMTransitionBuilder()
//				.name("loadAppearing")
//				.sourceState(loadAppearing)
//				.eventType(ReceiveLoadEvent.class)
//				.eventHandler(new RecieveLoadHandler())
//				.targetState(gathereMatrix)
//				.build();
//
//		EMTransition gatheredMatrixTramission = new EMTransitionBuilder()
//				.name("gatheredMatrix")
//				.sourceState(gathereMatrix)
//				.eventType(GatherMatrixEvent.class)
//				.eventHandler(new GatherMatrixHandler())
//				.targetState(benchMarkResult)
//				.build();
//
//		EMTransition benchMarkingResultTransmission = new EMTransitionBuilder()
//				.name("userresponse")
//				.sourceState(benchMarkResult)
//				.eventType(BenchMarkResultEvent.class)
//				.eventHandler(new BenchMarkResultHandler())
//				.targetState(userResponse)
//				.build();
//		
//		EMTransition userResultTransmission = new EMTransitionBuilder()
//				.name("userresponse")
//				.sourceState(userResponse)
//				.eventType(UserResponseEvent.class)
//				.eventHandler(new UserResponseHandler())
//				.targetState(userResponse)
//				.build();
		
		//initialize EMFSM 
        emFSMSimulator = new EMFiniteStateMachineBuilder(states, recommededConfig)
                .registerTransition(receivedRecAppConfigTransmission)
                .registerTransition(deployedAppWithConfigTransmission)
//                .registerTransition(loadAppearingTransmission)
//                .registerTransition(gatheredMatrixTramission)
//                .registerTransition(benchMarkingResultTransmission)
//                .registerTransition(userResultTransmission)
                .build();
	}
	
	public EMFiniteStateMachine getEMFiniteStateMachine() {
		return emFSMSimulator;
	}
}
