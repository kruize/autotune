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

import com.autotune.experimentManager.finiteStateMachine.api.EMEventHandler;
import com.autotune.experimentManager.finiteStateMachine.api.EMState;
import com.autotune.experimentManager.finiteStateMachine.api.EMEvent;

/**
 * EMTransition create a transition from one state to other state.
 */

public interface EMTransition {
    /**
     * Return transition name.
     * @return transition name
     */
    String getName();

    /**
     * Return transition source state.
     * @return transition source state
     */
    EMState getSourceState();

    /**
     * Return transition target state.
     * @return transition target state
     */
    EMState getTargetState();

    /**
     * Return fired event type upon which the transition should be made.
     * @return Event type class
     */
    Class<? extends EMEvent> getEventType();

    /**
     * Return event handler to execute when an event is fired.
     * @return transition event handler
     */
    EMEventHandler getEventHandler();
}
