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

import com.autotune.experimentManager.finiteStateMachine.api.EMEvent;
import com.autotune.experimentManager.finiteStateMachine.api.EMEventHandler;
import com.autotune.experimentManager.finiteStateMachine.api.EMState;
import com.autotune.experimentManager.finiteStateMachine.api.EMTransition;

/**
 * EMTransitionBuilder is implementation of the transition between the two states in finite state machine.
 */

public class EMTransitionBuilder {

    private final EMTransitionImpl transition;

    /**
     * Create a new {@link EMTransitionBuilder}.
     */
    public EMTransitionBuilder() {
        transition = new EMTransitionImpl();
    }

    /**
     * Set the name of the transition.
     * @param name of the transition
     * @return FSM transition builder
     */
    public EMTransitionBuilder name(final String name) {
        transition.setName(name);
        return this;
    }

    /**
     * Set the source state of the transition.
     * @param sourceState of the transition
     * @return FSM transition builder
     */
    public EMTransitionBuilder sourceState(final EMState sourceState) {
        transition.setSourceState(sourceState);
        return this;
    }

    /**
     * Set the target state of the transition.
     * @param targetState of the transition
     * @return FSM transition builder
     */
    public EMTransitionBuilder targetState(final EMState targetState) {
        transition.setTargetState(targetState);
        return this;
    }

    /**
     * Set event type upon which the transition should be triggered.
     * @param eventType of the transition
     * @return FSM transition builder
     */
    public EMTransitionBuilder eventType(final Class<? extends EMEvent> eventType) {
        transition.setEventType(eventType);
        return this;
    }

    /**
     * Set the event handler of the transition.
     * @param eventHandler of the transition
     * @return FSM transition builder
     */
    public <E extends EMEvent> EMTransitionBuilder eventHandler(final EMEventHandler<E> eventHandler) {
        transition.setEventHandler(eventHandler);
        return this;
    }

    /**
     * Build a transition instance.
     * @return a transition instance.
     */
    public EMTransition build() {
        return transition;
    }

}
