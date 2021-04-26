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

package com.autotune.experimentManager.core;

import com.autotune.experimentManager.finiteStateMachine.api.EMAbstractEvent;
import com.autotune.experimentManager.finiteStateMachine.api.EMFiniteStateMachine;
import com.autotune.experimentManager.finiteStateMachine.api.EMState;
import com.autotune.experimentManager.finiteStateMachine.api.EMTransition;
import com.autotune.experimentManager.finiteStateMachine.core.EMFiniteStateMachineBuilder;
import com.autotune.experimentManager.finiteStateMachine.core.EMTransitionBuilder;
import com.autotune.experimentManager.finiteStateMachine.events.DeployAppConfigurationEvent;
import com.autotune.experimentManager.finiteStateMachine.events.RecommendedConfigReceiveEvent;
import com.autotune.experimentManager.finiteStateMachine.handlers.ConfigurationReceiverHandler;
import com.autotune.experimentManager.finiteStateMachine.handlers.DeployAppConfigurationHandler;
import com.autotune.experimentManager.finiteStateMachine.objects.ExperimentTrialObject;
import com.autotune.experimentManager.utils.EMUtils.EMFSMStates;
import com.autotune.experimentManager.utils.EMUtils.EMFSMTransition;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * RunExperiment class is a callable type to run multiple experiment in
 * parallel.
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

        return expTrialInput;
    }

    private EMFiniteStateMachine initializeFSM() {
        // EMFSM states
        EMState recommededConfigState = new EMState(EMFSMStates.RECOMMENDED_CONFIG_STATE.name());
        EMState deployRecommendedConfigState = new EMState(EMFSMStates.DEPLOYED_RECOMMENDED_CONFIG_STATE.name());

        EMState userResponseState = new EMState(EMFSMStates.USER_RESPONSE_STATE.name());

        Set<EMState> states = new HashSet<EMState>();
        states.add(recommededConfigState);
        states.add(deployRecommendedConfigState);

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


        // initialize EMFSM
        EMFiniteStateMachine fsmSimulator = new EMFiniteStateMachineBuilder(states, recommededConfigState)
                .registerTransition(receivedRecAppConfigTransmission)
                .registerTransition(deployedAppWithConfigTransmission)
                .build();

        return fsmSimulator;

    }
}
