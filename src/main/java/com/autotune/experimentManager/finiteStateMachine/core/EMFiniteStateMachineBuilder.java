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

package com.autotune.experimentManager.finiteStateMachine.core;

import com.autotune.experimentManager.finiteStateMachine.api.EMFiniteStateMachine;
import com.autotune.experimentManager.finiteStateMachine.api.EMState;
import com.autotune.experimentManager.finiteStateMachine.api.EMTransition;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * EMFiniteStateMachineBuilder is use to initiate and build the finite state machine.
 */

public class EMFiniteStateMachineBuilder {

    private static final Logger LOGGER = Logger.getLogger(EMFiniteStateMachineBuilder.class.getName());

    static {
        try {
            if (System.getProperty("java.util.logging.config.file") == null &&
                    System.getProperty("java.util.logging.config.class") == null) {
                LogManager.getLogManager().readConfiguration(EMFiniteStateMachineBuilder.class.getResourceAsStream("/logging.properties"));
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Unable to load log configuration file", e);
        }
    }

    private final EMFiniteStateMachineImpl finiteStateMachine;
    private final EMFiniteStateMachineDefinitionValidator finiteStateMachineDefinitionValidator;
    private final EMTransitionDefinitionValidator transitionDefinitionValidator;

    /**
     * Create a new {@link EMFiniteStateMachineBuilder}.
     *
     * @param states set of the machine
     * @param initialState of the machine
     */
    public EMFiniteStateMachineBuilder(final Set<EMState> states, final EMState initialState) {
        finiteStateMachine = new EMFiniteStateMachineImpl(states, initialState);
        finiteStateMachineDefinitionValidator = new EMFiniteStateMachineDefinitionValidator();
        transitionDefinitionValidator = new EMTransitionDefinitionValidator();
    }

    /**
     * Register a transition within FSM transitions set.
     * If the transition is not valid, this method may throw an {@link IllegalArgumentException}.
     * @param transition the transition to register
     * @return a configured FSM Builder instance
     */
    public EMFiniteStateMachineBuilder registerTransition(final EMTransition transition) {
        transitionDefinitionValidator.validateTransitionDefinition(transition, finiteStateMachine);
        finiteStateMachine.registerTransition(transition);
        return this;
    }

    /**
     * Register a set of transitions within FSM transitions set.
     * If a transition is not valid, this method throws an {@link IllegalArgumentException}.
     * @param transitions the transitions set to register
     * @return a configured FSM Builder instance
     */
    public EMFiniteStateMachineBuilder registerTransitions(final Set<EMTransition> transitions) {
        for (EMTransition transition : transitions) {
            registerTransition(transition);
        }
        return this;
    }

    /**
     * Register FSM final state which is not mandatory.
     * Once in final state, the FSM will ignore all incoming events.
     * @param finalState the FSM final state
     * @return a configured FSM Builder instance
     */
    public EMFiniteStateMachineBuilder registerFinalState(final EMState finalState) {
        finiteStateMachine.registerFinalState(finalState);
        return this;
    }

    /**
     * Register FSM final states set.
     * Once in final state, the FSM will ignore all incoming events.
     * @param finalStates the FSM final states to register
     * @return a configured FSM Builder instance
     */
    public EMFiniteStateMachineBuilder registerFinalStates(final Set<EMState> finalStates) {
        for (EMState finalState : finalStates) {
            registerFinalState(finalState);
        }
        return this;
    }

    /**
     * Build a FSM instance. This method checks if FSM definition is valid.
     * If FSM state is not valid, this methods throws an {@link IllegalStateException}
     * @return a configured FSM instance
     */
    public EMFiniteStateMachine build() {
        finiteStateMachineDefinitionValidator.validateFiniteStateMachineDefinition(finiteStateMachine);
        return finiteStateMachine;
    }

}
