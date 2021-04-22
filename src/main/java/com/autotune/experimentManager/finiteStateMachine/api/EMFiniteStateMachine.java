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

package com.autotune.experimentManager.finiteStateMachine.api;

import com.autotune.experimentManager.finiteStateMachine.exceptions.EMFiniteStateMachineException;

import java.util.Set;

/**
 * This interface provide all the required abstraction for the finite state machine.
 */

public interface EMFiniteStateMachine {
    /**
     * Return current FSM state.
     * @return current FSM state
     */
    EMState getCurrentState();

    /**
     * Return FSM initial state.
     * @return FSM initial state
     */
    EMState getInitialState();

    /**
     * Return FSM final states.
     * @return FSM final states
     */
    Set<EMState> getFinalStates();

    /**
     * Return FSM registered states.
     * @return FSM registered states
     */
    Set<EMState> getStates();

    /**
     * Return FSM registered transitions.
     * @return FSM registered transitions
     */
    Set<EMTransition> getTransitions();

    /**
     * Return the last triggered event.
     * @return the last triggered event
     */
    EMEvent getLastEvent();

    /**
     * Return the last transition made.
     * @return the last transition made
     */
    EMTransition getLastTransition();

    /**
     * Fire an event. According to event type, the FSM will make the right transition.
     * @param event to fire
     * @return The next FSM state defined by the transition to make
     * @throws EMFiniteStateMachineException thrown if an exception occurs during event handling
     */
    EMState fire(EMEvent event) throws EMFiniteStateMachineException;
}
