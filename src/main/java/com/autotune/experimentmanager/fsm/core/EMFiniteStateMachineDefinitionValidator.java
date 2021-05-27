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
package com.autotune.experimentmanager.fsm.core;

import java.util.Set;

import com.autotune.experimentmanager.fsm.api.EMFiniteStateMachine;
import com.autotune.experimentmanager.fsm.api.EMState;
import com.autotune.experimentmanager.utils.EMUtils;
/**
 * EMFiniteStateMachineDefinitionValidator is used to validate the state and there transitions between he states.
 * @author Bipin Kumar
 *
 * @Date Mar 30, 2021
 */
class EMFiniteStateMachineDefinitionValidator {

    /**
     * Deterministic FSM validation : for each state, exactly one outgoing transition for an event type must be defined.
     */
    void validateFiniteStateMachineDefinition(EMFiniteStateMachine finiteStateMachine) {

        Set<EMState> states = finiteStateMachine.getStates();

        //check if initial state belongs to FSM declared states.
        EMState initialState = finiteStateMachine.getInitialState();
        if (!states.contains(initialState)) {
            throw new IllegalStateException("Initial state '" + initialState.getName() + "' must belong to FSM states: " +
                    EMUtils.dumpFSMStates(states));
        }

        //check if registered final states belong to FSM declared states.
        for (EMState finalState : finiteStateMachine.getFinalStates()) {
            if (!states.contains(finalState)) {
                throw new IllegalStateException("Final state '" + finalState.getName() + "' must belong to FSM states: " +
                        EMUtils.dumpFSMStates(states));
            }
        }
    }
}
